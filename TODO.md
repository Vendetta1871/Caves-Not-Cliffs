# Caves Not Cliffs — Roadmap

## v1.0 — Current Release
- Lush caves, dripstone caves, amethyst geodes, lava cauldron
- Built on MCreator + external `cavesandcliffs` dependency

---

## v1.5 — Refactor & Decouple
- Drop MCreator dependency entirely; manual Forge mod structure
- Replace the separate lava cauldron block with a mixin that extends vanilla cauldron behavior
- Internalize all content from the external `cavesandcliffs` library (moss, glow berries, dripleaf, axolotl, etc.) — no more third-party mod dependency
- Add dripstone growth mechanics natively
- General refactoring and cleanup

---

## v2.0 — World Height Extension
- Extend the world downward to Y=-64 and upward to Y=320 (matching 1.17–1.18 behavior)
- Below Y=0: replace stone with deepslate and tuff
- Add deepslate variants of all vanilla ores (deepslate coal ore, deepslate iron ore, etc.)

---

## v2.5 — Split into Library + Mod
- Extract world-height and underground-biome APIs into a standalone library mod
- Library provides: configurable world height bounds, underground biome registration hooks
- Main mod depends on the library and uses it to register lush caves, dripstone caves, deepslate layer, full Y=-64 to +320 height

---

## v3.0 — New Cave Generation
- Rewrite cave generation to match 1.17–1.18 style: large open caverns, cheese caves, spaghetti caves
- Replace the old worm-style carver with noise-based generation
