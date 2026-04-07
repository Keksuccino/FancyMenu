package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class AfmaEncodeOptions {

    public static final int DEFAULT_NEAR_LOSSLESS_MAX_CHANNEL_DELTA = 2;

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
    protected double maxDeltaAreaRatioWithoutStrongSavings = 0.94D;
    protected double maxCopyPatchAreaRatioWithoutStrongSavings = 0.90D;
    protected long minComplexCandidateSavingsBytes = 24L * 1024L;
    protected long minStrongComplexCandidateSavingsBytes = 96L * 1024L;
    protected double minComplexCandidateSavingsRatio = 0.015D;
    protected double minStrongComplexCandidateSavingsRatio = 0.06D;
    protected int nearLosslessMaxChannelDelta = 0;
    protected boolean adaptiveKeyframePlacement = false;
    protected int adaptiveMaxKeyframeInterval = AfmaMetadata.DEFAULT_KEYFRAME_INTERVAL;
    protected long adaptiveContinuationMinSavingsBytes = 512L;
    protected double adaptiveContinuationMinSavingsRatio = 0.005D;
    protected int perceptualBinIntraMaxVisibleColorDelta = 0;
    protected int perceptualBinIntraMaxAlphaDelta = 0;
    protected double perceptualBinIntraMaxAverageError = 0D;

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
        if (this.adaptiveMaxKeyframeInterval < keyframeInterval) {
            this.adaptiveMaxKeyframeInterval = keyframeInterval;
        }
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

    public double getMaxDeltaAreaRatioWithoutStrongSavings() {
        return this.maxDeltaAreaRatioWithoutStrongSavings;
    }

    public double getMaxCopyPatchAreaRatioWithoutStrongSavings() {
        return this.maxCopyPatchAreaRatioWithoutStrongSavings;
    }

    public long getMinComplexCandidateSavingsBytes() {
        return this.minComplexCandidateSavingsBytes;
    }

    public long getMinStrongComplexCandidateSavingsBytes() {
        return this.minStrongComplexCandidateSavingsBytes;
    }

    public double getMinComplexCandidateSavingsRatio() {
        return this.minComplexCandidateSavingsRatio;
    }

    public double getMinStrongComplexCandidateSavingsRatio() {
        return this.minStrongComplexCandidateSavingsRatio;
    }

    public int getNearLosslessMaxChannelDelta() {
        return this.nearLosslessMaxChannelDelta;
    }

    public boolean isNearLosslessEnabled() {
        return this.nearLosslessMaxChannelDelta > 0;
    }

    public AfmaEncodeOptions setNearLosslessMaxChannelDelta(int nearLosslessMaxChannelDelta) {
        this.nearLosslessMaxChannelDelta = nearLosslessMaxChannelDelta;
        return this;
    }

    public boolean isAdaptiveKeyframePlacementEnabled() {
        return this.adaptiveKeyframePlacement;
    }

    public AfmaEncodeOptions setAdaptiveKeyframePlacement(boolean adaptiveKeyframePlacement) {
        this.adaptiveKeyframePlacement = adaptiveKeyframePlacement;
        return this;
    }

    public int getAdaptiveMaxKeyframeInterval() {
        return this.adaptiveMaxKeyframeInterval;
    }

    public AfmaEncodeOptions setAdaptiveMaxKeyframeInterval(int adaptiveMaxKeyframeInterval) {
        this.adaptiveMaxKeyframeInterval = adaptiveMaxKeyframeInterval;
        return this;
    }

    public long getAdaptiveContinuationMinSavingsBytes() {
        return this.adaptiveContinuationMinSavingsBytes;
    }

    public AfmaEncodeOptions setAdaptiveContinuationMinSavingsBytes(long adaptiveContinuationMinSavingsBytes) {
        this.adaptiveContinuationMinSavingsBytes = adaptiveContinuationMinSavingsBytes;
        return this;
    }

    public double getAdaptiveContinuationMinSavingsRatio() {
        return this.adaptiveContinuationMinSavingsRatio;
    }

    public AfmaEncodeOptions setAdaptiveContinuationMinSavingsRatio(double adaptiveContinuationMinSavingsRatio) {
        this.adaptiveContinuationMinSavingsRatio = adaptiveContinuationMinSavingsRatio;
        return this;
    }

    public int getPerceptualBinIntraMaxVisibleColorDelta() {
        return this.perceptualBinIntraMaxVisibleColorDelta;
    }

    public AfmaEncodeOptions setPerceptualBinIntraMaxVisibleColorDelta(int perceptualBinIntraMaxVisibleColorDelta) {
        this.perceptualBinIntraMaxVisibleColorDelta = perceptualBinIntraMaxVisibleColorDelta;
        return this;
    }

    public int getPerceptualBinIntraMaxAlphaDelta() {
        return this.perceptualBinIntraMaxAlphaDelta;
    }

    public AfmaEncodeOptions setPerceptualBinIntraMaxAlphaDelta(int perceptualBinIntraMaxAlphaDelta) {
        this.perceptualBinIntraMaxAlphaDelta = perceptualBinIntraMaxAlphaDelta;
        return this;
    }

    public double getPerceptualBinIntraMaxAverageError() {
        return this.perceptualBinIntraMaxAverageError;
    }

    public boolean isPerceptualBinIntraEnabled() {
        return this.perceptualBinIntraMaxAverageError > 0D
                && this.perceptualBinIntraMaxVisibleColorDelta > 0
                && this.perceptualBinIntraMaxAlphaDelta >= 0;
    }

    public AfmaEncodeOptions setPerceptualBinIntraMaxAverageError(double perceptualBinIntraMaxAverageError) {
        this.perceptualBinIntraMaxAverageError = perceptualBinIntraMaxAverageError;
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
        if (this.adaptiveMaxKeyframeInterval <= 0 || this.adaptiveMaxKeyframeInterval < this.keyframeInterval) {
            throw new IllegalArgumentException("AFMA adaptive keyframe interval must be greater than or equal to the preferred keyframe interval");
        }
        if (this.maxCopySearchDistance < 0) {
            throw new IllegalArgumentException("AFMA copy search distance cannot be negative");
        }
        if (this.maxCandidateAxisOffsets <= 0) {
            throw new IllegalArgumentException("AFMA candidate axis count must be greater than 0");
        }
        if (this.maxDeltaAreaRatioWithoutStrongSavings <= 0D || this.maxDeltaAreaRatioWithoutStrongSavings > 1D) {
            throw new IllegalArgumentException("AFMA delta area ratio must stay within (0, 1]");
        }
        if (this.maxCopyPatchAreaRatioWithoutStrongSavings <= 0D || this.maxCopyPatchAreaRatioWithoutStrongSavings > 1D) {
            throw new IllegalArgumentException("AFMA copy patch area ratio must stay within (0, 1]");
        }
        if (this.minComplexCandidateSavingsBytes < 0L || this.minStrongComplexCandidateSavingsBytes < 0L) {
            throw new IllegalArgumentException("AFMA candidate savings thresholds cannot be negative");
        }
        if (this.minComplexCandidateSavingsRatio < 0D || this.minStrongComplexCandidateSavingsRatio < 0D) {
            throw new IllegalArgumentException("AFMA candidate savings ratios cannot be negative");
        }
        if (this.nearLosslessMaxChannelDelta < 0 || this.nearLosslessMaxChannelDelta > 255) {
            throw new IllegalArgumentException("AFMA near-lossless channel delta must stay within [0, 255]");
        }
        if (this.adaptiveContinuationMinSavingsBytes < 0L || this.adaptiveContinuationMinSavingsRatio < 0D) {
            throw new IllegalArgumentException("AFMA adaptive GOP continuation thresholds cannot be negative");
        }
        if (this.perceptualBinIntraMaxVisibleColorDelta < 0 || this.perceptualBinIntraMaxVisibleColorDelta > 255
                || this.perceptualBinIntraMaxAlphaDelta < 0 || this.perceptualBinIntraMaxAlphaDelta > 255) {
            throw new IllegalArgumentException("AFMA perceptual BIN_INTRA deltas must stay within [0, 255]");
        }
        if (this.perceptualBinIntraMaxAverageError < 0D) {
            throw new IllegalArgumentException("AFMA perceptual BIN_INTRA average error cannot be negative");
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
