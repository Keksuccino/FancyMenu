package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaBinaryFrameIndexHelper;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaChunkedPayloadHelper;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaContainerV2;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaIoHelper;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaPayloadArchiveLayout;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaStoredPayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.zip.Deflater;

public class AfmaArchiveWriter {

    private static final Gson GSON = new GsonBuilder().create();
    protected static final int DEFLATE_LEVEL = 9;
    protected static final int IO_BUFFER_BYTES = 8192;

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

        LinkedHashMap<String, AfmaStoredPayload> payloads = plan.getPayloads();
        HashMap<String, AfmaStoredPayload> payloadsByNormalizedPath = buildNormalizedPayloadMap(payloads);
        AfmaPayloadArchiveLayout payloadArchive = plan.getPayloadArchive();

        byte[] metadataBytes = GSON.toJson(plan.getMetadata()).getBytes(StandardCharsets.UTF_8);
        byte[] frameIndexBytes = AfmaBinaryFrameIndexHelper.encodeFrameIndex(plan.getFrameIndex(), payloadArchive.payloadIdsByPath());
        byte[] payloadTableBytes = payloadArchive.encodePayloadTable();

        int totalEntries = payloadArchive.chunkPlans().size() + 3;
        int writtenEntries = 0;

        checkCancelled(cancellationRequested);
        writtenEntries++;
        reportProgress(progressListener, "metadata.json", writtenEntries, totalEntries);
        checkCancelled(cancellationRequested);
        writtenEntries++;
        reportProgress(progressListener, AfmaBinaryFrameIndexHelper.FRAME_INDEX_ENTRY_PATH, writtenEntries, totalEntries);
        checkCancelled(cancellationRequested);
        writtenEntries++;
        reportProgress(progressListener, "payload_table.bin", writtenEntries, totalEntries);

