package net.celestiald.cavesnotcliffs.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Extends vanilla flower support with the remaining Java 1.18.2 dirt-tag peers. */
public final class FlowerSupportTransformer implements IClassTransformer {
    public static final String TARGET = "net.minecraft.block.BlockFlower";
    static final String BLOCK_BUSH_OWNER = "net/minecraft/block/BlockBush";
    static final String SUPPORT_MCP = "canSustainBush";
    static final String SUPPORT_SRG = "func_185514_i";
    static final String SUPPORT_DESC = "(Lnet/minecraft/block/state/IBlockState;)Z";
    static final String META_MCP = "getMetaFromState";
    static final String META_SRG = "func_176201_c";
    static final String META_DESC = "(Lnet/minecraft/block/state/IBlockState;)I";
    static final String HOOK_OWNER =
        "net/celestiald/cavesnotcliffs/content/FlowerSupportHooks";
    static final String HOOK_NAME = "isAdditionalSupport";
    static final String HOOK_DESC = "(Lnet/minecraft/block/Block;"
        + "Lnet/minecraft/block/state/IBlockState;)Z";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !(TARGET.equals(name) || TARGET.equals(transformedName))) {
            return basicClass;
        }
        ClassNode node = new ClassNode(Opcodes.ASM5);
        new ClassReader(basicClass).accept(node, 0);
        String supportName = isMcpShape(node) ? SUPPORT_MCP : SUPPORT_SRG;
        requireAbsent(node, SUPPORT_DESC);
        addSupportOverride(node, supportName);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static boolean isMcpShape(ClassNode node) {
        MethodNode probe = null;
        for (MethodNode method : node.methods) {
            if ((method.access & Opcodes.ACC_STATIC) != 0
                    || !META_DESC.equals(method.desc)
                    || !(META_MCP.equals(method.name) || META_SRG.equals(method.name))) {
                continue;
            }
            if (probe != null) {
                throw failure("a unique MCP/SRG metadata probe");
            }
            probe = method;
        }
        if (probe == null) {
            throw failure("the MCP/SRG metadata probe");
        }
        return META_MCP.equals(probe.name);
    }

    private static void requireAbsent(ClassNode node, String descriptor) {
        for (MethodNode method : node.methods) {
            if ((method.access & Opcodes.ACC_STATIC) == 0
                    && descriptor.equals(method.desc)) {
                throw failure("no pre-existing flower support override");
            }
        }
    }

    private static void addSupportOverride(ClassNode node, String methodName) {
        MethodNode method = new MethodNode(Opcodes.ASM5, Opcodes.ACC_PROTECTED,
            methodName, SUPPORT_DESC, null, null);
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
            BLOCK_BUSH_OWNER, methodName, SUPPORT_DESC, false));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
            HOOK_OWNER, HOOK_NAME, HOOK_DESC, false));
        method.instructions.add(new InsnNode(Opcodes.IOR));
        method.instructions.add(new InsnNode(Opcodes.IRETURN));
        node.methods.add(method);
    }

    private static IllegalStateException failure(String point) {
        return new IllegalStateException("Caves Not Cliffs flower support transformer "
            + "could not verify " + point + " in " + TARGET);
    }
}
