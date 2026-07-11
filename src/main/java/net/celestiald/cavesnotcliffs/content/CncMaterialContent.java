package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockWall;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemSlab;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Canonical material items and the deepslate building family introduced by v2. */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID)
public final class CncMaterialContent {
    private static final List<String> ITEM_NAMES = Arrays.asList(
            "raw_copper", "copper_ingot", "raw_iron", "raw_gold", "amethyst_shard");

    private static final List<String> SOLID_BLOCK_NAMES = Arrays.asList(
            "cobbled_deepslate", "polished_deepslate", "deepslate_bricks",
            "cracked_deepslate_bricks", "deepslate_tiles", "cracked_deepslate_tiles",
            "chiseled_deepslate", "raw_copper_block", "raw_iron_block", "raw_gold_block");

    private static final Map<String, String> DEEPSLATE_SHAPES = createShapeNames();

    private static final Map<String, Block> CREATED_BLOCKS = new LinkedHashMap<>();

    private CncMaterialContent() {
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        register(event, rock("cobbled_deepslate", 3.5F, 6.0F, 0));
        register(event, rock("polished_deepslate", 3.5F, 6.0F, 0));
        register(event, rock("deepslate_bricks", 3.5F, 6.0F, 0));
        register(event, rock("cracked_deepslate_bricks", 3.5F, 6.0F, 0));
        register(event, rock("deepslate_tiles", 3.5F, 6.0F, 0));
        register(event, rock("cracked_deepslate_tiles", 3.5F, 6.0F, 0));
        register(event, rock("chiseled_deepslate", 3.5F, 6.0F, 0));
        register(event, createRawCopperBlock());
        register(event, rawOreBlock("raw_iron_block", CncBlockProperties.RAW_IRON, 1));
        register(event, rawOreBlock("raw_gold_block", MapColor.GOLD, 2));

        for (Map.Entry<String, String> shape : DEEPSLATE_SHAPES.entrySet()) {
            Block base = CREATED_BLOCKS.get(shape.getKey());
            registerShapes(event, shape.getValue(), base);
        }
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        for (String name : ITEM_NAMES) {
            event.getRegistry().register(new Item()
                    .setRegistryName(id(name))
                    .setUnlocalizedName(name)
                    .setCreativeTab(CreativeTabs.MATERIALS));
        }

        for (String name : SOLID_BLOCK_NAMES) {
            registerItemBlock(event, name);
        }

        for (String shapeName : DEEPSLATE_SHAPES.values()) {
            registerSlabItem(event, shapeName + "_slab", shapeName + "_slab_double");
            registerItemBlock(event, shapeName + "_stairs");
            registerItemBlock(event, shapeName + "_wall");
        }
    }

    public static void registerSmelting() {
        GameRegistry.addSmelting(new ItemStack(block("copper_ore")),
                new ItemStack(item("copper_ingot")), 0.7F);
        addSmelting("raw_copper", "copper_ingot", 0.7F);
        addSmelting("raw_iron", Items.IRON_INGOT, 0.7F);
        addSmelting("raw_gold", Items.GOLD_INGOT, 1.0F);
        addBlockSmelting("cobbled_deepslate", block("deepslate"), 0.1F);
        addBlockSmelting("deepslate_bricks", block("cracked_deepslate_bricks"), 0.1F);
        addBlockSmelting("deepslate_tiles", block("cracked_deepslate_tiles"), 0.1F);
    }

    public static Block block(String name) {
        return ForgeRegistries.BLOCKS.getValue(id(name));
    }

    public static Item item(String name) {
        return ForgeRegistries.ITEMS.getValue(id(name));
    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation(CavesNotCliffs.MODID, name);
    }

    private static Map<String, String> createShapeNames() {
        Map<String, String> names = new LinkedHashMap<>();
        names.put("cobbled_deepslate", "cobbled_deepslate");
        names.put("polished_deepslate", "polished_deepslate");
        names.put("deepslate_bricks", "deepslate_brick");
        names.put("deepslate_tiles", "deepslate_tile");
        return names;
    }

