package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Dependency-free Java 8 representation of Java 1.18.2's {@code SurfaceRules} DSL.
 *
 * <p>The rule tree deliberately remains generation-local: it returns {@link V118Material}
 * values and knows nothing about 1.12 block states.  Conditions are evaluated with the same
 * inclusive bounds, integer truncation, positional randomness, and height-map conventions as
 * the vanilla implementation.</p>
 */
public final class V118SurfaceRules {
    private static final int NOISE_CACHE_SIZE = 8;
    public static final ConditionSource ON_FLOOR = stoneDepthCheck(0, false, Surface.FLOOR);
    public static final ConditionSource UNDER_FLOOR = stoneDepthCheck(0, true, Surface.FLOOR);
    public static final ConditionSource DEEP_UNDER_FLOOR = stoneDepthCheck(0, true, 6,
        Surface.FLOOR);
    public static final ConditionSource VERY_DEEP_UNDER_FLOOR = stoneDepthCheck(0, true, 30,
        Surface.FLOOR);
    public static final ConditionSource ON_CEILING = stoneDepthCheck(0, false, Surface.CEILING);
    public static final ConditionSource UNDER_CEILING = stoneDepthCheck(0, true, Surface.CEILING);

    private V118SurfaceRules() {
    }

    public interface ConditionSource {
        boolean test(Context context);
    }

    public interface RuleSource {
        /** Returns {@code null} when this rule does not select a material. */
        V118Material tryApply(Context context);
    }

    public enum Surface {
        FLOOR,
        CEILING
    }

    /** Vanilla vertical-anchor semantics for a fixed world-generation height context. */
    public abstract static class VerticalAnchor {
        public abstract int resolveY(int minBuildHeight, int maxBuildHeight);

        public static VerticalAnchor absolute(final int y) {
            return new VerticalAnchor() {
                @Override
                public int resolveY(int minBuildHeight, int maxBuildHeight) {
                    return y;
                }
            };
        }

        public static VerticalAnchor bottom() {
            return aboveBottom(0);
        }

        public static VerticalAnchor aboveBottom(final int offset) {
            return new VerticalAnchor() {
                @Override
                public int resolveY(int minBuildHeight, int maxBuildHeight) {
                    return minBuildHeight + offset;
                }
            };
        }

        public static VerticalAnchor top() {
            return belowTop(0);
        }

        public static VerticalAnchor belowTop(final int offset) {
            return new VerticalAnchor() {
                @Override
                public int resolveY(int minBuildHeight, int maxBuildHeight) {
                    return maxBuildHeight - 1 - offset;
                }
            };
        }
    }

    public static ConditionSource stoneDepthCheck(final int offset,
            final boolean addSurfaceDepth, final Surface surface) {
        return stoneDepthCheck(offset, addSurfaceDepth, 0, surface);
    }

    public static ConditionSource stoneDepthCheck(final int offset,
            final boolean addSurfaceDepth, final int secondaryDepthRange,
            final Surface surface) {
        return new ConditionSource() {
            @Override
            public boolean test(Context context) {
                int stoneDepth = surface == Surface.CEILING
                    ? context.stoneDepthBelow : context.stoneDepthAbove;
                int primaryDepth = addSurfaceDepth ? context.surfaceDepth : 0;
                int secondaryDepth = secondaryDepthRange == 0 ? 0
                    : (int) map(context.surfaceSecondary(), -1.0D, 1.0D,
                        0.0D, secondaryDepthRange);
                return stoneDepth <= 1 + offset + primaryDepth + secondaryDepth;
            }
        };
    }

    public static ConditionSource not(final ConditionSource target) {
        return new ConditionSource() {
            @Override
            public boolean test(Context context) {
                return !target.test(context);
            }
        };
    }

    public static ConditionSource yBlockCheck(final VerticalAnchor anchor,
            final int surfaceDepthMultiplier) {
        return yCheck(anchor, surfaceDepthMultiplier, false);
    }

    public static ConditionSource yStartCheck(final VerticalAnchor anchor,
            final int surfaceDepthMultiplier) {
        return yCheck(anchor, surfaceDepthMultiplier, true);
    }

    private static ConditionSource yCheck(final VerticalAnchor anchor,
            final int surfaceDepthMultiplier, final boolean addStoneDepth) {
        return new ConditionSource() {
            @Override
            public boolean test(Context context) {
                int y = context.blockY + (addStoneDepth ? context.stoneDepthAbove : 0);
                return y >= anchor.resolveY(context.minBuildHeight(), context.maxBuildHeight())
                    + context.surfaceDepth * surfaceDepthMultiplier;
            }
        };
    }

    public static ConditionSource waterBlockCheck(final int offset,
            final int surfaceDepthMultiplier) {
        return waterCheck(offset, surfaceDepthMultiplier, false);
    }

