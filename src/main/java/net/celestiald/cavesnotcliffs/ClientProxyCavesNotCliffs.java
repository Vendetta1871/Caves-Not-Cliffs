package net.celestiald.cavesnotcliffs;

import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.celestiald.cavesnotcliffs.client.RenderAxolotl;
import net.celestiald.cavesnotcliffs.entity.EntityAxolotl;
import net.celestiald.cavesnotcliffs.entity.EntityFallingPointedDripstone;
import net.minecraft.client.renderer.entity.RenderFallingBlock;
import net.celestiald.cavesnotcliffs.client.RenderBee;
import net.celestiald.cavesnotcliffs.client.BeeLoopSoundController;
import net.celestiald.cavesnotcliffs.entity.EntityBee;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxyCavesNotCliffs implements IProxyCavesNotCliffs {
	@Override
	public void init(FMLInitializationEvent event) {
	}

	@Override
	public void preInit(FMLPreInitializationEvent event) {
		OBJLoader.INSTANCE.addDomain("cavesnotcliffs");
		RenderingRegistry.registerEntityRenderingHandler(EntityAxolotl.EntityCustom.class,
				RenderAxolotl::new);
		RenderingRegistry.registerEntityRenderingHandler(
				EntityFallingPointedDripstone.EntityCustom.class, RenderFallingBlock::new);
		RenderingRegistry.registerEntityRenderingHandler(EntityBee.EntityCustom.class,
				RenderBee::new);
		MinecraftForge.EVENT_BUS.register(BeeLoopSoundController.INSTANCE);
	}

	@Override
	public void postInit(FMLPostInitializationEvent event) {
	}

	@Override
	public void serverLoad(FMLServerStartingEvent event) {
	}
}
