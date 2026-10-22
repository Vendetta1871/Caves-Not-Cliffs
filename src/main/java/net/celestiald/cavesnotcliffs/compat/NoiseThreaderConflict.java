package net.celestiald.cavesnotcliffs.compat;

import net.celestiald.cavesnotcliffs.client.NoiseThreaderErrorScreen;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.LoaderException;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Arrays;
import java.util.List;

/**
 * NoiseThreader refuses to start next to Caves Not Cliffs. The pack already gets its noise
 * speed from this mod's native sampler, so NoiseThreader would add nothing there, and running
 * two noise accelerators at once only risks their interaction. A client learns about the
 * refusal through FML's custom loading error screen; a dedicated server, which has no screen,
 * through the exception that stops it.
 */
public final class NoiseThreaderConflict {
    public static final String MODID = "noisethreader";
    public static final String TITLE =
            "Caves Not Cliffs cannot run together with NoiseThreader";

    private NoiseThreaderConflict() {
    }

    /** The refusal text, pre-wrapped for the error screen's font. */
    public static List<String> messageLines() {
        return Arrays.asList(
                "Caves Not Cliffs already includes the speedups NoiseThreader provides:",
                "its world generation noise runs on a native accelerator. NoiseThreader",
                "would not give this pack any extra performance, so the two mods refuse",
                "to start together.",
                "",
                "Remove NoiseThreader (noisethreader) from the mods folder and start",
                "the game again.");
    }

    /** Never returns when NoiseThreader is installed; halts the game with the reason. */
    public static void haltIfInstalled() {
        if (!Loader.isModLoaded(MODID)) {
            return;
        }
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            throw new NoiseThreaderErrorScreen();
        }
        throw new LoaderException(TITLE + ". " + String.join(" ", messageLines()));
    }
}
