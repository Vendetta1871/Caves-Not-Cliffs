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
import net.minecraft.entity.passive.EntityVillager;
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
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.registry.VillagerRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryModifiable;

import java.util.List;

/** Public Java 1.18.2 plain-pumpkin peer for the carved 1.12 pumpkin split. */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID)
public final class PlainPumpkinContent {
    static final int CARVED_UPDATE_FLAGS = 11;
    static final int CARVE_SEED_COUNT = 4;
    static final int SHEARS_DAMAGE = 1;
    static final double SEED_Y_OFFSET = 0.1D;
    static final double SEED_Y_MOTION = 0.05D;
    static final int FARMER_PUMPKIN_COST = 6;
    static final int FARMER_PUMPKIN_MAX_USES = 12;
    private static final int VANILLA_PLACEHOLDER_COST = 1;
    private static final int VANILLA_PLACEHOLDER_MAX_USES = 7;
    private static final ResourceLocation FARMER_ID =
            new ResourceLocation("minecraft", "farmer");
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
        ItemBlock item = createItem(pumpkin);
        event.getRegistry().register(item);
        installFarmerPumpkinTrade(item);
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

    static void installFarmerPumpkinTrade(Item plainPumpkin) {
        EntityVillager.ITradeList[][][][] sourceTrades =
                EntityVillager.GET_TRADES_DONT_USE();
        EntityVillager.ITradeList source = farmerPumpkinSource(sourceTrades);

        VillagerRegistry.VillagerProfession profession = VillagerRegistry.getById(0);
        if (profession == null || !FARMER_ID.equals(profession.getRegistryName())) {
            throw tradeFailure("the vanilla farmer profession at registry id 0");
        }
        VillagerRegistry.VillagerCareer career = profession.getCareer(0);
        if (career == null || !"farmer".equals(career.getName())) {
            throw tradeFailure("the vanilla farmer career at index 0");
        }
        List<EntityVillager.ITradeList> activeTrades = career.getTrades(1);
        installFarmerPumpkinTrade(plainPumpkin, source, activeTrades);
    }

    static void installFarmerPumpkinTrade(Item plainPumpkin,
            EntityVillager.ITradeList source,
            List<EntityVillager.ITradeList> activeTrades) {
        if (plainPumpkin == null
                || !CncRegistryIds.PUMPKIN.equals(plainPumpkin.getRegistryName())) {
            throw tradeFailure("the canonical plain-pumpkin item");
        }
        if (!(source instanceof EntityVillager.EmeraldForItems)) {
            throw tradeFailure("an EmeraldForItems source at [0][0][1][0]");
        }
        if (!containsUniqueIdentity(activeTrades, source)) {
            throw tradeFailure("the shared active farmer level-two trade");
        }

        EntityVillager.EmeraldForItems trade =
                (EntityVillager.EmeraldForItems) source;
        if (trade.buyingItem == plainPumpkin && trade.price == null) {
            return;
        }
        EntityVillager.PriceInfo price = trade.price;
        if (trade.buyingItem != Item.getItemFromBlock(Blocks.PUMPKIN)
                || price == null
                || ((Integer) price.getFirst()).intValue() != 8
                || ((Integer) price.getSecond()).intValue() != 13) {
            throw tradeFailure("the vanilla carved-pumpkin 8..13 source contract");
        }

        // Null is a private marker that also prevents the obsolete 1.12 price RNG draw.
        trade.buyingItem = plainPumpkin;
        trade.price = null;
    }

    private static boolean containsUniqueIdentity(
            List<EntityVillager.ITradeList> trades,
            EntityVillager.ITradeList expected) {
        if (trades == null) {
            return false;
        }
        boolean found = false;
        for (EntityVillager.ITradeList trade : trades) {
            if (trade != expected) {
                continue;
            }
            if (found) {
                return false;
            }
            found = true;
        }
        return found;
    }

    private static EntityVillager.ITradeList farmerPumpkinSource(
            EntityVillager.ITradeList[][][][] trades) {
        try {
            return trades[0][0][1][0];
        } catch (NullPointerException | ArrayIndexOutOfBoundsException failure) {
            throw tradeFailure("the source trade path [0][0][1][0]");
        }
    }

    /** Finalizes only the exact vanilla source offer mutated during item registration. */
    public static void finishFarmerPumpkinTrade(Object source,
            MerchantRecipeList recipes) {
        EntityVillager.ITradeList expected = farmerPumpkinSource(
                EntityVillager.GET_TRADES_DONT_USE());
        if (source != expected) {
            return;
        }
        EntityVillager.EmeraldForItems trade =
                (EntityVillager.EmeraldForItems) expected;
        Item buyingItem = trade.buyingItem;
        if (trade.price != null || buyingItem == null
                || !CncRegistryIds.PUMPKIN.equals(buyingItem.getRegistryName())) {
            throw tradeFailure("the installed plain-pumpkin source marker");
        }
        if (recipes == null || recipes.isEmpty()) {
            throw tradeFailure("the newly appended pumpkin offer");
        }
        MerchantRecipe recipe = recipes.get(recipes.size() - 1);
        ItemStack buy = recipe.getItemToBuy();
        ItemStack sell = recipe.getItemToSell();
        if (buy.getItem() != buyingItem
                || buy.getCount() != VANILLA_PLACEHOLDER_COST
                || buy.getMetadata() != 0
                || recipe.hasSecondItemToBuy()
                || sell.getItem() != Items.EMERALD
                || sell.getCount() != 1
                || recipe.getToolUses() != 0
                || recipe.getMaxTradeUses() != VANILLA_PLACEHOLDER_MAX_USES) {
            throw tradeFailure("the vanilla EmeraldForItems placeholder offer");
        }
        buy.setCount(FARMER_PUMPKIN_COST);
        recipe.increaseMaxTradeUses(
                FARMER_PUMPKIN_MAX_USES - VANILLA_PLACEHOLDER_MAX_USES);
    }

    private static IllegalStateException tradeFailure(String point) {
        return new IllegalStateException(
                "Caves Not Cliffs farmer pumpkin trade could not verify " + point);
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
