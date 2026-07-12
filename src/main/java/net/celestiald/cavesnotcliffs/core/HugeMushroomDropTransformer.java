package net.celestiald.cavesnotcliffs.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Backports Java 1.18.2's exact huge-mushroom cap count roll. */
public final class HugeMushroomDropTransformer implements IClassTransformer {
    public static final String TARGET = "net.minecraft.block.BlockHugeMushroom";
    private static final String MCP_METHOD = "quantityDropped";
    private static final String SRG_METHOD = "func_149745_a";
    private static final String METHOD_DESC = "(Ljava/util/Random;)I";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !(TARGET.equals(name) || TARGET.equals(transformedName))) {
            return basicClass;
        }

        ClassNode node = new ClassNode(Opcodes.ASM5);
        new ClassReader(basicClass).accept(node, 0);
        MethodNode quantity = uniqueQuantityMethod(node);
        verifyOriginalShape(quantity);

        // UniformInt[-6, 2], then LimitCount(min=0). This deliberately replaces the
        // legacy nextInt(10)-7 roll so the random stream is consumed exactly once.
        InsnList instructions = new InsnList();
        instructions.add(new InsnNode(Opcodes.ICONST_0));
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        instructions.add(new IntInsnNode(Opcodes.BIPUSH, 9));
        instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "java/util/Random", "nextInt", "(I)I", false));
        instructions.add(new IntInsnNode(Opcodes.BIPUSH, 6));
        instructions.add(new InsnNode(Opcodes.ISUB));
        instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                "java/lang/Math", "max", "(II)I", false));
        instructions.add(new InsnNode(Opcodes.IRETURN));
        quantity.instructions.clear();
        quantity.tryCatchBlocks.clear();
        quantity.localVariables = null;
        quantity.instructions.add(instructions);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static MethodNode uniqueQuantityMethod(ClassNode node) {
        MethodNode result = null;
        for (MethodNode method : node.methods) {
            if ((method.access & Opcodes.ACC_STATIC) != 0
                    || !METHOD_DESC.equals(method.desc)
                    || !(MCP_METHOD.equals(method.name) || SRG_METHOD.equals(method.name))) {
                continue;
            }
            if (result != null) {
                throw failure("a unique MCP/SRG quantity method");
            }
            result = method;
        }
        if (result == null) {
            throw failure("the MCP/SRG quantity method");
        }
        return result;
    }

    private static void verifyOriginalShape(MethodNode method) {
        int nextIntCalls = 0;
        int returns = 0;
        boolean legacyBound = false;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof IntInsnNode
                    && instruction.getOpcode() == Opcodes.BIPUSH
                    && ((IntInsnNode) instruction).operand == 10) {
                legacyBound = true;
            } else if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.getOpcode() == Opcodes.INVOKEVIRTUAL
                        && "java/util/Random".equals(call.owner)
                        && "nextInt".equals(call.name)
                        && "(I)I".equals(call.desc)) {
                    nextIntCalls++;
                }
            } else if (instruction.getOpcode() == Opcodes.IRETURN) {
                returns++;
            }
        }
        if (!legacyBound || nextIntCalls != 1 || returns != 1) {
            throw failure("the legacy nextInt(10) quantity body");
        }
    }

    private static IllegalStateException failure(String point) {
        return new IllegalStateException("Caves Not Cliffs huge-mushroom drop transformer "
                + "could not verify " + point + " in " + TARGET);
    }
}
