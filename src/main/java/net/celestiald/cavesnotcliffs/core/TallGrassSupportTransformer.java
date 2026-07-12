package net.celestiald.cavesnotcliffs.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Applies exact finite-Overworld short-grass/fern support and drop behavior. */
public final class TallGrassSupportTransformer implements IClassTransformer {
    public static final String TALL_GRASS_TARGET = "net.minecraft.block.BlockTallGrass";
    public static final String EXPLOSION_TARGET = "net.minecraft.world.Explosion";
    static final String BLOCK_OWNER = "net/minecraft/block/Block";
    static final String BLOCK_BUSH_OWNER = "net/minecraft/block/BlockBush";
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
    static final String CAN_PLACE_MCP = "canPlaceBlockAt";
    static final String CAN_PLACE_SRG = "func_176196_c";
    static final String CAN_PLACE_DESC = "(Lnet/minecraft/world/World;"
            + "Lnet/minecraft/util/math/BlockPos;)Z";
    static final String GROW_MCP = "grow";
    static final String GROW_SRG = "func_176474_b";
    static final String GROW_DESC = "(Lnet/minecraft/world/World;Ljava/util/Random;"
            + "Lnet/minecraft/util/math/BlockPos;"
            + "Lnet/minecraft/block/state/IBlockState;)V";
    static final String GET_DROPS = "getDrops";
    static final String GET_DROPS_DESC = "(Lnet/minecraft/util/NonNullList;"
            + "Lnet/minecraft/world/IBlockAccess;"
            + "Lnet/minecraft/util/math/BlockPos;"
            + "Lnet/minecraft/block/state/IBlockState;I)V";
    static final String DROP_MCP = "dropBlockAsItemWithChance";
    static final String DROP_SRG = "func_180653_a";
    static final String DROP_DESC = "(Lnet/minecraft/world/World;"
            + "Lnet/minecraft/util/math/BlockPos;"
            + "Lnet/minecraft/block/state/IBlockState;FI)V";
    static final String CAN_DROP_MCP = "canDropFromExplosion";
    static final String CAN_DROP_SRG = "func_149659_a";
    static final String CAN_DROP_DESC = "(Lnet/minecraft/world/Explosion;)Z";
    static final String EXPLODE_MCP = "doExplosionB";
    static final String EXPLODE_SRG = "func_77279_a";
    static final String EXPLODE_DESC = "(Z)V";
    static final String HOOK_OWNER =
            "net/celestiald/cavesnotcliffs/content/TallGrassSupportHooks";
    static final String USE_HOOK = "usesJava118";
    static final String STAY_HOOK = "canStay";
    static final String STAY_HOOK_DESC = "(Lnet/minecraft/world/World;"
            + "Lnet/minecraft/util/math/BlockPos;)Z";
    static final String CHECK_HOOK = "handleCheckAndDrop";
    static final String USE_PLACEMENT_HOOK = "usesJava118Placement";
    static final String USE_PLACEMENT_DESC = "(Lnet/minecraft/block/Block;"
            + "Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)Z";
    static final String CAN_PLACE_HOOK = "canPlaceBlockAt";
    static final String GROW_HOOK = "grow";
    static final String GROW_HOOK_DESC = "(Lnet/minecraft/world/World;"
            + "Lnet/minecraft/util/math/BlockPos;"
            + "Lnet/minecraft/block/state/IBlockState;)V";
    static final String GET_DROPS_HOOK = "addDrops";
    static final String GET_DROPS_HOOK_DESC = "(Lnet/minecraft/util/NonNullList;"
            + "Lnet/minecraft/world/IBlockAccess;"
            + "Lnet/minecraft/util/math/BlockPos;"
            + "Lnet/minecraft/block/state/IBlockState;I)Z";
    static final String DROP_HOOK = "dropBlockAsItemWithChance";
    static final String DROP_HOOK_DESC = "(Lnet/minecraft/world/World;"
            + "Lnet/minecraft/util/math/BlockPos;"
            + "Lnet/minecraft/block/state/IBlockState;FI"
            + "Lnet/minecraft/entity/player/EntityPlayer;)Z";
    static final String MARK_EXPLOSION_HOOK = "markExplosionDrop";
    static final String MARK_EXPLOSION_DESC = "(ZLnet/minecraft/block/Block;)Z";
    static final String END_EXPLOSION_HOOK = "endExplosionDrop";
    static final String HARVESTERS_FIELD = "harvesters";
    static final String HARVESTERS_DESC = "Ljava/lang/ThreadLocal;";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }
        if (matches(TALL_GRASS_TARGET, name, transformedName)) {
            return transformTallGrass(basicClass);
        }
        if (matches(EXPLOSION_TARGET, name, transformedName)) {
            return transformExplosion(basicClass);
        }
        return basicClass;
    }

    private static byte[] transformTallGrass(byte[] basicClass) {
        ClassNode node = read(basicClass);
        MethodNode survival = uniqueMethod(node, SURVIVAL_DESC,
            SURVIVAL_MCP, SURVIVAL_SRG, "survival");
        MethodNode getDrops = uniqueMethod(node, GET_DROPS_DESC,
            GET_DROPS, GET_DROPS, "drop-list");
        MethodNode grow = uniqueMethod(node, GROW_DESC,
            GROW_MCP, GROW_SRG, "bonemeal growth");
        requireReturns(survival, Opcodes.IRETURN, 1, "survival");
        requireReturns(getDrops, Opcodes.RETURN, 2, "drop-list");
        requireReturns(grow, Opcodes.RETURN, 1, "bonemeal growth");
        requireAbsentMethod(node, CHECK_DESC, "support-loss");
        requireAbsentMethod(node, CAN_PLACE_DESC, "placement");
        requireAbsentMethod(node, DROP_DESC, "chance-drop");

        boolean mcp = SURVIVAL_MCP.equals(survival.name);
        insertSurvivalGuard(survival);
        insertGetDropsGuard(getDrops);
        insertGrowGuard(grow);
        addCheckOverride(node, mcp ? CHECK_MCP : CHECK_SRG);
        addCanPlaceOverride(node, mcp ? CAN_PLACE_MCP : CAN_PLACE_SRG);
        addDropOverride(node, mcp ? DROP_MCP : DROP_SRG);
        return write(node);
    }

    private static byte[] transformExplosion(byte[] basicClass) {
        ClassNode node = read(basicClass);
        MethodNode explosion = uniqueMethod(node, EXPLODE_DESC,
            EXPLODE_MCP, EXPLODE_SRG, "explosion application");
        requireReturns(explosion, Opcodes.RETURN, 1, "explosion application");

        MethodInsnNode canDropCall = null;
        MethodInsnNode dropCall = null;
        for (AbstractInsnNode instruction : explosion.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) {
                continue;
            }
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (call.getOpcode() != Opcodes.INVOKEVIRTUAL
                    || !BLOCK_OWNER.equals(call.owner)) {
                continue;
            }
            if (CAN_DROP_DESC.equals(call.desc)
                    && (CAN_DROP_MCP.equals(call.name) || CAN_DROP_SRG.equals(call.name))) {
                if (canDropCall != null) {
                    throw failure("one Block can-drop call in Explosion");
                }
                canDropCall = call;
            } else if (DROP_DESC.equals(call.desc)
                    && (DROP_MCP.equals(call.name) || DROP_SRG.equals(call.name))) {
                if (dropCall != null) {
                    throw failure("one Block chance-drop call in Explosion");
                }
                dropCall = call;
            }
        }
        if (canDropCall == null || dropCall == null) {
            throw failure("the Block can-drop and chance-drop calls in Explosion");
        }
        AbstractInsnNode explosionLoad = previousMeaningful(canDropCall);
        AbstractInsnNode blockLoad = previousMeaningful(explosionLoad);
        if (!(explosionLoad instanceof VarInsnNode)
                || explosionLoad.getOpcode() != Opcodes.ALOAD
                || ((VarInsnNode) explosionLoad).var != 0
                || !(blockLoad instanceof VarInsnNode)
                || blockLoad.getOpcode() != Opcodes.ALOAD
                || ((VarInsnNode) blockLoad).var != 5) {
            throw failure("Explosion's local-5 Block and local-0 Explosion can-drop operands");
        }

        InsnList mark = new InsnList();
        mark.add(new VarInsnNode(Opcodes.ALOAD, 5));
        mark.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
            MARK_EXPLOSION_HOOK, MARK_EXPLOSION_DESC, false));
        explosion.instructions.insert(canDropCall, mark);
        explosion.instructions.insert(dropCall,
            new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
                END_EXPLOSION_HOOK, "()V", false));
        return write(node);
    }

    private static void insertSurvivalGuard(MethodNode method) {
        LabelNode original = new LabelNode();
        InsnList guard = new InsnList();
        guard.add(new VarInsnNode(Opcodes.ALOAD, 1));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 2));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 3));
        guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
            USE_HOOK, SURVIVAL_DESC, false));
        guard.add(new JumpInsnNode(Opcodes.IFEQ, original));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 1));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 2));
        guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
            STAY_HOOK, STAY_HOOK_DESC, false));
        guard.add(new InsnNode(Opcodes.IRETURN));
        guard.add(original);
        guard.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        method.instructions.insert(guard);
    }

    private static void insertGetDropsGuard(MethodNode method) {
        LabelNode original = new LabelNode();
        InsnList guard = new InsnList();
        guard.add(new VarInsnNode(Opcodes.ALOAD, 1));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 2));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 3));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 4));
        guard.add(new VarInsnNode(Opcodes.ILOAD, 5));
        guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
            GET_DROPS_HOOK, GET_DROPS_HOOK_DESC, false));
        guard.add(new JumpInsnNode(Opcodes.IFEQ, original));
        guard.add(new InsnNode(Opcodes.RETURN));
        guard.add(original);
        guard.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        method.instructions.insert(guard);
    }

    private static void insertGrowGuard(MethodNode method) {
        LabelNode original = new LabelNode();
        InsnList guard = new InsnList();
        guard.add(new VarInsnNode(Opcodes.ALOAD, 1));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 3));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 4));
        guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
            USE_HOOK, SURVIVAL_DESC, false));
        guard.add(new JumpInsnNode(Opcodes.IFEQ, original));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 1));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 3));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 4));
        guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
            GROW_HOOK, GROW_HOOK_DESC, false));
        guard.add(new InsnNode(Opcodes.RETURN));
        guard.add(original);
        guard.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        method.instructions.insert(guard);
    }

    private static void addCheckOverride(ClassNode node, String methodName) {
        MethodNode method = new MethodNode(Opcodes.ASM5, Opcodes.ACC_PROTECTED,
            methodName, CHECK_DESC, null, null);
        LabelNode delegate = new LabelNode();
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 3));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
            CHECK_HOOK, SURVIVAL_DESC, false));
        method.instructions.add(new JumpInsnNode(Opcodes.IFEQ, delegate));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.instructions.add(delegate);
        method.instructions.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 3));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
            BLOCK_BUSH_OWNER, methodName, CHECK_DESC, false));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        node.methods.add(method);
    }

    private static void addCanPlaceOverride(ClassNode node, String methodName) {
        MethodNode method = new MethodNode(Opcodes.ASM5, Opcodes.ACC_PUBLIC,
            methodName, CAN_PLACE_DESC, null, null);
        LabelNode delegate = new LabelNode();
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
            USE_PLACEMENT_HOOK, USE_PLACEMENT_DESC, false));
        method.instructions.add(new JumpInsnNode(Opcodes.IFEQ, delegate));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
            CAN_PLACE_HOOK, CAN_PLACE_DESC, false));
        method.instructions.add(new InsnNode(Opcodes.IRETURN));
        method.instructions.add(delegate);
        method.instructions.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
            BLOCK_BUSH_OWNER, methodName, CAN_PLACE_DESC, false));
        method.instructions.add(new InsnNode(Opcodes.IRETURN));
        node.methods.add(method);
    }

    private static void addDropOverride(ClassNode node, String methodName) {
        MethodNode method = new MethodNode(Opcodes.ASM5, Opcodes.ACC_PUBLIC,
            methodName, DROP_DESC, null, null);
        LabelNode delegate = new LabelNode();
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 3));
        method.instructions.add(new VarInsnNode(Opcodes.FLOAD, 4));
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 5));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, BLOCK_OWNER,
            HARVESTERS_FIELD, HARVESTERS_DESC));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
            "java/lang/ThreadLocal", "get", "()Ljava/lang/Object;", false));
        method.instructions.add(new TypeInsnNode(Opcodes.CHECKCAST,
            "net/minecraft/entity/player/EntityPlayer"));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
            DROP_HOOK, DROP_HOOK_DESC, false));
        method.instructions.add(new JumpInsnNode(Opcodes.IFEQ, delegate));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.instructions.add(delegate);
        method.instructions.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 3));
        method.instructions.add(new VarInsnNode(Opcodes.FLOAD, 4));
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 5));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
            BLOCK_BUSH_OWNER, methodName, DROP_DESC, false));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        node.methods.add(method);
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

    private static void requireAbsentMethod(ClassNode node, String descriptor,
            String point) {
        for (MethodNode method : node.methods) {
            if (descriptor.equals(method.desc)) {
                throw failure("no pre-existing " + point + " override " + descriptor);
            }
        }
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

    private static AbstractInsnNode previousMeaningful(AbstractInsnNode instruction) {
        AbstractInsnNode cursor = instruction == null ? null : instruction.getPrevious();
        while (cursor != null && cursor.getOpcode() < 0) {
            cursor = cursor.getPrevious();
        }
        return cursor;
    }

    private static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    private static byte[] write(ClassNode node) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static boolean matches(String target, String name, String transformedName) {
        return target.equals(name) || target.equals(transformedName);
    }

    private static IllegalStateException failure(String point) {
        return new IllegalStateException("Caves Not Cliffs tall grass transformer "
            + "could not verify " + point);
    }
}
