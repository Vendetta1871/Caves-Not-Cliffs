package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.block.BlockAmethystGrowth;
import net.celestiald.cavesnotcliffs.block.BlockBuddingAmethyst;
import net.celestiald.cavesnotcliffs.block.BlockTintedGlass;
import net.celestiald.cavesnotcliffs.item.ItemSpyglass;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Missing public amethyst content that was not represented by the legacy MCreator elements. */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID)
public final class AmethystContent {
    private AmethystContent() {
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(
                new BlockBuddingAmethyst()
                        .setRegistryName(id("budding_amethyst"))
                        .setCreativeTab(CreativeTabs.BUILDING_BLOCKS),
                new BlockAmethystGrowth("large_amethyst_bud", 5, 3, 4, false)
                        .setRegistryName(id("large_amethyst_bud"))
                        .setCreativeTab(CreativeTabs.DECORATIONS),
                new BlockTintedGlass()
                        .setRegistryName(id("tinted_glass"))
                        .setCreativeTab(CreativeTabs.BUILDING_BLOCKS));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        registerItemBlock(event, "budding_amethyst");
        registerItemBlock(event, "large_amethyst_bud");
        registerItemBlock(event, "tinted_glass");
        event.getRegistry().register(new ItemSpyglass()
                .setRegistryName(id("spyglass"))
                .setCreativeTab(CreativeTabs.TOOLS));
    }

    private static void registerItemBlock(RegistryEvent.Register<Item> event, String name) {
        Block block = ForgeRegistries.BLOCKS.getValue(id(name));
        if (block == null) {
            throw new IllegalStateException("Amethyst block was not registered: " + name);
        }
        event.getRegistry().register(new ItemBlock(block).setRegistryName(block.getRegistryName()));
    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation(CavesNotCliffs.MODID, name);
    }

    @SideOnly(Side.CLIENT)
    @Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID, value = Side.CLIENT)
    public static final class Models {
        private Models() {
        }

        @SubscribeEvent
        public static void registerModels(ModelRegistryEvent event) {
            Block large = ForgeRegistries.BLOCKS.getValue(id("large_amethyst_bud"));
            if (large == null) {
                throw new IllegalStateException("Large amethyst bud was not registered");
            }
            BlockAmethystGrowth.registerItemModel(large,
                    new ModelResourceLocation(id("large_amethyst_bud"), "inventory"));
            registerItemModel("budding_amethyst");
            registerItemModel("tinted_glass");
            registerItemModel("spyglass");
        }

        private static void registerItemModel(String name) {
            Item item = ForgeRegistries.ITEMS.getValue(id(name));
            if (item == null) {
                throw new IllegalStateException("Amethyst item was not registered: " + name);
            }
            ModelLoader.setCustomModelResourceLocation(item, 0,
                    new ModelResourceLocation(id(name), "inventory"));
        }
    }
}
