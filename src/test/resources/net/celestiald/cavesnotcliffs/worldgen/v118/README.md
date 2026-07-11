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
