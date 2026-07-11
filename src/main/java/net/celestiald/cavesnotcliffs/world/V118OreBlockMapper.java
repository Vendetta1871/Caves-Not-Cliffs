package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.worldgen.v118.V118OreMaterial;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSilverfish;
import net.minecraft.block.BlockStone;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.IdentityHashMap;
import java.util.Map;

/** Runtime translation for the non-persisted ordinary-decoration material palette. */
final class V118OreBlockMapper {
    private final IBlockState[] states;
    private final Map<Block, V118OreMaterial> materialByBlock = new IdentityHashMap<>();

    static V118OreBlockMapper fromRegisteredBlocks() {
        IBlockState[] states = new IBlockState[V118OreMaterial.values().length];
        put(states, V118OreMaterial.AIR, Blocks.AIR.getDefaultState());
        put(states, V118OreMaterial.WATER, Blocks.WATER.getDefaultState());
        put(states, V118OreMaterial.STONE, Blocks.STONE.getDefaultState());
        put(states, V118OreMaterial.DEEPSLATE, registered("deepslate"));
        put(states, V118OreMaterial.TUFF, registered("tuff"));
        put(states, V118OreMaterial.GRANITE, stone(1));
        put(states, V118OreMaterial.DIORITE, stone(3));
        put(states, V118OreMaterial.ANDESITE, stone(5));
        put(states, V118OreMaterial.DIRT, Blocks.DIRT.getDefaultState());
        put(states, V118OreMaterial.GRAVEL, Blocks.GRAVEL.getDefaultState());
        put(states, V118OreMaterial.CLAY, Blocks.CLAY.getDefaultState());
        put(states, V118OreMaterial.GRASS_BLOCK, Blocks.GRASS.getDefaultState());
        put(states, V118OreMaterial.SAND, Blocks.SAND.getDefaultState());
        put(states, V118OreMaterial.SANDSTONE, Blocks.SANDSTONE.getDefaultState());
        put(states, V118OreMaterial.COAL_ORE, Blocks.COAL_ORE.getDefaultState());
        put(states, V118OreMaterial.DEEPSLATE_COAL_ORE, registered("deepslate_coal_ore"));
        put(states, V118OreMaterial.IRON_ORE, Blocks.IRON_ORE.getDefaultState());
        put(states, V118OreMaterial.DEEPSLATE_IRON_ORE, registered("deepslate_iron_ore"));
        put(states, V118OreMaterial.GOLD_ORE, Blocks.GOLD_ORE.getDefaultState());
        put(states, V118OreMaterial.DEEPSLATE_GOLD_ORE, registered("deepslate_gold_ore"));
        put(states, V118OreMaterial.COPPER_ORE, registered("copper_ore"));
        put(states, V118OreMaterial.DEEPSLATE_COPPER_ORE,
            registered("deepslate_copper_ore"));
        put(states, V118OreMaterial.REDSTONE_ORE, Blocks.REDSTONE_ORE.getDefaultState());
        put(states, V118OreMaterial.DEEPSLATE_REDSTONE_ORE,
            registered("deepslate_redstone_ore"));
        put(states, V118OreMaterial.LAPIS_ORE, Blocks.LAPIS_ORE.getDefaultState());
        put(states, V118OreMaterial.DEEPSLATE_LAPIS_ORE, registered("deepslate_lapis_ore"));
        put(states, V118OreMaterial.DIAMOND_ORE, Blocks.DIAMOND_ORE.getDefaultState());
        put(states, V118OreMaterial.DEEPSLATE_DIAMOND_ORE,
            registered("deepslate_diamond_ore"));
        put(states, V118OreMaterial.EMERALD_ORE, Blocks.EMERALD_ORE.getDefaultState());
        put(states, V118OreMaterial.DEEPSLATE_EMERALD_ORE,
            registered("deepslate_emerald_ore"));
        put(states, V118OreMaterial.INFESTED_STONE, Blocks.MONSTER_EGG.getDefaultState()
            .withProperty(BlockSilverfish.VARIANT, BlockSilverfish.EnumType.byMetadata(0)));
        put(states, V118OreMaterial.INFESTED_DEEPSLATE, registered("infested_deepslate"));
        return new V118OreBlockMapper(states);
    }

    V118OreBlockMapper(IBlockState[] states) {
        if (states == null || states.length != V118OreMaterial.values().length) {
            throw new IllegalArgumentException("Expected one state slot per ore material");
        }
        this.states = states.clone();
        for (V118OreMaterial material : V118OreMaterial.values()) {
            if (material == V118OreMaterial.OTHER) {
                continue;
            }
            IBlockState state = this.states[material.ordinal()];
            if (state == null) {
                throw new IllegalArgumentException("Missing state for " + material);
            }
            Block block = state.getBlock();
            if (block != Blocks.STONE && block != Blocks.MONSTER_EGG) {
                V118OreMaterial previous = materialByBlock.put(block, material);
                if (previous != null && previous != material) {
                    materialByBlock.remove(block);
                }
            }
        }
    }

    V118OreMaterial materialFor(IBlockState state) {
        if (state == null || state.getBlock() == Blocks.AIR) {
            return V118OreMaterial.AIR;
        }
        if (state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.FLOWING_WATER) {
            return V118OreMaterial.WATER;
        }
        if (state.getBlock() == Blocks.STONE) {
            int metadata = state.getValue(BlockStone.VARIANT).getMetadata();
            switch (metadata) {
                case 1:
                    return V118OreMaterial.GRANITE;
                case 3:
                    return V118OreMaterial.DIORITE;
                case 5:
                    return V118OreMaterial.ANDESITE;
                case 0:
                    return V118OreMaterial.STONE;
                default:
                    return V118OreMaterial.OTHER;
            }
        }
        if (state.getBlock() == Blocks.MONSTER_EGG) {
            return state.getValue(BlockSilverfish.VARIANT).getMetadata() == 0
                ? V118OreMaterial.INFESTED_STONE : V118OreMaterial.OTHER;
        }
        V118OreMaterial material = materialByBlock.get(state.getBlock());
        return material == null ? V118OreMaterial.OTHER : material;
    }

    IBlockState stateFor(V118OreMaterial material) {
        if (material == null || material == V118OreMaterial.OTHER) {
            throw new IllegalArgumentException("No output block state for " + material);
        }
        return states[material.ordinal()];
    }

    private static void put(IBlockState[] states, V118OreMaterial material,
            IBlockState state) {
        states[material.ordinal()] = state;
    }

    private static IBlockState stone(int metadata) {
        return Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT,
            BlockStone.EnumType.byMetadata(metadata));
    }

    private static IBlockState registered(String path) {
        Block block = GameRegistry.findRegistry(Block.class).getValue(
            new ResourceLocation("cavesnotcliffs", path));
        if (block == null) {
            throw new IllegalStateException("Required ore-decoration block is not registered: "
                + path);
        }
        return block.getDefaultState();
    }
}
