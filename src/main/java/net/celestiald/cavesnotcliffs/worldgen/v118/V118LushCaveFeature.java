package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/** Dependency-free Java 8 port of Java 1.18.2's configured lush-cave features. */
public final class V118LushCaveFeature {
    private static final Direction[] VINE_DIRECTIONS = {
        Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH,
        Direction.WEST, Direction.EAST
    };
    private static final Direction[] SMALL_DRIPLEAF_DIRECTIONS = {
        Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH
    };
    private static final Direction[] BIG_DRIPLEAF_DIRECTIONS = {
        Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH
    };
    private static final Direction[] POOL_EXPOSURE_DIRECTIONS = {
        Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.DOWN
    };

    private V118LushCaveFeature() {
    }

    public static boolean place(V118LushCavePlacements.PlacedFeature feature,
            WorldAccess world, Random random, int originX, int originY, int originZ) {
        if (feature == null || world == null || random == null) {
            throw new NullPointerException("feature, world, and random are required");
        }
        if (!world.ensureCanWrite(originX, originY, originZ)) {
            return false;
        }
        switch (feature) {
            case CEILING_VEGETATION:
                return placeVegetationPatch(world, random, originX, originY, originZ,
                    PatchConfiguration.MOSS_CEILING);
            case CAVE_VINES:
                return placeCaveVineColumn(world, random, originX, originY, originZ, false);
            case LUSH_CAVES_CLAY:
                return placeClaySelector(world, random, originX, originY, originZ);
            case FLOOR_VEGETATION:
                return placeVegetationPatch(world, random, originX, originY, originZ,
                    PatchConfiguration.MOSS_FLOOR);
            case ROOTED_AZALEA_TREE:
                return placeRootSystem(world, random, originX, originY, originZ);
            case SPORE_BLOSSOM:
                return placeSimpleBlock(world, State.SPORE_BLOSSOM,
                    originX, originY, originZ);
            case CLASSIC_VINES:
                return placeClassicVine(world, originX, originY, originZ);
            default:
                throw new AssertionError(feature);
        }
    }

    private static boolean placeClaySelector(WorldAccess world, Random random,
            int originX, int originY, int originZ) {
        PatchConfiguration configuration = random.nextBoolean()
            ? PatchConfiguration.CLAY_DRY : PatchConfiguration.CLAY_POOL;
        return placeVegetationPatch(world, random, originX, originY, originZ,
            configuration);
    }

    private static boolean placeVegetationPatch(WorldAccess world, Random random,
            int originX, int originY, int originZ, PatchConfiguration configuration) {
        if (!world.ensureCanWrite(originX, originY, originZ)) {
            return false;
        }
        int radiusX = randomBetweenInclusive(random, 4, 7) + 1;
        int radiusZ = randomBetweenInclusive(random, 4, 7) + 1;
        Set<Position> ground = placeGroundPatch(world, random, originX, originY, originZ,
            configuration, radiusX, radiusZ);
        if (configuration.waterlogged) {
            Set<Position> enclosed = new HashSet<Position>();
            for (Position position : ground) {
                if (!isExposed(world, position)) {
                    enclosed.add(position);
                }
            }
            for (Position position : enclosed) {
                world.setState(position.x, position.y, position.z, State.WATER);
            }
            ground = enclosed;
        }
        distributeVegetation(world, random, configuration, ground);
        return !ground.isEmpty();
    }

