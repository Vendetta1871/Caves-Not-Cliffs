package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.world.ILockableContainer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/** Hidden 1.12 storage blocks for Java 1.18's independently single dungeon chests. */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID)
public final class DungeonChestContent {
    public static final PropertyEnum<ChestPart> CHEST_PART =
            PropertyEnum.create("type", ChestPart.class);
    private static final ThreadLocal<ChestClick> CHEST_CLICK = new ThreadLocal<>();

    private DungeonChestContent() {
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(
                block(CncRegistryIds.DUNGEON_CHEST_FIRST),
                block(CncRegistryIds.DUNGEON_CHEST_SECOND));
    }

    public static void registerTileEntity() {
        GameRegistry.registerTileEntity(SingleChestTileEntity.class,
                CncRegistryIds.DUNGEON_CHEST_TILE);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerBuiltInModels(ModelRegistryEvent event) {
        Block first = ForgeRegistries.BLOCKS.getValue(CncRegistryIds.DUNGEON_CHEST_FIRST);
        Block second = ForgeRegistries.BLOCKS.getValue(CncRegistryIds.DUNGEON_CHEST_SECOND);
        Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes()
                .registerBuiltInBlocks(first, second);
    }

    public static IBlockState stateForOrdinal(int ordinal) {
        net.minecraft.util.ResourceLocation id = ordinal == 0
                ? CncRegistryIds.DUNGEON_CHEST_FIRST
                : CncRegistryIds.DUNGEON_CHEST_SECOND;
        Block block = ForgeRegistries.BLOCKS.getValue(id);
        if (!(block instanceof DungeonChestBlock)) {
            throw new IllegalStateException("Dungeon chest block was not registered: " + id);
        }
        return block.getDefaultState().withProperty(BlockChest.FACING, EnumFacing.NORTH)
                .withProperty(CHEST_PART, ChestPart.SINGLE);
    }

    public static boolean isChest(Block block) {
        if (block == Blocks.CHEST) {
            return true;
        }
        net.minecraft.util.ResourceLocation id = block.getRegistryName();
        return CncRegistryIds.DUNGEON_CHEST_FIRST.equals(id)
                || CncRegistryIds.DUNGEON_CHEST_SECOND.equals(id);
    }

    public static boolean isHiddenDungeonChest(Block block) {
        net.minecraft.util.ResourceLocation id = block.getRegistryName();
        return CncRegistryIds.DUNGEON_CHEST_FIRST.equals(id)
                || CncRegistryIds.DUNGEON_CHEST_SECOND.equals(id);
    }

    /** Lets a player-placed vanilla chest form its canonical double beside generated storage. */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public static void onHiddenChestClicked(PlayerInteractEvent.RightClickBlock event) {
        CHEST_CLICK.remove();
        EnumFacing face = event.getFace();
        if (event.getWorld().isRemote || !event.getEntityPlayer().isSneaking()
                || face == null || face.getAxis().isVertical()
                || !isHiddenDungeonChest(event.getWorld().getBlockState(
                        event.getPos()).getBlock())) {
            return;
        }
        CHEST_CLICK.set(new ChestClick(event.getEntityPlayer(), event.getHand(),
                event.getPos(), face));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public static void onChestPlaced(BlockEvent.PlaceEvent event) {
        ChestClick click = CHEST_CLICK.get();
        CHEST_CLICK.remove();
        World world = event.getWorld();
        IBlockState placed = event.getPlacedBlock();
        if (world.isRemote || placed.getBlock() != Blocks.CHEST) {
            return;
        }
        BlockPos hiddenPos;
        EnumFacing facing;
        ChestPart newPart;
        boolean sneaking = event.getPlayer() != null && event.getPlayer().isSneaking();
        if (sneaking) {
            if (click == null || !click.matches(event)) {
                return;
            }
            hiddenPos = click.pos;
            IBlockState hidden = world.getBlockState(hiddenPos);
            if (!isEligibleSingle(hidden)
                    || click.face.getAxis() == hidden.getValue(BlockChest.FACING).getAxis()) {
                return;
            }
            facing = hidden.getValue(BlockChest.FACING);
            EnumFacing fromNewToHidden = click.face.getOpposite();
            newPart = facing.rotateYCCW() == fromNewToHidden
                    ? ChestPart.RIGHT : ChestPart.LEFT;
        } else {
            facing = placed.getValue(BlockChest.FACING);
            EnumFacing clockwise = facing.rotateY();
            hiddenPos = event.getPos().offset(clockwise);
            if (eligibleSingle(world, hiddenPos, facing)) {
                newPart = ChestPart.LEFT;
            } else {
                hiddenPos = event.getPos().offset(facing.rotateYCCW());
                if (!eligibleSingle(world, hiddenPos, facing)) {
                    return;
                }
                newPart = ChestPart.RIGHT;
            }
        }
        if (!convertToHiddenDouble(world, event.getPos(), placed, hiddenPos,
                facing, newPart)) {
            event.setCanceled(true);
        }
    }

    private static boolean eligibleSingle(World world, BlockPos pos, EnumFacing facing) {
        IBlockState state = world.getBlockState(pos);
        return isEligibleSingle(state) && state.getValue(BlockChest.FACING) == facing;
    }

    private static boolean isEligibleSingle(IBlockState state) {
        return isHiddenDungeonChest(state.getBlock())
                && state.getValue(CHEST_PART) == ChestPart.SINGLE;
    }

    private static final class ChestClick {
        private final EntityPlayer player;
        private final net.minecraft.util.EnumHand hand;
        private final BlockPos pos;
        private final EnumFacing face;

        private ChestClick(EntityPlayer player, net.minecraft.util.EnumHand hand,
                BlockPos pos, EnumFacing face) {
            this.player = player;
            this.hand = hand;
            this.pos = pos.toImmutable();
            this.face = face;
        }

        private boolean matches(BlockEvent.PlaceEvent event) {
            return player == event.getPlayer() && hand == event.getHand()
                    && event.getPos().equals(pos.offset(face));
        }
    }

    private static boolean convertToHiddenDouble(World world, BlockPos placedPos,
            IBlockState placedState, BlockPos hiddenPos, EnumFacing facing,
            ChestPart placedPart) {
        IBlockState hiddenState = world.getBlockState(hiddenPos);
        TileEntity placedTile = world.getTileEntity(placedPos);
        TileEntity hiddenTile = world.getTileEntity(hiddenPos);
        if (!isEligibleSingle(hiddenState) || !(placedTile instanceof TileEntityChest)
                || !(hiddenTile instanceof TileEntityChest)) {
            return false;
        }
        NBTTagCompound placedNbt = placedTile.writeToNBT(new NBTTagCompound());
        NBTTagCompound hiddenNbt = hiddenTile.writeToNBT(new NBTTagCompound());
        IBlockState convertedPlaced = oppositeStorageState(hiddenState, facing, placedPart);
        IBlockState convertedHidden = hiddenState.withProperty(BlockChest.FACING, facing)
                .withProperty(CHEST_PART, placedPart.opposite());

        world.removeTileEntity(placedPos);
        if (!world.setBlockState(placedPos, convertedPlaced, 3)) {
            restoreChest(world, placedPos, placedState, placedNbt);
            return false;
        }
        TileEntity convertedPlacedTile = ensureChestTile(world, placedPos,
                convertedPlaced);
        if (!(convertedPlacedTile instanceof TileEntityChest)) {
            restoreChest(world, placedPos, placedState, placedNbt);
            return false;
        }
        convertedPlacedTile.readFromNBT(placedNbt);
        convertedPlacedTile.markDirty();
        if (!world.setBlockState(hiddenPos, convertedHidden, 3)) {
            restoreChest(world, placedPos, placedState, placedNbt);
            return false;
        }
        TileEntity convertedHiddenTile = ensureChestTile(world, hiddenPos,
                convertedHidden);
        convertedHiddenTile.readFromNBT(hiddenNbt);
        convertedHiddenTile.markDirty();
        convertedPlacedTile.updateContainingBlockInfo();
        convertedHiddenTile.updateContainingBlockInfo();
        return true;
    }

    private static IBlockState oppositeStorageState(IBlockState hiddenState,
            EnumFacing facing, ChestPart part) {
        ResourceLocation id = CncRegistryIds.DUNGEON_CHEST_FIRST.equals(
                hiddenState.getBlock().getRegistryName())
                ? CncRegistryIds.DUNGEON_CHEST_SECOND
                : CncRegistryIds.DUNGEON_CHEST_FIRST;
        Block block = ForgeRegistries.BLOCKS.getValue(id);
        return block.getDefaultState().withProperty(BlockChest.FACING, facing)
                .withProperty(CHEST_PART, part);
    }

    private static TileEntity ensureChestTile(World world, BlockPos pos,
            IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityChest) {
            return tile;
        }
        tile = ((BlockChest) state.getBlock()).createNewTileEntity(
                world, state.getBlock().getMetaFromState(state));
        world.setTileEntity(pos, tile);
        return tile;
    }

    private static void restoreChest(World world, BlockPos pos, IBlockState state,
            NBTTagCompound chestNbt) {
        world.removeTileEntity(pos);
        world.setBlockState(pos, state, 3);
        TileEntity restored = ensureChestTile(world, pos, state);
        restored.readFromNBT(chestNbt);
        restored.markDirty();
    }

    private static DungeonChestBlock block(net.minecraft.util.ResourceLocation id) {
        return (DungeonChestBlock) new DungeonChestBlock().setRegistryName(id);
    }

    private static final class DungeonChestBlock extends BlockChest {
        private DungeonChestBlock() {
            super(Type.BASIC);
            setDefaultState(blockState.getBaseState()
                    .withProperty(FACING, EnumFacing.NORTH)
                    .withProperty(CHEST_PART, ChestPart.SINGLE));
            setUnlocalizedName("chest");
            setHardness(2.5F);
            setSoundType(SoundType.WOOD);
            setCreativeTab(null);
        }

        @Override
        public IBlockState checkForSurroundingChests(World world, BlockPos pos,
                IBlockState state) {
            return state;
        }

        @Override
        public void neighborChanged(IBlockState state, World world, BlockPos pos,
                Block changedBlock, BlockPos changedPos) {
            if (state.getValue(CHEST_PART) != ChestPart.SINGLE
                    && matchingNeighbor(world, pos, state) == null) {
                world.setBlockState(pos, state.withProperty(
                        CHEST_PART, ChestPart.SINGLE), 3);
                TileEntity tile = world.getTileEntity(pos);
                if (tile != null) {
                    tile.updateContainingBlockInfo();
                }
                return;
            }
            super.neighborChanged(state, world, pos, changedBlock, changedPos);
        }

        @Override
        public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world,
                BlockPos pos) {
            EnumFacing connected = connectionDirection(state);
            if (connected == null || matchingNeighbor(world, pos, state) == null) {
                return NOT_CONNECTED_AABB;
            }
            switch (connected) {
                case NORTH: return NORTH_CHEST_AABB;
                case SOUTH: return SOUTH_CHEST_AABB;
                case WEST: return WEST_CHEST_AABB;
                case EAST: return EAST_CHEST_AABB;
                default: return NOT_CONNECTED_AABB;
            }
        }

        @Override
        public Item getItemDropped(IBlockState state, Random random, int fortune) {
            return Item.getItemFromBlock(Blocks.CHEST);
        }

        @Override
        public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
            return new ItemStack(Blocks.CHEST);
        }

