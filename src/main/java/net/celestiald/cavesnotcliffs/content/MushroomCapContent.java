package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHugeMushroom;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Random;

/** Lossless hidden storage for post-flattening red and brown mushroom-cap states. */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID)
public final class MushroomCapContent {
    private MushroomCapContent() {
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(
                cap(CncRegistryIds.RED_MUSHROOM_CAP_NO_VERTICAL,
                        Blocks.RED_MUSHROOM_BLOCK, false, false),
                cap(CncRegistryIds.RED_MUSHROOM_CAP_UP,
                        Blocks.RED_MUSHROOM_BLOCK, true, false),
                cap(CncRegistryIds.RED_MUSHROOM_CAP_DOWN,
                        Blocks.RED_MUSHROOM_BLOCK, false, true),
                cap(CncRegistryIds.RED_MUSHROOM_CAP_UP_DOWN,
                        Blocks.RED_MUSHROOM_BLOCK, true, true),
                cap(CncRegistryIds.BROWN_MUSHROOM_CAP_NO_VERTICAL,
                        Blocks.BROWN_MUSHROOM_BLOCK, false, false),
                cap(CncRegistryIds.BROWN_MUSHROOM_CAP_UP,
                        Blocks.BROWN_MUSHROOM_BLOCK, true, false),
                cap(CncRegistryIds.BROWN_MUSHROOM_CAP_DOWN,
                        Blocks.BROWN_MUSHROOM_BLOCK, false, true),
                cap(CncRegistryIds.BROWN_MUSHROOM_CAP_UP_DOWN,
                        Blocks.BROWN_MUSHROOM_BLOCK, true, true));
    }

    private static HiddenMushroomCapBlock cap(net.minecraft.util.ResourceLocation id,
            Block canonical, boolean up, boolean down) {
        return (HiddenMushroomCapBlock) new HiddenMushroomCapBlock(
                canonical, up, down).setRegistryName(id);
    }

    public static IBlockState state(Block canonical, boolean north, boolean east,
            boolean south, boolean west, boolean up, boolean down) {
        if (north && east && south && west && up && down) {
            return canonical.getDefaultState();
        }
        return hiddenState(canonical, north, east, south, west, up, down);
    }

    private static IBlockState hiddenState(Block canonical, boolean north, boolean east,
            boolean south, boolean west, boolean up, boolean down) {
        Block block = ForgeRegistries.BLOCKS.getValue(hiddenId(canonical, up, down));
        if (!(block instanceof HiddenMushroomCapBlock)) {
            throw new IllegalStateException("Hidden mushroom cap was not registered: "
                    + hiddenId(canonical, up, down));
        }
        return block.getDefaultState()
                .withProperty(MushroomStemContent.NORTH, north)
                .withProperty(MushroomStemContent.EAST, east)
                .withProperty(MushroomStemContent.SOUTH, south)
                .withProperty(MushroomStemContent.WEST, west);
    }

    public static IBlockState decodePublic(IBlockState state) {
        Block canonical = state.getBlock();
        if (canonical != Blocks.RED_MUSHROOM_BLOCK
                && canonical != Blocks.BROWN_MUSHROOM_BLOCK) {
            return null;
        }
        BlockHugeMushroom.EnumType variant = state.getValue(BlockHugeMushroom.VARIANT);
        switch (variant) {
            case ALL_OUTSIDE:
                return hiddenState(canonical, true, true, true, true, true, true);
            case ALL_INSIDE:
                return hiddenState(canonical, false, false, false, false, false, false);
            case NORTH_WEST:
                return hiddenState(canonical, true, false, false, true, true, false);
            case NORTH:
                return hiddenState(canonical, true, false, false, false, true, false);
            case NORTH_EAST:
                return hiddenState(canonical, true, true, false, false, true, false);
            case WEST:
                return hiddenState(canonical, false, false, false, true, true, false);
            case CENTER:
                return hiddenState(canonical, false, false, false, false, true, false);
            case EAST:
                return hiddenState(canonical, false, true, false, false, true, false);
            case SOUTH_WEST:
                return hiddenState(canonical, false, false, true, true, true, false);
            case SOUTH:
                return hiddenState(canonical, false, false, true, false, true, false);
            case SOUTH_EAST:
                return hiddenState(canonical, false, true, true, false, true, false);
            default:
                // STEM and ALL_STEM are genuine legacy states, not cap-face states.
                return null;
        }
    }

