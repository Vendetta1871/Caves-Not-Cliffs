package net.celestiald.cavesnotcliffs.world;

import io.github.opencubicchunks.cubicchunks.api.util.IntRange;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldServerMulti;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.ChunkGeneratorSettings;
import net.minecraft.world.gen.layer.GenLayer;

import java.util.Random;
import java.util.function.Supplier;

/** Hidden ICubicWorldType which preserves a selected two-dimensional generator as its base. */
public final class CavesNotCliffsWorldTypeWrapper extends WorldType
        implements CavesNotCliffsCubicWorldType {
    private final WorldType baseType;
    private final TerrainProfile terrainProfile;

    CavesNotCliffsWorldTypeWrapper(String name, WorldType baseType, TerrainProfile terrainProfile) {
        super(name);
        this.baseType = baseType;
        this.terrainProfile = terrainProfile;
    }

    public WorldType getBaseType() {
        return baseType;
    }

    @Override
    public int getTerrainSchema() {
        return CavesNotCliffsWorldData.CURRENT_SCHEMA;
    }

    @Override
    public TerrainProfile getTerrainProfile() {
        return terrainProfile;
    }

    @Override
    public ICubeGenerator createCubeGenerator(World world) {
        CavesNotCliffsWorldData data = CavesNotCliffsWorldData.read(world.getWorldInfo());
        if (data == null) {
            throw new IllegalStateException("Schema-2 Caves Not Cliffs world has no persisted generator data");
        }
        data.validateGeneratorContract(getTerrainSchema(), baseType, terrainProfile);
        TerrainProfile persistedProfile = data.getTerrainProfile();
        if (V118CubicChunksGenerator.isNativeProfile(persistedProfile)) {
            return new V118CubicChunksGenerator(world, persistedProfile);
        }
        String options = data.getGeneratorOptions();
        IChunkGenerator baseGenerator = delegate(world,
                () -> baseType.getChunkGenerator(world, options == null ? "" : options));
        return new CavesNotCliffsCubeGenerator(world, baseGenerator, persistedProfile);
    }

    @Override
    public IntRange calculateGenerationHeightRange(WorldServer world) {
        return new IntRange(CavesNotCliffsWorldType.MIN_HEIGHT, CavesNotCliffsWorldType.MAX_HEIGHT);
    }

    @Override
    public boolean hasCubicGeneratorForWorld(World world) {
        return !(world instanceof WorldServerMulti) && world.provider.getDimension() == 0;
    }

    @Override
    public boolean canBeCreated() {
        return false;
    }

    @Override
    public boolean isCustomizable() {
        return false;
    }

    @Override
    public BiomeProvider getBiomeProvider(World world) {
        return delegate(world, () -> baseType.getBiomeProvider(world));
    }

    @Override
    public IChunkGenerator getChunkGenerator(World world, String generatorOptions) {
        return delegate(world, () -> baseType.getChunkGenerator(world, generatorOptions));
    }

    @Override
    public int getMinimumSpawnHeight(World world) {
        return delegate(world, () -> baseType.getMinimumSpawnHeight(world));
    }

    @Override
    public double getHorizon(World world) {
        return delegate(world, () -> baseType.getHorizon(world));
    }

    @Override
    public double voidFadeMagnitude() {
        return baseType.voidFadeMagnitude();
    }

    @Override
    public boolean handleSlimeSpawnReduction(Random random, World world) {
        return delegate(world, () -> baseType.handleSlimeSpawnReduction(random, world));
    }

    @Override
    public int getSpawnFuzz(WorldServer world, MinecraftServer server) {
        return delegate(world, () -> baseType.getSpawnFuzz(world, server));
    }

    @Override
    public float getCloudHeight() {
        return baseType.getCloudHeight();
    }

    @Override
    public GenLayer getBiomeLayer(long seed, GenLayer parent, ChunkGeneratorSettings settings) {
        return baseType.getBiomeLayer(seed, parent, settings);
    }

    private <T> T delegate(World world, Supplier<T> operation) {
        WorldType selected = world.getWorldInfo().getTerrainType();
        world.getWorldInfo().setTerrainType(baseType);
        try {
            return operation.get();
        } finally {
            world.getWorldInfo().setTerrainType(selected);
        }
    }
}
