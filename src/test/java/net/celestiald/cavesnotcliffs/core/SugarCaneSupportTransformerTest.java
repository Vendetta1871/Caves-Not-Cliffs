package net.celestiald.cavesnotcliffs.core;

import net.celestiald.cavesnotcliffs.block.LushAzaleaBlocks;
import net.celestiald.cavesnotcliffs.block.LushDripleafBlocks;
import net.celestiald.cavesnotcliffs.block.LushMossBlocks;
import net.celestiald.cavesnotcliffs.content.SugarCaneSupportHooks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import sun.misc.Unsafe;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SugarCaneSupportTransformerTest {
    private static final BlockPos CANE = new BlockPos(4, 65, -7);

    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void actualForgeBinMcpBytecodeHooksEveryOriginalReturn() throws Exception {
        byte[] original = targetBytes();
        MethodNode before = method(read(original), SugarCaneSupportTransformer.MCP_METHOD);
        int originalReturns = integerReturns(before);
        assertTrue(originalReturns > 1);

        byte[] transformed = new SugarCaneSupportTransformer().transform(
                SugarCaneSupportTransformer.TARGET,
                SugarCaneSupportTransformer.TARGET, original);

        assertHookShape(transformed, SugarCaneSupportTransformer.MCP_METHOD,
                originalReturns);
        new TargetLoader(transformed).loadClass(SugarCaneSupportTransformer.TARGET)
                .getDeclaredMethods();
    }

    @Test
    public void srgMethodAndObfuscatedLaunchNameUseTheSameHook() throws Exception {
        ClassNode node = read(targetBytes());
        method(node, SugarCaneSupportTransformer.MCP_METHOD).name =
                SugarCaneSupportTransformer.SRG_METHOD;
        int returns = integerReturns(method(node, SugarCaneSupportTransformer.SRG_METHOD));

        byte[] transformed = new SugarCaneSupportTransformer().transform(
                "ati", SugarCaneSupportTransformer.TARGET, write(node));

        assertHookShape(transformed, SugarCaneSupportTransformer.SRG_METHOD, returns);
    }

    @Test
    public void transformedForgeBinAcceptsTheJava118GroundAndWaterMatrix()
            throws Exception {
        byte[] transformed = new SugarCaneSupportTransformer().transform(
                SugarCaneSupportTransformer.TARGET,
                SugarCaneSupportTransformer.TARGET, targetBytes());
        Class<?> type = new TargetLoader(transformed)
                .loadClass(SugarCaneSupportTransformer.TARGET);
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object reed = constructor.newInstance();
        Method canPlace = type.getDeclaredMethod(
                SugarCaneSupportTransformer.MCP_METHOD, World.class, BlockPos.class);
        canPlace.setAccessible(true);

        List<IBlockState> grounds = new ArrayList<IBlockState>();
        for (BlockDirt.DirtType dirt : BlockDirt.DirtType.values()) {
            grounds.add(Blocks.DIRT.getDefaultState().withProperty(
                    BlockDirt.VARIANT, dirt));
        }
        grounds.add(Blocks.GRASS.getDefaultState());
        grounds.add(Blocks.MYCELIUM.getDefaultState());
        grounds.add(Blocks.SAND.getStateFromMeta(0));
        grounds.add(Blocks.SAND.getStateFromMeta(1));
        grounds.add(new LushAzaleaBlocks.RootedDirt().getDefaultState());
        grounds.add(new LushMossBlocks.Moss().getDefaultState());

        LushDripleafBlocks.Small dripleaf = new LushDripleafBlocks.Small();
        IBlockState retainedWater = dripleaf.getDefaultState().withProperty(
                LushDripleafBlocks.WATERLOGGED, true);
        IBlockState[] wetPeers = {
            Blocks.WATER.getDefaultState(), Blocks.FLOWING_WATER.getDefaultState(),
            Blocks.FROSTED_ICE.getDefaultState(), retainedWater
        };
        for (IBlockState ground : grounds) {
            for (IBlockState wet : wetPeers) {
                FakeWorld world = FakeWorld.create();
                world.put(CANE.down(), ground);
                world.put(CANE.down().north(), wet);
                assertTrue(ground + " beside " + wet,
                        (Boolean) canPlace.invoke(reed, world, CANE));
                assertTrue(ground + " beside " + wet,
                        SugarCaneSupportHooks.canPlaceOnFutureSupport(world, CANE));
            }
        }

        FakeWorld dry = FakeWorld.create();
        dry.put(CANE.down(), new LushMossBlocks.Moss().getDefaultState());
        assertFalse((Boolean) canPlace.invoke(reed, dry, CANE));
        assertFalse(SugarCaneSupportHooks.canPlaceOnFutureSupport(dry, CANE));

        FakeWorld wrongGround = FakeWorld.create();
        wrongGround.put(CANE.down(), Blocks.STONE.getDefaultState());
        wrongGround.put(CANE.down().north(), retainedWater);
        assertFalse((Boolean) canPlace.invoke(reed, wrongGround, CANE));

        FakeWorld broadMaterial = FakeWorld.create();
        broadMaterial.put(CANE.down(), new LushAzaleaBlocks.RootedDirt().getDefaultState());
        broadMaterial.put(CANE.down().north(),
                new Block(Material.WATER) { }.getDefaultState());
        assertFalse((Boolean) canPlace.invoke(reed, broadMaterial, CANE));

        FakeWorld stacked = FakeWorld.create();
        Method defaultState = type.getMethod("getDefaultState");
        stacked.put(CANE.down(), (IBlockState) defaultState.invoke(reed));
        assertTrue((Boolean) canPlace.invoke(reed, stacked, CANE));
    }

    @Test
    public void pureHookIsFailClosedAndUsesNorthEastSouthWestOrder()
            throws Exception {
        assertFalse(SugarCaneSupportHooks.canPlaceOnFutureSupport(null, CANE));
        assertFalse(SugarCaneSupportHooks.canPlaceOnFutureSupport(
                FakeWorld.create(), null));

        FakeWorld dry = FakeWorld.create();
        dry.put(CANE.down(), new LushAzaleaBlocks.RootedDirt().getDefaultState());
        assertFalse(SugarCaneSupportHooks.canPlaceOnFutureSupport(dry, CANE));
        assertEquals(Arrays.asList(CANE.down(), CANE.down().north(),
                CANE.down().east(), CANE.down().south(), CANE.down().west()),
                dry.queries);
    }

    @Test
    public void changedMethodShapesFailClosedAndUnrelatedClassesPassThrough() {
        assertTransformFails(write(baseClass()), "placement method");

        ClassNode duplicate = baseClass();
        duplicate.methods.add(returningMethod(
                SugarCaneSupportTransformer.MCP_METHOD, true));
        duplicate.methods.add(returningMethod(
                SugarCaneSupportTransformer.SRG_METHOD, true));
        assertTransformFails(write(duplicate), "unique MCP/SRG");

        ClassNode noReturn = baseClass();
        noReturn.methods.add(returningMethod(
                SugarCaneSupportTransformer.MCP_METHOD, false));
        assertTransformFails(write(noReturn), "at least one integer return");

        byte[] unrelated = write(baseClass());
        SugarCaneSupportTransformer transformer = new SugarCaneSupportTransformer();
        assertSame(unrelated, transformer.transform(
                "example.Unrelated", "example.Unrelated", unrelated));
        assertSame(null, transformer.transform(SugarCaneSupportTransformer.TARGET,
                SugarCaneSupportTransformer.TARGET, null));
    }

    @Test
    public void corePluginRegistersSugarCaneImmediatelyAfterDeadBush() {
        String[] transformers = new CavesNotCliffsCorePlugin()
                .getASMTransformerClass();
        assertEquals(DeadBushSupportTransformer.class.getName(),
                transformers[transformers.length - 2]);
        assertEquals(SugarCaneSupportTransformer.class.getName(),
                transformers[transformers.length - 1]);
    }

    private static void assertHookShape(byte[] bytes, String methodName,
            int expectedReturns) {
        MethodNode method = method(read(bytes), methodName);
        int hooks = 0;
        int returns = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.getOpcode() == Opcodes.INVOKESTATIC
                        && SugarCaneSupportTransformer.HOOK_OWNER.equals(call.owner)
                        && SugarCaneSupportTransformer.HOOK_NAME.equals(call.name)
                        && SugarCaneSupportTransformer.METHOD_DESC.equals(call.desc)) {
                    hooks++;
                }
            }
            if (instruction.getOpcode() != Opcodes.IRETURN) {
                continue;
            }
            returns++;
            AbstractInsnNode or = previousMeaningful(instruction);
            assertEquals(Opcodes.IOR, or.getOpcode());
            AbstractInsnNode hook = previousMeaningful(or);
            assertTrue(hook instanceof MethodInsnNode);
            AbstractInsnNode position = previousMeaningful(hook);
            AbstractInsnNode world = previousMeaningful(position);
            assertTrue(position instanceof VarInsnNode);
            assertTrue(world instanceof VarInsnNode);
            assertEquals(2, ((VarInsnNode) position).var);
            assertEquals(1, ((VarInsnNode) world).var);
        }
        assertEquals(expectedReturns, returns);
        assertEquals(expectedReturns, hooks);
    }

    private static int integerReturns(MethodNode method) {
        int result = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction.getOpcode() == Opcodes.IRETURN) result++;
        }
        return result;
    }

    private static MethodNode method(ClassNode node, String name) {
        MethodNode result = null;
        for (MethodNode method : node.methods) {
            if (!name.equals(method.name)
                    || !SugarCaneSupportTransformer.METHOD_DESC.equals(method.desc)) {
                continue;
            }
            if (result != null) fail("Expected one " + name);
            result = method;
        }
        assertNotNull(name, result);
        return result;
    }

    private static AbstractInsnNode previousMeaningful(AbstractInsnNode instruction) {
        AbstractInsnNode cursor = instruction.getPrevious();
        while (cursor != null && cursor.getOpcode() < 0) cursor = cursor.getPrevious();
        assertNotNull(cursor);
        return cursor;
    }

    private static MethodNode returningMethod(String name, boolean withReturn) {
        MethodNode method = new MethodNode(Opcodes.ASM5, Opcodes.ACC_PUBLIC,
                name, SugarCaneSupportTransformer.METHOD_DESC, null, null);
        if (withReturn) {
            method.instructions.add(new InsnNode(Opcodes.ICONST_0));
            method.instructions.add(new InsnNode(Opcodes.IRETURN));
        } else {
            method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
            method.instructions.add(new InsnNode(Opcodes.ATHROW));
        }
        return method;
    }

    private static ClassNode baseClass() {
        ClassNode node = new ClassNode(Opcodes.ASM5);
        node.version = Opcodes.V1_8;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = SugarCaneSupportTransformer.TARGET_INTERNAL;
        node.superName = "java/lang/Object";
        return node;
    }

    private static void assertTransformFails(byte[] bytes, String point) {
        try {
            new SugarCaneSupportTransformer().transform(
                    SugarCaneSupportTransformer.TARGET,
                    SugarCaneSupportTransformer.TARGET, bytes);
            fail("A changed sugar-cane support shape must abort the transform");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("sugar cane support transformer"));
            assertTrue(expected.getMessage().contains(point));
            assertTrue(expected.getMessage().contains(SugarCaneSupportTransformer.TARGET));
        }
    }

    private static byte[] targetBytes() throws IOException {
        String path = SugarCaneSupportTransformer.TARGET_INTERNAL + ".class";
        InputStream input = SugarCaneSupportTransformerTest.class
                .getClassLoader().getResourceAsStream(path);
        if (input == null) throw new IOException("Missing ForgeBin target " + path);
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
            return output.toByteArray();
        } finally {
            input.close();
        }
    }

    private static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    private static byte[] write(ClassNode node) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static final class TargetLoader extends ClassLoader {
        private final byte[] bytes;

        private TargetLoader(byte[] bytes) {
            super(SugarCaneSupportTransformerTest.class.getClassLoader());
            this.bytes = bytes;
        }

        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException {
            if (!SugarCaneSupportTransformer.TARGET.equals(name)) {
                return super.loadClass(name, resolve);
            }
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) loaded = defineClass(name, bytes, 0, bytes.length);
            if (resolve) resolveClass(loaded);
            return loaded;
        }
    }

    private static final class FakeWorld extends World {
        private Map<BlockPos, IBlockState> states;
        private List<BlockPos> queries;

        private FakeWorld() {
            super(null, null, null, new Profiler(), false);
        }

        static FakeWorld create() throws Exception {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Unsafe unsafe = (Unsafe) field.get(null);
            FakeWorld world = (FakeWorld) unsafe.allocateInstance(FakeWorld.class);
            world.states = new HashMap<BlockPos, IBlockState>();
            world.queries = new ArrayList<BlockPos>();
            return world;
        }

        void put(BlockPos pos, IBlockState state) {
            states.put(pos.toImmutable(), state);
        }

        @Override
        public IBlockState getBlockState(BlockPos pos) {
            queries.add(pos.toImmutable());
            IBlockState state = states.get(pos);
            return state == null ? Blocks.AIR.getDefaultState() : state;
        }

        @Override
        protected IChunkProvider createChunkProvider() {
            return null;
        }

        @Override
        protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
            return true;
        }
    }
}
