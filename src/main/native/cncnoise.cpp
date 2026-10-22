// AVX2 sampler for the 1.18.2 noise primitives (see NativeNoise.java).
//
// Every result must match the Java port bit for bit: worlds generated with and without the
// native library have to be identical. The kernels therefore repeat the Java expressions in
// the same evaluation order, with no FMA contraction (-ffp-contract=off) and no fast-math.
// Vector lanes only parallelize independent octaves; every sum is still accumulated serially
// in octave order, exactly like the Java loops.
//
// The library is freestanding: no C runtime, no libstdc++, no imports at all. jni.h is not
// used either (it pulls in <stdio.h>), so the few JNI types are declared below.

#include <stdint.h>
#if defined(_MSC_VER)
// Clang targeting MSVC defines _MSC_VER, and its immintrin.h (through clang 19) hides every
// intrinsic header behind the matching feature macro. The kernels enable AVX2 per function
// with target attributes instead of -mavx2, so define the macros here. They only control
// which declarations the headers show; the code generator still emits AVX2 instructions
// exclusively inside the target("avx2") functions below.
#define __AVX__ 1
#define __AVX2__ 1
#define __SSE4_1__ 1
#endif
#include <immintrin.h>
#include <cpuid.h>

#if defined(_WIN32)
#define CNC_EXPORT extern "C" __declspec(dllexport)
// The MSVC ABI expects this symbol in any image that uses floating point.
extern "C" int _fltused = 0;
#else
#define CNC_EXPORT extern "C" __attribute__((visibility("default")))
#endif

typedef int32_t jint;
typedef int64_t jlong;
typedef double jdouble;
typedef void *jobject;
typedef void *jclass;
typedef void *const *JNIEnv;

#define JNI_FN(name) Java_net_celestiald_cavesnotcliffs_worldgen_v118_NativeNoise_##name

