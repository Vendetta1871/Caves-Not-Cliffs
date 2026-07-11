package net.celestiald.cavesnotcliffs.dripstone;

import net.minecraft.util.IStringSerializable;

/**
 * Version-independent Java 1.18.2 pointed-dripstone constants and state transitions.
 *
 * <p>Dry and waterlogged states use separate 1.12 block identities, leaving all four metadata
 * bits available for the ten direction/thickness combinations.</p>
 */
public final class PointedDripstoneMechanics {
    public static final int MAX_DRIP_TYPE_SEARCH = 11;
    public static final int MAX_CAULDRON_SEARCH = 11;
    public static final int MAX_GROWTH_LENGTH = 7;
    public static final int MAX_STALAGMITE_GROWTH_SEARCH = 10;
    public static final int FALLING_DELAY = 2;
    public static final double MIN_TRIDENT_BREAK_SPEED = 0.6D;
    public static final float WATER_CAULDRON_FILL_CHANCE = 0.17578125F;
    public static final float LAVA_CAULDRON_FILL_CHANCE = 0.05859375F;
    public static final float GROWTH_CHANCE = 0.011377778F;
    public static final int FALLING_STALACTITE_MAX_DAMAGE = 40;
    public static final float FALLING_STALACTITE_DAMAGE_PER_DISTANCE = 1.0F;

    private PointedDripstoneMechanics() {
    }

    public enum Thickness implements IStringSerializable {
        TIP_MERGE("tip_merge"),
        TIP("tip"),
        FRUSTUM("frustum"),
        MIDDLE("middle"),
        BASE("base");

        private final String name;

        Thickness(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static final class Neighbor {
        public static final Neighbor OTHER = new Neighbor(false, false, Thickness.TIP);

        private final boolean pointed;
        private final boolean tipUp;
        private final Thickness thickness;

        public Neighbor(boolean pointed, boolean tipUp, Thickness thickness) {
            this.pointed = pointed;
            this.tipUp = tipUp;
            this.thickness = thickness;
        }

        public static Neighbor pointed(boolean tipUp, Thickness thickness) {
            return new Neighbor(true, tipUp, thickness);
        }

        public boolean isPointedInDirection(boolean directionUp) {
            return pointed && tipUp == directionUp;
        }

        public Thickness getThickness() {
            return thickness;
        }
    }

    /** Exact 1.18.2 updateShape/getStateForPlacement thickness transition. */
    public static Thickness calculateThickness(boolean tipUp, Neighbor forward,
            Neighbor backward, boolean mergeOpposingTips) {
        if (forward.isPointedInDirection(!tipUp)) {
            return mergeOpposingTips || forward.getThickness() == Thickness.TIP_MERGE
                    ? Thickness.TIP_MERGE : Thickness.TIP;
        }
        if (!forward.isPointedInDirection(tipUp)) {
            return Thickness.TIP;
        }
        Thickness forwardThickness = forward.getThickness();
        if (forwardThickness == Thickness.TIP
                || forwardThickness == Thickness.TIP_MERGE) {
            return Thickness.FRUSTUM;
        }
        return backward.isPointedInDirection(tipUp)
                ? Thickness.MIDDLE : Thickness.BASE;
    }

    public static boolean canSurvive(boolean supportFaceSolid,
            Neighbor supportNeighbor, boolean tipUp) {
        return supportFaceSolid || supportNeighbor.isPointedInDirection(tipUp);
    }

    public static int metadata(boolean tipUp, Thickness thickness) {
        return (tipUp ? Thickness.values().length : 0) + thickness.ordinal();
    }

    public static boolean tipUpFromMetadata(int metadata) {
        return clampMetadata(metadata) >= Thickness.values().length;
    }

    public static Thickness thicknessFromMetadata(int metadata) {
        return Thickness.values()[clampMetadata(metadata) % Thickness.values().length];
    }

    private static int clampMetadata(int metadata) {
        return Math.max(0, Math.min(Thickness.values().length * 2 - 1, metadata));
    }

    public static boolean shouldBreakFromTrident(double speed) {
        return speed > MIN_TRIDENT_BREAK_SPEED;
    }

    public static int cauldronDelay(int tipY, int cauldronY) {
        return 50 + tipY - cauldronY;
    }

    public static boolean shouldAttemptCauldronFill(boolean lava, float randomValue) {
        return randomValue < (lava ? LAVA_CAULDRON_FILL_CHANCE
                : WATER_CAULDRON_FILL_CHANCE);
    }

    public static float fallingDamagePerDistance(int rootY, int tipY) {
        return FALLING_STALACTITE_DAMAGE_PER_DISTANCE
                * Math.max(1 + rootY - tipY, 6);
    }
}
