package net.celestiald.cavesnotcliffs.client;

import net.celestiald.cavesnotcliffs.entity.EntityAxolotl;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Lightweight 1.12 renderer so the backported axolotl is visible instead of an unrendered entity. */
@SideOnly(Side.CLIENT)
public final class RenderAxolotl extends RenderLiving<EntityAxolotl.EntityCustom> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("cavesnotcliffs", "textures/entity/axolotl_lucy.png");

    public RenderAxolotl(RenderManager manager) {
        super(manager, new ModelAxolotl(), 0.30F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityAxolotl.EntityCustom entity) {
        return TEXTURE;
    }
}
