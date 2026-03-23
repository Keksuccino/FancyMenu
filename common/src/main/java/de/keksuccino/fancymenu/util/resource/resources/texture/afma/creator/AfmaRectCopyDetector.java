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
    protected List<Integer> collectAxisCandidates(@NotNull AfmaPixelFrame previous, @NotNull AfmaPixelFrame next, boolean horizontal, int maxOffset) {
        Set<Integer> offsets = new LinkedHashSet<>();
        offsets.add(0);
        if (maxOffset <= 0) {
            return List.copyOf(offsets);
        }

        List<AxisCandidate> candidates = new ArrayList<>();
        for (int offset = -maxOffset; offset <= maxOffset; offset++) {
            if (offset == 0) continue;
            double score = this.scoreOffset(previous, next, horizontal, offset);
            candidates.add(new AxisCandidate(offset, score));
        }

        candidates.sort(Comparator.comparingDouble(AxisCandidate::score).reversed());
        for (AxisCandidate candidate : candidates) {
            if (candidate.score() <= 0.65D) continue;
            offsets.add(candidate.offset());
            if (offsets.size() >= (this.maxCandidateAxisOffsets + 1)) {
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

        int stepX = Math.max(1, overlapWidth / 32);
        int stepY = Math.max(1, overlapHeight / 12);
        int matches = 0;
        int samples = 0;

        for (int y = 0; y < overlapHeight; y += stepY) {
            for (int x = 0; x < overlapWidth; x += stepX) {
                samples++;
                if (previous.getPixelRGBA(srcX + x, srcY + y) == next.getPixelRGBA(dstX + x, dstY + y)) {
                    matches++;
                }
            }
        }

        return (samples > 0) ? ((double)matches / samples) : 0D;
    }

    protected record AxisCandidate(int offset, double score) {
    }

    public record Detection(@NotNull AfmaCopyRect copyRect, @Nullable AfmaRect patchBounds, long usefulness) {
        public long patchArea() {
            return (this.patchBounds != null) ? this.patchBounds.area() : 0L;
        }
    }

}
