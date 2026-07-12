package net.celestiald.cavesnotcliffs.content;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;

/** Java 1.18.2 short-grass/fern support, loss, and wheat-seed loot. */
public final class TallGrassSupportHooks {
    private static final int OVERWORLD_MIN_Y = -64;
    private static final int OVERWORLD_MAX_Y_EXCLUSIVE = 320;
    private static final ThreadLocal<Boolean> EXPLOSION_DROP = new ThreadLocal<>();

    private TallGrassSupportHooks() {
    }

    public static boolean usesJava118(World world, BlockPos pos, IBlockState state) {
        return state != null && isRepresented(state)
            && usesJava118Placement(Blocks.TALLGRASS, world, pos);
    }

    public static boolean canStay(World world, BlockPos pos) {
        return DoublePlantSupportHooks.canLowerSurvive(world, pos);
    }

    public static boolean usesJava118Placement(Block block, World world, BlockPos pos) {
        if (block != Blocks.TALLGRASS || world == null || pos == null || world.provider == null
                || world.provider.getDimension() != 0) {
            return false;
        }
        int y = pos.getY();
        return y >= OVERWORLD_MIN_Y && y < OVERWORLD_MAX_Y_EXCLUSIVE;
    }

    public static boolean canPlaceBlockAt(World world, BlockPos pos) {
        return world.getBlockState(pos).getBlock().isReplaceable(world, pos)
            && canStay(world, pos);
    }

    /** Exact bonemeal promotion to the corresponding represented double plant. */
    public static void grow(World world, BlockPos pos, IBlockState state) {
        if (!canStay(world, pos) || !world.isAirBlock(pos.up())) {
            return;
        }
        int doublePlantMetadata = Blocks.TALLGRASS.getMetaFromState(state) == 2 ? 3 : 2;
        Blocks.DOUBLE_PLANT.placeAt(world, pos,
            BlockDoublePlant.EnumPlantType.byMetadata(doublePlantMetadata), 2);
    }

    /** Handles represented persistence and removes failed support without loot. */
    public static boolean handleCheckAndDrop(World world, BlockPos pos, IBlockState state) {
        if (!usesJava118(world, pos, state)) {
            return false;
        }
        if (!canStay(world, pos)) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
        }
        return true;
    }

    /** Replaces ForgeHooks#getGrassSeed for direct drop-list callers. */
    public static boolean addDrops(NonNullList<ItemStack> drops, IBlockAccess access,
            BlockPos pos, IBlockState state, int fortune) {
        if (!(access instanceof World)) {
            return false;
        }
        World world = (World) access;
        if (!usesJava118(world, pos, state)) {
            return false;
        }
        addSeedDrops(drops, world, fortune);
        return true;
    }

    /** Preserves Explosion's can-drop result while marking only the adjacent tall-grass call. */
    public static boolean markExplosionDrop(boolean allowed, Block block) {
        if (allowed && block == Blocks.TALLGRASS) {
            EXPLOSION_DROP.set(Boolean.TRUE);
        } else {
            EXPLOSION_DROP.remove();
        }
        return allowed;
    }

    public static void endExplosionDrop() {
        EXPLOSION_DROP.remove();
    }

    /**
     * Owns the represented normal/explosion drop path so 1.12 cannot add a second outer roll.
     */
    public static boolean dropBlockAsItemWithChance(World world, BlockPos pos,
            IBlockState state, float chance, int fortune, EntityPlayer harvester) {
        boolean explosion = consumeExplosionDrop();
        if (!usesJava118(world, pos, state)
                || (!explosion && Float.compare(chance, 1.0F) != 0)) {
            return false;
        }
        if (world.isRemote || world.restoringBlockSnapshots) {
            return true;
        }

        NonNullList<ItemStack> drops = NonNullList.create();
        addSeedDrops(drops, world, fortune);
        float eventChance = ForgeEventFactory.fireBlockHarvesting(drops, world, pos,
            state, fortune, chance, false, harvester);
        for (ItemStack stack : drops) {
            if (explosion) {
                int survivors = 0;
                for (int item = 0; item < stack.getCount(); ++item) {
                    if (world.rand.nextFloat() <= eventChance) {
                        survivors++;
                    }
                }
                if (survivors > 0) {
                    ItemStack surviving = stack.copy();
                    surviving.setCount(survivors);
                    Block.spawnAsEntity(world, pos, surviving);
                }
            } else if (eventChance >= 1.0F || world.rand.nextFloat() <= eventChance) {
                Block.spawnAsEntity(world, pos, stack);
            }
        }
        return true;
    }

    private static boolean consumeExplosionDrop() {
        boolean marked = Boolean.TRUE.equals(EXPLOSION_DROP.get());
        EXPLOSION_DROP.remove();
        return marked;
    }

    private static void addSeedDrops(NonNullList<ItemStack> drops, World world,
            int fortune) {
        if (world.rand.nextFloat() < 0.125F) {
            int count = 1 + world.rand.nextInt(2 * fortune + 1);
            drops.add(new ItemStack(Items.WHEAT_SEEDS, count));
        }
    }

    private static boolean isRepresented(IBlockState state) {
        if (state.getBlock() != Blocks.TALLGRASS) {
            return false;
        }
        int metadata = Blocks.TALLGRASS.getMetaFromState(state);
        return metadata == 1 || metadata == 2;
    }
}
