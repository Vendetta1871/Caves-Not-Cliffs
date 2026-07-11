# Asset sources

The seven `deepslate_*_ore.png` textures added for v2.0.0 are unmodified Minecraft 1.17.1
textures extracted from Mojang's official client artifact:

- Version: `1.17.1`
- Client SHA-1: `8d9b65467c7913fcf6f5b2e729d44a1e00fde150`
- Version metadata: <https://piston-meta.mojang.com/mc/game/version_manifest_v2.json>

The canonical deepslate building-family, deepslate copper ore, raw-material block, raw-material
item, copper ingot, amethyst shard, and deepslate top/side textures are unmodified Minecraft
1.18.2 assets extracted from Mojang's official client artifact:

- Version: `1.18.2`
- Client SHA-1: `2e9a3e3107cca00d6bc9c97bf7d149cae163ef21`
- Version metadata: <https://piston-meta.mojang.com/mc/game/version_manifest_v2.json>

The 27 deepslate and deepslate-brick Ogg Vorbis files used by the deepslate,
polished-deepslate, brick, and tile sound types are unmodified objects from the official
Minecraft 1.18 asset index referenced by Java 1.18.2. The tile variants reuse the brick sounds
with Mojang's canonical per-entry pitch and volume settings.

- Asset index: `1.18`
- Asset-index SHA-1: `d31a2e85ae149dd1b1a7070b22cb8887892fda6c`
- Sound-definition SHA-1: `438ca03b7388044ed19264a3813cf9cc992df4d5`
- Asset base: <https://resources.download.minecraft.net/>

The amethyst block/bud/cluster, smooth-basalt, and tinted-glass textures, amethyst shard and
spyglass item textures, and the spyglass scope overlay are unmodified assets extracted from the same official
Java 1.18.2 client artifact. The 31 amethyst Ogg Vorbis files and two spyglass Ogg Vorbis files
are unmodified objects from the official 1.18 asset index and use the canonical 1.18.2 sound
definitions, including their per-entry pitch and volume settings.

The eleven basalt Ogg Vorbis files used by smooth basalt are likewise unmodified objects from
that asset index and are wired through the canonical `block.basalt.*` sound definitions.

- Java 1.18.2 client SHA-1: `2e9a3e3107cca00d6bc9c97bf7d149cae163ef21`
- Asset-index SHA-1: `d31a2e85ae149dd1b1a7070b22cb8887892fda6c`
- Sound-definition SHA-1: `438ca03b7388044ed19264a3813cf9cc992df4d5`
- Asset base: <https://resources.download.minecraft.net/>

Waterlogged amethyst companions are hidden block-only storage states. Their translucent overlay
references the animated `minecraft:blocks/water_still` texture supplied by the required Java
1.12.2 runtime and applies the runtime biome-water tint; no copied water texture is distributed.

Minecraft assets remain the property of Mojang Studios. They are included only as required
backport resources and are not covered by this project's source-code license.

The powder-snow block and bucket textures, plus all powder-snow block and bucket sounds, are
unmodified Minecraft 1.18.2 assets from Mojang's official client and asset-index artifacts:

- Version: `1.18.2`
- Client SHA-1: `2e9a3e3107cca00d6bc9c97bf7d149cae163ef21`
- Version metadata: <https://piston-meta.mojang.com/mc/game/version_manifest_v2.json>
- Asset paths: `minecraft/textures/block/powder_snow.png`,
  `minecraft/textures/item/powder_snow_bucket.png`,
  `minecraft/sounds/block/powder_snow/*.ogg`, and
  `minecraft/sounds/item/bucket/*_powder_snow*.ogg`

The powder-snow block model is a syntax-only 1.12 adaptation of Mojang's 1.18.2 model. The
cauldron models reuse Minecraft 1.12.2 cauldron geometry with the official powder-snow texture.
These assets remain the property of Mojang Studios and are not covered by this project's
source-code license.

The composter side/top/bottom/compost/ready textures and its fifteen Ogg Vorbis sound files are
unmodified Minecraft 1.18.2 assets from Mojang's official client and 1.18 asset index. The base,
contents, and item models preserve Mojang's geometry and level heights with only the resource-path
syntax adapted for Java 1.12.2. The composter particle uses the 1.12 dirt block-crack peer because
the dedicated 1.18 composter particle type does not exist in the target runtime.

