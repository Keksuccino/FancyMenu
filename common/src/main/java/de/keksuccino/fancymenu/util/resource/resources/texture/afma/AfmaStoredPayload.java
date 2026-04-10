package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.file.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.Deflater;

public final class AfmaStoredPayload implements AutoCloseable {

    protected static final int ANALYSIS_BUFFER_BYTES = 8192;
    private static final File TEMP_DIR = FileUtils.createDirectory(new File(FancyMenu.TEMP_DATA_DIR, "/encoded_afma_payloads"));

    @NotNull
    private final File tempFile;
    private final int length;
    private final long estimatedArchiveBytes;
    @NotNull
    private final String fingerprint;
    @NotNull
    private final byte[] tailBytes;
    private boolean closed = false;

    private AfmaStoredPayload(@NotNull File tempFile, int length, long estimatedArchiveBytes,
                              @NotNull String fingerprint, @NotNull byte[] tailBytes) {
        this.tempFile = Objects.requireNonNull(tempFile);
        this.length = Math.max(0, length);
        this.estimatedArchiveBytes = Math.max(0L, estimatedArchiveBytes);
        this.fingerprint = Objects.requireNonNull(fingerprint);
        this.tailBytes = Objects.requireNonNull(tailBytes);
    }

    @NotNull
    public static AfmaStoredPayload fromBytes(@NotNull byte[] payloadBytes) throws IOException {
        Objects.requireNonNull(payloadBytes);
        File tempFile = createTempFile();
        boolean success = false;
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tempFile))) {
            out.write(payloadBytes);
            out.flush();
            success = true;
        } finally {
            if (!success) {
                org.apache.commons.io.FileUtils.deleteQuietly(tempFile);
            }
        }

        return new AfmaStoredPayload(
                tempFile,
                payloadBytes.length,
                AfmaPayloadMetricsHelper.estimateArchiveBytes(payloadBytes),
                AfmaPayloadMetricsHelper.fingerprintPayload(payloadBytes),
                tailBytes(payloadBytes)
        );
    }

    @NotNull
    public static PayloadSummary summarize(@NotNull byte[] payloadBytes) {
        Objects.requireNonNull(payloadBytes);
        return new PayloadSummary(
                payloadBytes.length,
                AfmaPayloadMetricsHelper.estimateArchiveBytes(payloadBytes),
                AfmaPayloadMetricsHelper.fingerprintPayload(payloadBytes),
                tailBytes(payloadBytes)
        );
    }

    @NotNull
    public static AfmaStoredPayload write(@NotNull Writer writer) throws IOException {
        Objects.requireNonNull(writer);
        File tempFile = createTempFile();
        boolean success = false;
        try (PayloadAnalysisOutputStream out = new PayloadAnalysisOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile)))) {
            writer.write(out);
            PayloadAnalysis analysis = out.finishAnalysis();
            success = true;
            return new AfmaStoredPayload(
                    tempFile,
                    analysis.length(),
                    analysis.estimatedArchiveBytes(),
                    analysis.fingerprint(),
                    analysis.tailBytes()
            );
        } finally {
            if (!success) {
                org.apache.commons.io.FileUtils.deleteQuietly(tempFile);
            }
        }
    }

    @NotNull
    public static PayloadSummary summarize(@NotNull Writer writer) throws IOException {
        Objects.requireNonNull(writer);
        try (PayloadAnalysisOutputStream out = new PayloadAnalysisOutputStream(OutputStream.nullOutputStream())) {
            writer.write(out);
            PayloadAnalysis analysis = out.finishAnalysis();
            return new PayloadSummary(
                    analysis.length(),
                    analysis.estimatedArchiveBytes(),
                    analysis.fingerprint(),
                    analysis.tailBytes()
            );
        }
    }

    public int length() {
        this.ensureOpen();
        return this.length;
    }

    public boolean isEmpty() {
        return this.length() <= 0;
    }

    public long estimatedArchiveBytes() {
        this.ensureOpen();
        return this.estimatedArchiveBytes;
    }

    @NotNull
    public String fingerprint() {
        this.ensureOpen();
        return this.fingerprint;
    }

    @NotNull
    public PayloadSummary summarize() {
        this.ensureOpen();
        return new PayloadSummary(this.length, this.estimatedArchiveBytes, this.fingerprint, this.tailBytes);
    }

    @NotNull
    public InputStream openStream() throws IOException {
        this.ensureOpen();
        return new BufferedInputStream(new FileInputStream(this.tempFile));
    }

    @NotNull
    public byte[] readAllBytes() throws IOException {
        this.ensureOpen();
        return Files.readAllBytes(this.tempFile.toPath());
    }

    public void writeTo(@NotNull OutputStream out) throws IOException {
        Objects.requireNonNull(out);
        this.ensureOpen();
        try (InputStream in = this.openStream()) {
            IOUtils.copyLarge(in, out);
        }
    }

    @NotNull
    protected byte[] tailBytes() {
        this.ensureOpen();
        return this.tailBytes.clone();
    }

    @NotNull
    protected byte[] tailBytesUnsafe() {
        this.ensureOpen();
        return this.tailBytes;
    }

    @NotNull
    protected File tempFile() {
        this.ensureOpen();
        return this.tempFile;
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        org.apache.commons.io.FileUtils.deleteQuietly(this.tempFile);
    }

    protected void ensureOpen() {
        if (this.closed) {
            throw new IllegalStateException("AFMA stored payload has already been closed");
        }
    }

    @NotNull
    protected static File createTempFile() throws IOException {
        return File.createTempFile("afma_payload_", ".bin", TEMP_DIR);
    }

    @NotNull
    protected static byte[] tailBytes(@NotNull byte[] payloadBytes) {
        Objects.requireNonNull(payloadBytes);
        int tailLength = Math.min(payloadBytes.length, AfmaChunkedPayloadHelper.PLANNER_DEFLATE_TAIL_BYTES);
        return Arrays.copyOfRange(payloadBytes, payloadBytes.length - tailLength, payloadBytes.length);
    }

    @NotNull
    protected static String hexDigest(@NotNull byte[] digestBytes) {
        StringBuilder builder = new StringBuilder(digestBytes.length * 2);
        for (byte digestByte : digestBytes) {
            builder.append(Character.forDigit((digestByte >>> 4) & 0xF, 16));
            builder.append(Character.forDigit(digestByte & 0xF, 16));
        }
        return builder.toString();
    }

    @FunctionalInterface
    public interface Writer {
        void write(@NotNull OutputStream out) throws IOException;
    }

    public record PayloadSummary(int length, long estimatedArchiveBytes, @NotNull String fingerprint, @NotNull byte[] tailBytes) {

        public PayloadSummary {
            fingerprint = Objects.requireNonNull(fingerprint);
            tailBytes = Objects.requireNonNull(tailBytes).clone();
        }

        @Override
        @NotNull
        public byte[] tailBytes() {
            return this.tailBytes.clone();
        }
    }

    protected record PayloadAnalysis(int length, long estimatedArchiveBytes, @NotNull String fingerprint, @NotNull byte[] tailBytes) {
    }

    // Capture payload metrics while bytes are being written so the encoder does not need a second temp-file read.
    protected static final class PayloadAnalysisOutputStream extends OutputStream {

        @NotNull
        protected final OutputStream delegate;
        @NotNull
        protected final MessageDigest digest;
        @NotNull
        protected final Deflater deflater = new Deflater(9, true);
        @NotNull
        protected final byte[] analysisBuffer = new byte[ANALYSIS_BUFFER_BYTES];
        @NotNull
        protected final byte[] deflateBuffer = new byte[ANALYSIS_BUFFER_BYTES];
        @NotNull
        protected final TailAccumulator tailAccumulator = new TailAccumulator(AfmaChunkedPayloadHelper.PLANNER_DEFLATE_TAIL_BYTES);
        protected int analysisBufferLength = 0;
        protected int length = 0;
        protected long compressedBytes = 0L;
        protected boolean closed = false;
        @Nullable
        protected PayloadAnalysis finishedAnalysis = null;

        protected PayloadAnalysisOutputStream(@NotNull OutputStream delegate) throws IOException {
            this.delegate = Objects.requireNonNull(delegate);
            try {
                this.digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException ex) {
                throw new IOException("Failed to create AFMA payload fingerprint digest", ex);
            }
        }

        @Override
        public void write(int value) throws IOException {
            this.ensureWritable(1);
            this.delegate.write(value);
            this.length++;
            this.analysisBuffer[this.analysisBufferLength++] = (byte) value;
            if (this.analysisBufferLength >= this.analysisBuffer.length) {
                this.flushAnalysisBuffer();
            }
        }

        @Override
        public void write(@NotNull byte[] source, int offset, int count) throws IOException {
            Objects.requireNonNull(source);
            if (offset < 0 || count < 0 || count > source.length - offset) {
                throw new IndexOutOfBoundsException();
            }
            if (count <= 0) {
                return;
            }

            this.ensureWritable(count);
            this.delegate.write(source, offset, count);
            this.length += count;
            this.bufferAnalysis(source, offset, count);
        }

        @Override
        public void flush() throws IOException {
            this.delegate.flush();
        }

        @Override
        public void close() throws IOException {
            if (this.closed) {
                return;
            }
            this.closed = true;
            try {
                this.delegate.close();
            } finally {
                this.deflater.end();
            }
        }

        @NotNull
        public PayloadAnalysis finishAnalysis() throws IOException {
            if (this.finishedAnalysis != null) {
                return this.finishedAnalysis;
            }
            if (this.closed) {
                throw new IOException("AFMA payload output has already been closed");
            }

            this.flushAnalysisBuffer();
            this.delegate.flush();
            this.deflater.finish();
            while (!this.deflater.finished()) {
                this.compressedBytes += this.deflater.deflate(this.deflateBuffer);
            }

            this.finishedAnalysis = new PayloadAnalysis(
                    this.length,
                    Math.max(1L, this.compressedBytes),
                    hexDigest(this.digest.digest()),
                    this.tailAccumulator.toByteArray()
            );
            return this.finishedAnalysis;
        }

        protected void ensureWritable(int additionalBytes) throws IOException {
            if (this.closed) {
                throw new IOException("AFMA payload output has already been closed");
            }
            if (this.finishedAnalysis != null) {
                throw new IOException("AFMA payload output analysis has already been finalized");
            }
            if (additionalBytes > Integer.MAX_VALUE - this.length) {
                throw new IOException("AFMA payload exceeds the supported size limit: " + (((long) this.length) + additionalBytes));
            }
        }

        protected void bufferAnalysis(@NotNull byte[] source, int offset, int count) {
            int remaining = count;
            int nextOffset = offset;
            if (this.analysisBufferLength > 0) {
                int bytesToCopy = Math.min(remaining, this.analysisBuffer.length - this.analysisBufferLength);
                System.arraycopy(source, nextOffset, this.analysisBuffer, this.analysisBufferLength, bytesToCopy);
                this.analysisBufferLength += bytesToCopy;
                nextOffset += bytesToCopy;
                remaining -= bytesToCopy;
                if (this.analysisBufferLength >= this.analysisBuffer.length) {
                    this.flushAnalysisBuffer();
                }
            }

            if (remaining >= this.analysisBuffer.length) {
                this.analyzeChunk(source, nextOffset, remaining);
                return;
            }

            if (remaining > 0) {
                System.arraycopy(source, nextOffset, this.analysisBuffer, this.analysisBufferLength, remaining);
                this.analysisBufferLength += remaining;
            }
        }

        protected void flushAnalysisBuffer() {
            if (this.analysisBufferLength <= 0) {
                return;
            }
            this.analyzeChunk(this.analysisBuffer, 0, this.analysisBufferLength);
            this.analysisBufferLength = 0;
        }

        protected void analyzeChunk(@NotNull byte[] source, int offset, int count) {
            this.digest.update(source, offset, count);
            this.tailAccumulator.append(source, offset, count);
            this.deflater.setInput(source, offset, count);
            while (!this.deflater.needsInput()) {
                this.compressedBytes += this.deflater.deflate(this.deflateBuffer);
            }
        }
    }

    protected static final class TailAccumulator {

        @NotNull
        protected final byte[] buffer;
        protected int length = 0;

        protected TailAccumulator(int maxBytes) {
            this.buffer = new byte[Math.max(0, maxBytes)];
        }

        public void append(@NotNull byte[] source, int offset, int count) {
            if (this.buffer.length <= 0 || count <= 0) {
                return;
            }

            if (count >= this.buffer.length) {
                System.arraycopy(source, offset + count - this.buffer.length, this.buffer, 0, this.buffer.length);
                this.length = this.buffer.length;
                return;
            }

            int nextLength = this.length + count;
            if (nextLength > this.buffer.length) {
                int bytesToDiscard = nextLength - this.buffer.length;
                System.arraycopy(this.buffer, bytesToDiscard, this.buffer, 0, this.length - bytesToDiscard);
                this.length -= bytesToDiscard;
            }
            System.arraycopy(source, offset, this.buffer, this.length, count);
            this.length += count;
        }

        @NotNull
        public byte[] toByteArray() {
            if (this.length <= 0) {
                return new byte[0];
            }
            return Arrays.copyOf(this.buffer, this.length);
        }

    }

}