    private static Set<Position> placeGroundPatch(WorldAccess world, Random random,
            int originX, int originY, int originZ, PatchConfiguration configuration,
            int radiusX, int radiusZ) {
        Set<Position> result = new HashSet<Position>();
        int surfaceDirectionY = configuration.ceiling ? 1 : -1;
        int oppositeDirectionY = -surfaceDirectionY;
        for (int offsetX = -radiusX; offsetX <= radiusX; ++offsetX) {
            boolean xEdge = offsetX == -radiusX || offsetX == radiusX;
            for (int offsetZ = -radiusZ; offsetZ <= radiusZ; ++offsetZ) {
                boolean zEdge = offsetZ == -radiusZ || offsetZ == radiusZ;
                boolean edge = xEdge || zEdge;
                boolean corner = xEdge && zEdge;
                if (corner || edge && (configuration.extraEdgeColumnChance == 0.0F
                        || random.nextFloat() > configuration.extraEdgeColumnChance)) {
                    continue;
                }
                int blockX = originX + offsetX;
                int blockZ = originZ + offsetZ;
                int cursorY = originY;
                int moved = 0;
                while (world.isAir(blockX, cursorY, blockZ)
                        && moved < configuration.verticalRange) {
                    cursorY += surfaceDirectionY;
                    ++moved;
                }
                moved = 0;
                while (!world.isAir(blockX, cursorY, blockZ)
                        && moved < configuration.verticalRange) {
                    cursorY += oppositeDirectionY;
                    ++moved;
                }
                int groundY = cursorY + surfaceDirectionY;
                Direction sturdyFace = configuration.ceiling ? Direction.DOWN : Direction.UP;
                if (!world.isAir(blockX, cursorY, blockZ)
                        || !world.hasSturdyFace(blockX, groundY, blockZ, sturdyFace)) {
                    continue;
                }
                int depth = configuration.sampleDepth(random);
                if (configuration.extraBottomBlockChance > 0.0F
                        && random.nextFloat() < configuration.extraBottomBlockChance) {
                    ++depth;
                }
                Position groundPosition = new Position(blockX, groundY, blockZ);
                if (placeGround(world, configuration, groundPosition, depth,
                        surfaceDirectionY)) {
                    result.add(groundPosition);
                }
            }
        }
        return result;
    }

    private static boolean placeGround(WorldAccess world, PatchConfiguration configuration,
            Position start, int depth, int directionY) {
        int blockY = start.y;
        for (int layer = 0; layer < depth; ++layer) {
            State current = world.getState(start.x, blockY, start.z);
            if (current.block() == configuration.ground.block()) {
                continue;
            }
            boolean replaceable = configuration.ground.block() == Block.MOSS_BLOCK
                ? world.isMossReplaceable(start.x, blockY, start.z)
                : world.isLushGroundReplaceable(start.x, blockY, start.z);
            if (!replaceable) {
                return layer != 0;
            }
            world.setState(start.x, blockY, start.z, configuration.ground);
            blockY += directionY;
        }
        return true;
    }

    private static void distributeVegetation(WorldAccess world, Random random,
            PatchConfiguration configuration, Set<Position> ground) {
        for (Position position : ground) {
            if (!(configuration.vegetationChance > 0.0F)
                    || !(random.nextFloat() < configuration.vegetationChance)) {
                continue;
            }
            if (configuration.waterlogged) {
                boolean placed = placePatchVegetation(world, random,
                    position.x, position.y - 1, position.z, configuration);
                if (placed) {
                    State state = world.getState(position.x, position.y, position.z);
                    if (state.supportsWaterlogging() && !state.waterlogged()) {
                        world.setState(position.x, position.y, position.z,
                            state.withWaterlogged(true));
                    }
                }
            } else {
                placePatchVegetation(world, random, position.x, position.y, position.z,
                    configuration);
            }
        }
    }

    private static boolean placePatchVegetation(WorldAccess world, Random random,
            int groundX, int groundY, int groundZ, PatchConfiguration configuration) {
        int vegetationY = groundY + (configuration.ceiling ? -1 : 1);
        if (configuration == PatchConfiguration.MOSS_FLOOR) {
            return placeMossVegetation(world, random, groundX, vegetationY, groundZ);
        }
        if (configuration == PatchConfiguration.MOSS_CEILING) {
            return placeCaveVineColumn(world, random, groundX, vegetationY, groundZ, true);
        }
        return placeDripleaf(world, random, groundX, vegetationY, groundZ);
    }

    private static boolean placeMossVegetation(WorldAccess world, Random random,
            int blockX, int blockY, int blockZ) {
        if (!world.ensureCanWrite(blockX, blockY, blockZ)) {
            return false;
        }
        int selected = random.nextInt(96);
        State state;
        if (selected < 4) {
            state = State.FLOWERING_AZALEA;
        } else if (selected < 11) {
            state = State.AZALEA;
        } else if (selected < 36) {
            state = State.MOSS_CARPET;
        } else if (selected < 86) {
            state = State.GRASS;
        } else {
            state = State.tallGrass(false);
        }
        return placeSimpleBlock(world, state, blockX, blockY, blockZ);
    }

