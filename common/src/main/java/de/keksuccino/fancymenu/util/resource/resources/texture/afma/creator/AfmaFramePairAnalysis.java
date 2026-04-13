package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaCopyRect;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaRect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class AfmaFramePairAnalysis {

    protected static final long TILE_HASH_OFFSET_BASIS_FANCYMENU = 0xCBF29CE484222325L;
    protected static final long TILE_HASH_PRIME_FANCYMENU = 0x100000001B3L;

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
    @NotNull
    protected final SharedAnalysisIndex sharedAnalysisIndex;
    @Nullable
    protected Map<CopyRectKey, DirtyBoundsAfterCopyResult> dirtyBoundsAfterCopyByKey;
    @Nullable
    protected Map<MotionSearchKey, AfmaRectCopyDetector.MotionSearchAnalysis> motionSearchAnalysesByKey;
    @NotNull
    protected Map<Integer, TileGridSummary> tileGridSummariesByTileSize = new LinkedHashMap<>();

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
        this.sharedAnalysisIndex = this.buildSharedAnalysisIndex();
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
        return this.sharedAnalysisIndex.identical();
    }

    public @Nullable AfmaRect differenceBounds() {
        return this.sharedAnalysisIndex.differenceBounds();
    }

    public int changedPixelCount() {
        return this.sharedAnalysisIndex.changedPixelCount();
    }

    public @Nullable AfmaRect findDirtyBoundsAfterCopy(@NotNull AfmaCopyRect copyRect) {
        return this.analyzeDirtyAfterCopy(copyRect).bounds();
    }

    public int countDirtyPixels(int x, int y, int width, int height) {
        ClippedRegion clippedRegion = this.clipRegion(x, y, width, height);
        if (clippedRegion == null) {
            return 0;
        }
        return sumIntPrefix(this.sharedAnalysisIndex.dirtyPrefixSum(), clippedRegion, this.width + 1);
    }

    public boolean hasAlphaDifference(int x, int y, int width, int height) {
        return this.regionErrorMetrics(x, y, width, height).alphaErrorSum() > 0L;
    }

    public boolean nextRegionHasNonOpaquePixels(int x, int y, int width, int height) {
        ClippedRegion clippedRegion = this.clipRegion(x, y, width, height);
        if (clippedRegion == null) {
            return false;
        }
        return sumIntPrefix(this.sharedAnalysisIndex.nextNonOpaquePrefixSum(), clippedRegion, this.width + 1) > 0;
    }

    @NotNull
    public RegionErrorMetrics regionErrorMetrics(int x, int y, int width, int height) {
        ClippedRegion clippedRegion = this.clipRegion(x, y, width, height);
        if (clippedRegion == null) {
            return new RegionErrorMetrics(0, 0L, 0L, false);
        }
        return new RegionErrorMetrics(
                sumIntPrefix(this.sharedAnalysisIndex.dirtyPrefixSum(), clippedRegion, this.width + 1),
                sumLongPrefix(this.sharedAnalysisIndex.colorErrorPrefixSum(), clippedRegion, this.width + 1),
                sumLongPrefix(this.sharedAnalysisIndex.alphaErrorPrefixSum(), clippedRegion, this.width + 1),
                sumIntPrefix(this.sharedAnalysisIndex.nextNonOpaquePrefixSum(), clippedRegion, this.width + 1) > 0
        );
    }

    public @Nullable AfmaRect intersectDifferenceBounds(int x, int y, int width, int height) {
        ClippedRegion clippedRegion = this.clipRegion(x, y, width, height);
        if (clippedRegion == null) {
            return null;
        }
        AfmaRect differenceBounds = this.sharedAnalysisIndex.differenceBounds();
        if (differenceBounds == null) {
            return null;
        }

        int minX = Math.max(clippedRegion.x(), differenceBounds.x());
        int minY = Math.max(clippedRegion.y(), differenceBounds.y());
        int maxX = Math.min(clippedRegion.right(), differenceBounds.x() + differenceBounds.width());
        int maxY = Math.min(clippedRegion.bottom(), differenceBounds.y() + differenceBounds.height());
        if ((maxX <= minX) || (maxY <= minY)) {
            return null;
        }
        return new AfmaRect(minX, minY, maxX - minX, maxY - minY);
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
        return this.sharedAnalysisIndex.perceptualDriftMetrics();
    }

    public double sampleMatchRatio(int srcX, int srcY, int dstX, int dstY, int sampleWidth, int sampleHeight) {
        int stepX = Math.max(1, sampleWidth / 48);
        int stepY = Math.max(1, sampleHeight / 16);
        int matches = 0;
        int samples = 0;

        for (int y = 0; y < sampleHeight; y += stepY) {
            int previousRowOffset = ((srcY + y) * this.width) + srcX;
            int nextRowOffset = ((dstY + y) * this.width) + dstX;
            for (int x = 0; x < sampleWidth; x += stepX) {
                samples++;
                if (this.previousPixels[previousRowOffset + x] == this.nextPixels[nextRowOffset + x]) {
                    matches++;
                }
            }
        }

        return (samples > 0) ? ((double) matches / samples) : 0D;
    }

    @NotNull
    public TileGridSummary tileGridSummary(int tileSize) {
        if (tileSize <= 0) {
            throw new IllegalArgumentException("AFMA tile size must be greater than zero");
        }
        TileGridSummary cachedSummary = this.tileGridSummariesByTileSize.get(tileSize);
        if (cachedSummary != null) {
            return cachedSummary;
        }

        TileGridSummary tileGridSummary = this.buildTileGridSummary(tileSize);
        this.tileGridSummariesByTileSize.put(tileSize, tileGridSummary);
        return tileGridSummary;
    }

    @NotNull
    protected SharedAnalysisIndex buildSharedAnalysisIndex() {
        int prefixWidth = this.width + 1;
        int prefixSize = prefixWidth * (this.height + 1);
        int[] differenceRowFirstDirtyX = new int[this.height];
        int[] differenceRowLastDirtyX = new int[this.height];
        Arrays.fill(differenceRowFirstDirtyX, -1);
        Arrays.fill(differenceRowLastDirtyX, -1);
        int[] dirtyPrefixSum = new int[prefixSize];
        int[] nextNonOpaquePrefixSum = new int[prefixSize];
        long[] colorErrorPrefixSum = new long[prefixSize];
        long[] alphaErrorPrefixSum = new long[prefixSize];
        int minX = this.width;
        int minY = this.height;
        int maxX = -1;
        int maxY = -1;
        int changedPixelCount = 0;
        double totalPerceptualError = 0D;
        int maxVisibleColorDelta = 0;
        int maxAlphaDelta = 0;
        for (int y = 0; y < this.height; y++) {
            int rowOffset = y * this.width;
            for (int x = 0; x < this.width; x++) {
                int pixelIndex = rowOffset + x;
                int previousColor = this.previousPixels[pixelIndex];
                int nextColor = this.nextPixels[pixelIndex];
                boolean dirty = previousColor != nextColor;
                if (dirty) {
                    changedPixelCount++;
                    if (differenceRowFirstDirtyX[y] < 0) {
                        differenceRowFirstDirtyX[y] = x;
                    }
                    differenceRowLastDirtyX[y] = x;
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }

                int previousAlpha = (previousColor >>> 24) & 0xFF;
                int nextAlpha = (nextColor >>> 24) & 0xFF;
                int alphaDelta = Math.abs(previousAlpha - nextAlpha);
                if (alphaDelta > maxAlphaDelta) {
                    maxAlphaDelta = alphaDelta;
                }

                int redDelta = channelDifference(previousColor >> 16, nextColor >> 16);
                int greenDelta = channelDifference(previousColor >> 8, nextColor >> 8);
                int blueDelta = channelDifference(previousColor, nextColor);
                int visibilityAlpha = Math.max(previousAlpha, nextAlpha);
                if (visibilityAlpha > 0) {
                    int visibleColorDelta = Math.max(redDelta, Math.max(greenDelta, blueDelta));
                    if (visibleColorDelta > maxVisibleColorDelta) {
                        maxVisibleColorDelta = visibleColorDelta;
                    }
                    totalPerceptualError += (alphaDelta * 2.0D) + ((redDelta + greenDelta + blueDelta) * (visibilityAlpha / 255.0D));
                }

                int prefixArrayIndex = prefixIndex(x + 1, y + 1, prefixWidth);
                int aboveIndex = prefixIndex(x + 1, y, prefixWidth);
                int leftIndex = prefixIndex(x, y + 1, prefixWidth);
                int upperLeftIndex = prefixIndex(x, y, prefixWidth);
                dirtyPrefixSum[prefixArrayIndex] = dirtyPrefixSum[aboveIndex] + dirtyPrefixSum[leftIndex] - dirtyPrefixSum[upperLeftIndex] + (dirty ? 1 : 0);
                nextNonOpaquePrefixSum[prefixArrayIndex] = nextNonOpaquePrefixSum[aboveIndex] + nextNonOpaquePrefixSum[leftIndex]
                        - nextNonOpaquePrefixSum[upperLeftIndex] + ((nextAlpha != 0xFF) ? 1 : 0);
                colorErrorPrefixSum[prefixArrayIndex] = colorErrorPrefixSum[aboveIndex] + colorErrorPrefixSum[leftIndex]
                        - colorErrorPrefixSum[upperLeftIndex] + redDelta + greenDelta + blueDelta;
                alphaErrorPrefixSum[prefixArrayIndex] = alphaErrorPrefixSum[aboveIndex] + alphaErrorPrefixSum[leftIndex]
                        - alphaErrorPrefixSum[upperLeftIndex] + alphaDelta;
            }
        }

        boolean identical = (maxX < minX) || (maxY < minY);
        AfmaRect differenceBounds = identical ? null : new AfmaRect(minX, minY, (maxX - minX) + 1, (maxY - minY) + 1);
        return new SharedAnalysisIndex(
                identical,
                changedPixelCount,
                differenceBounds,
                differenceRowFirstDirtyX,
                differenceRowLastDirtyX,
                dirtyPrefixSum,
                nextNonOpaquePrefixSum,
                colorErrorPrefixSum,
                alphaErrorPrefixSum,
                new PerceptualDriftMetrics(
                        totalPerceptualError / Math.max(1, this.previousPixels.length),
                        maxVisibleColorDelta,
                        maxAlphaDelta
                )
        );
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
        int initialDirtyPixelsInsideCopy = this.countDirtyPixels(dstLeft, dstTop, copyRect.getWidth(), copyRect.getHeight());
        int dirtyPixelsInsideCopyAfterCopy = 0;

        // A copy only changes dirty membership inside its destination rectangle.
        for (int localY = 0; localY < copyRect.getHeight(); localY++) {
            int pixelY = dstTop + localY;
            int dstRowOffset = (pixelY * this.width) + dstLeft;
            int srcRowOffset = ((srcTop + localY) * this.width) + srcLeft;
            for (int localX = 0; localX < copyRect.getWidth(); localX++) {
                int pixelIndex = dstRowOffset + localX;
                if (this.previousPixels[srcRowOffset + localX] == this.nextPixels[pixelIndex]) {
                    continue;
                }

                dirtyPixelsInsideCopyAfterCopy++;
                int pixelX = dstLeft + localX;
                if (pixelX < minX) minX = pixelX;
                if (pixelY < minY) minY = pixelY;
                if (pixelX > maxX) maxX = pixelX;
                if (pixelY > maxY) maxY = pixelY;
            }
        }

        int dirtyPixelCount = this.sharedAnalysisIndex.changedPixelCount() - initialDirtyPixelsInsideCopy + dirtyPixelsInsideCopyAfterCopy;
        AfmaRect initialDirtyBounds = this.sharedAnalysisIndex.differenceBounds();
        if (initialDirtyBounds != null) {
            int[] differenceRowFirstDirtyX = this.sharedAnalysisIndex.differenceRowFirstDirtyX();
            int[] differenceRowLastDirtyX = this.sharedAnalysisIndex.differenceRowLastDirtyX();
            int scanStartY = initialDirtyBounds.y();
            int scanEndY = initialDirtyBounds.y() + initialDirtyBounds.height();
            for (int y = scanStartY; y < scanEndY; y++) {
                int rowFirstDirtyX = differenceRowFirstDirtyX[y];
                if (rowFirstDirtyX < 0) {
                    continue;
                }

                int rowLastDirtyX = differenceRowLastDirtyX[y];
                int rowMinX = rowFirstDirtyX;
                int rowMaxX = rowLastDirtyX;
                if ((y >= dstTop) && (y < dstBottom)) {
                    boolean hasLeftDirty = rowFirstDirtyX < dstLeft;
                    boolean hasRightDirty = rowLastDirtyX >= dstRight;
                    if (!hasLeftDirty && !hasRightDirty) {
                        continue;
                    }
                    rowMinX = hasLeftDirty ? rowFirstDirtyX : rowLastDirtyX;
                    rowMaxX = hasRightDirty ? rowLastDirtyX : rowFirstDirtyX;
                }

                if (rowMinX < minX) minX = rowMinX;
                if (y < minY) minY = y;
                if (rowMaxX > maxX) maxX = rowMaxX;
                if (y > maxY) maxY = y;
            }
        }

        if (dirtyPixelCount <= 0) {
            return new DirtyBoundsAfterCopyResult(null, 0);
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

    protected static int prefixIndex(int x, int y, int prefixWidth) {
        return (y * prefixWidth) + x;
    }

    protected static int sumIntPrefix(@NotNull int[] prefixSum, @NotNull ClippedRegion region, int prefixWidth) {
        int left = region.x();
        int top = region.y();
        int right = region.right();
        int bottom = region.bottom();
        return prefixSum[prefixIndex(right, bottom, prefixWidth)]
                - prefixSum[prefixIndex(left, bottom, prefixWidth)]
                - prefixSum[prefixIndex(right, top, prefixWidth)]
                + prefixSum[prefixIndex(left, top, prefixWidth)];
    }

    protected static long sumLongPrefix(@NotNull long[] prefixSum, @NotNull ClippedRegion region, int prefixWidth) {
        int left = region.x();
        int top = region.y();
        int right = region.right();
        int bottom = region.bottom();
        return prefixSum[prefixIndex(right, bottom, prefixWidth)]
                - prefixSum[prefixIndex(left, bottom, prefixWidth)]
                - prefixSum[prefixIndex(right, top, prefixWidth)]
                + prefixSum[prefixIndex(left, top, prefixWidth)];
    }

    @Nullable
    protected ClippedRegion clipRegion(int x, int y, int width, int height) {
        if ((width <= 0) || (height <= 0)) {
            return null;
        }

        int minX = Math.max(0, x);
        int minY = Math.max(0, y);
        int maxX = Math.min(this.width, x + width);
        int maxY = Math.min(this.height, y + height);
        if ((maxX <= minX) || (maxY <= minY)) {
            return null;
        }
        return new ClippedRegion(minX, minY, maxX - minX, maxY - minY);
    }

    @NotNull
    protected TileGridSummary buildTileGridSummary(int tileSize) {
        int tileCountX = (this.width + tileSize - 1) / tileSize;
        int tileCountY = (this.height + tileSize - 1) / tileSize;
        TileStats[] tileStatsByIndex = new TileStats[tileCountX * tileCountY];

        int tileIndex = 0;
        for (int tileY = 0; tileY < tileCountY; tileY++) {
            int startY = tileY * tileSize;
            int tileHeight = Math.min(tileSize, this.height - startY);
            for (int tileX = 0; tileX < tileCountX; tileX++, tileIndex++) {
                int startX = tileX * tileSize;
                int tileWidth = Math.min(tileSize, this.width - startX);
                long previousHash = TILE_HASH_OFFSET_BASIS_FANCYMENU;
                long nextHash = TILE_HASH_OFFSET_BASIS_FANCYMENU;
                int changedPixelCount = 0;
                long colorErrorSum = 0L;
                long alphaErrorSum = 0L;
                int maxVisibleColorDelta = 0;
                int maxAlphaDelta = 0;
                boolean nextHasNonOpaquePixels = false;

                for (int localY = 0; localY < tileHeight; localY++) {
                    int rowOffset = ((startY + localY) * this.width) + startX;
                    for (int localX = 0; localX < tileWidth; localX++) {
                        int pixelIndex = rowOffset + localX;
                        int previousColor = this.previousPixels[pixelIndex];
                        int nextColor = this.nextPixels[pixelIndex];
                        previousHash = mixTileHash(previousHash, previousColor);
                        nextHash = mixTileHash(nextHash, nextColor);
                        if (previousColor != nextColor) {
                            changedPixelCount++;
                        }

                        int previousAlpha = (previousColor >>> 24) & 0xFF;
                        int nextAlpha = (nextColor >>> 24) & 0xFF;
                        int alphaDelta = Math.abs(previousAlpha - nextAlpha);
                        alphaErrorSum += alphaDelta;
                        if (alphaDelta > maxAlphaDelta) {
                            maxAlphaDelta = alphaDelta;
                        }
                        if (nextAlpha != 0xFF) {
                            nextHasNonOpaquePixels = true;
                        }

                        int redDelta = channelDifference(previousColor >> 16, nextColor >> 16);
                        int greenDelta = channelDifference(previousColor >> 8, nextColor >> 8);
                        int blueDelta = channelDifference(previousColor, nextColor);
                        colorErrorSum += redDelta + greenDelta + blueDelta;
                        if (Math.max(previousAlpha, nextAlpha) > 0) {
                            int visibleColorDelta = Math.max(redDelta, Math.max(greenDelta, blueDelta));
                            if (visibleColorDelta > maxVisibleColorDelta) {
                                maxVisibleColorDelta = visibleColorDelta;
                            }
                        }
                    }
                }

                tileStatsByIndex[tileIndex] = new TileStats(
                        previousHash,
                        nextHash,
                        changedPixelCount,
                        colorErrorSum,
                        alphaErrorSum,
                        maxVisibleColorDelta,
                        maxAlphaDelta,
                        nextHasNonOpaquePixels
                );
            }
        }

        return new TileGridSummary(tileSize, tileCountX, tileCountY, tileStatsByIndex);
    }

    protected static long mixTileHash(long hash, int color) {
        hash ^= Integer.toUnsignedLong(color);
        hash *= TILE_HASH_PRIME_FANCYMENU;
        hash ^= (hash >>> 32);
        return hash;
    }

    public record PerceptualDriftMetrics(double averageError, int maxVisibleColorDelta, int maxAlphaDelta) {
    }

    public record RegionErrorMetrics(int changedPixelCount, long colorErrorSum, long alphaErrorSum,
                                     boolean nextHasNonOpaquePixels) {
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

    protected record ClippedRegion(int x, int y, int width, int height) {

        public int right() {
            return this.x + this.width;
        }

        public int bottom() {
            return this.y + this.height;
        }

    }

    protected record SharedAnalysisIndex(boolean identical, int changedPixelCount,
                                         @Nullable AfmaRect differenceBounds,
                                         @NotNull int[] differenceRowFirstDirtyX,
                                         @NotNull int[] differenceRowLastDirtyX,
                                         @NotNull int[] dirtyPrefixSum,
                                         @NotNull int[] nextNonOpaquePrefixSum,
                                         @NotNull long[] colorErrorPrefixSum,
                                         @NotNull long[] alphaErrorPrefixSum,
                                         @NotNull PerceptualDriftMetrics perceptualDriftMetrics) {
    }

    public static final class TileGridSummary {

        protected final int tileSize;
        protected final int tileCountX;
        protected final int tileCountY;
        @NotNull
        protected final TileStats[] tileStatsByIndex;

        protected TileGridSummary(int tileSize, int tileCountX, int tileCountY, @NotNull TileStats[] tileStatsByIndex) {
            this.tileSize = tileSize;
            this.tileCountX = tileCountX;
            this.tileCountY = tileCountY;
            this.tileStatsByIndex = tileStatsByIndex;
        }

        public int tileSize() {
            return this.tileSize;
        }

        public int tileCountX() {
            return this.tileCountX;
        }

        public int tileCountY() {
            return this.tileCountY;
        }

        @NotNull
        public TileStats tileStats(int tileX, int tileY) {
            if ((tileX < 0) || (tileY < 0) || (tileX >= this.tileCountX) || (tileY >= this.tileCountY)) {
                throw new IndexOutOfBoundsException("AFMA tile coordinates out of bounds");
            }
            return this.tileStatsByIndex[(tileY * this.tileCountX) + tileX];
        }

    }

    public record TileStats(long previousHash, long nextHash, int changedPixelCount, long colorErrorSum,
                            long alphaErrorSum, int maxVisibleColorDelta, int maxAlphaDelta,
                            boolean nextHasNonOpaquePixels) {

        public boolean isIdentical() {
            return this.changedPixelCount <= 0;
        }

    }

}
