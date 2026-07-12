# Asset sources

The blue-ice texture is the unmodified Java 1.18.2 client asset at
`assets/minecraft/textures/block/blue_ice.png`, renamed only to Java 1.12.2's `blocks`
texture directory. Its cube model and nine-packed-ice recipe preserve the canonical peer.

- Java 1.18.2 client SHA-1: `2e9a3e3107cca00d6bc9c97bf7d149cae163ef21`
- Version metadata: <https://piston-meta.mojang.com/mc/game/version_manifest_v2.json>

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
The dedicated 1.18 scrape and wax-off particle sprites do not exist in Java 1.12.2; their bridge
retains the exact six-face positions, velocities, and 3-to-5 count while using block-crack and
instant-spell target-runtime sprites respectively.

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

The lush-cave block and item textures, blockstates, model geometry, and ninety-nine Ogg Vorbis
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

The four bee renderer textures, unused canonical stinger atlas, bee-nest/beehive block textures,
honeycomb item texture, and all 31 bee and beehive Ogg Vorbis files are unmodified Minecraft Java
1.18.2 assets. Entity geometry, block models, blockstate rotations, and sound definitions preserve
the official 1.18.2 values, with only Java 1.12.2 resource-path syntax and a hidden full-honey block
state used to bridge the legacy four-bit metadata limit.

- Java 1.18.2 client SHA-1: `2e9a3e3107cca00d6bc9c97bf7d149cae163ef21`
- Asset-index SHA-1: `d31a2e85ae149dd1b1a7070b22cb8887892fda6c`
- Sound-definition SHA-1: `438ca03b7388044ed19264a3813cf9cc992df4d5`
- Asset base: <https://resources.download.minecraft.net/>

The honey-block, honeycomb-block, and honey-bottle textures; honey-block, coral-block,
honey-drink, and honeycomb-waxing sounds; and their models and sound definitions are direct
Java 1.18.2 assets. Resource paths and model syntax are adapted only for Java 1.12.2. The wax-on
effect uses the closest target-runtime spell sprite while retaining the official per-face count,
position, velocity, and sound contract because the dedicated 1.18 particle type is unavailable.

- Java 1.18.2 client SHA-1: `2e9a3e3107cca00d6bc9c97bf7d149cae163ef21`
- Asset-index SHA-1: `d31a2e85ae149dd1b1a7070b22cb8887892fda6c`
- Sound-definition SHA-1: `438ca03b7388044ed19264a3813cf9cc992df4d5`
- Asset base: <https://resources.download.minecraft.net/>

The normal and soul campfire block/item textures, animated flames and lit logs, soul-soil
texture, six campfire crackles, and eleven soul-soil break/step sounds are unmodified Java 1.18.2
assets. Campfire geometry, display transforms, rotations, recipes, animation metadata, sound
definitions, and cooking-item transforms preserve the official values. Resource paths are adapted
for Java 1.12.2, and waterlogged states add the existing source-water overlay because the target
runtime has no separate fluid-state renderer. Signal smoke uses the target runtime's large-smoke
sprite with the official tall-column velocity because its dedicated signal-smoke particle does
not exist in Java 1.12.2. Nether biome generation is intentionally unchanged by the v2 scope.

The seventeen candle block and item textures, all candle and candle-cake model geometry, and the
twenty-five candle/cake Ogg Vorbis files are unmodified Java 1.18.2 resources. The blockstates and
models retain Mojang's exact color, count, lit-state, and cake geometry; only namespaces, legacy
`blocks`/`items` texture-directory syntax, and a multipart source-water overlay are adapted for
Forge 1.12.2. Java 1.12.2 has no `SMALL_FLAME` particle, so
the runtime uses its otherwise equivalent `FLAME` sprite at Mojang's exact offsets and timing.

- Java 1.18.2 client SHA-1: `2e9a3e3107cca00d6bc9c97bf7d149cae163ef21`
- Asset-index SHA-1: `d31a2e85ae149dd1b1a7070b22cb8887892fda6c`
- Sound-definition SHA-1: `438ca03b7388044ed19264a3813cf9cc992df4d5`
- Asset base: <https://resources.download.minecraft.net/>

The plain-pumpkin side/top textures, carved-pumpkin face, jack-o-lantern face, and two
pumpkin-carving sounds are unmodified Java 1.18.2 assets. The stateless block and item models
preserve Mojang's texture selection and display transform, with only the `blocks` texture
directory and mod namespace adapted for Forge 1.12.2. The target runtime's
`minecraft:pumpkin` remains the carved, directional peer; its four legacy texture slots are
overridden with the matching official Java 1.18.2 client resources:

- `assets/minecraft/textures/block/pumpkin_side.png` ->
  `assets/minecraft/textures/blocks/pumpkin_side.png`
- `assets/minecraft/textures/block/pumpkin_top.png` ->
  `assets/minecraft/textures/blocks/pumpkin_top.png`
- `assets/minecraft/textures/block/carved_pumpkin.png` ->
  `assets/minecraft/textures/blocks/pumpkin_face_off.png`
- `assets/minecraft/textures/block/jack_o_lantern.png` ->
  `assets/minecraft/textures/blocks/pumpkin_face_on.png`

- Java 1.18.2 client SHA-1: `2e9a3e3107cca00d6bc9c97bf7d149cae163ef21`
- Version metadata: <https://piston-meta.mojang.com/mc/game/version_manifest_v2.json>
- Asset-index SHA-1: `d31a2e85ae149dd1b1a7070b22cb8887892fda6c`
- Sound-definition SHA-1: `438ca03b7388044ed19264a3813cf9cc992df4d5`
- Asset base: <https://resources.download.minecraft.net/>

