package net.celestiald.cavesnotcliffs.content;

import net.minecraft.block.BlockDispenser;
import net.minecraft.block.state.IBlockState;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Java 1.18.2 optional dispenser behavior for waxing copper. */
public final class HoneycombDispenserBehavior extends BehaviorDefaultDispenseItem {
    private boolean successful = true;

    @Override
    protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
        World world = source.getWorld();
        EnumFacing facing = source.getBlockState().getValue(BlockDispenser.FACING);
        BlockPos target = source.getBlockPos().offset(facing);
        IBlockState waxed = CopperContent.waxed(world.getBlockState(target));
        if (waxed == null) {
            successful = true;
            return super.dispenseStack(source, stack);
        }
        successful = true;
        world.setBlockState(target, waxed, 3);
        HoneyWaxingEffects.play(world, target);
        stack.shrink(1);
        return stack;
    }

    @Override
    protected void playDispenseSound(IBlockSource source) {
        source.getWorld().playEvent(successful ? 1000 : 1001,
                source.getBlockPos(), 0);
    }
}
