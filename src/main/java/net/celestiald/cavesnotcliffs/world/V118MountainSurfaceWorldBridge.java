package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.block.BlockPowderSnow;
import net.celestiald.cavesnotcliffs.block.CncFluidState;
import net.celestiald.cavesnotcliffs.block.LushAzaleaBlocks;
import net.celestiald.cavesnotcliffs.block.LushMossBlocks;
import net.celestiald.cavesnotcliffs.content.DeadBushSupportHooks;
import net.celestiald.cavesnotcliffs.content.LushCaveContent;
import net.celestiald.cavesnotcliffs.content.PlainPumpkinContent;
import net.celestiald.cavesnotcliffs.worldgen.v118.TerrainColumn;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118Biome;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118DefaultSpringPlacements;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118DefaultSpringPlacements.SpringFluid;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118Material;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118MountainSurfacePlacements;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockStone;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import java.util.Set;
import java.util.function.Predicate;

/** Mutable finite-world adapter for the represented native schema-2 surface features. */
final class V118MountainSurfaceWorldBridge
        implements V118MountainSurfacePlacements.WorldAccess,
            V118DefaultSpringPlacements.WorldAccess {
    private final World world;
    private final V118ChunkGenerator generator;
    private final SpringValidBlocks springValidBlocks;

    V118MountainSurfaceWorldBridge(World world, V118ChunkGenerator generator,
            V118BlockStateMapper blockStates) {
        if (world == null || generator == null || blockStates == null) {
            throw new NullPointerException("world, generator, and blockStates are required");
        }
        this.world = world;
        this.generator = generator;
        springValidBlocks = springValidBlocks(blockStates);
    }

    V118DefaultSpringPlacements.DecorationResult populateDefaultSprings(
            int chunkX, int chunkZ, Set<V118Biome> regionBiomes) {
        return V118DefaultSpringPlacements.decorate(this, world.getSeed(),
            chunkX, chunkZ, regionBiomes);
    }

    V118MountainSurfacePlacements.DecorationResult populateFrozenSprings(
            int chunkX, int chunkZ, Set<V118Biome> regionBiomes) {
        return V118MountainSurfacePlacements.decorateFrozenSprings(this, world.getSeed(),
            chunkX, chunkZ, regionBiomes);
    }

    V118MountainSurfacePlacements.DecorationResult populateVegetation(
            int chunkX, int chunkZ, Set<V118Biome> regionBiomes) {
        return V118MountainSurfacePlacements.decorateVegetation(this, world.getSeed(),
            chunkX, chunkZ, regionBiomes);
    }

    V118MountainSurfacePlacements.DecorationResult populateTopLayer(
            int chunkX, int chunkZ, Set<V118Biome> regionBiomes) {
        return V118MountainSurfacePlacements.decorateTopLayer(this, world.getSeed(),
            chunkX, chunkZ, regionBiomes);
    }

    @Override
    public int minBuildHeight() {
        return TerrainColumn.MIN_Y;
    }

    @Override
    public int maxBuildHeight() {
        return TerrainColumn.MAX_Y_EXCLUSIVE;
    }

    @Override
    public V118Biome biomeAt(BlockPos pos) {
        return generator.getDecorationBiome(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public int worldSurfaceHeight(int blockX, int blockZ) {
        return firstAvailableHeight(blockX, blockZ,
            state -> state.getMaterial() != Material.AIR);
    }

    @Override
    public int oceanFloorHeight(int blockX, int blockZ) {
        return firstAvailableHeight(blockX, blockZ,
            state -> !isPowderSnow(state) && state.getMaterial().blocksMovement());
    }

    @Override
    public int motionBlockingHeight(int blockX, int blockZ) {
        return firstAvailableHeight(blockX, blockZ,
            V118MountainSurfaceWorldBridge::isMotionBlockingState);
    }

    static boolean isMotionBlockingState(IBlockState state) {
        return !isPowderSnow(state) && (state.getMaterial().blocksMovement()
            || state.getMaterial().isLiquid() || CncFluidState.containsWater(state));
    }

    private int firstAvailableHeight(int blockX, int blockZ,
            Predicate<IBlockState> predicate) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = TerrainColumn.MAX_Y; y >= TerrainColumn.MIN_Y; --y) {
            cursor.setPos(blockX, y, blockZ);
            if (predicate.test(world.getBlockState(cursor))) {
                return y + 1;
            }
        }
        return TerrainColumn.MIN_Y;
    }

    @Override
    public int blockLight(BlockPos pos) {
        return world.getLightFor(EnumSkyBlock.BLOCK, pos);
    }

    @Override
    public boolean isAir(BlockPos pos) {
        return inside(pos) && world.isAirBlock(pos);
    }

    @Override
    public boolean isWater(BlockPos pos) {
        return inside(pos) && world.getBlockState(pos).getMaterial() == Material.WATER;
    }

    @Override
    public boolean isPowderSnow(BlockPos pos) {
        return inside(pos) && isPowderSnow(world.getBlockState(pos));
    }

    private static boolean isPowderSnow(IBlockState state) {
        return BlockPowderSnow.block != null && state.getBlock() == BlockPowderSnow.block;
    }

    @Override
    public boolean isSnowTreeSupport(BlockPos pos) {
        if (!inside(pos)) {
            return false;
        }
        Block block = world.getBlockState(pos).getBlock();
        return block == Blocks.SNOW || block == BlockPowderSnow.block;
    }

    @Override
    public boolean isFrozenSpringValid(BlockPos pos) {
        if (!inside(pos)) {
            return false;
        }
        Block block = world.getBlockState(pos).getBlock();
        return block == Blocks.SNOW || block == Blocks.PACKED_ICE
            || block == BlockPowderSnow.block;
    }

    @Override
    public boolean isSpringValid(BlockPos pos, SpringFluid fluid) {
        if (!inside(pos) || fluid == null) {
            return false;
        }
        return springValidBlocks.accepts(world.getBlockState(pos), fluid);
    }

    @Override
    public boolean isGrassBlock(BlockPos pos) {
        return inside(pos) && world.getBlockState(pos).getBlock() == Blocks.GRASS;
    }

    @Override
    public boolean canDeadBushSurvive(BlockPos pos) {
        return inside(pos) && inside(pos.down())
            && isDeadBushSupport(world.getBlockState(pos.down()));
    }

    @Override
    public boolean canSugarCaneSurvive(BlockPos pos) {
        if (!inside(pos)) {
            return false;
        }
        Block support = world.getBlockState(pos.down()).getBlock();
        return support == Blocks.REEDS
            || isSugarCaneGround(support) && hasSugarCaneSurvivalFluid(pos);
    }

    @Override
    public boolean isSugarCanePlacementAir(BlockPos pos) {
        return inside(pos) && world.getBlockState(pos).getBlock() == Blocks.AIR;
    }

    @Override
    public boolean canCactusSurvive(BlockPos pos) {
        // Keep generation explicit so Forge's extensible canSustainPlant hook cannot widen the
        // registered 1.18.2 configured feature to unrelated third-party ground blocks.
        if (!inside(pos)) {
            return false;
        }
        for (EnumFacing direction : EnumFacing.Plane.HORIZONTAL) {
            IBlockState neighbor = world.getBlockState(pos.offset(direction));
            if (neighbor.getMaterial().isSolid()
                    || neighbor.getMaterial() == Material.LAVA) {
                return false;
            }
        }
        Block support = inside(pos.down())
            ? world.getBlockState(pos.down()).getBlock() : Blocks.AIR;
        if (support != Blocks.CACTUS && support != Blocks.SAND) {
            return false;
        }
        IBlockState above = inside(pos.up())
            ? world.getBlockState(pos.up()) : Blocks.AIR.getDefaultState();
        return !above.getMaterial().isLiquid();
    }

    @Override
    public boolean supportsCactusPlacement() {
        return true;
    }

    @Override
    public boolean hasAdjacentWaterBelow(BlockPos pos) {
        BlockPos below = pos.down();
        return containsWater(below.east()) || containsWater(below.west())
            || containsWater(below.south()) || containsWater(below.north());
    }

    private boolean hasSugarCaneSurvivalFluid(BlockPos pos) {
        BlockPos below = pos.down();
        return isWaterOrFrostedIce(below.north()) || isWaterOrFrostedIce(below.east())
            || isWaterOrFrostedIce(below.south()) || isWaterOrFrostedIce(below.west());
    }

    private boolean isWaterOrFrostedIce(BlockPos pos) {
        if (!inside(pos)) {
            return false;
        }
        IBlockState state = world.getBlockState(pos);
        return CncFluidState.containsWater(state) || state.getBlock() == Blocks.FROSTED_ICE;
    }

    private boolean containsWater(BlockPos pos) {
        return inside(pos) && CncFluidState.containsWater(world.getBlockState(pos));
    }

    @Override
    public boolean canSnowSurvive(BlockPos pos) {
        return inside(pos) && Blocks.SNOW_LAYER.canPlaceBlockAt(world, pos);
    }

    @Override
    public boolean isFree(BlockPos pos) {
        if (!inside(pos)) {
            return false;
        }
        IBlockState state = world.getBlockState(pos);
        return isValidTreePos(pos) || state.getBlock().isWood(world, pos);
    }

    @Override
    public boolean isValidTreePos(BlockPos pos) {
        if (!inside(pos)) {
            return false;
        }
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        Material material = state.getMaterial();
        return material == Material.AIR || block.isLeaves(state, world, pos)
            || material == Material.PLANTS || material == Material.VINE
            || block == Blocks.WATER || block == Blocks.FLOWING_WATER;
    }

    @Override
    public void setDirt(BlockPos pos) {
        if (!inside(pos)) {
            return;
        }
        Block block = world.getBlockState(pos).getBlock();
        if (!isDirtTag(block) || block == Blocks.GRASS || block == Blocks.MYCELIUM) {
            world.setBlockState(pos, Blocks.DIRT.getDefaultState(), 2);
        }
    }

    @Override
    public void setSpruceLog(BlockPos pos) {
        if (inside(pos)) {
            world.setBlockState(pos, Blocks.LOG.getStateFromMeta(1), 2);
        }
    }

    @Override
    public void setSpruceLeaves(BlockPos pos) {
        if (!inside(pos)) {
            return;
        }
        IBlockState state = Blocks.LEAVES.getStateFromMeta(1)
            .withProperty(BlockLeaves.CHECK_DECAY, false)
            .withProperty(BlockLeaves.DECAYABLE, true);
        world.setBlockState(pos, state, 2);
    }

    @Override
    public void setLava(BlockPos pos) {
        if (inside(pos)) {
            world.setBlockState(pos, Blocks.FLOWING_LAVA.getDefaultState(), 2);
        }
    }

    @Override
    public void scheduleLavaTick(BlockPos pos) {
        if (inside(pos)) {
            world.scheduleUpdate(pos, Blocks.FLOWING_LAVA, 0);
        }
    }

    @Override
    public void setSpring(BlockPos pos, SpringFluid fluid) {
        if (!inside(pos)) {
            return;
        }
        world.setBlockState(pos, springBlock(fluid).getDefaultState(), 2);
    }

    @Override
    public void scheduleSpringTick(BlockPos pos, SpringFluid fluid) {
        if (!inside(pos)) {
            return;
        }
        world.scheduleUpdate(pos, springBlock(fluid), 0);
    }

    @Override
    public void setIce(BlockPos pos) {
        if (inside(pos)) {
            world.setBlockState(pos, Blocks.ICE.getDefaultState(), 2);
        }
    }

    @Override
    public void setSnowLayer(BlockPos pos) {
        if (inside(pos)) {
            world.setBlockState(pos, Blocks.SNOW_LAYER.getDefaultState(), 2);
        }
    }

    @Override
    public void setDeadBush(BlockPos pos) {
        if (inside(pos)) {
            world.setBlockState(pos, Blocks.DEADBUSH.getDefaultState(), 2);
        }
    }

    @Override
    public void setSugarCane(BlockPos pos) {
        if (inside(pos)) {
            world.setBlockState(pos, Blocks.REEDS.getDefaultState(), 2);
        }
    }

    @Override
    public void setCactus(BlockPos pos) {
        if (inside(pos)) {
            world.setBlockState(pos, Blocks.CACTUS.getDefaultState(), 2);
        }
    }

    @Override
    public void setPumpkin(BlockPos pos) {
        if (inside(pos)) {
            if (PlainPumpkinContent.PUMPKIN == null) {
                throw new IllegalStateException("Plain pumpkin block is not registered");
            }
            world.setBlockState(pos, PlainPumpkinContent.PUMPKIN.getDefaultState(), 2);
        }
    }

    static boolean isSugarCaneGround(Block block) {
        return isDirtTag(block) || block == Blocks.SAND;
    }

    static boolean isDeadBushSupport(IBlockState state) {
        return DeadBushSupportHooks.isJava118Support(state);
    }

    static SpringValidBlocks springValidBlocks(V118BlockStateMapper blockStates) {
        if (blockStates == null) {
            throw new NullPointerException("blockStates");
        }
        return new SpringValidBlocks(
            blockStates.stateFor(V118Material.DEEPSLATE).getBlock(),
            blockStates.stateFor(V118Material.TUFF).getBlock(),
            blockStates.stateFor(V118Material.CALCITE).getBlock(),
            blockStates.stateFor(V118Material.POWDER_SNOW).getBlock());
    }

    static Block springBlock(SpringFluid fluid) {
        if (fluid == null) {
            throw new NullPointerException("fluid");
        }
        return fluid == SpringFluid.WATER ? Blocks.FLOWING_WATER : Blocks.FLOWING_LAVA;
    }

    private static boolean isDirtTag(Block block) {
        return block == Blocks.DIRT || block == Blocks.GRASS || block == Blocks.MYCELIUM
            || block == LushCaveContent.ROOTED_DIRT || block == LushCaveContent.MOSS_BLOCK
            || block instanceof LushAzaleaBlocks.RootedDirt
            || block instanceof LushMossBlocks.Moss;
    }

    private static boolean inside(BlockPos pos) {
        return pos.getY() >= TerrainColumn.MIN_Y
            && pos.getY() < TerrainColumn.MAX_Y_EXCLUSIVE;
    }

    static final class SpringValidBlocks {
        private final Block deepslate;
        private final Block tuff;
        private final Block calcite;
        private final Block powderSnow;

        private SpringValidBlocks(Block deepslate, Block tuff,
                Block calcite, Block powderSnow) {
            this.deepslate = deepslate;
            this.tuff = tuff;
            this.calcite = calcite;
            this.powderSnow = powderSnow;
        }

        boolean accepts(IBlockState state, SpringFluid fluid) {
            if (state == null || fluid == null) {
                return false;
            }
            Block block = state.getBlock();
            if (block == deepslate || block == tuff || block == calcite) {
                return true;
            }
            if (block == Blocks.STONE) {
                int metadata = state.getValue(BlockStone.VARIANT).getMetadata();
                return metadata == 0 || metadata == 1 || metadata == 3 || metadata == 5;
            }
            if (block == Blocks.DIRT) {
                return state.getValue(BlockDirt.VARIANT) == BlockDirt.DirtType.DIRT;
            }
            return fluid == SpringFluid.WATER
                && (block == Blocks.SNOW || block == Blocks.PACKED_ICE
                    || block == powderSnow);
        }
    }
}
