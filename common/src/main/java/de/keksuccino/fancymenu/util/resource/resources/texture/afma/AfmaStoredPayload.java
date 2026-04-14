package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.file.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.zip.Deflater;

public final class AfmaStoredPayload implements AutoCloseable {

    protected static final int ANALYSIS_BUFFER_BYTES = 8192;
    private static final File TEMP_DIR = FileUtils.createDirectory(new File(FancyMenu.TEMP_DATA_DIR, "/encoded_afma_payloads"));
    protected static final int INLINE_MEMORY_PAYLOAD_BYTES = 128 * 1024;
    protected static final long MIN_HEAP_BUDGET_BYTES = 16L * 1024L * 1024L;
    protected static final long MAX_HEAP_BUDGET_BYTES = 128L * 1024L * 1024L;
    @NotNull
    private static final PayloadStore PAYLOAD_STORE = new PayloadStore(INLINE_MEMORY_PAYLOAD_BYTES, computeHeapBudgetBytes());

    @NotNull
    private final PayloadStorage storage;
    private final int length;
    private final long estimatedArchiveBytes;
    @NotNull
    private final String fingerprint;
    @NotNull
    private final byte[] tailBytes;
    private boolean closed = false;

    private AfmaStoredPayload(@NotNull PayloadStorage storage, int length, long estimatedArchiveBytes,
                              @NotNull String fingerprint, @NotNull byte[] tailBytes) {
        this.storage = Objects.requireNonNull(storage);
        this.length = Math.max(0, length);
        this.estimatedArchiveBytes = Math.max(0L, estimatedArchiveBytes);
        this.fingerprint = Objects.requireNonNull(fingerprint);
        this.tailBytes = Objects.requireNonNull(tailBytes);
    }

    @NotNull
    public static AfmaStoredPayload fromBytes(@NotNull byte[] payloadBytes) throws IOException {
        Objects.requireNonNull(payloadBytes);
        return new AfmaStoredPayload(
                PAYLOAD_STORE.storeBytes(payloadBytes.clone()),
                payloadBytes.length,
                AfmaPayloadMetricsHelper.estimateArchiveBytes(payloadBytes),
                AfmaPayloadMetricsHelper.fingerprintPayload(payloadBytes),
                tailBytes(payloadBytes)
        );
    }

