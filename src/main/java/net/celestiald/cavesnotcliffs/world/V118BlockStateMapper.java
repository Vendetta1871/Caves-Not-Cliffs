package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.worldgen.v118.V118Material;
import net.minecraft.block.Block;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockSand;
import net.minecraft.block.BlockStone;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.EnumDyeColor;
import net.minecraftforge.fml.common.registry.GameRegistry;

/**
 * Immutable translation from generation-local 1.18 materials to registered 1.12 block states.
 *
 * <p>The column cache deliberately contains no Forge registry objects. Resolving the registry once
 * at the section boundary keeps cached columns deterministic and prevents later registry lookups from
 * changing the meaning of a stored material id.</p>
 */
public final class V118BlockStateMapper {
    private static final String DEEPSLATE = "cavesnotcliffs:deepslate";
    private static final String TUFF = "cavesnotcliffs:tuff";
    private static final String COPPER_ORE = "cavesnotcliffs:copper_ore";
    private static final String RAW_COPPER_BLOCK = "cavesnotcliffs:raw_copper_block";
    private static final String DEEPSLATE_IRON_ORE = "cavesnotcliffs:deepslate_iron_ore";
    private static final String RAW_IRON_BLOCK = "cavesnotcliffs:raw_iron_block";
    private static final String CALCITE = "cavesnotcliffs:calcite";
    private static final String POWDER_SNOW = "cavesnotcliffs:powder_snow";

    private final IBlockState[] states = new IBlockState[V118Material.values().length];

    /** Resolves every mod state after Forge registries have finished loading. */
    public static V118BlockStateMapper fromRegisteredBlocks() {
        return new V118BlockStateMapper(
            registeredState(DEEPSLATE),
            registeredState(TUFF),
            registeredState(COPPER_ORE),
            registeredState(RAW_COPPER_BLOCK),
            registeredState(DEEPSLATE_IRON_ORE),
            registeredState(RAW_IRON_BLOCK),
            registeredState(CALCITE),
            registeredState(POWDER_SNOW));
    }

    V118BlockStateMapper(IBlockState deepslate, IBlockState tuff,
            IBlockState copperOre, IBlockState rawCopperBlock,
            IBlockState deepslateIronOre, IBlockState rawIronBlock, IBlockState calcite,
            IBlockState powderSnow) {
        states[V118Material.AIR.ordinal()] = Blocks.AIR.getDefaultState();
        states[V118Material.STONE.ordinal()] = Blocks.STONE.getDefaultState();
        states[V118Material.WATER.ordinal()] = Blocks.WATER.getDefaultState();
        states[V118Material.LAVA.ordinal()] = Blocks.LAVA.getDefaultState();
        states[V118Material.BEDROCK.ordinal()] = Blocks.BEDROCK.getDefaultState();
        states[V118Material.DEEPSLATE.ordinal()] = requireState(deepslate, DEEPSLATE);
        states[V118Material.TUFF.ordinal()] = requireState(tuff, TUFF);
        states[V118Material.GRANITE.ordinal()] = Blocks.STONE.getDefaultState()
            .withProperty(BlockStone.VARIANT, BlockStone.EnumType.GRANITE);
        states[V118Material.COPPER_ORE.ordinal()] = requireState(copperOre, COPPER_ORE);

        states[V118Material.RAW_COPPER_BLOCK.ordinal()] =
            requireState(rawCopperBlock, RAW_COPPER_BLOCK);
        states[V118Material.DEEPSLATE_IRON_ORE.ordinal()] =
            requireState(deepslateIronOre, DEEPSLATE_IRON_ORE);
        states[V118Material.RAW_IRON_BLOCK.ordinal()] =
            requireState(rawIronBlock, RAW_IRON_BLOCK);
        states[V118Material.DIRT.ordinal()] = Blocks.DIRT.getDefaultState()
            .withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.DIRT);
        states[V118Material.PODZOL.ordinal()] = Blocks.DIRT.getDefaultState()
            .withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.PODZOL);
        states[V118Material.COARSE_DIRT.ordinal()] = Blocks.DIRT.getDefaultState()
            .withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.COARSE_DIRT);
        states[V118Material.MYCELIUM.ordinal()] = Blocks.MYCELIUM.getDefaultState();
        states[V118Material.GRASS_BLOCK.ordinal()] = Blocks.GRASS.getDefaultState();
        states[V118Material.CALCITE.ordinal()] = requireState(calcite, CALCITE);
        states[V118Material.GRAVEL.ordinal()] = Blocks.GRAVEL.getDefaultState();
        states[V118Material.SAND.ordinal()] = Blocks.SAND.getDefaultState()
            .withProperty(BlockSand.VARIANT, BlockSand.EnumType.SAND);
        states[V118Material.SANDSTONE.ordinal()] = Blocks.SANDSTONE.getDefaultState();
        states[V118Material.RED_SAND.ordinal()] = Blocks.SAND.getDefaultState()
            .withProperty(BlockSand.VARIANT, BlockSand.EnumType.RED_SAND);
        states[V118Material.RED_SANDSTONE.ordinal()] = Blocks.RED_SANDSTONE.getDefaultState();
        states[V118Material.TERRACOTTA.ordinal()] = Blocks.HARDENED_CLAY.getDefaultState();
        states[V118Material.WHITE_TERRACOTTA.ordinal()] = clay(EnumDyeColor.WHITE);
        states[V118Material.ORANGE_TERRACOTTA.ordinal()] = clay(EnumDyeColor.ORANGE);
        states[V118Material.YELLOW_TERRACOTTA.ordinal()] = clay(EnumDyeColor.YELLOW);
        states[V118Material.BROWN_TERRACOTTA.ordinal()] = clay(EnumDyeColor.BROWN);
        states[V118Material.RED_TERRACOTTA.ordinal()] = clay(EnumDyeColor.RED);
        states[V118Material.LIGHT_GRAY_TERRACOTTA.ordinal()] = clay(EnumDyeColor.SILVER);
        states[V118Material.PACKED_ICE.ordinal()] = Blocks.PACKED_ICE.getDefaultState();
        states[V118Material.SNOW_BLOCK.ordinal()] = Blocks.SNOW.getDefaultState();
        states[V118Material.POWDER_SNOW.ordinal()] = requireState(powderSnow, POWDER_SNOW);
        states[V118Material.ICE.ordinal()] = Blocks.ICE.getDefaultState();

        for (V118Material material : V118Material.values()) {
            if (states[material.ordinal()] == null) {
                throw new IllegalStateException("No block state mapping for " + material);
            }
        }
    }

    public IBlockState stateFor(int storageId) {
        return stateFor(V118Material.fromStorageId(storageId));
    }

    public IBlockState stateFor(V118Material material) {
        if (material == null) {
            throw new NullPointerException("material");
        }
        return states[material.ordinal()];
    }

    private static IBlockState registeredState(String id) {
        Block block = GameRegistry.findRegistry(Block.class)
            .getValue(new ResourceLocation(id));
        if (block == null) {
            throw new IllegalStateException("Required terrain block is not registered: " + id);
        }
        return block.getDefaultState();
    }

    private static IBlockState requireState(IBlockState state, String id) {
        if (state == null) {
            throw new IllegalStateException("Required terrain block state is unavailable: " + id);
        }
        return state;
    }

    private static IBlockState clay(EnumDyeColor color) {
        return Blocks.STAINED_HARDENED_CLAY.getDefaultState()
            .withProperty(BlockColored.COLOR, color);
    }
}
