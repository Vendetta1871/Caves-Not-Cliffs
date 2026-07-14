package net.celestiald.cavesnotcliffs.client;

import net.celestiald.cavesnotcliffs.content.AxolotlMechanics;
import net.celestiald.cavesnotcliffs.entity.EntityAxolotl;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Variant-aware renderer using the five canonical Java 1.18.2 textures. */
@SideOnly(Side.CLIENT)
public final class RenderAxolotl extends RenderLiving<EntityAxolotl.EntityCustom> {
    private static final ResourceLocation[] TEXTURES = createTextures();

    public RenderAxolotl(RenderManager manager) {
        super(manager, new ModelAxolotl(), 0.5F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityAxolotl.EntityCustom entity) {
        return TEXTURES[entity.getVariant().id()];
    }

    private static ResourceLocation[] createTextures() {
        ResourceLocation[] values = new ResourceLocation[AxolotlMechanics.Variant.values().length];
        for (AxolotlMechanics.Variant variant : AxolotlMechanics.Variant.values()) {
            values[variant.id()] = new ResourceLocation("cavesnotcliffs",
                    "textures/entity/axolotl/axolotl_"
                        + variant.serializedName() + ".png");
        }
        return values;
    }
}
