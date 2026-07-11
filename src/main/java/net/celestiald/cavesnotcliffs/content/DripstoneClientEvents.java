package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.block.BlockPointedDripstone;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.Block;
import net.minecraft.world.biome.BiomeColorHelper;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;

/** Client-only water tint for the hidden pointed-dripstone storage model. */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID, value = Side.CLIENT)
public final class DripstoneClientEvents {
    private DripstoneClientEvents() {
    }

    @SubscribeEvent
    public static void registerBlockColors(ColorHandlerEvent.Block event) {
        Block companion = ForgeRegistries.BLOCKS.getValue(
                CncRegistryIds.POINTED_DRIPSTONE_WATERLOGGED);
        if (!(companion instanceof BlockPointedDripstone)) {
            throw new IllegalStateException(
                    "Waterlogged pointed-dripstone companion was not registered");
        }
        event.getBlockColors().registerBlockColorHandler((state, world, pos, tintIndex) -> {
            if (tintIndex != 0) {
                return 0xFFFFFF;
            }
            return world == null || pos == null
                    ? 0x3F76E4 : BiomeColorHelper.getWaterColorAtPos(world, pos);
        }, companion);
    }
}
