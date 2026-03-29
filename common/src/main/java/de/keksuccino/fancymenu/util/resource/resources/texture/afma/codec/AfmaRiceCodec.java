package de.keksuccino.fancymenu.util.resource.resources.texture.afma.codec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Small byte-oriented Rice codec used as one candidate in AFMA section
 * packing.
 */
public final class AfmaRiceCodec {

    private AfmaRiceCodec() {
    }

    public static byte[] compress(byte[] input) {
        if (input.length == 0) {
            return input;
        }

        byte[] best = null;
        for (int parameter = 0; parameter <= 7; parameter++) {
            byte[] encoded = encode(input, parameter);
            if (best == null || encoded.length < best.length) {
                best = encoded;
            }
        }
        return best;
    }

    public static byte[] decompress(byte[] input, int expectedLength) throws IOException {
        if (expectedLength == 0) {
            return new byte[0];
        }
        if (input.length == 0) {
            throw new IOException("Rice stream is empty");
        }

        int parameter = input[0] & 0xFF;
        if (parameter > 7) {
            throw new IOException("Unsupported Rice parameter: " + parameter);
        }

        ByteArrayInputStream byteIn = new ByteArrayInputStream(input, 1, input.length - 1);
        BitInput bits = new BitInput(byteIn);
        byte[] output = new byte[expectedLength];
        for (int index = 0; index < expectedLength; index++) {
            int quotient = 0;
            while (true) {
                int bit = bits.readBit();
                if (bit < 0) {
                    throw new IOException("Rice unary code ended early");
                }
                if (bit == 1) {
                    break;
                }
                quotient++;
            }
            int remainder = (parameter == 0) ? 0 : bits.readBits(parameter);
            int value = (quotient << parameter) | remainder;
            if (value < 0 || value > 255) {
                throw new IOException("Rice value exceeds byte range: " + value);
            }
            output[index] = (byte) value;
        }
        return output;
    }

    private static byte[] encode(byte[] input, int parameter) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(16, input.length));
        out.write(parameter);
        BitOutput bits = new BitOutput(out);
        for (byte value : input) {
            int unsigned = value & 0xFF;
            int quotient = unsigned >>> parameter;
            int remainder = unsigned & ((1 << parameter) - 1);
            for (int i = 0; i < quotient; i++) {
                bits.writeBit(0);
            }
            bits.writeBit(1);
            bits.writeBits(remainder, parameter);
        }
        bits.flush();
        return out.toByteArray();
    }

    private static final class BitOutput {

        private final ByteArrayOutputStream out;
        private int currentByte;
        private int bitCount;

        private BitOutput(ByteArrayOutputStream out) {
            this.out = out;
        }

        private void writeBit(int bit) {
            this.currentByte = (this.currentByte << 1) | (bit & 1);
            this.bitCount++;
            if (this.bitCount == 8) {
                flushByte();
            }
        }

        private void writeBits(int value, int bitCount) {
            for (int shift = bitCount - 1; shift >= 0; shift--) {
                writeBit((value >>> shift) & 1);
            }
        }

        private void flush() {
            if (this.bitCount <= 0) {
                return;
            }
            this.currentByte <<= (8 - this.bitCount);
            flushByte();
        }

        private void flushByte() {
            this.out.write(this.currentByte);
            this.currentByte = 0;
            this.bitCount = 0;
        }
    }

    private static final class BitInput {

        private final ByteArrayInputStream in;
        private int currentByte;
        private int remainingBits;

        private BitInput(ByteArrayInputStream in) {
            this.in = in;
        }

        private int readBit() {
            if (this.remainingBits == 0) {
                this.currentByte = this.in.read();
                if (this.currentByte < 0) {
                    return -1;
                }
                this.remainingBits = 8;
            }
            this.remainingBits--;
            return (this.currentByte >>> this.remainingBits) & 1;
        }

        private int readBits(int bitCount) throws IOException {
            int value = 0;
            for (int i = 0; i < bitCount; i++) {
                int bit = readBit();
                if (bit < 0) {
                    throw new IOException("Rice remainder ended early");
                }
                value = (value << 1) | bit;
            }
            return value;
        }
    }
}
