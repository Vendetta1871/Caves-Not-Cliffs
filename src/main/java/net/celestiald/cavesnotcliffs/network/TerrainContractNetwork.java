package net.celestiald.cavesnotcliffs.network;

import net.celestiald.cavesnotcliffs.world.V118CubicChunksGenerator;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/** Sends the native terrain contract before multiplayer chunk streaming begins. */
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
        if (!(event.player instanceof EntityPlayerMP)) {
            return;
        }
        V118CubicChunksGenerator generator =
                V118CubicChunksGenerator.forWorld(event.player.world);
        if (generator != null) {
            CHANNEL.sendTo(new TerrainContractMessage(
                    event.player.world.getSeed(), generator.getTerrainProfile()),
                    (EntityPlayerMP) event.player);
        }
    }
}
