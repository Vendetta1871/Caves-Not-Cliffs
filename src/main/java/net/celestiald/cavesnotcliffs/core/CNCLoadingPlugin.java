package net.celestiald.cavesnotcliffs.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions("net.celestiald.cavesnotcliffs.mixin")
@IFMLLoadingPlugin.SortingIndex(1001)
public class CNCLoadingPlugin implements IFMLLoadingPlugin {

    public CNCLoadingPlugin() {
        MixinBootstrap.init();
        Mixins.addConfiguration("mixins.cavesnotcliffs.json");
        MixinEnvironment.getDefaultEnvironment().setObfuscationContext("searge");
    }

    @Override public String[] getASMTransformerClass() { return null; }
    @Override public String getModContainerClass() { return null; }
    @Override public String getSetupClass() { return null; }
    @Override public void injectData(Map<String, Object> data) {}
    @Override public String getAccessTransformerClass() { return null; }
}
