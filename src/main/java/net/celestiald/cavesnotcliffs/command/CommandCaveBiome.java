package net.celestiald.cavesnotcliffs.command;

import net.celestiald.cavesnotcliffs.world.CaveBiomeSampler;
import net.celestiald.cavesnotcliffs.world.CavesNotCliffsWorldType;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

/** Small acceptance-test command for the otherwise non-visual 1.12 cave-biome identity. */
public final class CommandCaveBiome extends CommandBase {
    @Override
    public String getName() {
        return "cncbiome";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/cncbiome [x y z]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] arguments)
            throws CommandException {
        World world = sender.getEntityWorld();
        BlockPos origin = sender.getPosition();
        BlockPos position = arguments.length == 0 ? origin : parseBlockPos(sender, arguments, 0, false);
        CaveBiomeSampler.Type type = CaveBiomeSampler.sample(world.getSeed(),
                position.getX(), position.getY(), position.getZ());

        String suffix = CavesNotCliffsWorldType.isCavesNotCliffs(world)
                ? "" : " (sampler preview; this is not a Caves Not Cliffs v2 world)";
        sender.sendMessage(new TextComponentString(type.getDisplayName() + " at "
                + position.getX() + ", " + position.getY() + ", " + position.getZ() + suffix));
    }
}
