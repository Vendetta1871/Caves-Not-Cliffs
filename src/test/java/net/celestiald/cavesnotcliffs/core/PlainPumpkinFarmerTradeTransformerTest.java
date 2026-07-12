package net.celestiald.cavesnotcliffs.core;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.village.MerchantRecipeList;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PlainPumpkinFarmerTradeTransformerTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void actualForgeBinTargetGetsOneIdentityScopedHook()
            throws Exception {
        byte[] transformed = new PlainPumpkinFarmerTradeTransformer().transform(
                PlainPumpkinFarmerTradeTransformer.TARGET,
                PlainPumpkinFarmerTradeTransformer.TARGET, targetBytes());
        assertHookShape(transformed);

        Class<?> type = new TargetLoader(transformed)
                .loadClass(PlainPumpkinFarmerTradeTransformer.TARGET);
        Constructor<?> constructor = type.getConstructor(
                Item.class, EntityVillager.PriceInfo.class);
        Block block = new Block(Material.GOURD)
                .setRegistryName("cavesnotcliffs", "pumpkin");
        ItemBlock plain = (ItemBlock) new ItemBlock(block)
                .setRegistryName("cavesnotcliffs", "pumpkin");
        EntityVillager.ITradeList marked = (EntityVillager.ITradeList)
                constructor.newInstance(plain, null);
        MerchantRecipeList recipes = new MerchantRecipeList();
        CountingRandom random = new CountingRandom();

        marked.addMerchantRecipe(null, recipes, random);
        assertEquals(0, random.calls);
        assertEquals(1, recipes.size());
        assertSame(plain, recipes.get(0).getItemToBuy().getItem());
        // This child-loader instance is deliberately not the installed global source.
        assertEquals(1, recipes.get(0).getItemToBuy().getCount());
        assertEquals(7, recipes.get(0).getMaxTradeUses());
    }

    @Test
    public void srgLikeMembersAndObfuscatedLaunchNameUseDescriptorAnchors()
            throws IOException {
        byte[] transformed = new PlainPumpkinFarmerTradeTransformer().transform(
                "ady$a", PlainPumpkinFarmerTradeTransformer.TARGET,
                renameTargetMembers(targetBytes()));
        assertHookShape(transformed);
    }

    @Test
    public void missingMethodAnchorFailsClearly() {
        ClassNode node = baseClass();
        node.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "item",
                PlainPumpkinFarmerTradeTransformer.ITEM_DESC, null, null));
        node.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "price",
                PlainPumpkinFarmerTradeTransformer.PRICE_DESC, null, null));
        assertTransformFails(write(node), "addMerchantRecipe descriptor");
    }

    @Test
    public void changedAppendOrReturnShapeFailsClearly() {
        ClassNode noAppend = baseClass();
        addRequiredFields(noAppend);
        MethodNode method = new MethodNode(Opcodes.ASM5, Opcodes.ACC_PUBLIC,
                "addMerchantRecipe",
                PlainPumpkinFarmerTradeTransformer.ADD_RECIPE_DESC, null, null);
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        noAppend.methods.add(method);
        assertTransformFails(write(noAppend), "MerchantRecipeList append");

        ClassNode twoReturns = read(targetBytesUnchecked());
        addRecipeMethod(twoReturns).instructions.add(new InsnNode(Opcodes.RETURN));
        assertTransformFails(write(twoReturns), "one method return");
    }

    @Test
    public void duplicateFieldDescriptorFailsClearly() {
        ClassNode node = read(targetBytesUnchecked());
        node.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "otherItem",
                PlainPumpkinFarmerTradeTransformer.ITEM_DESC, null, null));
        assertTransformFails(write(node), "unique buying-item field");
    }

    @Test
    public void unrelatedClassesAreUntouched() {
        byte[] original = write(baseClass());
        assertSame(original, new PlainPumpkinFarmerTradeTransformer().transform(
                "example.Unrelated", "example.Unrelated", original));
    }

    private static void assertHookShape(byte[] bytes) {
        ClassNode node = read(bytes);
        MethodNode method = addRecipeMethod(node);
        int hooks = 0;
        int itemReads = 0;
        int priceReads = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode) {
                FieldInsnNode field = (FieldInsnNode) instruction;
                if (field.getOpcode() == Opcodes.GETFIELD
                        && PlainPumpkinFarmerTradeTransformer.TARGET_INTERNAL
                                .equals(field.owner)) {
                    if (PlainPumpkinFarmerTradeTransformer.ITEM_DESC.equals(field.desc)) {
                        itemReads++;
                    } else if (PlainPumpkinFarmerTradeTransformer.PRICE_DESC
                            .equals(field.desc)) {
                        priceReads++;
                    }
                }
            } else if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.getOpcode() == Opcodes.INVOKESTATIC
                        && PlainPumpkinFarmerTradeTransformer.HOOK_OWNER.equals(call.owner)
                        && PlainPumpkinFarmerTradeTransformer.HOOK_NAME.equals(call.name)
                        && PlainPumpkinFarmerTradeTransformer.HOOK_DESC.equals(call.desc)) {
                    hooks++;
                }
            }
        }
        assertEquals(1, itemReads);
        assertEquals(2, priceReads);
        assertEquals(1, hooks);
    }

    private static byte[] renameTargetMembers(byte[] bytes) {
        ClassNode node = read(bytes);
        Map<String, String> renamed = new HashMap<>();
        for (FieldNode field : node.fields) {
            if (PlainPumpkinFarmerTradeTransformer.ITEM_DESC.equals(field.desc)) {
                renamed.put(field.name, "field_179405_a");
                field.name = "field_179405_a";
            } else if (PlainPumpkinFarmerTradeTransformer.PRICE_DESC.equals(field.desc)) {
                renamed.put(field.name, "field_179404_b");
                field.name = "field_179404_b";
            }
        }
        for (MethodNode method : node.methods) {
            if (PlainPumpkinFarmerTradeTransformer.ADD_RECIPE_DESC.equals(method.desc)) {
                method.name = "func_190888_a";
            }
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof FieldInsnNode)) {
                    continue;
                }
                FieldInsnNode field = (FieldInsnNode) instruction;
                if (PlainPumpkinFarmerTradeTransformer.TARGET_INTERNAL.equals(field.owner)
                        && renamed.containsKey(field.name)) {
                    field.name = renamed.get(field.name);
                }
            }
        }
        return write(node);
    }

    private static void addRequiredFields(ClassNode node) {
        node.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "item",
                PlainPumpkinFarmerTradeTransformer.ITEM_DESC, null, null));
        node.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "price",
                PlainPumpkinFarmerTradeTransformer.PRICE_DESC, null, null));
    }

    private static ClassNode baseClass() {
        ClassNode node = new ClassNode(Opcodes.ASM5);
        node.version = Opcodes.V1_8;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = PlainPumpkinFarmerTradeTransformer.TARGET_INTERNAL;
        node.superName = "java/lang/Object";
        return node;
    }

    private static MethodNode addRecipeMethod(ClassNode node) {
        MethodNode result = null;
        for (MethodNode method : node.methods) {
            if (!PlainPumpkinFarmerTradeTransformer.ADD_RECIPE_DESC.equals(method.desc)) {
                continue;
            }
            assertSame(null, result);
            result = method;
        }
        assertNotNull(result);
        return result;
    }

    private static void assertTransformFails(byte[] bytes, String point) {
        try {
            new PlainPumpkinFarmerTradeTransformer().transform(
                    PlainPumpkinFarmerTradeTransformer.TARGET,
                    PlainPumpkinFarmerTradeTransformer.TARGET, bytes);
            fail("A changed farmer-trade anchor must abort the transform");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains(
                    "farmer pumpkin trade transformer"));
            assertTrue(expected.getMessage().contains(point));
            assertTrue(expected.getMessage().contains(
                    PlainPumpkinFarmerTradeTransformer.TARGET));
        }
    }

    private static byte[] targetBytesUnchecked() {
        try {
            return targetBytes();
        } catch (IOException failure) {
            throw new AssertionError(failure);
        }
    }

    private static byte[] targetBytes() throws IOException {
        String path = PlainPumpkinFarmerTradeTransformer.TARGET_INTERNAL + ".class";
        InputStream input = PlainPumpkinFarmerTradeTransformerTest.class
                .getClassLoader().getResourceAsStream(path);
        if (input == null) {
            throw new IOException("Missing ForgeBin target " + path);
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        } finally {
            input.close();
        }
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

    private static final class CountingRandom extends Random {
        private int calls;

        @Override
        public int nextInt(int bound) {
            calls++;
            return super.nextInt(bound);
        }
    }

    private static final class TargetLoader extends ClassLoader {
        private final byte[] bytes;

        private TargetLoader(byte[] bytes) {
            super(PlainPumpkinFarmerTradeTransformerTest.class.getClassLoader());
            this.bytes = bytes;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException {
            if (!PlainPumpkinFarmerTradeTransformer.TARGET.equals(name)) {
                return super.loadClass(name, resolve);
            }
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) {
                loaded = defineClass(name, bytes, 0, bytes.length);
            }
            if (resolve) {
                resolveClass(loaded);
            }
            return loaded;
        }
    }
}
