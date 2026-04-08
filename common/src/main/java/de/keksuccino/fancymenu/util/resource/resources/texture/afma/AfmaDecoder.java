package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.file.FileUtils;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class AfmaDecoder implements Closeable {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    private static final File TEMP_DIR = FileUtils.createDirectory(new File(FancyMenu.TEMP_DATA_DIR, "/decoded_afma_images"));
    @Nullable
    protected ZipFile zipFile = null;
    @Nullable
    protected AfmaMetadata metadata = null;
    @Nullable
    protected AfmaFrameIndex frameIndex = null;
    @NotNull
    protected final Map<String, ZipArchiveEntry> entriesByNormalizedPath = new HashMap<>();
    @NotNull
    protected final Map<String, AfmaChunkedPayloadHelper.PayloadLocator> payloadLocatorsByNormalizedPath = new HashMap<>();
    @NotNull
    protected int[] payloadChunkLengths = new int[0];
    protected final Object payloadChunkCacheLock = new Object();
    @Nullable
    protected LoadedPayloadChunk loadedPayloadChunk = null;

    @Nullable
    protected File tempArchiveFile = null;
    protected boolean deleteTempArchiveOnClose = false;

    public void read(@NotNull InputStream in) throws IOException {
        Objects.requireNonNull(in);
        if (this.zipFile != null) throw new IllegalStateException("The decoder is already reading a file!");

        try {
            File tempArchive = this.spoolToTempArchive(in);
            this.openArchive(tempArchive, true);
            this.initializeArchiveState();
        } catch (Exception ex) {
            this.close();
            throw new IOException(ex);
        } finally {
            try {
                in.close();
            } catch (Exception ignored) {
            }
        }
    }

    public void read(@NotNull File afmaFile) throws IOException {
        Objects.requireNonNull(afmaFile);
        if (this.zipFile != null) throw new IllegalStateException("The decoder is already reading a file!");

        try {
            this.openArchive(afmaFile, false);
            this.initializeArchiveState();
        } catch (Exception ex) {
            this.close();
            throw new IOException(ex);
        }
    }

    @NotNull
    protected File spoolToTempArchive(@NotNull InputStream in) throws IOException {
        File tempArchive = File.createTempFile("afma_stream_", ".afma", TEMP_DIR);
        try (BufferedInputStream bufferedIn = new BufferedInputStream(in);
             BufferedOutputStream bufferedOut = new BufferedOutputStream(new FileOutputStream(tempArchive))) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = bufferedIn.read(buffer)) >= 0) {
                bufferedOut.write(buffer, 0, read);
            }
            bufferedOut.flush();
        } catch (Exception ex) {
            if (tempArchive.exists() && !tempArchive.delete()) {
                LOGGER.warn("[FANCYMENU] Failed to delete temporary AFMA archive after spool failure: {}", tempArchive.getAbsolutePath());
            }
            throw ex;
        }
        return tempArchive;
    }

    protected void openArchive(@NotNull File archiveFile, boolean deleteOnClose) throws IOException {
        this.zipFile = new ZipFile(archiveFile);
        this.tempArchiveFile = deleteOnClose ? archiveFile : null;
        this.deleteTempArchiveOnClose = deleteOnClose;
    }

    protected void initializeArchiveState() throws IOException {
        this.indexEntries();
        this.readMetadata();
        this.readPayloadIndex();
        this.readFrameIndex();
        this.validateArchiveState();
    }

    protected void indexEntries() throws IOException {
        Objects.requireNonNull(this.zipFile);
        this.entriesByNormalizedPath.clear();

        var entries = this.zipFile.getEntries();
        while (entries.hasMoreElements()) {
            ZipArchiveEntry entry = entries.nextElement();
            if (entry == null || entry.isDirectory()) continue;
            String normalized = normalizeEntryPath(entry.getName()).toLowerCase(Locale.ROOT);
            this.entriesByNormalizedPath.putIfAbsent(normalized, entry);
        }
    }

    protected void readMetadata() throws IOException {
        Objects.requireNonNull(this.zipFile);
        ZipArchiveEntry metadataEntry = this.findEntry("metadata.json");
        if (metadataEntry == null) {
            throw new FileNotFoundException("No metadata.json found in AFMA file");
        }

        try (InputStream metadataIn = this.zipFile.getInputStream(metadataEntry)) {
            String metadataString = new String(metadataIn.readAllBytes(), StandardCharsets.UTF_8);
            if (metadataString.trim().isEmpty()) {
                throw new IOException("metadata.json of AFMA file is empty");
            }

            AfmaMetadata parsedMetadata = GSON.fromJson(metadataString, AfmaMetadata.class);
            if (parsedMetadata == null) {
                throw new IOException("Unable to parse metadata.json of AFMA file");
            }
            parsedMetadata.validate();
            this.metadata = parsedMetadata;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    protected void readPayloadIndex() throws IOException {
        Objects.requireNonNull(this.zipFile);
        this.payloadLocatorsByNormalizedPath.clear();
        this.payloadChunkLengths = new int[0];
        this.loadedPayloadChunk = null;

        ZipArchiveEntry payloadIndexEntry = this.findEntry(AfmaChunkedPayloadHelper.PAYLOAD_INDEX_ENTRY_PATH);
        if (payloadIndexEntry == null) {
            throw new FileNotFoundException("No " + AfmaChunkedPayloadHelper.PAYLOAD_INDEX_ENTRY_PATH + " found in AFMA file");
        }

        try (InputStream payloadIndexIn = this.zipFile.getInputStream(payloadIndexEntry)) {
            byte[] payloadIndexBytes = payloadIndexIn.readAllBytes();
            if (payloadIndexBytes.length <= 0) {
                throw new IOException("AFMA payload index is empty");
            }

            AfmaChunkedPayloadHelper.DecodedPayloadIndex decodedPayloadIndex = AfmaChunkedPayloadHelper.decodePayloadIndex(payloadIndexBytes);
            this.payloadLocatorsByNormalizedPath.putAll(decodedPayloadIndex.payloadLocatorsByPath());
            this.payloadChunkLengths = decodedPayloadIndex.chunkLengths();
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    protected void readFrameIndex() throws IOException {
        Objects.requireNonNull(this.zipFile);
        ZipArchiveEntry frameIndexEntry = this.findEntry(AfmaBinaryFrameIndexHelper.FRAME_INDEX_ENTRY_PATH);
        if (frameIndexEntry == null) {
            throw new FileNotFoundException("No " + AfmaBinaryFrameIndexHelper.FRAME_INDEX_ENTRY_PATH + " found in AFMA file");
        }

        try (InputStream frameIndexIn = this.zipFile.getInputStream(frameIndexEntry)) {
            byte[] frameIndexBytes = frameIndexIn.readAllBytes();
            if (frameIndexBytes.length <= 0) {
                throw new IOException(AfmaBinaryFrameIndexHelper.FRAME_INDEX_ENTRY_PATH + " of AFMA file is empty");
            }
            AfmaFrameIndex parsedFrameIndex = AfmaBinaryFrameIndexHelper.decodeFrameIndex(frameIndexBytes);
            if (parsedFrameIndex == null) {
                throw new IOException("Unable to parse " + AfmaBinaryFrameIndexHelper.FRAME_INDEX_ENTRY_PATH + " of AFMA file");
            }
            this.frameIndex = parsedFrameIndex;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    protected void validateArchiveState() throws IOException {
        AfmaMetadata activeMetadata = Objects.requireNonNull(this.metadata, "AFMA metadata was NULL");
        AfmaFrameIndex activeFrameIndex = Objects.requireNonNull(this.frameIndex, "AFMA frame index was NULL");

        List<AfmaFrameDescriptor> frames = new ArrayList<>(activeFrameIndex.getFrames());
        List<AfmaFrameDescriptor> introFrames = new ArrayList<>(activeFrameIndex.getIntroFrames());

        if (frames.isEmpty() && introFrames.isEmpty()) {
            throw new IOException("AFMA file does not contain any frames");
        }

        if (!frames.isEmpty()) {
            this.validateSequence(frames, false, activeMetadata);
        }
        if (!introFrames.isEmpty()) {
            this.validateSequence(introFrames, true, activeMetadata);
        }

        this.validateBootstrapPayloads(frames, activeMetadata, false);
        this.validateBootstrapPayloads(introFrames, activeMetadata, true);
    }

    protected void validateSequence(@NotNull List<AfmaFrameDescriptor> sequence, boolean introSequence, @NotNull AfmaMetadata activeMetadata) {
        for (int i = 0; i < sequence.size(); i++) {
            AfmaFrameDescriptor descriptor = sequence.get(i);
            if (descriptor == null) {
                throw new IllegalArgumentException((introSequence ? "Intro" : "Main") + " frame " + i + " is NULL");
            }
            descriptor.validate((introSequence ? "Intro" : "Main") + " frame " + i, activeMetadata.getCanvasWidth(), activeMetadata.getCanvasHeight(), i == 0);
            if (descriptor.requiresPrimaryPayload() && !this.hasPayload(Objects.requireNonNull(descriptor.getPrimaryPayloadPath()))) {
                throw new IllegalArgumentException("Referenced AFMA payload was not found: " + descriptor.getPrimaryPayloadPath());
            }
            if (descriptor.requiresPatchPayload() && !this.hasPayload(Objects.requireNonNull(descriptor.getSecondaryPayloadPath()))) {
                throw new IllegalArgumentException("Referenced AFMA secondary payload was not found: " + descriptor.getSecondaryPayloadPath());
            }
        }
    }

    protected void validateBootstrapPayloads(@NotNull List<AfmaFrameDescriptor> sequence, @NotNull AfmaMetadata activeMetadata, boolean introSequence) throws IOException {
        if (sequence.isEmpty()) {
            return;
        }

        AfmaFrameDescriptor descriptor = sequence.get(0);
        String context = (introSequence ? "Intro" : "Main") + " frame 0";
        if (descriptor.getType() == AfmaFrameOperationType.FULL) {
            this.validateBinIntraPayload(context, Objects.requireNonNull(descriptor.getPrimaryPayloadPath()), activeMetadata.getCanvasWidth(), activeMetadata.getCanvasHeight());
        } else if (descriptor.getType() == AfmaFrameOperationType.DELTA_RECT) {
            this.validateBinIntraPayload(context, Objects.requireNonNull(descriptor.getPrimaryPayloadPath()), descriptor.getWidth(), descriptor.getHeight());
        } else if (descriptor.getType() == AfmaFrameOperationType.RESIDUAL_DELTA_RECT) {
            AfmaResidualPayload residual = Objects.requireNonNull(descriptor.getResidual());
            this.validateRawPayloadSize(context, Objects.requireNonNull(descriptor.getPrimaryPayloadPath()),
                    AfmaResidualPayloadHelper.expectedDenseResidualBytes(descriptor.getWidth(), descriptor.getHeight(), residual.getChannels()));
        } else if (descriptor.getType() == AfmaFrameOperationType.SPARSE_DELTA_RECT) {
            AfmaSparsePayload sparse = Objects.requireNonNull(descriptor.getSparse());
            this.validateRawPayloadSize(context, Objects.requireNonNull(descriptor.getPrimaryPayloadPath()),
                    AfmaResidualPayloadHelper.expectedSparseMaskBytes(descriptor.getWidth(), descriptor.getHeight()));
            this.validateRawPayloadSize(context, Objects.requireNonNull(sparse.getPixelsPath()),
                    AfmaResidualPayloadHelper.expectedSparseResidualBytes(sparse.getChangedPixelCount(), sparse.getChannels()));
        } else if (descriptor.getType() == AfmaFrameOperationType.COPY_RECT_RESIDUAL_PATCH) {
            AfmaResidualPayload residual = Objects.requireNonNull(descriptor.getResidual());
            this.validateRawPayloadSize(context, Objects.requireNonNull(descriptor.getPrimaryPayloadPath()),
                    AfmaResidualPayloadHelper.expectedDenseResidualBytes(descriptor.getWidth(), descriptor.getHeight(), residual.getChannels()));
        } else if (descriptor.getType() == AfmaFrameOperationType.COPY_RECT_SPARSE_PATCH) {
            AfmaSparsePayload sparse = Objects.requireNonNull(descriptor.getSparse());
            this.validateRawPayloadSize(context, Objects.requireNonNull(descriptor.getPrimaryPayloadPath()),
                    AfmaResidualPayloadHelper.expectedSparseMaskBytes(descriptor.getWidth(), descriptor.getHeight()));
            this.validateRawPayloadSize(context, Objects.requireNonNull(sparse.getPixelsPath()),
                    AfmaResidualPayloadHelper.expectedSparseResidualBytes(sparse.getChangedPixelCount(), sparse.getChannels()));
        } else if ((descriptor.getType() == AfmaFrameOperationType.COPY_RECT_PATCH) && descriptor.requiresPatchPayload()) {
            AfmaPatchRegion patch = Objects.requireNonNull(descriptor.getPatch());
            this.validateBinIntraPayload(context, Objects.requireNonNull(descriptor.getSecondaryPayloadPath()), patch.getWidth(), patch.getHeight());
        } else if (descriptor.getType() == AfmaFrameOperationType.BLOCK_INTER) {
            this.validateBlockInterPayload(context, descriptor);
        }
    }

    protected void validateBinIntraPayload(@NotNull String context, @NotNull String path, int expectedWidth, int expectedHeight) throws IOException {
        PayloadBytes payloadBytes = this.readPayloadBytes(path);
        AfmaBinIntraPayloadHelper.validatePayload(payloadBytes.bytes(), payloadBytes.offset(), payloadBytes.length(), expectedWidth, expectedHeight);
    }

    protected void validateRawPayloadSize(@NotNull String context, @NotNull String path, int expectedSize) throws IOException {
        if (expectedSize <= 0) {
            throw new IOException(context + " references an invalid raw payload size for " + path);
        }
        PayloadBytes payloadBytes = this.readPayloadBytes(path);
        if (payloadBytes.length() != expectedSize) {
            throw new IOException(context + " raw payload size does not match the descriptor for " + path);
        }
    }

    @NotNull
    protected Set<String> collectReferencedPayloadPaths(@NotNull List<AfmaFrameDescriptor> frames, @NotNull List<AfmaFrameDescriptor> introFrames) {
        Set<String> payloads = new LinkedHashSet<>();
        this.collectReferencedPayloadPaths(frames, payloads);
        this.collectReferencedPayloadPaths(introFrames, payloads);
        return payloads;
    }

    protected void collectReferencedPayloadPaths(@NotNull List<AfmaFrameDescriptor> sequence, @NotNull Set<String> payloads) {
        for (AfmaFrameDescriptor descriptor : sequence) {
            if (descriptor.requiresPrimaryPayload()) {
                payloads.add(normalizeEntryPath(Objects.requireNonNull(descriptor.getPrimaryPayloadPath())).toLowerCase(Locale.ROOT));
            }
            if (descriptor.requiresPatchPayload()) {
                payloads.add(normalizeEntryPath(Objects.requireNonNull(descriptor.getSecondaryPayloadPath())).toLowerCase(Locale.ROOT));
            }
        }
    }

    public void validateAllReferencedPayloadHeaders() throws IOException {
        AfmaMetadata activeMetadata = Objects.requireNonNull(this.metadata, "AFMA metadata was NULL");
        AfmaFrameIndex activeFrameIndex = Objects.requireNonNull(this.frameIndex, "AFMA frame index was NULL");
        this.validateAllPayloadHeaders(activeFrameIndex.getFrames(), false, activeMetadata);
        this.validateAllPayloadHeaders(activeFrameIndex.getIntroFrames(), true, activeMetadata);
    }

    protected void validateAllPayloadHeaders(@NotNull List<AfmaFrameDescriptor> sequence, boolean introSequence, @NotNull AfmaMetadata activeMetadata) throws IOException {
        for (int i = 0; i < sequence.size(); i++) {
            AfmaFrameDescriptor descriptor = sequence.get(i);
            if (descriptor == null) {
                continue;
            }

            String context = (introSequence ? "Intro" : "Main") + " frame " + i;
            if (descriptor.getType() == AfmaFrameOperationType.FULL) {
                this.validateBinIntraPayload(context, Objects.requireNonNull(descriptor.getPrimaryPayloadPath()), activeMetadata.getCanvasWidth(), activeMetadata.getCanvasHeight());
            } else if (descriptor.getType() == AfmaFrameOperationType.DELTA_RECT) {
                this.validateBinIntraPayload(context, Objects.requireNonNull(descriptor.getPrimaryPayloadPath()), descriptor.getWidth(), descriptor.getHeight());
            } else if (descriptor.getType() == AfmaFrameOperationType.RESIDUAL_DELTA_RECT) {
                AfmaResidualPayload residual = Objects.requireNonNull(descriptor.getResidual());
                this.validateRawPayloadSize(context, Objects.requireNonNull(descriptor.getPrimaryPayloadPath()),
                        AfmaResidualPayloadHelper.expectedDenseResidualBytes(descriptor.getWidth(), descriptor.getHeight(), residual.getChannels()));
            } else if (descriptor.getType() == AfmaFrameOperationType.SPARSE_DELTA_RECT) {
                AfmaSparsePayload sparse = Objects.requireNonNull(descriptor.getSparse());
                this.validateRawPayloadSize(context, Objects.requireNonNull(descriptor.getPrimaryPayloadPath()),
                        AfmaResidualPayloadHelper.expectedSparseMaskBytes(descriptor.getWidth(), descriptor.getHeight()));
                this.validateRawPayloadSize(context, Objects.requireNonNull(sparse.getPixelsPath()),
                        AfmaResidualPayloadHelper.expectedSparseResidualBytes(sparse.getChangedPixelCount(), sparse.getChannels()));
            } else if (descriptor.getType() == AfmaFrameOperationType.COPY_RECT_RESIDUAL_PATCH) {
                AfmaResidualPayload residual = Objects.requireNonNull(descriptor.getResidual());
                this.validateRawPayloadSize(context, Objects.requireNonNull(descriptor.getPrimaryPayloadPath()),
                        AfmaResidualPayloadHelper.expectedDenseResidualBytes(descriptor.getWidth(), descriptor.getHeight(), residual.getChannels()));
            } else if (descriptor.getType() == AfmaFrameOperationType.COPY_RECT_SPARSE_PATCH) {
                AfmaSparsePayload sparse = Objects.requireNonNull(descriptor.getSparse());
                this.validateRawPayloadSize(context, Objects.requireNonNull(descriptor.getPrimaryPayloadPath()),
                        AfmaResidualPayloadHelper.expectedSparseMaskBytes(descriptor.getWidth(), descriptor.getHeight()));
                this.validateRawPayloadSize(context, Objects.requireNonNull(sparse.getPixelsPath()),
                        AfmaResidualPayloadHelper.expectedSparseResidualBytes(sparse.getChangedPixelCount(), sparse.getChannels()));
            } else if ((descriptor.getType() == AfmaFrameOperationType.COPY_RECT_PATCH) && descriptor.requiresPatchPayload()) {
                AfmaPatchRegion patch = Objects.requireNonNull(descriptor.getPatch());
                this.validateBinIntraPayload(context, Objects.requireNonNull(descriptor.getSecondaryPayloadPath()), patch.getWidth(), patch.getHeight());
            } else if (descriptor.getType() == AfmaFrameOperationType.BLOCK_INTER) {
                this.validateBlockInterPayload(context, descriptor);
            }
        }
    }

    protected void validateBlockInterPayload(@NotNull String context, @NotNull AfmaFrameDescriptor descriptor) throws IOException {
        AfmaBlockInter blockInter = Objects.requireNonNull(descriptor.getBlockInter(), "AFMA block_inter metadata was NULL");
        PayloadBytes payloadBytes = this.readPayloadBytes(Objects.requireNonNull(descriptor.getPrimaryPayloadPath()));
        AfmaMetadata activeMetadata = Objects.requireNonNull(this.metadata, "AFMA metadata was NULL");
        AfmaBlockInterPayloadHelper.validatePayload(
                payloadBytes.bytes(),
                payloadBytes.offset(),
                payloadBytes.length(),
                blockInter.getTileSize(),
                descriptor.getX(),
                descriptor.getY(),
                descriptor.getWidth(),
                descriptor.getHeight(),
                activeMetadata.getCanvasWidth(),
                activeMetadata.getCanvasHeight()
        );
    }

    public int getFrameCount() {
        return (this.frameIndex != null) ? this.frameIndex.getFrames().size() : 0;
    }

    public int getIntroFrameCount() {
        return (this.frameIndex != null) ? this.frameIndex.getIntroFrames().size() : 0;
    }

    public boolean hasIntroFrames() {
        return this.getIntroFrameCount() > 0;
    }

    @Nullable
    public AfmaMetadata getMetadata() {
        return this.metadata;
    }

    @Nullable
    public AfmaFrameIndex getFrameIndex() {
        return this.frameIndex;
    }

    @Nullable
    public AfmaFrameDescriptor getFrame(int index) {
        if (this.frameIndex == null) return null;
        List<AfmaFrameDescriptor> frames = this.frameIndex.getFrames();
        if (index < 0 || index >= frames.size()) return null;
        return frames.get(index);
    }

    @Nullable
    public AfmaFrameDescriptor getIntroFrame(int index) {
        if (this.frameIndex == null) return null;
        List<AfmaFrameDescriptor> frames = this.frameIndex.getIntroFrames();
        if (index < 0 || index >= frames.size()) return null;
        return frames.get(index);
    }

    @NotNull
    public InputStream openPayload(@NotNull String path) throws IOException {
        PayloadBytes payloadBytes = this.readPayloadBytes(path);
        return new ByteArrayInputStream(payloadBytes.bytes(), payloadBytes.offset(), payloadBytes.length());
    }

    @NotNull
    public PayloadBytes readPayloadBytes(@NotNull String path) throws IOException {
        Objects.requireNonNull(this.zipFile);
        AfmaChunkedPayloadHelper.PayloadLocator payloadLocator = this.findPayloadLocator(path);
        if (payloadLocator == null) {
            throw new FileNotFoundException("AFMA payload file not found: " + path);
        }

        if (AfmaChunkedPayloadHelper.isWholeChunkPayload(payloadLocator, this.payloadChunkLengths)) {
            byte[] payloadBytes = (payloadLocator.length() > AfmaChunkedPayloadHelper.TARGET_CHUNK_BYTES)
                    ? this.readWholeChunk(payloadLocator.chunkId())
                    : this.loadPayloadChunk(payloadLocator.chunkId());
            return new PayloadBytes(payloadBytes, 0, payloadLocator.length());
        }

        byte[] chunkBytes = this.loadPayloadChunk(payloadLocator.chunkId());
        int endOffset = payloadLocator.offset() + payloadLocator.length();
        if (endOffset > chunkBytes.length) {
            throw new IOException("AFMA payload range exceeds its chunk bounds: " + path);
        }
        return new PayloadBytes(chunkBytes, payloadLocator.offset(), payloadLocator.length());
    }

    @Nullable
    protected ZipArchiveEntry findEntry(@NotNull String path) {
        return this.entriesByNormalizedPath.get(normalizeEntryPath(path).toLowerCase(Locale.ROOT));
    }

    @Nullable
    protected AfmaChunkedPayloadHelper.PayloadLocator findPayloadLocator(@NotNull String path) {
        return this.payloadLocatorsByNormalizedPath.get(normalizeEntryPath(path).toLowerCase(Locale.ROOT));
    }

    protected boolean hasPayload(@NotNull String path) {
        AfmaChunkedPayloadHelper.PayloadLocator payloadLocator = this.findPayloadLocator(path);
        return (payloadLocator != null) && (this.findEntry(AfmaChunkedPayloadHelper.chunkEntryPath(payloadLocator.chunkId())) != null);
    }

    @NotNull
    protected byte[] loadPayloadChunk(int chunkId) throws IOException {
        synchronized (this.payloadChunkCacheLock) {
            LoadedPayloadChunk cachedChunk = this.loadedPayloadChunk;
            if ((cachedChunk != null) && (cachedChunk.chunkId() == chunkId)) {
                return cachedChunk.bytes();
            }
        }

        byte[] chunkBytes = this.readWholeChunk(chunkId);
        synchronized (this.payloadChunkCacheLock) {
            LoadedPayloadChunk cachedChunk = this.loadedPayloadChunk;
            if ((cachedChunk != null) && (cachedChunk.chunkId() == chunkId)) {
                return cachedChunk.bytes();
            }
            this.loadedPayloadChunk = new LoadedPayloadChunk(chunkId, chunkBytes);
            return chunkBytes;
        }
    }

    @NotNull
    protected byte[] readWholeChunk(int chunkId) throws IOException {
        Objects.requireNonNull(this.zipFile);
        ZipArchiveEntry chunkEntry = this.findEntry(AfmaChunkedPayloadHelper.chunkEntryPath(chunkId));
        if (chunkEntry == null) {
            throw new FileNotFoundException("AFMA payload chunk not found: " + AfmaChunkedPayloadHelper.chunkEntryPath(chunkId));
        }
        try (InputStream chunkInput = this.zipFile.getInputStream(chunkEntry)) {
            byte[] chunkBytes = chunkInput.readAllBytes();
            int expectedLength = ((chunkId >= 0) && (chunkId < this.payloadChunkLengths.length)) ? this.payloadChunkLengths[chunkId] : -1;
            if ((expectedLength >= 0) && (chunkBytes.length != expectedLength)) {
                throw new IOException("AFMA payload chunk size does not match the payload index: " + chunkEntry.getName());
            }
            return chunkBytes;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    @NotNull
    public static String normalizeEntryPath(@NotNull String entryPath) {
        String normalized = entryPath.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        return normalized;
    }

    @Override
    public void close() throws IOException {
        this.metadata = null;
        this.frameIndex = null;
        this.entriesByNormalizedPath.clear();
        this.payloadLocatorsByNormalizedPath.clear();
        this.payloadChunkLengths = new int[0];
        this.loadedPayloadChunk = null;

        if (this.zipFile != null) {
            try {
                this.zipFile.close();
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to close AFMA ZipFile", ex);
            }
            this.zipFile = null;
        }

        File tempArchive = this.tempArchiveFile;
        boolean shouldDeleteTempArchive = this.deleteTempArchiveOnClose;
        this.tempArchiveFile = null;
        this.deleteTempArchiveOnClose = false;

        if (shouldDeleteTempArchive && (tempArchive != null) && tempArchive.exists() && !tempArchive.delete()) {
            LOGGER.warn("[FANCYMENU] Failed to delete temporary AFMA archive: {}", tempArchive.getAbsolutePath());
        }
    }

    public record PayloadBytes(@NotNull byte[] bytes, int offset, int length) {
    }

    protected record LoadedPayloadChunk(int chunkId, @NotNull byte[] bytes) {
    }

}
