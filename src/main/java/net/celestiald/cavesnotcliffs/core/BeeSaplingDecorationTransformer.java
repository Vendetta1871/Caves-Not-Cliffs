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

/** Runs the bee-nest decorator immediately after each individual sapling growth attempt. */
public final class BeeSaplingDecorationTransformer implements IClassTransformer {
    public static final String TARGET = "net.minecraft.block.BlockSapling";
    static final String TARGET_INTERNAL = TARGET.replace('.', '/');
    static final String GENERATE_TREE_DESC = "(Lnet/minecraft/world/World;"
            + "Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;"
            + "Ljava/util/Random;)V";
    static final String TERRAIN_GEN_OWNER =
            "net/minecraftforge/event/terraingen/TerrainGen";
    static final String SAPLING_EVENT_DESC = "(Lnet/minecraft/world/World;Ljava/util/Random;"
            + "Lnet/minecraft/util/math/BlockPos;)Z";
    static final String WORLD_GENERATOR_OWNER =
            "net/minecraft/world/gen/feature/WorldGenerator";
    static final String WORLD_GENERATOR_DESC = "(Lnet/minecraft/world/World;Ljava/util/Random;"
            + "Lnet/minecraft/util/math/BlockPos;)Z";
    static final String HOOK_OWNER =
            "net/celestiald/cavesnotcliffs/world/BeeSaplingNestHandler";
    static final String HOOK_NAME = "finishGrowth";
    static final String HOOK_DESC = "(Lnet/minecraft/world/World;"
            + "Lnet/minecraft/util/math/BlockPos;Ljava/util/Random;)V";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !(TARGET.equals(transformedName) || TARGET.equals(name))) {
            return basicClass;
        }
        ClassNode node = new ClassNode(Opcodes.ASM5);
        new ClassReader(basicClass).accept(node, 0);
        MethodNode generateTree = uniqueGenerateTree(node);

        int returns = 0;
        for (AbstractInsnNode instruction : generateTree.instructions.toArray()) {
            if (instruction.getOpcode() != Opcodes.RETURN) {
                continue;
            }
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
            hook.add(new VarInsnNode(Opcodes.ALOAD, 2));
            hook.add(new VarInsnNode(Opcodes.ALOAD, 4));
            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
                    HOOK_NAME, HOOK_DESC, false));
            generateTree.instructions.insertBefore(instruction, hook);
            returns++;
        }
        if (returns == 0) {
            throw failure("a return from the sapling generation method");
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static MethodNode uniqueGenerateTree(ClassNode node) {
        MethodNode result = null;
        for (MethodNode method : node.methods) {
            if (!GENERATE_TREE_DESC.equals(method.desc)
                    || countCalls(method, TERRAIN_GEN_OWNER, SAPLING_EVENT_DESC) != 1
                    || countCalls(method, WORLD_GENERATOR_OWNER, WORLD_GENERATOR_DESC) != 1) {
                continue;
            }
            if (result != null) {
                throw failure("multiple sapling generation methods");
            }
            result = method;
        }
        if (result == null) {
            throw failure("the sapling event and tree-generator call shape");
        }
        return result;
    }

    private static int countCalls(MethodNode method, String owner, String descriptor) {
        int result = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (owner.equals(call.owner) && descriptor.equals(call.desc)) {
                    result++;
                }
            }
        }
        return result;
    }

    private static IllegalStateException failure(String point) {
        return new IllegalStateException("Caves Not Cliffs bee sapling decoration transformer "
                + "could not verify " + point);
    }
}
