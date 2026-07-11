package net.celestiald.cavesnotcliffs.client;

import net.celestiald.cavesnotcliffs.entity.EntityAxolotl;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Compact 1.12-compatible axolotl model with animated tail and paddling legs. */
@SideOnly(Side.CLIENT)
public final class ModelAxolotl extends ModelBase {
    private final ModelRenderer body;
    private final ModelRenderer head;
    private final ModelRenderer tail;
    private final ModelRenderer leftGills;
    private final ModelRenderer rightGills;
    private final ModelRenderer leftFrontLeg;
    private final ModelRenderer rightFrontLeg;
    private final ModelRenderer leftRearLeg;
    private final ModelRenderer rightRearLeg;

    public ModelAxolotl() {
        textureWidth = 64;
        textureHeight = 64;

        body = new ModelRenderer(this, 0, 11);
        body.addBox(-4.0F, -2.0F, -3.0F, 8, 4, 10);
        body.setRotationPoint(0.0F, 20.0F, 0.0F);

        head = new ModelRenderer(this, 0, 1);
        head.addBox(-4.0F, -3.0F, -5.0F, 8, 5, 5);
        head.setRotationPoint(0.0F, 20.0F, -2.0F);

        tail = new ModelRenderer(this, 2, 25);
        tail.addBox(-0.5F, -2.5F, 0.0F, 1, 5, 9);
        tail.setRotationPoint(0.0F, 20.0F, 7.0F);

        leftGills = new ModelRenderer(this, 3, 40);
        leftGills.addBox(0.0F, -3.0F, -1.0F, 4, 6, 1);
        leftGills.setRotationPoint(4.0F, 19.0F, -4.0F);
        rightGills = new ModelRenderer(this, 3, 40);
        rightGills.mirror = true;
        rightGills.addBox(-4.0F, -3.0F, -1.0F, 4, 6, 1);
        rightGills.setRotationPoint(-4.0F, 19.0F, -4.0F);

        leftFrontLeg = leg(4.0F, 21.0F, -1.0F, false);
        rightFrontLeg = leg(-4.0F, 21.0F, -1.0F, true);
        leftRearLeg = leg(4.0F, 21.0F, 5.0F, false);
        rightRearLeg = leg(-4.0F, 21.0F, 5.0F, true);
    }

    private ModelRenderer leg(float x, float y, float z, boolean mirrored) {
        ModelRenderer leg = new ModelRenderer(this, 22, 25);
        leg.mirror = mirrored;
        leg.addBox(mirrored ? -3.0F : 0.0F, 0.0F, -0.5F, 3, 1, 1);
        leg.setRotationPoint(x, y, z);
        return leg;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch, float scale) {
        setRotationAngles(limbSwing, limbSwingAmount, ageInTicks,
                netHeadYaw, headPitch, scale, entity);
        body.render(scale);
        head.render(scale);
        tail.render(scale);
        leftGills.render(scale);
        rightGills.render(scale);
        leftFrontLeg.render(scale);
        rightFrontLeg.render(scale);
        leftRearLeg.render(scale);
        rightRearLeg.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch, float scaleFactor, Entity entity) {
        float swim = entity instanceof EntityAxolotl.EntityCustom && entity.isInWater() ? 1.0F : 0.35F;
        tail.rotateAngleY = MathHelper.sin(ageInTicks * 0.22F) * 0.38F * swim;
        head.rotateAngleY = netHeadYaw * 0.017453292F * 0.35F;
        head.rotateAngleX = headPitch * 0.017453292F * 0.25F;
        float paddle = MathHelper.sin(ageInTicks * 0.35F) * 0.45F * swim;
        leftFrontLeg.rotateAngleZ = paddle;
        rightFrontLeg.rotateAngleZ = -paddle;
        leftRearLeg.rotateAngleZ = -paddle;
        rightRearLeg.rotateAngleZ = paddle;
        leftGills.rotateAngleZ = 0.15F + MathHelper.sin(ageInTicks * 0.15F) * 0.08F;
        rightGills.rotateAngleZ = -leftGills.rotateAngleZ;
    }
}
