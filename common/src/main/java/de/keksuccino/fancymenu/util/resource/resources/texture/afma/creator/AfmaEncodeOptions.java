package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class AfmaEncodeOptions {

    public static final int DEFAULT_NEAR_LOSSLESS_MAX_CHANNEL_DELTA = 2;
    public static final int DEFAULT_MAX_COPY_SEARCH_DISTANCE = 512;
    public static final int DEFAULT_MAX_CANDIDATE_AXIS_OFFSETS = 5;

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
    protected int maxCopySearchDistance = DEFAULT_MAX_COPY_SEARCH_DISTANCE;
    protected int maxCandidateAxisOffsets = DEFAULT_MAX_CANDIDATE_AXIS_OFFSETS;
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
    protected int plannerSearchWindowFrames = 12;
    protected int plannerBeamWidth = 8;
    protected long plannerDecodeCostPenaltyBytes = 160L;
    protected long plannerComplexityPenaltyBytes = 16L;
    protected double plannerAverageDriftPenaltyBytes = 48D;
    protected long plannerVisibleColorDriftPenaltyBytes = 12L;
    protected long plannerAlphaDriftPenaltyBytes = 12L;
    protected long plannerLossyContinuationPenaltyBytes = 96L;
    protected long plannerKeyframeDistancePenaltyBytes = 160L;
    protected double plannerMaxCumulativeAverageError = 0D;
    protected int plannerMaxCumulativeVisibleColorDelta = 0;
    protected int plannerMaxCumulativeAlphaDelta = 0;
    protected int plannerMaxConsecutiveLossyFrames = 0;

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

    public int getPlannerSearchWindowFrames() {
        return this.plannerSearchWindowFrames;
    }

    public AfmaEncodeOptions setPlannerSearchWindowFrames(int plannerSearchWindowFrames) {
        this.plannerSearchWindowFrames = plannerSearchWindowFrames;
        return this;
    }

    public int getPlannerBeamWidth() {
        return this.plannerBeamWidth;
    }

    public AfmaEncodeOptions setPlannerBeamWidth(int plannerBeamWidth) {
        this.plannerBeamWidth = plannerBeamWidth;
        return this;
    }

    public long getPlannerDecodeCostPenaltyBytes() {
        return this.plannerDecodeCostPenaltyBytes;
    }

    public AfmaEncodeOptions setPlannerDecodeCostPenaltyBytes(long plannerDecodeCostPenaltyBytes) {
        this.plannerDecodeCostPenaltyBytes = plannerDecodeCostPenaltyBytes;
        return this;
    }

    public long getPlannerComplexityPenaltyBytes() {
        return this.plannerComplexityPenaltyBytes;
    }

    public AfmaEncodeOptions setPlannerComplexityPenaltyBytes(long plannerComplexityPenaltyBytes) {
        this.plannerComplexityPenaltyBytes = plannerComplexityPenaltyBytes;
        return this;
    }

    public double getPlannerAverageDriftPenaltyBytes() {
        return this.plannerAverageDriftPenaltyBytes;
    }

    public AfmaEncodeOptions setPlannerAverageDriftPenaltyBytes(double plannerAverageDriftPenaltyBytes) {
        this.plannerAverageDriftPenaltyBytes = plannerAverageDriftPenaltyBytes;
        return this;
    }

    public long getPlannerVisibleColorDriftPenaltyBytes() {
        return this.plannerVisibleColorDriftPenaltyBytes;
    }

    public AfmaEncodeOptions setPlannerVisibleColorDriftPenaltyBytes(long plannerVisibleColorDriftPenaltyBytes) {
        this.plannerVisibleColorDriftPenaltyBytes = plannerVisibleColorDriftPenaltyBytes;
        return this;
    }

    public long getPlannerAlphaDriftPenaltyBytes() {
        return this.plannerAlphaDriftPenaltyBytes;
    }

    public AfmaEncodeOptions setPlannerAlphaDriftPenaltyBytes(long plannerAlphaDriftPenaltyBytes) {
        this.plannerAlphaDriftPenaltyBytes = plannerAlphaDriftPenaltyBytes;
        return this;
    }

    public long getPlannerLossyContinuationPenaltyBytes() {
        return this.plannerLossyContinuationPenaltyBytes;
    }

    public AfmaEncodeOptions setPlannerLossyContinuationPenaltyBytes(long plannerLossyContinuationPenaltyBytes) {
        this.plannerLossyContinuationPenaltyBytes = plannerLossyContinuationPenaltyBytes;
        return this;
    }

    public long getPlannerKeyframeDistancePenaltyBytes() {
        return this.plannerKeyframeDistancePenaltyBytes;
    }

    public AfmaEncodeOptions setPlannerKeyframeDistancePenaltyBytes(long plannerKeyframeDistancePenaltyBytes) {
        this.plannerKeyframeDistancePenaltyBytes = plannerKeyframeDistancePenaltyBytes;
        return this;
    }

    public double getPlannerMaxCumulativeAverageError() {
        return this.plannerMaxCumulativeAverageError;
    }

    public AfmaEncodeOptions setPlannerMaxCumulativeAverageError(double plannerMaxCumulativeAverageError) {
        this.plannerMaxCumulativeAverageError = plannerMaxCumulativeAverageError;
        return this;
    }

    public int getPlannerMaxCumulativeVisibleColorDelta() {
        return this.plannerMaxCumulativeVisibleColorDelta;
    }

    public AfmaEncodeOptions setPlannerMaxCumulativeVisibleColorDelta(int plannerMaxCumulativeVisibleColorDelta) {
        this.plannerMaxCumulativeVisibleColorDelta = plannerMaxCumulativeVisibleColorDelta;
        return this;
    }

    public int getPlannerMaxCumulativeAlphaDelta() {
        return this.plannerMaxCumulativeAlphaDelta;
    }

    public AfmaEncodeOptions setPlannerMaxCumulativeAlphaDelta(int plannerMaxCumulativeAlphaDelta) {
        this.plannerMaxCumulativeAlphaDelta = plannerMaxCumulativeAlphaDelta;
        return this;
    }

    public int getPlannerMaxConsecutiveLossyFrames() {
        return this.plannerMaxConsecutiveLossyFrames;
    }

    public AfmaEncodeOptions setPlannerMaxConsecutiveLossyFrames(int plannerMaxConsecutiveLossyFrames) {
        this.plannerMaxConsecutiveLossyFrames = plannerMaxConsecutiveLossyFrames;
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
        if (this.plannerSearchWindowFrames <= 0) {
            throw new IllegalArgumentException("AFMA planner search window must be greater than 0");
        }
        if (this.plannerBeamWidth <= 0) {
            throw new IllegalArgumentException("AFMA planner beam width must be greater than 0");
        }
        if (this.plannerDecodeCostPenaltyBytes < 0L
                || this.plannerComplexityPenaltyBytes < 0L
                || this.plannerVisibleColorDriftPenaltyBytes < 0L
                || this.plannerAlphaDriftPenaltyBytes < 0L
                || this.plannerLossyContinuationPenaltyBytes < 0L
                || this.plannerKeyframeDistancePenaltyBytes < 0L) {
            throw new IllegalArgumentException("AFMA planner penalty values cannot be negative");
        }
        if (this.plannerAverageDriftPenaltyBytes < 0D) {
            throw new IllegalArgumentException("AFMA planner drift penalty cannot be negative");
        }
        if (this.plannerMaxCumulativeAverageError < 0D
                || this.plannerMaxCumulativeVisibleColorDelta < 0
                || this.plannerMaxCumulativeAlphaDelta < 0
                || this.plannerMaxConsecutiveLossyFrames < 0) {
            throw new IllegalArgumentException("AFMA planner cumulative drift limits cannot be negative");
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
