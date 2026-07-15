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
    static final String SAVE_FORMAT_OWNER = "net/minecraft/world/storage/ISaveFormat";
    static final String SAVE_HANDLER_OWNER = "net/minecraft/world/storage/ISaveHandler";
    static final String GET_SAVE_LOADER_DESC = "(Ljava/lang/String;Z)L"
            + SAVE_HANDLER_OWNER + ";";
    static final String LOAD_WORLD_INFO_DESC = "()Lnet/minecraft/world/storage/WorldInfo;";
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

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !isTarget(name, transformedName)) {
            return basicClass;
        }
        ClassNode node = new ClassNode(Opcodes.ASM5);
        new ClassReader(basicClass).accept(node, 0);
        MethodNode loadWorlds = uniqueLoadWorldsMethod(node);
        MethodInsnNode saveLoader = uniqueSaveLoaderCall(loadWorlds);
        MethodInsnNode loadWorldInfo = uniqueWorldInfoCall(loadWorlds);
        int saveHandlerVariable = objectStoreAfter(saveLoader, "save handler");
        int worldInfoVariable = objectStoreAfter(loadWorldInfo, "WorldInfo");
        MethodInsnNode constructorDecision = uniqueConstructorDecision(loadWorlds, node.name);

        InsnList importHook = new InsnList();
        importHook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        importHook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
                HOOK_NAME, HOOK_DESC, false));
        loadWorlds.instructions.insert(saveLoader, importHook);

        InsnList formatHook = new InsnList();
        formatHook.add(new VarInsnNode(Opcodes.ALOAD, worldInfoVariable));
        formatHook.add(new VarInsnNode(Opcodes.ALOAD, saveHandlerVariable));
        formatHook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, FORMAT_HOOK_OWNER,
                FORMAT_HOOK_NAME, FORMAT_HOOK_DESC, false));
        loadWorlds.instructions.insertBefore(constructorDecision, formatHook);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static boolean isTarget(String name, String transformedName) {
        return TARGET.equals(name) || TARGET.equals(transformedName)
                || INTEGRATED_TARGET.equals(name) || INTEGRATED_TARGET.equals(transformedName);
    }

    private static MethodNode uniqueLoadWorldsMethod(ClassNode node) {
        MethodNode result = null;
        for (MethodNode method : node.methods) {
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

    private static MethodInsnNode uniqueConstructorDecision(MethodNode method,
            String declaringOwner) {
        TypeInsnNode demoWorld = null;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof TypeInsnNode
                    && instruction.getOpcode() == Opcodes.NEW
                    && DEMO_WORLD_OWNER.equals(((TypeInsnNode) instruction).desc)) {
                if (demoWorld != null) {
                    throw failure("multiple demo Overworld constructor allocations in "
                            + method.name + method.desc);
                }
                demoWorld = (TypeInsnNode) instruction;
            }
        }
        if (demoWorld == null) {
            throw failure("demo Overworld constructor allocation in "
                    + method.name + method.desc);
        }
        for (AbstractInsnNode instruction = demoWorld.getPrevious(); instruction != null;
                instruction = instruction.getPrevious()) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (declaringOwner.equals(call.owner) && "()Z".equals(call.desc)) {
                    verifyConstructorBranch(call, demoWorld, method);
                    return call;
                }
            }
        }
        throw failure("Overworld constructor selection in " + method.name + method.desc);
    }

    private static void verifyConstructorBranch(MethodInsnNode decision,
            TypeInsnNode demoWorld, MethodNode method) {
        AbstractInsnNode branchNode = nextMeaningful(decision);
        if (!(branchNode instanceof JumpInsnNode)
                || branchNode.getOpcode() != Opcodes.IFEQ) {
            throw failure("immediate demo Overworld branch in " + method.name + method.desc);
        }
        JumpInsnNode branch = (JumpInsnNode) branchNode;
        boolean demoInFallthrough = false;
        for (AbstractInsnNode cursor = branch.getNext(); cursor != null && cursor != branch.label;
                cursor = cursor.getNext()) {
            if (cursor == demoWorld) {
                demoInFallthrough = true;
            }
        }
        if (!demoInFallthrough) {
            throw failure("demo Overworld fallthrough in " + method.name + method.desc);
        }
        AbstractInsnNode normalWorld = nextMeaningful(branch.label);
        if (!(normalWorld instanceof TypeInsnNode)
                || normalWorld.getOpcode() != Opcodes.NEW
                || !NORMAL_WORLD_OWNER.equals(((TypeInsnNode) normalWorld).desc)) {
            throw failure("normal Overworld branch in " + method.name + method.desc);
        }
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

    private static IllegalStateException failure(String point) {
        return new IllegalStateException("Caves Not Cliffs cubic import session-lock transformer "
                + "could not verify " + point);
    }
}
