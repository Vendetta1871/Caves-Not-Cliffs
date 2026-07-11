package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Canonical Java 1.18.2 axolotl and bucket sound identities. */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID)
public final class AxolotlSoundEvents {
    public static final SoundEvent ATTACK = sound("entity.axolotl.attack");
    public static final SoundEvent DEATH = sound("entity.axolotl.death");
    public static final SoundEvent HURT = sound("entity.axolotl.hurt");
    public static final SoundEvent IDLE_AIR = sound("entity.axolotl.idle_air");
    public static final SoundEvent IDLE_WATER = sound("entity.axolotl.idle_water");
    public static final SoundEvent SPLASH = sound("entity.axolotl.splash");
    public static final SoundEvent SWIM = sound("entity.axolotl.swim");
    public static final SoundEvent BUCKET_EMPTY = sound("item.bucket.empty_axolotl");
    public static final SoundEvent BUCKET_FILL = sound("item.bucket.fill_axolotl");

    private static final SoundEvent[] VALUES = {
        ATTACK, DEATH, HURT, IDLE_AIR, IDLE_WATER, SPLASH, SWIM,
        BUCKET_EMPTY, BUCKET_FILL
    };

    private AxolotlSoundEvents() {
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().registerAll(VALUES);
    }

    private static SoundEvent sound(String path) {
        return new SoundEvent(CncRegistryIds.id(path))
                .setRegistryName(CncRegistryIds.id(path));
    }
}
