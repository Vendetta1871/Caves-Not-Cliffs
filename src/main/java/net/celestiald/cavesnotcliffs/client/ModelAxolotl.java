package net.celestiald.cavesnotcliffs.client;

import net.celestiald.cavesnotcliffs.entity.EntityAxolotl;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Java 1.12 rendering adaptation of the exact Java 1.18.2 axolotl model geometry. */
@SideOnly(Side.CLIENT)
public final class ModelAxolotl extends ModelBase {
    private final ModelRenderer body;
    private final ModelRenderer head;
    private final ModelRenderer tail;
    private final ModelRenderer topGills;
    private final ModelRenderer leftGills;
    private final ModelRenderer rightGills;
    private final ModelRenderer leftFrontLeg;
    private final ModelRenderer rightFrontLeg;
    private final ModelRenderer leftHindLeg;
    private final ModelRenderer rightHindLeg;

    public ModelAxolotl() {
        textureWidth = 64;
        textureHeight = 64;

        body = new ModelRenderer(this, 0, 11);
        body.addBox(-4.0F, -2.0F, -9.0F, 8, 4, 10);
        body.setTextureOffset(2, 17).addBox(0.0F, -3.0F, -8.0F, 0, 5, 9);
        body.setRotationPoint(0.0F, 20.0F, 5.0F);

        head = new ModelRenderer(this, 0, 1);
        head.addBox(-4.0F, -3.0F, -5.0F, 8, 5, 5, 0.001F);
        head.setRotationPoint(0.0F, 20.0F, -4.0F);

        topGills = new ModelRenderer(this, 3, 37);
        topGills.addBox(-4.0F, -3.0F, 0.0F, 8, 3, 0, 0.001F);
        topGills.setRotationPoint(0.0F, 17.0F, -5.0F);
        leftGills = new ModelRenderer(this, 0, 40);
        leftGills.addBox(-3.0F, -5.0F, 0.0F, 3, 7, 0, 0.001F);
        leftGills.setRotationPoint(-4.0F, 20.0F, -5.0F);
        rightGills = new ModelRenderer(this, 11, 40);
        rightGills.addBox(0.0F, -5.0F, 0.0F, 3, 7, 0, 0.001F);
        rightGills.setRotationPoint(4.0F, 20.0F, -5.0F);

        leftHindLeg = leg(3.5F, 21.0F, 4.0F, false);
        rightHindLeg = leg(-3.5F, 21.0F, 4.0F, true);
        leftFrontLeg = leg(3.5F, 21.0F, -3.0F, false);
        rightFrontLeg = leg(-3.5F, 21.0F, -3.0F, true);

        tail = new ModelRenderer(this, 2, 19);
        tail.addBox(0.0F, -3.0F, 0.0F, 0, 5, 12);
        tail.setRotationPoint(0.0F, 20.0F, 6.0F);
    }

    private ModelRenderer leg(float x, float y, float z, boolean right) {
        ModelRenderer leg = new ModelRenderer(this, 2, 13);
        leg.addBox(right ? -2.0F : -1.0F, 0.0F, 0.0F, 3, 5, 0, 0.001F);
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
        topGills.render(scale);
        leftGills.render(scale);
        rightGills.render(scale);
        leftFrontLeg.render(scale);
        rightFrontLeg.render(scale);
        leftHindLeg.render(scale);
        rightHindLeg.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch, float scaleFactor, Entity entity) {
        resetPose();
        EntityAxolotl.EntityCustom axolotl = (EntityAxolotl.EntityCustom) entity;
        if (axolotl.isPlayingDead()) {
            playDead(netHeadYaw);
        } else if (axolotl.isInWater()) {
            double speedSq = entity.motionX * entity.motionX + entity.motionY * entity.motionY
                    + entity.motionZ * entity.motionZ;
            if (speedSq > 1.0E-7D) {
                swimming(ageInTicks, headPitch);
            } else {
                hovering(ageInTicks);
            }
        } else if (limbSwingAmount > 0.01F) {
            crawling(ageInTicks, netHeadYaw);
        } else {
            lyingStill(ageInTicks, netHeadYaw);
        }
    }

    private void resetPose() {
        body.setRotationPoint(0.0F, 20.0F, 5.0F);
        rotate(body, 0.0F, 0.0F, 0.0F);
        rotate(head, 0.0F, 0.0F, 0.0F);
        rotate(tail, 0.0F, 0.0F, 0.0F);
        rotate(topGills, 0.0F, 0.0F, 0.0F);
        rotate(leftGills, 0.0F, 0.0F, 0.0F);
        rotate(rightGills, 0.0F, 0.0F, 0.0F);
        rotate(leftFrontLeg, 0.0F, 0.0F, 0.0F);
        rotate(rightFrontLeg, 0.0F, 0.0F, 0.0F);
        rotate(leftHindLeg, 0.0F, 0.0F, 0.0F);
        rotate(rightHindLeg, 0.0F, 0.0F, 0.0F);
    }

