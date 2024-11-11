
package net.mcreator.cavesnotcliffs.world.structure;

import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.World;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Rotation;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Mirror;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.Block;

import net.mcreator.cavesnotcliffs.ElementsCavesNotCliffs;

import java.util.Random;


@ElementsCavesNotCliffs.ModElement.Tag
public class StructureAmethystGeode0 extends ElementsCavesNotCliffs.ModElement {
	public StructureAmethystGeode0(ElementsCavesNotCliffs instance) {
		super(instance, 1);
	}

	protected boolean isInsideRock(World world, int x, int y, int z) {
		int count = 0;
		for (int i = x - 5; i < x + 5; ++i) for (int j = y - 5; j < y + 5; ++j) for (int k = z - 5; k < z + 5; ++k) {
			if (world.isAirBlock(new BlockPos(i, j, k))) {
				count += 1;
			}
		}
		return (count / 1000F) <= 0.1F; 
	}

	@Override
	public void generateWorld(Random random, int i2, int k2, World world, int dimID, IChunkGenerator cg, IChunkProvider cp) {
		boolean dimensionCriteria = false;
		boolean isNetherType = false;
		if (dimID == 0)
			dimensionCriteria = true;
		if (!dimensionCriteria)
			return;
		if ((random.nextInt(1000) + 1) <= 50) {
			int count = random.nextInt(5) / 3 + 1;
			for (int a = 0; a < count; a++) {
				int i = i2 + random.nextInt(16) + 8;
				int k = k2 + random.nextInt(16) + 8;
				int height = 255;
				if (isNetherType) {
					boolean notpassed = true;
					while (height > 0) {
						if (notpassed && (world.isAirBlock(new BlockPos(i, height, k)) || !world.getBlockState(new BlockPos(i, height, k)).getBlock()
								.getMaterial(world.getBlockState(new BlockPos(i, height, k))).blocksMovement()))
							notpassed = false;
						else if (!notpassed && !world.isAirBlock(new BlockPos(i, height, k)) && world.getBlockState(new BlockPos(i, height, k))
								.getBlock().getMaterial(world.getBlockState(new BlockPos(i, height, k))).blocksMovement())
							break;
						height--;
					}
				} else {
					while (height > 0) {
						if (!world.isAirBlock(new BlockPos(i, height, k)) && world.getBlockState(new BlockPos(i, height, k)).getBlock()
								.getMaterial(world.getBlockState(new BlockPos(i, height, k))).blocksMovement())
							break;
						height--;
					}
				}
				int j = Math.abs(random.nextInt(Math.max(1, height)) - 24);
				if (world.isRemote || !isInsideRock(world, i, j + 5, k)) {
					continue;
				}

				int rnd = random.nextInt(1000);
				String templateName = "amethyst_geode0";
				if (rnd < 200) {
					templateName = "amethyst_geode1";
				}
				else if (rnd < 400) {
					templateName = "amethyst_geode2";
				}
				else if (rnd < 600) {
					templateName = "amethyst_geode3";
				}
				else if (rnd < 800) {
					templateName = "amethyst_geode4";
				}
				
				Template template = ((WorldServer) world).getStructureTemplateManager().getTemplate(world.getMinecraftServer(),
						new ResourceLocation("cavesnotcliffs", templateName));
				if (template == null)
					return;
				Rotation rotation = Rotation.values()[random.nextInt(3)];
				Mirror mirror = Mirror.values()[random.nextInt(2)];
				BlockPos spawnTo = new BlockPos(i, j + 5, k);
				IBlockState iblockstate = world.getBlockState(spawnTo);
				world.notifyBlockUpdate(spawnTo, iblockstate, iblockstate, 3);
				template.addBlocksToWorldChunk(world, spawnTo, new PlacementSettings().setRotation(rotation).setMirror(mirror)
						.setChunk((ChunkPos) null).setReplacedBlock((Block) null).setIgnoreStructureBlock(false).setIgnoreEntities(false));
			}
		}
	}
}
