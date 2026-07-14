package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.content.CncBlockProperties;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.celestiald.cavesnotcliffs.stonecutter.CncGuiHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatBasic;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Functional Java 1.18.2 stonecutter block and GUI entry point. */
@ElementsCavesNotCliffs.ModElement.Tag
public final class BlockStonecutter extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:stonecutter")
    public static final Block block = null;

    public static final SoundEvent SELECT_RECIPE_SOUND =
            sound("ui.stonecutter.select_recipe");
    public static final SoundEvent TAKE_RESULT_SOUND =
            sound("ui.stonecutter.take_result");
    public static final StatBase INTERACT_STAT = new StatBasic(
            "stat.interactWithStonecutter",
            new TextComponentTranslation("stat.interactWithStonecutter")).registerStat();

    public BlockStonecutter(ElementsCavesNotCliffs elements) {
        super(elements, 331);
    }

    @Override
    public void initElements() {
        ElementsCavesNotCliffs.sounds.put(SELECT_RECIPE_SOUND.getRegistryName(),
                SELECT_RECIPE_SOUND);
        ElementsCavesNotCliffs.sounds.put(TAKE_RESULT_SOUND.getRegistryName(),
                TAKE_RESULT_SOUND);
        elements.blocks.add(() -> new BlockCustom()
                .setRegistryName(CncRegistryIds.STONECUTTER));
        elements.items.add(() -> new ItemBlock(block)
                .setRegistryName(CncRegistryIds.STONECUTTER));
    }

    private static SoundEvent sound(String path) {
        net.minecraft.util.ResourceLocation id = CncRegistryIds.id(path);
        return new SoundEvent(id).setRegistryName(id);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        Item item = Item.getItemFromBlock(block);
        ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation(CncRegistryIds.STONECUTTER, "inventory"));
    }

    public static final class BlockCustom extends BlockHorizontal {
        public static final AxisAlignedBB SHAPE =
                new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 9.0D / 16.0D, 1.0D);

        public BlockCustom() {
            super(Material.ROCK);
            setUnlocalizedName("stonecutter");
            setCreativeTab(CreativeTabs.DECORATIONS);
            setSoundType(SoundType.STONE);
            setHardness(3.5F);
            setResistance(CncBlockProperties.legacyResistance(3.5F));
            setHarvestLevel("pickaxe", 0);
            setDefaultState(blockState.getBaseState().withProperty(FACING,
                    EnumFacing.NORTH));
            useNeighborBrightness = true;
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return new BlockStateContainer(this, FACING);
        }

        @Override
        public IBlockState getStateForPlacement(World world, BlockPos pos,
                EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
                EntityLivingBase placer, EnumHand hand) {
            return getDefaultState().withProperty(FACING,
                    placer.getHorizontalFacing().getOpposite());
        }

        @Override
        public IBlockState getStateFromMeta(int meta) {
            EnumFacing facing = EnumFacing.getHorizontal(meta);
            return getDefaultState().withProperty(FACING, facing);
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            return state.getValue(FACING).getHorizontalIndex();
        }

        @Override
        public IBlockState withRotation(IBlockState state, Rotation rotation) {
            return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
        }

        @Override
        public IBlockState withMirror(IBlockState state, Mirror mirror) {
            return state.withRotation(mirror.toRotation(state.getValue(FACING)));
        }

        @Override
        public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source,
                BlockPos pos) {
            return SHAPE;
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
        public EnumBlockRenderType getRenderType(IBlockState state) {
            return EnumBlockRenderType.MODEL;
        }

        @Override
        public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state,
                BlockPos pos, EnumFacing face) {
            return face == EnumFacing.DOWN ? BlockFaceShape.SOLID
                    : BlockFaceShape.UNDEFINED;
        }

        @Override
        public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX,
                float hitY, float hitZ) {
            if (!world.isRemote) {
                player.openGui(CavesNotCliffs.instance, CncGuiHandler.STONECUTTER_GUI,
                        world, pos.getX(), pos.getY(), pos.getZ());
                player.addStat(INTERACT_STAT);
            }
            return true;
        }
    }

}
