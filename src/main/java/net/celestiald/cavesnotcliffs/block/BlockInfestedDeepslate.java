package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.content.CncBlockProperties;
import net.celestiald.cavesnotcliffs.content.DeepslateSoundEvents;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/** Canonical infested-deepslate host used by the 1.18 mountain feature. */
@ElementsCavesNotCliffs.ModElement.Tag
public final class BlockInfestedDeepslate extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:infested_deepslate")
    public static final Block block = null;

    public BlockInfestedDeepslate(ElementsCavesNotCliffs elements) {
        super(elements, 332);
    }

    @Override
    public void initElements() {
        elements.blocks.add(() -> new BlockCustom()
            .setRegistryName(CncRegistryIds.INFESTED_DEEPSLATE));
        // Vanilla registers an item identity for infested blocks but leaves it off creative tabs.
        elements.items.add(() -> new ItemBlock(block)
            .setRegistryName(CncRegistryIds.INFESTED_DEEPSLATE));
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0,
            new ModelResourceLocation(CncRegistryIds.INFESTED_DEEPSLATE, "inventory"));
    }

    static final class BlockCustom extends BlockRotatedPillar {
        BlockCustom() {
            super(Material.CLAY, CncBlockProperties.DEEPSLATE);
            setUnlocalizedName("infested_deepslate");
            setSoundType(DeepslateSoundEvents.DEEPSLATE);
            setHardness(1.5F);
            setResistance(CncBlockProperties.legacyResistance(0.75F));
            setHarvestLevel("pickaxe", 0);
        }

        @Override
        public int quantityDropped(Random random) {
            return 0;
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
            return hostStack(state);
        }

        @Override
        public void dropBlockAsItemWithChance(World world, BlockPos pos, IBlockState state,
                float chance, int fortune) {
            if (!world.isRemote && world.getGameRules().getBoolean("doTileDrops")) {
                EntitySilverfish silverfish = new EntitySilverfish(world);
                silverfish.setLocationAndAngles(pos.getX() + 0.5D, pos.getY(),
                    pos.getZ() + 0.5D, 0.0F, 0.0F);
                world.spawnEntity(silverfish);
                silverfish.spawnExplosionParticle();
            }
        }

        @Override
        public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world,
                BlockPos pos, EntityPlayer player) {
            return hostStack(state);
        }

        private ItemStack hostStack(IBlockState state) {
            Block host = GameRegistry.findRegistry(Block.class).getValue(
                new net.minecraft.util.ResourceLocation("cavesnotcliffs:deepslate"));
            if (host == null) {
                return ItemStack.EMPTY;
            }
            return new ItemStack(host);
        }
    }
}
