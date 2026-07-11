
package net.celestiald.cavesnotcliffs.world.structure;

import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.block.BlockAmethystCrystal;
import net.celestiald.cavesnotcliffs.block.BlockAmethystCrystalStage1;
import net.celestiald.cavesnotcliffs.block.BlockAmethystCrystalStage2;

import java.util.Random;

@ElementsCavesNotCliffs.ModElement.Tag
public class StructureAmethystGeode0 extends ElementsCavesNotCliffs.ModElement {
	public StructureAmethystGeode0(ElementsCavesNotCliffs instance) {
		super(instance, 1);
	}

	private boolean isInsideRock(World world, int x, int y, int z) {
		int count = 0;
		for (int i = x - 5; i < x + 5; ++i)
			for (int j = y - 5; j < y + 5; ++j)
				for (int k = z - 5; k < z + 5; ++k)
					if (world.isAirBlock(new BlockPos(i, j, k)))
						count++;
		return (count / 1000f) <= 0.1f;
	}

	// Deterministic noise: returns -1..1 based on position
	private double noiseAt(int x, int y, int z) {
		return (Math.sin(x * 1.37 + z * 0.83 + y * 0.41)
			  + Math.sin(y * 1.19 + x * 0.67 + z * 1.07)
			  + Math.sin(z * 0.93 + y * 1.47 + x * 0.59)) / 3.0;
	}

	/** Keep geode carving out of modded machines, chests, and other player-built blocks. */
	private boolean canReplace(World world, BlockPos pos) {
		if (world.getTileEntity(pos) != null) return false;
		Block block = world.getBlockState(pos).getBlock();
		if (block == Blocks.STONE || block == Blocks.DIRT || block == Blocks.GRAVEL
				|| block == Blocks.COAL_ORE || block == Blocks.IRON_ORE || block == Blocks.GOLD_ORE
				|| block == Blocks.REDSTONE_ORE || block == Blocks.LAPIS_ORE
				|| block == Blocks.DIAMOND_ORE || block == Blocks.EMERALD_ORE) {
			return true;
		}
		ResourceLocation name = block.getRegistryName();
		if (name == null || !"cavesnotcliffs".equals(name.getResourceDomain())) return false;
		String path = name.getResourcePath();
		return "unnamed_stone".equals(path) || "dark_stone".equals(path);
	}

	private void generateGeode(World world, Random random, int cx, int cy, int cz) {
		Block casingBlock  = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("cavesnotcliffs", "geode_casing"));
		Block geodeBlock   = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("cavesnotcliffs", "amethyst_geode"));
		Block calciteBlock = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("cavesnotcliffs", "unknown_stone"));
		Block stage1Block  = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("cavesnotcliffs", "amethyst_crystal_stage_1"));
		Block stage2Block  = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("cavesnotcliffs", "amethyst_crystal_stage_2"));
		Block crystalBlock = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("cavesnotcliffs", "amethyst_crystal"));
		if (casingBlock == null || geodeBlock == null) return;

		int radius = 6 + random.nextInt(4); // 6–9
		double noiseAmp = 0.9;
		int ext = radius + 2;

		IBlockState casingState  = casingBlock.getDefaultState();
		IBlockState geodeState   = geodeBlock.getDefaultState();
		IBlockState calciteState = calciteBlock != null ? calciteBlock.getDefaultState() : casingState;

		// Pass 1: carve shells
		for (int dx = -ext; dx <= ext; dx++) {
			for (int dy = -ext; dy <= ext; dy++) {
				int ny = cy + dy;
				if (ny < 1 || ny > 254) continue;
				for (int dz = -ext; dz <= ext; dz++) {
					double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
					double n = noiseAt(cx + dx, cy + dy, cz + dz) * noiseAmp;

					double outerR   = radius + n;
					double calciteR = radius - 0.75 + n * 0.8;
					double innerR   = radius - 1.5 + n * 0.7;
					double hollowR  = radius - 3.0 + n * 0.5;

					if (dist > outerR) continue;

					BlockPos pos = new BlockPos(cx + dx, ny, cz + dz);
					if (!canReplace(world, pos)) continue;
					if (dist > calciteR) {
						world.setBlockState(pos, casingState, 2);
					} else if (dist > innerR) {
						world.setBlockState(pos, calciteState, 2);
					} else if (dist > hollowR) {
						world.setBlockState(pos, geodeState, 2);
					} else {
						world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
					}
				}
			}
		}

		// Pass 2: crystals on inner surface
		if (stage1Block == null || stage2Block == null || crystalBlock == null) return;

		for (int dx = -ext; dx <= ext; dx++) {
			for (int dy = -ext; dy <= ext; dy++) {
				int ny = cy + dy;
				if (ny < 2 || ny > 253) continue;
				for (int dz = -ext; dz <= ext; dz++) {
					BlockPos pos = new BlockPos(cx + dx, ny, cz + dz);
					if (world.getBlockState(pos).getBlock() != geodeBlock) continue;

					for (EnumFacing face : EnumFacing.values()) {
						BlockPos neighbor = pos.offset(face);
						if (!world.isAirBlock(neighbor)) continue;
						if (random.nextFloat() > 0.20f) continue;

						// Crystal grows from geode surface inward; facing = direction from geode to air
						float rnd = random.nextFloat();
						Block chosen = rnd < 0.40f ? stage1Block
								: (rnd < 0.75f ? stage2Block : crystalBlock);
						IBlockState crystalState;
						if (chosen == crystalBlock) {
							crystalState = chosen.getDefaultState()
								.withProperty(BlockAmethystCrystal.BlockCustom.FACING, face);
						} else if (chosen == stage1Block) {
							crystalState = chosen.getDefaultState()
								.withProperty(BlockAmethystCrystalStage1.BlockCustom.FACING, face);
						} else {
							crystalState = chosen.getDefaultState()
								.withProperty(BlockAmethystCrystalStage2.BlockCustom.FACING, face);
						}
						world.setBlockState(neighbor, crystalState, 18);
					}
				}
			}
		}
	}

	@Override
	public void generateWorld(Random random, int i2, int k2, World world, int dimID,
			IChunkGenerator cg, IChunkProvider cp) {
		if (dimID != 0) return;
		if (world.isRemote) return;
		if ((random.nextInt(1000) + 1) > 50) return;

		int count = random.nextInt(5) / 3 + 1;
		for (int a = 0; a < count; a++) {
			int i = i2 + random.nextInt(16) + 8;
			int k = k2 + random.nextInt(16) + 8;
			int height = 255;
			while (height > 0) {
				BlockPos bp = new BlockPos(i, height, k);
				if (!world.isAirBlock(bp)
						&& world.getBlockState(bp).getBlock()
							.getMaterial(world.getBlockState(bp)).blocksMovement())
					break;
				height--;
			}
			int j = Math.abs(random.nextInt(Math.max(1, height)) - 24);
			if (!isInsideRock(world, i, j + 5, k)) continue;
			generateGeode(world, random, i, j + 5, k);
		}
	}
}
