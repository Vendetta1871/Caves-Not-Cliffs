# Caves Not Cliffs [Backported]

[![GitHub][github-badge]][github]
[![Discord][discord-badge]][discord]
[![Modrinth][modrinth-badge]][modrinth]
[![CurseForge][curseforge-badge]][curseforge]

[github-badge]: https://img.shields.io/badge/GitHub-Source-181717?style=for-the-badge&logo=github&logoColor=white
[github]: https://github.com/Vendetta1871/Caves-Not-Cliffs
[discord-badge]: https://raw.githubusercontent.com/Vendetta1871/Caves-Not-Cliffs/main/docs/badges/discord.svg
[discord]: https://discord.gg/gvmeswCQFP
[modrinth-badge]: https://img.shields.io/badge/Modrinth-Download-00AF5C?style=for-the-badge&logo=modrinth&logoColor=white
[modrinth]: https://modrinth.com/mod/caves-not-cliffs-backported
[curseforge-badge]: https://raw.githubusercontent.com/Vendetta1871/Caves-Not-Cliffs/main/docs/badges/curseforge.svg
[curseforge]: https://www.curseforge.com/minecraft/mc-mods/caves-not-cliffs-backport

Caves Not Cliffs 2.0 backports Java 1.18.2 Overworld terrain and its represented Caves & Cliffs
content to Minecraft 1.12.2.

***

## Key features

### Native Java 1.18.2 Overworld generation

- Default, Large Biomes, and Amplified profiles backed by the ported positional RNG, noise,
  spline, climate, density-cave, aquifer, carver, and surface-rule stacks
- Complete deterministic `16×384×16` columns with bedrock, the Y=0..8 deepslate transition,
  ore veins, fluid scheduling, and a bounded weighted cache
- A virtual 3D biome resolver with a legacy surface-biome projection for Minecraft 1.12
  spawning, colors, and chunk storage

![Biomes](https://github.com/user-attachments/assets/c15db242-fe78-4cb1-abbc-a748d9996448)

### Faithful block and crafting families

![Blocks](https://github.com/user-attachments/assets/d22c54a2-9b83-444c-be77-25d61a0afae7)

### Living caves and dripstone mechanics

![Mechanics](https://github.com/user-attachments/assets/9d462641-c2f0-4bcc-8035-f28cf1483d46)

### Axolotls, bees, and honey

![Axobee](https://github.com/user-attachments/assets/9f1d34f3-7fe8-4fa7-9ff6-1b8fc7611511)

### World and save compatibility

- `world.enableForNewOverworlds=true` applies only when an Overworld is first created
- Terrain schema, base world type, generator options, terrain profile, and modded-biome layout
  are persisted so later config changes cannot convert an existing world
- Released placeholder IDs and state-split blocks are remapped or migrated to canonical content
  while preserving inventories and block/entity NBT

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

## Known limitations

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
