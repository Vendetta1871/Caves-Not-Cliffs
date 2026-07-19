# Changelog

## 2.0.0

- Fill each terrain column's density cells and virtual biome quarts on a configurable worker
  pool (`cavesnotcliffs.terrainThreads`, default half the available processors, capped at 4),
  cutting spawn-area preparation by roughly a third on quad-core hosts; column output stays
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
- Require Forge 14.23.5.2860+, CaveBiomesAPI 1.1.0+, and MixinBootstrap 1.1.0 at runtime.
