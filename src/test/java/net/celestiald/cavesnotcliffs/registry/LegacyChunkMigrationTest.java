package net.celestiald.cavesnotcliffs.registry;

import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
    public void preservesCanonicalAndInternalDraftV2States() throws Exception {
        NBTTagCompound fixture = loadFixture("fixtures/draft_v2_chunk.snbt");
        FixtureVolume volume = new FixtureVolume(fixture,
                LegacyChunkMigration.AMETHYST_BLOCK_PATH,
                LegacyChunkMigration.BUDDING_AMETHYST_PATH);

        LegacyChunkMigration.Result result = LegacyChunkMigration.migrate(
                ContentMigrationVersion.read(fixture), fixture.getLong("seed"), bounds(fixture), volume);

        assertTrue(result.isComplete());
        assertEquals(1, result.getConvertedBlocks());
        assertEquals("amethyst_block", volume.pathAt(-15, -64, 16));
        assertEquals("dripleaf_stem", volume.pathAt(-14, -64, 16));
        assertEquals("pointed_dripstone", volume.pathAt(-13, -64, 16));
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

    private static LegacyChunkMigration.Bounds bounds(NBTTagCompound fixture) {
        return new LegacyChunkMigration.Bounds(
                fixture.getInteger("minX"), fixture.getInteger("minY"),
                fixture.getInteger("minZ"), fixture.getInteger("sizeX"),
                fixture.getInteger("sizeY"), fixture.getInteger("sizeZ"));
    }

    private static final class FixtureVolume implements LegacyChunkMigration.Volume {
        private final NBTTagList blocks;
        private final Set<String> targets;

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

        @Override
        public boolean hasTarget(String registryPath) {
            return targets.contains(registryPath);
        }

        @Override
        public boolean replace(int x, int y, int z, String targetRegistryPath) {
            if (!hasTarget(targetRegistryPath)) {
                return false;
            }
            NBTTagCompound block = entryAt(x, y, z);
            if (block == null) {
                return false;
            }
            block.setString("id", "cavesnotcliffs:" + targetRegistryPath);
            return true;
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
    }
}
