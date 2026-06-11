
package net.celestiald.cavesnotcliffs.world.structure;

import net.celestiald.cavesnotcliffs.block.BlockMoss;
import net.celestiald.cavesnotcliffs.block.BlockMossLayer;
import net.celestiald.cavesnotcliffs.block.BlockGlowBerryVines;
import net.celestiald.cavesnotcliffs.block.BlockGlowBerryMiddleFill;
import net.celestiald.cavesnotcliffs.block.BlockSporeBlossom;
import net.celestiald.cavesnotcliffs.block.BlockBabyAzaleaTree;
import net.celestiald.cavesnotcliffs.block.BlockBloomingBabyAzaleaTree;
import net.celestiald.cavesnotcliffs.block.BlockBabyDripleaf;
import net.celestiald.cavesnotcliffs.block.BlockDripleafPlant;
import net.celestiald.cavesnotcliffs.block.BlockDripleafStem;

import net.celestiald.cavesnotcliffs.entity.EntityAxolotl;

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

import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;

import java.util.Random;
import javax.annotation.Generated;

@ElementsCavesNotCliffs.ModElement.Tag
public class StructureLushCaves extends ElementsCavesNotCliffs.ModElement {
	public StructureLushCaves(ElementsCavesNotCliffs instance) {
		super(instance, 15);
	}

	private void generateBerry(Random random, World world, int x, int y, int z) {
		int len = 5;
		for (int i = random.nextInt(16) + 1; i > 0; i /= 2) {
			len -= 1;
		}
		
		BlockPos bp = new BlockPos(x, y, z);
		for (int i = 0; (i < len) && world.isAirBlock(bp); ++i) {
			if ((random.nextInt(1000) + 1) <= 600) {
				world.setBlockState(bp, BlockGlowBerryVines.block.getDefaultState(), 3);
			}
			else {
				world.setBlockState(bp, BlockGlowBerryMiddleFill.block.getDefaultState(), 3);
			}
			bp = new BlockPos(x, bp.getY() - 1, z);
		}
	}

	private void generateDripleaf(Random random, World world, int x, int y, int z) {
		if(!world.isAirBlock(new BlockPos(x, y, z))) {
			return;
		}
		if ((random.nextInt(5) == 1)) {
			world.setBlockState(new BlockPos(x, y, z), BlockBabyDripleaf.block.getDefaultState(), 3);
			return;
		}
		
		int len = 3;
		for (int i = random.nextInt(4) + 1; i > 0; i /= 2) {
			len -= 1;
		}
		
		world.setBlockState(new BlockPos(x, y, z), BlockDripleafPlant.block.getDefaultState(), 3);
		for (int i = 1; (i < len) && world.isAirBlock(new BlockPos(x, y + i, z)); ++i) {
				world.setBlockState(new BlockPos(x, y + i - 1, z), BlockDripleafStem.block.getDefaultState(), 3);
				world.setBlockState(new BlockPos(x, y + i, z), BlockDripleafPlant.block.getDefaultState(), 3);			
		}
	}

