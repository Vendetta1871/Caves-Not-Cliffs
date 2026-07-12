package net.celestiald.cavesnotcliffs.core;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PlainPumpkinStemTransformerTest {
    @Test
    public void developmentBytecodeGetsExactlyTheThreeStemHooks() throws Exception {
        byte[] transformed = new PlainPumpkinStemTransformer().transform(
                PlainPumpkinStemTransformer.TARGET,
                PlainPumpkinStemTransformer.TARGET, targetBytes());

        assertHookShape(transformed);
        new TargetLoader(transformed).loadClass(PlainPumpkinStemTransformer.TARGET)
                .getDeclaredMethods();
    }

    @Test
    public void srgLikeMembersUseTheSameDescriptorAndBytecodeAnchors() throws Exception {
        byte[] transformed = new PlainPumpkinStemTransformer().transform(
                "aug", PlainPumpkinStemTransformer.TARGET,
                renameTargetMembers(targetBytes()));

        assertHookShape(transformed);
        new TargetLoader(transformed).loadClass(PlainPumpkinStemTransformer.TARGET)
                .getDeclaredMethods();
    }

    @Test
    public void changedStemShapeFailsClearlyInsteadOfSilentlyMisbehaving() {
        ClassNode node = new ClassNode(Opcodes.ASM5);
        node.version = Opcodes.V1_8;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = PlainPumpkinStemTransformer.TARGET_INTERNAL;
        node.superName = "java/lang/Object";
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);

        try {
            new PlainPumpkinStemTransformer().transform(
                    PlainPumpkinStemTransformer.TARGET,
                    PlainPumpkinStemTransformer.TARGET, writer.toByteArray());
            fail("A changed stem shape must abort the transform");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("pumpkin stem transformer"));
        }
    }

    private static void assertHookShape(byte[] bytes) {
        MethodNode actualState = method(readNode(bytes),
                PlainPumpkinStemTransformer.ACTUAL_STATE_DESC);
        MethodNode updateTick = method(readNode(bytes),
                PlainPumpkinStemTransformer.UPDATE_TICK_DESC);

        assertEquals(1, hookCount(actualState,
                PlainPumpkinStemTransformer.MATCH_HOOK));
        assertEquals(1, hookCount(updateTick,
                PlainPumpkinStemTransformer.MATCH_HOOK));
        assertEquals(1, hookCount(updateTick,
                PlainPumpkinStemTransformer.STATE_HOOK));
        assertEquals(0, residualCropComparisons(actualState));
        assertEquals(0, residualCropComparisons(updateTick));
        assertEquals(0, residualCropDefaultStateCalls(updateTick));

        assertMatchHookBranchesOnFalse(actualState);
        assertMatchHookBranchesOnFalse(updateTick);
    }

    private static void assertMatchHookBranchesOnFalse(MethodNode method) {
        int checked = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (!isHook(instruction, PlainPumpkinStemTransformer.MATCH_HOOK)) {
                continue;
            }
            assertEquals(Opcodes.IFEQ, nextMeaningful(instruction).getOpcode());
            checked++;
        }
        assertEquals(1, checked);
    }

    private static int hookCount(MethodNode method, String name) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (isHook(instruction, name)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isHook(AbstractInsnNode instruction, String name) {
        if (!(instruction instanceof MethodInsnNode)) {
            return false;
        }
        MethodInsnNode call = (MethodInsnNode) instruction;
        return call.getOpcode() == Opcodes.INVOKESTATIC
                && PlainPumpkinStemTransformer.HOOK_OWNER.equals(call.owner)
                && name.equals(call.name);
    }

    private static int residualCropComparisons(MethodNode method) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction.getOpcode() == Opcodes.IF_ACMPNE
                    && isCropField(previousMeaningful(instruction))) {
                count++;
            }
        }
        return count;
    }

    private static int residualCropDefaultStateCalls(MethodNode method) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)
                    || !isCropField(previousMeaningful(instruction))) {
                continue;
            }
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (PlainPumpkinStemTransformer.BLOCK_OWNER.equals(call.owner)
                    && PlainPumpkinStemTransformer.DEFAULT_STATE_DESC.equals(call.desc)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isCropField(AbstractInsnNode instruction) {
        if (!(instruction instanceof FieldInsnNode)) {
            return false;
        }
        FieldInsnNode field = (FieldInsnNode) instruction;
        return field.getOpcode() == Opcodes.GETFIELD
                && PlainPumpkinStemTransformer.TARGET_INTERNAL.equals(field.owner)
                && PlainPumpkinStemTransformer.BLOCK_DESC.equals(field.desc);
    }

    private static byte[] renameTargetMembers(byte[] bytes) {
        ClassNode node = readNode(bytes);
        String cropName = null;
        for (FieldNode field : node.fields) {
            if (PlainPumpkinStemTransformer.BLOCK_DESC.equals(field.desc)) {
                if (cropName != null) {
                    fail("Expected one BlockStem crop field");
                }
                cropName = field.name;
                field.name = "field_149877_a";
            }
        }
        assertNotNull(cropName);

        for (MethodNode method : node.methods) {
            if (PlainPumpkinStemTransformer.ACTUAL_STATE_DESC.equals(method.desc)) {
                method.name = "func_176221_a";
            } else if (PlainPumpkinStemTransformer.UPDATE_TICK_DESC.equals(method.desc)) {
                method.name = "func_180650_b";
            }
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof FieldInsnNode) {
                    FieldInsnNode field = (FieldInsnNode) instruction;
                    if (PlainPumpkinStemTransformer.TARGET_INTERNAL.equals(field.owner)
                            && cropName.equals(field.name)
                            && PlainPumpkinStemTransformer.BLOCK_DESC.equals(field.desc)) {
                        field.name = "field_149877_a";
                    }
                } else if (instruction instanceof MethodInsnNode
                        && isCropField(previousMeaningful(instruction))) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (PlainPumpkinStemTransformer.BLOCK_OWNER.equals(call.owner)
                            && PlainPumpkinStemTransformer.DEFAULT_STATE_DESC
                            .equals(call.desc)) {
                        call.name = "func_176223_P";
                    }
                }
            }
        }

        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static MethodNode method(ClassNode node, String descriptor) {
        MethodNode result = null;
        for (MethodNode method : node.methods) {
            if (!descriptor.equals(method.desc)) {
                continue;
            }
            if (result != null) {
                fail("Expected a unique method for " + descriptor);
            }
            result = method;
        }
        assertNotNull(descriptor, result);
        return result;
    }

    private static AbstractInsnNode previousMeaningful(AbstractInsnNode instruction) {
        AbstractInsnNode cursor = instruction.getPrevious();
        while (cursor != null && cursor.getOpcode() < 0) {
            cursor = cursor.getPrevious();
        }
        return cursor;
    }

    private static AbstractInsnNode nextMeaningful(AbstractInsnNode instruction) {
        AbstractInsnNode cursor = instruction.getNext();
        while (cursor != null && cursor.getOpcode() < 0) {
            cursor = cursor.getNext();
        }
        assertNotNull(cursor);
        return cursor;
    }

    private static ClassNode readNode(byte[] bytes) {
        ClassNode node = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    private static byte[] targetBytes() throws IOException {
        String path = PlainPumpkinStemTransformer.TARGET_INTERNAL + ".class";
        InputStream input = PlainPumpkinStemTransformerTest.class.getClassLoader()
                .getResourceAsStream(path);
        assertNotNull(path, input);
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

    private static final class TargetLoader extends ClassLoader {
        private final byte[] bytes;

        TargetLoader(byte[] bytes) {
            super(PlainPumpkinStemTransformerTest.class.getClassLoader());
            this.bytes = bytes;
        }

        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException {
            if (!PlainPumpkinStemTransformer.TARGET.equals(name)) {
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
