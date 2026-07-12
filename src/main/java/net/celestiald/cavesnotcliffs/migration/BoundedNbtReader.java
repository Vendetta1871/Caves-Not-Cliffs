package net.celestiald.cavesnotcliffs.migration;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.GZIPInputStream;

/** Strict bounded reader for importer-owned and legacy save sidecar NBT. */
final class BoundedNbtReader {
    static final long MAX_COMPRESSED_BYTES = 64L * 1024L * 1024L;
    static final long MAX_DECOMPRESSED_BYTES = 64L * 1024L * 1024L;

    private BoundedNbtReader() {
    }

    static NBTTagCompound readCompressed(Path path, String description) throws IOException {
        return readCompressed(path, description,
                MAX_COMPRESSED_BYTES, MAX_DECOMPRESSED_BYTES);
    }

    static NBTTagCompound readCompressed(Path path, String description,
            long compressedLimit, long decompressedLimit) throws IOException {
        return read(path, description, true, compressedLimit, decompressedLimit);
    }

    static NBTTagCompound readUncompressed(Path path, String description) throws IOException {
        return read(path, description, false,
                MAX_DECOMPRESSED_BYTES, MAX_DECOMPRESSED_BYTES);
    }

    private static NBTTagCompound read(Path path, String description, boolean compressed,
            long encodedLimit, long decompressedLimit) throws IOException {
        if (encodedLimit <= 0L || decompressedLimit <= 0L) {
            throw new IllegalArgumentException("NBT limits must be positive");
        }
        Path normalized = path.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalized, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(normalized)) {
            throw new IOException(description + " is not a regular file (symbolic links are "
                    + "not allowed): " + normalized);
        }

