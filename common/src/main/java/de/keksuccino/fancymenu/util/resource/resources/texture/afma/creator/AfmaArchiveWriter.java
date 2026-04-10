package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaBinaryFrameIndexHelper;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaChunkedPayloadHelper;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaDecoder;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaStoredPayload;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.UnixStat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

public class AfmaArchiveWriter {

    private static final Gson GSON = new GsonBuilder().create();
    private static final FileTime ZIP_EPOCH = FileTime.fromMillis(0L);

    public void write(@NotNull AfmaEncodePlan plan, @NotNull File outputFile) throws IOException {
        this.write(plan, outputFile, null);
    }

    public void write(@NotNull AfmaEncodePlan plan, @NotNull File outputFile, @Nullable BooleanSupplier cancellationRequested) throws IOException {
        this.write(plan, outputFile, cancellationRequested, null);
    }

    public void write(@NotNull AfmaEncodePlan plan, @NotNull File outputFile, @Nullable BooleanSupplier cancellationRequested,
                      @Nullable ProgressListener progressListener) throws IOException {
        Objects.requireNonNull(plan);
        Objects.requireNonNull(outputFile);

        File parent = outputFile.getParentFile();
        if ((parent != null) && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Failed to create AFMA output directory: " + parent.getAbsolutePath());
        }

        try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)))) {
            out.setEncoding(StandardCharsets.UTF_8.name());
            out.setLevel(9);
            LinkedHashMap<String, AfmaStoredPayload> chunkedPayloads = new LinkedHashMap<>();
            AfmaStoredPayload thumbnailPayload = null;
            for (Map.Entry<String, AfmaStoredPayload> entry : plan.getPayloads().entrySet()) {
                String normalizedPath = AfmaDecoder.normalizeEntryPath(entry.getKey());
                if ("thumbnail.bin".equalsIgnoreCase(normalizedPath)) {
                    thumbnailPayload = entry.getValue();
                    continue;
                }
                chunkedPayloads.put(entry.getKey(), entry.getValue());
            }

            AfmaChunkedPayloadHelper.PackedPayloadArchive payloadArchive = plan.getPayloadArchive();
            byte[] frameIndexBytes = AfmaBinaryFrameIndexHelper.encodeFrameIndex(plan.getFrameIndex(), payloadArchive.payloadIdsByPath());
            byte[] payloadIndexBytes = AfmaChunkedPayloadHelper.encodePayloadIndex(payloadArchive);

            int totalEntries = payloadArchive.chunkPlans().size() + 3 + ((thumbnailPayload != null) ? 1 : 0);
            int writtenEntries = 0;

            checkCancelled(cancellationRequested);
            this.writeJsonEntry(out, "metadata.json", GSON.toJson(plan.getMetadata()));
            writtenEntries++;
            reportProgress(progressListener, "metadata.json", writtenEntries, totalEntries);
            checkCancelled(cancellationRequested);
            this.writeBinaryEntry(out, AfmaBinaryFrameIndexHelper.FRAME_INDEX_ENTRY_PATH, frameIndexBytes);
            writtenEntries++;
            reportProgress(progressListener, AfmaBinaryFrameIndexHelper.FRAME_INDEX_ENTRY_PATH, writtenEntries, totalEntries);
            checkCancelled(cancellationRequested);
            this.writeBinaryEntry(out, AfmaChunkedPayloadHelper.PAYLOAD_INDEX_ENTRY_PATH, payloadIndexBytes);
            writtenEntries++;
            reportProgress(progressListener, AfmaChunkedPayloadHelper.PAYLOAD_INDEX_ENTRY_PATH, writtenEntries, totalEntries);
            for (AfmaChunkedPayloadHelper.ChunkPlan chunkPlan : payloadArchive.chunkPlans()) {
                checkCancelled(cancellationRequested);
                this.writePayloadChunkEntry(out, chunkPlan, chunkedPayloads, cancellationRequested);
                writtenEntries++;
                reportProgress(progressListener, chunkPlan.entryPath(), writtenEntries, totalEntries);
            }
            if (thumbnailPayload != null) {
                checkCancelled(cancellationRequested);
                this.writeBinaryEntry(out, "thumbnail.bin", thumbnailPayload);
                writtenEntries++;
                reportProgress(progressListener, "thumbnail.bin", writtenEntries, totalEntries);
            }
            out.finish();
        }
    }

    protected void writeJsonEntry(@NotNull ZipArchiveOutputStream out, @NotNull String path, @NotNull String json) throws IOException {
        this.writeBinaryEntry(out, path, json.getBytes(StandardCharsets.UTF_8));
    }

    protected void writeBinaryEntry(@NotNull ZipArchiveOutputStream out, @NotNull String path, @NotNull byte[] bytes) throws IOException {
        ZipArchiveEntry entry = new ZipArchiveEntry(path);
        entry.setSize(bytes.length);
        entry.setTime(0L);
        entry.setCreationTime(ZIP_EPOCH);
        entry.setLastModifiedTime(ZIP_EPOCH);
        entry.setLastAccessTime(ZIP_EPOCH);
        entry.setUnixMode(UnixStat.FILE_FLAG | 0644);
        out.putArchiveEntry(entry);
        out.write(bytes);
        out.closeArchiveEntry();
    }

    protected void writeBinaryEntry(@NotNull ZipArchiveOutputStream out, @NotNull String path, @NotNull AfmaStoredPayload payload) throws IOException {
        ZipArchiveEntry entry = new ZipArchiveEntry(path);
        entry.setSize(payload.length());
        entry.setTime(0L);
        entry.setCreationTime(ZIP_EPOCH);
        entry.setLastModifiedTime(ZIP_EPOCH);
        entry.setLastAccessTime(ZIP_EPOCH);
        entry.setUnixMode(UnixStat.FILE_FLAG | 0644);
        out.putArchiveEntry(entry);
        try {
            payload.writeTo(out);
        } finally {
            out.closeArchiveEntry();
        }
    }

    protected void writePayloadChunkEntry(@NotNull ZipArchiveOutputStream out, @NotNull AfmaChunkedPayloadHelper.ChunkPlan chunkPlan,
                                          @NotNull Map<String, AfmaStoredPayload> payloads, @Nullable BooleanSupplier cancellationRequested) throws IOException {
        ZipArchiveEntry entry = new ZipArchiveEntry(chunkPlan.entryPath());
        entry.setSize(chunkPlan.uncompressedLength());
        entry.setTime(0L);
        entry.setCreationTime(ZIP_EPOCH);
        entry.setLastModifiedTime(ZIP_EPOCH);
        entry.setLastAccessTime(ZIP_EPOCH);
        entry.setUnixMode(UnixStat.FILE_FLAG | 0644);
        out.putArchiveEntry(entry);
        try {
            for (String payloadPath : chunkPlan.payloadPaths()) {
                checkCancelled(cancellationRequested);
                AfmaStoredPayload payload = Objects.requireNonNull(payloads.get(payloadPath), "AFMA payload was NULL for " + payloadPath);
                payload.writeTo(out);
            }
        } finally {
            out.closeArchiveEntry();
        }
    }

    protected static void checkCancelled(@Nullable BooleanSupplier cancellationRequested) {
        if ((cancellationRequested != null) && cancellationRequested.getAsBoolean()) {
            throw new CancellationException("AFMA archive writing was cancelled");
        }
    }

    protected static void reportProgress(@Nullable ProgressListener progressListener, @NotNull String path, int writtenEntries, int totalEntries) {
        if (progressListener != null) {
            progressListener.update(path, (double) writtenEntries / Math.max(1, totalEntries));
        }
    }

    @FunctionalInterface
    public interface ProgressListener {
        void update(@NotNull String path, double progress);
    }

}
