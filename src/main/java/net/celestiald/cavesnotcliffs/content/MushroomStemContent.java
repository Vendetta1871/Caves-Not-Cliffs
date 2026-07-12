package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

/** Lossless Java 1.18.2 mushroom-stem state and item backport. */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID)
public final class MushroomStemContent {
    public static final PropertyBool NORTH = PropertyBool.create("north");
    public static final PropertyBool EAST = PropertyBool.create("east");
    public static final PropertyBool SOUTH = PropertyBool.create("south");
    public static final PropertyBool WEST = PropertyBool.create("west");

    @GameRegistry.ObjectHolder("cavesnotcliffs:mushroom_stem")
    public static final Block MUSHROOM_STEM = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:mushroom_stem_up")
    public static final Block MUSHROOM_STEM_UP = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:mushroom_stem_down")
    public static final Block MUSHROOM_STEM_DOWN = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:mushroom_stem_up_down")
    public static final Block MUSHROOM_STEM_UP_DOWN = null;

    private MushroomStemContent() {
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(
                block(CncRegistryIds.MUSHROOM_STEM, true, true),
                block(CncRegistryIds.MUSHROOM_STEM_UP, true, false),
                block(CncRegistryIds.MUSHROOM_STEM_DOWN, false, true),
                block(CncRegistryIds.MUSHROOM_STEM_UP_DOWN, false, false));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        Block block = ForgeRegistries.BLOCKS.getValue(CncRegistryIds.MUSHROOM_STEM);
        if (block == null) {
            throw new IllegalStateException("Mushroom stem was not registered before its item");
        }
        event.getRegistry().register(new ItemBlock(block)
                .setRegistryName(CncRegistryIds.MUSHROOM_STEM));
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {
        Item item = ForgeRegistries.ITEMS.getValue(CncRegistryIds.MUSHROOM_STEM);
        if (item == null) {
            throw new IllegalStateException("Mushroom stem item was not registered");
        }
        ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation(CncRegistryIds.MUSHROOM_STEM, "inventory"));
    }

    private static MushroomStemBlock block(ResourceLocation id, boolean up, boolean down) {
        return (MushroomStemBlock) new MushroomStemBlock(up, down).setRegistryName(id);
    }

    /** State used by the configured huge-mushroom feature for a generated stem body. */
    public static IBlockState generatedStemState() {
        return state(true, true, true, true, false, false);
    }

    /** Resolves all six canonical face properties to their lossless 1.12 representation. */
    public static IBlockState state(boolean north, boolean east, boolean south,
            boolean west, boolean up, boolean down) {
        MushroomStemBlock block = blockForVerticalFaces(up, down);
        return block.getDefaultState()
                .withProperty(NORTH, north)
                .withProperty(EAST, east)
                .withProperty(SOUTH, south)
                .withProperty(WEST, west);
    }

    public static MushroomStemBlock blockForVerticalFaces(boolean up, boolean down) {
        Block block = ForgeRegistries.BLOCKS.getValue(verticalId(up, down));
        if (!(block instanceof MushroomStemBlock)) {
            throw new IllegalStateException("Mushroom stem state group was not registered: "
                    + verticalId(up, down));
        }
        return (MushroomStemBlock) block;
    }

    public static boolean isStemFamily(Block block) {
        return block instanceof MushroomStemBlock;
    }

    public static boolean hasFace(IBlockState state, EnumFacing face) {
        if (!(state.getBlock() instanceof MushroomStemBlock)) {
            return false;
        }
        MushroomStemBlock block = (MushroomStemBlock) state.getBlock();
        switch (face) {
            case UP: return block.up;
            case DOWN: return block.down;
            case NORTH: return state.getValue(NORTH);
            case EAST: return state.getValue(EAST);
            case SOUTH: return state.getValue(SOUTH);
            case WEST: return state.getValue(WEST);
            default: throw new AssertionError(face);
        }
    }

    public static IBlockState withFace(IBlockState state, EnumFacing face, boolean visible) {
        if (!(state.getBlock() instanceof MushroomStemBlock)) {
            throw new IllegalArgumentException("Not a mushroom stem state: " + state);
        }
        MushroomStemBlock block = (MushroomStemBlock) state.getBlock();
        if (face == EnumFacing.UP || face == EnumFacing.DOWN) {
            boolean up = face == EnumFacing.UP ? visible : block.up;
            boolean down = face == EnumFacing.DOWN ? visible : block.down;
            return copyHorizontal(state, blockForVerticalFaces(up, down).getDefaultState());
        }
        return state.withProperty(horizontalProperty(face), visible);
    }

    private static ResourceLocation verticalId(boolean up, boolean down) {
        if (up && down) {
            return CncRegistryIds.MUSHROOM_STEM;
        }
        if (up) {
            return CncRegistryIds.MUSHROOM_STEM_UP;
        }
        return down ? CncRegistryIds.MUSHROOM_STEM_DOWN
                : CncRegistryIds.MUSHROOM_STEM_UP_DOWN;
    }

