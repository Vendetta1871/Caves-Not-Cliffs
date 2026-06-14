package net.celestiald.cavesnotcliffs.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions("net.celestiald.cavesnotcliffs.core")
@IFMLLoadingPlugin.SortingIndex(1001)
public class CNCLoadingPlugin implements IFMLLoadingPlugin {

    /** Set when Mixin can't be found, so the @Mod can fail with a clear message instead of an
     *  obscure NoClassDefFoundError crash here in the coremod phase. */
    public static final String MIXIN_MISSING_PROPERTY = "cavesnotcliffs.mixinMissing";

    public CNCLoadingPlugin() {
        try {
            MixinBootstrap.init();
            Mixins.addConfiguration("mixins.cavesnotcliffs.json");
        } catch (Throwable t) {
            // Mixin is supplied at runtime by the MixinBootstrap mod. If it's absent, the classes
            // referenced above can't link — don't crash the whole game launch here; let
            // CavesNotCliffs#preInit report the missing dependency cleanly.
            System.setProperty(MIXIN_MISSING_PROPERTY, "true");
            System.err.println("[Caves Not Cliffs] SpongePowered Mixin not found - is the "
                    + "MixinBootstrap mod installed? (https://modrinth.com/mod/mixinbootstrap)");
        }
    }

    @Override public String[] getASMTransformerClass() { return null; }
    @Override public String getModContainerClass() { return null; }
    @Override public String getSetupClass() { return null; }
    @Override public void injectData(Map<String, Object> data) {}
    @Override public String getAccessTransformerClass() { return null; }
}