        @Override
        public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world,
                BlockPos pos, EntityPlayer player) {
            return new ItemStack(Blocks.CHEST);
        }

        @Override
        public TileEntity createNewTileEntity(World world, int meta) {
            return new SingleChestTileEntity();
        }

        @Override
        public ILockableContainer getContainer(World world, BlockPos pos,
                boolean ignoreBlocked) {
            TileEntity tileEntity = world.getTileEntity(pos);
            if (!(tileEntity instanceof TileEntityChest)) {
                return null;
            }
            if (!ignoreBlocked && isBlocked(world, pos)) {
                return null;
            }
            IBlockState state = world.getBlockState(pos);
            BlockPos neighborPos = matchingNeighbor(world, pos, state);
            if (neighborPos == null) {
                return (TileEntityChest) tileEntity;
            }
            TileEntity neighbor = world.getTileEntity(neighborPos);
            if (!(neighbor instanceof TileEntityChest)) {
                return (TileEntityChest) tileEntity;
            }
            if (!ignoreBlocked && isBlocked(world, neighborPos)) {
                return null;
            }
            EnumFacing connected = connectionDirection(state);
            return connected == EnumFacing.WEST || connected == EnumFacing.NORTH
                    ? new InventoryLargeChest("container.chestDouble",
                            (TileEntityChest) neighbor, (TileEntityChest) tileEntity)
                    : new InventoryLargeChest("container.chestDouble",
                            (TileEntityChest) tileEntity, (TileEntityChest) neighbor);
        }

        @Override
        public IBlockState getStateFromMeta(int meta) {
            EnumFacing facing = EnumFacing.getHorizontal(meta & 3);
            ChestPart part = ChestPart.byIndex(meta / 4);
            return getDefaultState().withProperty(FACING, facing)
                    .withProperty(CHEST_PART, part);
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            return state.getValue(FACING).getHorizontalIndex()
                    | state.getValue(CHEST_PART).index * 4;
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return new BlockStateContainer(this, FACING, CHEST_PART);
        }

        private static boolean isBlocked(World world, BlockPos pos) {
            return world.getBlockState(pos.up()).doesSideBlockChestOpening(
                    world, pos.up(), EnumFacing.DOWN) || hasSittingOcelot(world, pos);
        }

        private static boolean hasSittingOcelot(World world, BlockPos pos) {
            AxisAlignedBB box = new AxisAlignedBB(pos.getX(), pos.getY() + 1,
                    pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1);
            for (EntityOcelot ocelot : world.getEntitiesWithinAABB(EntityOcelot.class, box)) {
                if (ocelot.isSitting()) {
                    return true;
                }
            }
            return false;
        }
    }

    private static EnumFacing connectionDirection(IBlockState state) {
        ChestPart part = state.getValue(CHEST_PART);
        if (part == ChestPart.SINGLE) {
            return null;
        }
        EnumFacing facing = state.getValue(BlockChest.FACING);
        return part == ChestPart.LEFT ? facing.rotateY() : facing.rotateYCCW();
    }

    private static BlockPos matchingNeighbor(IBlockAccess world, BlockPos pos,
            IBlockState state) {
        EnumFacing direction = connectionDirection(state);
        if (direction == null) {
            return null;
        }
        BlockPos neighborPos = pos.offset(direction);
        IBlockState neighbor = world.getBlockState(neighborPos);
        return isHiddenDungeonChest(neighbor.getBlock())
                && neighbor.getValue(BlockChest.FACING) == state.getValue(BlockChest.FACING)
                && neighbor.getValue(CHEST_PART) == state.getValue(CHEST_PART).opposite()
                ? neighborPos : null;
    }

    public enum ChestPart implements IStringSerializable {
        SINGLE(0, "single"),
        LEFT(1, "left"),
        RIGHT(2, "right");

        private final int index;
        private final String name;

        ChestPart(int index, String name) {
            this.index = index;
            this.name = name;
        }

        private ChestPart opposite() {
            return this == LEFT ? RIGHT : this == RIGHT ? LEFT : SINGLE;
        }

        private static ChestPart byIndex(int index) {
            return index == 1 ? LEFT : index == 2 ? RIGHT : SINGLE;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    /** Vanilla chest inventory/rendering joined only by persisted complementary states. */
    public static final class SingleChestTileEntity extends TileEntityChest {
        public SingleChestTileEntity() {
            super(BlockChest.Type.BASIC);
        }

        @Override
        protected TileEntityChest getAdjacentChest(EnumFacing side) {
            if (world == null) {
                return null;
            }
            IBlockState state = world.getBlockState(pos);
            BlockPos neighborPos = matchingNeighbor(world, pos, state);
            if (neighborPos == null || !neighborPos.equals(pos.offset(side))) {
                return null;
            }
            TileEntity tile = world.getTileEntity(neighborPos);
            return tile instanceof TileEntityChest ? (TileEntityChest) tile : null;
        }
    }
}
