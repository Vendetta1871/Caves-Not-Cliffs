package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.block.AmethystWaterlogging;
import net.celestiald.cavesnotcliffs.block.BlockAmethystGrowth;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118GeodeFeature;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;

/** Runtime state translation for the dependency-free Java 1.18.2 geode port. */
final class V118GeodeBlockMapper {
    private final IBlockState[] plainStates;
    private final BlockAmethystGrowth[] dryGrowth;
    private final BlockAmethystGrowth[] waterloggedGrowth;

    static V118GeodeBlockMapper fromRegisteredBlocks() {
        int count = V118GeodeFeature.Material.values().length;
        IBlockState[] plain = new IBlockState[count];
        BlockAmethystGrowth[] dry = new BlockAmethystGrowth[count];
        BlockAmethystGrowth[] waterlogged = new BlockAmethystGrowth[count];
        plain[V118GeodeFeature.Material.AIR.ordinal()] = Blocks.AIR.getDefaultState();
        plain[V118GeodeFeature.Material.AMETHYST_BLOCK.ordinal()] =
            registered("amethyst_block").getDefaultState();
        plain[V118GeodeFeature.Material.BUDDING_AMETHYST.ordinal()] =
            registered("budding_amethyst").getDefaultState();
        plain[V118GeodeFeature.Material.CALCITE.ordinal()] =
            registered("calcite").getDefaultState();
        plain[V118GeodeFeature.Material.SMOOTH_BASALT.ordinal()] =
            registered("smooth_basalt").getDefaultState();
        for (V118GeodeFeature.Material material : V118GeodeFeature.Material.values()) {
            if (!material.hasFacing()) {
                continue;
            }
            String path = material.name().toLowerCase(java.util.Locale.ROOT);
            dry[material.ordinal()] = growth(path);
            waterlogged[material.ordinal()] = growth(
                AmethystWaterlogging.companionPath(path));
        }
        return new V118GeodeBlockMapper(plain, dry, waterlogged);
    }

    V118GeodeBlockMapper(IBlockState[] plainStates, BlockAmethystGrowth[] dryGrowth,
            BlockAmethystGrowth[] waterloggedGrowth) {
        int count = V118GeodeFeature.Material.values().length;
        if (plainStates == null || dryGrowth == null || waterloggedGrowth == null
                || plainStates.length != count || dryGrowth.length != count
                || waterloggedGrowth.length != count) {
            throw new IllegalArgumentException("Expected one state slot per geode material");
        }
        this.plainStates = plainStates.clone();
        this.dryGrowth = dryGrowth.clone();
        this.waterloggedGrowth = waterloggedGrowth.clone();
        for (V118GeodeFeature.Material material : V118GeodeFeature.Material.values()) {
            if (material.hasFacing()) {
                if (this.dryGrowth[material.ordinal()] == null
                        || this.waterloggedGrowth[material.ordinal()] == null) {
                    throw new IllegalArgumentException("Missing growth state for " + material);
                }
            } else if (this.plainStates[material.ordinal()] == null) {
                throw new IllegalArgumentException("Missing plain state for " + material);
            }
        }
    }

    IBlockState stateFor(V118GeodeFeature.State state) {
        if (state == null) {
            throw new NullPointerException("state");
        }
        V118GeodeFeature.Material material = state.material();
        if (!material.hasFacing()) {
            return plainStates[material.ordinal()];
        }
        BlockAmethystGrowth block = state.waterlogged()
            ? waterloggedGrowth[material.ordinal()] : dryGrowth[material.ordinal()];
        EnumFacing facing = EnumFacing.valueOf(state.facing().name());
        return block.getDefaultState()
            .withProperty(BlockAmethystGrowth.FACING, facing)
            .withProperty(BlockAmethystGrowth.WATERLOGGED, state.waterlogged());
    }

    private static BlockAmethystGrowth growth(String path) {
        Block block = registered(path);
        if (!(block instanceof BlockAmethystGrowth)) {
            throw new IllegalStateException("Required geode growth block has the wrong type: "
                + path + " -> " + block.getClass().getName());
        }
        return (BlockAmethystGrowth) block;
    }

    private static Block registered(String path) {
        Block block = GameRegistry.findRegistry(Block.class).getValue(
            new ResourceLocation("cavesnotcliffs", path));
        if (block == null) {
            throw new IllegalStateException("Required geode block is not registered: " + path);
        }
        return block;
    }
}