namespace {

// Bumped whenever the blob layout or the native method signatures change.
constexpr jint ABI_VERSION = 1;
constexpr int LANES = 4;
// Index of GetDirectBufferAddress in the JNI function table (fixed since JNI 1.4).
constexpr int JNI_GET_DIRECT_BUFFER_ADDRESS = 230;

// Four octaves side by side; lane l reads its permutation table at perm + l * 256.
struct Group {
    double xo[LANES], yo[LANES], zo[LANES];
    double coordScale[LANES];   // NormalNoise: 1.0 (first Perlin) or INPUT_FACTOR (second)
    double inputFactor[LANES];  // NormalNoise: octave input factor; BlendedNoise: octaveScale
    double valueFactor[LANES];  // NormalNoise: octave value factor; BlendedNoise: 1/octaveScale
    double amplitude[LANES];
    double reserved[LANES];
    uint8_t perm[LANES * 256];
};
static_assert(sizeof(Group) == 1280, "Group layout is shared with NativeNoise.java");

struct NormalHeader {
    double valueFactor;
    jint firstCount;   // lanes [0, firstCount) belong to the first Perlin noise
    jint secondCount;  // the next secondCount lanes to the second
    jint groups;
    jint reserved0;
    jlong reserved1;
};
static_assert(sizeof(NormalHeader) == 32, "NormalHeader layout is shared with NativeNoise.java");

// Followed by 2 main-noise groups, then 4 min-limit and 4 max-limit groups, each in
// getOctaveNoise(octave) order.
struct BlendedHeader {
    double xzScale, yScale, xzMainScale, yMainScale;
    jint cellWidth, cellHeight;
    jlong reserved[3];
};
static_assert(sizeof(BlendedHeader) == 64, "BlendedHeader layout is shared with NativeNoise.java");

constexpr int MAX_NORMAL_LANES = 64;

const int8_t GRADIENT[3][16] = {
    {1, -1, 1, -1, 1, -1, 1, -1, 0, 0, 0, 0, 1, 0, -1, 0},
    {1, 1, -1, -1, 0, 0, 0, 0, 1, -1, 1, -1, 1, -1, 1, -1},
    {0, 0, 0, 0, 1, 1, -1, -1, 1, 1, -1, -1, 0, 1, 0, -1}
};

#define AVX2 __attribute__((target("avx2")))

struct GradientTables {
    __m128i x, y, z;
};

AVX2 inline GradientTables gradientTables() {
    GradientTables tables;
    tables.x = _mm_loadu_si128((const __m128i *) GRADIENT[0]);
    tables.y = _mm_loadu_si128((const __m128i *) GRADIENT[1]);
    tables.z = _mm_loadu_si128((const __m128i *) GRADIENT[2]);
    return tables;
}

// Mth.lerp: start + delta * (end - start).
AVX2 inline __m256d lerp(__m256d delta, __m256d start, __m256d end) {
    return _mm256_add_pd(start, _mm256_mul_pd(delta, _mm256_sub_pd(end, start)));
}

// Mth.smoothstep: v * v * v * (v * (v * 6 - 15) + 10), left to right.
AVX2 inline __m256d smoothstep(__m256d v) {
    __m256d cube = _mm256_mul_pd(_mm256_mul_pd(v, v), v);
    __m256d inner = _mm256_add_pd(_mm256_mul_pd(v,
        _mm256_sub_pd(_mm256_mul_pd(v, _mm256_set1_pd(6.0)), _mm256_set1_pd(15.0))),
        _mm256_set1_pd(10.0));
    return _mm256_mul_pd(cube, inner);
}

// PerlinNoise.wrap: v - lfloor(v / 2^25 + 0.5) * 2^25. Scaling by a power of two rounds the
// same exact product whether written as a division or as a multiplication by the reciprocal,
// and floor() agrees with Java's lfloor for every finite input.
AVX2 inline __m256d wrap(__m256d v) {
    __m256d k = _mm256_floor_pd(_mm256_add_pd(
        _mm256_mul_pd(v, _mm256_set1_pd(1.0 / 33554432.0)), _mm256_set1_pd(0.5)));
    return _mm256_sub_pd(v, _mm256_mul_pd(k, _mm256_set1_pd(33554432.0)));
}

// Four signed gradient components (one per lane) widened to doubles.
AVX2 inline __m256d gradient(const int8_t *components) {
    int32_t packed;
    __builtin_memcpy(&packed, components, 4);
    return _mm256_cvtepi32_pd(_mm_cvtepi8_epi32(_mm_cvtsi32_si128(packed)));
}

// Four ImprovedNoise.noise samples, one per lane. With smeared set, lanes whose yScale is
// non-zero apply the legacy y-smearing offset; zero lanes take the plain path, as in Java.
// Permutation lookups stay scalar: AVX2 gathers are microcoded slowly on CPUs with the
// Gather Data Sampling mitigation, which made a gather kernel slower than the Java loop.
AVX2 inline __m256d improvedNoise(const Group &group, const GradientTables &tables,
        __m256d x, __m256d y, __m256d z, __m256d yScale, __m256d yMax, bool smeared) {
    const __m256d one = _mm256_set1_pd(1.0);
    __m256d shiftedX = _mm256_add_pd(x, _mm256_loadu_pd(group.xo));
    __m256d shiftedY = _mm256_add_pd(y, _mm256_loadu_pd(group.yo));
    __m256d shiftedZ = _mm256_add_pd(z, _mm256_loadu_pd(group.zo));
    __m256d floorX = _mm256_floor_pd(shiftedX);
    __m256d floorY = _mm256_floor_pd(shiftedY);
    __m256d floorZ = _mm256_floor_pd(shiftedZ);
    alignas(16) int32_t cellX[LANES], cellY[LANES], cellZ[LANES];
    _mm_store_si128((__m128i *) cellX, _mm256_cvttpd_epi32(floorX));
    _mm_store_si128((__m128i *) cellY, _mm256_cvttpd_epi32(floorY));
    _mm_store_si128((__m128i *) cellZ, _mm256_cvttpd_epi32(floorZ));
    __m256d localX = _mm256_sub_pd(shiftedX, floorX);
    __m256d localY = _mm256_sub_pd(shiftedY, floorY);
    __m256d localZ = _mm256_sub_pd(shiftedZ, floorZ);

    __m256d sampleY = localY;
    if (smeared) {
        const __m256d zero = _mm256_setzero_pd();
        __m256d useMax = _mm256_and_pd(_mm256_cmp_pd(yMax, zero, _CMP_GE_OQ),
            _mm256_cmp_pd(yMax, localY, _CMP_LT_OQ));
        __m256d limitedY = _mm256_blendv_pd(localY, yMax, useMax);
        __m256d yOffset = _mm256_mul_pd(_mm256_floor_pd(_mm256_add_pd(
            _mm256_div_pd(limitedY, yScale), _mm256_set1_pd((double) 1.0E-7F))), yScale);
        __m256d active = _mm256_cmp_pd(yScale, zero, _CMP_NEQ_UQ);
        sampleY = _mm256_sub_pd(localY, _mm256_and_pd(yOffset, active));
    }

    // hashes[corner * LANES + lane], corners in lerp3 argument order.
    alignas(16) uint8_t hashes[8 * LANES];
    for (int lane = 0; lane < LANES; ++lane) {
        const uint8_t *p = group.perm + lane * 256;
        int fx = cellX[lane], fy = cellY[lane], fz = cellZ[lane];
        int px0 = p[fx & 255];
        int px1 = p[(fx + 1) & 255];
        int pxy00 = p[(px0 + fy) & 255];
        int pxy01 = p[(px0 + fy + 1) & 255];
        int pxy10 = p[(px1 + fy) & 255];
        int pxy11 = p[(px1 + fy + 1) & 255];
        hashes[0 * LANES + lane] = p[(pxy00 + fz) & 255];
        hashes[1 * LANES + lane] = p[(pxy10 + fz) & 255];
        hashes[2 * LANES + lane] = p[(pxy01 + fz) & 255];
        hashes[3 * LANES + lane] = p[(pxy11 + fz) & 255];
        hashes[4 * LANES + lane] = p[(pxy00 + fz + 1) & 255];
        hashes[5 * LANES + lane] = p[(pxy10 + fz + 1) & 255];
        hashes[6 * LANES + lane] = p[(pxy01 + fz + 1) & 255];
        hashes[7 * LANES + lane] = p[(pxy11 + fz + 1) & 255];
    }
    const __m128i low = _mm_set1_epi8(15);
    __m128i hashes0 = _mm_and_si128(_mm_load_si128((const __m128i *) hashes), low);
    __m128i hashes1 = _mm_and_si128(_mm_load_si128((const __m128i *) (hashes + 16)), low);
    alignas(16) int8_t gx[8 * LANES], gy[8 * LANES], gz[8 * LANES];
    _mm_store_si128((__m128i *) gx, _mm_shuffle_epi8(tables.x, hashes0));
    _mm_store_si128((__m128i *) (gx + 16), _mm_shuffle_epi8(tables.x, hashes1));
    _mm_store_si128((__m128i *) gy, _mm_shuffle_epi8(tables.y, hashes0));
    _mm_store_si128((__m128i *) (gy + 16), _mm_shuffle_epi8(tables.y, hashes1));
    _mm_store_si128((__m128i *) gz, _mm_shuffle_epi8(tables.z, hashes0));
    _mm_store_si128((__m128i *) (gz + 16), _mm_shuffle_epi8(tables.z, hashes1));

    __m256d localX1 = _mm256_sub_pd(localX, one);
    __m256d sampleY1 = _mm256_sub_pd(sampleY, one);
    __m256d localZ1 = _mm256_sub_pd(localZ, one);
    // gradient[0] * x + gradient[1] * y + gradient[2] * z, with integer gradients widened
    // to double before the products, exactly as Java's int * double promotion.
#define GRAD_DOT(corner, X, Y, Z) _mm256_add_pd(_mm256_add_pd( \
        _mm256_mul_pd(gradient(gx + (corner) * LANES), X), \
        _mm256_mul_pd(gradient(gy + (corner) * LANES), Y)), \
        _mm256_mul_pd(gradient(gz + (corner) * LANES), Z))
    __m256d x0y0z0 = GRAD_DOT(0, localX, sampleY, localZ);
    __m256d x1y0z0 = GRAD_DOT(1, localX1, sampleY, localZ);
    __m256d x0y1z0 = GRAD_DOT(2, localX, sampleY1, localZ);
    __m256d x1y1z0 = GRAD_DOT(3, localX1, sampleY1, localZ);
    __m256d x0y0z1 = GRAD_DOT(4, localX, sampleY, localZ1);
    __m256d x1y0z1 = GRAD_DOT(5, localX1, sampleY, localZ1);
    __m256d x0y1z1 = GRAD_DOT(6, localX, sampleY1, localZ1);
    __m256d x1y1z1 = GRAD_DOT(7, localX1, sampleY1, localZ1);