    private static IBlockState copyHorizontal(IBlockState source, IBlockState target) {
        return target.withProperty(NORTH, source.getValue(NORTH))
                .withProperty(EAST, source.getValue(EAST))
                .withProperty(SOUTH, source.getValue(SOUTH))
                .withProperty(WEST, source.getValue(WEST));
    }

    static PropertyBool horizontalProperty(EnumFacing face) {
        switch (face) {
            case NORTH: return NORTH;
            case EAST: return EAST;
            case SOUTH: return SOUTH;
            case WEST: return WEST;
            default: throw new IllegalArgumentException("Not horizontal: " + face);
        }
    }

    public static final class MushroomStemBlock extends Block {
        private final boolean up;
        private final boolean down;

        private MushroomStemBlock(boolean up, boolean down) {
            super(Material.WOOD, MapColor.CLOTH);
            this.up = up;
            this.down = down;
            setUnlocalizedName("mushroom_stem");
            setHardness(0.2F);
            setResistance(CncBlockProperties.legacyResistance(0.2F));
            setSoundType(SoundType.WOOD);
            setDefaultState(blockState.getBaseState()
                    .withProperty(NORTH, true)
                    .withProperty(EAST, true)
                    .withProperty(SOUTH, true)
                    .withProperty(WEST, true));
            if (up && down) {
                setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
            }
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return new BlockStateContainer(this, NORTH, EAST, SOUTH, WEST);
        }

        @Override
        public IBlockState getStateFromMeta(int meta) {
            return getDefaultState()
                    .withProperty(NORTH, (meta & 1) != 0)
                    .withProperty(EAST, (meta & 2) != 0)
                    .withProperty(SOUTH, (meta & 4) != 0)
                    .withProperty(WEST, (meta & 8) != 0);
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            return (state.getValue(NORTH) ? 1 : 0)
                    | (state.getValue(EAST) ? 2 : 0)
                    | (state.getValue(SOUTH) ? 4 : 0)
                    | (state.getValue(WEST) ? 8 : 0);
        }

        @Override
        public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
                float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer,
                EnumHand hand) {
            IBlockState placed = state(true, true, true, true, true, true);
            for (EnumFacing direction : EnumFacing.values()) {
                if (isStemFamily(world.getBlockState(pos.offset(direction)).getBlock())) {
                    placed = withFace(placed, direction, false);
                }
            }
            return placed;
        }

        @Override
        public void neighborChanged(IBlockState state, World world, BlockPos pos,
                Block neighbor, BlockPos fromPos) {
            EnumFacing direction = directionTo(pos, fromPos);
            if (direction != null && hasFace(state, direction)
                    && isStemFamily(world.getBlockState(fromPos).getBlock())) {
                world.setBlockState(pos, withFace(state, direction, false), 2);
            }
        }

        @Nullable
        private static EnumFacing directionTo(BlockPos pos, BlockPos other) {
            int dx = other.getX() - pos.getX();
            int dy = other.getY() - pos.getY();
            int dz = other.getZ() - pos.getZ();
            for (EnumFacing direction : EnumFacing.values()) {
                if (direction.getFrontOffsetX() == dx && direction.getFrontOffsetY() == dy
                        && direction.getFrontOffsetZ() == dz) {
                    return direction;
                }
            }
            return null;
        }

        @Override
        public Item getItemDropped(IBlockState state, Random random, int fortune) {
            return Items.AIR;
        }

        @Override
        protected boolean canSilkHarvest() {
            return true;
        }

        @Override
        protected ItemStack getSilkTouchDrop(IBlockState state) {
            Item item = ForgeRegistries.ITEMS.getValue(CncRegistryIds.MUSHROOM_STEM);
            return item == null ? ItemStack.EMPTY : new ItemStack(item);
        }

        @Override
        public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world,
                BlockPos pos, EntityPlayer player) {
            return getSilkTouchDrop(state);
        }

        @Override
        public IBlockState withRotation(IBlockState state, Rotation rotation) {
            IBlockState rotated = state;
            for (EnumFacing direction : EnumFacing.HORIZONTALS) {
                rotated = rotated.withProperty(horizontalProperty(rotation.rotate(direction)),
                        state.getValue(horizontalProperty(direction)));
            }
            return rotated;
        }

        @Override
        public IBlockState withMirror(IBlockState state, Mirror mirror) {
            IBlockState mirrored = state;
            for (EnumFacing direction : EnumFacing.HORIZONTALS) {
                mirrored = mirrored.withProperty(horizontalProperty(mirror.mirror(direction)),
                        state.getValue(horizontalProperty(direction)));
            }
            return mirrored;
        }
    }

}
