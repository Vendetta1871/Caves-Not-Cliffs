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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SchemaOnePopulationTransformerTest {
    @Test
    public void developmentBytecodeGetsExactlyOneImmediatePostForgeHook()
            throws Exception {
        byte[] transformed = new SchemaOnePopulationTransformer().transform(
                SchemaOnePopulationTransformer.TARGET,
                SchemaOnePopulationTransformer.TARGET, targetBytes());

        assertHookShape(transformed);
        Class<?> verified = new TargetLoader(transformed)
                .loadClass(SchemaOnePopulationTransformer.TARGET);
        assertEquals(SchemaOnePopulationTransformer.TARGET, verified.getName());
    }

    @Test
    public void srgLikePopulateNameUsesTheSameDescriptorAndCallAnchors() throws Exception {
        ClassNode node = readNode(targetBytes());
        for (MethodNode method : node.methods) {
            if (SchemaOnePopulationTransformer.POPULATE_DESC.equals(method.desc)) {
                method.name = "func_186034_a";
            }
        }
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);

        byte[] transformed = new SchemaOnePopulationTransformer().transform(
                SchemaOnePopulationTransformer.TARGET,
                SchemaOnePopulationTransformer.TARGET, writer.toByteArray());
        assertHookShape(transformed);
    }

    @Test
    public void ignoresMixinInvokerWithTheSameDescriptorButNoForgeCall() throws Exception {
        ClassNode node = readNode(targetBytes());
        MethodNode invoker = new MethodNode(Opcodes.ASM5, Opcodes.ACC_PUBLIC,
                "cavebiomes$populate", SchemaOnePopulationTransformer.POPULATE_DESC,
                null, null);
        invoker.instructions.add(new InsnNode(Opcodes.RETURN));
        node.methods.add(invoker);
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);

        byte[] transformed = new SchemaOnePopulationTransformer().transform(
                SchemaOnePopulationTransformer.TARGET,
                SchemaOnePopulationTransformer.TARGET, writer.toByteArray());
        assertHookShape(transformed);
    }

    @Test
    public void changedForgeShapeFailsClearlyInsteadOfSkippingCompletion() {
        ClassNode node = new ClassNode(Opcodes.ASM5);
        node.version = Opcodes.V1_8;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = SchemaOnePopulationTransformer.TARGET_INTERNAL;
        node.superName = "java/lang/Object";
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        try {
            new SchemaOnePopulationTransformer().transform(
                    SchemaOnePopulationTransformer.TARGET,
                    SchemaOnePopulationTransformer.TARGET, writer.toByteArray());
            fail("Missing chunk-population shape must abort the transform");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("population transformer"));
        }
    }

    private static void assertHookShape(byte[] bytes) {
        int hooks = 0;
        int forgeCalls = 0;
        for (MethodNode method : readNode(bytes).methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode)) {
                    continue;
                }
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (SchemaOnePopulationTransformer.FORGE_OWNER.equals(call.owner)
                        && SchemaOnePopulationTransformer.FORGE_NAME.equals(call.name)
                        && SchemaOnePopulationTransformer.FORGE_DESC.equals(call.desc)) {
                    forgeCalls++;
                    AbstractInsnNode first = nextMeaningful(call);
                    AbstractInsnNode second = nextMeaningful(first);
                    assertEquals(Opcodes.ALOAD, first.getOpcode());
                    assertTrue(second instanceof MethodInsnNode);
                    MethodInsnNode hook = (MethodInsnNode) second;
                    assertEquals(SchemaOnePopulationTransformer.HOOK_OWNER, hook.owner);
                    assertEquals(SchemaOnePopulationTransformer.HOOK_NAME, hook.name);
                    assertEquals(SchemaOnePopulationTransformer.HOOK_DESC, hook.desc);
                }
                if (SchemaOnePopulationTransformer.HOOK_OWNER.equals(call.owner)
                        && SchemaOnePopulationTransformer.HOOK_NAME.equals(call.name)) {
                    hooks++;
                }
            }
        }
        assertEquals(1, forgeCalls);
        assertEquals(1, hooks);
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
        String path = "/" + SchemaOnePopulationTransformer.TARGET_INTERNAL + ".class";
        InputStream input = SchemaOnePopulationTransformerTest.class
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

        private TargetLoader(byte[] bytes) {
            super(SchemaOnePopulationTransformerTest.class.getClassLoader());
            this.bytes = bytes;
        }

        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException {
            if (!SchemaOnePopulationTransformer.TARGET.equals(name)) {
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
