package net.celestiald.cavesnotcliffs.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

import java.util.Map;

@IFMLLoadingPlugin.Name("CavesNotCliffsCore")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(1001)
@IFMLLoadingPlugin.TransformerExclusions({"net.celestiald.cavesnotcliffs.core"})
public final class CavesNotCliffsCorePlugin implements IFMLLoadingPlugin {
    /** Set when Mixin can't be found, so the @Mod can fail with a clear message instead of an
     *  obscure NoClassDefFoundError crash here in the coremod phase. */
    public static final String MIXIN_MISSING_PROPERTY = "cavesnotcliffs.mixinMissing";

    public CavesNotCliffsCorePlugin() {
        try {
            MixinBootstrap.init();
            Mixins.addConfiguration("mixins.cavesnotcliffs.json");
        } catch (Throwable t) {
            // Mixin is supplied at runtime by the MixinBootstrap mod (required transitively via
            // CaveBiomesAPI). If it's absent, the classes referenced above can't link — don't
            // crash the whole game launch here; let CavesNotCliffs#preInit report the missing
            // dependency cleanly.
            System.setProperty(MIXIN_MISSING_PROPERTY, "true");
            System.err.println("[Caves Not Cliffs] SpongePowered Mixin not found - is the "
                    + "MixinBootstrap mod installed? (https://modrinth.com/mod/mixinbootstrap)");
        }
    }
    @Override
    public String[] getASMTransformerClass() {
        return new String[]{
                HoneyPistonTransformer.class.getName(),
                InventoryMigrationTransformer.class.getName(),
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
                HugeMushroomDropTransformer.class.getName(),
                HugeMushroomStateTransformer.class.getName(),
                OthersideComparatorTransformer.class.getName(),
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
