# Minecraft 1.18.2 primitive oracle

`oracle-1.18.2.tsv` was generated independently from this backport by compiling and running
`Cnc118Oracle.java.txt` against Mojang's original obfuscated Java Edition 1.18.2 server classes.
The harness calls the official classes directly (`cuo`, `cuw`, `cux`, `dia`, `did`, `dic`, `air`,
and `cbz`) after the normal 1.18.2 bootstrap. It records raw IEEE-754 bits so the checks do not
depend on decimal formatting or a tolerance chosen by the port.

Reference hashes:

- Official bundled server jar SHA-256:
  `57be9d1e35aa91cfdfa246adb63a0ea11a946081e0464d08bc3d36651718a343`
- Official inner `server-1.18.2.jar` SHA-256:
  `ed066d092f6748b00efffa82d79f4ea248ad31972c089ce02d71f611ca2bf635`
- Official server mappings SHA-256:
  `2a674d9721824beb424337dab39a2ec3553babf0fa4c75fde66706c73c17539a`
- Oracle harness SHA-256:
  `b99add0f0fac6bd74608c08e27387184e7c7a238be3b4b81f24414b24d3a4a8c`
- Generated TSV SHA-256:
  `fb047300e2d756e90929a6893a4cf37260104d8c14ebdb04957d4d35d53affad`

The seed matrix is `0`, `1`, `-1`, `123456789`, `Long.MIN_VALUE`, and `Long.MAX_VALUE`.
Coordinates cover negative block positions, negative fractional coordinates, and values beyond
the 33,554,432 Perlin wrapping interval. The fixture covers seed upgrade, raw xoroshiro128++,
bounded/scalar/Gaussian random output, positional and MD5-keyed factories, modern and legacy
Perlin stacks, modern and legacy normal noise, improved-noise derivatives, cubic splines, and
climate fitness/index lookup.

To regenerate, extract the nested libraries and inner server jar from the official bundled jar,
remove only the inner jar's signature metadata in a temporary copy so the unsigned harness can
share its default package, compile with Java 17, and run:

```text
javac -cp <unsigned-inner-server.jar>:<extracted-libraries/*> Cnc118Oracle.java
java -cp .:<unsigned-inner-server.jar>:<extracted-libraries/*> \
  Cnc118Oracle oracle-1.18.2.tsv
```

The production sources and tests remain Java 8 and have no dependency on the oracle jars.

## Native Overworld carvers

`carver-oracle-1.18.2.tsv` executes Mojang's mapped `CaveWorldCarver`,
`CanyonWorldCarver`, `WorldgenRandom`, `CarvingMask`, and noise aquifer directly. Its catalog rows
record the raw configured values and indices for `cave`, `cave_extra_underground`, and `canyon`.
The six required edge seeds cover negative source and target chunks, the complete `-8..8` source
halo, mask collisions, every 16-block vertical boundary, the `-64..319` build range, the carver
lava anchor, and all three native terrain profiles.

Each seed first carves a deterministic stone/deepslate/tuff/calcite/water scaffold through a
coordinate-hashed aquifer. Those rows independently verify exact geometry, replacement rules,
barriers, water/lava scheduling, and mask bits without relying on this backport's terrain. The
terrain rows then run the same official carvers after official raw density and surface generation
and record complete-column material hashes and histograms. The harness is
`Cnc118CarverOracle.java.txt`.

- Carver harness SHA-256:
  `73da9c441cda513b652e9e4943c9c36f7c854ec60fc02a2ddde5afd26eeb6319`
- Carver TSV SHA-256:
  `7effd3bee64b751598b8c6682074d57130c17d23388bab58fa09971e7da8a1dd`

Regenerate it with Java 17 against the mapped official server jar and its extracted libraries:

```text
javac -cp <mapped-server.jar>:<extracted-libraries/*> Cnc118CarverOracle.java
java -cp .:<mapped-server.jar>:<extracted-libraries/*> \
  net.minecraft.world.level.levelgen.Cnc118CarverOracle carver-oracle-1.18.2.tsv
```

## Seeded Overworld surface primitives

`surface-primitives-oracle-1.18.2.tsv` executes Mojang's mapped official `SurfaceSystem`
directly. It records exact surface depth, raw secondary-noise bits, and badlands clay-band
materials for all six edge seeds. The coordinate matrix covers negative chunk/cube boundaries,
ordinary positive boundaries, and both sides of the Perlin wrapping interval. The independent
harness is `Cnc118SurfacePrimitiveOracle.java.txt`.

