package net.celestiald.cavesnotcliffs;

import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraft.world.World;

import java.util.function.Consumer;

public interface IProxyCavesNotCliffs {
	void preInit(FMLPreInitializationEvent event);

	void init(FMLInitializationEvent event);

	void postInit(FMLPostInitializationEvent event);

	void serverLoad(FMLServerStartingEvent event);

	/** Schedule work that needs the physical client's current world. */
	void scheduleClientWorldTask(Consumer<World> task);
}