    public static IBlockState publicPlacement(BlockHugeMushroom canonical,
            World world, BlockPos pos) {
        IBlockState result = canonical.getDefaultState();
        for (EnumFacing face : EnumFacing.values()) {
            if (isSameFamily(world.getBlockState(pos.offset(face)), canonical)) {
                result = withFace(result, face, false);
            }
        }
        return result;
    }

    public static void publicNeighborChanged(BlockHugeMushroom canonical,
            IBlockState current, World world, BlockPos pos, BlockPos fromPos) {
        IBlockState decoded = decodePublic(current);
        EnumFacing face = directionTo(pos, fromPos);
        if (decoded == null || face == null
                || !isSameFamily(world.getBlockState(fromPos), canonical)) {
            return;
        }
        world.setBlockState(pos, withFace(decoded, face, false), 2);
    }

    private static boolean isSameFamily(IBlockState state, Block canonical) {
        Block block = state.getBlock();
        return block == canonical || block instanceof HiddenMushroomCapBlock
                && ((HiddenMushroomCapBlock) block).canonical == canonical;
    }

    private static IBlockState withFace(IBlockState source, EnumFacing face,
            boolean visible) {
        Block canonical;
        boolean north;
        boolean east;
        boolean south;
        boolean west;
        boolean up;
        boolean down;
        if (source.getBlock() instanceof HiddenMushroomCapBlock) {
            HiddenMushroomCapBlock block = (HiddenMushroomCapBlock) source.getBlock();
            canonical = block.canonical;
            north = source.getValue(MushroomStemContent.NORTH);
            east = source.getValue(MushroomStemContent.EAST);
            south = source.getValue(MushroomStemContent.SOUTH);
            west = source.getValue(MushroomStemContent.WEST);
            up = block.up;
            down = block.down;
        } else {
            IBlockState decoded = decodePublic(source);
            if (decoded == null) {
                throw new IllegalArgumentException("Not a mushroom cap face state: " + source);
            }
            return withFace(decoded, face, visible);
        }
        switch (face) {
            case NORTH: north = visible; break;
            case EAST: east = visible; break;
            case SOUTH: south = visible; break;
            case WEST: west = visible; break;
            case UP: up = visible; break;
            case DOWN: down = visible; break;
            default: throw new AssertionError(face);
        }
        return state(canonical, north, east, south, west, up, down);
    }

    private static net.minecraft.util.ResourceLocation hiddenId(Block canonical,
            boolean up, boolean down) {
        boolean red = canonical == Blocks.RED_MUSHROOM_BLOCK;
        if (canonical != Blocks.RED_MUSHROOM_BLOCK
                && canonical != Blocks.BROWN_MUSHROOM_BLOCK) {
            throw new IllegalArgumentException("Not a canonical mushroom cap: " + canonical);
        }
        if (red) {
            if (up && down) return CncRegistryIds.RED_MUSHROOM_CAP_UP_DOWN;
            if (up) return CncRegistryIds.RED_MUSHROOM_CAP_UP;
            return down ? CncRegistryIds.RED_MUSHROOM_CAP_DOWN
                    : CncRegistryIds.RED_MUSHROOM_CAP_NO_VERTICAL;
        }
        if (up && down) return CncRegistryIds.BROWN_MUSHROOM_CAP_UP_DOWN;
        if (up) return CncRegistryIds.BROWN_MUSHROOM_CAP_UP;
        return down ? CncRegistryIds.BROWN_MUSHROOM_CAP_DOWN
                : CncRegistryIds.BROWN_MUSHROOM_CAP_NO_VERTICAL;
    }

