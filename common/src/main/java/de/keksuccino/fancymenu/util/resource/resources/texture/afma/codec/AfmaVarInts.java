package de.keksuccino.fancymenu.util.resource.resources.texture.afma.codec;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Compact unsigned and zig-zag integer encoding used by the AFMA atlas/program
 * stream.
 */
public final class AfmaVarInts {

    private AfmaVarInts() {
    }

    public static void writeUnsigned(@NotNull ByteArrayOutputStream out, int value) {
        long remaining = Integer.toUnsignedLong(value);
        while ((remaining & ~0x7FL) != 0L) {
            out.write((int) ((remaining & 0x7FL) | 0x80L));
            remaining >>>= 7;
        }
        out.write((int) remaining);
    }

    public static void writeUnsigned(@NotNull OutputStream out, int value) throws IOException {
        long remaining = Integer.toUnsignedLong(value);
        while ((remaining & ~0x7FL) != 0L) {
            out.write((int) ((remaining & 0x7FL) | 0x80L));
            remaining >>>= 7;
        }
        out.write((int) remaining);
    }

    public static int readUnsigned(@NotNull ByteArrayInputStream in) throws IOException {
        long result = 0L;
        int shift = 0;
        while (shift < 35) {
            int current = in.read();
            if (current < 0) {
                throw new IOException("Unexpected end of varint stream");
            }
            result |= (long) (current & 0x7F) << shift;
            if ((current & 0x80) == 0) {
                if (result > Integer.MAX_VALUE) {
                    throw new IOException("Unsigned varint exceeds int range");
                }
                return (int) result;
            }
            shift += 7;
        }
        throw new IOException("Varint is too long");
    }

    public static int zigZagEncode(int value) {
        return (value << 1) ^ (value >> 31);
    }

    public static int zigZagDecode(int value) {
        return (value >>> 1) ^ -(value & 1);
    }
}