- Surface-primitive harness SHA-256:
  `3c66d26b08e6213518a519bcf56dc15dfca6c4806747d735c6149a5bf44b18c9`
- Surface-primitive TSV SHA-256:
  `ee62990ccf80d77a619c57b94577d4fe31064b792789263d1837c99b88406a8b`

Compile the harness with Java 17 against the mapped official server jar and its extracted
libraries. It bootstraps the official registries, then reflectively invokes only the protected
surface methods; production code is not present on its classpath.

## Complete Overworld surface columns and material matrix

`surface-columns-oracle-1.18.2.tsv` fills official raw terrain columns and invokes Mojang's
`SurfaceSystem` for default, large-biomes, and amplified settings. Its 18 cases cover every edge
seed, negative chunks, chunk/cube/build-height boundaries, sampled raw and final blocks, complete
material counts, bedrock, and the 0-through-8 deepslate gradient. Sample columns are stored as
lossless vertical runs to keep the fixture compact. The independent harness is
`Cnc118SurfaceColumnOracle.java.txt`.

`surface-materials-oracle-1.18.2.tsv` uses a deterministic terrain scaffold to execute every
1.18.2 Overworld biome at eight seeded coordinate sets. The 400 complete-chunk hashes and material
histograms exercise all 27 raw/rule outputs, including calcite's narrow band, badlands clay bands
and pillars, frozen-ocean icebergs, powder snow, sandstone ceiling selection, and temperature
branches. Its harness is `Cnc118SurfaceMaterialOracle.java.txt`.

`biome-temperature-oracle-1.18.2.tsv` records raw base/adjusted float bits and both surface
temperature predicates for every built-in biome at five ordinary, negative, high-altitude, and
large-coordinate points. Its harness is `Cnc118BiomeTemperatureOracle.java.txt`.

- Full-column harness SHA-256:
  `d0c5ecdde30df89c7601b0aa87be9912e2436b9ac67cbdf9d9836d29e38695e4`
- Full-column TSV SHA-256:
  `3cfbbbb1d36e22eac3809f655f85f4821910ec32979c37b4a0b2458c052d9039`
- Material-matrix harness SHA-256:
  `b47b56c0d42ae6088e4e603e9557f2108ccd858b23011935bca3ccc29452668a`
- Material-matrix TSV SHA-256:
  `1c00048a9ca1f095178834335460c33e2a28d6679675b1e545f9768334500bec`
- Biome-temperature harness SHA-256:
  `890d0b582e8efca4d3f56f0f20447cfa8639794000e5336c7c6cf69d859ec54b`
- Biome-temperature TSV SHA-256:
  `ec62a4216729d8a186c040f63a6330dacc198fece10c39481f57a8a04057054c`

## Built-in noise registry and blended base density

`noise-parameters-oracle-1.18.2.tsv` enumerates all 60 entries in Mojang's built-in noise
registry, including every first octave and amplitude as raw IEEE-754 bits. It also instantiates
every entry through the official resource-key hashing path for all six required seeds and samples
both ordinary and Perlin-wrap-boundary coordinates. The independent harness is
`Cnc118NoiseRegistryOracle.java.txt`.

`blended-noise-oracle-1.18.2.tsv` samples the official legacy three-Perlin `BlendedNoise` used by
the normal 1.18.2 Overworld. Its 150 samples cover negative floor division, 4x8x4 density cells,
16-block cube boundaries, the full `-64..319` build range, large coordinates, and the same seed
matrix. The independent harness is `Cnc118BlendedNoiseOracle.java.txt`.

Reference hashes:

- Noise-registry harness SHA-256:
  `3bf8a26890a47b5b96c5b68cbb7ea35d80425a857e0f764b271e1a849844730b`
- Noise-registry TSV SHA-256:
  `65b77530391ce929b8b34d1b6cb23cede251c261c7513cc1050154046f88ed86`
- Blended-noise harness SHA-256:
  `64c8f5d36240606e2ebf7aeb21809740193e3016583a10c74318415b86e0738b`
- Blended-noise TSV SHA-256:
  `5c6d589e54eb83df87b52b64fde4692dd4ef611c0ea69309a12639f207148f28`

Regenerate either fixture with Java 17 using the same unsigned official server and extracted
libraries described above:

```text
javac -cp <unsigned-inner-server.jar>:<extracted-libraries/*> \
  Cnc118NoiseRegistryOracle.java Cnc118BlendedNoiseOracle.java
java -cp .:<unsigned-inner-server.jar>:<extracted-libraries/*> \
  Cnc118NoiseRegistryOracle noise-parameters-oracle-1.18.2.tsv
java -cp .:<unsigned-inner-server.jar>:<extracted-libraries/*> \
  Cnc118BlendedNoiseOracle blended-noise-oracle-1.18.2.tsv
```

