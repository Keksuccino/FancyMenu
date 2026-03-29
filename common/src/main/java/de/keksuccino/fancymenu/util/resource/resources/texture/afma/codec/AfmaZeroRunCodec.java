package de.keksuccino.fancymenu.util.resource.resources.texture.afma.codec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Small zero-run codec used by AFMA section packing for metadata-heavy
 * substreams with frequent zero bytes.
 */
public final class AfmaZeroRunCodec {

    private static final int MAX_LITERAL_RUN = 128;
    private static final int MAX_ZERO_RUN = 128;

    private AfmaZeroRunCodec() {
    }

    public static byte[] compress(byte[] input) {
        if (input.length == 0) {
            return input;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream(input.length);
        ByteArrayOutputStream literals = new ByteArrayOutputStream();
        int index = 0;
        while (index < input.length) {
            int zeroRun = countZeroRun(input, index);
            if (zeroRun >= 4) {
                flushLiterals(literals, out);
                int remaining = zeroRun;
                while (remaining > 0) {
                    int chunk = Math.min(MAX_ZERO_RUN, remaining);
                    out.write(128 + (chunk - 1));
                    remaining -= chunk;
                }
                index += zeroRun;
                continue;
            }

            literals.write(input[index]);
            index++;
            if (literals.size() == MAX_LITERAL_RUN) {
                flushLiterals(literals, out);
            }
        }

        flushLiterals(literals, out);
        return out.toByteArray();
    }

    public static byte[] decompress(byte[] input) throws IOException {
        if (input.length == 0) {
            return input;
        }

        ByteArrayInputStream in = new ByteArrayInputStream(input);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while (in.available() > 0) {
            int tag = in.read();
            if (tag < 0) {
                break;
            }
            if (tag < 128) {
                int length = tag + 1;
                byte[] literals = in.readNBytes(length);
                if (literals.length != length) {
                    throw new IOException("Zero-run literal run ended early");
                }
                out.write(literals, 0, literals.length);
                continue;
            }

            int length = (tag - 128) + 1;
            out.write(new byte[length], 0, length);
        }
        return out.toByteArray();
    }

    private static int countZeroRun(byte[] input, int offset) {
        int end = Math.min(input.length, offset + MAX_ZERO_RUN);
        int index = offset;
        while (index < end && input[index] == 0) {
            index++;
        }
        return index - offset;
    }

    private static void flushLiterals(ByteArrayOutputStream literals, ByteArrayOutputStream out) {
        if (literals.size() == 0) {
            return;
        }
        byte[] bytes = literals.toByteArray();
        int offset = 0;
        while (offset < bytes.length) {
            int chunk = Math.min(MAX_LITERAL_RUN, bytes.length - offset);
            out.write(chunk - 1);
            out.write(bytes, offset, chunk);
            offset += chunk;
        }
        literals.reset();
    }
}
