package de.keksuccino.fancymenu.util.resource.resources.texture.afma.codec;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
        packTo(out, originalBytes, originalBytes.length);
        return out.toByteArray();
    }

    public static void packTo(@NotNull OutputStream out, byte[] originalBytes, int originalLength) throws IOException {
        Candidate best = findBestStreamingCandidate(originalBytes, originalLength);
        try {
            writePacked(out, best, originalLength);
        } finally {
            best.close();
        }
    }

    public static @NotNull Analysis analyze(byte[] originalBytes) throws IOException {
        return analyze(originalBytes, originalBytes.length);
    }

    public static @NotNull Analysis analyze(byte[] originalBytes, int originalLength) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Candidate best = findBestStreamingCandidate(originalBytes, originalLength);
        try {
            writePacked(out, best, originalLength);
            return new Analysis(best.method.name(), originalLength, best.length, out.toByteArray());
        } finally {
            best.close();
        }
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

    private static @NotNull Candidate findBestStreamingCandidate(byte[] originalBytes, int originalLength) throws IOException {
        byte[] sourceBytes = (originalLength == originalBytes.length)
                ? originalBytes
                : Arrays.copyOf(originalBytes, originalLength);
        Candidate best = Candidate.raw(sourceBytes, originalLength);
        Candidate candidate = null;
        try {
            candidate = encodeCandidate(Method.ZERO_RLE, sourceBytes, originalLength, best.length);
            best = chooseBetter(best, candidate);
            candidate = null;

            candidate = encodeCandidate(Method.ZERO_RLE_HUFFMAN, sourceBytes, originalLength, best.length);
            best = chooseBetter(best, candidate);
            candidate = null;

            candidate = encodeCandidate(Method.MINILZ, sourceBytes, originalLength, best.length);
            best = chooseBetter(best, candidate);
            candidate = null;

            candidate = encodeCandidate(Method.MINILZ_HUFFMAN, sourceBytes, originalLength, best.length);
            best = chooseBetter(best, candidate);
            candidate = null;

            candidate = encodeCandidate(Method.RICE, sourceBytes, originalLength, best.length);
            best = chooseBetter(best, candidate);
            candidate = null;

            candidate = encodeCandidate(Method.HUFFMAN, sourceBytes, originalLength, best.length);
            best = chooseBetter(best, candidate);
            candidate = null;

            return best;
        } finally {
            if (candidate != null) {
                candidate.close();
            }
        }
    }

    private static @NotNull Candidate findBestCandidate(byte[] originalBytes, int originalLength) throws IOException {
        byte[] sourceBytes = (originalLength == originalBytes.length)
                ? originalBytes
                : Arrays.copyOf(originalBytes, originalLength);
        Candidate best = Candidate.raw(sourceBytes, originalLength);

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

    private static Candidate encodeCandidate(@NotNull Method method, byte[] sourceBytes, int originalLength, int currentBestLength) throws IOException {
        if (currentBestLength <= 0) {
            return null;
        }
        return switch (method) {
            case RAW -> Candidate.raw(sourceBytes, originalLength);
            case ZERO_RLE -> encodeDirectCandidate(method, currentBestLength, out -> AfmaZeroRunCodec.compressTo(out, sourceBytes, originalLength));
            case MINILZ -> encodeDirectCandidate(method, currentBestLength, out -> AfmaMiniLzCodec.compressTo(out, sourceBytes, originalLength));
            case HUFFMAN -> encodeDirectCandidate(method, currentBestLength, out -> AfmaHuffmanCodec.compressTo(out, sourceBytes, originalLength));
            case RICE -> encodeDirectCandidate(method, currentBestLength, out -> AfmaRiceCodec.compressTo(out, sourceBytes, originalLength));
            case ZERO_RLE_HUFFMAN -> encodeTwoStageCandidate(method, currentBestLength,
                    intermediate -> AfmaZeroRunCodec.compressTo(intermediate, sourceBytes, originalLength),
                    (intermediatePath, intermediateLength, out) -> AfmaHuffmanCodec.compress(intermediatePath, intermediateLength, out));
            case MINILZ_HUFFMAN -> encodeTwoStageCandidate(method, currentBestLength,
                    intermediate -> AfmaMiniLzCodec.compressTo(intermediate, sourceBytes, originalLength),
                    (intermediatePath, intermediateLength, out) -> AfmaHuffmanCodec.compress(intermediatePath, intermediateLength, out));
        };
    }

    private static @NotNull Candidate encodeDirectCandidate(@NotNull Method method, int currentBestLength, @NotNull CandidateEncoder encoder) throws IOException {
        Path candidateFile = Files.createTempFile("fancymenu_afma_pack_", ".tmp");
        boolean success = false;
        try (OutputStream fileOut = new BufferedOutputStream(Files.newOutputStream(candidateFile));
             OutputStream limitedOut = new CandidateLimitOutputStream(fileOut, currentBestLength - 1L)) {
            try {
                encoder.encode(limitedOut);
            } catch (CandidateExceededException ignored) {
                return null;
            }
            limitedOut.flush();
            success = true;
        } finally {
            if (!success) {
                Files.deleteIfExists(candidateFile);
            }
        }
        return Candidate.file(method, candidateFile, (int) Files.size(candidateFile));
    }

    private static Candidate encodeTwoStageCandidate(@NotNull Method method, int currentBestLength, @NotNull CandidateEncoder firstStage,
                                                     @NotNull TempFileCandidateEncoder secondStage) throws IOException {
        Path intermediateFile = Files.createTempFile("fancymenu_afma_pack_stage_", ".tmp");
        Path candidateFile = null;
        boolean success = false;
        try {
            try (OutputStream intermediateOut = new BufferedOutputStream(Files.newOutputStream(intermediateFile))) {
                firstStage.encode(intermediateOut);
            }
            int intermediateLength = (int) Files.size(intermediateFile);
            candidateFile = Files.createTempFile("fancymenu_afma_pack_", ".tmp");
            try (OutputStream fileOut = new BufferedOutputStream(Files.newOutputStream(candidateFile));
                 OutputStream limitedOut = new CandidateLimitOutputStream(fileOut, currentBestLength - 1L)) {
                try {
                    secondStage.encode(intermediateFile, intermediateLength, limitedOut);
                } catch (CandidateExceededException ignored) {
                    return null;
                }
                limitedOut.flush();
                success = true;
            }
            return Candidate.file(method, candidateFile, (int) Files.size(candidateFile));
        } finally {
            Files.deleteIfExists(intermediateFile);
            if (!success && candidateFile != null) {
                Files.deleteIfExists(candidateFile);
            }
        }
    }

    private static @NotNull Candidate chooseBetter(@NotNull Candidate best, @NotNull Method method, byte[] bytes) throws IOException {
        if (bytes.length < best.length) {
            best.close();
            return Candidate.bytes(method, bytes, bytes.length);
        }
        return best;
    }

    private static @NotNull Candidate chooseBetter(@NotNull Candidate best, Candidate candidate) throws IOException {
        if (candidate == null) {
            return best;
        }
        if (candidate.length < best.length) {
            best.close();
            return candidate;
        }
        candidate.close();
        return best;
    }

    private static void writePacked(@NotNull OutputStream out, @NotNull Candidate best, int originalLength) throws IOException {
        out.write(best.method.id);
        AfmaVarInts.writeUnsigned(out, originalLength);
        AfmaVarInts.writeUnsigned(out, best.length);
        best.writePayloadTo(out);
    }

    private static int measureUnsignedVarIntSize(int value) {
        int remaining = value;
        int bytes = 1;
        while ((remaining >>>= 7) != 0) {
            bytes++;
        }
        return bytes;
    }

    private interface CandidateEncoder {
        void encode(@NotNull OutputStream out) throws IOException;
    }

    private interface TempFileCandidateEncoder {
        void encode(@NotNull Path inputFile, int inputLength, @NotNull OutputStream out) throws IOException;
    }

    private static final class CandidateExceededException extends IOException {
        private CandidateExceededException() {
            super("Packed section candidate exceeded current best size");
        }
    }

    private static final class CandidateLimitOutputStream extends OutputStream {

        private final OutputStream delegate;
        private final long maxBytes;
        private long writtenBytes;

        private CandidateLimitOutputStream(@NotNull OutputStream delegate, long maxBytes) {
            this.delegate = delegate;
            this.maxBytes = maxBytes;
        }

        @Override
        public void write(int b) throws IOException {
            ensureCapacity(1L);
            this.delegate.write(b);
            this.writtenBytes++;
        }

        @Override
        public void write(byte @NotNull [] b, int off, int len) throws IOException {
            if (len <= 0) {
                return;
            }
            ensureCapacity(len);
            this.delegate.write(b, off, len);
            this.writtenBytes += len;
        }

        @Override
        public void flush() throws IOException {
            this.delegate.flush();
        }

        @Override
        public void close() throws IOException {
            this.delegate.close();
        }

        private void ensureCapacity(long byteCount) throws CandidateExceededException {
            if ((this.maxBytes >= 0L) && ((this.writtenBytes + byteCount) > this.maxBytes)) {
                throw new CandidateExceededException();
            }
        }
    }

    private static final class Candidate implements AutoCloseable {

        private final Method method;
        private final byte[] bytes;
        private final Path file;
        private final int length;

        private Candidate(@NotNull Method method, byte[] bytes, Path file, int length) {
            this.method = method;
            this.bytes = bytes;
            this.file = file;
            this.length = length;
        }

        private static @NotNull Candidate raw(byte[] bytes, int length) {
            return new Candidate(Method.RAW, bytes, null, length);
        }

        private static @NotNull Candidate bytes(@NotNull Method method, byte[] bytes, int length) {
            return new Candidate(method, bytes, null, length);
        }

        private static @NotNull Candidate file(@NotNull Method method, @NotNull Path file, int length) {
            return new Candidate(method, null, file, length);
        }

        private void writePayloadTo(@NotNull OutputStream out) throws IOException {
            if (this.bytes != null) {
                out.write(this.bytes, 0, this.length);
                return;
            }
            try (InputStream in = new BufferedInputStream(Files.newInputStream(this.file))) {
                in.transferTo(out);
            }
        }

        @Override
        public void close() throws IOException {
            if (this.file != null) {
                Files.deleteIfExists(this.file);
            }
        }
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
