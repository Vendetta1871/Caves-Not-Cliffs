package net.celestiald.cavesnotcliffs.client;

import net.celestiald.cavesnotcliffs.entity.EntityBee;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Java 1.12 rendering adaptation of Java 1.18.2's exact 64x64 bee geometry. */
@SideOnly(Side.CLIENT)
public final class ModelBee extends ModelBase {
    private final ModelRenderer body;
    private final ModelRenderer stinger;
    private final ModelRenderer leftAntenna;
    private final ModelRenderer rightAntenna;
    private final ModelRenderer rightWing;
    private final ModelRenderer leftWing;
    private final ModelRenderer frontLegs;
    private final ModelRenderer middleLegs;
    private final ModelRenderer backLegs;

    public ModelBee() {
        textureWidth = 64;
        textureHeight = 64;

        body = new ModelRenderer(this, 0, 0);
        body.addBox(-3.5F, -4.0F, -5.0F, 7, 7, 10);
        body.setRotationPoint(0.0F, 19.0F, 0.0F);

        stinger = new ModelRenderer(this, 26, 7);
        stinger.addBox(0.0F, -1.0F, 5.0F, 0, 1, 2);
        stinger.setRotationPoint(0.0F, 19.0F, 0.0F);

        leftAntenna = new ModelRenderer(this, 2, 0);
        leftAntenna.addBox(1.5F, -2.0F, -3.0F, 1, 2, 3);
        leftAntenna.setRotationPoint(0.0F, 17.0F, -5.0F);
        rightAntenna = new ModelRenderer(this, 2, 3);
        rightAntenna.addBox(-2.5F, -2.0F, -3.0F, 1, 2, 3);
        rightAntenna.setRotationPoint(0.0F, 17.0F, -5.0F);

        rightWing = new ModelRenderer(this, 0, 18);
        rightWing.addBox(-9.0F, 0.0F, 0.0F, 9, 0, 6, 0.001F);
        rightWing.setRotationPoint(-1.5F, 15.0F, -3.0F);
        leftWing = new ModelRenderer(this, 0, 18);
        leftWing.mirror = true;
        leftWing.addBox(0.0F, 0.0F, 0.0F, 9, 0, 6, 0.001F);
        leftWing.setRotationPoint(1.5F, 15.0F, -3.0F);

        frontLegs = leg(26, 1, -2.0F);
        middleLegs = leg(26, 3, 0.0F);
        backLegs = leg(26, 5, 2.0F);
    }

    private ModelRenderer leg(int textureX, int textureY, float z) {
        ModelRenderer legs = new ModelRenderer(this, textureX, textureY);
        legs.addBox(-5.0F, 0.0F, 0.0F, 7, 2, 0);
        legs.setRotationPoint(1.5F, 22.0F, z);
        return legs;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        setRotationAngles(limbSwing, limbSwingAmount, ageInTicks,
                netHeadYaw, headPitch, scale, entity);
        body.render(scale);
        stinger.render(scale);
        leftAntenna.render(scale);
        rightAntenna.render(scale);
        rightWing.render(scale);
        leftWing.render(scale);
        frontLegs.render(scale);
        middleLegs.render(scale);
        backLegs.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch,
            float scaleFactor, Entity entity) {
        EntityBee.EntityCustom bee = (EntityBee.EntityCustom) entity;
        resetPose();
        stinger.showModel = !bee.hasStung();
        boolean resting = bee.onGround
                && bee.motionX * bee.motionX + bee.motionY * bee.motionY
                    + bee.motionZ * bee.motionZ < 1.0E-7D;
        if (resting) {
            rightWing.rotateAngleY = -0.2618F;
            leftWing.rotateAngleY = 0.2618F;
        } else {
            float flap = ageInTicks * 120.32113F * 0.017453292F;
            rightWing.rotateAngleZ = MathHelper.cos(flap) * (float) Math.PI * 0.15F;
            leftWing.rotateAngleZ = -rightWing.rotateAngleZ;
            frontLegs.rotateAngleX = 0.7853982F;
            middleLegs.rotateAngleX = 0.7853982F;
            backLegs.rotateAngleX = 0.7853982F;
        }
        if (!bee.isAngry() && !resting) {
            float bob = MathHelper.cos(ageInTicks * 0.18F);
            body.rotateAngleX = 0.1F + bob * (float) Math.PI * 0.025F;
            leftAntenna.rotateAngleX = bob * (float) Math.PI * 0.03F;
            rightAntenna.rotateAngleX = leftAntenna.rotateAngleX;
            frontLegs.rotateAngleX = -bob * (float) Math.PI * 0.1F + 0.3926991F;
            backLegs.rotateAngleX = -bob * (float) Math.PI * 0.05F + 0.7853982F;
            float y = 19.0F - bob * 0.9F;
            setRootY(y);
        }
        if (bee.isRolling()) {
            body.rotateAngleX = 3.0915928F;
        }
    }

    private void resetPose() {
        setRootY(19.0F);
        rotate(body, 0.0F, 0.0F, 0.0F);
        rotate(stinger, 0.0F, 0.0F, 0.0F);
        rotate(leftAntenna, 0.0F, 0.0F, 0.0F);
        rotate(rightAntenna, 0.0F, 0.0F, 0.0F);
        rotate(rightWing, 0.0F, 0.0F, 0.0F);
        rotate(leftWing, 0.0F, 0.0F, 0.0F);
        rotate(frontLegs, 0.0F, 0.0F, 0.0F);
        rotate(middleLegs, 0.0F, 0.0F, 0.0F);
        rotate(backLegs, 0.0F, 0.0F, 0.0F);
    }

    private void setRootY(float y) {
        body.rotationPointY = y;
        stinger.rotationPointY = y;
        leftAntenna.rotationPointY = y - 2.0F;
        rightAntenna.rotationPointY = y - 2.0F;
        rightWing.rotationPointY = y - 4.0F;
        leftWing.rotationPointY = y - 4.0F;
        frontLegs.rotationPointY = y + 3.0F;
        middleLegs.rotationPointY = y + 3.0F;
        backLegs.rotationPointY = y + 3.0F;
    }

    private static void rotate(ModelRenderer part, float x, float y, float z) {
        part.rotateAngleX = x;
        part.rotateAngleY = y;
        part.rotateAngleZ = z;
    }
}
