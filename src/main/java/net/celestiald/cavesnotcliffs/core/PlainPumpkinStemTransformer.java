package net.celestiald.cavesnotcliffs.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Redirects the three vanilla stem fruit anchors to the plain pumpkin peer. */
public final class PlainPumpkinStemTransformer implements IClassTransformer {
    public static final String TARGET = "net.minecraft.block.BlockStem";
    static final String TARGET_INTERNAL = TARGET.replace('.', '/');
    static final String BLOCK_OWNER = "net/minecraft/block/Block";
    static final String BLOCK_DESC = "Lnet/minecraft/block/Block;";
    static final String STATE_DESC = "Lnet/minecraft/block/state/IBlockState;";
    static final String ACTUAL_STATE_DESC = "(" + STATE_DESC
            + "Lnet/minecraft/world/IBlockAccess;"
            + "Lnet/minecraft/util/math/BlockPos;)" + STATE_DESC;
    static final String UPDATE_TICK_DESC = "(Lnet/minecraft/world/World;"
            + "Lnet/minecraft/util/math/BlockPos;" + STATE_DESC
            + "Ljava/util/Random;)V";
    static final String DEFAULT_STATE_DESC = "()" + STATE_DESC;
    static final String HOOK_OWNER =
            "net/celestiald/cavesnotcliffs/content/PlainPumpkinStemHooks";
    static final String MATCH_HOOK = "matchesFruit";
    static final String MATCH_HOOK_DESC = "(" + BLOCK_DESC + BLOCK_DESC + ")Z";
    static final String STATE_HOOK = "fruitState";
    static final String STATE_HOOK_DESC = "(" + BLOCK_DESC + ")" + STATE_DESC;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !(TARGET.equals(transformedName) || TARGET.equals(name))) {
            return basicClass;
        }

        ClassNode node = new ClassNode(Opcodes.ASM5);
        new ClassReader(basicClass).accept(node, 0);
        MethodNode actualState = uniqueMethod(node, ACTUAL_STATE_DESC,
                "the actual-state method");
        MethodNode updateTick = uniqueMethod(node, UPDATE_TICK_DESC,
                "the update-tick method");

        requireCount(rewriteFruitComparisons(actualState), 1,
                "one actual-state fruit comparison");
        requireCount(rewriteFruitComparisons(updateTick), 1,
                "one update-tick fruit comparison");
        requireCount(rewriteFruitPlacement(updateTick), 1,
                "one update-tick fruit placement");

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static int rewriteFruitComparisons(MethodNode method) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction.getOpcode() != Opcodes.IF_ACMPNE
                    || !isCropField(previousMeaningful(instruction))) {
                continue;
            }
            method.instructions.insertBefore(instruction,
                    new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
                            MATCH_HOOK, MATCH_HOOK_DESC, false));
            ((JumpInsnNode) instruction).setOpcode(Opcodes.IFEQ);
            count++;
        }
        return count;
    }

    private static int rewriteFruitPlacement(MethodNode method) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)
                    || !isCropField(previousMeaningful(instruction))) {
                continue;
            }
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (!BLOCK_OWNER.equals(call.owner)
                    || !DEFAULT_STATE_DESC.equals(call.desc)) {
                continue;
            }
            call.setOpcode(Opcodes.INVOKESTATIC);
            call.owner = HOOK_OWNER;
            call.name = STATE_HOOK;
            call.desc = STATE_HOOK_DESC;
            call.itf = false;
            count++;
        }
        return count;
    }

    private static MethodNode uniqueMethod(ClassNode node, String descriptor,
            String point) {
        MethodNode result = null;
        for (MethodNode method : node.methods) {
            if (!descriptor.equals(method.desc)) {
                continue;
            }
            if (result != null) {
                throw failure("a unique " + point);
            }
            result = method;
        }
        if (result == null) {
            throw failure(point);
        }
        return result;
    }

    private static boolean isCropField(AbstractInsnNode instruction) {
        if (!(instruction instanceof FieldInsnNode)
                || instruction.getOpcode() != Opcodes.GETFIELD) {
            return false;
        }
        FieldInsnNode field = (FieldInsnNode) instruction;
        return TARGET_INTERNAL.equals(field.owner) && BLOCK_DESC.equals(field.desc);
    }

    private static AbstractInsnNode previousMeaningful(AbstractInsnNode instruction) {
        AbstractInsnNode cursor = instruction.getPrevious();
        while (cursor != null && cursor.getOpcode() < 0) {
            cursor = cursor.getPrevious();
        }
        return cursor;
    }

    private static void requireCount(int actual, int expected, String point) {
        if (actual != expected) {
            throw failure(point + " (found " + actual + ")");
        }
    }

    private static IllegalStateException failure(String point) {
        return new IllegalStateException("Caves Not Cliffs pumpkin stem transformer "
                + "could not verify " + point);
    }
}
