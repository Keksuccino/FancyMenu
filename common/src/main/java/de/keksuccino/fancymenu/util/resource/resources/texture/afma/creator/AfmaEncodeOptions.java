package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class AfmaEncodeOptions {

    protected int loopCount = 0;
    protected long frameTimeMs = 41L;
    protected long introFrameTimeMs = 41L;
    @NotNull
    protected Map<Integer, Long> customFrameTimes = new LinkedHashMap<>();
    @NotNull
    protected Map<Integer, Long> customIntroFrameTimes = new LinkedHashMap<>();
    protected int keyframeInterval = AfmaMetadata.DEFAULT_KEYFRAME_INTERVAL;
    protected boolean rectCopyEnabled = true;
    protected boolean duplicateFrameElision = true;
    protected int maxCopySearchDistance = AfmaOptimizationPreset.BALANCED.getMaxCopySearchDistance();
    protected int maxCandidateAxisOffsets = AfmaOptimizationPreset.BALANCED.getMaxCandidateAxisOffsets();

    @NotNull
    public static AfmaEncodeOptions balanced() {
        return new AfmaEncodeOptions();
    }

    public int getLoopCount() {
        return this.loopCount;
    }

    public AfmaEncodeOptions setLoopCount(int loopCount) {
        this.loopCount = loopCount;
        return this;
    }

    public long getFrameTimeMs() {
        return this.frameTimeMs;
    }

    public AfmaEncodeOptions setFrameTimeMs(long frameTimeMs) {
        this.frameTimeMs = frameTimeMs;
        return this;
    }

    public long getIntroFrameTimeMs() {
        return this.introFrameTimeMs;
    }

    public AfmaEncodeOptions setIntroFrameTimeMs(long introFrameTimeMs) {
        this.introFrameTimeMs = introFrameTimeMs;
        return this;
    }

    @NotNull
    public Map<Integer, Long> getCustomFrameTimes() {
        return this.customFrameTimes;
    }

    public AfmaEncodeOptions setCustomFrameTimes(@Nullable Map<Integer, Long> customFrameTimes) {
        this.customFrameTimes = (customFrameTimes != null) ? new LinkedHashMap<>(customFrameTimes) : new LinkedHashMap<>();
        return this;
    }

    @NotNull
    public Map<Integer, Long> getCustomIntroFrameTimes() {
        return this.customIntroFrameTimes;
    }

    public AfmaEncodeOptions setCustomIntroFrameTimes(@Nullable Map<Integer, Long> customIntroFrameTimes) {
        this.customIntroFrameTimes = (customIntroFrameTimes != null) ? new LinkedHashMap<>(customIntroFrameTimes) : new LinkedHashMap<>();
        return this;
    }

    public int getKeyframeInterval() {
        return this.keyframeInterval;
    }

    public AfmaEncodeOptions setKeyframeInterval(int keyframeInterval) {
        this.keyframeInterval = keyframeInterval;
        return this;
    }

    public boolean isRectCopyEnabled() {
        return this.rectCopyEnabled;
    }

    public AfmaEncodeOptions setRectCopyEnabled(boolean rectCopyEnabled) {
        this.rectCopyEnabled = rectCopyEnabled;
        return this;
    }

    public boolean isDuplicateFrameElision() {
        return this.duplicateFrameElision;
    }

    public AfmaEncodeOptions setDuplicateFrameElision(boolean duplicateFrameElision) {
        this.duplicateFrameElision = duplicateFrameElision;
        return this;
    }

    public int getMaxCopySearchDistance() {
        return this.maxCopySearchDistance;
    }

    public AfmaEncodeOptions setMaxCopySearchDistance(int maxCopySearchDistance) {
        this.maxCopySearchDistance = maxCopySearchDistance;
        return this;
    }

    public int getMaxCandidateAxisOffsets() {
        return this.maxCandidateAxisOffsets;
    }

    public AfmaEncodeOptions setMaxCandidateAxisOffsets(int maxCandidateAxisOffsets) {
        this.maxCandidateAxisOffsets = maxCandidateAxisOffsets;
        return this;
    }

    public void validateForCounts(int mainFrameCount, int introFrameCount) {
        if (mainFrameCount <= 0 && introFrameCount <= 0) {
            throw new IllegalArgumentException("AFMA encoding requires at least one main or intro frame");
        }
        if (this.frameTimeMs <= 0L || this.introFrameTimeMs <= 0L) {
            throw new IllegalArgumentException("AFMA frame times must be greater than 0");
        }
        if (this.keyframeInterval <= 0) {
            throw new IllegalArgumentException("AFMA keyframe interval must be greater than 0");
        }
        if (this.maxCopySearchDistance < 0) {
            throw new IllegalArgumentException("AFMA copy search distance cannot be negative");
        }
        if (this.maxCandidateAxisOffsets <= 0) {
            throw new IllegalArgumentException("AFMA candidate axis count must be greater than 0");
        }

        validateCustomFrameTimes(this.customFrameTimes, mainFrameCount, "main");
        validateCustomFrameTimes(this.customIntroFrameTimes, introFrameCount, "intro");
    }

    protected static void validateCustomFrameTimes(@NotNull Map<Integer, Long> frameTimes, int frameCount, @NotNull String sequenceName) {
        for (Map.Entry<Integer, Long> entry : frameTimes.entrySet()) {
            Integer frameIndex = entry.getKey();
            Long delay = entry.getValue();
            if (frameIndex == null || frameIndex < 0 || frameIndex >= frameCount) {
                throw new IllegalArgumentException("Invalid custom " + sequenceName + " frame index: " + frameIndex);
            }
            if (delay == null || delay <= 0L) {
                throw new IllegalArgumentException("Invalid custom " + sequenceName + " frame delay at index " + frameIndex);
            }
        }
    }

}
