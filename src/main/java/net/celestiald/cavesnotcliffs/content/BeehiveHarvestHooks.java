package net.celestiald.cavesnotcliffs.content;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Integration seam for honey bottles and later dispenser/copper-waxing work. */
public final class BeehiveHarvestHooks {
    private static final BottleHarvester NONE =
            (world, pos, state, player, hand, stack) -> false;
    private static volatile BottleHarvester bottleHarvester = NONE;

    private BeehiveHarvestHooks() {
    }

    public static boolean tryBottleHarvest(World world, BlockPos pos, IBlockState state,
            EntityPlayer player, EnumHand hand, ItemStack stack) {
        return bottleHarvester.harvest(world, pos, state, player, hand, stack);
    }

    public static void registerBottleHarvester(BottleHarvester harvester) {
        if (harvester == null) {
            throw new NullPointerException("harvester");
        }
        bottleHarvester = harvester;
    }

    public static void clear() {
        bottleHarvester = NONE;
    }

    public interface BottleHarvester {
        boolean harvest(World world, BlockPos pos, IBlockState state,
                EntityPlayer player, EnumHand hand, ItemStack stack);
    }
}