        File blobTempFile = File.createTempFile("afma_v2_blobs_", ".tmp", parent);
        ArrayList<RelativeChunkDescriptor> relativeChunkDescriptors = new ArrayList<>(payloadArchive.chunkPlans().size());
        try {
            long blobOffset = 0L;
            try (BufferedOutputStream blobOut = new BufferedOutputStream(new FileOutputStream(blobTempFile))) {
                for (AfmaPayloadArchiveLayout.ChunkPlan chunkPlan : payloadArchive.chunkPlans()) {
                    checkCancelled(cancellationRequested);
                    ChunkBlob chunkBlob = this.buildChunkBlob(chunkPlan, payloadsByNormalizedPath, cancellationRequested);
                    blobOut.write(chunkBlob.bytes());
                    relativeChunkDescriptors.add(new RelativeChunkDescriptor(
                            blobOffset,
                            chunkBlob.bytes().length,
                            chunkPlan.uncompressedLength(),
                            chunkBlob.compressionMode()
                    ));
                    blobOffset += chunkBlob.bytes().length;
                    writtenEntries++;
                    reportProgress(progressListener, AfmaChunkedPayloadHelper.chunkEntryPath(chunkPlan.chunkId()), writtenEntries, totalEntries);
                }
                blobOut.flush();
            }

            long chunkDataOffset = AfmaContainerV2.HEADER_BYTES
                    + metadataBytes.length
                    + frameIndexBytes.length
                    + payloadTableBytes.length
                    + ((long) relativeChunkDescriptors.size() * (long) AfmaContainerV2.CHUNK_DESCRIPTOR_BYTES);

            ArrayList<AfmaContainerV2.ChunkDescriptor> chunkDescriptors = new ArrayList<>(relativeChunkDescriptors.size());
            for (RelativeChunkDescriptor relativeDescriptor : relativeChunkDescriptors) {
                chunkDescriptors.add(new AfmaContainerV2.ChunkDescriptor(
                        chunkDataOffset + relativeDescriptor.relativeOffset(),
                        relativeDescriptor.compressedLength(),
                        relativeDescriptor.uncompressedLength(),
                        relativeDescriptor.compressionMode()
                ));
            }

            checkCancelled(cancellationRequested);
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
                 InputStream blobIn = new BufferedInputStream(new FileInputStream(blobTempFile))) {
                AfmaContainerV2.writeHeader(out, new AfmaContainerV2.Header(
                        metadataBytes.length,
                        frameIndexBytes.length,
                        payloadTableBytes.length,
                        chunkDescriptors.size()
                ));
                out.write(metadataBytes);
                out.write(frameIndexBytes);
                out.write(payloadTableBytes);
                for (AfmaContainerV2.ChunkDescriptor chunkDescriptor : chunkDescriptors) {
                    AfmaContainerV2.writeChunkDescriptor(out, chunkDescriptor);
                }
                out.flush();
                blobIn.transferTo(out);
                out.flush();
            }
        } finally {
            if (blobTempFile.exists()) {
                blobTempFile.delete();
            }
        }
    }

    @NotNull
    protected static HashMap<String, AfmaStoredPayload> buildNormalizedPayloadMap(@NotNull Map<String, AfmaStoredPayload> payloads) {
        HashMap<String, AfmaStoredPayload> payloadsByNormalizedPath = new HashMap<>(payloads.size());
        for (Map.Entry<String, AfmaStoredPayload> entry : payloads.entrySet()) {
            payloadsByNormalizedPath.putIfAbsent(normalizePath(entry.getKey()), entry.getValue());
        }
        return payloadsByNormalizedPath;
    }

    @NotNull
    protected ChunkBlob buildChunkBlob(@NotNull AfmaPayloadArchiveLayout.ChunkPlan chunkPlan,
                                       @NotNull Map<String, AfmaStoredPayload> payloadsByNormalizedPath,
                                       @Nullable BooleanSupplier cancellationRequested) throws IOException {
        byte[] rawChunkBytes = this.materializeChunkBytes(chunkPlan, payloadsByNormalizedPath, cancellationRequested);
        if (rawChunkBytes.length <= 0) {
            return new ChunkBlob(rawChunkBytes, AfmaContainerV2.COMPRESSION_STORED);
        }

        Deflater deflater = new Deflater(DEFLATE_LEVEL, true);
        byte[] deflateBuffer = new byte[IO_BUFFER_BYTES];
        try {
            ByteArrayOutputStream compressedOut = new ByteArrayOutputStream(Math.max(32, Math.min(rawChunkBytes.length, AfmaPayloadArchiveLayout.TARGET_CHUNK_BYTES)));
            deflater.setInput(rawChunkBytes);
            deflater.finish();
            while (!deflater.finished()) {
                drainDeflater(compressedOut, deflater, deflateBuffer);
            }
            byte[] compressedChunkBytes = compressedOut.toByteArray();
            if (compressedChunkBytes.length >= rawChunkBytes.length) {
                return new ChunkBlob(rawChunkBytes, AfmaContainerV2.COMPRESSION_STORED);
            }
            return new ChunkBlob(compressedChunkBytes, AfmaContainerV2.COMPRESSION_RAW_DEFLATE);
        } finally {
            deflater.end();
        }
    }

    @NotNull
    protected byte[] materializeChunkBytes(@NotNull AfmaPayloadArchiveLayout.ChunkPlan chunkPlan,
                                           @NotNull Map<String, AfmaStoredPayload> payloadsByNormalizedPath,
                                           @Nullable BooleanSupplier cancellationRequested) throws IOException {
        ByteArrayOutputStream rawOut = new ByteArrayOutputStream(Math.max(32, chunkPlan.uncompressedLength()));
        byte[] readBuffer = new byte[IO_BUFFER_BYTES];
        for (String payloadPath : chunkPlan.payloadPaths()) {
            checkCancelled(cancellationRequested);
            AfmaStoredPayload payload = Objects.requireNonNull(payloadsByNormalizedPath.get(normalizePath(payloadPath)),
                    "AFMA payload was NULL for " + payloadPath);
            try (InputStream in = payload.openStream()) {
                int read;
                while ((read = in.read(readBuffer)) >= 0) {
                    if (read <= 0) {
                        continue;
                    }
                    rawOut.write(readBuffer, 0, read);
                }
            }
        }
        byte[] rawChunkBytes = rawOut.toByteArray();
        if (rawChunkBytes.length != chunkPlan.uncompressedLength()) {
            throw new IOException("AFMA v2 chunk length mismatch while materializing " + AfmaChunkedPayloadHelper.chunkEntryPath(chunkPlan.chunkId()));
        }
        return rawChunkBytes;
    }

    protected static int drainDeflater(@NotNull OutputStream out, @NotNull Deflater deflater, @NotNull byte[] deflateBuffer) throws IOException {
        int writtenBytes = 0;
        while (true) {
            int compressed = deflater.deflate(deflateBuffer);
            if (compressed > 0) {
                out.write(deflateBuffer, 0, compressed);
                writtenBytes += compressed;
                continue;
            }
            if (deflater.needsInput() || deflater.finished()) {
                return writtenBytes;
            }
            throw new IOException("AFMA v2 payload deflater stalled unexpectedly");
        }
    }

    @NotNull
    protected static String normalizePath(@NotNull String path) {
        return AfmaIoHelper.normalizeEntryPath(path).toLowerCase(Locale.ROOT);
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

    protected record RelativeChunkDescriptor(long relativeOffset, int compressedLength, int uncompressedLength, int compressionMode) {
    }

    protected record ChunkBlob(@NotNull byte[] bytes, int compressionMode) {
    }

    @FunctionalInterface
    public interface ProgressListener {
        void update(@NotNull String path, double progress);
    }

}
