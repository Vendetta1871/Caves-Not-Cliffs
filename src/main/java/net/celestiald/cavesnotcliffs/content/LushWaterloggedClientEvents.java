package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.minecraft.block.Block;
import net.minecraft.world.biome.BiomeColorHelper;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/** Water tint for the translucent overlay rendered by every retained-water lush state. */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID, value = Side.CLIENT)
public final class LushWaterloggedClientEvents {
    private LushWaterloggedClientEvents() {
    }

    @SubscribeEvent
    public static void registerBlockColors(ColorHandlerEvent.Block event) {
        Block[] waterCapable = {
                LushCaveContent.SMALL_DRIPLEAF,
                LushCaveContent.BIG_DRIPLEAF_WATERLOGGED,
                LushCaveContent.BIG_DRIPLEAF_STEM,
                LushCaveContent.HANGING_ROOTS_WATERLOGGED
        };
        for (Block block : waterCapable) {
            if (block == null) {
                throw new IllegalStateException(
                        "A retained-water lush block was not registered before color setup");
            }
        }
        event.getBlockColors().registerBlockColorHandler((state, world, pos, tintIndex) -> {
            if (tintIndex != 0) {
                return 0xFFFFFF;
            }
            return world == null || pos == null
                    ? 0x3F76E4 : BiomeColorHelper.getWaterColorAtPos(world, pos);
        }, waterCapable);
    }
}