        try (SeekableByteChannel channel = openNoFollow(normalized, description)) {
            long encodedSize = channel.size();
            if (encodedSize <= 0L) {
                throw new IOException(description + " is empty: " + normalized);
            }
            if (encodedSize > encodedLimit) {
                throw new IOException(description + " exceeds the encoded input limit of "
                        + encodedLimit + " bytes at " + normalized + " (actual "
                        + encodedSize + " bytes)");
            }

            InputStream raw = Channels.newInputStream(channel);
            InputStream encoded = new LimitedInputStream(raw, encodedLimit,
                    description + " encoded input at " + normalized);
            InputStream decoded = compressed ? new GZIPInputStream(encoded) : encoded;
            DecodedBuffer buffer = new DecodedBuffer();
            try (InputStream bounded = new LimitedInputStream(decoded, decompressedLimit,
                    description + " decompressed NBT at " + normalized)) {
                byte[] transfer = new byte[8192];
                int read;
                while ((read = bounded.read(transfer)) != -1) {
                    buffer.write(transfer, 0, read);
                }
            }
            buffer.validateSingleRoot(description, normalized);

            try (DataInputStream input = buffer.open()) {
                NBTTagCompound root;
                try {
                    root = CompressedStreamTools.read(
                            input, new NBTSizeTracker(decompressedLimit));
                } catch (RuntimeException exception) {
                    throw new IOException(description + " could not be decoded within the "
                            + "decompressed NBT limit of " + decompressedLimit + " bytes at "
                            + normalized, exception);
                }
                if (input.read() != -1) {
                    throw new IOException(description + " parser left trailing decompressed "
                            + "bytes after its root NBT tag at " + normalized);
                }
                return root;
            }
        }
    }

    private static SeekableByteChannel openNoFollow(Path path, String description)
            throws IOException {
        try {
            return Files.newByteChannel(path,
                    StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
        } catch (UnsupportedOperationException exception) {
            throw new IOException("The filesystem cannot guarantee no-follow reads for "
                    + description + " at " + path, exception);
        }
    }

    private static final class LimitedInputStream extends InputStream {
        private final InputStream delegate;
        private final long limit;
        private final String description;
        private long count;

        private LimitedInputStream(InputStream delegate, long limit, String description) {
            this.delegate = delegate;
            this.limit = limit;
            this.description = description;
        }

        @Override
        public int read() throws IOException {
            int value = delegate.read();
            if (value >= 0) {
                account(1L);
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (buffer == null) {
                throw new NullPointerException("buffer");
            }
            if (offset < 0 || length < 0 || offset > buffer.length - length) {
                throw new IndexOutOfBoundsException();
            }
            if (length == 0) {
                return 0;
            }
            long remaining = limit - count;
            int allowed = (int) Math.min((long) length, remaining + 1L);
            int read = delegate.read(buffer, offset, allowed);
            if (read > 0) {
                account(read);
            }
            return read;
        }

        private void account(long bytes) throws IOException {
            count += bytes;
            if (count > limit) {
                throw new IOException(description + " exceeds its limit of " + limit
                        + " bytes");
            }
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }

    /**
     * Preflights collection lengths before Minecraft's 1.12 NBT classes allocate them. Its array
     * trackers multiply lengths as {@code int} before widening, so the stock size tracker alone can
     * be bypassed by an overflow-sized declared length in a tiny file.
     */
    private static final class DecodedBuffer extends ByteArrayOutputStream {
        private DataInputStream open() {
            return new DataInputStream(new ByteArrayInputStream(buf, 0, count));
        }

        private void validateSingleRoot(String description, Path path) throws IOException {
            try (DataInputStream input = open()) {
                int type = input.readUnsignedByte();
                if (type != 10) {
                    throw malformed(description, path,
                            "root tag type is " + type + ", expected compound type 10");
                }
                skipUtf(input, description, path);
                validatePayload(input, type, 0, description, path);
                int trailing = input.available();
                if (trailing != 0) {
                    throw new IOException(description + " contains " + trailing
                            + " trailing decompressed bytes after its root NBT tag at " + path);
                }
            }
        }

        private static void validatePayload(DataInputStream input, int type, int depth,
                String description, Path path) throws IOException {
            if (depth > 512) {
                throw malformed(description, path, "NBT depth exceeds 512");
            }
            switch (type) {
                case 1:
                    skip(input, 1L, description, path);
                    return;
                case 2:
                    skip(input, 2L, description, path);
                    return;
                case 3:
                case 5:
                    skip(input, 4L, description, path);
                    return;
                case 4:
                case 6:
                    skip(input, 8L, description, path);
                    return;
                case 7:
                    validateArray(input, 1, description, path);
                    return;
                case 8:
                    skipUtf(input, description, path);
                    return;
                case 9:
                    validateList(input, depth, description, path);
                    return;
                case 10:
                    validateCompound(input, depth, description, path);
                    return;
                case 11:
                    validateArray(input, Integer.BYTES, description, path);
                    return;
                case 12:
                    validateArray(input, Long.BYTES, description, path);
                    return;
                default:
                    throw malformed(description, path, "unsupported tag type " + type);
            }
        }

        private static void validateCompound(DataInputStream input, int depth,
                String description, Path path) throws IOException {
            while (true) {
                int type = input.readUnsignedByte();
                if (type == 0) {
                    return;
                }
                if (type > 12) {
                    throw malformed(description, path, "unsupported compound tag type " + type);
                }
                skipUtf(input, description, path);
                validatePayload(input, type, depth + 1, description, path);
            }
        }

        private static void validateList(DataInputStream input, int depth,
                String description, Path path) throws IOException {
            int type = input.readUnsignedByte();
            int length = input.readInt();
            if (length < 0 || type == 0 && length != 0 || type > 12) {
                throw malformed(description, path, "invalid list type/length "
                        + type + '/' + length);
            }
            if (length > 0 && (long) length * minimumPayloadBytes(type) > input.available()) {
                throw malformed(description, path, "list length " + length
                        + " exceeds the remaining payload");
            }
            for (int index = 0; index < length; index++) {
                validatePayload(input, type, depth + 1, description, path);
            }
        }

        private static void validateArray(DataInputStream input, int bytesPerEntry,
                String description, Path path) throws IOException {
            int length = input.readInt();
            if (length < 0) {
                throw malformed(description, path, "negative array length " + length);
            }
            long bytes = (long) length * bytesPerEntry;
            skip(input, bytes, description, path);
        }

        private static int minimumPayloadBytes(int type) {
            switch (type) {
                case 1:
                    return 1;
                case 2:
                case 8:
                    return 2;
                case 3:
                case 5:
                case 7:
                case 11:
                case 12:
                    return 4;
                case 4:
                case 6:
                    return 8;
                case 9:
                    return 5;
                case 10:
                    return 1;
                default:
                    return Integer.MAX_VALUE;
            }
        }

        private static void skipUtf(DataInputStream input, String description, Path path)
                throws IOException {
            skip(input, input.readUnsignedShort(), description, path);
        }

        private static void skip(DataInputStream input, long bytes,
                String description, Path path) throws IOException {
            if (bytes < 0L || bytes > input.available()) {
                throw malformed(description, path, "declared payload length " + bytes
                        + " exceeds the remaining " + input.available() + " bytes");
            }
            int remaining = (int) bytes;
            while (remaining > 0) {
                int skipped = input.skipBytes(remaining);
                if (skipped <= 0) {
                    throw malformed(description, path, "could not skip declared payload");
                }
                remaining -= skipped;
            }
        }

        private static IOException malformed(String description, Path path, String message) {
            return new IOException(description + " has malformed bounded NBT at " + path
                    + ": " + message);
        }
    }
}
