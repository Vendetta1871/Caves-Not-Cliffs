package net.celestiald.cavesnotcliffs.client.debug;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.entity.EntityAxolotl;
import net.celestiald.cavesnotcliffs.entity.EntityBee;
import net.celestiald.cavesnotcliffs.worldgen.v118.NativeNoise;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Development autotest, inert unless {@code CNC_AUTOTEST} is set. Opens a Caves Not Cliffs
 * creative world (or joins {@code CNC_AUTOTEST_SERVER=host:port}, where the player must be op),
 * drives it through commands and world reads, and saves screenshots to {@code run/screenshots}:
 * <ul>
 *     <li>the v2 world contract: 384 blocks tall, the native or Java noise sampler in use</li>
 *     <li>deepslate under Y=0 and 1.18 biomes active around the spawn</li>
 *     <li>building above the old 1.12 height limit: a deepslate block at Y=300, screenshotted</li>
 *     <li>a museum row of the mod's blocks (dripstone, powder snow, campfire, deepslate iron
 *     and gold ore) placed and read back above the clouds</li>
 *     <li>bee and axolotl summoned and ticked client-side, then a deepslate cave view</li>
 * </ul>
 * Every step logs with the {@code [CNC-autotest]} prefix and a failed check exits with code 1;
 * {@code CNC_AUTOTEST_SEED} picks the seed (default 118), {@code CNC_AUTOTEST_STAY} keeps the
 * game open at the end.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID, value = Side.CLIENT)
public final class AutoTest {
    private static final Logger LOGGER = LogManager.getLogger("CavesNotCliffs/AutoTest");
    private static final boolean ENABLED = System.getenv("CNC_AUTOTEST") != null;
    private static final String SERVER = System.getenv("CNC_AUTOTEST_SERVER");

    private static int stage;
    private static int ticks;
    private static int step;
    private static int stepTicks;
    private static final List<Step> STEPS = new ArrayList<Step>();
    private static int failures;
    private static int caveX = Integer.MIN_VALUE;
    private static int caveZ;

    private AutoTest() {
    }

    private interface Action {
        /**
         * @return true when the step is done, false to run it again next tick
         */
        boolean run(Minecraft mc, int tick);
    }

    private static final class Step {
        final String name;
        final int delay;
        final Action action;

        Step(String name, int delay, Action action) {
            this.name = name;
            this.delay = delay;
            this.action = action;
        }
    }

    private static void step(String name, int delay, Action action) {
        STEPS.add(new Step(name, delay, action));
    }

    private static void log(String format, Object... args) {
        LOGGER.info("[CNC-autotest] " + format, args);
    }