    private static EnumFacing directionTo(BlockPos pos, BlockPos other) {
        int dx = other.getX() - pos.getX();
        int dy = other.getY() - pos.getY();
        int dz = other.getZ() - pos.getZ();
        for (EnumFacing face : EnumFacing.values()) {
            if (face.getFrontOffsetX() == dx && face.getFrontOffsetY() == dy
                    && face.getFrontOffsetZ() == dz) {
                return face;
            }
        }
        return null;
    }

    public static final class HiddenMushroomCapBlock extends Block {
        private final Block canonical;
        private final boolean up;
        private final boolean down;

        private HiddenMushroomCapBlock(Block canonical, boolean up, boolean down) {
            super(Material.WOOD, canonical == Blocks.RED_MUSHROOM_BLOCK
                    ? MapColor.RED : MapColor.DIRT);
            this.canonical = canonical;
            this.up = up;
            this.down = down;
            setHardness(0.2F);
            setResistance(CncBlockProperties.legacyResistance(0.2F));
            setSoundType(SoundType.WOOD);
            setDefaultState(blockState.getBaseState()
                    .withProperty(MushroomStemContent.NORTH, true)
                    .withProperty(MushroomStemContent.EAST, true)
                    .withProperty(MushroomStemContent.SOUTH, true)
                    .withProperty(MushroomStemContent.WEST, true));
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return new BlockStateContainer(this, MushroomStemContent.NORTH,
                    MushroomStemContent.EAST, MushroomStemContent.SOUTH,
                    MushroomStemContent.WEST);
        }

        @Override
        public IBlockState getStateFromMeta(int meta) {
            return getDefaultState()
                    .withProperty(MushroomStemContent.NORTH, (meta & 1) != 0)
                    .withProperty(MushroomStemContent.EAST, (meta & 2) != 0)
                    .withProperty(MushroomStemContent.SOUTH, (meta & 4) != 0)
                    .withProperty(MushroomStemContent.WEST, (meta & 8) != 0);
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            return (state.getValue(MushroomStemContent.NORTH) ? 1 : 0)
                    | (state.getValue(MushroomStemContent.EAST) ? 2 : 0)
                    | (state.getValue(MushroomStemContent.SOUTH) ? 4 : 0)
                    | (state.getValue(MushroomStemContent.WEST) ? 8 : 0);
        }

        @Override
        public void neighborChanged(IBlockState state, World world, BlockPos pos,
                Block neighbor, BlockPos fromPos) {
            EnumFacing face = directionTo(pos, fromPos);
            if (face != null && isSameFamily(world.getBlockState(fromPos), canonical)) {
                world.setBlockState(pos, withFace(state, face, false), 2);
            }
        }

        @Override
        public IBlockState withRotation(IBlockState state, Rotation rotation) {
            IBlockState result = state;
            for (EnumFacing face : EnumFacing.HORIZONTALS) {
                result = result.withProperty(MushroomStemContent.horizontalProperty(
                        rotation.rotate(face)), state.getValue(
                                MushroomStemContent.horizontalProperty(face)));
            }
            return result;
        }

        @Override
        public IBlockState withMirror(IBlockState state, Mirror mirror) {
            IBlockState result = state;
            for (EnumFacing face : EnumFacing.HORIZONTALS) {
                result = result.withProperty(MushroomStemContent.horizontalProperty(
                        mirror.mirror(face)), state.getValue(
                                MushroomStemContent.horizontalProperty(face)));
            }
            return result;
        }

        @Override
        public Item getItemDropped(IBlockState state, Random random, int fortune) {
            return Item.getItemFromBlock(canonical == Blocks.RED_MUSHROOM_BLOCK
                    ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM);
        }

        @Override
        public int quantityDropped(Random random) {
            return Math.max(0, random.nextInt(9) - 6);
        }

        @Override
        protected boolean canSilkHarvest() {
            return true;
        }

        @Override
        protected ItemStack getSilkTouchDrop(IBlockState state) {
            return new ItemStack(Item.getItemFromBlock(canonical));
        }

        @Override
        public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world,
                BlockPos pos, EntityPlayer player) {
            return getSilkTouchDrop(state);
        }
    }
}