    public static ConditionSource waterStartCheck(final int offset,
            final int surfaceDepthMultiplier) {
        return waterCheck(offset, surfaceDepthMultiplier, true);
    }

    private static ConditionSource waterCheck(final int offset,
            final int surfaceDepthMultiplier, final boolean addStoneDepth) {
        return new ConditionSource() {
            @Override
            public boolean test(Context context) {
                if (context.waterHeight == Integer.MIN_VALUE) {
                    return true;
                }
                int y = context.blockY + (addStoneDepth ? context.stoneDepthAbove : 0);
                return y >= context.waterHeight + offset
                    + context.surfaceDepth * surfaceDepthMultiplier;
            }
        };
    }

    public static ConditionSource isBiome(V118Biome... biomes) {
        final Set<V118Biome> accepted = biomes.length == 0
            ? Collections.<V118Biome>emptySet()
            : EnumSet.copyOf(Arrays.asList(biomes));
        return new ConditionSource() {
            @Override
            public boolean test(Context context) {
                return accepted.contains(context.biome());
            }
        };
    }

    public static ConditionSource noiseCondition(final String noiseName,
            final double minimum) {
        return noiseCondition(noiseName, minimum, Double.MAX_VALUE);
    }

    public static ConditionSource noiseCondition(final String noiseName,
            final double minimum, final double maximum) {
        return new ConditionSource() {
            @Override
            public boolean test(Context context) {
                double value = context.noise(noiseName);
                return value >= minimum && value <= maximum;
            }
        };
    }

    public static ConditionSource verticalGradient(final String randomName,
            final VerticalAnchor trueAtAndBelow, final VerticalAnchor falseAtAndAbove) {
        return new ConditionSource() {
            @Override
            public boolean test(Context context) {
                int lower = trueAtAndBelow.resolveY(context.minBuildHeight(),
                    context.maxBuildHeight());
                int upper = falseAtAndAbove.resolveY(context.minBuildHeight(),
                    context.maxBuildHeight());
                if (context.blockY <= lower) {
                    return true;
                }
                if (context.blockY >= upper) {
                    return false;
                }
                double threshold = map(context.blockY, lower, upper, 1.0D, 0.0D);
                RandomSource random = context.system.getOrCreateRandomFactory(randomName)
                    .at(context.blockX, context.blockY, context.blockZ);
                return random.nextFloat() < threshold;
            }
        };
    }

    public static ConditionSource steep() {
        return new ConditionSource() {
            @Override
            public boolean test(Context context) {
                int localX = context.blockX - context.chunkMinX;
                int localZ = context.blockZ - context.chunkMinZ;
                int northZ = Math.max(localZ - 1, 0);
                int southZ = Math.min(localZ + 1, 15);
                int north = context.access.worldSurfaceHeight(context.chunkMinX + localX,
                    context.chunkMinZ + northZ);
                int south = context.access.worldSurfaceHeight(context.chunkMinX + localX,
                    context.chunkMinZ + southZ);
                if (south >= north + 4) {
                    return true;
                }
                int westX = Math.max(localX - 1, 0);
                int eastX = Math.min(localX + 1, 15);
                int west = context.access.worldSurfaceHeight(context.chunkMinX + westX,
                    context.chunkMinZ + localZ);
                int east = context.access.worldSurfaceHeight(context.chunkMinX + eastX,
                    context.chunkMinZ + localZ);
                return west >= east + 4;
            }
        };
    }

    public static ConditionSource hole() {
        return new ConditionSource() {
            @Override
            public boolean test(Context context) {
                return context.surfaceDepth <= 0;
            }
        };
    }

    public static ConditionSource abovePreliminarySurface() {
        return new ConditionSource() {
            @Override
            public boolean test(Context context) {
                return context.blockY >= context.minimumSurfaceLevel();
            }
        };
    }

    public static ConditionSource temperature() {
        return new ConditionSource() {
            @Override
            public boolean test(Context context) {
                return context.access.coldEnoughToSnow(context.biome(), context.blockX,
                    context.blockY, context.blockZ);
            }
        };
    }

    public static RuleSource ifTrue(final ConditionSource condition,
            final RuleSource followup) {
        return new RuleSource() {
            @Override
            public V118Material tryApply(Context context) {
                return condition.test(context) ? followup.tryApply(context) : null;
            }
        };
    }

    public static RuleSource sequence(final RuleSource... rules) {
        if (rules.length == 0) {
            throw new IllegalArgumentException("Need at least 1 rule for a sequence");
        }
        return new RuleSource() {
            @Override
            public V118Material tryApply(Context context) {
                for (RuleSource rule : rules) {
                    V118Material result = rule.tryApply(context);
                    if (result != null) {
                        return result;
                    }
                }
                return null;
            }
        };
    }

    public static RuleSource state(final V118Material material) {
        return new RuleSource() {
            @Override
            public V118Material tryApply(Context context) {
                return material;
            }
        };
    }

