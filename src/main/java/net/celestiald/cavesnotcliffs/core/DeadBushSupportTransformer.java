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

/** Extends BlockDeadBush's original support result with Java 1.18.2 dirt peers. */
public final class DeadBushSupportTransformer implements IClassTransformer {
    public static final String TARGET = "net.minecraft.block.BlockDeadBush";
    static final String TARGET_INTERNAL = TARGET.replace('.', '/');
    static final String MCP_METHOD = "canSustainBush";
    static final String SRG_METHOD = "func_185514_i";
    static final String STATE_DESC = "Lnet/minecraft/block/state/IBlockState;";
    static final String SUPPORT_DESC = "(" + STATE_DESC + ")Z";
    static final String HOOK_OWNER =
            "net/celestiald/cavesnotcliffs/content/DeadBushSupportHooks";
    static final String HOOK_NAME = "isAdditionalSupport";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !(TARGET.equals(name) || TARGET.equals(transformedName))) {
            return basicClass;
        }

        ClassNode node = new ClassNode(Opcodes.ASM5);
        new ClassReader(basicClass).accept(node, 0);
        MethodNode support = uniqueSupportMethod(node);
        int returns = 0;
        for (AbstractInsnNode instruction : support.instructions.toArray()) {
            if (instruction.getOpcode() != Opcodes.IRETURN) {
                continue;
            }
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
                    HOOK_NAME, SUPPORT_DESC, false));
            hook.add(new InsnNode(Opcodes.IOR));
            support.instructions.insertBefore(instruction, hook);
            returns++;
        }
        if (returns != 1) {
            throw failure("one integer return from " + support.name
                    + SUPPORT_DESC + " (found " + returns + ")");
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static MethodNode uniqueSupportMethod(ClassNode node) {
        MethodNode result = null;
        for (MethodNode method : node.methods) {
            if ((method.access & Opcodes.ACC_STATIC) != 0
                    || !SUPPORT_DESC.equals(method.desc)
                    || !(MCP_METHOD.equals(method.name) || SRG_METHOD.equals(method.name))) {
                continue;
            }
            if (result != null) {
                throw failure("a unique MCP/SRG dead-bush support method");
            }
            result = method;
        }
        if (result == null) {
            throw failure("the MCP/SRG dead-bush support method");
        }
        return result;
    }

    private static IllegalStateException failure(String point) {
        return new IllegalStateException("Caves Not Cliffs dead bush support transformer "
                + "could not verify " + point + " in " + TARGET);
    }
}
