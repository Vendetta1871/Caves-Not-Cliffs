package net.celestiald.cavesnotcliffs.compat;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.content.HoneycombDispenserBehavior;
import net.celestiald.cavesnotcliffs.content.LushCaveContent;
import net.celestiald.cavesnotcliffs.item.ItemHoneycomb;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;

/** Small optional bridge for Future MC's public bee pollination API. */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID)
public final class FutureMcCompat {
    private static final Logger LOGGER = LogManager.getLogger("CavesNotCliffs/FutureMC");
    private static boolean initialized;
    private static Item honeycomb;

    private FutureMcCompat() {
    }

    public static void initialize() {
        if (initialized || !Loader.isModLoaded("futuremc")) {
            return;
        }
        initialized = true;
        honeycomb = ForgeRegistries.ITEMS.getValue(new ResourceLocation("futuremc", "honeycomb"));
        if (honeycomb != null) {
            BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(
                    honeycomb, new HoneycombDispenserBehavior());
        } else {
            LOGGER.warn("Future MC is present without futuremc:honeycomb; skipping waxing bridge");
        }
        try {
            Class<?> api = Class.forName(
                    "thedarkcolour.futuremc.api.BeePollinationTargetsJVM");
            Method addTarget = api.getMethod("addPollinationTarget", IBlockState.class);
            addStates(addTarget, LushCaveContent.FLOWERING_AZALEA);
            addStates(addTarget, LushCaveContent.FLOWERING_AZALEA_LEAVES);
            LOGGER.info("Registered flowering azalea states with Future MC bees");
        } catch (ReflectiveOperationException | LinkageError error) {
            LOGGER.warn("Future MC is present but its bee pollination API is incompatible; "
                    + "continuing without the optional bridge", error);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void useHoneycombOnCopper(PlayerInteractEvent.RightClickBlock event) {
        if (honeycomb == null || event.getItemStack().getItem() != honeycomb
                || event.getFace() == null || event.getUseItem() == Event.Result.DENY) {
            return;
        }
        EnumActionResult result = ItemHoneycomb.useOnCopper(event.getEntityPlayer(),
                event.getWorld(), event.getPos(), event.getHand(), event.getFace());
        if (result == EnumActionResult.PASS) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(result);
        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.DENY);
    }

    private static void addStates(Method addTarget, Block block)
            throws ReflectiveOperationException {
        if (block == null) {
            throw new IllegalStateException("Flowering azalea blocks are not registered");
        }
        for (IBlockState state : block.getBlockState().getValidStates()) {
            addTarget.invoke(null, state);
        }
    }
}
