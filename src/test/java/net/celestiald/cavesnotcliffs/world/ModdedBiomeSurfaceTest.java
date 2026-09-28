package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.worldgen.v118.TerrainColumn;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118Biome;
import net.minecraft.block.BlockSand;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ModdedBiomeSurfaceTest {
    private static final int GROUND = 70;

    private final Map<Integer, ExtendedBlockStorage> sections =
            new HashMap<Integer, ExtendedBlockStorage>();
    private final ModdedBiomeSurface.Sections chunk = y -> sections.get(y >> 4);
    private V118Biome[] hosts;
    private Biome[] claims;

    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    @Before
    public void plainsColumns() {
        hosts = new V118Biome[TerrainColumn.SURFACE_BIOME_COUNT];
        Arrays.fill(hosts, V118Biome.PLAINS);
        claims = new Biome[TerrainColumn.SURFACE_BIOME_COUNT];
        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                set(x, GROUND - 5, z, Blocks.STONE.getDefaultState());
                for (int y = GROUND - 4; y < GROUND; ++y) {
                    set(x, y, z, Blocks.DIRT.getDefaultState());
                }
                set(x, GROUND, z, Blocks.GRASS.getDefaultState());
            }
        }
    }

    @Test
    public void claimedColumnsGetTheModdedTopAndFiller() {
        IBlockState redSand = Blocks.SAND.getDefaultState()
                .withProperty(BlockSand.VARIANT, BlockSand.EnumType.RED_SAND);
        claims[0] = soilBiome(redSand, Blocks.CLAY.getDefaultState());

        surface().apply(chunk, hosts, claims);

        assertSame(redSand, get(0, GROUND, 0));
        for (int y = GROUND - 4; y < GROUND; ++y) {
            assertSame(Blocks.CLAY.getDefaultState(), get(0, y, 0));
        }
        assertSame(Blocks.STONE.getDefaultState(), get(0, GROUND - 5, 0));
        // Unclaimed columns keep the 1.18 soil.
        assertSame(Blocks.GRASS.getDefaultState(), get(1, GROUND, 0));
        assertSame(Blocks.DIRT.getDefaultState(), get(1, GROUND - 1, 0));
    }

    @Test
    public void unchangedTopStillLetsTheFillerChange() {
        claims[17] = soilBiome(Blocks.GRASS.getDefaultState(), Blocks.CLAY.getDefaultState());

        surface().apply(chunk, hosts, claims);

        assertSame(Blocks.GRASS.getDefaultState(), get(1, GROUND, 1));
        assertSame(Blocks.CLAY.getDefaultState(), get(1, GROUND - 1, 1));
    }

    @Test
    public void nonSoilBlocksFallBackToTheHostSoil() {
        claims[0] = soilBiome(Blocks.LEAVES.getDefaultState(), Blocks.CHEST.getDefaultState());

        surface().apply(chunk, hosts, claims);

        assertSame(Blocks.GRASS.getDefaultState(), get(0, GROUND, 0));
        assertSame(Blocks.DIRT.getDefaultState(), get(0, GROUND - 1, 0));
    }

    @Test
    public void onlyTheHostsOwnSoilIsReplaced() {
        // A 1.18 surface rule chose gravel here; it is not the host's soil and stays.
        set(2, GROUND, 0, Blocks.GRAVEL.getDefaultState());
        claims[2] = soilBiome(Blocks.MYCELIUM.getDefaultState(), Blocks.CLAY.getDefaultState());

        surface().apply(chunk, hosts, claims);

        assertSame(Blocks.GRAVEL.getDefaultState(), get(2, GROUND, 0));
        assertSame(Blocks.DIRT.getDefaultState(), get(2, GROUND - 1, 0));
    }

    @Test
    public void shallowFloorsAreDressedButDeepWaterHidesTheFloor() {
        for (int y = GROUND + 1; y <= GROUND + 3; ++y) {
            set(3, y, 0, Blocks.WATER.getDefaultState());
        }
        for (int y = GROUND + 1; y <= GROUND + 6; ++y) {
            set(4, y, 0, Blocks.WATER.getDefaultState());
        }
        Biome claim = soilBiome(Blocks.MYCELIUM.getDefaultState(), Blocks.CLAY.getDefaultState());
        claims[3] = claim;
        claims[4] = claim;

        surface().apply(chunk, hosts, claims);

        assertSame(Blocks.MYCELIUM.getDefaultState(), get(3, GROUND, 0));
        assertSame(Blocks.GRASS.getDefaultState(), get(4, GROUND, 0));
    }

    @Test
    public void stoneHostsAreNeverDressed() {
        Arrays.fill(hosts, V118Biome.STONY_SHORE);
        set(5, GROUND, 0, Blocks.STONE.getDefaultState());
        claims[5] = soilBiome(Blocks.SAND.getDefaultState(), Blocks.SAND.getDefaultState());

        surface().apply(chunk, hosts, claims);

        assertSame(Blocks.STONE.getDefaultState(), get(5, GROUND, 0));
        assertEquals(Blocks.DIRT, get(5, GROUND - 1, 0).getBlock());
    }

    private static ModdedBiomeSurface surface() {
        Biome[] projection = new Biome[V118Biome.values().length];
        Arrays.fill(projection, Biomes.PLAINS);
        projection[V118Biome.STONY_SHORE.ordinal()] = Biomes.STONE_BEACH;
        return new ModdedBiomeSurface(new V118BiomeMapper(projection));
    }

    private static Biome soilBiome(IBlockState top, IBlockState filler) {
        Biome biome = new Biome(new Biome.BiomeProperties("Soil")) {
        };
        biome.setRegistryName("testmod", "soil");
        biome.topBlock = top;
        biome.fillerBlock = filler;
        return biome;
    }

    private void set(int x, int y, int z, IBlockState state) {
        ExtendedBlockStorage section = sections.get(y >> 4);
        if (section == null) {
            section = new ExtendedBlockStorage(y & ~15, true);
            sections.put(y >> 4, section);
        }
        section.set(x, y & 15, z, state);
    }

    private IBlockState get(int x, int y, int z) {
        return sections.get(y >> 4).get(x, y & 15, z);
    }
}