#undef GRAD_DOT
    // The y fade uses the unsmeared local coordinate, like sampleAndLerp's fadeYInput.
    __m256d fadeX = smoothstep(localX);
    __m256d fadeY = smoothstep(localY);
    __m256d fadeZ = smoothstep(localZ);
    __m256d z0 = lerp(fadeY, lerp(fadeX, x0y0z0, x1y0z0), lerp(fadeX, x0y1z0, x1y1z0));
    __m256d z1 = lerp(fadeY, lerp(fadeX, x0y0z1, x1y0z1), lerp(fadeX, x0y1z1, x1y1z1));
    return lerp(fadeZ, z0, z1);
}

// NormalNoise.getValue over both Perlin stacks; each lane is one present octave.
AVX2 double normalNoise(const uint8_t *blob, double x, double y, double z) {
    const NormalHeader *header = (const NormalHeader *) blob;
    const Group *groups = (const Group *) (blob + sizeof(NormalHeader));
    const GradientTables tables = gradientTables();
    const __m256d vx = _mm256_set1_pd(x), vy = _mm256_set1_pd(y), vz = _mm256_set1_pd(z);
    const __m256d zero = _mm256_setzero_pd();
    alignas(32) double terms[MAX_NORMAL_LANES];
    for (int index = 0; index < header->groups; ++index) {
        const Group &group = groups[index];
        // (x * INPUT_FACTOR) * inputFactor for the second stack, x * 1.0 * inputFactor (exactly
        // x * inputFactor) for the first.
        __m256d coordScale = _mm256_loadu_pd(group.coordScale);
        __m256d inputFactor = _mm256_loadu_pd(group.inputFactor);
        __m256d noise = improvedNoise(group, tables,
            wrap(_mm256_mul_pd(_mm256_mul_pd(vx, coordScale), inputFactor)),
            wrap(_mm256_mul_pd(_mm256_mul_pd(vy, coordScale), inputFactor)),
            wrap(_mm256_mul_pd(_mm256_mul_pd(vz, coordScale), inputFactor)),
            zero, zero, false);
        // amplitudes[index] * octaveValue * valueFactor
        _mm256_store_pd(terms + index * LANES, _mm256_mul_pd(
            _mm256_mul_pd(_mm256_loadu_pd(group.amplitude), noise),
            _mm256_loadu_pd(group.valueFactor)));
    }
    double first = 0.0;
    double second = 0.0;
    int lane = 0;
    for (; lane < header->firstCount; ++lane) {
        first += terms[lane];
    }
    for (int end = header->firstCount + header->secondCount; lane < end; ++lane) {
        second += terms[lane];
    }
    return (first + second) * header->valueFactor;
}

