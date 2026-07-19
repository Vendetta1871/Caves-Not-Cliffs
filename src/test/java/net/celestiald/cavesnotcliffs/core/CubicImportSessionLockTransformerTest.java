package net.celestiald.cavesnotcliffs.core;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CubicImportSessionLockTransformerTest {
    /** Internal (slash) form of the transformer target used for bytecode fixtures. */
    private static final String TARGET_INTERNAL =
            CubicImportSessionLockTransformer.TARGET.replace('.', '/');

    @Test
    public void developmentBytecodeHooksAfterSaveHandlerAndBeforeWorldInfo() throws Exception {
        byte[] transformed = new CubicImportSessionLockTransformer().transform(
                CubicImportSessionLockTransformer.TARGET,
                CubicImportSessionLockTransformer.TARGET, targetBytes());
        assertHookShape(transformed);
        Class<?> verified = new TargetLoader(transformed)
                .loadClass(CubicImportSessionLockTransformer.TARGET);
        assertEquals(CubicImportSessionLockTransformer.TARGET, verified.getName());
    }

    @Test
    public void srgLikeMethodNamesStillUseDescriptorAndOwnerAnchors() throws Exception {
        ClassNode node = readNode(targetBytes());
        for (MethodNode method : node.methods) {
            if (containsCall(method, CubicImportSessionLockTransformer.SAVE_FORMAT_OWNER,
                    CubicImportSessionLockTransformer.GET_SAVE_LOADER_DESC)) {
                method.name = "func_71247_a";
            }
        }
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);

        byte[] transformed = new CubicImportSessionLockTransformer().transform(
                CubicImportSessionLockTransformer.TARGET,
                CubicImportSessionLockTransformer.TARGET, writer.toByteArray());
        assertHookShape(transformed);
    }

    @Test
    public void duplicateSaveLoaderCallFailsClearlyInsteadOfMovingTheFormatHook()
            throws Exception {
        ClassNode node = readNode(targetBytes());
        TypeInsnNode demoWorld = null;
        MethodNode loadWorlds = null;
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof TypeInsnNode
                        && instruction.getOpcode() == Opcodes.NEW
                        && CubicImportSessionLockTransformer.DEMO_WORLD_OWNER.equals(
                            ((TypeInsnNode) instruction).desc)) {
                    demoWorld = (TypeInsnNode) instruction;
                    loadWorlds = method;
                }
            }
        }
        assertNotNull(demoWorld);
        assertNotNull(loadWorlds);

        InsnList collision = new InsnList();
        collision.add(new InsnNode(Opcodes.ACONST_NULL));
        collision.add(new InsnNode(Opcodes.ACONST_NULL));
        collision.add(new InsnNode(Opcodes.ICONST_0));
        collision.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                CubicImportSessionLockTransformer.SAVE_FORMAT_OWNER,
                "getSaveLoader",
                CubicImportSessionLockTransformer.GET_SAVE_LOADER_DESC, true));
        collision.add(new InsnNode(Opcodes.POP));
        loadWorlds.instructions.insertBefore(demoWorld, collision);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);

        try {
            new CubicImportSessionLockTransformer().transform(
                    CubicImportSessionLockTransformer.TARGET,
                    CubicImportSessionLockTransformer.TARGET, writer.toByteArray());
            fail("A near-collision must abort instead of moving the format hook");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("multiple save-loader calls"));
        }
    }

    @Test
    public void forgeLifecycleHandlerNoLongerMutatesSaveBeforeSessionLock() throws Exception {
        assertEquals(0, hookCalls(classBytes(CavesNotCliffs.class)));
    }

    @Test
    public void changedServerShapeFailsClearly() {
        ClassNode node = new ClassNode(Opcodes.ASM5);
        node.version = Opcodes.V1_8;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = TARGET_INTERNAL;
        node.superName = "java/lang/Object";
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        try {
            new CubicImportSessionLockTransformer().transform(
                    CubicImportSessionLockTransformer.TARGET,
                    CubicImportSessionLockTransformer.TARGET, writer.toByteArray());
            fail("Missing save-handler ordering must abort the transform");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("session-lock transformer"));
        }
    }

    @Test
    public void optifineSplitShapeHooksBothBranches() {
        byte[] transformed = new CubicImportSessionLockTransformer().transform(
                CubicImportSessionLockTransformer.TARGET,
                CubicImportSessionLockTransformer.TARGET, optifineSplitBytes());
        assertOptifineHookShape(transformed);

        byte[] retransformed = new CubicImportSessionLockTransformer().transform(
                CubicImportSessionLockTransformer.TARGET,
                CubicImportSessionLockTransformer.TARGET, transformed);
        assertTrue("already-transformed bytes must pass through unchanged",
                java.util.Arrays.equals(transformed, retransformed));
    }

    /** Asserts two world-format hooks: OptiFine Forge branch and vanilla merge. */
    private static void assertOptifineHookShape(byte[] bytes) {
        int importHooks = 0;
        int formatHooks = 0;
        int newFormatHooks = 0;
        boolean sawBranchHookAfterSplit = false;
        boolean sawMergeHook = false;
        for (MethodNode method : readNode(bytes).methods) {
            if (!CubicImportSessionLockTransformer.LOAD_WORLDS_DESC.equals(method.desc)) {
                continue;
            }
            AbstractInsnNode[] instructions = method.instructions.toArray();
            for (int index = 0; index < instructions.length; index++) {
                AbstractInsnNode instruction = instructions[index];
                if (!(instruction instanceof MethodInsnNode)) {
                    continue;
                }
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (CubicImportSessionLockTransformer.HOOK_OWNER.equals(call.owner)) {
                    importHooks++;
                } else if (CubicImportSessionLockTransformer.FORMAT_HOOK_OWNER.equals(
                        call.owner)) {
                    if (CubicImportSessionLockTransformer.NEW_FORMAT_HOOK_NAME.equals(
                            call.name)) {
                        newFormatHooks++;
                        continue;
                    }
                    formatHooks++;
                    AbstractInsnNode worldInfo = previousMeaningful(
                            previousMeaningful(call));
                    assertEquals(Opcodes.ALOAD, worldInfo.getOpcode());
                    if (index >= 3 && instructions[index - 3] instanceof JumpInsnNode
                            && instructions[index - 3].getOpcode() == Opcodes.IFEQ) {
                        sawBranchHookAfterSplit = true;
                    } else {
                        sawMergeHook = true;
                    }
                }
            }
        }
        assertEquals(1, importHooks);
        assertEquals(2, formatHooks);
        assertEquals(1, newFormatHooks);
        assertTrue("format hook must open the OptiFine Forge branch",
                sawBranchHookAfterSplit);
        assertTrue("format hook must stay at the vanilla merge", sawMergeHook);
    }

    /** Mirrors OptiFine's IntegratedServer rewrite around the WorldInfo null check. */
    private static byte[] optifineSplitBytes() {
        ClassNode node = new ClassNode(Opcodes.ASM5);
        node.version = Opcodes.V1_8;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = TARGET_INTERNAL;
        node.superName = "java/lang/Object";

        MethodNode method = new MethodNode(Opcodes.ASM5, Opcodes.ACC_PUBLIC, "loadAllWorlds",
                CubicImportSessionLockTransformer.LOAD_WORLDS_DESC, null, null);
        LabelNode vanillaBranch = new LabelNode();
        LabelNode existingSave = new LabelNode();
        LabelNode merge = new LabelNode();
        LabelNode end = new LabelNode();
        InsnList code = method.instructions;
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, TARGET_INTERNAL,
                "getActiveAnvilConverter",
                "()L" + CubicImportSessionLockTransformer.SAVE_FORMAT_OWNER + ";", false));
        code.add(new VarInsnNode(Opcodes.ALOAD, 1));
        code.add(new InsnNode(Opcodes.ICONST_1));
        code.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                CubicImportSessionLockTransformer.SAVE_FORMAT_OWNER, "getSaveLoader",
                CubicImportSessionLockTransformer.GET_SAVE_LOADER_DESC, true));
        code.add(new VarInsnNode(Opcodes.ASTORE, 7));
        code.add(new VarInsnNode(Opcodes.ALOAD, 7));
        code.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                CubicImportSessionLockTransformer.SAVE_HANDLER_OWNER, "loadWorldInfo",
                CubicImportSessionLockTransformer.LOAD_WORLD_INFO_DESC, true));
        code.add(new VarInsnNode(Opcodes.ASTORE, 8));
        code.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETSTATIC,
                CubicImportSessionLockTransformer.OPTIFINE_REFLECTOR_OWNER,
                "DimensionManager",
                "L" + CubicImportSessionLockTransformer.OPTIFINE_REFLECTOR_CLASS_OWNER
                        + ";"));
        code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                CubicImportSessionLockTransformer.OPTIFINE_REFLECTOR_CLASS_OWNER,
                "exists", CubicImportSessionLockTransformer.OPTIFINE_EXISTS_DESC, false));
        code.add(new JumpInsnNode(Opcodes.IFEQ, vanillaBranch));
        code.add(new VarInsnNode(Opcodes.ALOAD, 8));
        code.add(new InsnNode(Opcodes.POP));
        code.add(new JumpInsnNode(Opcodes.GOTO, end));
        code.add(vanillaBranch);
        code.add(new VarInsnNode(Opcodes.ALOAD, 8));
        code.add(new JumpInsnNode(Opcodes.IFNONNULL, existingSave));
        code.add(new TypeInsnNode(Opcodes.NEW,
                CubicImportSessionLockTransformer.WORLD_INFO_OWNER));
        code.add(new InsnNode(Opcodes.DUP));
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD, TARGET_INTERNAL,
                "worldSettings", "Lnet/minecraft/world/WorldSettings;"));
        code.add(new VarInsnNode(Opcodes.ALOAD, 2));
        code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                CubicImportSessionLockTransformer.WORLD_INFO_OWNER, "<init>",
                CubicImportSessionLockTransformer.WORLD_INFO_INIT_DESC, false));
        code.add(new VarInsnNode(Opcodes.ASTORE, 8));
        code.add(new JumpInsnNode(Opcodes.GOTO, merge));
        code.add(existingSave);
        code.add(new VarInsnNode(Opcodes.ALOAD, 8));
        code.add(new VarInsnNode(Opcodes.ALOAD, 2));
        code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                CubicImportSessionLockTransformer.WORLD_INFO_OWNER, "setWorldName",
                "(Ljava/lang/String;)V", false));
        code.add(merge);
        code.add(new InsnNode(Opcodes.RETURN));
        code.add(end);
        code.add(new InsnNode(Opcodes.RETURN));
        node.methods.add(method);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static void assertHookShape(byte[] bytes) {
        int saveLoaderCalls = 0;
        int worldInfoCalls = 0;
        int hooks = 0;
        int formatHooks = 0;
        int demoWorldAllocations = 0;
        for (MethodNode method : readNode(bytes).methods) {
            boolean sawSaveLoader = false;
            boolean sawHook = false;
            boolean sawFormatHook = false;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof TypeInsnNode
                        && instruction.getOpcode() == Opcodes.NEW
                        && CubicImportSessionLockTransformer.DEMO_WORLD_OWNER.equals(
                            ((TypeInsnNode) instruction).desc)) {
                    demoWorldAllocations++;
                    assertTrue("world format must be selected before provider construction",
                            sawFormatHook);
                }
                if (!(instruction instanceof MethodInsnNode)) {
                    continue;
                }
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (CubicImportSessionLockTransformer.SAVE_FORMAT_OWNER.equals(call.owner)
                        && CubicImportSessionLockTransformer.GET_SAVE_LOADER_DESC.equals(call.desc)) {
                    saveLoaderCalls++;
                    sawSaveLoader = true;
                    AbstractInsnNode first = nextMeaningful(call);
                    AbstractInsnNode second = nextMeaningful(first);
                    assertEquals(Opcodes.ALOAD, first.getOpcode());
                    assertTrue(second instanceof MethodInsnNode);
                    MethodInsnNode hook = (MethodInsnNode) second;
                    assertEquals(CubicImportSessionLockTransformer.HOOK_OWNER, hook.owner);
                    assertEquals(CubicImportSessionLockTransformer.HOOK_NAME, hook.name);
                    assertEquals(CubicImportSessionLockTransformer.HOOK_DESC, hook.desc);
                } else if (CubicImportSessionLockTransformer.HOOK_OWNER.equals(call.owner)
                        && CubicImportSessionLockTransformer.HOOK_NAME.equals(call.name)) {
                    hooks++;
                    assertTrue(sawSaveLoader);
                    sawHook = true;
                } else if (CubicImportSessionLockTransformer.SAVE_HANDLER_OWNER.equals(call.owner)
                        && CubicImportSessionLockTransformer.LOAD_WORLD_INFO_DESC.equals(call.desc)) {
                    worldInfoCalls++;
                    assertTrue(sawHook);
                } else if (CubicImportSessionLockTransformer.FORMAT_HOOK_OWNER.equals(call.owner)
                        && CubicImportSessionLockTransformer.FORMAT_HOOK_NAME.equals(call.name)) {
                    formatHooks++;
                    AbstractInsnNode saveHandler = previousMeaningful(call);
                    AbstractInsnNode worldInfo = previousMeaningful(saveHandler);
                    assertEquals(Opcodes.ALOAD, worldInfo.getOpcode());
                    assertEquals(Opcodes.ALOAD, saveHandler.getOpcode());
                    assertTrue(sawHook);
                    sawFormatHook = true;
                }
            }
        }
        assertEquals(1, saveLoaderCalls);
        assertEquals(1, worldInfoCalls);
        assertEquals(1, hooks);
        assertEquals(1, formatHooks);
        assertEquals(1, demoWorldAllocations);
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

    private static int hookCalls(byte[] bytes) {
        int result = 0;
        for (MethodNode method : readNode(bytes).methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (CubicImportSessionLockTransformer.HOOK_OWNER.equals(call.owner)
                            && CubicImportSessionLockTransformer.HOOK_NAME.equals(call.name)) {
                        result++;
                    }
                }
            }
        }
        return result;
    }

    private static AbstractInsnNode nextMeaningful(AbstractInsnNode instruction) {
        AbstractInsnNode cursor = instruction.getNext();
        while (cursor != null && cursor.getOpcode() < 0) {
            cursor = cursor.getNext();
        }
        assertNotNull(cursor);
        return cursor;
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
        return classBytes(TARGET_INTERNAL + ".class");
    }

    private static byte[] classBytes(Class<?> type) throws IOException {
        return classBytes(type.getName().replace('.', '/') + ".class");
    }

    private static byte[] classBytes(String resource) throws IOException {
        InputStream input = CubicImportSessionLockTransformerTest.class
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

    private static final class TargetLoader extends ClassLoader {
        private final byte[] bytes;

        private TargetLoader(byte[] bytes) {
            super(CubicImportSessionLockTransformerTest.class.getClassLoader());
            this.bytes = bytes;
        }

        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException {
            if (!CubicImportSessionLockTransformer.TARGET.equals(name)) {
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
