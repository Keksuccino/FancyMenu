package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaCopyRect;
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

    protected final int maxSearchDistance;
    protected final int maxCandidateAxisOffsets;

    public AfmaRectCopyDetector(int maxSearchDistance, int maxCandidateAxisOffsets) {
        this.maxSearchDistance = maxSearchDistance;
        this.maxCandidateAxisOffsets = maxCandidateAxisOffsets;
    }

    @Nullable
    public Detection detect(@NotNull AfmaPixelFrame previous, @NotNull AfmaPixelFrame next) {
        AfmaPixelFrameHelper.ensureSameSize(previous, next);

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

                AfmaRect patchBounds = AfmaPixelFrameHelper.findDirtyBoundsAfterCopy(previous, next, copyRect);
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

    @NotNull
    public List<MotionVector> collectMotionVectors(@NotNull AfmaPixelFrame previous, @NotNull AfmaPixelFrame next, boolean includeZeroVector) {
        AfmaPixelFrameHelper.ensureSameSize(previous, next);

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

    protected record AxisCandidate(int offset, double score) {
    }

    public record Detection(@NotNull AfmaCopyRect copyRect, @Nullable AfmaRect patchBounds, long usefulness) {
        public long patchArea() {
            return (this.patchBounds != null) ? this.patchBounds.area() : 0L;
        }
    }

    public record MotionVector(int dx, int dy) {
    }

}
