package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.content.DungeonChestContent;
import net.celestiald.cavesnotcliffs.content.MusicDiscOthersideContent;
import net.celestiald.cavesnotcliffs.worldgen.v118.TerrainColumn;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118MonsterRoomFeature;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

/** Finite Forge 1.12 adapter for the Java 1.18.2 monster-room feature. */
final class V118MonsterRoomWorldBridge implements V118MonsterRoomFeature.WorldAccess {
    private static final ResourceLocation SKELETON = new ResourceLocation("skeleton");
    private static final ResourceLocation ZOMBIE = new ResourceLocation("zombie");
    private static final ResourceLocation SPIDER = new ResourceLocation("spider");

    private final World world;
    private int targetChunkX;
    private int targetChunkZ;

    V118MonsterRoomWorldBridge(World world) {
        if (world == null) {
            throw new NullPointerException("world");
        }
        this.world = world;
    }

    int populate(int chunkX, int chunkZ) {
        targetChunkX = chunkX;
        targetChunkZ = chunkZ;
        return V118MonsterRoomFeature.decorate(this, world.getSeed(), chunkX, chunkZ);
    }

    @Override
    public boolean isAir(int blockX, int blockY, int blockZ) {
        return inside(blockX, blockY, blockZ)
            && world.getBlockState(pos(blockX, blockY, blockZ)).getBlock() == Blocks.AIR;
    }

    @Override
    public boolean isSolid(int blockX, int blockY, int blockZ) {
        return inside(blockX, blockY, blockZ)
            && world.getBlockState(pos(blockX, blockY, blockZ)).getMaterial().isSolid();
    }

    @Override
    public boolean isChest(int blockX, int blockY, int blockZ) {
        return inside(blockX, blockY, blockZ)
            && DungeonChestContent.isChest(
                    world.getBlockState(pos(blockX, blockY, blockZ)).getBlock());
    }

    @Override
    public boolean isSpawner(int blockX, int blockY, int blockZ) {
        return inside(blockX, blockY, blockZ)
            && world.getBlockState(pos(blockX, blockY, blockZ)).getBlock()
                == Blocks.MOB_SPAWNER;
    }

    @Override
    public void setCaveAir(int blockX, int blockY, int blockZ, boolean safe) {
        if (!inside(blockX, blockY, blockZ)
                || safe && featuresCannotReplace(blockX, blockY, blockZ)) {
            return;
        }
        world.setBlockState(pos(blockX, blockY, blockZ), Blocks.AIR.getDefaultState(), 2);
    }

    @Override
    public void setMossyCobblestone(int blockX, int blockY, int blockZ) {
        safeSet(blockX, blockY, blockZ, Blocks.MOSSY_COBBLESTONE.getDefaultState());
    }

    @Override
    public void setCobblestone(int blockX, int blockY, int blockZ) {
        safeSet(blockX, blockY, blockZ, Blocks.COBBLESTONE.getDefaultState());
    }

    @Override
    public void setDungeonChest(int blockX, int blockY, int blockZ, int ordinal,
            Random random) {
        BlockPos position = pos(blockX, blockY, blockZ);
        if (!inside(blockX, blockY, blockZ)
                || featuresCannotReplace(blockX, blockY, blockZ)) {
            return;
        }
        world.setBlockState(position, reorientChest(position,
                DungeonChestContent.stateForOrdinal(ordinal)), 2);
        TileEntity blockEntity = world.getTileEntity(position);
        if (blockEntity instanceof TileEntityChest) {
            ((TileEntityChest) blockEntity).setLootTable(
                MusicDiscOthersideContent.DUNGEON_LOOT, random.nextLong());
        }
    }

    @Override
    public void setSpawner(int blockX, int blockY, int blockZ,
            V118MonsterRoomFeature.MobKind mob) {
        BlockPos position = pos(blockX, blockY, blockZ);
        if (!inside(blockX, blockY, blockZ)) {
            return;
        }
        if (!featuresCannotReplace(blockX, blockY, blockZ)) {
            world.setBlockState(position, Blocks.MOB_SPAWNER.getDefaultState(), 2);
        }
        TileEntity blockEntity = world.getTileEntity(position);
        if (blockEntity instanceof TileEntityMobSpawner) {
            ((TileEntityMobSpawner) blockEntity).getSpawnerBaseLogic()
                .setEntityId(entityId(mob));
        }
    }

    private void safeSet(int blockX, int blockY, int blockZ, IBlockState state) {
        if (inside(blockX, blockY, blockZ)
                && !featuresCannotReplace(blockX, blockY, blockZ)) {
            world.setBlockState(pos(blockX, blockY, blockZ), state, 2);
        }
    }

    /** Java 1.18 {@code StructurePiece.reorient} for a newly placed chest. */
    private IBlockState reorientChest(BlockPos position, IBlockState state) {
        EnumFacing solidNeighbor = null;
        for (EnumFacing direction : EnumFacing.Plane.HORIZONTAL) {
            IBlockState neighbor = world.getBlockState(position.offset(direction));
            if (DungeonChestContent.isChest(neighbor.getBlock())) {
                return state;
            }
            if (neighbor.isOpaqueCube()) {
                if (solidNeighbor != null) {
                    return state;
                }
                solidNeighbor = direction;
            }
        }
        return solidNeighbor == null ? state : state.withProperty(
                net.minecraft.block.BlockChest.FACING, solidNeighbor.getOpposite());
    }

    private boolean featuresCannotReplace(int blockX, int blockY, int blockZ) {
        net.minecraft.block.Block block = world.getBlockState(
            pos(blockX, blockY, blockZ)).getBlock();
        return block == Blocks.BEDROCK || block == Blocks.MOB_SPAWNER
            || DungeonChestContent.isChest(block) || block == Blocks.END_PORTAL_FRAME;
    }

    private boolean inside(int blockX, int blockY, int blockZ) {
        return blockY >= TerrainColumn.MIN_Y && blockY < TerrainColumn.MAX_Y_EXCLUSIVE
            && V118LushCaveWorldBridge.insideFeatureChunks(
                blockX, blockZ, targetChunkX, targetChunkZ);
    }

    private static BlockPos pos(int blockX, int blockY, int blockZ) {
        return new BlockPos(blockX, blockY, blockZ);
    }

    private static ResourceLocation entityId(V118MonsterRoomFeature.MobKind mob) {
        switch (mob) {
            case SKELETON:
                return SKELETON;
            case ZOMBIE:
                return ZOMBIE;
            case SPIDER:
                return SPIDER;
            default:
                throw new AssertionError(mob);
        }
    }
}
