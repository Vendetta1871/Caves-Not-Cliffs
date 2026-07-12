package net.celestiald.cavesnotcliffs.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Finalizes the fixed Java 1.18.2 pumpkin offer after Forge creates it. */
public final class PlainPumpkinFarmerTradeTransformer implements IClassTransformer {
    static final String TARGET =
            "net.minecraft.entity.passive.EntityVillager$EmeraldForItems";
    static final String TARGET_INTERNAL = TARGET.replace('.', '/');
    static final String ITEM_DESC = "Lnet/minecraft/item/Item;";
    static final String PRICE_DESC =
            "Lnet/minecraft/entity/passive/EntityVillager$PriceInfo;";
    static final String RECIPES_DESC = "Lnet/minecraft/village/MerchantRecipeList;";
    static final String ADD_RECIPE_DESC = "(Lnet/minecraft/entity/IMerchant;"
            + RECIPES_DESC + "Ljava/util/Random;)V";
    static final String LIST_OWNER = "net/minecraft/village/MerchantRecipeList";
    static final String LIST_ADD_DESC = "(Ljava/lang/Object;)Z";
    static final String HOOK_OWNER =
            "net/celestiald/cavesnotcliffs/content/PlainPumpkinContent";
    static final String HOOK_NAME = "finishFarmerPumpkinTrade";
    static final String HOOK_DESC = "(Ljava/lang/Object;" + RECIPES_DESC + ")V";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null
                || !(TARGET.equals(name) || TARGET.equals(transformedName))) {
            return basicClass;
        }

        ClassNode node = new ClassNode(Opcodes.ASM5);
        new ClassReader(basicClass).accept(node, 0);
        FieldNode item = uniqueField(node, ITEM_DESC, "buying-item field");
        FieldNode price = uniqueField(node, PRICE_DESC, "price field");
        MethodNode addRecipe = uniqueMethod(node);

        int listAdds = 0;
        int returns = 0;
        for (AbstractInsnNode instruction : addRecipe.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (LIST_OWNER.equals(call.owner) && LIST_ADD_DESC.equals(call.desc)) {
                    listAdds++;
                }
            }
            if (instruction.getOpcode() != Opcodes.RETURN) {
                continue;
            }
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
            hook.add(new VarInsnNode(Opcodes.ALOAD, 2));
            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER,
                    HOOK_NAME, HOOK_DESC, false));
            addRecipe.instructions.insertBefore(instruction, hook);
            returns++;
        }
        requireCount(listAdds, 1, "one MerchantRecipeList append");
        requireCount(returns, 1, "one method return");

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static FieldNode uniqueField(ClassNode node, String descriptor,
            String point) {
        FieldNode result = null;
        for (FieldNode field : node.fields) {
            if ((field.access & Opcodes.ACC_STATIC) != 0
                    || !descriptor.equals(field.desc)) {
                continue;
            }
            if (result != null) {
                throw failure("a unique " + point);
            }
            result = field;
        }
        if (result == null) {
            throw failure(point);
        }
        return result;
    }

    private static MethodNode uniqueMethod(ClassNode node) {
        MethodNode result = null;
        for (MethodNode method : node.methods) {
            if ((method.access & Opcodes.ACC_STATIC) != 0
                    || !ADD_RECIPE_DESC.equals(method.desc)) {
                continue;
            }
            if (result != null) {
                throw failure("a unique addMerchantRecipe descriptor");
            }
            result = method;
        }
        if (result == null) {
            throw failure("the addMerchantRecipe descriptor");
        }
        return result;
    }

    private static void requireCount(int actual, int expected, String point) {
        if (actual != expected) {
            throw failure(point + " (found " + actual + ")");
        }
    }

    private static IllegalStateException failure(String point) {
        return new IllegalStateException(
                "Caves Not Cliffs farmer pumpkin trade transformer could not verify "
                        + point + " in " + TARGET);
    }
}
