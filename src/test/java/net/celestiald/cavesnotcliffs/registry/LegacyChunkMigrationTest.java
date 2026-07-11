package net.celestiald.cavesnotcliffs.registry;

import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LegacyChunkMigrationTest {
    @Test
    public void migratesV15FixtureExactlyOnceAndPreservesSiblingNbt() throws Exception {
        NBTTagCompound fixture = loadFixture("fixtures/v1_5_chunk.snbt");
        FixtureVolume volume = new FixtureVolume(fixture,
                LegacyChunkMigration.AMETHYST_BLOCK_PATH,
                LegacyChunkMigration.BUDDING_AMETHYST_PATH);
        LegacyChunkMigration.Bounds bounds = bounds(fixture);

        assertEquals(CncDataVersions.LEGACY_CONTENT_VERSION,
                ContentMigrationVersion.read(fixture));
        assertTrue(LegacyChunkMigration.isBuddingCandidate(0L, 0, 0, 0));

        LegacyChunkMigration.Result first = LegacyChunkMigration.migrate(
                ContentMigrationVersion.read(fixture), fixture.getLong("seed"), bounds, volume);
        assertTrue(first.isComplete());
        assertEquals(3, first.getConvertedBlocks());
        assertEquals(0, first.getDeferredBlocks());
        assertFalse(LegacyChunkMigration.containsLegacyGeode(bounds, volume));
        assertEquals(LegacyChunkMigration.BUDDING_AMETHYST_PATH,
                volume.pathAt(0, 0, 0));

        ContentMigrationVersion.write(fixture, first.getResultingVersion());
        assertEquals(15, fixture.getCompoundTag("cavesnotcliffs").getInteger("preserved"));

        String beforeSecondPass = fixture.toString();
        LegacyChunkMigration.Result second = LegacyChunkMigration.migrate(
                ContentMigrationVersion.read(fixture), fixture.getLong("seed"), bounds, volume);
        assertEquals(0, second.getConvertedBlocks());
        assertEquals(0, second.getDeferredBlocks());
        assertEquals(beforeSecondPass, fixture.toString());
    }

    @Test
    public void convertsDraftV2StateSplitBlocksToCanonicalStorage() throws Exception {
        NBTTagCompound fixture = loadFixture("fixtures/draft_v2_chunk.snbt");
        FixtureVolume volume = new FixtureVolume(fixture,
                LegacyChunkMigration.AMETHYST_BLOCK_PATH,
                LegacyChunkMigration.BUDDING_AMETHYST_PATH,
                "big_dripleaf_stem");

        LegacyChunkMigration.Result result = LegacyChunkMigration.migrate(
                ContentMigrationVersion.read(fixture), fixture.getLong("seed"), bounds(fixture), volume);

        assertTrue(result.isComplete());
        assertEquals(2, result.getConvertedBlocks());
        assertEquals("amethyst_block", volume.pathAt(-15, -64, 16));
        assertEquals("big_dripleaf_stem", volume.pathAt(-14, -64, 16));
        assertEquals(2, volume.metadataAt(-14, -64, 16));
        assertEquals("pointed_dripstone", volume.pathAt(-13, -64, 16));
    }

    @Test
    public void convertsEveryLegacyLushCompanionWithoutInventedItems() {
        NBTTagCompound fixture = new NBTTagCompound();
        fixture.setInteger("minX", 0);
        fixture.setInteger("minY", 0);
        fixture.setInteger("minZ", 0);
        fixture.setInteger("sizeX", 6);
        fixture.setInteger("sizeY", 6);
        fixture.setInteger("sizeZ", 1);
        fixture.setLong("seed", 0L);
        ContentMigrationVersion.write(fixture,
                CncDataVersions.CANONICAL_REGISTRY_CONTENT_VERSION);
        NBTTagList blocks = new NBTTagList();
        addBlock(blocks, 0, 4, 0, "glow_berry_vines");
        addBlock(blocks, 0, 3, 0, "glow_berry_middle_fill");
        addBlock(blocks, 0, 2, 0, "glow_berry_vines");
        addBlock(blocks, 1, 0, 0, "baby_dripleaf");
        addBlock(blocks, 2, 0, 0, "dripleaf_stem");
        addBlock(blocks, 3, 0, 0, "dripleafplant_1");
        addBlock(blocks, 4, 0, 0, "dripleaf_plant_2");
        fixture.setTag("blocks", blocks);

        FixtureVolume volume = new FixtureVolume(fixture,
                "cave_vines_plant", "cave_vines_age_24_25",
                "small_dripleaf", "big_dripleaf_stem", "big_dripleaf");
        LegacyChunkMigration.Result result = LegacyChunkMigration.migrate(
                ContentMigrationVersion.read(fixture), 0L, bounds(fixture), volume);

        assertTrue(result.isComplete());
        assertEquals(8, result.getConvertedBlocks());
        assertEquals("cave_vines_plant", volume.pathAt(0, 4, 0));
        assertEquals(1, volume.metadataAt(0, 4, 0));
        assertEquals("cave_vines_plant", volume.pathAt(0, 3, 0));
        assertEquals(0, volume.metadataAt(0, 3, 0));
        assertEquals("cave_vines_age_24_25", volume.pathAt(0, 2, 0));
        assertEquals(9, volume.metadataAt(0, 2, 0));
        assertEquals("small_dripleaf", volume.pathAt(1, 0, 0));
        assertEquals(2, volume.metadataAt(1, 0, 0));
        assertEquals("small_dripleaf", volume.pathAt(1, 1, 0));
        assertEquals(6, volume.metadataAt(1, 1, 0));
        assertEquals(2, volume.metadataAt(2, 0, 0));
        assertEquals(10, volume.metadataAt(3, 0, 0));
        assertEquals(14, volume.metadataAt(4, 0, 0));
        assertEquals(Arrays.asList("3,0,0:big_dripleaf#10",
                "4,0,0:big_dripleaf#100"), volume.scheduled);
        assertFalse(LegacyChunkMigration.containsLegacyLushState(bounds(fixture), volume));
    }

    @Test
    public void defersBuddingCandidateWithoutAdvancingVersion() throws Exception {
        NBTTagCompound fixture = loadFixture("fixtures/v1_5_chunk.snbt");
        FixtureVolume volume = new FixtureVolume(fixture,
                LegacyChunkMigration.AMETHYST_BLOCK_PATH);

        LegacyChunkMigration.Result result = LegacyChunkMigration.migrate(
                ContentMigrationVersion.read(fixture), fixture.getLong("seed"), bounds(fixture), volume);

        assertFalse(result.isComplete());
        assertTrue(result.getDeferredBlocks() >= 1);
        assertEquals(LegacyChunkMigration.LEGACY_GEODE_PATH, volume.pathAt(0, 0, 0));
        assertEquals(CncDataVersions.LEGACY_CONTENT_VERSION, result.getResultingVersion());
    }

    @Test
    public void canonicalizesV15InventoryFixtureExactlyOnce() throws Exception {
        NBTTagCompound fixture = loadFixture("fixtures/v1_5_chunk.snbt");
        NBTTagList inventory = fixture.getTagList("inventory", 10);

        assertEquals(4, LegacyInventoryMigration.migrateSerializedStacks(inventory));
        assertEquals("cavesnotcliffs:amethyst_block",
                inventory.getCompoundTagAt(0).getString("id"));
        assertEquals("cavesnotcliffs:glow_berries",
                inventory.getCompoundTagAt(1).getString("id"));
        assertEquals("cavesnotcliffs:big_dripleaf",
                inventory.getCompoundTagAt(2).getString("id"));
        assertEquals("cavesnotcliffs:pointed_dripstone",
                inventory.getCompoundTagAt(3).getString("id"));
        assertEquals("minecraft:stone", inventory.getCompoundTagAt(4).getString("id"));
        assertEquals(0, LegacyInventoryMigration.migrateSerializedStacks(inventory));
    }

    @Test
    public void consolidatesEveryLegacyPointedSegmentWithoutTraversalDependence()
            throws Exception {
        NBTTagCompound fixture = loadFixture("fixtures/draft_v2_dripstone_chunk.snbt");
        FixtureVolume volume = new FixtureVolume(fixture,
                LegacyChunkMigration.POINTED_DRIPSTONE_PATH);

        LegacyChunkMigration.Result result = LegacyChunkMigration.migrate(
                ContentMigrationVersion.read(fixture), fixture.getLong("seed"),
                bounds(fixture), volume);

        assertTrue(result.isComplete());
        assertEquals(12, result.getConvertedBlocks());
        assertEquals(0, result.getDeferredBlocks());
        assertFalse(LegacyChunkMigration.containsLegacyContent(bounds(fixture), volume));
        assertEquals(27, fixture.getCompoundTag("cavesnotcliffs").getInteger("preserved"));

        // Two-block roots are frustums; three-block roots are bases. Tips retain direction.
        assertState(volume, 0, 3, 0, false,
                net.celestiald.cavesnotcliffs.dripstone.PointedDripstoneMechanics.Thickness.FRUSTUM);
        assertState(volume, 1, 3, 0, false,
                net.celestiald.cavesnotcliffs.dripstone.PointedDripstoneMechanics.Thickness.BASE);
        assertState(volume, 1, 2, 0, false,
                net.celestiald.cavesnotcliffs.dripstone.PointedDripstoneMechanics.Thickness.MIDDLE);
        assertState(volume, 0, 2, 0, false,
                net.celestiald.cavesnotcliffs.dripstone.PointedDripstoneMechanics.Thickness.TIP);
        assertState(volume, 0, 0, 1, true,
                net.celestiald.cavesnotcliffs.dripstone.PointedDripstoneMechanics.Thickness.FRUSTUM);
        assertState(volume, 1, 0, 1, true,
                net.celestiald.cavesnotcliffs.dripstone.PointedDripstoneMechanics.Thickness.BASE);
        assertState(volume, 1, 1, 1, true,
                net.celestiald.cavesnotcliffs.dripstone.PointedDripstoneMechanics.Thickness.MIDDLE);
        assertState(volume, 2, 3, 0, false,
                net.celestiald.cavesnotcliffs.dripstone.PointedDripstoneMechanics.Thickness.TIP);
        assertState(volume, 2, 0, 1, true,
                net.celestiald.cavesnotcliffs.dripstone.PointedDripstoneMechanics.Thickness.TIP);
    }

    @Test
    public void defersLegacyPointedSegmentsWhenCanonicalStateIsUnavailable()
            throws Exception {
        NBTTagCompound fixture = loadFixture("fixtures/draft_v2_dripstone_chunk.snbt");
        FixtureVolume volume = new FixtureVolume(fixture);
        LegacyChunkMigration.Result result = LegacyChunkMigration.migrate(
                ContentMigrationVersion.read(fixture), fixture.getLong("seed"),
                bounds(fixture), volume);
        assertFalse(result.isComplete());
        assertEquals(12, result.getDeferredBlocks());
        assertEquals(CncDataVersions.CANONICAL_REGISTRY_CONTENT_VERSION,
                result.getResultingVersion());
        assertEquals("bottom_stalactite", volume.pathAt(0, 3, 0));
    }

    @Test
    public void readsPointedTopologyAcrossBothVerticalCubeBoundaries() {
        NBTTagCompound fixture = emptyCubeFixture(
                CncDataVersions.CANONICAL_REGISTRY_CONTENT_VERSION);
        NBTTagList blocks = fixture.getTagList("blocks", 10);
        addBlock(blocks, 0, 16, 0, "bottom_stalactite");
        addBlock(blocks, 0, 15, 0, "top_stalactite");
        addBlock(blocks, 1, 31, 0, "bottom_stalagmite");
        addBlock(blocks, 1, 32, 0, "top_stalagmite");

        FixtureVolume volume = new FixtureVolume(fixture,
                LegacyChunkMigration.POINTED_DRIPSTONE_PATH);
        LegacyChunkMigration.Result result = LegacyChunkMigration.migrate(
                ContentMigrationVersion.read(fixture), 0L, bounds(fixture), volume);

        assertTrue(result.isComplete());
        assertEquals(2, result.getConvertedBlocks());
        assertState(volume, 0, 16, 0, false,
                net.celestiald.cavesnotcliffs.dripstone.PointedDripstoneMechanics.Thickness.FRUSTUM);
        assertState(volume, 1, 31, 0, true,
                net.celestiald.cavesnotcliffs.dripstone.PointedDripstoneMechanics.Thickness.FRUSTUM);
        assertEquals("top_stalactite", volume.pathAt(0, 15, 0));
        assertEquals("top_stalagmite", volume.pathAt(1, 32, 0));
    }

    @Test
    public void defersPointedRootsUntilTheirBoundaryHaloIsLoaded() {
        NBTTagCompound fixture = emptyCubeFixture(
                CncDataVersions.CANONICAL_REGISTRY_CONTENT_VERSION);
        NBTTagList blocks = fixture.getTagList("blocks", 10);
        addBlock(blocks, 0, 16, 0, "bottom_stalactite");
        addBlock(blocks, 1, 31, 0, "bottom_stalagmite");

        FixtureVolume volume = new FixtureVolume(fixture,
                LegacyChunkMigration.POINTED_DRIPSTONE_PATH);
        volume.makeUnavailable(0, 15, 0);
        volume.makeUnavailable(1, 32, 0);
        LegacyChunkMigration.Result result = LegacyChunkMigration.migrate(
                ContentMigrationVersion.read(fixture), 0L, bounds(fixture), volume);

        assertFalse(result.isComplete());
        assertEquals(CncDataVersions.CANONICAL_REGISTRY_CONTENT_VERSION,
                result.getResultingVersion());
        assertEquals(0, result.getConvertedBlocks());
        assertEquals(2, result.getDeferredBlocks());
        assertEquals("bottom_stalactite", volume.pathAt(0, 16, 0));
        assertEquals("bottom_stalagmite", volume.pathAt(1, 31, 0));
        assertTrue(LegacyChunkMigration.containsLegacyContent(bounds(fixture), volume));
    }

    @Test
    public void classifiesCaveVineAtCubeBottomFromLoadedHalo() {
        NBTTagCompound fixture = emptyCubeFixture(
                CncDataVersions.POINTED_DRIPSTONE_CONTENT_VERSION);
        NBTTagList blocks = fixture.getTagList("blocks", 10);
        addBlock(blocks, 0, 16, 0, "glow_berry_vines");
        addBlock(blocks, 0, 15, 0, "glow_berry_middle_fill");

        FixtureVolume volume = new FixtureVolume(fixture, "cave_vines_plant");
        LegacyChunkMigration.Result result = LegacyChunkMigration.migrate(
                ContentMigrationVersion.read(fixture), 0L, bounds(fixture), volume);

        assertTrue(result.isComplete());
        assertEquals(1, result.getConvertedBlocks());
        assertEquals("cave_vines_plant", volume.pathAt(0, 16, 0));
        assertEquals(1, volume.metadataAt(0, 16, 0));
        assertEquals("glow_berry_middle_fill", volume.pathAt(0, 15, 0));
    }

    @Test
    public void defersBottomCaveVineInsteadOfMisclassifyingMissingHaloAsAir() {
        NBTTagCompound fixture = emptyCubeFixture(
                CncDataVersions.POINTED_DRIPSTONE_CONTENT_VERSION);
        addBlock(fixture.getTagList("blocks", 10), 0, 16, 0, "glow_berry_vines");

        FixtureVolume volume = new FixtureVolume(fixture,
                "cave_vines_plant", "cave_vines_age_24_25");
        volume.makeUnavailable(0, 15, 0);
        LegacyChunkMigration.Result result = LegacyChunkMigration.migrate(
                ContentMigrationVersion.read(fixture), 0L, bounds(fixture), volume);

        assertFalse(result.isComplete());
        assertEquals(CncDataVersions.POINTED_DRIPSTONE_CONTENT_VERSION,
                result.getResultingVersion());
        assertEquals(0, result.getConvertedBlocks());
        assertEquals(1, result.getDeferredBlocks());
        assertEquals("glow_berry_vines", volume.pathAt(0, 16, 0));
        assertTrue(LegacyChunkMigration.containsLegacyContent(bounds(fixture), volume));
    }

    @Test
    public void atomicallyCreatesSmallDripleafUpperHalfAcrossCubeTop() {
        NBTTagCompound fixture = emptyCubeFixture(
                CncDataVersions.POINTED_DRIPSTONE_CONTENT_VERSION);
        addBlock(fixture.getTagList("blocks", 10), 0, 31, 0, "baby_dripleaf");

        FixtureVolume volume = new FixtureVolume(fixture, "small_dripleaf");
        LegacyChunkMigration.Result result = LegacyChunkMigration.migrate(
                ContentMigrationVersion.read(fixture), 0L, bounds(fixture), volume);

        assertTrue(result.isComplete());
        assertEquals(2, result.getConvertedBlocks());
        assertEquals("small_dripleaf", volume.pathAt(0, 31, 0));
        assertEquals(2, volume.metadataAt(0, 31, 0));
        assertEquals("small_dripleaf", volume.pathAt(0, 32, 0));
        assertEquals(6, volume.metadataAt(0, 32, 0));
    }

    @Test
    public void defersSmallDripleafPairWithoutChangingLowerHalfWhenTopCubeIsAbsent() {
        NBTTagCompound fixture = emptyCubeFixture(
                CncDataVersions.POINTED_DRIPSTONE_CONTENT_VERSION);
        addBlock(fixture.getTagList("blocks", 10), 0, 31, 0, "baby_dripleaf");

        FixtureVolume volume = new FixtureVolume(fixture, "small_dripleaf");
        volume.makeUnavailable(0, 32, 0);
        LegacyChunkMigration.Result result = LegacyChunkMigration.migrate(
                ContentMigrationVersion.read(fixture), 0L, bounds(fixture), volume);

        assertFalse(result.isComplete());
        assertEquals(CncDataVersions.POINTED_DRIPSTONE_CONTENT_VERSION,
                result.getResultingVersion());
        assertEquals(0, result.getConvertedBlocks());
        assertEquals(1, result.getDeferredBlocks());
        assertEquals("baby_dripleaf", volume.pathAt(0, 31, 0));
        assertNull(volume.pathAt(0, 32, 0));
        assertTrue(LegacyChunkMigration.containsLegacyContent(bounds(fixture), volume));

        volume.makeAvailable(0, 32, 0);
        LegacyChunkMigration.Result retry = LegacyChunkMigration.migrate(
                result.getResultingVersion(), 0L, bounds(fixture), volume);
        assertTrue(retry.isComplete());
        assertEquals(2, retry.getConvertedBlocks());
        assertEquals("small_dripleaf", volume.pathAt(0, 31, 0));
        assertEquals("small_dripleaf", volume.pathAt(0, 32, 0));
    }

    private static void assertState(FixtureVolume volume, int x, int y, int z,
            boolean tipUp,
            net.celestiald.cavesnotcliffs.dripstone.PointedDripstoneMechanics.Thickness thickness) {
        assertEquals(LegacyChunkMigration.POINTED_DRIPSTONE_PATH, volume.pathAt(x, y, z));
        int metadata = volume.metadataAt(x, y, z);
        assertEquals(tipUp,
                net.celestiald.cavesnotcliffs.dripstone.PointedDripstoneMechanics
                        .tipUpFromMetadata(metadata));
        assertEquals(thickness,
                net.celestiald.cavesnotcliffs.dripstone.PointedDripstoneMechanics
                        .thicknessFromMetadata(metadata));
    }

    private static NBTTagCompound loadFixture(String resource) throws Exception {
        InputStream stream = LegacyChunkMigrationTest.class.getClassLoader()
                .getResourceAsStream(resource);
        if (stream == null) {
            throw new IllegalArgumentException("Missing fixture " + resource);
        }
        try (Scanner scanner = new Scanner(stream, "UTF-8").useDelimiter("\\A")) {
            return JsonToNBT.getTagFromJson(scanner.hasNext() ? scanner.next() : "{}");
        }
    }

    private static NBTTagCompound emptyCubeFixture(int contentVersion) {
        NBTTagCompound fixture = new NBTTagCompound();
        fixture.setInteger("minX", 0);
        fixture.setInteger("minY", 16);
        fixture.setInteger("minZ", 0);
        fixture.setInteger("sizeX", 2);
        fixture.setInteger("sizeY", 16);
        fixture.setInteger("sizeZ", 1);
        fixture.setTag("blocks", new NBTTagList());
        ContentMigrationVersion.write(fixture, contentVersion);
        return fixture;
    }

    private static LegacyChunkMigration.Bounds bounds(NBTTagCompound fixture) {
        return new LegacyChunkMigration.Bounds(
                fixture.getInteger("minX"), fixture.getInteger("minY"),
                fixture.getInteger("minZ"), fixture.getInteger("sizeX"),
                fixture.getInteger("sizeY"), fixture.getInteger("sizeZ"));
    }

    private static void addBlock(NBTTagList blocks, int x, int y, int z, String path) {
        NBTTagCompound block = new NBTTagCompound();
        block.setInteger("x", x);
        block.setInteger("y", y);
        block.setInteger("z", z);
        block.setString("id", "cavesnotcliffs:" + path);
        blocks.appendTag(block);
    }

    private static final class FixtureVolume implements LegacyChunkMigration.Volume {
        private final NBTTagList blocks;
        private final Set<String> targets;
        private final List<String> scheduled = new ArrayList<>();
        private final Set<String> unavailable = new HashSet<>();

        private FixtureVolume(NBTTagCompound fixture, String... targets) {
            this.blocks = fixture.getTagList("blocks", 10);
            this.targets = new HashSet<>(Arrays.asList(targets));
        }

        @Override
        public String blockPathAt(int x, int y, int z) {
            NBTTagCompound block = entryAt(x, y, z);
            if (block == null) {
                return null;
            }
            ResourceLocation id = new ResourceLocation(block.getString("id"));
            return "cavesnotcliffs".equals(id.getResourceDomain())
                    ? id.getResourcePath() : null;
        }

        private String pathAt(int x, int y, int z) {
            return blockPathAt(x, y, z);
        }

        private int metadataAt(int x, int y, int z) {
            NBTTagCompound block = entryAt(x, y, z);
            return block == null ? 0 : block.getInteger("meta");
        }

        @Override
        public int blockMetadataAt(int x, int y, int z) {
            return metadataAt(x, y, z);
        }

        @Override
        public boolean isAirAt(int x, int y, int z) {
            return entryAt(x, y, z) == null;
        }

        @Override
        public boolean isPositionAvailable(int x, int y, int z) {
            return !unavailable.contains(coordinates(x, y, z));
        }

        @Override
        public boolean hasTarget(String registryPath) {
            return targets.contains(registryPath);
        }

        @Override
        public boolean replace(int x, int y, int z, String targetRegistryPath) {
            return replace(x, y, z, targetRegistryPath, 0);
        }

        @Override
        public boolean replace(int x, int y, int z, String targetRegistryPath,
                int metadata) {
            if (!hasTarget(targetRegistryPath)) {
                return false;
            }
            NBTTagCompound block = entryAt(x, y, z);
            if (block == null) {
                block = new NBTTagCompound();
                block.setInteger("x", x);
                block.setInteger("y", y);
                block.setInteger("z", z);
                blocks.appendTag(block);
            }
            block.setString("id", "cavesnotcliffs:" + targetRegistryPath);
            block.setInteger("meta", metadata);
            return true;
        }

        @Override
        public boolean replacePair(int firstX, int firstY, int firstZ,
                String firstTarget, int firstMetadata, int secondX, int secondY, int secondZ,
                String secondTarget, int secondMetadata) {
            if (!isPositionAvailable(firstX, firstY, firstZ)
                    || !isPositionAvailable(secondX, secondY, secondZ)
                    || !hasTarget(firstTarget) || !hasTarget(secondTarget)) {
                return false;
            }
            // Both writes are now guaranteed for this in-memory fixture; write the created upper
            // half first so the source lower remains legacy until the pair can be completed.
            replace(secondX, secondY, secondZ, secondTarget, secondMetadata);
            replace(firstX, firstY, firstZ, firstTarget, firstMetadata);
            return true;
        }

        @Override
        public void scheduleUpdate(int x, int y, int z, String targetRegistryPath,
                int delay) {
            scheduled.add(x + "," + y + "," + z + ':' + targetRegistryPath + '#'
                    + delay);
        }

        private NBTTagCompound entryAt(int x, int y, int z) {
            for (int index = 0; index < blocks.tagCount(); index++) {
                NBTTagCompound block = blocks.getCompoundTagAt(index);
                if (block.getInteger("x") == x && block.getInteger("y") == y
                        && block.getInteger("z") == z) {
                    return block;
                }
            }
            return null;
        }

        private void makeUnavailable(int x, int y, int z) {
            unavailable.add(coordinates(x, y, z));
        }

        private void makeAvailable(int x, int y, int z) {
            unavailable.remove(coordinates(x, y, z));
        }

        private String coordinates(int x, int y, int z) {
            return x + "," + y + "," + z;
        }
    }
}