    private static boolean placeSimpleBlock(WorldAccess world, State state,
            int blockX, int blockY, int blockZ) {
        if (!world.ensureCanWrite(blockX, blockY, blockZ)
                || !world.canSurvive(state, blockX, blockY, blockZ)) {
            return false;
        }
        if (state.block() == Block.TALL_GRASS) {
            if (!world.isAir(blockX, blockY + 1, blockZ)) {
                return false;
            }
            world.setState(blockX, blockY, blockZ, State.tallGrass(false));
            world.setState(blockX, blockY + 1, blockZ, State.tallGrass(true));
            return true;
        }
        if (state.block() == Block.SMALL_DRIPLEAF) {
            if (!world.isAir(blockX, blockY + 1, blockZ)) {
                return false;
            }
            boolean lowerWater = world.isWaterAt(blockX, blockY, blockZ);
            boolean upperWater = world.isWaterAt(blockX, blockY + 1, blockZ);
            world.setState(blockX, blockY, blockZ,
                State.smallDripleaf(state.facing(), false, lowerWater));
            world.setState(blockX, blockY + 1, blockZ,
                State.smallDripleaf(state.facing(), true, upperWater));
            return true;
        }
        world.setState(blockX, blockY, blockZ, state);
        return true;
    }

    private static boolean placeCaveVineColumn(WorldAccess world, Random random,
            int originX, int originY, int originZ, boolean inMoss) {
        if (!world.ensureCanWrite(originX, originY, originZ)) {
            return false;
        }
        int bodyHeight = inMoss ? sampleMossVineBodyHeight(random)
            : sampleCaveVineBodyHeight(random);
        int[] layers = {bodyHeight, 1};
        int totalHeight = bodyHeight + 1;
        int checkY = originY - 1;
        for (int offset = 0; offset < totalHeight; ++offset) {
            if (!world.isAir(originX, checkY, originZ)) {
                truncate(layers, totalHeight, offset, true);
                break;
            }
            --checkY;
        }
        int blockY = originY;
        for (int body = 0; body < layers[0]; ++body) {
            world.setState(originX, blockY, originZ,
                State.caveVineBody(random.nextInt(5) >= 4));
            --blockY;
        }
        for (int head = 0; head < layers[1]; ++head) {
            boolean berries = random.nextInt(5) >= 4;
            int age = randomBetweenInclusive(random, 23, 25);
            world.setState(originX, blockY, originZ, State.caveVineHead(age, berries));
            --blockY;
        }
        return true;
    }

    private static int sampleCaveVineBodyHeight(Random random) {
        int selected = random.nextInt(15);
        if (selected < 2) {
            return randomBetweenInclusive(random, 0, 19);
        }
        if (selected < 5) {
            return randomBetweenInclusive(random, 0, 2);
        }
        return randomBetweenInclusive(random, 0, 6);
    }

    private static int sampleMossVineBodyHeight(Random random) {
        return random.nextInt(6) < 5
            ? randomBetweenInclusive(random, 0, 3)
            : randomBetweenInclusive(random, 1, 7);
    }

    private static boolean placeDripleaf(WorldAccess world, Random random,
            int blockX, int blockY, int blockZ) {
        if (!world.ensureCanWrite(blockX, blockY, blockZ)) {
            return false;
        }
        int selected = random.nextInt(5);
        if (selected == 0) {
            Direction direction = SMALL_DRIPLEAF_DIRECTIONS[random.nextInt(4)];
            return placeSimpleBlock(world,
                State.smallDripleaf(direction, false, false), blockX, blockY, blockZ);
        }
        return placeBigDripleafColumn(world, random, blockX, blockY, blockZ,
            BIG_DRIPLEAF_DIRECTIONS[selected - 1]);
    }

