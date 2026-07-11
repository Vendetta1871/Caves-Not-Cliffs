package net.celestiald.cavesnotcliffs;

import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraft.world.World;

import java.util.function.Consumer;

public class ServerProxyCavesNotCliffs implements IProxyCavesNotCliffs {
	@Override
	public void preInit(FMLPreInitializationEvent event) {
	}

	@Override
	public void init(FMLInitializationEvent event) {
	}

	@Override
	public void postInit(FMLPostInitializationEvent event) {
	}

	@Override
	public void serverLoad(FMLServerStartingEvent event) {
	}

	@Override
	public void scheduleClientWorldTask(Consumer<World> task) {
		throw new IllegalStateException("Client-world work was requested on a dedicated server");
	}
}
