package net.celestiald.cavesnotcliffs.tile;

import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BeehiveNbtContractTest {
    @BeforeClass
    public static void bootstrapAndRegisterTile() {
        Bootstrap.register();
        if (TileEntity.getKey(TileEntityBeehive.class) == null) {
            TileEntity.register("cavesnotcliffs:beehive", TileEntityBeehive.class);
        }
    }

    @Test
    public void hiveCapacityAndEntryShapeAreExact() {
        TileEntityBeehive hive = new TileEntityBeehive();
        for (int index = 0; index < 4; index++) {
            NBTTagCompound bee = beeTag("bee-" + index);
            hive.storeBee(bee, index * 100, index % 2 == 0);
        }
        assertEquals(3, hive.getOccupantCount());
        assertTrue(hive.isFull());

        NBTTagList bees = hive.writeBees();
        assertEquals(3, bees.tagCount());
        for (int index = 0; index < bees.tagCount(); index++) {
            NBTTagCompound entry = bees.getCompoundTagAt(index);
            assertEquals(index * 100, entry.getInteger("TicksInHive"));
            assertEquals(index % 2 == 0 ? 2400 : 600,
                    entry.getInteger("MinOccupationTicks"));
            assertTrue(entry.hasKey("EntityData", 10));
        }
    }

    @Test
    public void ignoredRuntimeTagsNeverLeakIntoStoredEntityData() {
        TileEntityBeehive hive = new TileEntityBeehive();
        NBTTagCompound bee = beeTag("kept");
        bee.setInteger("Air", 300);
        bee.setTag("Motion", new NBTTagList());
        bee.setUniqueId("UUID", UUID.fromString(
                "00112233-4455-6677-8899-aabbccddeeff"));
        bee.setInteger("CannotEnterHiveTicks", 99);
        hive.storeBee(bee, 0, true);

        NBTTagCompound stored = hive.writeBees().getCompoundTagAt(0)
                .getCompoundTag("EntityData");
        assertEquals("kept", stored.getString("CustomName"));
        assertTrue(stored.getBoolean("HasNectar"));
        assertFalse(stored.hasKey("Air"));
        assertFalse(stored.hasKey("Motion"));
        assertFalse(stored.hasUniqueId("UUID"));
        assertFalse(stored.hasKey("CannotEnterHiveTicks"));
    }

    @Test
    public void saveReloadPreservesBeesFlowerAndHoneyWithoutDuplication() {
        TileEntityBeehive original = new TileEntityBeehive();
        original.setPos(new BlockPos(12, 34, -56));
        original.storeBee(beeTag("first"), 599, false);
        original.storeBee(beeTag("second"), 2399, true);
        original.setHoneyLevel(5);

        NBTTagCompound saved = original.writeToNBT(new NBTTagCompound());
        assertTrue(saved.hasKey("Bees", 9));
        assertEquals(5, saved.getInteger("HoneyLevel"));

        TileEntityBeehive loaded = new TileEntityBeehive();
        loaded.readFromNBT(saved);
        assertEquals(new BlockPos(12, 34, -56), loaded.getPos());
        assertEquals(2, loaded.getOccupantCount());
        assertEquals(5, loaded.getHoneyLevel());
        assertEquals(2, loaded.writeBees().tagCount());
        assertEquals("first", loaded.writeBees().getCompoundTagAt(0)
                .getCompoundTag("EntityData").getString("CustomName"));
    }

    private static NBTTagCompound beeTag(String name) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", "cavesnotcliffs:bee");
        tag.setString("CustomName", name);
        tag.setInteger("Age", -24000);
        tag.setInteger("InLove", 0);
        return tag;
    }
}
