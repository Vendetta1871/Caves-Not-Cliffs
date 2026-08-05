# Changelog

## Unreleased

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