    private void lyingStill(float age, float yaw) {
        float phase = age * 0.09F;
        float sine = MathHelper.sin(phase);
        float cosine = MathHelper.cos(phase);
        float curve = sine * sine - 2.0F * sine;
        float gillCurve = cosine * cosine - 3.0F * sine;
        head.rotateAngleX = -0.09F * curve;
        head.rotateAngleZ = -0.2F;
        tail.rotateAngleY = -0.1F + 0.1F * curve;
        topGills.rotateAngleX = 0.6F + 0.05F * gillCurve;
        leftGills.rotateAngleY = -topGills.rotateAngleX;
        rightGills.rotateAngleY = topGills.rotateAngleX;
        rotate(leftHindLeg, 1.1F, 1.0F, 0.0F);
        rotate(leftFrontLeg, 0.8F, 2.3F, -0.5F);
        mirrorLegs();
        body.rotateAngleY = yaw * 0.017453292F;
    }

    private void crawling(float age, float yaw) {
        float phase = age * 0.11F;
        float cosine = MathHelper.cos(phase);
        float curve = (cosine * cosine - 2.0F * cosine) / 5.0F;
        float sweep = 0.7F * cosine;
        head.rotateAngleY = 0.09F * cosine;
        tail.rotateAngleY = head.rotateAngleY;
        topGills.rotateAngleX = 0.6F
                - 0.08F * (cosine * cosine + 2.0F * MathHelper.sin(phase));
        leftGills.rotateAngleY = -topGills.rotateAngleX;
        rightGills.rotateAngleY = topGills.rotateAngleX;
        rotate(leftHindLeg, 0.9424779F, 1.5F - curve, -0.1F);
        rotate(leftFrontLeg, 1.0995574F, 1.5707964F - sweep, 0.0F);
        rotate(rightHindLeg, leftHindLeg.rotateAngleX, -1.0F - curve, 0.0F);
        rotate(rightFrontLeg, leftFrontLeg.rotateAngleX, -1.5707964F - sweep, 0.0F);
        body.rotateAngleY = yaw * 0.017453292F;
    }

    private void hovering(float age) {
        float phase = age * 0.075F;
        float cosine = MathHelper.cos(phase);
        body.rotateAngleX = -0.15F + 0.075F * cosine;
        body.rotationPointY = 20.0F - MathHelper.sin(phase) * 0.15F;
        head.rotateAngleX = -body.rotateAngleX;
        topGills.rotateAngleX = 0.2F * cosine;
        leftGills.rotateAngleY = -0.3F * cosine - 0.19F;
        rightGills.rotateAngleY = -leftGills.rotateAngleY;
        rotate(leftHindLeg, 2.3561945F - cosine * 0.11F, 0.47123894F, 1.7278761F);
        rotate(leftFrontLeg, 0.7853982F - cosine * 0.2F, 2.042035F, 0.0F);
        mirrorLegs();
        tail.rotateAngleY = 0.5F * cosine;
    }

    private void swimming(float age, float pitch) {
        float phase = age * 0.33F;
        float sine = MathHelper.sin(phase);
        float cosine = MathHelper.cos(phase);
        body.rotateAngleX = pitch * 0.017453292F + 0.13F * sine;
        head.rotateAngleX = -0.234F * sine;
        body.rotationPointY = 20.0F - 0.45F * cosine;
        topGills.rotateAngleX = -0.5F * sine - 0.8F;
        leftGills.rotateAngleY = 0.3F * sine + 0.9F;
        rightGills.rotateAngleY = -leftGills.rotateAngleY;
        tail.rotateAngleY = 0.3F * MathHelper.cos(phase * 0.9F);
        rotate(leftHindLeg, 1.8849558F, -0.4F * sine, 1.5707964F);
        rotate(leftFrontLeg, 1.8849558F, -0.2F * cosine - 0.1F, 1.5707964F);
        mirrorLegs();
    }

    private void playDead(float yaw) {
        rotate(leftHindLeg, 1.4137167F, 1.0995574F, 0.7853982F);
        rotate(leftFrontLeg, 0.7853982F, 2.042035F, 0.0F);
        body.rotateAngleX = -0.15F;
        body.rotateAngleZ = 0.35F;
        body.rotateAngleY = yaw * 0.017453292F;
        mirrorLegs();
    }

    private void mirrorLegs() {
        rotate(rightHindLeg, leftHindLeg.rotateAngleX,
                -leftHindLeg.rotateAngleY, -leftHindLeg.rotateAngleZ);
        rotate(rightFrontLeg, leftFrontLeg.rotateAngleX,
                -leftFrontLeg.rotateAngleY, -leftFrontLeg.rotateAngleZ);
    }

    private static void rotate(ModelRenderer part, float x, float y, float z) {
        part.rotateAngleX = x;
        part.rotateAngleY = y;
        part.rotateAngleZ = z;
    }
}
