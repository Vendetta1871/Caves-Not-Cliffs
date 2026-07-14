# Minecraft 1.18.2 noise-based aquifer oracle

`aquifer-oracle-1.18.2.tsv` was generated independently from the backport by compiling and
running `Cnc118AquiferOracle.java.txt` against a named remap of Mojang's original Java Edition
1.18.2 server. The harness invokes Mojang's `Aquifer.NoiseBasedAquifer` itself. It records raw
IEEE-754 bits for every floating-point contract.

The end-to-end harness supplies an official `NoiseChunk` allocated without its full router and
prefills only its private preliminary-surface cache. This preserves Mojang's own quart-coordinate
lookup, center search, two caches, pressure calculation, floodedness/spread/lava decisions, and
fluid-update flag while allowing a small deterministic surface function. The four density inputs
are deterministic coordinate probes rather than the complete Overworld router; noise sampler
parity and router wiring are covered separately by their own oracle fixtures.

Coverage:

- all 13 surface sampling offsets in their official iteration order;
- negative-boundary grid division for 16x12x16 aquifer cells;
- center positions from the actual `minecraft:aquifer` positional stream;
- fluid-status strict-level behavior and the `-32512` dry sentinel;
- pressure branches, barrier sampling, and water/lava boundaries;
- 624 official end-to-end decisions across seeds `0`, `1`, `-1`, `123456789`,
  `Long.MIN_VALUE`, and `Long.MAX_VALUE`;
- negative chunks, `-65/-64` and `319/320` build boundaries, sea/lava levels, stable/scheduled
  air and water, lava, and solid decisions;
- an additional Java 8 test matrix for request-order independence and cache reuse.

Reference SHA-256 hashes:

- Official bundled server jar:
  `57be9d1e35aa91cfdfa246adb63a0ea11a946081e0464d08bc3d36651718a343`
- Official inner server jar:
  `ed066d092f6748b00efffa82d79f4ea248ad31972c089ce02d71f611ca2bf635`
- Official server mappings:
  `2a674d9721824beb424337dab39a2ec3553babf0fa4c75fde66706c73c17539a`
- Derived named server jar used by the harness:
  `c9a1ac245bc9d6d7f89444e4e948b54a27a59331c84d811a8ed956b3016ae72c`
- Oracle harness:
  `78ce3a4f163000052322e5386c5da3ef4d5c49ce93a255f7a705b9fbcc194467`
- Generated fixture:
  `ddf8f56b12de69e48a3e3911d22714025ea6cbd5196c675df1145eeb0da3f8d7`

To regenerate, place the harness in its declared package and run it with Java 17 against the
derived named server and the libraries extracted from Mojang's bundled server:

```text
mkdir -p oracle-src/net/minecraft/world/level/levelgen oracle-classes
cp Cnc118AquiferOracle.java.txt \
  oracle-src/net/minecraft/world/level/levelgen/Cnc118AquiferOracle.java
javac -cp <named-inner-server.jar>:<extracted-libraries/*> -d oracle-classes \
  oracle-src/net/minecraft/world/level/levelgen/Cnc118AquiferOracle.java
java -cp oracle-classes:<named-inner-server.jar>:<extracted-libraries/*> \
  net.minecraft.world.level.levelgen.Cnc118AquiferOracle aquifer-oracle-1.18.2.tsv
```

The `Unsafe` use is isolated to this offline oracle harness so it can populate the official final
surface-cache field without constructing the complete router. Production sources and JUnit tests
remain Java 8 and have no dependency on Mojang's jars.
