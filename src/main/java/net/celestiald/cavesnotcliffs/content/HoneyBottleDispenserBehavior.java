package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.block.BlockBeehive;
import net.celestiald.cavesnotcliffs.tile.TileEntityBeehive;
import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Glass-bottle dispenser bridge for full bee nests and beehives. */
public final class HoneyBottleDispenserBehavior implements IBehaviorDispenseItem {
    private final IBehaviorDispenseItem fallback;
    private final BehaviorDefaultDispenseItem eject = new BehaviorDefaultDispenseItem();

    public HoneyBottleDispenserBehavior(IBehaviorDispenseItem fallback) {
        this.fallback = fallback == null
                ? IBehaviorDispenseItem.DEFAULT_BEHAVIOR : fallback;
    }

    public static void register() {
        IBehaviorDispenseItem previous =
                BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(Items.GLASS_BOTTLE);
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(Items.GLASS_BOTTLE,
                new HoneyBottleDispenserBehavior(previous));
    }

    @Override
    public ItemStack dispense(IBlockSource source, ItemStack stack) {
        World world = source.getWorld();
        EnumFacing facing = source.getBlockState().getValue(BlockDispenser.FACING);
        BlockPos target = source.getBlockPos().offset(facing);
        TileEntity tile = world.getTileEntity(target);
        if (!(tile instanceof TileEntityBeehive)
                || !(world.getBlockState(target).getBlock()
                    instanceof BlockBeehive.BlockCustom)
                || ((TileEntityBeehive) tile).getHoneyLevel()
                    < BeeMechanics.MAX_HONEY_LEVEL) {
            return fallback.dispense(source, stack);
        }

        TileEntityBeehive hive = (TileEntityBeehive) tile;
        hive.setHoneyLevel(0);
        hive.releaseAllOccupants(TileEntityBeehive.BeeReleaseStatus.BEE_RELEASED);
        ItemStack filled = new ItemStack(HoneyContent.honeyBottle);
        stack.shrink(1);
        ItemStack result = stack;
        if (stack.isEmpty()) {
            result = filled;
        } else {
            TileEntityDispenser dispenser = source.getBlockTileEntity();
            if (dispenser.addItemStack(filled) < 0) {
                eject.dispense(source, filled);
            }
        }
        world.playEvent(1000, source.getBlockPos(), 0);
        world.playEvent(2000, source.getBlockPos(),
                facing.getFrontOffsetX() + 1
                    + (facing.getFrontOffsetZ() + 1) * 3);
        return result;
    }
}
