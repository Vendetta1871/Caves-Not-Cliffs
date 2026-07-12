package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPumpkin;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.stats.StatList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryModifiable;

/** Public Java 1.18.2 plain-pumpkin peer for the carved 1.12 pumpkin split. */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID)
public final class PlainPumpkinContent {
    static final int CARVED_UPDATE_FLAGS = 11;
    static final int CARVE_SEED_COUNT = 4;
    static final int SHEARS_DAMAGE = 1;
    static final double SEED_Y_OFFSET = 0.1D;
    static final double SEED_Y_MOTION = 0.05D;
    private static final ResourceLocation CARVE_SOUND_ID =
            CncRegistryIds.id("block.pumpkin.carve");
    static final ResourceLocation PUMPKIN_SEEDS_RECIPE_ID =
            new ResourceLocation("minecraft", "pumpkin_seeds");
    static final ResourceLocation PUMPKIN_PIE_RECIPE_ID =
            new ResourceLocation("minecraft", "pumpkin_pie");

    @GameRegistry.ObjectHolder("cavesnotcliffs:pumpkin")
    public static final Block PUMPKIN = null;

    public static final SoundEvent PUMPKIN_CARVE = new SoundEvent(CARVE_SOUND_ID)
            .setRegistryName(CARVE_SOUND_ID);

    private PlainPumpkinContent() {
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(createBlock());
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        Block pumpkin = ForgeRegistries.BLOCKS.getValue(CncRegistryIds.PUMPKIN);
        if (pumpkin == null) {
            throw new IllegalStateException(
                    "Plain pumpkin block was not registered before its item");
        }
        event.getRegistry().register(createItem(pumpkin));
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().register(PUMPKIN_CARVE);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void replacePumpkinRecipes(RegistryEvent.Register<IRecipe> event) {
        IForgeRegistry<IRecipe> registry = event.getRegistry();
        if (!(registry instanceof IForgeRegistryModifiable)) {
            throw new IllegalStateException(
                    "Recipe registry does not support canonical pumpkin replacement");
        }
        Item pumpkin = ForgeRegistries.ITEMS.getValue(CncRegistryIds.PUMPKIN);
        if (pumpkin == null) {
            throw new IllegalStateException(
                    "Plain pumpkin item was not registered before its recipes");
        }
        @SuppressWarnings("unchecked")
        IForgeRegistryModifiable<IRecipe> modifiable =
                (IForgeRegistryModifiable<IRecipe>) registry;
        replacePumpkinRecipes(modifiable, pumpkin);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {
        Item item = Item.getItemFromBlock(PUMPKIN);
        ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation(CncRegistryIds.PUMPKIN, "inventory"));
    }

    static PlainPumpkinBlock createBlock() {
        PlainPumpkinBlock block = new PlainPumpkinBlock();
        block.setRegistryName(CncRegistryIds.PUMPKIN);
        return block;
    }

    static ItemBlock createItem(Block block) {
        return (ItemBlock) new ItemBlock(block).setRegistryName(CncRegistryIds.PUMPKIN);
    }

    static void replacePumpkinRecipes(IForgeRegistryModifiable<IRecipe> registry,
            Item pumpkin) {
        if (registry.isLocked()) {
            throw new IllegalStateException(
                    "Recipe registry was locked before canonical pumpkin replacement");
        }
        if (!registry.containsKey(PUMPKIN_SEEDS_RECIPE_ID)
                || !registry.containsKey(PUMPKIN_PIE_RECIPE_ID)) {
            throw new IllegalStateException(
                    "Canonical vanilla pumpkin recipes were missing during replacement");
        }
        // Forge loads JSON recipes before firing this registry event, so keep the
        // vanilla recipe-book IDs while replacing only their ingredient contracts.
        registry.remove(PUMPKIN_SEEDS_RECIPE_ID);
        registry.remove(PUMPKIN_PIE_RECIPE_ID);
        registry.register(pumpkinSeedsRecipe(pumpkin));
        registry.register(pumpkinPieRecipe(pumpkin));
    }

    static ShapelessRecipes pumpkinSeedsRecipe(Item pumpkin) {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.fromItem(pumpkin));
        ShapelessRecipes recipe = new ShapelessRecipes("",
                new ItemStack(Items.PUMPKIN_SEEDS, CARVE_SEED_COUNT), ingredients);
        recipe.setRegistryName(PUMPKIN_SEEDS_RECIPE_ID);
        return recipe;
    }

