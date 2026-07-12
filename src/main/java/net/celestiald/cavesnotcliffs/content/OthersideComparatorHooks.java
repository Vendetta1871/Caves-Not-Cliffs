package net.celestiald.cavesnotcliffs.content;

import net.minecraft.block.BlockJukebox;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Supplies the explicit Java 1.18.2 comparator value absent from 1.12's numeric-ID formula. */
public final class OthersideComparatorHooks {
    private static final int NOT_OTHERSIDE = -1;
    private static final int OTHERSIDE_ANALOG_OUTPUT = 14;

    private OthersideComparatorHooks() {
    }

    public static int comparatorOverride(World world, BlockPos pos) {
        TileEntity blockEntity = world.getTileEntity(pos);
        if (!(blockEntity instanceof BlockJukebox.TileEntityJukebox)) {
            return NOT_OTHERSIDE;
        }
        ItemStack record = ((BlockJukebox.TileEntityJukebox) blockEntity).getRecord();
        return !record.isEmpty() && MusicDiscOthersideContent.ITEM_ID.equals(
            record.getItem().getRegistryName()) ? OTHERSIDE_ANALOG_OUTPUT : NOT_OTHERSIDE;
    }
}
