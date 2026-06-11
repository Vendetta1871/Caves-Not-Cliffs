# Block Reference — Caves Not Cliffs

Source of truth: decompiled `libs/cavesandcliffs-1.0.9.jar`.

---

## Block Name Mapping

| Registry name       | Texture file      | In-game name |
|---------------------|-------------------|--------------|
| `dark_stone`        | `dark_stone.png`  | Tuff         |
| `unknown_stone`     | `unknown_stone.png` | Calcite    |
| `unnamed_stone`     | `unnamed_stone.png` | Deepslate  |

---

## Amethyst Crystals

All 3 stages use `block/cross` model — flat X-shaped sprite, no FACING property.

| Block                    | Texture                     | Blockstate variant |
|--------------------------|-----------------------------|--------------------|
| `amethyst_crystal`       | `blocks/amethyst_crystal`   | `"normal"`         |
| `amethyst_crystal_stage_1` | `blocks/amethyst_crystal_stage_1` | `"normal"` |
| `amethyst_crystal_stage_2` | `blocks/amethyst_crystal_stage_2` | `"normal"` |

Geode generation: radius `6 + random.nextInt(4)` (6–9). Use `getDefaultState()`, not `getStateFromMeta(face.getIndex())`.

---

## Stairs — extends `BlockStairs`

Constructor: `super(new Block(Material.ROCK).getDefaultState())`

Blockstate: 40 variants (`facing` × `half` × `shape`).  
Models needed: `{name}.json`, `{name}_inner.json`, `{name}_outer.json` — parents `block/stairs`, `block/inner_stairs`, `block/outer_stairs`.

| Block                    | Registry name             | Texture                |
|--------------------------|---------------------------|------------------------|
| `BlockDarkStoneStairs`   | `dark_stone_stairs`       | `blocks/dark_stone`    |
| `BlockUnknownStoneStairs`| `unknown_stone_stairs`    | `blocks/unknown_stone` |
| `BlockCopperStairs`      | `copper_stairs`           | `blocks/copper_block`  |
| `BlockCopperStairsStage1`| `copper_stairs_stage1`    | `blocks/copper_block_stage1` |
| `BlockCopperStairsStage2`| `copper_stairs_stage2`    | `blocks/copper_block_stage2` |
| `BlockCopperStairsStage3`| `copper_stairs_stage3`    | `blocks/copper_block_stage3` |

Item model: `"parent": "cavesnotcliffs:block/{name}"`

---

## Slabs — extends `BlockSlab`

Constructor: `super(Material.ROCK)` — requires VARIANT PropertyEnum + Double inner class.  
Register TWO blocks: `{name}` (single) + `{name}_double` (Double subclass, no item).  
Item: `new ItemSlab(block, (BlockSlab) block, (BlockSlab) block_slab_double)`.

Blockstate: `half=bottom,variant=default` → `{name}.json`; `half=top,variant=default` → `{name}_top.json`.  
Double blockstate: `variant=default` → `{name}_double.json`.  
Models: `{name}.json` (`block/half_slab`), `{name}_top.json` (`block/upper_slab`), `{name}_double.json` (`block/cube_all`).

| Block                   | Registry name          | Double registry        | Texture                |
|-------------------------|------------------------|------------------------|------------------------|
| `BlockDarkStoneSlab`    | `dark_stone_slab`      | `dark_stone_slab_double`    | `blocks/dark_stone`    |
| `BlockUnknownStoneSlab` | `unknown_stone_slab`   | `unknown_stone_slab_double` | `blocks/unknown_stone` |
| `BlockCopperSlab`       | `copper_slab`          | `copper_slab_double`        | `blocks/copper_block`  |
| `BlockCopperSlabStage1` | `copper_slab_stage1`   | `copper_slab_stage1_double` | `blocks/copper_block_stage1` |
| `BlockCopperSlabStage2` | `copper_slab_stage2`   | `copper_slab_stage2_double` | `blocks/copper_block_stage2` |
| `BlockCopperSlabStage3` | `copper_slab_stage3`   | `copper_slab_stage3_double` | `blocks/copper_block_stage3` |

Item model: `"parent": "cavesnotcliffs:block/{name}"` (shows bottom half in inventory)

---

## Walls — extends `BlockWall`

Constructor: `super(new Block(Material.ROCK))`

Blockstate: multipart with `up`, `north`, `south`, `east`, `west` conditions.  
Models: `{name}.json` (`block/wall_side`), `{name}_post.json` (`block/wall_post`), `{name}_inventory.json` (`block/wall_inventory`).  
Item model: `"parent": "cavesnotcliffs:block/{name}_inventory"`

| Block                  | Registry name        | Texture                |
|------------------------|----------------------|------------------------|
| `BlockDarkStoneWalls`  | `dark_stone_walls`   | `blocks/dark_stone`    |
| `BlockUnknownStoneWall`| `unknown_stone_wall` | `blocks/unknown_stone` |

---

## Dripleaf Tilt Mechanic — 3 separate blocks

| Block                 | Registry name      | State         | Transition                    |
|-----------------------|--------------------|---------------|-------------------------------|
| `BlockDripleafPlant`  | `dripleaf_plant`   | Upright       | `onEntityWalk` → `dripleafplant_1` |
| `BlockDripleafplant1` | `dripleafplant_1`  | Slightly tilted | `onEntityWalk` → `dripleaf_plant_2`; `randomTick` → `dripleaf_plant` |
| `BlockDripleafPlant2` | `dripleaf_plant_2` | Fully tilted  | `randomTick` → `dripleafplant_1` |

All 3 have `Material.AIR`, `setTickRandomly(true)`, `getCollisionBoundingBox` → `NULL_AABB`, `getBlockLayer` → `CUTOUT`.

Custom Blockbench models from JAR: `custom/dripleafplant.json`, `custom/dripleafplant1.json`, `custom/dripleafplant2.json`.  
Textures: `#1` = `dripleaf_plant.png`, `#2` = `dripleaf_plant_1_1_1.png`, `#3` = `dripleafsides.png`.

Blockstate: 4 FACING variants (`facing=north` = default/0°, `east` = y:90, `south` = y:180, `west` = y:270).

---

## Spore Blossom

Custom Blockbench model: `custom/sporeblossom.json` (5 elements: 4 petals ±22.5°, flat green disc).  
Block model: `"parent": "cavesnotcliffs:custom/sporeblossom"`, textures `"0"` and `"2"` → `blocks/spore_blossom`.  
Blockstate: `"normal"` variant (no FACING).
