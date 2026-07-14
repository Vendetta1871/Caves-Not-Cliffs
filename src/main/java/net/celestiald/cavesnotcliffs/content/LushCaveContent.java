package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.block.LushAzaleaBlocks;
import net.celestiald.cavesnotcliffs.block.LushCaveVinesBlock;
import net.celestiald.cavesnotcliffs.block.LushDripleafBlocks;
import net.celestiald.cavesnotcliffs.block.LushMossBlocks;
import net.celestiald.cavesnotcliffs.block.LushSporeBlossomBlock;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Registers the canonical public lush-cave family and all hidden storage/migration states. */
@ElementsCavesNotCliffs.ModElement.Tag
public final class LushCaveContent extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:cave_vines")
    public static final Block CAVE_VINES = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:cave_vines_age_8_15")
    public static final Block CAVE_VINES_AGE_8_15 = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:cave_vines_age_16_23")
    public static final Block CAVE_VINES_AGE_16_23 = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:cave_vines_age_24_25")
    public static final Block CAVE_VINES_AGE_24_25 = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:cave_vines_plant")
    public static final Block CAVE_VINES_PLANT = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:azalea")
    public static final Block AZALEA = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:flowering_azalea")
    public static final Block FLOWERING_AZALEA = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:azalea_leaves")
    public static final Block AZALEA_LEAVES = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:flowering_azalea_leaves")
    public static final Block FLOWERING_AZALEA_LEAVES = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:rooted_dirt")
    public static final Block ROOTED_DIRT = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:hanging_roots")
    public static final Block HANGING_ROOTS = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:hanging_roots_waterlogged")
    public static final Block HANGING_ROOTS_WATERLOGGED = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:moss_block")
    public static final Block MOSS_BLOCK = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:moss_carpet")
    public static final Block MOSS_CARPET = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:small_dripleaf")
    public static final Block SMALL_DRIPLEAF = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:big_dripleaf")
    public static final Block BIG_DRIPLEAF = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:big_dripleaf_waterlogged")
    public static final Block BIG_DRIPLEAF_WATERLOGGED = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:big_dripleaf_stem")
    public static final Block BIG_DRIPLEAF_STEM = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:spore_blossom")
    public static final Block SPORE_BLOSSOM = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:potted_azalea_bush")
    public static final Block POTTED_AZALEA = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:potted_flowering_azalea_bush")
    public static final Block POTTED_FLOWERING_AZALEA = null;

    public LushCaveContent(ElementsCavesNotCliffs elements) {
        super(elements, 334);
    }

    @Override
    public void initElements() {
        LushCaveSounds.registerAll();

        block(() -> new LushCaveVinesBlock.Head(0, 7), CncRegistryIds.CAVE_VINES);
        block(() -> new LushCaveVinesBlock.Head(8, 7),
                CncRegistryIds.CAVE_VINES_AGE_8_15);
        block(() -> new LushCaveVinesBlock.Head(16, 7),
                CncRegistryIds.CAVE_VINES_AGE_16_23);
        block(() -> new LushCaveVinesBlock.Head(24, 1),
                CncRegistryIds.CAVE_VINES_AGE_24_25);
        block(LushCaveVinesBlock.Body::new, CncRegistryIds.CAVE_VINES_PLANT);

        publicBlock(() -> new LushAzaleaBlocks.AzaleaBush(false), CncRegistryIds.AZALEA);
        publicBlock(() -> new LushAzaleaBlocks.AzaleaBush(true),
                CncRegistryIds.FLOWERING_AZALEA);
        publicBlock(() -> new LushAzaleaBlocks.AzaleaLeaves(false),
                CncRegistryIds.AZALEA_LEAVES);
        publicBlock(() -> new LushAzaleaBlocks.AzaleaLeaves(true),
                CncRegistryIds.FLOWERING_AZALEA_LEAVES);
        publicBlock(LushAzaleaBlocks.RootedDirt::new, CncRegistryIds.ROOTED_DIRT);
        publicBlock(() -> new LushAzaleaBlocks.HangingRoots(false),
                CncRegistryIds.HANGING_ROOTS);
        block(() -> new LushAzaleaBlocks.HangingRoots(true),
                CncRegistryIds.HANGING_ROOTS_WATERLOGGED);
        publicBlock(LushMossBlocks.Moss::new, CncRegistryIds.MOSS_BLOCK);
        publicBlock(LushMossBlocks.Carpet::new, CncRegistryIds.MOSS_CARPET);
        publicBlock(LushDripleafBlocks.Small::new, CncRegistryIds.SMALL_DRIPLEAF);
        publicBlock(() -> new LushDripleafBlocks.Head(false), CncRegistryIds.BIG_DRIPLEAF);
        block(() -> new LushDripleafBlocks.Head(true),
                CncRegistryIds.BIG_DRIPLEAF_WATERLOGGED);
        block(LushDripleafBlocks.Stem::new, CncRegistryIds.BIG_DRIPLEAF_STEM);
        publicBlock(LushSporeBlossomBlock::new, CncRegistryIds.SPORE_BLOSSOM);
        block(() -> new LushAzaleaBlocks.PottedAzalea(false),
                CncRegistryIds.POTTED_AZALEA);
        block(() -> new LushAzaleaBlocks.PottedAzalea(true),
                CncRegistryIds.POTTED_FLOWERING_AZALEA);

        // Released state-split blocks remain registered until their one-time chunk conversion.
        legacy("glow_berry_vines", Material.VINE, LushCaveSounds.CAVE_VINES);
        legacy("glow_berry_middle_fill", Material.VINE, LushCaveSounds.CAVE_VINES);
        legacy("baby_dripleaf", Material.PLANTS, LushCaveSounds.SMALL_DRIPLEAF);
        legacy("dripleaf_stem", Material.PLANTS, LushCaveSounds.BIG_DRIPLEAF);
        legacy("dripleafplant_1", Material.PLANTS, LushCaveSounds.BIG_DRIPLEAF);
        legacy("dripleaf_plant_2", Material.PLANTS, LushCaveSounds.BIG_DRIPLEAF);
    }

    private void publicBlock(java.util.function.Supplier<Block> supplier, ResourceLocation id) {
        block(supplier, id);
        elements.items.add(() -> new ItemBlock(requireBlock(id)).setRegistryName(id));
    }

    private void block(java.util.function.Supplier<Block> supplier, ResourceLocation id) {
        elements.blocks.add(() -> supplier.get().setRegistryName(id));
    }

    private void legacy(String path, Material material, SoundType sound) {
        block(() -> new LegacyStateBlock(material, path, sound), CncRegistryIds.id(path));
    }

    private static Block requireBlock(ResourceLocation id) {
        Block block = net.minecraftforge.fml.common.registry.ForgeRegistries.BLOCKS.getValue(id);
        if (block == null) {
            throw new IllegalStateException("Missing lush-cave block before item registration: " + id);
        }
        return block;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        item(AZALEA, CncRegistryIds.AZALEA);
        item(FLOWERING_AZALEA, CncRegistryIds.FLOWERING_AZALEA);
        item(AZALEA_LEAVES, CncRegistryIds.AZALEA_LEAVES);
        item(FLOWERING_AZALEA_LEAVES, CncRegistryIds.FLOWERING_AZALEA_LEAVES);
        item(ROOTED_DIRT, CncRegistryIds.ROOTED_DIRT);
        item(HANGING_ROOTS, CncRegistryIds.HANGING_ROOTS);
        item(MOSS_BLOCK, CncRegistryIds.MOSS_BLOCK);
        item(MOSS_CARPET, CncRegistryIds.MOSS_CARPET);
        item(SMALL_DRIPLEAF, CncRegistryIds.SMALL_DRIPLEAF);
        item(BIG_DRIPLEAF, CncRegistryIds.BIG_DRIPLEAF);
        item(SPORE_BLOSSOM, CncRegistryIds.SPORE_BLOSSOM);

        ignore(AZALEA_LEAVES, LushAzaleaBlocks.AzaleaLeaves.DISTANCE,
                LushAzaleaBlocks.AzaleaLeaves.PERSISTENT);
        ignore(FLOWERING_AZALEA_LEAVES, LushAzaleaBlocks.AzaleaLeaves.DISTANCE,
                LushAzaleaBlocks.AzaleaLeaves.PERSISTENT);
        ignore(CAVE_VINES, LushCaveVinesBlock.LOCAL_AGE);
        ignore(CAVE_VINES_AGE_8_15, LushCaveVinesBlock.LOCAL_AGE);
        ignore(CAVE_VINES_AGE_16_23, LushCaveVinesBlock.LOCAL_AGE);
        ignore(CAVE_VINES_AGE_24_25, LushCaveVinesBlock.LOCAL_AGE);
    }

    @SideOnly(Side.CLIENT)
    private static void item(Block block, ResourceLocation id) {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0,
                new ModelResourceLocation(id, "inventory"));
    }

    @SideOnly(Side.CLIENT)
    private static void ignore(Block block,
            net.minecraft.block.properties.IProperty<?>... properties) {
        ModelLoader.setCustomStateMapper(block,
                new StateMap.Builder().ignore(properties).build());
    }

    private static final class LegacyStateBlock extends Block {
        private LegacyStateBlock(Material material, String path, SoundType sound) {
            super(material);
            setUnlocalizedName(path);
            setSoundType(sound);
            setHardness(0.0F);
        }

        @Override public boolean isOpaqueCube(IBlockState state) { return false; }
        @Override public boolean isFullCube(IBlockState state) { return false; }
        @SideOnly(Side.CLIENT) @Override public BlockRenderLayer getBlockLayer() {
            return BlockRenderLayer.CUTOUT;
        }
    }
}
