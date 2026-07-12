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

/** Applies exact tall-grass/large-fern pairing, support, and seed-drop behavior. */
public final class DoublePlantSupportTransformer implements IClassTransformer {
    public static final String TARGET = "net.minecraft.block.BlockDoublePlant";
    static final String BLOCK_OWNER = "net/minecraft/block/Block";
    static final String SURVIVAL_MCP = "canBlockStay";
    static final String SURVIVAL_SRG = "func_180671_f";
    static final String SURVIVAL_DESC = "(Lnet/minecraft/world/World;"
            + "Lnet/minecraft/util/math/BlockPos;"
            + "Lnet/minecraft/block/state/IBlockState;)Z";
    static final String CHECK_MCP = "checkAndDropBlock";
    static final String CHECK_SRG = "func_176475_e";
    static final String CHECK_DESC = "(Lnet/minecraft/world/World;"
            + "Lnet/minecraft/util/math/BlockPos;"
            + "Lnet/minecraft/block/state/IBlockState;)V";
    static final String HARVEST_MCP = "onBlockHarvested";
    static final String HARVEST_SRG = "func_176208_a";
    static final String HARVEST_DESC = "(Lnet/minecraft/world/World;"
            + "Lnet/minecraft/util/math/BlockPos;"
            + "Lnet/minecraft/block/state/IBlockState;"
            + "Lnet/minecraft/entity/player/EntityPlayer;)V";
    static final String HARVEST_END_MCP = "harvestBlock";
    static final String HARVEST_END_SRG = "func_180657_a";
    static final String HARVEST_END_DESC = "(Lnet/minecraft/world/World;"
            + "Lnet/minecraft/entity/player/EntityPlayer;"
            + "Lnet/minecraft/util/math/BlockPos;"
            + "Lnet/minecraft/block/state/IBlockState;"
            + "Lnet/minecraft/tileentity/TileEntity;"
            + "Lnet/minecraft/item/ItemStack;)V";
    static final String GET_DROPS = "getDrops";
    static final String GET_DROPS_DESC = "(Lnet/minecraft/util/NonNullList;"
            + "Lnet/minecraft/world/IBlockAccess;"
            + "Lnet/minecraft/util/math/BlockPos;"
            + "Lnet/minecraft/block/state/IBlockState;I)V";
    static final String SHEARABLE = "isShearable";
    static final String SHEARABLE_DESC = "(Lnet/minecraft/item/ItemStack;"
            + "Lnet/minecraft/world/IBlockAccess;"
            + "Lnet/minecraft/util/math/BlockPos;)Z";
    static final String SHEARED = "onSheared";
    static final String SHEARED_DESC = "(Lnet/minecraft/item/ItemStack;"
            + "Lnet/minecraft/world/IBlockAccess;"
            + "Lnet/minecraft/util/math/BlockPos;I)Ljava/util/List;";
    static final String HOOK_OWNER =
            "net/celestiald/cavesnotcliffs/content/DoublePlantSupportHooks";
    static final String SURVIVAL_GUARD = "usesJava118Survival";
    static final String SURVIVAL_HOOK = "canStay";
    static final String CHECK_HOOK = "handleCheckAndDrop";
    static final String HARVEST_HOOK = "beginHarvest";
    static final String HARVEST_END_HOOK = "endHarvest";
    static final String GET_DROPS_HOOK = "addDrops";
    static final String GET_DROPS_HOOK_DESC = "(Lnet/minecraft/util/NonNullList;"
            + "Lnet/minecraft/world/IBlockAccess;"
            + "Lnet/minecraft/util/math/BlockPos;"
            + "Lnet/minecraft/block/state/IBlockState;I)Z";
    static final String SHEAR_GUARD = "usesJava118Shearing";
    static final String SHEAR_QUERY_DESC = "(Lnet/minecraft/world/IBlockAccess;"
            + "Lnet/minecraft/util/math/BlockPos;)Z";
    static final String SHEARABLE_HOOK = "canShear";
    static final String SHEARED_HOOK = "shearedDrops";
    static final String SHEARED_HOOK_DESC = "(Lnet/minecraft/world/IBlockAccess;"
            + "Lnet/minecraft/util/math/BlockPos;)Ljava/util/List;";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !(TARGET.equals(name) || TARGET.equals(transformedName))) {
            return basicClass;
        }

        ClassNode node = new ClassNode(Opcodes.ASM5);
        new ClassReader(basicClass).accept(node, 0);
        MethodNode survival = uniqueMethod(node, SURVIVAL_DESC,
            SURVIVAL_MCP, SURVIVAL_SRG, "survival");
        MethodNode check = uniqueMethod(node, CHECK_DESC,
            CHECK_MCP, CHECK_SRG, "support-loss");
        MethodNode harvest = uniqueMethod(node, HARVEST_DESC,
            HARVEST_MCP, HARVEST_SRG, "harvest");
        MethodNode harvestEnd = uniqueMethod(node, HARVEST_END_DESC,
            HARVEST_END_MCP, HARVEST_END_SRG, "harvest cleanup");
        MethodNode shearable = uniqueMethod(node, SHEARABLE_DESC,
            SHEARABLE, SHEARABLE, "shearability");
        MethodNode sheared = uniqueMethod(node, SHEARED_DESC,
            SHEARED, SHEARED, "sheared drops");
        requireReturns(survival, Opcodes.IRETURN, 3, "survival");
        requireReturns(check, Opcodes.RETURN, 1, "support-loss");
        requireReturns(harvest, Opcodes.RETURN, 1, "harvest");
        requireReturns(harvestEnd, Opcodes.RETURN, 1, "harvest cleanup");
        requireReturns(shearable, Opcodes.IRETURN, 1, "shearability");
        requireReturns(sheared, Opcodes.ARETURN, 1, "sheared drops");
        requireAbsentMethod(node, GET_DROPS_DESC, GET_DROPS);
        insertSurvivalGuard(survival);
        insertCheckGuard(check);
        insertHarvestSnapshot(harvest);
        insertHarvestCleanup(harvestEnd);
        insertShearableGuard(shearable);
        insertShearedGuard(sheared);
        addGetDropsOverride(node);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static void insertSurvivalGuard(MethodNode method) {
        LabelNode original = new LabelNode();
        InsnList guard = new InsnList();
        guard.add(new VarInsnNode(Opcodes.ALOAD, 1));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 2));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 3));
        guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
                SURVIVAL_GUARD, SURVIVAL_DESC, false));
        guard.add(new JumpInsnNode(Opcodes.IFEQ, original));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 1));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 2));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 3));
        guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
                SURVIVAL_HOOK, SURVIVAL_DESC, false));
        guard.add(new InsnNode(Opcodes.IRETURN));
        guard.add(original);
        guard.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        method.instructions.insert(guard);
    }

    private static void insertCheckGuard(MethodNode method) {
        LabelNode original = new LabelNode();
        InsnList guard = new InsnList();
        guard.add(new VarInsnNode(Opcodes.ALOAD, 1));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 2));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 3));
        guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
                CHECK_HOOK, SURVIVAL_DESC, false));
        guard.add(new JumpInsnNode(Opcodes.IFEQ, original));
        guard.add(new InsnNode(Opcodes.RETURN));
        guard.add(original);
        guard.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        method.instructions.insert(guard);
    }

    private static void insertHarvestSnapshot(MethodNode method) {
        InsnList snapshot = new InsnList();
        snapshot.add(new VarInsnNode(Opcodes.ALOAD, 1));
        snapshot.add(new VarInsnNode(Opcodes.ALOAD, 2));
        snapshot.add(new VarInsnNode(Opcodes.ALOAD, 3));
        snapshot.add(new VarInsnNode(Opcodes.ALOAD, 4));
        snapshot.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
                HARVEST_HOOK, HARVEST_DESC, false));
        method.instructions.insert(snapshot);
    }

    private static void insertHarvestCleanup(MethodNode method) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction.getOpcode() != Opcodes.RETURN) {
                continue;
            }
            method.instructions.insertBefore(instruction,
                new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
                    HARVEST_END_HOOK, "()V", false));
        }
    }

    private static void insertShearableGuard(MethodNode method) {
        LabelNode original = new LabelNode();
        InsnList guard = new InsnList();
        guard.add(new VarInsnNode(Opcodes.ALOAD, 2));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 3));
        guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
                SHEAR_GUARD, SHEAR_QUERY_DESC, false));
        guard.add(new JumpInsnNode(Opcodes.IFEQ, original));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 2));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 3));
        guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
                SHEARABLE_HOOK, SHEAR_QUERY_DESC, false));
        guard.add(new InsnNode(Opcodes.IRETURN));
        guard.add(original);
        guard.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        method.instructions.insert(guard);
    }

    private static void insertShearedGuard(MethodNode method) {
        LabelNode original = new LabelNode();
        InsnList guard = new InsnList();
        guard.add(new VarInsnNode(Opcodes.ALOAD, 2));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 3));
        guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
                SHEAR_GUARD, SHEAR_QUERY_DESC, false));
        guard.add(new JumpInsnNode(Opcodes.IFEQ, original));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 2));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 3));
        guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
                SHEARED_HOOK, SHEARED_HOOK_DESC, false));
        guard.add(new InsnNode(Opcodes.ARETURN));
        guard.add(original);
        guard.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        method.instructions.insert(guard);
    }

    private static void addGetDropsOverride(ClassNode node) {
        MethodNode method = new MethodNode(Opcodes.ASM5, Opcodes.ACC_PUBLIC,
            GET_DROPS, GET_DROPS_DESC, null, null);
        LabelNode delegate = new LabelNode();
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 3));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 4));
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 5));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
            GET_DROPS_HOOK, GET_DROPS_HOOK_DESC, false));
        method.instructions.add(new JumpInsnNode(Opcodes.IFEQ, delegate));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.instructions.add(delegate);
        method.instructions.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 3));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 4));
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 5));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, BLOCK_OWNER,
            GET_DROPS, GET_DROPS_DESC, false));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        node.methods.add(method);
    }

    private static void requireAbsentMethod(ClassNode node, String descriptor,
            String point) {
        for (MethodNode method : node.methods) {
            if (descriptor.equals(method.desc)) {
                throw failure("no pre-existing " + point + descriptor + " override");
            }
        }
    }

    private static MethodNode uniqueMethod(ClassNode node, String descriptor,
            String mcpName, String srgName, String point) {
        MethodNode result = null;
        for (MethodNode method : node.methods) {
            if ((method.access & Opcodes.ACC_STATIC) != 0
                    || !descriptor.equals(method.desc)
                    || !(mcpName.equals(method.name) || srgName.equals(method.name))) {
                continue;
            }
            if (result != null) {
                throw failure("a unique MCP/SRG " + point + " method");
            }
            result = method;
        }
        if (result == null) {
            throw failure("the MCP/SRG " + point + " method " + descriptor);
        }
        return result;
    }

    private static void requireReturns(MethodNode method, int opcode,
            int expected, String point) {
        int actual = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction.getOpcode() == opcode) {
                actual++;
            }
        }
        if (actual != expected) {
            throw failure(expected + " original " + point + " returns in "
                + method.name + method.desc + " (found " + actual + ")");
        }
    }

    private static IllegalStateException failure(String point) {
        return new IllegalStateException("Caves Not Cliffs double plant transformer "
            + "could not verify " + point + " in " + TARGET);
    }
}
