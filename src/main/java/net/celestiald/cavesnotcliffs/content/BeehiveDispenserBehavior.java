package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.block.BlockBeehive;
import net.celestiald.cavesnotcliffs.tile.TileEntityBeehive;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Exact full-hive branch of Java 1.18.2's shears dispenser behavior. */
public final class BeehiveDispenserBehavior extends BehaviorDefaultDispenseItem {
    private boolean successful;

    @Override
    protected ItemStack dispenseStack(IBlockSource source, ItemStack shears) {
        World world = source.getWorld();
        EnumFacing facing = source.getBlockState().getValue(BlockDispenser.FACING);
        BlockPos target = source.getBlockPos().offset(facing);
        TileEntity tile = world.getTileEntity(target);
        successful = tile instanceof TileEntityBeehive
                && world.getBlockState(target).getBlock()
                    instanceof BlockBeehive.BlockCustom
                && ((TileEntityBeehive) tile).getHoneyLevel()
                    >= BeeMechanics.MAX_HONEY_LEVEL;
        if (!successful || world.isRemote) {
            return shears;
        }
        TileEntityBeehive hive = (TileEntityBeehive) tile;
        world.playSound(null, target, BeeSoundEvents.BEEHIVE_SHEAR,
                SoundCategory.BLOCKS, 1.0F, 1.0F);
        Block.spawnAsEntity(world, target,
                new ItemStack(BlockBeehive.honeycomb, 3));
        hive.setHoneyLevel(0);
        hive.releaseAllOccupants(TileEntityBeehive.BeeReleaseStatus.BEE_RELEASED);
        if (shears.attemptDamageItem(1, world.rand, null)) {
            shears.setCount(0);
        }
        return shears;
    }

    @Override
    protected void playDispenseSound(IBlockSource source) {
        source.getWorld().playEvent(successful ? 1000 : 1001,
                source.getBlockPos(), 0);
    }
}
