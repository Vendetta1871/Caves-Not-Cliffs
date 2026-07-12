package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemRecord;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Canonical Java 1.18.2 Otherside music disc required by dungeon loot. */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID)
public final class MusicDiscOthersideContent {
    public static final ResourceLocation ITEM_ID = CncRegistryIds.MUSIC_DISC_OTHERSIDE;
    public static final ResourceLocation SOUND_ID = CncRegistryIds.id("music_disc.otherside");
    public static final ResourceLocation DUNGEON_LOOT =
        CncRegistryIds.id("chests/simple_dungeon_118");

    public static final SoundEvent MUSIC_DISC_OTHERSIDE = new SoundEvent(SOUND_ID)
        .setRegistryName(SOUND_ID);

    @GameRegistry.ObjectHolder("cavesnotcliffs:music_disc_otherside")
    public static final Item OTHERSIDE = null;

    private MusicDiscOthersideContent() {
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().register(MUSIC_DISC_OTHERSIDE);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(new OthersideRecord());
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(OTHERSIDE, 0,
            new ModelResourceLocation(ITEM_ID, "inventory"));
    }

    private static final class OthersideRecord extends ItemRecord {
        private OthersideRecord() {
            super("otherside", MUSIC_DISC_OTHERSIDE);
            setUnlocalizedName("music_disc_otherside");
            setRegistryName(ITEM_ID);
        }
    }
}
