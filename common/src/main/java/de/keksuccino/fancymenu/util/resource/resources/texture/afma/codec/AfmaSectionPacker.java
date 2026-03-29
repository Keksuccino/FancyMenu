package de.keksuccino.fancymenu.util.resource.resources.texture.afma.codec;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Packs AFMA codec sub-sections with a small method header and the cheapest of
 * several lightweight reversible byte coders.
 */
public final class AfmaSectionPacker {

    private AfmaSectionPacker() {
    }

    public static byte[] pack(byte[] originalBytes) throws IOException {
        return analyze(originalBytes).packedBytes();
    }

    public static @NotNull Analysis analyze(byte[] originalBytes) throws IOException {
        Candidate best = new Candidate(Method.RAW, originalBytes);

        byte[] zeroRun = AfmaZeroRunCodec.compress(originalBytes);
        if (zeroRun.length < best.bytes.length) {
            best = new Candidate(Method.ZERO_RLE, zeroRun);
        }

        byte[] miniLz = AfmaMiniLzCodec.compress(originalBytes);
        if (miniLz.length < best.bytes.length) {
            best = new Candidate(Method.MINILZ, miniLz);
        }

        byte[] rice = AfmaRiceCodec.compress(originalBytes);
        if (rice.length < best.bytes.length) {
            best = new Candidate(Method.RICE, rice);
        }

        byte[] zeroRunHuffman = AfmaHuffmanCodec.compress(zeroRun);
        if (zeroRunHuffman.length < best.bytes.length) {
            best = new Candidate(Method.ZERO_RLE_HUFFMAN, zeroRunHuffman);
        }

        byte[] huffman = AfmaHuffmanCodec.compress(originalBytes);
        if (huffman.length < best.bytes.length) {
            best = new Candidate(Method.HUFFMAN, huffman);
        }

        byte[] miniLzHuffman = AfmaHuffmanCodec.compress(miniLz);
        if (miniLzHuffman.length < best.bytes.length) {
            best = new Candidate(Method.MINILZ_HUFFMAN, miniLzHuffman);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(best.method.id);
        AfmaVarInts.writeUnsigned(out, originalBytes.length);
        AfmaVarInts.writeUnsigned(out, best.bytes.length);
        out.write(best.bytes);
        return new Analysis(best.method.name(), originalBytes.length, best.bytes.length, out.toByteArray());
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

    private record Candidate(@NotNull Method method, byte @NotNull [] bytes) {
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
