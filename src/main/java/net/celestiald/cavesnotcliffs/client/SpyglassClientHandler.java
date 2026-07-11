package net.celestiald.cavesnotcliffs.client;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.item.ItemSpyglass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/** First-person zoom and circular overlay matching the 1.18 spyglass presentation. */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID, value = Side.CLIENT)
public final class SpyglassClientHandler {
    private static final ResourceLocation SCOPE = new ResourceLocation(
            CavesNotCliffs.MODID, "textures/misc/spyglass_scope.png");

    private SpyglassClientHandler() {
    }

    @SubscribeEvent
    public static void updateFov(FOVUpdateEvent event) {
        if (isScoping(event.getEntity())) {
            event.setNewfov(event.getFov() * ItemSpyglass.ZOOM_FOV_MODIFIER);
        }
    }

    @SubscribeEvent
    public static void hideFirstPersonHand(RenderHandEvent event) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (isScoping(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void renderScope(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.HELMET) {
            return;
        }
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (!isScoping(player) || Minecraft.getMinecraft().gameSettings.thirdPersonView != 0) {
            return;
        }

        // Scope and carved-pumpkin overlays are mutually exclusive in Java 1.18.2.
        event.setCanceled(true);

        ScaledResolution resolution = event.getResolution();
        int width = resolution.getScaledWidth();
        int height = resolution.getScaledHeight();
        int elapsed = player.getItemInUseMaxCount();
        float scopeScale = (float) (1.125D - 0.625D * Math.pow(0.5D, elapsed));
        int diameter = Math.round(Math.min(width, height) * scopeScale);
        int left = (width - diameter) / 2;
        int top = (height - diameter) / 2;

        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(SCOPE);
        Gui.drawScaledCustomSizeModalRect(left, top, 0.0F, 0.0F,
                256, 256, diameter, diameter, 256.0F, 256.0F);

        Gui.drawRect(0, 0, left, height, 0xFF000000);
        Gui.drawRect(left + diameter, 0, width, height, 0xFF000000);
        Gui.drawRect(left, 0, left + diameter, top, 0xFF000000);
        Gui.drawRect(left, top + diameter, left + diameter, height, 0xFF000000);
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
    }

    private static boolean isScoping(EntityPlayer player) {
        return player != null
                && player.isHandActive()
                && !player.getActiveItemStack().isEmpty()
                && player.getActiveItemStack().getItem() instanceof ItemSpyglass;
    }
}
