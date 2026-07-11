package net.celestiald.cavesnotcliffs.content;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemFishFood;
import net.minecraft.item.ItemStack;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class CampfireCookingTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void catalogContainsEveryRepresentableOfficialRecipe() {
        assertEquals(8, CampfireCooking.recipes().size());
        Set<String> ids = new HashSet<String>();
        for (CampfireCooking.Recipe recipe : CampfireCooking.recipes()) {
            assertFalse(recipe.output().isEmpty());
            assertEquals(600, recipe.cookingTime());
            ids.add(recipe.id());
        }
        assertEquals(8, ids.size());
        assertEquals(0.35F, CampfireCooking.find(new ItemStack(Items.POTATO))
            .experience(), 0.0F);
    }

    @Test
    public void fishMetadataSeparatesCodSalmonAndUnsupportedPeers() {
        CampfireCooking.Recipe cod = CampfireCooking.find(new ItemStack(Items.FISH, 1,
            ItemFishFood.FishType.COD.getMetadata()));
        CampfireCooking.Recipe salmon = CampfireCooking.find(new ItemStack(Items.FISH, 1,
            ItemFishFood.FishType.SALMON.getMetadata()));
        assertNotNull(cod);
        assertNotNull(salmon);
        assertEquals(ItemFishFood.FishType.COD.getMetadata(), cod.output().getMetadata());
        assertEquals(ItemFishFood.FishType.SALMON.getMetadata(),
            salmon.output().getMetadata());
        assertNull(CampfireCooking.find(new ItemStack(Items.FISH, 1,
            ItemFishFood.FishType.CLOWNFISH.getMetadata())));
        assertNull(CampfireCooking.find(new ItemStack(Items.FISH, 1,
            ItemFishFood.FishType.PUFFERFISH.getMetadata())));
    }

    @Test
    public void allNonFishInputsResolveAndEmptyOrUnrelatedStacksDoNot() {
        assertNotNull(CampfireCooking.find(new ItemStack(Items.POTATO)));
        assertNotNull(CampfireCooking.find(new ItemStack(Items.BEEF)));
        assertNotNull(CampfireCooking.find(new ItemStack(Items.CHICKEN)));
        assertNotNull(CampfireCooking.find(new ItemStack(Items.MUTTON)));
        assertNotNull(CampfireCooking.find(new ItemStack(Items.PORKCHOP)));
        assertNotNull(CampfireCooking.find(new ItemStack(Items.RABBIT)));
        assertNull(CampfireCooking.find(ItemStack.EMPTY));
        assertNull(CampfireCooking.find(new ItemStack(Items.ROTTEN_FLESH)));
    }
}
