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

/** Adds one post-Forge completion hook to vanilla's finite chunk population transaction. */
public final class SchemaOnePopulationTransformer implements IClassTransformer {
    public static final String TARGET = "net.minecraft.world.chunk.Chunk";
    static final String TARGET_INTERNAL = TARGET.replace('.', '/');
    static final String GENERATOR_DESC = "Lnet/minecraft/world/gen/IChunkGenerator;";
    static final String POPULATE_DESC = "(" + GENERATOR_DESC + ")V";
    static final String FORGE_OWNER =
            "net/minecraftforge/fml/common/registry/GameRegistry";
    static final String FORGE_NAME = "generateWorld";
    static final String FORGE_DESC = "(IILnet/minecraft/world/World;"
            + GENERATOR_DESC + "Lnet/minecraft/world/chunk/IChunkProvider;)V";
    static final String HOOK_OWNER =
            "net/celestiald/cavesnotcliffs/world/LegacySchemaOnePopulationHandler";
    static final String HOOK_NAME = "afterForgeWorldGeneration";
    static final String HOOK_DESC = "(Lnet/minecraft/world/chunk/Chunk;)V";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !(TARGET.equals(transformedName) || TARGET.equals(name))) {
            return basicClass;
        }
        ClassNode node = new ClassNode(Opcodes.ASM5);
        new ClassReader(basicClass).accept(node, 0);
        MethodNode populate = uniqueMethod(node);
        MethodInsnNode forgeWorldgen = uniqueForgeCall(populate);

        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
                HOOK_NAME, HOOK_DESC, false));
        populate.instructions.insert(forgeWorldgen, hook);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static MethodNode uniqueMethod(ClassNode node) {
        MethodNode result = null;
        for (MethodNode method : node.methods) {
            if (!POPULATE_DESC.equals(method.desc) || findForgeCall(method) == null) {
                continue;
            }
            if (result != null) {
                throw failure("multiple finite populate methods containing Forge world generation");
            }
            result = method;
        }
        if (result == null) {
            throw failure("finite populate method containing Forge world generation "
                    + POPULATE_DESC);
        }
        return result;
    }

    private static MethodInsnNode uniqueForgeCall(MethodNode method) {
        MethodInsnNode result = findForgeCall(method);
        if (result == null) {
            throw failure("post-Forge world-generation call in "
                    + method.name + method.desc);
        }
        return result;
    }

    private static MethodInsnNode findForgeCall(MethodNode method) {
        MethodInsnNode result = null;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) {
                continue;
            }
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (call.getOpcode() == Opcodes.INVOKESTATIC
                    && FORGE_OWNER.equals(call.owner)
                    && FORGE_NAME.equals(call.name)
                    && FORGE_DESC.equals(call.desc)) {
                if (result != null) {
                    throw failure("multiple Forge world-generation calls in "
                            + method.name + method.desc);
                }
                result = call;
            }
        }
        return result;
    }

    private static IllegalStateException failure(String point) {
        return new IllegalStateException("Caves Not Cliffs schema-1 population transformer "
                + "could not verify " + point);
    }
}