    private static Block rock(String name, float hardness, float resistance, int harvestLevel) {
        return new BasicMaterialBlock(name, Material.ROCK, CncBlockProperties.DEEPSLATE,
                DeepslateSoundEvents.forBuildingBlock(name),
                hardness, resistance, harvestLevel);
    }

    static Block createRawCopperBlock() {
        return rawOreBlock("raw_copper_block", MapColor.ADOBE, 1);
    }

    private static Block rawOreBlock(String name, MapColor color, int harvestLevel) {
        return new BasicMaterialBlock(name, Material.ROCK, color, SoundType.STONE,
                5.0F, 6.0F, harvestLevel);
    }

    private static void register(RegistryEvent.Register<Block> event, Block block) {
        CREATED_BLOCKS.put(block.getRegistryName().getResourcePath(), block);
        event.getRegistry().register(block);
    }

    private static void registerShapes(RegistryEvent.Register<Block> event,
            String baseName, Block base) {
        String slabName = baseName + "_slab";
        SoundType sound = base.getSoundType();
        register(event, new MaterialSlab(slabName, sound).setRegistryName(id(slabName)));
        register(event, new MaterialDoubleSlab(slabName, sound)
                .setRegistryName(id(slabName + "_double")));
        register(event, new MaterialStairs(baseName + "_stairs", base.getDefaultState()));
        register(event, new MaterialWall(baseName + "_wall", base));
    }

    private static void registerItemBlock(RegistryEvent.Register<Item> event, String name) {
        Block block = block(name);
        if (block == null) {
            throw new IllegalStateException("Block was not registered before its item: " + name);
        }
        event.getRegistry().register(new ItemBlock(block).setRegistryName(block.getRegistryName()));
    }

    private static void registerSlabItem(RegistryEvent.Register<Item> event,
            String slabName, String doubleName) {
        Block single = block(slabName);
        Block doubled = block(doubleName);
        if (!(single instanceof BlockSlab) || !(doubled instanceof BlockSlab)) {
            throw new IllegalStateException("Slab pair was not registered: " + slabName);
        }
        event.getRegistry().register(new ItemSlab(single, (BlockSlab) single, (BlockSlab) doubled)
                .setRegistryName(single.getRegistryName()));
    }

    private static void addSmelting(String inputName, String outputName, float experience) {
        GameRegistry.addSmelting(new ItemStack(item(inputName)),
                new ItemStack(item(outputName)), experience);
    }

    private static void addSmelting(String inputName, Item output, float experience) {
        GameRegistry.addSmelting(new ItemStack(item(inputName)),
                new ItemStack(output), experience);
    }

    private static void addBlockSmelting(String inputName, Block output, float experience) {
        GameRegistry.addSmelting(new ItemStack(block(inputName)), new ItemStack(output), experience);
    }

    private static class BasicMaterialBlock extends Block {
        BasicMaterialBlock(String name, Material material, MapColor color, SoundType sound,
                float hardness, float resistance, int harvestLevel) {
            super(material, color);
            setRegistryName(id(name));
            setUnlocalizedName(name);
            setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
            setSoundType(sound);
            setHardness(hardness);
            setResistance(CncBlockProperties.legacyResistance(resistance));
            setHarvestLevel("pickaxe", harvestLevel);
        }
    }

    private static final class MaterialStairs extends BlockStairs {
        MaterialStairs(String name, IBlockState state) {
            super(state);
            setRegistryName(id(name));
            setUnlocalizedName(name);
            setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
            setSoundType(state.getBlock().getSoundType());
            setHardness(3.5F);
            setResistance(CncBlockProperties.legacyResistance(6.0F));
            setHarvestLevel("pickaxe", 0);
        }
    }

