package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaCopyRect;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaMultiCopy;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaRect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AfmaRectCopyDetector {

    protected static final double MIN_CANDIDATE_SCORE = 0.20D;
    protected static final int SMALL_OFFSET_PROBE = 4;
    protected static final int NEIGHBOR_OFFSET_RADIUS = 2;
    protected static final int MAX_MULTI_COPY_RECTS = 4;
    protected static final int MAX_MULTI_COPY_MOTION_VECTORS = 12;
    protected static final int MIN_MULTI_COPY_RECT_AREA = 64;
    protected static final int MIN_MULTI_COPY_DIRTY_PIXELS = 48;
    protected static final double MIN_MULTI_COPY_DIRTY_DENSITY = 0.35D;

    protected final int maxSearchDistance;
    protected final int maxCandidateAxisOffsets;

    public AfmaRectCopyDetector(int maxSearchDistance, int maxCandidateAxisOffsets) {
        this.maxSearchDistance = maxSearchDistance;
        this.maxCandidateAxisOffsets = maxCandidateAxisOffsets;
    }

    @Nullable
    public Detection detect(@NotNull AfmaPixelFrame previous, @NotNull AfmaPixelFrame next) {
        return this.detect(new AfmaFramePairAnalysis(previous, next));
    }

    @Nullable
    public Detection detect(@NotNull AfmaFramePairAnalysis pairAnalysis) {
        AfmaPixelFrame previous = pairAnalysis.previousFrame();
        AfmaPixelFrame next = pairAnalysis.nextFrame();

        int width = previous.getWidth();
        int height = previous.getHeight();
        int maxDx = Math.min(width - 1, this.maxSearchDistance);
        int maxDy = Math.min(height - 1, this.maxSearchDistance);

        List<Integer> candidateDx = this.collectAxisCandidates(previous, next, true, maxDx);
        List<Integer> candidateDy = this.collectAxisCandidates(previous, next, false, maxDy);

        Detection bestDetection = null;
        for (int dx : candidateDx) {
            for (int dy : candidateDy) {
                if (dx == 0 && dy == 0) continue;

                int overlapWidth = width - Math.abs(dx);
                int overlapHeight = height - Math.abs(dy);
                if (overlapWidth <= 0 || overlapHeight <= 0) continue;

                AfmaCopyRect copyRect = new AfmaCopyRect(
                        Math.max(0, -dx),
                        Math.max(0, -dy),
                        Math.max(0, dx),
                        Math.max(0, dy),
                        overlapWidth,
                        overlapHeight
                );

                AfmaRect patchBounds = pairAnalysis.findDirtyBoundsAfterCopy(copyRect);
                long patchArea = (patchBounds != null) ? patchBounds.area() : 0L;
                long copyArea = copyRect.getArea();
                long usefulness = copyArea - patchArea;
                if (usefulness <= 0L) continue;

                Detection detection = new Detection(copyRect, patchBounds, usefulness);
                if ((bestDetection == null) || (detection.usefulness() > bestDetection.usefulness())
                        || ((detection.usefulness() == bestDetection.usefulness()) && (patchArea < bestDetection.patchArea()))) {
                    bestDetection = detection;
                }
            }
        }

        return bestDetection;
    }

    @Nullable
    public MultiDetection detectMulti(@NotNull AfmaPixelFrame previous, @NotNull AfmaPixelFrame next) {
        return this.detectMulti(new AfmaFramePairAnalysis(previous, next));
    }

    @Nullable
    public MultiDetection detectMulti(@NotNull AfmaFramePairAnalysis pairAnalysis) {
        AfmaPixelFrame previous = pairAnalysis.previousFrame();
        AfmaPixelFrame next = pairAnalysis.nextFrame();

        AfmaRect initialDirtyBounds = pairAnalysis.differenceBounds();
        if (initialDirtyBounds == null) {
            return null;
        }

        int width = previous.getWidth();
        int height = previous.getHeight();
        int[] predictedPixels = previous.copyPixels();
        ArrayList<AfmaCopyRect> copyRects = new ArrayList<>();
        long totalDirtyCoverage = 0L;

        for (int copyIndex = 0; copyIndex < MAX_MULTI_COPY_RECTS; copyIndex++) {
            CopyRectCandidate nextCandidate = this.findBestMultiCopyRect(predictedPixels, width, height, next);
            if (nextCandidate == null) {
                break;
            }

            AfmaPixelFrameHelper.applyCopyRect(predictedPixels, width, nextCandidate.copyRect());
            copyRects.add(nextCandidate.copyRect());
            totalDirtyCoverage += nextCandidate.dirtyPixels();
        }

        if (copyRects.size() < 2) {
            return null;
        }

        AfmaPixelFrame predictedFrame = new AfmaPixelFrame(width, height, predictedPixels);
        AfmaRect patchBounds = new AfmaFramePairAnalysis(predictedFrame, next).differenceBounds();
        long remainingPatchArea = (patchBounds != null) ? patchBounds.area() : 0L;
        long patchReduction = initialDirtyBounds.area() - remainingPatchArea;
        if (patchReduction <= 0L) {
            return null;
        }

        return new MultiDetection(new AfmaMultiCopy(copyRects), patchBounds, patchReduction + totalDirtyCoverage);
    }

    @NotNull
    public List<MotionVector> collectMotionVectors(@NotNull AfmaPixelFrame previous, @NotNull AfmaPixelFrame next, boolean includeZeroVector) {
        return this.collectMotionVectors(new AfmaFramePairAnalysis(previous, next), includeZeroVector);
    }

    @NotNull
    public List<MotionVector> collectMotionVectors(@NotNull AfmaFramePairAnalysis pairAnalysis, boolean includeZeroVector) {
        AfmaPixelFrame previous = pairAnalysis.previousFrame();
        AfmaPixelFrame next = pairAnalysis.nextFrame();

        int width = previous.getWidth();
        int height = previous.getHeight();
        int maxDx = Math.min(width - 1, this.maxSearchDistance);
        int maxDy = Math.min(height - 1, this.maxSearchDistance);
        List<Integer> candidateDx = this.collectAxisCandidates(previous, next, true, maxDx);
        List<Integer> candidateDy = this.collectAxisCandidates(previous, next, false, maxDy);

        Set<MotionVector> vectors = new LinkedHashSet<>();
        if (includeZeroVector) {
            vectors.add(new MotionVector(0, 0));
        }
        for (int dx : candidateDx) {
            for (int dy : candidateDy) {
                if (!includeZeroVector && dx == 0 && dy == 0) {
                    continue;
                }
                if ((dx == 0) && (dy == 0)) {
                    continue;
                }

                int overlapWidth = width - Math.abs(dx);
                int overlapHeight = height - Math.abs(dy);
                if (overlapWidth <= 0 || overlapHeight <= 0) {
                    continue;
                }
                vectors.add(new MotionVector(dx, dy));
            }
        }

        List<MotionVector> sortedVectors = new ArrayList<>(vectors);
        sortedVectors.sort((first, second) -> Double.compare(
                this.scoreMotionVector(previous, next, second.dx(), second.dy()),
                this.scoreMotionVector(previous, next, first.dx(), first.dy())
        ));
        return List.copyOf(sortedVectors);
    }

    @NotNull
    protected List<Integer> collectAxisCandidates(@NotNull AfmaPixelFrame previous, @NotNull AfmaPixelFrame next, boolean horizontal, int maxOffset) {
        Set<Integer> offsets = new LinkedHashSet<>();
        offsets.add(0);
        if (maxOffset <= 0) {
            return List.copyOf(offsets);
        }
        for (int offset = 1; offset <= Math.min(SMALL_OFFSET_PROBE, maxOffset); offset++) {
            offsets.add(offset);
            offsets.add(-offset);
        }

        List<AxisCandidate> candidates = new ArrayList<>();
        for (int offset = -maxOffset; offset <= maxOffset; offset++) {
            if (offset == 0) continue;
            double score = this.scoreOffset(previous, next, horizontal, offset);
            candidates.add(new AxisCandidate(offset, score));
        }

        candidates.sort(Comparator.comparingDouble(AxisCandidate::score).reversed());
        int acceptedCandidates = 0;
        for (AxisCandidate candidate : candidates) {
            if ((candidate.score() <= MIN_CANDIDATE_SCORE) && (acceptedCandidates >= this.maxCandidateAxisOffsets)) continue;
            offsets.add(candidate.offset());
            for (int neighborOffset = 1; neighborOffset <= NEIGHBOR_OFFSET_RADIUS; neighborOffset++) {
                int negativeNeighbor = candidate.offset() - neighborOffset;
                if (negativeNeighbor >= -maxOffset) {
                    offsets.add(negativeNeighbor);
                }
                int positiveNeighbor = candidate.offset() + neighborOffset;
                if (positiveNeighbor <= maxOffset) {
                    offsets.add(positiveNeighbor);
                }
            }
            acceptedCandidates++;
            if (acceptedCandidates >= this.maxCandidateAxisOffsets) {
                break;
            }
        }

        return List.copyOf(offsets);
    }

    protected double scoreOffset(@NotNull AfmaPixelFrame previous, @NotNull AfmaPixelFrame next, boolean horizontal, int offset) {
        int width = previous.getWidth();
        int height = previous.getHeight();
        int overlapWidth = horizontal ? width - Math.abs(offset) : width;
        int overlapHeight = horizontal ? height : height - Math.abs(offset);
        if (overlapWidth <= 0 || overlapHeight <= 0) return 0D;

        int srcX = horizontal ? Math.max(0, -offset) : 0;
        int dstX = horizontal ? Math.max(0, offset) : 0;
        int srcY = horizontal ? 0 : Math.max(0, -offset);
        int dstY = horizontal ? 0 : Math.max(0, offset);

        double fullScore = this.sampleMatchRatio(previous, next, srcX, srcY, dstX, dstY, overlapWidth, overlapHeight);
        if (horizontal) {
            int bandHeight = Math.max(1, overlapHeight / 2);
            int bandY = srcY + Math.max(0, (overlapHeight - bandHeight) / 2);
            int dstBandY = dstY + Math.max(0, (overlapHeight - bandHeight) / 2);
            return Math.max(fullScore, this.sampleMatchRatio(previous, next, srcX, bandY, dstX, dstBandY, overlapWidth, bandHeight));
        }

        int bandWidth = Math.max(1, overlapWidth / 2);
        int bandX = srcX + Math.max(0, (overlapWidth - bandWidth) / 2);
        int dstBandX = dstX + Math.max(0, (overlapWidth - bandWidth) / 2);
        return Math.max(fullScore, this.sampleMatchRatio(previous, next, bandX, srcY, dstBandX, dstY, bandWidth, overlapHeight));
    }

    protected double sampleMatchRatio(@NotNull AfmaPixelFrame previous, @NotNull AfmaPixelFrame next,
                                      int srcX, int srcY, int dstX, int dstY, int sampleWidth, int sampleHeight) {
        int frameWidth = previous.getWidth();
        int[] previousPixels = previous.getPixelsUnsafe();
        int[] nextPixels = next.getPixelsUnsafe();
        int stepX = Math.max(1, sampleWidth / 48);
        int stepY = Math.max(1, sampleHeight / 16);
        int matches = 0;
        int samples = 0;

        for (int y = 0; y < sampleHeight; y += stepY) {
            int previousRowOffset = ((srcY + y) * frameWidth) + srcX;
            int nextRowOffset = ((dstY + y) * frameWidth) + dstX;
            for (int x = 0; x < sampleWidth; x += stepX) {
                samples++;
                if (previousPixels[previousRowOffset + x] == nextPixels[nextRowOffset + x]) {
                    matches++;
                }
            }
        }

        return (samples > 0) ? ((double)matches / samples) : 0D;
    }

    protected double scoreMotionVector(@NotNull AfmaPixelFrame previous, @NotNull AfmaPixelFrame next, int dx, int dy) {
        int overlapWidth = previous.getWidth() - Math.abs(dx);
        int overlapHeight = previous.getHeight() - Math.abs(dy);
        if (overlapWidth <= 0 || overlapHeight <= 0) {
            return 0D;
        }
        return this.sampleMatchRatio(
                previous,
                next,
                Math.max(0, -dx),
                Math.max(0, -dy),
                Math.max(0, dx),
                Math.max(0, dy),
                overlapWidth,
                overlapHeight
        );
    }

    @Nullable
    protected CopyRectCandidate findBestMultiCopyRect(@NotNull int[] predictedPixels, int width, int height, @NotNull AfmaPixelFrame next) {
        AfmaPixelFrame predictedFrame = new AfmaPixelFrame(width, height, predictedPixels);
        List<MotionVector> motionVectors = this.collectMotionVectors(predictedFrame, next, false);
        if (motionVectors.isEmpty()) {
            return null;
        }

        int[] nextPixels = next.getPixelsUnsafe();
        CopyRectCandidate bestCandidate = null;
        int maxMotionVectors = Math.min(MAX_MULTI_COPY_MOTION_VECTORS, motionVectors.size());
        for (int motionIndex = 0; motionIndex < maxMotionVectors; motionIndex++) {
            MotionVector motionVector = motionVectors.get(motionIndex);
            CopyRectCandidate candidate = this.findBestMultiCopyRectForMotion(predictedPixels, nextPixels, width, height,
                    motionVector.dx(), motionVector.dy());
            if ((candidate != null) && ((bestCandidate == null) || candidate.isBetterThan(bestCandidate))) {
                bestCandidate = candidate;
            }
        }
        return bestCandidate;
    }

    @Nullable
    protected CopyRectCandidate findBestMultiCopyRectForMotion(@NotNull int[] predictedPixels, @NotNull int[] nextPixels,
                                                               int width, int height, int dx, int dy) {
        int overlapWidth = width - Math.abs(dx);
        int overlapHeight = height - Math.abs(dy);
        if ((overlapWidth <= 0) || (overlapHeight <= 0)) {
            return null;
        }

        int srcBaseX = Math.max(0, -dx);
        int srcBaseY = Math.max(0, -dy);
        int dstBaseX = Math.max(0, dx);
        int dstBaseY = Math.max(0, dy);
        LinkedHashMap<Long, RectAccumulator> activeRects = new LinkedHashMap<>();
        CopyRectCandidate bestCandidate = null;

        for (int localY = 0; localY < overlapHeight; localY++) {
            int srcRowOffset = ((srcBaseY + localY) * width) + srcBaseX;
            int dstRowOffset = ((dstBaseY + localY) * width) + dstBaseX;
            ArrayList<RowRun> rowRuns = new ArrayList<>();
            int runStartX = -1;
            int runDirtyPixels = 0;

            for (int localX = 0; localX < overlapWidth; localX++) {
                boolean motionMatch = predictedPixels[srcRowOffset + localX] == nextPixels[dstRowOffset + localX];
                if (!motionMatch) {
                    if (runStartX >= 0) {
                        rowRuns.add(new RowRun(runStartX, localX, runDirtyPixels));
                        runStartX = -1;
                        runDirtyPixels = 0;
                    }
                    continue;
                }

                if (runStartX < 0) {
                    runStartX = localX;
                    runDirtyPixels = 0;
                }
                if (predictedPixels[dstRowOffset + localX] != nextPixels[dstRowOffset + localX]) {
                    runDirtyPixels++;
                }
            }
            if (runStartX >= 0) {
                rowRuns.add(new RowRun(runStartX, overlapWidth, runDirtyPixels));
            }

            LinkedHashMap<Long, RectAccumulator> nextActiveRects = new LinkedHashMap<>();
            for (RowRun rowRun : rowRuns) {
                long spanKey = spanKey(rowRun.startX(), rowRun.endXExclusive());
                RectAccumulator continuedRect = activeRects.remove(spanKey);
                RectAccumulator nextRect = (continuedRect != null)
                        ? continuedRect.extend(rowRun.dirtyPixels())
                        : new RectAccumulator(rowRun.startX(), localY, rowRun.width(), 1, rowRun.dirtyPixels());
                nextActiveRects.put(spanKey, nextRect);
            }

            for (RectAccumulator completedRect : activeRects.values()) {
                bestCandidate = this.selectBetterMultiCopyCandidate(bestCandidate, completedRect,
                        srcBaseX, srcBaseY, dstBaseX, dstBaseY, dx, dy);
            }
            activeRects = nextActiveRects;
        }

        for (RectAccumulator completedRect : activeRects.values()) {
            bestCandidate = this.selectBetterMultiCopyCandidate(bestCandidate, completedRect,
                    srcBaseX, srcBaseY, dstBaseX, dstBaseY, dx, dy);
        }
        return bestCandidate;
    }

    @Nullable
    protected CopyRectCandidate selectBetterMultiCopyCandidate(@Nullable CopyRectCandidate bestCandidate,
                                                               @NotNull RectAccumulator rectAccumulator,
                                                               int srcBaseX, int srcBaseY, int dstBaseX, int dstBaseY,
                                                               int dx, int dy) {
        long rectArea = rectAccumulator.area();
        long dirtyPixels = rectAccumulator.dirtyPixels();
        if ((rectArea < MIN_MULTI_COPY_RECT_AREA) || (dirtyPixels < MIN_MULTI_COPY_DIRTY_PIXELS)) {
            return bestCandidate;
        }
        if (((double) dirtyPixels / (double) rectArea) < MIN_MULTI_COPY_DIRTY_DENSITY) {
            return bestCandidate;
        }

        AfmaCopyRect copyRect = new AfmaCopyRect(
                srcBaseX + rectAccumulator.startX(),
                srcBaseY + rectAccumulator.startY(),
                dstBaseX + rectAccumulator.startX(),
                dstBaseY + rectAccumulator.startY(),
                rectAccumulator.width(),
                rectAccumulator.height()
        );
        CopyRectCandidate candidate = new CopyRectCandidate(copyRect, dirtyPixels, rectArea, dx, dy);
        if ((bestCandidate == null) || candidate.isBetterThan(bestCandidate)) {
            return candidate;
        }
        return bestCandidate;
    }

    protected static long spanKey(int startX, int endXExclusive) {
        return (((long) startX) << 32) | (endXExclusive & 0xFFFFFFFFL);
    }

    protected record AxisCandidate(int offset, double score) {
    }

    public record Detection(@NotNull AfmaCopyRect copyRect, @Nullable AfmaRect patchBounds, long usefulness) {
        public long patchArea() {
            return (this.patchBounds != null) ? this.patchBounds.area() : 0L;
        }
    }

    public record MotionVector(int dx, int dy) {
    }

    public record MultiDetection(@NotNull AfmaMultiCopy multiCopy, @Nullable AfmaRect patchBounds, long usefulness) {
        public long patchArea() {
            return (this.patchBounds != null) ? this.patchBounds.area() : 0L;
        }
    }

    protected record RowRun(int startX, int endXExclusive, int dirtyPixels) {
        public int width() {
            return this.endXExclusive - this.startX;
        }
    }

    protected record RectAccumulator(int startX, int startY, int width, int height, long dirtyPixels) {
        public RectAccumulator extend(int additionalDirtyPixels) {
            return new RectAccumulator(this.startX, this.startY, this.width, this.height + 1, this.dirtyPixels + additionalDirtyPixels);
        }

        public long area() {
            return (long) this.width * this.height;
        }
    }

    protected record CopyRectCandidate(@NotNull AfmaCopyRect copyRect, long dirtyPixels, long area, int dx, int dy) {
        public boolean isBetterThan(@NotNull CopyRectCandidate other) {
            if (this.dirtyPixels != other.dirtyPixels) {
                return this.dirtyPixels > other.dirtyPixels;
            }
            if (this.area != other.area) {
                return this.area > other.area;
            }

            int motionMagnitude = Math.abs(this.dx) + Math.abs(this.dy);
            int otherMotionMagnitude = Math.abs(other.dx) + Math.abs(other.dy);
            if (motionMagnitude != otherMotionMagnitude) {
                return motionMagnitude < otherMotionMagnitude;
            }

            if (this.copyRect.getDstY() != other.copyRect.getDstY()) {
                return this.copyRect.getDstY() < other.copyRect.getDstY();
            }
            return this.copyRect.getDstX() < other.copyRect.getDstX();
        }
    }

}
