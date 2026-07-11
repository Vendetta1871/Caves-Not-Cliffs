package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemSlab;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/** Runtime registration and state-preserving behavior for every Java 1.18.2 copper variant. */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID)
public final class CopperContent {
    private static final Map<String, Block> BLOCKS_BY_PATH = new LinkedHashMap<>();
    private static final Map<Block, CopperWeathering.Variant> VARIANTS_BY_BLOCK =
            new IdentityHashMap<>();
    private static final Map<Block, Block> NEXT = new IdentityHashMap<>();
    private static final Map<Block, Block> PREVIOUS = new IdentityHashMap<>();
    private static final Map<Block, Block> WAXED = new IdentityHashMap<>();
    private static final Map<Block, Block> UNWAXED = new IdentityHashMap<>();

    private CopperContent() {
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!BLOCKS_BY_PATH.isEmpty()) {
            throw new IllegalStateException("Copper blocks were registered twice");
        }
        for (CopperWeathering.Variant variant : CopperWeathering.variants()) {
            Block block = createBlock(variant);
            block.setRegistryName(id(variant.getPath()));
            BLOCKS_BY_PATH.put(variant.getPath(), block);
            VARIANTS_BY_BLOCK.put(block, variant);
            event.getRegistry().register(block);
        }
        buildTransitionMaps();
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        for (CopperWeathering.Variant variant : CopperWeathering.variants()) {
            if (!variant.hasPublicItem()) {
                continue;
            }
            Block block = requiredBlock(variant.getPath());
            Item item;
            if (variant.getShape() == CopperWeathering.Shape.SLAB) {
                Block doubled = requiredBlock(CopperWeathering.path(variant.getStage(),
                        CopperWeathering.Shape.DOUBLE_SLAB, variant.isWaxed()));
                item = new ItemSlab(block, (BlockSlab) block, (BlockSlab) doubled);
            } else {
                item = new ItemBlock(block);
            }
            event.getRegistry().register(item.setRegistryName(block.getRegistryName()));
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {
        for (CopperWeathering.Variant variant : CopperWeathering.variants()) {
            if (!variant.hasPublicItem()) {
                continue;
            }
            Item item = ForgeRegistries.ITEMS.getValue(id(variant.getPath()));
            if (item != null) {
                ModelLoader.setCustomModelResourceLocation(item, 0,
                        new ModelResourceLocation(id(variant.getPath()), "inventory"));
            }
        }
    }

    public static Block block(String path) {
        Block block = BLOCKS_BY_PATH.get(path);
        return block == null ? ForgeRegistries.BLOCKS.getValue(id(path)) : block;
    }

    public static CopperWeathering.Variant variant(Block block) {
        return VARIANTS_BY_BLOCK.get(block);
    }

    public static Map<Block, Block> nextByBlock() {
        return Collections.unmodifiableMap(NEXT);
    }

    public static Map<Block, Block> previousByBlock() {
        return Collections.unmodifiableMap(PREVIOUS);
    }

    public static Map<Block, Block> waxedByBlock() {
        return Collections.unmodifiableMap(WAXED);
    }

    public static Map<Block, Block> unwaxedByBlock() {
        return Collections.unmodifiableMap(UNWAXED);
    }

    public static IBlockState next(IBlockState state) {
        return transfer(state, NEXT.get(state.getBlock()));
    }

    public static IBlockState previous(IBlockState state) {
        return transfer(state, PREVIOUS.get(state.getBlock()));
    }

    public static IBlockState waxed(IBlockState state) {
        return transfer(state, WAXED.get(state.getBlock()));
    }

    public static IBlockState unwaxed(IBlockState state) {
        return transfer(state, UNWAXED.get(state.getBlock()));
    }

    public static IBlockState first(IBlockState state) {
        IBlockState result = state;
        IBlockState previous;
        while ((previous = previous(result)) != null) {
            result = previous;
        }
        return result;
    }

    /** Accepts both vanilla's bit-8 slab encoding and the draft-v2 MCreator ordinal encoding. */
    public static boolean isTopSlabMetadata(int meta) {
        return (meta & 8) != 0 || (meta & 1) != 0;
    }

    private static Block createBlock(CopperWeathering.Variant variant) {
        switch (variant.getShape()) {
            case BLOCK:
            case CUT:
                return new CopperFullBlock(variant);
            case STAIRS:
                Block base = requiredBlock(CopperWeathering.path(variant.getStage(),
                        CopperWeathering.Shape.CUT, variant.isWaxed()));
                return new CopperStairsBlock(variant, base.getDefaultState());
            case SLAB:
                return new CopperSlabBlock(variant);
            case DOUBLE_SLAB:
                return new CopperDoubleSlabBlock(variant);
            default:
                throw new IllegalArgumentException("Unknown copper shape " + variant.getShape());
        }
    }

    private static void buildTransitionMaps() {
        for (CopperWeathering.Variant variant : CopperWeathering.variants()) {
            Block source = requiredBlock(variant.getPath());
            CopperWeathering.Variant target = CopperWeathering.next(variant);
            if (target != null) {
                mapPair(NEXT, PREVIOUS, source, requiredBlock(target.getPath()));
            }
            target = CopperWeathering.waxed(variant);
            if (target != null) {
                mapPair(WAXED, UNWAXED, source, requiredBlock(target.getPath()));
            }
        }
    }

    private static void mapPair(Map<Block, Block> forward, Map<Block, Block> reverse,
            Block source, Block target) {
        if (forward.put(source, target) != null || reverse.put(target, source) != null) {
            throw new IllegalStateException("Duplicate copper state transition");
        }
    }

    private static IBlockState transfer(IBlockState source, Block target) {
        if (target == null) {
            return null;
        }
        IBlockState result = target.getDefaultState();
        if (source.getBlock() instanceof BlockStairs && target instanceof BlockStairs) {
            result = result.withProperty(BlockStairs.FACING, source.getValue(BlockStairs.FACING))
                    .withProperty(BlockStairs.HALF, source.getValue(BlockStairs.HALF))
                    .withProperty(BlockStairs.SHAPE, source.getValue(BlockStairs.SHAPE));
        } else if (source.getBlock() instanceof CopperSlabBlock
                && target instanceof CopperSlabBlock
                && !((CopperSlabBlock) target).isDouble()) {
            result = result.withProperty(BlockSlab.HALF, source.getValue(BlockSlab.HALF));
        }
        return result;
    }

    private static void randomTick(CopperWeathering.Variant variant, World world, BlockPos pos,
            IBlockState state, Random random) {
        if (world.isRemote || variant.isWaxed() || variant.getStage().next() == null
                || !(random.nextFloat() < CopperWeathering.RANDOM_TICK_CHANCE)) {
            return;
        }

        int same = 0;
        int older = 0;
        for (int dx = -CopperWeathering.SCAN_DISTANCE;
                dx <= CopperWeathering.SCAN_DISTANCE; dx++) {
            for (int dy = -CopperWeathering.SCAN_DISTANCE;
                    dy <= CopperWeathering.SCAN_DISTANCE; dy++) {
                for (int dz = -CopperWeathering.SCAN_DISTANCE;
                        dz <= CopperWeathering.SCAN_DISTANCE; dz++) {
                    int distance = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                    if (distance == 0 || distance > CopperWeathering.SCAN_DISTANCE) {
                        continue;
                    }
                    CopperWeathering.Variant neighbor = variant(
                            world.getBlockState(pos.add(dx, dy, dz)).getBlock());
                    if (neighbor == null || neighbor.isWaxed()) {
                        continue;
                    }
                    if (neighbor.getStage().ordinal() < variant.getStage().ordinal()) {
                        return;
                    }
                    if (neighbor.getStage().ordinal() > variant.getStage().ordinal()) {
                        older++;
                    } else {
                        same++;
                    }
                }
            }
        }

        float ratio = (float) (older + 1) / (float) (older + same + 1);
        float chance = ratio * ratio * CopperWeathering.chanceModifier(variant.getStage());
        if (random.nextFloat() < chance) {
            IBlockState next = next(state);
            if (next != null) {
                world.setBlockState(pos, next, 3);
            }
        }
    }

    private static MapColor mapColor(CopperWeathering.Stage stage) {
        switch (stage) {
            case UNAFFECTED:
                return MapColor.ADOBE;
            case EXPOSED:
                return MapColor.SILVER_STAINED_HARDENED_CLAY;
            case WEATHERED:
                return CncBlockProperties.WARPED_STEM;
            case OXIDIZED:
                return CncBlockProperties.WARPED_NYLIUM;
            default:
                throw new IllegalArgumentException("Unknown copper stage " + stage);
        }
    }

    private static void configure(Block block, CopperWeathering.Variant variant) {
        block.setUnlocalizedName(variant.getPath());
        block.setCreativeTab(variant.hasPublicItem() ? CreativeTabs.BUILDING_BLOCKS : null);
        block.setHardness(3.0F);
        block.setResistance(CncBlockProperties.legacyResistance(6.0F));
        block.setHarvestLevel("pickaxe", 1);
        block.setTickRandomly(!variant.isWaxed()
                && variant.getStage() != CopperWeathering.Stage.OXIDIZED);
    }

    private static Block requiredBlock(String path) {
        Block block = BLOCKS_BY_PATH.get(path);
        if (block == null) {
            throw new IllegalStateException("Copper block was not registered: " + path);
        }
        return block;
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(CavesNotCliffs.MODID, path);
    }

    private static final class CopperFullBlock extends Block {
        private final CopperWeathering.Variant variant;

        CopperFullBlock(CopperWeathering.Variant variant) {
            super(Material.IRON, mapColor(variant.getStage()));
            this.variant = variant;
            configure(this, variant);
            setSoundType(CopperSoundEvents.COPPER);
        }

        @Override
        public void randomTick(World world, BlockPos pos, IBlockState state, Random random) {
            CopperContent.randomTick(variant, world, pos, state, random);
        }
    }

    private static final class CopperStairsBlock extends BlockStairs {
        private final CopperWeathering.Variant variant;

        CopperStairsBlock(CopperWeathering.Variant variant, IBlockState modelState) {
            super(modelState);
            this.variant = variant;
            configure(this, variant);
            setSoundType(CopperSoundEvents.COPPER);
        }

        @Override
        public void randomTick(World world, BlockPos pos, IBlockState state, Random random) {
            CopperContent.randomTick(variant, world, pos, state, random);
        }
    }

    private static class CopperSlabBlock extends BlockSlab {
        private static final PropertyEnum<VariantProperty> VARIANT =
                PropertyEnum.create("variant", VariantProperty.class);

        private final CopperWeathering.Variant variant;

        CopperSlabBlock(CopperWeathering.Variant variant) {
            super(Material.IRON, mapColor(variant.getStage()));
            this.variant = variant;
            configure(this, variant);
            setSoundType(CopperSoundEvents.COPPER);
            IBlockState state = blockState.getBaseState().withProperty(VARIANT,
                    VariantProperty.DEFAULT);
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
            return VariantProperty.DEFAULT;
        }

        @Override
        public Item getItemDropped(IBlockState state, Random random, int fortune) {
            String single = CopperWeathering.path(variant.getStage(),
                    CopperWeathering.Shape.SLAB, variant.isWaxed());
            return Item.getItemFromBlock(block(single));
        }

        @Override
        public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
            String single = CopperWeathering.path(variant.getStage(),
                    CopperWeathering.Shape.SLAB, variant.isWaxed());
            return new ItemStack(block(single));
        }

        @Override
        public String getUnlocalizedName(int meta) {
            return super.getUnlocalizedName();
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return isDouble() ? new BlockStateContainer(this, VARIANT)
                    : new BlockStateContainer(this, HALF, VARIANT);
        }

        @Override
        public IBlockState getStateFromMeta(int meta) {
            return isDouble() ? getDefaultState() : getDefaultState().withProperty(HALF,
                    isTopSlabMetadata(meta) ? EnumBlockHalf.TOP : EnumBlockHalf.BOTTOM);
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            return !isDouble() && state.getValue(HALF) == EnumBlockHalf.TOP ? 8 : 0;
        }

        @Override
        public void randomTick(World world, BlockPos pos, IBlockState state, Random random) {
            CopperContent.randomTick(variant, world, pos, state, random);
        }
    }

    private static final class CopperDoubleSlabBlock extends CopperSlabBlock {
        CopperDoubleSlabBlock(CopperWeathering.Variant variant) {
            super(variant);
        }

        @Override
        public boolean isDouble() {
            return true;
        }
    }

    private enum VariantProperty implements IStringSerializable {
        DEFAULT;

        @Override
        public String getName() {
            return "default";
        }
    }
}
