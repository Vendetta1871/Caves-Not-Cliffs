package net.celestiald.cavesnotcliffs;

import org.junit.Test;

import java.io.DataInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CavesNotCliffsVariablesDedicatedServerTest {
    @Test
    public void commonSavedDataMessagesHaveNoClientClassDependencies() throws Exception {
        assertNoClientReferences(CavesNotCliffsVariables.class);
    }

    private static void assertNoClientReferences(Class<?> type) throws Exception {
        String resource = type.getName().replace('.', '/') + ".class";
        InputStream raw = type.getClassLoader().getResourceAsStream(resource);
        assertNotNull(raw);
        for (String name : classReferences(new DataInputStream(raw))) {
            assertFalse(type.getName() + " links " + name,
                    name.startsWith("net/minecraft/client/"));
        }
    }

    private static List<String> classReferences(DataInputStream input) throws Exception {
        try {
            assertTrue(input.readInt() == 0xCAFEBABE);
            input.readUnsignedShort();
            input.readUnsignedShort();
            int count = input.readUnsignedShort();
            String[] utf8 = new String[count];
            int[] classNames = new int[count];
            for (int index = 1; index < count; ++index) {
                int tag = input.readUnsignedByte();
                switch (tag) {
                    case 1:
                        utf8[index] = input.readUTF();
                        break;
                    case 3:
                    case 4:
                        input.skipBytes(4);
                        break;
                    case 5:
                    case 6:
                        input.skipBytes(8);
                        ++index;
                        break;
                    case 7:
                        classNames[index] = input.readUnsignedShort();
                        break;
                    case 8:
                    case 16:
                        input.skipBytes(2);
                        break;
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 18:
                        input.skipBytes(4);
                        break;
                    case 15:
                        input.skipBytes(3);
                        break;
                    default:
                        throw new AssertionError("Unexpected constant-pool tag " + tag);
                }
            }
            List<String> names = new ArrayList<>();
            for (int className : classNames) {
                if (className != 0) {
                    names.add(utf8[className]);
                }
            }
            return names;
        } finally {
            input.close();
        }
    }
}