	private void generateLake(Random random, World world, int x, int y, int z, int depth, int cx, int cz) {
		if (x < cx || x >= cx + 16 || z < cz || z >= cz + 16) return;
	    boolean isOnAir = world.isAirBlock(new BlockPos(x, y - 2, z));
		if (world.isAirBlock(new BlockPos(x, y + 1, z)) && !isOnAir) {
			world.setBlockState(new BlockPos(x, y - 1, z), Blocks.CLAY.getDefaultState(), 3);
			world.setBlockState(new BlockPos(x - 1, y, z), Blocks.CLAY.getDefaultState(), 3);
			world.setBlockState(new BlockPos(x + 1, y, z), Blocks.CLAY.getDefaultState(), 3);
			world.setBlockState(new BlockPos(x, y, z - 1), Blocks.CLAY.getDefaultState(), 3);
			world.setBlockState(new BlockPos(x, y, z + 1), Blocks.CLAY.getDefaultState(), 3);
			generateDripleaf(random, world, x + 1 - 2 * random.nextInt(2), y + 1, z + 1 - 2 * random.nextInt(2));
		}

		if (depth < 4) {
			int rnd = random.nextInt(4);
			 if (rnd == 0) {
			 	generateLake(random, world, x - 1, y, z, depth + 1, cx, cz);
			 }
			 else if (rnd == 1) {
			 	generateLake(random, world, x + 1, y, z, depth + 1, cx, cz);
			 }
			 else if (rnd == 2) {
			 	generateLake(random, world, x, y, z - 1, depth + 1, cx, cz);
			 }
			 else {
			 	generateLake(random, world, x, y, z + 1, depth + 1, cx, cz);
			 }
		}

		if (world.isAirBlock(new BlockPos(x, y + 1, z)) && !isOnAir) {
			world.setBlockState(new BlockPos(x, y, z), Blocks.WATER.getDefaultState(), 3);
			if (random.nextInt(5) == 1) {
				Entity entityToSpawn = new EntityAxolotl.EntityCustom(world);
				if (entityToSpawn != null) {
					entityToSpawn.setLocationAndAngles(x, y + 1, z, world.rand.nextFloat() * 360F, 0.0F);
					world.spawnEntity(entityToSpawn);
			  }
			}
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
			int x0 = i2 + 1;
			int x1 = i2 + 15;
			int y0 = 16;
			int y1 = 48;
			int z0 = k2 + 1;
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
					boolean isMoss = false;
					if ((random.nextInt(1000) + 1) <= 900) {
						world.setBlockState(bp, BlockMoss.block.getDefaultState(), 3);
						isMoss = true;
					}
					if ((random.nextInt(1000) + 1) <= 400) {
						generateBerry(random, world, x, y - 1, z);
					}
					else if ((random.nextInt(1000) + 1 <= 50) && isMoss) {
						world.setBlockState(new BlockPos(x, y - 1, z), BlockSporeBlossom.block.getDefaultState(), 3);
					}
				}
				else if (world.isAirBlock(new BlockPos(x, y + 1, z))) { // bottom
					boolean isMoss = false;
					if ((random.nextInt(1000) + 1) <= 850) {
						world.setBlockState(bp, BlockMoss.block.getDefaultState(), 3);
						isMoss = true;
					}
					if ((random.nextInt(1000) + 1) <= 100) {
						world.setBlockState(new BlockPos(x, y + 1, z), BlockMossLayer.block.getDefaultState(), 3);
					}
					if (((random.nextInt(1000) + 1) <= 30) && isMoss) {
					    if (random.nextInt(3) == 1) {
					       	world.setBlockState(new BlockPos(x, y + 1, z), BlockBabyAzaleaTree.block.getDefaultState(), 3);
					   	}
					   	else {
					   		world.setBlockState(new BlockPos(x, y + 1, z), BlockBloomingBabyAzaleaTree.block.getDefaultState(), 3);
				    	}
					}
					if ((random.nextInt(1000) + 1) <= 5) {
						generateLake(random, world, x, y, z, 0, i2, k2);
					}
				}
				else if (world.isAirBlock(new BlockPos(x - 1, y, z))) { // side
					if ((random.nextInt(1000) + 1) <= 750) {
						world.setBlockState(bp, BlockMoss.block.getDefaultState(), 3);
					}
				}
				else if (world.isAirBlock(new BlockPos(x + 1, y, z))) { // side
					if ((random.nextInt(1000) + 1) <= 750) {
						world.setBlockState(bp, BlockMoss.block.getDefaultState(), 3);
					}
				}
				else if (world.isAirBlock(new BlockPos(x, y, z - 1))) { // side
					if ((random.nextInt(1000) + 1) <= 750) {
						world.setBlockState(bp, BlockMoss.block.getDefaultState(), 3);
					}
				}
				else if (world.isAirBlock(new BlockPos(x, y, z + 1))) { // side
					if ((random.nextInt(1000) + 1) <= 750) {
						world.setBlockState(bp, BlockMoss.block.getDefaultState(), 3);
					}
				}			
			}
		}
	
	}
}
