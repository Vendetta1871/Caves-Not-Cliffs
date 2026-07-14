package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavebiomes.api.ExtendedChunkAPI;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.gen.IChunkGenerator;

/** Hidden schema-1 alias retained so worlds created by the earlier v2 draft keep their terrain. */
public final class CavesNotCliffsWorldType extends WorldType
        implements CavesNotCliffsFiniteWorldType {
    public static final int MIN_HEIGHT = -64;
    public static final int MAX_HEIGHT = 320;

    public CavesNotCliffsWorldType() {
        super("cavesnotcliffs");
    }

    @Override
    public IChunkGenerator getChunkGenerator(World world, String generatorOptions) {
        CavesNotCliffsWorldData data = CavesNotCliffsWorldData.read(world.getWorldInfo());
        if (data == null) {
            throw new IllegalStateException("Schema-1 Caves Not Cliffs world has no persisted generator data");
        }
        data.validateGeneratorContract(getTerrainSchema(), WorldType.DEFAULT, TerrainProfile.DEFAULT);
        ExtendedChunkAPI.requireRange("Caves Not Cliffs", MIN_HEIGHT, MAX_HEIGHT);

        String options = data.getGeneratorOptions();
        WorldType selected = world.getWorldInfo().getTerrainType();
        world.getWorldInfo().setTerrainType(WorldType.DEFAULT);
        IChunkGenerator baseGenerator;
        try {
            baseGenerator = WorldType.DEFAULT.getChunkGenerator(
                    world, options == null ? "" : options);
        } finally {
            world.getWorldInfo().setTerrainType(selected);
        }
        return new LegacyFiniteChunkGenerator(world, baseGenerator, data.getTerrainProfile());
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
        return world != null && world.getWorldType() instanceof CavesNotCliffsFiniteWorldType;
    }
}