    private static boolean placeBigDripleafColumn(WorldAccess world, Random random,
            int originX, int originY, int originZ, Direction direction) {
        int bodyHeight = random.nextInt(3) < 2
            ? randomBetweenInclusive(random, 0, 4) : 0;
        int[] layers = {bodyHeight, 1};
        int totalHeight = bodyHeight + 1;
        int checkY = originY + 1;
        for (int offset = 0; offset < totalHeight; ++offset) {
            State state = world.getState(originX, checkY, originZ);
            if (state.block() != Block.AIR && state.block() != Block.WATER) {
                truncate(layers, totalHeight, offset, true);
                break;
            }
            ++checkY;
        }
        int blockY = originY;
        for (int body = 0; body < layers[0]; ++body) {
            world.setState(originX, blockY++, originZ,
                State.bigDripleafStem(direction, false));
        }
        for (int head = 0; head < layers[1]; ++head) {
            world.setState(originX, blockY++, originZ,
                State.bigDripleaf(direction, false));
        }
        return true;
    }

    private static void truncate(int[] layers, int totalHeight, int blockedOffset,
            boolean prioritizeTip) {
        int remove = totalHeight - blockedOffset;
        int direction = prioritizeTip ? 1 : -1;
        int layer = prioritizeTip ? 0 : layers.length - 1;
        int end = prioritizeTip ? layers.length : -1;
        while (layer != end && remove > 0) {
            int removed = Math.min(layers[layer], remove);
            layers[layer] -= removed;
            remove -= removed;
            layer += direction;
        }
    }

    private static boolean placeRootSystem(WorldAccess world, Random random,
            int originX, int originY, int originZ) {
        if (!world.isAir(originX, originY, originZ)) {
            return false;
        }
        boolean treePlaced = false;
        int treeY = originY;
        int heightIndex = -1;
        for (int height = 0; height < 100; ++height) {
            ++treeY;
            if (!allowedTreePosition(world, originX, treeY, originZ)
                    || !spaceForTree(world, originX, treeY, originZ)) {
                continue;
            }
            if (world.isLavaAt(originX, treeY - 1, originZ)
                    || !world.isSolid(originX, treeY - 1, originZ)) {
                break;
            }
            if (!world.ensureCanWrite(originX, treeY, originZ)
                    || !world.placeAzaleaTree(random, originX, treeY, originZ)) {
                continue;
            }
            treePlaced = true;
            heightIndex = height;
            break;
        }
        if (treePlaced) {
            for (int blockY = originY; blockY < originY + heightIndex; ++blockY) {
                placeRootedDirt(world, random, originX, blockY, originZ);
            }
            placeHangingRoots(world, random, originX, originY, originZ);
        }
        return true;
    }

    private static boolean allowedTreePosition(WorldAccess world,
            int blockX, int blockY, int blockZ) {
        State state = world.getState(blockX, blockY, blockZ);
        boolean replaceable = state.block() == Block.AIR || state.block() == Block.WATER
            || world.isLeaves(blockX, blockY, blockZ)
            || world.isReplaceablePlant(blockX, blockY, blockZ);
        return replaceable && world.isAzaleaGrowsOn(blockX, blockY - 1, blockZ);
    }

    private static boolean spaceForTree(WorldAccess world,
            int blockX, int blockY, int blockZ) {
        for (int step = 1; step <= 3; ++step) {
            State state = world.getState(blockX, blockY + step, blockZ);
            if (state.block() == Block.AIR) {
                continue;
            }
            if (step + 1 <= 2 && state.block() == Block.WATER) {
                continue;
            }
            return false;
        }
        return true;
    }

    private static void placeRootedDirt(WorldAccess world, Random random,
            int centerX, int blockY, int centerZ) {
        int cursorX = centerX;
        int cursorZ = centerZ;
        for (int attempt = 0; attempt < 20; ++attempt) {
            cursorX += random.nextInt(3) - random.nextInt(3);
            cursorZ += random.nextInt(3) - random.nextInt(3);
            if (world.isAzaleaRootReplaceable(cursorX, blockY, cursorZ)) {
                world.setState(cursorX, blockY, cursorZ, State.ROOTED_DIRT);
            }
            cursorX = centerX;
            cursorZ = centerZ;
        }
    }

