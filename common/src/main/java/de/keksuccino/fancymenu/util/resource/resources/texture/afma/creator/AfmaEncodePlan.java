package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaChunkedPayloadHelper;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaDecoder;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameIndex;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameOperationType;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaMetadata;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaStoredPayload;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class AfmaEncodePlan implements AutoCloseable {

    @NotNull
    protected final AfmaMetadata metadata;
    @NotNull
    protected final AfmaFrameIndex frameIndex;
    @NotNull
    protected final LinkedHashMap<String, AfmaStoredPayload> payloads;
    @NotNull
    protected final AfmaChunkedPayloadHelper.PackedPayloadArchive payloadArchive;
    protected final long totalPayloadBytes;

    public AfmaEncodePlan(@NotNull AfmaMetadata metadata, @NotNull AfmaFrameIndex frameIndex, @NotNull LinkedHashMap<String, AfmaStoredPayload> payloads) {
        this(metadata, frameIndex, payloads, calculateTotalPayloadBytes(Objects.requireNonNull(payloads)), true);
    }

    public AfmaEncodePlan(@NotNull AfmaMetadata metadata, @NotNull AfmaFrameIndex frameIndex, @NotNull LinkedHashMap<String, AfmaStoredPayload> payloads, long totalPayloadBytes) {
        this(metadata, frameIndex, payloads, totalPayloadBytes, true);
    }

    protected AfmaEncodePlan(@NotNull AfmaMetadata metadata, @NotNull AfmaFrameIndex frameIndex,
                             @NotNull LinkedHashMap<String, AfmaStoredPayload> payloads, long totalPayloadBytes, boolean copyPayloads) {
        this.metadata = Objects.requireNonNull(metadata);
        this.frameIndex = Objects.requireNonNull(frameIndex);
        this.payloads = copyPayloads ? new LinkedHashMap<>(Objects.requireNonNull(payloads)) : Objects.requireNonNull(payloads);
        this.payloadArchive = buildPayloadArchive(this.payloads, this.frameIndex, this.metadata);
        this.totalPayloadBytes = Math.max(0L, totalPayloadBytes);
    }

    @NotNull
    public AfmaMetadata getMetadata() {
        return this.metadata;
    }

    @NotNull
    public AfmaFrameIndex getFrameIndex() {
        return this.frameIndex;
    }

    @NotNull
    public LinkedHashMap<String, AfmaStoredPayload> getPayloads() {
        return new LinkedHashMap<>(this.payloads);
    }

    @NotNull
    public AfmaChunkedPayloadHelper.PackedPayloadArchive getPayloadArchive() {
        return this.payloadArchive;
    }

    public long getTotalPayloadBytes() {
        return this.totalPayloadBytes;
    }

    public int countFrames(@NotNull AfmaFrameOperationType type) {
        int count = 0;
        count += countFrames(type, this.frameIndex.getFrames());
        count += countFrames(type, this.frameIndex.getIntroFrames());
        return count;
    }

    private static int countFrames(@NotNull AfmaFrameOperationType type, @NotNull java.util.List<de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameDescriptor> frames) {
        int count = 0;
        for (var frame : frames) {
            if (frame != null && type == frame.getType()) {
                count++;
            }
        }
        return count;
    }

    private static long calculateTotalPayloadBytes(@NotNull Map<String, AfmaStoredPayload> payloads) {
        long total = 0L;
        for (AfmaStoredPayload payload : payloads.values()) {
            if (payload != null) {
                total += payload.length();
            }
        }
        return total;
    }

    @NotNull
    private static AfmaChunkedPayloadHelper.PackedPayloadArchive buildPayloadArchive(@NotNull Map<String, AfmaStoredPayload> payloads,
                                                                                     @NotNull AfmaFrameIndex frameIndex,
                                                                                     @NotNull AfmaMetadata metadata) {
        LinkedHashMap<String, AfmaStoredPayload> chunkedPayloads = new LinkedHashMap<>();
        for (Map.Entry<String, AfmaStoredPayload> entry : payloads.entrySet()) {
            String normalizedPath = AfmaDecoder.normalizeEntryPath(entry.getKey());
            if ("thumbnail.bin".equalsIgnoreCase(normalizedPath)) {
                continue;
            }
            chunkedPayloads.put(entry.getKey(), entry.getValue());
        }
        return AfmaChunkedPayloadHelper.buildArchiveLayout(chunkedPayloads, AfmaChunkedPayloadHelper.buildPackingHints(frameIndex, metadata.getLoopCount()));
    }

    @Override
    public void close() {
        for (AfmaStoredPayload payload : this.payloads.values()) {
            if (payload != null) {
                payload.close();
            }
        }
    }

}