The spruce log, log-top, leaves, and sapling textures are unmodified Java 1.18.2 client assets.
Their filenames are adapted only to Java 1.12.2's existing spruce block and item models:

- `assets/minecraft/textures/block/spruce_log.png` ->
  `assets/minecraft/textures/blocks/log_spruce.png`
- `assets/minecraft/textures/block/spruce_log_top.png` ->
  `assets/minecraft/textures/blocks/log_spruce_top.png`
- `assets/minecraft/textures/block/spruce_leaves.png` ->
  `assets/minecraft/textures/blocks/leaves_spruce.png`
- `assets/minecraft/textures/block/spruce_sapling.png` ->
  `assets/minecraft/textures/blocks/sapling_spruce.png`

- Java 1.18.2 client SHA-1: `2e9a3e3107cca00d6bc9c97bf7d149cae163ef21`
- Version metadata: <https://piston-meta.mojang.com/mc/game/version_manifest_v2.json>

The oak log, log-top, leaves, and sapling textures are unmodified Java 1.18.2 client assets.
Their filenames are adapted only to Java 1.12.2's existing oak block and item models:

- `assets/minecraft/textures/block/oak_log.png` ->
  `assets/minecraft/textures/blocks/log_oak.png`
- `assets/minecraft/textures/block/oak_log_top.png` ->
  `assets/minecraft/textures/blocks/log_oak_top.png`
- `assets/minecraft/textures/block/oak_leaves.png` ->
  `assets/minecraft/textures/blocks/leaves_oak.png`
- `assets/minecraft/textures/block/oak_sapling.png` ->
  `assets/minecraft/textures/blocks/sapling_oak.png`

Fancy-oak branches use the official Java 1.18.2 `cube_column_horizontal` face layout and oak
horizontal-log model. Their JSON namespace, texture paths, and blockstate syntax are adapted for
Forge 1.12.2. The overridden vanilla oak-log blockstate preserves 1.12.2's `axis=none` bark model.
The two generic models live in the mod namespace so this backport does not claim a shared
`minecraft` model path that another 1.12.2 backport may also supply:

- `assets/minecraft/models/block/cube_column_horizontal.json` ->
  `assets/cavesnotcliffs/models/block/cube_column_horizontal_118.json`
- `assets/minecraft/models/block/oak_log_horizontal.json` ->
  `assets/cavesnotcliffs/models/block/oak_log_horizontal_118.json`
- `assets/minecraft/blockstates/oak_log.json` ->
  `assets/minecraft/blockstates/oak_log.json` (1.12.2 state schema retained)

- Java 1.18.2 client SHA-1: `2e9a3e3107cca00d6bc9c97bf7d149cae163ef21`
- Version metadata: <https://piston-meta.mojang.com/mc/game/version_manifest_v2.json>

The dandelion, poppy, and blue-orchid textures are unmodified Java 1.18.2 client assets. Their
filenames are adapted only to the texture paths used by Java 1.12.2's existing flower models:

- `assets/minecraft/textures/block/dandelion.png` ->
  `assets/minecraft/textures/blocks/flower_dandelion.png`
- `assets/minecraft/textures/block/poppy.png` ->
  `assets/minecraft/textures/blocks/flower_rose.png`
- `assets/minecraft/textures/block/blue_orchid.png` ->
  `assets/minecraft/textures/blocks/flower_blue_orchid.png`

- Java 1.18.2 client SHA-1: `2e9a3e3107cca00d6bc9c97bf7d149cae163ef21`
- Version metadata: <https://piston-meta.mojang.com/mc/game/version_manifest_v2.json>

The mushroom-stem, red/brown-mushroom-block, and mushroom-block-inside textures, six multipart face mappings, block face
models, and inventory model are adapted directly from the Java 1.18.2 client. The textures are
unmodified; model namespaces and the legacy `blocks` texture directory are syntax-only Forge
1.12.2 adaptations. Hidden block-only identities preserve each vertical face pair that does not
fit beside the four horizontal booleans in legacy metadata; the canonical public cap identities
remain the vanilla red and brown mushroom blocks.

- Java 1.18.2 client SHA-1: `2e9a3e3107cca00d6bc9c97bf7d149cae163ef21`
- Version metadata: <https://piston-meta.mojang.com/mc/game/version_manifest_v2.json>

The Otherside music-disc texture and Ogg Vorbis recording are the unmodified Java 1.18.2
resources. Its item model changes only the namespace and Java 1.12.2 `items` texture-directory
syntax. The sound event preserves Mojang's streamed `music_disc.otherside` definition; the item
tooltip preserves the official `Lena Raine - otherside` credit. Mojang's sound definition has no
separate accessibility-subtitle key for this streamed record.

- Java 1.18.2 client SHA-1: `2e9a3e3107cca00d6bc9c97bf7d149cae163ef21`
- Item texture SHA-1: `44983ebd8b59412f2fba26d5f752b0c78c27a197`
- Asset-index SHA-1: `d31a2e85ae149dd1b1a7070b22cb8887892fda6c`
- Sound-definition SHA-1: `438ca03b7388044ed19264a3813cf9cc992df4d5`
- Otherside Ogg SHA-1: `a5effd79795773422bb4de85841838f3ad9c216d`
- Asset base: <https://resources.download.minecraft.net/>
