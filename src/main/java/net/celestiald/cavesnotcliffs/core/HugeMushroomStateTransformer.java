package net.celestiald.cavesnotcliffs.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Adds the post-flattening six-face placement and neighbor contract to legacy caps. */
public final class HugeMushroomStateTransformer implements IClassTransformer {
    public static final String TARGET = "net.minecraft.block.BlockHugeMushroom";
    private static final String MCP_PLACEMENT = "getStateForPlacement";
    private static final String SRG_PLACEMENT = "func_180642_a";
    private static final String PLACEMENT_DESC = "(Lnet/minecraft/world/World;"
            + "Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;"
            + "FFFILnet/minecraft/entity/EntityLivingBase;)"
            + "Lnet/minecraft/block/state/IBlockState;";
    private static final String MCP_NEIGHBOR = "neighborChanged";
    private static final String SRG_NEIGHBOR = "func_189540_a";
    private static final String NEIGHBOR_DESC = "(Lnet/minecraft/block/state/IBlockState;"
            + "Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;"
            + "Lnet/minecraft/block/Block;Lnet/minecraft/util/math/BlockPos;)V";
    private static final String HOOKS =
            "net/celestiald/cavesnotcliffs/content/MushroomCapContent";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !(TARGET.equals(name) || TARGET.equals(transformedName))) {
            return basicClass;
        }
        ClassNode node = new ClassNode(Opcodes.ASM5);
        new ClassReader(basicClass).accept(node, 0);
        MethodNode placement = uniquePlacement(node);
        boolean mcp = MCP_PLACEMENT.equals(placement.name);
        verifyNoNeighborOverride(node);

        InsnList placementBody = new InsnList();
        placementBody.add(new VarInsnNode(Opcodes.ALOAD, 0));
        placementBody.add(new VarInsnNode(Opcodes.ALOAD, 1));
        placementBody.add(new VarInsnNode(Opcodes.ALOAD, 2));
        placementBody.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS,
                "publicPlacement", "(Lnet/minecraft/block/BlockHugeMushroom;"
                        + "Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)"
                        + "Lnet/minecraft/block/state/IBlockState;", false));
        placementBody.add(new org.objectweb.asm.tree.InsnNode(Opcodes.ARETURN));
        placement.instructions.clear();
        placement.tryCatchBlocks.clear();
        placement.localVariables = null;
        placement.instructions.add(placementBody);

        MethodNode neighbor = new MethodNode(Opcodes.ACC_PUBLIC,
                mcp ? MCP_NEIGHBOR : SRG_NEIGHBOR, NEIGHBOR_DESC, null, null);
        neighbor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        neighbor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        neighbor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        neighbor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 3));
        neighbor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 5));
        neighbor.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS,
                "publicNeighborChanged", "(Lnet/minecraft/block/BlockHugeMushroom;"
                        + "Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/World;"
                        + "Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/BlockPos;)V",
                false));
        neighbor.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
        node.methods.add(neighbor);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static MethodNode uniquePlacement(ClassNode node) {
        MethodNode result = null;
        for (MethodNode method : node.methods) {
            if (!PLACEMENT_DESC.equals(method.desc)
                    || !(MCP_PLACEMENT.equals(method.name)
                            || SRG_PLACEMENT.equals(method.name))) {
                continue;
            }
            if (result != null) {
                throw failure("a unique MCP/SRG placement method");
            }
            result = method;
        }
        if (result == null) {
            throw failure("the MCP/SRG placement method");
        }
        return result;
    }

    private static void verifyNoNeighborOverride(ClassNode node) {
        for (MethodNode method : node.methods) {
            if (NEIGHBOR_DESC.equals(method.desc)
                    && (MCP_NEIGHBOR.equals(method.name)
                            || SRG_NEIGHBOR.equals(method.name))) {
                throw failure("no pre-existing neighbor override");
            }
        }
    }

    private static IllegalStateException failure(String point) {
        return new IllegalStateException("Caves Not Cliffs huge-mushroom state transformer "
                + "could not verify " + point + " in " + TARGET);
    }
}
