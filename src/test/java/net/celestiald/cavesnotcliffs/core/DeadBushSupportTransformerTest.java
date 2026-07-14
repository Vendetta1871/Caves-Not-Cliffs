package net.celestiald.cavesnotcliffs.core;

import net.celestiald.cavesnotcliffs.block.LushAzaleaBlocks;
import net.celestiald.cavesnotcliffs.block.LushMossBlocks;
import net.celestiald.cavesnotcliffs.content.DeadBushSupportHooks;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DeadBushSupportTransformerTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void actualForgeBinDevelopmentBytecodeGetsOnePreservingHook()
            throws Exception {
        byte[] original = targetBytes();
        assertNotNull(method(read(original), DeadBushSupportTransformer.MCP_METHOD));

        byte[] transformed = new DeadBushSupportTransformer().transform(
                DeadBushSupportTransformer.TARGET,
                DeadBushSupportTransformer.TARGET, original);

        assertHookShape(transformed, DeadBushSupportTransformer.MCP_METHOD);
        new TargetLoader(transformed).loadClass(DeadBushSupportTransformer.TARGET)
                .getDeclaredMethods();
    }

    @Test
    public void srgLikeMethodNameAndObfuscatedLaunchNameUseTheSameNarrowHook()
            throws Exception {
        ClassNode node = read(targetBytes());
        MethodNode support = method(node, DeadBushSupportTransformer.MCP_METHOD);
        support.name = DeadBushSupportTransformer.SRG_METHOD;

        byte[] transformed = new DeadBushSupportTransformer().transform(
                "apu", DeadBushSupportTransformer.TARGET, write(node));

        assertHookShape(transformed, DeadBushSupportTransformer.SRG_METHOD);
        new TargetLoader(transformed).loadClass(DeadBushSupportTransformer.TARGET)
                .getDeclaredMethods();
    }

    @Test
    public void transformedDeadBushPersistsOnEveryJava118SupportPeer()
            throws Exception {
        byte[] transformed = new DeadBushSupportTransformer().transform(
                DeadBushSupportTransformer.TARGET,
                DeadBushSupportTransformer.TARGET, targetBytes());
        Class<?> type = new TargetLoader(transformed)
                .loadClass(DeadBushSupportTransformer.TARGET);
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object deadBush = constructor.newInstance();
        Method support = type.getDeclaredMethod(
                DeadBushSupportTransformer.MCP_METHOD, IBlockState.class);
        support.setAccessible(true);

        for (NamedState peer : officialSupports()) {
            assertTrue(peer.name, (Boolean) support.invoke(deadBush, peer.state));
            assertTrue(peer.name, DeadBushSupportHooks.isJava118Support(peer.state));
        }
        for (NamedState rejected : rejectedSupports()) {
            assertFalse(rejected.name,
                    (Boolean) support.invoke(deadBush, rejected.state));
            assertFalse(rejected.name,
                    DeadBushSupportHooks.isJava118Support(rejected.state));
        }
    }

    @Test
    public void additionalHookContainsOnlyTheFourJava118DirtPeers() {
        assertTrue(DeadBushSupportHooks.isAdditionalSupport(
                Blocks.GRASS.getDefaultState()));
        assertTrue(DeadBushSupportHooks.isAdditionalSupport(
                Blocks.MYCELIUM.getDefaultState()));
        assertTrue(DeadBushSupportHooks.isAdditionalSupport(
                new LushAzaleaBlocks.RootedDirt().getDefaultState()));
        assertTrue(DeadBushSupportHooks.isAdditionalSupport(
                new LushMossBlocks.Moss().getDefaultState()));

        assertFalse(DeadBushSupportHooks.isAdditionalSupport(
                Blocks.DIRT.getDefaultState()));
        assertFalse(DeadBushSupportHooks.isAdditionalSupport(
                Blocks.SAND.getDefaultState()));
        assertFalse(DeadBushSupportHooks.isAdditionalSupport(
                Blocks.HARDENED_CLAY.getDefaultState()));
        assertFalse(DeadBushSupportHooks.isAdditionalSupport(
                Blocks.STAINED_HARDENED_CLAY.getDefaultState()));
        for (NamedState rejected : rejectedSupports()) {
            assertFalse(rejected.name,
                    DeadBushSupportHooks.isAdditionalSupport(rejected.state));
        }
        assertFalse(DeadBushSupportHooks.isAdditionalSupport(null));
        assertFalse(DeadBushSupportHooks.isJava118Support(null));
    }

    @Test
    public void missingAndDuplicateSupportMethodsFailClearly() {
        assertTransformFails(write(baseClass()), "support method");

        ClassNode duplicate = baseClass();
        duplicate.methods.add(returningMethod(
                DeadBushSupportTransformer.MCP_METHOD, 1));
        duplicate.methods.add(returningMethod(
                DeadBushSupportTransformer.SRG_METHOD, 1));
        assertTransformFails(write(duplicate), "unique MCP/SRG");
    }

    @Test
    public void zeroAndMultipleIntegerReturnsFailClearly() {
        ClassNode noReturn = baseClass();
        noReturn.methods.add(returningMethod(
                DeadBushSupportTransformer.MCP_METHOD, 0));
        assertTransformFails(write(noReturn), "found 0");

        ClassNode twoReturns = baseClass();
        twoReturns.methods.add(returningMethod(
                DeadBushSupportTransformer.MCP_METHOD, 2));
        assertTransformFails(write(twoReturns), "found 2");
    }

    @Test
    public void unrelatedAndNullClassesAreUntouched() {
        byte[] original = write(baseClass());
        DeadBushSupportTransformer transformer = new DeadBushSupportTransformer();
        assertSame(original, transformer.transform(
                "example.Unrelated", "example.Unrelated", original));
        assertSame(null, transformer.transform(
                DeadBushSupportTransformer.TARGET,
                DeadBushSupportTransformer.TARGET, null));
    }

    @Test
    public void corePluginRegistersTheDeadBushTransformer() {
        String[] transformers = new CavesNotCliffsCorePlugin()
                .getASMTransformerClass();
        assertEquals(DeadBushSupportTransformer.class.getName(),
                transformers[transformers.length - 2]);
    }

    private static List<NamedState> officialSupports() {
        List<NamedState> result = new ArrayList<NamedState>();
        for (BlockDirt.DirtType dirt : BlockDirt.DirtType.values()) {
            result.add(new NamedState("dirt/" + dirt.getName(),
                    Blocks.DIRT.getDefaultState().withProperty(
                            BlockDirt.VARIANT, dirt)));
        }
        result.add(new NamedState("grass", Blocks.GRASS.getDefaultState()));
        result.add(new NamedState("mycelium", Blocks.MYCELIUM.getDefaultState()));
        result.add(new NamedState("rooted dirt",
                new LushAzaleaBlocks.RootedDirt().getDefaultState()));
        result.add(new NamedState("moss",
                new LushMossBlocks.Moss().getDefaultState()));
        result.add(new NamedState("sand", Blocks.SAND.getStateFromMeta(0)));
        result.add(new NamedState("red sand", Blocks.SAND.getStateFromMeta(1)));
        result.add(new NamedState("terracotta",
                Blocks.HARDENED_CLAY.getDefaultState()));
        for (int color = 0; color < 16; ++color) {
            result.add(new NamedState("stained terracotta " + color,
                    Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(color)));
        }
        return result;
    }

    private static List<NamedState> rejectedSupports() {
        List<NamedState> result = new ArrayList<NamedState>();
        result.add(new NamedState("farmland", Blocks.FARMLAND.getDefaultState()));
        result.add(new NamedState("clay", Blocks.CLAY.getDefaultState()));
        result.add(new NamedState("gravel", Blocks.GRAVEL.getDefaultState()));
        result.add(new NamedState("sandstone", Blocks.SANDSTONE.getDefaultState()));
        result.add(new NamedState("stone", Blocks.STONE.getDefaultState()));
        return result;
    }

    private static void assertHookShape(byte[] bytes, String methodName) {
        MethodNode support = method(read(bytes), methodName);
        int hooks = 0;
        int returns = 0;
        for (AbstractInsnNode instruction : support.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.getOpcode() == Opcodes.INVOKESTATIC
                        && DeadBushSupportTransformer.HOOK_OWNER.equals(call.owner)
                        && DeadBushSupportTransformer.HOOK_NAME.equals(call.name)
                        && DeadBushSupportTransformer.SUPPORT_DESC.equals(call.desc)) {
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
            AbstractInsnNode stateLoad = previousMeaningful(hook);
            assertTrue(stateLoad instanceof VarInsnNode);
            assertEquals(Opcodes.ALOAD, stateLoad.getOpcode());
            assertEquals(1, ((VarInsnNode) stateLoad).var);
        }
        assertEquals(1, returns);
        assertEquals(1, hooks);
    }

    private static MethodNode method(ClassNode node, String name) {
        MethodNode result = null;
        for (MethodNode method : node.methods) {
            if (!name.equals(method.name)
                    || !DeadBushSupportTransformer.SUPPORT_DESC.equals(method.desc)) {
                continue;
            }
            if (result != null) {
                fail("Expected one " + name + DeadBushSupportTransformer.SUPPORT_DESC);
            }
            result = method;
        }
        assertNotNull(name, result);
        return result;
    }

    private static AbstractInsnNode previousMeaningful(AbstractInsnNode instruction) {
        AbstractInsnNode cursor = instruction.getPrevious();
        while (cursor != null && cursor.getOpcode() < 0) {
            cursor = cursor.getPrevious();
        }
        assertNotNull(cursor);
        return cursor;
    }

    private static MethodNode returningMethod(String name, int returns) {
        MethodNode method = new MethodNode(Opcodes.ASM5, Opcodes.ACC_PROTECTED,
                name, DeadBushSupportTransformer.SUPPORT_DESC, null, null);
        if (returns == 0) {
            method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
            method.instructions.add(new InsnNode(Opcodes.ATHROW));
            return method;
        }
        for (int index = 0; index < returns; ++index) {
            method.instructions.add(new InsnNode(index == 0
                    ? Opcodes.ICONST_0 : Opcodes.ICONST_1));
            method.instructions.add(new InsnNode(Opcodes.IRETURN));
        }
        return method;
    }

    private static ClassNode baseClass() {
        ClassNode node = new ClassNode(Opcodes.ASM5);
        node.version = Opcodes.V1_8;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = DeadBushSupportTransformer.TARGET_INTERNAL;
        node.superName = "java/lang/Object";
        return node;
    }

    private static void assertTransformFails(byte[] bytes, String point) {
        try {
            new DeadBushSupportTransformer().transform(
                    DeadBushSupportTransformer.TARGET,
                    DeadBushSupportTransformer.TARGET, bytes);
            fail("A changed dead-bush support shape must abort the transform");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains(
                    "dead bush support transformer"));
            assertTrue(expected.getMessage().contains(point));
            assertTrue(expected.getMessage().contains(
                    DeadBushSupportTransformer.TARGET));
        }
    }

    private static byte[] targetBytes() throws IOException {
        String path = DeadBushSupportTransformer.TARGET_INTERNAL + ".class";
        InputStream input = DeadBushSupportTransformerTest.class
                .getClassLoader().getResourceAsStream(path);
        if (input == null) {
            throw new IOException("Missing ForgeBin target " + path);
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                output.write(buffer, 0, count);
            }
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

    private static final class NamedState {
        private final String name;
        private final IBlockState state;

        private NamedState(String name, IBlockState state) {
            this.name = name;
            this.state = state;
        }
    }

    private static final class TargetLoader extends ClassLoader {
        private final byte[] bytes;

        private TargetLoader(byte[] bytes) {
            super(DeadBushSupportTransformerTest.class.getClassLoader());
            this.bytes = bytes;
        }

        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException {
            if (!DeadBushSupportTransformer.TARGET.equals(name)) {
                return super.loadClass(name, resolve);
            }
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) {
                loaded = defineClass(name, bytes, 0, bytes.length);
            }
            if (resolve) {
                resolveClass(loaded);
            }
            return loaded;
        }
    }
}
