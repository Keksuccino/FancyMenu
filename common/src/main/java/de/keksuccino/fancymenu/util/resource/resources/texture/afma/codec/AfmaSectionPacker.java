package de.keksuccino.fancymenu.util.resource.resources.texture.afma.codec;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Packs AFMA codec sub-sections with a small method header and the cheapest of
 * several lightweight reversible byte coders.
 */
public final class AfmaSectionPacker {

    private AfmaSectionPacker() {
    }

    public static byte[] pack(byte[] originalBytes) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writePacked(out, findBestCandidate(originalBytes, originalBytes.length), originalBytes.length);
        return out.toByteArray();
    }

    public static void packTo(@NotNull OutputStream out, byte[] originalBytes, int originalLength) throws IOException {
        writePacked(out, findBestCandidate(originalBytes, originalLength), originalLength);
    }

    public static @NotNull Analysis analyze(byte[] originalBytes) throws IOException {
        return analyze(originalBytes, originalBytes.length);
    }

    public static @NotNull Analysis analyze(byte[] originalBytes, int originalLength) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Candidate best = findBestCandidate(originalBytes, originalLength);
        writePacked(out, best, originalLength);
        return new Analysis(best.method.name(), originalLength, best.length, out.toByteArray());
    }

    public static int measurePackedLength(byte[] originalBytes) throws IOException {
        return measurePackedLength(originalBytes, originalBytes.length);
    }

    public static int measurePackedLength(byte[] originalBytes, int originalLength) throws IOException {
        Candidate best = findBestCandidate(originalBytes, originalLength);
        return 1 + measureUnsignedVarIntSize(originalLength) + measureUnsignedVarIntSize(best.length) + best.length;
    }

    public static byte[] unpack(@NotNull ByteArrayInputStream in) throws IOException {
        int methodId = in.read();
        if (methodId < 0) {
            throw new IOException("Section header ended early");
        }
        Method method = Method.byId(methodId);
        int expectedLength = AfmaVarInts.readUnsigned(in);
        int storedLength = AfmaVarInts.readUnsigned(in);
        byte[] storedBytes = in.readNBytes(storedLength);
        if (storedBytes.length != storedLength) {
            throw new IOException("Section payload ended early");
        }

        byte[] unpacked = switch (method) {
            case RAW -> storedBytes;
            case ZERO_RLE -> AfmaZeroRunCodec.decompress(storedBytes);
            case MINILZ -> AfmaMiniLzCodec.decompress(storedBytes);
            case RICE -> AfmaRiceCodec.decompress(storedBytes, expectedLength);
            case HUFFMAN -> AfmaHuffmanCodec.decompress(storedBytes);
            case ZERO_RLE_HUFFMAN -> AfmaZeroRunCodec.decompress(AfmaHuffmanCodec.decompress(storedBytes));
            case MINILZ_HUFFMAN -> AfmaMiniLzCodec.decompress(AfmaHuffmanCodec.decompress(storedBytes));
        };
        if (unpacked.length != expectedLength) {
            throw new IOException("Section unpacked length mismatch");
        }
        return unpacked;
    }

    private static @NotNull Candidate findBestCandidate(byte[] originalBytes, int originalLength) throws IOException {
        byte[] sourceBytes = (originalLength == originalBytes.length)
                ? originalBytes
                : Arrays.copyOf(originalBytes, originalLength);
        Candidate best = new Candidate(Method.RAW, sourceBytes, originalLength);

        byte[] zeroRun = AfmaZeroRunCodec.compress(sourceBytes);
        best = chooseBetter(best, Method.ZERO_RLE, zeroRun);

        byte[] zeroRunHuffman = AfmaHuffmanCodec.compress(zeroRun);
        best = chooseBetter(best, Method.ZERO_RLE_HUFFMAN, zeroRunHuffman);
        zeroRun = null;
        zeroRunHuffman = null;

        byte[] miniLz = AfmaMiniLzCodec.compress(sourceBytes);
        best = chooseBetter(best, Method.MINILZ, miniLz);

        byte[] miniLzHuffman = AfmaHuffmanCodec.compress(miniLz);
        best = chooseBetter(best, Method.MINILZ_HUFFMAN, miniLzHuffman);
        miniLz = null;
        miniLzHuffman = null;

        byte[] rice = AfmaRiceCodec.compress(sourceBytes);
        best = chooseBetter(best, Method.RICE, rice);
        rice = null;

        byte[] huffman = AfmaHuffmanCodec.compress(sourceBytes);
        best = chooseBetter(best, Method.HUFFMAN, huffman);

        return best;
    }

    private static @NotNull Candidate chooseBetter(@NotNull Candidate best, @NotNull Method method, byte[] bytes) {
        return (bytes.length < best.length) ? new Candidate(method, bytes, bytes.length) : best;
    }

    private static void writePacked(@NotNull OutputStream out, @NotNull Candidate best, int originalLength) throws IOException {
        out.write(best.method.id);
        AfmaVarInts.writeUnsigned(out, originalLength);
        AfmaVarInts.writeUnsigned(out, best.length);
        out.write(best.bytes, 0, best.length);
    }

    private static int measureUnsignedVarIntSize(int value) {
        int remaining = value;
        int bytes = 1;
        while ((remaining >>>= 7) != 0) {
            bytes++;
        }
        return bytes;
    }

    private record Candidate(@NotNull Method method, byte @NotNull [] bytes, int length) {
    }

    public record Analysis(@NotNull String methodName, int originalLength, int payloadLength, byte @NotNull [] packedBytes) {
    }

    private enum Method {
        RAW(0),
        ZERO_RLE(1),
        MINILZ(2),
        HUFFMAN(3),
        ZERO_RLE_HUFFMAN(4),
        MINILZ_HUFFMAN(5),
        RICE(6);

        private final int id;

        Method(int id) {
            this.id = id;
        }

        private static @NotNull Method byId(int id) throws IOException {
            for (Method method : values()) {
                if (method.id == id) {
                    return method;
                }
            }
            throw new IOException("Unsupported section method: " + id);
        }
    }
}