## Overworld six-parameter biome table

`overworld-biomes-oracle-1.18.2.tsv` records all 7,578 entries emitted by the official
`OverworldBiomeBuilder`, including exact insertion order, every quantized parameter bound, offset,
and canonical biome resource key. It then records 13,280 official nearest-neighbor resolutions:
one midpoint lookup per entry, every unique axis boundary and its adjacent quantized values in two
cross-axis contexts, an explicit underground depth/humidity/continentalness/weirdness matrix, and
2,048 deterministic points spanning beyond the normal climate range. The independent obfuscated
server harness is `Cnc118OverworldBiomeOracle.java.txt`.

- Overworld-biome harness SHA-256:
  `ee1ec1753437228d0860c617b9a6c07d68a85ef556610d172d57d438233ca42c`
- Overworld-biome TSV SHA-256:
  `2d5993d08ad043cd645dd8178fba0540f9199d092e1719ad239c4149b841edfc`

Regenerate it with Java 17 and the official unsigned inner server jar plus its extracted libraries:

```text
javac -cp <unsigned-inner-server.jar>:<extracted-libraries/*> \
  Cnc118OverworldBiomeOracle.java
java -cp .:<unsigned-inner-server.jar>:<extracted-libraries/*> \
  Cnc118OverworldBiomeOracle overworld-biomes-oracle-1.18.2.tsv
```

## Overworld noise settings and density slides

`noise-settings-oracle-1.18.2.tsv` records both normal and amplified Overworld settings directly
from the official server. In addition to the exact `-64..319` range, 4x8x4 density cells, sampling
scales, and slider constants, it verifies 144 raw-bit slide results around negative division and
the top and bottom build boundaries. The independent harness is
`Cnc118NoiseSettingsOracle.java.txt`.

- Noise-settings harness SHA-256:
  `8330dfca538ed1035d67150c85df547ecfca5f7164589d8be693281ff9768aee`
- Noise-settings TSV SHA-256:
  `57957bc2f791da5245646c574aa05372a39faeb1860b36984e86b5a7dcfd9f58`

## Seeded Overworld noise router

`noise-router-oracle-1.18.2.tsv` executes the official seeded router for default, large-biomes,
and amplified worlds. It samples all 15 density outputs at 25 negative, density-cell, cube,
build-height, and Perlin-wrap boundaries for all six edge seeds. The resulting 6,750 values are
compared as raw IEEE-754 bits by `V118NoiseRouterOracleTest`; the independent official harness is
`Cnc118NoiseRouterOracle.java.txt`.

- Noise-router harness SHA-256:
  `1f5446795b0d9b4185a0f465a678e76fb6b20fd5271d44c0820dc2e5cc335e29`
- Noise-router TSV SHA-256:
  `79444c4a20c41bead9e1061f1265ef3fb795e73c92a3fc11557134b6a9c43d22`

## Ordinary Overworld ore and blob decoration

Five independent harnesses cover the ordinary 1.18.2 Overworld ore pass. The decoration-random
fixture exercises Mojang's xoroshiro-backed `WorldgenRandom` with the legacy decoration and
feature seed formulas. The catalog fixture discovers the registered configured/placed features,
their global step indices, exact modifier/configuration values, target outputs, and membership in
all 50 Overworld biomes. The placement fixture runs the registered count/rarity, in-square, and
height modifiers for every scoped feature, all six edge seeds, negative chunks, and normal,
large-biomes, and amplified generators. Ordinary placement is intentionally profile-independent,
which the test asserts.

`ore-shape-oracle-1.18.2.tsv` invokes the official `OreFeature` against deterministic sparse
chunks. Its 54 cases cover all configured sizes and discard probabilities, stone/deepslate
outputs, negative chunk edges, cube boundaries, and both build limits. It records every changed
coordinate and material. `ore-decoration-oracle-1.18.2.tsv` then executes the complete scoped
feature sequence for plains, dripstone caves, badlands, and meadow representatives. Each case
records per-material counts and a SHA-256 over every sorted `x,y,z,material` result.

