package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.block.BlockCandle;
import net.celestiald.cavesnotcliffs.block.BlockCandleCake;
import net.celestiald.cavesnotcliffs.handler.CandleInteractionHandler;
import net.celestiald.cavesnotcliffs.item.ItemBlockCandle;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Registers seventeen public candles and their seventeen hidden candle-cake states. */
@ElementsCavesNotCliffs.ModElement.Tag
public final class CandleContent extends ElementsCavesNotCliffs.ModElement {
    public CandleContent(ElementsCavesNotCliffs elements) {
        super(elements, 360);
    }

    @Override
    public void initElements() {
        CandleSoundEvents.registerAll();
        for (CandleMechanics.Color color : CandleMechanics.colors()) {
            final CandleMechanics.Color fixed = color;
            elements.blocks.add(() -> new BlockCandle(fixed)
                    .setRegistryName(CncRegistryIds.id(fixed.getCandlePath())));
            elements.blocks.add(() -> new BlockCandleCake(fixed)
                    .setRegistryName(CncRegistryIds.id(fixed.getCandleCakePath())));
        }
        for (CandleMechanics.Color color : CandleMechanics.colors()) {
            final CandleMechanics.Color fixed = color;
            elements.items.add(() -> {
                Block block = ForgeRegistries.BLOCKS.getValue(
                        CncRegistryIds.id(fixed.getCandlePath()));
                if (!(block instanceof BlockCandle)) {
                    throw new IllegalStateException("Missing registered candle block: "
                            + fixed.getCandlePath());
                }
                return new ItemBlockCandle((BlockCandle) block)
                        .setRegistryName(CncRegistryIds.id(fixed.getCandlePath()));
            });
        }
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(CandleInteractionHandler.INSTANCE);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        for (CandleMechanics.Color color : CandleMechanics.colors()) {
            Item item = ForgeRegistries.ITEMS.getValue(
                    CncRegistryIds.id(color.getCandlePath()));
            if (item != null) {
                ModelLoader.setCustomModelResourceLocation(item, 0,
                        new ModelResourceLocation(CncRegistryIds.id(
                                color.getCandlePath()), "inventory"));
            }
        }
    }
}
