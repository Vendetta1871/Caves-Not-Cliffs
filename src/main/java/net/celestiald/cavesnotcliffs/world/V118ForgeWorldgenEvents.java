package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.worldgen.v118.V118OrePlacements.PlacedOre;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.event.terraingen.OreGenEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.terraingen.TerrainGen;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/** Forge 1.12 event facade for the isolated Java 1.18 population pipeline. */
final class V118ForgeWorldgenEvents {
    private static final Map<PlacedOre, WorldGenerator> ORE_GENERATORS = oreGenerators();

    private final V118ChunkGenerator generator;
    private final World world;
    private final int chunkX;
    private final int chunkZ;
    private final Random random;
    private final ChunkPos chunkPos;
    private final BlockPos blockPos;
    private final Map<OreGenEvent.GenerateMinable.EventType, Boolean> oreDecisions =
            new EnumMap<OreGenEvent.GenerateMinable.EventType, Boolean>(
                    OreGenEvent.GenerateMinable.EventType.class);

    V118ForgeWorldgenEvents(V118ChunkGenerator generator, World world,
            int chunkX, int chunkZ) {
        this.generator = generator;
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        random = new Random(VanillaStructureBridge.populationSeed(
                world.getSeed(), chunkX, chunkZ));
        chunkPos = new ChunkPos(chunkX, chunkZ);
        blockPos = new BlockPos(chunkX << 4, 0, chunkZ << 4);
    }

    void populationPre() {
        ForgeEventFactory.onChunkPopulate(true, generator, world, random,
                chunkX, chunkZ, false);
    }

    void populationPost(boolean hasVillageGenerated) {
        ForgeEventFactory.onChunkPopulate(false, generator, world, random,
                chunkX, chunkZ, hasVillageGenerated);
    }

    void decorationPre() {
        MinecraftForge.EVENT_BUS.post(new DecorateBiomeEvent.Pre(
                world, random, chunkPos));
    }

    void decorationPost() {
        MinecraftForge.EVENT_BUS.post(new DecorateBiomeEvent.Post(
                world, random, chunkPos));
    }

    boolean allowPopulation(boolean hasVillageGenerated,
            PopulateChunkEvent.Populate.EventType type) {
        return TerrainGen.populate(generator, world, random, chunkX, chunkZ,
                hasVillageGenerated, type);
    }

    boolean allowDecoration(DecorateBiomeEvent.Decorate.EventType type) {
        return TerrainGen.decorate(world, random, chunkPos, type);
    }

    void orePre() {
        MinecraftForge.ORE_GEN_BUS.post(new OreGenEvent.Pre(world, random, blockPos));
    }

    void orePost() {
        MinecraftForge.ORE_GEN_BUS.post(new OreGenEvent.Post(world, random, blockPos));
    }

    boolean allowOre(PlacedOre ore) {
        OreGenEvent.GenerateMinable.EventType type = oreEventType(ore);
        Boolean allowed = oreDecisions.get(type);
        if (allowed == null) {
            allowed = TerrainGen.generateOre(world, random, ORE_GENERATORS.get(ore),
                    blockPos, type);
            oreDecisions.put(type, allowed);
        }
        return allowed;
    }

    private static Map<PlacedOre, WorldGenerator> oreGenerators() {
        EnumMap<PlacedOre, WorldGenerator> generators =
                new EnumMap<PlacedOre, WorldGenerator>(PlacedOre.class);
        for (PlacedOre ore : PlacedOre.values()) {
            generators.put(ore, new OreEventGenerator(ore));
        }
        return generators;
    }

    private static OreGenEvent.GenerateMinable.EventType oreEventType(PlacedOre ore) {
        switch (ore) {
            case ORE_DIRT:
                return OreGenEvent.GenerateMinable.EventType.DIRT;
            case ORE_GRAVEL:
                return OreGenEvent.GenerateMinable.EventType.GRAVEL;
            case ORE_GRANITE_UPPER:
            case ORE_GRANITE_LOWER:
                return OreGenEvent.GenerateMinable.EventType.GRANITE;
            case ORE_DIORITE_UPPER:
            case ORE_DIORITE_LOWER:
                return OreGenEvent.GenerateMinable.EventType.DIORITE;
            case ORE_ANDESITE_UPPER:
            case ORE_ANDESITE_LOWER:
                return OreGenEvent.GenerateMinable.EventType.ANDESITE;
            case ORE_COAL_UPPER:
            case ORE_COAL_LOWER:
                return OreGenEvent.GenerateMinable.EventType.COAL;
            case ORE_IRON_UPPER:
            case ORE_IRON_MIDDLE:
            case ORE_IRON_SMALL:
                return OreGenEvent.GenerateMinable.EventType.IRON;
            case ORE_GOLD:
            case ORE_GOLD_LOWER:
            case ORE_GOLD_EXTRA:
                return OreGenEvent.GenerateMinable.EventType.GOLD;
            case ORE_REDSTONE:
            case ORE_REDSTONE_LOWER:
                return OreGenEvent.GenerateMinable.EventType.REDSTONE;
            case ORE_DIAMOND:
            case ORE_DIAMOND_LARGE:
            case ORE_DIAMOND_BURIED:
                return OreGenEvent.GenerateMinable.EventType.DIAMOND;
            case ORE_LAPIS:
            case ORE_LAPIS_BURIED:
                return OreGenEvent.GenerateMinable.EventType.LAPIS;
            case ORE_EMERALD:
                return OreGenEvent.GenerateMinable.EventType.EMERALD;
            case ORE_INFESTED:
                return OreGenEvent.GenerateMinable.EventType.SILVERFISH;
            case ORE_TUFF:
            case ORE_COPPER_LARGE:
            case ORE_COPPER:
            case ORE_CLAY:
                return OreGenEvent.GenerateMinable.EventType.CUSTOM;
            default:
                throw new AssertionError(ore);
        }
    }

    /** Event-only descriptor; the exact feature remains in the isolated 1.18 port. */
    private static final class OreEventGenerator extends WorldGenerator {
        private final PlacedOre ore;

        private OreEventGenerator(PlacedOre ore) {
            this.ore = ore;
        }

        @Override
        public boolean generate(World world, Random random, BlockPos position) {
            return false;
        }

        @Override
        public String toString() {
            return "CavesNotCliffs[" + ore.placedId() + ']';
        }
    }
}