    private static void placeHangingRoots(WorldAccess world, Random random,
            int originX, int originY, int originZ) {
        for (int attempt = 0; attempt < 20; ++attempt) {
            int blockX = originX + random.nextInt(3) - random.nextInt(3);
            int blockY = originY + random.nextInt(2) - random.nextInt(2);
            int blockZ = originZ + random.nextInt(3) - random.nextInt(3);
            if (!world.isAir(blockX, blockY, blockZ)
                    || !world.canSurvive(State.HANGING_ROOTS, blockX, blockY, blockZ)
                    || !world.hasSturdyFace(blockX, blockY + 1, blockZ, Direction.DOWN)) {
                continue;
            }
            world.setState(blockX, blockY, blockZ, State.HANGING_ROOTS);
        }
    }

    private static boolean placeClassicVine(WorldAccess world,
            int originX, int originY, int originZ) {
        if (!world.isAir(originX, originY, originZ)) {
            return false;
        }
        for (Direction direction : VINE_DIRECTIONS) {
            if (direction == Direction.DOWN) {
                continue;
            }
            int neighborX = originX + direction.stepX();
            int neighborY = originY + direction.stepY();
            int neighborZ = originZ + direction.stepZ();
            if (!world.isAcceptableVineNeighbor(neighborX, neighborY, neighborZ, direction)) {
                continue;
            }
            world.setState(originX, originY, originZ, State.vine(direction));
            return true;
        }
        return false;
    }

    private static boolean isExposed(WorldAccess world, Position position) {
        for (Direction direction : POOL_EXPOSURE_DIRECTIONS) {
            int neighborX = position.x + direction.stepX();
            int neighborY = position.y + direction.stepY();
            int neighborZ = position.z + direction.stepZ();
            if (!world.hasSturdyFace(neighborX, neighborY, neighborZ,
                    direction.opposite())) {
                return true;
            }
        }
        return false;
    }

    private static int randomBetweenInclusive(Random random, int minimum, int maximum) {
        return minimum == maximum ? minimum : minimum + random.nextInt(maximum - minimum + 1);
    }

    private enum PatchConfiguration {
        MOSS_FLOOR(false, State.MOSS_BLOCK, 1, 1, 0.0F, 5, 0.8F, 0.3F, false),
        MOSS_CEILING(true, State.MOSS_BLOCK, 1, 2, 0.0F, 5, 0.08F, 0.3F, false),
        CLAY_DRY(false, State.CLAY, 3, 3, 0.8F, 2, 0.05F, 0.7F, false),
        CLAY_POOL(false, State.CLAY, 3, 3, 0.8F, 5, 0.1F, 0.7F, true);

        private final boolean ceiling;
        private final State ground;
        private final int minimumDepth;
        private final int maximumDepth;
        private final float extraBottomBlockChance;
        private final int verticalRange;
        private final float vegetationChance;
        private final float extraEdgeColumnChance;
        private final boolean waterlogged;

        PatchConfiguration(boolean ceiling, State ground, int minimumDepth, int maximumDepth,
                float extraBottomBlockChance, int verticalRange, float vegetationChance,
                float extraEdgeColumnChance, boolean waterlogged) {
            this.ceiling = ceiling;
            this.ground = ground;
            this.minimumDepth = minimumDepth;
            this.maximumDepth = maximumDepth;
            this.extraBottomBlockChance = extraBottomBlockChance;
            this.verticalRange = verticalRange;
            this.vegetationChance = vegetationChance;
            this.extraEdgeColumnChance = extraEdgeColumnChance;
            this.waterlogged = waterlogged;
        }

        private int sampleDepth(Random random) {
            return randomBetweenInclusive(random, minimumDepth, maximumDepth);
        }
    }

    public enum Direction {
        DOWN(0, -1, 0),
        UP(0, 1, 0),
        NORTH(0, 0, -1),
        SOUTH(0, 0, 1),
        WEST(-1, 0, 0),
        EAST(1, 0, 0);

        private final int stepX;
        private final int stepY;
        private final int stepZ;

        Direction(int stepX, int stepY, int stepZ) {
            this.stepX = stepX;
            this.stepY = stepY;
            this.stepZ = stepZ;
        }

        public int stepX() { return stepX; }
        public int stepY() { return stepY; }
        public int stepZ() { return stepZ; }

