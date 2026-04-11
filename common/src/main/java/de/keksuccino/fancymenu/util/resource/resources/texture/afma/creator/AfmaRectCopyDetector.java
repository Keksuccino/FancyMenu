package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaCopyRect;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaMultiCopy;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaRect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
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
    protected static final double MAX_MULTI_COPY_DIRTY_DENSITY_WITHOUT_SINGLE_EVIDENCE = 0.80D;
    protected static final double MIN_MULTI_COPY_REMAINING_DIRTY_RATIO_AFTER_SINGLE = 0.20D;
    protected static final double MIN_MULTI_COPY_REMAINING_PATCH_RATIO_AFTER_SINGLE = 0.25D;

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
        MotionSearchAnalysis motionSearchAnalysis = this.getMotionSearchAnalysis(pairAnalysis);

        int width = previous.getWidth();
        int height = previous.getHeight();

        Detection bestDetection = null;
        for (int dx : motionSearchAnalysis.candidateDx()) {
            for (int dy : motionSearchAnalysis.candidateDy()) {
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

                var dirtyAfterCopy = pairAnalysis.analyzeDirtyAfterCopy(copyRect);
                AfmaRect patchBounds = dirtyAfterCopy.bounds();
                long patchArea = (patchBounds != null) ? patchBounds.area() : 0L;
                long copyArea = copyRect.getArea();
                long usefulness = copyArea - patchArea;
                if (usefulness <= 0L) continue;

                Detection detection = new Detection(copyRect, patchBounds, usefulness, dirtyAfterCopy.dirtyPixelCount());
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
        return this.detectMulti(pairAnalysis, null);
    }

    @Nullable
    public MultiDetection detectMulti(@NotNull AfmaFramePairAnalysis pairAnalysis, @Nullable Detection singleDetection) {
        AfmaPixelFrame previous = pairAnalysis.previousFrame();
        AfmaPixelFrame next = pairAnalysis.nextFrame();

        AfmaRect initialDirtyBounds = pairAnalysis.differenceBounds();
        if ((initialDirtyBounds == null) || !this.shouldAttemptMultiCopy(pairAnalysis, singleDetection, initialDirtyBounds)) {
            return null;
        }

        long initialDirtyPixelCount = pairAnalysis.changedPixelCount();
        int width = previous.getWidth();
        int height = previous.getHeight();
        int[] predictedPixels = previous.copyPixels();
        ArrayList<AfmaCopyRect> copyRects = new ArrayList<>();
        long totalDirtyCoverage = 0L;
        AfmaFramePairAnalysis motionSearchPairAnalysis = pairAnalysis;

        for (int copyIndex = 0; copyIndex < MAX_MULTI_COPY_RECTS; copyIndex++) {
            CopyRectCandidate nextCandidate = this.findBestMultiCopyRect(motionSearchPairAnalysis, predictedPixels, width, height, next);
            if (nextCandidate == null) {
                break;
            }

            AfmaPixelFrameHelper.applyCopyRect(predictedPixels, width, nextCandidate.copyRect());
            copyRects.add(nextCandidate.copyRect());
            totalDirtyCoverage += nextCandidate.dirtyPixels();
            if ((initialDirtyPixelCount - totalDirtyCoverage) < MIN_MULTI_COPY_DIRTY_PIXELS) {
                break;
            }
            motionSearchPairAnalysis = null;
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

    protected boolean shouldAttemptMultiCopy(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                             @Nullable Detection singleDetection, @NotNull AfmaRect initialDirtyBounds) {
        long initialDirtyPixelCount = pairAnalysis.changedPixelCount();
        if (initialDirtyPixelCount < ((long) MIN_MULTI_COPY_DIRTY_PIXELS * 2L)) {
            return false;
        }

        long dirtyBoundsArea = initialDirtyBounds.area();
        if (dirtyBoundsArea <= 0L) {
            return false;
        }

        if (singleDetection == null) {
            return ((double) initialDirtyPixelCount / (double) dirtyBoundsArea) <= MAX_MULTI_COPY_DIRTY_DENSITY_WITHOUT_SINGLE_EVIDENCE;
        }

        if (singleDetection.patchArea() <= 0L) {
            return false;
        }

        // Skip multi-copy when the best single-copy already collapses almost all remaining work.
        double remainingDirtyRatio = (double) singleDetection.remainingDirtyPixelCount() / (double) initialDirtyPixelCount;
        double remainingPatchRatio = (double) singleDetection.patchArea() / (double) dirtyBoundsArea;
        return (remainingDirtyRatio >= MIN_MULTI_COPY_REMAINING_DIRTY_RATIO_AFTER_SINGLE)
                || (remainingPatchRatio >= MIN_MULTI_COPY_REMAINING_PATCH_RATIO_AFTER_SINGLE);
    }

    @NotNull
    public List<MotionVector> collectMotionVectors(@NotNull AfmaPixelFrame previous, @NotNull AfmaPixelFrame next, boolean includeZeroVector) {
        return this.collectMotionVectors(new AfmaFramePairAnalysis(previous, next), includeZeroVector);
    }

    @NotNull
    public List<MotionVector> collectMotionVectors(@NotNull AfmaFramePairAnalysis pairAnalysis, boolean includeZeroVector) {
        MotionSearchAnalysis motionSearchAnalysis = this.getMotionSearchAnalysis(pairAnalysis);
        this.ensureRankedMotionVectors(pairAnalysis, motionSearchAnalysis);
        return includeZeroVector ? motionSearchAnalysis.rankedMotionVectors() : motionSearchAnalysis.rankedMotionVectorsWithoutZero();
    }

    @NotNull
    protected MotionSearchAnalysis getMotionSearchAnalysis(@NotNull AfmaFramePairAnalysis pairAnalysis) {
        // Reuse the expensive axis scan for detect(), detectMulti(), and block_inter within the same frame pair.
        MotionSearchAnalysis cachedAnalysis = pairAnalysis.getMotionSearchAnalysis(this.maxSearchDistance, this.maxCandidateAxisOffsets);
        if (cachedAnalysis != null) {
            return cachedAnalysis;
        }

        AfmaPixelFrame previous = pairAnalysis.previousFrame();
        AfmaPixelFrame next = pairAnalysis.nextFrame();
        int maxDx = Math.min(previous.getWidth() - 1, this.maxSearchDistance);
        int maxDy = Math.min(previous.getHeight() - 1, this.maxSearchDistance);
        MotionSearchAnalysis analysis = new MotionSearchAnalysis(
                this.collectAxisCandidates(previous, next, true, maxDx),
                this.collectAxisCandidates(previous, next, false, maxDy)
        );
        pairAnalysis.cacheMotionSearchAnalysis(this.maxSearchDistance, this.maxCandidateAxisOffsets, analysis);
        return analysis;
    }

    protected void ensureRankedMotionVectors(@NotNull AfmaFramePairAnalysis pairAnalysis, @NotNull MotionSearchAnalysis motionSearchAnalysis) {
        if (motionSearchAnalysis.hasRankedMotionVectors()) {
            return;
        }

        AfmaPixelFrame previous = pairAnalysis.previousFrame();
        AfmaPixelFrame next = pairAnalysis.nextFrame();
        int width = previous.getWidth();
        int height = previous.getHeight();
        ArrayList<ScoredMotionVector> scoredVectors = new ArrayList<>(
                (motionSearchAnalysis.candidateDx().size() * motionSearchAnalysis.candidateDy().size()) + 1
        );
        scoredVectors.add(new ScoredMotionVector(new MotionVector(0, 0), this.scoreMotionVector(previous, next, 0, 0)));
        for (int dx : motionSearchAnalysis.candidateDx()) {
            for (int dy : motionSearchAnalysis.candidateDy()) {
                if ((dx == 0) && (dy == 0)) {
                    continue;
                }

                int overlapWidth = width - Math.abs(dx);
                int overlapHeight = height - Math.abs(dy);
                if (overlapWidth <= 0 || overlapHeight <= 0) {
                    continue;
                }

                scoredVectors.add(new ScoredMotionVector(
                        new MotionVector(dx, dy),
                        this.scoreMotionVector(previous, next, dx, dy)
                ));
            }
        }

        scoredVectors.sort(Comparator.comparingDouble(ScoredMotionVector::score).reversed());
        ArrayList<MotionVector> rankedVectors = new ArrayList<>(scoredVectors.size());
        ArrayList<MotionVector> rankedVectorsWithoutZero = new ArrayList<>(Math.max(0, scoredVectors.size() - 1));
        for (ScoredMotionVector scoredVector : scoredVectors) {
            MotionVector motionVector = scoredVector.vector();
            rankedVectors.add(motionVector);
            if ((motionVector.dx() != 0) || (motionVector.dy() != 0)) {
                rankedVectorsWithoutZero.add(motionVector);
            }
        }

        motionSearchAnalysis.cacheRankedMotionVectors(List.copyOf(rankedVectors), List.copyOf(rankedVectorsWithoutZero));
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
    protected CopyRectCandidate findBestMultiCopyRect(@Nullable AfmaFramePairAnalysis pairAnalysis,
                                                      @NotNull int[] predictedPixels, int width, int height, @NotNull AfmaPixelFrame next) {
        List<MotionVector> motionVectors;
        if (pairAnalysis != null) {
            motionVectors = this.collectMotionVectors(pairAnalysis, false);
        } else {
            AfmaPixelFrame predictedFrame = new AfmaPixelFrame(width, height, predictedPixels);
            motionVectors = this.collectMotionVectors(new AfmaFramePairAnalysis(predictedFrame, next), false);
        }
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
        int maxRuns = (overlapWidth / 2) + 2;
        int[] prevStartX = new int[maxRuns];
        int[] prevEndX = new int[maxRuns];
        int[] prevStartY = new int[maxRuns];
        int[] prevHeight = new int[maxRuns];
        long[] prevDirty = new long[maxRuns];
        int prevCount = 0;
        int[] currStartX = new int[maxRuns];
        int[] currEndX = new int[maxRuns];
        int[] currStartY = new int[maxRuns];
        int[] currHeight = new int[maxRuns];
        long[] currDirty = new long[maxRuns];
        CopyRectCandidate bestCandidate = null;

        for (int localY = 0; localY < overlapHeight; localY++) {
            int srcRowOffset = ((srcBaseY + localY) * width) + srcBaseX;
            int dstRowOffset = ((dstBaseY + localY) * width) + dstBaseX;
            int currCount = 0;
            int runStartX = -1;
            int runDirtyPixels = 0;

            for (int localX = 0; localX < overlapWidth; localX++) {
                boolean motionMatch = predictedPixels[srcRowOffset + localX] == nextPixels[dstRowOffset + localX];
                if (!motionMatch) {
                    if (runStartX >= 0) {
                        currStartX[currCount] = runStartX;
                        currEndX[currCount] = localX;
                        currStartY[currCount] = localY;
                        currHeight[currCount] = 1;
                        currDirty[currCount] = runDirtyPixels;
                        currCount++;
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
                currStartX[currCount] = runStartX;
                currEndX[currCount] = overlapWidth;
                currStartY[currCount] = localY;
                currHeight[currCount] = 1;
                currDirty[currCount] = runDirtyPixels;
                currCount++;
            }

            for (int previousIndex = 0; previousIndex < prevCount; previousIndex++) {
                boolean matched = false;
                for (int currentIndex = 0; currentIndex < currCount; currentIndex++) {
                    if ((prevStartX[previousIndex] == currStartX[currentIndex])
                            && (prevEndX[previousIndex] == currEndX[currentIndex])) {
                        currStartY[currentIndex] = prevStartY[previousIndex];
                        currHeight[currentIndex] = prevHeight[previousIndex] + 1;
                        currDirty[currentIndex] = prevDirty[previousIndex] + currDirty[currentIndex];
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    bestCandidate = this.selectBetterMultiCopyCandidate(
                            bestCandidate,
                            prevStartX[previousIndex],
                            prevStartY[previousIndex],
                            prevEndX[previousIndex] - prevStartX[previousIndex],
                            prevHeight[previousIndex],
                            prevDirty[previousIndex],
                            srcBaseX,
                            srcBaseY,
                            dstBaseX,
                            dstBaseY,
                            dx,
                            dy
                    );
                }
            }

            int[] tempStartX = prevStartX;
            prevStartX = currStartX;
            currStartX = tempStartX;
            int[] tempEndX = prevEndX;
            prevEndX = currEndX;
            currEndX = tempEndX;
            int[] tempStartY = prevStartY;
            prevStartY = currStartY;
            currStartY = tempStartY;
            int[] tempHeight = prevHeight;
            prevHeight = currHeight;
            currHeight = tempHeight;
            long[] tempDirty = prevDirty;
            prevDirty = currDirty;
            currDirty = tempDirty;
            prevCount = currCount;
        }

        for (int previousIndex = 0; previousIndex < prevCount; previousIndex++) {
            bestCandidate = this.selectBetterMultiCopyCandidate(
                    bestCandidate,
                    prevStartX[previousIndex],
                    prevStartY[previousIndex],
                    prevEndX[previousIndex] - prevStartX[previousIndex],
                    prevHeight[previousIndex],
                    prevDirty[previousIndex],
                    srcBaseX,
                    srcBaseY,
                    dstBaseX,
                    dstBaseY,
                    dx,
                    dy
            );
        }
        return bestCandidate;
    }

    @Nullable
    protected CopyRectCandidate selectBetterMultiCopyCandidate(@Nullable CopyRectCandidate bestCandidate,
                                                               int startX, int startY, int width, int height, long dirtyPixels,
                                                               int srcBaseX, int srcBaseY, int dstBaseX, int dstBaseY,
                                                               int dx, int dy) {
        long rectArea = (long) width * height;
        if ((rectArea < MIN_MULTI_COPY_RECT_AREA) || (dirtyPixels < MIN_MULTI_COPY_DIRTY_PIXELS)) {
            return bestCandidate;
        }
        if (((double) dirtyPixels / (double) rectArea) < MIN_MULTI_COPY_DIRTY_DENSITY) {
            return bestCandidate;
        }

        AfmaCopyRect copyRect = new AfmaCopyRect(
                srcBaseX + startX,
                srcBaseY + startY,
                dstBaseX + startX,
                dstBaseY + startY,
                width,
                height
        );
        CopyRectCandidate candidate = new CopyRectCandidate(copyRect, dirtyPixels, rectArea, dx, dy);
        if ((bestCandidate == null) || candidate.isBetterThan(bestCandidate)) {
            return candidate;
        }
        return bestCandidate;
    }

    protected static final class MotionSearchAnalysis {

        @NotNull
        private final List<Integer> candidateDx;
        @NotNull
        private final List<Integer> candidateDy;
        @Nullable
        private List<MotionVector> rankedMotionVectors;
        @Nullable
        private List<MotionVector> rankedMotionVectorsWithoutZero;

        protected MotionSearchAnalysis(@NotNull List<Integer> candidateDx, @NotNull List<Integer> candidateDy) {
            this.candidateDx = List.copyOf(candidateDx);
            this.candidateDy = List.copyOf(candidateDy);
        }

        @NotNull
        public List<Integer> candidateDx() {
            return this.candidateDx;
        }

        @NotNull
        public List<Integer> candidateDy() {
            return this.candidateDy;
        }

        public boolean hasRankedMotionVectors() {
            return (this.rankedMotionVectors != null) && (this.rankedMotionVectorsWithoutZero != null);
        }

        public void cacheRankedMotionVectors(@NotNull List<MotionVector> rankedMotionVectors,
                                             @NotNull List<MotionVector> rankedMotionVectorsWithoutZero) {
            this.rankedMotionVectors = rankedMotionVectors;
            this.rankedMotionVectorsWithoutZero = rankedMotionVectorsWithoutZero;
        }

        @NotNull
        public List<MotionVector> rankedMotionVectors() {
            if (this.rankedMotionVectors == null) {
                throw new IllegalStateException("AFMA motion vectors have not been ranked yet");
            }
            return this.rankedMotionVectors;
        }

        @NotNull
        public List<MotionVector> rankedMotionVectorsWithoutZero() {
            if (this.rankedMotionVectorsWithoutZero == null) {
                throw new IllegalStateException("AFMA non-zero motion vectors have not been ranked yet");
            }
            return this.rankedMotionVectorsWithoutZero;
        }

    }

    protected record AxisCandidate(int offset, double score) {
    }

    protected record ScoredMotionVector(@NotNull MotionVector vector, double score) {
    }

    public record Detection(@NotNull AfmaCopyRect copyRect, @Nullable AfmaRect patchBounds, long usefulness,
                            int remainingDirtyPixelCount) {
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
