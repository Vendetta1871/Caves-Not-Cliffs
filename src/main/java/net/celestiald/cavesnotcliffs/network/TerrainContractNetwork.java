package net.celestiald.cavesnotcliffs.network;

import net.celestiald.cavesnotcliffs.world.V118ChunkGenerator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Sends the native terrain contract whenever a player's client starts a world backed by the
 * native generator. Every dimension change — including respawning out of the Nether or the End —
 * gives the client a fresh {@code WorldClient} without a resolver, so login alone is not enough.
 */
public final class TerrainContractNetwork {
    private static final SimpleNetworkWrapper CHANNEL =
            NetworkRegistry.INSTANCE.newSimpleChannel("cavesnotcliffs:t");
    private static final TerrainContractNetwork INSTANCE = new TerrainContractNetwork();
    private static boolean initialized;

    private TerrainContractNetwork() {}

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        CHANNEL.registerMessage(TerrainContractMessage.Handler.class,
                TerrainContractMessage.class, 0, Side.CLIENT);
        MinecraftForge.EVENT_BUS.register(INSTANCE);
        initialized = true;
    }

    @SubscribeEvent
    public void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        sendContract(event.player);
    }

    @SubscribeEvent
    public void playerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        sendContract(event.player);
    }

    @SubscribeEvent
    public void playerRespawned(PlayerEvent.PlayerRespawnEvent event) {
        sendContract(event.player);
    }

    // Each event fires after the server has sent the join/respawn packet, so the message is
    // handled after the client created the WorldClient it belongs to.
    private static void sendContract(EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP)) {
            return;
        }
        V118ChunkGenerator generator = V118ChunkGenerator.forWorld(player.world);
        if (generator != null) {
            CHANNEL.sendTo(new TerrainContractMessage(
                    player.world.getSeed(), generator.getTerrainProfile()),
                    (EntityPlayerMP) player);
        }
    }
}