The standalone registry bootstrap does not load data-pack block tags. The decoration harness
therefore expands the three official 1.18.2 tag JSON files (`base_stone_overworld`,
`stone_ore_replaceables`, and `deepslate_ore_replaceables`) into equivalent official
`BlockMatchTest` targets. Sizes, output states, exposure probabilities, and modifier chains still
come from Mojang's registered features. It removes the terminal biome filter after applying the
catalog-oracle membership table. Its `ensureCanWrite` emulates an ordinary, non-retrogen
`WorldGenRegion`: horizontal write-radius validation is active, while vertical clipping remains
inside `OreFeature`.

- Decoration-random harness SHA-256:
  `6e879863f1508cf6b84b864cfc378dc33a4f8e4e29f198f7b43c4f687d66ead7`
- Decoration-random TSV SHA-256:
  `abd23cba0280bf149430a3008446d859af133fe08223db373d9e9949e8f69fc4`
- Ore-catalog harness SHA-256:
  `1c9581f563d4ab4182a2dacccb17b7cb9d23b8c9b24d626a85153ada2b70ea84`
- Ore-catalog TSV SHA-256:
  `7a18a19f65c0de456fa732495538e553c93ed4528acbb8b37945f59f9318244e`
- Ore-placement harness SHA-256:
  `099b6a35606d9cff0bf805a55222716cebf673364ceb5e397afb69f1fa68eccb`
- Ore-placement TSV SHA-256:
  `feb023fdaa409dd4bf279fcec665dacf603f0bddbada5de5d167fd6bc7cae2ac`
- Ore-shape harness SHA-256:
  `86628d7b6aa9c3bcff3e5fc13cb08832d512ed70a850bb84f373a966010feeaf`
- Ore-shape TSV SHA-256:
  `f51910ca170efe54ee7b4950cf9ef61ac5835dd57bad28700ccfb7ceae580b05`
- Ore-decoration harness SHA-256:
  `d0f6c6ea79b865c11d1e556aee36611972725636ea4276294bbcc40d0f3ab4ea`
- Ore-decoration TSV SHA-256:
  `3ee4112bdfaf8e0c18ff253d13ba7abeda106bac78e29d9773e59f9493a29261`

## Lush-cave vegetal decoration

`lush-cave-placement-oracle-1.18.2.tsv` discovers and executes the complete placement-modifier
chains for the seven lush-cave entries in the official `VEGETAL_DECORATION` step. Its 126 cases
cover every native terrain profile, all six edge seeds, negative chunks, the exact global feature
indices 22 through 28, environment scans, height anchors, counts, and terminal biome filters.

`lush-cave-decoration-oracle-1.18.2.tsv` then runs those registered placed features against a
deterministic 5-by-5 sparse chunk region. It hashes every final moss, clay, water, plant, cave-vine,
dripleaf, root-system, azalea-tree, spore-blossom, and vine state after their configured-feature
random calls have been interleaved with placement sampling. The standalone bootstrap explicitly
binds the flattened contents of the official 1.18.2 block and fluid tag JSON used by these
features; no production backport classes are on the oracle classpath. Its 5-by-5 region is a read
fixture; ordinary non-retrogen `WorldGenRegion` semantics restrict writes to the center plus one
chunk in X/Z, while ProtoChunk rather than `ensureCanWrite` discards out-of-height writes.

- Lush-placement harness SHA-256:
  `fa5141c5f9d920bcbb15c080a6dc541628eac95ce00ee1ba8b2b5bb992eda358`
- Lush-placement TSV SHA-256:
  `d304e4b24169d0ff9eff519f946627344dc2c31e1c2bd5e1087a9aa07f633f78`
- Lush-decoration harness SHA-256:
  `913d4b5a0dcc94efa187902ea69da6d17151e358d2eb89e1969e39c64a5c8c0b`
- Lush-decoration TSV SHA-256:
  `e51587ee5daf51590a4e626df52c716b78496c3e0c561b44609a31f1e4ab74df`

## Desert dead-bush decoration

`dead-bush-decoration-oracle-1.18.2.tsv` invokes the registered
`patch_dead_bush_2` placed feature in the mapped official server. It pins the desert-only global
slot, codec configuration, support catalog, lazy Count(2) event order, all 52 bounded random
draws, writes, result, and trailing Xoroshiro state for the six edge seeds at chunk `(-3, 5)`.
The standalone harness is `Cnc118DeadBushDecorationOracle.java.txt`.

- Dead-bush harness SHA-256:
  `28c2610ca6f9484e973249bdd3a03a0d683cfce7cf461cee5bcacea33de48507`
- Dead-bush TSV SHA-256:
  `8b595f57e436b2fe10a3ce453baeaedf62736e193922604b7bf9f85bd323ae8e`
