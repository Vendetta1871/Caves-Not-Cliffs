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

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MushroomSupportTransformerTest {
    @Test
    public void mojangFourReturnShapeIsAccepted() {
        assertGuardPrepended(transformWithReturns(
                MushroomSupportTransformer.MCP_METHOD, 4),
                MushroomSupportTransformer.MCP_METHOD);
    }

    @Test
    public void cleanroomThreeReturnShapeIsAccepted() {
        // Cleanroom rebuilds Minecraft from decompiled sources; the podzol
        // branch and the light check share one ternary return there.
        assertGuardPrepended(transformWithReturns(
                MushroomSupportTransformer.SRG_METHOD, 3),
                MushroomSupportTransformer.SRG_METHOD);
    }

    @Test
    public void unexpectedReturnCountsFailClearly() {
        assertTransformFails(write(classWithSurvival(
                MushroomSupportTransformer.MCP_METHOD, 2)), "found 2");
        assertTransformFails(write(classWithSurvival(
                MushroomSupportTransformer.MCP_METHOD, 5)), "found 5");
    }

    @Test
    public void missingAndDuplicateSurvivalMethodsFailClearly() {
        assertTransformFails(write(baseClass()), "survival method");

        ClassNode duplicate = baseClass();
        duplicate.methods.add(returningMethod(
                MushroomSupportTransformer.MCP_METHOD, 4));
        duplicate.methods.add(returningMethod(
                MushroomSupportTransformer.SRG_METHOD, 4));
        assertTransformFails(write(duplicate), "unique MCP/SRG");
    }

    @Test
    public void unrelatedAndNullClassesAreUntouched() {
        byte[] original = write(baseClass());
        MushroomSupportTransformer transformer = new MushroomSupportTransformer();
        assertSame(original, transformer.transform(
                "example.Unrelated", "example.Unrelated", original));
        assertSame(null, transformer.transform(
                MushroomSupportTransformer.TARGET,
                MushroomSupportTransformer.TARGET, null));
    }

    @Test
    public void corePluginRegistersTheMushroomTransformer() {
        String[] transformers = new CavesNotCliffsCorePlugin()
                .getASMTransformerClass();
        assertTrue(Arrays.asList(transformers).contains(
                MushroomSupportTransformer.class.getName()));
    }

    private static byte[] transformWithReturns(String methodName, int returns) {
        return new MushroomSupportTransformer().transform(
                MushroomSupportTransformer.TARGET,
                MushroomSupportTransformer.TARGET,
                write(classWithSurvival(methodName, returns)));
    }

    private static void assertGuardPrepended(byte[] bytes, String methodName) {
        MethodNode survival = null;
        ClassNode node = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(node, 0);
        for (MethodNode method : node.methods) {
            if (methodName.equals(method.name)
                    && MushroomSupportTransformer.METHOD_DESC.equals(method.desc)) {
                survival = method;
            }
        }
        assertNotNull(survival);

        int guardCalls = 0;
        int hookCalls = 0;
        for (AbstractInsnNode instruction : survival.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) {
                continue;
            }
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (call.getOpcode() == Opcodes.INVOKESTATIC
                    && MushroomSupportTransformer.HOOK_OWNER.equals(call.owner)) {
                if (MushroomSupportTransformer.GUARD_NAME.equals(call.name)) {
                    guardCalls++;
                } else if (MushroomSupportTransformer.HOOK_NAME.equals(call.name)) {
                    hookCalls++;
                }
            }
        }
        assertEquals(1, guardCalls);
        assertEquals(1, hookCalls);

        AbstractInsnNode first = survival.instructions.getFirst();
        while (first != null && first.getOpcode() < 0) {
            first = first.getNext();
        }
        assertNotNull(first);
        assertEquals(Opcodes.ALOAD, first.getOpcode());
    }

    private static MethodNode returningMethod(String name, int returns) {
        MethodNode method = new MethodNode(Opcodes.ASM5, Opcodes.ACC_PUBLIC,
                name, MushroomSupportTransformer.METHOD_DESC, null, null);
        for (int index = 0; index < returns; ++index) {
            method.instructions.add(new InsnNode(index == 0
                    ? Opcodes.ICONST_0 : Opcodes.ICONST_1));
            method.instructions.add(new InsnNode(Opcodes.IRETURN));
        }
        return method;
    }

    private static ClassNode classWithSurvival(String methodName, int returns) {
        ClassNode node = baseClass();
        node.methods.add(returningMethod(methodName, returns));
        return node;
    }

    private static ClassNode baseClass() {
        ClassNode node = new ClassNode(Opcodes.ASM5);
        node.version = Opcodes.V1_8;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = MushroomSupportTransformer.TARGET.replace('.', '/');
        node.superName = "java/lang/Object";
        return node;
    }

    private static void assertTransformFails(byte[] bytes, String point) {
        try {
            new MushroomSupportTransformer().transform(
                    MushroomSupportTransformer.TARGET,
                    MushroomSupportTransformer.TARGET, bytes);
            fail("A changed mushroom survival shape must abort the transform");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains(
                    "mushroom support transformer"));
            assertTrue(expected.getMessage().contains(point));
            assertTrue(expected.getMessage().contains(
                    MushroomSupportTransformer.TARGET));
        }
    }

    private static byte[] write(ClassNode node) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }
}
