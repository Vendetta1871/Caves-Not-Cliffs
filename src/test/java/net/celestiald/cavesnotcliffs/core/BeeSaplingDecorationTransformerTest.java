package net.celestiald.cavesnotcliffs.core;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BeeSaplingDecorationTransformerTest {
    @Test
    public void developmentBytecodeFinishesEveryGenerationReturnImmediately() throws Exception {
        byte[] transformed = new BeeSaplingDecorationTransformer().transform(
                BeeSaplingDecorationTransformer.TARGET,
                BeeSaplingDecorationTransformer.TARGET, targetBytes());
        assertHookShape(transformed);
    }

    @Test
    public void srgLikeMethodNameStillUsesCallAndDescriptorAnchors() throws Exception {
        ClassNode node = readNode(targetBytes());
        for (MethodNode method : node.methods) {
            if (BeeSaplingDecorationTransformer.GENERATE_TREE_DESC.equals(method.desc)) {
                method.name = "func_176476_e";
            }
        }
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);

        byte[] transformed = new BeeSaplingDecorationTransformer().transform(
                BeeSaplingDecorationTransformer.TARGET,
                BeeSaplingDecorationTransformer.TARGET, writer.toByteArray());
        assertHookShape(transformed);
    }

    @Test
    public void changedSaplingShapeFailsClearly() {
        ClassNode node = new ClassNode(Opcodes.ASM5);
        node.version = Opcodes.V1_8;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = BeeSaplingDecorationTransformer.TARGET_INTERNAL;
        node.superName = "java/lang/Object";
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        try {
            new BeeSaplingDecorationTransformer().transform(
                    BeeSaplingDecorationTransformer.TARGET,
                    BeeSaplingDecorationTransformer.TARGET, writer.toByteArray());
            fail("A changed sapling method must abort the transform");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("bee sapling decoration transformer"));
        }
    }

    private static void assertHookShape(byte[] bytes) {
        int returns = 0;
        int hooks = 0;
        for (MethodNode method : readNode(bytes).methods) {
            if (!BeeSaplingDecorationTransformer.GENERATE_TREE_DESC.equals(method.desc)
                    || !containsCall(method,
                    BeeSaplingDecorationTransformer.TERRAIN_GEN_OWNER,
                    BeeSaplingDecorationTransformer.SAPLING_EVENT_DESC)) {
                continue;
            }
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (BeeSaplingDecorationTransformer.HOOK_OWNER.equals(call.owner)
                            && BeeSaplingDecorationTransformer.HOOK_NAME.equals(call.name)
                            && BeeSaplingDecorationTransformer.HOOK_DESC.equals(call.desc)) {
                        hooks++;
                    }
                }
                if (instruction.getOpcode() != Opcodes.RETURN) {
                    continue;
                }
                returns++;
                AbstractInsnNode hookNode = previousMeaningful(instruction);
                assertTrue(hookNode instanceof MethodInsnNode);
                MethodInsnNode hook = (MethodInsnNode) hookNode;
                assertEquals(BeeSaplingDecorationTransformer.HOOK_OWNER, hook.owner);
                assertEquals(BeeSaplingDecorationTransformer.HOOK_NAME, hook.name);
                assertEquals(BeeSaplingDecorationTransformer.HOOK_DESC, hook.desc);
                assertLoad(previousMeaningful(hookNode), 4);
                assertLoad(previousMeaningful(previousMeaningful(hookNode)), 2);
                assertLoad(previousMeaningful(previousMeaningful(
                        previousMeaningful(hookNode))), 1);
            }
        }
        assertTrue("Expected more than one vanilla return path", returns > 1);
        assertEquals(returns, hooks);
    }

    private static void assertLoad(AbstractInsnNode instruction, int variable) {
        assertTrue(instruction instanceof VarInsnNode);
        assertEquals(Opcodes.ALOAD, instruction.getOpcode());
        assertEquals(variable, ((VarInsnNode) instruction).var);
    }

    private static boolean containsCall(MethodNode method, String owner, String descriptor) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (owner.equals(call.owner) && descriptor.equals(call.desc)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static AbstractInsnNode previousMeaningful(AbstractInsnNode instruction) {
        AbstractInsnNode cursor = instruction.getPrevious();
        while (cursor != null && cursor.getOpcode() < 0) {
            cursor = cursor.getPrevious();
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
        return classBytes(BeeSaplingDecorationTransformer.TARGET_INTERNAL + ".class");
    }

    private static byte[] classBytes(String resource) throws IOException {
        InputStream input = BeeSaplingDecorationTransformerTest.class
                .getClassLoader().getResourceAsStream(resource);
        assertNotNull(resource, input);
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
}
