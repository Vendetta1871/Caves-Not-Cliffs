package net.celestiald.cavesnotcliffs.network;

import io.netty.buffer.ByteBuf;
import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.world.TerrainProfile;
import net.celestiald.cavesnotcliffs.world.VirtualBiomeResolverRegistry;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Seed/profile contract used to reproduce the virtual 3D biome volume on clients. */
public final class TerrainContractMessage implements IMessage {
    private long seed;
    private String profileName;

    public TerrainContractMessage() {}

    TerrainContractMessage(long seed, TerrainProfile profile) {
        this.seed = seed;
        this.profileName = profile.getSerializedName();
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        seed = buffer.readLong();
        profileName = ByteBufUtils.readUTF8String(buffer);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeLong(seed);
        ByteBufUtils.writeUTF8String(buffer, profileName);
    }

    long getSeed() {
        return seed;
    }

    TerrainProfile getProfile() {
        return TerrainProfile.bySerializedName(profileName);
    }

    public static final class Handler implements IMessageHandler<TerrainContractMessage, IMessage> {
        @Override
        public IMessage onMessage(final TerrainContractMessage message, MessageContext context) {
            CavesNotCliffs.proxy.scheduleClientWorldTask(world ->
                    VirtualBiomeResolverRegistry.install(
                            world, message.getSeed(), message.getProfile()));
            return null;
        }
    }
}
