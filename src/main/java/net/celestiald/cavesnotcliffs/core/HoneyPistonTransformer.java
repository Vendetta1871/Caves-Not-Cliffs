package net.celestiald.cavesnotcliffs.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Adds only the honey/slime incompatibility omitted by Forge 1.12's sticky-block hook.
 * Method and field discovery is descriptor-based so both MCP development and SRG runtime
 * bytecode take the same verified path.
 */
public final class HoneyPistonTransformer implements IClassTransformer {
    public static final String TARGET =
            "net.minecraft.block.state.BlockPistonStructureHelper";
    static final String TARGET_INTERNAL = TARGET.replace('.', '/');
    static final String WORLD_DESC = "Lnet/minecraft/world/World;";
    static final String FACING_DESC = "Lnet/minecraft/util/EnumFacing;";
    static final String POS_DESC = "Lnet/minecraft/util/math/BlockPos;";
    static final String STATE_DESC = "Lnet/minecraft/block/state/IBlockState;";
    static final String ADD_LINE_DESC = "(" + POS_DESC + FACING_DESC + ")Z";
    static final String BRANCH_DESC = "(" + POS_DESC + ")Z";
    static final String HOOK_OWNER =
            "net/celestiald/cavesnotcliffs/content/HoneyPistonHooks";
    static final String WORLD_POS_FACING = "(" + WORLD_DESC + POS_DESC
            + FACING_DESC + ")Z";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !(TARGET.equals(transformedName)
                || TARGET.equals(name))) {
            return basicClass;
        }
        ClassNode node = new ClassNode(Opcodes.ASM5);
        new ClassReader(basicClass).accept(node, 0);
        FieldNode world = uniqueField(node, WORLD_DESC);
        FieldNode moveDirection = uniqueField(node, FACING_DESC);
        MethodNode addLine = uniquePrivateMethod(node, ADD_LINE_DESC);
        MethodNode addBranch = uniquePrivateMethod(node, BRANCH_DESC);

        injectBackwardAdhesion(addLine, world, moveDirection);
        injectBranchAdhesion(addBranch, addLine.name, world);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static void injectBackwardAdhesion(MethodNode method, FieldNode world,
            FieldNode moveDirection) {
        MethodInsnNode sticky = null;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if ("net/minecraft/block/Block".equals(call.owner)
                        && "isStickyBlock".equals(call.name)
                        && ("(" + STATE_DESC + ")Z").equals(call.desc)) {
                    sticky = call;
                    break;
                }
            }
        }
        if (sticky == null || !(nextMeaningful(sticky) instanceof JumpInsnNode)) {
            throw failure("sticky-loop exit", method);
        }
        JumpInsnNode stickyExit = (JumpInsnNode) nextMeaningful(sticky);
        if (stickyExit.getOpcode() != Opcodes.IFEQ) {
            throw failure("sticky-loop IFEQ", method);
        }

        MethodInsnNode neighborLookup = null;
        for (AbstractInsnNode cursor = stickyExit.getNext(); cursor != null;
                cursor = cursor.getNext()) {
            if (cursor instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) cursor;
                if ("net/minecraft/world/World".equals(call.owner)
                        && ("(" + POS_DESC + ")" + STATE_DESC).equals(call.desc)) {
                    neighborLookup = call;
                    break;
                }
            }
        }
        if (neighborLookup == null
                || !(previousMeaningful(neighborLookup) instanceof VarInsnNode)
                || !(nextMeaningful(neighborLookup) instanceof VarInsnNode)) {
            throw failure("backward neighbor lookup", method);
        }
        VarInsnNode positionLoad = (VarInsnNode) previousMeaningful(neighborLookup);
        VarInsnNode stateStore = (VarInsnNode) nextMeaningful(neighborLookup);
        if (positionLoad.getOpcode() != Opcodes.ALOAD
                || stateStore.getOpcode() != Opcodes.ASTORE) {
            throw failure("backward neighbor locals", method);
        }

        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        hook.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET_INTERNAL,
                world.name, world.desc));
        hook.add(new VarInsnNode(Opcodes.ALOAD, positionLoad.var));
        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        hook.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET_INTERNAL,
                moveDirection.name, moveDirection.desc));
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
                "canStickBehind", WORLD_POS_FACING, false));
        hook.add(new JumpInsnNode(Opcodes.IFEQ, stickyExit.label));
        method.instructions.insert(stateStore, hook);
    }

    private static void injectBranchAdhesion(MethodNode method, String addLineName,
            FieldNode world) {
        MethodInsnNode addLineCall = null;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (TARGET_INTERNAL.equals(call.owner)
                        && addLineName.equals(call.name)
                        && ADD_LINE_DESC.equals(call.desc)) {
                    addLineCall = call;
                    break;
                }
            }
        }
        if (addLineCall == null
                || !(nextMeaningful(addLineCall) instanceof JumpInsnNode)) {
            throw failure("branch add-line call", method);
        }
        JumpInsnNode success = (JumpInsnNode) nextMeaningful(addLineCall);
        if (success.getOpcode() != Opcodes.IFNE) {
            throw failure("branch success jump", method);
        }

        AbstractInsnNode directionLoadNode = previousMeaningful(addLineCall);
        if (!(directionLoadNode instanceof VarInsnNode)
                || directionLoadNode.getOpcode() != Opcodes.ALOAD) {
            throw failure("branch direction local", method);
        }
        int directionVar = ((VarInsnNode) directionLoadNode).var;
        AbstractInsnNode offsetCall = previousMeaningful(directionLoadNode);
        AbstractInsnNode offsetDirection = previousMeaningful(offsetCall);
        AbstractInsnNode sourceLoad = previousMeaningful(offsetDirection);
        AbstractInsnNode anchor = previousMeaningful(sourceLoad);
        if (!(offsetCall instanceof MethodInsnNode)
                || !(sourceLoad instanceof VarInsnNode)
                || ((VarInsnNode) sourceLoad).var != 1
                || !(anchor instanceof VarInsnNode)
                || anchor.getOpcode() != Opcodes.ALOAD
                || ((VarInsnNode) anchor).var != 0) {
            throw failure("branch call shape", method);
        }

        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        hook.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET_INTERNAL,
                world.name, world.desc));
        hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
        hook.add(new VarInsnNode(Opcodes.ALOAD, directionVar));
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
                "canAttachBranch", WORLD_POS_FACING, false));
        hook.add(new JumpInsnNode(Opcodes.IFEQ, success.label));
        method.instructions.insertBefore(anchor, hook);
    }

    private static FieldNode uniqueField(ClassNode node, String descriptor) {
        FieldNode result = null;
        for (FieldNode field : node.fields) {
            if (!descriptor.equals(field.desc)) {
                continue;
            }
            if (result != null) {
                throw new IllegalStateException("Caves Not Cliffs piston transformer found "
                        + "multiple fields with descriptor " + descriptor);
            }
            result = field;
        }
        if (result == null) {
            throw new IllegalStateException("Caves Not Cliffs piston transformer could not "
                    + "find field " + descriptor);
        }
        return result;
    }

    private static MethodNode uniquePrivateMethod(ClassNode node, String descriptor) {
        MethodNode result = null;
        for (MethodNode method : node.methods) {
            if ((method.access & Opcodes.ACC_PRIVATE) == 0
                    || !descriptor.equals(method.desc)) {
                continue;
            }
            if (result != null) {
                throw new IllegalStateException("Caves Not Cliffs piston transformer found "
                        + "multiple private methods " + descriptor);
            }
            result = method;
        }
        if (result == null) {
            throw new IllegalStateException("Caves Not Cliffs piston transformer could not "
                    + "find private method " + descriptor);
        }
        return result;
    }

    private static IllegalStateException failure(String point, MethodNode method) {
        return new IllegalStateException("Caves Not Cliffs piston transformer could not "
                + "verify " + point + " in " + method.name + method.desc);
    }

    private static AbstractInsnNode nextMeaningful(AbstractInsnNode instruction) {
        AbstractInsnNode cursor = instruction.getNext();
        while (cursor != null && cursor.getOpcode() < 0) {
            cursor = cursor.getNext();
        }
        return cursor;
    }

    private static AbstractInsnNode previousMeaningful(AbstractInsnNode instruction) {
        AbstractInsnNode cursor = instruction.getPrevious();
        while (cursor != null && cursor.getOpcode() < 0) {
            cursor = cursor.getPrevious();
        }
        return cursor;
    }
}
