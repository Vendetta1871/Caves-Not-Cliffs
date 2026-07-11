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
