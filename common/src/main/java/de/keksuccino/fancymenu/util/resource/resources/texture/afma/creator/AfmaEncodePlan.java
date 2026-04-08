package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameIndex;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameOperationType;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaMetadata;
import org.jetbrains.annotations.NotNull;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class AfmaEncodePlan {

    @NotNull
    protected final AfmaMetadata metadata;
    @NotNull
    protected final AfmaFrameIndex frameIndex;
    @NotNull
    protected final LinkedHashMap<String, byte[]> payloads;
    protected final long totalPayloadBytes;

    public AfmaEncodePlan(@NotNull AfmaMetadata metadata, @NotNull AfmaFrameIndex frameIndex, @NotNull LinkedHashMap<String, byte[]> payloads) {
        this(metadata, frameIndex, payloads, calculateTotalPayloadBytes(Objects.requireNonNull(payloads)), true);
    }

    public AfmaEncodePlan(@NotNull AfmaMetadata metadata, @NotNull AfmaFrameIndex frameIndex, @NotNull LinkedHashMap<String, byte[]> payloads, long totalPayloadBytes) {
        this(metadata, frameIndex, payloads, totalPayloadBytes, true);
    }

    protected AfmaEncodePlan(@NotNull AfmaMetadata metadata, @NotNull AfmaFrameIndex frameIndex,
                             @NotNull LinkedHashMap<String, byte[]> payloads, long totalPayloadBytes, boolean copyPayloads) {
        this.metadata = Objects.requireNonNull(metadata);
        this.frameIndex = Objects.requireNonNull(frameIndex);
        this.payloads = copyPayloads ? new LinkedHashMap<>(Objects.requireNonNull(payloads)) : Objects.requireNonNull(payloads);
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
    public LinkedHashMap<String, byte[]> getPayloads() {
        return this.payloads;
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

    private static long calculateTotalPayloadBytes(@NotNull Map<String, byte[]> payloads) {
        long total = 0L;
        for (byte[] payload : payloads.values()) {
            if (payload != null) {
                total += payload.length;
            }
        }
        return total;
    }

}
