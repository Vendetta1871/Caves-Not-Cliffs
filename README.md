# Caves Not Cliffs [Backported]

Caves Not Cliffs 2.0 backports Java 1.18.2 Overworld terrain and its represented Caves & Cliffs
content to Minecraft 1.12.2. New Overworlds use a default-on, finite CaveBiomesAPI-backed schema
from Y=-64 through Y=319, while existing worlds keep their saved generator contract.

***

## Key features

### Native Java 1.18.2 Overworld generation

- Default, Large Biomes, and Amplified profiles backed by the ported positional RNG, noise,
  spline, climate, density-cave, aquifer, carver, and surface-rule stacks
- Complete deterministic `16×384×16` columns with bedrock, the Y=0..8 deepslate transition,
  ore veins, fluid scheduling, and a bounded weighted cache
- A virtual 3D biome resolver with a legacy surface-biome projection for Minecraft 1.12
  spawning, colors, and chunk storage
- The six available Minecraft 1.12 structure families retained through a structure-only bridge
- `/cncbiome` for querying the resolved biome at any position

### Caves, mountains, and native features

![Biomes](https://github.com/user-attachments/assets/c15db242-fe78-4cb1-abbc-a748d9996448)

- Lush and dripstone cave biomes with Java 1.18.2 feature ordering
- Meadow, Grove, Snowy Slopes, Jagged Peaks, Frozen Peaks, and Stony Peaks
- Exact ordinary ore bands and exposure reduction, large copper and iron veins, soft disks,
  underwater magma, tuff placement, and amethyst geodes
- Functional terrain-generated powder snow, buckets, dispensers, layered cauldrons, sinking,
  freezing, and leather protection

### Faithful block and crafting families

![Blocks](https://github.com/user-attachments/assets/d22c54a2-9b83-444c-be77-25d61a0afae7)

- The complete deepslate family, all eight deepslate ores, raw copper/iron/gold and their blocks,
  smelting, crafting, and stonecutting
- Later-vanilla tuff shape variants and the retained custom calcite decorative set
- Every copper oxidation and waxed stage across blocks, cut blocks, stairs, and slabs, with
  radius-four aging, axe scraping/unwaxing, lightning cleaning, and lightning rods
- Budding amethyst and all four growth stages, canonical shard drops and sounds, tinted glass,
  and a functional spyglass
- Functional stonecutter and composter systems

### Living caves and dripstone mechanics

![Mechanics](https://github.com/user-attachments/assets/9d462641-c2f0-4bcc-8035-f28cf1483d46)

- Glow berries as the edible cave-vine planting item, with no invented seed item
- Azaleas and flowering azaleas, both leaf types, rooted dirt, hanging roots, moss spreading,
  small and big dripleaf, spore blossoms, potting, composting, and azalea-tree growth
- One canonical pointed-dripstone family with growth, thickness recalculation, falling
  stalactites, stalagmite damage, trident breaking, water retention, and faithful water/lava
  cauldron behavior

### Axolotls, bees, and honey

![Axobee](https://github.com/user-attachments/assets/9f1d34f3-7fe8-4fa7-9ff6-1b8fc7611511)

- Five axolotl variants, breeding and blue mutation odds, bucket/NBT round trips, aging,
  dehydration, play-dead behavior, combat support, and lush-cave spawning
- Bee pollination, breeding, anger, stinging, hive routing, crop growth, sounds, animation, and
  persistent NBT
- Generated and sapling-grown bee nests, three-occupant nests and hives, honey production,
  smoke-safe harvesting, Silk Touch occupant preservation, comparators, and dispensers
- Honeycomb waxing through interaction, crafting, and dispensers, plus honey-block movement and
  piston adhesion including honey/slime incompatibility
- Normal and soul campfires with four-slot cooking, smoke and signal-smoke columns, projectile
  lighting, water dousing, hive calming, container handling, drops, sounds, and particles
- All sixteen dyed candles plus the undyed candle, one-to-four candle stacking, waterlogging,
  lighting and extinguishing, and the complete hidden candle-cake state family

### World and save compatibility

- `world.enableForNewOverworlds=true` applies only when an Overworld is first created
- Terrain schema, base world type, generator options, terrain profile, and modded-biome layout
  are persisted so later config changes cannot convert an existing world
- Released placeholder IDs and state-split blocks are remapped or migrated to canonical content
  while preserving inventories and block/entity NBT

## Creating a v2 world

Install the requirements below and create a world normally. Caves Not Cliffs applies its v2 format
to newly created Overworlds by default while preserving the selected base world type and its
options. Default, Large Biomes, and Amplified receive native Java 1.18.2 terrain profiles; other
compatible 2D types retain their selected generator through the finite delegated bridge. No
`level-type` change is required on a dedicated server.

The setting is written to `config/cavesnotcliffs.cfg` as
`world.enableForNewOverworlds=true`. Set it to `false` before creating a world to leave that new
Overworld unchanged. The setting is evaluated only at first creation: existing vanilla worlds,
schema-1 Caves Not Cliffs worlds, and schema-2 worlds always keep their recorded format. A stale
`level-type=cavesnotcliffs` on a newly created server is treated like the normal default selection
and still obeys this config.

Use `/cncbiome` in-game to identify the cave-biome region at your current position, or
`/cncbiome <x> <y> <z>` to inspect another coordinate.

## Biomes from other mods

Surface biomes that other mods add to the Overworld (through Forge's `BiomeManager` or the base
world type's biome layers) generate in native worlds without breaking the 1.18 layout. Each
modded biome is matched to the 1.18 biomes it fits by temperature, rainfall, height and biome
type: a modded swamp replaces parts of 1.18 swamps, a modded alpine biome replaces peaks and
slopes, a modded forest replaces forests, and land and water biomes never swap places. Large
regions with natural borders decide, for every 1.18 biome inside them, whether it stays vanilla
or which fitting modded biome takes over, weighted by each mod's own biome weights.

Terrain, caves, aquifers and ores stay 1.18 everywhere. A modded biome brings its name, colors,
mob spawns and vegetation: its grass/dirt or sand becomes the biome's own `topBlock` and
`fillerBlock`, its decorator replaces the 1.18 vegetation in the chunks it covers, and its 1.12
ores, dirt/gravel blobs, springs, lakes and sand/clay disks are skipped because the 1.18 ones are
already there. Structures follow the biome that is actually there, as in 1.12.

The server log lists which 1.18 biomes every modded biome replaces. The `moddedBiomes` section of
`config/cavesnotcliffs.cfg` tunes the layout for chunks generated afterwards: `regionSize`
(1024 blocks, ×4 in Large Biomes), `vanillaWeight` (10; higher makes modded biomes rarer),
`blacklist` (`modid:biome` or `modid:*`) and `hosts` (`modid:biome=plains,forest` pins a biome to
chosen 1.18 biomes).

Worlds created with 2.0.3 or 2.0.4 keep the earlier layout, which paints modded biomes wherever
the 1.12 biome layers put them, so their explored land and new chunks keep matching. Set
`moddedBiomes.upgradeExistingWorlds=true` to switch such a world to the 1.18 fit; new chunks then
use it, and borders with already explored modded biomes show seams.

## Performance

Java 1.18.2-style terrain is inherently heavier than vanilla 1.12.2 generation. 2.0.0 fills each
terrain column's density cells and virtual biome quarts on a small worker pool, controlled by
`-Dcavesnotcliffs.terrainThreads=N` (default: half the available processors, capped at 8;
`1` restores the fully serial path), and pre-starts the likely next column while the server
thread populates the current one. Column output is bit-identical either way. First-time spawn
preparation still takes a minute or two on older CPUs, and lowering the view distance helps
during both spawn preparation and exploration.

## Known limitations

- Native Java 1.18.2 terrain is supplied for Default, Large Biomes, and Amplified. Flat,
  Customized, Default 1.1, debug, and compatible third-party 2D types retain their selected
  generator and options through the finite delegated bridge. Existing third-party cubic types
  remain authoritative and are not wrapped.
- Schema-2 worlds retain only Minecraft 1.12's mineshaft, village, stronghold, temple,
  ocean-monument, and woodland-mansion structure families. Post-1.12 structures are not
  backported.
- Goats, foxes, and glow squids are not backported. Tropical fish are represented only by the
  narrow clownfish-based bucket bridge used by axolotl breeding.
- Same-seed fidelity covers native terrain density, climate, caves, aquifers, surfaces, and the
  ported feature pipelines. Whole-chunk parity is not claimed around retained 1.12 structures or
  omitted modern ecosystems.
- Existing schema-1 draft-v2 saves intentionally retain their original vanilla surface, deep worm
  caves, and upper headroom to prevent chunk seams.
- Nether and End generation remain unchanged.
- CaveBiomesAPI owns the process-wide finite chunk height. Client and server must use the same
  API build; the server synchronizes its authoritative range when a player joins.

## Requirements

Caves Not Cliffs 2.0.x targets Minecraft 1.12.2 and requires:

- [Minecraft Forge 14.23.5.2860 or newer](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.12.2.html)
- [CaveBiomesAPI 1.1.2 or newer](https://github.com/Vendetta1871/CaveBiomesAPI) (1.1.3 or newer
  for extended-height rendering under Celeritas / Actinium, 1.1.4 or newer together with
  Fluidlogged API)
- [MixinBootstrap 1.1.0](https://github.com/LXGaming/MixinBootstrap/releases/tag/v1.1.0)

OptiFine HD_U_E3 is supported, including extended-height rendering; other OptiFine builds are
untested.

## Building

Use a Java 8 JDK and the checked-in wrapper; no system Gradle installation is needed:

```bash
./gradlew clean test build verifyReleaseJar
```

On Windows, run `gradlew.bat clean test build verifyReleaseJar`. The release artifact is
`build/libs/cavesnotcliffs-<version>.jar`. The build fails if that jar is not reobfuscated, if its
release metadata is wrong, if CaveBiomesAPI classes were accidentally bundled, or if static
CubicChunks linkage remains.

The Java 8/ForgeGradle 2.3 development toolchain compiles against Forge 14.23.5.2847, the newest
Forge release that still publishes the legacy `userdev` artifact. The produced mod declares and
requires Forge 14.23.5.2860 or newer at runtime.

`./gradlew runClient` and `./gradlew runServer` launch that 2847 toolchain, so a Gradle invocation
that runs either task compiles `@Mod` with a 2847 Forge floor. Keep dev launches and release
builds in separate invocations: the build refuses to mix them, and `verifyReleaseJar` rejects a jar
that does not require 2860. Dev launches run Mixin 0.8.4 — the runtime MixinBootstrap 1.1.0
ships — and take CaveBiomesAPI from the FG-deobfuscated dependency classpath, so its release jar
must not be placed in `run/mods`. Fluidlogged API is compiled against but left out of dev launches:
its transformers use MCP names from other mappings and break world generation there.
