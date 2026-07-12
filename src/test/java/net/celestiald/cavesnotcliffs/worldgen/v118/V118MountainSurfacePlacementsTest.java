package net.celestiald.cavesnotcliffs.worldgen.v118;

import net.celestiald.cavesnotcliffs.worldgen.v118.V118MountainSurfacePlacements.DecorationResult;
import net.minecraft.util.math.BlockPos;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class V118MountainSurfacePlacementsTest {
    @Test
    public void catalogPinsOfficialGlobalFeatureSlots() {
        assertEquals(8, V118MountainSurfacePlacements.FLUID_SPRINGS_STEP);
        assertEquals(2, V118MountainSurfacePlacements.SPRING_LAVA_FROZEN_INDEX);
        assertEquals(9, V118MountainSurfacePlacements.VEGETAL_DECORATION_STEP);
        assertEquals(40, V118MountainSurfacePlacements.TREES_GROVE_INDEX);
        assertEquals(58, V118MountainSurfacePlacements.PATCH_DEAD_BUSH_2_INDEX);
        assertEquals(68, V118MountainSurfacePlacements.PATCH_SUGAR_CANE_INDEX);
        assertEquals(69, V118MountainSurfacePlacements.PATCH_PUMPKIN_INDEX);
        assertEquals(10, V118MountainSurfacePlacements.TOP_LAYER_MODIFICATION_STEP);
        assertEquals(0, V118MountainSurfacePlacements.FREEZE_TOP_LAYER_INDEX);
    }

    @Test
    public void committedOfficialRuntimeOracleMatchesThePort() throws IOException {
        InputStream stream = getClass().getResourceAsStream(
            "/net/celestiald/cavesnotcliffs/worldgen/v118/"
                + "mountain-surface-oracle-1.18.2.tsv");
        assertTrue(stream != null);
        BufferedReader reader = new BufferedReader(new InputStreamReader(
            stream, StandardCharsets.UTF_8));
        List<BlockPos> candidates = V118MountainSurfacePlacements.frozenSpringCandidates(
            RecordingWorld.flat(Surface.AIR, V118Biome.FROZEN_PEAKS),
            123456789L, -3, 5);
        Set<String> membershipIds = new java.util.HashSet<String>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }
            String[] fields = line.split("\\t");
            if ("feature".equals(fields[0])) {
                assertFeatureSlot(fields[1], Integer.parseInt(fields[2]),
                    Integer.parseInt(fields[3]));
            } else if ("spring".equals(fields[0])) {
                assertEquals(new BlockPos(Integer.parseInt(fields[2]),
                    Integer.parseInt(fields[3]), Integer.parseInt(fields[4])),
                    candidates.get(Integer.parseInt(fields[1])));
            } else if ("rarity".equals(fields[0])) {
                long expectedSeed = "patch_sugar_cane".equals(fields[1]) ? 11L : 466L;
                assertEquals(expectedSeed, Long.parseLong(fields[2]));
                int index = "patch_sugar_cane".equals(fields[1])
                    ? V118MountainSurfacePlacements.PATCH_SUGAR_CANE_INDEX
                    : V118MountainSurfacePlacements.PATCH_PUMPKIN_INDEX;
                assertEquals(Float.parseFloat(fields[3]),
                    V118MountainSurfacePlacements.initialFeatureFloat(
                        expectedSeed, 0, 0, index,
                        V118MountainSurfacePlacements.VEGETAL_DECORATION_STEP), 0.0F);
            } else if ("tree".equals(fields[0])) {
                assertTreeOracle(fields);
            } else if ("biome".equals(fields[0])) {
                assertBiomeCatalog(fields);
            } else if ("membership".equals(fields[0])) {
                assertVegetationMembership(fields);
                assertTrue("Duplicate membership oracle: " + fields[1],
                    membershipIds.add(fields[1]));
            }
        }
        reader.close();
        assertEquals(new java.util.HashSet<String>(java.util.Arrays.asList(
            "patch_dead_bush_2", "patch_sugar_cane", "patch_pumpkin")),
            membershipIds);
    }

    @Test
    public void deadBushDecorationMatchesTheOfficialLazyRuntimeOracle()
            throws IOException {
        InputStream stream = getClass().getResourceAsStream(
            "/net/celestiald/cavesnotcliffs/worldgen/v118/"
                + "dead-bush-decoration-oracle-1.18.2.tsv");
        assertTrue(stream != null);
        BufferedReader reader = new BufferedReader(new InputStreamReader(
            stream, StandardCharsets.UTF_8));
        int rows = 0;
        int metadataRows = 0;
        int totalWrites = 0;
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }
            String[] fields = line.split("\\t");
            if (!"trace".equals(fields[0])) {
                assertDeadBushOracleMetadata(fields);
                metadataRows++;
                continue;
            }
            long seed = Long.parseLong(fields[1]);
            int chunkX = Integer.parseInt(fields[2]);
            int chunkZ = Integer.parseInt(fields[3]);
            RecordingWorld world = RecordingWorld.atHeight(
                Surface.SAND, V118Biome.DESERT, 63);

            DecorationResult result = V118MountainSurfacePlacements.decorateVegetation(
                world, seed, chunkX, chunkZ, EnumSet.of(V118Biome.DESERT));

            List<BlockPos> expectedOrigins = parseOracleTokenPositions(fields[6]);
            assertTrue(world.worldSurfaceQueries.size() >= 2);
            assertEquals(expectedOrigins, world.worldSurfaceQueries.subList(0, 2));
            List<BlockPos> expectedCandidates = parseOracleTokenPositions(fields[7]);
            assertEquals(8, expectedCandidates.size());
            assertTrue(world.airQueries.size() >= 8);
            assertEquals(expectedCandidates, world.airQueries.subList(0, 8));
            Set<BlockPos> expectedWrites = new java.util.HashSet<BlockPos>(
                parsePositions(fields[8]));
            assertEquals(expectedWrites, writePositions(world, Cell.DEAD_BUSH));
            assertEquals(expectedWrites.size(), result.deadBushesPlaced());
            List<String> expectedEvents = parseDeadBushEvents(fields[9]);
            assertTrue(world.deadBushEvents.size() >= expectedEvents.size());
            assertEquals(expectedEvents,
                world.deadBushEvents.subList(0, expectedEvents.size()));
            assertEquals(52, Integer.parseInt(fields[10]));
            assertEquals(!expectedWrites.isEmpty(), Boolean.parseBoolean(fields[11]));
            long oracleTrailing = Long.parseLong(fields[12]);
            assertEquals(String.format(java.util.Locale.ROOT, "%016x",
                oracleTrailing), fields[13]);
            totalWrites += result.deadBushesPlaced();
            rows++;
        }
        reader.close();
        assertEquals(6, metadataRows);
        assertEquals(6, rows);
        assertTrue(totalWrites > 0);
    }

    @Test
    public void frozenSpringCandidatesMatchOfficialRuntimeOracle() {
        RecordingWorld world = RecordingWorld.flat(Surface.AIR, V118Biome.FROZEN_PEAKS);
        List<BlockPos> candidates = V118MountainSurfacePlacements.frozenSpringCandidates(
            world, 123456789L, -3, 5);
        assertEquals(new BlockPos(-34, 119, 80), candidates.get(0));
        assertEquals(new BlockPos(-47, -3, 88), candidates.get(1));
        assertEquals(new BlockPos(-33, -29, 89), candidates.get(2));
        assertEquals(new BlockPos(-39, -53, 82), candidates.get(3));
        assertEquals(new BlockPos(-43, -56, 94), candidates.get(4));
    }

    @Test
    public void frozenPeaksSpringAndTopLayerPerformActualWrites() {
        long seed = 123456789L;
        RecordingWorld springWorld = RecordingWorld.flat(
            Surface.AIR, V118Biome.FROZEN_PEAKS);
        BlockPos spring = V118MountainSurfacePlacements.frozenSpringCandidates(
            springWorld, seed, -3, 5).get(0);
        springWorld.base(spring, Cell.SNOW_BLOCK);
        springWorld.base(spring.up(), Cell.SNOW_BLOCK);
        springWorld.base(spring.down(), Cell.SNOW_BLOCK);
        springWorld.base(spring.east(), Cell.SNOW_BLOCK);
        springWorld.base(spring.north(), Cell.SNOW_BLOCK);
        springWorld.base(spring.south(), Cell.SNOW_BLOCK);
        springWorld.base(spring.west(), Cell.AIR);

        DecorationResult springs = V118MountainSurfacePlacements.decorateFrozenSprings(
            springWorld, seed, -3, 5, EnumSet.of(V118Biome.FROZEN_PEAKS));
        assertEquals(1, springs.frozenSpringsPlaced());
        assertEquals(Cell.LAVA, springWorld.cell(spring));
        assertEquals(Collections.singletonList(spring), springWorld.scheduledLava);

        RecordingWorld frozenSurface = RecordingWorld.flat(
            Surface.WATER, V118Biome.FROZEN_PEAKS);
        DecorationResult top = V118MountainSurfacePlacements.decorateTopLayer(
            frozenSurface, 0L, 0, 0, EnumSet.of(V118Biome.FROZEN_PEAKS));
        assertEquals(256, top.waterFrozen());
        assertEquals(256, top.snowLayersPlaced());
        assertEquals(Cell.ICE, frozenSurface.cell(new BlockPos(7, 80, 9)));
        assertEquals(Cell.SNOW_LAYER, frozenSurface.cell(new BlockPos(7, 81, 9)));
    }

    @Test
    public void topLayerFreezesCeilingWaterUsingTheBiomeAtY320() {
        RecordingWorld ceiling = RecordingWorld.atHeight(
            Surface.WATER, V118Biome.STONY_PEAKS, 319);
        ceiling.biomeAtHeight(320, V118Biome.FROZEN_PEAKS);

        DecorationResult top = V118MountainSurfacePlacements.decorateTopLayer(
            ceiling, 0L, 0, 0, EnumSet.of(
                V118Biome.STONY_PEAKS, V118Biome.FROZEN_PEAKS));

        assertEquals(256, top.waterFrozen());
        assertEquals(0, top.snowLayersPlaced());
        assertEquals(Cell.ICE, ceiling.cell(new BlockPos(7, 319, 9)));
        assertEquals(Cell.AIR, ceiling.cell(new BlockPos(7, 320, 9)));
        assertTrue(ceiling.biomeQueries.contains(new BlockPos(7, 320, 9)));
    }

    @Test
    public void topLayerAcceptsNonMountainOriginThenProcessesEveryColumn() {
        RecordingWorld nonMountainOrigin = RecordingWorld.flat(
            Surface.WATER, V118Biome.FROZEN_PEAKS);
        nonMountainOrigin.biomeAtPosition(
            new BlockPos(0, -64, 0), V118Biome.PLAINS);
        DecorationResult originResult = V118MountainSurfacePlacements.decorateTopLayer(
            nonMountainOrigin, 0L, 0, 0, EnumSet.of(
                V118Biome.FROZEN_PEAKS, V118Biome.PLAINS));
        assertEquals(256, originResult.waterFrozen());
        assertEquals(256, originResult.snowLayersPlaced());

        RecordingWorld mixed = RecordingWorld.flat(
            Surface.WATER, V118Biome.SNOWY_TAIGA);
        mixed.biomeAtPosition(new BlockPos(0, -64, 0), V118Biome.FROZEN_PEAKS);
        DecorationResult mixedResult = V118MountainSurfacePlacements.decorateTopLayer(
            mixed, 0L, 0, 0, EnumSet.of(
                V118Biome.FROZEN_PEAKS, V118Biome.SNOWY_TAIGA));
        assertEquals(256, mixedResult.waterFrozen());
        assertEquals(256, mixedResult.snowLayersPlaced());
    }

    @Test
    public void freezeTopLayerBelongsToEveryOfficialOverworldBiome() {
        for (V118Biome biome : V118Biome.values()) {
            assertTrue(biome.id(),
                V118MountainSurfacePlacements.supportsFreezeTopLayer(biome));
        }
    }

    @Test
    public void ceilingHeightmapsStillRunBiomeAndConfiguredFeatureStages() {
        RecordingWorld treeCeiling = RecordingWorld.atHeight(
            Surface.SNOW_BLOCK, V118Biome.GROVE, 319);

        DecorationResult result = V118MountainSurfacePlacements.decorateVegetation(
            treeCeiling, 0L, 0, 0, EnumSet.of(V118Biome.GROVE));

        assertEquals(0, result.treesPlaced());
        assertTrue(treeCeiling.biomeQueries.stream()
            .anyMatch(pos -> pos.getY() == 320));

        RecordingWorld patchCeiling = RecordingWorld.atHeight(
            Surface.WET_GRASS, V118Biome.SNOWY_SLOPES, 319);
        V118MountainSurfacePlacements.decorateVegetation(
            patchCeiling, 11L, 0, 0, EnumSet.of(V118Biome.SNOWY_SLOPES));
        assertTrue(patchCeiling.biomeQueries.stream()
            .anyMatch(pos -> pos.getY() == 320));
    }

    @Test
    public void groveWritesBothNativeTreeShapesThenSnow() {
        RecordingWorld world = RecordingWorld.flat(Surface.SNOW_BLOCK, V118Biome.GROVE);
        DecorationResult vegetation = V118MountainSurfacePlacements.decorateVegetation(
            world, 0L, 0, 0, EnumSet.of(V118Biome.GROVE));

        assertTrue(vegetation.treesPlaced() > 0);
        assertTrue(vegetation.pinesPlaced() > 0);
        assertTrue(vegetation.sprucesPlaced() > 0);
        assertTrue(vegetation.logsPlaced() > 0);
        assertTrue(vegetation.leavesPlaced() > 0);
        assertEquals(vegetation.logsPlaced(), world.writeCount(Cell.SPRUCE_LOG));
        assertEquals(vegetation.leavesPlaced(), world.writeCount(Cell.SPRUCE_LEAVES));

        DecorationResult top = V118MountainSurfacePlacements.decorateTopLayer(
            world, 0L, 0, 0, EnumSet.of(V118Biome.GROVE));
        assertTrue(top.snowLayersPlaced() > 0);
        assertTrue(world.writeCount(Cell.SNOW_LAYER) > 0);
        assertTrue(world.firstWrite(Cell.SPRUCE_LOG) < world.firstWrite(Cell.SNOW_LAYER));
    }

    @Test
    public void officialStageOrderIsVisibleInActualWorldWrites() {
        long seed = 77L;
        RecordingWorld world = RecordingWorld.flat(Surface.SNOW_BLOCK, V118Biome.GROVE);
        BlockPos spring = V118MountainSurfacePlacements.frozenSpringCandidates(
            world, seed, 0, 0).get(0);
        world.base(spring, Cell.SNOW_BLOCK);
        world.base(spring.up(), Cell.SNOW_BLOCK);
        world.base(spring.down(), Cell.SNOW_BLOCK);
        world.base(spring.east(), Cell.SNOW_BLOCK);
        world.base(spring.north(), Cell.SNOW_BLOCK);
        world.base(spring.south(), Cell.SNOW_BLOCK);
        world.base(spring.west(), Cell.AIR);

        decorateAll(world, seed, 0, 0);

        assertTrue(world.firstWrite(Cell.LAVA) >= 0);
        assertTrue(world.firstWrite(Cell.SPRUCE_LOG) > world.firstWrite(Cell.LAVA));
        assertTrue(world.firstWrite(Cell.SNOW_LAYER) > world.firstWrite(Cell.SPRUCE_LOG));
    }

    @Test
    public void snowySlopesDefaultExtraVegetationWritesRepresentedPeers() {
        RecordingWorld caneWorld = RecordingWorld.flat(
            Surface.WET_GRASS, V118Biome.SNOWY_SLOPES);
        DecorationResult cane = V118MountainSurfacePlacements.decorateVegetation(
            caneWorld, 11L, 0, 0, EnumSet.of(V118Biome.SNOWY_SLOPES));
        assertTrue(cane.sugarCanePlaced() > 0);
        assertEquals(cane.sugarCanePlaced(), caneWorld.writeCount(Cell.SUGAR_CANE));

        RecordingWorld pumpkinWorld = RecordingWorld.flat(
            Surface.GRASS, V118Biome.SNOWY_SLOPES);
        DecorationResult pumpkins = V118MountainSurfacePlacements.decorateVegetation(
            pumpkinWorld, 466L, 0, 0, EnumSet.of(V118Biome.SNOWY_SLOPES));
        assertTrue(pumpkins.pumpkinsPlaced() > 0);
        assertEquals(pumpkins.pumpkinsPlaced(), pumpkinWorld.writeCount(Cell.PUMPKIN));
    }

    @Test
    public void desertDeadBushesWriteTheirRepresentedPeer() {
        RecordingWorld world = RecordingWorld.flat(Surface.SAND, V118Biome.DESERT);
        DecorationResult result = V118MountainSurfacePlacements.decorateVegetation(
            world, 0L, 0, 0, EnumSet.of(V118Biome.DESERT));

        assertEquals(4, result.deadBushesPlaced());
        assertEquals(result.deadBushesPlaced(), world.writeCount(Cell.DEAD_BUSH));
        assertEquals(Cell.DEAD_BUSH, world.cell(new BlockPos(11, 81, 9)));
    }

    @Test
    public void deadBushBiomeFilterRunsAfterEachWorldSurfaceLookup() {
        RecordingWorld mixed = RecordingWorld.flat(Surface.SAND, V118Biome.PLAINS);
        mixed.biomeAtPosition(new BlockPos(12, 81, 10), V118Biome.DESERT);

        DecorationResult result = V118MountainSurfacePlacements.decorateVegetation(
            mixed, 0L, 0, 0, EnumSet.of(V118Biome.DESERT, V118Biome.PLAINS));

        assertEquals(3, result.deadBushesPlaced());
        assertEquals(java.util.Arrays.asList(
            new BlockPos(12, 81, 10), new BlockPos(3, 81, 7)),
            mixed.worldSurfaceQueries.subList(0, 2));
        assertEquals(java.util.Arrays.asList(
            new BlockPos(12, 81, 10), new BlockPos(3, 81, 7)),
            mixed.biomeQueries.subList(0, 2));
        assertEquals(java.util.Arrays.asList(
            new BlockPos(10, 80, 13), new BlockPos(11, 81, 9),
            new BlockPos(10, 81, 6), new BlockPos(17, 81, 9)),
            mixed.airQueries.subList(0, 4));
        assertEquals(3, mixed.deadBushSurvivalQueries.size());
    }

    @Test
    public void minimumHeightOriginIsFilteredBeforeTheConfiguredPatch() {
        RecordingWorld world = RecordingWorld.flat(Surface.SAND, V118Biome.DESERT);
        world.overrideNextWorldSurfaceHeight(-64);

        V118MountainSurfacePlacements.decorateVegetation(
            world, 0L, 0, 0, EnumSet.of(V118Biome.DESERT));

        assertEquals(2, world.worldSurfaceQueries.size());
        assertEquals(new BlockPos(12, -64, 10), world.worldSurfaceQueries.get(0));
        assertEquals(4, world.airQueries.size());
    }

    @Test
    public void mixedDesertRegionsInvokeDeadBushPlacementOnlyOnce() {
        RecordingWorld single = RecordingWorld.flat(Surface.SAND, V118Biome.DESERT);
        RecordingWorld mixed = RecordingWorld.flat(Surface.SAND, V118Biome.DESERT);
        DecorationResult singleResult = V118MountainSurfacePlacements.decorateVegetation(
            single, 0L, 0, 0, EnumSet.of(V118Biome.DESERT));
        DecorationResult mixedResult = V118MountainSurfacePlacements.decorateVegetation(
            mixed, 0L, 0, 0, EnumSet.of(
                V118Biome.BADLANDS, V118Biome.DESERT, V118Biome.PLAINS));

        assertEquals(singleResult.deadBushesPlaced(), mixedResult.deadBushesPlaced());
        assertEquals(writePositions(single, Cell.DEAD_BUSH),
            writePositions(mixed, Cell.DEAD_BUSH));
    }

    @Test
    public void patchDeadBush2BelongsOnlyToTheDesert() {
        int supported = 0;
        for (V118Biome biome : V118Biome.values()) {
            if (V118MountainSurfacePlacements.supportsDeadBush2(biome)) {
                supported++;
            }
        }
        assertEquals(1, supported);
        assertTrue(V118MountainSurfacePlacements.supportsDeadBush2(V118Biome.DESERT));
        assertFalse(V118MountainSurfacePlacements.supportsDeadBush2(
            V118Biome.BADLANDS));
        assertFalse(V118MountainSurfacePlacements.supportsDeadBush2(
            V118Biome.WOODED_BADLANDS));
    }

    @Test
    public void ordinaryCaneAndPumpkinUseTheirExactBiomeMemberships() {
        int caneBiomes = 0;
        int pumpkinBiomes = 0;
        for (V118Biome biome : V118Biome.values()) {
            if (V118MountainSurfacePlacements.supportsOrdinarySugarCane(biome)) {
                caneBiomes++;
            }
            if (V118MountainSurfacePlacements.supportsPumpkin(biome)) {
                pumpkinBiomes++;
            }
        }
        assertEquals(50, V118Biome.values().length);
        assertEquals(40, caneBiomes);
        assertEquals(45, pumpkinBiomes);

        assertFalse(V118MountainSurfacePlacements.supportsOrdinarySugarCane(
            V118Biome.DESERT));
        assertTrue(V118MountainSurfacePlacements.supportsPumpkin(V118Biome.DESERT));
        assertFalse(V118MountainSurfacePlacements.supportsOrdinarySugarCane(
            V118Biome.FROZEN_PEAKS));
        assertFalse(V118MountainSurfacePlacements.supportsPumpkin(V118Biome.FROZEN_PEAKS));
        assertFalse(V118MountainSurfacePlacements.supportsOrdinarySugarCane(
            V118Biome.LUSH_CAVES));
        assertFalse(V118MountainSurfacePlacements.supportsPumpkin(V118Biome.LUSH_CAVES));
        assertTrue(V118MountainSurfacePlacements.supportsOrdinarySugarCane(
            V118Biome.SNOWY_SLOPES));
        assertTrue(V118MountainSurfacePlacements.supportsPumpkin(V118Biome.SNOWY_SLOPES));
    }

    @Test
    public void mixedFeatureRegionsInvokeEachSharedPlacementOnlyOnce() {
        RecordingWorld singleCane = RecordingWorld.flat(
            Surface.WET_GRASS, V118Biome.PLAINS);
        RecordingWorld mixedCane = RecordingWorld.flat(
            Surface.WET_GRASS, V118Biome.PLAINS);
        DecorationResult singleCaneResult = V118MountainSurfacePlacements.decorateVegetation(
            singleCane, 11L, 0, 0, EnumSet.of(V118Biome.PLAINS));
        DecorationResult mixedCaneResult = V118MountainSurfacePlacements.decorateVegetation(
            mixedCane, 11L, 0, 0, EnumSet.of(
                V118Biome.PLAINS, V118Biome.GROVE, V118Biome.SNOWY_SLOPES,
                V118Biome.DRIPSTONE_CAVES));
        assertTrue(singleCaneResult.sugarCanePlaced() > 0);
        assertEquals(singleCaneResult.sugarCanePlaced(),
            mixedCaneResult.sugarCanePlaced());
        assertEquals(singleCane.writes, mixedCane.writes);

        RecordingWorld singlePumpkin = RecordingWorld.flat(
            Surface.GRASS, V118Biome.DESERT);
        RecordingWorld mixedPumpkin = RecordingWorld.flat(
            Surface.GRASS, V118Biome.DESERT);
        DecorationResult singlePumpkinResult = V118MountainSurfacePlacements.decorateVegetation(
            singlePumpkin, 466L, 0, 0, EnumSet.of(V118Biome.DESERT));
        DecorationResult mixedPumpkinResult = V118MountainSurfacePlacements.decorateVegetation(
            mixedPumpkin, 466L, 0, 0, EnumSet.of(
                V118Biome.BADLANDS, V118Biome.DESERT, V118Biome.ERODED_BADLANDS,
                V118Biome.SWAMP, V118Biome.WOODED_BADLANDS));
        assertTrue(singlePumpkinResult.pumpkinsPlaced() > 0);
        assertEquals(singlePumpkinResult.pumpkinsPlaced(),
            mixedPumpkinResult.pumpkinsPlaced());
        assertEquals(singlePumpkin.writes, mixedPumpkin.writes);
    }

    @Test
    public void candidateBiomeFilterRejectsUnsupportedBiomesInMixedRegions() {
        RecordingWorld caneWorld = RecordingWorld.flat(
            Surface.WET_GRASS, V118Biome.LUSH_CAVES);
        DecorationResult cane = V118MountainSurfacePlacements.decorateVegetation(
            caneWorld, 11L, 0, 0, EnumSet.of(
                V118Biome.LUSH_CAVES, V118Biome.PLAINS));
        assertEquals(0, cane.sugarCanePlaced());
        assertFalse(caneWorld.biomeQueries.isEmpty());

        RecordingWorld pumpkinWorld = RecordingWorld.flat(
            Surface.GRASS, V118Biome.LUSH_CAVES);
        DecorationResult pumpkin = V118MountainSurfacePlacements.decorateVegetation(
            pumpkinWorld, 466L, 0, 0, EnumSet.of(
                V118Biome.LUSH_CAVES, V118Biome.DESERT));
        assertEquals(0, pumpkin.pumpkinsPlaced());
        assertFalse(pumpkinWorld.biomeQueries.isEmpty());
    }

    @Test
    public void adjacentCanonicalPassesRetainCrossBorderTreeWrites() {
        RecordingWorld world = RecordingWorld.flat(Surface.SNOW_BLOCK, V118Biome.GROVE);
        decorateAll(world, 918273645L, 0, 0);
        assertTrue(world.writes.entrySet().stream().anyMatch(entry ->
            entry.getKey().getX() >= 16
                && (entry.getValue() == Cell.SPRUCE_LOG
                    || entry.getValue() == Cell.SPRUCE_LEAVES)));
        decorateAll(world, 918273645L, 1, 0);
    }

    private static BlockPos parsePosition(String value) {
        String[] coordinates = value.split(",");
        return new BlockPos(Integer.parseInt(coordinates[0]),
            Integer.parseInt(coordinates[1]), Integer.parseInt(coordinates[2]));
    }

    private static List<BlockPos> parseOracleTokenPositions(String value) {
        List<BlockPos> result = new ArrayList<BlockPos>();
        for (String token : value.split(";")) {
            int start = token.indexOf('@') + 1;
            int end = token.indexOf(':', start);
            result.add(parsePosition(token.substring(start,
                end < 0 ? token.length() : end)));
        }
        return result;
    }

    private static List<String> parseDeadBushEvents(String value) {
        List<String> result = new ArrayList<String>();
        for (String token : value.split(">")) {
            int at = token.indexOf('@');
            String position = token.substring(at + 1);
            if (token.startsWith("w@")) {
                result.add("write:" + position);
            } else if (token.substring(0, at).contains("a")) {
                result.add("air:" + position);
            } else {
                result.add("surface:" + position);
            }
        }
        return result;
    }

    private static void assertDeadBushOracleMetadata(String[] fields) {
        if ("slot".equals(fields[0])) {
            assertEquals("patch_dead_bush_2", fields[1]);
            assertEquals("9", fields[2]);
            assertEquals("58", fields[3]);
        } else if ("membership".equals(fields[0])) {
            assertEquals("patch_dead_bush_2", fields[1]);
            assertEquals("minecraft:desert", fields[2]);
        } else if ("placed_json".equals(fields[0])) {
            assertTrue(fields[1].contains("\"count\":2"));
            assertTrue(fields[1].contains("\"heightmap\":\"WORLD_SURFACE_WG\""));
        } else if ("configured_json".equals(fields[0])) {
            assertTrue(fields[1].contains("\"tries\":4"));
            assertTrue(fields[1].contains("\"xz_spread\":7"));
            assertTrue(fields[1].contains("\"y_spread\":3"));
            assertTrue(fields[1].contains("\"Name\":\"minecraft:dead_bush\""));
        } else if ("support_explicit".equals(fields[0])) {
            assertTrue(fields[1].contains("minecraft:red_sand"));
            assertTrue(fields[1].contains("minecraft:black_terracotta"));
        } else {
            assertEquals("support_tag", fields[0]);
            assertEquals("minecraft:dirt", fields[1]);
            assertTrue(fields[2].contains("minecraft:rooted_dirt"));
            assertTrue(fields[2].contains("minecraft:moss_block"));
        }
    }

    private static List<BlockPos> parsePositions(String value) {
        if ("-".equals(value)) {
            return Collections.emptyList();
        }
        List<BlockPos> result = new ArrayList<BlockPos>();
        for (String position : value.split(";")) {
            result.add(parsePosition(position));
        }
        return result;
    }

    private static Set<BlockPos> writePositions(RecordingWorld world, Cell cell) {
        Set<BlockPos> result = new java.util.HashSet<BlockPos>();
        for (Map.Entry<BlockPos, Cell> entry : world.writes.entrySet()) {
            if (entry.getValue() == cell) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    private static void assertTreeOracle(String[] fields) {
        V118MountainTreeFeature.Kind kind =
            V118MountainTreeFeature.Kind.valueOf(fields[1].toUpperCase(java.util.Locale.ROOT));
        long seed = Long.parseLong(fields[2]);
        RecordingWorld world = RecordingWorld.atHeight(
            Surface.SNOW_BLOCK, V118Biome.GROVE, 63);
        Random random = new Random(seed);
        V118MountainTreeFeature.Result result = V118MountainTreeFeature.place(
            world, random, new BlockPos(-19, 64, 37), kind);

        assertEquals(Boolean.parseBoolean(fields[3]), result.placed());
        assertEquals(Integer.parseInt(fields[4]), result.logs());
        assertEquals(Long.parseLong(fields[5]), hashWrites(world, Cell.SPRUCE_LOG));
        assertEquals(Integer.parseInt(fields[6]), result.leaves());
        assertEquals(Long.parseLong(fields[7]), hashWrites(world, Cell.SPRUCE_LEAVES));
        assertEquals(Long.parseLong(fields[8]), random.nextLong());
    }

    private static long hashWrites(RecordingWorld world, Cell cell) {
        List<BlockPos> positions = new ArrayList<BlockPos>();
        for (Map.Entry<BlockPos, Cell> entry : world.writes.entrySet()) {
            if (entry.getValue() == cell) {
                positions.add(entry.getKey());
            }
        }
        Collections.sort(positions, Comparator
            .comparingInt(BlockPos::getY)
            .thenComparingInt(BlockPos::getX)
            .thenComparingInt(BlockPos::getZ));
        long hash = 0xcbf29ce484222325L;
        for (BlockPos position : positions) {
            hash ^= packJava118(position);
            hash *= 0x100000001b3L;
        }
        return hash;
    }

    private static long packJava118(BlockPos position) {
        return ((long) position.getX() & 0x3FFFFFFL) << 38
            | ((long) position.getZ() & 0x3FFFFFFL) << 12
            | (long) position.getY() & 0xFFFL;
    }

    private static void assertBiomeCatalog(String[] fields) {
        V118Biome biome = V118Biome.valueOf(fields[1].toUpperCase(java.util.Locale.ROOT));
        Set<String> features = fields.length < 3 || fields[2].isEmpty()
            ? Collections.<String>emptySet()
            : new java.util.HashSet<String>(java.util.Arrays.asList(fields[2].split(",")));
        assertEquals(features.contains("spring_lava_frozen"),
            V118MountainSurfacePlacements.supportsFrozenSpring(biome));
        assertEquals(features.contains("trees_grove"),
            V118MountainSurfacePlacements.supportsGroveTrees(biome));
        assertEquals(features.contains("patch_dead_bush_2"),
            V118MountainSurfacePlacements.supportsDeadBush2(biome));
        assertEquals(features.contains("patch_sugar_cane"),
            V118MountainSurfacePlacements.supportsOrdinarySugarCane(biome));
        assertEquals(features.contains("patch_pumpkin"),
            V118MountainSurfacePlacements.supportsPumpkin(biome));
        assertEquals(features.contains("freeze_top_layer"),
            V118MountainSurfacePlacements.supportsFreezeTopLayer(biome));
    }

    private static void assertVegetationMembership(String[] fields) {
        Set<V118Biome> expected = EnumSet.noneOf(V118Biome.class);
        for (String id : fields[2].split(",")) {
            expected.add(V118Biome.valueOf(id.toUpperCase(java.util.Locale.ROOT)));
        }
        boolean deadBush = "patch_dead_bush_2".equals(fields[1]);
        boolean cane = "patch_sugar_cane".equals(fields[1]);
        assertTrue("Unknown vegetation membership: " + fields[1],
            deadBush || cane || "patch_pumpkin".equals(fields[1]));
        assertEquals(deadBush ? 1 : cane ? 40 : 45, expected.size());
        for (V118Biome biome : V118Biome.values()) {
            boolean actual = deadBush
                ? V118MountainSurfacePlacements.supportsDeadBush2(biome)
                : cane
                    ? V118MountainSurfacePlacements.supportsOrdinarySugarCane(biome)
                    : V118MountainSurfacePlacements.supportsPumpkin(biome);
            assertEquals(biome.id(), expected.contains(biome), actual);
        }
    }

    private static void decorateAll(RecordingWorld world, long seed, int chunkX, int chunkZ) {
        Set<V118Biome> biomes = EnumSet.of(V118Biome.GROVE);
        V118MountainSurfacePlacements.decorateFrozenSprings(
            world, seed, chunkX, chunkZ, biomes);
        V118MountainSurfacePlacements.decorateVegetation(
            world, seed, chunkX, chunkZ, biomes);
        V118MountainSurfacePlacements.decorateTopLayer(
            world, seed, chunkX, chunkZ, biomes);
    }

    private static void assertFeatureSlot(String id, int step, int index) {
        if ("spring_lava_frozen".equals(id)) {
            assertEquals(V118MountainSurfacePlacements.FLUID_SPRINGS_STEP, step);
            assertEquals(V118MountainSurfacePlacements.SPRING_LAVA_FROZEN_INDEX, index);
        } else if ("trees_grove".equals(id)) {
            assertEquals(V118MountainSurfacePlacements.VEGETAL_DECORATION_STEP, step);
            assertEquals(V118MountainSurfacePlacements.TREES_GROVE_INDEX, index);
        } else if ("patch_dead_bush_2".equals(id)) {
            assertEquals(V118MountainSurfacePlacements.VEGETAL_DECORATION_STEP, step);
            assertEquals(V118MountainSurfacePlacements.PATCH_DEAD_BUSH_2_INDEX, index);
        } else if ("patch_sugar_cane".equals(id)) {
            assertEquals(V118MountainSurfacePlacements.VEGETAL_DECORATION_STEP, step);
            assertEquals(V118MountainSurfacePlacements.PATCH_SUGAR_CANE_INDEX, index);
        } else if ("patch_pumpkin".equals(id)) {
            assertEquals(V118MountainSurfacePlacements.VEGETAL_DECORATION_STEP, step);
            assertEquals(V118MountainSurfacePlacements.PATCH_PUMPKIN_INDEX, index);
        } else if ("freeze_top_layer".equals(id)) {
            assertEquals(V118MountainSurfacePlacements.TOP_LAYER_MODIFICATION_STEP, step);
            assertEquals(V118MountainSurfacePlacements.FREEZE_TOP_LAYER_INDEX, index);
        } else {
            throw new AssertionError("Unknown oracle feature: " + id);
        }
    }

    private enum Surface {
        AIR,
        SNOW_BLOCK,
        WATER,
        GRASS,
        SAND,
        WET_GRASS
    }

    private enum Cell {
        AIR,
        STONE,
        DIRT,
        GRASS,
        SAND,
        SNOW_BLOCK,
        POWDER_SNOW,
        PACKED_ICE,
        WATER,
        LAVA,
        ICE,
        SNOW_LAYER,
        SPRUCE_LOG,
        SPRUCE_LEAVES,
        DEAD_BUSH,
        SUGAR_CANE,
        PUMPKIN
    }

    private static final class RecordingWorld
            implements V118MountainSurfacePlacements.WorldAccess {
        private static final int SURFACE_Y = 80;
        private final Surface surface;
        private final V118Biome biome;
        private final int surfaceY;
        private final Map<BlockPos, Cell> base = new HashMap<BlockPos, Cell>();
        private final Map<BlockPos, Cell> writes = new HashMap<BlockPos, Cell>();
        private final Map<Integer, V118Biome> heightBiomes =
            new HashMap<Integer, V118Biome>();
        private final Map<BlockPos, V118Biome> positionBiomes =
            new HashMap<BlockPos, V118Biome>();
        private final EnumMap<Cell, Integer> writeCounts = new EnumMap<Cell, Integer>(Cell.class);
        private final List<Cell> writeOrder = new ArrayList<Cell>();
        private final List<BlockPos> scheduledLava = new ArrayList<BlockPos>();
        private final List<BlockPos> biomeQueries = new ArrayList<BlockPos>();
        private final List<BlockPos> worldSurfaceQueries = new ArrayList<BlockPos>();
        private final List<BlockPos> airQueries = new ArrayList<BlockPos>();
        private final List<BlockPos> deadBushSurvivalQueries =
            new ArrayList<BlockPos>();
        private final List<String> deadBushEvents = new ArrayList<String>();
        private final List<Integer> worldSurfaceHeightOverrides =
            new ArrayList<Integer>();

        static RecordingWorld flat(Surface surface, V118Biome biome) {
            return atHeight(surface, biome, SURFACE_Y);
        }

        static RecordingWorld atHeight(Surface surface, V118Biome biome, int surfaceY) {
            return new RecordingWorld(surface, biome, surfaceY);
        }

        private RecordingWorld(Surface surface, V118Biome biome, int surfaceY) {
            this.surface = surface;
            this.biome = biome;
            this.surfaceY = surfaceY;
        }

        void biomeAtHeight(int y, V118Biome heightBiome) {
            heightBiomes.put(y, heightBiome);
        }

        void biomeAtPosition(BlockPos pos, V118Biome positionBiome) {
            positionBiomes.put(pos.toImmutable(), positionBiome);
        }

        void base(BlockPos pos, Cell cell) {
            base.put(pos.toImmutable(), cell);
        }

        void overrideNextWorldSurfaceHeight(int height) {
            worldSurfaceHeightOverrides.add(height);
        }

        Cell cell(BlockPos pos) {
            Cell written = writes.get(pos);
            if (written != null) {
                return written;
            }
            Cell override = base.get(pos);
            if (override != null) {
                return override;
            }
            if (pos.getY() < surfaceY) {
                return Cell.STONE;
            }
            if (pos.getY() > surfaceY) {
                return Cell.AIR;
            }
            switch (surface) {
                case SNOW_BLOCK:
                    return Cell.SNOW_BLOCK;
                case WATER:
                    return Cell.WATER;
                case GRASS:
                    return Cell.GRASS;
                case SAND:
                    return Cell.SAND;
                case WET_GRASS:
                    return Math.floorMod(pos.getX(), 2) == 0 ? Cell.GRASS : Cell.WATER;
                case AIR:
                default:
                    return Cell.AIR;
            }
        }

        int writeCount(Cell cell) {
            Integer count = writeCounts.get(cell);
            return count == null ? 0 : count;
        }

        int firstWrite(Cell cell) {
            return writeOrder.indexOf(cell);
        }

        private void write(BlockPos pos, Cell cell) {
            writes.put(pos.toImmutable(), cell);
            Integer count = writeCounts.get(cell);
            writeCounts.put(cell, count == null ? 1 : count + 1);
            writeOrder.add(cell);
        }

        @Override
        public int minBuildHeight() {
            return -64;
        }

        @Override
        public int maxBuildHeight() {
            return 320;
        }

        @Override
        public V118Biome biomeAt(BlockPos pos) {
            biomeQueries.add(pos.toImmutable());
            V118Biome positionBiome = positionBiomes.get(pos);
            if (positionBiome != null) {
                return positionBiome;
            }
            V118Biome heightBiome = heightBiomes.get(pos.getY());
            return heightBiome == null ? biome : heightBiome;
        }

        @Override
        public int worldSurfaceHeight(int blockX, int blockZ) {
            int height = worldSurfaceHeightOverrides.isEmpty()
                ? firstAvailable(blockX, blockZ, false, false)
                : worldSurfaceHeightOverrides.remove(0);
            worldSurfaceQueries.add(new BlockPos(blockX, height, blockZ));
            deadBushEvents.add("surface:" + positionString(blockX, height, blockZ));
            return height;
        }

        @Override
        public int oceanFloorHeight(int blockX, int blockZ) {
            return firstAvailable(blockX, blockZ, true, false);
        }

        @Override
        public int motionBlockingHeight(int blockX, int blockZ) {
            return firstAvailable(blockX, blockZ, true, true);
        }

        private int firstAvailable(int x, int z, boolean motion, boolean fluids) {
            for (int y = 319; y >= -64; --y) {
                Cell cell = cell(new BlockPos(x, y, z));
                if (!motion && cell != Cell.AIR || motion && blocksMotion(cell)
                        || motion && fluids && isFluid(cell)) {
                    return y + 1;
                }
            }
            return -64;
        }

        private static boolean blocksMotion(Cell cell) {
            return cell == Cell.STONE || cell == Cell.DIRT || cell == Cell.GRASS
                || cell == Cell.SAND
                || cell == Cell.SNOW_BLOCK || cell == Cell.PACKED_ICE || cell == Cell.ICE
                || cell == Cell.SPRUCE_LOG || cell == Cell.SPRUCE_LEAVES
                || cell == Cell.PUMPKIN;
        }

        private static boolean isFluid(Cell cell) {
            return cell == Cell.WATER || cell == Cell.LAVA;
        }

        @Override
        public int blockLight(BlockPos pos) {
            return 0;
        }

        @Override
        public boolean isAir(BlockPos pos) {
            airQueries.add(pos.toImmutable());
            deadBushEvents.add("air:" + positionString(
                pos.getX(), pos.getY(), pos.getZ()));
            return cell(pos) == Cell.AIR;
        }

        @Override
        public boolean isWater(BlockPos pos) {
            return cell(pos) == Cell.WATER;
        }

        @Override
        public boolean isPowderSnow(BlockPos pos) {
            return cell(pos) == Cell.POWDER_SNOW;
        }

        @Override
        public boolean isSnowTreeSupport(BlockPos pos) {
            Cell cell = cell(pos);
            return cell == Cell.SNOW_BLOCK || cell == Cell.POWDER_SNOW;
        }

        @Override
        public boolean isFrozenSpringValid(BlockPos pos) {
            Cell cell = cell(pos);
            return cell == Cell.SNOW_BLOCK || cell == Cell.POWDER_SNOW
                || cell == Cell.PACKED_ICE;
        }

        @Override
        public boolean isGrassBlock(BlockPos pos) {
            return cell(pos) == Cell.GRASS;
        }

        @Override
        public boolean canDeadBushSurvive(BlockPos pos) {
            deadBushSurvivalQueries.add(pos.toImmutable());
            Cell below = cell(pos.down());
            return below == Cell.DIRT || below == Cell.GRASS || below == Cell.SAND;
        }

        @Override
        public boolean canSugarCaneSurvive(BlockPos pos) {
            Cell below = cell(pos.down());
            return (below == Cell.GRASS || below == Cell.DIRT)
                && hasAdjacentWaterBelow(pos);
        }

        @Override
        public boolean hasAdjacentWaterBelow(BlockPos pos) {
            BlockPos below = pos.down();
            return isWater(below.east()) || isWater(below.west())
                || isWater(below.north()) || isWater(below.south());
        }

        @Override
        public boolean canSnowSurvive(BlockPos pos) {
            return blocksMotion(cell(pos.down()));
        }

        @Override
        public boolean isFree(BlockPos pos) {
            return isValidTreePos(pos) || cell(pos) == Cell.SPRUCE_LOG;
        }

        @Override
        public boolean isValidTreePos(BlockPos pos) {
            Cell cell = cell(pos);
            return cell == Cell.AIR || cell == Cell.SPRUCE_LEAVES
                || cell == Cell.WATER || cell == Cell.SUGAR_CANE;
        }

        @Override
        public void setDirt(BlockPos pos) {
            if (cell(pos) != Cell.DIRT) {
                write(pos, Cell.DIRT);
            }
        }

        @Override
        public void setSpruceLog(BlockPos pos) {
            write(pos, Cell.SPRUCE_LOG);
        }

        @Override
        public void setSpruceLeaves(BlockPos pos) {
            write(pos, Cell.SPRUCE_LEAVES);
        }

        @Override
        public void setLava(BlockPos pos) {
            write(pos, Cell.LAVA);
        }

        @Override
        public void scheduleLavaTick(BlockPos pos) {
            scheduledLava.add(pos.toImmutable());
        }

        @Override
        public void setIce(BlockPos pos) {
            write(pos, Cell.ICE);
        }

        @Override
        public void setSnowLayer(BlockPos pos) {
            write(pos, Cell.SNOW_LAYER);
        }

        @Override
        public void setDeadBush(BlockPos pos) {
            deadBushEvents.add("write:" + positionString(
                pos.getX(), pos.getY(), pos.getZ()));
            write(pos, Cell.DEAD_BUSH);
        }

        private static String positionString(int x, int y, int z) {
            return x + "," + y + "," + z;
        }

        @Override
        public void setSugarCane(BlockPos pos) {
            write(pos, Cell.SUGAR_CANE);
        }

        @Override
        public void setPumpkin(BlockPos pos) {
            write(pos, Cell.PUMPKIN);
        }
    }
}
