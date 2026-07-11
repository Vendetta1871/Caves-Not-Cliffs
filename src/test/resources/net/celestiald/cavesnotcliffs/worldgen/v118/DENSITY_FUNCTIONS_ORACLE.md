# Minecraft 1.18.2 density-function oracle

`density-functions-oracle-1.18.2.tsv` was generated independently from this backport by compiling
and running `Cnc118DensityFunctionsOracle.java.txt` against a named remap of Mojang's original
Java Edition 1.18.2 server. The harness lives in Mojang's density-function package so it can call
the official package/protected expression nodes directly. It records raw IEEE-754 double bits.

Coverage includes all six mapped operations (including infinities, signed zero, subnormal values,
and NaN), clamped gradients, binary bounds and evaluation short-circuits, constant-specialized
addition/multiplication, range boundaries, both spaghetti rarity quantizers, normal/amplified
Overworld slides, generic and terrain-shaper splines, and seeded noise/shift/shifted/weird samplers.
Seed coverage is `0`, `1`, `-1`, `123456789`, `Long.MIN_VALUE`, and `Long.MAX_VALUE`, including
negative coordinates and coordinates beyond Perlin's 33,554,432 wrapping interval.

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
  `76bc2e7f12e2a8790e099d45896317e7ecffbec595bec1c1aaa3585804311bd1`
- Generated fixture:
  `b0160e46c6f2734c65f4cd2003400f93f5f54e372c8490e5f98d40a78a3907de`

The production source and test suite remain Java 8 and do not depend on the official jars.
