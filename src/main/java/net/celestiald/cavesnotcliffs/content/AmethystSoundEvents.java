package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.block.AmethystChimeSource;
import net.minecraft.block.SoundType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Java 1.18.2 amethyst sound types and projectile chimes. */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID)
public final class AmethystSoundEvents {
    public static final SoundEvent BLOCK_BREAK = sound("block.amethyst_block.break");
    public static final SoundEvent BLOCK_CHIME = sound("block.amethyst_block.chime");
    public static final SoundEvent BLOCK_FALL = sound("block.amethyst_block.fall");
    public static final SoundEvent BLOCK_HIT = sound("block.amethyst_block.hit");
    public static final SoundEvent BLOCK_PLACE = sound("block.amethyst_block.place");
    public static final SoundEvent BLOCK_STEP = sound("block.amethyst_block.step");

    public static final SoundEvent CLUSTER_BREAK = sound("block.amethyst_cluster.break");
    public static final SoundEvent CLUSTER_FALL = sound("block.amethyst_cluster.fall");
    public static final SoundEvent CLUSTER_HIT = sound("block.amethyst_cluster.hit");
    public static final SoundEvent CLUSTER_PLACE = sound("block.amethyst_cluster.place");
    public static final SoundEvent CLUSTER_STEP = sound("block.amethyst_cluster.step");
    public static final SoundEvent SMALL_BUD_BREAK = sound("block.small_amethyst_bud.break");
    public static final SoundEvent SMALL_BUD_PLACE = sound("block.small_amethyst_bud.place");
    public static final SoundEvent MEDIUM_BUD_BREAK = sound("block.medium_amethyst_bud.break");
    public static final SoundEvent MEDIUM_BUD_PLACE = sound("block.medium_amethyst_bud.place");
    public static final SoundEvent LARGE_BUD_BREAK = sound("block.large_amethyst_bud.break");
    public static final SoundEvent LARGE_BUD_PLACE = sound("block.large_amethyst_bud.place");
    public static final SoundEvent SPYGLASS_USE = sound("item.spyglass.use");
    public static final SoundEvent SPYGLASS_STOP = sound("item.spyglass.stop_using");

    public static final SoundType AMETHYST = new SoundType(1.0F, 1.0F,
            BLOCK_BREAK, BLOCK_STEP, BLOCK_PLACE, BLOCK_HIT, BLOCK_FALL);
    public static final SoundType AMETHYST_CLUSTER = new SoundType(1.0F, 1.0F,
            CLUSTER_BREAK, CLUSTER_STEP, CLUSTER_PLACE, CLUSTER_HIT, CLUSTER_FALL);
    public static final SoundType SMALL_AMETHYST_BUD = new SoundType(1.0F, 1.0F,
            SMALL_BUD_BREAK, CLUSTER_STEP, SMALL_BUD_PLACE, CLUSTER_HIT, CLUSTER_FALL);
    public static final SoundType MEDIUM_AMETHYST_BUD = new SoundType(1.0F, 1.0F,
            MEDIUM_BUD_BREAK, CLUSTER_STEP, MEDIUM_BUD_PLACE, CLUSTER_HIT, CLUSTER_FALL);
    public static final SoundType LARGE_AMETHYST_BUD = new SoundType(1.0F, 1.0F,
            LARGE_BUD_BREAK, CLUSTER_STEP, LARGE_BUD_PLACE, CLUSTER_HIT, CLUSTER_FALL);

    private AmethystSoundEvents() {
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().registerAll(
                BLOCK_BREAK, BLOCK_CHIME, BLOCK_FALL, BLOCK_HIT, BLOCK_PLACE, BLOCK_STEP,
                CLUSTER_BREAK, CLUSTER_FALL, CLUSTER_HIT, CLUSTER_PLACE, CLUSTER_STEP,
                SMALL_BUD_BREAK, SMALL_BUD_PLACE, MEDIUM_BUD_BREAK, MEDIUM_BUD_PLACE,
                LARGE_BUD_BREAK, LARGE_BUD_PLACE,
                SPYGLASS_USE, SPYGLASS_STOP);
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        RayTraceResult hit = event.getRayTraceResult();
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
            return;
        }
        World world = event.getEntity().world;
        BlockPos pos = hit.getBlockPos();
        if (world.isRemote
                || !(world.getBlockState(pos).getBlock() instanceof AmethystChimeSource)) {
            return;
        }

        world.playSound(null, pos, BLOCK_CHIME, SoundCategory.BLOCKS, 1.0F,
                0.5F + world.rand.nextFloat() * 1.2F);
        world.playSound(null, pos, BLOCK_HIT, SoundCategory.BLOCKS, 1.0F,
                0.5F + world.rand.nextFloat() * 1.2F);
    }

    private static SoundEvent sound(String name) {
        ResourceLocation id = new ResourceLocation(CavesNotCliffs.MODID, name);
        return new SoundEvent(id).setRegistryName(id);
    }

    public static SoundType forGrowthStage(String name) {
        if ("small_amethyst_bud".equals(name)) {
            return SMALL_AMETHYST_BUD;
        }
        if ("medium_amethyst_bud".equals(name)) {
            return MEDIUM_AMETHYST_BUD;
        }
        if ("large_amethyst_bud".equals(name)) {
            return LARGE_AMETHYST_BUD;
        }
        return AMETHYST_CLUSTER;
    }
}
