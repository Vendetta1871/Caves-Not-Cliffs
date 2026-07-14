package net.celestiald.cavesnotcliffs.migration;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/** Disk-backed cube NBT index that bounds importer heap use to coordinates and record pointers. */
final class CubicNbtSpool implements Closeable {
    static final class ColumnKey implements Comparable<ColumnKey> {
        private final int x;
        private final int z;

        ColumnKey(int x, int z) {
            this.x = x;
            this.z = z;
        }

        int getX() {
            return x;
        }

        int getZ() {
            return z;
        }

        @Override
        public int compareTo(ColumnKey other) {
            int byX = Integer.compare(x, other.x);
            return byX != 0 ? byX : Integer.compare(z, other.z);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || other instanceof ColumnKey
                    && x == ((ColumnKey) other).x && z == ((ColumnKey) other).z;
        }

        @Override
        public int hashCode() {
            return 31 * x + z;
        }

        @Override
        public String toString() {
            return "(" + x + "," + z + ")";
        }
    }

    private static final int MAX_COMPRESSED_RECORD_BYTES = 64 * 1024 * 1024;

    private final RandomAccessFile file;
    private final int maxRecordBytes;
    private final long maxTotalBytes;
    private final NavigableMap<ColumnKey, NavigableMap<Integer, Pointer>> columns =
            new TreeMap<ColumnKey, NavigableMap<Integer, Pointer>>();
    private long cubeCount;
    private boolean forced;
    private boolean closed;

    CubicNbtSpool(Path path) throws IOException {
        this(path, MAX_COMPRESSED_RECORD_BYTES, Long.MAX_VALUE);
    }

    CubicNbtSpool(Path path, int maxRecordBytes, long maxTotalBytes) throws IOException {
        if (maxRecordBytes <= 0) {
            throw new IllegalArgumentException("maxRecordBytes must be positive");
        }
        if (maxTotalBytes <= 0L) {
            throw new IllegalArgumentException("maxTotalBytes must be positive");
        }
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        this.maxRecordBytes = maxRecordBytes;
        this.maxTotalBytes = maxTotalBytes;
        file = new RandomAccessFile(path.toFile(), "rw");
        file.setLength(0L);
    }

    void append(int cubeX, int cubeY, int cubeZ, NBTTagCompound nbt) throws IOException {
        requireOpen();
        if (forced) {
            throw new IOException("Cannot append after the cubic NBT spool has been forced");
        }
        ColumnKey key = new ColumnKey(cubeX, cubeZ);
        NavigableMap<Integer, Pointer> cubes = columns.get(key);
        if (cubes != null && cubes.containsKey(cubeY)) {
            throw new IOException("Duplicate cubic NBT record for cube (" + cubeX + ","
                    + cubeY + "," + cubeZ + ")");
        }

        long recordOffset = file.length();
        try {
            if (recordOffset > maxTotalBytes - Integer.BYTES) {
                throw new IOException("Cubic NBT spool exceeds its total limit of "
                        + maxTotalBytes + " bytes");
            }
            file.seek(recordOffset);
            file.writeInt(0);
            RecordOutputStream output = new RecordOutputStream(
                    recordOffset, cubeX, cubeY, cubeZ);
            CompressedStreamTools.writeCompressed(nbt, output);
            int compressedLength = output.getWritten();
            if (compressedLength <= 0) {
                throw new IOException("Compressed cube (" + cubeX + "," + cubeY + "," + cubeZ
                        + ") has unsafe spool length " + compressedLength);
            }
            file.seek(recordOffset);
            file.writeInt(compressedLength);
            file.seek(recordOffset + Integer.BYTES + compressedLength);

            Pointer pointer = new Pointer(recordOffset, compressedLength);
            if (cubes == null) {
                cubes = new TreeMap<Integer, Pointer>();
                cubes.put(cubeY, pointer);
                columns.put(key, cubes);
            } else {
                cubes.put(cubeY, pointer);
            }
            cubeCount++;
        } catch (IOException | RuntimeException exception) {
            rollback(recordOffset, key, cubes, cubeY, exception);
            throw exception;
        }
    }

