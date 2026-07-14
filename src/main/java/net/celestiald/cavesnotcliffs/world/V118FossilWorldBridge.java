package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.worldgen.v118.TerrainColumn;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118Biome;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118FossilPlacements;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.template.ITemplateProcessor;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.Random;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Finite Forge 1.12 template adapter for the Java 1.18.2 fossil feature. */
final class V118FossilWorldBridge implements V118FossilPlacements.WorldAccess {
    private static final ResourceLocation[] FOSSILS = resources(false);
    private static final ResourceLocation[] OVERLAYS = resources(true);
    private static final Rotation[] ROTATIONS = Rotation.values();

    private final World world;
    private final V118ChunkGenerator generator;
    private final IBlockState deepslateDiamondOre;

    V118FossilWorldBridge(World world, V118ChunkGenerator generator) {
        if (world == null || generator == null) {
            throw new NullPointerException("Fossil bridge arguments");
        }
        this.world = world;
        this.generator = generator;
        Block diamond = GameRegistry.findRegistry(Block.class).getValue(
            new ResourceLocation("cavesnotcliffs", "deepslate_diamond_ore"));
        if (diamond == null) {
            throw new IllegalStateException("Required fossil overlay block is not registered");
        }
        deepslateDiamondOre = diamond.getDefaultState();
    }

    int populate(int chunkX, int chunkZ) {
        return V118FossilPlacements.decorate(this, world.getSeed(), chunkX, chunkZ);
    }

    @Override
    public V118Biome biomeAt(int blockX, int blockY, int blockZ) {
        return generator.getDecorationBiome(blockX, blockY, blockZ);
    }

    @Override
    public boolean placeFossil(Random random, int originX, int originY, int originZ,
            boolean diamondOverlay) {
        Rotation rotation = ROTATIONS[random.nextInt(ROTATIONS.length)];
        int templateIndex = random.nextInt(FOSSILS.length);
        TemplateManager manager = world.getSaveHandler().getStructureTemplateManager();
        Template fossil = normalizedTemplate(manager.getTemplate(
            world.getMinecraftServer(), FOSSILS[templateIndex]));
        Template overlay = normalizedTemplate(manager.getTemplate(
            world.getMinecraftServer(), OVERLAYS[templateIndex]));
        if (fossil == null || overlay == null) {
            throw new IllegalStateException("Vanilla fossil templates are unavailable");
        }

        ChunkPos chunk = new ChunkPos(new BlockPos(originX, originY, originZ));
        StructureBoundingBox featureBounds = new StructureBoundingBox(
            chunk.getXStart() - 16, TerrainColumn.MIN_Y, chunk.getZStart() - 16,
            chunk.getXEnd() + 16, TerrainColumn.MAX_Y_EXCLUSIVE,
            chunk.getZEnd() + 16);
        PlacementSettings settings = new PlacementSettings().setRotation(rotation)
            .setBoundingBox(featureBounds).setRandom(random);
        BlockPos size = fossil.transformedSize(rotation);
        BlockPos centered = new BlockPos(originX - size.getX() / 2, originY,
            originZ - size.getZ() / 2);
        int floor = originY;
        for (int x = 0; x < size.getX(); ++x) {
            for (int z = 0; z < size.getZ(); ++z) {
                floor = Math.min(floor, oceanFloorWgHeight(
                    centered.getX() + x, centered.getZ() + z));
            }
        }
        int y = Math.max(floor - 15 - random.nextInt(10), TerrainColumn.MIN_Y + 10);
        BlockPos placementOrigin = fossil.getZeroPositionWithTransform(
            new BlockPos(centered.getX(), y, centered.getZ()), Mirror.NONE, rotation);
        StructureBoundingBox fossilBounds = transformedBounds(fossil, settings,
            placementOrigin);
        if (countEmptyCorners(fossilBounds) > 4) {
            return false;
        }

        // StructureTemplate#getRandomPalette consumes nextInt(1) even though every
        // vanilla fossil template has exactly one palette.
        random.nextInt(1);
        fossil.addBlocksToWorld(world, placementOrigin,
            processor(random, 0.9F, false), settings, 4);
        random.nextInt(1);
        overlay.addBlocksToWorld(world, placementOrigin,
            processor(random, 0.1F, diamondOverlay), settings, 4);
        return true;
    }

    private ITemplateProcessor processor(final Random random, final float integrity,
            final boolean diamondOverlay) {
        return (level, position, info) -> {
            // Processor order is BlockRot, optional coal-to-diamond rule, then protected blocks.
            if (random.nextFloat() > integrity || protectedAt(position)) {
                return null;
            }
            if (diamondOverlay && info.blockState.getBlock() == Blocks.COAL_ORE) {
                return new Template.BlockInfo(info.pos, deepslateDiamondOre,
                    info.tileentityData);
            }
            return info;
        };
    }

