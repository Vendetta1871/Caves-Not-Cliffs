# Caves Not Cliffs [Backported]

Caves Not Cliffs backports the underground half of Minecraft's Caves & Cliffs update to 1.12.2.
Version 2.0.0 adds a default-on finite-height CubicChunks world format, true 3D cave-biome regions,
and the complete deepslate ore family.

![Caves not Cliffs](https://github.com/user-attachments/assets/b3210380-8264-4887-99dc-03522af9a10f)

***

## Key features

1. **Lush and dripstone cave biomes**

![Lush cave](https://github.com/user-attachments/assets/1c13ec5a-a13a-42a5-b899-7979f2e197d5)

![Dripstone cave](https://github.com/user-attachments/assets/8cd7017a-ca13-473c-bebf-62a6c4bf0a85)

2. **Amethyst geodes**

![Amethyst geode](https://github.com/user-attachments/assets/8cd2237e-d1c0-4ca0-be9d-f6f156b3baff)

3. **Cauldron and dripstone lava farming**

![Filling cauldron by different liquids](https://github.com/user-attachments/assets/267a7270-f19b-4f32-b720-94ae56d7c91d)


4. **The v2 world format**

   - Config-driven Overworld format with buildable space from Y=-64 through Y=319
   - Deepslate and tuff below Y=0, with all seven vanilla deepslate ores
   - Deterministic 3D normal, lush, and dripstone cave regions
   - Seed-stable deep caves and decorations
   - Vanilla surface terrain and structures preserved from Y=0 through Y=255
   - Overworld-only custom terrain; the Nether uses bounded CubicChunks compatibility and the End stays vanilla

5. **Backported mobs and mechanics**

   - Swimming, rendered axolotls
   - Big dripleaf tilt and reset behavior
   - Pointed-dripstone cauldrons that support stalactites of any valid length

## Creating a v2 world

Install the requirements below and create a world normally. Caves Not Cliffs applies its v2 format
to newly created Overworlds by default while preserving the selected surface-generator family and
its options. No `level-type` change is required on a dedicated server.

The setting is written to `config/cavesnotcliffs.cfg` as
`world.enableForNewOverworlds=true`. Set it to `false` before creating a world to leave that new
Overworld unchanged. The setting is evaluated only at first creation: existing vanilla worlds,
schema-1 Caves Not Cliffs worlds, and schema-2 worlds always keep their recorded format. A stale
`level-type=cavesnotcliffs` on a newly created server is treated like the normal default selection
and still obeys this config.

Use `/cncbiome` in-game to identify the cave-biome region at your current position, or
`/cncbiome <x> <y> <z>` to inspect another coordinate.

## Roadmap

- v0.0.1 base mod mechanics - lush and dripstone caves, amethyst geode, cauldron can store lava and filled by pointed dripstone
- v1.5.0 remove dependency from other project, improve code structure
- v2.0.0 **[current]** increase world height (change bounds to -64 +320) and make new cave biomes, not just modifications of vanilla caves
- v3.0.0 add new mountains, powdered snow etc.
- v4.0.0 implement all the missing crafts and features 

## Known limitations

- Amethyst buds are generated with geodes but do not yet grow through all vanilla 1.17 stages.
- Terrain above Y=255 is intentionally left as v3 mountain headroom.
- v2 uses seed-stable worm caves below Y=0; the noise-cave rewrite remains planned for v3.
- CubicChunks 1301 logs a non-fatal missing `MixinItemMap` warning from its optional fixes config.

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
