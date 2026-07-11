package net.celestiald.cavesnotcliffs.stonecutter;

import net.celestiald.cavesnotcliffs.block.BlockStonecutter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Common GUI handler with no dedicated-server linkage to client classes. */
public final class CncGuiHandler implements IGuiHandler {
    public static final int STONECUTTER_GUI = 0;
    private static final String CLIENT_FACTORY =
            "net.celestiald.cavesnotcliffs.client.GuiStonecutter";

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world,
            int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (!isStonecutter(id, world, pos)) {
            return null;
        }
        return new ContainerStonecutter(player.inventory, world, pos);
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world,
            int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (!isStonecutter(id, world, pos)) {
            return null;
        }
        try {
            Class<?> factory = Class.forName(CLIENT_FACTORY);
            Method create = factory.getMethod("create", EntityPlayer.class,
                    World.class, BlockPos.class);
            return create.invoke(null, player, world, pos);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                | InvocationTargetException exception) {
            throw new IllegalStateException("Could not construct the stonecutter screen",
                    exception);
        }
    }

    private static boolean isStonecutter(int id, World world, BlockPos pos) {
        return id == STONECUTTER_GUI
                && world.getBlockState(pos).getBlock() == BlockStonecutter.block;
    }
}
