package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.worldgen.v118.V118Material;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStone;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;

/**
 * Immutable translation from generation-local 1.18 materials to registered 1.12 block states.
 *
 * <p>The column cache deliberately contains no Forge registry objects. Resolving the registry once
 * at the cube boundary keeps cached columns deterministic and prevents later registry lookups from
 * changing the meaning of a stored material id.</p>
 */
public final class V118BlockStateMapper {
    private static final String DEEPSLATE = "cavesnotcliffs:deepslate";
    private static final String TUFF = "cavesnotcliffs:tuff";
    private static final String COPPER_ORE = "cavesnotcliffs:copper_ore";
    private static final String COPPER_BLOCK = "cavesnotcliffs:copper_block";
    private static final String DEEPSLATE_IRON_ORE = "cavesnotcliffs:deepslate_iron_ore";

    private final IBlockState[] states = new IBlockState[V118Material.values().length];

    /** Resolves every mod state after Forge registries have finished loading. */
    public static V118BlockStateMapper fromRegisteredBlocks() {
        return new V118BlockStateMapper(
            registeredState(DEEPSLATE),
            registeredState(TUFF),
            registeredState(COPPER_ORE),
            registeredState(COPPER_BLOCK),
            registeredState(DEEPSLATE_IRON_ORE),
            Blocks.IRON_BLOCK.getDefaultState());
    }

    V118BlockStateMapper(IBlockState deepslate, IBlockState tuff,
            IBlockState copperOre, IBlockState copperBlock,
            IBlockState deepslateIronOre, IBlockState ironBlock) {
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

        // Raw metal blocks have not yet landed in this checkpoint. These registered blocks retain
        // the same nine-ingot resource value and are deterministic until their canonical peers are
        // added by the content checkpoint.
        states[V118Material.RAW_COPPER_BLOCK.ordinal()] =
            requireState(copperBlock, COPPER_BLOCK);
        states[V118Material.DEEPSLATE_IRON_ORE.ordinal()] =
            requireState(deepslateIronOre, DEEPSLATE_IRON_ORE);
        states[V118Material.RAW_IRON_BLOCK.ordinal()] =
            requireState(ironBlock, "minecraft:iron_block");

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
}