    private int oceanFloorWgHeight(int blockX, int blockZ) {
        for (int y = TerrainColumn.MAX_Y_EXCLUSIVE - 1; y >= TerrainColumn.MIN_Y; --y) {
            IBlockState state = world.getBlockState(new BlockPos(blockX, y, blockZ));
            if (state.getMaterial().blocksMovement() && !isPowderSnow(state)) {
                return y + 1;
            }
        }
        return TerrainColumn.MIN_Y;
    }

    private int countEmptyCorners(StructureBoundingBox bounds) {
        int empty = 0;
        for (int x : new int[]{bounds.minX, bounds.maxX}) {
            for (int y : new int[]{bounds.minY, bounds.maxY}) {
                for (int z : new int[]{bounds.minZ, bounds.maxZ}) {
                    IBlockState state = world.getBlockState(new BlockPos(x, y, z));
                    Block block = state.getBlock();
                    if (state.getMaterial() == Material.AIR || block == Blocks.WATER
                            || block == Blocks.FLOWING_WATER || block == Blocks.LAVA
                            || block == Blocks.FLOWING_LAVA) {
                        ++empty;
                    }
                }
            }
        }
        return empty;
    }

    private static StructureBoundingBox transformedBounds(Template template,
            PlacementSettings settings, BlockPos origin) {
        BlockPos max = template.getSize().add(-1, -1, -1);
        BlockPos first = Template.transformedBlockPos(settings, BlockPos.ORIGIN).add(origin);
        int minX = first.getX();
        int minY = first.getY();
        int minZ = first.getZ();
        int maxX = minX;
        int maxY = minY;
        int maxZ = minZ;
        for (int x : new int[]{0, max.getX()}) {
            for (int y : new int[]{0, max.getY()}) {
                for (int z : new int[]{0, max.getZ()}) {
                    BlockPos pos = Template.transformedBlockPos(settings,
                        new BlockPos(x, y, z)).add(origin);
                    minX = Math.min(minX, pos.getX());
                    minY = Math.min(minY, pos.getY());
                    minZ = Math.min(minZ, pos.getZ());
                    maxX = Math.max(maxX, pos.getX());
                    maxY = Math.max(maxY, pos.getY());
                    maxZ = Math.max(maxZ, pos.getZ());
                }
            }
        }
        return new StructureBoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private boolean protectedAt(BlockPos pos) {
        Block block = world.getBlockState(pos).getBlock();
        return block == Blocks.BEDROCK || block == Blocks.MOB_SPAWNER
            || block == Blocks.CHEST || block == Blocks.END_PORTAL_FRAME;
    }

    private static boolean isPowderSnow(IBlockState state) {
        ResourceLocation name = state.getBlock().getRegistryName();
        return name != null && "cavesnotcliffs".equals(name.getResourceDomain())
            && "powder_snow".equals(name.getResourcePath());
    }

    private static ResourceLocation[] resources(boolean coal) {
        ResourceLocation[] result = new ResourceLocation[8];
        for (int index = 0; index < result.length; ++index) {
            String family = index < 4 ? "spine" : "skull";
            int number = index % 4 + 1;
            result[index] = new ResourceLocation("fossils/fossil_" + family + "_0"
                + number + (coal ? "_coal" : ""));
        }
        return result;
    }

    /** 1.18 sorts each full-block palette by Y, X, Z before processor RNG. */
    private static Template normalizedTemplate(Template source) {
        if (source == null) {
            return null;
        }
        NBTTagCompound serialized = source.writeToNBT(new NBTTagCompound());
        NBTTagList blocks = serialized.getTagList("blocks", 10);
        List<NBTTagCompound> sorted = new ArrayList<>(blocks.tagCount());
        for (int index = 0; index < blocks.tagCount(); ++index) {
            sorted.add(blocks.getCompoundTagAt(index));
        }
        sorted.sort(Comparator.comparingInt(V118FossilWorldBridge::blockY)
            .thenComparingInt(V118FossilWorldBridge::blockX)
            .thenComparingInt(V118FossilWorldBridge::blockZ));
        NBTTagList normalized = new NBTTagList();
        for (NBTTagCompound block : sorted) {
            normalized.appendTag(block);
        }
        serialized.setTag("blocks", normalized);
        Template template = new Template();
        template.read(serialized);
        return template;
    }

    private static int blockX(NBTTagCompound block) {
        return block.getTagList("pos", 3).getIntAt(0);
    }

    private static int blockY(NBTTagCompound block) {
        return block.getTagList("pos", 3).getIntAt(1);
    }

    private static int blockZ(NBTTagCompound block) {
        return block.getTagList("pos", 3).getIntAt(2);
    }
}
