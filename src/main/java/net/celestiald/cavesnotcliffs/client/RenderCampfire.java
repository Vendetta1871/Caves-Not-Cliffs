package net.celestiald.cavesnotcliffs.client;

import net.celestiald.cavesnotcliffs.content.CampfireContent;
import net.celestiald.cavesnotcliffs.tile.TileEntityCampfire;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

/** Renders the four cooking slots with the Java 1.18.2 campfire transforms. */
public final class RenderCampfire extends TileEntitySpecialRenderer<TileEntityCampfire> {
    private static final double SLOT_OFFSET = 0.3125D;
    private static final float ITEM_SCALE = 0.375F;

    @Override
    public void render(TileEntityCampfire tile, double x, double y, double z,
            float partialTicks, int destroyStage, float alpha) {
        EnumFacing facing = EnumFacing.NORTH;
        if (tile.hasWorld()) {
            net.minecraft.block.state.IBlockState state = tile.getWorld()
                .getBlockState(tile.getPos());
            if (state.getBlock() instanceof CampfireContent.BlockCustom) {
                facing = state.getValue(CampfireContent.BlockCustom.FACING);
            }
        }
        int facingIndex = facing.getHorizontalIndex();
        for (int slot = 0; slot < tile.getSizeInventory(); ++slot) {
            ItemStack stack = tile.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            EnumFacing slotFacing = EnumFacing.getHorizontal(
                Math.floorMod(slot + facingIndex, 4));
            GlStateManager.pushMatrix();
            GlStateManager.translate(x + 0.5D, y + 0.44921875D, z + 0.5D);
            GlStateManager.rotate(-slotFacing.getHorizontalAngle(), 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.translate(-SLOT_OFFSET, -SLOT_OFFSET, 0.0D);
            GlStateManager.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
            Minecraft.getMinecraft().getRenderItem().renderItem(stack,
                ItemCameraTransforms.TransformType.FIXED);
            GlStateManager.popMatrix();
        }
    }
}
