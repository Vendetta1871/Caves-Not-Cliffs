package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.block.BlockPowderSnow;
import net.celestiald.cavesnotcliffs.block.LushAzaleaBlocks;
import net.celestiald.cavesnotcliffs.block.LushCaveVinesBlock;
import net.celestiald.cavesnotcliffs.block.LushDripleafBlocks;
import net.celestiald.cavesnotcliffs.content.AzaleaTreeFeature;
import net.celestiald.cavesnotcliffs.content.LushCaveContent;
import net.celestiald.cavesnotcliffs.worldgen.v118.TerrainColumn;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118Biome;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118LushCaveFeature;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118LushCavePlacements;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockStone;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.BlockVine;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;
import java.util.Set;

/** Mutable CubicChunks population view for the exact schema-2 lush-cave feature port. */
final class V118LushCaveWorldBridge implements V118LushCaveFeature.WorldAccess {
    private static final int FEATURE_CHUNK_RADIUS = 1;

    private final World world;
    private final V118CubicChunksGenerator generator;
    private int targetChunkX;
    private int targetChunkZ;

    V118LushCaveWorldBridge(World world, V118CubicChunksGenerator generator) {
        if (world == null || generator == null) {
            throw new NullPointerException("world and generator are required");
        }
        this.world = world;
        this.generator = generator;
    }

