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
        if (pairAnalysis.isIdentical()) {
            return null;
        }
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
        if (pairAnalysis.isIdentical()) {
            return null;
        }
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
        MultiCopyScratchWorkspace multiCopyScratchWorkspace = new MultiCopyScratchWorkspace((width / 2) + 2);
        long totalDirtyCoverage = 0L;
        AfmaFramePairAnalysis motionSearchPairAnalysis = pairAnalysis;

        for (int copyIndex = 0; copyIndex < MAX_MULTI_COPY_RECTS; copyIndex++) {
            CopyRectCandidate nextCandidate = this.findBestMultiCopyRect(
                    motionSearchPairAnalysis,
                    predictedPixels,
                    width,
                    height,
                    next,
                    multiCopyScratchWorkspace
            );
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
    protected List<MotionVector> collectTopMotionVectors(@NotNull AfmaFramePairAnalysis pairAnalysis, int maxNonZeroVectors) {
        if (maxNonZeroVectors <= 0) {
            return List.of();
        }

        MotionSearchAnalysis motionSearchAnalysis = this.getMotionSearchAnalysis(pairAnalysis);
        this.ensureTopRankedMotionVectors(pairAnalysis, motionSearchAnalysis, maxNonZeroVectors);
        return motionSearchAnalysis.topRankedMotionVectorsWithoutZero(maxNonZeroVectors);
    }

    @NotNull
    protected MotionSearchAnalysis getMotionSearchAnalysis(@NotNull AfmaFramePairAnalysis pairAnalysis) {
        // Reuse the expensive axis scan for detect(), detectMulti(), and block_inter within the same frame pair.
        MotionSearchAnalysis cachedAnalysis = pairAnalysis.getMotionSearchAnalysis(this.maxSearchDistance, this.maxCandidateAxisOffsets);
        if (cachedAnalysis != null) {
            return cachedAnalysis;
        }

        AfmaPixelFrame previous = pairAnalysis.previousFrame();
        int maxDx = Math.min(previous.getWidth() - 1, this.maxSearchDistance);
        int maxDy = Math.min(previous.getHeight() - 1, this.maxSearchDistance);
        MotionSearchAnalysis analysis = new MotionSearchAnalysis(
                this.collectAxisCandidates(pairAnalysis, true, maxDx),
                this.collectAxisCandidates(pairAnalysis, false, maxDy)
        );
        pairAnalysis.cacheMotionSearchAnalysis(this.maxSearchDistance, this.maxCandidateAxisOffsets, analysis);
        return analysis;
    }

    protected void ensureRankedMotionVectors(@NotNull AfmaFramePairAnalysis pairAnalysis, @NotNull MotionSearchAnalysis motionSearchAnalysis) {
        if (motionSearchAnalysis.hasRankedMotionVectors()) {
            return;
        }

        AfmaPixelFrame previous = pairAnalysis.previousFrame();
        int width = previous.getWidth();
        int height = previous.getHeight();
        ArrayList<ScoredMotionVector> scoredVectors = new ArrayList<>(
                (motionSearchAnalysis.candidateDx().size() * motionSearchAnalysis.candidateDy().size()) + 1
        );
        scoredVectors.add(new ScoredMotionVector(new MotionVector(0, 0), this.scoreMotionVector(pairAnalysis, 0, 0)));
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
                        this.scoreMotionVector(pairAnalysis, dx, dy)
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

    protected void ensureTopRankedMotionVectors(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                                @NotNull MotionSearchAnalysis motionSearchAnalysis, int maxNonZeroVectors) {
        if (motionSearchAnalysis.hasTopRankedMotionVectorsWithoutZero(maxNonZeroVectors)) {
            return;
        }

        AfmaPixelFrame previous = pairAnalysis.previousFrame();
        int width = previous.getWidth();
        int height = previous.getHeight();
        ArrayList<ScoredMotionVector> rankedVectors = new ArrayList<>(maxNonZeroVectors);
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

                this.insertRankedMotionVector(
                        rankedVectors,
                        maxNonZeroVectors,
                        dx,
                        dy,
                        this.scoreMotionVector(pairAnalysis, dx, dy)
                );
            }
        }

        ArrayList<MotionVector> topRankedVectors = new ArrayList<>(rankedVectors.size());
        for (ScoredMotionVector rankedVector : rankedVectors) {
            topRankedVectors.add(rankedVector.vector());
        }
        motionSearchAnalysis.cacheTopRankedMotionVectorsWithoutZero(
                List.copyOf(topRankedVectors),
                topRankedVectors.size() < maxNonZeroVectors
        );
    }

    @NotNull
    protected List<Integer> collectAxisCandidates(@NotNull AfmaFramePairAnalysis pairAnalysis, boolean horizontal, int maxOffset) {
        AfmaPixelFrame previous = pairAnalysis.previousFrame();
        Set<Integer> offsets = new LinkedHashSet<>();
        offsets.add(0);
        if (maxOffset <= 0) {
            return List.copyOf(offsets);
        }
        for (int offset = 1; offset <= Math.min(SMALL_OFFSET_PROBE, maxOffset); offset++) {
            offsets.add(offset);
            offsets.add(-offset);
        }

        if (this.maxCandidateAxisOffsets <= 0) {
            AxisCandidate bestCandidate = null;
            for (int offset = -maxOffset; offset <= maxOffset; offset++) {
                if (offset == 0) {
                    continue;
                }

                double score = this.scoreOffset(pairAnalysis, horizontal, offset);
                if ((score > MIN_CANDIDATE_SCORE) && ((bestCandidate == null) || (score > bestCandidate.score()))) {
                    bestCandidate = new AxisCandidate(offset, score);
                }
            }
            if (bestCandidate != null) {
                this.addAxisCandidateOffsets(offsets, bestCandidate.offset(), maxOffset);
            }
            return List.copyOf(offsets);
        }

        ArrayList<AxisCandidate> rankedCandidates = new ArrayList<>(this.maxCandidateAxisOffsets);
        for (int offset = -maxOffset; offset <= maxOffset; offset++) {
            if (offset == 0) {
                continue;
            }

            this.insertRankedAxisCandidate(
                    rankedCandidates,
                    this.maxCandidateAxisOffsets,
                    offset,
                    this.scoreOffset(pairAnalysis, horizontal, offset)
            );
        }

        for (AxisCandidate candidate : rankedCandidates) {
            this.addAxisCandidateOffsets(offsets, candidate.offset(), maxOffset);
        }

        return List.copyOf(offsets);
    }

    protected double scoreOffset(@NotNull AfmaFramePairAnalysis pairAnalysis, boolean horizontal, int offset) {
        AfmaPixelFrame previous = pairAnalysis.previousFrame();
        int width = previous.getWidth();
        int height = previous.getHeight();
        int overlapWidth = horizontal ? width - Math.abs(offset) : width;
        int overlapHeight = horizontal ? height : height - Math.abs(offset);
        if (overlapWidth <= 0 || overlapHeight <= 0) return 0D;

        int srcX = horizontal ? Math.max(0, -offset) : 0;
        int dstX = horizontal ? Math.max(0, offset) : 0;
        int srcY = horizontal ? 0 : Math.max(0, -offset);
        int dstY = horizontal ? 0 : Math.max(0, offset);
        AfmaRect scoringBounds = pairAnalysis.intersectDifferenceBounds(dstX, dstY, overlapWidth, overlapHeight);
        if (scoringBounds == null) {
            return pairAnalysis.isIdentical() ? 1D : 0D;
        }

        int sampleSrcX = srcX + (scoringBounds.x() - dstX);
        int sampleSrcY = srcY + (scoringBounds.y() - dstY);
        int sampleWidth = scoringBounds.width();
        int sampleHeight = scoringBounds.height();

        double fullScore = this.sampleMatchRatio(pairAnalysis, sampleSrcX, sampleSrcY,
                scoringBounds.x(), scoringBounds.y(), sampleWidth, sampleHeight);
        if (horizontal) {
            int bandHeight = Math.max(1, sampleHeight / 2);
            int bandY = sampleSrcY + Math.max(0, (sampleHeight - bandHeight) / 2);
            int dstBandY = scoringBounds.y() + Math.max(0, (sampleHeight - bandHeight) / 2);
            return Math.max(fullScore, this.sampleMatchRatio(pairAnalysis,
                    sampleSrcX, bandY, scoringBounds.x(), dstBandY, sampleWidth, bandHeight));
        }

        int bandWidth = Math.max(1, sampleWidth / 2);
        int bandX = sampleSrcX + Math.max(0, (sampleWidth - bandWidth) / 2);
        int dstBandX = scoringBounds.x() + Math.max(0, (sampleWidth - bandWidth) / 2);
        return Math.max(fullScore, this.sampleMatchRatio(pairAnalysis,
                bandX, sampleSrcY, dstBandX, scoringBounds.y(), bandWidth, sampleHeight));
    }

    protected double sampleMatchRatio(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                      int srcX, int srcY, int dstX, int dstY, int sampleWidth, int sampleHeight) {
        return pairAnalysis.sampleMatchRatio(srcX, srcY, dstX, dstY, sampleWidth, sampleHeight);
    }

    protected double scoreMotionVector(@NotNull AfmaFramePairAnalysis pairAnalysis, int dx, int dy) {
        AfmaPixelFrame previous = pairAnalysis.previousFrame();
        int overlapWidth = previous.getWidth() - Math.abs(dx);
        int overlapHeight = previous.getHeight() - Math.abs(dy);
        if (overlapWidth <= 0 || overlapHeight <= 0) {
            return 0D;
        }

        int srcX = Math.max(0, -dx);
        int srcY = Math.max(0, -dy);
        int dstX = Math.max(0, dx);
        int dstY = Math.max(0, dy);
        AfmaRect scoringBounds = pairAnalysis.intersectDifferenceBounds(dstX, dstY, overlapWidth, overlapHeight);
        if (scoringBounds == null) {
            return pairAnalysis.isIdentical() ? 1D : 0D;
        }
        return this.sampleMatchRatio(
                pairAnalysis,
                srcX + (scoringBounds.x() - dstX),
                srcY + (scoringBounds.y() - dstY),
                scoringBounds.x(),
                scoringBounds.y(),
                scoringBounds.width(),
                scoringBounds.height()
        );
    }

    @Nullable
    protected CopyRectCandidate findBestMultiCopyRect(@Nullable AfmaFramePairAnalysis pairAnalysis,
                                                      @NotNull int[] predictedPixels, int width, int height, @NotNull AfmaPixelFrame next,
                                                      @NotNull MultiCopyScratchWorkspace multiCopyScratchWorkspace) {
        List<MotionVector> motionVectors;
        if (pairAnalysis != null) {
            motionVectors = this.collectTopMotionVectors(pairAnalysis, MAX_MULTI_COPY_MOTION_VECTORS);
        } else {
            AfmaPixelFrame predictedFrame = new AfmaPixelFrame(width, height, predictedPixels);
            motionVectors = this.collectTopMotionVectors(new AfmaFramePairAnalysis(predictedFrame, next), MAX_MULTI_COPY_MOTION_VECTORS);
        }
        if (motionVectors.isEmpty()) {
            return null;
        }

        int[] nextPixels = next.getPixelsUnsafe();
        CopyRectCandidate bestCandidate = null;
        for (int motionIndex = 0; motionIndex < motionVectors.size(); motionIndex++) {
            MotionVector motionVector = motionVectors.get(motionIndex);
            CopyRectCandidate candidate = this.findBestMultiCopyRectForMotion(
                    predictedPixels,
                    nextPixels,
                    width,
                    height,
                    motionVector.dx(),
                    motionVector.dy(),
                    multiCopyScratchWorkspace
            );
            if ((candidate != null) && ((bestCandidate == null) || candidate.isBetterThan(bestCandidate))) {
                bestCandidate = candidate;
            }
        }
        return bestCandidate;
    }

    @Nullable
    protected CopyRectCandidate findBestMultiCopyRectForMotion(@NotNull int[] predictedPixels, @NotNull int[] nextPixels,
                                                               int width, int height, int dx, int dy,
                                                               @NotNull MultiCopyScratchWorkspace multiCopyScratchWorkspace) {
        int overlapWidth = width - Math.abs(dx);
        int overlapHeight = height - Math.abs(dy);
        if ((overlapWidth <= 0) || (overlapHeight <= 0)) {
            return null;
        }

        int srcBaseX = Math.max(0, -dx);
        int srcBaseY = Math.max(0, -dy);
        int dstBaseX = Math.max(0, dx);
        int dstBaseY = Math.max(0, dy);
        int[] prevStartX = multiCopyScratchWorkspace.runStartXBufferA();
        int[] prevEndX = multiCopyScratchWorkspace.runEndXBufferA();
        int[] prevStartY = multiCopyScratchWorkspace.runStartYBufferA();
        int[] prevHeight = multiCopyScratchWorkspace.runHeightBufferA();
        long[] prevDirty = multiCopyScratchWorkspace.runDirtyBufferA();
        int prevCount = 0;
        int[] currStartX = multiCopyScratchWorkspace.runStartXBufferB();
        int[] currEndX = multiCopyScratchWorkspace.runEndXBufferB();
        int[] currStartY = multiCopyScratchWorkspace.runStartYBufferB();
        int[] currHeight = multiCopyScratchWorkspace.runHeightBufferB();
        long[] currDirty = multiCopyScratchWorkspace.runDirtyBufferB();
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

    protected void insertRankedMotionVector(@NotNull List<ScoredMotionVector> rankedVectors, int maxVectors,
                                            int dx, int dy, double score) {
        int insertIndex = rankedVectors.size();
        while ((insertIndex > 0) && (score > rankedVectors.get(insertIndex - 1).score())) {
            insertIndex--;
        }
        if ((insertIndex >= maxVectors) && (rankedVectors.size() >= maxVectors)) {
            return;
        }

        rankedVectors.add(insertIndex, new ScoredMotionVector(new MotionVector(dx, dy), score));
        if (rankedVectors.size() > maxVectors) {
            rankedVectors.remove(rankedVectors.size() - 1);
        }
    }

    protected void insertRankedAxisCandidate(@NotNull List<AxisCandidate> rankedCandidates, int maxCandidates,
                                             int offset, double score) {
        int insertIndex = rankedCandidates.size();
        while ((insertIndex > 0) && (score > rankedCandidates.get(insertIndex - 1).score())) {
            insertIndex--;
        }
        if ((insertIndex >= maxCandidates) && (rankedCandidates.size() >= maxCandidates)) {
            return;
        }

        rankedCandidates.add(insertIndex, new AxisCandidate(offset, score));
        if (rankedCandidates.size() > maxCandidates) {
            rankedCandidates.remove(rankedCandidates.size() - 1);
        }
    }

    protected void addAxisCandidateOffsets(@NotNull Set<Integer> offsets, int offset, int maxOffset) {
        offsets.add(offset);
        for (int neighborOffset = 1; neighborOffset <= NEIGHBOR_OFFSET_RADIUS; neighborOffset++) {
            int negativeNeighbor = offset - neighborOffset;
            if (negativeNeighbor >= -maxOffset) {
                offsets.add(negativeNeighbor);
            }
            int positiveNeighbor = offset + neighborOffset;
            if (positiveNeighbor <= maxOffset) {
                offsets.add(positiveNeighbor);
            }
        }
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
        @Nullable
        private List<MotionVector> topRankedMotionVectorsWithoutZero;
        private boolean topRankedMotionVectorsWithoutZeroComplete;

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
            this.topRankedMotionVectorsWithoutZero = rankedMotionVectorsWithoutZero;
            this.topRankedMotionVectorsWithoutZeroComplete = true;
        }

        public boolean hasTopRankedMotionVectorsWithoutZero(int requestedCount) {
            return (this.topRankedMotionVectorsWithoutZero != null)
                    && (this.topRankedMotionVectorsWithoutZeroComplete
                    || (this.topRankedMotionVectorsWithoutZero.size() >= requestedCount));
        }

        public void cacheTopRankedMotionVectorsWithoutZero(@NotNull List<MotionVector> topRankedMotionVectorsWithoutZero,
                                                           boolean complete) {
            this.topRankedMotionVectorsWithoutZero = topRankedMotionVectorsWithoutZero;
            this.topRankedMotionVectorsWithoutZeroComplete = complete;
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

        @NotNull
        public List<MotionVector> topRankedMotionVectorsWithoutZero(int maxCount) {
            if (this.topRankedMotionVectorsWithoutZero == null) {
                throw new IllegalStateException("AFMA top motion vectors have not been ranked yet");
            }
            if (this.topRankedMotionVectorsWithoutZero.size() <= maxCount) {
                if (!this.topRankedMotionVectorsWithoutZeroComplete
                        && (this.topRankedMotionVectorsWithoutZero.size() < maxCount)) {
                    throw new IllegalStateException("AFMA top motion vectors have not been ranked deeply enough");
                }
                return this.topRankedMotionVectorsWithoutZero;
            }
            return this.topRankedMotionVectorsWithoutZero.subList(0, maxCount);
        }

    }

    protected record AxisCandidate(int offset, double score) {
    }

    protected record ScoredMotionVector(@NotNull MotionVector vector, double score) {
    }

    protected record MultiCopyScratchWorkspace(@NotNull int[] runStartXBufferA, @NotNull int[] runEndXBufferA,
                                               @NotNull int[] runStartYBufferA, @NotNull int[] runHeightBufferA,
                                               @NotNull long[] runDirtyBufferA, @NotNull int[] runStartXBufferB,
                                               @NotNull int[] runEndXBufferB, @NotNull int[] runStartYBufferB,
                                               @NotNull int[] runHeightBufferB, @NotNull long[] runDirtyBufferB) {

        protected MultiCopyScratchWorkspace(int maxRuns) {
            this(
                    new int[maxRuns],
                    new int[maxRuns],
                    new int[maxRuns],
                    new int[maxRuns],
                    new long[maxRuns],
                    new int[maxRuns],
                    new int[maxRuns],
                    new int[maxRuns],
                    new int[maxRuns],
                    new long[maxRuns]
            );
        }

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
