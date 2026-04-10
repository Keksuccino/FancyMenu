package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.file.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

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
    public static AfmaStoredPayload write(@NotNull Writer writer) throws IOException {
        Objects.requireNonNull(writer);
        File tempFile = createTempFile();
        boolean success = false;
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tempFile))) {
            writer.write(out);
            out.flush();
            success = true;
        } finally {
            if (!success) {
                org.apache.commons.io.FileUtils.deleteQuietly(tempFile);
            }
        }

        long fileLength = tempFile.length();
        if (fileLength > Integer.MAX_VALUE) {
            org.apache.commons.io.FileUtils.deleteQuietly(tempFile);
            throw new IOException("AFMA payload exceeds the supported size limit: " + fileLength);
        }

        try {
            PayloadAnalysis analysis = analyze(tempFile);
            return new AfmaStoredPayload(tempFile, (int) fileLength, analysis.estimatedArchiveBytes(), analysis.fingerprint(), analysis.tailBytes());
        } catch (IOException ex) {
            org.apache.commons.io.FileUtils.deleteQuietly(tempFile);
            throw ex;
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
    protected static PayloadAnalysis analyze(@NotNull File tempFile) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException("Failed to create AFMA payload fingerprint digest", ex);
        }

        Deflater deflater = new Deflater(9, true);
        byte[] readBuffer = new byte[ANALYSIS_BUFFER_BYTES];
        byte[] deflateBuffer = new byte[ANALYSIS_BUFFER_BYTES];
        TailAccumulator tailAccumulator = new TailAccumulator(AfmaChunkedPayloadHelper.PLANNER_DEFLATE_TAIL_BYTES);
        long compressedBytes = 0L;
        try (InputStream in = new BufferedInputStream(new FileInputStream(tempFile))) {
            int read;
            while ((read = in.read(readBuffer)) >= 0) {
                if (read == 0) {
                    continue;
                }
                digest.update(readBuffer, 0, read);
                tailAccumulator.append(readBuffer, 0, read);
                deflater.setInput(readBuffer, 0, read);
                while (!deflater.needsInput()) {
                    compressedBytes += deflater.deflate(deflateBuffer);
                }
            }
            deflater.finish();
            while (!deflater.finished()) {
                compressedBytes += deflater.deflate(deflateBuffer);
            }
        } finally {
            deflater.end();
        }

        return new PayloadAnalysis(
                Math.max(1L, compressedBytes),
                hexDigest(digest.digest()),
                tailAccumulator.toByteArray()
        );
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

    protected record PayloadAnalysis(long estimatedArchiveBytes, @NotNull String fingerprint, @NotNull byte[] tailBytes) {
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
