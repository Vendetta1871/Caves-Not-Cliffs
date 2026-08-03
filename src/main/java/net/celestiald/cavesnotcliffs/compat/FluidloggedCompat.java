package net.celestiald.cavesnotcliffs.compat;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;

/** Optional Fluidlogged API entry point; this class never links the API directly. */
public final class FluidloggedCompat {
    private static final String MODID = "fluidlogged_api";

    private FluidloggedCompat() {
    }

    public static boolean isLoaded() {
        return Loader.isModLoaded(MODID);
    }

    public static boolean hasFluid(IBlockAccess world, BlockPos pos) {
        return isLoaded() && FluidloggedApiBridge.hasFluid(world, pos);
    }

    public static boolean hasWater(IBlockAccess world, BlockPos pos) {
        return isLoaded() && FluidloggedApiBridge.hasWater(world, pos);
    }

    public static boolean storeWater(World world, BlockPos pos, IBlockState state,
            IBlockState existing, int blockFlags) {
        return isLoaded() && FluidloggedApiBridge.storeWater(world, pos, state, existing,
                blockFlags);
    }

    public static boolean storeWater(World world, BlockPos pos, IBlockState state,
            int blockFlags) {
        return storeWater(world, pos, state, null, blockFlags);
    }

    public static void notifyFluids(World world, BlockPos pos, boolean notifyHere,
            EnumFacing... except) {
        if (isLoaded()) {
            FluidloggedApiBridge.notifyFluids(world, pos, notifyHere, except);
        }
    }

    public static boolean restoreFluid(World world, BlockPos pos, int blockFlags) {
        return isLoaded() && FluidloggedApiBridge.restoreFluid(world, pos, blockFlags);
    }
}
