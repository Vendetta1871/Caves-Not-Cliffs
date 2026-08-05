package net.celestiald.cavesnotcliffs.compat;

import git.jbredwards.fluidlogged_api.api.util.FluidState;
import git.jbredwards.fluidlogged_api.api.util.FluidloggedUtils;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;

/** Direct Fluidlogged API calls kept behind the optional-mod boundary. */
final class FluidloggedApiBridge {
    private FluidloggedApiBridge() {
    }

    @Optional.Method(modid = "fluidlogged_api")
    static boolean hasFluid(IBlockAccess world, BlockPos pos) {
        return !FluidloggedUtils.getFluidState(world, pos).isEmpty();
    }

    @Optional.Method(modid = "fluidlogged_api")
    static boolean hasWater(IBlockAccess world, BlockPos pos) {
        FluidState fluid = FluidloggedUtils.getFluidState(world, pos);
        return !fluid.isEmpty() && fluid.getMaterial() == Material.WATER;
    }

    @Optional.Method(modid = "fluidlogged_api")
    static boolean storeWater(World world, BlockPos pos, IBlockState state,
            IBlockState existing, int blockFlags) {
        FluidState fluid = existing != null && existing.getMaterial() == Material.WATER
                ? FluidState.of(existing) : FluidloggedUtils.getFluidState(world, pos);
        if (fluid.isEmpty() || fluid.getMaterial() != Material.WATER) {
            return false;
        }
        boolean changed = FluidloggedUtils.setFluidState(world, pos, state, fluid,
                false, blockFlags);
        FluidloggedUtils.notifyFluids(world, pos, null, true);
        return changed;
    }

    @Optional.Method(modid = "fluidlogged_api")
    static void notifyFluids(World world, BlockPos pos, boolean notifyHere,
            EnumFacing... except) {
        FluidloggedUtils.notifyFluids(world, pos, null, notifyHere, except);
    }

    @Optional.Method(modid = "fluidlogged_api")
    static boolean restoreFluid(World world, BlockPos pos, int blockFlags) {
        FluidState fluid = FluidloggedUtils.getFluidState(world, pos);
        if (fluid.isEmpty()) {
            return false;
        }
        return world.setBlockState(pos, fluid.getState(), blockFlags | 32);
    }
}