    V118LushCavePlacements.DecorationResult populate(int chunkX, int chunkZ,
            Set<V118Biome> regionBiomes) {
        targetChunkX = chunkX;
        targetChunkZ = chunkZ;
        return V118LushCavePlacements.decorate(this, world.getSeed(), chunkX, chunkZ,
            regionBiomes);
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
    public V118LushCaveFeature.State getState(int blockX, int blockY, int blockZ) {
        if (isOutsideBuildHeight(blockY)) {
            return V118LushCaveFeature.State.AIR;
        }
        IBlockState state = world.getBlockState(new BlockPos(blockX, blockY, blockZ));
        Block block = state.getBlock();
        if (block == Blocks.AIR) {
            return V118LushCaveFeature.State.AIR;
        }
        if (block == Blocks.WATER || block == Blocks.FLOWING_WATER) {
            return V118LushCaveFeature.State.WATER;
        }
        if (block == Blocks.LAVA || block == Blocks.FLOWING_LAVA) {
            return V118LushCaveFeature.State.LAVA;
        }
        if (block == LushCaveContent.CAVE_VINES_PLANT) {
            return V118LushCaveFeature.State.caveVineBody(
                state.getValue(LushCaveVinesBlock.BERRIES));
        }
        if (block instanceof LushCaveVinesBlock.Head) {
            return V118LushCaveFeature.State.caveVineHead(
                ((LushCaveVinesBlock.Head) block).age(state),
                state.getValue(LushCaveVinesBlock.BERRIES));
        }
        if (block == LushCaveContent.SMALL_DRIPLEAF) {
            return V118LushCaveFeature.State.smallDripleaf(
                direction(state.getValue(LushDripleafBlocks.FACING)),
                state.getValue(LushDripleafBlocks.HALF) == LushDripleafBlocks.Half.UPPER,
                state.getValue(LushDripleafBlocks.WATERLOGGED));
        }
        if (block == LushCaveContent.BIG_DRIPLEAF
                || block == LushCaveContent.BIG_DRIPLEAF_WATERLOGGED) {
            return V118LushCaveFeature.State.bigDripleaf(
                direction(state.getValue(LushDripleafBlocks.FACING)),
                block == LushCaveContent.BIG_DRIPLEAF_WATERLOGGED);
        }
        if (block == LushCaveContent.BIG_DRIPLEAF_STEM) {
            return V118LushCaveFeature.State.bigDripleafStem(
                direction(state.getValue(LushDripleafBlocks.FACING)),
                state.getValue(LushDripleafBlocks.WATERLOGGED));
        }
        if (block == Blocks.DOUBLE_PLANT
                && state.getValue(BlockDoublePlant.VARIANT).getMeta() == 2) {
            return V118LushCaveFeature.State.tallGrass(
                state.getValue(BlockDoublePlant.HALF)
                    == BlockDoublePlant.EnumBlockHalf.UPPER);
        }
        if (block == Blocks.VINE) {
            for (EnumFacing facing : EnumFacing.values()) {
                if (facing != EnumFacing.DOWN
                        && state.getValue(BlockVine.getPropertyFor(facing))) {
                    return V118LushCaveFeature.State.vine(direction(facing));
                }
            }
            return V118LushCaveFeature.State.OTHER;
        }
        if (block == Blocks.TALLGRASS
                && state.getValue(BlockTallGrass.TYPE).getMeta() == 1) {
            return V118LushCaveFeature.State.GRASS;
        }
        if (block == LushCaveContent.MOSS_BLOCK) {
            return V118LushCaveFeature.State.MOSS_BLOCK;
        }
        if (block == LushCaveContent.MOSS_CARPET) {
            return V118LushCaveFeature.State.MOSS_CARPET;
        }
        if (block == LushCaveContent.AZALEA) {
            return V118LushCaveFeature.State.AZALEA;
        }
        if (block == LushCaveContent.FLOWERING_AZALEA) {
            return V118LushCaveFeature.State.FLOWERING_AZALEA;
        }
        if (block == LushCaveContent.ROOTED_DIRT) {
            return V118LushCaveFeature.State.ROOTED_DIRT;
        }
        if (block == LushCaveContent.HANGING_ROOTS
                || block == LushCaveContent.HANGING_ROOTS_WATERLOGGED) {
            return V118LushCaveFeature.State.HANGING_ROOTS.withWaterlogged(
                block == LushCaveContent.HANGING_ROOTS_WATERLOGGED);
        }
        if (block == LushCaveContent.SPORE_BLOSSOM) {
            return V118LushCaveFeature.State.SPORE_BLOSSOM;
        }
        if (block == Blocks.CLAY) {
            return V118LushCaveFeature.State.CLAY;
        }
        if (block == Blocks.GRAVEL) {
            return V118LushCaveFeature.State.GRAVEL;
        }
        if (block == Blocks.SAND) {
            return V118LushCaveFeature.State.SAND;
        }
        if (isDirtTag(state)) {
            return V118LushCaveFeature.State.DIRT;
        }
        if (isBaseStoneOverworld(state)) {
            return V118LushCaveFeature.State.STONE;
        }
        return V118LushCaveFeature.State.OTHER;
    }

    @Override
    public void setState(int blockX, int blockY, int blockZ,
            V118LushCaveFeature.State state) {
        if (isOutsideBuildHeight(blockY)) {
            return;
        }
        if (!insideFeatureChunks(blockX, blockZ)) {
            return;
        }
        world.setBlockState(new BlockPos(blockX, blockY, blockZ), stateFor(state), 2);
    }

    @Override
    public boolean ensureCanWrite(int blockX, int blockY, int blockZ) {
        return insideFeatureChunks(blockX, blockZ);
    }

    @Override
    public boolean isMossReplaceable(int blockX, int blockY, int blockZ) {
        IBlockState state = worldState(blockX, blockY, blockZ);
        return isBaseStoneOverworld(state) || isDirtTag(state)
            || LushCaveVinesBlock.isVine(state.getBlock());
    }

    @Override
    public boolean isLushGroundReplaceable(int blockX, int blockY, int blockZ) {
        IBlockState state = worldState(blockX, blockY, blockZ);
        Block block = state.getBlock();
        return isMossReplaceable(blockX, blockY, blockZ) || block == Blocks.CLAY
            || block == Blocks.GRAVEL || block == Blocks.SAND;
    }

    @Override
    public boolean isAzaleaRootReplaceable(int blockX, int blockY, int blockZ) {
        IBlockState state = worldState(blockX, blockY, blockZ);
        Block block = state.getBlock();
        return isBaseStoneOverworld(state) || isDirtTag(state) || isTerracotta(state)
            || block == Blocks.CLAY || block == Blocks.GRAVEL || block == Blocks.SAND
            || block == Blocks.SNOW || block == BlockPowderSnow.block;
    }

    @Override
    public boolean isAzaleaGrowsOn(int blockX, int blockY, int blockZ) {
        IBlockState state = worldState(blockX, blockY, blockZ);
        Block block = state.getBlock();
        return isDirtTag(state) || block == Blocks.SAND || isTerracotta(state)
            || block == Blocks.SNOW || block == BlockPowderSnow.block;
    }

    @Override
    public boolean isLeaves(int blockX, int blockY, int blockZ) {
        Block block = worldState(blockX, blockY, blockZ).getBlock();
        return block == Blocks.LEAVES || block == Blocks.LEAVES2
            || block == LushCaveContent.AZALEA_LEAVES
            || block == LushCaveContent.FLOWERING_AZALEA_LEAVES;
    }

    @Override
    public boolean isReplaceablePlant(int blockX, int blockY, int blockZ) {
        Block block = worldState(blockX, blockY, blockZ).getBlock();
        return block == Blocks.TALLGRASS || block == Blocks.DEADBUSH
            || block == Blocks.VINE || block == Blocks.DOUBLE_PLANT
            || block == LushCaveContent.HANGING_ROOTS
            || block == LushCaveContent.HANGING_ROOTS_WATERLOGGED
            || hasPath(block, "glow_lichen");
    }

    @Override
    public boolean isLavaAt(int blockX, int blockY, int blockZ) {
        Block block = worldState(blockX, blockY, blockZ).getBlock();
        return block == Blocks.LAVA || block == Blocks.FLOWING_LAVA;
    }

    @Override
    public boolean isWaterAt(int blockX, int blockY, int blockZ) {
        IBlockState state = worldState(blockX, blockY, blockZ);
        Block block = state.getBlock();
        if (block == Blocks.WATER || block == Blocks.FLOWING_WATER
                || block == LushCaveContent.BIG_DRIPLEAF_WATERLOGGED
                || block == LushCaveContent.HANGING_ROOTS_WATERLOGGED) {
            return true;
        }
        return (block == LushCaveContent.SMALL_DRIPLEAF
                || block == LushCaveContent.BIG_DRIPLEAF_STEM)
            && state.getValue(LushDripleafBlocks.WATERLOGGED);
    }

    @Override
    public boolean isSolid(int blockX, int blockY, int blockZ) {
        return worldState(blockX, blockY, blockZ).getMaterial().isSolid();
    }

    @Override
    public boolean hasSturdyFace(int blockX, int blockY, int blockZ,
            V118LushCaveFeature.Direction face) {
        BlockPos position = new BlockPos(blockX, blockY, blockZ);
        return world.getBlockState(position).isSideSolid(world, position, facing(face));
    }

    @Override
    public boolean canSurvive(V118LushCaveFeature.State state,
            int blockX, int blockY, int blockZ) {
        BlockPos position = new BlockPos(blockX, blockY, blockZ);
        return stateFor(state).getBlock().canPlaceBlockAt(world, position);
    }

    @Override
    public boolean isAcceptableVineNeighbor(int blockX, int blockY, int blockZ,
            V118LushCaveFeature.Direction attachmentDirection) {
        BlockPos position = new BlockPos(blockX, blockY, blockZ);
        return world.getBlockState(position).isSideSolid(world, position,
            facing(attachmentDirection).getOpposite());
    }

    @Override
    public boolean placeAzaleaTree(Random random, int blockX, int blockY, int blockZ) {
        return AzaleaTreeFeature.grow(world, random, new BlockPos(blockX, blockY, blockZ),
            LushCaveContent.ROOTED_DIRT, LushCaveContent.AZALEA_LEAVES,
            LushCaveContent.FLOWERING_AZALEA_LEAVES,
            LushAzaleaBlocks.AzaleaLeaves.DISTANCE,
            LushAzaleaBlocks.AzaleaLeaves.PERSISTENT);
    }

    @Override
    public V118Biome biomeAt(int blockX, int blockY, int blockZ) {
        return generator.getVirtualBiome(blockX, V118OreWorldBridge.clampBiomeY(blockY),
            blockZ);
    }

    private IBlockState stateFor(V118LushCaveFeature.State state) {
        switch (state.block()) {
            case AIR:
                return Blocks.AIR.getDefaultState();
            case WATER:
                return Blocks.WATER.getDefaultState();
            case LAVA:
                return Blocks.LAVA.getDefaultState();
            case STONE:
                return Blocks.STONE.getDefaultState();
            case DIRT:
                return Blocks.DIRT.getDefaultState();
            case CLAY:
                return Blocks.CLAY.getDefaultState();
            case GRAVEL:
                return Blocks.GRAVEL.getDefaultState();
            case SAND:
                return Blocks.SAND.getDefaultState();
            case MOSS_BLOCK:
                return LushCaveContent.MOSS_BLOCK.getDefaultState();
            case MOSS_CARPET:
                return LushCaveContent.MOSS_CARPET.getDefaultState();
            case AZALEA:
                return LushCaveContent.AZALEA.getDefaultState();
            case FLOWERING_AZALEA:
                return LushCaveContent.FLOWERING_AZALEA.getDefaultState();
            case GRASS:
                return Blocks.TALLGRASS.getDefaultState()
                    .withProperty(BlockTallGrass.TYPE,
                        BlockTallGrass.EnumType.byMetadata(1));
            case TALL_GRASS:
                return Blocks.DOUBLE_PLANT.getDefaultState()
                    .withProperty(BlockDoublePlant.VARIANT,
                        BlockDoublePlant.EnumPlantType.byMetadata(2))
                    .withProperty(BlockDoublePlant.HALF, state.upper()
                        ? BlockDoublePlant.EnumBlockHalf.UPPER
                        : BlockDoublePlant.EnumBlockHalf.LOWER);
            case ROOTED_DIRT:
                return LushCaveContent.ROOTED_DIRT.getDefaultState();
            case HANGING_ROOTS:
                return (state.waterlogged() ? LushCaveContent.HANGING_ROOTS_WATERLOGGED
                    : LushCaveContent.HANGING_ROOTS).getDefaultState();
            case CAVE_VINES_BODY:
                return LushCaveVinesBlock.bodyState(state.berries());
            case CAVE_VINES_HEAD:
                return LushCaveVinesBlock.headState(state.age(), state.berries());
            case SMALL_DRIPLEAF:
                return LushCaveContent.SMALL_DRIPLEAF.getDefaultState()
                    .withProperty(LushDripleafBlocks.FACING, facing(state.facing()))
                    .withProperty(LushDripleafBlocks.HALF, state.upper()
                        ? LushDripleafBlocks.Half.UPPER
                        : LushDripleafBlocks.Half.LOWER)
                    .withProperty(LushDripleafBlocks.WATERLOGGED, state.waterlogged());
            case BIG_DRIPLEAF_STEM:
                return LushDripleafBlocks.stemState(facing(state.facing()),
                    state.waterlogged());
            case BIG_DRIPLEAF:
                return LushDripleafBlocks.headState(facing(state.facing()),
                    state.waterlogged(), LushDripleafBlocks.Tilt.NONE);
            case SPORE_BLOSSOM:
                return LushCaveContent.SPORE_BLOSSOM.getDefaultState();
            case VINE:
                return Blocks.VINE.getDefaultState().withProperty(
                    BlockVine.getPropertyFor(facing(state.facing())), true);
            case OTHER:
            default:
                throw new IllegalArgumentException("No runtime output state for " + state.encode());
        }
    }

    private IBlockState worldState(int blockX, int blockY, int blockZ) {
        if (isOutsideBuildHeight(blockY)) {
            return Blocks.AIR.getDefaultState();
        }
        return world.getBlockState(new BlockPos(blockX, blockY, blockZ));
    }

    private boolean insideFeatureChunks(int blockX, int blockZ) {
        return insideFeatureChunks(blockX, blockZ, targetChunkX, targetChunkZ);
    }

    static boolean insideFeatureChunks(int blockX, int blockZ,
            int targetChunkX, int targetChunkZ) {
        int chunkX = Math.floorDiv(blockX, 16);
        int chunkZ = Math.floorDiv(blockZ, 16);
        return Math.abs(chunkX - targetChunkX) <= FEATURE_CHUNK_RADIUS
            && Math.abs(chunkZ - targetChunkZ) <= FEATURE_CHUNK_RADIUS;
    }

    private static boolean isOutsideBuildHeight(int blockY) {
        return blockY < TerrainColumn.MIN_Y || blockY >= TerrainColumn.MAX_Y_EXCLUSIVE;
    }

    static boolean isBaseStoneOverworld(IBlockState state) {
        if (state.getBlock() == Blocks.STONE) {
            BlockStone.EnumType variant = state.getValue(BlockStone.VARIANT);
            return variant.isNatural();
        }
        return hasPath(state.getBlock(), "deepslate") || hasPath(state.getBlock(), "tuff");
    }

    static boolean isDirtTag(IBlockState state) {
        Block block = state.getBlock();
        return block == Blocks.DIRT || block == Blocks.GRASS || block == Blocks.MYCELIUM
            || block == LushCaveContent.ROOTED_DIRT || block == LushCaveContent.MOSS_BLOCK;
    }

    static boolean isTerracotta(IBlockState state) {
        Block block = state.getBlock();
        return block == Blocks.HARDENED_CLAY || block == Blocks.STAINED_HARDENED_CLAY;
    }

    private static boolean hasPath(Block block, String path) {
        ResourceLocation id = block == null ? null : block.getRegistryName();
        return id != null && "cavesnotcliffs".equals(id.getResourceDomain())
            && path.equals(id.getResourcePath());
    }

    private static EnumFacing facing(V118LushCaveFeature.Direction direction) {
        if (direction == null) {
            throw new IllegalArgumentException("Direction is required for this lush state");
        }
        return EnumFacing.valueOf(direction.name());
    }

    private static V118LushCaveFeature.Direction direction(EnumFacing facing) {
        return V118LushCaveFeature.Direction.valueOf(facing.name());
    }
}
