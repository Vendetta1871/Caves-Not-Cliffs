package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavebiomes.api.IExtendedPopulationGenerator;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.IChunkGenerator;

import java.util.List;

/**
 * Transparent schema-2 adapter for selected two-dimensional generators.
 *
 * <p>CaveBiomesAPI owns the finite section array. The selected generator remains responsible for
 * every terrain, population, structure, and creature decision; sections outside its native range
 * stay empty. This is deliberately separate from the draft-v2 schema-1 compatibility generator,
 * which must retain its historical deepslate extension and replay journal.</p>
 */
class DelegatingFiniteChunkGenerator implements IChunkGenerator {
    private final IChunkGenerator delegate;

    DelegatingFiniteChunkGenerator(IChunkGenerator delegate) {
        if (delegate == null) {
            throw new NullPointerException("delegate");
        }
        this.delegate = delegate;
    }

    static IChunkGenerator wrap(IChunkGenerator delegate) {
        return delegate instanceof IExtendedPopulationGenerator
                ? new Extended(delegate, ((IExtendedPopulationGenerator) delegate)
                        .getPopulationRadius())
                : new DelegatingFiniteChunkGenerator(delegate);
    }

    @Override
    public Chunk generateChunk(int chunkX, int chunkZ) {
        return delegate.generateChunk(chunkX, chunkZ);
    }

    @Override
    public void populate(int chunkX, int chunkZ) {
        delegate.populate(chunkX, chunkZ);
    }

    @Override
    public boolean generateStructures(Chunk chunk, int chunkX, int chunkZ) {
        return delegate.generateStructures(chunk, chunkX, chunkZ);
    }

    @Override
    public void recreateStructures(Chunk chunk, int chunkX, int chunkZ) {
        delegate.recreateStructures(chunk, chunkX, chunkZ);
    }

    @Override
    public List<Biome.SpawnListEntry> getPossibleCreatures(
            EnumCreatureType type, BlockPos pos) {
        return delegate.getPossibleCreatures(type, pos);
    }

    @Override
    public BlockPos getNearestStructurePos(World world, String name, BlockPos pos,
            boolean findUnexplored) {
        return delegate.getNearestStructurePos(world, name, pos, findUnexplored);
    }

    @Override
    public boolean isInsideStructure(World world, String name, BlockPos pos) {
        return delegate.isInsideStructure(world, name, pos);
    }

    /** Preserves CaveBiomesAPI's opt-in population scheduling contract when the base uses it. */
    private static final class Extended extends DelegatingFiniteChunkGenerator
            implements IExtendedPopulationGenerator {
        private final int populationRadius;

        private Extended(IChunkGenerator delegate, int populationRadius) {
            super(delegate);
            this.populationRadius = populationRadius;
        }

        @Override
        public int getPopulationRadius() {
            return populationRadius;
        }
    }
}