    private static final class MaterialWall extends BlockWall {
        MaterialWall(String name, Block modelBlock) {
            super(modelBlock);
            setRegistryName(id(name));
            setUnlocalizedName(name);
            setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
            setSoundType(modelBlock.getSoundType());
            setHardness(3.5F);
            setResistance(CncBlockProperties.legacyResistance(6.0F));
            setHarvestLevel("pickaxe", 0);
        }
    }

    private static class MaterialSlab extends BlockSlab {
        private static final PropertyEnum<Variant> VARIANT =
                PropertyEnum.create("variant", Variant.class);

        MaterialSlab(String name, SoundType sound) {
            super(Material.ROCK, CncBlockProperties.DEEPSLATE);
            setUnlocalizedName(name);
            setCreativeTab(isDouble() ? null : CreativeTabs.BUILDING_BLOCKS);
            setSoundType(sound);
            setHardness(3.5F);
            setResistance(CncBlockProperties.legacyResistance(6.0F));
            setHarvestLevel("pickaxe", 0);
            IBlockState state = blockState.getBaseState().withProperty(VARIANT, Variant.DEFAULT);
            if (!isDouble()) {
                state = state.withProperty(HALF, EnumBlockHalf.BOTTOM);
            }
            setDefaultState(state);
            useNeighborBrightness = !isDouble();
        }

        @Override
        public boolean isDouble() {
            return false;
        }

        @Override
        public IProperty<?> getVariantProperty() {
            return VARIANT;
        }

        @Override
        public Comparable<?> getTypeForItem(ItemStack stack) {
            return Variant.DEFAULT;
        }

        @Override
        public Item getItemDropped(IBlockState state, Random rand, int fortune) {
            String path = getRegistryName().getResourcePath().replace("_double", "");
            return Item.getItemFromBlock(block(path));
        }

        @Override
        public String getUnlocalizedName(int meta) {
            return super.getUnlocalizedName();
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return isDouble()
                    ? new BlockStateContainer(this, VARIANT)
                    : new BlockStateContainer(this, HALF, VARIANT);
        }

        @Override
        public IBlockState getStateFromMeta(int meta) {
            return isDouble() ? getDefaultState()
                    : getDefaultState().withProperty(HALF,
                            (meta & 8) == 0 ? EnumBlockHalf.BOTTOM : EnumBlockHalf.TOP);
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            return !isDouble() && state.getValue(HALF) == EnumBlockHalf.TOP ? 8 : 0;
        }

        private enum Variant implements IStringSerializable {
            DEFAULT;

            @Override
            public String getName() {
                return "default";
            }
        }
    }

    private static final class MaterialDoubleSlab extends MaterialSlab {
        MaterialDoubleSlab(String name, SoundType sound) {
            super(name, sound);
        }

        @Override
        public boolean isDouble() {
            return true;
        }
    }

    @SideOnly(Side.CLIENT)
    @Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID, value = Side.CLIENT)
    public static final class Models {
        private Models() {
        }

        @SubscribeEvent
        public static void registerModels(ModelRegistryEvent event) {
            for (String name : ITEM_NAMES) {
                registerItemModel(item(name), name);
            }
            for (String name : SOLID_BLOCK_NAMES) {
                registerItemModel(Item.getItemFromBlock(block(name)), name);
            }
            for (String shapeName : DEEPSLATE_SHAPES.values()) {
                registerItemModel(Item.getItemFromBlock(block(shapeName + "_slab")),
                        shapeName + "_slab");
                registerItemModel(Item.getItemFromBlock(block(shapeName + "_stairs")),
                        shapeName + "_stairs");
                registerItemModel(Item.getItemFromBlock(block(shapeName + "_wall")),
                        shapeName + "_wall");
            }
        }

        private static void registerItemModel(Item item, String name) {
            ModelLoader.setCustomModelResourceLocation(item, 0,
                    new ModelResourceLocation(id(name), "inventory"));
        }
    }
}