        public Direction opposite() {
            switch (this) {
                case DOWN: return UP;
                case UP: return DOWN;
                case NORTH: return SOUTH;
                case SOUTH: return NORTH;
                case WEST: return EAST;
                case EAST: return WEST;
                default: throw new AssertionError(this);
            }
        }
    }

    public enum Block {
        AIR,
        WATER,
        LAVA,
        STONE,
        DIRT,
        CLAY,
        GRAVEL,
        SAND,
        MOSS_BLOCK,
        MOSS_CARPET,
        AZALEA,
        FLOWERING_AZALEA,
        GRASS,
        TALL_GRASS,
        ROOTED_DIRT,
        HANGING_ROOTS,
        CAVE_VINES_BODY,
        CAVE_VINES_HEAD,
        SMALL_DRIPLEAF,
        BIG_DRIPLEAF_STEM,
        BIG_DRIPLEAF,
        SPORE_BLOSSOM,
        VINE,
        OTHER
    }

    public static final class State {
        public static final State AIR = simple(Block.AIR);
        public static final State WATER = simple(Block.WATER);
        public static final State LAVA = simple(Block.LAVA);
        public static final State STONE = simple(Block.STONE);
        public static final State DIRT = simple(Block.DIRT);
        public static final State CLAY = simple(Block.CLAY);
        public static final State GRAVEL = simple(Block.GRAVEL);
        public static final State SAND = simple(Block.SAND);
        public static final State MOSS_BLOCK = simple(Block.MOSS_BLOCK);
        public static final State MOSS_CARPET = simple(Block.MOSS_CARPET);
        public static final State AZALEA = simple(Block.AZALEA);
        public static final State FLOWERING_AZALEA = simple(Block.FLOWERING_AZALEA);
        public static final State GRASS = simple(Block.GRASS);
        public static final State ROOTED_DIRT = simple(Block.ROOTED_DIRT);
        public static final State HANGING_ROOTS = new State(Block.HANGING_ROOTS,
            null, false, false, 0, false);
        public static final State SPORE_BLOSSOM = simple(Block.SPORE_BLOSSOM);
        public static final State OTHER = simple(Block.OTHER);

        private final Block block;
        private final Direction facing;
        private final boolean upper;
        private final boolean berries;
        private final int age;
        private final boolean waterlogged;

        private State(Block block, Direction facing, boolean upper, boolean berries,
                int age, boolean waterlogged) {
            this.block = block;
            this.facing = facing;
            this.upper = upper;
            this.berries = berries;
            this.age = age;
            this.waterlogged = waterlogged;
        }

        private static State simple(Block block) {
            return new State(block, null, false, false, 0, false);
        }

        public static State tallGrass(boolean upper) {
            return new State(Block.TALL_GRASS, null, upper, false, 0, false);
        }

        public static State caveVineBody(boolean berries) {
            return new State(Block.CAVE_VINES_BODY, null, false, berries, 0, false);
        }

        public static State caveVineHead(int age, boolean berries) {
            return new State(Block.CAVE_VINES_HEAD, null, false, berries, age, false);
        }

        public static State smallDripleaf(Direction facing, boolean upper,
                boolean waterlogged) {
            return new State(Block.SMALL_DRIPLEAF, facing, upper, false, 0, waterlogged);
        }

        public static State bigDripleafStem(Direction facing, boolean waterlogged) {
            return new State(Block.BIG_DRIPLEAF_STEM, facing, false, false, 0, waterlogged);
        }

        public static State bigDripleaf(Direction facing, boolean waterlogged) {
            return new State(Block.BIG_DRIPLEAF, facing, false, false, 0, waterlogged);
        }

        public static State vine(Direction face) {
            return new State(Block.VINE, face, false, false, 0, false);
        }

        public Block block() { return block; }
        public Direction facing() { return facing; }
        public boolean upper() { return upper; }
        public boolean berries() { return berries; }
        public int age() { return age; }
        public boolean waterlogged() { return waterlogged; }

        public boolean supportsWaterlogging() {
            return block == Block.SMALL_DRIPLEAF || block == Block.BIG_DRIPLEAF
                || block == Block.BIG_DRIPLEAF_STEM || block == Block.HANGING_ROOTS;
        }

