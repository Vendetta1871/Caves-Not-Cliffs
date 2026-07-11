package net.celestiald.cavesnotcliffs.block;

import net.minecraft.block.BlockGlass;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/** Transparent rendering with the full light blocking and self-drop of 1.18 tinted glass. */
public final class BlockTintedGlass extends BlockGlass {
    public BlockTintedGlass() {
        super(Material.GLASS, false);
        setUnlocalizedName("tinted_glass");
        setHardness(0.3F);
        setResistance(0.5F);
        setLightOpacity(255);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        Item item = Item.getItemFromBlock(this);
        return item == null ? Items.AIR : item;
    }

    @Override
    public int quantityDropped(Random random) {
        return 1;
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
        return MapColor.GRAY;
    }

    @Override
    protected boolean canSilkHarvest() {
        return true;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.TRANSLUCENT;
    }
}