    @NotNull
    public static AfmaStoredPayload fromBytes(@NotNull PayloadSummary payloadSummary, @NotNull byte[] payloadBytes) throws IOException {
        Objects.requireNonNull(payloadSummary);
        Objects.requireNonNull(payloadBytes);
        if (payloadSummary.length() != payloadBytes.length) {
            throw new IOException("AFMA payload summary length does not match the provided payload bytes");
        }

        return new AfmaStoredPayload(
                PAYLOAD_STORE.storeBytes(payloadBytes),
                payloadSummary.length(),
                payloadSummary.estimatedArchiveBytes(),
                payloadSummary.fingerprint(),
                payloadSummary.tailBytes()
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
        SpillablePayloadOutputStream payloadOut = new SpillablePayloadOutputStream(PAYLOAD_STORE);
        boolean success = false;
        try (PayloadAnalysisOutputStream out = new PayloadAnalysisOutputStream(payloadOut)) {
            writer.write(out);
            PayloadAnalysis analysis = out.finishAnalysis();
            success = true;
            return new AfmaStoredPayload(
                    payloadOut.finishPayload(),
                    analysis.length(),
                    analysis.estimatedArchiveBytes(),
                    analysis.fingerprint(),
                    analysis.tailBytes()
            );
        } finally {
            if (!success) {
                payloadOut.abort();
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

    @NotNull
    public static BufferedPayload capture(@NotNull Writer writer) throws IOException {
        Objects.requireNonNull(writer);
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try (PayloadAnalysisOutputStream out = new PayloadAnalysisOutputStream(byteOut)) {
            writer.write(out);
            PayloadAnalysis analysis = out.finishAnalysis();
            return new BufferedPayload(
                    byteOut.toByteArray(),
                    new PayloadSummary(
                            analysis.length(),
                            analysis.estimatedArchiveBytes(),
                            analysis.fingerprint(),
                            analysis.tailBytes()
                    )
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
        return this.storage.openStream();
    }

    @NotNull
    public byte[] readAllBytes() throws IOException {
        this.ensureOpen();
        return this.storage.readAllBytes();
    }

    public void writeTo(@NotNull OutputStream out) throws IOException {
        Objects.requireNonNull(out);
        this.ensureOpen();
        this.storage.writeTo(out);
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
        return this.storage.requireFileBacked();
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        this.storage.release();
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

    protected static long computeHeapBudgetBytes() {
        long maxMemory = Runtime.getRuntime().maxMemory();
        if (maxMemory <= 0L || maxMemory == Long.MAX_VALUE) {
            return MAX_HEAP_BUDGET_BYTES;
        }

        long targetBudget = maxMemory / 8L;
        targetBudget = Math.max(MIN_HEAP_BUDGET_BYTES, targetBudget);
        return Math.min(MAX_HEAP_BUDGET_BYTES, targetBudget);
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

    public record BufferedPayload(@NotNull byte[] payloadBytes, @NotNull PayloadSummary payloadSummary) {

        public BufferedPayload {
            payloadBytes = Objects.requireNonNull(payloadBytes);
            payloadSummary = Objects.requireNonNull(payloadSummary);
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

    protected static final class SpillablePayloadOutputStream extends OutputStream {

        @NotNull
        protected final PayloadStore payloadStore;
        @NotNull
        protected byte[] heapBuffer = new byte[Math.min(8192, INLINE_MEMORY_PAYLOAD_BYTES)];
        protected int heapLength = 0;
        @Nullable
        protected SpoolAppendSession spoolSession = null;
        @Nullable
        protected PayloadStorage finishedPayload = null;
        protected boolean closed = false;

        protected SpillablePayloadOutputStream(@NotNull PayloadStore payloadStore) {
            this.payloadStore = Objects.requireNonNull(payloadStore);
        }

        @Override
        public void write(int value) throws IOException {
            this.ensureOpen();
            if (this.spoolSession != null) {
                this.spoolSession.write(value);
                return;
            }

            if ((this.heapLength + 1) > INLINE_MEMORY_PAYLOAD_BYTES) {
                this.spillHeapBuffer();
                this.spoolSession.write(value);
                return;
            }

            this.ensureHeapCapacity(1);
            this.heapBuffer[this.heapLength++] = (byte) value;
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

            this.ensureOpen();
            if (this.spoolSession != null) {
                this.spoolSession.write(source, offset, count);
                return;
            }

            if ((this.heapLength + count) > INLINE_MEMORY_PAYLOAD_BYTES) {
                this.spillHeapBuffer();
                this.spoolSession.write(source, offset, count);
                return;
            }

            this.ensureHeapCapacity(count);
            System.arraycopy(source, offset, this.heapBuffer, this.heapLength, count);
            this.heapLength += count;
        }

        @Override
        public void close() throws IOException {
            if (this.closed) {
                return;
            }
            this.closed = true;
            if (this.finishedPayload == null) {
                this.abort();
            }
        }

        @NotNull
        public PayloadStorage finishPayload() throws IOException {
            this.ensureOpen();
            if (this.finishedPayload != null) {
                return this.finishedPayload;
            }

            PayloadStorage payload;
            if (this.spoolSession != null) {
                payload = this.payloadStore.storeSegment(this.spoolSession.finish());
                this.spoolSession = null;
            } else {
                payload = this.payloadStore.storeBytes(Arrays.copyOf(this.heapBuffer, this.heapLength));
                this.heapLength = 0;
            }

            this.finishedPayload = payload;
            return payload;
        }

        public void abort() {
            if (this.finishedPayload != null) {
                return;
            }
            if (this.spoolSession != null) {
                this.spoolSession.abort();
                this.spoolSession = null;
            }
            this.heapLength = 0;
        }

        protected void ensureHeapCapacity(int additionalBytes) {
            int requiredLength = this.heapLength + additionalBytes;
            if (requiredLength <= this.heapBuffer.length) {
                return;
            }

            int nextLength = this.heapBuffer.length;
            while (nextLength < requiredLength) {
                nextLength = Math.min(INLINE_MEMORY_PAYLOAD_BYTES, Math.max(nextLength << 1, requiredLength));
            }
            this.heapBuffer = Arrays.copyOf(this.heapBuffer, nextLength);
        }

        protected void spillHeapBuffer() throws IOException {
            if (this.spoolSession == null) {
                this.spoolSession = this.payloadStore.beginAppendSession();
            }
            if (this.heapLength > 0) {
                this.spoolSession.write(this.heapBuffer, 0, this.heapLength);
                this.heapLength = 0;
            }
        }

        protected void ensureOpen() throws IOException {
            if (this.closed) {
                throw new IOException("AFMA spillable payload output has already been closed");
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

    protected static final class PayloadStorage {

        @NotNull
        protected final PayloadStore payloadStore;
        protected final int length;
        @Nullable
        protected byte[] heapBytes;
        @Nullable
        protected SpoolSegment spoolSegment;
        protected boolean released = false;

        protected PayloadStorage(@NotNull PayloadStore payloadStore, @Nullable byte[] heapBytes, @Nullable SpoolSegment spoolSegment, int length) {
            this.payloadStore = Objects.requireNonNull(payloadStore);
            this.heapBytes = heapBytes;
            this.spoolSegment = spoolSegment;
            this.length = Math.max(0, length);
        }

        @NotNull
        public InputStream openStream() throws IOException {
            return this.payloadStore.openStream(this);
        }

        @NotNull
        public byte[] readAllBytes() throws IOException {
            return this.payloadStore.readAllBytes(this);
        }

        public void writeTo(@NotNull OutputStream out) throws IOException {
            this.payloadStore.writeTo(this, out);
        }

        public void release() {
            this.payloadStore.release(this);
        }

        @NotNull
        public File requireFileBacked() {
            return this.payloadStore.requireFileBacked(this);
        }
    }

    protected static final class PayloadStore {

        protected final int inlineMemoryPayloadBytes;
        protected final long maxHeapBytes;
        @NotNull
        protected final LinkedHashMap<PayloadStorage, Boolean> heapPayloads = new LinkedHashMap<>(16, 0.75F, true);
        protected long heapBytes = 0L;
        @Nullable
        protected File spoolFile = null;
        @Nullable
        protected FileOutputStream spoolOut = null;
        protected long spoolLength = 0L;
        protected int activeSpoolSegments = 0;
        protected boolean appendReserved = false;

        protected PayloadStore(int inlineMemoryPayloadBytes, long maxHeapBytes) {
            this.inlineMemoryPayloadBytes = Math.max(0, inlineMemoryPayloadBytes);
            this.maxHeapBytes = Math.max(0L, maxHeapBytes);
        }

        @NotNull
        public synchronized PayloadStorage storeBytes(@NotNull byte[] payloadBytes) throws IOException {
            Objects.requireNonNull(payloadBytes);
            if (payloadBytes.length <= this.inlineMemoryPayloadBytes) {
                PayloadStorage payload = new PayloadStorage(this, payloadBytes, null, payloadBytes.length);
                this.heapPayloads.put(payload, Boolean.TRUE);
                this.heapBytes += payloadBytes.length;
                this.enforceHeapBudget();
                return payload;
            }
            return this.storeSegment(this.appendBytes(payloadBytes, 0, payloadBytes.length));
        }

        @NotNull
        public synchronized PayloadStorage storeSegment(@NotNull SpoolSegment spoolSegment) {
            Objects.requireNonNull(spoolSegment);
            return new PayloadStorage(this, null, spoolSegment, spoolSegment.length());
        }

        @NotNull
        public synchronized SpoolAppendSession beginAppendSession() throws IOException {
            while (this.appendReserved) {
                try {
                    this.wait();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while reserving the AFMA payload spool", ex);
                }
            }

            this.ensureSpoolOutput();
            this.appendReserved = true;
            return new SpoolAppendSession(this, this.spoolLength);
        }

        @NotNull
        public InputStream openStream(@NotNull PayloadStorage payload) throws IOException {
            PayloadSnapshot payloadSnapshot = this.snapshot(payload);
            if (payloadSnapshot.heapBytes() != null) {
                return new ByteArrayInputStream(payloadSnapshot.heapBytes());
            }
            return this.openSpoolStream(Objects.requireNonNull(payloadSnapshot.spoolSegment()));
        }

        @NotNull
        public byte[] readAllBytes(@NotNull PayloadStorage payload) throws IOException {
            PayloadSnapshot payloadSnapshot = this.snapshot(payload);
            if (payloadSnapshot.heapBytes() != null) {
                return payloadSnapshot.heapBytes().clone();
            }
            try (InputStream in = this.openSpoolStream(Objects.requireNonNull(payloadSnapshot.spoolSegment()));
                 ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(32, payloadSnapshot.length()))) {
                IOUtils.copyLarge(in, out);
                return out.toByteArray();
            }
        }

        public void writeTo(@NotNull PayloadStorage payload, @NotNull OutputStream out) throws IOException {
            Objects.requireNonNull(out);
            PayloadSnapshot payloadSnapshot = this.snapshot(payload);
            if (payloadSnapshot.heapBytes() != null) {
                out.write(payloadSnapshot.heapBytes());
                return;
            }

            try (InputStream in = this.openSpoolStream(Objects.requireNonNull(payloadSnapshot.spoolSegment()))) {
                IOUtils.copyLarge(in, out);
            }
        }

        public synchronized void release(@NotNull PayloadStorage payload) {
            if (payload.released) {
                return;
            }

            payload.released = true;
            if (payload.heapBytes != null) {
                if (this.heapPayloads.remove(payload) != null) {
                    this.heapBytes -= payload.heapBytes.length;
                }
                payload.heapBytes = null;
                return;
            }

            if (payload.spoolSegment != null) {
                payload.spoolSegment = null;
                if (this.activeSpoolSegments > 0) {
                    this.activeSpoolSegments--;
                }
                this.cleanupSpoolIfUnused();
            }
        }

        @NotNull
        public synchronized File requireFileBacked(@NotNull PayloadStorage payload) {
            this.ensureActive(payload);
            if (payload.spoolSegment == null) {
                throw new IllegalStateException("AFMA payload is currently stored in memory");
            }
            return payload.spoolSegment.file();
        }

        protected synchronized void append(@NotNull SpoolAppendSession session, @NotNull byte[] source, int offset, int count) throws IOException {
            if (count <= 0) {
                return;
            }
            if (!this.appendReserved || session.finished) {
                throw new IOException("AFMA payload spool append session is no longer active");
            }

            Objects.requireNonNull(this.spoolOut).write(source, offset, count);
            session.length += count;
            this.spoolLength += count;
        }

        protected synchronized SpoolSegment finishAppend(@NotNull SpoolAppendSession session) throws IOException {
            if (session.finished) {
                throw new IOException("AFMA payload spool append session has already been closed");
            }

            Objects.requireNonNull(this.spoolOut).flush();
            session.finished = true;
            this.appendReserved = false;
            this.activeSpoolSegments++;
            this.notifyAll();
            return new SpoolSegment(Objects.requireNonNull(this.spoolFile), session.offset, session.length);
        }

        protected synchronized void abortAppend(@NotNull SpoolAppendSession session) {
            if (session.finished) {
                return;
            }

            session.finished = true;
            this.appendReserved = false;
            this.notifyAll();
            this.cleanupSpoolIfUnused();
        }

        protected synchronized void ensureSpoolOutput() throws IOException {
            if (this.spoolOut != null) {
                return;
            }

            this.spoolFile = createTempFile();
            this.spoolOut = new FileOutputStream(this.spoolFile, true);
            this.spoolLength = 0L;
        }

        protected synchronized void cleanupSpoolIfUnused() {
            if (this.appendReserved || (this.activeSpoolSegments > 0)) {
                return;
            }
            if (this.spoolOut != null) {
                IOUtils.closeQuietly(this.spoolOut);
                this.spoolOut = null;
            }
            if (this.spoolFile != null) {
                org.apache.commons.io.FileUtils.deleteQuietly(this.spoolFile);
                this.spoolFile = null;
            }
            this.spoolLength = 0L;
        }

        protected synchronized void ensureActive(@NotNull PayloadStorage payload) {
            if (payload.released) {
                throw new IllegalStateException("AFMA payload storage has already been released");
            }
        }

        @NotNull
        protected synchronized PayloadSnapshot snapshot(@NotNull PayloadStorage payload) {
            this.ensureActive(payload);
            if (payload.heapBytes != null) {
                this.heapPayloads.get(payload);
                return new PayloadSnapshot(payload.heapBytes, null, payload.length);
            }
            return new PayloadSnapshot(null, Objects.requireNonNull(payload.spoolSegment), payload.length);
        }

        protected synchronized void enforceHeapBudget() throws IOException {
            if (this.maxHeapBytes <= 0L) {
                this.evictAllHeapPayloads();
                return;
            }

            Iterator<PayloadStorage> iterator = this.heapPayloads.keySet().iterator();
            while ((this.heapBytes > this.maxHeapBytes) && iterator.hasNext()) {
                PayloadStorage payload = iterator.next();
                byte[] heapBytes = payload.heapBytes;
                iterator.remove();
                if (heapBytes == null || payload.released) {
                    continue;
                }

                this.heapBytes -= heapBytes.length;
                payload.heapBytes = null;
                payload.spoolSegment = this.appendBytes(heapBytes, 0, heapBytes.length);
            }
        }

        protected synchronized void evictAllHeapPayloads() throws IOException {
            Iterator<PayloadStorage> iterator = this.heapPayloads.keySet().iterator();
            while (iterator.hasNext()) {
                PayloadStorage payload = iterator.next();
                byte[] heapBytes = payload.heapBytes;
                iterator.remove();
                if (heapBytes == null || payload.released) {
                    continue;
                }

                this.heapBytes -= heapBytes.length;
                payload.heapBytes = null;
                payload.spoolSegment = this.appendBytes(heapBytes, 0, heapBytes.length);
            }
        }

        @NotNull
        protected synchronized SpoolSegment appendBytes(@NotNull byte[] source, int offset, int count) throws IOException {
            SpoolAppendSession session = this.beginAppendSession();
            boolean success = false;
            try {
                session.write(source, offset, count);
                SpoolSegment segment = session.finish();
                success = true;
                return segment;
            } finally {
                if (!success) {
                    session.abort();
                }
            }
        }

        @NotNull
        protected InputStream openSpoolStream(@NotNull SpoolSegment spoolSegment) throws IOException {
            FileInputStream fileIn = new FileInputStream(spoolSegment.file());
            boolean success = false;
            try {
                fileIn.getChannel().position(spoolSegment.offset());
                InputStream in = new BoundedInputStream(fileIn, spoolSegment.length());
                success = true;
                return new BufferedInputStream(in);
            } finally {
                if (!success) {
                    IOUtils.closeQuietly(fileIn);
                }
            }
        }
    }

    protected static final class SpoolAppendSession {

        @NotNull
        protected final PayloadStore payloadStore;
        @NotNull
        protected final byte[] singleByteBuffer = new byte[1];
        protected final long offset;
        protected int length = 0;
        protected boolean finished = false;

        protected SpoolAppendSession(@NotNull PayloadStore payloadStore, long offset) {
            this.payloadStore = Objects.requireNonNull(payloadStore);
            this.offset = Math.max(0L, offset);
        }

        public void write(int value) throws IOException {
            this.singleByteBuffer[0] = (byte) value;
            this.write(this.singleByteBuffer, 0, 1);
        }

        public void write(@NotNull byte[] source, int offset, int count) throws IOException {
            this.payloadStore.append(this, source, offset, count);
        }

        @NotNull
        public SpoolSegment finish() throws IOException {
            return this.payloadStore.finishAppend(this);
        }

        public void abort() {
            this.payloadStore.abortAppend(this);
        }
    }

    protected record SpoolSegment(@NotNull File file, long offset, int length) {

        public SpoolSegment {
            file = Objects.requireNonNull(file);
            offset = Math.max(0L, offset);
            length = Math.max(0, length);
        }
    }

    protected record PayloadSnapshot(@Nullable byte[] heapBytes, @Nullable SpoolSegment spoolSegment, int length) {
    }

    protected static final class BoundedInputStream extends InputStream {

        @NotNull
        protected final InputStream delegate;
        protected int remainingBytes;

        protected BoundedInputStream(@NotNull InputStream delegate, int remainingBytes) {
            this.delegate = Objects.requireNonNull(delegate);
            this.remainingBytes = Math.max(0, remainingBytes);
        }

        @Override
        public int read() throws IOException {
            if (this.remainingBytes <= 0) {
                return -1;
            }
            int read = this.delegate.read();
            if (read >= 0) {
                this.remainingBytes--;
            }
            return read;
        }

        @Override
        public int read(@NotNull byte[] target, int offset, int length) throws IOException {
            Objects.requireNonNull(target);
            if (offset < 0 || length < 0 || length > target.length - offset) {
                throw new IndexOutOfBoundsException();
            }
            if (this.remainingBytes <= 0) {
                return -1;
            }

            int read = this.delegate.read(target, offset, Math.min(length, this.remainingBytes));
            if (read > 0) {
                this.remainingBytes -= read;
            }
            return read;
        }

        @Override
        public void close() throws IOException {
            this.delegate.close();
        }
    }

}
