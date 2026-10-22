package net.celestiald.cavesnotcliffs.worldgen.v118;

import net.minecraftforge.fml.common.Loader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Optional AVX2 sampler for the density router's noises ({@code src/main/native/cncnoise.cpp}).
 *
 * <p>The kernels reproduce the Java arithmetic bit for bit, so a world generates identically
 * with or without them; the Java classes stay the reference and the fallback. The library is
 * used only on x86-64 Linux and Windows CPUs with AVX2, after it agrees with the Java samplers
 * on a self-test. {@code -Dcavesnotcliffs.nativeNoise=false} forces the Java path, and so does
 * a NoiseThreader install: one noise accelerator per pack is enough.</p>
 *
 * <p>Each accelerated sampler copies its octave tables into a direct buffer in the kernels'
 * layout and passes the buffer's address on every call. The sampler keeps the buffer
 * reachable; router noises live as long as their generator.</p>
 */
public final class NativeNoise {
    public static final String PROPERTY = "cavesnotcliffs.nativeNoise";
    /** NoiseThreader already accelerates noise generation; the sampler stands down for it. */
    private static final String NOISE_THREADER = "noisethreader";
    static final String RESOURCE_ROOT = "/net/celestiald/cavesnotcliffs/natives/";
    /** Must equal ABI_VERSION in cncnoise.cpp. */
    private static final int ABI_VERSION = 1;
    private static final int LANES = 4;
    private static final int GROUP_BYTES = 1280;
    private static final int NORMAL_HEADER_BYTES = 32;
    private static final int BLENDED_HEADER_BYTES = 64;
    private static final int MAX_NORMAL_LANES = 64;
    private static final int ALIGNMENT = 64;

    static final boolean AVAILABLE;
    private static final String STATUS;

    static {
        String failure;
        try {
            failure = load();
        } catch (Throwable throwable) {
            failure = throwable.toString();
        }
        AVAILABLE = failure == null;
        STATUS = AVAILABLE ? "native AVX2 noise sampler (" + platform() + ")"
            : "Java noise sampler: " + failure;
    }

    private NativeNoise() {
    }

    /** One line describing the active noise sampler, for the log. */
    public static String status() {
        return STATUS;
    }

    private static native int abiVersion();

    private static native int supportsAvx2();

    private static native long address(ByteBuffer buffer);

    static native double normal(long handle, double x, double y, double z);

    static native double blended(long handle, int blockX, int blockY, int blockZ);

    /** A present octave with the factors its owner applies to it, in summation order. */
    static final class Octave {
        private final ImprovedNoise noise;
        private final double coordinateScale;
        private final double inputFactor;
        private final double valueFactor;
        private final double amplitude;

        Octave(ImprovedNoise noise, double coordinateScale, double inputFactor,
                double valueFactor, double amplitude) {
            this.noise = noise;
            this.coordinateScale = coordinateScale;
            this.inputFactor = inputFactor;
            this.valueFactor = valueFactor;
            this.amplitude = amplitude;
        }
    }

    /** A sampler's octave tables in the kernels' layout; valid while this object is reachable. */
    static final class Blob {
        private final ByteBuffer storage;
        final long handle;

        private Blob(ByteBuffer storage, long handle) {
            this.storage = storage;
            this.handle = handle;
        }
    }

    /**
     * NormalNoise layout: the two Perlin stacks' octaves share the lanes, first stack first;
     * {@code null} when they exceed the kernel's lane budget.
     */
    static Blob exportNormal(PerlinNoise first, PerlinNoise second, double secondInputFactor,
            double valueFactor) {
        List<Octave> octaves = new ArrayList<Octave>();
        first.collectNativeOctaves(octaves, 1.0D);
        int firstCount = octaves.size();
        second.collectNativeOctaves(octaves, secondInputFactor);
        if (octaves.size() > MAX_NORMAL_LANES) {
            return null;
        }
        int groups = (octaves.size() + LANES - 1) / LANES;
        ByteBuffer out = ByteBuffer.allocateDirect(NORMAL_HEADER_BYTES + groups * GROUP_BYTES
            + ALIGNMENT);
        Blob blob = align(out);
        out.putDouble(valueFactor)
            .putInt(firstCount)
            .putInt(octaves.size() - firstCount)
            .putInt(groups)
            .putInt(0)
            .putLong(0L);
        putGroups(out, octaves);
        return blob;
    }