    private static void check(boolean condition, String description) {
        if (condition) {
            log("PASS: {}", description);
        } else {
            failures++;
            log("FAIL: {}", description);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (stage == 0) {
            if (mc.currentScreen instanceof GuiMainMenu) {
                mc.gameSettings.pauseOnLostFocus = false;
                mc.gameSettings.tutorialStep = TutorialSteps.NONE;
                mc.getTutorial().setStep(TutorialSteps.NONE);
                if (SERVER != null) {
                    log("joining {}", SERVER);
                    mc.displayGuiScreen(new GuiConnecting(mc.currentScreen, mc,
                            new ServerData("cnc_autotest", SERVER, false)));
                } else {
                    WorldSettings settings = new WorldSettings(seed(), GameType.CREATIVE,
                            false, false, WorldType.parseWorldType(CavesNotCliffs.MODID));
                    settings.enableCommands();
                    mc.launchIntegratedServer("cnc_autotest", "cnc_autotest", settings);
                }
                buildScript();
                stage = 1;
            }
        } else if (stage == 1) {
            if (mc.player != null && mc.world != null && mc.currentScreen == null && ++ticks > 60) {
                log("in world, {} steps", STEPS.size());
                stage = 2;
            }
        } else if (stage == 2) {
            if (mc.player == null) {
                log("FAIL: disconnected during step {}",
                        step < STEPS.size() ? STEPS.get(step).name : "end");
                failures++;
                finish(mc);
                return;
            }
            if (step >= STEPS.size()) {
                log("{}: all steps finished ({} failed checks)", failures == 0 ? "PASS" : "FAIL",
                        failures);
                stage = 3;
                if (System.getenv("CNC_AUTOTEST_STAY") == null) {
                    finish(mc);
                }
                return;
            }
            // command feedback would cover the screenshots; frames render after this
            mc.ingameGUI.getChatGUI().clearChatMessages(false);

            Step current = STEPS.get(step);
            if (++stepTicks < current.delay) {
                return;
            }
            if (current.action.run(mc, stepTicks - current.delay)) {
                log("step '{}' done", current.name);
                step++;
                stepTicks = 0;
            }
        }
    }

    /** A failed run must leave a non-zero exit code so scripts and CI can notice it. */
    private static void finish(Minecraft mc) {
        if (failures == 0) {
            mc.shutdown();
        } else {
            FMLCommonHandler.instance().exitJava(1, false);
        }
    }

    private static long seed() {
        try {
            return Long.parseLong(System.getenv("CNC_AUTOTEST_SEED"));
        } catch (NumberFormatException absentOrUnknown) {
            return 118L;
        }
    }

    private static void command(Minecraft mc, String command) {
        mc.player.sendChatMessage(command);
    }

    private static ResourceLocation blockAt(Minecraft mc, int x, int y, int z) {
        return Block.REGISTRY.getNameForObject(
                mc.world.getBlockState(new BlockPos(x, y, z)).getBlock());
    }

    private static boolean isBlock(Minecraft mc, int x, int y, int z, String path) {
        ResourceLocation id = blockAt(mc, x, y, z);
        return id != null && CavesNotCliffs.MODID.equals(id.getResourceDomain())
                && path.equals(id.getResourcePath());
    }

    private static void screenshot(Minecraft mc, String name) {
        ScreenShotHelper.saveScreenshot(mc.mcDataDir, "cnc_" + name + ".png",
                mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        log("screenshot {} (fps {})", name, Minecraft.getDebugFPS());
    }

    private static void buildScript() {
        step("setup", 0, (mc, t) -> {
            command(mc, "/time set 6000");
            command(mc, "/gamerule doDaylightCycle false");
            command(mc, "/gamerule doMobSpawning false");
            command(mc, "/weather clear 100000");
            return true;
        });

        step("world contract", 20, (mc, t) -> {
            log("world type {}", mc.world.getWorldInfo().getTerrainType().getName());
            check(CavesNotCliffs.MODID.equals(mc.world.getWorldInfo().getTerrainType().getName()),
                    "the world uses the cavesnotcliffs world type");
            log("world height {}", mc.world.getActualHeight());
            check(mc.world.getActualHeight() == 384, "the world is 384 blocks tall");
            log("noise sampler: {}", NativeNoise.status());
            return true;
        });

        step("deepslate and biomes", 0, (mc, t) -> {
            int deepslate = 0;
            for (int x = -32; x <= 32; x += 16) {
                for (int z = -32; z <= 32; z += 16) {
                    ResourceLocation id = blockAt(mc, x, -58, z);
                    if (id != null && CavesNotCliffs.MODID.equals(id.getResourceDomain())
                            && id.getResourcePath().startsWith("deepslate")) {
                        deepslate++;
                    }
                }
            }
            log("deepslate at Y=-58 in {}/25 columns", deepslate);
            check(deepslate >= 15, "deepslate lies under Y=0");
            int modded = 0;
            for (int x = 0; x <= 512; x += 64) {
                ResourceLocation biome = Biome.REGISTRY
                        .getNameForObject(mc.world.getBiome(new BlockPos(x, 80, x)));
                if (biome != null && CavesNotCliffs.MODID.equals(biome.getResourceDomain())) {
                    modded++;
                }
                log("biome at {}: {}", x, biome);
            }
            check(modded > 0, "1.18 biomes are active around the spawn");
            return true;
        });

        step("above the old limit", 0, (mc, t) -> {
            command(mc, "/setblock 0 300 0 cavesnotcliffs:deepslate");
            return true;
        });
        step("check above the old limit", 20, (mc, t) -> {
            check(isBlock(mc, 0, 300, 0, "deepslate"), "blocks place at Y=300, above 256");
            command(mc, "/tp @p 0 302 6 0 5");
            return true;
        });
        step("above screenshot", 40, (mc, t) -> {
            screenshot(mc, "above_256");
            return true;
        });

        step("museum", 0, (mc, t) -> {
            command(mc, "/fill -3 299 0 3 299 0 cavesnotcliffs:deepslate");
            command(mc, "/setblock -2 300 0 cavesnotcliffs:pointed_dripstone");
            command(mc, "/setblock -1 300 0 cavesnotcliffs:powder_snow");
            command(mc, "/setblock 0 300 0 cavesnotcliffs:campfire");
            command(mc, "/setblock 1 300 0 cavesnotcliffs:deepslate_iron_ore");
            command(mc, "/setblock 2 300 0 cavesnotcliffs:deepslate_gold_ore");
            command(mc, "/tp @p 0 302 8 0 8");
            return true;
        });
        step("check museum", 30, (mc, t) -> {
            check(isBlock(mc, -2, 300, 0, "pointed_dripstone"), "pointed dripstone placed");
            check(isBlock(mc, -1, 300, 0, "powder_snow"), "powder snow placed");
            check(isBlock(mc, 0, 300, 0, "campfire"), "campfire placed");
            check(isBlock(mc, 1, 300, 0, "deepslate_iron_ore"), "deepslate iron ore placed");
            check(isBlock(mc, 2, 300, 0, "deepslate_gold_ore"), "deepslate gold ore placed");
            return true;
        });
        step("museum screenshot", 30, (mc, t) -> {
            screenshot(mc, "museum");
            return true;
        });

        step("mobs", 0, (mc, t) -> {
            command(mc, "/summon cavesnotcliffs:bee 0 301 3");
            command(mc, "/summon cavesnotcliffs:axolotl 2 300 3");
            return true;
        });
        step("check mobs", 60, (mc, t) -> {
            int bees = mc.world.getEntities(EntityBee.EntityCustom.class, e -> true).size();
            int axolotls = mc.world.getEntities(EntityAxolotl.EntityCustom.class, e -> true).size();
            log("client-side mobs: {} bees, {} axolotls", bees, axolotls);
            check(bees >= 1, "the bee spawned and renders");
            check(axolotls >= 1, "the axolotl spawned and renders");
            return true;
        });
        step("mobs screenshot", 20, (mc, t) -> {
            screenshot(mc, "mobs");
            return true;
        });

        step("cave view", 0, (mc, t) -> {
            for (int x = -32; x <= 32 && caveX == Integer.MIN_VALUE; x += 4) {
                for (int z = -32; z <= 32; z += 4) {
                    if (mc.world.isAirBlock(new BlockPos(x, -40, z))
                            && mc.world.isAirBlock(new BlockPos(x, -39, z))) {
                        caveX = x;
                        caveZ = z;
                        break;
                    }
                }
            }
            if (caveX == Integer.MIN_VALUE) {
                log("no cave pocket found near the spawn, skipping the view");
                return true;
            }
            command(mc, "/setblock " + caveX + " -39 " + caveZ + " minecraft:torch 0");
            command(mc, "/tp @p " + caveX + " -38.5 " + (caveZ + 3) + " 0 -15");
            return true;
        });
        step("cave screenshot", 60, (mc, t) -> {
            if (caveX != Integer.MIN_VALUE) {
                screenshot(mc, "deepslate_cave");
            }
            return true;
        });

        step("idle", 40, (mc, t) -> true);
    }
}
