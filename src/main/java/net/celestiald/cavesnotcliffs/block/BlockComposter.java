package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.content.ComposterCompostables;
import net.celestiald.cavesnotcliffs.content.ComposterMechanics;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.celestiald.cavesnotcliffs.tile.TileEntityComposter;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/** Functional Java 1.18.2 composter with a 1.12 sided-inventory automation bridge. */
@ElementsCavesNotCliffs.ModElement.Tag
public final class BlockComposter extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:composter")
    public static final Block block = null;

    public BlockComposter(ElementsCavesNotCliffs elements) {
        super(elements, 330);
    }

    @Override
    public void initElements() {
        elements.blocks.add(() -> new BlockCustom()
            .setRegistryName(CncRegistryIds.COMPOSTER));
        elements.items.add(() -> new ItemBlock(block)
            .setRegistryName(CncRegistryIds.COMPOSTER));
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        GameRegistry.registerTileEntity(TileEntityComposter.class,
            CncRegistryIds.COMPOSTER);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        Item item = Item.getItemFromBlock(block);
        ModelLoader.setCustomModelResourceLocation(item, 0,
            new ModelResourceLocation(CncRegistryIds.COMPOSTER, "inventory"));
    }

    public static final class BlockCustom extends Block {
        public static final PropertyInteger LEVEL = PropertyInteger.create("level", 0, 8);

        private static final AxisAlignedBB BOTTOM =
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.125D, 1.0D);
        private static final AxisAlignedBB WEST =
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 0.125D, 1.0D, 1.0D);
        private static final AxisAlignedBB EAST =
            new AxisAlignedBB(0.875D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
        private static final AxisAlignedBB NORTH =
            new AxisAlignedBB(0.125D, 0.0D, 0.0D, 0.875D, 1.0D, 0.125D);
        private static final AxisAlignedBB SOUTH =
            new AxisAlignedBB(0.125D, 0.0D, 0.875D, 0.875D, 1.0D, 1.0D);

        public BlockCustom() {
            super(Material.WOOD);
            setUnlocalizedName("composter");
            setHardness(0.6F);
            setSoundType(SoundType.WOOD);
            setLightOpacity(0);
            setDefaultState(blockState.getBaseState().withProperty(LEVEL, 0));
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return new BlockStateContainer(this, LEVEL);
        }

        @Override
        public IBlockState getStateFromMeta(int meta) {
            return getDefaultState().withProperty(LEVEL,
                Math.max(0, Math.min(8, meta)));
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean hasTileEntity(IBlockState state) {
            return true;
        }

        @Nullable
        @Override
        public TileEntity createTileEntity(World world, IBlockState state) {
            return new TileEntityComposter();
        }

        @Override
        public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX,
                float hitY, float hitZ) {
            int level = state.getValue(LEVEL);
            ItemStack held = player.getHeldItem(hand);
            float chance = ComposterCompostables.chance(held);
            if (level < ComposterMechanics.READY_LEVEL && chance >= 0.0F) {
                if (level < ComposterMechanics.MAX_FILL_LEVEL && !world.isRemote) {
                    boolean success = addItem(world, pos, state, chance);
                    playFillFeedback(world, pos, success);
                    StatBase stat = StatList.getObjectUseStats(held.getItem());
                    if (stat != null) {
                        player.addStat(stat);
                    }
                    if (!player.capabilities.isCreativeMode) {
                        held.shrink(1);
                    }
                }
                return true;
            }
            if (level == ComposterMechanics.READY_LEVEL) {
                if (!world.isRemote) {
                    extractProduce(world, pos, state, true);
                }
                return true;
            }
            return false;
        }

        /** Consumes the RNG decision and returns whether the level increased. */
        public static boolean addItem(World world, BlockPos pos, IBlockState state,
                float chance) {
            int oldLevel = state.getValue(LEVEL);
            int newLevel = ComposterMechanics.addItem(oldLevel, chance,
                world.rand.nextDouble());
            if (newLevel == oldLevel) {
                return false;
            }
            IBlockState changed = state.withProperty(LEVEL, newLevel);
            world.setBlockState(pos, changed, 3);
            if (newLevel == ComposterMechanics.MAX_FILL_LEVEL) {
                world.scheduleUpdate(pos, state.getBlock(),
                    ComposterMechanics.READY_DELAY_TICKS);
            }
            return true;
        }

        public static void playFillFeedback(World world, BlockPos pos, boolean success) {
            world.playSound(null, pos,
                success ? SoundEvents.BLOCK_GRASS_PLACE : SoundEvents.BLOCK_SAND_PLACE,
                SoundCategory.BLOCKS, success ? 1.0F : 0.3F,
                success ? 1.0F : 0.8F);
            if (world instanceof WorldServer) {
                ((WorldServer) world).spawnParticle(EnumParticleTypes.BLOCK_CRACK,
                    pos.getX() + 0.5D, pos.getY() + 0.7D, pos.getZ() + 0.5D,
                    10, 0.36D, 0.2D, 0.36D, 0.02D,
                    Block.getStateId(Blocks.DIRT.getDefaultState()));
            }
        }

        public static ItemStack extractProduce(World world, BlockPos pos,
                IBlockState state, boolean spawnItem) {
            if (state.getValue(LEVEL) != ComposterMechanics.READY_LEVEL) {
                return ItemStack.EMPTY;
            }
            ItemStack boneMeal = new ItemStack(Items.DYE, 1, 15);
            if (spawnItem) {
                double x = pos.getX() + world.rand.nextFloat() * 0.7F + 0.15F;
                double y = pos.getY() + world.rand.nextFloat() * 0.7F + 0.66F;
                double z = pos.getZ() + world.rand.nextFloat() * 0.7F + 0.15F;
                EntityItem entity = new EntityItem(world, x, y, z, boneMeal.copy());
                entity.setDefaultPickupDelay();
                world.spawnEntity(entity);
            }
            world.setBlockState(pos, state.withProperty(LEVEL, 0), 3);
            if (spawnItem) {
                world.playSound(null, pos, SoundEvents.BLOCK_WOOD_BREAK,
                    SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
            return boneMeal;
        }

        @Override
        public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
            if (!world.isRemote
                    && state.getValue(LEVEL) == ComposterMechanics.MAX_FILL_LEVEL) {
                world.setBlockState(pos, state.withProperty(LEVEL,
                    ComposterMechanics.READY_LEVEL), 3);
                world.playSound(null, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                    SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
        }

        @Override
        public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
                AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes,
                @Nullable Entity entity, boolean actualState) {
            addCollisionBoxToList(pos, entityBox, collidingBoxes, BOTTOM);
            addCollisionBoxToList(pos, entityBox, collidingBoxes, WEST);
            addCollisionBoxToList(pos, entityBox, collidingBoxes, EAST);
            addCollisionBoxToList(pos, entityBox, collidingBoxes, NORTH);
            addCollisionBoxToList(pos, entityBox, collidingBoxes, SOUTH);
        }

        @Override
        public boolean isOpaqueCube(IBlockState state) {
            return false;
        }

        @Override
        public boolean isFullCube(IBlockState state) {
            return false;
        }

        @Override
        public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state,
                BlockPos pos, EnumFacing face) {
            return BlockFaceShape.UNDEFINED;
        }

        @Override
        public boolean hasComparatorInputOverride(IBlockState state) {
            return true;
        }

        @Override
        public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
            return ComposterMechanics.comparatorOutput(state.getValue(LEVEL));
        }

        @Override
        public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos,
                IBlockState state, int fortune) {
            super.getDrops(drops, world, pos, state, fortune);
            if (state.getValue(LEVEL) == ComposterMechanics.READY_LEVEL) {
                drops.add(new ItemStack(Items.DYE, 1, 15));
            }
        }
    }
}
