package net.celestiald.cavesnotcliffs.content;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class BeeAngerTargetResolverTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void savedUuidResolvesAnyLivingEntityNotOnlyPlayers() {
        UUID wanted = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");
        EntityArmorStand other = living(UUID.randomUUID());
        EntityArmorStand target = living(wanted);

        EntityLivingBase resolved = BeeAngerTargetResolver.findLoaded(
                Arrays.asList(other, target), wanted);

        assertSame(target, resolved);
    }

    @Test
    public void absentOrDeadSavedTargetDoesNotRetargetAnotherEntity() {
        UUID wanted = UUID.fromString("fedcba98-7654-3210-fedc-ba9876543210");
        EntityArmorStand unrelated = living(UUID.randomUUID());
        assertNull(BeeAngerTargetResolver.findLoaded(
                Arrays.asList(unrelated), wanted));

        EntityArmorStand dead = living(wanted);
        dead.setDead();
        assertNull(BeeAngerTargetResolver.findLoaded(Arrays.asList(dead), wanted));
    }

    private static EntityArmorStand living(UUID id) {
        EntityArmorStand entity = new EntityArmorStand(null);
        entity.setUniqueId(id);
        return entity;
    }
}
