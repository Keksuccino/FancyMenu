package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameIndex;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameOperationType;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    public AfmaEncodePlan(@NotNull AfmaMetadata metadata, @NotNull AfmaFrameIndex frameIndex, @NotNull LinkedHashMap<String, byte[]> payloads) {
        this.metadata = Objects.requireNonNull(metadata);
        this.frameIndex = Objects.requireNonNull(frameIndex);
        this.payloads = new LinkedHashMap<>(Objects.requireNonNull(payloads));
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
        long total = 0L;
        for (byte[] payload : this.payloads.values()) {
            total += payload.length;
        }
        return total;
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

}
