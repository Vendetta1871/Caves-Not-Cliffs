package net.celestiald.cavesnotcliffs.core;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PlainPumpkinConnectionTransformerTest {
    private static final Target[] TARGETS = {
            new Target(PlainPumpkinConnectionTransformer.FENCE_TARGET,
                    "aqo", "isExcepBlockForAttachWithPiston", "func_194142_e"),
            new Target(PlainPumpkinConnectionTransformer.WALL_TARGET,
                    "auv", "isExcepBlockForAttachWithPiston", "func_194143_e"),
            new Target(PlainPumpkinConnectionTransformer.PANE_TARGET,
                    "auo", "isExcepBlockForAttachWithPiston", "func_193394_e")
    };

    @Test
    public void mcpNamedAnchorsForAllThreeTargetsGetOneVerifiedHook() throws Exception {
        assertAllTargets(false);
    }

    @Test
    public void srgNamedAnchorsForAllThreeTargetsUseTheSameDescriptorPath()
            throws Exception {
        assertAllTargets(true);
    }

    @Test
    public void actualForgeBinClassesRetainTheReviewedRuntimeShape()
            throws IOException {
        for (Target target : TARGETS) {
            byte[] transformed = new PlainPumpkinConnectionTransformer().transform(
                    target.name, target.name, targetBytes(target));
            assertHookShape(transformed);
        }
    }

    @Test
    public void missingAnchorFailsClearly() {
        assertTransformFails(TARGETS[0], emptyClass(TARGETS[0].name),
                "connection-exception method");
    }

    @Test
    public void duplicateDescriptorAnchorFailsClearly() {
        ClassNode node = baseClass(TARGETS[1].name);
        node.methods.add(exceptionMethod(TARGETS[1].mcpName, 0));
        node.methods.add(exceptionMethod(TARGETS[1].srgName, 1));
        assertTransformFails(TARGETS[1], write(node), "unique static");
    }

    @Test
    public void changedReturnShapeFailsClearly() {
        ClassNode node = baseClass(TARGETS[2].name);
        MethodNode method = exceptionMethod(TARGETS[2].mcpName, 0);
        method.instructions.add(new InsnNode(Opcodes.ICONST_1));
        method.instructions.add(new InsnNode(Opcodes.IRETURN));
        node.methods.add(method);
        assertTransformFails(TARGETS[2], write(node), "one integer return");
    }

    @Test
    public void unrelatedClassesAreUntouched() {
        byte[] original = emptyClass("example.Unrelated");
        assertSame(original, new PlainPumpkinConnectionTransformer().transform(
                "example.Unrelated", "example.Unrelated", original));
    }

    private static void assertAllTargets(boolean srg) throws Exception {
        for (Target target : TARGETS) {
            byte[] transformed = new PlainPumpkinConnectionTransformer().transform(
                    srg ? target.obfuscatedName : target.name, target.name,
                    targetClass(target, srg ? target.srgName : target.mcpName));
            assertHookShape(transformed);
            Class<?> verified = new TargetLoader(target.name, transformed)
                    .loadClass(target.name);
            assertEquals(target.name, verified.getName());
            assertEquals(1, verified.getDeclaredMethods().length);
        }
    }

    private static void assertHookShape(byte[] bytes) {
        ClassNode node = read(bytes);
        int hooks = 0;
        int ors = 0;
        int originalFalse = 0;
        int returns = 0;
        for (MethodNode method : node.methods) {
            if (!PlainPumpkinConnectionTransformer.EXCEPTION_DESC.equals(method.desc)) {
                continue;
            }
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (call.getOpcode() == Opcodes.INVOKESTATIC
                            && PlainPumpkinConnectionTransformer.HOOK_OWNER.equals(call.owner)
                            && PlainPumpkinConnectionTransformer.HOOK_NAME.equals(call.name)
                            && PlainPumpkinConnectionTransformer.EXCEPTION_DESC
                                    .equals(call.desc)) {
                        hooks++;
                    }
                } else if (instruction.getOpcode() == Opcodes.IOR) {
                    ors++;
                } else if (instruction.getOpcode() == Opcodes.ICONST_0) {
                    originalFalse++;
                } else if (instruction.getOpcode() == Opcodes.IRETURN) {
                    returns++;
                }
            }
        }
        assertEquals(1, hooks);
        assertEquals(1, ors);
        assertEquals(1, originalFalse);
        assertEquals(1, returns);
    }

    private static byte[] targetClass(Target target, String methodName) {
        ClassNode node = baseClass(target.name);
        node.methods.add(exceptionMethod(methodName, 0));
        return write(node);
    }

    private static MethodNode exceptionMethod(String name, int value) {
        MethodNode method = new MethodNode(Opcodes.ASM5,
                Opcodes.ACC_PROTECTED | Opcodes.ACC_STATIC, name,
                PlainPumpkinConnectionTransformer.EXCEPTION_DESC, null, null);
        method.instructions.add(new InsnNode(value == 0
                ? Opcodes.ICONST_0 : Opcodes.ICONST_1));
        method.instructions.add(new InsnNode(Opcodes.IRETURN));
        return method;
    }

    private static byte[] emptyClass(String name) {
        return write(baseClass(name));
    }

    private static byte[] targetBytes(Target target) throws IOException {
        String path = target.name.replace('.', '/') + ".class";
        InputStream input = PlainPumpkinConnectionTransformerTest.class
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

    private static ClassNode baseClass(String name) {
        ClassNode node = new ClassNode(Opcodes.ASM5);
        node.version = Opcodes.V1_8;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = name.replace('.', '/');
        node.superName = "java/lang/Object";
        return node;
    }

    private static byte[] write(ClassNode node) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    private static void assertTransformFails(Target target, byte[] bytes,
            String expectedPoint) {
        try {
            new PlainPumpkinConnectionTransformer().transform(
                    target.name, target.name, bytes);
            fail("Changed connection anchor must abort the transform");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains(
                    "plain pumpkin connection transformer"));
            assertTrue(expected.getMessage().contains(expectedPoint));
            assertTrue(expected.getMessage().contains(target.name));
        }
    }

    private static final class Target {
        private final String name;
        private final String obfuscatedName;
        private final String mcpName;
        private final String srgName;

        private Target(String name, String obfuscatedName, String mcpName,
                String srgName) {
            this.name = name;
            this.obfuscatedName = obfuscatedName;
            this.mcpName = mcpName;
            this.srgName = srgName;
        }
    }

    private static final class TargetLoader extends ClassLoader {
        private final String target;
        private final byte[] bytes;

        private TargetLoader(String target, byte[] bytes) {
            super(PlainPumpkinConnectionTransformerTest.class.getClassLoader());
            this.target = target;
            this.bytes = bytes;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException {
            if (!target.equals(name)) {
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
