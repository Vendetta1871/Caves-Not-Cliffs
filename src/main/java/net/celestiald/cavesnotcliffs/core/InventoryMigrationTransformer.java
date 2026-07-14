package net.celestiald.cavesnotcliffs.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Migrates legacy registry names at ItemStack's shared NBT deserialization choke point. */
public final class InventoryMigrationTransformer implements IClassTransformer {
    static final String TARGET = "net.minecraft.item.ItemStack";
    static final String NBT_CONSTRUCTOR_DESC = "(Lnet/minecraft/nbt/NBTTagCompound;)V";
    static final String HOOK_OWNER =
            "net/celestiald/cavesnotcliffs/registry/LegacyInventoryMigration";
    static final String HOOK_NAME = "migrateSerializedStack";
    static final String HOOK_DESC = "(Lnet/minecraft/nbt/NBTTagCompound;)Z";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !(TARGET.equals(name) || TARGET.equals(transformedName))) {
            return basicClass;
        }

        ClassNode node = new ClassNode(Opcodes.ASM5);
        new ClassReader(basicClass).accept(node, 0);
        MethodNode constructor = null;
        for (MethodNode method : node.methods) {
            if (!"<init>".equals(method.name) || !NBT_CONSTRUCTOR_DESC.equals(method.desc)) {
                continue;
            }
            if (constructor != null) {
                throw failure("multiple NBT constructors");
            }
            constructor = method;
        }
        if (constructor == null) {
            throw failure("NBT constructor was not found");
        }

        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
                HOOK_NAME, HOOK_DESC, false));
        hook.add(new InsnNode(Opcodes.POP));
        constructor.instructions.insert(hook);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static IllegalStateException failure(String detail) {
        return new IllegalStateException(
                "Caves Not Cliffs inventory migration transformer failed: " + detail);
    }
}
