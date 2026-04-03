package de.keksuccino.fancymenu.util.resource.resources.texture.afma.codec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

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

        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(16, input.length));
        try {
            compressTo(out, input, input.length);
        } catch (IOException e) {
            throw new IllegalStateException("Unexpected rice compression failure", e);
        }
        return out.toByteArray();
    }

    public static void compressTo(OutputStream out, byte[] input, int inputLength) throws IOException {
        if (inputLength == 0) {
            return;
        }

        int bestParameter = 0;
        int bestLength = Integer.MAX_VALUE;
        for (int parameter = 0; parameter <= 7; parameter++) {
            int encodedLength = measureEncodedLength(input, inputLength, parameter);
            if (encodedLength < bestLength) {
                bestLength = encodedLength;
                bestParameter = parameter;
            }
        }
        encode(out, input, inputLength, bestParameter);
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

    private static void encode(OutputStream out, byte[] input, int inputLength, int parameter) throws IOException {
        out.write(parameter);
        BitOutput bits = new BitOutput(out);
        for (int index = 0; index < inputLength; index++) {
            int unsigned = input[index] & 0xFF;
            int quotient = unsigned >>> parameter;
            int remainder = unsigned & ((1 << parameter) - 1);
            for (int i = 0; i < quotient; i++) {
                bits.writeBit(0);
            }
            bits.writeBit(1);
            bits.writeBits(remainder, parameter);
        }
        bits.flush();
    }

    private static int measureEncodedLength(byte[] input, int inputLength, int parameter) {
        long totalBits = 0L;
        for (int index = 0; index < inputLength; index++) {
            int unsigned = input[index] & 0xFF;
            totalBits += (unsigned >>> parameter) + 1L + parameter;
        }
        long payloadBytes = 1L + ((totalBits + 7L) >>> 3);
        return (payloadBytes > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) payloadBytes;
    }

    private static final class BitOutput {

        private final OutputStream out;
        private int currentByte;
        private int bitCount;

        private BitOutput(OutputStream out) {
            this.out = out;
        }

        private void writeBit(int bit) throws IOException {
            this.currentByte = (this.currentByte << 1) | (bit & 1);
            this.bitCount++;
            if (this.bitCount == 8) {
                flushByte();
            }
        }

        private void writeBits(int value, int bitCount) throws IOException {
            for (int shift = bitCount - 1; shift >= 0; shift--) {
                writeBit((value >>> shift) & 1);
            }
        }

        private void flush() throws IOException {
            if (this.bitCount <= 0) {
                return;
            }
            this.currentByte <<= (8 - this.bitCount);
            flushByte();
        }

        private void flushByte() throws IOException {
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
