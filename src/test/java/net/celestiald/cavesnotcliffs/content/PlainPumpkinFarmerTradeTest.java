package net.celestiald.cavesnotcliffs.content;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.fml.common.registry.VillagerRegistry;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PlainPumpkinFarmerTradeTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void vanillaCareerViewSharesTheReviewedSourceTradeObject() {
        EntityVillager.ITradeList source =
                EntityVillager.GET_TRADES_DONT_USE()[0][0][1][0];
        VillagerRegistry.VillagerProfession profession = VillagerRegistry.getById(0);
        assertEquals("minecraft:farmer", profession.getRegistryName().toString());
        VillagerRegistry.VillagerCareer career = profession.getCareer(0);
        assertEquals("farmer", career.getName());
        assertSame(source, career.getTrades(1).get(0));

        EntityVillager.EmeraldForItems pumpkin =
                (EntityVillager.EmeraldForItems) source;
        assertSame(Item.getItemFromBlock(Blocks.PUMPKIN), pumpkin.buyingItem);
        assertEquals(8, ((Integer) pumpkin.price.getFirst()).intValue());
        assertEquals(13, ((Integer) pumpkin.price.getSecond()).intValue());
    }

    @Test
    public void installMutatesOnlyTheSharedPumpkinSourceAndIsIdempotent() {
        ItemBlock plain = plainPumpkin();
        EntityVillager.EmeraldForItems pumpkin = vanillaPumpkinTrade();
        EntityVillager.PriceInfo wheatPrice = new EntityVillager.PriceInfo(18, 22);
        EntityVillager.EmeraldForItems wheat =
                new EntityVillager.EmeraldForItems(Items.WHEAT, wheatPrice);
        List<EntityVillager.ITradeList> active = Collections.unmodifiableList(
                Arrays.<EntityVillager.ITradeList>asList(wheat, pumpkin));

        PlainPumpkinContent.installFarmerPumpkinTrade(plain, pumpkin, active);
        assertSame(plain, pumpkin.buyingItem);
        assertNull(pumpkin.price);
        assertSame(wheat, active.get(0));
        assertSame(pumpkin, active.get(1));
        assertSame(Items.WHEAT, wheat.buyingItem);
        assertSame(wheatPrice, wheat.price);

        PlainPumpkinContent.installFarmerPumpkinTrade(plain, pumpkin, active);
        assertSame(plain, pumpkin.buyingItem);
        assertNull(pumpkin.price);
        assertSame(wheatPrice, wheat.price);
    }

    @Test
    public void productionSourceBecomesTheFixedContractWithoutRandom() {
        EntityVillager.EmeraldForItems source =
                (EntityVillager.EmeraldForItems)
                EntityVillager.GET_TRADES_DONT_USE()[0][0][1][0];
        Item originalItem = source.buyingItem;
        EntityVillager.PriceInfo originalPrice = source.price;
        ItemBlock plain = plainPumpkin();
        try {
            PlainPumpkinContent.installFarmerPumpkinTrade(plain);
            assertSame(plain, source.buyingItem);
            assertNull(source.price);
            assertSame(source, VillagerRegistry.getById(0).getCareer(0)
                    .getTrades(1).get(0));

            MerchantRecipeList recipes = new MerchantRecipeList();
            CountingRandom random = new CountingRandom();
            source.addMerchantRecipe(null, recipes, random);
            assertEquals(0, random.calls);
            assertEquals(1, recipes.get(0).getItemToBuy().getCount());
            assertEquals(7, recipes.get(0).getMaxTradeUses());

            PlainPumpkinContent.finishFarmerPumpkinTrade(source, recipes);
            MerchantRecipe recipe = recipes.get(0);
            assertSame(plain, recipe.getItemToBuy().getItem());
            assertEquals(PlainPumpkinContent.FARMER_PUMPKIN_COST,
                    recipe.getItemToBuy().getCount());
            assertFalse(recipe.hasSecondItemToBuy());
            assertSame(Items.EMERALD, recipe.getItemToSell().getItem());
            assertEquals(0, recipe.getToolUses());
            assertEquals(PlainPumpkinContent.FARMER_PUMPKIN_MAX_USES,
                    recipe.getMaxTradeUses());
            assertTrue(recipe.getRewardsExp());
        } finally {
            source.buyingItem = originalItem;
            source.price = originalPrice;
        }
    }

    @Test
    public void installRejectsDriftedSourceAndActiveAnchors() {
        ItemBlock plain = plainPumpkin();
        EntityVillager.EmeraldForItems pumpkin = vanillaPumpkinTrade();
        EntityVillager.EmeraldForItems different = vanillaPumpkinTrade();
        assertInstallFails(plain, pumpkin,
                Collections.<EntityVillager.ITradeList>singletonList(different),
                "shared active");
        assertInstallFails(plain, pumpkin,
                Arrays.<EntityVillager.ITradeList>asList(pumpkin, pumpkin),
                "shared active");

        EntityVillager.EmeraldForItems melon = new EntityVillager.EmeraldForItems(
                Item.getItemFromBlock(Blocks.MELON_BLOCK),
                new EntityVillager.PriceInfo(8, 13));
        assertInstallFails(plain, melon,
                Collections.<EntityVillager.ITradeList>singletonList(melon),
                "carved-pumpkin");

        EntityVillager.EmeraldForItems changedPrice =
                new EntityVillager.EmeraldForItems(
                        Item.getItemFromBlock(Blocks.PUMPKIN),
                        new EntityVillager.PriceInfo(7, 13));
        assertInstallFails(plain, changedPrice,
                Collections.<EntityVillager.ITradeList>singletonList(changedPrice),
                "8..13");
    }

    @Test
    public void hookLeavesEveryUnmarkedTradeUntouched() {
        ItemBlock plain = plainPumpkin();
        MerchantRecipeList plainRecipes = new MerchantRecipeList();
        EntityVillager.PriceInfo fixed = new EntityVillager.PriceInfo(3, 3);
        EntityVillager.EmeraldForItems fixedTrade =
                new EntityVillager.EmeraldForItems(plain, fixed);
        fixedTrade.addMerchantRecipe(null, plainRecipes, new Random(0L));
        PlainPumpkinContent.finishFarmerPumpkinTrade(fixedTrade, plainRecipes);
        assertEquals(3, plainRecipes.get(0).getItemToBuy().getCount());
        assertEquals(7, plainRecipes.get(0).getMaxTradeUses());

        MerchantRecipeList onePumpkinRecipes = new MerchantRecipeList();
        EntityVillager.EmeraldForItems onePumpkin =
                new EntityVillager.EmeraldForItems(plain, null);
        onePumpkin.addMerchantRecipe(null, onePumpkinRecipes, new Random(0L));
        PlainPumpkinContent.finishFarmerPumpkinTrade(
                onePumpkin, onePumpkinRecipes);
        assertEquals(1, onePumpkinRecipes.get(0).getItemToBuy().getCount());
        assertEquals(7, onePumpkinRecipes.get(0).getMaxTradeUses());

        Item other = new Item().setRegistryName("example", "other");
        MerchantRecipeList otherRecipes = new MerchantRecipeList();
        EntityVillager.EmeraldForItems otherTrade =
                new EntityVillager.EmeraldForItems(other, null);
        otherTrade.addMerchantRecipe(null, otherRecipes, new Random(0L));
        PlainPumpkinContent.finishFarmerPumpkinTrade(otherTrade, otherRecipes);
        assertEquals(1, otherRecipes.get(0).getItemToBuy().getCount());
        assertEquals(7, otherRecipes.get(0).getMaxTradeUses());
    }

    @Test
    public void markedTradeWithChangedPlaceholderFailsClearly() {
        ItemBlock plain = plainPumpkin();
        EntityVillager.EmeraldForItems source =
                (EntityVillager.EmeraldForItems)
                EntityVillager.GET_TRADES_DONT_USE()[0][0][1][0];
        Item originalItem = source.buyingItem;
        EntityVillager.PriceInfo originalPrice = source.price;
        try {
            PlainPumpkinContent.installFarmerPumpkinTrade(plain);
            MerchantRecipeList recipes = new MerchantRecipeList();
            recipes.add(new MerchantRecipe(new ItemStack(plain, 2), Items.EMERALD));
            try {
                PlainPumpkinContent.finishFarmerPumpkinTrade(source, recipes);
                fail("A changed vanilla placeholder must not be silently rewritten");
            } catch (IllegalStateException expected) {
                assertTrue(expected.getMessage().contains("farmer pumpkin trade"));
                assertTrue(expected.getMessage().contains("placeholder offer"));
            }
        } finally {
            source.buyingItem = originalItem;
            source.price = originalPrice;
        }
    }

    private static ItemBlock plainPumpkin() {
        return PlainPumpkinContent.createItem(PlainPumpkinContent.createBlock());
    }

    private static EntityVillager.EmeraldForItems vanillaPumpkinTrade() {
        return new EntityVillager.EmeraldForItems(
                Item.getItemFromBlock(Blocks.PUMPKIN),
                new EntityVillager.PriceInfo(8, 13));
    }

    private static void assertInstallFails(Item plain,
            EntityVillager.ITradeList source,
            List<EntityVillager.ITradeList> active, String point) {
        try {
            PlainPumpkinContent.installFarmerPumpkinTrade(plain, source, active);
            fail("A drifted farmer trade anchor must fail clearly");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("farmer pumpkin trade"));
            assertTrue(expected.getMessage().contains(point));
        }
    }

    private static final class CountingRandom extends Random {
        private int calls;

        @Override
        public int nextInt(int bound) {
            calls++;
            return super.nextInt(bound);
        }
    }
}