        public State withWaterlogged(boolean value) {
            if (!supportsWaterlogging()) {
                return this;
            }
            return new State(block, facing, upper, berries, age, value);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof State)) return false;
            State other = (State) object;
            return block == other.block && facing == other.facing && upper == other.upper
                && berries == other.berries && age == other.age
                && waterlogged == other.waterlogged;
        }

        @Override
        public int hashCode() {
            int result = block.hashCode();
            result = 31 * result + (facing == null ? 0 : facing.hashCode());
            result = 31 * result + (upper ? 1 : 0);
            result = 31 * result + (berries ? 1 : 0);
            result = 31 * result + age;
            result = 31 * result + (waterlogged ? 1 : 0);
            return result;
        }

        public String encode() {
            String name = block.name().toLowerCase(java.util.Locale.ROOT);
            if (block == Block.CAVE_VINES_BODY) {
                return name + "[berries=" + berries + ']';
            }
            if (block == Block.CAVE_VINES_HEAD) {
                return name + "[age=" + age + ",berries=" + berries + ']';
            }
            if (block == Block.SMALL_DRIPLEAF) {
                return name + "[facing=" + directionName(facing) + ",half="
                    + (upper ? "upper" : "lower") + ",waterlogged=" + waterlogged + ']';
            }
            if (block == Block.BIG_DRIPLEAF || block == Block.BIG_DRIPLEAF_STEM) {
                return name + "[facing=" + directionName(facing) + ",waterlogged="
                    + waterlogged + ']';
            }
            if (block == Block.TALL_GRASS) {
                return name + "[half=" + (upper ? "upper" : "lower") + ']';
            }
            if (block == Block.VINE) {
                return name + "[face=" + directionName(facing) + ']';
            }
            if (block == Block.HANGING_ROOTS) {
                return name + "[waterlogged=" + waterlogged + ']';
            }
            return name;
        }

        private static String directionName(Direction direction) {
            return direction == null ? "none"
                : direction.name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private static final class Position {
        private final int x;
        private final int y;
        private final int z;

        private Position(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof Position)) return false;
            Position other = (Position) object;
            return x == other.x && y == other.y && z == other.z;
        }

        @Override
        public int hashCode() {
            return (y + z * 31) * 31 + x;
        }
    }

    public interface WorldAccess extends V118LushCavePlacements.PlacementAccess {
        State getState(int blockX, int blockY, int blockZ);

        void setState(int blockX, int blockY, int blockZ, State state);

        boolean ensureCanWrite(int blockX, int blockY, int blockZ);

        boolean isMossReplaceable(int blockX, int blockY, int blockZ);

        boolean isLushGroundReplaceable(int blockX, int blockY, int blockZ);

        boolean isAzaleaRootReplaceable(int blockX, int blockY, int blockZ);

        boolean isAzaleaGrowsOn(int blockX, int blockY, int blockZ);

        boolean isLeaves(int blockX, int blockY, int blockZ);

        boolean isReplaceablePlant(int blockX, int blockY, int blockZ);

        boolean isLavaAt(int blockX, int blockY, int blockZ);

        boolean isWaterAt(int blockX, int blockY, int blockZ);

        boolean hasSturdyFace(int blockX, int blockY, int blockZ, Direction face);

        boolean canSurvive(State state, int blockX, int blockY, int blockZ);

        boolean isAcceptableVineNeighbor(int blockX, int blockY, int blockZ,
            Direction attachmentDirection);

        /**
         * Places the canonical AZALEA_TREE configured feature and consumes its random stream.
         * The 1.18.2 azalea configuration has no beehive decorator; bee integrations must keep
         * this callback distinct from ordinary bee-decorated oak/birch tree callbacks.
         */
        boolean placeAzaleaTree(Random random, int blockX, int blockY, int blockZ);

        @Override
        default boolean isAir(int blockX, int blockY, int blockZ) {
            return getState(blockX, blockY, blockZ).block() == Block.AIR;
        }

        @Override
        boolean isSolid(int blockX, int blockY, int blockZ);

        @Override
        default boolean hasSturdyDownFace(int blockX, int blockY, int blockZ) {
            return hasSturdyFace(blockX, blockY, blockZ, Direction.DOWN);
        }
    }
}