    /** Vanilla's misspelled public surface-rule name is retained in its source; this API is not. */
    public static RuleSource badlandsBands() {
        return new RuleSource() {
            @Override
            public V118Material tryApply(Context context) {
                return context.system.getBand(context.blockX, context.blockY, context.blockZ);
            }
        };
    }

    /** Mutable evaluation state, matching the fields populated by vanilla's surface scan. */
    public static final class Context {
        private final V118SurfaceSystem system;
        private final V118SurfaceSystem.SurfaceAccess access;
        private final int chunkMinX;
        private final int chunkMinZ;
        private int blockX;
        private int blockZ;
        private int blockY;
        private int surfaceDepth;
        private int waterHeight;
        private int stoneDepthBelow;
        private int stoneDepthAbove;
        private boolean secondaryValid;
        private double secondary;
        private boolean minimumSurfaceValid;
        private int minimumSurface;
        private boolean biomeValid;
        private V118Biome biome;
        private final String[] noiseNames = new String[NOISE_CACHE_SIZE];
        private final double[] noiseValues = new double[NOISE_CACHE_SIZE];
        private int noiseCount;

        Context(V118SurfaceSystem system, V118SurfaceSystem.SurfaceAccess access,
                int chunkMinX, int chunkMinZ) {
            this.system = system;
            this.access = access;
            this.chunkMinX = chunkMinX;
            this.chunkMinZ = chunkMinZ;
        }

        void updateXZ(int x, int z) {
            blockX = x;
            blockZ = z;
            surfaceDepth = system.getSurfaceDepth(x, z);
            secondaryValid = false;
            minimumSurfaceValid = false;
            biomeValid = false;
            noiseCount = 0;
        }

        void updateY(int depthAbove, int depthBelow, int fluidHeight,
                int x, int y, int z) {
            blockX = x;
            blockY = y;
            blockZ = z;
            waterHeight = fluidHeight;
            stoneDepthBelow = depthBelow;
            stoneDepthAbove = depthAbove;
            biomeValid = false;
        }

        public int blockX() {
            return blockX;
        }

        public int blockY() {
            return blockY;
        }

        public int blockZ() {
            return blockZ;
        }

        public int surfaceDepth() {
            return surfaceDepth;
        }

        public int waterHeight() {
            return waterHeight;
        }

        public int stoneDepthBelow() {
            return stoneDepthBelow;
        }

        public int stoneDepthAbove() {
            return stoneDepthAbove;
        }

        public V118Biome biome() {
            if (!biomeValid) {
                biome = access.biomeAt(blockX, blockY, blockZ);
                biomeValid = true;
            }
            return biome;
        }

        public int minimumSurfaceLevel() {
            if (!minimumSurfaceValid) {
                int cellX = blockX >> 4;
                int cellZ = blockZ >> 4;
                int x0z0 = access.preliminarySurfaceLevel(cellX << 4, cellZ << 4);
                int x1z0 = access.preliminarySurfaceLevel((cellX + 1) << 4, cellZ << 4);
                int x0z1 = access.preliminarySurfaceLevel(cellX << 4, (cellZ + 1) << 4);
                int x1z1 = access.preliminarySurfaceLevel((cellX + 1) << 4,
                    (cellZ + 1) << 4);
                double xFraction = (float) (blockX & 15) / 16.0F;
                double zFraction = (float) (blockZ & 15) / 16.0F;
                double north = lerp(xFraction, x0z0, x1z0);
                double south = lerp(xFraction, x0z1, x1z1);
                minimumSurface = floor(lerp(zFraction, north, south)) + surfaceDepth - 8;
                minimumSurfaceValid = true;
            }
            return minimumSurface;
        }

        private double surfaceSecondary() {
            if (!secondaryValid) {
                secondary = system.getSurfaceSecondary(blockX, blockZ);
                secondaryValid = true;
            }
            return secondary;
        }

        private double noise(String name) {
            for (int index = 0; index < noiseCount; ++index) {
                if (name.equals(noiseNames[index])) {
                    return noiseValues[index];
                }
            }
            double value = system.getOrCreateNoise(name).getValue(blockX, 0.0D, blockZ);
            if (noiseCount < noiseNames.length) {
                noiseNames[noiseCount] = name;
                noiseValues[noiseCount++] = value;
            }
            return value;
        }

        private int minBuildHeight() {
            return access.minBuildHeight();
        }

        private int maxBuildHeight() {
            return access.maxBuildHeight();
        }
    }

    private static double map(double value, double fromLow, double fromHigh,
            double toLow, double toHigh) {
        return lerp((value - fromLow) / (fromHigh - fromLow), toLow, toHigh);
    }

    private static double lerp(double delta, double start, double end) {
        return start + delta * (end - start);
    }

    private static int floor(double value) {
        int truncated = (int) value;
        return value < truncated ? truncated - 1 : truncated;
    }
}
