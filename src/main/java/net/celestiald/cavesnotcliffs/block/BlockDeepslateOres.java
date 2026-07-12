package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.content.CncMaterialContent;
import net.celestiald.cavesnotcliffs.content.CncBlockProperties;
import net.celestiald.cavesnotcliffs.content.DeepslateSoundEvents;
import net.celestiald.cavesnotcliffs.content.OreDropLogic;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Items;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
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
            "deepslate_coal_ore", "deepslate_iron_ore", "deepslate_copper_ore",
            "deepslate_gold_ore",
            "deepslate_redstone_ore", "deepslate_lapis_ore", "deepslate_diamond_ore",
            "deepslate_emerald_ore"
    };

    @GameRegistry.ObjectHolder("cavesnotcliffs:deepslate_coal_ore")
    public static final Block DEEPSLATE_COAL_ORE = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:deepslate_iron_ore")
    public static final Block DEEPSLATE_IRON_ORE = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:deepslate_copper_ore")
    public static final Block DEEPSLATE_COPPER_ORE = null;
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
        register("deepslate_iron_ore", Drop.RAW_IRON, 1);
        register("deepslate_copper_ore", Drop.RAW_COPPER, 1);
        register("deepslate_gold_ore", Drop.RAW_GOLD, 2);
        register("deepslate_redstone_ore", Drop.REDSTONE, 2);
        register("deepslate_lapis_ore", Drop.LAPIS, 1);
        register("deepslate_diamond_ore", Drop.DIAMOND, 2);
        register("deepslate_emerald_ore", Drop.EMERALD, 2);
    }

    private void register(final String name, final Drop drop, final int harvestLevel) {
        elements.blocks.add(() -> createOreBlock(name, drop, harvestLevel)
                .setRegistryName(new ResourceLocation("cavesnotcliffs", name)));
        elements.items.add(() -> {
            Block block = GameRegistry.findRegistry(Block.class)
                    .getValue(new ResourceLocation("cavesnotcliffs", name));
            return new ItemBlock(block).setRegistryName(block.getRegistryName());
        });
    }

    private static Block createOreBlock(String name, Drop drop, int harvestLevel) {
        return drop == Drop.REDSTONE
                ? new DeepslateRedstoneOreBlock()
                : new DeepslateOreBlock(name, drop, harvestLevel);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        GameRegistry.addSmelting(new ItemStack(DEEPSLATE_COAL_ORE),
                new ItemStack(Items.COAL), 0.1F);
        GameRegistry.addSmelting(new ItemStack(DEEPSLATE_IRON_ORE),
                new ItemStack(Items.IRON_INGOT), 0.7F);
        GameRegistry.addSmelting(new ItemStack(DEEPSLATE_COPPER_ORE),
                new ItemStack(CncMaterialContent.item("copper_ingot")), 0.7F);
        GameRegistry.addSmelting(new ItemStack(DEEPSLATE_GOLD_ORE),
                new ItemStack(Items.GOLD_INGOT), 1.0F);
        GameRegistry.addSmelting(new ItemStack(DEEPSLATE_REDSTONE_ORE),
                new ItemStack(Items.REDSTONE), 0.7F);
        GameRegistry.addSmelting(new ItemStack(DEEPSLATE_LAPIS_ORE),
                new ItemStack(Items.DYE, 1, 4), 0.2F);
        GameRegistry.addSmelting(new ItemStack(DEEPSLATE_DIAMOND_ORE),
                new ItemStack(Items.DIAMOND), 1.0F);
        GameRegistry.addSmelting(new ItemStack(DEEPSLATE_EMERALD_ORE),
                new ItemStack(Items.EMERALD), 1.0F);
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
        RAW_IRON, RAW_COPPER, RAW_GOLD, COAL, REDSTONE, LAPIS, DIAMOND, EMERALD
    }

    private static class DeepslateOreBlock extends Block {
        private final Drop drop;

        DeepslateOreBlock(String name, Drop drop, int harvestLevel) {
            super(Material.ROCK, CncBlockProperties.DEEPSLATE);
            this.drop = drop;
            setUnlocalizedName(name);
            setCreativeTab(net.minecraft.creativetab.CreativeTabs.BUILDING_BLOCKS);
            setSoundType(DeepslateSoundEvents.DEEPSLATE);
            setHardness(4.5F);
            setResistance(CncBlockProperties.legacyResistance(3.0F));
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
                case RAW_IRON:
                    return CncMaterialContent.item("raw_iron");
                case RAW_COPPER:
                    return CncMaterialContent.item("raw_copper");
                case RAW_GOLD:
                    return CncMaterialContent.item("raw_gold");
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
                return 4 + random.nextInt(6);
            }
            if (drop == Drop.REDSTONE) {
                return 4 + random.nextInt(2);
            }
            if (drop == Drop.RAW_COPPER) {
                return 2 + random.nextInt(4);
            }
            return 1;
        }

        @Override
        public int quantityDroppedWithBonus(int fortune, Random random) {
            int base = quantityDropped(random);
            if (drop == Drop.REDSTONE) {
                return OreDropLogic.applyUniformBonus(base, fortune, random);
            }
            return OreDropLogic.applyOreBonus(base, fortune, random);
        }

        @Override
        public void dropBlockAsItemWithChance(World world, BlockPos pos, IBlockState state,
                float chance, int fortune) {
            if (!OreDropLogic.dropWithExplosionDecay(this, world, pos, state,
                    chance, fortune, harvesters.get())) {
                super.dropBlockAsItemWithChance(world, pos, state, chance, fortune);
            }
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

        @Override
        protected boolean canSilkHarvest() {
            return true;
        }
    }

    /** One-block-state backport of 1.18.2's {@code RedStoneOreBlock}. */
    static final class DeepslateRedstoneOreBlock extends DeepslateOreBlock {
        static final PropertyBool LIT = PropertyBool.create("lit");
        private static final double PARTICLE_OFFSET = 0.5625D;

        DeepslateRedstoneOreBlock() {
            super("deepslate_redstone_ore", Drop.REDSTONE, 2);
            setTickRandomly(true);
            setDefaultState(blockState.getBaseState().withProperty(LIT, false));
        }

        @Override
        public void onBlockClicked(World world, BlockPos pos, EntityPlayer player) {
            interact(world.getBlockState(pos), world, pos);
            super.onBlockClicked(world, pos, player);
        }

        @Override
        public void onEntityWalk(World world, BlockPos pos, Entity entity) {
            interact(world.getBlockState(pos), world, pos);
            super.onEntityWalk(world, pos, entity);
        }

        @Override
        public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                EntityPlayer player, EnumHand hand, EnumFacing facing,
                float hitX, float hitY, float hitZ) {
            interact(state, world, pos);
            return super.onBlockActivated(world, pos, state, player, hand, facing,
                    hitX, hitY, hitZ);
        }

        private static void interact(IBlockState state, World world, BlockPos pos) {
            spawnParticles(world, pos);
            if (!state.getValue(LIT)) {
                world.setBlockState(pos, state.withProperty(LIT, true), 3);
            }
        }

        @Override
        public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
            if (state.getValue(LIT)) {
                world.setBlockState(pos, state.withProperty(LIT, false), 3);
            }
        }

        @Override
        public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random random) {
            if (state.getValue(LIT)) {
                spawnParticles(world, pos);
            }
        }

        private static void spawnParticles(World world, BlockPos pos) {
            for (EnumFacing direction : EnumFacing.values()) {
                BlockPos neighbor = pos.offset(direction);
                if (world.getBlockState(neighbor).isOpaqueCube()) {
                    continue;
                }
                double x = pos.getX() + 0.5D
                        + PARTICLE_OFFSET * direction.getDirectionVec().getX();
                double y = pos.getY() + 0.5D
                        + PARTICLE_OFFSET * direction.getDirectionVec().getY();
                double z = pos.getZ() + 0.5D
                        + PARTICLE_OFFSET * direction.getDirectionVec().getZ();
                world.spawnParticle(EnumParticleTypes.REDSTONE, x, y, z,
                        0.0D, 0.0D, 0.0D);
            }
        }

        @Override
        public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
            return state.getValue(LIT) ? 9 : 0;
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return new BlockStateContainer(this, LIT);
        }

        @Override
        public IBlockState getStateFromMeta(int meta) {
            return getDefaultState().withProperty(LIT, (meta & 1) != 0);
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            return state.getValue(LIT) ? 1 : 0;
        }

        @Override
        protected ItemStack getSilkTouchDrop(IBlockState state) {
            return new ItemStack(this);
        }

        @Override
        public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world,
                BlockPos pos, EntityPlayer player) {
            return new ItemStack(this);
        }
    }
}