    /** BlendedNoise layout: 8 main, 16 min-limit and 16 max-limit octaves. */
    static Blob exportBlended(double xzScale, double yScale, double xzMainScale,
            double yMainScale, int cellWidth, int cellHeight, List<Octave> main,
            List<Octave> minLimit, List<Octave> maxLimit) {
        if (main.size() != 8 || minLimit.size() != 16 || maxLimit.size() != 16) {
            throw new IllegalArgumentException("BlendedNoise needs 8/16/16 octaves");
        }
        ByteBuffer out = ByteBuffer.allocateDirect(BLENDED_HEADER_BYTES + 10 * GROUP_BYTES
            + ALIGNMENT);
        Blob blob = align(out);
        out.putDouble(xzScale)
            .putDouble(yScale)
            .putDouble(xzMainScale)
            .putDouble(yMainScale)
            .putInt(cellWidth)
            .putInt(cellHeight)
            .putLong(0L)
            .putLong(0L)
            .putLong(0L);
        putGroups(out, main);
        putGroups(out, minLimit);
        putGroups(out, maxLimit);
        return blob;
    }

    /** Positions the buffer at its first aligned byte and returns that byte's address. */
    private static Blob align(ByteBuffer buffer) {
        buffer.order(ByteOrder.nativeOrder());
        long base = address(buffer);
        if (base == 0L) {
            throw new IllegalStateException("Direct buffer has no address");
        }
        int offset = (int) ((ALIGNMENT - (base & (ALIGNMENT - 1))) & (ALIGNMENT - 1));
        buffer.position(offset);
        return new Blob(buffer, base + offset);
    }

    /** Four octaves per group, structure-of-arrays; unused lanes are zero and never summed. */
    private static void putGroups(ByteBuffer out, List<Octave> octaves) {
        for (int first = 0; first < octaves.size(); first += LANES) {
            Octave[] lanes = new Octave[LANES];
            for (int lane = 0; lane < LANES && first + lane < octaves.size(); ++lane) {
                lanes[lane] = octaves.get(first + lane);
            }
            for (Octave octave : lanes) {
                out.putDouble(octave == null ? 0.0D : octave.noise.xo);
            }
            for (Octave octave : lanes) {
                out.putDouble(octave == null ? 0.0D : octave.noise.yo);
            }
            for (Octave octave : lanes) {
                out.putDouble(octave == null ? 0.0D : octave.noise.zo);
            }
            for (Octave octave : lanes) {
                out.putDouble(octave == null ? 0.0D : octave.coordinateScale);
            }
            for (Octave octave : lanes) {
                out.putDouble(octave == null ? 0.0D : octave.inputFactor);
            }
            for (Octave octave : lanes) {
                out.putDouble(octave == null ? 0.0D : octave.valueFactor);
            }
            for (Octave octave : lanes) {
                out.putDouble(octave == null ? 0.0D : octave.amplitude);
            }
            for (int lane = 0; lane < LANES; ++lane) {
                out.putDouble(0.0D);
            }
            for (Octave octave : lanes) {
                if (octave == null) {
                    out.put(new byte[256]);
                } else {
                    octave.noise.putPermutation(out);
                }
            }
        }
    }

    /**
     * NoiseThreader detection. Unit tests load this class outside a Forge runtime, where the
     * mod registry cannot be built; there the sampler is judged on its own merits.
     */
    private static boolean noiseThreaderInstalled() {
        try {
            return Loader.isModLoaded(NOISE_THREADER);
        } catch (Throwable noForgeRuntime) {
            return false;
        }
    }

    private static String load() throws IOException {
        if (!Boolean.parseBoolean(System.getProperty(PROPERTY, "true"))) {
            return "disabled by -D" + PROPERTY + "=false";
        }
        if (noiseThreaderInstalled()) {
            return "NoiseThreader is installed";
        }
        String platform = platform();
        if (platform == null) {
            return "no native library for " + System.getProperty("os.name") + " "
                + System.getProperty("os.arch");
        }
        String fileName = libraryFileName(platform);
        byte[] library;
        InputStream in = NativeNoise.class.getResourceAsStream(
            RESOURCE_ROOT + platform + "/" + fileName);
        if (in == null) {
            return "the mod jar has no " + platform + " library";
        }
        try {
            library = readFully(in);
        } finally {
            in.close();
        }
        File file = extract(library, fileName);
        System.load(file.getAbsolutePath());
        int abi = abiVersion();
        if (abi != ABI_VERSION) {
            return "library ABI " + abi + " does not match " + ABI_VERSION;
        }
        if (supportsAvx2() == 0) {
            return "CPU or OS without AVX2";
        }
        return selfTest();
    }

