package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.block.BlockPowderSnow;
import net.celestiald.cavesnotcliffs.block.LushAzaleaBlocks;
import net.celestiald.cavesnotcliffs.block.LushMossBlocks;
import net.celestiald.cavesnotcliffs.content.LushCaveContent;
import net.celestiald.cavesnotcliffs.content.PlainPumpkinContent;
import net.celestiald.cavesnotcliffs.worldgen.v118.TerrainColumn;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118Biome;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118MountainSurfacePlacements;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import java.util.Set;
import java.util.function.Predicate;

/** Mutable finite-world adapter for the native schema-2 mountain surface features. */
final class V118MountainSurfaceWorldBridge
        implements V118MountainSurfacePlacements.WorldAccess {
    private final World world;
    private final V118ChunkGenerator generator;

    V118MountainSurfaceWorldBridge(World world, V118ChunkGenerator generator) {
        if (world == null || generator == null) {
            throw new NullPointerException("world and generator are required");
        }
        this.world = world;
        this.generator = generator;
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
        return firstAvailableHeight(blockX, blockZ, state ->
            !isPowderSnow(state) && (state.getMaterial().blocksMovement()
                || state.getMaterial().isLiquid()));
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
    public boolean isGrassBlock(BlockPos pos) {
        return inside(pos) && world.getBlockState(pos).getBlock() == Blocks.GRASS;
    }

    @Override
    public boolean canSugarCaneSurvive(BlockPos pos) {
        if (!inside(pos)) {
            return false;
        }
        Block support = world.getBlockState(pos.down()).getBlock();
        return support == Blocks.REEDS
            || isSugarCaneGround(support) && hasAdjacentWaterBelow(pos);
    }

    @Override
    public boolean hasAdjacentWaterBelow(BlockPos pos) {
        BlockPos below = pos.down();
        return isWater(below.east()) || isWater(below.west())
            || isWater(below.north()) || isWater(below.south());
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
            world.setBlockState(pos, Blocks.LAVA.getDefaultState(), 2);
        }
    }

    @Override
    public void scheduleLavaTick(BlockPos pos) {
        if (inside(pos)) {
            world.scheduleUpdate(pos, Blocks.LAVA, 0);
        }
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
    public void setSugarCane(BlockPos pos) {
        if (inside(pos)) {
            world.setBlockState(pos, Blocks.REEDS.getDefaultState(), 2);
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
}
