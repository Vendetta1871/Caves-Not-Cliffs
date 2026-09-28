package net.celestiald.cavesnotcliffs.world;

import net.minecraft.init.Bootstrap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent.Decorate.EventType;
import net.minecraftforge.event.terraingen.OreGenEvent.GenerateMinable;
import net.minecraftforge.fml.common.eventhandler.Event;
import org.junit.BeforeClass;
import org.junit.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ModdedBiomeFeatureFilterTest {
    private static final BlockPos ORIGIN = new BlockPos(64, 0, -32);

    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    @Test
    public void hostedDecorationKeepsVegetationButNot112TerrainFeatures() throws Exception {
        World world = allocateWorld();
        ProbeBiome biome = new ProbeBiome();

        new ModdedBiomeDecoration().decorateHosted(biome, world, new Random(1L), ORIGIN);

        assertTrue(biome.allowed.get("TREE"));
        assertTrue(biome.allowed.get("FLOWERS"));
        assertTrue(biome.allowed.get("CUSTOM"));
        assertFalse(biome.allowed.get("CLAY"));
        assertFalse(biome.allowed.get("LAKE_WATER"));
        assertFalse(biome.allowed.get("FOSSIL"));
        assertFalse(biome.allowed.get("ore COAL"));
        assertFalse(biome.allowed.get("ore GRAVEL"));
        assertTrue(biome.allowed.get("ore CUSTOM"));
    }

    @Test
    public void everythingPassesOutsideAHostedDecoration() throws Exception {
        World world = allocateWorld();
        ProbeBiome biome = new ProbeBiome();

        // Legacy-layout decoration and the native pipeline's own events are never filtered.
        new ModdedBiomeDecoration().decorate(biome, world, new Random(1L), ORIGIN);

        for (Map.Entry<String, Boolean> entry : biome.allowed.entrySet()) {
            assertTrue(entry.getKey(), entry.getValue());
        }
    }

    @Test
    public void nestedNativePopulationIsNotFiltered() throws Exception {
        World world = allocateWorld();
        ProbeBiome outer = new ProbeBiome() {
            @Override
            public void decorate(World decorated, Random random, BlockPos pos) {
                // A cascading load populates a neighbour from inside the modded decorator.
                World filtering = ModdedBiomeDecoration.suspendFiltering();
                try {
                    super.decorate(decorated, random, pos);
                } finally {
                    ModdedBiomeDecoration.resumeFiltering(filtering);
                }
                assertTrue(ModdedBiomeDecoration.isFiltering(decorated));
            }
        };

        new ModdedBiomeDecoration().decorateHosted(outer, world, new Random(1L), ORIGIN);

        assertTrue(outer.allowed.get("ore COAL"));
        assertTrue(outer.allowed.get("CLAY"));
        assertFalse(ModdedBiomeDecoration.isFiltering(world));
    }

    @Test
    public void onlyTheDecoratedWorldIsFiltered() throws Exception {
        World decorated = allocateWorld();
        World other = allocateWorld();
        ProbeBiome biome = new ProbeBiome() {
            @Override
            public void decorate(World world, Random random, BlockPos pos) {
                super.decorate(other, random, pos);
            }
        };

        new ModdedBiomeDecoration().decorateHosted(biome, decorated, new Random(1L), ORIGIN);

        assertTrue(biome.allowed.get("CLAY"));
        assertEquals(Boolean.TRUE, biome.allowed.get("ore COAL"));
    }

    /**
     * Fires the decorator events a BiomeDecorator would, straight into the filter's handlers:
     * outside a loaded game the Forge buses refuse registrations without an active mod.
     */
    private static class ProbeBiome extends Biome {
        final Map<String, Boolean> allowed = new LinkedHashMap<String, Boolean>();

        ProbeBiome() {
            super(new Biome.BiomeProperties("Probe"));
            setRegistryName("testmod", "probe");
        }

        @Override
        public void decorate(World world, Random random, BlockPos pos) {
            ChunkPos chunk = new ChunkPos(pos);
            for (EventType type : new EventType[] {EventType.TREE, EventType.FLOWERS,
                    EventType.CUSTOM, EventType.CLAY, EventType.LAKE_WATER, EventType.FOSSIL}) {
                DecorateBiomeEvent.Decorate event =
                        new DecorateBiomeEvent.Decorate(world, random, chunk, null, type);
                ModdedBiomeFeatureFilter.onDecorate(event);
                allowed.put(type.name(), event.getResult() != Event.Result.DENY);
            }
            for (GenerateMinable.EventType type : new GenerateMinable.EventType[] {
                    GenerateMinable.EventType.COAL, GenerateMinable.EventType.GRAVEL,
                    GenerateMinable.EventType.CUSTOM}) {
                GenerateMinable event = new GenerateMinable(world, random, null, pos, type);
                ModdedBiomeFeatureFilter.onOre(event);
                allowed.put("ore " + type.name(), event.getResult() != Event.Result.DENY);
            }
        }
    }

    private static World allocateWorld() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (World) ((Unsafe) field.get(null)).allocateInstance(WorldServer.class);
    }
}
