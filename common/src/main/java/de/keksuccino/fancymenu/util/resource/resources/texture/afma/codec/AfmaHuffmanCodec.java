package de.keksuccino.fancymenu.util.resource.resources.texture.afma.codec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Canonical Huffman coder used by AFMA's packed substreams.
 */
public final class AfmaHuffmanCodec {

    private AfmaHuffmanCodec() {
    }

    public static byte[] compress(byte[] input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AfmaVarInts.writeUnsigned(out, input.length);
        if (input.length == 0) {
            return out.toByteArray();
        }

        int[] frequencies = new int[256];
        for (byte value : input) {
            frequencies[value & 0xFF]++;
        }

        int[] codeLengths = buildCodeLengths(frequencies);
        for (int length : codeLengths) {
            out.write(length);
        }

        CanonicalTable table = buildCanonicalTable(codeLengths);
        BitOutput bitOutput = new BitOutput(out);
        for (byte value : input) {
            int symbol = value & 0xFF;
            bitOutput.writeBits(table.codes[symbol], table.codeLengths[symbol]);
        }
        bitOutput.flush();
        return out.toByteArray();
    }

    public static byte[] decompress(byte[] input) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(input);
        int outputLength = AfmaVarInts.readUnsigned(in);
        if (outputLength == 0) {
            return new byte[0];
        }

        int[] codeLengths = new int[256];
        for (int i = 0; i < codeLengths.length; i++) {
            int length = in.read();
            if (length < 0) {
                throw new IOException("Huffman header ended early");
            }
            codeLengths[i] = length;
        }

        DecoderTable decoder = buildDecoderTable(codeLengths);
        BitInput bitInput = new BitInput(in);
        byte[] output = new byte[outputLength];
        for (int i = 0; i < outputLength; i++) {
            output[i] = (byte) decoder.readSymbol(bitInput);
        }
        return output;
    }

    private static int[] buildCodeLengths(int[] frequencies) {
        PriorityQueue<Node> nodes = new PriorityQueue<>(Comparator
                .comparingLong(Node::frequency)
                .thenComparingInt(Node::symbol));
        for (int symbol = 0; symbol < frequencies.length; symbol++) {
            if (frequencies[symbol] > 0) {
                nodes.add(new Node(symbol, frequencies[symbol], null, null));
            }
        }

        int[] codeLengths = new int[256];
        if (nodes.isEmpty()) {
            return codeLengths;
        }
        if (nodes.size() == 1) {
            Node only = nodes.remove();
            codeLengths[only.symbol] = 1;
            return codeLengths;
        }

        while (nodes.size() > 1) {
            Node first = nodes.remove();
            Node second = nodes.remove();
            nodes.add(new Node(Math.min(first.symbol, second.symbol), first.frequency + second.frequency, first, second));
        }

        assignCodeLengths(nodes.remove(), 0, codeLengths);
        return codeLengths;
    }

    private static void assignCodeLengths(Node node, int depth, int[] codeLengths) {
        if (node.left == null && node.right == null) {
            codeLengths[node.symbol] = Math.max(1, depth);
            return;
        }
        assignCodeLengths(node.left, depth + 1, codeLengths);
        assignCodeLengths(node.right, depth + 1, codeLengths);
    }

    private static CanonicalTable buildCanonicalTable(int[] codeLengths) {
        int maxLength = Arrays.stream(codeLengths).max().orElse(0);
        int[] blCount = new int[maxLength + 1];
        for (int length : codeLengths) {
            if (length > 0) {
                blCount[length]++;
            }
        }

        int[] nextCode = new int[maxLength + 1];
        int code = 0;
        for (int bits = 1; bits <= maxLength; bits++) {
            code = (code + blCount[bits - 1]) << 1;
            nextCode[bits] = code;
        }

        int[] codes = new int[256];
        for (int symbol = 0; symbol < codeLengths.length; symbol++) {
            int length = codeLengths[symbol];
            if (length > 0) {
                codes[symbol] = nextCode[length]++;
            }
        }
        return new CanonicalTable(codes, codeLengths);
    }

    private static DecoderTable buildDecoderTable(int[] codeLengths) {
        CanonicalTable canonicalTable = buildCanonicalTable(codeLengths);
        DecoderNode root = new DecoderNode();
        for (int symbol = 0; symbol < canonicalTable.codes.length; symbol++) {
            int length = canonicalTable.codeLengths[symbol];
            if (length <= 0) {
                continue;
            }
            int code = canonicalTable.codes[symbol];
            DecoderNode node = root;
            for (int bitIndex = length - 1; bitIndex >= 0; bitIndex--) {
                int bit = (code >>> bitIndex) & 1;
                if (bit == 0) {
                    if (node.zero == null) {
                        node.zero = new DecoderNode();
                    }
                    node = node.zero;
                } else {
                    if (node.one == null) {
                        node.one = new DecoderNode();
                    }
                    node = node.one;
                }
            }
            node.symbol = symbol;
        }
        return new DecoderTable(root);
    }

    private record CanonicalTable(int[] codes, int[] codeLengths) {
    }

    private record Node(int symbol, long frequency, Node left, Node right) {
    }

    private static final class DecoderTable {

        private final DecoderNode root;

        private DecoderTable(DecoderNode root) {
            this.root = root;
        }

        private int readSymbol(BitInput input) throws IOException {
            DecoderNode node = this.root;
            while (node.symbol < 0) {
                int bit = input.readBit();
                node = (bit == 0) ? node.zero : node.one;
                if (node == null) {
                    throw new IOException("Invalid Huffman bit stream");
                }
            }
            return node.symbol;
        }
    }

    private static final class DecoderNode {
        private int symbol = -1;
        private DecoderNode zero;
        private DecoderNode one;
    }

    private static final class BitOutput {

        private final ByteArrayOutputStream out;
        private int currentByte;
        private int bitCount;

        private BitOutput(ByteArrayOutputStream out) {
            this.out = out;
        }

        private void writeBits(int value, int length) {
            for (int bitIndex = length - 1; bitIndex >= 0; bitIndex--) {
                this.currentByte = (this.currentByte << 1) | ((value >>> bitIndex) & 1);
                this.bitCount++;
                if (this.bitCount == 8) {
                    this.out.write(this.currentByte);
                    this.currentByte = 0;
                    this.bitCount = 0;
                }
            }
        }

        private void flush() {
            if (this.bitCount > 0) {
                this.out.write(this.currentByte << (8 - this.bitCount));
                this.currentByte = 0;
                this.bitCount = 0;
            }
        }
    }

    private static final class BitInput {

        private final ByteArrayInputStream in;
        private int currentByte;
        private int bitsRemaining;

        private BitInput(ByteArrayInputStream in) {
            this.in = in;
        }

        private int readBit() throws IOException {
            if (this.bitsRemaining == 0) {
                this.currentByte = this.in.read();
                if (this.currentByte < 0) {
                    throw new IOException("Unexpected end of Huffman bit stream");
                }
                this.bitsRemaining = 8;
            }
            this.bitsRemaining--;
            return (this.currentByte >>> this.bitsRemaining) & 1;
        }
    }
}
