package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.block.LushAzaleaBlocks;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BeeFlowerPredicateTest {
    private static final BlockPos LOWER = new BlockPos(4, 8, 15);
    private static final BlockPos UPPER = LOWER.up();

    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void allTallFlowerHalvesResolveThroughTheLowerBlock() {
        for (BlockDoublePlant.EnumPlantType type : new BlockDoublePlant.EnumPlantType[]{
                BlockDoublePlant.EnumPlantType.SYRINGA,
                BlockDoublePlant.EnumPlantType.ROSE,
                BlockDoublePlant.EnumPlantType.PAEONIA}) {
            FakeAccess world = doublePlant(type);
            assertTrue(type.name() + " lower", BeeFlowerPredicate.isFlower(world, LOWER));
            assertTrue(type.name() + " upper", BeeFlowerPredicate.isFlower(world, UPPER));
        }
    }

    @Test
    public void sunflowerIsAFlowerOnBothHalvesButOnlyUpperIsPollinated() {
        FakeAccess world = doublePlant(BlockDoublePlant.EnumPlantType.SUNFLOWER);
        assertTrue(BeeFlowerPredicate.isFlower(world, LOWER));
        assertTrue(BeeFlowerPredicate.isFlower(world, UPPER));
        assertFalse(BeeFlowerPredicate.isPollinationTarget(world, LOWER));
        assertTrue(BeeFlowerPredicate.isPollinationTarget(world, UPPER));
    }

    @Test
    public void tallGrassFernAndOrphanUpperHalvesAreRejected() {
        for (BlockDoublePlant.EnumPlantType type : new BlockDoublePlant.EnumPlantType[]{
                BlockDoublePlant.EnumPlantType.GRASS,
                BlockDoublePlant.EnumPlantType.FERN}) {
            FakeAccess world = doublePlant(type);
            assertFalse(BeeFlowerPredicate.isFlower(world, LOWER));
            assertFalse(BeeFlowerPredicate.isFlower(world, UPPER));
            assertFalse(BeeFlowerPredicate.isPollinationTarget(world, LOWER));
            assertFalse(BeeFlowerPredicate.isPollinationTarget(world, UPPER));
        }
        FakeAccess orphan = new FakeAccess();
        orphan.states.put(UPPER, upperState(BlockDoublePlant.EnumPlantType.ROSE));
        assertFalse(BeeFlowerPredicate.isFlower(orphan, UPPER));
    }

    @Test
    public void smallFlowersAndFloweringAzaleaPeersAreIncluded() {
        FakeAccess world = new FakeAccess();
        world.states.put(LOWER, Blocks.RED_FLOWER.getDefaultState());
        assertTrue(BeeFlowerPredicate.isFlower(world, LOWER));

        LushAzaleaBlocks.AzaleaBush flowering =
                (LushAzaleaBlocks.AzaleaBush) new LushAzaleaBlocks.AzaleaBush(true)
                        .setRegistryName("cavesnotcliffs", "flowering_azalea");
        world.states.put(LOWER, flowering.getDefaultState());
        assertTrue(BeeFlowerPredicate.isFlower(world, LOWER));

        LushAzaleaBlocks.AzaleaBush plain =
                (LushAzaleaBlocks.AzaleaBush) new LushAzaleaBlocks.AzaleaBush(false)
                        .setRegistryName("cavesnotcliffs", "azalea");
        world.states.put(LOWER, plain.getDefaultState());
        assertFalse(BeeFlowerPredicate.isFlower(world, LOWER));
    }

    @Test
    public void flowerItemsExcludeDoubleTallGrassAndFern() {
        assertTrue(BeeFlowerPredicate.isFlowerItem(new ItemStack(Blocks.RED_FLOWER)));
        assertTrue(doublePlantItem(BlockDoublePlant.EnumPlantType.SUNFLOWER));
        assertTrue(doublePlantItem(BlockDoublePlant.EnumPlantType.SYRINGA));
        assertTrue(doublePlantItem(BlockDoublePlant.EnumPlantType.ROSE));
        assertTrue(doublePlantItem(BlockDoublePlant.EnumPlantType.PAEONIA));
        assertFalse(doublePlantItem(BlockDoublePlant.EnumPlantType.GRASS));
        assertFalse(doublePlantItem(BlockDoublePlant.EnumPlantType.FERN));
    }

    private static boolean doublePlantItem(BlockDoublePlant.EnumPlantType type) {
        return BeeFlowerPredicate.isFlowerItem(
                new ItemStack(Blocks.DOUBLE_PLANT, 1, type.getMeta()));
    }

    private static FakeAccess doublePlant(BlockDoublePlant.EnumPlantType type) {
        FakeAccess world = new FakeAccess();
        world.states.put(LOWER, lowerState(type));
        world.states.put(UPPER, upperState(type));
        return world;
    }

    private static IBlockState lowerState(BlockDoublePlant.EnumPlantType type) {
        return Blocks.DOUBLE_PLANT.getDefaultState()
                .withProperty(BlockDoublePlant.VARIANT, type)
                .withProperty(BlockDoublePlant.HALF,
                        BlockDoublePlant.EnumBlockHalf.LOWER);
    }

    private static IBlockState upperState(BlockDoublePlant.EnumPlantType type) {
        return Blocks.DOUBLE_PLANT.getDefaultState()
                .withProperty(BlockDoublePlant.VARIANT, type)
                .withProperty(BlockDoublePlant.HALF,
                        BlockDoublePlant.EnumBlockHalf.UPPER);
    }

    private static final class FakeAccess implements IBlockAccess {
        final Map<BlockPos, IBlockState> states = new HashMap<>();

        @Override public TileEntity getTileEntity(BlockPos pos) { return null; }
        @Override public int getCombinedLight(BlockPos pos, int light) { return 0; }
        @Override public IBlockState getBlockState(BlockPos pos) {
            IBlockState state = states.get(pos);
            return state == null ? Blocks.AIR.getDefaultState() : state;
        }
        @Override public boolean isAirBlock(BlockPos pos) {
            return getBlockState(pos).getBlock() == Blocks.AIR;
        }
        @Override public Biome getBiome(BlockPos pos) { return Biome.getBiome(1); }
        @Override public int getStrongPower(BlockPos pos, EnumFacing facing) { return 0; }
        @Override public WorldType getWorldType() { return WorldType.DEFAULT; }
        @Override public boolean isSideSolid(BlockPos pos, EnumFacing side,
                boolean defaultValue) { return defaultValue; }
    }
}
