package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.init.Items;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/** Canonical Java 1.18.2 blue-ice block used by frozen-ocean decoration. */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID)
public final class BlueIceContent {
    private static final ResourceLocation ID =
        new ResourceLocation(CavesNotCliffs.MODID, "blue_ice");

    @GameRegistry.ObjectHolder("cavesnotcliffs:blue_ice")
    public static final Block BLUE_ICE = null;

    private BlueIceContent() {
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(new BlueIceBlock().setRegistryName(ID));
    }

    private static final class BlueIceBlock extends Block {
        private BlueIceBlock() {
            super(Material.PACKED_ICE, MapColor.ICE);
            setUnlocalizedName("blue_ice");
            setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
            setHardness(2.8F);
            setResistance(CncBlockProperties.legacyResistance(2.8F));
            setSoundType(SoundType.GLASS);
            slipperiness = 0.989F;
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
        public boolean isOpaqueCube(IBlockState state) {
            return false;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public BlockRenderLayer getBlockLayer() {
            return BlockRenderLayer.TRANSLUCENT;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public boolean shouldSideBeRendered(IBlockState state, IBlockAccess world,
                BlockPos pos, EnumFacing side) {
            return world.getBlockState(pos.offset(side)).getBlock() != this
                && super.shouldSideBeRendered(state, world, pos, side);
        }
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        Block block = ForgeRegistries.BLOCKS.getValue(ID);
        if (block == null) {
            throw new IllegalStateException("Blue ice block was not registered before its item");
        }
        event.getRegistry().register(new ItemBlock(block).setRegistryName(ID));
    }

    @SideOnly(Side.CLIENT)
    @Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID, value = Side.CLIENT)
    public static final class Models {
        private Models() {
        }

        @SubscribeEvent
        public static void registerModels(ModelRegistryEvent event) {
            Item item = ForgeRegistries.ITEMS.getValue(ID);
            if (item == null) {
                throw new IllegalStateException("Blue ice item was not registered");
            }
            ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation(ID, "inventory"));
        }
    }
}
