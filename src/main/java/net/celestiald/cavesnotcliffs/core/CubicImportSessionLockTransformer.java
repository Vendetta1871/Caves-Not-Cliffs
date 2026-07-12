package net.celestiald.cavesnotcliffs.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Runs cubic-save import after vanilla creates the save handler but before it reads WorldInfo. */
public final class CubicImportSessionLockTransformer implements IClassTransformer {
    public static final String TARGET = "net.minecraft.server.MinecraftServer";
    static final String TARGET_INTERNAL = TARGET.replace('.', '/');
    static final String SAVE_FORMAT_OWNER = "net/minecraft/world/storage/ISaveFormat";
    static final String SAVE_HANDLER_OWNER = "net/minecraft/world/storage/ISaveHandler";
    static final String GET_SAVE_LOADER_DESC = "(Ljava/lang/String;Z)L"
            + SAVE_HANDLER_OWNER + ";";
    static final String LOAD_WORLD_INFO_DESC = "()Lnet/minecraft/world/storage/WorldInfo;";
    static final String HOOK_OWNER =
            "net/celestiald/cavesnotcliffs/migration/LegacyCubicSaveImporter";
    static final String HOOK_NAME = "prepareForServer";
    static final String HOOK_DESC = "(Lnet/minecraft/server/MinecraftServer;)V";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !(TARGET.equals(transformedName) || TARGET.equals(name))) {
            return basicClass;
        }
        ClassNode node = new ClassNode(Opcodes.ASM5);
        new ClassReader(basicClass).accept(node, 0);
        MethodNode loadWorlds = uniqueLoadWorldsMethod(node);
        MethodInsnNode saveLoader = uniqueSaveLoaderCall(loadWorlds);

        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
                HOOK_NAME, HOOK_DESC, false));
        loadWorlds.instructions.insert(saveLoader, hook);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
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
