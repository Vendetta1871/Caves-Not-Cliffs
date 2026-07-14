package net.celestiald.cavesnotcliffs.stonecutter;

import org.junit.Test;

import java.io.DataInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CncGuiHandlerDedicatedServerTest {
    @Test
    public void commonHandlerHasNoClientClassConstantPoolDependencies() throws Exception {
        String resource = CncGuiHandler.class.getName().replace('.', '/') + ".class";
        InputStream raw = CncGuiHandler.class.getClassLoader().getResourceAsStream(resource);
        assertNotNull(raw);
        List<String> classes = classReferences(new DataInputStream(raw));
        assertTrue(classes.contains("net/minecraftforge/fml/common/network/IGuiHandler"));
        for (String name : classes) {
            assertFalse(name, name.startsWith("net/minecraft/client/"));
            assertFalse(name, name.equals(
                    "net/celestiald/cavesnotcliffs/client/GuiStonecutter"));
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
