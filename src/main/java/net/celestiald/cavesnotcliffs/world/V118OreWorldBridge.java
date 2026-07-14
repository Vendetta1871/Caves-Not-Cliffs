package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.worldgen.v118.TerrainColumn;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118Biome;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118OreMaterial;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118OrePlacements;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;

/** Mutable finite-column population view used by the exact ordinary ore/blob port. */
final class V118OreWorldBridge implements V118OrePlacements.WorldAccess {
    private final World world;
    private final V118ChunkGenerator generator;
    private final V118OreBlockMapper blocks;
    private final Map<Long, Integer> oceanFloorHeights = new HashMap<>();

    V118OreWorldBridge(World world, V118ChunkGenerator generator,
            V118OreBlockMapper blocks) {
        if (world == null || generator == null || blocks == null) {
            throw new NullPointerException("world, generator, and blocks are required");
        }
        this.world = world;
        this.generator = generator;
        this.blocks = blocks;
    }

    V118OrePlacements.DecorationResult populate(int chunkX, int chunkZ,
            Set<V118Biome> regionBiomes) {
        return populate(chunkX, chunkZ, regionBiomes,
            V118OrePlacements.BetweenDecorationSteps.NONE);
    }

    V118OrePlacements.DecorationResult populate(int chunkX, int chunkZ,
            Set<V118Biome> regionBiomes,
            V118OrePlacements.BetweenDecorationSteps betweenSteps) {
        oceanFloorHeights.clear();
        return V118OrePlacements.decorate(this, world.getSeed(), chunkX, chunkZ,
            regionBiomes, betweenSteps);
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
    public V118OreMaterial getMaterial(int blockX, int blockY, int blockZ) {
        if (isOutsideBuildHeight(blockY)) {
            return V118OreMaterial.AIR;
        }
        return blocks.materialFor(world.getBlockState(new BlockPos(blockX, blockY, blockZ)));
    }

    @Override
    public void setMaterial(int blockX, int blockY, int blockZ,
            V118OreMaterial material) {
        if (isOutsideBuildHeight(blockY)) {
            return;
        }
        world.setBlockState(new BlockPos(blockX, blockY, blockZ),
            blocks.stateFor(material), 2);
    }

    @Override
    public int oceanFloorHeight(int blockX, int blockZ) {
        long key = ((long) blockX & 0xffffffffL) << 32 | (blockZ & 0xffffffffL);
        Integer cached = oceanFloorHeights.get(key);
        if (cached != null) {
            return cached;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int height = highestMotionBlockingY(TerrainColumn.MIN_Y, TerrainColumn.MAX_Y,
            blockY -> {
                cursor.setPos(blockX, blockY, blockZ);
                IBlockState state = world.getBlockState(cursor);
                return state.getMaterial().blocksMovement();
            });
        oceanFloorHeights.put(key, height);
        return height;
    }

    @Override
    public V118Biome biomeAt(int blockX, int blockY, int blockZ) {
        return generator.getVirtualBiome(blockX, clampBiomeY(blockY), blockZ);
    }

    static int clampBiomeY(int blockY) {
        return Math.max(TerrainColumn.MIN_Y, Math.min(TerrainColumn.MAX_Y, blockY));
    }

    static int highestMotionBlockingY(int minimumY, int maximumY,
            IntPredicate blocksMovementAtY) {
        if (blocksMovementAtY == null) {
            throw new NullPointerException("blocksMovementAtY");
        }
        for (int blockY = maximumY; blockY >= minimumY; --blockY) {
            if (blocksMovementAtY.test(blockY)) {
                return blockY;
            }
        }
        // ChunkAccess#getHeight returns Heightmap#getFirstAvailable - 1.
        return minimumY - 1;
    }
}
