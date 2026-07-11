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
