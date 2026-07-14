package net.celestiald.cavesnotcliffs.client;

import net.celestiald.cavesnotcliffs.entity.EntityBee;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Selects the exact normal/angry and nectar/no-nectar Java 1.18.2 textures. */
@SideOnly(Side.CLIENT)
public final class RenderBee extends RenderLiving<EntityBee.EntityCustom> {
    private static final ResourceLocation NORMAL = texture("bee");
    private static final ResourceLocation NORMAL_NECTAR = texture("bee_nectar");
    private static final ResourceLocation ANGRY = texture("bee_angry");
    private static final ResourceLocation ANGRY_NECTAR = texture("bee_angry_nectar");

    public RenderBee(RenderManager manager) {
        super(manager, new ModelBee(), 0.4F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityBee.EntityCustom bee) {
        if (bee.isAngry()) {
            return bee.hasNectar() ? ANGRY_NECTAR : ANGRY;
        }
        return bee.hasNectar() ? NORMAL_NECTAR : NORMAL;
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation("cavesnotcliffs",
                "textures/entity/bee/" + name + ".png");
    }
}
