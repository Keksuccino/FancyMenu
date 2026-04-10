package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaCopyRect;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaRect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class AfmaFramePairAnalysis {

    @NotNull
    protected final AfmaPixelFrame previousFrame;
    @NotNull
    protected final AfmaPixelFrame nextFrame;
    protected final int width;
    protected final int height;
    @NotNull
    protected final int[] previousPixels;
    @NotNull
    protected final int[] nextPixels;
    protected boolean differenceBoundsComputed;
    protected boolean identical;
    protected int changedPixelCount;
    @Nullable
    protected AfmaRect differenceBounds;
    @Nullable
    protected Map<CopyRectKey, DirtyBoundsAfterCopyResult> dirtyBoundsAfterCopyByKey;
    @Nullable
    protected Map<MotionSearchKey, AfmaRectCopyDetector.MotionSearchAnalysis> motionSearchAnalysesByKey;
    protected boolean perceptualDriftComputed;
    @NotNull
    protected PerceptualDriftMetrics perceptualDriftMetrics = new PerceptualDriftMetrics(0D, 0, 0);

    public AfmaFramePairAnalysis(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame nextFrame) {
        previousFrame = Objects.requireNonNull(previousFrame);
        nextFrame = Objects.requireNonNull(nextFrame);
        AfmaPixelFrameHelper.ensureSameSize(previousFrame, nextFrame);
        this.previousFrame = previousFrame;
        this.nextFrame = nextFrame;
        this.width = previousFrame.getWidth();
        this.height = previousFrame.getHeight();
        this.previousPixels = previousFrame.getPixelsUnsafe();
        this.nextPixels = nextFrame.getPixelsUnsafe();
    }

    @NotNull
    public AfmaPixelFrame previousFrame() {
        return this.previousFrame;
    }

    @NotNull
    public AfmaPixelFrame nextFrame() {
        return this.nextFrame;
    }

    public boolean isIdentical() {
        this.ensureDifferenceBounds();
        return this.identical;
    }

    public @Nullable AfmaRect differenceBounds() {
        this.ensureDifferenceBounds();
        return this.differenceBounds;
    }

    public int changedPixelCount() {
        this.ensureDifferenceBounds();
        return this.changedPixelCount;
    }

    public @Nullable AfmaRect findDirtyBoundsAfterCopy(@NotNull AfmaCopyRect copyRect) {
        return this.analyzeDirtyAfterCopy(copyRect).bounds();
    }

    @NotNull
    DirtyBoundsAfterCopyResult analyzeDirtyAfterCopy(@NotNull AfmaCopyRect copyRect) {
        CopyRectKey cacheKey = CopyRectKey.of(copyRect);
        if (this.dirtyBoundsAfterCopyByKey == null) {
            this.dirtyBoundsAfterCopyByKey = new LinkedHashMap<>();
        } else {
            DirtyBoundsAfterCopyResult cachedResult = this.dirtyBoundsAfterCopyByKey.get(cacheKey);
            if (cachedResult != null) {
                return cachedResult;
            }
        }

        DirtyBoundsAfterCopyResult dirtyAfterCopy = this.scanDirtyBoundsAfterCopy(copyRect);
        this.dirtyBoundsAfterCopyByKey.put(cacheKey, dirtyAfterCopy);
        return dirtyAfterCopy;
    }

    @Nullable
    AfmaRectCopyDetector.MotionSearchAnalysis getMotionSearchAnalysis(int maxSearchDistance, int maxCandidateAxisOffsets) {
        if (this.motionSearchAnalysesByKey == null) {
            return null;
        }
        return this.motionSearchAnalysesByKey.get(new MotionSearchKey(maxSearchDistance, maxCandidateAxisOffsets));
    }

    void cacheMotionSearchAnalysis(int maxSearchDistance, int maxCandidateAxisOffsets,
                                   @NotNull AfmaRectCopyDetector.MotionSearchAnalysis motionSearchAnalysis) {
        if (this.motionSearchAnalysesByKey == null) {
            this.motionSearchAnalysesByKey = new LinkedHashMap<>();
        }
        this.motionSearchAnalysesByKey.put(new MotionSearchKey(maxSearchDistance, maxCandidateAxisOffsets), motionSearchAnalysis);
    }

    @NotNull
    public PerceptualDriftMetrics perceptualDriftMetrics() {
        if (this.perceptualDriftComputed) {
            return this.perceptualDriftMetrics;
        }

        double totalError = 0D;
        int maxVisibleColorDelta = 0;
        int maxAlphaDelta = 0;
        for (int pixelIndex = 0; pixelIndex < this.previousPixels.length; pixelIndex++) {
            int previousColor = this.previousPixels[pixelIndex];
            int nextColor = this.nextPixels[pixelIndex];
            int previousAlpha = (previousColor >>> 24) & 0xFF;
            int nextAlpha = (nextColor >>> 24) & 0xFF;
            int alphaDelta = Math.abs(previousAlpha - nextAlpha);
            if (alphaDelta > maxAlphaDelta) {
                maxAlphaDelta = alphaDelta;
            }

            int visibilityAlpha = Math.max(previousAlpha, nextAlpha);
            if (visibilityAlpha <= 0) {
                continue;
            }

            int redDelta = channelDifference(previousColor >> 16, nextColor >> 16);
            int greenDelta = channelDifference(previousColor >> 8, nextColor >> 8);
            int blueDelta = channelDifference(previousColor, nextColor);
            int visibleColorDelta = Math.max(redDelta, Math.max(greenDelta, blueDelta));
            if (visibleColorDelta > maxVisibleColorDelta) {
                maxVisibleColorDelta = visibleColorDelta;
            }

            double visibilityWeight = visibilityAlpha / 255.0D;
            totalError += (alphaDelta * 2.0D) + ((redDelta + greenDelta + blueDelta) * visibilityWeight);
        }

        this.perceptualDriftMetrics = new PerceptualDriftMetrics(
                totalError / Math.max(1, this.previousPixels.length),
                maxVisibleColorDelta,
                maxAlphaDelta
        );
        this.perceptualDriftComputed = true;
        return this.perceptualDriftMetrics;
    }

    protected void ensureDifferenceBounds() {
        if (this.differenceBoundsComputed) {
            return;
        }

        int minX = this.width;
        int minY = this.height;
        int maxX = -1;
        int maxY = -1;
        int changedPixelCount = 0;
        for (int y = 0; y < this.height; y++) {
            int rowOffset = y * this.width;
            for (int x = 0; x < this.width; x++) {
                if (this.previousPixels[rowOffset + x] == this.nextPixels[rowOffset + x]) {
                    continue;
                }
                changedPixelCount++;
                if (x < minX) minX = x;
                if (y < minY) minY = y;
                if (x > maxX) maxX = x;
                if (y > maxY) maxY = y;
            }
        }

        this.changedPixelCount = changedPixelCount;
        this.identical = (maxX < minX) || (maxY < minY);
        this.differenceBounds = this.identical ? null : new AfmaRect(minX, minY, (maxX - minX) + 1, (maxY - minY) + 1);
        this.differenceBoundsComputed = true;
    }

    protected @NotNull DirtyBoundsAfterCopyResult scanDirtyBoundsAfterCopy(@NotNull AfmaCopyRect copyRect) {
        int dstLeft = copyRect.getDstX();
        int dstTop = copyRect.getDstY();
        int dstRight = dstLeft + copyRect.getWidth();
        int dstBottom = dstTop + copyRect.getHeight();
        int srcLeft = copyRect.getSrcX();
        int srcTop = copyRect.getSrcY();
        int minX = this.width;
        int minY = this.height;
        int maxX = -1;
        int maxY = -1;
        int dirtyPixelCount = 0;

        for (int y = 0; y < this.height; y++) {
            int rowOffset = y * this.width;
            boolean copiedRow = (y >= dstTop) && (y < dstBottom);
            int copiedSourceRowOffset = copiedRow ? ((srcTop + (y - dstTop)) * this.width) : 0;
            for (int x = 0; x < this.width; x++) {
                int pixelIndex = rowOffset + x;
                int expected = this.previousPixels[pixelIndex];
                if (copiedRow && (x >= dstLeft) && (x < dstRight)) {
                    expected = this.previousPixels[copiedSourceRowOffset + srcLeft + (x - dstLeft)];
                }

                if (expected == this.nextPixels[pixelIndex]) {
                    continue;
                }
                dirtyPixelCount++;
                if (x < minX) minX = x;
                if (y < minY) minY = y;
                if (x > maxX) maxX = x;
                if (y > maxY) maxY = y;
            }
        }

        if ((maxX < minX) || (maxY < minY)) {
            return new DirtyBoundsAfterCopyResult(null, dirtyPixelCount);
        }
        return new DirtyBoundsAfterCopyResult(
                new AfmaRect(minX, minY, (maxX - minX) + 1, (maxY - minY) + 1),
                dirtyPixelCount
        );
    }

    protected static int channelDifference(int first, int second) {
        return Math.abs((first & 0xFF) - (second & 0xFF));
    }

    public record PerceptualDriftMetrics(double averageError, int maxVisibleColorDelta, int maxAlphaDelta) {
    }

    protected record CopyRectKey(int srcX, int srcY, int dstX, int dstY, int width, int height) {

        @NotNull
        public static CopyRectKey of(@NotNull AfmaCopyRect copyRect) {
            return new CopyRectKey(
                    copyRect.getSrcX(),
                    copyRect.getSrcY(),
                    copyRect.getDstX(),
                    copyRect.getDstY(),
                    copyRect.getWidth(),
                    copyRect.getHeight()
            );
        }

    }

    protected record MotionSearchKey(int maxSearchDistance, int maxCandidateAxisOffsets) {
    }

    protected record DirtyBoundsAfterCopyResult(@Nullable AfmaRect bounds, int dirtyPixelCount) {
    }

}
