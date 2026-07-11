package net.celestiald.cavesnotcliffs.world;

import io.github.opencubicchunks.cubicchunks.api.util.IntRange;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldServerMulti;
import net.minecraft.world.WorldType;

/** Hidden schema-1 alias retained so worlds created by the earlier v2 draft keep their terrain. */
public final class CavesNotCliffsWorldType extends WorldType
        implements CavesNotCliffsCubicWorldType {
    public static final int MIN_HEIGHT = -64;
    public static final int MAX_HEIGHT = 320;

    public CavesNotCliffsWorldType() {
        super("cavesnotcliffs");
    }

    @Override
    public ICubeGenerator createCubeGenerator(World world) {
        return new CavesNotCliffsCubeGenerator(world, world.provider.createChunkGenerator(),
                TerrainProfile.DEFAULT);
    }

    @Override
    public IntRange calculateGenerationHeightRange(WorldServer world) {
        return new IntRange(MIN_HEIGHT, MAX_HEIGHT);
    }

    @Override
    public boolean hasCubicGeneratorForWorld(World world) {
        // Secondary WorldServerMulti instances are constructed before their provider receives its
        // final dimension id, so the id alone briefly reports 0 for Nether and End as well.
        return !(world instanceof WorldServerMulti) && world.provider.getDimension() == 0;
    }

    @Override
    public boolean canBeCreated() {
        return false;
    }

    @Override
    public int getTerrainSchema() {
        return CavesNotCliffsWorldData.LEGACY_SCHEMA;
    }

    @Override
    public TerrainProfile getTerrainProfile() {
        return TerrainProfile.DEFAULT;
    }

    public static boolean isCavesNotCliffs(World world) {
        return world != null && world.getWorldType() instanceof CavesNotCliffsCubicWorldType;
    }
}
