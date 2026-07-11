# Minecraft 1.18.2 terrain-shaper oracle

`terrain-shaper-oracle-1.18.2.tsv` was generated independently from the Java 8 backport by
compiling and running `Cnc118TerrainShaperOracle.java.txt` against Mojang's original obfuscated
Java Edition 1.18.2 server classes. The harness directly constructs both official Overworld
terrain shapers (`cce.a(false)` and `cce.a(true)`) and samples official `cce$c` points. All inputs
and outputs are stored as raw IEEE-754 float bits.

Reference hashes:

- Official bundled server jar SHA-256:
  `57be9d1e35aa91cfdfa246adb63a0ea11a946081e0464d08bc3d36651718a343`
- Official inner `server-1.18.2.jar` SHA-256:
  `ed066d092f6748b00efffa82d79f4ea248ad31972c089ce02d71f611ca2bf635`
- Official server mappings SHA-256:
  `2a674d9721824beb424337dab39a2ec3553babf0fa4c75fde66706c73c17539a`
- Oracle harness SHA-256:
  `5c5c6df927e79c0755a4f8669f5e83764ec204ed4570a466d977f55c92e2e919`
- Generated TSV SHA-256:
  `5c945425bfbcf0cec86e694411afb0f3150fa2040884d7a546cbf8abe253d9d4`

The fixture has 992 complete points. It samples the previous representable float, exact value,
and next representable float at every offset/factor/jaggedness knot exposed on the four terrain
axes. Multiple cross-axis contexts exercise each nested spline family. It also includes finite
extrapolation through `-16..16`, signed zero, deterministic scatter through `-3..3`, the official
`makePoint` ridge derivation, and direct peaks-and-valleys samples. `TerrainShaperOracleTest`
performs 6,107 raw-bit assertions over normal and amplified offset, factor, and jaggedness.

To regenerate, extract the nested libraries and inner server jar from the official bundled jar,
remove only the inner jar's signature metadata in a temporary copy so the unsigned harness can
share its default package, compile with Java 17, and run:

```text
javac -cp <unsigned-inner-server.jar>:<extracted-libraries/*> \
  Cnc118TerrainShaperOracle.java
java -cp .:<unsigned-inner-server.jar>:<extracted-libraries/*> \
  Cnc118TerrainShaperOracle terrain-shaper-oracle-1.18.2.tsv
```

The production source and tests remain Java 8 and have no runtime or build dependency on the
oracle artifacts.
