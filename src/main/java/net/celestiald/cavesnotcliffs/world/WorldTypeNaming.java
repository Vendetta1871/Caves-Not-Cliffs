package net.celestiald.cavesnotcliffs.world;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Pure deterministic naming rules for hidden world-type wrappers. */
public final class WorldTypeNaming {
    public static final int MAX_WORLD_TYPE_NAME_LENGTH = 16;
    private static final String MODDED_PREFIX = "cnc_m_";

    private WorldTypeNaming() {
    }

    public static String moddedWrapperName(String baseName, String className) {
        String input = requireText(baseName, "baseName") + '\0' + requireText(className, "className");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder name = new StringBuilder(MODDED_PREFIX);
            for (int index = 0; index < 5; index++) {
                name.append(String.format("%02x", digest[index] & 0xff));
            }
            if (name.length() > MAX_WORLD_TYPE_NAME_LENGTH) {
                throw new IllegalStateException("Generated world type name is too long: " + name);
            }
            return name.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("Every Java runtime must provide SHA-1", exception);
        }
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(label + " must not be empty");
        }
        return value;
    }
}