inline int floorDiv(int value, int divisor) {
    int quotient = value / divisor;
    if (value % divisor != 0 && (value < 0) != (divisor < 0)) {
        --quotient;
    }
    return quotient;
}

// BlendedNoise.compute. Each "/ octaveScale" multiplies by the exact power of two
// 1/octaveScale instead, which yields the same double.
AVX2 double blendedNoise(const uint8_t *blob, int blockX, int blockY, int blockZ) {
    const BlendedHeader *header = (const BlendedHeader *) blob;
    const Group *mainNoise = (const Group *) (blob + sizeof(BlendedHeader));
    const Group *minLimitNoise = mainNoise + 2;
    const Group *maxLimitNoise = minLimitNoise + 4;
    const GradientTables tables = gradientTables();
    int cellX = floorDiv(blockX, header->cellWidth);
    int cellY = floorDiv(blockY, header->cellHeight);
    int cellZ = floorDiv(blockZ, header->cellWidth);
    alignas(32) double mainTerms[8], minTerms[16], maxTerms[16];

    // Java: wrap(cellX * xzMainScale * octaveScale), i.e. (cellX * xzMainScale) * octaveScale.
    const __m256d mainX = _mm256_set1_pd(cellX * header->xzMainScale);
    const __m256d mainY = _mm256_set1_pd(cellY * header->yMainScale);
    const __m256d mainZ = _mm256_set1_pd(cellZ * header->xzMainScale);
    const __m256d yMainScale = _mm256_set1_pd(header->yMainScale);
    for (int index = 0; index < 2; ++index) {
        const Group &group = mainNoise[index];
        __m256d octaveScale = _mm256_loadu_pd(group.inputFactor);
        __m256d noise = improvedNoise(group, tables,
            wrap(_mm256_mul_pd(mainX, octaveScale)),
            wrap(_mm256_mul_pd(mainY, octaveScale)),
            wrap(_mm256_mul_pd(mainZ, octaveScale)),
            _mm256_mul_pd(yMainScale, octaveScale),
            _mm256_mul_pd(mainY, octaveScale), true);
        _mm256_store_pd(mainTerms + index * LANES,
            _mm256_mul_pd(noise, _mm256_loadu_pd(group.valueFactor)));
    }
    double mainValue = 0.0;
    for (int octave = 0; octave < 8; ++octave) {
        mainValue += mainTerms[octave];
    }

    double blend = (mainValue / 10.0 + 1.0) / 2.0;
    bool onlyMax = blend >= 1.0;
    bool onlyMin = blend <= 0.0;
    const __m256d limitX = _mm256_set1_pd(cellX * header->xzScale);
    const __m256d limitY = _mm256_set1_pd(cellY * header->yScale);
    const __m256d limitZ = _mm256_set1_pd(cellZ * header->xzScale);
    const __m256d yScale = _mm256_set1_pd(header->yScale);
    const __m256d cellYValue = _mm256_set1_pd((double) cellY);
    for (int index = 0; index < 4; ++index) {
        __m256d octaveScale = _mm256_loadu_pd(minLimitNoise[index].inputFactor);
        __m256d x = wrap(_mm256_mul_pd(limitX, octaveScale));
        __m256d y = wrap(_mm256_mul_pd(limitY, octaveScale));
        __m256d z = wrap(_mm256_mul_pd(limitZ, octaveScale));
        __m256d verticalScale = _mm256_mul_pd(yScale, octaveScale);
        __m256d yMax = _mm256_mul_pd(cellYValue, verticalScale);
        __m256d inverseScale = _mm256_loadu_pd(minLimitNoise[index].valueFactor);
        if (!onlyMax) {
            _mm256_store_pd(minTerms + index * LANES, _mm256_mul_pd(improvedNoise(
                minLimitNoise[index], tables, x, y, z, verticalScale, yMax, true),
                inverseScale));
        }
        if (!onlyMin) {
            _mm256_store_pd(maxTerms + index * LANES, _mm256_mul_pd(improvedNoise(
                maxLimitNoise[index], tables, x, y, z, verticalScale, yMax, true),
                inverseScale));
        }
    }
    double minValue = 0.0;
    double maxValue = 0.0;
    if (!onlyMax) {
        for (int octave = 0; octave < 16; ++octave) {
            minValue += minTerms[octave];
        }
    }
    if (!onlyMin) {
        for (int octave = 0; octave < 16; ++octave) {
            maxValue += maxTerms[octave];
        }
    }

    double start = minValue / 512.0;
    double end = maxValue / 512.0;
    double value;
    if (blend < 0.0) {
        value = start;
    } else if (blend > 1.0) {
        value = end;
    } else {
        value = start + blend * (end - start);
    }
    return value / 128.0;
}

