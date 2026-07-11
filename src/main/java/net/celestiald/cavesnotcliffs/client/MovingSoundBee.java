package net.celestiald.cavesnotcliffs.client;

import net.celestiald.cavesnotcliffs.content.BeeSoundEvents;
import net.celestiald.cavesnotcliffs.entity.EntityBee;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Java 1.12 moving-sound adaptation of 1.18.2 BeeSoundInstance. */
@SideOnly(Side.CLIENT)
public final class MovingSoundBee extends MovingSound {
    public static final float VOLUME_MIN = 0.0F;
    public static final float VOLUME_MAX = 1.2F;
    public static final float PITCH_MIN = 0.0F;
    private static final float NORMAL_MIN_PITCH = 0.7F;
    private static final float NORMAL_MAX_PITCH = 1.1F;
    private static final float BABY_MIN_PITCH = 1.1F;
    private static final float BABY_MAX_PITCH = 1.5F;
    private static final float AUDIBLE_SPEED = 0.01F;

    private final EntityBee.EntityCustom bee;
    private final boolean aggressive;

    public MovingSoundBee(EntityBee.EntityCustom bee, boolean aggressive) {
        super(aggressive ? BeeSoundEvents.BEE_LOOP_AGGRESSIVE
                : BeeSoundEvents.BEE_LOOP, SoundCategory.NEUTRAL);
        this.bee = bee;
        this.aggressive = aggressive;
        xPosF = (float) bee.posX;
        yPosF = (float) bee.posY;
        zPosF = (float) bee.posZ;
        repeat = true;
        repeatDelay = 0;
        // 1.18 can register a zero-volume tickable sound. 1.12 rejects exactly zero,
        // so use the smallest positive float only for the registration tick.
        volume = Float.MIN_VALUE;
    }

    @Override
    public void update() {
        if (bee.isDead || aggressive != bee.isAngry()) {
            donePlaying = true;
            return;
        }
        xPosF = (float) bee.posX;
        yPosF = (float) bee.posY;
        zPosF = (float) bee.posZ;
        float speed = MathHelper.sqrt(bee.motionX * bee.motionX
                + bee.motionY * bee.motionY + bee.motionZ * bee.motionZ);
        if (bee.isSilent()) {
            volume = 0.0F;
            return;
        }
        volume = volumeForSpeed(speed);
        pitch = pitchForSpeed(speed, bee.isChild());
    }

    public boolean isAggressiveLoop() {
        return aggressive;
    }

    public void stopPlaying() {
        donePlaying = true;
    }

    public static float volumeForSpeed(float speed) {
        if (speed < AUDIBLE_SPEED) {
            return 0.0F;
        }
        return lerp(MathHelper.clamp(speed, 0.0F, 0.5F),
                VOLUME_MIN, VOLUME_MAX);
    }

    public static float pitchForSpeed(float speed, boolean baby) {
        if (speed < AUDIBLE_SPEED) {
            return PITCH_MIN;
        }
        float minimum = baby ? BABY_MIN_PITCH : NORMAL_MIN_PITCH;
        float maximum = baby ? BABY_MAX_PITCH : NORMAL_MAX_PITCH;
        return lerp(MathHelper.clamp(speed, minimum, maximum),
                minimum, maximum);
    }

    private static float lerp(float delta, float start, float end) {
        return start + delta * (end - start);
    }
}
