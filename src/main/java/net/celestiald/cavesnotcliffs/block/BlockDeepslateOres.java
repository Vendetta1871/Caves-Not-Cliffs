package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/** Deepslate variants of every ore present in vanilla 1.12.2. */
@ElementsCavesNotCliffs.ModElement.Tag
public final class BlockDeepslateOres extends ElementsCavesNotCliffs.ModElement {
    private static final String[] NAMES = {
            "deepslate_coal_ore", "deepslate_iron_ore", "deepslate_gold_ore",
            "deepslate_redstone_ore", "deepslate_lapis_ore", "deepslate_diamond_ore",
            "deepslate_emerald_ore"
    };

    @GameRegistry.ObjectHolder("cavesnotcliffs:deepslate_coal_ore")
    public static final Block DEEPSLATE_COAL_ORE = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:deepslate_iron_ore")
    public static final Block DEEPSLATE_IRON_ORE = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:deepslate_gold_ore")
    public static final Block DEEPSLATE_GOLD_ORE = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:deepslate_redstone_ore")
    public static final Block DEEPSLATE_REDSTONE_ORE = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:deepslate_lapis_ore")
    public static final Block DEEPSLATE_LAPIS_ORE = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:deepslate_diamond_ore")
    public static final Block DEEPSLATE_DIAMOND_ORE = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:deepslate_emerald_ore")
    public static final Block DEEPSLATE_EMERALD_ORE = null;

    public BlockDeepslateOres(ElementsCavesNotCliffs elements) {
        super(elements, 301);
    }

    @Override
    public void initElements() {
        register("deepslate_coal_ore", Drop.COAL, 0);
        register("deepslate_iron_ore", Drop.SELF, 1);
        register("deepslate_gold_ore", Drop.SELF, 2);
        register("deepslate_redstone_ore", Drop.REDSTONE, 2);
        register("deepslate_lapis_ore", Drop.LAPIS, 1);
        register("deepslate_diamond_ore", Drop.DIAMOND, 2);
        register("deepslate_emerald_ore", Drop.EMERALD, 2);
    }

    private void register(final String name, final Drop drop, final int harvestLevel) {
        elements.blocks.add(() -> new DeepslateOreBlock(name, drop, harvestLevel)
                .setRegistryName(new ResourceLocation("cavesnotcliffs", name)));
        elements.items.add(() -> {
            Block block = GameRegistry.findRegistry(Block.class)
                    .getValue(new ResourceLocation("cavesnotcliffs", name));
            return new ItemBlock(block).setRegistryName(block.getRegistryName());
        });
    }

    @Override
    public void init(FMLInitializationEvent event) {
        GameRegistry.addSmelting(new ItemStack(DEEPSLATE_IRON_ORE),
                new ItemStack(Items.IRON_INGOT), 0.7F);
        GameRegistry.addSmelting(new ItemStack(DEEPSLATE_GOLD_ORE),
                new ItemStack(Items.GOLD_INGOT), 1.0F);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        for (String name : NAMES) {
            Item item = GameRegistry.findRegistry(Item.class)
                    .getValue(new ResourceLocation("cavesnotcliffs", name));
            if (item != null) {
                ModelLoader.setCustomModelResourceLocation(item, 0,
                        new ModelResourceLocation("cavesnotcliffs:" + name, "inventory"));
            }
        }
    }

    private enum Drop {
        SELF, COAL, REDSTONE, LAPIS, DIAMOND, EMERALD
    }

    private static final class DeepslateOreBlock extends Block {
        private final Drop drop;

        DeepslateOreBlock(String name, Drop drop, int harvestLevel) {
            super(Material.ROCK);
            this.drop = drop;
            setUnlocalizedName(name);
            setCreativeTab(net.minecraft.creativetab.CreativeTabs.BUILDING_BLOCKS);
            setSoundType(SoundType.STONE);
            setHardness(4.5F);
            setResistance(6.0F);
            setHarvestLevel("pickaxe", harvestLevel);
        }

        @Override
        public Item getItemDropped(IBlockState state, Random random, int fortune) {
            switch (drop) {
                case COAL:
                    return Items.COAL;
                case REDSTONE:
                    return Items.REDSTONE;
                case LAPIS:
                    return Items.DYE;
                case DIAMOND:
                    return Items.DIAMOND;
                case EMERALD:
                    return Items.EMERALD;
                default:
                    return Item.getItemFromBlock(this);
            }
        }

        @Override
        public int damageDropped(IBlockState state) {
            return drop == Drop.LAPIS ? 4 : 0;
        }

        @Override
        public int quantityDropped(Random random) {
            if (drop == Drop.LAPIS) {
                return 4 + random.nextInt(5);
            }
            if (drop == Drop.REDSTONE) {
                return 4 + random.nextInt(2);
            }
            return 1;
        }

        @Override
        public int quantityDroppedWithBonus(int fortune, Random random) {
            if (fortune > 0 && drop != Drop.SELF) {
                int multiplier = random.nextInt(fortune + 2) - 1;
                return quantityDropped(random) * (Math.max(0, multiplier) + 1);
            }
            return quantityDropped(random);
        }

        @Override
        public int getExpDrop(IBlockState state, IBlockAccess world, BlockPos pos, int fortune) {
            switch (drop) {
                case COAL:
                    return MathHelper.getInt(RANDOM, 0, 2);
                case REDSTONE:
                    return MathHelper.getInt(RANDOM, 1, 5);
                case LAPIS:
                    return MathHelper.getInt(RANDOM, 2, 5);
                case DIAMOND:
                case EMERALD:
                    return MathHelper.getInt(RANDOM, 3, 7);
                default:
                    return 0;
            }
        }
    }
}