// AVX2 needs the CPU feature and the OS saving the YMM state (OSXSAVE + XCR0 bits 1-2).
bool supportsAvx2() {
    unsigned eax, ebx, ecx, edx;
    if (!__get_cpuid(1, &eax, &ebx, &ecx, &edx)) {
        return false;
    }
    if (!(ecx & (1u << 27)) || !(ecx & (1u << 28))) {
        return false;
    }
    unsigned xcr0Low, xcr0High;
    __asm__ volatile("xgetbv" : "=a"(xcr0Low), "=d"(xcr0High) : "c"(0));
    if ((xcr0Low & 6u) != 6u) {
        return false;
    }
    if (!__get_cpuid_count(7, 0, &eax, &ebx, &ecx, &edx)) {
        return false;
    }
    return (ebx & (1u << 5)) != 0;
}

} // namespace

CNC_EXPORT jint JNI_FN(abiVersion)(JNIEnv *, jclass) {
    return ABI_VERSION;
}

CNC_EXPORT jint JNI_FN(supportsAvx2)(JNIEnv *, jclass) {
    return supportsAvx2() ? 1 : 0;
}

CNC_EXPORT jlong JNI_FN(address)(JNIEnv *env, jclass, jobject buffer) {
    typedef void *(*GetDirectBufferAddress)(JNIEnv *, jobject);
    GetDirectBufferAddress function =
        (GetDirectBufferAddress) (*env)[JNI_GET_DIRECT_BUFFER_ADDRESS];
    return (jlong) (intptr_t) function(env, buffer);
}

CNC_EXPORT jdouble JNI_FN(normal)(JNIEnv *, jclass, jlong handle, jdouble x, jdouble y,
        jdouble z) {
    return normalNoise((const uint8_t *) (intptr_t) handle, x, y, z);
}

CNC_EXPORT jdouble JNI_FN(blended)(JNIEnv *, jclass, jlong handle, jint x, jint y, jint z) {
    return blendedNoise((const uint8_t *) (intptr_t) handle, x, y, z);
}