    void force() throws IOException {
        requireOpen();
        file.getFD().sync();
        forced = true;
    }

    long getCubeCount() {
        return cubeCount;
    }

    boolean containsColumn(int cubeX, int cubeZ) {
        return columns.containsKey(new ColumnKey(cubeX, cubeZ));
    }

    Map<Integer, NBTTagCompound> takeColumn(int cubeX, int cubeZ) throws IOException {
        requireReadable();
        ColumnKey key = new ColumnKey(cubeX, cubeZ);
        NavigableMap<Integer, Pointer> pointers = columns.remove(key);
        if (pointers == null) {
            return Collections.emptyMap();
        }
        NavigableMap<Integer, NBTTagCompound> result =
                new TreeMap<Integer, NBTTagCompound>();
        for (Map.Entry<Integer, Pointer> entry : pointers.entrySet()) {
            result.put(entry.getKey(), read(entry.getValue()));
        }
        return result;
    }

    List<ColumnKey> remainingColumns() {
        return Collections.unmodifiableList(new ArrayList<ColumnKey>(columns.keySet()));
    }

    private NBTTagCompound read(Pointer pointer) throws IOException {
        file.seek(pointer.recordOffset);
        int length;
        try {
            length = file.readInt();
        } catch (EOFException exception) {
            throw new IOException("Cubic NBT spool ended before record length at "
                    + pointer.recordOffset, exception);
        }
        if (length != pointer.compressedLength || length <= 0
                || length > maxRecordBytes) {
            throw new IOException("Cubic NBT spool record at " + pointer.recordOffset
                    + " declares " + length + " bytes; expected " + pointer.compressedLength);
        }
        byte[] compressed = new byte[length];
        file.readFully(compressed);
        return CompressedStreamTools.readCompressed(new ByteArrayInputStream(compressed));
    }

    private void rollback(long recordOffset, ColumnKey key,
            NavigableMap<Integer, Pointer> cubes, int cubeY, Throwable failure) {
        try {
            file.setLength(recordOffset);
            file.seek(recordOffset);
        } catch (IOException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        }
        if (cubes != null) {
            cubes.remove(cubeY);
            if (cubes.isEmpty()) {
                columns.remove(key);
            }
        }
    }

    private void requireReadable() throws IOException {
        requireOpen();
        if (!forced) {
            throw new IOException("Cubic NBT spool must be forced before records are read");
        }
    }

    private void requireOpen() throws IOException {
        if (closed) {
            throw new IOException("Cubic NBT spool is closed");
        }
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            file.close();
        }
    }

    private static final class Pointer {
        private final long recordOffset;
        private final int compressedLength;

        private Pointer(long recordOffset, int compressedLength) {
            this.recordOffset = recordOffset;
            this.compressedLength = compressedLength;
        }
    }

    private final class RecordOutputStream extends OutputStream {
        private final long payloadOffset;
        private final String coordinates;
        private int written;

        private RecordOutputStream(long recordOffset, int cubeX, int cubeY, int cubeZ) {
            payloadOffset = recordOffset + Integer.BYTES;
            coordinates = "(" + cubeX + "," + cubeY + "," + cubeZ + ")";
        }

        @Override
        public void write(int value) throws IOException {
            reserve(1);
            file.write(value);
            written++;
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            if (bytes == null) {
                throw new NullPointerException("bytes");
            }
            if (offset < 0 || length < 0 || offset > bytes.length - length) {
                throw new IndexOutOfBoundsException();
            }
            reserve(length);
            file.write(bytes, offset, length);
            written += length;
        }

        private void reserve(int length) throws IOException {
            if (length > maxRecordBytes - written) {
                throw new IOException("Compressed cube " + coordinates
                        + " exceeds the spool record limit of " + maxRecordBytes + " bytes");
            }
            long totalWritten = payloadOffset + written;
            if ((long) length > maxTotalBytes - totalWritten) {
                throw new IOException("Cubic NBT spool exceeds its total limit of "
                        + maxTotalBytes + " bytes");
            }
        }

        private int getWritten() {
            return written;
        }

        @Override
        public void close() {
            // CompressedStreamTools closes its destination; the spool owns the file lifecycle.
        }
    }
}
