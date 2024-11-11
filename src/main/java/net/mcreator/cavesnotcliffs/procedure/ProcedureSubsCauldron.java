package net.mcreator.cavesnotcliffs.procedure;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.common.MinecraftForge;

import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumHand;
import net.minecraft.init.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.block.state.IBlockState;

import net.mcreator.cavesnotcliffs.block.BlockLavaCauldron;
import net.mcreator.cavesnotcliffs.ElementsCavesNotCliffs;

@ElementsCavesNotCliffs.ModElement.Tag
public class ProcedureSubsCauldron extends ElementsCavesNotCliffs.ModElement {
	public ProcedureSubsCauldron(ElementsCavesNotCliffs instance) {
		super(instance, 14);
	}

	public static void executeProcedure(java.util.HashMap<String, Object> dependencies) {
		if (dependencies.get("x") == null) {
			System.err.println("Failed to load dependency x for procedure SubsCauldron!");
			return;
		}
		if (dependencies.get("y") == null) {
			System.err.println("Failed to load dependency y for procedure SubsCauldron!");
			return;
		}
		if (dependencies.get("z") == null) {
			System.err.println("Failed to load dependency z for procedure SubsCauldron!");
			return;
		}
		if (dependencies.get("world") == null) {
			System.err.println("Failed to load dependency world for procedure SubsCauldron!");
			return;
		}
		int x = (int) dependencies.get("x");
		int y = (int) dependencies.get("y");
		int z = (int) dependencies.get("z");
		World world = (World) dependencies.get("world");
		if (((world.getBlockState(new BlockPos((int) x, (int) y, (int) z))).getBlock() == Blocks.CAULDRON.getDefaultState().getBlock())) {
			{
				BlockPos _bp = new BlockPos((int) x, (int) y, (int) z);
				IBlockState _bs = BlockLavaCauldron.block.getDefaultState();
				world.setBlockState(_bp, _bs, 3);
			}
		}
	}

	@SubscribeEvent
	public void onBlockPlace(BlockEvent.PlaceEvent event) {
		Entity entity = event.getPlayer();
		java.util.HashMap<String, Object> dependencies = new java.util.HashMap<>();
		dependencies.put("ismainhand", event.getHand() == EnumHand.MAIN_HAND);
		dependencies.put("x", (int) event.getPos().getX());
		dependencies.put("y", (int) event.getPos().getY());
		dependencies.put("z", (int) event.getPos().getZ());
		dependencies.put("px", (int) entity.posX);
		dependencies.put("py", (int) entity.posY);
		dependencies.put("pz", (int) entity.posZ);
		dependencies.put("world", event.getWorld());
		dependencies.put("entity", entity);
		dependencies.put("event", event);
		this.executeProcedure(dependencies);
	}

	@Override
	public void preInit(FMLPreInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(this);
	}
}
