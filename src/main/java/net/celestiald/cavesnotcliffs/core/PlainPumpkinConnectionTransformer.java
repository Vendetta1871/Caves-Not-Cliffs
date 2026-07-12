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

/** Adds the plain pumpkin to fence, wall, and pane connection exceptions. */
public final class PlainPumpkinConnectionTransformer implements IClassTransformer {
    static final String FENCE_TARGET = "net.minecraft.block.BlockFence";
    static final String WALL_TARGET = "net.minecraft.block.BlockWall";
    static final String PANE_TARGET = "net.minecraft.block.BlockPane";
    static final String BLOCK_DESC = "Lnet/minecraft/block/Block;";
    static final String EXCEPTION_DESC = "(" + BLOCK_DESC + ")Z";
    static final String HOOK_OWNER =
            "net/celestiald/cavesnotcliffs/content/PlainPumpkinConnectionHooks";
    static final String HOOK_NAME = "isPlainPumpkin";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        String target = target(name, transformedName);
        if (basicClass == null || target == null) {
            return basicClass;
        }

        ClassNode node = new ClassNode(Opcodes.ASM5);
        new ClassReader(basicClass).accept(node, 0);
        MethodNode exception = uniqueExceptionMethod(node, target);
        int returns = 0;
        for (AbstractInsnNode instruction : exception.instructions.toArray()) {
            if (instruction.getOpcode() != Opcodes.IRETURN) {
                continue;
            }
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
                    HOOK_NAME, EXCEPTION_DESC, false));
            hook.add(new InsnNode(Opcodes.IOR));
            exception.instructions.insertBefore(instruction, hook);
            returns++;
        }
        if (returns != 1) {
            throw failure(target, "one integer return (found " + returns + ")");
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static MethodNode uniqueExceptionMethod(ClassNode node, String target) {
        MethodNode result = null;
        for (MethodNode method : node.methods) {
            if ((method.access & Opcodes.ACC_STATIC) == 0
                    || !EXCEPTION_DESC.equals(method.desc)) {
                continue;
            }
            if (result != null) {
                throw failure(target, "a unique static connection-exception method");
            }
            result = method;
        }
        if (result == null) {
            throw failure(target, "the static connection-exception method");
        }
        return result;
    }

    private static String target(String name, String transformedName) {
        if (matches(FENCE_TARGET, name, transformedName)) {
            return FENCE_TARGET;
        }
        if (matches(WALL_TARGET, name, transformedName)) {
            return WALL_TARGET;
        }
        return matches(PANE_TARGET, name, transformedName) ? PANE_TARGET : null;
    }

    private static boolean matches(String target, String name, String transformedName) {
        return target.equals(name) || target.equals(transformedName);
    }

    private static IllegalStateException failure(String target, String point) {
        return new IllegalStateException(
                "Caves Not Cliffs plain pumpkin connection transformer could not verify "
                        + point + " in " + target);
    }
}