    static ShapelessRecipes pumpkinPieRecipe(Item pumpkin) {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.fromItem(pumpkin));
        ingredients.add(Ingredient.fromItem(Items.SUGAR));
        ingredients.add(Ingredient.fromItem(Items.EGG));
        ShapelessRecipes recipe = new ShapelessRecipes("",
                new ItemStack(Items.PUMPKIN_PIE), ingredients);
        recipe.setRegistryName(PUMPKIN_PIE_RECIPE_ID);
        return recipe;
    }

    static EnumFacing carvedFacing(EnumFacing clickedFace, EnumFacing playerFacing) {
        return clickedFace.getAxis() == EnumFacing.Axis.Y
                ? playerFacing.getOpposite() : clickedFace;
    }

    static IBlockState carvedState(EnumFacing facing) {
        return Blocks.PUMPKIN.getDefaultState()
                .withProperty(BlockPumpkin.FACING, facing);
    }

    static double seedHorizontalPosition(int blockCoordinate, int directionStep) {
        return blockCoordinate + 0.5D + directionStep * 0.65D;
    }

    static double seedHorizontalMotion(int directionStep, double randomUnit) {
        return directionStep * 0.05D + randomUnit * 0.02D;
    }

    static final class PlainPumpkinBlock extends Block {
        PlainPumpkinBlock() {
            super(Material.GOURD, MapColor.ADOBE);
            // The 1.12 carved peer already owns tile.pumpkin; keep the item names distinct.
            setUnlocalizedName("plain_pumpkin");
            setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
            setSoundType(SoundType.WOOD);
            setHardness(1.0F);
            EntityEnderman.setCarriable(this, true);
        }

        @Override
        public boolean isToolEffective(String type, IBlockState state) {
            return "axe".equals(type) || super.isToolEffective(type, state);
        }

        @Override
        public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                EntityPlayer player, EnumHand hand, EnumFacing clickedFace,
                float hitX, float hitY, float hitZ) {
            ItemStack held = player.getHeldItem(hand);
            if (held.getItem() != Items.SHEARS) {
                return super.onBlockActivated(world, pos, state, player, hand,
                        clickedFace, hitX, hitY, hitZ);
            }
            if (!world.isRemote) {
                EnumFacing carvedFacing = carvedFacing(clickedFace,
                        player.getHorizontalFacing());
                world.playSound(null, pos, PUMPKIN_CARVE, SoundCategory.BLOCKS,
                        1.0F, 1.0F);
                world.setBlockState(pos, carvedState(carvedFacing), CARVED_UPDATE_FLAGS);

                EntityItem seeds = new EntityItem(world,
                        seedHorizontalPosition(pos.getX(), carvedFacing.getFrontOffsetX()),
                        pos.getY() + SEED_Y_OFFSET,
                        seedHorizontalPosition(pos.getZ(), carvedFacing.getFrontOffsetZ()),
                        new ItemStack(Items.PUMPKIN_SEEDS, CARVE_SEED_COUNT));
                seeds.setVelocity(
                        seedHorizontalMotion(carvedFacing.getFrontOffsetX(),
                                world.rand.nextDouble()),
                        SEED_Y_MOTION,
                        seedHorizontalMotion(carvedFacing.getFrontOffsetZ(),
                                world.rand.nextDouble()));
                world.spawnEntity(seeds);
                held.damageItem(SHEARS_DAMAGE, player);
                player.addStat(StatList.getObjectUseStats(Items.SHEARS));
            }
            return true;
        }
    }
}
