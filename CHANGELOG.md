# Changelog

## Unreleased

- Fix native 1.18 worlds generating without passive animals: new chunks now get their
  original cows, sheep, pigs and chickens right after decoration, as in 1.12 and 1.18.
- Fix lightning rods never attracting lightning, and the original strike point still
  catching fire: the rod search skipped the non-opaque rod, and the bolt set its target
  on fire before it was redirected. Strikes now land on the closest exposed rod first.
- Fix lush/dripstone cave biome tints and fog disappearing on the client after changing
  dimension or respawning.
- Fix the mod's own mountain biomes being treated as modded overlay biomes on the client.
- Fix one failing modded biome decorator disabling decoration of every modded biome until
  restart (and crashing the next chunk of that biome with "Already decorating").
- Fix a multi-second server stall right after new terrain generates: gravel and sand
  blobs that caves cut open were all scheduled to fall at once (about 1,800 falling-block
  entities after spawn preparation). Blocks placed during native population now stay where
  they generate, as in 1.18, until a neighbour update makes them fall.
- Rework biomes from other mods in native worlds (issue #15): modded biomes no longer appear
  wherever the 1.12 biome layers put them, on top of unrelated 1.18 terrain ("biomes mixed in a
  pot", swamps without water, jungles next to taiga). Each modded biome now replaces only the
  1.18 biomes it fits by temperature, rainfall, height and biome type, across large regions
  with natural borders, so the world keeps the 1.18 layout. Inside, it uses its own
  top/filler soil, decorator and mob spawns, while its 1.12 ores, blobs, springs and disks are
  skipped in favour of the 1.18 ones. Structures follow the biome actually there. New
  `moddedBiomes` config: region size, vanilla weight, blacklist and host pins. Worlds created
  with 2.0.3/2.0.4 keep the old layout unless `moddedBiomes.upgradeExistingWorlds` is set.
- Mobs spawn from the modded biome's own spawn lists where a modded biome covers the surface.
- Depend on CaveBiomesAPI 1.1.4: world generation no longer crashes with
  "Chunk has 16 sections, expected 24" when Fluidlogged API is installed.

## 2.0.4

- Fix modded biome decorators running on whole chunks where the biome barely appears
  (issue #16): with Cherry_on installed, pink petals spawned everywhere grass generates.
  The modded biome decoration pass now samples the biome at the center of the population
  region — like vanilla 1.12.2 does — instead of majority-voting the whole 16x16 area.
- Fix a crash in custom dungeon chest placement when another mod's chest block state
  lacks the expected chest part/facing properties (from PR #17, thanks @NNYYOONNIIOO).
- Faster terrain generation: repeated density evaluations (CACHE_ONCE markers such as
  the spaghetti cave roughness, pillars and entrances) are now memoized per position,
  the preliminary surface heightmap is computed once per column instead of once per
  parallel stripe, and the mountain surface bridge caches column heightmaps during
  decoration instead of rescanning up to 384 blocks per query. Generation output is
  bit-identical to 2.0.3.
- Ship a mod icon shown by launchers and the in-game mod list.
- Depend on CaveBiomesAPI 1.1.3: extended-height worlds now render correctly under
  Celeritas and its shader-enabled fork Actinium (issue #14).

## 2.0.3

- Support modded surface biomes in native-profile worlds: biomes other mods add through
  Forge's BiomeManager (e.g. Thaumcraft's Magical Forest) now overlay the 1.18 climate
  projection — they generate in the world, drive biome tints, and get their vanilla
  `biome.decorate` pass, so biome-locked mod content like Silverwood and Greatwood trees
  generates again (issue #7). Biome search tools (Nature's Compass) can locate these
  overlay biomes: `findBiomePosition` now consults the overlay too, sampled at the same
  block scale the chunk biome array is written with, so reported positions are exact.
  The F3 debug line and biome tint reads (`World.getBiome` through the vertical biome
  resolver) now also respect the overlay: a modded biome claim from the chunk array wins
  over the 1.18 climate projection at the surface, while lush/dripstone cave biomes still
  override it underground. Lush caves and dripstone caves are now real registered biomes
  (`cavesnotcliffs:lush_caves` / `cavesnotcliffs:dripstone_caves`, spawn lists copied from
  their previous vanilla projection targets), so F3 and biome-reading mods report them
  instead of "Forest"/"Extreme Hills".
- Fix the F3 debug screen showing "Outside of world..." below Y=0 in extended-height
  worlds: the vanilla overlay hardcoded a 0..256 window (new client-side
  `GuiOverlayDebugMixin`).
- Fix a startup crash when another mod (e.g. Better With Mods) had already removed the
  vanilla pumpkin recipes that Caves Not Cliffs replaces (issue #15).
- When another mod changes the vanilla farmer trades, the plain-pumpkin trade rewrite is
  now skipped with a warning instead of crashing (issue #15).
- Fix deepslate and infested deepslate generating sideways: the pillar default state was
  left at axis=X and worldgen places the default state; both now default to axis=Y.
- Fix the production jar crashing at startup: the CauldronMixin refmap was missing the SRG
  mapping for the shadowed `LEVEL` field (`@Shadow field LEVEL was not located`).
- Pointed dripstone is no longer a waterlogged block pair: the hidden
  `pointed_dripstone_waterlogged` companion block is gone and waterlogging is handled
  through the optional Fluidlogged API mod when it is installed (without it, dripstone
  placed in water simply displaces the water, like most vanilla blocks). Saved
  `pointed_dripstone_waterlogged` states from 2.0.x worlds remap onto the canonical
  `pointed_dripstone` block — they keep their shape but lose the stored water unless
  Fluidlogged API is present.
- Fix the stonecutter not rendering transparent textures (glass-style cutout layer).
- Speed up virtual biome resolution during terrain generation.

## 2.0.2

- Store lava and powder snow cauldron contents on the vanilla `minecraft:cauldron` block itself
  via a new `CauldronMixin` (metadata 7 = lava, 8-10 = powder snow layers) instead of replacing
  placed cauldrons with hidden blocks, so third-party identity checks — e.g. Immersive
  Engineering's Arc Furnace multiblock — work again. Hidden `lava_cauldron` and
  `powder_snow_cauldron` blocks from 2.0.x worlds are migrated back to equivalent vanilla states
  on chunk load.
- Require the MixinBootstrap mod at runtime (already pulled in by CaveBiomesAPI).
- Fix stalactite cauldron fills burst-firing every stage at once under elevated
  `randomTickSpeed`: pending cauldron fills are now deduplicated per position, matching
  Java 1.18 scheduled-tick semantics, so water layers rise one drip at a time.
- Show the drip that is travelling from a stalactite tip into a cauldron below: a
  server-side drip particle detaches from the tip when the fill is scheduled and lands
  roughly when the layer rises.
- Fix the axolotl's head (and gills, tail, legs) visually detaching from the body
  during swimming/hovering/playing-dead animations: the model parts now form the same
  parent-child hierarchy as Java 1.18 (head, tail and legs parented to the body,
  gills parented to the head), so body bobbing and tilting carries every part with it.
- Fix villages (and other biome-gated vanilla structures) spawning in biomes the world
  does not actually have — e.g. a village floating on the ocean. Structure viability
  checks used the untouched vanilla 1.12 GenLayer biome layout while native-profile
  worlds lay terrain down with the Java 1.18 multi-noise climate map; the world type
  now installs a `V118BiomeProvider` backed by that same 1.18 map (sea-level sampling,
  Voronoi zoom included), so biome checks agree with the terrain being generated.

## 2.0.1

- Accept Cleanroom's recompiled `BlockMushroom.canBlockStay` shape (three integer
  returns, with the podzol branch folded into a ternary) alongside Mojang's
  four-return bytecode in the mushroom support transformer, fixing an instant
  crash on Cleanroom 0.6.x.

## 2.0.0

- Fill each terrain column's density cells and virtual biome quarts on a configurable worker
  pool (`cavesnotcliffs.terrainThreads`, default half the available processors, capped at 16)
  and pre-start the likely next column while the server thread populates the current one,
  cutting spawn-area preparation by roughly 40% on quad-core hosts; column output stays
  bit-identical to the serial path.
- Replace the selectable level type with default-on `world.enableForNewOverworlds=true`; evaluate
  it only when first creating an Overworld and preserve existing-world generator contracts.
- Persist terrain schema, selected base type, generator options, and terrain profile; protect
  schema-1 draft saves and handle stale `level-type=cavesnotcliffs` selections.
- Register deterministic hidden wrappers for vanilla and compatible third-party 2D world types
  while leaving existing cubic world types authoritative.
- Port the Java 1.18.2 positional RNG, noise registry, spline terrain shaper, six-parameter climate
  table, density router, cheese/spaghetti/noodle caves, aquifers, carvers, surface rules, bedrock,
  and deepslate transition.
- Generate deterministic Y=-64..319 terrain columns for Default, Large Biomes, and Amplified,
  then write their signed sections into finite CaveBiomesAPI chunks through a bounded weighted
  LRU.
- Add a virtual 3D biome resolver and `/cncbiome`, including Meadow, Grove, Snowy Slopes, Jagged
  Peaks, Frozen Peaks, Stony Peaks, Lush Caves, and Dripstone Caves.
- Retain the six available Minecraft 1.12 structure families through a structure-only bridge
  without invoking the old terrain or decorator pipeline.
- Port Java 1.18.2 ore bands, exposure reduction, large copper and iron veins, geodes, soft disks,
  underwater magma, lush features, dripstone features, and bee-bearing surface trees and
  vegetation.
- Add functional powder snow with terrain placement, sinking, freezing, leather protection,
  buckets, dispensers, and layered cauldrons.
- Canonicalize public registry IDs and add missing-mapping, inventory, and chunk/cube migrations
  for released and draft-v2 saves.
- Complete deepslate, tuff, retained calcite extras, all eight deepslate ores, raw materials and
  blocks, exact recipes and smelting, and functional stonecutter and composter systems.
- Complete copper ores, oxidation and waxed shape matrices, radius-four aging, axe interactions,
  lightning cleaning, lightning rods, crafting, stonecutting, and dispenser waxing.
- Complete amethyst growth, water retention, light and drop rules, chimes, tinted glass, and
  spyglass zoom and overlay behavior.
- Replace invented cave-plant items with glow berries and canonical lush-cave blocks; add moss
  spreading, azalea trees, dripleaf state machines, support rules, potting, composting, and
  particles.
- Consolidate pointed dripstone and complete growth, falling and impact behavior, trident breaking,
  water retention, and layered-water/full-lava cauldrons.
- Complete five-variant axolotls, bucket/NBT lifecycle, breeding, aging, dehydration, play-dead
  behavior, targeting, regeneration support, sounds, rendering, and lush-cave spawning.
- Add bees, generated and sapling-grown nests, three-occupant hives, residence and honey
  production, smoke-safe harvesting, Silk Touch NBT preservation, comparators, dispensers, honey
  products, honey physics, and piston adhesion.
- Add normal and soul campfires with four-slot cooking, smoke and signal smoke, projectile
  lighting, water dousing, hive calming, container handling, drops, sounds, particles, and exact
  recipe contracts.
- Add all seventeen candle colors and their hidden candle-cake states with one-to-four stacking,
  waterlogging, lighting, extinguishing, eating, projectiles, drops, sounds, particles, recipes,
  and canonical resources.
- Make every Forge 1.12 slab recipe declare subtype metadata and add the standard tuff and retained
  calcite slab, stair, and wall recipes.
- Add exhaustive official-oracle, registry, mechanics, migration, asset-graph, dedicated-server
  linkage, reobfuscation, and release-jar verification.
- Render placed beehives, bee nests, and campfires with their block models instead of
  BlockContainer's default invisible render type.
- Support OptiFine HD_U_E3: hook both branches of its split integrated-server world-loading
  flow and keep extended-height chunk visibility and the render grid working beyond Y 0..255
  (requires CaveBiomesAPI 1.1.1).
- Require Forge 14.23.5.2860+, CaveBiomesAPI 1.1.1+, and MixinBootstrap 1.1.0 at runtime.
