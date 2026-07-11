# Changelog

## 2.0.0

- Add the opt-in `Caves Not Cliffs` CubicChunks world type with finite Y=-64..319 bounds.
- Add deterministic 3D normal, lush, and dripstone cave-biome regions.
- Generate schema-2 lush caves with the exact 1.18.2 moss, clay, cave-vine, dripleaf,
  root-system, azalea-tree, spore-blossom, and classic-vine feature pipelines.
- Add deepslate, tuff, bottom bedrock, deep caves, and all vanilla deepslate ore variants below Y=0.
- Add `/cncbiome` for inspecting cave-biome regions.
- Add a visible animated axolotl model and aquatic movement.
- Fix big dripleaf tilt/reset behavior and missing transition-state inventory models.
- Fix lava cauldron filling through variable-length pointed-dripstone chains.
- Prevent geodes from overwriting tile entities and player-built blocks.
- Restore a reproducible Gradle wrapper, CI build, tests, reobfuscation, and release-jar validation.
- Replace the fragile vanilla-cauldron Mixin with a dedicated Forge lava-cauldron block.
- Require Forge 14.23.5.2860+ and CubicChunks 1.12.2-0.0.1301.0-SNAPSHOT+ at runtime.
