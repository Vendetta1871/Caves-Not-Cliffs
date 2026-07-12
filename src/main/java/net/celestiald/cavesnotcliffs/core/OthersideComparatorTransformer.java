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

/** Corrects 1.12's numeric-item-ID jukebox comparator formula for Otherside's value 14. */
public final class OthersideComparatorTransformer implements IClassTransformer {
    private static final String TARGET = "net.minecraft.block.BlockJukebox";
    private static final String MCP_METHOD = "getComparatorInputOverride";
    private static final String SRG_METHOD = "func_180641_l";
    private static final String METHOD_DESC =
        "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/World;"
            + "Lnet/minecraft/util/math/BlockPos;)I";
    private static final String HOOK_OWNER =
        "net/celestiald/cavesnotcliffs/content/OthersideComparatorHooks";
    private static final String HOOK_DESC =
        "(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)I";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !(TARGET.equals(name) || TARGET.equals(transformedName))) {
            return basicClass;
        }
        ClassNode node = new ClassNode(Opcodes.ASM5);
        new ClassReader(basicClass).accept(node, 0);
        MethodNode target = uniqueMethod(node);
        verifyOriginalShape(target);

        LabelNode vanilla = new LabelNode();
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 2));
        hook.add(new VarInsnNode(Opcodes.ALOAD, 3));
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
            "comparatorOverride", HOOK_DESC, false));
        hook.add(new InsnNode(Opcodes.DUP));
        hook.add(new InsnNode(Opcodes.ICONST_M1));
        hook.add(new JumpInsnNode(Opcodes.IF_ICMPEQ, vanilla));
        hook.add(new InsnNode(Opcodes.IRETURN));
        hook.add(vanilla);
        hook.add(new FrameNode(Opcodes.F_SAME1, 0, null, 1,
            new Object[]{Opcodes.INTEGER}));
        hook.add(new InsnNode(Opcodes.POP));
        target.instructions.insert(hook);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static MethodNode uniqueMethod(ClassNode node) {
        MethodNode result = null;
        for (MethodNode method : node.methods) {
            if ((method.access & Opcodes.ACC_STATIC) == 0
                    && METHOD_DESC.equals(method.desc)
                    && (MCP_METHOD.equals(method.name) || SRG_METHOD.equals(method.name))) {
                if (result != null) {
                    throw failure("multiple matching methods");
                }
                result = method;
            }
        }
        if (result == null) {
            throw failure("no matching method");
        }
        return result;
    }

    private static void verifyOriginalShape(MethodNode method) {
        int itemIdCalls = 0;
        int subtracts = 0;
        int adds = 0;
        int returns = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.getOpcode() == Opcodes.INVOKESTATIC
                        && "net/minecraft/item/Item".equals(call.owner)
                        && ("getIdFromItem".equals(call.name)
                            || "func_150891_b".equals(call.name))
                        && "(Lnet/minecraft/item/Item;)I".equals(call.desc)) {
                    itemIdCalls++;
                }
            } else if (instruction.getOpcode() == Opcodes.ISUB) {
                subtracts++;
            } else if (instruction.getOpcode() == Opcodes.IADD) {
                adds++;
            } else if (instruction.getOpcode() == Opcodes.IRETURN) {
                returns++;
            }
        }
        if (itemIdCalls != 2 || subtracts != 1 || adds != 1 || returns != 1) {
            throw failure("an unexpected vanilla record-ID comparator body");
        }
    }

    private static IllegalStateException failure(String detail) {
        return new IllegalStateException(
            "Caves Not Cliffs Otherside comparator transformer found " + detail);
    }
}
