package net.celestiald.cavesnotcliffs.world;

import org.junit.Test;

import static net.celestiald.cavesnotcliffs.world.WorldActivationPolicy.Action;
import static net.celestiald.cavesnotcliffs.world.WorldActivationPolicy.SelectedKind;
import static org.junit.Assert.assertEquals;

public class WorldActivationPolicyTest {
    @Test
    public void enabledConfigActivatesEveryCompatibleNewTwoDimensionalSelection() {
        assertNew(true, SelectedKind.COMPATIBLE_TWO_DIMENSIONAL, Action.ACTIVATE_SCHEMA_2);
        assertNew(true, SelectedKind.LEGACY_ALIAS, Action.ACTIVATE_SCHEMA_2);
        assertNew(true, SelectedKind.WRAPPER, Action.ACTIVATE_SCHEMA_2);
        assertNew(true, SelectedKind.OTHER_CUBIC, Action.NONE);
    }

    @Test
    public void disabledConfigLeavesNewBaseTypesAloneAndNeutralizesStaleCncSelections() {
        assertNew(false, SelectedKind.COMPATIBLE_TWO_DIMENSIONAL, Action.NONE);
        assertNew(false, SelectedKind.OTHER_CUBIC, Action.NONE);
        assertNew(false, SelectedKind.LEGACY_ALIAS, Action.REVERT_NEW_SELECTION);
        assertNew(false, SelectedKind.WRAPPER, Action.REVERT_NEW_SELECTION);
    }

    @Test
    public void existingWorldsIgnoreTheCurrentConfigValue() {
        for (boolean enabled : new boolean[]{false, true}) {
            assertExisting(enabled, SelectedKind.LEGACY_ALIAS, 0, Action.KEEP_SCHEMA_1);
            assertExisting(enabled, SelectedKind.WRAPPER, 0, Action.KEEP_SCHEMA_2);
            assertExisting(enabled, SelectedKind.COMPATIBLE_TWO_DIMENSIONAL, 0, Action.NONE);
            assertExisting(enabled, SelectedKind.OTHER_CUBIC, 0, Action.NONE);

            assertExisting(enabled, SelectedKind.LEGACY_ALIAS,
                    CavesNotCliffsWorldData.LEGACY_SCHEMA, Action.KEEP_SCHEMA_1);
            assertExisting(enabled, SelectedKind.WRAPPER,
                    CavesNotCliffsWorldData.LEGACY_SCHEMA, Action.RESTORE_SCHEMA_1);
            assertExisting(enabled, SelectedKind.COMPATIBLE_TWO_DIMENSIONAL,
                    CavesNotCliffsWorldData.LEGACY_SCHEMA, Action.RESTORE_SCHEMA_1);

            assertExisting(enabled, SelectedKind.WRAPPER,
                    CavesNotCliffsWorldData.CURRENT_SCHEMA, Action.KEEP_SCHEMA_2);
            assertExisting(enabled, SelectedKind.LEGACY_ALIAS,
                    CavesNotCliffsWorldData.CURRENT_SCHEMA, Action.RESTORE_SCHEMA_2);
            assertExisting(enabled, SelectedKind.COMPATIBLE_TWO_DIMENSIONAL,
                    CavesNotCliffsWorldData.CURRENT_SCHEMA, Action.RESTORE_SCHEMA_2);
        }
    }

    private static void assertNew(boolean enabled, SelectedKind kind, Action expected) {
        assertEquals(expected, WorldActivationPolicy.decide(false, enabled, kind, 0));
    }

    private static void assertExisting(boolean enabled, SelectedKind kind,
            int schema, Action expected) {
        assertEquals(expected, WorldActivationPolicy.decide(true, enabled, kind, schema));
    }
}
