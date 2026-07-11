# Caves Not Cliffs [Backported]

Caves Not Cliffs 2.0.0 backports Java 1.18.2 Overworld terrain and its represented Caves & Cliffs
content to Minecraft 1.12.2. New Overworlds use a default-on, finite CubicChunks schema from Y=-64
through Y=319, while existing worlds keep their saved generator contract.

![Caves not Cliffs](https://github.com/user-attachments/assets/b3210380-8264-4887-99dc-03522af9a10f)

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

![Lush cave](https://github.com/user-attachments/assets/1c13ec5a-a13a-42a5-b899-7979f2e197d5)

![Dripstone cave](https://github.com/user-attachments/assets/8cd7017a-ca13-473c-bebf-62a6c4bf0a85)

- Lush and dripstone cave biomes with Java 1.18.2 feature ordering
- Meadow, Grove, Snowy Slopes, Jagged Peaks, Frozen Peaks, and Stony Peaks
- Exact ordinary ore bands and exposure reduction, large copper and iron veins, soft disks,
  underwater magma, tuff placement, and amethyst geodes
- Functional terrain-generated powder snow, buckets, dispensers, layered cauldrons, sinking,
  freezing, and leather protection

### Faithful block and crafting families

![Amethyst geode](https://github.com/user-attachments/assets/8cd2237e-d1c0-4ca0-be9d-f6f156b3baff)

- The complete deepslate family, all eight deepslate ores, raw copper/iron/gold and their blocks,
  smelting, crafting, and stonecutting
- Later-vanilla tuff shape variants and the retained custom calcite decorative set
- Every copper oxidation and waxed stage across blocks, cut blocks, stairs, and slabs, with
  radius-four aging, axe scraping/unwaxing, lightning cleaning, and lightning rods
- Budding amethyst and all four growth stages, canonical shard drops and sounds, tinted glass,
  and a functional spyglass
- Functional stonecutter and composter systems

### Living caves and dripstone mechanics

![Filling cauldron by different liquids](https://github.com/user-attachments/assets/267a7270-f19b-4f32-b720-94ae56d7c91d)

- Glow berries as the edible cave-vine planting item, with no invented seed item
- Azaleas and flowering azaleas, both leaf types, rooted dirt, hanging roots, moss spreading,
  small and big dripleaf, spore blossoms, potting, composting, and azalea-tree growth
- One canonical pointed-dripstone family with growth, thickness recalculation, falling
  stalactites, stalagmite damage, trident breaking, water retention, and faithful water/lava
  cauldron behavior

### Axolotls, bees, and honey

- Five axolotl variants, breeding and blue mutation odds, bucket/NBT round trips, aging,
  dehydration, play-dead behavior, combat support, and lush-cave spawning
- Bee pollination, breeding, anger, stinging, hive routing, crop growth, sounds, animation, and
  persistent NBT
- Generated and sapling-grown bee nests, three-occupant nests and hives, honey production,
  smoke-safe harvesting, Silk Touch occupant preservation, comparators, and dispensers
- Honeycomb waxing through interaction, crafting, and dispensers, plus honey-block movement and
  piston adhesion including honey/slime incompatibility

### World and save compatibility

- `world.enableForNewOverworlds=true` applies only when an Overworld is first created
- Terrain schema, base world type, generator options, and terrain profile are persisted so later
  config changes cannot convert an existing world
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

## Requirements

Caves Not Cliffs 2.0.0 targets Minecraft 1.12.2 and requires:

- [Minecraft Forge 14.23.5.2860 or newer](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.12.2.html)
- [CubicChunks `1.12.2-0.0.1301.0-SNAPSHOT` or newer](https://maven.daporkchop.net/snapshot/io/github/opencubicchunks/cubicchunks/)

## Building 2.0.0

Use a Java 8 JDK and the checked-in wrapper; no system Gradle installation is needed:

```bash
./gradlew clean build
```

On Windows, run `gradlew.bat clean build`. The release artifact is
`build/libs/cavesnotcliffs-2.0.0.jar`. The build fails if that jar is not reobfuscated, if its
release metadata is wrong, or if CubicChunks API classes were accidentally bundled.

The Java 8/ForgeGradle 2.3 development toolchain compiles against Forge 14.23.5.2847, the newest
Forge release that still publishes the legacy `userdev` artifact. The produced mod declares and
requires Forge 14.23.5.2860 or newer at runtime.