    static String platform() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (!arch.equals("amd64") && !arch.equals("x86_64")) {
            return null;
        }
        if (os.startsWith("windows")) {
            return "windows-x86_64";
        }
        if (os.startsWith("linux")) {
            return "linux-x86_64";
        }
        return null;
    }

    static String libraryFileName(String platform) {
        return platform.startsWith("windows") ? "cavesnotcliffs_noise.dll"
            : "libcavesnotcliffs_noise.so";
    }

    /**
     * Writes the library under a content-addressed temp directory, so instances of different
     * mod versions never overwrite a library another process has loaded (Windows locks it).
     */
    private static File extract(byte[] library, String fileName) throws IOException {
        String user = System.getProperty("user.name", "user").replaceAll("[^A-Za-z0-9._-]", "_");
        File directory = new File(System.getProperty("java.io.tmpdir"),
            "cavesnotcliffs-natives-" + user + File.separator + sha1(library).substring(0, 16));
        File file = new File(directory, fileName);
        if (file.isFile() && Arrays.equals(Files.readAllBytes(file.toPath()), library)) {
            return file;
        }
        if (!directory.isDirectory() && !directory.mkdirs() && !directory.isDirectory()) {
            throw new IOException("Cannot create " + directory);
        }
        File temporary = File.createTempFile(fileName, ".tmp", directory);
        try {
            OutputStream out = new FileOutputStream(temporary);
            try {
                out.write(library);
            } finally {
                out.close();
            }
            try {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException raced) {
                // Another instance extracted the same bytes first; theirs is as good as ours.
                if (!file.isFile() || !Arrays.equals(Files.readAllBytes(file.toPath()), library)) {
                    throw raced;
                }
            }
        } finally {
            temporary.delete();
        }
        return file;
    }

    /**
     * Compares the kernels with the Java samplers before trusting them, including far-out
     * coordinates where the wrap and floor edges live. Returns null when they agree.
     */
    private static String selfTest() {
        XoroshiroRandomSource random = new XoroshiroRandomSource(0x5EEDL);
        PositionalRandomFactory positional = random.forkPositional();
        String[] noiseNames = {"continentalness", "jagged", "cave_entrance", "noodle"};
        double[] coordinates = {0.0D, -0.5D, 1.25D, 4096.75D, -123456.789D, 1.0E6D + 0.5D,
            29999983.0D, -29999984.25D, 3.0E7D * 1.0181268882175227D};
        for (String name : noiseNames) {
            NormalNoise noise = NormalNoise.create(positional.fromHashOf(name),
                V118NoiseParameters.get(name));
            Blob blob = noise.exportNative();
            if (blob == null) {
                return name + " was not exported";
            }
            for (double x : coordinates) {
                for (double y : new double[] {-64.0D, 0.3D, 319.0D}) {
                    for (double z : coordinates) {
                        double expected = noise.sampleJava(x, y, z);
                        double actual = normal(blob.handle, x, y, z);
                        if (Double.doubleToRawLongBits(expected)
                                != Double.doubleToRawLongBits(actual)) {
                            return name + " at " + x + ", " + y + ", " + z;
                        }
                    }
                }
            }
        }
        BlendedNoise blendedNoise = new BlendedNoise(positional.fromHashOf("terrain"),
            V118NoiseSettings.overworld(false).sampling(), 4, 8, false);
        Blob blob = blendedNoise.exportNative();
        if (blob == null) {
            return "terrain was not exported";
        }
        int[] blocks = {0, -1, 7, -9, 4097, -123457, 29999983, -29999984};
        for (int x : blocks) {
            for (int y : new int[] {-64, -1, 0, 63, 319}) {
                for (int z : blocks) {
                    double expected = blendedNoise.computeJava(x, y, z);
                    double actual = blended(blob.handle, x, y, z);
                    if (Double.doubleToRawLongBits(expected)
                            != Double.doubleToRawLongBits(actual)) {
                        return "terrain at " + x + ", " + y + ", " + z;
                    }
                }
            }
        }
        return null;
    }

    private static byte[] readFully(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static String sha1(byte[] bytes) {
        try {
            StringBuilder hex = new StringBuilder();
            for (byte value : MessageDigest.getInstance("SHA-1").digest(bytes)) {
                hex.append(String.format(Locale.ROOT, "%02x", value & 0xFF));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
