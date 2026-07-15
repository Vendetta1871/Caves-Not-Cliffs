package net.celestiald.cavesnotcliffs.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Runs save import and world-format selection before vanilla constructs the Overworld. */
public final class CubicImportSessionLockTransformer implements IClassTransformer {
    public static final String TARGET = "net.minecraft.server.MinecraftServer";
    public static final String INTEGRATED_TARGET =
            "net.minecraft.server.integrated.IntegratedServer";
    public static final String CLIENT_TARGET = "net.minecraft.client.Minecraft";
    static final String SAVE_FORMAT_OWNER = "net/minecraft/world/storage/ISaveFormat";
    static final String SAVE_HANDLER_OWNER = "net/minecraft/world/storage/ISaveHandler";
    static final String GET_SAVE_LOADER_DESC = "(Ljava/lang/String;Z)L"
            + SAVE_HANDLER_OWNER + ";";
    static final String LOAD_WORLD_INFO_DESC = "()Lnet/minecraft/world/storage/WorldInfo;";
    static final String WORLD_INFO_OWNER = "net/minecraft/world/storage/WorldInfo";
    static final String LOAD_WORLDS_DESC = "(Ljava/lang/String;Ljava/lang/String;J"
            + "Lnet/minecraft/world/WorldType;Ljava/lang/String;)V";
    static final String DEMO_WORLD_OWNER = "net/minecraft/world/WorldServerDemo";
    static final String NORMAL_WORLD_OWNER = "net/minecraft/world/WorldServer";
    static final String HOOK_OWNER =
            "net/celestiald/cavesnotcliffs/migration/LegacyCubicSaveImporter";
    static final String HOOK_NAME = "prepareForServer";
    static final String HOOK_DESC = "(Lnet/minecraft/server/MinecraftServer;)V";
    static final String FORMAT_HOOK_OWNER =
            "net/celestiald/cavesnotcliffs/world/WorldHeightBootstrap";
    static final String FORMAT_HOOK_NAME = "prepareBeforeWorldConstruction";
    static final String FORMAT_HOOK_DESC = "(Lnet/minecraft/world/storage/WorldInfo;L"
            + SAVE_HANDLER_OWNER + ";)V";
    static final String NEW_FORMAT_HOOK_NAME = "prepareNewWorld";
    static final String NEW_FORMAT_HOOK_DESC =
            "(Lnet/minecraft/world/storage/WorldInfo;)V";
    static final String WORLD_INFO_INIT_DESC =
            "(Lnet/minecraft/world/WorldSettings;Ljava/lang/String;)V";
    static final String CLIENT_LOAD_WORLD_DESC =
            "(Ljava/lang/String;Ljava/lang/String;Lnet/minecraft/world/WorldSettings;)V";
    static final String SAVE_WORLD_INFO_DESC =
            "(Lnet/minecraft/world/storage/WorldInfo;)V";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return basicClass;
        }
        try {
            if (isClientTarget(name, transformedName)) {
                return transformClientWorldCreation(basicClass);
            }
            if (!isTarget(name, transformedName)) {
                return basicClass;
            }
            ClassNode node = new ClassNode(Opcodes.ASM5);
            new ClassReader(basicClass).accept(node, 0);
            MethodNode loadWorlds = uniqueLoadWorldsMethod(node);
            MethodInsnNode saveLoader = uniqueSaveLoaderCall(loadWorlds);
            int importHookCount = countCalls(loadWorlds, HOOK_OWNER, HOOK_NAME, HOOK_DESC);
            int formatHookCount = countCalls(loadWorlds, FORMAT_HOOK_OWNER,
                    FORMAT_HOOK_NAME, FORMAT_HOOK_DESC);
            int newFormatHookCount = countCalls(loadWorlds, FORMAT_HOOK_OWNER,
                    NEW_FORMAT_HOOK_NAME, NEW_FORMAT_HOOK_DESC);
            if (importHookCount == 1 && formatHookCount == 1
                    && newFormatHookCount == 1) {
                verifyAppliedHooks(loadWorlds, saveLoader);
                return basicClass;
            }
            if (importHookCount != 0 || formatHookCount != 0 || newFormatHookCount != 0) {
                throw failure("partial or duplicate world bootstrap hooks in "
                        + loadWorlds.name + loadWorlds.desc);
            }
            MethodInsnNode loadWorldInfo = uniqueWorldInfoCall(loadWorlds);
            int saveHandlerVariable = objectStoreAfter(saveLoader, "save handler");
            int worldInfoVariable = objectStoreAfter(loadWorldInfo, "WorldInfo");
            WorldInfoFlow worldInfoFlow = uniqueWorldInfoFlow(
                    loadWorlds, loadWorldInfo, worldInfoVariable);

            InsnList importHook = new InsnList();
            importHook.add(new VarInsnNode(Opcodes.ALOAD, 0));
            importHook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
                    HOOK_NAME, HOOK_DESC, false));
            loadWorlds.instructions.insert(saveLoader, importHook);

            InsnList newFormatHook = new InsnList();
            newFormatHook.add(new VarInsnNode(Opcodes.ALOAD, worldInfoVariable));
            newFormatHook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, FORMAT_HOOK_OWNER,
                    NEW_FORMAT_HOOK_NAME, NEW_FORMAT_HOOK_DESC, false));
            loadWorlds.instructions.insert(worldInfoFlow.newWorldInfoStore, newFormatHook);

            InsnList formatHook = new InsnList();
            formatHook.add(new VarInsnNode(Opcodes.ALOAD, worldInfoVariable));
            formatHook.add(new VarInsnNode(Opcodes.ALOAD, saveHandlerVariable));
            formatHook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, FORMAT_HOOK_OWNER,
                    FORMAT_HOOK_NAME, FORMAT_HOOK_DESC, false));
            loadWorlds.instructions.insertBefore(worldInfoFlow.merge, formatHook);

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            node.accept(writer);
            return writer.toByteArray();
        } catch (RuntimeException exception) {
            System.err.println("[Caves Not Cliffs] Failed to transform " + transformedName
                    + ": " + exception.getMessage());
            exception.printStackTrace(System.err);
            throw exception;
        }
    }

    /** Activates a new single-player save before the client writes its initial level.dat. */
    private static byte[] transformClientWorldCreation(byte[] basicClass) {
        ClassNode node = new ClassNode(Opcodes.ASM5);
        new ClassReader(basicClass).accept(node, 0);
        MethodNode launchIntegratedServer = null;
        for (MethodNode method : node.methods) {
            if (CLIENT_LOAD_WORLD_DESC.equals(method.desc)
                    && hasClientNewWorldFlow(method)) {
                if (launchIntegratedServer != null) {
                    throw failure("multiple client integrated-server launch methods");
                }
                launchIntegratedServer = method;
            }
        }
        if (launchIntegratedServer == null) {
            throw failure("client integrated-server launch method");
        }

        MethodInsnNode loadWorldInfo = findUniqueCall(launchIntegratedServer,
                SAVE_HANDLER_OWNER, LOAD_WORLD_INFO_DESC, "client WorldInfo load");
        MethodInsnNode constructor = findUniqueCall(launchIntegratedServer,
                WORLD_INFO_OWNER, WORLD_INFO_INIT_DESC, "client new WorldInfo constructor");
        MethodInsnNode saveWorldInfo = findUniqueCall(launchIntegratedServer,
                SAVE_HANDLER_OWNER, SAVE_WORLD_INFO_DESC, "client initial WorldInfo save");
        if (loadWorldInfo == null || constructor == null || saveWorldInfo == null
                || !appearsBefore(loadWorldInfo, constructor)
                || !appearsBefore(constructor, saveWorldInfo)) {
            throw failure("ordered client new-save WorldInfo branch");
        }
        int worldInfoVariable = objectStoreAfter(constructor, "client new WorldInfo");
        int hookCount = countCalls(launchIntegratedServer, FORMAT_HOOK_OWNER,
                NEW_FORMAT_HOOK_NAME, NEW_FORMAT_HOOK_DESC);
        if (hookCount == 1) {
            verifyClientHook(constructor, worldInfoVariable);
            return basicClass;
        }
        if (hookCount != 0) {
            throw failure("duplicate client new-world format hooks");
        }

        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, worldInfoVariable));
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, FORMAT_HOOK_OWNER,
                NEW_FORMAT_HOOK_NAME, NEW_FORMAT_HOOK_DESC, false));
        launchIntegratedServer.instructions.insert(
                nextMeaningful(constructor), hook);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static boolean hasClientNewWorldFlow(MethodNode method) {
        return countCalls(method, SAVE_HANDLER_OWNER, LOAD_WORLD_INFO_DESC) == 1
                && countCalls(method, WORLD_INFO_OWNER, WORLD_INFO_INIT_DESC) == 1
                && countCalls(method, SAVE_HANDLER_OWNER, SAVE_WORLD_INFO_DESC) == 1;
    }

    private static void verifyClientHook(MethodInsnNode constructor, int worldInfoVariable) {
        AbstractInsnNode store = nextMeaningful(constructor);
        AbstractInsnNode load = nextMeaningful(store);
        AbstractInsnNode hook = nextMeaningful(load);
        if (!(load instanceof VarInsnNode) || load.getOpcode() != Opcodes.ALOAD
                || ((VarInsnNode) load).var != worldInfoVariable
                || !isCall(hook, FORMAT_HOOK_OWNER,
                        NEW_FORMAT_HOOK_NAME, NEW_FORMAT_HOOK_DESC)) {
            throw failure("client new-world format hook placement");
        }
    }

    private static boolean isTarget(String name, String transformedName) {
        return TARGET.equals(name) || TARGET.equals(transformedName)
                || INTEGRATED_TARGET.equals(name) || INTEGRATED_TARGET.equals(transformedName);
    }

    private static boolean isClientTarget(String name, String transformedName) {
        return CLIENT_TARGET.equals(name) || CLIENT_TARGET.equals(transformedName);
    }

    private static MethodNode uniqueLoadWorldsMethod(ClassNode node) {
        MethodNode result = null;
        for (MethodNode method : node.methods) {
            if (!LOAD_WORLDS_DESC.equals(method.desc)) {
                continue;
            }
            MethodInsnNode saveLoader = findUniqueCall(method, SAVE_FORMAT_OWNER,
                    GET_SAVE_LOADER_DESC, "save-loader");
            if (saveLoader == null || !hasLaterCall(method, saveLoader,
                    SAVE_HANDLER_OWNER, LOAD_WORLD_INFO_DESC)) {
                continue;
            }
            if (result != null) {
                throw failure("multiple world-loading methods with save-handler ordering");
            }
            result = method;
        }
        if (result == null) {
            throw failure("world-loading method with save-handler then WorldInfo reads");
        }
        return result;
    }

    private static MethodInsnNode uniqueSaveLoaderCall(MethodNode method) {
        MethodInsnNode result = findUniqueCall(method, SAVE_FORMAT_OWNER,
                GET_SAVE_LOADER_DESC, "save-loader");
        if (result == null) {
            throw failure("save-handler creation in " + method.name + method.desc);
        }
        return result;
    }

    private static MethodInsnNode uniqueWorldInfoCall(MethodNode method) {
        MethodInsnNode result = findUniqueCall(method, SAVE_HANDLER_OWNER,
                LOAD_WORLD_INFO_DESC, "WorldInfo load");
        if (result == null) {
            throw failure("WorldInfo load in " + method.name + method.desc);
        }
        return result;
    }

    private static int objectStoreAfter(AbstractInsnNode instruction, String description) {
        AbstractInsnNode next = nextMeaningful(instruction);
        if (!(next instanceof VarInsnNode) || next.getOpcode() != Opcodes.ASTORE) {
            throw failure(description + " local store");
        }
        return ((VarInsnNode) next).var;
    }

    private static void verifyAppliedHooks(MethodNode method, MethodInsnNode saveLoader) {
        AbstractInsnNode importLoad = nextMeaningful(saveLoader);
        AbstractInsnNode importCall = nextMeaningful(importLoad);
        if (!(importLoad instanceof VarInsnNode) || importLoad.getOpcode() != Opcodes.ALOAD
                || ((VarInsnNode) importLoad).var != 0
                || !isCall(importCall, HOOK_OWNER, HOOK_NAME, HOOK_DESC)) {
            throw failure("import hook placement in " + method.name + method.desc);
        }
        int saveHandlerVariable = objectStoreAfter(importCall, "save handler");
        MethodInsnNode loadWorldInfo = uniqueWorldInfoCall(method);
        int worldInfoVariable = objectStoreAfter(loadWorldInfo, "WorldInfo");
        WorldInfoFlow worldInfoFlow = uniqueWorldInfoFlow(
                method, loadWorldInfo, worldInfoVariable);
        AbstractInsnNode newWorldInfoLoad = nextMeaningful(worldInfoFlow.newWorldInfoStore);
        AbstractInsnNode newFormatCall = nextMeaningful(newWorldInfoLoad);
        if (!(newWorldInfoLoad instanceof VarInsnNode)
                || newWorldInfoLoad.getOpcode() != Opcodes.ALOAD
                || ((VarInsnNode) newWorldInfoLoad).var != worldInfoVariable
                || !isCall(newFormatCall, FORMAT_HOOK_OWNER,
                        NEW_FORMAT_HOOK_NAME, NEW_FORMAT_HOOK_DESC)) {
            throw failure("new-world format hook placement in " + method.name + method.desc);
        }
        AbstractInsnNode worldInfoLoad = worldInfoFlow.merge;
        AbstractInsnNode saveHandlerLoad = nextMeaningful(worldInfoLoad);
        AbstractInsnNode formatCall = nextMeaningful(saveHandlerLoad);
        if (!isCall(formatCall, FORMAT_HOOK_OWNER, FORMAT_HOOK_NAME, FORMAT_HOOK_DESC)
                || !(saveHandlerLoad instanceof VarInsnNode)
                || saveHandlerLoad.getOpcode() != Opcodes.ALOAD
                || ((VarInsnNode) saveHandlerLoad).var != saveHandlerVariable
                || !(worldInfoLoad instanceof VarInsnNode)
                || worldInfoLoad.getOpcode() != Opcodes.ALOAD
                || ((VarInsnNode) worldInfoLoad).var != worldInfoVariable) {
            throw failure("world-format hook placement in " + method.name + method.desc);
        }
    }

    private static WorldInfoFlow uniqueWorldInfoFlow(MethodNode method,
            MethodInsnNode loadWorldInfo, int worldInfoVariable) {
        AbstractInsnNode store = nextMeaningful(loadWorldInfo);
        AbstractInsnNode load = nextMeaningful(store);
        if (!(load instanceof VarInsnNode) || load.getOpcode() != Opcodes.ALOAD
                || ((VarInsnNode) load).var != worldInfoVariable) {
            throw failure("WorldInfo null check in " + method.name + method.desc);
        }
        AbstractInsnNode branchNode = nextMeaningful(load);
        if (!(branchNode instanceof JumpInsnNode)
                || branchNode.getOpcode() != Opcodes.IFNONNULL) {
            throw failure("WorldInfo existing-save branch in " + method.name + method.desc);
        }

        JumpInsnNode existingSaveBranch = (JumpInsnNode) branchNode;
        boolean createdWorldInfo = false;
        boolean storedWorldInfo = false;
        VarInsnNode newWorldInfoStore = null;
        JumpInsnNode mergeJump = null;
        for (AbstractInsnNode cursor = existingSaveBranch.getNext(); cursor != null
                && cursor != existingSaveBranch.label; cursor = cursor.getNext()) {
            if (cursor.getOpcode() == Opcodes.NEW && cursor instanceof TypeInsnNode
                    && WORLD_INFO_OWNER.equals(((TypeInsnNode) cursor).desc)) {
                if (createdWorldInfo) {
                    throw failure("multiple new WorldInfo allocations in "
                            + method.name + method.desc);
                }
                createdWorldInfo = true;
            } else if (createdWorldInfo && cursor instanceof VarInsnNode
                    && cursor.getOpcode() == Opcodes.ASTORE
                    && ((VarInsnNode) cursor).var == worldInfoVariable) {
                storedWorldInfo = true;
                newWorldInfoStore = (VarInsnNode) cursor;
            } else if (storedWorldInfo && cursor instanceof JumpInsnNode
                    && cursor.getOpcode() == Opcodes.GOTO) {
                if (mergeJump != null) {
                    throw failure("multiple new/existing WorldInfo merges in "
                            + method.name + method.desc);
                }
                mergeJump = (JumpInsnNode) cursor;
            }
        }
        if (!createdWorldInfo || !storedWorldInfo || mergeJump == null) {
            throw failure("new/existing WorldInfo merge in " + method.name + method.desc);
        }
        verifyStraightLineFallthrough(existingSaveBranch.label, mergeJump.label, method);
        AbstractInsnNode insertionPoint = nextMeaningful(mergeJump.label);
        if (insertionPoint == null) {
            throw failure("WorldInfo merge insertion point in " + method.name + method.desc);
        }
        return new WorldInfoFlow(newWorldInfoStore, insertionPoint);
    }

    private static final class WorldInfoFlow {
        private final VarInsnNode newWorldInfoStore;
        private final AbstractInsnNode merge;

        private WorldInfoFlow(VarInsnNode newWorldInfoStore, AbstractInsnNode merge) {
            this.newWorldInfoStore = newWorldInfoStore;
            this.merge = merge;
        }
    }

    private static void verifyStraightLineFallthrough(AbstractInsnNode first,
            AbstractInsnNode second, MethodNode method) {
        for (AbstractInsnNode cursor = first; cursor != null; cursor = cursor.getNext()) {
            if (cursor == second) {
                return;
            }
            int opcode = cursor.getOpcode();
            if (cursor != first && (cursor instanceof JumpInsnNode
                    || opcode == Opcodes.TABLESWITCH || opcode == Opcodes.LOOKUPSWITCH
                    || opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN
                    || opcode == Opcodes.ATHROW)) {
                throw failure("existing-save WorldInfo path to merge in "
                        + method.name + method.desc);
            }
        }
        throw failure("ordered existing-save WorldInfo merge in "
                + method.name + method.desc);
    }

    private static boolean isCall(AbstractInsnNode instruction, String owner,
            String name, String descriptor) {
        if (!(instruction instanceof MethodInsnNode)) {
            return false;
        }
        MethodInsnNode call = (MethodInsnNode) instruction;
        return owner.equals(call.owner) && name.equals(call.name)
                && descriptor.equals(call.desc);
    }

    private static int countCalls(MethodNode method, String owner, String name,
            String descriptor) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (owner.equals(call.owner) && name.equals(call.name)
                        && descriptor.equals(call.desc)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int countCalls(MethodNode method, String owner, String descriptor) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (owner.equals(call.owner) && descriptor.equals(call.desc)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static AbstractInsnNode nextMeaningful(AbstractInsnNode instruction) {
        AbstractInsnNode cursor = instruction.getNext();
        while (cursor != null && cursor.getOpcode() < 0) {
            cursor = cursor.getNext();
        }
        return cursor;
    }

    private static MethodInsnNode findUniqueCall(MethodNode method, String owner,
            String descriptor, String description) {
        MethodInsnNode result = null;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) {
                continue;
            }
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (!owner.equals(call.owner) || !descriptor.equals(call.desc)) {
                continue;
            }
            if (result != null) {
                throw failure("multiple " + description + " calls in "
                        + method.name + method.desc);
            }
            result = call;
        }
        return result;
    }

    private static boolean hasLaterCall(MethodNode method, AbstractInsnNode after,
            String owner, String descriptor) {
        for (AbstractInsnNode instruction = after.getNext(); instruction != null;
                instruction = instruction.getNext()) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (owner.equals(call.owner) && descriptor.equals(call.desc)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean appearsBefore(AbstractInsnNode first, AbstractInsnNode second) {
        for (AbstractInsnNode instruction = first; instruction != null;
                instruction = instruction.getNext()) {
            if (instruction == second) {
                return true;
            }
        }
        return false;
    }

    private static IllegalStateException failure(String point) {
        return new IllegalStateException("Caves Not Cliffs cubic import session-lock transformer "
                + "could not verify " + point);
    }
}
