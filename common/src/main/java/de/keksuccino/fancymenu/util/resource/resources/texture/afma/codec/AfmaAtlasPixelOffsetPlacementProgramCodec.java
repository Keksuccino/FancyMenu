package de.keksuccino.fancymenu.util.resource.resources.texture.afma.codec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Native AFMA animation codec based on atlas reuse, regional placement programs,
 * sparse patches, and transform fallbacks.
 *
 * <p>The codec models an animation as:
 * <ul>
 *     <li>a base tile field that explains the most common state of every spatial position,</li>
 *     <li>a small set of regularized atlases that capture repeated non-local tile appearances,</li>
 *     <li>time-pattern dictionaries so positions with the same activation schedule share timing metadata,</li>
 *     <li>placement programs that collapse repeated atlas-window regions into one regional program,</li>
 *     <li>and sparse patch/transform fallbacks for the remaining local exceptions.</li>
 * </ul>
 *
 * <p>This keeps the on-disk format exact while moving AFMA away from entry-per-payload ZIP packing.
 */
public final class AfmaAtlasPixelOffsetPlacementProgramCodec implements AfmaAnimationCodec {

    private static final int MAGIC = 0x41504F31; // APO1
    private static final int VERSION = 3;
    private static final int PATCH_TRANSFORM_MAX_COLORS = 4;
    private static final double RUN_VALUE = 6.0D;
    private static final double OCCURRENCE_VALUE = 0.25D;

    private final int tileSize;
    private final int maxAtlasCount;
    private final int minRepeatCount;
    private final int minScheduleRepeatCount;
    private final int regularizedTileBudget;
    private final int localCandidateLimit;
    private final int maxPatchPixels;
    private final int maxRemapColors;
    private final int minPlacementArea;

    /**
     * Returns the production-tuned codec configuration used by AFMA files.
     */
    public static AfmaAtlasPixelOffsetPlacementProgramCodec production() {
        return new AfmaAtlasPixelOffsetPlacementProgramCodec(8, 3, 4, 4, 8, 2, 6, 0, 4);
    }

    public AfmaAtlasPixelOffsetPlacementProgramCodec(int tileSize, int maxAtlasCount, int minRepeatCount,
                                                 int minScheduleRepeatCount, int regularizedTileBudget, int localCandidateLimit) {
        this(tileSize, maxAtlasCount, minRepeatCount, minScheduleRepeatCount, regularizedTileBudget, localCandidateLimit, 6, 0, 4);
    }

    public AfmaAtlasPixelOffsetPlacementProgramCodec(int tileSize, int maxAtlasCount, int minRepeatCount,
                                                 int minScheduleRepeatCount, int regularizedTileBudget,
                                                 int localCandidateLimit, int maxPatchPixels) {
        this(tileSize, maxAtlasCount, minRepeatCount, minScheduleRepeatCount, regularizedTileBudget, localCandidateLimit, maxPatchPixels, 0, 4);
    }

    public AfmaAtlasPixelOffsetPlacementProgramCodec(int tileSize, int maxAtlasCount, int minRepeatCount,
                                                 int minScheduleRepeatCount, int regularizedTileBudget,
                                                 int localCandidateLimit, int maxPatchPixels, int maxRemapColors,
                                                 int minPlacementArea) {
        if (tileSize < 2) {
            throw new IllegalArgumentException("Tile size is invalid: " + tileSize);
        }
        if (maxAtlasCount < 1) {
            throw new IllegalArgumentException("Atlas count is invalid: " + maxAtlasCount);
        }
        if (minRepeatCount < 2) {
            throw new IllegalArgumentException("Min repeat count is invalid: " + minRepeatCount);
        }
        if (minScheduleRepeatCount < 2) {
            throw new IllegalArgumentException("Min schedule repeat count is invalid: " + minScheduleRepeatCount);
        }
        if (regularizedTileBudget < 1) {
            throw new IllegalArgumentException("Regularized tile budget is invalid: " + regularizedTileBudget);
        }
        if (localCandidateLimit < 1) {
            throw new IllegalArgumentException("Local candidate limit is invalid: " + localCandidateLimit);
        }
        if (maxPatchPixels < 1) {
            throw new IllegalArgumentException("Max patch pixels is invalid: " + maxPatchPixels);
        }
        if (maxRemapColors < 0) {
            throw new IllegalArgumentException("Max remap colors is invalid: " + maxRemapColors);
        }
        if (minPlacementArea < 2) {
            throw new IllegalArgumentException("Min placement area is invalid: " + minPlacementArea);
        }
        this.tileSize = tileSize;
        this.maxAtlasCount = maxAtlasCount;
        this.minRepeatCount = minRepeatCount;
        this.minScheduleRepeatCount = minScheduleRepeatCount;
        this.regularizedTileBudget = regularizedTileBudget;
        this.localCandidateLimit = localCandidateLimit;
        this.maxPatchPixels = maxPatchPixels;
        this.maxRemapColors = maxRemapColors;
        this.minPlacementArea = minPlacementArea;
    }

    @Override
    public String name() {
        return "atlas-pixel-offset-placement-program-s" + this.tileSize
                + "-a" + this.maxAtlasCount
                + "-r" + this.minRepeatCount
                + "-sch" + this.minScheduleRepeatCount
                + "-b" + this.regularizedTileBudget
                + "-k" + this.localCandidateLimit
                + "-p" + this.maxPatchPixels
                + "-m" + this.maxRemapColors
                + "-pa" + this.minPlacementArea
                + "-pt" + PATCH_TRANSFORM_MAX_COLORS
                + "-v3";
    }

    public static WindowStats analyze(AfmaDecodedAnimation animation, int tileSize, int maxAtlasCount,
                                      int regularizedTileBudget, int localCandidateLimit) throws IOException {
        PreparedData prepared = prepare(animation, tileSize);
        TileBlock[][] atlasTiles = buildRegularizedAtlases(
                prepared.tilesByPositionFrame,
                prepared.baseTiles,
                prepared.totalFrames,
                prepared.columns,
                prepared.rows,
                maxAtlasCount,
                regularizedTileBudget,
                localCandidateLimit,
                new LinkedHashMap<>()
        );

        boolean[][] sameUncovered = initialUncovered(prepared.tilesByPositionFrame, prepared.baseTiles, prepared.positionCount, prepared.totalFrames);
        boolean[][] windowUncovered = initialUncovered(prepared.tilesByPositionFrame, prepared.baseTiles, prepared.positionCount, prepared.totalFrames);
        long[] sameCoveredAfterStep = new long[Math.max(1, atlasTiles.length)];
        long[] windowCoveredAfterStep = new long[Math.max(1, atlasTiles.length)];
        long[] windowOnlyCoveredAfterStep = new long[Math.max(1, atlasTiles.length)];
        int[] uniqueAtlasTiles = new int[Math.max(1, atlasTiles.length)];
        int[] leftEqualCounts = new int[Math.max(1, atlasTiles.length)];
        long sameCovered = prepared.baseCoveredOccurrences;
        long windowCovered = prepared.baseCoveredOccurrences;

        for (int atlasIndex = 0; atlasIndex < atlasTiles.length; atlasIndex++) {
            TileBlock[] atlas = atlasTiles[atlasIndex];
            LinkedHashMap<TileBlock, Integer> atlasLookup = new LinkedHashMap<>();
            int leftEqualCount = 0;
            for (int row = 0; row < prepared.rows; row++) {
                TileBlock leftTile = null;
                for (int column = 0; column < prepared.columns; column++) {
                    int positionIndex = row * prepared.columns + column;
                    TileBlock tile = atlas[positionIndex];
                    atlasLookup.putIfAbsent(tile, positionIndex);
                    if (leftTile != null && leftTile.equals(tile)) {
                        leftEqualCount++;
                    }
                    leftTile = tile;
                }
            }

            for (int positionIndex = 0; positionIndex < prepared.positionCount; positionIndex++) {
                TileBlock sameTile = atlas[positionIndex];
                for (int frameIndex = 0; frameIndex < prepared.totalFrames; frameIndex++) {
                    TileBlock tile = prepared.tilesByPositionFrame[positionIndex][frameIndex];
                    if (sameUncovered[positionIndex][frameIndex] && sameTile.equals(tile)) {
                        sameUncovered[positionIndex][frameIndex] = false;
                        sameCovered++;
                    }
                    if (windowUncovered[positionIndex][frameIndex] && atlasLookup.containsKey(tile)) {
                        windowUncovered[positionIndex][frameIndex] = false;
                        windowCovered++;
                    }
                }
            }

            sameCoveredAfterStep[atlasIndex] = sameCovered;
            windowCoveredAfterStep[atlasIndex] = windowCovered;
            windowOnlyCoveredAfterStep[atlasIndex] = windowCovered - sameCovered;
            uniqueAtlasTiles[atlasIndex] = atlasLookup.size();
            leftEqualCounts[atlasIndex] = leftEqualCount;
        }

        return new WindowStats(
                tileSize,
                prepared.columns,
                prepared.rows,
                prepared.totalFrames,
                prepared.positionCount,
                prepared.totalOccurrences,
                prepared.baseCoveredOccurrences,
                atlasTiles.length,
                sameCoveredAfterStep,
                windowCoveredAfterStep,
                windowOnlyCoveredAfterStep,
                uniqueAtlasTiles,
                leftEqualCounts
        );
    }

    public static TailStats analyzeTail(AfmaDecodedAnimation animation, int tileSize, int maxAtlasCount,
                                        int regularizedTileBudget, int localCandidateLimit,
                                        int maxPatchPixels) throws IOException {
        PreparedData prepared = prepare(animation, tileSize);
        LinkedHashMap<TileBlock, Integer> payloadCostCache = new LinkedHashMap<>();
        TileBlock[][] atlasTiles = buildRegularizedAtlases(
                prepared.tilesByPositionFrame,
                prepared.baseTiles,
                prepared.totalFrames,
                prepared.columns,
                prepared.rows,
                maxAtlasCount,
                regularizedTileBudget,
                localCandidateLimit,
                payloadCostCache
        );

        @SuppressWarnings("unchecked")
        LinkedHashMap<TileBlock, Integer>[] atlasLookups = new LinkedHashMap[atlasTiles.length];
        for (int atlasIndex = 0; atlasIndex < atlasTiles.length; atlasIndex++) {
            LinkedHashMap<TileBlock, Integer> atlasLookup = new LinkedHashMap<>();
            for (int positionIndex = 0; positionIndex < atlasTiles[atlasIndex].length; positionIndex++) {
                atlasLookup.putIfAbsent(atlasTiles[atlasIndex][positionIndex], positionIndex);
            }
            atlasLookups[atlasIndex] = atlasLookup;
        }

        @SuppressWarnings("unchecked")
        List<PatchSource>[] patchSourcesByPosition = new List[prepared.positionCount];
        for (int positionIndex = 0; positionIndex < prepared.positionCount; positionIndex++) {
            patchSourcesByPosition[positionIndex] = buildPatchSources(positionIndex, prepared.baseTiles, atlasTiles);
        }

        long residualOccurrences = prepared.totalOccurrences - prepared.baseCoveredOccurrences;
        long exactWindowCoveredOccurrences = 0L;
        long patchCoveredOccurrences = 0L;
        long basePatchOccurrences = 0L;
        long samePositionPatchOccurrences = 0L;
        long windowPatchOccurrences = 0L;
        long changedPixelsTotal = 0L;
        long[] patchableAtMost = new long[maxPatchPixels];
        for (int positionIndex = 0; positionIndex < prepared.positionCount; positionIndex++) {
            TileBlock baseTile = prepared.baseTiles[positionIndex];
            int tileWidth = Math.min(tileSize, prepared.width - (positionIndex % prepared.columns) * tileSize);
            int tileHeight = Math.min(tileSize, prepared.height - (positionIndex / prepared.columns) * tileSize);
            for (int frameIndex = 0; frameIndex < prepared.totalFrames; frameIndex++) {
                TileBlock tile = prepared.tilesByPositionFrame[positionIndex][frameIndex];
                if (tile.equals(baseTile)) {
                    continue;
                }
                AtlasMatch exactMatch = findAtlasMatch(tile, positionIndex, atlasTiles, atlasLookups);
                if (exactMatch != null) {
                    exactWindowCoveredOccurrences++;
                    continue;
                }

                PatchMatch patchMatch = findBestPatchMatch(
                        tile,
                        patchSourcesByPosition[positionIndex],
                        payloadCostCache,
                        new LinkedHashMap<>(),
                        Map.of(),
                        new LinkedHashMap<>(),
                        maxPatchPixels
                );
                if (patchMatch == null) {
                    continue;
                }
                patchCoveredOccurrences++;
                changedPixelsTotal += patchMatch.patch.changedPixelCount();
                for (int threshold = patchMatch.patch.changedPixelCount(); threshold <= maxPatchPixels; threshold++) {
                    patchableAtMost[threshold - 1]++;
                }
                switch (patchMatch.source.type) {
                    case BASE -> basePatchOccurrences++;
                    case SAME_POSITION_ATLAS -> samePositionPatchOccurrences++;
                    case WINDOW_ATLAS -> windowPatchOccurrences++;
                }
            }
        }

        return new TailStats(
                tileSize,
                prepared.positionCount,
                prepared.totalOccurrences,
                residualOccurrences,
                exactWindowCoveredOccurrences,
                patchCoveredOccurrences,
                basePatchOccurrences,
                samePositionPatchOccurrences,
                windowPatchOccurrences,
                patchableAtMost,
                changedPixelsTotal
        );
    }

    public static TransformStats analyzeTransforms(AfmaDecodedAnimation animation, int tileSize, int maxAtlasCount,
                                                   int regularizedTileBudget, int localCandidateLimit,
                                                   int maxPatchPixels, int maxRemapColors) throws IOException {
        PreparedData prepared = prepare(animation, tileSize);
        LinkedHashMap<TileBlock, Integer> payloadCostCache = new LinkedHashMap<>();
        TileBlock[][] atlasTiles = buildRegularizedAtlases(
                prepared.tilesByPositionFrame,
                prepared.baseTiles,
                prepared.totalFrames,
                prepared.columns,
                prepared.rows,
                maxAtlasCount,
                regularizedTileBudget,
                localCandidateLimit,
                payloadCostCache
        );

        @SuppressWarnings("unchecked")
        LinkedHashMap<TileBlock, Integer>[] atlasLookups = new LinkedHashMap[atlasTiles.length];
        for (int atlasIndex = 0; atlasIndex < atlasTiles.length; atlasIndex++) {
            LinkedHashMap<TileBlock, Integer> atlasLookup = new LinkedHashMap<>();
            for (int positionIndex = 0; positionIndex < atlasTiles[atlasIndex].length; positionIndex++) {
                atlasLookup.putIfAbsent(atlasTiles[atlasIndex][positionIndex], positionIndex);
            }
            atlasLookups[atlasIndex] = atlasLookup;
        }

        @SuppressWarnings("unchecked")
        List<PatchSource>[] patchSourcesByPosition = new List[prepared.positionCount];
        for (int positionIndex = 0; positionIndex < prepared.positionCount; positionIndex++) {
            patchSourcesByPosition[positionIndex] = buildPatchSources(positionIndex, prepared.baseTiles, atlasTiles);
        }

        long residualOccurrences = prepared.totalOccurrences - prepared.baseCoveredOccurrences;
        long exactWindowCoveredOccurrences = 0L;
        long patchCoveredOccurrences = 0L;
        long transformCoveredOccurrences = 0L;
        long argbDeltaOccurrences = 0L;
        long paletteRemapOccurrences = 0L;
        long baseTransformOccurrences = 0L;
        long samePositionTransformOccurrences = 0L;
        long windowTransformOccurrences = 0L;
        long remapColorsTotal = 0L;
        LinkedHashMap<TileBlock, int[]> paletteCache = new LinkedHashMap<>();
        LinkedHashMap<RemapKey, Integer> remapCostCache = new LinkedHashMap<>();
        for (int positionIndex = 0; positionIndex < prepared.positionCount; positionIndex++) {
            TileBlock baseTile = prepared.baseTiles[positionIndex];
            List<PatchSource> patchSources = patchSourcesByPosition[positionIndex];
            for (int frameIndex = 0; frameIndex < prepared.totalFrames; frameIndex++) {
                TileBlock tile = prepared.tilesByPositionFrame[positionIndex][frameIndex];
                if (tile.equals(baseTile)) {
                    continue;
                }
                AtlasMatch exactMatch = findAtlasMatch(tile, positionIndex, atlasTiles, atlasLookups);
                if (exactMatch != null) {
                    exactWindowCoveredOccurrences++;
                    continue;
                }

                PatchMatch patchMatch = findBestPatchMatch(
                        tile,
                        patchSources,
                        payloadCostCache,
                        new LinkedHashMap<>(),
                        Map.of(),
                        new LinkedHashMap<>(),
                        maxPatchPixels
                );
                if (patchMatch != null) {
                    patchCoveredOccurrences++;
                    continue;
                }

                TransformMatch transformMatch = findBestTransformMatch(
                        tile,
                        patchSources,
                        payloadCostCache,
                        new LinkedHashMap<>(),
                        remapCostCache,
                        new LinkedHashMap<>(),
                        maxRemapColors,
                        paletteCache
                );
                if (transformMatch == null) {
                    continue;
                }
                transformCoveredOccurrences++;
                switch (transformMatch.transform.mode) {
                    case ARGB_DELTA -> argbDeltaOccurrences++;
                    case PALETTE_REMAP -> {
                        paletteRemapOccurrences++;
                        remapColorsTotal += transformMatch.transform.remapColors.length;
                    }
                }
                switch (transformMatch.source.type) {
                    case BASE -> baseTransformOccurrences++;
                    case SAME_POSITION_ATLAS -> samePositionTransformOccurrences++;
                    case WINDOW_ATLAS -> windowTransformOccurrences++;
                }
            }
        }

        return new TransformStats(
                tileSize,
                prepared.positionCount,
                prepared.totalOccurrences,
                residualOccurrences,
                exactWindowCoveredOccurrences,
                patchCoveredOccurrences,
                transformCoveredOccurrences,
                argbDeltaOccurrences,
                paletteRemapOccurrences,
                baseTransformOccurrences,
                samePositionTransformOccurrences,
                windowTransformOccurrences,
                remapColorsTotal
        );
    }

    public static TransformPatternStats analyzeTransformPatterns(AfmaDecodedAnimation animation, int tileSize, int maxAtlasCount,
                                                                 int regularizedTileBudget, int localCandidateLimit,
                                                                 int maxPatchPixels, int maxRemapColors) throws IOException {
        PreparedData prepared = prepare(animation, tileSize);
        LinkedHashMap<TileBlock, Integer> payloadCostCache = new LinkedHashMap<>();
        TileBlock[][] atlasTiles = buildRegularizedAtlases(
                prepared.tilesByPositionFrame,
                prepared.baseTiles,
                prepared.totalFrames,
                prepared.columns,
                prepared.rows,
                maxAtlasCount,
                regularizedTileBudget,
                localCandidateLimit,
                payloadCostCache
        );

        @SuppressWarnings("unchecked")
        LinkedHashMap<TileBlock, Integer>[] atlasLookups = new LinkedHashMap[atlasTiles.length];
        for (int atlasIndex = 0; atlasIndex < atlasTiles.length; atlasIndex++) {
            LinkedHashMap<TileBlock, Integer> atlasLookup = new LinkedHashMap<>();
            for (int positionIndex = 0; positionIndex < atlasTiles[atlasIndex].length; positionIndex++) {
                atlasLookup.putIfAbsent(atlasTiles[atlasIndex][positionIndex], positionIndex);
            }
            atlasLookups[atlasIndex] = atlasLookup;
        }

        @SuppressWarnings("unchecked")
        List<PatchSource>[] patchSourcesByPosition = new List[prepared.positionCount];
        for (int positionIndex = 0; positionIndex < prepared.positionCount; positionIndex++) {
            patchSourcesByPosition[positionIndex] = buildPatchSources(positionIndex, prepared.baseTiles, atlasTiles);
        }

        LinkedHashMap<TileBlock, int[]> paletteCache = new LinkedHashMap<>();
        LinkedHashMap<RemapKey, Integer> remapCostCache = new LinkedHashMap<>();
        LinkedHashMap<TileTransform, Integer> argbDeltaCounts = new LinkedHashMap<>();
        LinkedHashMap<TileTransform, Integer> paletteRemapCounts = new LinkedHashMap<>();
        long transformOccurrences = 0L;
        for (int positionIndex = 0; positionIndex < prepared.positionCount; positionIndex++) {
            TileBlock baseTile = prepared.baseTiles[positionIndex];
            List<PatchSource> patchSources = patchSourcesByPosition[positionIndex];
            for (int frameIndex = 0; frameIndex < prepared.totalFrames; frameIndex++) {
                TileBlock tile = prepared.tilesByPositionFrame[positionIndex][frameIndex];
                if (tile.equals(baseTile)) {
                    continue;
                }
                if (findAtlasMatch(tile, positionIndex, atlasTiles, atlasLookups) != null) {
                    continue;
                }
                if (findBestPatchMatch(tile, patchSources, payloadCostCache, new LinkedHashMap<>(), Map.of(), new LinkedHashMap<>(), maxPatchPixels) != null) {
                    continue;
                }
                TransformMatch transformMatch = findBestTransformMatch(
                        tile,
                        patchSources,
                        payloadCostCache,
                        new LinkedHashMap<>(),
                        remapCostCache,
                        new LinkedHashMap<>(),
                        maxRemapColors,
                        paletteCache
                );
                if (transformMatch == null) {
                    continue;
                }
                transformOccurrences++;
                if (transformMatch.transform.mode == TransformMode.ARGB_DELTA) {
                    argbDeltaCounts.merge(transformMatch.transform, 1, Integer::sum);
                } else {
                    paletteRemapCounts.merge(transformMatch.transform, 1, Integer::sum);
                }
            }
        }

        return new TransformPatternStats(
                tileSize,
                transformOccurrences,
                argbDeltaCounts.size(),
                repeatedOccurrenceCount(argbDeltaCounts),
                paletteRemapCounts.size(),
                repeatedOccurrenceCount(paletteRemapCounts),
                maxFrequency(argbDeltaCounts),
                maxFrequency(paletteRemapCounts)
        );
    }

    public static PatchPatternStats analyzePatchPatterns(AfmaDecodedAnimation animation, int tileSize, int maxAtlasCount,
                                                         int regularizedTileBudget, int localCandidateLimit,
                                                         int maxPatchPixels) throws IOException {
        PreparedData prepared = prepare(animation, tileSize);
        LinkedHashMap<TileBlock, Integer> payloadCostCache = new LinkedHashMap<>();
        TileBlock[][] atlasTiles = buildRegularizedAtlases(
                prepared.tilesByPositionFrame,
                prepared.baseTiles,
                prepared.totalFrames,
                prepared.columns,
                prepared.rows,
                maxAtlasCount,
                regularizedTileBudget,
                localCandidateLimit,
                payloadCostCache
        );

        @SuppressWarnings("unchecked")
        LinkedHashMap<TileBlock, Integer>[] atlasLookups = new LinkedHashMap[atlasTiles.length];
        for (int atlasIndex = 0; atlasIndex < atlasTiles.length; atlasIndex++) {
            LinkedHashMap<TileBlock, Integer> atlasLookup = new LinkedHashMap<>();
            for (int positionIndex = 0; positionIndex < atlasTiles[atlasIndex].length; positionIndex++) {
                atlasLookup.putIfAbsent(atlasTiles[atlasIndex][positionIndex], positionIndex);
            }
            atlasLookups[atlasIndex] = atlasLookup;
        }

        @SuppressWarnings("unchecked")
        List<PatchSource>[] patchSourcesByPosition = new List[prepared.positionCount];
        for (int positionIndex = 0; positionIndex < prepared.positionCount; positionIndex++) {
            patchSourcesByPosition[positionIndex] = buildPatchSources(positionIndex, prepared.baseTiles, atlasTiles);
        }

        LinkedHashMap<SparsePatchKey, Integer> exactPatchCounts = new LinkedHashMap<>();
        LinkedHashMap<PatchShapeKey, Integer> shapeCounts = new LinkedHashMap<>();
        long patchOccurrences = 0L;
        long changedPixelsTotal = 0L;
        for (int positionIndex = 0; positionIndex < prepared.positionCount; positionIndex++) {
            TileBlock baseTile = prepared.baseTiles[positionIndex];
            int tileWidth = Math.min(tileSize, prepared.width - (positionIndex % prepared.columns) * tileSize);
            int tileHeight = Math.min(tileSize, prepared.height - (positionIndex / prepared.columns) * tileSize);
            for (int frameIndex = 0; frameIndex < prepared.totalFrames; frameIndex++) {
                TileBlock tile = prepared.tilesByPositionFrame[positionIndex][frameIndex];
                if (tile.equals(baseTile)) {
                    continue;
                }
                AtlasMatch exactMatch = findAtlasMatch(tile, positionIndex, atlasTiles, atlasLookups);
                if (exactMatch != null) {
                    continue;
                }

                PatchMatch patchMatch = findBestPatchMatch(
                        tile,
                        patchSourcesByPosition[positionIndex],
                        payloadCostCache,
                        new LinkedHashMap<>(),
                        Map.of(),
                        new LinkedHashMap<>(),
                        maxPatchPixels
                );
                if (patchMatch == null) {
                    continue;
                }
                patchOccurrences++;
                changedPixelsTotal += patchMatch.patch.changedPixelCount();
                exactPatchCounts.merge(SparsePatchKey.from(patchMatch.patch), 1, Integer::sum);
                shapeCounts.merge(PatchShapeKey.from(patchMatch.patch, tileWidth, tileHeight), 1, Integer::sum);
            }
        }

        return new PatchPatternStats(
                tileSize,
                patchOccurrences,
                exactPatchCounts.size(),
                repeatedOccurrenceCount(exactPatchCounts),
                shapeCounts.size(),
                repeatedOccurrenceCount(shapeCounts),
                maxFrequency(exactPatchCounts),
                maxFrequency(shapeCounts),
                changedPixelsTotal
        );
    }

