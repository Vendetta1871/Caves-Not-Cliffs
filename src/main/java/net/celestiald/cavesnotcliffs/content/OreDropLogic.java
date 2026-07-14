package net.celestiald.cavesnotcliffs.content;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.List;
import java.util.Random;

/** Java 1.18.2 loot-function equivalents used by ore blocks on the 1.12 loot API. */
public final class OreDropLogic {
    private OreDropLogic() {
    }

    /** Equivalent to the 1.18.2 {@code minecraft:ore_drops} Fortune formula. */
    public static int applyOreBonus(int baseCount, int fortune, Random random) {
        if (fortune <= 0) {
            return baseCount;
        }
        int multiplier = random.nextInt(fortune + 2) - 1;
        return baseCount * (Math.max(0, multiplier) + 1);
    }

    /** Equivalent to {@code uniform_bonus_count} with a bonus multiplier of one. */
    public static int applyUniformBonus(int baseCount, int fortune, Random random) {
        return fortune <= 0 ? baseCount : baseCount + random.nextInt(fortune + 1);
    }

    /** Equivalent to the per-item {@code explosion_decay} loot function. */
    public static int applyExplosionDecay(int count, float survivalChance, Random random) {
        int surviving = 0;
        for (int item = 0; item < count; item++) {
            if (random.nextFloat() <= survivalChance) {
                surviving++;
            }
        }
        return surviving;
    }

    /**
     * Replaces 1.12's one-roll-per-stack explosion path when the caller supplies a real
     * explosion survival chance. A chance of one is left to vanilla so ordinary harvesting does
     * not consume additional world randomness.
     */
    public static boolean dropWithExplosionDecay(Block block, World world, BlockPos pos,
            IBlockState state, float chance, int fortune, EntityPlayer harvester) {
        if (chance >= 1.0F) {
            return false;
        }
        if (world.isRemote || world.restoringBlockSnapshots) {
            return true;
        }

        List<ItemStack> drops = block.getDrops(world, pos, state, fortune);
        float adjustedChance = ForgeEventFactory.fireBlockHarvesting(drops, world, pos, state,
                fortune, chance, false, harvester);
        for (ItemStack drop : drops) {
            int surviving = applyExplosionDecay(drop.getCount(), adjustedChance, world.rand);
            if (surviving > 0) {
                ItemStack result = drop.copy();
                result.setCount(surviving);
                Block.spawnAsEntity(world, pos, result);
            }
        }
        return true;
    }
}
