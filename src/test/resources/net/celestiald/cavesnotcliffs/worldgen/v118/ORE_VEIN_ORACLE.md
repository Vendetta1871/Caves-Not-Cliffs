# Minecraft 1.18.2 ore-vein oracle

`ore-vein-oracle-1.18.2.tsv` was produced by compiling and running
`Cnc118OreVeinOracle.java.txt` directly against Mojang's obfuscated Java Edition 1.18.2 server.
No backport class participates in fixture generation.

The harness invokes the official `OreVeinifier` with constant density functions and the seeded
`minecraft:ore` positional factory from the official Overworld router. This deliberately isolates
the material selector from the three router functions, which have their own raw-bit NoiseChunk
oracle. The selector itself is profile-independent; default, large-biomes, and amplified use the
same seeded ore positional stream.

The full harness emits 3,840 cases. The committed fixture is a deterministic 1,034-case subset
that retains every non-null official result plus null-result representatives for the float
threshold, ridge rejection, gap rejection, edge roundoff, and vertical-band branches. Coverage
selection keeps each non-null row and, within each 640-row seed group, rows whose index is
divisible by 17, whose ridge value is zero (index divisible by 31), or whose gap value is the
`-0.3F` boundary (index divisible by 29). Coverage
includes all six required edge seeds, 19 vertical samples around the iron and copper boundaries,
eight positive and negative toggle thresholds, and every official output family. In particular,
the fixture contains the rare raw-copper and raw-iron block decisions rather than only common ore
and filler results.

Reference SHA-256 hashes:

- Official bundled server jar:
  `57be9d1e35aa91cfdfa246adb63a0ea11a946081e0464d08bc3d36651718a343`
- Official inner server jar:
  `ed066d092f6748b00efffa82d79f4ea248ad31972c089ce02d71f611ca2bf635`
- Unsigned oracle copy of the obfuscated inner jar:
  `97dfcf4845fbaf5c25b7f4d4b949bf002e537c702a21db8a656e8e5b5aeaab00`
- Oracle harness:
  `f9400d11d016cf52cb2c04ffb36b4437aacc1f88c8e076fee371afdc4c40e445`
- Committed fixture:
  `e51a76ad14223c5d58be2ae3a7efc951263ebec5c88fa5a59f68fc3d48fc6526`

Regenerate with Java 17 by compiling the `.java.txt` file as
`Cnc118OreVeinOracle.java` against the unsigned official server and its bundled libraries. The
production implementation and JUnit test remain Java 8 and have no runtime dependency on oracle
artifacts.
