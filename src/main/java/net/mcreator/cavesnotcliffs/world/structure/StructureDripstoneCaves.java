
package net.mcreator.cavesnotcliffs.world.structure;

import net.mcreator.cavesandcliffs.block.BlockBottomStalactite;
import net.mcreator.cavesandcliffs.block.BlockBottomStalagmite;
import net.mcreator.cavesandcliffs.block.BlockMiddleStalactite;
import net.mcreator.cavesandcliffs.block.BlockMiddleStalagmite;
import net.mcreator.cavesandcliffs.block.BlockTopStalactite;
import net.mcreator.cavesandcliffs.block.BlockTopStalagmite;
import net.mcreator.cavesandcliffs.block.BlockDripstone;

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
import net.minecraft.block.BlockClay;
import net.minecraft.init.Blocks;
import net.minecraft.entity.Entity;

import net.mcreator.cavesnotcliffs.ElementsCavesNotCliffs;

import java.util.Random;
import javax.annotation.Generated;
import sun.security.action.GetLongAction;

@ElementsCavesNotCliffs.ModElement.Tag
public class StructureDripstoneCaves extends ElementsCavesNotCliffs.ModElement {
	public StructureDripstoneCaves(ElementsCavesNotCliffs instance) {
		super(instance, 16);
	}

	private int getDripstoneLength(Random random, World world, int x, int y, int z, int dir) {
		int len = random.nextInt(3) + 1;
		int count = 0;
		while (world.isAirBlock(new BlockPos(x, y, z)) && (count < 4)) {
			count += 1;
			y += dir;
		}
		return Math.min(len, count);
	}

	private void generateStalactite(Random random, World world, int x, int y, int z) {
		int len = getDripstoneLength(random, world, x, y, z, -1);

		if (len == 1) {
			world.setBlockState(new BlockPos(x, y, z), BlockTopStalactite.block.getDefaultState(), 3);
		}
		else if (len == 2) {
			world.setBlockState(new BlockPos(x, y, z), BlockMiddleStalactite.block.getDefaultState(), 3);
			world.setBlockState(new BlockPos(x, y - 1, z), BlockTopStalactite.block.getDefaultState(), 3);
		}
		else {
			world.setBlockState(new BlockPos(x, y, z), BlockBottomStalactite.block.getDefaultState(), 3);
			world.setBlockState(new BlockPos(x, y - 1, z), BlockMiddleStalactite.block.getDefaultState(), 3);
			world.setBlockState(new BlockPos(x, y - 2, z), BlockTopStalactite.block.getDefaultState(), 3);
		}
	}

	private void generateStalagmite(Random random, World world, int x, int y, int z) {
	  int len = getDripstoneLength(random, world, x, y, z, +1);

		if (len == 1) {
			world.setBlockState(new BlockPos(x, y, z), BlockTopStalagmite.block.getDefaultState(), 3);
		}
		else if (len == 2) {
			world.setBlockState(new BlockPos(x, y, z), BlockMiddleStalagmite.block.getDefaultState(), 3);
			world.setBlockState(new BlockPos(x, y + 1, z), BlockTopStalagmite.block.getDefaultState(), 3);
		}
		else {
			world.setBlockState(new BlockPos(x, y, z), BlockBottomStalagmite.block.getDefaultState(), 3);
			world.setBlockState(new BlockPos(x, y + 1, z), BlockMiddleStalagmite.block.getDefaultState(), 3);
			world.setBlockState(new BlockPos(x, y + 2, z), BlockTopStalagmite.block.getDefaultState(), 3);
		}
	}

