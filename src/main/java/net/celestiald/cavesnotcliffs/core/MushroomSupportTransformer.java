package net.celestiald.cavesnotcliffs.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Routes finite-Overworld BlockMushroom survival through the Java 1.18.2 predicate. */
public final class MushroomSupportTransformer implements IClassTransformer {
    public static final String TARGET = "net.minecraft.block.BlockMushroom";
    static final String MCP_METHOD = "canBlockStay";
    static final String SRG_METHOD = "func_180671_f";
    static final String METHOD_DESC = "(Lnet/minecraft/world/World;"
            + "Lnet/minecraft/util/math/BlockPos;"
            + "Lnet/minecraft/block/state/IBlockState;)Z";
    static final String HOOK_OWNER =
            "net/celestiald/cavesnotcliffs/content/MushroomSupportHooks";
    static final String HOOK_NAME = "canStay";
    static final String GUARD_NAME = "usesJava118Survival";
    static final String HOOK_DESC = "(Lnet/minecraft/world/World;"
            + "Lnet/minecraft/util/math/BlockPos;)Z";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !(TARGET.equals(name) || TARGET.equals(transformedName))) {
            return basicClass;
        }

        ClassNode node = new ClassNode(Opcodes.ASM5);
        new ClassReader(basicClass).accept(node, 0);
        MethodNode survival = uniqueSurvivalMethod(node);
        verifyOriginalShape(survival);

        LabelNode original = new LabelNode();
        InsnList guard = new InsnList();
        guard.add(new VarInsnNode(Opcodes.ALOAD, 1));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 2));
        guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
                GUARD_NAME, HOOK_DESC, false));
        guard.add(new JumpInsnNode(Opcodes.IFEQ, original));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 1));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 2));
        guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
                HOOK_NAME, HOOK_DESC, false));
        guard.add(new InsnNode(Opcodes.IRETURN));
        guard.add(original);
        guard.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        survival.instructions.insert(guard);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static void verifyOriginalShape(MethodNode survival) {
        int returns = 0;
        for (AbstractInsnNode instruction : survival.instructions.toArray()) {
            if (instruction.getOpcode() == Opcodes.IRETURN) {
                returns++;
            }
        }
        if (returns != 4) {
            throw failure("four integer returns from " + survival.name
                    + METHOD_DESC + " (found " + returns + ")");
        }
    }

    private static MethodNode uniqueSurvivalMethod(ClassNode node) {
        MethodNode result = null;
        for (MethodNode method : node.methods) {
            if ((method.access & Opcodes.ACC_STATIC) != 0
                    || !METHOD_DESC.equals(method.desc)
                    || !(MCP_METHOD.equals(method.name) || SRG_METHOD.equals(method.name))) {
                continue;
            }
            if (result != null) {
                throw failure("a unique MCP/SRG mushroom survival method");
            }
            result = method;
        }
        if (result == null) {
            throw failure("the MCP/SRG mushroom survival method");
        }
        return result;
    }

    private static IllegalStateException failure(String point) {
        return new IllegalStateException("Caves Not Cliffs mushroom support transformer "
                + "could not verify " + point + " in " + TARGET);
    }
}
