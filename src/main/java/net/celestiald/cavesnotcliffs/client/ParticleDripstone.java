package net.celestiald.cavesnotcliffs.client;

import net.celestiald.cavesnotcliffs.content.DripstoneSoundEvents;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleDrip;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** 1.12 particle bridge that adds the Java 1.18.2 dripstone landing sounds. */
@SideOnly(Side.CLIENT)
public final class ParticleDripstone extends ParticleDrip {
    private final boolean lava;
    private boolean landed;

    private ParticleDripstone(World world, double x, double y, double z, boolean lava) {
        super(world, x, y, z, lava ? Material.LAVA : Material.WATER);
        this.lava = lava;
    }

    public static void spawn(World world, double x, double y, double z, boolean lava) {
        Minecraft.getMinecraft().effectRenderer.addEffect(
                new ParticleDripstone(world, x, y, z, lava));
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!landed && onGround) {
            landed = true;
            float volume = 0.3F + rand.nextFloat() * 0.7F;
            world.playSound(posX, posY, posZ,
                    lava ? DripstoneSoundEvents.DRIP_LAVA
                            : DripstoneSoundEvents.DRIP_WATER,
                    SoundCategory.BLOCKS, volume, 1.0F, false);
            if (lava) {
                setExpired();
            }
        }
    }
}
