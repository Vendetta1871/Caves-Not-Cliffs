# Minecraft 1.18.2 NoiseChunk oracle

`noise-chunk-oracle-1.18.2.tsv` was generated independently from the backport by compiling and
running `Cnc118NoiseChunkOracle.java.txt` directly against Mojang's obfuscated Java Edition
1.18.2 server. No remapped backport class participates in fixture generation.

For every sample, the harness:

1. loads the built-in default, large-biomes, or amplified generator settings;
2. clones the setting with aquifers and ore placement disabled (the density router is unchanged);
3. creates the official seeded Xoroshiro router;
4. creates a one-cell `NoiseChunk` around the requested block;
5. recursively maps final density and all three vein functions through `NoiseChunk.wrap`;
6. drives `initializeForFirstCellX`, `advanceCellX`, `selectCellYZ`, and the official Y/X/Z update
   sequence; and
7. records raw IEEE-754 bits for direct final density, cache-all-in-cell final density, vein
   toggle, vein ridgedness, and vein gap.

The separate direct and cache-all final values matter. Ordinary `NoiseInterpolator` updates use
Y-then-X-then-Z lerps, while interpolation during `CacheAllInCell` filling uses `Mth.lerp3`'s
X-then-Y-then-Z order. Those orders differ at the raw-bit level for valid router samples.

Coverage is 576 samples and 2,880 raw-bit assertions: three native profiles, seeds `0`, `1`,
`-1`, `123456789`, `Long.MIN_VALUE`, and `Long.MAX_VALUE`, asymmetric negative coordinates,
density-cell boundaries, 16-block cube boundaries, the `-64..319` build boundaries and adjacent
outside points, and coordinates beyond Perlin's 33,554,432 wrapping interval.

Reference SHA-256 hashes:

- Official bundled server jar:
  `57be9d1e35aa91cfdfa246adb63a0ea11a946081e0464d08bc3d36651718a343`
- Official inner server jar:
  `ed066d092f6748b00efffa82d79f4ea248ad31972c089ce02d71f611ca2bf635`
- Unsigned oracle copy of the obfuscated inner jar:
  `97dfcf4845fbaf5c25b7f4d4b949bf002e537c702a21db8a656e8e5b5aeaab00`
- Official server mappings:
  `2a674d9721824beb424337dab39a2ec3553babf0fa4c75fde66706c73c17539a`
- Oracle harness:
  `fd209893e606f6671c0684f00fcf70a154457bde43736410ec1377604a0b0679`
- Generated fixture:
  `7d7f86c6057afd35980b05d684d440d79ad950871cb1ac24c3652458d8ab9c5d`

To regenerate, compile the `.java.txt` harness as `Cnc118NoiseChunkOracle.java` with Java 17 and
run it with Mojang's unsigned obfuscated server plus the bundled server libraries on the
classpath. Production sources and tests remain Java 8 and do not depend on oracle artifacts.
