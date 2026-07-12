package net.celestiald.cavesnotcliffs.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

@IFMLLoadingPlugin.Name("CavesNotCliffsCore")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(1001)
@IFMLLoadingPlugin.TransformerExclusions({"net.celestiald.cavesnotcliffs.core"})
public final class CavesNotCliffsCorePlugin implements IFMLLoadingPlugin {
    @Override
    public String[] getASMTransformerClass() {
        return new String[]{
                HoneyPistonTransformer.class.getName(),
                SchemaOnePopulationTransformer.class.getName(),
                CubicImportSessionLockTransformer.class.getName(),
                BeeSaplingDecorationTransformer.class.getName(),
                PlainPumpkinStemTransformer.class.getName(),
                PlainPumpkinConnectionTransformer.class.getName(),
                PlainPumpkinFarmerTradeTransformer.class.getName(),
                DeadBushSupportTransformer.class.getName(),
                SugarCaneSupportTransformer.class.getName(),
                LilyPadSupportTransformer.class.getName(),
                MushroomSupportTransformer.class.getName(),
                DoublePlantSupportTransformer.class.getName(),
                TallGrassSupportTransformer.class.getName(),
                FlowerSupportTransformer.class.getName()
        };
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
