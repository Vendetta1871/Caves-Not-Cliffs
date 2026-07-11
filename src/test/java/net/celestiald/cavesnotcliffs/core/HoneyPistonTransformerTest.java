package net.celestiald.cavesnotcliffs.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
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
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HoneyPistonTransformerTest {
    @Test
    public void developmentBytecodeGetsExactlyTwoNarrowHooksAndVerifies() throws Exception {
        byte[] transformed = new HoneyPistonTransformer().transform(
                HoneyPistonTransformer.TARGET, HoneyPistonTransformer.TARGET,
                targetBytes());
        assertHookCounts(transformed);
        Class<?> verified = new TargetLoader(transformed)
                .loadClass(HoneyPistonTransformer.TARGET);
        assertEquals(HoneyPistonTransformer.TARGET, verified.getName());
        assertTrue(verified.getDeclaredConstructors().length > 0);
    }

    @Test
    public void srgLikeRenamedMembersUseTheSameDescriptorDrivenPath() throws Exception {
        byte[] renamed = renameTargetMembers(targetBytes());
        byte[] transformed = new HoneyPistonTransformer().transform(
                HoneyPistonTransformer.TARGET, HoneyPistonTransformer.TARGET,
                renamed);
        assertHookCounts(transformed);
        new TargetLoader(transformed).loadClass(HoneyPistonTransformer.TARGET)
                .getDeclaredMethods();
    }

    @Test
    public void aChangedForgePatchFailsClearlyInsteadOfSilentlyMisbehaving() {
        ClassNode node = new ClassNode(Opcodes.ASM5);
        node.version = Opcodes.V1_8;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = HoneyPistonTransformer.TARGET_INTERNAL;
        node.superName = "java/lang/Object";
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        try {
            new HoneyPistonTransformer().transform(HoneyPistonTransformer.TARGET,
                    HoneyPistonTransformer.TARGET, writer.toByteArray());
            fail("Missing piston shape must abort the transform");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("piston transformer"));
        }
    }

    @Test
    public void corePluginDeclaresTransformerAndPostForgeSortingIndex() {
        CavesNotCliffsCorePlugin plugin = new CavesNotCliffsCorePlugin();
        assertEquals(HoneyPistonTransformer.class.getName(),
                plugin.getASMTransformerClass()[0]);
        assertEquals(SchemaOnePopulationTransformer.class.getName(),
                plugin.getASMTransformerClass()[1]);
        assertEquals(2, plugin.getASMTransformerClass().length);
        IFMLLoadingPlugin.SortingIndex sorting =
                CavesNotCliffsCorePlugin.class.getAnnotation(
                        IFMLLoadingPlugin.SortingIndex.class);
        assertNotNull(sorting);
        assertEquals(1001, sorting.value());
    }

    private static void assertHookCounts(byte[] bytes) {
        final Map<String, Integer> counts = new HashMap<>();
        ClassNode node = readNode(bytes);
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode)) {
                    continue;
                }
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.getOpcode() == Opcodes.INVOKESTATIC
                        && HoneyPistonTransformer.HOOK_OWNER.equals(call.owner)) {
                    counts.put(call.name, counts.containsKey(call.name)
                            ? counts.get(call.name) + 1 : 1);
                }
            }
        }
        assertEquals(Integer.valueOf(1), counts.get("canStickBehind"));
        assertEquals(Integer.valueOf(1), counts.get("canAttachBranch"));
        assertEquals(2, counts.size());
    }

    private static byte[] renameTargetMembers(byte[] bytes) {
        ClassNode node = readNode(bytes);
        Map<String, String> fields = new HashMap<>();
        int fieldIndex = 0;
        for (FieldNode field : node.fields) {
            if (HoneyPistonTransformer.WORLD_DESC.equals(field.desc)
                    || HoneyPistonTransformer.FACING_DESC.equals(field.desc)) {
                String renamed = "field_1772" + fieldIndex++ + "_srg";
                fields.put(field.name, renamed);
                field.name = renamed;
            }
        }
        Map<String, String> methods = new HashMap<>();
        for (MethodNode method : node.methods) {
            if ((method.access & Opcodes.ACC_PRIVATE) == 0) {
                continue;
            }
            if (HoneyPistonTransformer.ADD_LINE_DESC.equals(method.desc)) {
                methods.put(method.name + method.desc, "func_177251_a");
                method.name = "func_177251_a";
            } else if (HoneyPistonTransformer.BRANCH_DESC.equals(method.desc)) {
                methods.put(method.name + method.desc, "func_177250_b");
                method.name = "func_177250_b";
            }
        }
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof FieldInsnNode) {
                    FieldInsnNode field = (FieldInsnNode) instruction;
                    if (HoneyPistonTransformer.TARGET_INTERNAL.equals(field.owner)
                            && fields.containsKey(field.name)) {
                        field.name = fields.get(field.name);
                    }
                } else if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    String renamed = methods.get(call.name + call.desc);
                    if (HoneyPistonTransformer.TARGET_INTERNAL.equals(call.owner)
                            && renamed != null) {
                        call.name = renamed;
                    }
                }
            }
        }
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static ClassNode readNode(byte[] bytes) {
        ClassNode node = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    private static byte[] targetBytes() throws IOException {
        String path = "/" + HoneyPistonTransformer.TARGET_INTERNAL + ".class";
        InputStream input = HoneyPistonTransformerTest.class.getResourceAsStream(path);
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
            super(HoneyPistonTransformerTest.class.getClassLoader());
            this.bytes = bytes;
        }

        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException {
            if (!HoneyPistonTransformer.TARGET.equals(name)) {
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