    public static RepairProgramStats analyzeRepairPrograms(AfmaDecodedAnimation animation, int tileSize, int maxAtlasCount,
                                                           int regularizedTileBudget, int localCandidateLimit,
                                                           int maxPatchPixels, int minPlacementArea) throws IOException {
        PreparedData prepared = prepare(animation, tileSize);
        LinkedHashMap<TileBlock, Integer> payloadCostCache = new LinkedHashMap<>();
        TileBlock[][] atlasTiles = buildRegularizedAtlases(
                prepared.tilesByPositionFrame,
                prepared.baseTiles,
                prepared.totalFrames,
                prepared.columns,
                prepared.rows,
                maxAtlasCount,
                regularizedTileBudget,
                localCandidateLimit,
                payloadCostCache
        );

        @SuppressWarnings("unchecked")
        LinkedHashMap<TileBlock, Integer>[] atlasLookups = new LinkedHashMap[atlasTiles.length];
        LinkedHashMap<TileBlock, Integer> structuralTileFrequencies = new LinkedHashMap<>();
        for (TileBlock baseTile : prepared.baseTiles) {
            structuralTileFrequencies.merge(baseTile, 1, Integer::sum);
        }
        for (int atlasIndex = 0; atlasIndex < atlasTiles.length; atlasIndex++) {
            LinkedHashMap<TileBlock, Integer> atlasLookup = new LinkedHashMap<>();
            for (int positionIndex = 0; positionIndex < atlasTiles[atlasIndex].length; positionIndex++) {
                TileBlock tile = atlasTiles[atlasIndex][positionIndex];
                structuralTileFrequencies.merge(tile, 1, Integer::sum);
                atlasLookup.putIfAbsent(tile, positionIndex);
            }
            atlasLookups[atlasIndex] = atlasLookup;
        }
        AtlasImage[] atlasImages = buildAtlasImages(atlasTiles, prepared.width, prepared.height, tileSize, prepared.columns, prepared.rows);

        @SuppressWarnings("unchecked")
        List<PatchSource>[] patchSourcesByPosition = new List[prepared.positionCount];
        for (int positionIndex = 0; positionIndex < prepared.positionCount; positionIndex++) {
            patchSourcesByPosition[positionIndex] = buildPatchSources(positionIndex, prepared.baseTiles, atlasTiles);
        }
        LinkedHashMap<TileBlock, int[]> paletteCache = new LinkedHashMap<>();
        LinkedHashMap<RemapKey, Integer> remapCostCache = new LinkedHashMap<>();

        LinkedHashMap<TileBlock, Integer> tentativeLiteralFrequencies = new LinkedHashMap<>();
        @SuppressWarnings("unchecked")
        LinkedHashMap<TileBlock, EventState>[] stateCacheByPosition = new LinkedHashMap[prepared.positionCount];
        for (int positionIndex = 0; positionIndex < prepared.positionCount; positionIndex++) {
            stateCacheByPosition[positionIndex] = new LinkedHashMap<>();
            TileBlock baseTile = prepared.baseTiles[positionIndex];
            for (int frameIndex = 0; frameIndex < prepared.totalFrames; frameIndex++) {
                TileBlock tile = prepared.tilesByPositionFrame[positionIndex][frameIndex];
                if (tile.equals(baseTile)) {
                    continue;
                }
                AtlasMatch exactMatch = findAtlasMatch(tile, positionIndex, atlasTiles, atlasLookups);
                if (exactMatch == null && findHorizontalSamplerMatch(tile, positionIndex, atlasImages, prepared.width, tileSize, prepared.columns) == null) {
                    tentativeLiteralFrequencies.merge(tile, 1, Integer::sum);
                }
            }
        }

        LinkedHashMap<TileBlock, Integer> tentativeTileFrequencies = new LinkedHashMap<>(structuralTileFrequencies);
        for (Map.Entry<TileBlock, Integer> entry : tentativeLiteralFrequencies.entrySet()) {
            tentativeTileFrequencies.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
        LinkedHashMap<TileBlock, Integer> tentativeTileDictionaryIds = buildTileDictionary(tentativeTileFrequencies, 4);

        LinkedHashMap<TileTransform, Integer> provisionalTransformFrequencies = new LinkedHashMap<>();
        LinkedHashMap<PatchShapeKey, Integer> provisionalPatchShapeFrequencies = new LinkedHashMap<>();
        @SuppressWarnings("unchecked")
        LinkedHashMap<TileBlock, EventState>[] provisionalStateCacheByPosition = new LinkedHashMap[prepared.positionCount];
        for (int positionIndex = 0; positionIndex < prepared.positionCount; positionIndex++) {
            provisionalStateCacheByPosition[positionIndex] = new LinkedHashMap<>();
            TileBlock baseTile = prepared.baseTiles[positionIndex];
            for (int frameIndex = 0; frameIndex < prepared.totalFrames; frameIndex++) {
                TileBlock tile = prepared.tilesByPositionFrame[positionIndex][frameIndex];
                if (tile.equals(baseTile)) {
                    continue;
                }
                EventState cached = provisionalStateCacheByPosition[positionIndex].get(tile);
                if (cached == null) {
                    AtlasMatch exactMatch = findAtlasMatch(tile, positionIndex, atlasTiles, atlasLookups);
                    if (exactMatch != null) {
                        cached = exactMatch.samePosition
                                ? EventState.samePositionAtlas(exactMatch.atlasIndex)
                                : EventState.windowAtlas(exactMatch.atlasIndex, exactMatch.sourcePosition);
                    } else {
                        HorizontalSamplerMatch samplerMatch = findHorizontalSamplerMatch(tile, positionIndex, atlasImages, prepared.width, tileSize, prepared.columns);
                        if (samplerMatch != null) {
                            cached = EventState.horizontalSampler(samplerMatch.atlasIndex, samplerMatch.deltaX);
                        } else {
                            cached = resolveEventState(
                                    tile,
                                    tentativeTileDictionaryIds,
                                    patchSourcesByPosition[positionIndex],
                                    payloadCostCache,
                                    remapCostCache,
                                    paletteCache,
                                    new LinkedHashMap<>(),
                                    Map.of(),
                                    maxPatchPixels,
                                    0
                            );
                        }
                    }
                    provisionalStateCacheByPosition[positionIndex].put(tile, cached);
                }
                if (cached.patch != null) {
                    provisionalPatchShapeFrequencies.merge(PatchShapeKey.from(cached.patch, tile.width, tile.height), 1, Integer::sum);
                }
                if (cached.transform != null) {
                    provisionalTransformFrequencies.merge(cached.transform, 1, Integer::sum);
                }
            }
        }
        LinkedHashMap<TileTransform, Integer> provisionalTransformDictionaryIds = buildTransformDictionary(provisionalTransformFrequencies, 2);
        LinkedHashMap<PatchShapeKey, Integer> provisionalPatchShapeDictionaryIds = buildPatchShapeDictionary(provisionalPatchShapeFrequencies);

        ArrayList<PositionProgram> programs = new ArrayList<>(prepared.positionCount);
        for (int positionIndex = 0; positionIndex < prepared.positionCount; positionIndex++) {
            TileBlock baseTile = prepared.baseTiles[positionIndex];
            EventState[] states = new EventState[prepared.totalFrames];
            for (int frameIndex = 0; frameIndex < prepared.totalFrames; frameIndex++) {
                TileBlock tile = prepared.tilesByPositionFrame[positionIndex][frameIndex];
                if (tile.equals(baseTile)) {
                    states[frameIndex] = null;
                    continue;
                }
                EventState cached = stateCacheByPosition[positionIndex].get(tile);
                if (cached == null) {
                    AtlasMatch exactMatch = findAtlasMatch(tile, positionIndex, atlasTiles, atlasLookups);
                    if (exactMatch != null) {
                        cached = exactMatch.samePosition
                                ? EventState.samePositionAtlas(exactMatch.atlasIndex)
                                : EventState.windowAtlas(exactMatch.atlasIndex, exactMatch.sourcePosition);
                    } else {
                        HorizontalSamplerMatch samplerMatch = findHorizontalSamplerMatch(tile, positionIndex, atlasImages, prepared.width, tileSize, prepared.columns);
                        if (samplerMatch != null) {
                            cached = EventState.horizontalSampler(samplerMatch.atlasIndex, samplerMatch.deltaX);
                        } else {
                            cached = resolveEventState(
                                    tile,
                                    tentativeTileDictionaryIds,
                                    patchSourcesByPosition[positionIndex],
                                    payloadCostCache,
                                    remapCostCache,
                                    paletteCache,
                                    provisionalTransformDictionaryIds,
                                    provisionalPatchShapeDictionaryIds,
                                    maxPatchPixels,
                                    0
                            );
                        }
                    }
                    stateCacheByPosition[positionIndex].put(tile, cached);
                }
                states[frameIndex] = cached;
            }

            ArrayList<StateEvent> events = new ArrayList<>();
            int frameIndex = 0;
            while (frameIndex < prepared.totalFrames) {
                EventState state = states[frameIndex];
                if (state == null) {
                    frameIndex++;
                    continue;
                }
                int end = frameIndex + 1;
                while (end < prepared.totalFrames && state.equals(states[end])) {
                    end++;
                }
                events.add(new StateEvent(frameIndex, end - frameIndex, state));
                frameIndex = end;
            }
            programs.add(new PositionProgram(baseTile, events));
        }

        PlacementSelection placementSelection = selectPlacementPrograms(programs, prepared.columns, prepared.rows, minPlacementArea, tileSize);

        long patchOccurrences = 0L;
        long singleSamplerOccurrences = 0L;
        long sameSourceAtlasSingleSamplerOccurrences = 0L;
        long twoSamplerOccurrences = 0L;
        long patchArgbDeltaOccurrences = 0L;
        long patchPaletteRemapOccurrences = 0L;
        long singleSamplerAbsDxTotal = 0L;
        LinkedHashMap<RepairSamplerProgramKey, Integer> singleSamplerProgramCounts = new LinkedHashMap<>();
        for (int positionIndex = 0; positionIndex < prepared.positionCount; positionIndex++) {
            if (placementSelection.coveredPositions[positionIndex]) {
                continue;
            }
            int tileWidth = Math.min(tileSize, prepared.width - (positionIndex % prepared.columns) * tileSize);
            int tileHeight = Math.min(tileSize, prepared.height - (positionIndex / prepared.columns) * tileSize);
            PositionProgram program = programs.get(positionIndex);
            for (StateEvent event : program.events) {
                if (event.state.mode != StateMode.BASE_PATCH
                        && event.state.mode != StateMode.SAME_POSITION_PATCH
                        && event.state.mode != StateMode.WINDOW_PATCH) {
                    continue;
                }
                patchOccurrences++;
                TileBlock sourceTile = switch (event.state.mode) {
                    case BASE_PATCH -> prepared.baseTiles[positionIndex];
                    case SAME_POSITION_PATCH -> atlasTiles[event.state.atlasIndex][positionIndex];
                    case WINDOW_PATCH -> atlasTiles[event.state.atlasIndex][event.state.sourcePosition];
                    default -> throw new IllegalStateException("Unexpected patch state: " + event.state.mode);
                };
                RepairSamplerCoverage coverage = analyzeRepairSamplerCoverage(
                        event.state,
                        positionIndex,
                        atlasImages,
                        prepared.width,
                        tileSize,
                        prepared.columns,
                        tileWidth,
                        tileHeight
                );
                if (coverage.singleMatch != null) {
                    singleSamplerOccurrences++;
                    singleSamplerAbsDxTotal += Math.abs(coverage.singleMatch.deltaX);
                    boolean sameSourceAtlas = event.state.mode != StateMode.BASE_PATCH
                            && coverage.singleMatch.atlasIndex == event.state.atlasIndex;
                    if (sameSourceAtlas) {
                        sameSourceAtlasSingleSamplerOccurrences++;
                    }
                    singleSamplerProgramCounts.merge(
                            RepairSamplerProgramKey.from(event.state.patch, tileWidth, tileHeight, coverage.singleMatch.deltaX, sameSourceAtlas),
                            1,
                            Integer::sum
                    );
                }
                if (coverage.coveredByAtMostTwoSamplers) {
                    twoSamplerOccurrences++;
                }
                if (matchesSparseArgbDelta(sourceTile, event.state.patch)) {
                    patchArgbDeltaOccurrences++;
                }
                if (matchesSparsePaletteRemap(sourceTile, event.state.patch, 4)) {
                    patchPaletteRemapOccurrences++;
                }
            }
        }

        return new RepairProgramStats(
                tileSize,
                patchOccurrences,
                singleSamplerOccurrences,
                sameSourceAtlasSingleSamplerOccurrences,
                twoSamplerOccurrences,
                patchArgbDeltaOccurrences,
                patchPaletteRemapOccurrences,
                singleSamplerProgramCounts.size(),
                repeatedOccurrenceCount(singleSamplerProgramCounts),
                maxFrequency(singleSamplerProgramCounts),
                singleSamplerAbsDxTotal
        );
    }

    private static RepairSamplerCoverage analyzeRepairSamplerCoverage(EventState state, int positionIndex,
                                                                      AtlasImage[] atlasImages, int width,
                                                                      int tileSize, int columns,
                                                                      int tileWidth, int tileHeight) {
        SparsePatch patch = state.patch;
        if (patch == null || patch.indices.length <= 0) {
            return new RepairSamplerCoverage(null, false);
        }
        int positionX = positionIndex % columns;
        int positionY = positionIndex / columns;
        int tileX = positionX * tileSize;
        int tileY = positionY * tileSize;
        int minX = Math.max(0, tileX - tileSize + 1);
        int maxX = Math.min(width - tileWidth, tileX + tileSize - 1);
        long fullMask = (patch.indices.length >= Long.SIZE) ? -1L : ((1L << patch.indices.length) - 1L);

        ArrayList<RepairSamplerMatch> candidates = new ArrayList<>();
        for (int atlasIndex = 0; atlasIndex < atlasImages.length; atlasIndex++) {
            AtlasImage atlas = atlasImages[atlasIndex];
            for (int sourceX = minX; sourceX <= maxX; sourceX++) {
                if (sourceX == tileX) {
                    continue;
                }
                long matchedMask = 0L;
                for (int changeIndex = 0; changeIndex < patch.indices.length; changeIndex++) {
                    int pixelIndex = patch.indices[changeIndex];
                    int localX = pixelIndex % tileWidth;
                    int localY = pixelIndex / tileWidth;
                    int sample = atlas.pixels[(tileY + localY) * atlas.width + sourceX + localX];
                    if (sample == patch.colors[changeIndex]) {
                        matchedMask |= (1L << changeIndex);
                    }
                }
                if (matchedMask != 0L) {
                    candidates.add(new RepairSamplerMatch(atlasIndex, sourceX - tileX, matchedMask));
                }
            }
        }

        RepairSamplerMatch bestSingle = null;
        for (RepairSamplerMatch candidate : candidates) {
            if (candidate.matchedMask != fullMask) {
                continue;
            }
            if (bestSingle == null || compareRepairSamplerMatches(candidate, bestSingle, state) < 0) {
                bestSingle = candidate;
            }
        }
        if (bestSingle != null) {
            return new RepairSamplerCoverage(bestSingle, true);
        }

        boolean coveredByAtMostTwo = false;
        for (int firstIndex = 0; firstIndex < candidates.size(); firstIndex++) {
            RepairSamplerMatch first = candidates.get(firstIndex);
            for (int secondIndex = firstIndex + 1; secondIndex < candidates.size(); secondIndex++) {
                if ((first.matchedMask | candidates.get(secondIndex).matchedMask) == fullMask) {
                    coveredByAtMostTwo = true;
                    break;
                }
            }
            if (coveredByAtMostTwo) {
                break;
            }
        }
        return new RepairSamplerCoverage(null, coveredByAtMostTwo);
    }

    private static int compareRepairSamplerMatches(RepairSamplerMatch left, RepairSamplerMatch right, EventState state) {
        int leftSameAtlas = (state.mode != StateMode.BASE_PATCH && left.atlasIndex == state.atlasIndex) ? 0 : 1;
        int rightSameAtlas = (state.mode != StateMode.BASE_PATCH && right.atlasIndex == state.atlasIndex) ? 0 : 1;
        if (leftSameAtlas != rightSameAtlas) {
            return Integer.compare(leftSameAtlas, rightSameAtlas);
        }
        int leftAbsDx = Math.abs(left.deltaX);
        int rightAbsDx = Math.abs(right.deltaX);
        if (leftAbsDx != rightAbsDx) {
            return Integer.compare(leftAbsDx, rightAbsDx);
        }
        if (left.atlasIndex != right.atlasIndex) {
            return Integer.compare(left.atlasIndex, right.atlasIndex);
        }
        return Integer.compare(left.deltaX, right.deltaX);
    }

    private static boolean matchesSparseArgbDelta(TileBlock sourceTile, SparsePatch patch) {
        if (patch == null || patch.indices.length <= 0) {
            return false;
        }
        int firstSource = sourceTile.pixels[patch.indices[0]];
        int firstTarget = patch.colors[0];
        int deltaA = alpha(firstTarget) - alpha(firstSource);
        int deltaR = red(firstTarget) - red(firstSource);
        int deltaG = green(firstTarget) - green(firstSource);
        int deltaB = blue(firstTarget) - blue(firstSource);
        for (int changeIndex = 1; changeIndex < patch.indices.length; changeIndex++) {
            int source = sourceTile.pixels[patch.indices[changeIndex]];
            int target = patch.colors[changeIndex];
            if ((alpha(target) - alpha(source)) != deltaA
                    || (red(target) - red(source)) != deltaR
                    || (green(target) - green(source)) != deltaG
                    || (blue(target) - blue(source)) != deltaB) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesSparsePaletteRemap(TileBlock sourceTile, SparsePatch patch, int maxColors) {
        if (patch == null || patch.indices.length <= 0 || maxColors <= 0) {
            return false;
        }
        LinkedHashMap<Integer, Integer> remap = new LinkedHashMap<>();
        for (int changeIndex = 0; changeIndex < patch.indices.length; changeIndex++) {
            int source = sourceTile.pixels[patch.indices[changeIndex]];
            int target = patch.colors[changeIndex];
            Integer existing = remap.putIfAbsent(source, target);
            if (existing != null && existing != target) {
                return false;
            }
            if (remap.size() > maxColors) {
                return false;
            }
        }
        return !remap.isEmpty();
    }

    public static PlacementStats analyzePlacements(AfmaDecodedAnimation animation, int tileSize, int maxAtlasCount,
                                                   int regularizedTileBudget, int localCandidateLimit,
                                                   int minPlacementArea) throws IOException {
        PreparedData prepared = prepare(animation, tileSize);
        LinkedHashMap<TileBlock, Integer> payloadCostCache = new LinkedHashMap<>();
        TileBlock[][] atlasTiles = buildRegularizedAtlases(
                prepared.tilesByPositionFrame,
                prepared.baseTiles,
                prepared.totalFrames,
                prepared.columns,
                prepared.rows,
                maxAtlasCount,
                regularizedTileBudget,
                localCandidateLimit,
                payloadCostCache
        );

        @SuppressWarnings("unchecked")
        LinkedHashMap<TileBlock, Integer>[] atlasLookups = new LinkedHashMap[atlasTiles.length];
        for (int atlasIndex = 0; atlasIndex < atlasTiles.length; atlasIndex++) {
            LinkedHashMap<TileBlock, Integer> atlasLookup = new LinkedHashMap<>();
            for (int positionIndex = 0; positionIndex < atlasTiles[atlasIndex].length; positionIndex++) {
                atlasLookup.putIfAbsent(atlasTiles[atlasIndex][positionIndex], positionIndex);
            }
            atlasLookups[atlasIndex] = atlasLookup;
        }

        ArrayList<PositionProgram> exactPrograms = new ArrayList<>(prepared.positionCount);
        for (int positionIndex = 0; positionIndex < prepared.positionCount; positionIndex++) {
            TileBlock baseTile = prepared.baseTiles[positionIndex];
            EventState[] states = new EventState[prepared.totalFrames];
            boolean exact = true;
            for (int frameIndex = 0; frameIndex < prepared.totalFrames; frameIndex++) {
                TileBlock tile = prepared.tilesByPositionFrame[positionIndex][frameIndex];
                if (tile.equals(baseTile)) {
                    continue;
                }
                AtlasMatch exactMatch = findAtlasMatch(tile, positionIndex, atlasTiles, atlasLookups);
                if (exactMatch == null) {
                    exact = false;
                    break;
                }
                states[frameIndex] = exactMatch.samePosition
                        ? EventState.samePositionAtlas(exactMatch.atlasIndex)
                        : EventState.windowAtlas(exactMatch.atlasIndex, exactMatch.sourcePosition);
            }

            if (!exact) {
                exactPrograms.add(new PositionProgram(baseTile, List.of()));
                continue;
            }

            ArrayList<StateEvent> events = new ArrayList<>();
            int frameIndex = 0;
            while (frameIndex < prepared.totalFrames) {
                EventState state = states[frameIndex];
                if (state == null) {
                    frameIndex++;
                    continue;
                }
                int end = frameIndex + 1;
                while (end < prepared.totalFrames && state.equals(states[end])) {
                    end++;
                }
                events.add(new StateEvent(frameIndex, end - frameIndex, state));
                frameIndex = end;
            }
            exactPrograms.add(new PositionProgram(baseTile, events));
        }

        PlacementSelection selection = selectPlacementPrograms(exactPrograms, prepared.columns, prepared.rows, minPlacementArea, tileSize);
        return new PlacementStats(
                tileSize,
                prepared.positionCount,
                prepared.totalOccurrences,
                selection.eligiblePositions,
                selection.coveredPositionCount,
                selection.eligibleOccurrences,
                selection.coveredOccurrences,
                selection.templates.size(),
                selection.rectangleCount(),
                selection.averageRectangleArea()
        );
    }

    /**
     * Compresses a fully decoded animation into the atlas/program stream.
     */
    @Override
    public void compress(AfmaDecodedAnimation animation, OutputStream output) throws IOException {
        PreparedData prepared = prepare(animation, this.tileSize);
        LinkedHashMap<TileBlock, Integer> payloadCostCache = new LinkedHashMap<>();
        TileBlock[][] atlasTiles = buildRegularizedAtlases(
                prepared.tilesByPositionFrame,
                prepared.baseTiles,
                prepared.totalFrames,
                prepared.columns,
                prepared.rows,
                this.maxAtlasCount,
                this.regularizedTileBudget,
                this.localCandidateLimit,
                payloadCostCache
        );

        @SuppressWarnings("unchecked")
        LinkedHashMap<TileBlock, Integer>[] atlasLookups = new LinkedHashMap[atlasTiles.length];
        LinkedHashMap<TileBlock, Integer> structuralTileFrequencies = new LinkedHashMap<>();
        for (TileBlock baseTile : prepared.baseTiles) {
            structuralTileFrequencies.merge(baseTile, 1, Integer::sum);
        }
        for (int atlasIndex = 0; atlasIndex < atlasTiles.length; atlasIndex++) {
            LinkedHashMap<TileBlock, Integer> atlasLookup = new LinkedHashMap<>();
            for (int positionIndex = 0; positionIndex < atlasTiles[atlasIndex].length; positionIndex++) {
                TileBlock tile = atlasTiles[atlasIndex][positionIndex];
                structuralTileFrequencies.merge(tile, 1, Integer::sum);
                atlasLookup.putIfAbsent(tile, positionIndex);
            }
            atlasLookups[atlasIndex] = atlasLookup;
        }
        AtlasImage[] atlasImages = buildAtlasImages(atlasTiles, prepared.width, prepared.height, this.tileSize, prepared.columns, prepared.rows);
        LinkedHashMap<TileBlock, int[]> paletteCache = new LinkedHashMap<>();
        LinkedHashMap<RemapKey, Integer> remapCostCache = new LinkedHashMap<>();

        LinkedHashMap<TileBlock, Integer> tentativeLiteralFrequencies = new LinkedHashMap<>();
        @SuppressWarnings("unchecked")
        LinkedHashMap<TileBlock, EventState>[] stateCacheByPosition = new LinkedHashMap[prepared.positionCount];
        for (int positionIndex = 0; positionIndex < prepared.positionCount; positionIndex++) {
            stateCacheByPosition[positionIndex] = new LinkedHashMap<>();
            TileBlock baseTile = prepared.baseTiles[positionIndex];
            for (int frameIndex = 0; frameIndex < prepared.totalFrames; frameIndex++) {
                TileBlock tile = prepared.tilesByPositionFrame[positionIndex][frameIndex];
                if (tile.equals(baseTile)) {
                    continue;
                }
                AtlasMatch exactMatch = findAtlasMatch(tile, positionIndex, atlasTiles, atlasLookups);
                if (exactMatch == null && findHorizontalSamplerMatch(tile, positionIndex, atlasImages, prepared.width, this.tileSize, prepared.columns) == null) {
                    tentativeLiteralFrequencies.merge(tile, 1, Integer::sum);
                }
            }
        }

        LinkedHashMap<TileBlock, Integer> tentativeTileFrequencies = new LinkedHashMap<>(structuralTileFrequencies);
        for (Map.Entry<TileBlock, Integer> entry : tentativeLiteralFrequencies.entrySet()) {
            tentativeTileFrequencies.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
        LinkedHashMap<TileBlock, Integer> tentativeTileDictionaryIds = buildTileDictionary(tentativeTileFrequencies, this.minRepeatCount);

        LinkedHashMap<TileTransform, Integer> provisionalTransformFrequencies = new LinkedHashMap<>();
        LinkedHashMap<PatchShapeKey, Integer> provisionalPatchShapeFrequencies = new LinkedHashMap<>();
        @SuppressWarnings("unchecked")
        LinkedHashMap<TileBlock, EventState>[] provisionalStateCacheByPosition = new LinkedHashMap[prepared.positionCount];
        for (int positionIndex = 0; positionIndex < prepared.positionCount; positionIndex++) {
            List<PatchSource> patchSources = buildPatchSources(positionIndex, prepared.baseTiles, atlasTiles);
            provisionalStateCacheByPosition[positionIndex] = new LinkedHashMap<>();
            TileBlock baseTile = prepared.baseTiles[positionIndex];
            for (int frameIndex = 0; frameIndex < prepared.totalFrames; frameIndex++) {
                TileBlock tile = prepared.tilesByPositionFrame[positionIndex][frameIndex];
                if (tile.equals(baseTile)) {
                    continue;
                }
                EventState cached = provisionalStateCacheByPosition[positionIndex].get(tile);
                if (cached == null) {
                    AtlasMatch exactMatch = findAtlasMatch(tile, positionIndex, atlasTiles, atlasLookups);
                    if (exactMatch != null) {
                        cached = exactMatch.samePosition
                                ? EventState.samePositionAtlas(exactMatch.atlasIndex)
                                : EventState.windowAtlas(exactMatch.atlasIndex, exactMatch.sourcePosition);
                    } else {
                        HorizontalSamplerMatch samplerMatch = findHorizontalSamplerMatch(tile, positionIndex, atlasImages, prepared.width, this.tileSize, prepared.columns);
                        if (samplerMatch != null) {
                            cached = EventState.horizontalSampler(samplerMatch.atlasIndex, samplerMatch.deltaX);
                        } else {
                            cached = resolveEventState(
                                    tile,
                                    tentativeTileDictionaryIds,
                                    patchSources,
                                    payloadCostCache,
                                    remapCostCache,
                                    paletteCache,
                                    new LinkedHashMap<>(),
                                    Map.of(),
                                    this.maxPatchPixels,
                                    this.maxRemapColors
                            );
                        }
                    }
                    provisionalStateCacheByPosition[positionIndex].put(tile, cached);
                }
                if (cached.patch != null) {
                    provisionalPatchShapeFrequencies.merge(PatchShapeKey.from(cached.patch, tile.width, tile.height), 1, Integer::sum);
                }
                if (cached.transform != null) {
                    provisionalTransformFrequencies.merge(cached.transform, 1, Integer::sum);
                }
            }
        }
        LinkedHashMap<TileTransform, Integer> provisionalTransformDictionaryIds = buildTransformDictionary(provisionalTransformFrequencies, 2);
        LinkedHashMap<PatchShapeKey, Integer> provisionalPatchShapeDictionaryIds = buildPatchShapeDictionary(provisionalPatchShapeFrequencies);
        provisionalStateCacheByPosition = null;

        ArrayList<PositionProgram> programs = new ArrayList<>(prepared.positionCount);
        for (int positionIndex = 0; positionIndex < prepared.positionCount; positionIndex++) {
            List<PatchSource> patchSources = buildPatchSources(positionIndex, prepared.baseTiles, atlasTiles);
            TileBlock baseTile = prepared.baseTiles[positionIndex];
            EventState[] states = new EventState[prepared.totalFrames];
            for (int frameIndex = 0; frameIndex < prepared.totalFrames; frameIndex++) {
                TileBlock tile = prepared.tilesByPositionFrame[positionIndex][frameIndex];
                if (tile.equals(baseTile)) {
                    states[frameIndex] = null;
                    continue;
                }
                EventState cached = stateCacheByPosition[positionIndex].get(tile);
                if (cached == null) {
                    AtlasMatch exactMatch = findAtlasMatch(tile, positionIndex, atlasTiles, atlasLookups);
                    if (exactMatch != null) {
                        cached = exactMatch.samePosition
                                ? EventState.samePositionAtlas(exactMatch.atlasIndex)
                                : EventState.windowAtlas(exactMatch.atlasIndex, exactMatch.sourcePosition);
                    } else {
                        HorizontalSamplerMatch samplerMatch = findHorizontalSamplerMatch(tile, positionIndex, atlasImages, prepared.width, this.tileSize, prepared.columns);
                        if (samplerMatch != null) {
                            cached = EventState.horizontalSampler(samplerMatch.atlasIndex, samplerMatch.deltaX);
                        } else {
                            cached = resolveEventState(
                                    tile,
                                    tentativeTileDictionaryIds,
                                    patchSources,
                                    payloadCostCache,
                                    remapCostCache,
                                    paletteCache,
                                    provisionalTransformDictionaryIds,
                                    provisionalPatchShapeDictionaryIds,
                                    this.maxPatchPixels,
                                    this.maxRemapColors
                            );
                        }
                    }
                    stateCacheByPosition[positionIndex].put(tile, cached);
                }
                states[frameIndex] = cached;
            }

            ArrayList<StateEvent> events = new ArrayList<>();
            int frameIndex = 0;
            while (frameIndex < prepared.totalFrames) {
                EventState state = states[frameIndex];
                if (state == null) {
                    frameIndex++;
                    continue;
                }
                int end = frameIndex + 1;
                while (end < prepared.totalFrames && state.equals(states[end])) {
                    end++;
                }
                events.add(new StateEvent(frameIndex, end - frameIndex, state));
                frameIndex = end;
            }

            PositionProgram program = new PositionProgram(baseTile, events);
            programs.add(program);
        }
        atlasImages = null;
        atlasLookups = null;
        stateCacheByPosition = null;

        PlacementSelection placementSelection = selectPlacementPrograms(programs, prepared.columns, prepared.rows, this.minPlacementArea, this.tileSize);

        LinkedHashMap<TileBlock, Integer> finalTileFrequencies = new LinkedHashMap<>(structuralTileFrequencies);
        LinkedHashMap<TimePatternKey, Integer> timePatternFrequencies = new LinkedHashMap<>();
        LinkedHashMap<TileBlock, Integer> tileDictionaryIds = buildTileDictionary(finalTileFrequencies, this.minRepeatCount);
        LinkedHashMap<TileTransform, Integer> transformFrequencies = new LinkedHashMap<>();
        LinkedHashMap<PatchShapeKey, Integer> patchShapeFrequencies = new LinkedHashMap<>();
        for (int positionIndex = 0; positionIndex < prepared.positionCount; positionIndex++) {
            if (placementSelection.coveredPositions[positionIndex]) {
                continue;
            }
            int tileWidth = Math.min(this.tileSize, prepared.width - (positionIndex % prepared.columns) * this.tileSize);
            int tileHeight = Math.min(this.tileSize, prepared.height - (positionIndex / prepared.columns) * this.tileSize);
            PositionProgram program = programs.get(positionIndex);
            timePatternFrequencies.merge(TimePatternKey.from(program), 1, Integer::sum);
            for (StateEvent event : program.events) {
                if (event.state.mode == StateMode.LITERAL) {
                    finalTileFrequencies.merge(event.state.literalTile, 1, Integer::sum);
                }
                if (event.state.patch != null) {
                    patchShapeFrequencies.merge(PatchShapeKey.from(event.state.patch, tileWidth, tileHeight), 1, Integer::sum);
                }
                if (event.state.transform != null) {
                    transformFrequencies.merge(event.state.transform, 1, Integer::sum);
                }
            }
        }
        tileDictionaryIds = buildTileDictionary(finalTileFrequencies, this.minRepeatCount);
        LinkedHashMap<TimePatternKey, Integer> timePatternIds = buildTimePatternDictionary(timePatternFrequencies, this.minScheduleRepeatCount);
        LinkedHashMap<TileTransform, Integer> transformDictionaryIds = buildTransformDictionary(transformFrequencies, 2);
        LinkedHashMap<PatchShapeKey, Integer> patchShapeDictionaryIds = buildPatchShapeDictionary(patchShapeFrequencies);

        SectionBuffer tileDictionaryMeta = new SectionBuffer();
        SectionBuffer tileDictionaryPayload = new SectionBuffer();
        for (TileBlock tile : tileDictionaryIds.keySet()) {
            AfmaVarInts.writeUnsigned(tileDictionaryMeta, tile.width);
            AfmaVarInts.writeUnsigned(tileDictionaryMeta, tile.height);
            writeTilePayload(tileDictionaryPayload, tile);
        }

        SectionBuffer transformDictionaryMeta = new SectionBuffer();
        SectionBuffer transformDictionaryPayload = new SectionBuffer();
        for (TileTransform transform : transformDictionaryIds.keySet()) {
            writeDictionaryTileTransform(transformDictionaryMeta, transformDictionaryPayload, transform);
        }

        SectionBuffer patchShapeDictionaryPayload = new SectionBuffer();
        for (PatchShapeKey patchShape : patchShapeDictionaryIds.keySet()) {
            writePatchShape(patchShapeDictionaryPayload, patchShape);
        }

        SectionBuffer baseMeta = new SectionBuffer();
        SectionBuffer baseRefs = new SectionBuffer();
        SectionBuffer baseInlinePayload = new SectionBuffer();
        for (int row = 0; row < prepared.rows; row++) {
            TileBlock leftTile = null;
            for (int column = 0; column < prepared.columns; column++) {
                int positionIndex = row * prepared.columns + column;
                TileBlock tile = prepared.baseTiles[positionIndex];
                writePredictedTile(tile, leftTile, tileDictionaryIds, baseMeta, baseRefs, baseInlinePayload);
                leftTile = tile;
            }
        }

        SectionBuffer atlasMeta = new SectionBuffer();
        SectionBuffer atlasRefs = new SectionBuffer();
        SectionBuffer atlasInlinePayload = new SectionBuffer();
        for (int atlasIndex = 0; atlasIndex < atlasTiles.length; atlasIndex++) {
            for (int row = 0; row < prepared.rows; row++) {
                TileBlock leftTile = null;
                for (int column = 0; column < prepared.columns; column++) {
                    int positionIndex = row * prepared.columns + column;
                    TileBlock tile = atlasTiles[atlasIndex][positionIndex];
                    TileBlock baseTile = prepared.baseTiles[positionIndex];
                    TileBlock previousAtlasTile = (atlasIndex > 0) ? atlasTiles[atlasIndex - 1][positionIndex] : null;
                    writeAtlasTile(tile, leftTile, baseTile, previousAtlasTile, tileDictionaryIds, atlasMeta, atlasRefs, atlasInlinePayload);
                    leftTile = tile;
                }
            }
        }

        SectionBuffer placementPrograms = new SectionBuffer();
        AfmaVarInts.writeUnsigned(placementPrograms, placementSelection.templates.size());
        for (PlacementTemplateSelection templateSelection : placementSelection.templates) {
            writeTimePattern(templateSelection.template.pattern, placementPrograms);
            AfmaVarInts.writeUnsigned(placementPrograms, templateSelection.rectangles.size());
            for (PlacementRectangle rectangle : templateSelection.rectangles) {
                AfmaVarInts.writeUnsigned(placementPrograms, rectangle.x);
                AfmaVarInts.writeUnsigned(placementPrograms, rectangle.y);
                AfmaVarInts.writeUnsigned(placementPrograms, rectangle.width);
                AfmaVarInts.writeUnsigned(placementPrograms, rectangle.height);
            }
            for (PlacementStep step : templateSelection.template.steps) {
                AfmaVarInts.writeUnsigned(placementPrograms, step.atlasIndex);
                AfmaVarInts.writeUnsigned(placementPrograms, AfmaVarInts.zigZagEncode(step.deltaX));
                AfmaVarInts.writeUnsigned(placementPrograms, AfmaVarInts.zigZagEncode(step.deltaY));
            }
        }

        SectionBuffer timePatternDictionaryMeta = new SectionBuffer();
        for (TimePatternKey pattern : timePatternIds.keySet()) {
            writeTimePattern(pattern, timePatternDictionaryMeta);
        }

        SectionBuffer timePatternDispatch = new SectionBuffer();
        SectionBuffer timePatternRefs = new SectionBuffer();
        SectionBuffer timePatternInlineMeta = new SectionBuffer();
        SectionBuffer eventStateMeta = new SectionBuffer();
        SectionBuffer eventAtlasRefs = new SectionBuffer();
        SectionBuffer eventLiteralMeta = new SectionBuffer();
        SectionBuffer eventLiteralRefs = new SectionBuffer();
        SectionBuffer eventLiteralInlinePayload = new SectionBuffer();
        SectionBuffer eventPatchMeta = new SectionBuffer();
        SectionBuffer eventPatchRefs = new SectionBuffer();
        SectionBuffer eventPatchPayload = new SectionBuffer();
        SectionBuffer eventTransformMeta = new SectionBuffer();
        SectionBuffer eventTransformRefs = new SectionBuffer();
        SectionBuffer eventTransformPayload = new SectionBuffer();
        for (int row = 0; row < prepared.rows; row++) {
            TimePatternKey leftPattern = null;
            for (int column = 0; column < prepared.columns; column++) {
                int positionIndex = row * prepared.columns + column;
                if (placementSelection.coveredPositions[positionIndex]) {
                    continue;
                }
                int tileX = column * this.tileSize;
                int tileY = row * this.tileSize;
                int tileWidth = Math.min(this.tileSize, prepared.width - tileX);
                int tileHeight = Math.min(this.tileSize, prepared.height - tileY);
                PositionProgram program = programs.get(positionIndex);
                TimePatternKey pattern = TimePatternKey.from(program);
                Integer patternId = timePatternIds.get(pattern);
                if (leftPattern != null && leftPattern.equals(pattern)) {
                    AfmaVarInts.writeUnsigned(timePatternDispatch, 2);
                } else if (patternId != null) {
                    AfmaVarInts.writeUnsigned(timePatternDispatch, 1);
                    AfmaVarInts.writeUnsigned(timePatternRefs, patternId);
                } else {
                    AfmaVarInts.writeUnsigned(timePatternDispatch, 0);
                    writeTimePattern(pattern, timePatternInlineMeta);
                }
                leftPattern = pattern;

                for (StateEvent event : program.events) {
                    switch (event.state.mode) {
                        case SAME_POSITION_ATLAS -> {
                            AfmaVarInts.writeUnsigned(eventStateMeta, 2);
                            AfmaVarInts.writeUnsigned(eventAtlasRefs, event.state.atlasIndex);
                        }
                        case WINDOW_ATLAS -> {
                            AfmaVarInts.writeUnsigned(eventStateMeta, 1);
                            AfmaVarInts.writeUnsigned(eventAtlasRefs, event.state.atlasIndex);
                            AfmaVarInts.writeUnsigned(eventAtlasRefs, event.state.sourcePosition);
                        }
                        case HORIZONTAL_SAMPLER -> {
                            AfmaVarInts.writeUnsigned(eventStateMeta, 7);
                            AfmaVarInts.writeUnsigned(eventAtlasRefs, event.state.atlasIndex);
                            AfmaVarInts.writeUnsigned(eventAtlasRefs, AfmaVarInts.zigZagEncode(event.state.sourcePosition));
                        }
                        case LITERAL -> {
                            AfmaVarInts.writeUnsigned(eventStateMeta, 0);
                            writeTileReference(event.state.literalTile, tileDictionaryIds, eventLiteralMeta, eventLiteralRefs, eventLiteralInlinePayload);
                        }
                        case BASE_PATCH -> {
                            AfmaVarInts.writeUnsigned(eventStateMeta, 3);
                            writeSparsePatchReference(eventPatchMeta, eventPatchRefs, eventPatchPayload, event.state.patch, tileWidth, tileHeight, patchShapeDictionaryIds);
                        }
                        case SAME_POSITION_PATCH -> {
                            AfmaVarInts.writeUnsigned(eventStateMeta, 4);
                            AfmaVarInts.writeUnsigned(eventAtlasRefs, event.state.atlasIndex);
                            writeSparsePatchReference(eventPatchMeta, eventPatchRefs, eventPatchPayload, event.state.patch, tileWidth, tileHeight, patchShapeDictionaryIds);
                        }
                        case WINDOW_PATCH -> {
                            AfmaVarInts.writeUnsigned(eventStateMeta, 5);
                            AfmaVarInts.writeUnsigned(eventAtlasRefs, event.state.atlasIndex);
                            AfmaVarInts.writeUnsigned(eventAtlasRefs, event.state.sourcePosition);
                            writeSparsePatchReference(eventPatchMeta, eventPatchRefs, eventPatchPayload, event.state.patch, tileWidth, tileHeight, patchShapeDictionaryIds);
                        }
                        case BASE_PATCH_TRANSFORM -> {
                            AfmaVarInts.writeUnsigned(eventStateMeta, 8);
                            writeSparsePatchTransformReference(eventPatchMeta, eventPatchRefs, event.state.patch, tileWidth, tileHeight, patchShapeDictionaryIds);
                            writeTransformReference(eventTransformMeta, eventTransformRefs, eventTransformPayload, event.state.transform, transformDictionaryIds);
                        }
                        case SAME_POSITION_PATCH_TRANSFORM -> {
                            AfmaVarInts.writeUnsigned(eventStateMeta, 9);
                            AfmaVarInts.writeUnsigned(eventAtlasRefs, event.state.atlasIndex);
                            writeSparsePatchTransformReference(eventPatchMeta, eventPatchRefs, event.state.patch, tileWidth, tileHeight, patchShapeDictionaryIds);
                            writeTransformReference(eventTransformMeta, eventTransformRefs, eventTransformPayload, event.state.transform, transformDictionaryIds);
                        }
                        case WINDOW_PATCH_TRANSFORM -> {
                            AfmaVarInts.writeUnsigned(eventStateMeta, 10);
                            AfmaVarInts.writeUnsigned(eventAtlasRefs, event.state.atlasIndex);
                            AfmaVarInts.writeUnsigned(eventAtlasRefs, event.state.sourcePosition);
                            writeSparsePatchTransformReference(eventPatchMeta, eventPatchRefs, event.state.patch, tileWidth, tileHeight, patchShapeDictionaryIds);
                            writeTransformReference(eventTransformMeta, eventTransformRefs, eventTransformPayload, event.state.transform, transformDictionaryIds);
                        }
                        case TRANSFORM -> {
                            AfmaVarInts.writeUnsigned(eventStateMeta, 6);
                            writeTransformSourceMeta(eventTransformMeta, event.state.transformSourceType);
                            if (event.state.transformSourceType == PatchSourceType.SAME_POSITION_ATLAS) {
                                AfmaVarInts.writeUnsigned(eventAtlasRefs, event.state.atlasIndex);
                            } else if (event.state.transformSourceType == PatchSourceType.WINDOW_ATLAS) {
                                AfmaVarInts.writeUnsigned(eventAtlasRefs, event.state.atlasIndex);
                                AfmaVarInts.writeUnsigned(eventAtlasRefs, event.state.sourcePosition);
                            }
                            writeTransformReference(eventTransformMeta, eventTransformRefs, eventTransformPayload, event.state.transform, transformDictionaryIds);
                        }
                    }
                }
            }
        }

        DataOutputStream dataOut = new DataOutputStream(output);
        dataOut.writeInt(MAGIC);
        dataOut.writeByte(VERSION);
        AfmaVarInts.writeUnsigned(output, prepared.width);
        AfmaVarInts.writeUnsigned(output, prepared.height);
        AfmaVarInts.writeUnsigned(output, animation.introFrames().size());
        AfmaVarInts.writeUnsigned(output, animation.mainFrames().size());
        AfmaVarInts.writeUnsigned(output, this.tileSize);
        AfmaVarInts.writeUnsigned(output, prepared.columns);
        AfmaVarInts.writeUnsigned(output, prepared.rows);
        AfmaVarInts.writeUnsigned(output, tileDictionaryIds.size());
        AfmaVarInts.writeUnsigned(output, atlasTiles.length);
        AfmaVarInts.writeUnsigned(output, timePatternIds.size());
        AfmaVarInts.writeUnsigned(output, transformDictionaryIds.size());
        AfmaVarInts.writeUnsigned(output, patchShapeDictionaryIds.size());
        writePackedSection(output, tileDictionaryMeta);
        writePackedSection(output, tileDictionaryPayload);
        writePackedSection(output, transformDictionaryMeta);
        writePackedSection(output, transformDictionaryPayload);
        writePackedSection(output, patchShapeDictionaryPayload);
        writePackedSection(output, baseMeta);
        writePackedSection(output, baseRefs);
        writePackedSection(output, baseInlinePayload);
        writePackedSection(output, atlasMeta);
        writePackedSection(output, atlasRefs);
        writePackedSection(output, atlasInlinePayload);
        writePackedSection(output, placementPrograms);
        writePackedSection(output, timePatternDictionaryMeta);
        writePackedSection(output, timePatternDispatch);
        writePackedSection(output, timePatternRefs);
        writePackedSection(output, timePatternInlineMeta);
        writePackedSection(output, eventStateMeta);
        writePackedSection(output, eventAtlasRefs);
        writePackedSection(output, eventLiteralMeta);
        writePackedSection(output, eventLiteralRefs);
        writePackedSection(output, eventLiteralInlinePayload);
        writePackedSection(output, eventPatchMeta);
        writePackedSection(output, eventPatchRefs);
        writePackedSection(output, eventPatchPayload);
        writePackedSection(output, eventTransformMeta);
        writePackedSection(output, eventTransformRefs);
        writePackedSection(output, eventTransformPayload);
        dataOut.flush();
    }

    /**
     * Restores the exact decoded animation from the atlas/program stream.
     */
    @Override
    public AfmaDecodedAnimation decompress(String animationName, byte[] compressedBytes) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(compressedBytes);
        DataInputStream dataIn = new DataInputStream(in);
        if (dataIn.readInt() != MAGIC) {
            throw new IOException("Unsupported atlas pixel-offset placement program magic");
        }
        int version = dataIn.readUnsignedByte();
        if (version != VERSION) {
            throw new IOException("Unsupported atlas pixel-offset placement program version: " + version);
        }

        int width = AfmaVarInts.readUnsigned(in);
        int height = AfmaVarInts.readUnsigned(in);
        int introFrameCount = AfmaVarInts.readUnsigned(in);
        int mainFrameCount = AfmaVarInts.readUnsigned(in);
        int totalFrames = introFrameCount + mainFrameCount;
        int tileSize = AfmaVarInts.readUnsigned(in);
        int columns = AfmaVarInts.readUnsigned(in);
        int rows = AfmaVarInts.readUnsigned(in);
        int tileDictionaryCount = AfmaVarInts.readUnsigned(in);
        int atlasCount = AfmaVarInts.readUnsigned(in);
        int timePatternDictionaryCount = AfmaVarInts.readUnsigned(in);
        int transformDictionaryCount = AfmaVarInts.readUnsigned(in);
        int patchShapeDictionaryCount = AfmaVarInts.readUnsigned(in);

        ByteArrayInputStream tileDictionaryMeta = readPackedSection(in);
        ByteArrayInputStream tileDictionaryPayload = readPackedSection(in);
        ByteArrayInputStream transformDictionaryMeta = readPackedSection(in);
        ByteArrayInputStream transformDictionaryPayload = readPackedSection(in);
        ByteArrayInputStream patchShapeDictionaryPayload = readPackedSection(in);
        ByteArrayInputStream baseMeta = readPackedSection(in);
        ByteArrayInputStream baseRefs = readPackedSection(in);
        ByteArrayInputStream baseInlinePayload = readPackedSection(in);
        ByteArrayInputStream atlasMeta = readPackedSection(in);
        ByteArrayInputStream atlasRefs = readPackedSection(in);
        ByteArrayInputStream atlasInlinePayload = readPackedSection(in);
        ByteArrayInputStream placementPrograms = readPackedSection(in);
        ByteArrayInputStream timePatternDictionaryMeta = readPackedSection(in);
        ByteArrayInputStream timePatternDispatch = readPackedSection(in);
        ByteArrayInputStream timePatternRefs = readPackedSection(in);
        ByteArrayInputStream timePatternInlineMeta = readPackedSection(in);
        ByteArrayInputStream eventStateMeta = readPackedSection(in);
        ByteArrayInputStream eventAtlasRefs = readPackedSection(in);
        ByteArrayInputStream eventLiteralMeta = readPackedSection(in);
        ByteArrayInputStream eventLiteralRefs = readPackedSection(in);
        ByteArrayInputStream eventLiteralInlinePayload = readPackedSection(in);
        ByteArrayInputStream eventPatchMeta = readPackedSection(in);
        ByteArrayInputStream eventPatchRefs = readPackedSection(in);
        ByteArrayInputStream eventPatchPayload = readPackedSection(in);
        ByteArrayInputStream eventTransformMeta = readPackedSection(in);
        ByteArrayInputStream eventTransformRefs = readPackedSection(in);
        ByteArrayInputStream eventTransformPayload = readPackedSection(in);

        TileBlock[] tileDictionary = new TileBlock[tileDictionaryCount];
        for (int tileIndex = 0; tileIndex < tileDictionary.length; tileIndex++) {
            int tileWidth = AfmaVarInts.readUnsigned(tileDictionaryMeta);
            int tileHeight = AfmaVarInts.readUnsigned(tileDictionaryMeta);
            tileDictionary[tileIndex] = readTilePayload(tileDictionaryPayload, tileWidth, tileHeight);
        }

        TileTransform[] transformDictionary = new TileTransform[transformDictionaryCount];
        for (int transformIndex = 0; transformIndex < transformDictionary.length; transformIndex++) {
            transformDictionary[transformIndex] = readDictionaryTileTransform(transformDictionaryMeta, transformDictionaryPayload);
        }

        PatchShape[] patchShapeDictionary = new PatchShape[patchShapeDictionaryCount];
        for (int patchShapeIndex = 0; patchShapeIndex < patchShapeDictionary.length; patchShapeIndex++) {
            patchShapeDictionary[patchShapeIndex] = readPatchShape(patchShapeDictionaryPayload, width * height);
        }

        int positionCount = columns * rows;
        TileBlock[] baseTiles = new TileBlock[positionCount];
        for (int row = 0; row < rows; row++) {
            TileBlock leftTile = null;
            int tileY = row * tileSize;
            int tileHeight = Math.min(tileSize, height - tileY);
            for (int column = 0; column < columns; column++) {
                int tileX = column * tileSize;
                int tileWidth = Math.min(tileSize, width - tileX);
                int positionIndex = row * columns + column;
                TileBlock tile = readPredictedTile(baseMeta, baseRefs, baseInlinePayload, tileDictionary, tileWidth, tileHeight, leftTile);
                baseTiles[positionIndex] = tile;
                leftTile = tile;
            }
        }

        TileBlock[][] atlasTiles = new TileBlock[atlasCount][positionCount];
        for (int atlasIndex = 0; atlasIndex < atlasCount; atlasIndex++) {
            for (int row = 0; row < rows; row++) {
                TileBlock leftTile = null;
                int tileY = row * tileSize;
                int tileHeight = Math.min(tileSize, height - tileY);
                for (int column = 0; column < columns; column++) {
                    int tileX = column * tileSize;
                    int tileWidth = Math.min(tileSize, width - tileX);
                    int positionIndex = row * columns + column;
                    TileBlock baseTile = baseTiles[positionIndex];
                    TileBlock previousAtlasTile = (atlasIndex > 0) ? atlasTiles[atlasIndex - 1][positionIndex] : null;
                    TileBlock tile = readAtlasTile(atlasMeta, atlasRefs, atlasInlinePayload, tileDictionary, tileWidth, tileHeight, leftTile, baseTile, previousAtlasTile);
                    atlasTiles[atlasIndex][positionIndex] = tile;
                    leftTile = tile;
                }
            }
        }
        AtlasImage[] atlasImages = buildAtlasImages(atlasTiles, width, height, tileSize, columns, rows);

        TimePatternKey[] timePatterns = new TimePatternKey[timePatternDictionaryCount];
        for (int patternIndex = 0; patternIndex < timePatterns.length; patternIndex++) {
            timePatterns[patternIndex] = readTimePattern(timePatternDictionaryMeta);
        }

        int[][] framePixels = new int[totalFrames][width * height];
        for (int positionIndex = 0; positionIndex < positionCount; positionIndex++) {
            int row = positionIndex / columns;
            int column = positionIndex % columns;
            int tileX = column * tileSize;
            int tileY = row * tileSize;
            TileBlock baseTile = baseTiles[positionIndex];
            for (int frameIndex = 0; frameIndex < totalFrames; frameIndex++) {
                blitTile(baseTile, framePixels[frameIndex], width, tileX, tileY);
            }
        }

        boolean[] placementCovered = new boolean[positionCount];
        int placementTemplateCount = AfmaVarInts.readUnsigned(placementPrograms);
        for (int templateIndex = 0; templateIndex < placementTemplateCount; templateIndex++) {
            TimePatternKey pattern = readTimePattern(placementPrograms);
            int rectangleCount = AfmaVarInts.readUnsigned(placementPrograms);
            PlacementRectangle[] rectangles = new PlacementRectangle[rectangleCount];
            for (int rectangleIndex = 0; rectangleIndex < rectangleCount; rectangleIndex++) {
                int x = AfmaVarInts.readUnsigned(placementPrograms);
                int y = AfmaVarInts.readUnsigned(placementPrograms);
                int rectWidth = AfmaVarInts.readUnsigned(placementPrograms);
                int rectHeight = AfmaVarInts.readUnsigned(placementPrograms);
                if (x < 0 || y < 0 || rectWidth <= 0 || rectHeight <= 0 || (x + rectWidth) > columns || (y + rectHeight) > rows) {
                    throw new IOException("Placement rectangle is invalid");
                }
                rectangles[rectangleIndex] = new PlacementRectangle(x, y, rectWidth, rectHeight);
            }
            PlacementStep[] steps = new PlacementStep[pattern.eventCount];
            for (int eventIndex = 0; eventIndex < pattern.eventCount; eventIndex++) {
                int atlasIndex = AfmaVarInts.readUnsigned(placementPrograms);
                int deltaX = AfmaVarInts.zigZagDecode(AfmaVarInts.readUnsigned(placementPrograms));
                int deltaY = AfmaVarInts.zigZagDecode(AfmaVarInts.readUnsigned(placementPrograms));
                if (atlasIndex < 0 || atlasIndex >= atlasTiles.length) {
                    throw new IOException("Placement atlas index is invalid: " + atlasIndex);
                }
                steps[eventIndex] = new PlacementStep(atlasIndex, deltaX, deltaY);
            }

            for (PlacementRectangle rectangle : rectangles) {
                for (int localY = 0; localY < rectangle.height; localY++) {
                    for (int localX = 0; localX < rectangle.width; localX++) {
                        int column = rectangle.x + localX;
                        int row = rectangle.y + localY;
                        int positionIndex = row * columns + column;
                        if (placementCovered[positionIndex]) {
                            throw new IOException("Placement programs overlap at position " + positionIndex);
                        }
                        placementCovered[positionIndex] = true;
                        int tileX = column * tileSize;
                        int tileY = row * tileSize;
                        int tileWidth = Math.min(tileSize, width - tileX);
                        int tileHeight = Math.min(tileSize, height - tileY);
                        for (int eventIndex = 0; eventIndex < pattern.eventCount; eventIndex++) {
                            PlacementStep step = steps[eventIndex];
                            int sourceY = tileY + step.deltaY * tileSize;
                            TileBlock tile = extractAtlasWindow(atlasImages[step.atlasIndex], tileX + step.deltaX, sourceY, tileWidth, tileHeight);
                            int start = pattern.starts[eventIndex];
                            int end = start + pattern.lengths[eventIndex];
                            for (int frameIndex = start; frameIndex < end; frameIndex++) {
                                if (frameIndex < 0 || frameIndex >= totalFrames) {
                                    throw new IOException("Placement event frame range is invalid");
                                }
                                blitTile(tile, framePixels[frameIndex], width, tileX, tileY);
                            }
                        }
                    }
                }
            }
        }

        for (int row = 0; row < rows; row++) {
            TimePatternKey leftPattern = null;
            for (int column = 0; column < columns; column++) {
                int positionIndex = row * columns + column;
                if (placementCovered[positionIndex]) {
                    continue;
                }
                int tileX = column * tileSize;
                int tileY = row * tileSize;
                int tileWidth = Math.min(tileSize, width - tileX);
                int tileHeight = Math.min(tileSize, height - tileY);
                TimePatternKey pattern = readTimePatternReference(timePatternDispatch, timePatternRefs, timePatternInlineMeta, timePatterns, leftPattern);
                leftPattern = pattern;
                for (int eventIndex = 0; eventIndex < pattern.eventCount; eventIndex++) {
                    TileBlock tile;
                    int stateMode = AfmaVarInts.readUnsigned(eventStateMeta);
                    if (stateMode == 2) {
                        int atlasIndex = AfmaVarInts.readUnsigned(eventAtlasRefs);
                        if (atlasIndex < 0 || atlasIndex >= atlasTiles.length) {
                            throw new IOException("Atlas index is invalid: " + atlasIndex);
                        }
                        tile = atlasTiles[atlasIndex][positionIndex];
                    } else if (stateMode == 1) {
                        int atlasIndex = AfmaVarInts.readUnsigned(eventAtlasRefs);
                        int sourcePosition = AfmaVarInts.readUnsigned(eventAtlasRefs);
                        if (atlasIndex < 0 || atlasIndex >= atlasTiles.length) {
                            throw new IOException("Atlas index is invalid: " + atlasIndex);
                        }
                        if (sourcePosition < 0 || sourcePosition >= positionCount) {
                            throw new IOException("Atlas source position is invalid: " + sourcePosition);
                        }
                        tile = atlasTiles[atlasIndex][sourcePosition];
                    } else if (stateMode == 7) {
                        int atlasIndex = AfmaVarInts.readUnsigned(eventAtlasRefs);
                        int deltaX = AfmaVarInts.zigZagDecode(AfmaVarInts.readUnsigned(eventAtlasRefs));
                        if (atlasIndex < 0 || atlasIndex >= atlasImages.length) {
                            throw new IOException("Atlas index is invalid: " + atlasIndex);
                        }
                        tile = extractAtlasWindow(atlasImages[atlasIndex], tileX + deltaX, tileY, tileWidth, tileHeight);
                    } else if (stateMode == 0) {
                        tile = readTileReference(eventLiteralMeta, eventLiteralRefs, eventLiteralInlinePayload, tileDictionary, tileWidth, tileHeight);
                    } else if (stateMode == 3) {
                        SparsePatch patch = readSparsePatchReference(eventPatchMeta, eventPatchRefs, eventPatchPayload, patchShapeDictionary, tileWidth, tileHeight);
                        tile = applySparsePatch(baseTiles[positionIndex], patch);
                    } else if (stateMode == 4) {
                        int atlasIndex = AfmaVarInts.readUnsigned(eventAtlasRefs);
                        if (atlasIndex < 0 || atlasIndex >= atlasTiles.length) {
                            throw new IOException("Atlas index is invalid: " + atlasIndex);
                        }
                        SparsePatch patch = readSparsePatchReference(eventPatchMeta, eventPatchRefs, eventPatchPayload, patchShapeDictionary, tileWidth, tileHeight);
                        tile = applySparsePatch(atlasTiles[atlasIndex][positionIndex], patch);
                    } else if (stateMode == 5) {
                        int atlasIndex = AfmaVarInts.readUnsigned(eventAtlasRefs);
                        int sourcePosition = AfmaVarInts.readUnsigned(eventAtlasRefs);
                        if (atlasIndex < 0 || atlasIndex >= atlasTiles.length) {
                            throw new IOException("Atlas index is invalid: " + atlasIndex);
                        }
                        if (sourcePosition < 0 || sourcePosition >= positionCount) {
                            throw new IOException("Atlas source position is invalid: " + sourcePosition);
                        }
                        SparsePatch patch = readSparsePatchReference(eventPatchMeta, eventPatchRefs, eventPatchPayload, patchShapeDictionary, tileWidth, tileHeight);
                        tile = applySparsePatch(atlasTiles[atlasIndex][sourcePosition], patch);
                    } else if (stateMode == 8 || stateMode == 9 || stateMode == 10) {
                        TileBlock sourceTile;
                        if (stateMode == 8) {
                            sourceTile = baseTiles[positionIndex];
                        } else if (stateMode == 9) {
                            int atlasIndex = AfmaVarInts.readUnsigned(eventAtlasRefs);
                            if (atlasIndex < 0 || atlasIndex >= atlasTiles.length) {
                                throw new IOException("Atlas index is invalid: " + atlasIndex);
                            }
                            sourceTile = atlasTiles[atlasIndex][positionIndex];
                        } else {
                            int atlasIndex = AfmaVarInts.readUnsigned(eventAtlasRefs);
                            int sourcePosition = AfmaVarInts.readUnsigned(eventAtlasRefs);
                            if (atlasIndex < 0 || atlasIndex >= atlasTiles.length) {
                                throw new IOException("Atlas index is invalid: " + atlasIndex);
                            }
                            if (sourcePosition < 0 || sourcePosition >= positionCount) {
                                throw new IOException("Atlas source position is invalid: " + sourcePosition);
                            }
                            sourceTile = atlasTiles[atlasIndex][sourcePosition];
                        }
                        SparsePatch patch = readSparsePatchTransformReference(eventPatchMeta, eventPatchRefs, patchShapeDictionary, tileWidth, tileHeight);
                        int transformEncodingMode = AfmaVarInts.readUnsigned(eventTransformMeta);
                        TileTransform transform;
                        if (transformEncodingMode == 0) {
                            int transformId = AfmaVarInts.readUnsigned(eventTransformRefs);
                            if (transformId < 0 || transformId >= transformDictionary.length) {
                                throw new IOException("Transform dictionary reference is invalid: " + transformId);
                            }
                            transform = transformDictionary[transformId];
                        } else if (transformEncodingMode == 1) {
                            transform = readTileTransform(eventTransformMeta, eventTransformPayload, sourceTile);
                        } else {
                            throw new IOException("Unsupported patch transform encoding mode: " + transformEncodingMode);
                        }
                        tile = applySparsePatchTransform(sourceTile, patch, transform);
                    } else if (stateMode == 6) {
                        PatchSourceType sourceType = readTransformSourceMeta(eventTransformMeta);
                        TileBlock sourceTile = switch (sourceType) {
                            case BASE -> baseTiles[positionIndex];
                            case SAME_POSITION_ATLAS -> {
                                int atlasIndex = AfmaVarInts.readUnsigned(eventAtlasRefs);
                                if (atlasIndex < 0 || atlasIndex >= atlasTiles.length) {
                                    throw new IOException("Atlas index is invalid: " + atlasIndex);
                                }
                                yield atlasTiles[atlasIndex][positionIndex];
                            }
                            case WINDOW_ATLAS -> {
                                int atlasIndex = AfmaVarInts.readUnsigned(eventAtlasRefs);
                                int sourcePosition = AfmaVarInts.readUnsigned(eventAtlasRefs);
                                if (atlasIndex < 0 || atlasIndex >= atlasTiles.length) {
                                    throw new IOException("Atlas index is invalid: " + atlasIndex);
                                }
                                if (sourcePosition < 0 || sourcePosition >= positionCount) {
                                    throw new IOException("Atlas source position is invalid: " + sourcePosition);
                                }
                                yield atlasTiles[atlasIndex][sourcePosition];
                            }
                        };
                        int transformEncodingMode = AfmaVarInts.readUnsigned(eventTransformMeta);
                        TileTransform transform;
                        if (transformEncodingMode == 0) {
                            int transformId = AfmaVarInts.readUnsigned(eventTransformRefs);
                            if (transformId < 0 || transformId >= transformDictionary.length) {
                                throw new IOException("Transform dictionary reference is invalid: " + transformId);
                            }
                            transform = transformDictionary[transformId];
                        } else if (transformEncodingMode == 1) {
                            transform = readTileTransform(eventTransformMeta, eventTransformPayload, sourceTile);
                        } else {
                            throw new IOException("Unsupported transform encoding mode: " + transformEncodingMode);
                        }
                        tile = applyTileTransform(sourceTile, transform);
                    } else {
                        throw new IOException("Unsupported event state mode: " + stateMode);
                    }

                    int start = pattern.starts[eventIndex];
                    int end = start + pattern.lengths[eventIndex];
                    for (int frameIndex = start; frameIndex < end; frameIndex++) {
                        if (frameIndex < 0 || frameIndex >= totalFrames) {
                            throw new IOException("Event frame range is invalid");
                        }
                        blitTile(tile, framePixels[frameIndex], width, tileX, tileY);
                    }
                }
            }
        }

        return new AfmaDecodedAnimation(
                width,
                height,
                decodeFrames(framePixels, width, height, 0, introFrameCount),
                decodeFrames(framePixels, width, height, introFrameCount, totalFrames)
        );
    }

    private static PreparedData prepare(AfmaDecodedAnimation animation, int tileSize) {
        List<AfmaDecodedFrame> frames = allFrames(animation);
        int width = animation.width();
        int height = animation.height();
        int totalFrames = frames.size();
        int columns = ceilDiv(width, tileSize);
        int rows = ceilDiv(height, tileSize);
        int positionCount = columns * rows;

        LinkedHashMap<TileBlock, TileBlock> tilePool = new LinkedHashMap<>();
        TileBlock[][] tilesByPositionFrame = new TileBlock[positionCount][totalFrames];
        TileBlock[] baseTiles = new TileBlock[positionCount];
        long baseCoveredOccurrences = 0L;
        for (int row = 0; row < rows; row++) {
            int tileY = row * tileSize;
            int tileHeight = Math.min(tileSize, height - tileY);
            for (int column = 0; column < columns; column++) {
                int tileX = column * tileSize;
                int tileWidth = Math.min(tileSize, width - tileX);
                int positionIndex = row * columns + column;
                LinkedHashMap<TileBlock, Integer> frequencies = new LinkedHashMap<>();
                TileBlock baseTile = null;
                int baseFrequency = 0;
                for (int frameIndex = 0; frameIndex < totalFrames; frameIndex++) {
                    TileBlock tile = internTile(tilePool, extractTile(frames.get(frameIndex).argbPixels(), width, tileX, tileY, tileWidth, tileHeight));
                    tilesByPositionFrame[positionIndex][frameIndex] = tile;
                    int frequency = frequencies.merge(tile, 1, Integer::sum);
                    if (frequency > baseFrequency) {
                        baseTile = tile;
                        baseFrequency = frequency;
                    }
                }
                baseTiles[positionIndex] = baseTile;
                for (TileBlock tile : tilesByPositionFrame[positionIndex]) {
                    if (tile.equals(baseTile)) {
                        baseCoveredOccurrences++;
                    }
                }
            }
        }

        return new PreparedData(
                width,
                height,
                totalFrames,
                columns,
                rows,
                positionCount,
                (long) positionCount * (long) totalFrames,
                baseCoveredOccurrences,
                tilesByPositionFrame,
                baseTiles
        );
    }

    private static boolean[][] initialUncovered(TileBlock[][] tilesByPositionFrame, TileBlock[] baseTiles, int positionCount, int totalFrames) {
        boolean[][] uncovered = new boolean[positionCount][totalFrames];
        for (int positionIndex = 0; positionIndex < positionCount; positionIndex++) {
            TileBlock baseTile = baseTiles[positionIndex];
            for (int frameIndex = 0; frameIndex < totalFrames; frameIndex++) {
                uncovered[positionIndex][frameIndex] = !tilesByPositionFrame[positionIndex][frameIndex].equals(baseTile);
            }
        }
        return uncovered;
    }

    private static TileBlock[][] buildRegularizedAtlases(TileBlock[][] tilesByPositionFrame, TileBlock[] baseTiles,
                                                         int totalFrames, int columns, int rows, int maxAtlasCount,
                                                         int regularizedTileBudget, int localCandidateLimit,
                                                         Map<TileBlock, Integer> payloadCostCache) throws IOException {
        int positionCount = columns * rows;
        boolean[][] uncovered = initialUncovered(tilesByPositionFrame, baseTiles, positionCount, totalFrames);
        ArrayList<TileBlock[]> atlases = new ArrayList<>(maxAtlasCount);
        for (int step = 0; step < maxAtlasCount; step++) {
            @SuppressWarnings("unchecked")
            List<LocalTileScore>[] localScoresByPosition = new List[positionCount];
            LinkedHashMap<TileBlock, Double> globalScores = new LinkedHashMap<>();
            for (int positionIndex = 0; positionIndex < positionCount; positionIndex++) {
                List<LocalTileScore> localScores = computeLocalTileScores(tilesByPositionFrame[positionIndex], uncovered[positionIndex], payloadCostCache);
                localScoresByPosition[positionIndex] = localScores;
                for (LocalTileScore score : localScores) {
                    globalScores.merge(score.tile, score.score, Double::sum);
                }
            }

            ArrayList<Map.Entry<TileBlock, Double>> scoredTiles = new ArrayList<>();
            for (Map.Entry<TileBlock, Double> entry : globalScores.entrySet()) {
                double netScore = entry.getValue() - estimateTilePayloadSize(entry.getKey(), payloadCostCache);
                if (netScore > 0.0D) {
                    scoredTiles.add(Map.entry(entry.getKey(), netScore));
                }
            }
            scoredTiles.sort(Comparator.<Map.Entry<TileBlock, Double>>comparingDouble(Map.Entry::getValue).reversed());

            LinkedHashMap<TileBlock, Integer> allowedTiles = new LinkedHashMap<>();
            for (Map.Entry<TileBlock, Double> entry : scoredTiles) {
                allowedTiles.put(entry.getKey(), allowedTiles.size());
                if (allowedTiles.size() >= regularizedTileBudget) {
                    break;
                }
            }
            if (allowedTiles.isEmpty()) {
                break;
            }

            TileBlock[] atlas = new TileBlock[positionCount];
            for (int row = 0; row < rows; row++) {
                @SuppressWarnings("unchecked")
                List<RowCandidate>[] candidatesByColumn = new List[columns];
                for (int column = 0; column < columns; column++) {
                    int positionIndex = row * columns + column;
                    ArrayList<RowCandidate> candidates = new ArrayList<>();
                    TileBlock baseTile = baseTiles[positionIndex];
                    candidates.add(new RowCandidate(baseTile, 0.0D));
                    int added = 0;
                    for (LocalTileScore score : localScoresByPosition[positionIndex]) {
                        if (!allowedTiles.containsKey(score.tile) || score.tile.equals(baseTile)) {
                            continue;
                        }
                        candidates.add(new RowCandidate(score.tile, score.score));
                        added++;
                        if (added >= localCandidateLimit) {
                            break;
                        }
                    }
                    candidatesByColumn[column] = candidates;
                }

                RowCandidate[] selection = selectBestRow(candidatesByColumn, baseTiles, row, columns);
                for (int column = 0; column < columns; column++) {
                    int positionIndex = row * columns + column;
                    atlas[positionIndex] = selection[column].tile;
                }
            }

            long coveredOccurrences = 0L;
            for (int positionIndex = 0; positionIndex < positionCount; positionIndex++) {
                TileBlock tile = atlas[positionIndex];
                if (tile.equals(baseTiles[positionIndex])) {
                    continue;
                }
                for (int frameIndex = 0; frameIndex < totalFrames; frameIndex++) {
                    if (uncovered[positionIndex][frameIndex] && tile.equals(tilesByPositionFrame[positionIndex][frameIndex])) {
                        uncovered[positionIndex][frameIndex] = false;
                        coveredOccurrences++;
                    }
                }
            }
            if (coveredOccurrences <= 0L) {
                break;
            }
            atlases.add(atlas);
        }
        return atlases.toArray(new TileBlock[0][]);
    }

    private static PlacementSelection selectPlacementPrograms(List<PositionProgram> programs, int columns, int rows, int minPlacementArea, int tileSize) {
        int positionCount = columns * rows;
        LinkedHashMap<PlacementTemplateKey, boolean[]> masksByTemplate = new LinkedHashMap<>();
        int eligiblePositions = 0;
        long eligibleOccurrences = 0L;
        for (int positionIndex = 0; positionIndex < positionCount; positionIndex++) {
            PlacementTemplateKey template = placementTemplateFor(programs.get(positionIndex), positionIndex, columns, tileSize);
            if (template == null) {
                continue;
            }
            masksByTemplate.computeIfAbsent(template, ignored -> new boolean[positionCount])[positionIndex] = true;
            eligiblePositions++;
            eligibleOccurrences += activeOccurrenceCount(template.pattern);
        }

        ArrayList<PlacementTemplateSelection> selections = new ArrayList<>();
        boolean[] coveredPositions = new boolean[positionCount];
        int coveredPositionCount = 0;
        long coveredOccurrences = 0L;
        for (Map.Entry<PlacementTemplateKey, boolean[]> entry : masksByTemplate.entrySet()) {
            boolean[] mask = Arrays.copyOf(entry.getValue(), entry.getValue().length);
            ArrayList<PlacementRectangle> rectangles = extractPlacementRectangles(mask, rows, columns, minPlacementArea);
            if (rectangles.isEmpty()) {
                continue;
            }

            int oldCost = 0;
            int newCost = estimatePlacementTemplateCost(entry.getKey(), rectangles.size());
            int coveredHere = 0;
            for (PlacementRectangle rectangle : rectangles) {
                newCost += estimatePlacementRectangleCost(rectangle);
                for (int localY = 0; localY < rectangle.height; localY++) {
                    for (int localX = 0; localX < rectangle.width; localX++) {
                        int positionIndex = (rectangle.y + localY) * columns + rectangle.x + localX;
                        oldCost += estimateExactProgramCost(programs.get(positionIndex));
                        coveredHere++;
                    }
                }
            }
            if (newCost >= oldCost) {
                continue;
            }

            selections.add(new PlacementTemplateSelection(entry.getKey(), rectangles));
            coveredPositionCount += coveredHere;
            coveredOccurrences += (long) coveredHere * activeOccurrenceCount(entry.getKey().pattern);
            for (PlacementRectangle rectangle : rectangles) {
                for (int localY = 0; localY < rectangle.height; localY++) {
                    int rowOffset = (rectangle.y + localY) * columns + rectangle.x;
                    Arrays.fill(coveredPositions, rowOffset, rowOffset + rectangle.width, true);
                }
            }
        }

        return new PlacementSelection(selections, coveredPositions, eligiblePositions, coveredPositionCount, eligibleOccurrences, coveredOccurrences);
    }

    private static PlacementTemplateKey placementTemplateFor(PositionProgram program, int positionIndex, int columns, int tileSize) {
        if (program.events.isEmpty()) {
            return null;
        }
        int positionX = positionIndex % columns;
        int positionY = positionIndex / columns;
        PlacementStep[] steps = new PlacementStep[program.events.size()];
        for (int eventIndex = 0; eventIndex < program.events.size(); eventIndex++) {
            EventState state = program.events.get(eventIndex).state;
            if (state.mode == StateMode.SAME_POSITION_ATLAS) {
                steps[eventIndex] = new PlacementStep(state.atlasIndex, 0, 0);
                continue;
            }
            if (state.mode == StateMode.WINDOW_ATLAS) {
                int sourceX = state.sourcePosition % columns;
                int sourceY = state.sourcePosition / columns;
                steps[eventIndex] = new PlacementStep(state.atlasIndex, (sourceX - positionX) * tileSize, sourceY - positionY);
                continue;
            }
            if (state.mode == StateMode.HORIZONTAL_SAMPLER) {
                steps[eventIndex] = new PlacementStep(state.atlasIndex, state.sourcePosition, 0);
                continue;
            }
            return null;
        }
        return new PlacementTemplateKey(TimePatternKey.from(program), steps);
    }

    private static ArrayList<PlacementRectangle> extractPlacementRectangles(boolean[] mask, int rows, int columns, int minPlacementArea) {
        ArrayList<PlacementRectangle> rectangles = new ArrayList<>();
        while (true) {
            PlacementRectangle rectangle = findLargestPlacementRectangle(mask, rows, columns);
            if (rectangle == null || rectangle.area() < minPlacementArea) {
                break;
            }
            rectangles.add(rectangle);
            for (int localY = 0; localY < rectangle.height; localY++) {
                int rowOffset = (rectangle.y + localY) * columns + rectangle.x;
                Arrays.fill(mask, rowOffset, rowOffset + rectangle.width, false);
            }
        }
        return rectangles;
    }

    private static PlacementRectangle findLargestPlacementRectangle(boolean[] mask, int rows, int columns) {
        int[] heights = new int[columns];
        PlacementRectangle best = null;
        int bestArea = 0;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int positionIndex = row * columns + column;
                heights[column] = mask[positionIndex] ? heights[column] + 1 : 0;
            }

            int[] stack = new int[columns + 1];
            int stackSize = 0;
            for (int column = 0; column <= columns; column++) {
                int currentHeight = (column < columns) ? heights[column] : 0;
                while (stackSize > 0 && heights[stack[stackSize - 1]] >= currentHeight) {
                    int index = stack[--stackSize];
                    int height = heights[index];
                    if (height <= 0) {
                        continue;
                    }
                    int left = (stackSize == 0) ? 0 : (stack[stackSize - 1] + 1);
                    int width = column - left;
                    int area = width * height;
                    if (area > bestArea) {
                        bestArea = area;
                        best = new PlacementRectangle(left, row - height + 1, width, height);
                    }
                }
                stack[stackSize++] = column;
            }
        }
        return best;
    }

    private static int estimatePlacementTemplateCost(PlacementTemplateKey template, int rectangleCount) {
        int size = estimateTimePatternSize(template.pattern) + estimateVarIntSize(rectangleCount);
        for (PlacementStep step : template.steps) {
            size += estimateVarIntSize(step.atlasIndex);
            size += estimateSignedVarIntSize(step.deltaX);
            size += estimateSignedVarIntSize(step.deltaY);
        }
        return size;
    }

    private static int estimatePlacementRectangleCost(PlacementRectangle rectangle) {
        return estimateVarIntSize(rectangle.x)
                + estimateVarIntSize(rectangle.y)
                + estimateVarIntSize(rectangle.width)
                + estimateVarIntSize(rectangle.height);
    }

    private static int estimateExactProgramCost(PositionProgram program) {
        int size = 1 + estimateTimePatternSize(TimePatternKey.from(program));
        for (StateEvent event : program.events) {
            EventState state = event.state;
            size += 1;
            if (state.mode == StateMode.SAME_POSITION_ATLAS) {
                size += estimateVarIntSize(state.atlasIndex);
                continue;
            }
            if (state.mode == StateMode.WINDOW_ATLAS) {
                size += estimateVarIntSize(state.atlasIndex);
                size += estimateVarIntSize(state.sourcePosition);
                continue;
            }
            if (state.mode == StateMode.HORIZONTAL_SAMPLER) {
                size += estimateVarIntSize(state.atlasIndex);
                size += estimateSignedVarIntSize(state.sourcePosition);
                continue;
            }
            throw new IllegalArgumentException("Position program is not exact-placement eligible");
        }
        return size;
    }

    private static int estimateTimePatternSize(TimePatternKey pattern) {
        int size = estimateVarIntSize(pattern.eventCount);
        int previousEnd = 0;
        for (int eventIndex = 0; eventIndex < pattern.eventCount; eventIndex++) {
            size += estimateVarIntSize(pattern.starts[eventIndex] - previousEnd);
            size += estimateVarIntSize(pattern.lengths[eventIndex]);
            previousEnd = pattern.starts[eventIndex] + pattern.lengths[eventIndex];
        }
        return size;
    }

    private static int activeOccurrenceCount(TimePatternKey pattern) {
        int total = 0;
        for (int eventIndex = 0; eventIndex < pattern.eventCount; eventIndex++) {
            total += pattern.lengths[eventIndex];
        }
        return total;
    }

    private static RowCandidate[] selectBestRow(List<RowCandidate>[] candidatesByColumn, TileBlock[] baseTiles, int row, int columns) {
        int[][] parent = new int[columns][];
        double[][] best = new double[columns][];
        for (int column = 0; column < columns; column++) {
            int candidateCount = candidatesByColumn[column].size();
            parent[column] = new int[candidateCount];
            best[column] = new double[candidateCount];
            Arrays.fill(parent[column], -1);
            Arrays.fill(best[column], Double.NEGATIVE_INFINITY);
        }

        for (int candidateIndex = 0; candidateIndex < candidatesByColumn[0].size(); candidateIndex++) {
            RowCandidate candidate = candidatesByColumn[0].get(candidateIndex);
            TileBlock baseTile = baseTiles[row * columns];
            best[0][candidateIndex] = candidate.score - estimateAtlasTransitionCost(null, candidate.tile, baseTile);
        }

        for (int column = 1; column < columns; column++) {
            int positionIndex = row * columns + column;
            TileBlock baseTile = baseTiles[positionIndex];
            for (int candidateIndex = 0; candidateIndex < candidatesByColumn[column].size(); candidateIndex++) {
                RowCandidate candidate = candidatesByColumn[column].get(candidateIndex);
                double bestScore = Double.NEGATIVE_INFINITY;
                int bestParent = -1;
                for (int previousIndex = 0; previousIndex < candidatesByColumn[column - 1].size(); previousIndex++) {
                    RowCandidate previous = candidatesByColumn[column - 1].get(previousIndex);
                    double score = best[column - 1][previousIndex] + candidate.score
                            - estimateAtlasTransitionCost(previous.tile, candidate.tile, baseTile);
                    if (score > bestScore) {
                        bestScore = score;
                        bestParent = previousIndex;
                    }
                }
                if (bestParent < 0) {
                    bestScore = candidate.score - estimateAtlasTransitionCost(null, candidate.tile, baseTile);
                }
                best[column][candidateIndex] = bestScore;
                parent[column][candidateIndex] = bestParent;
            }
        }

        int bestIndex = 0;
        double bestScore = best[columns - 1][0];
        for (int candidateIndex = 1; candidateIndex < best[columns - 1].length; candidateIndex++) {
            if (best[columns - 1][candidateIndex] > bestScore) {
                bestScore = best[columns - 1][candidateIndex];
                bestIndex = candidateIndex;
            }
        }

        RowCandidate[] selection = new RowCandidate[columns];
        for (int column = columns - 1; column >= 0; column--) {
            selection[column] = candidatesByColumn[column].get(bestIndex);
            bestIndex = parent[column][bestIndex];
            if (column > 0 && bestIndex < 0) {
                bestIndex = 0;
            }
        }
        return selection;
    }

    private static List<LocalTileScore> computeLocalTileScores(TileBlock[] sequence, boolean[] uncovered,
                                                               Map<TileBlock, Integer> payloadCostCache) throws IOException {
        LinkedHashMap<TileBlock, MutableLocalScore> scores = new LinkedHashMap<>();
        TileBlock previousTile = null;
        boolean previousUncovered = false;
        for (int frameIndex = 0; frameIndex < sequence.length; frameIndex++) {
            if (!uncovered[frameIndex]) {
                previousTile = null;
                previousUncovered = false;
                continue;
            }
            TileBlock tile = sequence[frameIndex];
            MutableLocalScore score = scores.computeIfAbsent(tile, ignored -> new MutableLocalScore());
            score.occurrences++;
            if (!previousUncovered || !tile.equals(previousTile)) {
                score.runs++;
            }
            previousTile = tile;
            previousUncovered = true;
        }

        ArrayList<LocalTileScore> localScores = new ArrayList<>();
        for (Map.Entry<TileBlock, MutableLocalScore> entry : scores.entrySet()) {
            int payloadSize = estimateTilePayloadSize(entry.getKey(), payloadCostCache);
            double score = entry.getValue().runs * RUN_VALUE
                    + entry.getValue().occurrences * OCCURRENCE_VALUE
                    + Math.min(4.0D, payloadSize * 0.10D);
            localScores.add(new LocalTileScore(entry.getKey(), score));
        }
        localScores.sort(Comparator.comparingDouble(LocalTileScore::score).reversed());
        return localScores;
    }

    private static double estimateAtlasTransitionCost(TileBlock leftTile, TileBlock tile, TileBlock baseTile) {
        if (leftTile != null && leftTile.equals(tile)) {
            return 0.0D;
        }
        if (tile.equals(baseTile)) {
            return 0.35D;
        }
        return 1.0D;
    }

    private static int estimateTilePayloadSize(TileBlock tile, Map<TileBlock, Integer> payloadCostCache) throws IOException {
        Integer cached = payloadCostCache.get(tile);
        if (cached != null) {
            return cached;
        }
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        writeTilePayload(payload, tile);
        int size = AfmaSectionPacker.pack(payload.toByteArray()).length;
        payloadCostCache.put(tile, size);
        return size;
    }

    private static EventState resolveEventState(TileBlock tile, Map<TileBlock, Integer> tileDictionaryIds,
                                                List<PatchSource> patchSources, Map<TileBlock, Integer> payloadCostCache,
                                                Map<RemapKey, Integer> remapCostCache, Map<TileBlock, int[]> paletteCache,
                                                Map<TileTransform, Integer> transformDictionaryIds,
                                                Map<PatchShapeKey, Integer> patchShapeDictionaryIds,
                                                int maxPatchPixels, int maxRemapColors) throws IOException {
        PatchMatch patchMatch = findBestPatchMatch(
                tile,
                patchSources,
                payloadCostCache,
                tileDictionaryIds,
                patchShapeDictionaryIds,
                new LinkedHashMap<>(),
                maxPatchPixels
        );
        PatchTransformMatch patchTransformMatch = findBestPatchTransformMatch(
                tile,
                patchSources,
                payloadCostCache,
                tileDictionaryIds,
                remapCostCache,
                transformDictionaryIds,
                patchShapeDictionaryIds,
                maxPatchPixels,
                PATCH_TRANSFORM_MAX_COLORS
        );
        TransformMatch transformMatch = findBestTransformMatch(
                tile,
                patchSources,
                payloadCostCache,
                tileDictionaryIds,
                remapCostCache,
                transformDictionaryIds,
                maxRemapColors,
                paletteCache
        );
        if (patchMatch == null && transformMatch == null) {
            return (patchTransformMatch == null) ? EventState.literal(tile) : patchTransformMatch.toEventState();
        }
        int bestPatchCost = (patchMatch != null) ? patchMatch.estimatedCost : Integer.MAX_VALUE;
        int bestPatchTransformCost = (patchTransformMatch != null) ? patchTransformMatch.estimatedCost : Integer.MAX_VALUE;
        int bestTransformCost = (transformMatch != null) ? transformMatch.estimatedCost : Integer.MAX_VALUE;
        if (bestPatchCost <= bestPatchTransformCost && bestPatchCost <= bestTransformCost) {
            return patchMatch.toEventState();
        }
        if (bestPatchTransformCost <= bestTransformCost) {
            return patchTransformMatch.toEventState();
        }
        return transformMatch.toEventState();
    }

    private static List<PatchSource> buildPatchSources(int positionIndex, TileBlock[] baseTiles, TileBlock[][] atlasTiles) {
        LinkedHashMap<TileBlock, PatchSource> uniqueSources = new LinkedHashMap<>();
        putPatchSource(uniqueSources, PatchSource.base(baseTiles[positionIndex]));
        for (int atlasIndex = 0; atlasIndex < atlasTiles.length; atlasIndex++) {
            putPatchSource(uniqueSources, PatchSource.samePositionAtlas(atlasIndex, atlasTiles[atlasIndex][positionIndex]));
        }
        for (int atlasIndex = 0; atlasIndex < atlasTiles.length; atlasIndex++) {
            for (int sourcePosition = 0; sourcePosition < atlasTiles[atlasIndex].length; sourcePosition++) {
                putPatchSource(uniqueSources, PatchSource.windowAtlas(atlasIndex, sourcePosition, atlasTiles[atlasIndex][sourcePosition]));
            }
        }
        return new ArrayList<>(uniqueSources.values());
    }

    private static void putPatchSource(Map<TileBlock, PatchSource> uniqueSources, PatchSource candidate) {
        PatchSource existing = uniqueSources.get(candidate.tile);
        if (existing == null || candidate.referenceCostEstimate < existing.referenceCostEstimate) {
            uniqueSources.put(candidate.tile, candidate);
        }
    }

    private static PatchMatch findBestPatchMatch(TileBlock targetTile, List<PatchSource> patchSources,
                                                 Map<TileBlock, Integer> payloadCostCache, Map<TileBlock, Integer> tileDictionaryIds,
                                                 Map<PatchShapeKey, Integer> patchShapeDictionaryIds,
                                                 Map<SparsePatchKey, Integer> patchCostCache, int maxPatchPixels) throws IOException {
        int literalCost = estimateTileReferenceCost(targetTile, tileDictionaryIds, payloadCostCache);
        PatchMatch bestMatch = null;
        int bestCost = literalCost;
        for (PatchSource source : patchSources) {
            if (source.tile.equals(targetTile)) {
                continue;
            }
            SparsePatch patch = createSparsePatch(source.tile, targetTile, maxPatchPixels);
            if (patch == null) {
                continue;
            }
            int patchCost = estimatePatchStateCost(source, patch, patchCostCache, patchShapeDictionaryIds, targetTile.width, targetTile.height);
            if (patchCost < bestCost) {
                bestCost = patchCost;
                bestMatch = new PatchMatch(source, patch, patchCost);
            }
        }
        return bestMatch;
    }

    private static TransformMatch findBestTransformMatch(TileBlock targetTile, List<PatchSource> patchSources,
                                                         Map<TileBlock, Integer> payloadCostCache, Map<TileBlock, Integer> tileDictionaryIds,
                                                         Map<RemapKey, Integer> remapCostCache, Map<TileTransform, Integer> transformDictionaryIds,
                                                         int maxRemapColors,
                                                         Map<TileBlock, int[]> paletteCache) throws IOException {
        int literalCost = estimateTileReferenceCost(targetTile, tileDictionaryIds, payloadCostCache);
        TransformMatch bestMatch = null;
        int bestCost = literalCost;
        for (PatchSource source : patchSources) {
            if (source.tile.equals(targetTile)) {
                continue;
            }

            TileTransform deltaTransform = createArgbDeltaTransform(source.tile, targetTile);
            if (deltaTransform != null) {
                int transformCost = estimateTransformStateCost(source, deltaTransform, remapCostCache, transformDictionaryIds);
                if (transformCost < bestCost) {
                    bestCost = transformCost;
                    bestMatch = new TransformMatch(source, deltaTransform, transformCost);
                }
            }

            TileTransform remapTransform = createPaletteRemapTransform(source.tile, targetTile, maxRemapColors, paletteCache);
            if (remapTransform != null) {
                int transformCost = estimateTransformStateCost(source, remapTransform, remapCostCache, transformDictionaryIds);
                if (transformCost < bestCost) {
                    bestCost = transformCost;
                    bestMatch = new TransformMatch(source, remapTransform, transformCost);
                }
            }
        }
        return bestMatch;
    }

    private static PatchTransformMatch findBestPatchTransformMatch(TileBlock targetTile, List<PatchSource> patchSources,
                                                                   Map<TileBlock, Integer> payloadCostCache,
                                                                   Map<TileBlock, Integer> tileDictionaryIds,
                                                                   Map<RemapKey, Integer> remapCostCache,
                                                                   Map<TileTransform, Integer> transformDictionaryIds,
                                                                   Map<PatchShapeKey, Integer> patchShapeDictionaryIds,
                                                                   int maxPatchPixels, int maxPatchTransformColors) throws IOException {
        int literalCost = estimateTileReferenceCost(targetTile, tileDictionaryIds, payloadCostCache);
        PatchTransformMatch bestMatch = null;
        int bestCost = literalCost;
        for (PatchSource source : patchSources) {
            if (source.tile.equals(targetTile)) {
                continue;
            }
            SparsePatch patch = createSparsePatch(source.tile, targetTile, maxPatchPixels);
            if (patch == null) {
                continue;
            }

            TileTransform deltaTransform = createSparseArgbDeltaTransform(source.tile, patch);
            if (deltaTransform != null) {
                int patchTransformCost = estimatePatchTransformStateCost(
                        source,
                        patch,
                        deltaTransform,
                        patchShapeDictionaryIds,
                        remapCostCache,
                        transformDictionaryIds,
                        targetTile.width,
                        targetTile.height
                );
                if (patchTransformCost < bestCost) {
                    bestCost = patchTransformCost;
                    bestMatch = new PatchTransformMatch(source, patch, deltaTransform, patchTransformCost);
                }
            }

            TileTransform remapTransform = createSparsePaletteRemapTransform(source.tile, patch, maxPatchTransformColors);
            if (remapTransform != null) {
                int patchTransformCost = estimatePatchTransformStateCost(
                        source,
                        patch,
                        remapTransform,
                        patchShapeDictionaryIds,
                        remapCostCache,
                        transformDictionaryIds,
                        targetTile.width,
                        targetTile.height
                );
                if (patchTransformCost < bestCost) {
                    bestCost = patchTransformCost;
                    bestMatch = new PatchTransformMatch(source, patch, remapTransform, patchTransformCost);
                }
            }
        }
        return bestMatch;
    }

    private static SparsePatch createSparsePatch(TileBlock sourceTile, TileBlock targetTile, int maxPatchPixels) {
        if (sourceTile.width != targetTile.width || sourceTile.height != targetTile.height) {
            return null;
        }
        int[] sourcePixels = sourceTile.pixels;
        int[] targetPixels = targetTile.pixels;
        int[] indices = new int[maxPatchPixels];
        int[] colors = new int[maxPatchPixels];
        int changed = 0;
        for (int pixelIndex = 0; pixelIndex < sourcePixels.length; pixelIndex++) {
            if (sourcePixels[pixelIndex] == targetPixels[pixelIndex]) {
                continue;
            }
            if (changed >= maxPatchPixels) {
                return null;
            }
            indices[changed] = pixelIndex;
            colors[changed] = targetPixels[pixelIndex];
            changed++;
        }
        if (changed <= 0) {
            return null;
        }
        return new SparsePatch(Arrays.copyOf(indices, changed), Arrays.copyOf(colors, changed));
    }

    private static TileTransform createSparseArgbDeltaTransform(TileBlock sourceTile, SparsePatch patch) {
        if (!matchesSparseArgbDelta(sourceTile, patch)) {
            return null;
        }
        int firstSource = sourceTile.pixels[patch.indices[0]];
        int firstTarget = patch.colors[0];
        return TileTransform.argbDelta(
                alpha(firstTarget) - alpha(firstSource),
                red(firstTarget) - red(firstSource),
                green(firstTarget) - green(firstSource),
                blue(firstTarget) - blue(firstSource)
        );
    }

    private static TileTransform createSparsePaletteRemapTransform(TileBlock sourceTile, SparsePatch patch, int maxColors) {
        if (patch == null || patch.indices.length <= 0 || maxColors <= 0) {
            return null;
        }
        LinkedHashMap<Integer, Integer> sourceIds = new LinkedHashMap<>();
        ArrayList<Integer> remapColors = new ArrayList<>();
        for (int changeIndex = 0; changeIndex < patch.indices.length; changeIndex++) {
            int source = sourceTile.pixels[patch.indices[changeIndex]];
            int target = patch.colors[changeIndex];
            Integer existingId = sourceIds.get(source);
            if (existingId == null) {
                if (sourceIds.size() >= maxColors) {
                    return null;
                }
                sourceIds.put(source, sourceIds.size());
                remapColors.add(target);
                continue;
            }
            if (remapColors.get(existingId) != target) {
                return null;
            }
        }
        if (remapColors.isEmpty()) {
            return null;
        }
        int[] orderedRemap = new int[remapColors.size()];
        for (int index = 0; index < remapColors.size(); index++) {
            orderedRemap[index] = remapColors.get(index);
        }
        return TileTransform.paletteRemap(orderedRemap);
    }

    private static int estimateTileReferenceCost(TileBlock tile, Map<TileBlock, Integer> tileDictionaryIds,
                                                 Map<TileBlock, Integer> payloadCostCache) throws IOException {
        Integer tileId = tileDictionaryIds.get(tile);
        if (tileId != null) {
            return 1 + estimateVarIntSize(tileId);
        }
        return 1 + estimateTilePayloadSize(tile, payloadCostCache);
    }

    private static int estimatePatchStateCost(PatchSource source, SparsePatch patch,
                                              Map<SparsePatchKey, Integer> patchCostCache,
                                              Map<PatchShapeKey, Integer> patchShapeDictionaryIds,
                                              int width, int height) {
        return 1 + source.referenceCostEstimate
                + estimateSparsePatchReferenceSize(patch, patchCostCache, patchShapeDictionaryIds, width, height);
    }

    private static int estimatePatchTransformStateCost(PatchSource source, SparsePatch patch, TileTransform transform,
                                                       Map<PatchShapeKey, Integer> patchShapeDictionaryIds,
                                                       Map<RemapKey, Integer> remapCostCache,
                                                       Map<TileTransform, Integer> transformDictionaryIds,
                                                       int width, int height) {
        return 1 + source.referenceCostEstimate
                + estimateSparsePatchTransformReferenceSize(patch, patchShapeDictionaryIds, width, height)
                + estimateTransformReferenceSize(transform, remapCostCache, transformDictionaryIds);
    }

    private static int estimateTransformStateCost(PatchSource source, TileTransform transform,
                                                  Map<RemapKey, Integer> remapCostCache,
                                                  Map<TileTransform, Integer> transformDictionaryIds) {
        return 1 + source.referenceCostEstimate + estimateTransformReferenceSize(transform, remapCostCache, transformDictionaryIds);
    }

    private static int estimateSparsePatchReferenceSize(SparsePatch patch, Map<SparsePatchKey, Integer> patchCostCache,
                                                        Map<PatchShapeKey, Integer> patchShapeDictionaryIds,
                                                        int width, int height) {
        PatchShapeKey key = PatchShapeKey.from(patch, width, height);
        Integer shapeId = patchShapeDictionaryIds.get(key);
        if (shapeId != null) {
            return 1 + estimateVarIntSize(shapeId) + estimateColorSequenceSize(patch.colors);
        }
        return 1 + estimateSparsePatchSize(patch, patchCostCache);
    }

    private static int estimateSparsePatchTransformReferenceSize(SparsePatch patch,
                                                                 Map<PatchShapeKey, Integer> patchShapeDictionaryIds,
                                                                 int width, int height) {
        PatchShapeKey key = PatchShapeKey.from(patch, width, height);
        Integer shapeId = patchShapeDictionaryIds.get(key);
        if (shapeId != null) {
            return 1 + estimateVarIntSize(shapeId);
        }
        int size = estimateVarIntSize(patch.changedPixelCount());
        int previousPlusOne = 0;
        for (int index : patch.indices) {
            size += estimateVarIntSize(index - previousPlusOne);
            previousPlusOne = index + 1;
        }
        return 1 + size;
    }

    private static int estimateTransformReferenceSize(TileTransform transform, Map<RemapKey, Integer> remapCostCache,
                                                      Map<TileTransform, Integer> transformDictionaryIds) {
        Integer transformId = transformDictionaryIds.get(transform);
        if (transformId != null) {
            return 1 + estimateVarIntSize(transformId);
        }
        return 1 + estimateTileTransformSize(transform, remapCostCache);
    }

    private static int estimateSparsePatchSize(SparsePatch patch, Map<SparsePatchKey, Integer> patchCostCache) {
        SparsePatchKey key = SparsePatchKey.from(patch);
        Integer cached = patchCostCache.get(key);
        if (cached != null) {
            return cached;
        }
        int size = estimateVarIntSize(patch.changedPixelCount());
        int previousPlusOne = 0;
        for (int index : patch.indices) {
            size += estimateVarIntSize(index - previousPlusOne);
            previousPlusOne = index + 1;
        }
        size += estimateColorSequenceSize(patch.colors);
        patchCostCache.put(key, size);
        return size;
    }

    private static int estimatePatchShapeSize(PatchShapeKey patchShape) {
        int size = estimateVarIntSize(patchShape.width) + estimateVarIntSize(patchShape.height) + estimateVarIntSize(patchShape.indices.length);
        int previousPlusOne = 0;
        for (int index : patchShape.indices) {
            size += estimateVarIntSize(index - previousPlusOne);
            previousPlusOne = index + 1;
        }
        return size;
    }

    private static int estimateTileTransformSize(TileTransform transform, Map<RemapKey, Integer> remapCostCache) {
        return switch (transform.mode) {
            case ARGB_DELTA -> 1
                    + estimateSignedVarIntSize(transform.deltaA)
                    + estimateSignedVarIntSize(transform.deltaR)
                    + estimateSignedVarIntSize(transform.deltaG)
                    + estimateSignedVarIntSize(transform.deltaB);
            case PALETTE_REMAP -> {
                RemapKey key = RemapKey.from(transform.remapColors);
                Integer cached = remapCostCache.get(key);
                if (cached != null) {
                    yield 1 + cached;
                }
                int size = estimateColorSequenceSize(transform.remapColors);
                remapCostCache.put(key, size);
                yield 1 + size;
            }
        };
    }

    private static int estimateColorSequenceSize(int[] colors) {
        if (colors.length == 0) {
            return 1;
        }
        boolean allEqual = true;
        int firstColor = colors[0];
        for (int colorIndex = 1; colorIndex < colors.length; colorIndex++) {
            if (colors[colorIndex] != firstColor) {
                allEqual = false;
                break;
            }
        }
        if (allEqual) {
            return 1 + Integer.BYTES;
        }

        LinkedHashMap<Integer, Integer> palette = new LinkedHashMap<>();
        for (int color : colors) {
            palette.putIfAbsent(color, palette.size());
            if (palette.size() > 256) {
                break;
            }
        }
        if (palette.size() <= 256) {
            int bitWidth = (palette.size() <= 2) ? 1 : (palette.size() <= 4 ? 2 : (palette.size() <= 16 ? 4 : 8));
            return 1 + estimateVarIntSize(palette.size()) + palette.size() * Integer.BYTES
                    + ((colors.length * bitWidth + 7) / 8);
        }
        return 1 + colors.length * Integer.BYTES;
    }

    private static int estimateVarIntSize(int value) {
        int remaining = value;
        int bytes = 1;
        while ((remaining >>>= 7) != 0) {
            bytes++;
        }
        return bytes;
    }

    private static int estimateSignedVarIntSize(int value) {
        return estimateVarIntSize(AfmaVarInts.zigZagEncode(value));
    }

    private static TileTransform createArgbDeltaTransform(TileBlock sourceTile, TileBlock targetTile) {
        if (sourceTile.width != targetTile.width || sourceTile.height != targetTile.height || sourceTile.pixels.length == 0) {
            return null;
        }
        int firstSource = sourceTile.pixels[0];
        int firstTarget = targetTile.pixels[0];
        int deltaA = alpha(firstTarget) - alpha(firstSource);
        int deltaR = red(firstTarget) - red(firstSource);
        int deltaG = green(firstTarget) - green(firstSource);
        int deltaB = blue(firstTarget) - blue(firstSource);
        for (int pixelIndex = 1; pixelIndex < sourceTile.pixels.length; pixelIndex++) {
            int source = sourceTile.pixels[pixelIndex];
            int target = targetTile.pixels[pixelIndex];
            if ((alpha(target) - alpha(source)) != deltaA
                    || (red(target) - red(source)) != deltaR
                    || (green(target) - green(source)) != deltaG
                    || (blue(target) - blue(source)) != deltaB) {
                return null;
            }
        }
        return TileTransform.argbDelta(deltaA, deltaR, deltaG, deltaB);
    }

    private static TileTransform createPaletteRemapTransform(TileBlock sourceTile, TileBlock targetTile,
                                                             int maxRemapColors, Map<TileBlock, int[]> paletteCache) {
        if (maxRemapColors <= 0 || sourceTile.width != targetTile.width || sourceTile.height != targetTile.height) {
            return null;
        }
        int[] sourcePalette = orderedPalette(sourceTile, maxRemapColors, paletteCache);
        if (sourcePalette == null) {
            return null;
        }
        LinkedHashMap<Integer, Integer> sourceIds = new LinkedHashMap<>();
        for (int paletteIndex = 0; paletteIndex < sourcePalette.length; paletteIndex++) {
            sourceIds.put(sourcePalette[paletteIndex], paletteIndex);
        }
        int[] remapColors = new int[sourcePalette.length];
        Arrays.fill(remapColors, Integer.MIN_VALUE);
        for (int pixelIndex = 0; pixelIndex < sourceTile.pixels.length; pixelIndex++) {
            Integer paletteIndex = sourceIds.get(sourceTile.pixels[pixelIndex]);
            if (paletteIndex == null) {
                return null;
            }
            int targetColor = targetTile.pixels[pixelIndex];
            int existing = remapColors[paletteIndex];
            if (existing == Integer.MIN_VALUE) {
                remapColors[paletteIndex] = targetColor;
            } else if (existing != targetColor) {
                return null;
            }
        }
        for (int paletteIndex = 0; paletteIndex < remapColors.length; paletteIndex++) {
            if (remapColors[paletteIndex] == Integer.MIN_VALUE) {
                return null;
            }
        }
        return TileTransform.paletteRemap(remapColors);
    }

    private static int[] orderedPalette(TileBlock tile, int maxColors, Map<TileBlock, int[]> paletteCache) {
        int[] cached = paletteCache.get(tile);
        if (cached != null) {
            return (cached.length > maxColors) ? null : cached;
        }
        LinkedHashMap<Integer, Integer> palette = new LinkedHashMap<>();
        for (int pixel : tile.pixels) {
            palette.putIfAbsent(pixel, palette.size());
            if (palette.size() > maxColors) {
                int[] overflow = new int[maxColors + 1];
                paletteCache.put(tile, overflow);
                return null;
            }
        }
        int[] colors = new int[palette.size()];
        for (Map.Entry<Integer, Integer> entry : palette.entrySet()) {
            colors[entry.getValue()] = entry.getKey();
        }
        paletteCache.put(tile, colors);
        return colors;
    }

    private static int[] orderedPaletteExact(TileBlock tile) {
        LinkedHashMap<Integer, Integer> palette = new LinkedHashMap<>();
        for (int pixel : tile.pixels) {
            palette.putIfAbsent(pixel, palette.size());
        }
        int[] colors = new int[palette.size()];
        for (Map.Entry<Integer, Integer> entry : palette.entrySet()) {
            colors[entry.getValue()] = entry.getKey();
        }
        return colors;
    }

    private static int alpha(int color) {
        return (color >>> 24) & 0xFF;
    }

    private static int red(int color) {
        return (color >>> 16) & 0xFF;
    }

    private static int green(int color) {
        return (color >>> 8) & 0xFF;
    }

    private static int blue(int color) {
        return color & 0xFF;
    }

    private static int argb(int alpha, int red, int green, int blue) {
        return ((alpha & 0xFF) << 24)
                | ((red & 0xFF) << 16)
                | ((green & 0xFF) << 8)
                | (blue & 0xFF);
    }

    private static <T> long repeatedOccurrenceCount(Map<T, Integer> counts) {
        long total = 0L;
        for (int count : counts.values()) {
            if (count >= 2) {
                total += count;
            }
        }
        return total;
    }

    private static <T> int maxFrequency(Map<T, Integer> counts) {
        int max = 0;
        for (int count : counts.values()) {
            if (count > max) {
                max = count;
            }
        }
        return max;
    }

    private static AtlasMatch findAtlasMatch(TileBlock tile, int positionIndex, TileBlock[][] atlasTiles,
                                             LinkedHashMap<TileBlock, Integer>[] atlasLookups) {
        for (int atlasIndex = 0; atlasIndex < atlasTiles.length; atlasIndex++) {
            if (atlasTiles[atlasIndex][positionIndex].equals(tile)) {
                return new AtlasMatch(atlasIndex, positionIndex, true);
            }
        }
        for (int atlasIndex = 0; atlasIndex < atlasTiles.length; atlasIndex++) {
            Integer sourcePosition = atlasLookups[atlasIndex].get(tile);
            if (sourcePosition != null) {
                return new AtlasMatch(atlasIndex, sourcePosition, false);
            }
        }
        return null;
    }

    private static HorizontalSamplerMatch findHorizontalSamplerMatch(TileBlock tile, int positionIndex, AtlasImage[] atlasImages,
                                                                     int width, int tileSize, int columns) {
        int positionX = positionIndex % columns;
        int positionY = positionIndex / columns;
        int tileX = positionX * tileSize;
        int tileY = positionY * tileSize;
        int minX = Math.max(0, tileX - tileSize + 1);
        int maxX = Math.min(width - tile.width, tileX + tileSize - 1);
        for (int atlasIndex = 0; atlasIndex < atlasImages.length; atlasIndex++) {
            AtlasImage atlas = atlasImages[atlasIndex];
            for (int sourceX = minX; sourceX <= maxX; sourceX++) {
                if (sourceX == tileX) {
                    continue;
                }
                if (atlasWindowEquals(tile, atlas, sourceX, tileY)) {
                    return new HorizontalSamplerMatch(atlasIndex, sourceX - tileX);
                }
            }
        }
        return null;
    }

    private static boolean atlasWindowEquals(TileBlock tile, AtlasImage atlas, int sourceX, int sourceY) {
        for (int y = 0; y < tile.height; y++) {
            int atlasOffset = (sourceY + y) * atlas.width + sourceX;
            int tileOffset = y * tile.width;
            for (int x = 0; x < tile.width; x++) {
                if (atlas.pixels[atlasOffset + x] != tile.pixels[tileOffset + x]) {
                    return false;
                }
            }
        }
        return true;
    }

    private static AtlasImage[] buildAtlasImages(TileBlock[][] atlasTiles, int width, int height, int tileSize, int columns, int rows) {
        AtlasImage[] images = new AtlasImage[atlasTiles.length];
        for (int atlasIndex = 0; atlasIndex < atlasTiles.length; atlasIndex++) {
            int[] pixels = new int[width * height];
            for (int row = 0; row < rows; row++) {
                int tileY = row * tileSize;
                for (int column = 0; column < columns; column++) {
                    int positionIndex = row * columns + column;
                    TileBlock tile = atlasTiles[atlasIndex][positionIndex];
                    int tileX = column * tileSize;
                    for (int y = 0; y < tile.height; y++) {
                        int targetOffset = (tileY + y) * width + tileX;
                        int sourceOffset = y * tile.width;
                        System.arraycopy(tile.pixels, sourceOffset, pixels, targetOffset, tile.width);
                    }
                }
            }
            images[atlasIndex] = new AtlasImage(width, pixels);
        }
        return images;
    }

    private static TileBlock extractAtlasWindow(AtlasImage atlas, int sourceX, int sourceY, int tileWidth, int tileHeight) throws IOException {
        if (sourceX < 0 || sourceY < 0 || sourceX + tileWidth > atlas.width || sourceY + tileHeight > atlas.height()) {
            throw new IOException("Atlas sampler window is invalid");
        }
        int[] pixels = new int[tileWidth * tileHeight];
        int writeOffset = 0;
        for (int y = 0; y < tileHeight; y++) {
            int sourceOffset = (sourceY + y) * atlas.width + sourceX;
            System.arraycopy(atlas.pixels, sourceOffset, pixels, writeOffset, tileWidth);
            writeOffset += tileWidth;
        }
        return new TileBlock(tileWidth, tileHeight, pixels);
    }

    private static LinkedHashMap<TileBlock, Integer> buildTileDictionary(LinkedHashMap<TileBlock, Integer> frequencies, int minRepeatCount) {
        List<Map.Entry<TileBlock, Integer>> candidates = new ArrayList<>();
        for (Map.Entry<TileBlock, Integer> entry : frequencies.entrySet()) {
            if (entry.getValue() >= minRepeatCount) {
                candidates.add(entry);
            }
        }
        candidates.sort(Comparator.<Map.Entry<TileBlock, Integer>>comparingInt(Map.Entry::getValue).reversed());
        LinkedHashMap<TileBlock, Integer> ids = new LinkedHashMap<>();
        for (Map.Entry<TileBlock, Integer> entry : candidates) {
            ids.put(entry.getKey(), ids.size());
        }
        return ids;
    }

    private static LinkedHashMap<TileTransform, Integer> buildTransformDictionary(LinkedHashMap<TileTransform, Integer> frequencies, int minRepeatCount) {
        List<Map.Entry<TileTransform, Integer>> candidates = new ArrayList<>();
        for (Map.Entry<TileTransform, Integer> entry : frequencies.entrySet()) {
            if (entry.getValue() >= minRepeatCount) {
                candidates.add(entry);
            }
        }
        candidates.sort(Comparator.<Map.Entry<TileTransform, Integer>>comparingInt(Map.Entry::getValue).reversed());
        LinkedHashMap<TileTransform, Integer> ids = new LinkedHashMap<>();
        for (Map.Entry<TileTransform, Integer> entry : candidates) {
            ids.put(entry.getKey(), ids.size());
        }
        return ids;
    }

    private static LinkedHashMap<PatchShapeKey, Integer> buildPatchShapeDictionary(LinkedHashMap<PatchShapeKey, Integer> frequencies) {
        List<Map.Entry<PatchShapeKey, Integer>> candidates = new ArrayList<>(frequencies.entrySet());
        candidates.sort(Comparator.<Map.Entry<PatchShapeKey, Integer>>comparingInt(Map.Entry::getValue).reversed());
        LinkedHashMap<PatchShapeKey, Integer> ids = new LinkedHashMap<>();
        for (Map.Entry<PatchShapeKey, Integer> entry : candidates) {
            int shapeId = ids.size();
            int shapeSize = estimatePatchShapeSize(entry.getKey());
            int referenceSize = estimateVarIntSize(shapeId);
            int netSavings = entry.getValue() * (shapeSize - referenceSize) - shapeSize;
            if (netSavings <= 0) {
                continue;
            }
            ids.put(entry.getKey(), shapeId);
        }
        return ids;
    }

    private static LinkedHashMap<TimePatternKey, Integer> buildTimePatternDictionary(LinkedHashMap<TimePatternKey, Integer> frequencies,
                                                                                    int minRepeatCount) {
        List<Map.Entry<TimePatternKey, Integer>> candidates = new ArrayList<>();
        for (Map.Entry<TimePatternKey, Integer> entry : frequencies.entrySet()) {
            if (entry.getValue() >= minRepeatCount) {
                candidates.add(entry);
            }
        }
        candidates.sort(Comparator.<Map.Entry<TimePatternKey, Integer>>comparingInt(Map.Entry::getValue).reversed());
        LinkedHashMap<TimePatternKey, Integer> ids = new LinkedHashMap<>();
        for (Map.Entry<TimePatternKey, Integer> entry : candidates) {
            ids.put(entry.getKey(), ids.size());
        }
        return ids;
    }

    private static void writePredictedTile(TileBlock tile, TileBlock leftTile, Map<TileBlock, Integer> dictionaryIds,
                                           ByteArrayOutputStream metaOut, ByteArrayOutputStream refOut,
                                           ByteArrayOutputStream inlineOut) throws IOException {
        Integer tileId = dictionaryIds.get(tile);
        if (leftTile != null && leftTile.equals(tile)) {
            AfmaVarInts.writeUnsigned(metaOut, 2);
        } else if (tileId != null) {
            AfmaVarInts.writeUnsigned(metaOut, 1);
            AfmaVarInts.writeUnsigned(refOut, tileId);
        } else {
            AfmaVarInts.writeUnsigned(metaOut, 0);
            writeTilePayload(inlineOut, tile);
        }
    }

    private static void writeAtlasTile(TileBlock tile, TileBlock leftTile, TileBlock baseTile, TileBlock previousAtlasTile,
                                       Map<TileBlock, Integer> dictionaryIds, ByteArrayOutputStream metaOut,
                                       ByteArrayOutputStream refOut, ByteArrayOutputStream inlineOut) throws IOException {
        Integer tileId = dictionaryIds.get(tile);
        if (tile.equals(baseTile)) {
            AfmaVarInts.writeUnsigned(metaOut, 3);
        } else if (previousAtlasTile != null && previousAtlasTile.equals(tile)) {
            AfmaVarInts.writeUnsigned(metaOut, 4);
        } else if (leftTile != null && leftTile.equals(tile)) {
            AfmaVarInts.writeUnsigned(metaOut, 2);
        } else if (tileId != null) {
            AfmaVarInts.writeUnsigned(metaOut, 1);
            AfmaVarInts.writeUnsigned(refOut, tileId);
        } else {
            AfmaVarInts.writeUnsigned(metaOut, 0);
            writeTilePayload(inlineOut, tile);
        }
    }

    private static TileBlock readPredictedTile(ByteArrayInputStream metaIn, ByteArrayInputStream refIn,
                                               ByteArrayInputStream inlineIn, TileBlock[] dictionary,
                                               int expectedWidth, int expectedHeight, TileBlock leftTile) throws IOException {
        int mode = AfmaVarInts.readUnsigned(metaIn);
        TileBlock tile;
        if (mode == 2) {
            if (leftTile == null) {
                throw new IOException("Left predictor is unavailable");
            }
            tile = leftTile;
        } else if (mode == 1) {
            int tileId = AfmaVarInts.readUnsigned(refIn);
            if (tileId < 0 || tileId >= dictionary.length) {
                throw new IOException("Tile dictionary reference is invalid: " + tileId);
            }
            tile = dictionary[tileId];
        } else if (mode == 0) {
            tile = readTilePayload(inlineIn, expectedWidth, expectedHeight);
        } else {
            throw new IOException("Unsupported predicted tile mode: " + mode);
        }
        if (tile.width != expectedWidth || tile.height != expectedHeight) {
            throw new IOException("Tile dimensions do not match the position geometry");
        }
        return tile;
    }

    private static TileBlock readAtlasTile(ByteArrayInputStream metaIn, ByteArrayInputStream refIn,
                                           ByteArrayInputStream inlineIn, TileBlock[] dictionary,
                                           int expectedWidth, int expectedHeight, TileBlock leftTile,
                                           TileBlock baseTile, TileBlock previousAtlasTile) throws IOException {
        int mode = AfmaVarInts.readUnsigned(metaIn);
        TileBlock tile;
        if (mode == 4) {
            if (previousAtlasTile == null) {
                throw new IOException("Previous atlas predictor is unavailable");
            }
            tile = previousAtlasTile;
        } else if (mode == 3) {
            tile = baseTile;
        } else if (mode == 2) {
            if (leftTile == null) {
                throw new IOException("Left atlas predictor is unavailable");
            }
            tile = leftTile;
        } else if (mode == 1) {
            int tileId = AfmaVarInts.readUnsigned(refIn);
            if (tileId < 0 || tileId >= dictionary.length) {
                throw new IOException("Tile dictionary reference is invalid: " + tileId);
            }
            tile = dictionary[tileId];
        } else if (mode == 0) {
            tile = readTilePayload(inlineIn, expectedWidth, expectedHeight);
        } else {
            throw new IOException("Unsupported atlas tile mode: " + mode);
        }
        if (tile.width != expectedWidth || tile.height != expectedHeight) {
            throw new IOException("Tile dimensions do not match the position geometry");
        }
        return tile;
    }

    private static void writeTileReference(TileBlock tile, Map<TileBlock, Integer> dictionaryIds,
                                           ByteArrayOutputStream metaOut, ByteArrayOutputStream refOut,
                                           ByteArrayOutputStream inlineOut) throws IOException {
        Integer tileId = dictionaryIds.get(tile);
        if (tileId != null) {
            AfmaVarInts.writeUnsigned(metaOut, 1);
            AfmaVarInts.writeUnsigned(refOut, tileId);
        } else {
            AfmaVarInts.writeUnsigned(metaOut, 0);
            writeTilePayload(inlineOut, tile);
        }
    }

    private static TileBlock readTileReference(ByteArrayInputStream metaIn, ByteArrayInputStream refIn,
                                               ByteArrayInputStream inlineIn, TileBlock[] dictionary,
                                               int expectedWidth, int expectedHeight) throws IOException {
        int mode = AfmaVarInts.readUnsigned(metaIn);
        TileBlock tile;
        if (mode == 1) {
            int tileId = AfmaVarInts.readUnsigned(refIn);
            if (tileId < 0 || tileId >= dictionary.length) {
                throw new IOException("Tile dictionary reference is invalid: " + tileId);
            }
            tile = dictionary[tileId];
        } else if (mode == 0) {
            tile = readTilePayload(inlineIn, expectedWidth, expectedHeight);
        } else {
            throw new IOException("Unsupported tile reference mode: " + mode);
        }
        if (tile.width != expectedWidth || tile.height != expectedHeight) {
            throw new IOException("Tile dimensions do not match the position geometry");
        }
        return tile;
    }

    private static void writeSparsePatch(ByteArrayOutputStream out, SparsePatch patch) throws IOException {
        AfmaVarInts.writeUnsigned(out, patch.changedPixelCount());
        int previousPlusOne = 0;
        for (int index : patch.indices) {
            AfmaVarInts.writeUnsigned(out, index - previousPlusOne);
            previousPlusOne = index + 1;
        }
        writeColorSequence(out, patch.colors);
    }

    private static SparsePatch readSparsePatch(ByteArrayInputStream in, int pixelCount) throws IOException {
        int changedCount = AfmaVarInts.readUnsigned(in);
        int[] indices = new int[changedCount];
        int previousPlusOne = 0;
        for (int changeIndex = 0; changeIndex < changedCount; changeIndex++) {
            int delta = AfmaVarInts.readUnsigned(in);
            int index = previousPlusOne + delta;
            if (index < 0 || index >= pixelCount) {
                throw new IOException("Sparse patch index is invalid: " + index);
            }
            indices[changeIndex] = index;
            previousPlusOne = index + 1;
        }
        int[] colors = readColorSequence(in, changedCount);
        return new SparsePatch(indices, colors);
    }

    private static void writePatchShape(ByteArrayOutputStream out, PatchShapeKey patchShape) throws IOException {
        AfmaVarInts.writeUnsigned(out, patchShape.width);
        AfmaVarInts.writeUnsigned(out, patchShape.height);
        AfmaVarInts.writeUnsigned(out, patchShape.indices.length);
        int previousPlusOne = 0;
        for (int index : patchShape.indices) {
            AfmaVarInts.writeUnsigned(out, index - previousPlusOne);
            previousPlusOne = index + 1;
        }
    }

    private static PatchShape readPatchShape(ByteArrayInputStream in, int maxPixelCount) throws IOException {
        int width = AfmaVarInts.readUnsigned(in);
        int height = AfmaVarInts.readUnsigned(in);
        if (width <= 0 || height <= 0 || (width * height) > maxPixelCount) {
            throw new IOException("Patch shape dimensions are invalid");
        }
        int changedCount = AfmaVarInts.readUnsigned(in);
        int[] indices = new int[changedCount];
        int previousPlusOne = 0;
        for (int changeIndex = 0; changeIndex < changedCount; changeIndex++) {
            int delta = AfmaVarInts.readUnsigned(in);
            int index = previousPlusOne + delta;
            if (index < 0 || index >= (width * height)) {
                throw new IOException("Patch shape index is invalid: " + index);
            }
            indices[changeIndex] = index;
            previousPlusOne = index + 1;
        }
        return new PatchShape(width, height, indices);
    }

    private static void writeSparsePatchReference(ByteArrayOutputStream metaOut,
                                                  ByteArrayOutputStream refOut,
                                                  ByteArrayOutputStream payloadOut,
                                                  SparsePatch patch,
                                                  int width,
                                                  int height,
                                                  Map<PatchShapeKey, Integer> patchShapeDictionaryIds) throws IOException {
        PatchShapeKey key = PatchShapeKey.from(patch, width, height);
        Integer shapeId = patchShapeDictionaryIds.get(key);
        if (shapeId != null) {
            AfmaVarInts.writeUnsigned(metaOut, 1);
            AfmaVarInts.writeUnsigned(refOut, shapeId);
            writeColorSequence(payloadOut, patch.colors);
        } else {
            AfmaVarInts.writeUnsigned(metaOut, 0);
            writeSparsePatch(payloadOut, patch);
        }
    }

    private static SparsePatch readSparsePatchReference(ByteArrayInputStream metaIn,
                                                        ByteArrayInputStream refIn,
                                                        ByteArrayInputStream payloadIn,
                                                        PatchShape[] patchShapes,
                                                        int expectedWidth,
                                                        int expectedHeight) throws IOException {
        int mode = AfmaVarInts.readUnsigned(metaIn);
        if (mode == 0) {
            return readSparsePatch(payloadIn, expectedWidth * expectedHeight);
        }
        if (mode == 1) {
            int shapeId = AfmaVarInts.readUnsigned(refIn);
            if (shapeId < 0 || shapeId >= patchShapes.length) {
                throw new IOException("Patch shape reference is invalid: " + shapeId);
            }
            PatchShape shape = patchShapes[shapeId];
            if (shape.width != expectedWidth || shape.height != expectedHeight) {
                throw new IOException("Patch shape dimensions do not match the position geometry");
            }
            int[] colors = readColorSequence(payloadIn, shape.indices.length);
            return new SparsePatch(Arrays.copyOf(shape.indices, shape.indices.length), colors);
        }
        throw new IOException("Unsupported sparse patch reference mode: " + mode);
    }

    private static void writeSparsePatchTransformReference(ByteArrayOutputStream metaOut,
                                                           ByteArrayOutputStream refOut,
                                                           SparsePatch patch,
                                                           int width,
                                                           int height,
                                                           Map<PatchShapeKey, Integer> patchShapeDictionaryIds) throws IOException {
        PatchShapeKey key = PatchShapeKey.from(patch, width, height);
        Integer shapeId = patchShapeDictionaryIds.get(key);
        if (shapeId != null) {
            AfmaVarInts.writeUnsigned(metaOut, 1);
            AfmaVarInts.writeUnsigned(refOut, shapeId);
        } else {
            AfmaVarInts.writeUnsigned(metaOut, 0);
            writePatchShape(metaOut, key);
        }
    }

    private static SparsePatch readSparsePatchTransformReference(ByteArrayInputStream metaIn,
                                                                 ByteArrayInputStream refIn,
                                                                 PatchShape[] patchShapes,
                                                                 int expectedWidth,
                                                                 int expectedHeight) throws IOException {
        int mode = AfmaVarInts.readUnsigned(metaIn);
        if (mode == 0) {
            PatchShape shape = readPatchShape(metaIn, expectedWidth * expectedHeight);
            if (shape.width != expectedWidth || shape.height != expectedHeight) {
                throw new IOException("Inline patch transform shape dimensions do not match the position geometry");
            }
            return new SparsePatch(Arrays.copyOf(shape.indices, shape.indices.length), new int[shape.indices.length]);
        }
        if (mode == 1) {
            int shapeId = AfmaVarInts.readUnsigned(refIn);
            if (shapeId < 0 || shapeId >= patchShapes.length) {
                throw new IOException("Patch transform shape reference is invalid: " + shapeId);
            }
            PatchShape shape = patchShapes[shapeId];
            if (shape.width != expectedWidth || shape.height != expectedHeight) {
                throw new IOException("Patch transform shape dimensions do not match the position geometry");
            }
            return new SparsePatch(Arrays.copyOf(shape.indices, shape.indices.length), new int[shape.indices.length]);
        }
        throw new IOException("Unsupported sparse patch transform reference mode: " + mode);
    }

    private static void writeColorSequence(ByteArrayOutputStream out, int[] colors) throws IOException {
        if (colors.length == 0) {
            out.write(0);
            DataOutputStream dataOut = new DataOutputStream(out);
            dataOut.writeInt(0);
            dataOut.flush();
            return;
        }

        boolean allEqual = true;
        int firstColor = colors[0];
        for (int colorIndex = 1; colorIndex < colors.length; colorIndex++) {
            if (colors[colorIndex] != firstColor) {
                allEqual = false;
                break;
            }
        }
        if (allEqual) {
            out.write(0);
            DataOutputStream dataOut = new DataOutputStream(out);
            dataOut.writeInt(firstColor);
            dataOut.flush();
            return;
        }

        LinkedHashMap<Integer, Integer> palette = new LinkedHashMap<>();
        for (int color : colors) {
            palette.putIfAbsent(color, palette.size());
            if (palette.size() > 256) {
                break;
            }
        }
        if (palette.size() <= 256) {
            out.write(1);
            AfmaVarInts.writeUnsigned(out, palette.size());
            DataOutputStream dataOut = new DataOutputStream(out);
            int[] paletteEntries = new int[palette.size()];
            for (Map.Entry<Integer, Integer> entry : palette.entrySet()) {
                paletteEntries[entry.getValue()] = entry.getKey();
            }
            for (int paletteEntry : paletteEntries) {
                dataOut.writeInt(paletteEntry);
            }
            dataOut.flush();
            int bitWidth = (palette.size() <= 2) ? 1 : (palette.size() <= 4 ? 2 : (palette.size() <= 16 ? 4 : 8));
            int accumulator = 0;
            int bits = 0;
            for (int color : colors) {
                accumulator |= (palette.get(color) << bits);
                bits += bitWidth;
                while (bits >= 8) {
                    out.write(accumulator & 0xFF);
                    accumulator >>>= 8;
                    bits -= 8;
                }
            }
            if (bits > 0) {
                out.write(accumulator & 0xFF);
            }
            return;
        }

        out.write(2);
        DataOutputStream dataOut = new DataOutputStream(out);
        for (int color : colors) {
            dataOut.writeInt(color);
        }
        dataOut.flush();
    }

    private static int[] readColorSequence(ByteArrayInputStream in, int count) throws IOException {
        int mode = in.read();
        if (mode < 0) {
            throw new IOException("Color sequence ended early");
        }
        return switch (mode) {
            case 0 -> {
                DataInputStream dataIn = new DataInputStream(in);
                int[] colors = new int[count];
                Arrays.fill(colors, dataIn.readInt());
                yield colors;
            }
            case 1 -> {
                int paletteSize = AfmaVarInts.readUnsigned(in);
                int[] palette = new int[paletteSize];
                DataInputStream dataIn = new DataInputStream(in);
                for (int paletteIndex = 0; paletteIndex < paletteSize; paletteIndex++) {
                    palette[paletteIndex] = dataIn.readInt();
                }
                int bitWidth = (paletteSize <= 2) ? 1 : (paletteSize <= 4 ? 2 : (paletteSize <= 16 ? 4 : 8));
                int[] colors = new int[count];
                int accumulator = 0;
                int bits = 0;
                for (int colorIndex = 0; colorIndex < count; colorIndex++) {
                    while (bits < bitWidth) {
                        int next = in.read();
                        if (next < 0) {
                            throw new IOException("Palette color sequence ended early");
                        }
                        accumulator |= (next << bits);
                        bits += 8;
                    }
                    int mask = (1 << bitWidth) - 1;
                    int paletteIndex = accumulator & mask;
                    accumulator >>>= bitWidth;
                    bits -= bitWidth;
                    if (paletteIndex < 0 || paletteIndex >= palette.length) {
                        throw new IOException("Palette color index is invalid: " + paletteIndex);
                    }
                    colors[colorIndex] = palette[paletteIndex];
                }
                yield colors;
            }
            case 2 -> {
                int[] colors = new int[count];
                DataInputStream dataIn = new DataInputStream(in);
                for (int colorIndex = 0; colorIndex < count; colorIndex++) {
                    colors[colorIndex] = dataIn.readInt();
                }
                yield colors;
            }
            default -> throw new IOException("Unsupported color sequence mode: " + mode);
        };
    }

    private static void writeTransformSourceMeta(ByteArrayOutputStream out, PatchSourceType sourceType) {
        AfmaVarInts.writeUnsigned(out, switch (sourceType) {
            case BASE -> 0;
            case SAME_POSITION_ATLAS -> 1;
            case WINDOW_ATLAS -> 2;
        });
    }

    private static PatchSourceType readTransformSourceMeta(ByteArrayInputStream in) throws IOException {
        int mode = AfmaVarInts.readUnsigned(in);
        return switch (mode) {
            case 0 -> PatchSourceType.BASE;
            case 1 -> PatchSourceType.SAME_POSITION_ATLAS;
            case 2 -> PatchSourceType.WINDOW_ATLAS;
            default -> throw new IOException("Unsupported transform source mode: " + mode);
        };
    }

    private static void writeTransformReference(ByteArrayOutputStream metaOut,
                                                ByteArrayOutputStream refOut,
                                                ByteArrayOutputStream payloadOut,
                                                TileTransform transform,
                                                Map<TileTransform, Integer> transformDictionaryIds) throws IOException {
        Integer transformId = transformDictionaryIds.get(transform);
        if (transformId != null) {
            AfmaVarInts.writeUnsigned(metaOut, 0);
            AfmaVarInts.writeUnsigned(refOut, transformId);
        } else {
            AfmaVarInts.writeUnsigned(metaOut, 1);
            writeTileTransform(metaOut, payloadOut, transform);
        }
    }

    private static void writeTileTransform(ByteArrayOutputStream metaOut, ByteArrayOutputStream payloadOut,
                                           TileTransform transform) throws IOException {
        switch (transform.mode) {
            case ARGB_DELTA -> {
                AfmaVarInts.writeUnsigned(metaOut, 0);
                AfmaVarInts.writeUnsigned(payloadOut, AfmaVarInts.zigZagEncode(transform.deltaA));
                AfmaVarInts.writeUnsigned(payloadOut, AfmaVarInts.zigZagEncode(transform.deltaR));
                AfmaVarInts.writeUnsigned(payloadOut, AfmaVarInts.zigZagEncode(transform.deltaG));
                AfmaVarInts.writeUnsigned(payloadOut, AfmaVarInts.zigZagEncode(transform.deltaB));
            }
            case PALETTE_REMAP -> {
                AfmaVarInts.writeUnsigned(metaOut, 1);
                writeColorSequence(payloadOut, transform.remapColors);
            }
        }
    }

    private static void writeDictionaryTileTransform(ByteArrayOutputStream metaOut, ByteArrayOutputStream payloadOut,
                                                     TileTransform transform) throws IOException {
        switch (transform.mode) {
            case ARGB_DELTA -> writeTileTransform(metaOut, payloadOut, transform);
            case PALETTE_REMAP -> {
                AfmaVarInts.writeUnsigned(metaOut, 1);
                AfmaVarInts.writeUnsigned(metaOut, transform.remapColors.length);
                writeColorSequence(payloadOut, transform.remapColors);
            }
        }
    }

    private static TileTransform readTileTransform(ByteArrayInputStream metaIn, ByteArrayInputStream payloadIn,
                                                   TileBlock sourceTile) throws IOException {
        int mode = AfmaVarInts.readUnsigned(metaIn);
        if (mode == 0) {
            return TileTransform.argbDelta(
                    AfmaVarInts.zigZagDecode(AfmaVarInts.readUnsigned(payloadIn)),
                    AfmaVarInts.zigZagDecode(AfmaVarInts.readUnsigned(payloadIn)),
                    AfmaVarInts.zigZagDecode(AfmaVarInts.readUnsigned(payloadIn)),
                    AfmaVarInts.zigZagDecode(AfmaVarInts.readUnsigned(payloadIn))
            );
        }
        if (mode == 1) {
            return TileTransform.paletteRemap(readColorSequence(payloadIn, orderedPaletteExact(sourceTile).length));
        }
        throw new IOException("Unsupported tile transform mode: " + mode);
    }

    private static TileTransform readDictionaryTileTransform(ByteArrayInputStream metaIn, ByteArrayInputStream payloadIn) throws IOException {
        int mode = AfmaVarInts.readUnsigned(metaIn);
        if (mode == 0) {
            return TileTransform.argbDelta(
                    AfmaVarInts.zigZagDecode(AfmaVarInts.readUnsigned(payloadIn)),
                    AfmaVarInts.zigZagDecode(AfmaVarInts.readUnsigned(payloadIn)),
                    AfmaVarInts.zigZagDecode(AfmaVarInts.readUnsigned(payloadIn)),
                    AfmaVarInts.zigZagDecode(AfmaVarInts.readUnsigned(payloadIn))
            );
        }
        if (mode == 1) {
            return TileTransform.paletteRemap(readColorSequence(payloadIn, AfmaVarInts.readUnsigned(metaIn)));
        }
        throw new IOException("Unsupported dictionary tile transform mode: " + mode);
    }

    private static TileBlock applySparsePatch(TileBlock sourceTile, SparsePatch patch) {
        int[] pixels = Arrays.copyOf(sourceTile.pixels, sourceTile.pixels.length);
        for (int changeIndex = 0; changeIndex < patch.indices.length; changeIndex++) {
            pixels[patch.indices[changeIndex]] = patch.colors[changeIndex];
        }
        return new TileBlock(sourceTile.width, sourceTile.height, pixels);
    }

    private static TileBlock applySparsePatchTransform(TileBlock sourceTile, SparsePatch patch, TileTransform transform) throws IOException {
        int[] pixels = Arrays.copyOf(sourceTile.pixels, sourceTile.pixels.length);
        switch (transform.mode) {
            case ARGB_DELTA -> {
                for (int changeIndex = 0; changeIndex < patch.indices.length; changeIndex++) {
                    int pixelIndex = patch.indices[changeIndex];
                    int source = sourceTile.pixels[pixelIndex];
                    int alpha = alpha(source) + transform.deltaA;
                    int red = red(source) + transform.deltaR;
                    int green = green(source) + transform.deltaG;
                    int blue = blue(source) + transform.deltaB;
                    if (alpha < 0 || alpha > 255 || red < 0 || red > 255 || green < 0 || green > 255 || blue < 0 || blue > 255) {
                        throw new IOException("Sparse ARGB delta transform overflowed pixel range");
                    }
                    pixels[pixelIndex] = argb(alpha, red, green, blue);
                }
            }
            case PALETTE_REMAP -> {
                LinkedHashMap<Integer, Integer> sourceIds = new LinkedHashMap<>();
                for (int changeIndex = 0; changeIndex < patch.indices.length; changeIndex++) {
                    int pixelIndex = patch.indices[changeIndex];
                    int source = sourceTile.pixels[pixelIndex];
                    sourceIds.putIfAbsent(source, sourceIds.size());
                }
                if (sourceIds.size() != transform.remapColors.length) {
                    throw new IOException("Sparse palette remap cardinality is invalid");
                }
                for (int changeIndex = 0; changeIndex < patch.indices.length; changeIndex++) {
                    int pixelIndex = patch.indices[changeIndex];
                    Integer sourceId = sourceIds.get(sourceTile.pixels[pixelIndex]);
                    if (sourceId == null || sourceId < 0 || sourceId >= transform.remapColors.length) {
                        throw new IOException("Sparse palette remap source index is invalid");
                    }
                    pixels[pixelIndex] = transform.remapColors[sourceId];
                }
            }
        }
        return new TileBlock(sourceTile.width, sourceTile.height, pixels);
    }

    private static TileBlock applyTileTransform(TileBlock sourceTile, TileTransform transform) throws IOException {
        return switch (transform.mode) {
            case ARGB_DELTA -> {
                int[] pixels = new int[sourceTile.pixels.length];
                for (int pixelIndex = 0; pixelIndex < sourceTile.pixels.length; pixelIndex++) {
                    int source = sourceTile.pixels[pixelIndex];
                    int alpha = alpha(source) + transform.deltaA;
                    int red = red(source) + transform.deltaR;
                    int green = green(source) + transform.deltaG;
                    int blue = blue(source) + transform.deltaB;
                    if (alpha < 0 || alpha > 255 || red < 0 || red > 255 || green < 0 || green > 255 || blue < 0 || blue > 255) {
                        throw new IOException("ARGB delta transform overflowed pixel range");
                    }
                    pixels[pixelIndex] = (alpha << 24) | (red << 16) | (green << 8) | blue;
                }
                yield new TileBlock(sourceTile.width, sourceTile.height, pixels);
            }
            case PALETTE_REMAP -> {
                LinkedHashMap<Integer, Integer> sourceIds = new LinkedHashMap<>();
                int nextId = 0;
                int[] pixels = new int[sourceTile.pixels.length];
                for (int pixelIndex = 0; pixelIndex < sourceTile.pixels.length; pixelIndex++) {
                    int source = sourceTile.pixels[pixelIndex];
                    Integer paletteIndex = sourceIds.get(source);
                    if (paletteIndex == null) {
                        paletteIndex = nextId++;
                        sourceIds.put(source, paletteIndex);
                    }
                    if (paletteIndex < 0 || paletteIndex >= transform.remapColors.length) {
                        throw new IOException("Palette remap source index is invalid: " + paletteIndex);
                    }
                    pixels[pixelIndex] = transform.remapColors[paletteIndex];
                }
                yield new TileBlock(sourceTile.width, sourceTile.height, pixels);
            }
        };
    }

    private static void writeTimePattern(TimePatternKey pattern, ByteArrayOutputStream out) {
        AfmaVarInts.writeUnsigned(out, pattern.eventCount);
        int previousEnd = 0;
        for (int eventIndex = 0; eventIndex < pattern.eventCount; eventIndex++) {
            AfmaVarInts.writeUnsigned(out, pattern.starts[eventIndex] - previousEnd);
            AfmaVarInts.writeUnsigned(out, pattern.lengths[eventIndex]);
            previousEnd = pattern.starts[eventIndex] + pattern.lengths[eventIndex];
        }
    }

    private static TimePatternKey readTimePattern(ByteArrayInputStream in) throws IOException {
        int eventCount = AfmaVarInts.readUnsigned(in);
        int[] starts = new int[eventCount];
        int[] lengths = new int[eventCount];
        int previousEnd = 0;
        for (int eventIndex = 0; eventIndex < eventCount; eventIndex++) {
            starts[eventIndex] = previousEnd + AfmaVarInts.readUnsigned(in);
            lengths[eventIndex] = AfmaVarInts.readUnsigned(in);
            previousEnd = starts[eventIndex] + lengths[eventIndex];
        }
        return new TimePatternKey(eventCount, starts, lengths);
    }

    private static TimePatternKey readTimePatternReference(ByteArrayInputStream dispatchIn, ByteArrayInputStream refIn,
                                                           ByteArrayInputStream inlineIn, TimePatternKey[] dictionary,
                                                           TimePatternKey leftPattern) throws IOException {
        int mode = AfmaVarInts.readUnsigned(dispatchIn);
        if (mode == 2) {
            if (leftPattern == null) {
                throw new IOException("Left time-pattern predictor is unavailable");
            }
            return leftPattern;
        }
        if (mode == 1) {
            int patternId = AfmaVarInts.readUnsigned(refIn);
            if (patternId < 0 || patternId >= dictionary.length) {
                throw new IOException("Time-pattern dictionary reference is invalid: " + patternId);
            }
            return dictionary[patternId];
        }
        if (mode == 0) {
            return readTimePattern(inlineIn);
        }
        throw new IOException("Unsupported time-pattern dispatch mode: " + mode);
    }

    private static void writePackedSection(OutputStream out, SectionBuffer rawBytes) throws IOException {
        try {
            AfmaSectionPacker.Analysis analysis = AfmaSectionPacker.analyze(rawBytes.buffer(), rawBytes.length());
            byte[] packedBytes = analysis.packedBytes();
            AfmaVarInts.writeUnsigned(out, packedBytes.length);
            out.write(packedBytes);
        } finally {
            rawBytes.release();
        }
    }

    private static ByteArrayInputStream readPackedSection(ByteArrayInputStream in) throws IOException {
        int length = AfmaVarInts.readUnsigned(in);
        byte[] packed = in.readNBytes(length);
        if (packed.length != length) {
            throw new IOException("Packed section ended early");
        }
        return new ByteArrayInputStream(AfmaSectionPacker.unpack(new ByteArrayInputStream(packed)));
    }

    private static void writeTilePayload(ByteArrayOutputStream out, TileBlock tile) throws IOException {
        int[] pixels = tile.pixels;
        boolean allEqual = true;
        int firstPixel = pixels[0];
        for (int index = 1; index < pixels.length; index++) {
            if (pixels[index] != firstPixel) {
                allEqual = false;
                break;
            }
        }
        if (allEqual) {
            out.write(0);
            DataOutputStream dataOut = new DataOutputStream(out);
            dataOut.writeInt(firstPixel);
            dataOut.flush();
            return;
        }

        LinkedHashMap<Integer, Integer> palette = new LinkedHashMap<>();
        for (int pixel : pixels) {
            palette.putIfAbsent(pixel, palette.size());
            if (palette.size() > 256) {
                break;
            }
        }
        if (palette.size() <= 256) {
            out.write(1);
            AfmaVarInts.writeUnsigned(out, palette.size());
            DataOutputStream dataOut = new DataOutputStream(out);
            int[] entries = new int[palette.size()];
            for (Map.Entry<Integer, Integer> entry : palette.entrySet()) {
                entries[entry.getValue()] = entry.getKey();
            }
            for (int entry : entries) {
                dataOut.writeInt(entry);
            }
            dataOut.flush();
            int bitWidth = (palette.size() <= 2) ? 1 : (palette.size() <= 4 ? 2 : (palette.size() <= 16 ? 4 : 8));
            int accumulator = 0;
            int bits = 0;
            for (int pixel : pixels) {
                accumulator |= (palette.get(pixel) << bits);
                bits += bitWidth;
                while (bits >= 8) {
                    out.write(accumulator & 0xFF);
                    accumulator >>>= 8;
                    bits -= 8;
                }
            }
            if (bits > 0) {
                out.write(accumulator & 0xFF);
            }
            return;
        }

        out.write(2);
        DataOutputStream dataOut = new DataOutputStream(out);
        for (int pixel : pixels) {
            dataOut.writeInt(pixel);
        }
        dataOut.flush();
    }

    private static TileBlock readTilePayload(ByteArrayInputStream in, int width, int height) throws IOException {
        int mode = in.read();
        if (mode < 0) {
            throw new IOException("Tile payload ended early");
        }
        return switch (mode) {
            case 0 -> {
                DataInputStream dataIn = new DataInputStream(in);
                int[] pixels = new int[width * height];
                Arrays.fill(pixels, dataIn.readInt());
                yield new TileBlock(width, height, pixels);
            }
            case 1 -> {
                int paletteSize = AfmaVarInts.readUnsigned(in);
                int[] palette = new int[paletteSize];
                DataInputStream dataIn = new DataInputStream(in);
                for (int paletteIndex = 0; paletteIndex < paletteSize; paletteIndex++) {
                    palette[paletteIndex] = dataIn.readInt();
                }
                int bitWidth = (paletteSize <= 2) ? 1 : (paletteSize <= 4 ? 2 : (paletteSize <= 16 ? 4 : 8));
                int[] pixels = new int[width * height];
                int accumulator = 0;
                int bits = 0;
                for (int pixelIndex = 0; pixelIndex < pixels.length; pixelIndex++) {
                    while (bits < bitWidth) {
                        int next = in.read();
                        if (next < 0) {
                            throw new IOException("Indexed tile payload ended early");
                        }
                        accumulator |= (next << bits);
                        bits += 8;
                    }
                    int mask = (1 << bitWidth) - 1;
                    int paletteIndex = accumulator & mask;
                    accumulator >>>= bitWidth;
                    bits -= bitWidth;
                    if (paletteIndex < 0 || paletteIndex >= palette.length) {
                        throw new IOException("Indexed tile palette index is invalid: " + paletteIndex);
                    }
                    pixels[pixelIndex] = palette[paletteIndex];
                }
                yield new TileBlock(width, height, pixels);
            }
            case 2 -> {
                int[] pixels = new int[width * height];
                DataInputStream dataIn = new DataInputStream(in);
                for (int pixelIndex = 0; pixelIndex < pixels.length; pixelIndex++) {
                    pixels[pixelIndex] = dataIn.readInt();
                }
                yield new TileBlock(width, height, pixels);
            }
            default -> throw new IOException("Unsupported tile payload mode: " + mode);
        };
    }

    private static List<AfmaDecodedFrame> allFrames(AfmaDecodedAnimation animation) {
        ArrayList<AfmaDecodedFrame> frames = new ArrayList<>(animation.introFrames().size() + animation.mainFrames().size());
        frames.addAll(animation.introFrames());
        frames.addAll(animation.mainFrames());
        return frames;
    }

    private static List<AfmaDecodedFrame> decodeFrames(int[][] framePixels, int width, int height, int start, int end) {
        ArrayList<AfmaDecodedFrame> frames = new ArrayList<>(Math.max(0, end - start));
        for (int index = start; index < end; index++) {
            frames.add(new AfmaDecodedFrame(width, height, framePixels[index]));
        }
        return frames;
    }

    private static TileBlock extractTile(int[] framePixels, int frameWidth, int tileX, int tileY, int tileWidth, int tileHeight) {
        int[] tilePixels = new int[tileWidth * tileHeight];
        int writeOffset = 0;
        for (int y = 0; y < tileHeight; y++) {
            int sourceOffset = (tileY + y) * frameWidth + tileX;
            System.arraycopy(framePixels, sourceOffset, tilePixels, writeOffset, tileWidth);
            writeOffset += tileWidth;
        }
        return new TileBlock(tileWidth, tileHeight, tilePixels);
    }

    private static void blitTile(TileBlock tile, int[] framePixels, int frameWidth, int tileX, int tileY) {
        int readOffset = 0;
        for (int y = 0; y < tile.height; y++) {
            int targetOffset = (tileY + y) * frameWidth + tileX;
            System.arraycopy(tile.pixels, readOffset, framePixels, targetOffset, tile.width);
            readOffset += tile.width;
        }
    }

    private static TileBlock internTile(Map<TileBlock, TileBlock> pool, TileBlock tile) {
        TileBlock existing = pool.get(tile);
        if (existing != null) {
            return existing;
        }
        pool.put(tile, tile);
        return tile;
    }

    private static int ceilDiv(int numerator, int denominator) {
        return (numerator + denominator - 1) / denominator;
    }

    public record WindowStats(
            int tileSize,
            int columns,
            int rows,
            int totalFrames,
            int positionCount,
            long totalOccurrences,
            long baseCoveredOccurrences,
            int atlasCount,
            long[] sameCoveredOccurrencesAfterStep,
            long[] windowCoveredOccurrencesAfterStep,
            long[] windowOnlyCoveredOccurrencesAfterStep,
            int[] uniqueTilesPerAtlas,
            int[] leftEqualCountsPerAtlas
    ) {

        public double baseCoverageRatio() {
            return ratio(this.baseCoveredOccurrences, this.totalOccurrences);
        }

        public double sameCoverageRatioAfter(int index) {
            if (index < 0 || index >= this.sameCoveredOccurrencesAfterStep.length) {
                return 0.0D;
            }
            return ratio(this.sameCoveredOccurrencesAfterStep[index], this.totalOccurrences);
        }

        public double windowCoverageRatioAfter(int index) {
            if (index < 0 || index >= this.windowCoveredOccurrencesAfterStep.length) {
                return 0.0D;
            }
            return ratio(this.windowCoveredOccurrencesAfterStep[index], this.totalOccurrences);
        }

        public double windowOnlyCoverageRatioAfter(int index) {
            if (index < 0 || index >= this.windowOnlyCoveredOccurrencesAfterStep.length) {
                return 0.0D;
            }
            return ratio(this.windowOnlyCoveredOccurrencesAfterStep[index], this.totalOccurrences);
        }

        public double leftEqualRatioForAtlas(int index) {
            int comparable = this.rows * Math.max(0, this.columns - 1);
            if (index < 0 || index >= this.leftEqualCountsPerAtlas.length || comparable <= 0) {
                return 0.0D;
            }
            return (double) this.leftEqualCountsPerAtlas[index] / (double) comparable;
        }

        private static double ratio(long numerator, long denominator) {
            if (numerator <= 0L || denominator <= 0L) {
                return 0.0D;
            }
            return (double) numerator / (double) denominator;
        }
    }

    public record TailStats(
            int tileSize,
            int positionCount,
            long totalOccurrences,
            long residualOccurrences,
            long exactWindowCoveredOccurrences,
            long patchCoveredOccurrences,
            long basePatchOccurrences,
            long samePositionPatchOccurrences,
            long windowPatchOccurrences,
            long[] patchableAtMostOccurrences,
            long changedPixelsTotal
    ) {

        public double residualRatio() {
            return ratio(this.residualOccurrences, this.totalOccurrences);
        }

        public double exactWindowCoverageRatio() {
            return ratio(this.exactWindowCoveredOccurrences, this.totalOccurrences);
        }

        public double literalTailRatio() {
            return ratio(this.residualOccurrences - this.exactWindowCoveredOccurrences, this.totalOccurrences);
        }

        public double patchCoverageRatio() {
            return ratio(this.patchCoveredOccurrences, this.totalOccurrences);
        }

        public double patchCoverageWithinTailRatio() {
            long tail = this.residualOccurrences - this.exactWindowCoveredOccurrences;
            return ratio(this.patchCoveredOccurrences, tail);
        }

        public double patchableAtMostRatio(int maxChangedPixels) {
            if (maxChangedPixels <= 0 || maxChangedPixels > this.patchableAtMostOccurrences.length) {
                return 0.0D;
            }
            return ratio(this.patchableAtMostOccurrences[maxChangedPixels - 1], this.totalOccurrences);
        }

        public double patchableAtMostWithinTailRatio(int maxChangedPixels) {
            if (maxChangedPixels <= 0 || maxChangedPixels > this.patchableAtMostOccurrences.length) {
                return 0.0D;
            }
            long tail = this.residualOccurrences - this.exactWindowCoveredOccurrences;
            return ratio(this.patchableAtMostOccurrences[maxChangedPixels - 1], tail);
        }

        public double averageChangedPixelsPerPatch() {
            if (this.patchCoveredOccurrences <= 0L) {
                return 0.0D;
            }
            return (double) this.changedPixelsTotal / (double) this.patchCoveredOccurrences;
        }

        private static double ratio(long numerator, long denominator) {
            if (numerator <= 0L || denominator <= 0L) {
                return 0.0D;
            }
            return (double) numerator / (double) denominator;
        }
    }

    public record TransformStats(
            int tileSize,
            int positionCount,
            long totalOccurrences,
            long residualOccurrences,
            long exactWindowCoveredOccurrences,
            long patchCoveredOccurrences,
            long transformCoveredOccurrences,
            long argbDeltaOccurrences,
            long paletteRemapOccurrences,
            long baseTransformOccurrences,
            long samePositionTransformOccurrences,
            long windowTransformOccurrences,
            long remapColorsTotal
    ) {

        public double literalTailAfterPatchRatio() {
            return ratio(this.residualOccurrences - this.exactWindowCoveredOccurrences - this.patchCoveredOccurrences, this.totalOccurrences);
        }

        public double transformCoverageRatio() {
            return ratio(this.transformCoveredOccurrences, this.totalOccurrences);
        }

        public double transformCoverageWithinPostPatchTailRatio() {
            long tail = this.residualOccurrences - this.exactWindowCoveredOccurrences - this.patchCoveredOccurrences;
            return ratio(this.transformCoveredOccurrences, tail);
        }

        public double argbDeltaRatioWithinTransforms() {
            return ratio(this.argbDeltaOccurrences, this.transformCoveredOccurrences);
        }

        public double paletteRemapRatioWithinTransforms() {
            return ratio(this.paletteRemapOccurrences, this.transformCoveredOccurrences);
        }

        public double averageRemapColors() {
            if (this.paletteRemapOccurrences <= 0L) {
                return 0.0D;
            }
            return (double) this.remapColorsTotal / (double) this.paletteRemapOccurrences;
        }

        private static double ratio(long numerator, long denominator) {
            if (numerator <= 0L || denominator <= 0L) {
                return 0.0D;
            }
            return (double) numerator / (double) denominator;
        }
    }

    public record TransformPatternStats(
            int tileSize,
            long transformOccurrences,
            int uniqueArgbDeltas,
            long repeatedArgbDeltaOccurrences,
            int uniquePaletteRemaps,
            long repeatedPaletteRemapOccurrences,
            int maxArgbDeltaFrequency,
            int maxPaletteRemapFrequency
    ) {

        public double repeatedArgbDeltaRatio() {
            return ratio(this.repeatedArgbDeltaOccurrences, this.transformOccurrences);
        }

        public double repeatedPaletteRemapRatio() {
            return ratio(this.repeatedPaletteRemapOccurrences, this.transformOccurrences);
        }

        private static double ratio(long numerator, long denominator) {
            if (numerator <= 0L || denominator <= 0L) {
                return 0.0D;
            }
            return (double) numerator / (double) denominator;
        }
    }

    public record PatchPatternStats(
            int tileSize,
            long patchOccurrences,
            int uniqueExactPatches,
            long repeatedExactPatchOccurrences,
            int uniquePatchShapes,
            long repeatedPatchShapeOccurrences,
            int maxExactPatchFrequency,
            int maxPatchShapeFrequency,
            long changedPixelsTotal
    ) {

        public double repeatedExactPatchRatio() {
            return ratio(this.repeatedExactPatchOccurrences, this.patchOccurrences);
        }

        public double repeatedPatchShapeRatio() {
            return ratio(this.repeatedPatchShapeOccurrences, this.patchOccurrences);
        }

        public double averageChangedPixelsPerPatch() {
            if (this.patchOccurrences <= 0L) {
                return 0.0D;
            }
            return (double) this.changedPixelsTotal / (double) this.patchOccurrences;
        }

        private static double ratio(long numerator, long denominator) {
            if (numerator <= 0L || denominator <= 0L) {
                return 0.0D;
            }
            return (double) numerator / (double) denominator;
        }
    }

    public record RepairProgramStats(
            int tileSize,
            long patchOccurrences,
            long singleSamplerOccurrences,
            long sameSourceAtlasSingleSamplerOccurrences,
            long coveredByAtMostTwoSamplersOccurrences,
            long patchArgbDeltaOccurrences,
            long patchPaletteRemapOccurrences,
            int uniqueSingleSamplerPrograms,
            long repeatedSingleSamplerProgramOccurrences,
            int maxSingleSamplerProgramFrequency,
            long singleSamplerAbsDxTotal
    ) {

        public double singleSamplerRatio() {
            return ratio(this.singleSamplerOccurrences, this.patchOccurrences);
        }

        public double sameSourceAtlasSingleSamplerRatio() {
            return ratio(this.sameSourceAtlasSingleSamplerOccurrences, this.patchOccurrences);
        }

        public double coveredByAtMostTwoSamplersRatio() {
            return ratio(this.coveredByAtMostTwoSamplersOccurrences, this.patchOccurrences);
        }

        public double patchArgbDeltaRatio() {
            return ratio(this.patchArgbDeltaOccurrences, this.patchOccurrences);
        }

        public double patchPaletteRemapRatio() {
            return ratio(this.patchPaletteRemapOccurrences, this.patchOccurrences);
        }

        public double repeatedSingleSamplerProgramRatio() {
            return ratio(this.repeatedSingleSamplerProgramOccurrences, this.singleSamplerOccurrences);
        }

        public double averageSingleSamplerAbsDx() {
            return ratio(this.singleSamplerAbsDxTotal, this.singleSamplerOccurrences);
        }

        private static double ratio(long numerator, long denominator) {
            if (numerator <= 0L || denominator <= 0L) {
                return 0.0D;
            }
            return (double) numerator / (double) denominator;
        }
    }

    public record PlacementStats(
            int tileSize,
            int positionCount,
            long totalOccurrences,
            int eligiblePositions,
            int coveredPositions,
            long eligibleOccurrences,
            long coveredOccurrences,
            int selectedTemplateCount,
            int selectedRectangleCount,
            double averageRectangleArea
    ) {

        public double eligiblePositionRatio() {
            return ratio(this.eligiblePositions, this.positionCount);
        }

        public double coveredPositionRatio() {
            return ratio(this.coveredPositions, this.positionCount);
        }

        public double eligibleOccurrenceRatio() {
            return ratio(this.eligibleOccurrences, this.totalOccurrences);
        }

        public double coveredOccurrenceRatio() {
            return ratio(this.coveredOccurrences, this.totalOccurrences);
        }

        private static double ratio(long numerator, long denominator) {
            if (numerator <= 0L || denominator <= 0L) {
                return 0.0D;
            }
            return (double) numerator / (double) denominator;
        }
    }

    private record PreparedData(
            int width,
            int height,
            int totalFrames,
            int columns,
            int rows,
            int positionCount,
            long totalOccurrences,
            long baseCoveredOccurrences,
            TileBlock[][] tilesByPositionFrame,
            TileBlock[] baseTiles
    ) {
    }

    private record PositionProgram(TileBlock baseTile, List<StateEvent> events) {
    }

    private record StateEvent(int startFrame, int length, EventState state) {
    }

    private record AtlasMatch(int atlasIndex, int sourcePosition, boolean samePosition) {
    }

    private record HorizontalSamplerMatch(int atlasIndex, int deltaX) {
    }

    private record AtlasImage(int width, int[] pixels) {

        private int height() {
            return this.pixels.length / this.width;
        }
    }

    private record PlacementSelection(
            List<PlacementTemplateSelection> templates,
            boolean[] coveredPositions,
            int eligiblePositions,
            int coveredPositionCount,
            long eligibleOccurrences,
            long coveredOccurrences
    ) {

        private int rectangleCount() {
            int total = 0;
            for (PlacementTemplateSelection template : this.templates) {
                total += template.rectangles.size();
            }
            return total;
        }

        private double averageRectangleArea() {
            int rectangleCount = rectangleCount();
            if (rectangleCount <= 0) {
                return 0.0D;
            }
            long totalArea = 0L;
            for (PlacementTemplateSelection template : this.templates) {
                for (PlacementRectangle rectangle : template.rectangles) {
                    totalArea += rectangle.area();
                }
            }
            return (double) totalArea / (double) rectangleCount;
        }
    }

    private record PlacementTemplateSelection(PlacementTemplateKey template, List<PlacementRectangle> rectangles) {
    }

    private record PlacementTemplateKey(TimePatternKey pattern, PlacementStep[] steps) {

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof PlacementTemplateKey key)) {
                return false;
            }
            return this.pattern.equals(key.pattern) && Arrays.equals(this.steps, key.steps);
        }

        @Override
        public int hashCode() {
            return 31 * this.pattern.hashCode() + Arrays.hashCode(this.steps);
        }
    }

    private record PlacementStep(int atlasIndex, int deltaX, int deltaY) {
    }

    private record PlacementRectangle(int x, int y, int width, int height) {

        private int area() {
            return this.width * this.height;
        }
    }

    private record PatchMatch(PatchSource source, SparsePatch patch, int estimatedCost) {

        private EventState toEventState() {
            return switch (this.source.type) {
                case BASE -> EventState.basePatch(this.patch);
                case SAME_POSITION_ATLAS -> EventState.samePositionPatch(this.source.atlasIndex, this.patch);
                case WINDOW_ATLAS -> EventState.windowPatch(this.source.atlasIndex, this.source.sourcePosition, this.patch);
            };
        }
    }

    private record PatchTransformMatch(PatchSource source, SparsePatch patch, TileTransform transform, int estimatedCost) {

        private EventState toEventState() {
            return switch (this.source.type) {
                case BASE -> EventState.basePatchTransform(this.patch, this.transform);
                case SAME_POSITION_ATLAS -> EventState.samePositionPatchTransform(this.source.atlasIndex, this.patch, this.transform);
                case WINDOW_ATLAS -> EventState.windowPatchTransform(this.source.atlasIndex, this.source.sourcePosition, this.patch, this.transform);
            };
        }
    }

    private record TransformMatch(PatchSource source, TileTransform transform, int estimatedCost) {

        private EventState toEventState() {
            return EventState.transform(this.source.type, this.source.atlasIndex, this.source.sourcePosition, this.transform);
        }
    }

    private record LocalTileScore(TileBlock tile, double score) {
    }

    private record RowCandidate(TileBlock tile, double score) {
    }

    private record PatchSource(PatchSourceType type, TileBlock tile, int atlasIndex, int sourcePosition, int referenceCostEstimate) {

        private static PatchSource base(TileBlock tile) {
            return new PatchSource(PatchSourceType.BASE, tile, -1, -1, 0);
        }

        private static PatchSource samePositionAtlas(int atlasIndex, TileBlock tile) {
            return new PatchSource(PatchSourceType.SAME_POSITION_ATLAS, tile, atlasIndex, -1, estimateVarIntSize(atlasIndex));
        }

        private static PatchSource windowAtlas(int atlasIndex, int sourcePosition, TileBlock tile) {
            return new PatchSource(
                    PatchSourceType.WINDOW_ATLAS,
                    tile,
                    atlasIndex,
                    sourcePosition,
                    estimateVarIntSize(atlasIndex) + estimateVarIntSize(sourcePosition)
            );
        }
    }

    private record RepairSamplerCoverage(RepairSamplerMatch singleMatch, boolean coveredByAtMostTwoSamplers) {
    }

    private record RepairSamplerMatch(int atlasIndex, int deltaX, long matchedMask) {
    }

    private static final class RepairSamplerProgramKey {
        private final int width;
        private final int height;
        private final int[] indices;
        private final int deltaX;
        private final boolean sameSourceAtlas;
        private final int hashCode;

        private RepairSamplerProgramKey(int width, int height, int[] indices, int deltaX, boolean sameSourceAtlas) {
            this.width = width;
            this.height = height;
            this.indices = indices;
            this.deltaX = deltaX;
            this.sameSourceAtlas = sameSourceAtlas;
            int hash = Integer.hashCode(width);
            hash = 31 * hash + Integer.hashCode(height);
            hash = 31 * hash + Arrays.hashCode(indices);
            hash = 31 * hash + Integer.hashCode(deltaX);
            hash = 31 * hash + Boolean.hashCode(sameSourceAtlas);
            this.hashCode = hash;
        }

        private static RepairSamplerProgramKey from(SparsePatch patch, int width, int height, int deltaX, boolean sameSourceAtlas) {
            return new RepairSamplerProgramKey(width, height, Arrays.copyOf(patch.indices, patch.indices.length), deltaX, sameSourceAtlas);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof RepairSamplerProgramKey key)) {
                return false;
            }
            return this.width == key.width
                    && this.height == key.height
                    && this.deltaX == key.deltaX
                    && this.sameSourceAtlas == key.sameSourceAtlas
                    && Arrays.equals(this.indices, key.indices);
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }
    }

    private static final class SparsePatch {
        private final int[] indices;
        private final int[] colors;
        private final int hashCode;

        private SparsePatch(int[] indices, int[] colors) {
            this.indices = indices;
            this.colors = colors;
            int hash = Arrays.hashCode(indices);
            hash = 31 * hash + Arrays.hashCode(colors);
            this.hashCode = hash;
        }

        private int changedPixelCount() {
            return this.indices.length;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof SparsePatch patch)) {
                return false;
            }
            return Arrays.equals(this.indices, patch.indices) && Arrays.equals(this.colors, patch.colors);
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }
    }

    private static final class SparsePatchKey {
        private final int[] indices;
        private final int[] colors;
        private final int hashCode;

        private SparsePatchKey(int[] indices, int[] colors) {
            this.indices = indices;
            this.colors = colors;
            int hash = Arrays.hashCode(indices);
            hash = 31 * hash + Arrays.hashCode(colors);
            this.hashCode = hash;
        }

        private static SparsePatchKey from(SparsePatch patch) {
            return new SparsePatchKey(Arrays.copyOf(patch.indices, patch.indices.length), Arrays.copyOf(patch.colors, patch.colors.length));
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof SparsePatchKey key)) {
                return false;
            }
            return Arrays.equals(this.indices, key.indices) && Arrays.equals(this.colors, key.colors);
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }
    }

    private static final class PatchShapeKey {
        private final int width;
        private final int height;
        private final int[] indices;
        private final int hashCode;

        private PatchShapeKey(int width, int height, int[] indices) {
            this.width = width;
            this.height = height;
            this.indices = indices;
            int hash = Integer.hashCode(width);
            hash = 31 * hash + Integer.hashCode(height);
            hash = 31 * hash + Arrays.hashCode(indices);
            this.hashCode = hash;
        }

        private static PatchShapeKey from(SparsePatch patch, int width, int height) {
            return new PatchShapeKey(width, height, Arrays.copyOf(patch.indices, patch.indices.length));
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof PatchShapeKey key)) {
                return false;
            }
            return this.width == key.width
                    && this.height == key.height
                    && Arrays.equals(this.indices, key.indices);
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }
    }

    private static final class PatchShape {
        private final int width;
        private final int height;
        private final int[] indices;

        private PatchShape(int width, int height, int[] indices) {
            this.width = width;
            this.height = height;
            this.indices = indices;
        }
    }

    private static final class RemapKey {
        private final int[] colors;
        private final int hashCode;

        private RemapKey(int[] colors) {
            this.colors = colors;
            this.hashCode = Arrays.hashCode(colors);
        }

        private static RemapKey from(int[] colors) {
            return new RemapKey(Arrays.copyOf(colors, colors.length));
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof RemapKey key)) {
                return false;
            }
            return Arrays.equals(this.colors, key.colors);
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }
    }

    private static final class MutableLocalScore {
        private int occurrences;
        private int runs;
    }

    private static final class TileTransform {
        private final TransformMode mode;
        private final int deltaA;
        private final int deltaR;
        private final int deltaG;
        private final int deltaB;
        private final int[] remapColors;
        private final int hashCode;

        private TileTransform(TransformMode mode, int deltaA, int deltaR, int deltaG, int deltaB, int[] remapColors) {
            this.mode = mode;
            this.deltaA = deltaA;
            this.deltaR = deltaR;
            this.deltaG = deltaG;
            this.deltaB = deltaB;
            this.remapColors = (remapColors != null) ? Arrays.copyOf(remapColors, remapColors.length) : null;
            int hash = mode.hashCode();
            hash = 31 * hash + deltaA;
            hash = 31 * hash + deltaR;
            hash = 31 * hash + deltaG;
            hash = 31 * hash + deltaB;
            hash = 31 * hash + Arrays.hashCode(this.remapColors);
            this.hashCode = hash;
        }

        private static TileTransform argbDelta(int deltaA, int deltaR, int deltaG, int deltaB) {
            return new TileTransform(TransformMode.ARGB_DELTA, deltaA, deltaR, deltaG, deltaB, null);
        }

        private static TileTransform paletteRemap(int[] remapColors) {
            return new TileTransform(TransformMode.PALETTE_REMAP, 0, 0, 0, 0, remapColors);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof TileTransform transform)) {
                return false;
            }
            return this.mode == transform.mode
                    && this.deltaA == transform.deltaA
                    && this.deltaR == transform.deltaR
                    && this.deltaG == transform.deltaG
                    && this.deltaB == transform.deltaB
                    && Arrays.equals(this.remapColors, transform.remapColors);
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }
    }

    private static final class EventState {
        private final StateMode mode;
        private final PatchSourceType transformSourceType;
        private final int atlasIndex;
        private final int sourcePosition;
        private final TileBlock literalTile;
        private final SparsePatch patch;
        private final TileTransform transform;
        private final int hashCode;

        private EventState(StateMode mode, PatchSourceType transformSourceType, int atlasIndex, int sourcePosition,
                           TileBlock literalTile, SparsePatch patch, TileTransform transform) {
            this.mode = mode;
            this.transformSourceType = transformSourceType;
            this.atlasIndex = atlasIndex;
            this.sourcePosition = sourcePosition;
            this.literalTile = literalTile;
            this.patch = patch;
            this.transform = transform;
            int hash = mode.hashCode();
            hash = 31 * hash + ((transformSourceType != null) ? transformSourceType.hashCode() : 0);
            hash = 31 * hash + atlasIndex;
            hash = 31 * hash + sourcePosition;
            hash = 31 * hash + ((literalTile != null) ? literalTile.hashCode() : 0);
            hash = 31 * hash + ((patch != null) ? patch.hashCode() : 0);
            hash = 31 * hash + ((transform != null) ? transform.hashCode() : 0);
            this.hashCode = hash;
        }

        private static EventState samePositionAtlas(int atlasIndex) {
            return new EventState(StateMode.SAME_POSITION_ATLAS, null, atlasIndex, -1, null, null, null);
        }

        private static EventState windowAtlas(int atlasIndex, int sourcePosition) {
            return new EventState(StateMode.WINDOW_ATLAS, null, atlasIndex, sourcePosition, null, null, null);
        }

        private static EventState horizontalSampler(int atlasIndex, int deltaX) {
            return new EventState(StateMode.HORIZONTAL_SAMPLER, null, atlasIndex, deltaX, null, null, null);
        }

        private static EventState literal(TileBlock literalTile) {
            return new EventState(StateMode.LITERAL, null, -1, -1, literalTile, null, null);
        }

        private static EventState basePatch(SparsePatch patch) {
            return new EventState(StateMode.BASE_PATCH, null, -1, -1, null, patch, null);
        }

        private static EventState samePositionPatch(int atlasIndex, SparsePatch patch) {
            return new EventState(StateMode.SAME_POSITION_PATCH, null, atlasIndex, -1, null, patch, null);
        }

        private static EventState windowPatch(int atlasIndex, int sourcePosition, SparsePatch patch) {
            return new EventState(StateMode.WINDOW_PATCH, null, atlasIndex, sourcePosition, null, patch, null);
        }

        private static EventState basePatchTransform(SparsePatch patch, TileTransform transform) {
            return new EventState(StateMode.BASE_PATCH_TRANSFORM, null, -1, -1, null, patch, transform);
        }

        private static EventState samePositionPatchTransform(int atlasIndex, SparsePatch patch, TileTransform transform) {
            return new EventState(StateMode.SAME_POSITION_PATCH_TRANSFORM, null, atlasIndex, -1, null, patch, transform);
        }

        private static EventState windowPatchTransform(int atlasIndex, int sourcePosition, SparsePatch patch, TileTransform transform) {
            return new EventState(StateMode.WINDOW_PATCH_TRANSFORM, null, atlasIndex, sourcePosition, null, patch, transform);
        }

        private static EventState transform(PatchSourceType sourceType, int atlasIndex, int sourcePosition, TileTransform transform) {
            return new EventState(StateMode.TRANSFORM, sourceType, atlasIndex, sourcePosition, null, null, transform);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof EventState state)) {
                return false;
            }
            return this.mode == state.mode
                    && this.transformSourceType == state.transformSourceType
                    && this.atlasIndex == state.atlasIndex
                    && this.sourcePosition == state.sourcePosition
                    && java.util.Objects.equals(this.literalTile, state.literalTile)
                    && java.util.Objects.equals(this.patch, state.patch)
                    && java.util.Objects.equals(this.transform, state.transform);
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }
    }

    private enum StateMode {
        LITERAL,
        SAME_POSITION_ATLAS,
        WINDOW_ATLAS,
        HORIZONTAL_SAMPLER,
        BASE_PATCH,
        SAME_POSITION_PATCH,
        WINDOW_PATCH,
        BASE_PATCH_TRANSFORM,
        SAME_POSITION_PATCH_TRANSFORM,
        WINDOW_PATCH_TRANSFORM,
        TRANSFORM
    }

    private enum TransformMode {
        ARGB_DELTA,
        PALETTE_REMAP
    }

    private enum PatchSourceType {
        BASE,
        SAME_POSITION_ATLAS,
        WINDOW_ATLAS
    }

    private static final class TimePatternKey {
        private final int eventCount;
        private final int[] starts;
        private final int[] lengths;
        private final int hashCode;

        private TimePatternKey(int eventCount, int[] starts, int[] lengths) {
            this.eventCount = eventCount;
            this.starts = Arrays.copyOf(starts, starts.length);
            this.lengths = Arrays.copyOf(lengths, lengths.length);
            int hash = eventCount;
            hash = 31 * hash + Arrays.hashCode(this.starts);
            hash = 31 * hash + Arrays.hashCode(this.lengths);
            this.hashCode = hash;
        }

        private static TimePatternKey from(PositionProgram program) {
            int[] starts = new int[program.events.size()];
            int[] lengths = new int[program.events.size()];
            for (int eventIndex = 0; eventIndex < program.events.size(); eventIndex++) {
                StateEvent event = program.events.get(eventIndex);
                starts[eventIndex] = event.startFrame;
                lengths[eventIndex] = event.length;
            }
            return new TimePatternKey(program.events.size(), starts, lengths);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof TimePatternKey pattern)) {
                return false;
            }
            return this.eventCount == pattern.eventCount
                    && Arrays.equals(this.starts, pattern.starts)
                    && Arrays.equals(this.lengths, pattern.lengths);
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }
    }

    private static final class TileBlock {
        private final int width;
        private final int height;
        private final int[] pixels;
        private final int hashCode;

        private TileBlock(int width, int height, int[] pixels) {
            this.width = width;
            this.height = height;
            this.pixels = pixels;
            int hash = 31 * width + height;
            hash = 31 * hash + Arrays.hashCode(this.pixels);
            this.hashCode = hash;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof TileBlock tile)) {
                return false;
            }
            return this.width == tile.width
                    && this.height == tile.height
                    && Arrays.equals(this.pixels, tile.pixels);
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }
    }

    private static final class SectionBuffer extends ByteArrayOutputStream {
        private byte[] buffer() {
            return this.buf;
        }

        private int length() {
            return this.count;
        }

        private void release() {
            this.reset();
            this.buf = new byte[32];
        }
    }
}