	private void generateLake(Random random, World world, int x, int y, int z, int depth) {
	    boolean isOnAir = world.isAirBlock(new BlockPos(x, y - 2, z));
		if (world.isAirBlock(new BlockPos(x, y + 1, z)) && !isOnAir) {
			world.setBlockState(new BlockPos(x, y - 1, z), Blocks.CLAY.getDefaultState(), 3);
			world.setBlockState(new BlockPos(x - 1, y, z), Blocks.CLAY.getDefaultState(), 3);
			world.setBlockState(new BlockPos(x + 1, y, z), Blocks.CLAY.getDefaultState(), 3);
			world.setBlockState(new BlockPos(x, y, z - 1), Blocks.CLAY.getDefaultState(), 3);
			world.setBlockState(new BlockPos(x, y, z + 1), Blocks.CLAY.getDefaultState(), 3);
		}
		
		if (depth < 4) {
			int rnd = random.nextInt(4);
			 if (rnd == 0) {
			 	generateLake(random, world, x - 1, y, z, depth + 1);
			 }
			 else if (rnd == 1) {
			 	generateLake(random, world, x + 1, y, z, depth + 1);
			 }
			 else if (rnd == 2) {
			 	generateLake(random, world, x, y, z - 1, depth + 1);
			 }
			 else {
			 	generateLake(random, world, x, y, z + 1, depth + 1);
			 }
		}
		
		if (world.isAirBlock(new BlockPos(x, y + 1, z)) && !isOnAir) {
			world.setBlockState(new BlockPos(x, y, z), Blocks.WATER.getDefaultState(), 3);
		}
	}

	private boolean circleCheck(int dx, int dz, int r) {
		return (dx * dx + dz * dz) > (r * r);
	}

	@Override
	public void generateWorld(Random random, int i2, int k2, World world, int dimID, IChunkGenerator cg, IChunkProvider cp) {
		if (dimID != 0) {
			return;
		}

		if ((random.nextInt(1000) + 1) <= 100) {
			int x0 = i2;
			int x1 = i2 + 15;
			int y0 = 16;
			int y1 = 48;
			int z0 = k2;
			int z1 = k2 + 15;

			int r0 = random.nextInt(12) + 4;
			int r1 = random.nextInt(12) + 4;
			int r2 = random.nextInt(12) + 4;

			int ox0 = random.nextInt(16) + i2;
			int oz0 = random.nextInt(16) + k2;
			int ox1 = random.nextInt(16) + i2;
			int oz1 = random.nextInt(16) + k2;
			int ox2 = random.nextInt(16) + i2;
			int oz2 = random.nextInt(16) + k2;

			
			for (int x = x0; x < x1; ++x) for (int y = y0; y < y1; ++y) for (int z = z0; z < z1; ++z) {
				if (circleCheck(x - ox0, z - oz0, r0) && circleCheck(x - ox1, z - oz1, r1) && circleCheck(x - ox2, z - oz2, r2)) {
					continue;
				}
				if (((world.getBlockState(new BlockPos(x, y, z))).getBlock() != Blocks.STONE.getDefaultState().getBlock())
				 && ((world.getBlockState(new BlockPos(x, y, z))).getBlock() != Blocks.STONE.getStateFromMeta(1).getBlock())
				 && ((world.getBlockState(new BlockPos(x, y, z))).getBlock() != Blocks.STONE.getStateFromMeta(2).getBlock())
				 && ((world.getBlockState(new BlockPos(x, y, z))).getBlock() != Blocks.STONE.getStateFromMeta(3).getBlock())) {
					continue;
				}
				BlockPos bp = new BlockPos(x, y, z);
				if (world.isAirBlock(new BlockPos(x, y - 1, z))) { // roof
					if ((random.nextInt(1000) + 1) <= 900) {
						world.setBlockState(bp, BlockDripstone.block.getDefaultState(), 3);
						if ((random.nextInt(1000) + 1) <= 400) {
							generateStalactite(random, world, x, y - 1, z);
						}
					}
					
				}
				else if (world.isAirBlock(new BlockPos(x, y + 1, z))) { // bottom
					if ((random.nextInt(1000) + 1) <= 850) {
						world.setBlockState(bp, BlockDripstone.block.getDefaultState(), 3);
						if ((random.nextInt(1000) + 1) <= 400) {
							generateStalagmite(random, world, x, y + 1, z);
						}
					}
				}
				else if (world.isAirBlock(new BlockPos(x - 1, y, z)) 
				|| world.isAirBlock(new BlockPos(x + 1, y, z))
				|| world.isAirBlock(new BlockPos(x, y, z - 1))
				|| world.isAirBlock(new BlockPos(x, y, z + 1))) { // side
					if ((random.nextInt(1000) + 1) <= 600) {
						world.setBlockState(bp, BlockDripstone.block.getDefaultState(), 3);
					}
				}			
			}
		}
	
	}
}
