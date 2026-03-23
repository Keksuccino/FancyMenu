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
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private static final int PNG_HEADER_PROBE_BYTES = 33;
    private static final byte[] PNG_SIGNATURE = new byte[] {(byte)137, 80, 78, 71, 13, 10, 26, 10};

    @Nullable
    protected ZipFile zipFile = null;
    @Nullable
    protected AfmaMetadata metadata = null;
    @Nullable
    protected AfmaFrameIndex frameIndex = null;
    @NotNull
    protected final Map<String, ZipArchiveEntry> entriesByNormalizedPath = new HashMap<>();
    @NotNull
    protected final Map<String, PngFrameHeader> payloadHeadersByPath = new LinkedHashMap<>();

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

    protected void readFrameIndex() throws IOException {
        Objects.requireNonNull(this.zipFile);
        ZipArchiveEntry frameIndexEntry = this.findEntry("frame_index.json");
        if (frameIndexEntry == null) {
            throw new FileNotFoundException("No frame_index.json found in AFMA file");
        }

        try (InputStream frameIndexIn = this.zipFile.getInputStream(frameIndexEntry)) {
            String frameIndexString = new String(frameIndexIn.readAllBytes(), StandardCharsets.UTF_8);
            if (frameIndexString.trim().isEmpty()) {
                throw new IOException("frame_index.json of AFMA file is empty");
            }

            AfmaFrameIndex parsedFrameIndex = GSON.fromJson(frameIndexString, AfmaFrameIndex.class);
            if (parsedFrameIndex == null) {
                throw new IOException("Unable to parse frame_index.json of AFMA file");
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

        this.payloadHeadersByPath.clear();
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
            if (descriptor.requiresPrimaryPayload() && this.findEntry(Objects.requireNonNull(descriptor.getPrimaryPayloadPath())) == null) {
                throw new IllegalArgumentException("Referenced AFMA payload was not found: " + descriptor.getPrimaryPayloadPath());
            }
            if (descriptor.requiresPatchPayload() && this.findEntry(Objects.requireNonNull(descriptor.getSecondaryPayloadPath())) == null) {
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
            this.validatePayloadHeader(context, Objects.requireNonNull(descriptor.getPrimaryPayloadPath()), activeMetadata.getCanvasWidth(), activeMetadata.getCanvasHeight(), 8);
        } else if (descriptor.getType() == AfmaFrameOperationType.DELTA_RECT) {
            this.validatePayloadHeader(context, Objects.requireNonNull(descriptor.getPrimaryPayloadPath()), descriptor.getWidth(), descriptor.getHeight(), 8);
        } else if (descriptor.getType() == AfmaFrameOperationType.SPARSE_DELTA_RECT) {
            AfmaSparsePayload sparse = Objects.requireNonNull(descriptor.getSparse());
            this.validatePayloadHeader(context, Objects.requireNonNull(descriptor.getPrimaryPayloadPath()), descriptor.getWidth(), descriptor.getHeight(), 1, 8);
            this.validatePayloadHeader(context, Objects.requireNonNull(sparse.getPixelsPath()), sparse.getPackedWidth(), sparse.getPackedHeight(), 8);
        } else if (descriptor.getType() == AfmaFrameOperationType.COPY_RECT_SPARSE_PATCH) {
            AfmaSparsePayload sparse = Objects.requireNonNull(descriptor.getSparse());
            this.validatePayloadHeader(context, Objects.requireNonNull(descriptor.getPrimaryPayloadPath()), descriptor.getWidth(), descriptor.getHeight(), 1, 8);
            this.validatePayloadHeader(context, Objects.requireNonNull(sparse.getPixelsPath()), sparse.getPackedWidth(), sparse.getPackedHeight(), 8);
        } else if ((descriptor.getType() == AfmaFrameOperationType.COPY_RECT_PATCH) && descriptor.requiresPatchPayload()) {
            AfmaPatchRegion patch = Objects.requireNonNull(descriptor.getPatch());
            this.validatePayloadHeader(context, Objects.requireNonNull(descriptor.getSecondaryPayloadPath()), patch.getWidth(), patch.getHeight(), 8);
        }
    }

    protected void validatePayloadHeader(@NotNull String context, @NotNull String path, int expectedWidth, int expectedHeight, int... allowedBitDepths) throws IOException {
        PngFrameHeader header = this.getOrLoadPayloadHeader(path);
        if (header == null) {
            throw new IOException(context + " references a missing payload header: " + path);
        }
        if (header.width != expectedWidth || header.height != expectedHeight) {
            throw new IOException(context + " payload dimensions do not match the descriptor for " + path);
        }
        boolean allowed = (allowedBitDepths == null) || (allowedBitDepths.length == 0);
        if (!allowed) {
            for (int allowedBitDepth : allowedBitDepths) {
                if (header.bitDepth == allowedBitDepth) {
                    allowed = true;
                    break;
                }
            }
        }
        if (!allowed) {
            throw new IOException(context + " payload uses an unsupported PNG bit depth: " + path);
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
                this.validatePayloadHeader(context, Objects.requireNonNull(descriptor.getPrimaryPayloadPath()), activeMetadata.getCanvasWidth(), activeMetadata.getCanvasHeight(), 8);
            } else if (descriptor.getType() == AfmaFrameOperationType.DELTA_RECT) {
                this.validatePayloadHeader(context, Objects.requireNonNull(descriptor.getPrimaryPayloadPath()), descriptor.getWidth(), descriptor.getHeight(), 8);
            } else if (descriptor.getType() == AfmaFrameOperationType.SPARSE_DELTA_RECT) {
                AfmaSparsePayload sparse = Objects.requireNonNull(descriptor.getSparse());
                this.validatePayloadHeader(context, Objects.requireNonNull(descriptor.getPrimaryPayloadPath()), descriptor.getWidth(), descriptor.getHeight(), 1, 8);
                this.validatePayloadHeader(context, Objects.requireNonNull(sparse.getPixelsPath()), sparse.getPackedWidth(), sparse.getPackedHeight(), 8);
            } else if (descriptor.getType() == AfmaFrameOperationType.COPY_RECT_SPARSE_PATCH) {
                AfmaSparsePayload sparse = Objects.requireNonNull(descriptor.getSparse());
                this.validatePayloadHeader(context, Objects.requireNonNull(descriptor.getPrimaryPayloadPath()), descriptor.getWidth(), descriptor.getHeight(), 1, 8);
                this.validatePayloadHeader(context, Objects.requireNonNull(sparse.getPixelsPath()), sparse.getPackedWidth(), sparse.getPackedHeight(), 8);
            } else if ((descriptor.getType() == AfmaFrameOperationType.COPY_RECT_PATCH) && descriptor.requiresPatchPayload()) {
                AfmaPatchRegion patch = Objects.requireNonNull(descriptor.getPatch());
                this.validatePayloadHeader(context, Objects.requireNonNull(descriptor.getSecondaryPayloadPath()), patch.getWidth(), patch.getHeight(), 8);
            }
        }
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

    @Nullable
    public InputStream openPayload(@NotNull String path) throws IOException {
        Objects.requireNonNull(this.zipFile);
        ZipArchiveEntry entry = this.findEntry(path);
        if (entry == null) {
            throw new FileNotFoundException("AFMA payload file not found: " + path);
        }
        if (this.getOrLoadPayloadHeader(path) == null) {
            throw new IOException("AFMA payload is missing a valid PNG header: " + path);
        }
        try {
            return this.zipFile.getInputStream(entry);
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    @Nullable
    public InputStream openThumbnail() throws IOException {
        Objects.requireNonNull(this.zipFile);
        ZipArchiveEntry entry = this.findEntry("thumbnail.png");
        return (entry != null) ? this.zipFile.getInputStream(entry) : null;
    }

    @Nullable
    protected ZipArchiveEntry findEntry(@NotNull String path) {
        return this.entriesByNormalizedPath.get(normalizeEntryPath(path).toLowerCase(Locale.ROOT));
    }

    @Nullable
    protected PngFrameHeader getOrLoadPayloadHeader(@NotNull String framePath) throws IOException {
        String normalizedPath = normalizeEntryPath(framePath).toLowerCase(Locale.ROOT);
        PngFrameHeader cachedHeader = this.payloadHeadersByPath.get(normalizedPath);
        if (cachedHeader != null) {
            return cachedHeader;
        }

        PngFrameHeader header = this.readPngFrameHeader(normalizedPath);
        if (header != null) {
            this.payloadHeadersByPath.put(normalizedPath, header);
        }
        return header;
    }

    @Nullable
    protected PngFrameHeader readPngFrameHeader(@NotNull String framePath) throws IOException {
        Objects.requireNonNull(this.zipFile);

        ZipArchiveEntry frameEntry = this.findEntry(framePath);
        if (frameEntry == null) return null;

        byte[] headerBytes = new byte[PNG_HEADER_PROBE_BYTES];
        int totalRead = 0;
        try (InputStream frameIn = this.zipFile.getInputStream(frameEntry)) {
            while (totalRead < headerBytes.length) {
                int read = frameIn.read(headerBytes, totalRead, headerBytes.length - totalRead);
                if (read < 0) break;
                totalRead += read;
            }
        } catch (Exception ex) {
            throw new IOException(ex);
        }

        if (totalRead < PNG_HEADER_PROBE_BYTES) return null;
        if (!matchesPngSignature(headerBytes)) return null;
        if ((headerBytes[12] != 'I') || (headerBytes[13] != 'H') || (headerBytes[14] != 'D') || (headerBytes[15] != 'R')) return null;

        return new PngFrameHeader(
                readIntBigEndian(headerBytes, 16),
                readIntBigEndian(headerBytes, 20),
                Byte.toUnsignedInt(headerBytes[24])
        );
    }

    protected static boolean matchesPngSignature(@NotNull byte[] headerBytes) {
        if (headerBytes.length < PNG_SIGNATURE.length) return false;
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (headerBytes[i] != PNG_SIGNATURE[i]) return false;
        }
        return true;
    }

    protected static int readIntBigEndian(@NotNull byte[] bytes, int offset) {
        return ((bytes[offset] & 255) << 24)
                | ((bytes[offset + 1] & 255) << 16)
                | ((bytes[offset + 2] & 255) << 8)
                | (bytes[offset + 3] & 255);
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
        this.payloadHeadersByPath.clear();

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

    protected static class PngFrameHeader {
        protected final int width;
        protected final int height;
        protected final int bitDepth;

        protected PngFrameHeader(int width, int height, int bitDepth) {
            this.width = width;
            this.height = height;
            this.bitDepth = bitDepth;
        }
    }

}
