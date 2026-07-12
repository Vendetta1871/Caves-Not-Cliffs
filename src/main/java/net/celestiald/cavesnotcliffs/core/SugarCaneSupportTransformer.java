package net.celestiald.cavesnotcliffs.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Preserves BlockReed's result while adding exact Java 1.18.2 support and water peers. */
public final class SugarCaneSupportTransformer implements IClassTransformer {
    public static final String TARGET = "net.minecraft.block.BlockReed";
    static final String TARGET_INTERNAL = TARGET.replace('.', '/');
    static final String MCP_METHOD = "canPlaceBlockAt";
    static final String SRG_METHOD = "func_176196_c";
    static final String METHOD_DESC = "(Lnet/minecraft/world/World;"
            + "Lnet/minecraft/util/math/BlockPos;)Z";
    static final String HOOK_OWNER =
            "net/celestiald/cavesnotcliffs/content/SugarCaneSupportHooks";
    static final String HOOK_NAME = "canPlaceOnFutureSupport";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !(TARGET.equals(name) || TARGET.equals(transformedName))) {
            return basicClass;
        }

        ClassNode node = new ClassNode(Opcodes.ASM5);
        new ClassReader(basicClass).accept(node, 0);
        MethodNode placement = uniquePlacementMethod(node);
        int returns = 0;
        for (AbstractInsnNode instruction : placement.instructions.toArray()) {
            if (instruction.getOpcode() != Opcodes.IRETURN) {
                continue;
            }
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
            hook.add(new VarInsnNode(Opcodes.ALOAD, 2));
            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
                    HOOK_NAME, METHOD_DESC, false));
            hook.add(new InsnNode(Opcodes.IOR));
            placement.instructions.insertBefore(instruction, hook);
            returns++;
        }
        if (returns == 0) {
            throw failure("at least one integer return from " + placement.name
                    + METHOD_DESC);
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static MethodNode uniquePlacementMethod(ClassNode node) {
        MethodNode result = null;
        for (MethodNode method : node.methods) {
            if ((method.access & Opcodes.ACC_STATIC) != 0
                    || !METHOD_DESC.equals(method.desc)
                    || !(MCP_METHOD.equals(method.name) || SRG_METHOD.equals(method.name))) {
                continue;
            }
            if (result != null) {
                throw failure("a unique MCP/SRG sugar-cane placement method");
            }
            result = method;
        }
        if (result == null) {
            throw failure("the MCP/SRG sugar-cane placement method");
        }
        return result;
    }

    private static IllegalStateException failure(String point) {
        return new IllegalStateException("Caves Not Cliffs sugar cane support transformer "
                + "could not verify " + point + " in " + TARGET);
    }
}
