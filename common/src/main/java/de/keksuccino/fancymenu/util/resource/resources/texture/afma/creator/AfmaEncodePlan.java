package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameDescriptor;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameIndex;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameOperationType;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaMetadata;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaChunkedPayloadHelper;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaPayloadArchiveLayout;
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
    protected final AfmaPayloadArchiveLayout payloadArchive;
    protected final long totalPayloadBytes;

    public AfmaEncodePlan(@NotNull AfmaMetadata metadata, @NotNull AfmaFrameIndex frameIndex, @NotNull LinkedHashMap<String, AfmaStoredPayload> payloads) {
        this(metadata, frameIndex, payloads, AfmaPayloadArchiveLayout.build(payloads, frameIndex, metadata.getLoopCount()));
    }

    public AfmaEncodePlan(@NotNull AfmaMetadata metadata, @NotNull AfmaFrameIndex frameIndex,
                          @NotNull LinkedHashMap<String, AfmaStoredPayload> payloads,
                          @NotNull AfmaPayloadArchiveLayout payloadArchive) {
        this.metadata = Objects.requireNonNull(metadata);
        this.frameIndex = Objects.requireNonNull(frameIndex);
        this.payloads = new LinkedHashMap<>(Objects.requireNonNull(payloads));
        this.payloadArchive = Objects.requireNonNull(payloadArchive);
        this.totalPayloadBytes = calculateTotalPayloadBytes(this.payloads);
    }

    public AfmaEncodePlan(@NotNull AfmaMetadata metadata, @NotNull AfmaFrameIndex frameIndex,
                          @NotNull LinkedHashMap<String, AfmaStoredPayload> payloads,
                          @NotNull AfmaChunkedPayloadHelper.PackedPayloadArchive ignoredLegacyArchive) {
        this(metadata, frameIndex, payloads, AfmaPayloadArchiveLayout.build(payloads, frameIndex, metadata.getLoopCount()));
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
    public AfmaPayloadArchiveLayout getPayloadArchive() {
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

    protected static int countFrames(@NotNull AfmaFrameOperationType type, @NotNull java.util.List<AfmaFrameDescriptor> frames) {
        int count = 0;
        for (AfmaFrameDescriptor frame : frames) {
            if ((frame != null) && (frame.getType() == type)) {
                count++;
            }
        }
        return count;
    }

    protected static long calculateTotalPayloadBytes(@NotNull Map<String, AfmaStoredPayload> payloads) {
        long total = 0L;
        for (AfmaStoredPayload payload : payloads.values()) {
            if (payload != null) {
                total += payload.length();
            }
        }
        return total;
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
