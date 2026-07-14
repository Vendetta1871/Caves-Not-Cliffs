package net.celestiald.cavesnotcliffs.world;

/**
 * Pure world-format decision table. Keeping it independent of Minecraft makes the irreversible
 * new-world/existing-world boundary exhaustively testable.
 */
public final class WorldActivationPolicy {
    public enum SelectedKind {
        LEGACY_ALIAS,
        WRAPPER,
        COMPATIBLE_TWO_DIMENSIONAL,
        OTHER_CUBIC
    }

    public enum Action {
        NONE,
        ACTIVATE_SCHEMA_2,
        KEEP_SCHEMA_1,
        KEEP_SCHEMA_2,
        RESTORE_SCHEMA_1,
        RESTORE_SCHEMA_2,
        REVERT_NEW_SELECTION
    }

    private WorldActivationPolicy() {
    }

    public static Action decide(boolean existingWorld, boolean configEnabled,
            SelectedKind selectedKind, int persistedSchema) {
        if (existingWorld) {
            if (persistedSchema >= CavesNotCliffsWorldData.CURRENT_SCHEMA) {
                return selectedKind == SelectedKind.WRAPPER
                        ? Action.KEEP_SCHEMA_2 : Action.RESTORE_SCHEMA_2;
            }
            if (persistedSchema == CavesNotCliffsWorldData.LEGACY_SCHEMA) {
                return selectedKind == SelectedKind.LEGACY_ALIAS
                        ? Action.KEEP_SCHEMA_1 : Action.RESTORE_SCHEMA_1;
            }
            if (selectedKind == SelectedKind.LEGACY_ALIAS) {
                return Action.KEEP_SCHEMA_1;
            }
            if (selectedKind == SelectedKind.WRAPPER) {
                return Action.KEEP_SCHEMA_2;
            }
            return Action.NONE;
        }

        if (!configEnabled) {
            return selectedKind == SelectedKind.LEGACY_ALIAS || selectedKind == SelectedKind.WRAPPER
                    ? Action.REVERT_NEW_SELECTION : Action.NONE;
        }
        return selectedKind == SelectedKind.OTHER_CUBIC
                ? Action.NONE : Action.ACTIVATE_SCHEMA_2;
    }
}