- Java 1.18.2 client SHA-1: `2e9a3e3107cca00d6bc9c97bf7d149cae163ef21`
- Asset-index SHA-1: `d31a2e85ae149dd1b1a7070b22cb8887892fda6c`
- Sound-definition SHA-1: `438ca03b7388044ed19264a3813cf9cc992df4d5`
- Asset base: <https://resources.download.minecraft.net/>

The copper ore, deepslate copper ore, raw copper block, copper block oxidation stages, cut-copper
oxidation stages, raw copper, and copper ingot textures are unmodified Java 1.18.2 client assets.
The ten copper block sounds, six axe scrape/wax-off sounds, and two trident thunder sounds are
unmodified objects from the official 1.18 asset index. Models and blockstates preserve Mojang's
texture selection and stair rotations with only the Java 1.12 slab property names and resource
paths adapted. Waxed variants intentionally reuse the matching unwaxed-stage texture, as in
Java 1.18.2. The lightning rod's two textures and geometry are likewise adapted directly from
that client artifact; the hidden waterlogged storage block reuses the same model.

- Java 1.18.2 client SHA-1: `2e9a3e3107cca00d6bc9c97bf7d149cae163ef21`
- Asset-index SHA-1: `d31a2e85ae149dd1b1a7070b22cb8887892fda6c`
- Sound-definition SHA-1: `438ca03b7388044ed19264a3813cf9cc992df4d5`
- Asset base: <https://resources.download.minecraft.net/>

The stonecutter block textures, animated saw metadata, container texture, and two result Ogg
Vorbis files are unmodified Minecraft 1.18.2 assets from Mojang's official client and 1.18 asset
index. The block model and blockstate preserve Mojang's geometry and rotations with resource-path
syntax adapted for Java 1.12.2. The selection event delegates to the target runtime's identical
`minecraft:random/click` sound object.

- Java 1.18.2 client SHA-1: `2e9a3e3107cca00d6bc9c97bf7d149cae163ef21`
- Asset-index SHA-1: `d31a2e85ae149dd1b1a7070b22cb8887892fda6c`
- Result sound object SHA-1s: `f9c33914acfd606ea5c624f25d33cccc60663e22`,
  `34eec70d1a60aba94b92065274fd456a97b1e036`
- Asset base: <https://resources.download.minecraft.net/>

The five axolotl variant textures, axolotl-bucket and tropical-fish-bucket item textures, and all
36 axolotl, dolphin-derived swim/splash, fish-swim, and bucket Ogg Vorbis files are unmodified
Minecraft Java 1.18.2 assets. The entity model preserves the official 64x64 geometry while adapting
the hierarchical parts to Java 1.12.2's flat `ModelRenderer` API. Sound definitions preserve the
official per-entry pitch and volume values. The tropical-fish bucket is the documented narrow
1.12 clownfish bridge; its artwork is still the canonical 1.18.2 item texture.

The dripstone-block texture, ten directional pointed-dripstone textures, pointed-dripstone item
texture, and their model geometry are unmodified Java 1.18.2 client assets, with only namespace
and Java 1.12 model-path syntax adapted. The eleven shared dripstone break/step sounds, thirty-three
water/lava drip sounds, and five pointed-dripstone landing sounds are unmodified objects from the
official 1.18 asset index and use the Java 1.18.2 sound definitions.
The lava-cauldron model uses the target runtime's matching vanilla cauldron geometry and textures
with its content surface switched to the animated runtime lava texture; no copied lava texture is
distributed.

The lush-cave block and item textures, blockstates, model geometry, and ninety-four Ogg Vorbis
files are unmodified Java 1.18.2 client and Java 1.18 asset-index resources. This includes cave
vines and glow berries; azalea bushes, leaves, roots, and potted variants; moss; both dripleaf
families; spore blossom; the exact big-dripleaf tilt sounds; and the sweet-berry picking sounds
used by cave vines. Resource namespaces, no-property variant keys, and the 1.12 item texture path
are syntax-only adaptations required by Forge 1.12.2. Hidden age-band and waterlogged storage
blockstates reuse the matching canonical model exactly.

- Java 1.18.2 client SHA-1: `2e9a3e3107cca00d6bc9c97bf7d149cae163ef21`
- Asset-index SHA-1: `d31a2e85ae149dd1b1a7070b22cb8887892fda6c`
- Sound-definition SHA-1: `438ca03b7388044ed19264a3813cf9cc992df4d5`
- Asset base: <https://resources.download.minecraft.net/>
