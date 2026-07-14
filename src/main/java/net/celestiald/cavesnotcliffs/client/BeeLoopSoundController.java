package net.celestiald.cavesnotcliffs.client;

import net.celestiald.cavesnotcliffs.entity.EntityBee;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Starts one continuous moving loop per client bee and swaps it on anger changes. */
@SideOnly(Side.CLIENT)
public final class BeeLoopSoundController {
    public static final BeeLoopSoundController INSTANCE = new BeeLoopSoundController();
    private final Map<Integer, TrackedLoop> loops = new HashMap<>();

    private BeeLoopSoundController() {
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent event) {
        if (!event.getWorld().isRemote
                || !(event.getEntity() instanceof EntityBee.EntityCustom)) {
            return;
        }
        EntityBee.EntityCustom bee = (EntityBee.EntityCustom) event.getEntity();
        stop(loops.remove(bee.getEntityId()));
        loops.put(bee.getEntityId(), start(bee));
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.world == null) {
            clear();
            return;
        }
        Iterator<Map.Entry<Integer, TrackedLoop>> iterator =
                loops.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, TrackedLoop> entry = iterator.next();
            TrackedLoop tracked = entry.getValue();
            EntityBee.EntityCustom bee = tracked.bee;
            if (bee.isDead || bee.world != minecraft.world) {
                stop(tracked);
                iterator.remove();
            } else if (tracked.sound.isAggressiveLoop() != bee.isAngry()) {
                stop(tracked);
                entry.setValue(start(bee));
            }
        }
    }

    private static TrackedLoop start(EntityBee.EntityCustom bee) {
        MovingSoundBee sound = new MovingSoundBee(bee, bee.isAngry());
        Minecraft.getMinecraft().getSoundHandler().playSound(sound);
        return new TrackedLoop(bee, sound);
    }

    private static void stop(TrackedLoop tracked) {
        if (tracked != null) {
            tracked.sound.stopPlaying();
            Minecraft.getMinecraft().getSoundHandler().stopSound(tracked.sound);
        }
    }

    private void clear() {
        for (TrackedLoop tracked : loops.values()) {
            stop(tracked);
        }
        loops.clear();
    }

    private static final class TrackedLoop {
        final EntityBee.EntityCustom bee;
        final MovingSoundBee sound;

        TrackedLoop(EntityBee.EntityCustom bee, MovingSoundBee sound) {
            this.bee = bee;
            this.sound = sound;
        }
    }
}
