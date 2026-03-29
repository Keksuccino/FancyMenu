package de.keksuccino.fancymenu.util.resource.resources.texture.afma.codec;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Lightweight LZ-style section codec used by AFMA's packed substreams.
 */
public final class AfmaMiniLzCodec {

    private static final int MIN_MATCH = 4;
    private static final int MAX_LITERAL_RUN = 128;
    private static final int MAX_RLE_RUN = 67;
    private static final int MAX_MATCH_RUN = 67;
    private static final int HASH_SIZE = 1 << 16;
    private static final int MAX_CHAIN = 24;
    private static final int MAX_DISTANCE = 1 << 20;

    private AfmaMiniLzCodec() {
    }

    public static byte[] compress(byte[] input) throws IOException {
        if (input.length == 0) {
            return input;
        }

        int[] head = new int[HASH_SIZE];
        int[] prev = new int[input.length];
        Arrays.fill(head, -1);
        Arrays.fill(prev, -1);

        ByteArrayOutputStream out = new ByteArrayOutputStream(input.length);
        ByteArrayOutputStream literals = new ByteArrayOutputStream();

        int position = 0;
        while (position < input.length) {
            int rleLength = measureRle(input, position);
            Match bestMatch = findBestMatch(input, position, head, prev);

            if (rleLength >= MIN_MATCH && rleLength >= bestMatch.length) {
                flushLiterals(literals, out);
                int remaining = rleLength;
                while (remaining > 0) {
                    int chunk = Math.min(remaining, MAX_RLE_RUN);
                    out.write(128 + (chunk - MIN_MATCH));
                    out.write(input[position] & 0xFF);
                    for (int i = 0; i < chunk; i++) {
                        link(input, position + i, head, prev);
                    }
                    position += chunk;
                    remaining -= chunk;
                }
                continue;
            }

            if (bestMatch.length >= MIN_MATCH) {
                flushLiterals(literals, out);
                int remaining = bestMatch.length;
                int distance = bestMatch.distance;
                while (remaining > 0) {
                    int chunk = Math.min(remaining, MAX_MATCH_RUN);
                    out.write(192 + (chunk - MIN_MATCH));
                    AfmaVarInts.writeUnsigned(out, distance);
                    for (int i = 0; i < chunk; i++) {
                        link(input, position + i, head, prev);
                    }
                    position += chunk;
                    remaining -= chunk;
                }
                continue;
            }

            literals.write(input[position]);
            link(input, position, head, prev);
            position++;
            if (literals.size() == MAX_LITERAL_RUN) {
                flushLiterals(literals, out);
            }
        }

        flushLiterals(literals, out);
        return out.toByteArray();
    }

    public static byte[] decompress(byte[] compressed) throws IOException {
        if (compressed.length == 0) {
            return compressed;
        }

        ByteArrayInputStream in = new ByteArrayInputStream(compressed);
        OutputBuffer out = new OutputBuffer(Math.max(256, compressed.length * 4));
        while (in.available() > 0) {
            int tag = in.read();
            if (tag < 0) {
                break;
            }
            if (tag < 128) {
                int length = tag + 1;
                byte[] literals = in.readNBytes(length);
                if (literals.length != length) {
                    throw new IOException("Literal run ended early");
                }
                out.write(literals, 0, literals.length);
                continue;
            }
            if (tag < 192) {
                int length = (tag - 128) + MIN_MATCH;
                int value = in.read();
                if (value < 0) {
                    throw new IOException("RLE run is missing its value byte");
                }
                out.repeat((byte) value, length);
                continue;
            }

            int length = (tag - 192) + MIN_MATCH;
            int distance = AfmaVarInts.readUnsigned(in);
            if (distance <= 0 || distance > out.size()) {
                throw new IOException("Match distance is invalid: " + distance);
            }

            int copyStart = out.size() - distance;
            for (int i = 0; i < length; i++) {
                out.write(out.get(copyStart + i));
            }
        }
        return out.toByteArray();
    }

    private static void flushLiterals(ByteArrayOutputStream literals, ByteArrayOutputStream out) {
        if (literals.size() == 0) {
            return;
        }
        byte[] literalBytes = literals.toByteArray();
        int offset = 0;
        while (offset < literalBytes.length) {
            int chunk = Math.min(MAX_LITERAL_RUN, literalBytes.length - offset);
            out.write(chunk - 1);
            out.write(literalBytes, offset, chunk);
            offset += chunk;
        }
        literals.reset();
    }

    private static int measureRle(byte[] input, int position) {
        int end = Math.min(input.length, position + MAX_RLE_RUN);
        int index = position + 1;
        while (index < end && input[index] == input[position]) {
            index++;
        }
        return index - position;
    }

    private static @NotNull Match findBestMatch(byte[] input, int position, int[] head, int[] prev) {
        if ((position + MIN_MATCH) > input.length) {
            return Match.NONE;
        }

        int hash = hash(input, position);
        int candidate = head[hash];
        int bestLength = 0;
        int bestDistance = 0;
        int searched = 0;
        while (candidate >= 0 && searched < MAX_CHAIN) {
            int distance = position - candidate;
            if (distance > MAX_DISTANCE) {
                break;
            }
            int length = 0;
            int max = Math.min(input.length - position, MAX_MATCH_RUN);
            while (length < max && input[candidate + length] == input[position + length]) {
                length++;
            }
            if (length > bestLength) {
                bestLength = length;
                bestDistance = distance;
                if (bestLength == MAX_MATCH_RUN) {
                    break;
                }
            }
            candidate = prev[candidate];
            searched++;
        }

        return bestLength >= MIN_MATCH ? new Match(bestLength, bestDistance) : Match.NONE;
    }

    private static void link(byte[] input, int position, int[] head, int[] prev) {
        if ((position + MIN_MATCH) > input.length) {
            return;
        }
        int hash = hash(input, position);
        prev[position] = head[hash];
        head[hash] = position;
    }

    private static int hash(byte[] input, int position) {
        int a = input[position] & 0xFF;
        int b = input[position + 1] & 0xFF;
        int c = input[position + 2] & 0xFF;
        return ((a * 251) ^ (b * 911) ^ (c * 577)) & (HASH_SIZE - 1);
    }

    private record Match(int length, int distance) {
        private static final Match NONE = new Match(0, 0);
    }

    private static final class OutputBuffer {

        private byte[] bytes;
        private int size;

        private OutputBuffer(int initialCapacity) {
            this.bytes = new byte[Math.max(16, initialCapacity)];
            this.size = 0;
        }

        private int size() {
            return this.size;
        }

        private void write(byte value) {
            this.ensureCapacity(this.size + 1);
            this.bytes[this.size++] = value;
        }

        private void write(byte[] values, int offset, int length) {
            if (length <= 0) {
                return;
            }
            this.ensureCapacity(this.size + length);
            System.arraycopy(values, offset, this.bytes, this.size, length);
            this.size += length;
        }

        private void repeat(byte value, int length) {
            this.ensureCapacity(this.size + length);
            Arrays.fill(this.bytes, this.size, this.size + length, value);
            this.size += length;
        }

        private byte get(int index) {
            return this.bytes[index];
        }

        private void ensureCapacity(int requiredCapacity) {
            if (requiredCapacity <= this.bytes.length) {
                return;
            }
            int newCapacity = this.bytes.length;
            while (newCapacity < requiredCapacity) {
                newCapacity *= 2;
            }
            this.bytes = Arrays.copyOf(this.bytes, newCapacity);
        }

        private byte[] toByteArray() {
            return Arrays.copyOf(this.bytes, this.size);
        }
    }
}
