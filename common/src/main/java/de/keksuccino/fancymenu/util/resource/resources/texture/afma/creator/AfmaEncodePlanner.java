package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaBinIntraPayloadHelper;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaBlockInter;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaBlockInterPayloadHelper;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaChunkedPayloadHelper;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaCopyRect;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameDescriptor;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameIndex;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameOperationType;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaMetadata;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaMultiCopy;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaPatchRegion;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaAlphaResidualMode;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaPayloadMetricsHelper;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaRect;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaResidualCodec;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaResidualPayload;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaResidualPayloadHelper;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaSparseLayoutCodec;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaSparsePayload;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaSparsePayloadHelper;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaStoredPayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

public class AfmaEncodePlanner {

    protected static final int MIN_SPARSE_DELTA_CHANGED_PIXELS = 1;
    protected static final double MAX_SPARSE_DELTA_CHANGED_DENSITY = 0.75D;
    protected static final int BLOCK_INTER_TILE_SIZE = 16;
    protected static final int MIN_PARALLEL_BLOCK_INTER_TILES = 32;
    protected static final int BLOCK_INTER_MIN_NON_ZERO_MOTION_VECTORS = 8;
    protected static final int BLOCK_INTER_MAX_NON_ZERO_MOTION_VECTORS = 64;
    protected static final int BLOCK_INTER_NON_ZERO_MOTION_VECTORS_PER_CHANGED_TILE = 4;
    protected static final int BLOCK_INTER_LOCAL_REFINEMENT_RADIUS = 2;
    protected static final int BLOCK_INTER_LOCAL_REFINEMENT_SEEDS = 3;
    protected static final int BLOCK_INTER_LOCAL_REFINEMENT_PASSES = 2;
    protected static final int BLOCK_INTER_PAYLOAD_HEADER_BYTES = 9;
    protected static final int PLANNER_TARGET_CHUNK_BYTES = 256 * 1024;
    protected static final int PLANNER_DEFLATE_TAIL_BYTES = 32 * 1024;
    protected static final int PLANNER_MAX_CACHED_PAYLOAD_CHUNKS = 2;
    protected static final int PLANNER_MAX_EXACT_ARCHIVE_REFINEMENT_CACHE_ENTRIES = 8192;
    protected static final int PLANNER_CHUNK_CACHE_MISS_PENALTY_BYTES = 1024;
    protected static final int PLANNER_MULTI_CHUNK_FRAME_PENALTY_BYTES = 768;
    protected static final int ESTIMATED_ZIP_CHUNK_OVERHEAD_BYTES = 96;
    protected static final int FULL_SPARSE_LAYOUT_EVALUATION_MAX_CHANGED_PIXELS = 24;
    protected static final int FULL_SPARSE_LAYOUT_EVALUATION_MAX_AREA = 512;
    protected static final int MIN_SHORTLISTED_SPARSE_LAYOUTS = 2;
    protected static final int SPARSE_LAYOUT_SHORTLIST_MARGIN_BYTES = 24;
    protected static final double SPARSE_LAYOUT_SHORTLIST_MARGIN_RATIO = 0.15D;
    protected static final long MEMORY_SAFETY_RESERVE_BYTES = 192L * 1024L * 1024L;
    protected static final byte[] EMPTY_BYTES = new byte[0];
    @NotNull
    protected static final Map<String, String> EMPTY_PAYLOAD_PATHS_BY_FINGERPRINT = Collections.emptyMap();

    @NotNull
    protected final AfmaFrameNormalizer frameNormalizer;

    public AfmaEncodePlanner() {
        this(new AfmaFrameNormalizer());
    }

    public AfmaEncodePlanner(@NotNull AfmaFrameNormalizer frameNormalizer) {
        this.frameNormalizer = Objects.requireNonNull(frameNormalizer);
    }

    @NotNull
    public AfmaEncodePlan plan(@NotNull AfmaSourceSequence mainSequence, @Nullable AfmaSourceSequence introSequence, @NotNull AfmaEncodeOptions options) throws IOException {
        return this.plan(mainSequence, introSequence, options, null);
    }

    @NotNull
    public AfmaEncodePlan plan(@NotNull AfmaSourceSequence mainSequence, @Nullable AfmaSourceSequence introSequence,
                               @NotNull AfmaEncodeOptions options, @Nullable BooleanSupplier cancellationRequested) throws IOException {
        return this.plan(mainSequence, introSequence, options, cancellationRequested, null);
    }

    @NotNull
    public AfmaEncodePlan plan(@NotNull AfmaSourceSequence mainSequence, @Nullable AfmaSourceSequence introSequence,
                               @NotNull AfmaEncodeOptions options, @Nullable BooleanSupplier cancellationRequested,
                               @Nullable ProgressListener progressListener) throws IOException {
        Objects.requireNonNull(mainSequence);
        Objects.requireNonNull(options);

        AfmaSourceSequence intro = (introSequence != null) ? introSequence : AfmaSourceSequence.empty();
        options.validateForCounts(mainSequence.size(), intro.size());

        checkCancelled(cancellationRequested);
        AfmaSourceSequence dimensionSource = !mainSequence.isEmpty() ? mainSequence : intro;
        LoadedDimensionFrame loadedDimension = this.loadDimensionFrame(dimensionSource, cancellationRequested, progressListener);
        Dimension dimension = loadedDimension.dimension();
        AfmaPixelFrame preloadedMainFrame = (dimensionSource == mainSequence) ? loadedDimension.frame() : null;
        AfmaPixelFrame preloadedIntroFrame = (dimensionSource == intro) ? loadedDimension.frame() : null;
        int totalFrameCount = Math.max(1, mainSequence.size() + intro.size());

        AfmaRectCopyDetector copyDetector = new AfmaRectCopyDetector(options.getMaxCopySearchDistance(), options.getMaxCandidateAxisOffsets());
        LinkedHashMap<String, AfmaStoredPayload> payloads = new LinkedHashMap<>();
        Map<String, String> payloadPathsByFingerprint = new LinkedHashMap<>();
        try {
            PlannedSequence plannedIntroFrames = this.planSequence(intro, true, dimension, options, copyDetector, ArchivePlanningState.empty(), payloads, payloadPathsByFingerprint,
                    cancellationRequested, progressListener, 0, totalFrameCount, preloadedIntroFrame);
            preloadedIntroFrame = null;
            PlannedSequence plannedMainFrames = this.planSequence(mainSequence, false, dimension, options, copyDetector, plannedIntroFrames.archiveState(), payloads, payloadPathsByFingerprint,
                    cancellationRequested, progressListener, intro.size(), totalFrameCount, preloadedMainFrame);
            preloadedMainFrame = null;

            long mainFrameTime = plannedMainFrames.defaultDelayMs();
            long introFrameTime = plannedIntroFrames.defaultDelayMs();
            if (plannedMainFrames.frames().isEmpty() && !plannedIntroFrames.frames().isEmpty()) {
                mainFrameTime = introFrameTime;
            } else if (plannedIntroFrames.frames().isEmpty()) {
                introFrameTime = mainFrameTime;
            }

            AfmaMetadata metadata = AfmaMetadata.create(
                    dimension.width(),
                    dimension.height(),
                    options.getLoopCount(),
                    mainFrameTime,
                    introFrameTime,
                    plannedMainFrames.customFrameTimes(),
                    plannedIntroFrames.customFrameTimes(),
                    options.isAdaptiveKeyframePlacementEnabled() ? options.getAdaptiveMaxKeyframeInterval() : options.getKeyframeInterval(),
                    options.isRectCopyEnabled(),
                    options.isDuplicateFrameElision()
            );

            return new AfmaEncodePlan(metadata, new AfmaFrameIndex(plannedMainFrames.frames(), plannedIntroFrames.frames()), payloads);
        } finally {
            CloseableUtils.closeQuietly(preloadedIntroFrame);
            CloseableUtils.closeQuietly(preloadedMainFrame);
        }
    }

    @NotNull
    protected PlannedSequence planSequence(@NotNull AfmaSourceSequence sequence, boolean introSequence, @NotNull Dimension dimension,
                                           @NotNull AfmaEncodeOptions options, @NotNull AfmaRectCopyDetector copyDetector,
                                           @NotNull ArchivePlanningState initialArchiveState,
                                           @NotNull LinkedHashMap<String, AfmaStoredPayload> payloads,
                                           @NotNull Map<String, String> payloadPathsByFingerprint,
                                           @Nullable BooleanSupplier cancellationRequested, @Nullable ProgressListener progressListener,
                                           int startOffset, int totalFrameCount, @Nullable AfmaPixelFrame firstFrameOverride) throws IOException {
        List<PlannedTimedFrame> plannedFrames = new ArrayList<>();
        if (sequence.isEmpty()) {
            return this.buildPlannedSequence(plannedFrames, this.resolveSequenceDefaultDelay(options, introSequence), initialArchiveState);
        }

        BeamPlanningState baseState = BeamPlanningState.root(initialArchiveState);
        AfmaPixelFrame preloadedFrame = firstFrameOverride;
        int plannerWindowFrames = this.resolveEffectivePlannerWindowFrames(options, dimension);
        try {
            for (int windowStart = 0; windowStart < sequence.size(); windowStart += plannerWindowFrames) {
                checkCancelled(cancellationRequested);
                int windowEnd = Math.min(sequence.size(), windowStart + plannerWindowFrames);
                BeamPlanningState bestWindowState = this.planWindow(sequence, windowStart, windowEnd, dimension, preloadedFrame, introSequence,
                        options, copyDetector, baseState, cancellationRequested, progressListener, startOffset, totalFrameCount, sequence.size());
                preloadedFrame = null;
                this.commitPlannedWindow(bestWindowState, plannedFrames, payloads, payloadPathsByFingerprint);
                baseState = bestWindowState.toCommittedBaseState();
                reportProgress(progressListener,
                        "Planning " + (introSequence ? "intro" : "main") + " frame " + windowEnd + "/" + sequence.size(),
                        0.08D + (0.92D * ((double) (startOffset + windowEnd) / Math.max(1, totalFrameCount))));
            }
        } finally {
            CloseableUtils.closeQuietly(preloadedFrame);
        }

        return this.buildPlannedSequence(plannedFrames, this.resolveSequenceDefaultDelay(options, introSequence), baseState.archiveState());
    }

    protected long resolveSourceFrameDelay(@NotNull AfmaEncodeOptions options, boolean introSequence, int frameIndex) {
        Map<Integer, Long> customFrameTimes = introSequence ? options.getCustomIntroFrameTimes() : options.getCustomFrameTimes();
        Long customDelay = customFrameTimes.get(frameIndex);
        if ((customDelay != null) && (customDelay > 0L)) {
            return customDelay;
        }
        return this.resolveSequenceDefaultDelay(options, introSequence);
    }

    protected long resolveSequenceDefaultDelay(@NotNull AfmaEncodeOptions options, boolean introSequence) {
        return introSequence ? options.getIntroFrameTimeMs() : options.getFrameTimeMs();
    }

    protected void extendPlannedFrameDelay(@NotNull List<PlannedTimedFrame> plannedFrames, long additionalDelayMs) {
        if (plannedFrames.isEmpty()) {
            throw new IllegalStateException("AFMA temporal frame collapsing requires at least one emitted frame");
        }

        int lastIndex = plannedFrames.size() - 1;
        plannedFrames.set(lastIndex, plannedFrames.get(lastIndex).withAdditionalDelay(additionalDelayMs));
    }

    @NotNull
    protected PlannedSequence buildPlannedSequence(@NotNull List<PlannedTimedFrame> plannedFrames, long fallbackDefaultDelayMs,
                                                   @NotNull ArchivePlanningState archiveState) {
        List<AfmaFrameDescriptor> descriptors = new ArrayList<>(plannedFrames.size());
        List<Long> frameDelays = new ArrayList<>(plannedFrames.size());
        for (PlannedTimedFrame plannedFrame : plannedFrames) {
            descriptors.add(plannedFrame.descriptor());
            frameDelays.add(plannedFrame.delayMs());
        }

        AdaptiveTiming adaptiveTiming = this.buildAdaptiveTiming(frameDelays, fallbackDefaultDelayMs);
        return new PlannedSequence(descriptors, adaptiveTiming.defaultDelayMs(), adaptiveTiming.customFrameTimes(), archiveState);
    }

    @NotNull
    protected AdaptiveTiming buildAdaptiveTiming(@NotNull List<Long> frameDelays, long fallbackDefaultDelayMs) {
        long normalizedFallbackDelay = Math.max(1L, fallbackDefaultDelayMs);
        if (frameDelays.isEmpty()) {
            return new AdaptiveTiming(normalizedFallbackDelay, new LinkedHashMap<>());
        }

        // Pick the most metadata-efficient default delay and only keep emitted-frame overrides.
        List<Long> normalizedFrameDelays = new ArrayList<>(frameDelays.size());
        java.util.LinkedHashSet<Long> candidateDefaultDelays = new java.util.LinkedHashSet<>();
        candidateDefaultDelays.add(normalizedFallbackDelay);
        for (Long frameDelay : frameDelays) {
            long normalizedDelay = Math.max(1L, Objects.requireNonNull(frameDelay));
            normalizedFrameDelays.add(normalizedDelay);
            candidateDefaultDelays.add(normalizedDelay);
        }

        long bestDefaultDelay = normalizedFallbackDelay;
        long bestCost = Long.MAX_VALUE;
        int bestOverrideCount = Integer.MAX_VALUE;
        for (Long candidateDefaultDelay : candidateDefaultDelays) {
            long candidateDelay = Objects.requireNonNull(candidateDefaultDelay);
            long cost = this.estimateTimingMetadataBytes(normalizedFrameDelays, candidateDelay);
            int overrideCount = this.countTimingOverrides(normalizedFrameDelays, candidateDelay);
            if ((cost < bestCost)
                    || ((cost == bestCost) && (overrideCount < bestOverrideCount))
                    || ((cost == bestCost) && (overrideCount == bestOverrideCount)
                    && (candidateDelay == normalizedFallbackDelay) && (bestDefaultDelay != normalizedFallbackDelay))) {
                bestDefaultDelay = candidateDelay;
                bestCost = cost;
                bestOverrideCount = overrideCount;
            }
        }

        LinkedHashMap<Integer, Long> customFrameTimes = new LinkedHashMap<>();
        for (int frameIndex = 0; frameIndex < normalizedFrameDelays.size(); frameIndex++) {
            long frameDelay = normalizedFrameDelays.get(frameIndex);
            if (frameDelay != bestDefaultDelay) {
                customFrameTimes.put(frameIndex, frameDelay);
            }
        }

        return new AdaptiveTiming(bestDefaultDelay, customFrameTimes);
    }

    protected long estimateTimingMetadataBytes(@NotNull List<Long> frameDelays, long defaultDelayMs) {
        long bytes = decimalLength(defaultDelayMs);
        boolean hasCustomFrameTimes = false;
        for (int frameIndex = 0; frameIndex < frameDelays.size(); frameIndex++) {
            long frameDelay = frameDelays.get(frameIndex);
            if (frameDelay == defaultDelayMs) {
                continue;
            }

            hasCustomFrameTimes = true;
            bytes += decimalLength(frameIndex) + decimalLength(frameDelay) + 4L;
        }
        if (hasCustomFrameTimes) {
            bytes += 2L;
        }
        return bytes;
    }

    protected int countTimingOverrides(@NotNull List<Long> frameDelays, long defaultDelayMs) {
        int count = 0;
        for (Long frameDelay : frameDelays) {
            if (Objects.requireNonNull(frameDelay) != defaultDelayMs) {
                count++;
            }
        }
        return count;
    }

    protected static int decimalLength(long value) {
        return Long.toString(Math.max(0L, value)).length();
    }

    protected static long addDelaysSaturating(long left, long right) {
        if ((Long.MAX_VALUE - left) < right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    protected int resolveEffectivePlannerWindowFrames(@NotNull AfmaEncodeOptions options, @NotNull Dimension dimension) {
        int configuredWindowFrames = Math.max(1, options.getPlannerSearchWindowFrames());
        long frameBytes = estimateFrameBytes(dimension);
        long maxMemory = Runtime.getRuntime().maxMemory();
        if (frameBytes <= 0L || maxMemory <= 0L) {
            return configuredWindowFrames;
        }

        long memoryBudget = Math.max(frameBytes, Math.max(1L, (maxMemory - MEMORY_SAFETY_RESERVE_BYTES) / 12L));
        int memorySafeWindowFrames = (int) Math.max(1L, memoryBudget / Math.max(1L, frameBytes));
        return Math.min(configuredWindowFrames, memorySafeWindowFrames);
    }

    protected int resolveEffectivePlannerBeamWidth(@NotNull AfmaEncodeOptions options, @NotNull Dimension dimension) {
        int configuredBeamWidth = Math.max(1, options.getPlannerBeamWidth());
        long frameBytes = estimateFrameBytes(dimension);
        long maxMemory = Runtime.getRuntime().maxMemory();
        if (frameBytes <= 0L || maxMemory <= 0L) {
            return configuredBeamWidth;
        }

        long memoryBudget = Math.max(frameBytes, Math.max(1L, (maxMemory - MEMORY_SAFETY_RESERVE_BYTES) / 6L));
        long estimatedStateBytes = frameBytes + Math.max(frameBytes / 2L, 64L * 1024L);
        int memorySafeBeamWidth = (int) Math.max(1L, memoryBudget / Math.max(1L, estimatedStateBytes));
        return Math.min(configuredBeamWidth, memorySafeBeamWidth);
    }

    protected static long estimateFrameBytes(@NotNull Dimension dimension) {
        return Math.max(0L, (long) dimension.width() * (long) dimension.height() * Integer.BYTES);
    }

    protected boolean shouldParallelizeBlockInterTiles(int totalTileCount, @NotNull AfmaRect regionBounds) {
        if ((totalTileCount < MIN_PARALLEL_BLOCK_INTER_TILES) || (Runtime.getRuntime().availableProcessors() <= 1)) {
            return false;
        }

        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        if (maxMemory <= 0L) {
            return false;
        }

        long usedMemory = Math.max(0L, runtime.totalMemory() - runtime.freeMemory());
        long headroomBytes = Math.max(0L, maxMemory - usedMemory);
        long regionBytes = Math.max(0L, regionBounds.area()) * Integer.BYTES;
        long requiredHeadroomBytes = Math.max(MEMORY_SAFETY_RESERVE_BYTES, Math.max(regionBytes, 32L * 1024L * 1024L));
        return headroomBytes >= requiredHeadroomBytes;
    }

    @NotNull
    protected AfmaPixelFrame loadPlanningFrame(@NotNull AfmaSourceSequence sequence, int frameIndex,
                                               @NotNull Dimension dimension, @Nullable AfmaPixelFrame firstFrameOverride,
                                               @Nullable BooleanSupplier cancellationRequested,
                                               @Nullable ProgressListener progressListener, boolean introSequence,
                                               int startOffset, int totalFrameCount) throws IOException {
        checkCancelled(cancellationRequested);
        this.reportPlanningFrameProgress(progressListener, "Loading", introSequence,
                frameIndex + 1, sequence.size(), startOffset + frameIndex, totalFrameCount);
        File frameFile = Objects.requireNonNull(sequence.getFrame(frameIndex));
        AfmaPixelFrame sourceFrame = ((firstFrameOverride != null) && (frameIndex == 0))
                ? firstFrameOverride
                : this.frameNormalizer.loadFrame(frameFile);
        if ((sourceFrame.getWidth() != dimension.width()) || (sourceFrame.getHeight() != dimension.height())) {
            throw new IOException("AFMA source frame dimensions do not match the expected canvas size: " + frameFile.getAbsolutePath());
        }
        return sourceFrame;
    }

    @NotNull
    protected BeamPlanningState planWindow(@NotNull AfmaSourceSequence sequence, int windowStartFrameIndex, int windowEndFrameIndex,
                                           @NotNull Dimension dimension, @Nullable AfmaPixelFrame firstFrameOverride,
                                           boolean introSequence, @NotNull AfmaEncodeOptions options,
                                           @NotNull AfmaRectCopyDetector copyDetector, @NotNull BeamPlanningState baseState,
                                           @Nullable BooleanSupplier cancellationRequested,
                                           @Nullable ProgressListener progressListener,
                                           int startOffset, int totalFrameCount, int sequenceFrameCount) throws IOException {
        List<BeamPlanningState> beam = new ArrayList<>();
        beam.add(baseState.toCommittedBaseState());
        BeamPlanningState bestState = null;
        AfmaPixelFrame preloadedFrame = firstFrameOverride;
        int plannerBeamWidth = this.resolveEffectivePlannerBeamWidth(options, dimension);
        ArrayList<BeamPlanningState> expandedBeam = null;
        try {
            for (int absoluteFrameIndex = windowStartFrameIndex; absoluteFrameIndex < windowEndFrameIndex; absoluteFrameIndex++) {
                checkCancelled(cancellationRequested);
                AfmaPixelFrame sourceFrame = this.loadPlanningFrame(sequence, absoluteFrameIndex, dimension, preloadedFrame,
                        cancellationRequested, progressListener, introSequence, startOffset, totalFrameCount);
                preloadedFrame = null;
                this.reportPlanningFrameProgress(progressListener, "Planning", introSequence,
                        absoluteFrameIndex + 1, sequenceFrameCount, startOffset + absoluteFrameIndex + 0.5D, totalFrameCount);
                long frameDelayMs = this.resolveSourceFrameDelay(options, introSequence, absoluteFrameIndex);
                WindowCandidateCache windowCandidateCache = new WindowCandidateCache();
                try {
                    expandedBeam = new ArrayList<>();
                    for (BeamPlanningState state : beam) {
                        AfmaPixelFrame previousFrame = state.previousFrame();
                        AfmaPixelFrame workingFrame = sourceFrame;
                        if ((previousFrame != null) && options.isNearLosslessEnabled()
                                && this.shouldAllowPerceptualContinuation(state.framesSinceKeyframe(), options)) {
                            workingFrame = windowCandidateCache.getNearLosslessMergedFrame(
                                    previousFrame,
                                    sourceFrame,
                                    options.getNearLosslessMaxChannelDelta()
                            );
                        }

                        AfmaFramePairAnalysis workingPairAnalysis = (previousFrame != null)
                                ? windowCandidateCache.getFramePairAnalysis(previousFrame, workingFrame)
                                : null;
                        if ((workingPairAnalysis != null) && state.emittedFrameAvailable()
                                && options.isDuplicateFrameElision()
                                && workingPairAnalysis.isIdentical()) {
                            BeamPlanningState duplicateState = this.evaluateDuplicateContinuation(
                                    state,
                                    windowCandidateCache.getFramePairAnalysis(sourceFrame, previousFrame),
                                    frameDelayMs,
                                    options
                            );
                            if (duplicateState != null) {
                                expandedBeam.add(duplicateState);
                            }
                        }

                        List<PlannedCandidate> candidates = this.collectWindowCandidates(previousFrame, sourceFrame, workingFrame,
                                introSequence, absoluteFrameIndex, state.framesSinceKeyframe(), state.decodeComplexitySinceKeyframe(),
                                options, copyDetector, state.archiveState(), windowCandidateCache);
                        for (PlannedCandidate candidate : candidates) {
                            BeamPlanningState candidateState = this.evaluateCandidateTransition(
                                    state,
                                    candidate,
                                    windowCandidateCache.getCandidateReferenceFrameAnalysis(candidate, sourceFrame, workingFrame),
                                    windowCandidateCache,
                                    frameDelayMs,
                                    introSequence,
                                    options
                            );
                            if (candidateState != null) {
                                expandedBeam.add(candidateState);
                            }
                        }
                    }

                    List<BeamPlanningState> nextBeam = this.prunePlanningBeam(expandedBeam, plannerBeamWidth);
                    nextBeam = this.refinePlanningBeamArchiveScores(nextBeam);
                    Set<PlannedCandidate> retainedCandidates = Collections.newSetFromMap(new IdentityHashMap<>());
                    for (BeamPlanningState state : nextBeam) {
                        this.collectStateCandidates(state, retainedCandidates);
                    }
                    windowCandidateCache.closeCandidates(retainedCandidates);
                    this.closeDiscardedBeamStateCandidates(expandedBeam, nextBeam);
                    this.discardBeamCandidateReferencePixels(nextBeam);
                    if (nextBeam.isEmpty()) {
                        throw new IOException("AFMA planner failed to find a valid candidate for source frame " + (absoluteFrameIndex + 1));
                    }
                    beam = nextBeam;
                    expandedBeam = null;
                } catch (Throwable throwable) {
                    windowCandidateCache.closeAllCandidates();
                    throw throwable;
                }
            }
            bestState = beam.get(0);
            this.closeDiscardedBeamStateCandidates(beam, List.of(bestState));
            return bestState;
        } finally {
            if (bestState == null) {
                this.closeDiscardedBeamStateCandidates(beam, List.of());
                if (expandedBeam != null) {
                    this.closeDiscardedBeamStateCandidates(expandedBeam, List.of());
                }
            }
            CloseableUtils.closeQuietly(preloadedFrame);
        }
    }

    protected void closeDiscardedBeamStateCandidates(@NotNull List<BeamPlanningState> states,
                                                     @NotNull List<BeamPlanningState> retainedStates) {
        Set<PlannedCandidate> retainedCandidates = Collections.newSetFromMap(new IdentityHashMap<>());
        for (BeamPlanningState retainedState : retainedStates) {
            this.collectStateCandidates(retainedState, retainedCandidates);
        }

        Set<PlannedCandidate> closedCandidates = Collections.newSetFromMap(new IdentityHashMap<>());
        for (BeamPlanningState state : states) {
            PlanningStep step = state.tailStep;
            while (step != null) {
                PlannedCandidate candidate = step.candidate();
                if ((candidate != null) && !retainedCandidates.contains(candidate) && closedCandidates.add(candidate)) {
                    candidate.closePayloads();
                }
                step = step.previous;
            }
        }
    }

    protected void discardBeamCandidateReferencePixels(@NotNull List<BeamPlanningState> states) {
        Set<PlannedCandidate> visitedCandidates = Collections.newSetFromMap(new IdentityHashMap<>());
        for (BeamPlanningState state : states) {
            PlanningStep step = state.tailStep;
            while (step != null) {
                PlannedCandidate candidate = step.candidate();
                if ((candidate != null) && visitedCandidates.add(candidate)) {
                    candidate.discardReferencePixels();
                }
                step = step.previous;
            }
        }
    }

    protected void collectStateCandidates(@NotNull BeamPlanningState state, @NotNull Set<PlannedCandidate> collectedCandidates) {
        PlanningStep step = state.tailStep;
        while (step != null) {
            PlannedCandidate candidate = step.candidate();
            if (candidate != null) {
                collectedCandidates.add(candidate);
            }
            step = step.previous;
        }
    }

    @NotNull
    protected List<PlannedCandidate> collectWindowCandidates(@Nullable AfmaPixelFrame previousFrame,
                                                             @NotNull AfmaPixelFrame sourceFrame,
                                                             @NotNull AfmaPixelFrame workingFrame,
                                                             boolean introSequence, int frameIndex, int framesSinceKeyframe,
                                                             int decodeComplexitySinceKeyframe,
                                                             @NotNull AfmaEncodeOptions options,
                                                             @NotNull AfmaRectCopyDetector copyDetector,
                                                             @NotNull ArchivePlanningState archiveState,
                                                             @NotNull WindowCandidateCache windowCandidateCache) throws IOException {
        PlannedCandidate fullCandidate = windowCandidateCache.getFullCandidate(
                workingFrame,
                introSequence,
                frameIndex,
                options,
                true,
                ReferenceBase.WORKING_FRAME
        );
        PlannedCandidate exactFullCandidate = fullCandidate.isExactRelativeToSourceFrame(sourceFrame, workingFrame)
                ? fullCandidate
                : windowCandidateCache.getFullCandidate(
                sourceFrame,
                introSequence,
                frameIndex,
                options,
                false,
                ReferenceBase.SOURCE_FRAME
        );
        ArrayList<PlannedCandidate> candidates = new ArrayList<>();
        if ((previousFrame == null) || this.isHardKeyframeRefreshRequired(framesSinceKeyframe, decodeComplexitySinceKeyframe, options)) {
            candidates.add(exactFullCandidate);
            return candidates;
        }

        candidates.add(fullCandidate);
        if (!fullCandidate.isExactRelativeToSourceFrame(sourceFrame, workingFrame)) {
            candidates.add(exactFullCandidate);
        }
        // Use descriptor-only optimistic bounds first, then materialize exact payloads only for candidates that can still clear the archive-aware keep gate.
        long fullArchiveBytes = windowCandidateCache.getArchiveAppendCost(archiveState, fullCandidate, introSequence).marginalArchiveBytes();

        PairCandidateSet pairCandidateSet = windowCandidateCache.getPairCandidateSet(
                previousFrame,
                workingFrame,
                introSequence,
                frameIndex,
                options,
                copyDetector
        );
        AfmaRect deltaBounds = pairCandidateSet.deltaBounds();
        PlannedCandidate blockInterBaselineCandidate = fullCandidate;
        if (deltaBounds != null) {
            long deltaArea = deltaBounds.area();
            if (this.shouldAttemptComplexCandidate(pairCandidateSet.deltaCandidateArchiveLowerBoundBytes(), fullArchiveBytes,
                    deltaArea, workingFrame.getWidth(), workingFrame.getHeight(),
                    options.getMaxDeltaAreaRatioWithoutStrongSavings(), options)) {
                PlannedCandidate deltaCandidate = pairCandidateSet.getOrCreateDeltaCandidate();
                blockInterBaselineCandidate = this.selectBestEstimatedArchiveCandidate(blockInterBaselineCandidate, deltaCandidate);
                if ((deltaCandidate != null) && this.shouldKeepComplexCandidate(deltaCandidate, fullArchiveBytes,
                        deltaArea, workingFrame.getWidth(), workingFrame.getHeight(),
                        options.getMaxDeltaAreaRatioWithoutStrongSavings(), options, archiveState, introSequence, windowCandidateCache)) {
                    candidates.add(deltaCandidate);
                }
            }

            if (this.shouldAttemptResidualCandidate(pairCandidateSet.residualDeltaCandidateArchiveLowerBoundBytes(), fullArchiveBytes,
                    deltaArea, workingFrame.getWidth(), workingFrame.getHeight(),
                    options.getMaxDeltaAreaRatioWithoutStrongSavings(), options)) {
                PlannedCandidate residualDeltaCandidate = pairCandidateSet.getOrCreateResidualDeltaCandidate();
                blockInterBaselineCandidate = this.selectBestEstimatedArchiveCandidate(blockInterBaselineCandidate, residualDeltaCandidate);
                if ((residualDeltaCandidate != null) && this.shouldKeepResidualCandidate(residualDeltaCandidate, fullArchiveBytes,
                        deltaArea, workingFrame.getWidth(), workingFrame.getHeight(),
                        options.getMaxDeltaAreaRatioWithoutStrongSavings(), options, archiveState, introSequence, windowCandidateCache)) {
                    candidates.add(residualDeltaCandidate);
                }
            }

            if (this.shouldAttemptSparseCandidate(pairCandidateSet.sparseDeltaCandidateArchiveLowerBoundBytes(), fullArchiveBytes,
                    deltaArea, workingFrame.getWidth(), workingFrame.getHeight(),
                    options.getMaxDeltaAreaRatioWithoutStrongSavings(), options)) {
                PlannedCandidate sparseDeltaCandidate = pairCandidateSet.getOrCreateSparseDeltaCandidate();
                blockInterBaselineCandidate = this.selectBestEstimatedArchiveCandidate(blockInterBaselineCandidate, sparseDeltaCandidate);
                if ((sparseDeltaCandidate != null) && this.shouldKeepSparseCandidate(sparseDeltaCandidate, fullArchiveBytes,
                        deltaArea, workingFrame.getWidth(), workingFrame.getHeight(),
                        options.getMaxDeltaAreaRatioWithoutStrongSavings(), options, archiveState, introSequence, windowCandidateCache)) {
                    candidates.add(sparseDeltaCandidate);
                }
            }
        }

        if (options.isRectCopyEnabled()) {
            AfmaRectCopyDetector.Detection detection = pairCandidateSet.copyDetection();
            if (detection != null) {
                long patchArea = (detection.patchBounds() != null) ? detection.patchBounds().area() : 0L;
                if (this.shouldAttemptComplexCandidate(pairCandidateSet.copyCandidateArchiveLowerBoundBytes(), fullArchiveBytes,
                        patchArea, workingFrame.getWidth(), workingFrame.getHeight(),
                        options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options)) {
                    PlannedCandidate copyCandidate = pairCandidateSet.getOrCreateCopyCandidate();
                    blockInterBaselineCandidate = this.selectBestEstimatedArchiveCandidate(blockInterBaselineCandidate, copyCandidate);
                    if ((copyCandidate != null) && this.shouldKeepComplexCandidate(copyCandidate, fullArchiveBytes,
                            patchArea, workingFrame.getWidth(), workingFrame.getHeight(),
                            options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options, archiveState, introSequence, windowCandidateCache)) {
                        candidates.add(copyCandidate);
                    }
                }

                if (this.shouldAttemptResidualCandidate(pairCandidateSet.copyResidualCandidateArchiveLowerBoundBytes(), fullArchiveBytes,
                        patchArea, workingFrame.getWidth(), workingFrame.getHeight(),
                        options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options)) {
                    PlannedCandidate copyResidualCandidate = pairCandidateSet.getOrCreateCopyResidualCandidate();
                    blockInterBaselineCandidate = this.selectBestEstimatedArchiveCandidate(blockInterBaselineCandidate, copyResidualCandidate);
                    if ((copyResidualCandidate != null) && this.shouldKeepResidualCandidate(copyResidualCandidate, fullArchiveBytes,
                            patchArea, workingFrame.getWidth(), workingFrame.getHeight(),
                            options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options, archiveState, introSequence, windowCandidateCache)) {
                        candidates.add(copyResidualCandidate);
                    }
                }

                if (this.shouldAttemptSparseCandidate(pairCandidateSet.copySparseCandidateArchiveLowerBoundBytes(), fullArchiveBytes,
                        patchArea, workingFrame.getWidth(), workingFrame.getHeight(),
                        options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options)) {
                    PlannedCandidate copySparseCandidate = pairCandidateSet.getOrCreateCopySparseCandidate();
                    blockInterBaselineCandidate = this.selectBestEstimatedArchiveCandidate(blockInterBaselineCandidate, copySparseCandidate);
                    if ((copySparseCandidate != null) && this.shouldKeepSparseCandidate(copySparseCandidate, fullArchiveBytes,
                            patchArea, workingFrame.getWidth(), workingFrame.getHeight(),
                            options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options, archiveState, introSequence, windowCandidateCache)) {
                        candidates.add(copySparseCandidate);
                    }
                }
            }

            AfmaRectCopyDetector.MultiDetection multiDetection = pairCandidateSet.multiDetection();
            if (multiDetection != null) {
                long patchArea = multiDetection.patchArea();
                if (this.shouldAttemptComplexCandidate(pairCandidateSet.multiCopyCandidateArchiveLowerBoundBytes(), fullArchiveBytes,
                        patchArea, workingFrame.getWidth(), workingFrame.getHeight(),
                        options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options)) {
                    PlannedCandidate multiCopyCandidate = pairCandidateSet.getOrCreateMultiCopyCandidate();
                    blockInterBaselineCandidate = this.selectBestEstimatedArchiveCandidate(blockInterBaselineCandidate, multiCopyCandidate);
                    if ((multiCopyCandidate != null) && this.shouldKeepComplexCandidate(multiCopyCandidate, fullArchiveBytes,
                            patchArea, workingFrame.getWidth(), workingFrame.getHeight(),
                            options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options, archiveState, introSequence, windowCandidateCache)) {
                        candidates.add(multiCopyCandidate);
                    }
                }

                if (this.shouldAttemptResidualCandidate(pairCandidateSet.multiCopyResidualCandidateArchiveLowerBoundBytes(), fullArchiveBytes,
                        patchArea, workingFrame.getWidth(), workingFrame.getHeight(),
                        options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options)) {
                    PlannedCandidate multiCopyResidualCandidate = pairCandidateSet.getOrCreateMultiCopyResidualCandidate();
                    blockInterBaselineCandidate = this.selectBestEstimatedArchiveCandidate(blockInterBaselineCandidate, multiCopyResidualCandidate);
                    if ((multiCopyResidualCandidate != null) && this.shouldKeepResidualCandidate(multiCopyResidualCandidate, fullArchiveBytes,
                            patchArea, workingFrame.getWidth(), workingFrame.getHeight(),
                            options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options, archiveState, introSequence, windowCandidateCache)) {
                        candidates.add(multiCopyResidualCandidate);
                    }
                }

                if (this.shouldAttemptSparseCandidate(pairCandidateSet.multiCopySparseCandidateArchiveLowerBoundBytes(), fullArchiveBytes,
                        patchArea, workingFrame.getWidth(), workingFrame.getHeight(),
                        options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options)) {
                    PlannedCandidate multiCopySparseCandidate = pairCandidateSet.getOrCreateMultiCopySparseCandidate();
                    blockInterBaselineCandidate = this.selectBestEstimatedArchiveCandidate(blockInterBaselineCandidate, multiCopySparseCandidate);
                    if ((multiCopySparseCandidate != null) && this.shouldKeepSparseCandidate(multiCopySparseCandidate, fullArchiveBytes,
                            patchArea, workingFrame.getWidth(), workingFrame.getHeight(),
                            options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options, archiveState, introSequence, windowCandidateCache)) {
                        candidates.add(multiCopySparseCandidate);
                    }
                }
            }
        }

        if (options.isRectCopyEnabled() && (deltaBounds != null)) {
            AfmaRect blockInterBounds = pairCandidateSet.blockInterRegionBounds();
            if ((blockInterBounds != null) && this.shouldAttemptComplexCandidate(pairCandidateSet.blockInterCandidateArchiveLowerBoundBytes(),
                    fullArchiveBytes, blockInterBounds.area(), workingFrame.getWidth(), workingFrame.getHeight(),
                    options.getMaxDeltaAreaRatioWithoutStrongSavings(), options)) {
                PlannedCandidate blockInterCandidate = pairCandidateSet.getOrCreateBlockInterCandidate(blockInterBaselineCandidate);
                if ((blockInterCandidate != null) && this.shouldKeepComplexCandidate(blockInterCandidate, fullArchiveBytes,
                        (long) blockInterCandidate.descriptor().getWidth() * blockInterCandidate.descriptor().getHeight(),
                        workingFrame.getWidth(), workingFrame.getHeight(),
                        options.getMaxDeltaAreaRatioWithoutStrongSavings(), options, archiveState, introSequence, windowCandidateCache)) {
                    candidates.add(blockInterCandidate);
                }
            }
        }

        return candidates;
    }

    @Nullable
    protected BeamPlanningState evaluateDuplicateContinuation(@NotNull BeamPlanningState state,
                                                              @NotNull AfmaFramePairAnalysis sourcePairAnalysis,
                                                              long frameDelayMs, @NotNull AfmaEncodeOptions options) {
        AfmaPixelFrame previousFrame = state.previousFrame();
        if (previousFrame == null) {
            return null;
        }

        DriftTransition driftTransition = this.evaluateDriftTransition(state.driftState(), sourcePairAnalysis, options);
        if (driftTransition == null) {
            return null;
        }
        return state.advanceDelay(frameDelayMs, driftTransition.nextState(), driftTransition.scorePenalty());
    }

    @Nullable
    protected BeamPlanningState evaluateCandidateTransition(@NotNull BeamPlanningState state, @NotNull PlannedCandidate candidate,
                                                            @NotNull CandidateReferenceFrameAnalysis referenceFrameAnalysis,
                                                            @NotNull WindowCandidateCache windowCandidateCache,
                                                            long frameDelayMs, boolean introSequence, @NotNull AfmaEncodeOptions options) {
        AfmaPixelFrame reconstructedFrame = referenceFrameAnalysis.reconstructedFrame();
        DriftTransition driftTransition = this.evaluateDriftTransition(state.driftState(), referenceFrameAnalysis.sourcePairAnalysis(), options);
        if (driftTransition == null) {
            return null;
        }

        CandidateArchiveCost archiveCost = windowCandidateCache.getArchiveAppendCost(state.archiveState(), candidate, introSequence);
        int nextFramesSinceKeyframe = candidate.descriptor().isKeyframe() ? 0 : (state.framesSinceKeyframe() + 1);
        int nextDecodeComplexitySinceKeyframe = candidate.descriptor().isKeyframe()
                ? 0
                : (state.decodeComplexitySinceKeyframe() + candidate.complexityScore);
        long nextInterArchiveBytesSinceKeyframe = candidate.descriptor().isKeyframe()
                ? 0L
                : (state.interArchiveBytesSinceKeyframe() + archiveCost.marginalArchiveBytes());
        double scoreIncrement = archiveCost.marginalScoreBytes()
                + this.computeDecodePenalty(candidate, options)
                + this.computeComplexityPenalty(candidate, options)
                + driftTransition.scorePenalty()
                + this.computeKeyframeDistancePenalty(nextFramesSinceKeyframe, candidate.descriptor().isKeyframe(),
                nextDecodeComplexitySinceKeyframe, nextInterArchiveBytesSinceKeyframe, driftTransition.nextState(), options);
        return state.advanceCandidate(candidate, reconstructedFrame, nextFramesSinceKeyframe, driftTransition.nextState(),
                archiveCost.nextState(), frameDelayMs, scoreIncrement, archiveCost.marginalArchiveBytes(),
                nextDecodeComplexitySinceKeyframe, nextInterArchiveBytesSinceKeyframe);
    }

    protected void commitPlannedWindow(@NotNull BeamPlanningState bestWindowState, @NotNull List<PlannedTimedFrame> plannedFrames,
                                       @NotNull Map<String, AfmaStoredPayload> payloads,
                                       @NotNull Map<String, String> payloadPathsByFingerprint) throws IOException {
        for (PlanningStep planningStep : bestWindowState.stepsInOrder()) {
            if (planningStep.isDelayExtension()) {
                this.extendPlannedFrameDelay(plannedFrames, planningStep.delayMs());
                continue;
            }

            AfmaFrameDescriptor finalizedDescriptor = Objects.requireNonNull(planningStep.candidate()).internPayloads(payloads, payloadPathsByFingerprint);
            plannedFrames.add(new PlannedTimedFrame(finalizedDescriptor, planningStep.delayMs()));
        }
    }

    @NotNull
    protected List<BeamPlanningState> prunePlanningBeam(@NotNull List<BeamPlanningState> beam, int beamWidth) {
        ArrayList<BeamPlanningState> sortedBeam = new ArrayList<>(beam);
        sortedBeam.sort((first, second) -> {
            int compare = Double.compare(first.objectiveScore(), second.objectiveScore());
            if (compare != 0) {
                return compare;
            }
            compare = Long.compare(first.estimatedArchiveBytes(), second.estimatedArchiveBytes());
            if (compare != 0) {
                return compare;
            }
            compare = Integer.compare(first.driftState().consecutiveLossyFrames(), second.driftState().consecutiveLossyFrames());
            if (compare != 0) {
                return compare;
            }
            compare = Double.compare(first.driftState().cumulativeAverageError(), second.driftState().cumulativeAverageError());
            if (compare != 0) {
                return compare;
            }
            compare = Integer.compare(first.decodeComplexitySinceKeyframe(), second.decodeComplexitySinceKeyframe());
            if (compare != 0) {
                return compare;
            }
            return Integer.compare(first.framesSinceKeyframe(), second.framesSinceKeyframe());
        });
        if (sortedBeam.size() <= beamWidth) {
            return sortedBeam;
        }
        return new ArrayList<>(sortedBeam.subList(0, beamWidth));
    }

    @NotNull
    protected List<BeamPlanningState> refinePlanningBeamArchiveScores(@NotNull List<BeamPlanningState> beam) {
        if (beam.isEmpty()) {
            return beam;
        }

        // Keep the beam expansion loop cheap, then snap surviving states back to exact archive costs.
        ArrayList<BeamPlanningState> refinedBeam = new ArrayList<>(beam.size());
        boolean changed = false;
        for (BeamPlanningState state : beam) {
            BeamPlanningState refinedState = state.refineArchiveScore();
            refinedBeam.add(refinedState);
            changed |= refinedState != state;
        }
        if (!changed) {
            return beam;
        }
        return this.prunePlanningBeam(refinedBeam, refinedBeam.size());
    }

    @Nullable
    protected DriftTransition evaluateDriftTransition(@NotNull DriftState currentDriftState,
                                                      @NotNull AfmaFramePairAnalysis pairAnalysis,
                                                      @NotNull AfmaEncodeOptions options) {
        PerceptualDriftStats driftStats = this.measurePerceptualDrift(pairAnalysis);
        if (!driftStats.isExact()) {
            if (!options.isPerceptualBinIntraEnabled()) {
                return null;
            }
            if ((driftStats.averageError() > options.getPerceptualBinIntraMaxAverageError())
                    || (driftStats.maxVisibleColorDelta() > options.getPerceptualBinIntraMaxVisibleColorDelta())
                    || (driftStats.maxAlphaDelta() > options.getPerceptualBinIntraMaxAlphaDelta())) {
                return null;
            }
        }

        DriftState nextDriftState = driftStats.isExact()
                ? DriftState.exact()
                : currentDriftState.accumulate(driftStats);
        if (!driftStats.isExact()) {
            if ((options.getPlannerMaxCumulativeAverageError() > 0D)
                    && (nextDriftState.cumulativeAverageError() > options.getPlannerMaxCumulativeAverageError())) {
                return null;
            }
            if ((options.getPlannerMaxCumulativeVisibleColorDelta() > 0)
                    && (nextDriftState.cumulativeVisibleColorDelta() > options.getPlannerMaxCumulativeVisibleColorDelta())) {
                return null;
            }
            if ((options.getPlannerMaxCumulativeAlphaDelta() > 0)
                    && (nextDriftState.cumulativeAlphaDelta() > options.getPlannerMaxCumulativeAlphaDelta())) {
                return null;
            }
            if ((options.getPlannerMaxConsecutiveLossyFrames() > 0)
                    && (nextDriftState.consecutiveLossyFrames() > options.getPlannerMaxConsecutiveLossyFrames())) {
                return null;
            }
        }

        return new DriftTransition(nextDriftState, driftStats, this.computeDriftPenalty(driftStats, nextDriftState, options));
    }

    @Nullable
    protected DriftTransition evaluateDriftTransition(@NotNull DriftState currentDriftState,
                                                      @NotNull AfmaPixelFrame sourceFrame,
                                                      @NotNull AfmaPixelFrame reconstructedFrame,
                                                      @NotNull AfmaEncodeOptions options) {
        return this.evaluateDriftTransition(currentDriftState, new AfmaFramePairAnalysis(sourceFrame, reconstructedFrame), options);
    }

    protected double computeDriftPenalty(@NotNull PerceptualDriftStats driftStats, @NotNull DriftState nextDriftState,
                                         @NotNull AfmaEncodeOptions options) {
        double penalty = (driftStats.averageError() * options.getPlannerAverageDriftPenaltyBytes())
                + ((double) driftStats.maxVisibleColorDelta() * options.getPlannerVisibleColorDriftPenaltyBytes())
                + ((double) driftStats.maxAlphaDelta() * options.getPlannerAlphaDriftPenaltyBytes());
        if (!driftStats.isExact()) {
            penalty += options.getPlannerLossyContinuationPenaltyBytes();
            penalty += nextDriftState.consecutiveLossyFrames() * (options.getPlannerLossyContinuationPenaltyBytes() * 0.5D);
            penalty += nextDriftState.cumulativeAverageError() * (options.getPlannerAverageDriftPenaltyBytes() * 0.10D);
        }
        return penalty;
    }

    protected double computeDecodePenalty(@NotNull PlannedCandidate candidate, @NotNull AfmaEncodeOptions options) {
        return (double) candidate.decodeCost.ordinal() * options.getPlannerDecodeCostPenaltyBytes();
    }

    protected double computeComplexityPenalty(@NotNull PlannedCandidate candidate, @NotNull AfmaEncodeOptions options) {
        return (double) candidate.complexityScore * options.getPlannerComplexityPenaltyBytes();
    }

    protected double computeKeyframeDistancePenalty(int framesSinceKeyframe, boolean emittedKeyframe,
                                                    int decodeComplexitySinceKeyframe, long interArchiveBytesSinceKeyframe,
                                                    @NotNull DriftState driftState, @NotNull AfmaEncodeOptions options) {
        if (emittedKeyframe || !options.isAdaptiveKeyframePlacementEnabled()) {
            return 0D;
        }

        int preferredInterval = Math.max(1, options.getKeyframeInterval());
        int hardInterval = Math.max(preferredInterval, options.getAdaptiveMaxKeyframeInterval());
        double penalty = 0D;
        if (framesSinceKeyframe >= preferredInterval) {
            int overflow = (framesSinceKeyframe - preferredInterval) + 1;
            double normalizedOverflow = (double) overflow / Math.max(1D, (double) ((hardInterval - preferredInterval) + 1));
            penalty += normalizedOverflow * options.getPlannerKeyframeDistancePenaltyBytes();
        }

        int preferredComplexity = Math.max(8, preferredInterval * 4);
        if (decodeComplexitySinceKeyframe > preferredComplexity) {
            penalty += (decodeComplexitySinceKeyframe - preferredComplexity) * (options.getPlannerComplexityPenaltyBytes() * 0.20D);
        }
        if (interArchiveBytesSinceKeyframe > 0L) {
            penalty += Math.log1p(interArchiveBytesSinceKeyframe / 256.0D) * (options.getPlannerKeyframeDistancePenaltyBytes() * 0.15D);
        }
        if (driftState.consecutiveLossyFrames() > 0) {
            penalty += driftState.consecutiveLossyFrames() * (options.getPlannerLossyContinuationPenaltyBytes() * 0.35D);
            penalty += driftState.cumulativeAverageError() * (options.getPlannerAverageDriftPenaltyBytes() * 0.05D);
        }
        return penalty;
    }

    protected boolean isHardKeyframeRefreshRequired(int framesSinceKeyframe, int decodeComplexitySinceKeyframe,
                                                    @NotNull AfmaEncodeOptions options) {
        int hardInterval = options.isAdaptiveKeyframePlacementEnabled()
                ? options.getAdaptiveMaxKeyframeInterval()
                : options.getKeyframeInterval();
        if ((framesSinceKeyframe + 1) >= hardInterval) {
            return true;
        }

        int preferredInterval = Math.max(1, options.getKeyframeInterval());
        int complexityCap = Math.max(preferredInterval * 8, hardInterval * 5);
        return decodeComplexitySinceKeyframe >= complexityCap;
    }

    @NotNull
    protected PlannedCandidate chooseBestCandidate(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                                   boolean introSequence, int frameIndex, @NotNull AfmaEncodeOptions options,
                                                   @NotNull AfmaRectCopyDetector copyDetector,
                                                   @NotNull Map<String, String> payloadPathsByFingerprint,
                                                   @NotNull PlannedCandidate fullCandidate) throws IOException {
        List<PlannedCandidate> candidates = new ArrayList<>();
        candidates.add(fullCandidate);

        AfmaFramePairAnalysis pairAnalysis = new AfmaFramePairAnalysis(previousFrame, currentFrame);
        AfmaRect deltaBounds = pairAnalysis.differenceBounds();
        PlannedCandidate deltaCandidate = null;
        PlannedCandidate residualDeltaCandidate = null;
        PlannedCandidate sparseDeltaCandidate = null;
        if (deltaBounds != null) {
            deltaCandidate = this.createDeltaCandidate(currentFrame, introSequence, frameIndex, deltaBounds, options);
            if ((deltaCandidate != null) && this.shouldKeepComplexCandidate(deltaCandidate, fullCandidate,
                    deltaBounds.area(), currentFrame.getWidth(), currentFrame.getHeight(),
                    options.getMaxDeltaAreaRatioWithoutStrongSavings(), options, payloadPathsByFingerprint)) {
                candidates.add(deltaCandidate);
            }

            residualDeltaCandidate = this.createResidualDeltaCandidate(previousFrame, currentFrame, introSequence, frameIndex, deltaBounds);
            if ((residualDeltaCandidate != null) && this.shouldKeepResidualCandidate(residualDeltaCandidate, fullCandidate,
                    deltaBounds.area(), currentFrame.getWidth(), currentFrame.getHeight(),
                    options.getMaxDeltaAreaRatioWithoutStrongSavings(), options, payloadPathsByFingerprint)) {
                candidates.add(residualDeltaCandidate);
            }

            sparseDeltaCandidate = this.createSparseDeltaCandidate(previousFrame, currentFrame, introSequence, frameIndex, deltaBounds);
            if ((sparseDeltaCandidate != null) && this.shouldKeepSparseCandidate(sparseDeltaCandidate, fullCandidate,
                    deltaBounds.area(), currentFrame.getWidth(), currentFrame.getHeight(),
                    options.getMaxDeltaAreaRatioWithoutStrongSavings(), options, payloadPathsByFingerprint)) {
                candidates.add(sparseDeltaCandidate);
            }
        }

        AfmaRectCopyDetector.Detection detection = null;
        PlannedCandidate copyCandidate = null;
        PlannedCandidate copyResidualCandidate = null;
        PlannedCandidate copySparseCandidate = null;
        AfmaRectCopyDetector.MultiDetection multiDetection = null;
        PlannedCandidate multiCopyCandidate = null;
        PlannedCandidate multiCopyResidualCandidate = null;
        PlannedCandidate multiCopySparseCandidate = null;
        if (options.isRectCopyEnabled()) {
            detection = copyDetector.detect(pairAnalysis);
            if (detection != null) {
                copyCandidate = this.createCopyCandidate(currentFrame, introSequence, frameIndex, detection, options);
                long patchArea = (detection.patchBounds() != null) ? detection.patchBounds().area() : 0L;
                if ((copyCandidate != null) && this.shouldKeepComplexCandidate(copyCandidate, fullCandidate,
                        patchArea, currentFrame.getWidth(), currentFrame.getHeight(),
                        options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options, payloadPathsByFingerprint)) {
                    candidates.add(copyCandidate);
                }

                copyResidualCandidate = this.createCopyResidualCandidate(previousFrame, currentFrame, introSequence, frameIndex, detection);
                if ((copyResidualCandidate != null) && this.shouldKeepResidualCandidate(copyResidualCandidate, fullCandidate,
                        patchArea, currentFrame.getWidth(), currentFrame.getHeight(),
                        options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options, payloadPathsByFingerprint)) {
                    candidates.add(copyResidualCandidate);
                }

                copySparseCandidate = this.createCopySparseCandidate(previousFrame, currentFrame, introSequence, frameIndex, detection);
                if ((copySparseCandidate != null) && this.shouldKeepSparseCandidate(copySparseCandidate, fullCandidate,
                        patchArea, currentFrame.getWidth(), currentFrame.getHeight(),
                        options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options, payloadPathsByFingerprint)) {
                    candidates.add(copySparseCandidate);
                }
            }

            multiDetection = copyDetector.detectMulti(pairAnalysis, detection);
            if (multiDetection != null) {
                AfmaPixelFrame multiCopyReferenceFrame = this.buildMultiCopyReferenceFrame(previousFrame, multiDetection.multiCopy());
                multiCopyCandidate = this.createMultiCopyCandidate(currentFrame, introSequence, frameIndex, multiDetection, options);
                long patchArea = multiDetection.patchArea();
                if ((multiCopyCandidate != null) && this.shouldKeepComplexCandidate(multiCopyCandidate, fullCandidate,
                        patchArea, currentFrame.getWidth(), currentFrame.getHeight(),
                        options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options, payloadPathsByFingerprint)) {
                    candidates.add(multiCopyCandidate);
                }

                multiCopyResidualCandidate = this.createMultiCopyResidualCandidate(multiCopyReferenceFrame, currentFrame,
                        introSequence, frameIndex, multiDetection);
                if ((multiCopyResidualCandidate != null) && this.shouldKeepResidualCandidate(multiCopyResidualCandidate, fullCandidate,
                        patchArea, currentFrame.getWidth(), currentFrame.getHeight(),
                        options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options, payloadPathsByFingerprint)) {
                    candidates.add(multiCopyResidualCandidate);
                }

                multiCopySparseCandidate = this.createMultiCopySparseCandidate(multiCopyReferenceFrame, currentFrame,
                        introSequence, frameIndex, multiDetection);
                if ((multiCopySparseCandidate != null) && this.shouldKeepSparseCandidate(multiCopySparseCandidate, fullCandidate,
                        patchArea, currentFrame.getWidth(), currentFrame.getHeight(),
                        options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options, payloadPathsByFingerprint)) {
                    candidates.add(multiCopySparseCandidate);
                }
            }
        }

        if (options.isRectCopyEnabled() && (deltaBounds != null)) {
            PlannedCandidate blockInterBaselineCandidate = this.selectBestEstimatedArchiveCandidate(
                    fullCandidate,
                    deltaCandidate,
                    residualDeltaCandidate,
                    sparseDeltaCandidate,
                    copyCandidate,
                    copyResidualCandidate,
                    copySparseCandidate,
                    multiCopyCandidate,
                    multiCopyResidualCandidate,
                    multiCopySparseCandidate
            );
            PlannedCandidate blockInterCandidate = this.createBlockInterCandidate(
                    pairAnalysis,
                    introSequence,
                    frameIndex,
                    deltaBounds,
                    copyDetector,
                    detection,
                    multiDetection,
                    blockInterBaselineCandidate
            );
            if ((blockInterCandidate != null) && this.shouldKeepComplexCandidate(blockInterCandidate, fullCandidate,
                    (long) blockInterCandidate.descriptor().getWidth() * blockInterCandidate.descriptor().getHeight(),
                    currentFrame.getWidth(), currentFrame.getHeight(),
                    options.getMaxDeltaAreaRatioWithoutStrongSavings(), options, payloadPathsByFingerprint)) {
                candidates.add(blockInterCandidate);
            }
        }

        PlannedCandidate bestCandidate = null;
        for (PlannedCandidate candidate : candidates) {
            if ((bestCandidate == null) || candidate.isBetterThan(bestCandidate, payloadPathsByFingerprint)) {
                bestCandidate = candidate;
            }
        }

        return Objects.requireNonNull(bestCandidate, "Failed to choose an AFMA encode candidate");
    }

    @NotNull
    protected PlannedCandidate applyAdaptiveKeyframeDecision(@NotNull PlannedCandidate selectedCandidate,
                                                             @NotNull PlannedCandidate fullCandidate,
                                                             @NotNull AfmaPixelFrame sourceFrame,
                                                             @NotNull AfmaPixelFrame workingFrame,
                                                             boolean introSequence, int frameIndex, int framesSinceKeyframe,
                                                             @NotNull AfmaEncodeOptions options,
                                                             @NotNull Map<String, String> payloadPathsByFingerprint) throws IOException {
        if (!selectedCandidate.descriptor().isKeyframe() && ((framesSinceKeyframe + 1) >= options.getKeyframeInterval())) {
            long fullArchiveBytes = fullCandidate.estimatedArchiveBytes(payloadPathsByFingerprint);
            long candidateArchiveBytes = selectedCandidate.estimatedArchiveBytes(payloadPathsByFingerprint);
            long byteSavings = fullArchiveBytes - candidateArchiveBytes;
            long requiredSavings = this.computeRequiredComplexCandidateSavings(
                    fullArchiveBytes,
                    options.getAdaptiveContinuationMinSavingsBytes(),
                    options.getAdaptiveContinuationMinSavingsRatio()
            );
            if (byteSavings < requiredSavings) {
                selectedCandidate = fullCandidate;
            }
        }

        if (!options.isPerceptualBinIntraEnabled()) {
            return selectedCandidate;
        }
        if (selectedCandidate.isExactRelativeToSourceFrame(sourceFrame, workingFrame)) {
            return selectedCandidate;
        }

        AfmaPixelFrame reconstructedFrame = selectedCandidate.materializeReferenceFrame(sourceFrame, workingFrame);
        try {
            PerceptualDriftStats driftStats = this.measurePerceptualDrift(sourceFrame, reconstructedFrame);
            if (driftStats.averageError() <= options.getPerceptualBinIntraMaxAverageError()
                    && driftStats.maxVisibleColorDelta() <= options.getPerceptualBinIntraMaxVisibleColorDelta()
                    && driftStats.maxAlphaDelta() <= options.getPerceptualBinIntraMaxAlphaDelta()) {
                return selectedCandidate;
            }
        } finally {
            CloseableUtils.closeQuietly(reconstructedFrame);
        }

        return this.createExactFullCandidate(sourceFrame, introSequence, frameIndex, options);
    }

    @NotNull
    protected PerceptualDriftStats measurePerceptualDrift(@NotNull AfmaFramePairAnalysis pairAnalysis) {
        AfmaFramePairAnalysis.PerceptualDriftMetrics driftMetrics = pairAnalysis.perceptualDriftMetrics();
        return new PerceptualDriftStats(
                driftMetrics.averageError(),
                driftMetrics.maxVisibleColorDelta(),
                driftMetrics.maxAlphaDelta()
        );
    }

    @NotNull
    protected PerceptualDriftStats measurePerceptualDrift(@NotNull AfmaPixelFrame sourceFrame, @NotNull AfmaPixelFrame reconstructedFrame) {
        return this.measurePerceptualDrift(new AfmaFramePairAnalysis(sourceFrame, reconstructedFrame));
    }

    @NotNull
    protected AfmaPixelFrame applyNearLosslessTemporalMerge(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame, int maxChannelDelta) {
        AfmaPixelFrameHelper.ensureSameSize(previousFrame, currentFrame);
        if (maxChannelDelta <= 0) {
            return currentFrame;
        }

        int[] previousPixels = previousFrame.getPixelsUnsafe();
        int[] currentPixels = currentFrame.getPixelsUnsafe();
        int width = currentFrame.getWidth();
        int height = currentFrame.getHeight();
        int[] mergedPixels = null;
        for (int pixelIndex = 0; pixelIndex < currentPixels.length; pixelIndex++) {
            int previousColor = previousPixels[pixelIndex];
            int currentColor = currentPixels[pixelIndex];
            if (!shouldMergeNearLossless(previousColor, currentColor, maxChannelDelta)) {
                continue;
            }

            if (mergedPixels == null) {
                mergedPixels = Arrays.copyOf(currentPixels, currentPixels.length);
            }
            mergedPixels[pixelIndex] = previousColor;
        }

        if (mergedPixels == null) {
            return currentFrame;
        }
        return new AfmaPixelFrame(width, height, mergedPixels);
    }

    protected static boolean shouldMergeNearLossless(int previousColor, int currentColor, int maxChannelDelta) {
        if (previousColor == currentColor) {
            return false;
        }
        if (((previousColor ^ currentColor) & 0xFF000000) != 0) {
            return false;
        }

        return channelDifference(previousColor >> 16, currentColor >> 16) <= maxChannelDelta
                && channelDifference(previousColor >> 8, currentColor >> 8) <= maxChannelDelta
                && channelDifference(previousColor, currentColor) <= maxChannelDelta;
    }

    protected static int channelDifference(int first, int second) {
        return Math.abs((first & 0xFF) - (second & 0xFF));
    }

    protected boolean shouldAllowPerceptualContinuation(int framesSinceKeyframe, @NotNull AfmaEncodeOptions options) {
        int maxInterval = options.isAdaptiveKeyframePlacementEnabled()
                ? options.getAdaptiveMaxKeyframeInterval()
                : options.getKeyframeInterval();
        return (framesSinceKeyframe + 1) < maxInterval;
    }

    @NotNull
    protected PlannedCandidate createExactFullCandidate(@NotNull AfmaPixelFrame sourceFrame, boolean introSequence, int frameIndex,
                                                        @NotNull AfmaEncodeOptions options) throws IOException {
        return this.createFullCandidate(sourceFrame, introSequence, frameIndex, options, false, ReferenceBase.SOURCE_FRAME);
    }

    @NotNull
    protected AfmaBinIntraPayloadHelper.StoredEncodedPayloadResult encodeBinIntraPayload(@NotNull AfmaPixelFrame frame,
                                                                                         @NotNull AfmaEncodeOptions options,
                                                                                         boolean allowPerceptual) throws IOException {
        return this.encodeBinIntraPayload(frame.getWidth(), frame.getHeight(), frame.getPixelsUnsafe(), 0, frame.getWidth(), options, allowPerceptual);
    }

    @NotNull
    protected AfmaBinIntraPayloadHelper.StoredEncodedPayloadResult encodeBinIntraPayloadRegion(@NotNull AfmaPixelFrame frame,
                                                                                                int x, int y, int width, int height,
                                                                                                @NotNull AfmaEncodeOptions options,
                                                                                                boolean allowPerceptual) throws IOException {
        return this.encodeBinIntraPayload(width, height, frame.getPixelsUnsafe(), (y * frame.getWidth()) + x, frame.getWidth(), options, allowPerceptual);
    }

    @NotNull
    protected AfmaBinIntraPayloadHelper.StoredEncodedPayloadResult encodeBinIntraPayload(int width, int height,
                                                                                         @NotNull int[] pixels, int offset, int scanlineStride,
                                                                                         @NotNull AfmaEncodeOptions options,
                                                                                         boolean allowPerceptual) throws IOException {
        return AfmaBinIntraPayloadHelper.encodePayloadStoredDetailed(width, height, pixels, offset, scanlineStride,
                this.resolveBinIntraEncodePreferences(options, allowPerceptual));
    }

    @NotNull
    protected AfmaBinIntraPayloadHelper.ScoredPayloadResult scoreBinIntraPayload(@NotNull AfmaPixelFrame frame,
                                                                                 @NotNull AfmaEncodeOptions options,
                                                                                 boolean allowPerceptual) throws IOException {
        return this.scoreBinIntraPayload(frame.getWidth(), frame.getHeight(), frame.getPixelsUnsafe(), 0, frame.getWidth(), options, allowPerceptual);
    }

    @NotNull
    protected AfmaBinIntraPayloadHelper.ScoredPayloadResult scoreBinIntraPayloadRegion(@NotNull AfmaPixelFrame frame,
                                                                                        int x, int y, int width, int height,
                                                                                        @NotNull AfmaEncodeOptions options,
                                                                                        boolean allowPerceptual) throws IOException {
        return this.scoreBinIntraPayload(width, height, frame.getPixelsUnsafe(), (y * frame.getWidth()) + x, frame.getWidth(), options, allowPerceptual);
    }

    @NotNull
    protected AfmaBinIntraPayloadHelper.ScoredPayloadResult scoreBinIntraPayload(int width, int height,
                                                                                 @NotNull int[] pixels, int offset, int scanlineStride,
                                                                                 @NotNull AfmaEncodeOptions options,
                                                                                 boolean allowPerceptual) throws IOException {
        return AfmaBinIntraPayloadHelper.scorePayloadDetailed(width, height, pixels, offset, scanlineStride,
                this.resolveBinIntraEncodePreferences(options, allowPerceptual));
    }

    @NotNull
    protected AfmaBinIntraPayloadHelper.EncodePreferences resolveBinIntraEncodePreferences(@NotNull AfmaEncodeOptions options,
                                                                                           boolean allowPerceptual) {
        AfmaBinIntraPayloadHelper.EncodePreferences preferences = (allowPerceptual && options.isPerceptualBinIntraEnabled())
                ? AfmaBinIntraPayloadHelper.EncodePreferences.perceptual(
                options.getPerceptualBinIntraMaxVisibleColorDelta(),
                options.getPerceptualBinIntraMaxAlphaDelta(),
                options.getPerceptualBinIntraMaxAverageError()
        )
                : AfmaBinIntraPayloadHelper.EncodePreferences.lossless();
        return preferences;
    }

    @NotNull
    protected DeferredPayload storePayload(@NotNull byte[] payloadBytes) {
        return DeferredPayload.fromBytes(payloadBytes);
    }

    @NotNull
    protected DeferredPayload storePayload(@NotNull AfmaStoredPayload.PayloadSummary payloadSummary, @NotNull byte[] payloadBytes) {
        return DeferredPayload.fromBytes(payloadSummary, payloadBytes);
    }

    @NotNull
    protected DeferredPayload storePayload(@NotNull AfmaStoredPayload.PayloadSummary payloadSummary,
                                           @NotNull AfmaStoredPayload.Writer payloadWriter) {
        return DeferredPayload.fromWriter(payloadSummary, payloadWriter);
    }

    @NotNull
    protected PlannedCandidate createFullCandidate(@NotNull AfmaPixelFrame currentFrame, boolean introSequence, int frameIndex,
                                                   @NotNull AfmaEncodeOptions options, boolean allowPerceptual,
                                                   @NotNull ReferenceBase referenceBase) throws IOException {
        String payloadPath = this.buildPayloadPath(introSequence, frameIndex);
        AfmaBinIntraPayloadHelper.ScoredPayloadResult encodedPayload = this.scoreBinIntraPayload(currentFrame, options, allowPerceptual);
        AfmaRect referencePatchBounds = encodedPayload.lossless() ? null : new AfmaRect(0, 0, currentFrame.getWidth(), currentFrame.getHeight());
        return new PlannedCandidate(
                AfmaFrameDescriptor.full(payloadPath),
                payloadPath,
                this.storePayload(encodedPayload.payloadSummary(), encodedPayload.payloadBytes()),
                PayloadKind.BIN_INTRA,
                false,
                null,
                null,
                null,
                false,
                referenceBase,
                referencePatchBounds,
                encodedPayload.lossless() ? null : encodedPayload.reconstructedPixels(),
                DecodeCost.FULL,
                1
        );
    }

    @Nullable
    protected PlannedCandidate createDeltaCandidate(@NotNull AfmaPixelFrame currentFrame, boolean introSequence, int frameIndex,
                                                    @NotNull AfmaRect deltaBounds, @NotNull AfmaEncodeOptions options) throws IOException {
        if (deltaBounds.area() >= ((long) currentFrame.getWidth() * currentFrame.getHeight())) {
            return null;
        }

        String payloadPath = this.buildPayloadPath(introSequence, frameIndex);
        AfmaBinIntraPayloadHelper.ScoredPayloadResult encodedPayload = this.scoreBinIntraPayloadRegion(
                currentFrame,
                deltaBounds.x(),
                deltaBounds.y(),
                deltaBounds.width(),
                deltaBounds.height(),
                options,
                true
        );
        return new PlannedCandidate(
                AfmaFrameDescriptor.deltaRect(payloadPath, deltaBounds.x(), deltaBounds.y(), deltaBounds.width(), deltaBounds.height()),
                payloadPath,
                this.storePayload(encodedPayload.payloadSummary(), encodedPayload.payloadBytes()),
                PayloadKind.BIN_INTRA,
                false,
                null,
                null,
                null,
                false,
                ReferenceBase.WORKING_FRAME,
                encodedPayload.lossless() ? null : deltaBounds,
                encodedPayload.lossless() ? null : encodedPayload.reconstructedPixels(),
                DecodeCost.DELTA,
                2
        );
    }

    @Nullable
    protected PlannedCandidate createResidualDeltaCandidate(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                                            boolean introSequence, int frameIndex, @NotNull AfmaRect deltaBounds) throws IOException {
        if (deltaBounds.area() <= 0L) {
            return null;
        }

        ResidualPayloadData residualPayload = this.buildResidualPayload(previousFrame, currentFrame, deltaBounds);
        if (residualPayload == null) {
            return null;
        }

        String payloadPath = this.buildRawPayloadPath(introSequence, frameIndex, "r");
        return new PlannedCandidate(
                AfmaFrameDescriptor.residualDeltaRect(
                        payloadPath,
                        deltaBounds.x(),
                        deltaBounds.y(),
                        deltaBounds.width(),
                        deltaBounds.height(),
                        residualPayload.metadata()
                ),
                payloadPath,
                this.storePayload(residualPayload.payloadBytes()),
                PayloadKind.RAW,
                false,
                null,
                null,
                null,
                false,
                ReferenceBase.WORKING_FRAME,
                null,
                null,
                DecodeCost.RESIDUAL_DELTA_RECT,
                3 + residualPayload.complexityScore()
        );
    }

    @Nullable
    protected PlannedCandidate createSparseDeltaCandidate(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                                          boolean introSequence, int frameIndex, @NotNull AfmaRect deltaBounds) throws IOException {
        if (deltaBounds.area() <= 0L) {
            return null;
        }

        SparseResidualPayloadData sparsePayload = this.buildSparseDeltaPayload(previousFrame, currentFrame, deltaBounds);
        if (sparsePayload == null) {
            return null;
        }

        String maskPayloadPath = this.buildRawPayloadPath(introSequence, frameIndex, "m");
        String residualPayloadPath = this.buildRawPayloadPath(introSequence, frameIndex, "s");
        return new PlannedCandidate(
                AfmaFrameDescriptor.sparseDeltaRect(
                        maskPayloadPath,
                        deltaBounds.x(),
                        deltaBounds.y(),
                        deltaBounds.width(),
                        deltaBounds.height(),
                        sparsePayload.toMetadata(residualPayloadPath)
                ),
                maskPayloadPath,
                this.storePayload(sparsePayload.layoutPayload()),
                PayloadKind.RAW,
                false,
                residualPayloadPath,
                this.storePayload(sparsePayload.residualPayload()),
                PayloadKind.RAW,
                false,
                ReferenceBase.WORKING_FRAME,
                null,
                null,
                DecodeCost.SPARSE_DELTA_RECT,
                4 + sparsePayload.complexityScore()
        );
    }

    @Nullable
    protected PlannedCandidate createCopyResidualCandidate(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                                           boolean introSequence, int frameIndex,
                                                           @NotNull AfmaRectCopyDetector.Detection detection) throws IOException {
        AfmaRect patchBounds = detection.patchBounds();
        if ((patchBounds == null) || (patchBounds.area() <= 0L)) {
            return null;
        }

        ResidualPayloadData residualPayload = this.buildCopyResidualPayload(previousFrame, currentFrame, detection.copyRect(), patchBounds);
        if (residualPayload == null) {
            return null;
        }

        String payloadPath = this.buildRawPayloadPath(introSequence, frameIndex, "cr");
        return new PlannedCandidate(
                AfmaFrameDescriptor.copyRectResidualPatch(
                        detection.copyRect(),
                        payloadPath,
                        patchBounds.x(),
                        patchBounds.y(),
                        patchBounds.width(),
                        patchBounds.height(),
                        residualPayload.metadata()
                ),
                payloadPath,
                this.storePayload(residualPayload.payloadBytes()),
                PayloadKind.RAW,
                false,
                null,
                null,
                null,
                false,
                ReferenceBase.WORKING_FRAME,
                null,
                null,
                DecodeCost.COPY_RECT_RESIDUAL_PATCH,
                5 + residualPayload.complexityScore()
        );
    }

    @Nullable
    protected PlannedCandidate createCopySparseCandidate(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                                         boolean introSequence, int frameIndex,
                                                         @NotNull AfmaRectCopyDetector.Detection detection) throws IOException {
        AfmaRect patchBounds = detection.patchBounds();
        if ((patchBounds == null) || (patchBounds.area() <= 0L)) {
            return null;
        }

        SparseResidualPayloadData sparsePayload = this.buildCopySparsePayload(previousFrame, currentFrame, detection.copyRect(), patchBounds);
        if (sparsePayload == null) {
            return null;
        }

        String maskPayloadPath = this.buildRawPayloadPath(introSequence, frameIndex, "cm");
        String residualPayloadPath = this.buildRawPayloadPath(introSequence, frameIndex, "cs");
        return new PlannedCandidate(
                AfmaFrameDescriptor.copyRectSparsePatch(
                        detection.copyRect(),
                        maskPayloadPath,
                        patchBounds.x(),
                        patchBounds.y(),
                        patchBounds.width(),
                        patchBounds.height(),
                        sparsePayload.toMetadata(residualPayloadPath)
                ),
                maskPayloadPath,
                this.storePayload(sparsePayload.layoutPayload()),
                PayloadKind.RAW,
                false,
                residualPayloadPath,
                this.storePayload(sparsePayload.residualPayload()),
                PayloadKind.RAW,
                false,
                ReferenceBase.WORKING_FRAME,
                null,
                null,
                DecodeCost.COPY_RECT_SPARSE_PATCH,
                6 + sparsePayload.complexityScore()
        );
    }

    @NotNull
    protected AfmaPixelFrame buildMultiCopyReferenceFrame(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaMultiCopy multiCopy) {
        int[] copiedPixels = previousFrame.copyPixels();
        AfmaPixelFrameHelper.applyCopyRects(copiedPixels, previousFrame.getWidth(), multiCopy.getCopyRects());
        return new AfmaPixelFrame(previousFrame.getWidth(), previousFrame.getHeight(), copiedPixels);
    }

    @Nullable
    protected PlannedCandidate createMultiCopyResidualCandidate(@NotNull AfmaPixelFrame copiedReferenceFrame, @NotNull AfmaPixelFrame currentFrame,
                                                                boolean introSequence, int frameIndex,
                                                                @NotNull AfmaRectCopyDetector.MultiDetection detection) throws IOException {
        AfmaRect patchBounds = detection.patchBounds();
        if ((patchBounds == null) || (patchBounds.area() <= 0L)) {
            return null;
        }

        ResidualPayloadData residualPayload = this.buildResidualPayload(copiedReferenceFrame, currentFrame, patchBounds);
        if (residualPayload == null) {
            return null;
        }

        String payloadPath = this.buildRawPayloadPath(introSequence, frameIndex, "mr");
        int copyCount = detection.multiCopy().getCopyRectCount();
        return new PlannedCandidate(
                AfmaFrameDescriptor.multiCopyResidualPatch(
                        detection.multiCopy(),
                        payloadPath,
                        patchBounds.x(),
                        patchBounds.y(),
                        patchBounds.width(),
                        patchBounds.height(),
                        residualPayload.metadata()
                ),
                payloadPath,
                this.storePayload(residualPayload.payloadBytes()),
                PayloadKind.RAW,
                false,
                null,
                null,
                null,
                false,
                ReferenceBase.WORKING_FRAME,
                null,
                null,
                DecodeCost.MULTI_COPY_RESIDUAL_PATCH,
                4 + copyCount + residualPayload.complexityScore()
        );
    }

    @Nullable
    protected PlannedCandidate createMultiCopySparseCandidate(@NotNull AfmaPixelFrame copiedReferenceFrame, @NotNull AfmaPixelFrame currentFrame,
                                                              boolean introSequence, int frameIndex,
                                                              @NotNull AfmaRectCopyDetector.MultiDetection detection) throws IOException {
        AfmaRect patchBounds = detection.patchBounds();
        if ((patchBounds == null) || (patchBounds.area() <= 0L)) {
            return null;
        }

        SparseResidualPayloadData sparsePayload = this.buildSparseDeltaPayload(copiedReferenceFrame, currentFrame, patchBounds);
        if (sparsePayload == null) {
            return null;
        }

        String maskPayloadPath = this.buildRawPayloadPath(introSequence, frameIndex, "mm");
        String residualPayloadPath = this.buildRawPayloadPath(introSequence, frameIndex, "ms");
        int copyCount = detection.multiCopy().getCopyRectCount();
        return new PlannedCandidate(
                AfmaFrameDescriptor.multiCopySparsePatch(
                        detection.multiCopy(),
                        maskPayloadPath,
                        patchBounds.x(),
                        patchBounds.y(),
                        patchBounds.width(),
                        patchBounds.height(),
                        sparsePayload.toMetadata(residualPayloadPath)
                ),
                maskPayloadPath,
                this.storePayload(sparsePayload.layoutPayload()),
                PayloadKind.RAW,
                false,
                residualPayloadPath,
                this.storePayload(sparsePayload.residualPayload()),
                PayloadKind.RAW,
                false,
                ReferenceBase.WORKING_FRAME,
                null,
                null,
                DecodeCost.MULTI_COPY_SPARSE_PATCH,
                5 + copyCount + sparsePayload.complexityScore()
        );
    }

    @Nullable
    protected ResidualPayloadData buildResidualPayload(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                                       @NotNull AfmaRect deltaBounds) {
        int width = deltaBounds.width();
        int height = deltaBounds.height();
        if ((width <= 0) || (height <= 0)) {
            return null;
        }

        int frameWidth = currentFrame.getWidth();
        int[] previousPixels = previousFrame.getPixelsUnsafe();
        int[] currentPixels = currentFrame.getPixelsUnsafe();
        int startX = deltaBounds.x();
        int startY = deltaBounds.y();
        int pixelCount = width * height;
        int[] predictedColors = new int[pixelCount];
        int[] currentColorsDense = new int[pixelCount];
        int pixelOffset = 0;
        boolean includeAlpha = false;
        for (int localY = 0; localY < height; localY++) {
            int rowOffset = ((startY + localY) * frameWidth) + startX;
            for (int localX = 0; localX < width; localX++) {
                int sourceIndex = rowOffset + localX;
                int predictedColor = previousPixels[sourceIndex];
                int currentColor = currentPixels[sourceIndex];
                predictedColors[pixelOffset] = predictedColor;
                currentColorsDense[pixelOffset] = currentColor;
                if (((predictedColor ^ currentColor) & 0xFF000000) != 0) {
                    includeAlpha = true;
                }
                pixelOffset++;
            }
        }
        AfmaResidualPayloadHelper.EncodedResidualPayload encodedPayload = AfmaResidualPayloadHelper.encodeBestResidualPayload(
                predictedColors,
                currentColorsDense,
                pixelCount,
                includeAlpha
        );
        return new ResidualPayloadData(encodedPayload.payloadBytes(), encodedPayload.toResidualMetadata(), encodedPayload.complexityScore());
    }

    @Nullable
    protected SparseResidualPayloadData buildSparseDeltaPayload(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                                                @NotNull AfmaRect deltaBounds) throws IOException {
        int width = deltaBounds.width();
        int height = deltaBounds.height();
        long bboxArea = deltaBounds.area();
        if ((bboxArea <= 0L) || (bboxArea > Integer.MAX_VALUE)) {
            return null;
        }

        int frameWidth = currentFrame.getWidth();
        int[] previousPixels = previousFrame.getPixelsUnsafe();
        int[] currentPixels = currentFrame.getPixelsUnsafe();
        int startX = deltaBounds.x();
        int startY = deltaBounds.y();
        int changedPixelCount = 0;
        boolean includeAlpha = false;
        int[] changedIndices = new int[(int) bboxArea];
        int[] predictedColors = new int[(int) bboxArea];
        int[] changedColors = new int[(int) bboxArea];
        for (int localY = 0; localY < height; localY++) {
            int rowOffset = ((startY + localY) * frameWidth) + startX;
            for (int localX = 0; localX < width; localX++) {
                int pixelIndex = rowOffset + localX;
                int previousColor = previousPixels[pixelIndex];
                int currentColor = currentPixels[pixelIndex];
                if (previousColor == currentColor) {
                    continue;
                }

                changedIndices[changedPixelCount] = (localY * width) + localX;
                predictedColors[changedPixelCount] = previousColor;
                changedColors[changedPixelCount] = currentColor;
                changedPixelCount++;
                if (((previousColor ^ currentColor) & 0xFF000000) != 0) {
                    includeAlpha = true;
                }
            }
        }

        if (changedPixelCount < MIN_SPARSE_DELTA_CHANGED_PIXELS) {
            return null;
        }
        if (((double) changedPixelCount / (double) bboxArea) > MAX_SPARSE_DELTA_CHANGED_DENSITY) {
            return null;
        }
        return this.buildSparseResidualPayload(width, height, changedIndices, predictedColors, changedColors, changedPixelCount, includeAlpha);
    }

    @Nullable
    protected ResidualPayloadData buildCopyResidualPayload(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                                           @NotNull AfmaCopyRect copyRect, @NotNull AfmaRect patchBounds) {
        int width = patchBounds.width();
        int height = patchBounds.height();
        if ((width <= 0) || (height <= 0)) {
            return null;
        }

        int frameWidth = currentFrame.getWidth();
        int[] previousPixels = previousFrame.getPixelsUnsafe();
        int[] currentPixels = currentFrame.getPixelsUnsafe();
        int dstLeft = copyRect.getDstX();
        int dstTop = copyRect.getDstY();
        int dstRight = dstLeft + copyRect.getWidth();
        int dstBottom = dstTop + copyRect.getHeight();
        int srcLeft = copyRect.getSrcX();
        int srcTop = copyRect.getSrcY();
        int startX = patchBounds.x();
        int startY = patchBounds.y();
        int pixelCount = width * height;
        int[] predictedColors = new int[pixelCount];
        int[] currentColorsDense = new int[pixelCount];
        int pixelOffset = 0;
        boolean includeAlpha = false;
        for (int localY = 0; localY < height; localY++) {
            int y = startY + localY;
            int rowOffset = y * frameWidth;
            boolean copiedRow = (y >= dstTop) && (y < dstBottom);
            int copiedSourceRowOffset = copiedRow ? ((srcTop + (y - dstTop)) * frameWidth) : 0;
            for (int localX = 0; localX < width; localX++) {
                int x = startX + localX;
                int pixelIndex = rowOffset + x;
                int predictedColor = previousPixels[pixelIndex];
                if (copiedRow && (x >= dstLeft) && (x < dstRight)) {
                    predictedColor = previousPixels[copiedSourceRowOffset + srcLeft + (x - dstLeft)];
                }
                int currentColor = currentPixels[pixelIndex];
                predictedColors[pixelOffset] = predictedColor;
                currentColorsDense[pixelOffset] = currentColor;
                if (((predictedColor ^ currentColor) & 0xFF000000) != 0) {
                    includeAlpha = true;
                }
                pixelOffset++;
            }
        }
        AfmaResidualPayloadHelper.EncodedResidualPayload encodedPayload = AfmaResidualPayloadHelper.encodeBestResidualPayload(
                predictedColors,
                currentColorsDense,
                pixelCount,
                includeAlpha
        );
        return new ResidualPayloadData(encodedPayload.payloadBytes(), encodedPayload.toResidualMetadata(), encodedPayload.complexityScore());
    }

    @Nullable
    protected SparseResidualPayloadData buildCopySparsePayload(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                                               @NotNull AfmaCopyRect copyRect, @NotNull AfmaRect patchBounds) throws IOException {
        int width = patchBounds.width();
        int height = patchBounds.height();
        long bboxArea = patchBounds.area();
        if ((bboxArea <= 0L) || (bboxArea > Integer.MAX_VALUE)) {
            return null;
        }

        int frameWidth = currentFrame.getWidth();
        int[] previousPixels = previousFrame.getPixelsUnsafe();
        int[] currentPixels = currentFrame.getPixelsUnsafe();
        int dstLeft = copyRect.getDstX();
        int dstTop = copyRect.getDstY();
        int dstRight = dstLeft + copyRect.getWidth();
        int dstBottom = dstTop + copyRect.getHeight();
        int srcLeft = copyRect.getSrcX();
        int srcTop = copyRect.getSrcY();
        int startX = patchBounds.x();
        int startY = patchBounds.y();
        int changedPixelCount = 0;
        boolean includeAlpha = false;
        int[] changedIndices = new int[(int) bboxArea];
        int[] predictedColors = new int[(int) bboxArea];
        int[] changedColors = new int[(int) bboxArea];
        for (int localY = 0; localY < height; localY++) {
            int y = startY + localY;
            int rowOffset = y * frameWidth;
            boolean copiedRow = (y >= dstTop) && (y < dstBottom);
            int copiedSourceRowOffset = copiedRow ? ((srcTop + (y - dstTop)) * frameWidth) : 0;
            for (int localX = 0; localX < width; localX++) {
                int x = startX + localX;
                int pixelIndex = rowOffset + x;
                int predictedColor = previousPixels[pixelIndex];
                if (copiedRow && (x >= dstLeft) && (x < dstRight)) {
                    predictedColor = previousPixels[copiedSourceRowOffset + srcLeft + (x - dstLeft)];
                }
                int currentColor = currentPixels[pixelIndex];
                if (predictedColor == currentColor) {
                    continue;
                }

                changedIndices[changedPixelCount] = (localY * width) + localX;
                predictedColors[changedPixelCount] = predictedColor;
                changedColors[changedPixelCount] = currentColor;
                changedPixelCount++;
                if (((predictedColor ^ currentColor) & 0xFF000000) != 0) {
                    includeAlpha = true;
                }
            }
        }

        if (changedPixelCount < MIN_SPARSE_DELTA_CHANGED_PIXELS) {
            return null;
        }
        if (((double) changedPixelCount / (double) bboxArea) > MAX_SPARSE_DELTA_CHANGED_DENSITY) {
            return null;
        }
        return this.buildSparseResidualPayload(width, height, changedIndices, predictedColors, changedColors, changedPixelCount, includeAlpha);
    }

    @Nullable
    protected SparseResidualPayloadData buildSparseResidualPayload(int width, int height,
                                                                  @NotNull int[] changedIndices,
                                                                  @NotNull int[] predictedColors,
                                                                  @NotNull int[] currentColors,
                                                                  int changedPixelCount,
                                                                  boolean includeAlpha) throws IOException {
        if ((changedPixelCount <= 0) || (width <= 0) || (height <= 0)) {
            return null;
        }

        int[] normalizedChangedIndices = Arrays.copyOf(changedIndices, changedPixelCount);
        int[] normalizedPredictedColors = Arrays.copyOf(predictedColors, changedPixelCount);
        int[] normalizedCurrentColors = Arrays.copyOf(currentColors, changedPixelCount);
        AfmaResidualPayloadHelper.EncodedResidualPayload encodedResidual = AfmaResidualPayloadHelper.encodeBestResidualPayload(
                normalizedPredictedColors,
                normalizedCurrentColors,
                changedPixelCount,
                includeAlpha
        );
        SparseLayoutCandidate bestLayout = this.chooseBestSparseLayout(width, height, normalizedChangedIndices, changedPixelCount);
        return new SparseResidualPayloadData(bestLayout.layoutPayload(), encodedResidual.payloadBytes(), changedPixelCount,
                bestLayout.layoutCodec(), encodedResidual.channels(), encodedResidual.codec(),
                encodedResidual.alphaMode(), encodedResidual.alphaChangedPixelCount(),
                bestLayout.complexityScore() + encodedResidual.complexityScore());
    }

    @NotNull
    protected SparseLayoutCandidate chooseBestSparseLayout(int width, int height, @NotNull int[] changedIndices, int changedPixelCount) throws IOException {
        SparseLayoutCandidate bestCandidate = null;
        for (SparseLayoutPlan layoutPlan : this.selectSparseLayoutPlans(width, height, changedIndices, changedPixelCount)) {
            SparseLayoutCandidate candidate = this.buildSparseLayoutCandidate(layoutPlan.layoutCodec(), width, height, changedIndices, changedPixelCount);
            if ((bestCandidate == null) || candidate.isBetterThan(bestCandidate)) {
                bestCandidate = candidate;
            }
        }
        return Objects.requireNonNull(bestCandidate, "Failed to choose an AFMA sparse layout");
    }

    @NotNull
    protected ArrayList<SparseLayoutPlan> selectSparseLayoutPlans(int width, int height, @NotNull int[] changedIndices, int changedPixelCount) {
        ArrayList<SparseLayoutPlan> layoutPlans = new ArrayList<>(4);
        SparseLayoutEstimate layoutEstimate = this.estimateSparseLayoutBytes(width, height, changedIndices, changedPixelCount);
        layoutPlans.add(new SparseLayoutPlan(AfmaSparseLayoutCodec.BITMASK, layoutEstimate.bitmaskBytes()));
        layoutPlans.add(new SparseLayoutPlan(AfmaSparseLayoutCodec.ROW_SPANS, layoutEstimate.rowSpanBytes()));
        layoutPlans.add(new SparseLayoutPlan(AfmaSparseLayoutCodec.TILE_MASK, layoutEstimate.tileMaskBytes()));
        layoutPlans.add(new SparseLayoutPlan(AfmaSparseLayoutCodec.COORD_LIST, layoutEstimate.coordListBytes()));

        // Small sparse regions can still swing a lot after ZIP, so keep those exhaustive and only
        // short-list materially larger layouts once the region is big enough to matter.
        if ((changedPixelCount <= FULL_SPARSE_LAYOUT_EVALUATION_MAX_CHANGED_PIXELS) || (((long) width * height) <= FULL_SPARSE_LAYOUT_EVALUATION_MAX_AREA)) {
            return layoutPlans;
        }

        layoutPlans.sort((first, second) -> {
            int sizeCompare = Long.compare(first.estimatedRawBytes(), second.estimatedRawBytes());
            if (sizeCompare != 0) {
                return sizeCompare;
            }
            int complexityCompare = Integer.compare(first.layoutCodec().getComplexityScore(), second.layoutCodec().getComplexityScore());
            if (complexityCompare != 0) {
                return complexityCompare;
            }
            return Integer.compare(first.layoutCodec().getId(), second.layoutCodec().getId());
        });

        long bestEstimatedBytes = layoutPlans.get(0).estimatedRawBytes();
        long maxEstimatedBytes = bestEstimatedBytes
                + Math.max(SPARSE_LAYOUT_SHORTLIST_MARGIN_BYTES, Math.round(bestEstimatedBytes * SPARSE_LAYOUT_SHORTLIST_MARGIN_RATIO));
        ArrayList<SparseLayoutPlan> shortlistedPlans = new ArrayList<>(4);
        for (SparseLayoutPlan layoutPlan : layoutPlans) {
            if ((shortlistedPlans.size() < MIN_SHORTLISTED_SPARSE_LAYOUTS) || (layoutPlan.estimatedRawBytes() <= maxEstimatedBytes)) {
                shortlistedPlans.add(layoutPlan);
            }
        }
        return shortlistedPlans;
    }

    @NotNull
    protected SparseLayoutEstimate estimateSparseLayoutBytes(int width, int height, @NotNull int[] changedIndices, int changedPixelCount) {
        long bitmaskBytes = AfmaResidualPayloadHelper.expectedSparseMaskBytes(width, height);
        long coordListBytes = 0L;
        int previousIndex = -1;

        int tileSize = AfmaSparsePayloadHelper.TILE_MASK_TILE_SIZE;
        int tileCountX = (width + tileSize - 1) / tileSize;
        int tileCountY = (height + tileSize - 1) / tileSize;
        int tileCount = tileCountX * tileCountY;
        long tileMaskBytes = AfmaResidualPayloadHelper.expectedSparseBitsetBytes(tileCount);
        boolean[] activeTiles = new boolean[Math.max(0, tileCount)];

        for (int i = 0; i < changedPixelCount; i++) {
            int changedIndex = changedIndices[i];
            coordListBytes += estimateVarIntBytes(changedIndex - previousIndex - 1);
            previousIndex = changedIndex;

            int x = changedIndex % width;
            int y = changedIndex / width;
            int tileX = x / tileSize;
            int tileY = y / tileSize;
            int tileIndex = (tileY * tileCountX) + tileX;
            if (!activeTiles[tileIndex]) {
                activeTiles[tileIndex] = true;
                int tileWidth = Math.min(tileSize, width - (tileX * tileSize));
                int tileHeight = Math.min(tileSize, height - (tileY * tileSize));
                tileMaskBytes += AfmaResidualPayloadHelper.expectedSparseMaskBytes(tileWidth, tileHeight);
            }
        }

        return new SparseLayoutEstimate(bitmaskBytes, this.estimateRowSpanLayoutBytes(width, changedIndices, changedPixelCount),
                tileMaskBytes, coordListBytes);
    }

    protected long estimateRowSpanLayoutBytes(int width, @NotNull int[] changedIndices, int changedPixelCount) {
        if (changedPixelCount <= 0) {
            return 0L;
        }

        long totalBytes = 0L;
        int changedRowCount = 0;
        int cursor = 0;
        int previousRow = -1;
        while (cursor < changedPixelCount) {
            changedRowCount++;
            int row = changedIndices[cursor] / width;
            totalBytes += estimateVarIntBytes(row - previousRow - 1);
            previousRow = row;

            int previousEndX = 0;
            int spanCount = 0;
            long rowSpanBytes = 0L;
            while ((cursor < changedPixelCount) && ((changedIndices[cursor] / width) == row)) {
                int startX = changedIndices[cursor] % width;
                int endX = startX + 1;
                cursor++;
                while ((cursor < changedPixelCount)
                        && ((changedIndices[cursor] / width) == row)
                        && ((changedIndices[cursor] % width) == endX)) {
                    endX++;
                    cursor++;
                }
                spanCount++;
                rowSpanBytes += estimateVarIntBytes(startX - previousEndX);
                rowSpanBytes += estimateVarIntBytes(endX - startX - 1);
                previousEndX = endX;
            }
            totalBytes += estimateVarIntBytes(spanCount) + rowSpanBytes;
        }
        return estimateVarIntBytes(changedRowCount) + totalBytes;
    }

    @NotNull
    protected SparseLayoutCandidate buildSparseLayoutCandidate(@NotNull AfmaSparseLayoutCodec layoutCodec, int width, int height,
                                                               @NotNull int[] changedIndices, int changedPixelCount) throws IOException {
        return switch (layoutCodec) {
            case BITMASK -> new SparseLayoutCandidate(
                    layoutCodec,
                    AfmaSparsePayloadHelper.buildBitmaskLayout(width, height, changedIndices, changedPixelCount),
                    layoutCodec.getComplexityScore()
            );
            case ROW_SPANS -> new SparseLayoutCandidate(
                    layoutCodec,
                    AfmaSparsePayloadHelper.buildRowSpanLayout(width, changedIndices, changedPixelCount),
                    layoutCodec.getComplexityScore()
            );
            case TILE_MASK -> new SparseLayoutCandidate(
                    layoutCodec,
                    AfmaSparsePayloadHelper.buildTileMaskLayout(width, height, changedIndices, changedPixelCount),
                    layoutCodec.getComplexityScore()
            );
            case COORD_LIST -> new SparseLayoutCandidate(
                    layoutCodec,
                    AfmaSparsePayloadHelper.buildCoordListLayout(changedIndices, changedPixelCount),
                    layoutCodec.getComplexityScore()
            );
        };
    }

    protected boolean hasAlphaResidual(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame, @NotNull AfmaRect bounds) {
        int frameWidth = currentFrame.getWidth();
        int[] previousPixels = previousFrame.getPixelsUnsafe();
        int[] currentPixels = currentFrame.getPixelsUnsafe();
        int startX = bounds.x();
        int startY = bounds.y();
        for (int localY = 0; localY < bounds.height(); localY++) {
            int rowOffset = ((startY + localY) * frameWidth) + startX;
            for (int localX = 0; localX < bounds.width(); localX++) {
                int pixelIndex = rowOffset + localX;
                int previousColor = previousPixels[pixelIndex];
                int currentColor = currentPixels[pixelIndex];
                if (((previousColor ^ currentColor) & 0xFF000000) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean hasCopyAlphaResidual(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                           @NotNull AfmaCopyRect copyRect, @NotNull AfmaRect bounds) {
        int frameWidth = currentFrame.getWidth();
        int[] previousPixels = previousFrame.getPixelsUnsafe();
        int[] currentPixels = currentFrame.getPixelsUnsafe();
        int dstLeft = copyRect.getDstX();
        int dstTop = copyRect.getDstY();
        int dstRight = dstLeft + copyRect.getWidth();
        int dstBottom = dstTop + copyRect.getHeight();
        int srcLeft = copyRect.getSrcX();
        int srcTop = copyRect.getSrcY();
        int startX = bounds.x();
        int startY = bounds.y();
        for (int localY = 0; localY < bounds.height(); localY++) {
            int y = startY + localY;
            int rowOffset = y * frameWidth;
            boolean copiedRow = (y >= dstTop) && (y < dstBottom);
            int copiedSourceRowOffset = copiedRow ? ((srcTop + (y - dstTop)) * frameWidth) : 0;
            for (int localX = 0; localX < bounds.width(); localX++) {
                int x = startX + localX;
                int pixelIndex = rowOffset + x;
                int predictedColor = previousPixels[pixelIndex];
                if (copiedRow && (x >= dstLeft) && (x < dstRight)) {
                    predictedColor = previousPixels[copiedSourceRowOffset + srcLeft + (x - dstLeft)];
                }
                int currentColor = currentPixels[pixelIndex];
                if (((predictedColor ^ currentColor) & 0xFF000000) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    protected PlannedCandidate createCopyCandidate(@NotNull AfmaPixelFrame currentFrame, boolean introSequence, int frameIndex,
                                                   @NotNull AfmaRectCopyDetector.Detection detection,
                                                   @NotNull AfmaEncodeOptions options) throws IOException {
        AfmaCopyRect copyRect = detection.copyRect();
        AfmaRect patchBounds = detection.patchBounds();
        if ((patchBounds != null) && (patchBounds.area() >= ((long)currentFrame.getWidth() * currentFrame.getHeight()))) {
            return null;
        }

        String payloadPath = (patchBounds != null) ? this.buildPayloadPath(introSequence, frameIndex) : null;
        AfmaPatchRegion patchRegion = (patchBounds != null) ? patchBounds.toPatchRegion(payloadPath) : null;
        DeferredPayload payload = null;
        int[] referencePatchPixels = null;

        if (patchBounds != null) {
            AfmaBinIntraPayloadHelper.ScoredPayloadResult encodedPayload = this.scoreBinIntraPayloadRegion(
                    currentFrame,
                    patchBounds.x(),
                    patchBounds.y(),
                    patchBounds.width(),
                    patchBounds.height(),
                    options,
                    true
            );
            payload = this.storePayload(encodedPayload.payloadSummary(), encodedPayload.payloadBytes());
            referencePatchPixels = encodedPayload.lossless() ? null : encodedPayload.reconstructedPixels();
        }

        return new PlannedCandidate(
                AfmaFrameDescriptor.copyRectPatch(copyRect, patchRegion),
                null,
                null,
                null,
                false,
                payloadPath,
                payload,
                PayloadKind.BIN_INTRA,
                false,
                ReferenceBase.WORKING_FRAME,
                referencePatchPixels != null ? patchBounds : null,
                referencePatchPixels,
                DecodeCost.COPY_RECT_PATCH,
                3
        );
    }

    @Nullable
    protected PlannedCandidate createMultiCopyCandidate(@NotNull AfmaPixelFrame currentFrame, boolean introSequence, int frameIndex,
                                                        @NotNull AfmaRectCopyDetector.MultiDetection detection,
                                                        @NotNull AfmaEncodeOptions options) throws IOException {
        AfmaRect patchBounds = detection.patchBounds();
        if ((patchBounds != null) && (patchBounds.area() >= ((long) currentFrame.getWidth() * currentFrame.getHeight()))) {
            return null;
        }

        String payloadPath = (patchBounds != null) ? this.buildPayloadPath(introSequence, frameIndex) : null;
        AfmaPatchRegion patchRegion = (patchBounds != null) ? patchBounds.toPatchRegion(payloadPath) : null;
        DeferredPayload payload = null;
        int[] referencePatchPixels = null;

        if (patchBounds != null) {
            AfmaBinIntraPayloadHelper.ScoredPayloadResult encodedPayload = this.scoreBinIntraPayloadRegion(
                    currentFrame,
                    patchBounds.x(),
                    patchBounds.y(),
                    patchBounds.width(),
                    patchBounds.height(),
                    options,
                    true
            );
            payload = this.storePayload(encodedPayload.payloadSummary(), encodedPayload.payloadBytes());
            referencePatchPixels = encodedPayload.lossless() ? null : encodedPayload.reconstructedPixels();
        }

        int copyCount = detection.multiCopy().getCopyRectCount();
        return new PlannedCandidate(
                AfmaFrameDescriptor.multiCopyPatch(detection.multiCopy(), patchRegion),
                null,
                null,
                null,
                false,
                payloadPath,
                payload,
                PayloadKind.BIN_INTRA,
                false,
                ReferenceBase.WORKING_FRAME,
                referencePatchPixels != null ? patchBounds : null,
                referencePatchPixels,
                DecodeCost.MULTI_COPY_PATCH,
                2 + copyCount
        );
    }

    @Nullable
    protected PlannedCandidate createBlockInterCandidate(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                                         boolean introSequence, int frameIndex, @NotNull AfmaRect deltaBounds,
                                                         @NotNull AfmaRectCopyDetector copyDetector,
                                                         @Nullable AfmaRectCopyDetector.Detection copyDetection,
                                                         @Nullable AfmaRectCopyDetector.MultiDetection multiDetection,
                                                         @Nullable PlannedCandidate bestBaselineCandidate) throws IOException {
        AfmaPixelFrame previousFrame = pairAnalysis.previousFrame();
        AfmaPixelFrame currentFrame = pairAnalysis.nextFrame();
        AfmaRect regionBounds = this.alignBoundsToTileGrid(deltaBounds, BLOCK_INTER_TILE_SIZE, currentFrame.getWidth(), currentFrame.getHeight());
        int tileCountX = AfmaBlockInterPayloadHelper.tileCount(regionBounds.width(), BLOCK_INTER_TILE_SIZE);
        int tileCountY = AfmaBlockInterPayloadHelper.tileCount(regionBounds.height(), BLOCK_INTER_TILE_SIZE);
        if ((tileCountX <= 0) || (tileCountY <= 0)) {
            return null;
        }

        BlockInterRegionAnalysis regionAnalysis = this.analyzeBlockInterRegion(previousFrame, currentFrame, regionBounds, tileCountX, tileCountY);
        if (!this.shouldAttemptBlockInter(regionBounds, regionAnalysis, bestBaselineCandidate)) {
            return null;
        }
        return this.createBlockInterCandidate(pairAnalysis, introSequence, frameIndex, regionBounds, regionAnalysis,
                copyDetector, copyDetection, multiDetection);
    }

    @Nullable
    protected PlannedCandidate createBlockInterCandidate(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                                         boolean introSequence, int frameIndex,
                                                         @NotNull AfmaRect regionBounds, @NotNull BlockInterRegionAnalysis regionAnalysis,
                                                         @NotNull AfmaRectCopyDetector copyDetector,
                                                         @Nullable AfmaRectCopyDetector.Detection copyDetection,
                                                         @Nullable AfmaRectCopyDetector.MultiDetection multiDetection) throws IOException {
        AfmaPixelFrame previousFrame = pairAnalysis.previousFrame();
        AfmaPixelFrame currentFrame = pairAnalysis.nextFrame();
        int tileCountX = AfmaBlockInterPayloadHelper.tileCount(regionBounds.width(), BLOCK_INTER_TILE_SIZE);
        int tileCountY = AfmaBlockInterPayloadHelper.tileCount(regionBounds.height(), BLOCK_INTER_TILE_SIZE);
        if ((tileCountX <= 0) || (tileCountY <= 0)) {
            return null;
        }

        int totalTileCount = tileCountX * tileCountY;
        List<AfmaRectCopyDetector.MotionVector> motionVectors = this.collectBlockInterMotionVectors(
                pairAnalysis,
                copyDetection,
                multiDetection,
                copyDetector,
                regionAnalysis
        );
        BlockInterTileCandidate[] tileCandidates = new BlockInterTileCandidate[totalTileCount];
        if (this.shouldParallelizeBlockInterTiles(totalTileCount, regionBounds)) {
            IntStream.range(0, totalTileCount).parallel().forEach(tileIndex ->
                    tileCandidates[tileIndex] = this.buildBlockInterTileCandidate(previousFrame, currentFrame, motionVectors,
                            regionBounds, tileCountX, tileCountY, regionAnalysis, tileIndex));
        } else {
            for (int tileIndex = 0; tileIndex < totalTileCount; tileIndex++) {
                tileCandidates[tileIndex] = this.buildBlockInterTileCandidate(previousFrame, currentFrame, motionVectors,
                        regionBounds, tileCountX, tileCountY, regionAnalysis, tileIndex);
            }
        }

        String payloadPath = this.buildRawPayloadPath(introSequence, frameIndex, "bi");
        // Score block_inter from lightweight tile summaries first, then only materialize the winning tile operations
        // when the final payload needs to be scored or stored.
        AfmaStoredPayload.Writer payloadWriter = out -> AfmaBlockInterPayloadHelper.writePayload(
                out,
                BLOCK_INTER_TILE_SIZE,
                regionBounds.width(),
                regionBounds.height(),
                this.materializeBlockInterTileOperations(tileCandidates, previousFrame, currentFrame)
        );
        DeferredPayload payload = this.storePayload(AfmaStoredPayload.summarize(payloadWriter), payloadWriter);
        return new PlannedCandidate(
                AfmaFrameDescriptor.blockInter(payloadPath, regionBounds.x(), regionBounds.y(), regionBounds.width(), regionBounds.height(), new AfmaBlockInter(BLOCK_INTER_TILE_SIZE)),
                payloadPath,
                payload,
                PayloadKind.RAW,
                false,
                null,
                null,
                null,
                false,
                ReferenceBase.WORKING_FRAME,
                null,
                null,
                DecodeCost.BLOCK_INTER,
                7
        );
    }

    @NotNull
    protected List<AfmaBlockInterPayloadHelper.TileOperation> materializeBlockInterTileOperations(@NotNull BlockInterTileCandidate[] tileCandidates,
                                                                                                  @NotNull AfmaPixelFrame previousFrame,
                                                                                                  @NotNull AfmaPixelFrame currentFrame) {
        AfmaBlockInterPayloadHelper.TileOperation[] tileOperations = new AfmaBlockInterPayloadHelper.TileOperation[tileCandidates.length];
        for (int tileIndex = 0; tileIndex < tileCandidates.length; tileIndex++) {
            tileOperations[tileIndex] = this.materializeBlockInterTileOperation(
                    Objects.requireNonNull(tileCandidates[tileIndex], "AFMA block_inter tile candidate was NULL"),
                    previousFrame,
                    currentFrame
            );
        }
        return Arrays.asList(tileOperations);
    }

    @NotNull
    protected AfmaBlockInterPayloadHelper.TileOperation materializeBlockInterTileOperation(@NotNull BlockInterTileCandidate tileCandidate,
                                                                                           @NotNull AfmaPixelFrame previousFrame,
                                                                                           @NotNull AfmaPixelFrame currentFrame) {
        BlockInterTileEncoding encoding = tileCandidate.encoding();
        if (encoding instanceof SkipBlockInterTileEncoding) {
            return new AfmaBlockInterPayloadHelper.TileOperation(AfmaBlockInterPayloadHelper.TileMode.SKIP, 0, 0, 0, 0, null, null, null);
        }
        if (encoding instanceof CopyBlockInterTileEncoding copyEncoding) {
            return new AfmaBlockInterPayloadHelper.TileOperation(AfmaBlockInterPayloadHelper.TileMode.COPY,
                    copyEncoding.dx(), copyEncoding.dy(), 0, 0, null, null, null);
        }
        if (encoding instanceof DenseBlockInterTileEncoding denseEncoding) {
            ResidualPayloadData denseResidual = Objects.requireNonNull(
                    this.buildMotionResidualPayload(
                            previousFrame,
                            currentFrame,
                            denseEncoding.dstX(),
                            denseEncoding.dstY(),
                            denseEncoding.dstX() + denseEncoding.dx(),
                            denseEncoding.dstY() + denseEncoding.dy(),
                            denseEncoding.width(),
                            denseEncoding.height(),
                            denseEncoding.channels() == AfmaResidualPayloadHelper.RGBA_CHANNELS
                    ),
                    "AFMA block_inter dense tile payload was NULL during materialization"
            );
            return new AfmaBlockInterPayloadHelper.TileOperation(
                    AfmaBlockInterPayloadHelper.TileMode.COPY_DENSE,
                    denseEncoding.dx(),
                    denseEncoding.dy(),
                    denseResidual.metadata().getChannels(),
                    0,
                    denseResidual.payloadBytes(),
                    null,
                    null
            );
        }
        if (encoding instanceof SparseBlockInterTileEncoding sparseEncoding) {
            SparseResidualPayloadData sparsePayload = sparseEncoding.sparsePayload();
            return new AfmaBlockInterPayloadHelper.TileOperation(
                    AfmaBlockInterPayloadHelper.TileMode.COPY_SPARSE,
                    sparseEncoding.dx(),
                    sparseEncoding.dy(),
                    sparsePayload.channels(),
                    sparsePayload.changedPixelCount(),
                    sparsePayload.layoutPayload(),
                    sparsePayload.residualPayload(),
                    sparsePayload.toMetadata(null)
            );
        }
        if (encoding instanceof RawBlockInterTileEncoding rawEncoding) {
            return new AfmaBlockInterPayloadHelper.TileOperation(
                    AfmaBlockInterPayloadHelper.TileMode.RAW,
                    0,
                    0,
                    rawEncoding.channels(),
                    0,
                    this.buildRawTileBytes(currentFrame, rawEncoding.dstX(), rawEncoding.dstY(), rawEncoding.width(), rawEncoding.height(), rawEncoding.channels()),
                    null,
                    null
            );
        }
        throw new IllegalStateException("Unknown AFMA block_inter tile encoding: " + encoding.getClass().getName());
    }

    @NotNull
    protected BlockInterTileCandidate buildBlockInterTileCandidate(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                                                   @NotNull List<AfmaRectCopyDetector.MotionVector> motionVectors,
                                                                   @NotNull AfmaRect regionBounds, int tileCountX, int tileCountY,
                                                                   @NotNull BlockInterRegionAnalysis regionAnalysis,
                                                                   int tileIndex) {
        if (!regionAnalysis.isTileChanged(tileIndex)) {
            return new BlockInterTileCandidate(new SkipBlockInterTileEncoding(),
                    this.estimateBlockInterTileBytes(AfmaBlockInterPayloadHelper.TileMode.SKIP, 0, 0), 0);
        }

        int tileY = tileIndex / tileCountX;
        int tileX = tileIndex % tileCountX;
        int localY = tileY * BLOCK_INTER_TILE_SIZE;
        int localX = tileX * BLOCK_INTER_TILE_SIZE;
        int dstY = regionBounds.y() + localY;
        int dstX = regionBounds.x() + localX;
        int tileHeight = AfmaBlockInterPayloadHelper.tileDimension(tileY, tileCountY, BLOCK_INTER_TILE_SIZE, regionBounds.height());
        int tileWidth = AfmaBlockInterPayloadHelper.tileDimension(tileX, tileCountX, BLOCK_INTER_TILE_SIZE, regionBounds.width());
        return this.chooseBestBlockInterTile(previousFrame, currentFrame, dstX, dstY, tileWidth, tileHeight, motionVectors);
    }

    @NotNull
    protected BlockInterTileCandidate chooseBestBlockInterTile(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                                               int dstX, int dstY, int width, int height,
                                                               @NotNull List<AfmaRectCopyDetector.MotionVector> motionVectors) {
        int rawChannels = this.determineRawTileChannels(currentFrame, dstX, dstY, width, height);
        BlockInterTileCandidate bestCandidate = new BlockInterTileCandidate(
                new RawBlockInterTileEncoding(dstX, dstY, width, height, rawChannels),
                this.estimateBlockInterTileBytes(AfmaBlockInterPayloadHelper.TileMode.RAW,
                        AfmaBlockInterPayloadHelper.expectedRawTileBytes(width, height, rawChannels), 0),
                0
        );
        List<TileMotionSAD> scoredVectors = new ArrayList<>(motionVectors.size());
        for (AfmaRectCopyDetector.MotionVector motionVector : motionVectors) {
            scoredVectors.add(new TileMotionSAD(
                    motionVector,
                    this.computeTileSAD(previousFrame, currentFrame, dstX, dstY, dstX + motionVector.dx(), dstY + motionVector.dy(), width, height)
            ));
        }
        scoredVectors.sort((first, second) -> Long.compare(first.sad(), second.sad()));

        int maxEvals = Math.min(3, scoredVectors.size());
        List<MotionSearchSeed> refinementSeeds = new ArrayList<>(BLOCK_INTER_LOCAL_REFINEMENT_SEEDS);
        Set<Long> testedVectors = new HashSet<>(motionVectors.size() * 2);
        for (int vectorIndex = 0; vectorIndex < maxEvals; vectorIndex++) {
            AfmaRectCopyDetector.MotionVector motionVector = scoredVectors.get(vectorIndex).vector();
            testedVectors.add(packMotionVector(motionVector.dx(), motionVector.dy()));
            BlockInterTileCandidate motionCandidate = this.evaluateBlockInterMotionCandidate(previousFrame, currentFrame, dstX, dstY, width, height,
                    motionVector.dx(), motionVector.dy(), bestCandidate);
            if (motionCandidate == null) {
                continue;
            }
            bestCandidate = this.selectBetterBlockInterCandidate(bestCandidate, motionCandidate);
            if (this.isOptimalBlockInterTileCandidate(bestCandidate)) {
                return bestCandidate;
            }
            this.addMotionSearchSeed(refinementSeeds, motionVector, motionCandidate);
        }

        if (!refinementSeeds.isEmpty()) {
            bestCandidate = this.refineBlockInterTileMotion(previousFrame, currentFrame, dstX, dstY, width, height,
                    bestCandidate, refinementSeeds, testedVectors);
        }

        return bestCandidate;
    }

    protected long computeTileSAD(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                  int dstX, int dstY, int srcX, int srcY, int width, int height) {
        if (!this.isMotionTileInBounds(previousFrame, srcX, srcY, width, height)) {
            return Long.MAX_VALUE;
        }

        int frameWidth = currentFrame.getWidth();
        int[] previousPixels = previousFrame.getPixelsUnsafe();
        int[] currentPixels = currentFrame.getPixelsUnsafe();
        long sad = 0L;
        for (int localY = 0; localY < height; localY++) {
            int previousRowOffset = ((srcY + localY) * frameWidth) + srcX;
            int currentRowOffset = ((dstY + localY) * frameWidth) + dstX;
            for (int localX = 0; localX < width; localX++) {
                int previousColor = previousPixels[previousRowOffset + localX];
                int currentColor = currentPixels[currentRowOffset + localX];
                sad += Math.abs(((previousColor >> 16) & 0xFF) - ((currentColor >> 16) & 0xFF));
                sad += Math.abs(((previousColor >> 8) & 0xFF) - ((currentColor >> 8) & 0xFF));
                sad += Math.abs((previousColor & 0xFF) - (currentColor & 0xFF));
                sad += Math.abs(((previousColor >>> 24) & 0xFF) - ((currentColor >>> 24) & 0xFF));
            }
        }
        return sad;
    }

    @Nullable
    protected BlockInterTileCandidate evaluateBlockInterMotionCandidate(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                                                        int dstX, int dstY, int width, int height, int dx, int dy,
                                                                        @NotNull BlockInterTileCandidate currentBestCandidate) {
        int srcX = dstX + dx;
        int srcY = dstY + dy;
        if (!this.isMotionTileInBounds(previousFrame, srcX, srcY, width, height)) {
            return null;
        }

        MotionTileStats tileStats = this.scanMotionTile(previousFrame, currentFrame, dstX, dstY, srcX, srcY, width, height, currentBestCandidate);
        if (tileStats == null) {
            return null;
        }
        if (tileStats.changedPixelCount() <= 0) {
            return new BlockInterTileCandidate(
                    new CopyBlockInterTileEncoding(dx, dy),
                    this.estimateBlockInterTileBytes(AfmaBlockInterPayloadHelper.TileMode.COPY, 0, 0),
                    0
            );
        }

        BlockInterTileCandidate bestCandidate = null;
        int denseChannels = AfmaResidualPayloadHelper.channelCount(tileStats.includeAlpha());
        int densePayloadBytes = AfmaBlockInterPayloadHelper.expectedDenseResidualBytes(width, height, denseChannels);
        if (densePayloadBytes > 0) {
            BlockInterTileCandidate denseCandidate = new BlockInterTileCandidate(
                    new DenseBlockInterTileEncoding(dx, dy, dstX, dstY, width, height, denseChannels),
                    this.estimateBlockInterTileBytes(AfmaBlockInterPayloadHelper.TileMode.COPY_DENSE, densePayloadBytes, 0),
                    0
            );
            if (this.isBetterBlockInterCandidate(denseCandidate, currentBestCandidate)) {
                bestCandidate = denseCandidate;
            }
        }

        BlockInterTileCandidate sparseComparisonBest = (bestCandidate != null) ? bestCandidate : currentBestCandidate;
        if (this.canPotentialBlockInterModeBeat(
                sparseComparisonBest,
                AfmaBlockInterPayloadHelper.TileMode.COPY_SPARSE,
                this.estimateOptimisticBlockInterSparseBytes(width, height, tileStats.changedPixelCount())
        )) {
            SparseResidualPayloadData sparseResidual = this.buildMotionSparseResidualPayload(previousFrame, currentFrame, dstX, dstY, srcX, srcY, width, height, tileStats);
            if (sparseResidual != null) {
                BlockInterTileCandidate sparseCandidate = new BlockInterTileCandidate(
                        new SparseBlockInterTileEncoding(dx, dy, sparseResidual),
                        this.estimateBlockInterTileBytes(AfmaBlockInterPayloadHelper.TileMode.COPY_SPARSE,
                                sparseResidual.layoutPayload().length, sparseResidual.residualPayload().length),
                        sparseResidual.complexityScore()
                );
                if (this.isBetterBlockInterCandidate(sparseCandidate, currentBestCandidate)) {
                    bestCandidate = this.selectBetterBlockInterCandidate(bestCandidate, sparseCandidate);
                }
            }
        }
        return bestCandidate;
    }

    @NotNull
    protected BlockInterTileCandidate refineBlockInterTileMotion(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                                                 int dstX, int dstY, int width, int height,
                                                                 @NotNull BlockInterTileCandidate bestCandidate,
                                                                 @NotNull List<MotionSearchSeed> refinementSeeds,
                                                                 @NotNull Set<Long> testedVectors) {
        BlockInterTileCandidate bestMotionCandidate = bestCandidate;
        for (MotionSearchSeed initialSeed : refinementSeeds) {
            if (this.isOptimalBlockInterTileCandidate(bestMotionCandidate)) {
                break;
            }
            MotionSearchSeed seed = initialSeed;
            int centerDx = seed.motionVector().dx();
            int centerDy = seed.motionVector().dy();
            for (int pass = 0; pass < BLOCK_INTER_LOCAL_REFINEMENT_PASSES; pass++) {
                MotionSearchSeed bestSeedThisPass = seed;
                boolean improved = false;
                for (int candidateDy = centerDy - BLOCK_INTER_LOCAL_REFINEMENT_RADIUS; candidateDy <= centerDy + BLOCK_INTER_LOCAL_REFINEMENT_RADIUS; candidateDy++) {
                    for (int candidateDx = centerDx - BLOCK_INTER_LOCAL_REFINEMENT_RADIUS; candidateDx <= centerDx + BLOCK_INTER_LOCAL_REFINEMENT_RADIUS; candidateDx++) {
                        if ((candidateDx == centerDx) && (candidateDy == centerDy)) {
                            continue;
                        }

                        long packedVector = packMotionVector(candidateDx, candidateDy);
                        if (!testedVectors.add(packedVector)) {
                            continue;
                        }

                        BlockInterTileCandidate refinedCandidate = this.evaluateBlockInterMotionCandidate(previousFrame, currentFrame, dstX, dstY, width, height,
                                candidateDx, candidateDy, bestMotionCandidate);
                        if (refinedCandidate == null) {
                            continue;
                        }

                        bestMotionCandidate = this.selectBetterBlockInterCandidate(bestMotionCandidate, refinedCandidate);
                        if (this.isOptimalBlockInterTileCandidate(bestMotionCandidate)) {
                            return bestMotionCandidate;
                        }
                        if (this.isBetterBlockInterCandidate(refinedCandidate, bestSeedThisPass.candidate())) {
                            bestSeedThisPass = new MotionSearchSeed(new AfmaRectCopyDetector.MotionVector(candidateDx, candidateDy), refinedCandidate);
                            improved = true;
                        }
                    }
                }

                if (!improved) {
                    break;
                }
                seed = bestSeedThisPass;
                centerDx = seed.motionVector().dx();
                centerDy = seed.motionVector().dy();
            }
        }
        return bestMotionCandidate;
    }

    protected void addMotionSearchSeed(@NotNull List<MotionSearchSeed> refinementSeeds,
                                       @NotNull AfmaRectCopyDetector.MotionVector motionVector,
                                       @NotNull BlockInterTileCandidate candidate) {
        refinementSeeds.add(new MotionSearchSeed(motionVector, candidate));
        refinementSeeds.sort((first, second) -> {
            int byteCompare = Long.compare(first.candidate().estimatedBytes(), second.candidate().estimatedBytes());
            if (byteCompare != 0) {
                return byteCompare;
            }
            int complexityCompare = Integer.compare(first.candidate().complexityScore(), second.candidate().complexityScore());
            if (complexityCompare != 0) {
                return complexityCompare;
            }
            return Integer.compare(first.candidate().encoding().mode().ordinal(), second.candidate().encoding().mode().ordinal());
        });
        if (refinementSeeds.size() > BLOCK_INTER_LOCAL_REFINEMENT_SEEDS) {
            refinementSeeds.remove(refinementSeeds.size() - 1);
        }
    }

    protected BlockInterTileCandidate selectBetterBlockInterCandidate(@Nullable BlockInterTileCandidate currentBest,
                                                                      @Nullable BlockInterTileCandidate candidate) {
        if (candidate == null) {
            return Objects.requireNonNull(currentBest);
        }
        if ((currentBest == null) || this.isBetterBlockInterCandidate(candidate, currentBest)) {
            return candidate;
        }
        return currentBest;
    }

    protected boolean isBetterBlockInterCandidate(@NotNull BlockInterTileCandidate candidate, @NotNull BlockInterTileCandidate currentBest) {
        if (candidate.estimatedBytes() != currentBest.estimatedBytes()) {
            return candidate.estimatedBytes() < currentBest.estimatedBytes();
        }
        if (candidate.complexityScore() != currentBest.complexityScore()) {
            return candidate.complexityScore() < currentBest.complexityScore();
        }
        return candidate.encoding().mode().ordinal() < currentBest.encoding().mode().ordinal();
    }

    protected boolean isOptimalBlockInterTileCandidate(@NotNull BlockInterTileCandidate candidate) {
        return candidate.encoding().mode() == AfmaBlockInterPayloadHelper.TileMode.COPY;
    }

    protected boolean canPotentialBlockInterModeBeat(@NotNull BlockInterTileCandidate currentBestCandidate,
                                                     @NotNull AfmaBlockInterPayloadHelper.TileMode candidateMode,
                                                     long optimisticEstimatedBytes) {
        if (optimisticEstimatedBytes != currentBestCandidate.estimatedBytes()) {
            return optimisticEstimatedBytes < currentBestCandidate.estimatedBytes();
        }
        return candidateMode.ordinal() < currentBestCandidate.encoding().mode().ordinal();
    }

    protected long estimateOptimisticBlockInterDenseBytes(int width, int height) {
        return this.estimateBlockInterTileBytes(
                AfmaBlockInterPayloadHelper.TileMode.COPY_DENSE,
                AfmaBlockInterPayloadHelper.expectedDenseResidualBytes(width, height, AfmaResidualPayloadHelper.RGB_CHANNELS),
                0
        );
    }

    protected long estimateOptimisticBlockInterSparseBytes(int width, int height, int changedPixelCount) {
        int optimisticLayoutBytes = Math.min(
                AfmaResidualPayloadHelper.expectedSparseMaskBytes(width, height),
                changedPixelCount
        );
        return this.estimateBlockInterTileBytes(
                AfmaBlockInterPayloadHelper.TileMode.COPY_SPARSE,
                optimisticLayoutBytes,
                AfmaResidualPayloadHelper.expectedSparseResidualBytes(changedPixelCount, AfmaResidualPayloadHelper.RGB_CHANNELS)
        );
    }

    @NotNull
    protected BlockInterRegionAnalysis analyzeBlockInterRegion(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                                               @NotNull AfmaRect regionBounds, int tileCountX, int tileCountY) {
        int totalTileCount = tileCountX * tileCountY;
        boolean[] changedTiles = new boolean[totalTileCount];
        int changedTileCount = 0;
        int tileIndex = 0;
        for (int tileY = 0; tileY < tileCountY; tileY++) {
            int dstY = regionBounds.y() + (tileY * BLOCK_INTER_TILE_SIZE);
            int tileHeight = AfmaBlockInterPayloadHelper.tileDimension(tileY, tileCountY, BLOCK_INTER_TILE_SIZE, regionBounds.height());
            for (int tileX = 0; tileX < tileCountX; tileX++, tileIndex++) {
                int dstX = regionBounds.x() + (tileX * BLOCK_INTER_TILE_SIZE);
                int tileWidth = AfmaBlockInterPayloadHelper.tileDimension(tileX, tileCountX, BLOCK_INTER_TILE_SIZE, regionBounds.width());
                boolean tileChanged = !this.isTileIdentical(previousFrame, currentFrame, dstX, dstY, tileWidth, tileHeight);
                changedTiles[tileIndex] = tileChanged;
                if (tileChanged) {
                    changedTileCount++;
                }
            }
        }
        return new BlockInterRegionAnalysis(totalTileCount, changedTileCount, changedTiles);
    }

    protected boolean shouldAttemptBlockInter(@NotNull AfmaRect regionBounds, @NotNull BlockInterRegionAnalysis regionAnalysis,
                                              @Nullable PlannedCandidate bestBaselineCandidate) {
        if (regionAnalysis.changedTileCount() <= 0) {
            return false;
        }
        if (bestBaselineCandidate == null) {
            return true;
        }

        // If even an all-copy lower bound cannot beat the best simpler candidate, skip the expensive tile search entirely.
        long optimisticCandidateBytes = this.estimateOptimisticBlockInterCandidateBytes(regionBounds, regionAnalysis);
        long baselineCandidateBytes = bestBaselineCandidate.estimatedArchiveBytes(EMPTY_PAYLOAD_PATHS_BY_FINGERPRINT);
        return optimisticCandidateBytes < baselineCandidateBytes;
    }

    protected long estimateOptimisticBlockInterCandidateBytes(@NotNull AfmaRect regionBounds, @NotNull BlockInterRegionAnalysis regionAnalysis) {
        return this.estimateBlockInterDescriptorBytes(regionBounds)
                + BLOCK_INTER_PAYLOAD_HEADER_BYTES
                + regionAnalysis.identicalTileCount()
                + (regionAnalysis.changedTileCount() * 5L);
    }

    protected int estimateBlockInterDescriptorBytes(@NotNull AfmaRect regionBounds) {
        return 1
                + 2
                + this.estimateArchiveVarIntBytes(regionBounds.x())
                + this.estimateArchiveVarIntBytes(regionBounds.y())
                + this.estimateArchiveVarIntBytes(regionBounds.width())
                + this.estimateArchiveVarIntBytes(regionBounds.height())
                + this.estimateArchiveVarIntBytes(BLOCK_INTER_TILE_SIZE);
    }

    @NotNull
    protected List<AfmaRectCopyDetector.MotionVector> collectBlockInterMotionVectors(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                                                                     @Nullable AfmaRectCopyDetector.Detection copyDetection,
                                                                                     @Nullable AfmaRectCopyDetector.MultiDetection multiDetection,
                                                                                     @NotNull AfmaRectCopyDetector copyDetector,
                                                                                     @NotNull BlockInterRegionAnalysis regionAnalysis) {
        LinkedHashMap<Long, AfmaRectCopyDetector.MotionVector> motionVectorsByKey = new LinkedHashMap<>();
        this.addBlockInterMotionVector(motionVectorsByKey, 0, 0);
        if (copyDetection != null) {
            this.addBlockInterMotionVector(motionVectorsByKey,
                    copyDetection.copyRect().getSrcX() - copyDetection.copyRect().getDstX(),
                    copyDetection.copyRect().getSrcY() - copyDetection.copyRect().getDstY());
        }
        if (multiDetection != null) {
            for (AfmaCopyRect copyRect : multiDetection.multiCopy().getCopyRects()) {
                this.addBlockInterMotionVector(motionVectorsByKey,
                        copyRect.getSrcX() - copyRect.getDstX(),
                        copyRect.getSrcY() - copyRect.getDstY());
            }
        }

        int nonZeroSeedCount = Math.max(0, motionVectorsByKey.size() - 1);
        if (this.shouldUseSeededBlockInterMotionVectorsOnly(regionAnalysis, nonZeroSeedCount)) {
            return List.copyOf(motionVectorsByKey.values());
        }

        int maxNonZeroVectors = this.computeMaxBlockInterNonZeroMotionVectors(regionAnalysis.changedTileCount());
        if (nonZeroSeedCount < maxNonZeroVectors) {
            // Copy detections are cheap to reuse. Only pull as much of the global motion ranking as the seeded frontier still needs.
            int requestedRankedMotionVectors = maxNonZeroVectors + nonZeroSeedCount;
            for (AfmaRectCopyDetector.MotionVector motionVector : copyDetector.collectTopMotionVectors(pairAnalysis, requestedRankedMotionVectors)) {
                this.addBlockInterMotionVector(motionVectorsByKey, motionVector.dx(), motionVector.dy());
                if ((motionVectorsByKey.size() - 1) >= maxNonZeroVectors) {
                    break;
                }
            }
        }
        return List.copyOf(motionVectorsByKey.values());
    }

    protected void addBlockInterMotionVector(@NotNull Map<Long, AfmaRectCopyDetector.MotionVector> motionVectorsByKey, int dx, int dy) {
        motionVectorsByKey.putIfAbsent(packMotionVector(dx, dy), new AfmaRectCopyDetector.MotionVector(dx, dy));
    }

    protected boolean shouldUseSeededBlockInterMotionVectorsOnly(@NotNull BlockInterRegionAnalysis regionAnalysis, int nonZeroSeedCount) {
        return nonZeroSeedCount > 0
                && regionAnalysis.changedTileCount() <= Math.max(2, nonZeroSeedCount * 2);
    }

    protected int computeMaxBlockInterNonZeroMotionVectors(int changedTileCount) {
        if (changedTileCount <= 0) {
            return 0;
        }

        int scaledVectorCount = changedTileCount * BLOCK_INTER_NON_ZERO_MOTION_VECTORS_PER_CHANGED_TILE;
        return Math.max(
                BLOCK_INTER_MIN_NON_ZERO_MOTION_VECTORS,
                Math.min(BLOCK_INTER_MAX_NON_ZERO_MOTION_VECTORS, scaledVectorCount)
        );
    }

    protected int estimateArchiveVarIntBytes(int value) {
        if ((value & ~0x7F) == 0) {
            return 1;
        }
        if ((value & ~0x3FFF) == 0) {
            return 2;
        }
        if ((value & ~0x1FFFFF) == 0) {
            return 3;
        }
        if ((value & ~0xFFFFFFF) == 0) {
            return 4;
        }
        return 5;
    }

    protected static long packMotionVector(int dx, int dy) {
        return (((long) dx) << 32) ^ (dy & 0xFFFFFFFFL);
    }

    protected boolean isTileIdentical(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                      int dstX, int dstY, int width, int height) {
        int frameWidth = currentFrame.getWidth();
        int[] previousPixels = previousFrame.getPixelsUnsafe();
        int[] currentPixels = currentFrame.getPixelsUnsafe();
        for (int localY = 0; localY < height; localY++) {
            int rowOffset = ((dstY + localY) * frameWidth) + dstX;
            if (Arrays.mismatch(previousPixels, rowOffset, rowOffset + width, currentPixels, rowOffset, rowOffset + width) != -1) {
                return false;
            }
        }
        return true;
    }

    protected boolean isMotionTileInBounds(@NotNull AfmaPixelFrame previousFrame, int srcX, int srcY, int width, int height) {
        return srcX >= 0
                && srcY >= 0
                && (srcX + width) <= previousFrame.getWidth()
                && (srcY + height) <= previousFrame.getHeight();
    }

    @Nullable
    protected MotionTileStats scanMotionTile(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                             int dstX, int dstY, int srcX, int srcY, int width, int height,
                                             @NotNull BlockInterTileCandidate currentBestCandidate) {
        int frameWidth = currentFrame.getWidth();
        int[] previousPixels = previousFrame.getPixelsUnsafe();
        int[] currentPixels = currentFrame.getPixelsUnsafe();
        int changedPixelCount = 0;
        boolean includeAlpha = false;
        long optimisticDenseBytes = this.estimateOptimisticBlockInterDenseBytes(width, height);
        int totalPixels = width * height;
        for (int localY = 0; localY < height; localY++) {
            int previousRowOffset = ((srcY + localY) * frameWidth) + srcX;
            int currentRowOffset = ((dstY + localY) * frameWidth) + dstX;
            for (int localX = 0; localX < width; localX++) {
                int predictedColor = previousPixels[previousRowOffset + localX];
                int currentColor = currentPixels[currentRowOffset + localX];
                if (predictedColor == currentColor) {
                    continue;
                }
                changedPixelCount++;
                if (((predictedColor ^ currentColor) & 0xFF000000) != 0) {
                    includeAlpha = true;
                }

                boolean denseCanStillWin = this.canPotentialBlockInterModeBeat(
                        currentBestCandidate,
                        AfmaBlockInterPayloadHelper.TileMode.COPY_DENSE,
                        optimisticDenseBytes
                );
                boolean sparseCanStillWin = (changedPixelCount < totalPixels)
                        && this.canPotentialBlockInterModeBeat(
                        currentBestCandidate,
                        AfmaBlockInterPayloadHelper.TileMode.COPY_SPARSE,
                        this.estimateOptimisticBlockInterSparseBytes(width, height, changedPixelCount)
                );
                // After the first mismatch, COPY is impossible; stop once even the optimistic residual bounds cannot win.
                if (!denseCanStillWin && !sparseCanStillWin) {
                    return null;
                }
            }
        }
        return new MotionTileStats(changedPixelCount, includeAlpha);
    }

    @Nullable
    protected ResidualPayloadData buildMotionResidualPayload(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                                             int dstX, int dstY, int srcX, int srcY, int width, int height, boolean includeAlpha) {
        int channels = AfmaResidualPayloadHelper.channelCount(includeAlpha);
        int expectedBytes = AfmaBlockInterPayloadHelper.expectedDenseResidualBytes(width, height, channels);
        if (expectedBytes <= 0) {
            return null;
        }

        int frameWidth = currentFrame.getWidth();
        int[] previousPixels = previousFrame.getPixelsUnsafe();
        int[] currentPixels = currentFrame.getPixelsUnsafe();
        byte[] payloadBytes = new byte[expectedBytes];
        int payloadOffset = 0;
        for (int localY = 0; localY < height; localY++) {
            int previousRowOffset = ((srcY + localY) * frameWidth) + srcX;
            int currentRowOffset = ((dstY + localY) * frameWidth) + dstX;
            for (int localX = 0; localX < width; localX++) {
                int predictedColor = previousPixels[previousRowOffset + localX];
                int currentColor = currentPixels[currentRowOffset + localX];
                payloadOffset = AfmaResidualPayloadHelper.writeResidual(payloadBytes, payloadOffset, predictedColor, currentColor, includeAlpha);
            }
        }
        return new ResidualPayloadData(payloadBytes, new AfmaResidualPayload(channels), 0);
    }

    @Nullable
    protected SparseResidualPayloadData buildMotionSparseResidualPayload(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                                                         int dstX, int dstY, int srcX, int srcY, int width, int height,
                                                                         @NotNull MotionTileStats tileStats) {
        if (tileStats.changedPixelCount() <= 0 || tileStats.changedPixelCount() >= (width * height)) {
            return null;
        }

        int frameWidth = currentFrame.getWidth();
        int[] previousPixels = previousFrame.getPixelsUnsafe();
        int[] currentPixels = currentFrame.getPixelsUnsafe();
        int changedPixelCount = tileStats.changedPixelCount();
        int[] changedIndices = new int[changedPixelCount];
        int[] predictedColors = new int[changedPixelCount];
        int[] changedColors = new int[changedPixelCount];
        int changedOffset = 0;
        for (int localY = 0; localY < height; localY++) {
            int previousRowOffset = ((srcY + localY) * frameWidth) + srcX;
            int currentRowOffset = ((dstY + localY) * frameWidth) + dstX;
            for (int localX = 0; localX < width; localX++) {
                int predictedColor = previousPixels[previousRowOffset + localX];
                int currentColor = currentPixels[currentRowOffset + localX];
                if (predictedColor == currentColor) {
                    continue;
                }
                changedIndices[changedOffset] = (localY * width) + localX;
                predictedColors[changedOffset] = predictedColor;
                changedColors[changedOffset] = currentColor;
                changedOffset++;
            }
        }
        try {
            return this.buildSparseResidualPayload(width, height, changedIndices, predictedColors, changedColors, changedPixelCount, tileStats.includeAlpha());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to encode AFMA block_inter sparse tile payload", ex);
        }
    }

    protected int determineRawTileChannels(@NotNull AfmaPixelFrame currentFrame, int dstX, int dstY, int width, int height) {
        int frameWidth = currentFrame.getWidth();
        int[] currentPixels = currentFrame.getPixelsUnsafe();
        for (int localY = 0; localY < height; localY++) {
            int rowOffset = ((dstY + localY) * frameWidth) + dstX;
            for (int localX = 0; localX < width; localX++) {
                if (((currentPixels[rowOffset + localX] >>> 24) & 0xFF) != 0xFF) {
                    return AfmaResidualPayloadHelper.RGBA_CHANNELS;
                }
            }
        }
        return AfmaResidualPayloadHelper.RGB_CHANNELS;
    }

    @NotNull
    protected byte[] buildRawTileBytes(@NotNull AfmaPixelFrame currentFrame, int dstX, int dstY, int width, int height, int channels) {
        int frameWidth = currentFrame.getWidth();
        int[] currentPixels = currentFrame.getPixelsUnsafe();
        boolean includeAlpha = channels == AfmaResidualPayloadHelper.RGBA_CHANNELS;
        byte[] payloadBytes = new byte[AfmaBlockInterPayloadHelper.expectedRawTileBytes(width, height, channels)];
        int payloadOffset = 0;
        for (int localY = 0; localY < height; localY++) {
            int rowOffset = ((dstY + localY) * frameWidth) + dstX;
            for (int localX = 0; localX < width; localX++) {
                int color = currentPixels[rowOffset + localX];
                payloadBytes[payloadOffset++] = (byte) ((color >> 16) & 0xFF);
                payloadBytes[payloadOffset++] = (byte) ((color >> 8) & 0xFF);
                payloadBytes[payloadOffset++] = (byte) (color & 0xFF);
                if (includeAlpha) {
                    payloadBytes[payloadOffset++] = (byte) ((color >>> 24) & 0xFF);
                }
            }
        }
        return payloadBytes;
    }

    protected long estimateBlockInterTileBytes(@NotNull AfmaBlockInterPayloadHelper.TileMode mode, int primaryBytes, int secondaryBytes) {
        return switch (mode) {
            case SKIP -> 1L;
            case COPY -> 5L;
            case COPY_DENSE -> 6L + primaryBytes;
            case COPY_SPARSE -> 15L + primaryBytes + secondaryBytes;
            case RAW -> 2L + primaryBytes;
        };
    }

    @NotNull
    protected AfmaRect alignBoundsToTileGrid(@NotNull AfmaRect bounds, int tileSize, int canvasWidth, int canvasHeight) {
        int minX = Math.max(0, (bounds.x() / tileSize) * tileSize);
        int minY = Math.max(0, (bounds.y() / tileSize) * tileSize);
        int maxX = Math.min(canvasWidth, ((bounds.x() + bounds.width() + tileSize - 1) / tileSize) * tileSize);
        int maxY = Math.min(canvasHeight, ((bounds.y() + bounds.height() + tileSize - 1) / tileSize) * tileSize);
        return new AfmaRect(minX, minY, Math.max(1, maxX - minX), Math.max(1, maxY - minY));
    }

    protected boolean shouldKeepComplexCandidate(@NotNull PlannedCandidate candidate, @NotNull PlannedCandidate fullCandidate,
                                                 long patchArea, int frameWidth, int frameHeight, double maxAreaRatioWithoutStrongSavings,
                                                 @NotNull AfmaEncodeOptions options, @NotNull Map<String, String> payloadPathsByFingerprint) {
        long fullArchiveBytes = fullCandidate.estimatedArchiveBytes(payloadPathsByFingerprint);
        long candidateArchiveBytes = candidate.estimatedArchiveBytes(payloadPathsByFingerprint);
        return this.shouldKeepComplexCandidateByArchiveBytes(candidateArchiveBytes, fullArchiveBytes, patchArea,
                frameWidth, frameHeight, maxAreaRatioWithoutStrongSavings, options);
    }

    protected boolean shouldKeepResidualCandidate(@NotNull PlannedCandidate candidate, @NotNull PlannedCandidate fullCandidate,
                                                  long patchArea, int frameWidth, int frameHeight, double maxAreaRatioWithoutStrongSavings,
                                                  @NotNull AfmaEncodeOptions options, @NotNull Map<String, String> payloadPathsByFingerprint) {
        long fullArchiveBytes = fullCandidate.estimatedArchiveBytes(payloadPathsByFingerprint);
        long candidateArchiveBytes = candidate.estimatedArchiveBytes(payloadPathsByFingerprint);
        return this.shouldKeepResidualCandidateByArchiveBytes(candidateArchiveBytes, fullArchiveBytes, patchArea,
                frameWidth, frameHeight, maxAreaRatioWithoutStrongSavings, options);
    }

    protected boolean shouldKeepSparseCandidate(@NotNull PlannedCandidate candidate, @NotNull PlannedCandidate fullCandidate,
                                                long patchArea, int frameWidth, int frameHeight, double maxAreaRatioWithoutStrongSavings,
                                                @NotNull AfmaEncodeOptions options, @NotNull Map<String, String> payloadPathsByFingerprint) {
        long fullArchiveBytes = fullCandidate.estimatedArchiveBytes(payloadPathsByFingerprint);
        long candidateArchiveBytes = candidate.estimatedArchiveBytes(payloadPathsByFingerprint);
        return this.shouldKeepSparseCandidateByArchiveBytes(candidateArchiveBytes, fullArchiveBytes, patchArea,
                frameWidth, frameHeight, maxAreaRatioWithoutStrongSavings, options);
    }

    @Nullable
    protected PlannedCandidate selectBestEstimatedArchiveCandidate(@Nullable PlannedCandidate... candidates) {
        PlannedCandidate bestCandidate = null;
        for (PlannedCandidate candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            if ((bestCandidate == null) || candidate.isBetterThan(bestCandidate, EMPTY_PAYLOAD_PATHS_BY_FINGERPRINT)) {
                bestCandidate = candidate;
            }
        }
        return bestCandidate;
    }

    protected boolean shouldKeepComplexCandidate(@NotNull PlannedCandidate candidate, @NotNull PlannedCandidate fullCandidate,
                                                 long patchArea, int frameWidth, int frameHeight, double maxAreaRatioWithoutStrongSavings,
                                                 @NotNull AfmaEncodeOptions options, @NotNull ArchivePlanningState archiveState,
                                                 boolean introSequence, @NotNull WindowCandidateCache windowCandidateCache) {
        long fullArchiveBytes = windowCandidateCache.getArchiveAppendCost(archiveState, fullCandidate, introSequence).marginalArchiveBytes();
        return this.shouldKeepComplexCandidate(candidate, fullArchiveBytes, patchArea, frameWidth, frameHeight,
                maxAreaRatioWithoutStrongSavings, options, archiveState, introSequence, windowCandidateCache);
    }

    protected boolean shouldKeepComplexCandidate(@NotNull PlannedCandidate candidate, long fullArchiveBytes,
                                                 long patchArea, int frameWidth, int frameHeight, double maxAreaRatioWithoutStrongSavings,
                                                 @NotNull AfmaEncodeOptions options, @NotNull ArchivePlanningState archiveState,
                                                 boolean introSequence, @NotNull WindowCandidateCache windowCandidateCache) {
        long candidateArchiveBytes = windowCandidateCache.getArchiveAppendCost(archiveState, candidate, introSequence).marginalArchiveBytes();
        return this.shouldKeepComplexCandidateByArchiveBytes(candidateArchiveBytes, fullArchiveBytes, patchArea,
                frameWidth, frameHeight, maxAreaRatioWithoutStrongSavings, options);
    }

    protected boolean shouldKeepResidualCandidate(@NotNull PlannedCandidate candidate, @NotNull PlannedCandidate fullCandidate,
                                                  long patchArea, int frameWidth, int frameHeight, double maxAreaRatioWithoutStrongSavings,
                                                  @NotNull AfmaEncodeOptions options, @NotNull ArchivePlanningState archiveState,
                                                  boolean introSequence, @NotNull WindowCandidateCache windowCandidateCache) {
        long fullArchiveBytes = windowCandidateCache.getArchiveAppendCost(archiveState, fullCandidate, introSequence).marginalArchiveBytes();
        return this.shouldKeepResidualCandidate(candidate, fullArchiveBytes, patchArea, frameWidth, frameHeight,
                maxAreaRatioWithoutStrongSavings, options, archiveState, introSequence, windowCandidateCache);
    }

    protected boolean shouldKeepResidualCandidate(@NotNull PlannedCandidate candidate, long fullArchiveBytes,
                                                  long patchArea, int frameWidth, int frameHeight, double maxAreaRatioWithoutStrongSavings,
                                                  @NotNull AfmaEncodeOptions options, @NotNull ArchivePlanningState archiveState,
                                                  boolean introSequence, @NotNull WindowCandidateCache windowCandidateCache) {
        long candidateArchiveBytes = windowCandidateCache.getArchiveAppendCost(archiveState, candidate, introSequence).marginalArchiveBytes();
        return this.shouldKeepResidualCandidateByArchiveBytes(candidateArchiveBytes, fullArchiveBytes, patchArea,
                frameWidth, frameHeight, maxAreaRatioWithoutStrongSavings, options);
    }

    protected boolean shouldKeepSparseCandidate(@NotNull PlannedCandidate candidate, @NotNull PlannedCandidate fullCandidate,
                                                long patchArea, int frameWidth, int frameHeight, double maxAreaRatioWithoutStrongSavings,
                                                @NotNull AfmaEncodeOptions options, @NotNull ArchivePlanningState archiveState,
                                                boolean introSequence, @NotNull WindowCandidateCache windowCandidateCache) {
        long fullArchiveBytes = windowCandidateCache.getArchiveAppendCost(archiveState, fullCandidate, introSequence).marginalArchiveBytes();
        return this.shouldKeepSparseCandidate(candidate, fullArchiveBytes, patchArea, frameWidth, frameHeight,
                maxAreaRatioWithoutStrongSavings, options, archiveState, introSequence, windowCandidateCache);
    }

    protected boolean shouldKeepSparseCandidate(@NotNull PlannedCandidate candidate, long fullArchiveBytes,
                                                long patchArea, int frameWidth, int frameHeight, double maxAreaRatioWithoutStrongSavings,
                                                @NotNull AfmaEncodeOptions options, @NotNull ArchivePlanningState archiveState,
                                                boolean introSequence, @NotNull WindowCandidateCache windowCandidateCache) {
        long candidateArchiveBytes = windowCandidateCache.getArchiveAppendCost(archiveState, candidate, introSequence).marginalArchiveBytes();
        return this.shouldKeepSparseCandidateByArchiveBytes(candidateArchiveBytes, fullArchiveBytes, patchArea,
                frameWidth, frameHeight, maxAreaRatioWithoutStrongSavings, options);
    }

    protected boolean shouldAttemptComplexCandidate(long candidateArchiveLowerBoundBytes, long fullArchiveBytes,
                                                    long patchArea, int frameWidth, int frameHeight, double maxAreaRatioWithoutStrongSavings,
                                                    @NotNull AfmaEncodeOptions options) {
        return this.shouldKeepComplexCandidateByArchiveBytes(candidateArchiveLowerBoundBytes, fullArchiveBytes, patchArea,
                frameWidth, frameHeight, maxAreaRatioWithoutStrongSavings, options);
    }

    protected boolean shouldAttemptResidualCandidate(long candidateArchiveLowerBoundBytes, long fullArchiveBytes,
                                                     long patchArea, int frameWidth, int frameHeight, double maxAreaRatioWithoutStrongSavings,
                                                     @NotNull AfmaEncodeOptions options) {
        return this.shouldKeepResidualCandidateByArchiveBytes(candidateArchiveLowerBoundBytes, fullArchiveBytes, patchArea,
                frameWidth, frameHeight, maxAreaRatioWithoutStrongSavings, options);
    }

    protected boolean shouldAttemptSparseCandidate(long candidateArchiveLowerBoundBytes, long fullArchiveBytes,
                                                   long patchArea, int frameWidth, int frameHeight, double maxAreaRatioWithoutStrongSavings,
                                                   @NotNull AfmaEncodeOptions options) {
        return this.shouldKeepSparseCandidateByArchiveBytes(candidateArchiveLowerBoundBytes, fullArchiveBytes, patchArea,
                frameWidth, frameHeight, maxAreaRatioWithoutStrongSavings, options);
    }

    protected boolean shouldKeepComplexCandidateByArchiveBytes(long candidateArchiveBytes, long fullArchiveBytes,
                                                               long patchArea, int frameWidth, int frameHeight, double maxAreaRatioWithoutStrongSavings,
                                                               @NotNull AfmaEncodeOptions options) {
        long frameArea = (long) frameWidth * frameHeight;
        if (frameArea <= 0L || patchArea >= frameArea) {
            return false;
        }

        long byteSavings = fullArchiveBytes - candidateArchiveBytes;
        if (byteSavings <= 0L) {
            return false;
        }
        if (patchArea <= 0L) {
            return true;
        }

        return byteSavings >= this.computeRequiredCandidateSavings(fullArchiveBytes, patchArea, frameArea,
                maxAreaRatioWithoutStrongSavings, options);
    }

    protected boolean shouldKeepResidualCandidateByArchiveBytes(long candidateArchiveBytes, long fullArchiveBytes,
                                                                long patchArea, int frameWidth, int frameHeight, double maxAreaRatioWithoutStrongSavings,
                                                                @NotNull AfmaEncodeOptions options) {
        long frameArea = (long) frameWidth * frameHeight;
        if (frameArea <= 0L || patchArea <= 0L) {
            return false;
        }

        long byteSavings = fullArchiveBytes - candidateArchiveBytes;
        if (byteSavings <= 0L) {
            return false;
        }

        long boundedPatchArea = Math.min(patchArea, frameArea);
        return byteSavings >= this.computeRequiredCandidateSavings(fullArchiveBytes, boundedPatchArea, frameArea,
                maxAreaRatioWithoutStrongSavings, options);
    }

    protected boolean shouldKeepSparseCandidateByArchiveBytes(long candidateArchiveBytes, long fullArchiveBytes,
                                                              long patchArea, int frameWidth, int frameHeight, double maxAreaRatioWithoutStrongSavings,
                                                              @NotNull AfmaEncodeOptions options) {
        long frameArea = (long) frameWidth * frameHeight;
        if (frameArea <= 0L || patchArea <= 0L) {
            return false;
        }

        long byteSavings = fullArchiveBytes - candidateArchiveBytes;
        if (byteSavings <= 0L) {
            return false;
        }

        long boundedPatchArea = Math.min(patchArea, frameArea);
        return byteSavings >= this.computeRequiredCandidateSavings(fullArchiveBytes, boundedPatchArea, frameArea,
                maxAreaRatioWithoutStrongSavings, options);
    }

    protected long computeRequiredCandidateSavings(long fullArchiveBytes, long boundedPatchArea, long frameArea,
                                                   double maxAreaRatioWithoutStrongSavings, @NotNull AfmaEncodeOptions options) {
        long requiredSavings = this.computeRequiredComplexCandidateSavings(
                fullArchiveBytes,
                options.getMinComplexCandidateSavingsBytes(),
                options.getMinComplexCandidateSavingsRatio()
        );
        double areaRatio = (double) boundedPatchArea / (double) frameArea;
        if (areaRatio > maxAreaRatioWithoutStrongSavings) {
            requiredSavings = Math.max(requiredSavings, this.computeRequiredComplexCandidateSavings(
                    fullArchiveBytes,
                    options.getMinStrongComplexCandidateSavingsBytes(),
                    options.getMinStrongComplexCandidateSavingsRatio()
            ));
        }
        return requiredSavings;
    }

    protected long estimateDeltaCandidateArchiveLowerBoundBytes(@NotNull AfmaRect deltaBounds) {
        return 1L
                + 2L
                + estimateVarIntBytes(deltaBounds.x())
                + estimateVarIntBytes(deltaBounds.y())
                + estimateVarIntBytes(deltaBounds.width())
                + estimateVarIntBytes(deltaBounds.height());
    }

    protected long estimateResidualDeltaCandidateArchiveLowerBoundBytes(@NotNull AfmaRect deltaBounds) {
        return 1L
                + 2L
                + estimateVarIntBytes(deltaBounds.x())
                + estimateVarIntBytes(deltaBounds.y())
                + estimateVarIntBytes(deltaBounds.width())
                + estimateVarIntBytes(deltaBounds.height())
                + estimateVarIntBytes(AfmaResidualPayloadHelper.RGB_CHANNELS)
                + 2L
                + estimateVarIntBytes(0);
    }

    protected long estimateSparseDeltaCandidateArchiveLowerBoundBytes(@NotNull AfmaRect deltaBounds) {
        return 1L
                + 4L
                + estimateVarIntBytes(deltaBounds.x())
                + estimateVarIntBytes(deltaBounds.y())
                + estimateVarIntBytes(deltaBounds.width())
                + estimateVarIntBytes(deltaBounds.height())
                + estimateVarIntBytes(MIN_SPARSE_DELTA_CHANGED_PIXELS)
                + estimateVarIntBytes(AfmaResidualPayloadHelper.RGB_CHANNELS)
                + 3L
                + estimateVarIntBytes(0);
    }

    protected long estimateCopyCandidateArchiveLowerBoundBytes(@NotNull AfmaRectCopyDetector.Detection detection) {
        return 1L
                + this.estimateCopyRectBytes(detection.copyRect())
                + 1L
                + this.estimatePatchRegionBytes(detection.patchBounds());
    }

    protected long estimateCopyResidualCandidateArchiveLowerBoundBytes(@NotNull AfmaRectCopyDetector.Detection detection) {
        AfmaRect patchBounds = Objects.requireNonNull(detection.patchBounds());
        return 1L
                + this.estimateCopyRectBytes(detection.copyRect())
                + 2L
                + estimateVarIntBytes(patchBounds.x())
                + estimateVarIntBytes(patchBounds.y())
                + estimateVarIntBytes(patchBounds.width())
                + estimateVarIntBytes(patchBounds.height())
                + estimateVarIntBytes(AfmaResidualPayloadHelper.RGB_CHANNELS)
                + 2L
                + estimateVarIntBytes(0);
    }

    protected long estimateCopySparseCandidateArchiveLowerBoundBytes(@NotNull AfmaRectCopyDetector.Detection detection) {
        AfmaRect patchBounds = Objects.requireNonNull(detection.patchBounds());
        return 1L
                + this.estimateCopyRectBytes(detection.copyRect())
                + 4L
                + estimateVarIntBytes(patchBounds.x())
                + estimateVarIntBytes(patchBounds.y())
                + estimateVarIntBytes(patchBounds.width())
                + estimateVarIntBytes(patchBounds.height())
                + estimateVarIntBytes(MIN_SPARSE_DELTA_CHANGED_PIXELS)
                + estimateVarIntBytes(AfmaResidualPayloadHelper.RGB_CHANNELS)
                + 3L
                + estimateVarIntBytes(0);
    }

    protected long estimateMultiCopyCandidateArchiveLowerBoundBytes(@NotNull AfmaRectCopyDetector.MultiDetection detection) {
        return 1L
                + this.estimateMultiCopyBytes(detection.multiCopy())
                + 1L
                + this.estimatePatchRegionBytes(detection.patchBounds());
    }

    protected long estimateMultiCopyResidualCandidateArchiveLowerBoundBytes(@NotNull AfmaRectCopyDetector.MultiDetection detection) {
        AfmaRect patchBounds = Objects.requireNonNull(detection.patchBounds());
        return 1L
                + this.estimateMultiCopyBytes(detection.multiCopy())
                + 2L
                + estimateVarIntBytes(patchBounds.x())
                + estimateVarIntBytes(patchBounds.y())
                + estimateVarIntBytes(patchBounds.width())
                + estimateVarIntBytes(patchBounds.height())
                + estimateVarIntBytes(AfmaResidualPayloadHelper.RGB_CHANNELS)
                + 2L
                + estimateVarIntBytes(0);
    }

    protected long estimateMultiCopySparseCandidateArchiveLowerBoundBytes(@NotNull AfmaRectCopyDetector.MultiDetection detection) {
        AfmaRect patchBounds = Objects.requireNonNull(detection.patchBounds());
        return 1L
                + this.estimateMultiCopyBytes(detection.multiCopy())
                + 4L
                + estimateVarIntBytes(patchBounds.x())
                + estimateVarIntBytes(patchBounds.y())
                + estimateVarIntBytes(patchBounds.width())
                + estimateVarIntBytes(patchBounds.height())
                + estimateVarIntBytes(MIN_SPARSE_DELTA_CHANGED_PIXELS)
                + estimateVarIntBytes(AfmaResidualPayloadHelper.RGB_CHANNELS)
                + 3L
                + estimateVarIntBytes(0);
    }

    protected long estimatePatchRegionBytes(@Nullable AfmaRect patchBounds) {
        if (patchBounds == null) {
            return 0L;
        }
        return 2L
                + estimateVarIntBytes(patchBounds.x())
                + estimateVarIntBytes(patchBounds.y())
                + estimateVarIntBytes(patchBounds.width())
                + estimateVarIntBytes(patchBounds.height());
    }

    protected int estimateCopyRectBytes(@NotNull AfmaCopyRect copyRect) {
        return estimateVarIntBytes(copyRect.getSrcX())
                + estimateVarIntBytes(copyRect.getSrcY())
                + estimateVarIntBytes(copyRect.getDstX())
                + estimateVarIntBytes(copyRect.getDstY())
                + estimateVarIntBytes(copyRect.getWidth())
                + estimateVarIntBytes(copyRect.getHeight());
    }

    protected int estimateMultiCopyBytes(@NotNull AfmaMultiCopy multiCopy) {
        List<AfmaCopyRect> copyRects = multiCopy.getCopyRects();
        int bytes = estimateVarIntBytes(copyRects.size() - 1);
        AfmaCopyRect previousRect = null;
        for (AfmaCopyRect copyRect : copyRects) {
            AfmaCopyRect currentRect = Objects.requireNonNull(copyRect);
            bytes += 1;
            if (previousRect == null) {
                bytes += this.estimateCopyRectBytes(currentRect);
            } else {
                bytes += estimateSignedVarIntBytes(currentRect.getSrcX() - previousRect.getSrcX());
                bytes += estimateSignedVarIntBytes(currentRect.getSrcY() - previousRect.getSrcY());
                bytes += estimateSignedVarIntBytes(currentRect.getDstX() - previousRect.getDstX());
                bytes += estimateSignedVarIntBytes(currentRect.getDstY() - previousRect.getDstY());
                if (currentRect.getWidth() != previousRect.getWidth()) {
                    bytes += estimateVarIntBytes(currentRect.getWidth());
                }
                if (currentRect.getHeight() != previousRect.getHeight()) {
                    bytes += estimateVarIntBytes(currentRect.getHeight());
                }
            }
            previousRect = currentRect;
        }
        return bytes;
    }

    protected int estimateSignedVarIntBytes(int value) {
        int zigZag = (value << 1) ^ (value >> 31);
        return estimateVarIntBytes(zigZag);
    }

    protected long computeRequiredComplexCandidateSavings(long referenceBytes, long minAbsoluteSavings, double minSavingsRatio) {
        long ratioSavings = (referenceBytes > 0L && minSavingsRatio > 0D)
                ? (long) Math.ceil(referenceBytes * minSavingsRatio)
                : 0L;
        return Math.max(minAbsoluteSavings, ratioSavings);
    }

    @NotNull
    protected String buildPayloadPath(boolean introSequence, int frameIndex) {
        return (introSequence ? "intro_frames/" : "frames/") + Integer.toUnsignedString(frameIndex, 36) + ".bin";
    }

    @NotNull
    protected String buildRawPayloadPath(boolean introSequence, int frameIndex, @NotNull String suffix) {
        return (introSequence ? "intro_frames/" : "frames/") + Integer.toUnsignedString(frameIndex, 36) + "_" + suffix + ".bin";
    }

    protected static int estimateVarIntBytes(int value) {
        if ((value & ~0x7F) == 0) {
            return 1;
        }
        if ((value & ~0x3FFF) == 0) {
            return 2;
        }
        if ((value & ~0x1FFFFF) == 0) {
            return 3;
        }
        if ((value & ~0xFFFFFFF) == 0) {
            return 4;
        }
        return 5;
    }

    @NotNull
    protected LoadedDimensionFrame loadDimensionFrame(@NotNull AfmaSourceSequence sequence, @Nullable BooleanSupplier cancellationRequested,
                                                      @Nullable ProgressListener progressListener) throws IOException {
        if (sequence.isEmpty()) {
            throw new IOException("AFMA encoding requires at least one source frame");
        }

        reportProgress(progressListener, "Reading source frame dimensions...", 0.02D);
        checkCancelled(cancellationRequested);
        File firstFrame = Objects.requireNonNull(sequence.getFrame(0));
        AfmaPixelFrame firstImage = this.frameNormalizer.loadFrame(firstFrame);
        return new LoadedDimensionFrame(new Dimension(firstImage.getWidth(), firstImage.getHeight()), firstImage);
    }

    protected static void reportProgress(@Nullable ProgressListener progressListener, @NotNull String detail, double progress) {
        if (progressListener != null) {
            progressListener.update(detail, progress);
        }
    }

    protected void reportPlanningFrameProgress(@Nullable ProgressListener progressListener, @NotNull String action, boolean introSequence,
                                               int sequenceFrameNumber, int sequenceFrameCount,
                                               double absoluteFrameProgress, int totalFrameCount) {
        if (sequenceFrameCount <= 0) {
            return;
        }

        int clampedSequenceFrameNumber = Math.max(1, Math.min(sequenceFrameCount, sequenceFrameNumber));
        reportProgress(progressListener,
                action + " " + (introSequence ? "intro" : "main") + " frame " + clampedSequenceFrameNumber + "/" + sequenceFrameCount,
                this.computePlanningProgress(absoluteFrameProgress, totalFrameCount));
    }

    protected double computePlanningProgress(double absoluteFrameProgress, int totalFrameCount) {
        double clampedFrameProgress = Math.max(0D, Math.min(Math.max(1, totalFrameCount), absoluteFrameProgress));
        return 0.08D + (0.92D * (clampedFrameProgress / Math.max(1D, (double) totalFrameCount)));
    }

    protected static void checkCancelled(@Nullable BooleanSupplier cancellationRequested) {
        if ((cancellationRequested != null) && cancellationRequested.getAsBoolean()) {
            throw new CancellationException("AFMA encode planning was cancelled");
        }
    }

    // Candidate generation only depends on frame content inside a planner window; archive-state filtering still happens outside.
    protected final class WindowCandidateCache {

        @NotNull
        protected final IdentityHashMap<AfmaPixelFrame, FrameContentKey> frameKeysByIdentity = new IdentityHashMap<>();
        @NotNull
        protected final Map<FramePairKey, AfmaFramePairAnalysis> framePairAnalysesByKey = new LinkedHashMap<>();
        @NotNull
        protected final Map<FullCandidateKey, PlannedCandidate> fullCandidatesByKey = new LinkedHashMap<>();
        @NotNull
        protected final Map<PairCandidateKey, PairCandidateSet> pairCandidatesByKey = new LinkedHashMap<>();
        @NotNull
        protected final Map<CandidateReferenceFrameAnalysisKey, CandidateReferenceFrameAnalysis> candidateReferenceFrameAnalysesByKey = new LinkedHashMap<>();
        @NotNull
        protected final Map<ArchiveAppendKey, CandidateArchiveCost> archiveAppendCostsByKey = new LinkedHashMap<>();
        @NotNull
        // Keep this cache scoped to a single source-frame expansion so identical beam states can share merges
        // without pinning past window frames longer than necessary.
        protected final Map<NearLosslessMergeKey, AfmaPixelFrame> nearLosslessMergedFramesByKey = new LinkedHashMap<>();

        @NotNull
        public AfmaPixelFrame getNearLosslessMergedFrame(@NotNull AfmaPixelFrame previousFrame,
                                                         @NotNull AfmaPixelFrame currentFrame,
                                                         int maxChannelDelta) {
            if (maxChannelDelta <= 0) {
                return currentFrame;
            }

            NearLosslessMergeKey cacheKey = new NearLosslessMergeKey(
                    this.frameKey(previousFrame),
                    this.frameKey(currentFrame),
                    maxChannelDelta
            );
            AfmaPixelFrame cachedFrame = this.nearLosslessMergedFramesByKey.get(cacheKey);
            if (cachedFrame != null) {
                return cachedFrame;
            }

            AfmaPixelFrame mergedFrame = AfmaEncodePlanner.this.applyNearLosslessTemporalMerge(previousFrame, currentFrame, maxChannelDelta);
            this.nearLosslessMergedFramesByKey.put(cacheKey, mergedFrame);
            return mergedFrame;
        }

        @NotNull
        public PlannedCandidate getFullCandidate(@NotNull AfmaPixelFrame frame, boolean introSequence, int frameIndex,
                                                 @NotNull AfmaEncodeOptions options, boolean allowPerceptual,
                                                 @NotNull ReferenceBase referenceBase) throws IOException {
            FullCandidateKey cacheKey = new FullCandidateKey(
                    this.frameKey(frame),
                    introSequence,
                    frameIndex,
                    allowPerceptual,
                    referenceBase
            );
            PlannedCandidate cachedCandidate = this.fullCandidatesByKey.get(cacheKey);
            if (cachedCandidate != null) {
                return cachedCandidate;
            }

            PlannedCandidate candidate = AfmaEncodePlanner.this.createFullCandidate(
                    frame,
                    introSequence,
                    frameIndex,
                    options,
                    allowPerceptual,
                    referenceBase
            );
            this.fullCandidatesByKey.put(cacheKey, candidate);
            return candidate;
        }

        @NotNull
        public PairCandidateSet getPairCandidateSet(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame workingFrame,
                                                    boolean introSequence, int frameIndex,
                                                    @NotNull AfmaEncodeOptions options,
                                                    @NotNull AfmaRectCopyDetector copyDetector) throws IOException {
            PairCandidateKey cacheKey = new PairCandidateKey(
                    this.frameKey(previousFrame),
                    this.frameKey(workingFrame),
                    introSequence,
                    frameIndex
            );
            PairCandidateSet cachedCandidateSet = this.pairCandidatesByKey.get(cacheKey);
            if (cachedCandidateSet != null) {
                return cachedCandidateSet;
            }

            PairCandidateSet candidateSet = new PairCandidateSet(
                    this,
                    previousFrame,
                    workingFrame,
                    introSequence,
                    frameIndex,
                    options,
                    copyDetector
            );
            this.pairCandidatesByKey.put(cacheKey, candidateSet);
            return candidateSet;
        }

        @NotNull
        public AfmaFramePairAnalysis getFramePairAnalysis(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame nextFrame) {
            FramePairKey cacheKey = new FramePairKey(this.frameKey(previousFrame), this.frameKey(nextFrame));
            AfmaFramePairAnalysis cachedAnalysis = this.framePairAnalysesByKey.get(cacheKey);
            if (cachedAnalysis != null) {
                return cachedAnalysis;
            }

            AfmaFramePairAnalysis pairAnalysis = new AfmaFramePairAnalysis(previousFrame, nextFrame);
            this.framePairAnalysesByKey.put(cacheKey, pairAnalysis);
            return pairAnalysis;
        }

        @NotNull
        public CandidateReferenceFrameAnalysis getCandidateReferenceFrameAnalysis(@NotNull PlannedCandidate candidate,
                                                                                  @NotNull AfmaPixelFrame sourceFrame,
                                                                                  @NotNull AfmaPixelFrame workingFrame) {
            CandidateReferenceFrameAnalysisKey cacheKey = new CandidateReferenceFrameAnalysisKey(
                    candidate,
                    this.frameKey(sourceFrame),
                    this.frameKey(workingFrame)
            );
            CandidateReferenceFrameAnalysis cachedAnalysis = this.candidateReferenceFrameAnalysesByKey.get(cacheKey);
            if (cachedAnalysis != null) {
                return cachedAnalysis;
            }

            AfmaPixelFrame reconstructedFrame = candidate.materializeReferenceFrame(sourceFrame, workingFrame);
            CandidateReferenceFrameAnalysis referenceAnalysis = new CandidateReferenceFrameAnalysis(
                    reconstructedFrame,
                    this.getFramePairAnalysis(sourceFrame, reconstructedFrame)
            );
            this.candidateReferenceFrameAnalysesByKey.put(cacheKey, referenceAnalysis);
            return referenceAnalysis;
        }

        @NotNull
        public CandidateArchiveCost getArchiveAppendCost(@NotNull ArchivePlanningState archiveState,
                                                         @NotNull PlannedCandidate candidate,
                                                         boolean introSequence) {
            ArchiveAppendKey cacheKey = new ArchiveAppendKey(archiveState, candidate, introSequence);
            CandidateArchiveCost cachedCost = this.archiveAppendCostsByKey.get(cacheKey);
            if (cachedCost != null) {
                return cachedCost;
            }

            CandidateArchiveCost computedCost = archiveState.appendCandidate(candidate, introSequence);
            this.archiveAppendCostsByKey.put(cacheKey, computedCost);
            return computedCost;
        }

        @NotNull
        protected FrameContentKey frameKey(@NotNull AfmaPixelFrame frame) {
            return this.frameKeysByIdentity.computeIfAbsent(frame, FrameContentKey::new);
        }

        public void closeUnusedCandidates(@NotNull BeamPlanningState retainedState) {
            Set<PlannedCandidate> retainedCandidates = Collections.newSetFromMap(new IdentityHashMap<>());
            for (PlanningStep planningStep : retainedState.stepsInOrder()) {
                PlannedCandidate candidate = planningStep.candidate();
                if (candidate != null) {
                    retainedCandidates.add(candidate);
                }
            }
            this.closeCandidates(retainedCandidates);
        }

        public void closeAllCandidates() {
            this.closeCandidates(Collections.emptySet());
        }

        protected void closeCandidates(@NotNull Set<PlannedCandidate> retainedCandidates) {
            Set<PlannedCandidate> closedCandidates = Collections.newSetFromMap(new IdentityHashMap<>());
            for (PlannedCandidate candidate : this.fullCandidatesByKey.values()) {
                closeCandidateIfUnused(candidate, retainedCandidates, closedCandidates);
            }
            for (PairCandidateSet candidateSet : this.pairCandidatesByKey.values()) {
                candidateSet.closeUnusedCandidates(retainedCandidates, closedCandidates);
            }
        }

        protected void closeCandidateIfUnused(@Nullable PlannedCandidate candidate, @NotNull Set<PlannedCandidate> retainedCandidates,
                                              @NotNull Set<PlannedCandidate> closedCandidates) {
            if ((candidate == null) || retainedCandidates.contains(candidate) || !closedCandidates.add(candidate)) {
                return;
            }
            candidate.closePayloads();
        }

    }

    protected static final class FrameContentKey {

        protected final int width;
        protected final int height;
        @NotNull
        protected final int[] pixels;
        protected final int pixelHash;

        protected FrameContentKey(@NotNull AfmaPixelFrame frame) {
            Objects.requireNonNull(frame);
            this.width = frame.getWidth();
            this.height = frame.getHeight();
            this.pixels = frame.getPixelsUnsafe();
            this.pixelHash = Arrays.hashCode(this.pixels);
        }

        @Override
        public int hashCode() {
            int result = Integer.hashCode(this.width);
            result = (31 * result) + Integer.hashCode(this.height);
            result = (31 * result) + this.pixelHash;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof FrameContentKey other)) {
                return false;
            }
            return this.width == other.width
                    && this.height == other.height
                    && this.pixelHash == other.pixelHash
                    && Arrays.equals(this.pixels, other.pixels);
        }

    }

    protected record FullCandidateKey(@NotNull FrameContentKey frameKey, boolean introSequence, int frameIndex,
                                      boolean allowPerceptual, @NotNull ReferenceBase referenceBase) {
    }

    protected record FramePairKey(@NotNull FrameContentKey previousFrameKey, @NotNull FrameContentKey nextFrameKey) {
    }

    protected record PairCandidateKey(@NotNull FrameContentKey previousFrameKey, @NotNull FrameContentKey workingFrameKey,
                                      boolean introSequence, int frameIndex) {
    }

    protected record NearLosslessMergeKey(@NotNull FrameContentKey previousFrameKey,
                                          @NotNull FrameContentKey currentFrameKey,
                                          int maxChannelDelta) {
    }

    protected record BlockInterPreparation(@NotNull AfmaRect regionBounds,
                                           @NotNull BlockInterRegionAnalysis regionAnalysis) {
    }

    protected final class PairCandidateSet {

        @NotNull
        protected final WindowCandidateCache windowCandidateCache;
        @NotNull
        protected final AfmaPixelFrame previousFrame;
        @NotNull
        protected final AfmaPixelFrame workingFrame;
        protected final boolean introSequence;
        protected final int frameIndex;
        @NotNull
        protected final AfmaEncodeOptions options;
        @NotNull
        protected final AfmaRectCopyDetector copyDetector;
        @NotNull
        protected final AfmaFramePairAnalysis pairAnalysis;
        @Nullable
        protected final AfmaRect deltaBounds;
        protected final long deltaCandidateArchiveLowerBoundBytes;
        protected final long residualDeltaCandidateArchiveLowerBoundBytes;
        protected final long sparseDeltaCandidateArchiveLowerBoundBytes;
        @Nullable
        protected PlannedCandidate deltaCandidate;
        protected boolean deltaCandidateResolved;
        @Nullable
        protected PlannedCandidate residualDeltaCandidate;
        protected boolean residualDeltaCandidateResolved;
        @Nullable
        protected PlannedCandidate sparseDeltaCandidate;
        protected boolean sparseDeltaCandidateResolved;
        @Nullable
        protected AfmaRectCopyDetector.Detection copyDetection;
        protected boolean copyDetectionResolved;
        protected long copyCandidateArchiveLowerBoundBytes = Long.MAX_VALUE;
        protected long copyResidualCandidateArchiveLowerBoundBytes = Long.MAX_VALUE;
        protected long copySparseCandidateArchiveLowerBoundBytes = Long.MAX_VALUE;
        @Nullable
        protected PlannedCandidate copyCandidate;
        protected boolean copyCandidateResolved;
        @Nullable
        protected PlannedCandidate copyResidualCandidate;
        protected boolean copyResidualCandidateResolved;
        @Nullable
        protected PlannedCandidate copySparseCandidate;
        protected boolean copySparseCandidateResolved;
        @Nullable
        protected AfmaRectCopyDetector.MultiDetection multiDetection;
        protected boolean multiDetectionResolved;
        protected long multiCopyCandidateArchiveLowerBoundBytes = Long.MAX_VALUE;
        protected long multiCopyResidualCandidateArchiveLowerBoundBytes = Long.MAX_VALUE;
        protected long multiCopySparseCandidateArchiveLowerBoundBytes = Long.MAX_VALUE;
        @Nullable
        protected AfmaPixelFrame multiCopyReferenceFrame;
        protected boolean multiCopyReferenceFrameResolved;
        @Nullable
        protected PlannedCandidate multiCopyCandidate;
        protected boolean multiCopyCandidateResolved;
        @Nullable
        protected PlannedCandidate multiCopyResidualCandidate;
        protected boolean multiCopyResidualCandidateResolved;
        @Nullable
        protected PlannedCandidate multiCopySparseCandidate;
        protected boolean multiCopySparseCandidateResolved;
        @Nullable
        protected AfmaRect blockInterRegionBounds;
        protected boolean blockInterRegionBoundsResolved;
        protected long blockInterCandidateArchiveLowerBoundBytes = Long.MAX_VALUE;
        @Nullable
        protected BlockInterPreparation blockInterPreparation;
        protected boolean blockInterPreparationResolved;
        @Nullable
        protected PlannedCandidate blockInterCandidate;
        protected boolean blockInterCandidateResolved;

        protected PairCandidateSet(@NotNull WindowCandidateCache windowCandidateCache,
                                   @NotNull AfmaPixelFrame previousFrame,
                                   @NotNull AfmaPixelFrame workingFrame,
                                   boolean introSequence, int frameIndex,
                                   @NotNull AfmaEncodeOptions options,
                                   @NotNull AfmaRectCopyDetector copyDetector) {
            this.windowCandidateCache = Objects.requireNonNull(windowCandidateCache);
            this.previousFrame = Objects.requireNonNull(previousFrame);
            this.workingFrame = Objects.requireNonNull(workingFrame);
            this.introSequence = introSequence;
            this.frameIndex = frameIndex;
            this.options = Objects.requireNonNull(options);
            this.copyDetector = Objects.requireNonNull(copyDetector);
            this.pairAnalysis = this.windowCandidateCache.getFramePairAnalysis(previousFrame, workingFrame);
            this.deltaBounds = this.pairAnalysis.differenceBounds();
            this.deltaCandidateArchiveLowerBoundBytes = (this.deltaBounds != null)
                    ? AfmaEncodePlanner.this.estimateDeltaCandidateArchiveLowerBoundBytes(this.deltaBounds)
                    : Long.MAX_VALUE;
            this.residualDeltaCandidateArchiveLowerBoundBytes = (this.deltaBounds != null)
                    ? AfmaEncodePlanner.this.estimateResidualDeltaCandidateArchiveLowerBoundBytes(this.deltaBounds)
                    : Long.MAX_VALUE;
            this.sparseDeltaCandidateArchiveLowerBoundBytes = (this.deltaBounds != null)
                    ? AfmaEncodePlanner.this.estimateSparseDeltaCandidateArchiveLowerBoundBytes(this.deltaBounds)
                    : Long.MAX_VALUE;
        }

        @Nullable
        public AfmaRect deltaBounds() {
            return this.deltaBounds;
        }

        public long deltaCandidateArchiveLowerBoundBytes() {
            return this.deltaCandidateArchiveLowerBoundBytes;
        }

        public long residualDeltaCandidateArchiveLowerBoundBytes() {
            return this.residualDeltaCandidateArchiveLowerBoundBytes;
        }

        public long sparseDeltaCandidateArchiveLowerBoundBytes() {
            return this.sparseDeltaCandidateArchiveLowerBoundBytes;
        }

        @Nullable
        public PlannedCandidate getOrCreateDeltaCandidate() throws IOException {
            if (this.deltaCandidateResolved || (this.deltaBounds == null)) {
                return this.deltaCandidate;
            }
            this.deltaCandidateResolved = true;
            this.deltaCandidate = AfmaEncodePlanner.this.createDeltaCandidate(
                    this.workingFrame,
                    this.introSequence,
                    this.frameIndex,
                    this.deltaBounds,
                    this.options
            );
            return this.deltaCandidate;
        }

        @Nullable
        public PlannedCandidate getOrCreateResidualDeltaCandidate() throws IOException {
            if (this.residualDeltaCandidateResolved || (this.deltaBounds == null)) {
                return this.residualDeltaCandidate;
            }
            this.residualDeltaCandidateResolved = true;
            this.residualDeltaCandidate = AfmaEncodePlanner.this.createResidualDeltaCandidate(
                    this.previousFrame,
                    this.workingFrame,
                    this.introSequence,
                    this.frameIndex,
                    this.deltaBounds
            );
            return this.residualDeltaCandidate;
        }

        @Nullable
        public PlannedCandidate getOrCreateSparseDeltaCandidate() throws IOException {
            if (this.sparseDeltaCandidateResolved || (this.deltaBounds == null)) {
                return this.sparseDeltaCandidate;
            }
            this.sparseDeltaCandidateResolved = true;
            this.sparseDeltaCandidate = AfmaEncodePlanner.this.createSparseDeltaCandidate(
                    this.previousFrame,
                    this.workingFrame,
                    this.introSequence,
                    this.frameIndex,
                    this.deltaBounds
            );
            return this.sparseDeltaCandidate;
        }

        @Nullable
        public AfmaRectCopyDetector.Detection copyDetection() {
            if (!this.options.isRectCopyEnabled()) {
                return null;
            }
            if (this.copyDetectionResolved) {
                return this.copyDetection;
            }

            this.copyDetectionResolved = true;
            this.copyDetection = this.copyDetector.detect(this.pairAnalysis);
            if (this.copyDetection != null) {
                this.copyCandidateArchiveLowerBoundBytes = AfmaEncodePlanner.this.estimateCopyCandidateArchiveLowerBoundBytes(this.copyDetection);
                AfmaRect patchBounds = this.copyDetection.patchBounds();
                if ((patchBounds != null) && (patchBounds.area() > 0L)) {
                    this.copyResidualCandidateArchiveLowerBoundBytes = AfmaEncodePlanner.this.estimateCopyResidualCandidateArchiveLowerBoundBytes(this.copyDetection);
                    this.copySparseCandidateArchiveLowerBoundBytes = AfmaEncodePlanner.this.estimateCopySparseCandidateArchiveLowerBoundBytes(this.copyDetection);
                }
            }
            return this.copyDetection;
        }

        public long copyCandidateArchiveLowerBoundBytes() {
            this.copyDetection();
            return this.copyCandidateArchiveLowerBoundBytes;
        }

        public long copyResidualCandidateArchiveLowerBoundBytes() {
            this.copyDetection();
            return this.copyResidualCandidateArchiveLowerBoundBytes;
        }

        public long copySparseCandidateArchiveLowerBoundBytes() {
            this.copyDetection();
            return this.copySparseCandidateArchiveLowerBoundBytes;
        }

        @Nullable
        public PlannedCandidate getOrCreateCopyCandidate() throws IOException {
            AfmaRectCopyDetector.Detection detection = this.copyDetection();
            if (this.copyCandidateResolved || (detection == null)) {
                return this.copyCandidate;
            }
            this.copyCandidateResolved = true;
            this.copyCandidate = AfmaEncodePlanner.this.createCopyCandidate(
                    this.workingFrame,
                    this.introSequence,
                    this.frameIndex,
                    detection,
                    this.options
            );
            return this.copyCandidate;
        }

        @Nullable
        public PlannedCandidate getOrCreateCopyResidualCandidate() throws IOException {
            AfmaRectCopyDetector.Detection detection = this.copyDetection();
            if (this.copyResidualCandidateResolved || (detection == null)) {
                return this.copyResidualCandidate;
            }
            this.copyResidualCandidateResolved = true;
            this.copyResidualCandidate = AfmaEncodePlanner.this.createCopyResidualCandidate(
                    this.previousFrame,
                    this.workingFrame,
                    this.introSequence,
                    this.frameIndex,
                    detection
            );
            return this.copyResidualCandidate;
        }

        @Nullable
        public PlannedCandidate getOrCreateCopySparseCandidate() throws IOException {
            AfmaRectCopyDetector.Detection detection = this.copyDetection();
            if (this.copySparseCandidateResolved || (detection == null)) {
                return this.copySparseCandidate;
            }
            this.copySparseCandidateResolved = true;
            this.copySparseCandidate = AfmaEncodePlanner.this.createCopySparseCandidate(
                    this.previousFrame,
                    this.workingFrame,
                    this.introSequence,
                    this.frameIndex,
                    detection
            );
            return this.copySparseCandidate;
        }

        @Nullable
        public AfmaRectCopyDetector.MultiDetection multiDetection() {
            if (!this.options.isRectCopyEnabled()) {
                return null;
            }
            if (this.multiDetectionResolved) {
                return this.multiDetection;
            }

            this.multiDetectionResolved = true;
            this.multiDetection = this.copyDetector.detectMulti(this.pairAnalysis, this.copyDetection());
            if (this.multiDetection != null) {
                this.multiCopyCandidateArchiveLowerBoundBytes = AfmaEncodePlanner.this.estimateMultiCopyCandidateArchiveLowerBoundBytes(this.multiDetection);
                AfmaRect patchBounds = this.multiDetection.patchBounds();
                if ((patchBounds != null) && (patchBounds.area() > 0L)) {
                    this.multiCopyResidualCandidateArchiveLowerBoundBytes = AfmaEncodePlanner.this.estimateMultiCopyResidualCandidateArchiveLowerBoundBytes(this.multiDetection);
                    this.multiCopySparseCandidateArchiveLowerBoundBytes = AfmaEncodePlanner.this.estimateMultiCopySparseCandidateArchiveLowerBoundBytes(this.multiDetection);
                }
            }
            return this.multiDetection;
        }

        public long multiCopyCandidateArchiveLowerBoundBytes() {
            this.multiDetection();
            return this.multiCopyCandidateArchiveLowerBoundBytes;
        }

        public long multiCopyResidualCandidateArchiveLowerBoundBytes() {
            this.multiDetection();
            return this.multiCopyResidualCandidateArchiveLowerBoundBytes;
        }

        public long multiCopySparseCandidateArchiveLowerBoundBytes() {
            this.multiDetection();
            return this.multiCopySparseCandidateArchiveLowerBoundBytes;
        }

        @Nullable
        protected AfmaPixelFrame multiCopyReferenceFrame() {
            AfmaRectCopyDetector.MultiDetection detection = this.multiDetection();
            if (this.multiCopyReferenceFrameResolved || (detection == null)) {
                return this.multiCopyReferenceFrame;
            }
            this.multiCopyReferenceFrameResolved = true;
            this.multiCopyReferenceFrame = AfmaEncodePlanner.this.buildMultiCopyReferenceFrame(this.previousFrame, detection.multiCopy());
            return this.multiCopyReferenceFrame;
        }

        @Nullable
        public PlannedCandidate getOrCreateMultiCopyCandidate() throws IOException {
            AfmaRectCopyDetector.MultiDetection detection = this.multiDetection();
            if (this.multiCopyCandidateResolved || (detection == null)) {
                return this.multiCopyCandidate;
            }
            this.multiCopyCandidateResolved = true;
            this.multiCopyCandidate = AfmaEncodePlanner.this.createMultiCopyCandidate(
                    this.workingFrame,
                    this.introSequence,
                    this.frameIndex,
                    detection,
                    this.options
            );
            return this.multiCopyCandidate;
        }

        @Nullable
        public PlannedCandidate getOrCreateMultiCopyResidualCandidate() throws IOException {
            AfmaRectCopyDetector.MultiDetection detection = this.multiDetection();
            AfmaPixelFrame copiedReferenceFrame = this.multiCopyReferenceFrame();
            if (this.multiCopyResidualCandidateResolved || (detection == null) || (copiedReferenceFrame == null)) {
                return this.multiCopyResidualCandidate;
            }
            this.multiCopyResidualCandidateResolved = true;
            this.multiCopyResidualCandidate = AfmaEncodePlanner.this.createMultiCopyResidualCandidate(
                    copiedReferenceFrame,
                    this.workingFrame,
                    this.introSequence,
                    this.frameIndex,
                    detection
            );
            return this.multiCopyResidualCandidate;
        }

        @Nullable
        public PlannedCandidate getOrCreateMultiCopySparseCandidate() throws IOException {
            AfmaRectCopyDetector.MultiDetection detection = this.multiDetection();
            AfmaPixelFrame copiedReferenceFrame = this.multiCopyReferenceFrame();
            if (this.multiCopySparseCandidateResolved || (detection == null) || (copiedReferenceFrame == null)) {
                return this.multiCopySparseCandidate;
            }
            this.multiCopySparseCandidateResolved = true;
            this.multiCopySparseCandidate = AfmaEncodePlanner.this.createMultiCopySparseCandidate(
                    copiedReferenceFrame,
                    this.workingFrame,
                    this.introSequence,
                    this.frameIndex,
                    detection
            );
            return this.multiCopySparseCandidate;
        }

        @Nullable
        public AfmaRect blockInterRegionBounds() {
            if (this.blockInterRegionBoundsResolved || (this.deltaBounds == null)) {
                return this.blockInterRegionBounds;
            }
            this.blockInterRegionBoundsResolved = true;
            this.blockInterRegionBounds = AfmaEncodePlanner.this.alignBoundsToTileGrid(
                    this.deltaBounds,
                    BLOCK_INTER_TILE_SIZE,
                    this.workingFrame.getWidth(),
                    this.workingFrame.getHeight()
            );
            if (this.blockInterRegionBounds != null) {
                this.blockInterCandidateArchiveLowerBoundBytes = AfmaEncodePlanner.this.estimateBlockInterDescriptorBytes(this.blockInterRegionBounds);
            }
            return this.blockInterRegionBounds;
        }

        public long blockInterCandidateArchiveLowerBoundBytes() {
            this.blockInterRegionBounds();
            return this.blockInterCandidateArchiveLowerBoundBytes;
        }

        @Nullable
        protected BlockInterPreparation blockInterPreparation() {
            if (this.blockInterPreparationResolved) {
                return this.blockInterPreparation;
            }

            AfmaRect regionBounds = this.blockInterRegionBounds();
            this.blockInterPreparationResolved = true;
            if (regionBounds == null) {
                return null;
            }

            int tileCountX = AfmaBlockInterPayloadHelper.tileCount(regionBounds.width(), BLOCK_INTER_TILE_SIZE);
            int tileCountY = AfmaBlockInterPayloadHelper.tileCount(regionBounds.height(), BLOCK_INTER_TILE_SIZE);
            if ((tileCountX <= 0) || (tileCountY <= 0)) {
                return null;
            }

            BlockInterRegionAnalysis regionAnalysis = AfmaEncodePlanner.this.analyzeBlockInterRegion(
                    this.previousFrame,
                    this.workingFrame,
                    regionBounds,
                    tileCountX,
                    tileCountY
            );
            this.blockInterPreparation = new BlockInterPreparation(regionBounds, regionAnalysis);
            return this.blockInterPreparation;
        }

        @Nullable
        public PlannedCandidate getOrCreateBlockInterCandidate(@Nullable PlannedCandidate bestBaselineCandidate) throws IOException {
            if (this.blockInterCandidateResolved) {
                return this.blockInterCandidate;
            }

            BlockInterPreparation blockInterPreparation = this.blockInterPreparation();
            if (blockInterPreparation == null) {
                this.blockInterCandidateResolved = true;
                return null;
            }
            if (!AfmaEncodePlanner.this.shouldAttemptBlockInter(
                    blockInterPreparation.regionBounds(),
                    blockInterPreparation.regionAnalysis(),
                    bestBaselineCandidate
            )) {
                // Keep baseline-driven misses lazy so a different archive state can still request exact block_inter realization.
                return null;
            }

            this.blockInterCandidateResolved = true;
            this.blockInterCandidate = AfmaEncodePlanner.this.createBlockInterCandidate(
                    this.pairAnalysis,
                    this.introSequence,
                    this.frameIndex,
                    blockInterPreparation.regionBounds(),
                    blockInterPreparation.regionAnalysis(),
                    this.copyDetector,
                    this.copyDetection(),
                    this.multiDetection()
            );
            return this.blockInterCandidate;
        }

        protected void closeUnusedCandidates(@NotNull Set<PlannedCandidate> retainedCandidates,
                                             @NotNull Set<PlannedCandidate> closedCandidates) {
            this.windowCandidateCache.closeCandidateIfUnused(this.deltaCandidate, retainedCandidates, closedCandidates);
            this.windowCandidateCache.closeCandidateIfUnused(this.residualDeltaCandidate, retainedCandidates, closedCandidates);
            this.windowCandidateCache.closeCandidateIfUnused(this.sparseDeltaCandidate, retainedCandidates, closedCandidates);
            this.windowCandidateCache.closeCandidateIfUnused(this.copyCandidate, retainedCandidates, closedCandidates);
            this.windowCandidateCache.closeCandidateIfUnused(this.copyResidualCandidate, retainedCandidates, closedCandidates);
            this.windowCandidateCache.closeCandidateIfUnused(this.copySparseCandidate, retainedCandidates, closedCandidates);
            this.windowCandidateCache.closeCandidateIfUnused(this.multiCopyCandidate, retainedCandidates, closedCandidates);
            this.windowCandidateCache.closeCandidateIfUnused(this.multiCopyResidualCandidate, retainedCandidates, closedCandidates);
            this.windowCandidateCache.closeCandidateIfUnused(this.multiCopySparseCandidate, retainedCandidates, closedCandidates);
            this.windowCandidateCache.closeCandidateIfUnused(this.blockInterCandidate, retainedCandidates, closedCandidates);
        }
    }

    protected record CandidateReferenceFrameAnalysis(@NotNull AfmaPixelFrame reconstructedFrame,
                                                     @NotNull AfmaFramePairAnalysis sourcePairAnalysis) {
    }

    protected record CandidateReferenceFrameAnalysisKey(@NotNull PlannedCandidate candidate,
                                                        @NotNull FrameContentKey sourceFrameKey,
                                                        @NotNull FrameContentKey workingFrameKey) {
    }

    protected record ArchiveAppendKey(@NotNull ArchivePlanningState archiveState,
                                      @NotNull PlannedCandidate candidate,
                                      boolean introSequence) {
    }

    protected enum DecodeCost {
        SAME,
        FULL,
        DELTA,
        COPY_RECT_PATCH,
        MULTI_COPY_PATCH,
        RESIDUAL_DELTA_RECT,
        COPY_RECT_RESIDUAL_PATCH,
        MULTI_COPY_RESIDUAL_PATCH,
        SPARSE_DELTA_RECT,
        COPY_RECT_SPARSE_PATCH,
        MULTI_COPY_SPARSE_PATCH,
        BLOCK_INTER
    }

    protected enum PayloadKind {
        BIN_INTRA,
        RAW
    }

    protected enum ReferenceBase {
        SOURCE_FRAME,
        WORKING_FRAME
    }

    protected static class PlannedCandidate {

        @NotNull
        protected final AfmaFrameDescriptor descriptor;
        @Nullable
        protected final String primaryPayloadPath;
        @Nullable
        protected final DeferredPayload primaryPayload;
        @Nullable
        protected final PayloadKind primaryPayloadKind;
        protected final boolean primaryPayloadReusedFromSource;
        protected final long estimatedPrimaryArchiveBytes;
        @Nullable
        protected final String patchPayloadPath;
        @Nullable
        protected final DeferredPayload patchPayload;
        @Nullable
        protected final PayloadKind patchPayloadKind;
        protected final boolean patchPayloadReusedFromSource;
        protected final long estimatedPatchArchiveBytes;
        @NotNull
        protected final ReferenceBase referenceBase;
        @Nullable
        protected final AfmaRect referencePatchBounds;
        @Nullable
        protected int[] referencePatchPixels;
        protected boolean referencePixelsDiscarded;
        @NotNull
        protected final DecodeCost decodeCost;
        protected final int complexityScore;

        protected PlannedCandidate(@NotNull AfmaFrameDescriptor descriptor,
                                   @Nullable String primaryPayloadPath, @Nullable DeferredPayload primaryPayload, @Nullable PayloadKind primaryPayloadKind, boolean primaryPayloadReusedFromSource,
                                   @Nullable String patchPayloadPath, @Nullable DeferredPayload patchPayload, @Nullable PayloadKind patchPayloadKind, boolean patchPayloadReusedFromSource,
                                   @NotNull ReferenceBase referenceBase, @Nullable AfmaRect referencePatchBounds, @Nullable int[] referencePatchPixels,
                                   @NotNull DecodeCost decodeCost, int complexityScore) {
            this.descriptor = descriptor;
            this.primaryPayloadPath = primaryPayloadPath;
            this.primaryPayload = primaryPayload;
            this.primaryPayloadKind = primaryPayloadKind;
            this.primaryPayloadReusedFromSource = primaryPayloadReusedFromSource;
            this.estimatedPrimaryArchiveBytes = estimatePayloadArchiveBytes(primaryPayload, primaryPayloadKind);
            this.patchPayloadPath = patchPayloadPath;
            this.patchPayload = patchPayload;
            this.patchPayloadKind = patchPayloadKind;
            this.patchPayloadReusedFromSource = patchPayloadReusedFromSource;
            this.estimatedPatchArchiveBytes = estimatePayloadArchiveBytes(patchPayload, patchPayloadKind);
            this.referenceBase = referenceBase;
            this.referencePatchBounds = referencePatchBounds;
            this.referencePatchPixels = referencePatchPixels;
            this.referencePixelsDiscarded = false;
            this.decodeCost = decodeCost;
            this.complexityScore = complexityScore;
        }

        @NotNull
        public static PlannedCandidate same() {
            return new PlannedCandidate(AfmaFrameDescriptor.same(), null, null, null, false, null, null, null, false,
                    ReferenceBase.WORKING_FRAME, null, null, DecodeCost.SAME, 0);
        }

        @NotNull
        public AfmaFrameDescriptor descriptor() {
            return this.descriptor;
        }

        public long totalBytes() {
            long total = 0L;
            if (this.primaryPayload != null) total += this.primaryPayload.length();
            if (this.patchPayload != null) total += this.patchPayload.length();
            return total;
        }

        @NotNull
        public AfmaPixelFrame materializeReferenceFrame(@NotNull AfmaPixelFrame sourceFrame, @NotNull AfmaPixelFrame workingFrame) {
            if (this.referencePixelsDiscarded) {
                throw new IllegalStateException("AFMA planner reference pixels were already discarded for this candidate");
            }

            AfmaPixelFrame baseFrame = (this.referenceBase == ReferenceBase.SOURCE_FRAME) ? sourceFrame : workingFrame;
            if ((this.referencePatchBounds == null) || (this.referencePatchPixels == null)) {
                return baseFrame;
            }

            AfmaRect patchBounds = this.referencePatchBounds;
            if ((patchBounds.x() == 0) && (patchBounds.y() == 0)
                    && (patchBounds.width() == baseFrame.getWidth()) && (patchBounds.height() == baseFrame.getHeight())) {
                return new AfmaPixelFrame(baseFrame.getWidth(), baseFrame.getHeight(), Arrays.copyOf(this.referencePatchPixels, this.referencePatchPixels.length));
            }

            int[] referencePixels = baseFrame.copyPixels();
            int patchOffset = 0;
            for (int localY = 0; localY < patchBounds.height(); localY++) {
                int rowOffset = (patchBounds.y() + localY) * baseFrame.getWidth();
                for (int localX = 0; localX < patchBounds.width(); localX++) {
                    referencePixels[rowOffset + patchBounds.x() + localX] = this.referencePatchPixels[patchOffset++];
                }
            }
            return new AfmaPixelFrame(baseFrame.getWidth(), baseFrame.getHeight(), referencePixels);
        }

        public boolean isExactRelativeToSourceFrame(@NotNull AfmaPixelFrame sourceFrame, @NotNull AfmaPixelFrame workingFrame) {
            if (this.referencePixelsDiscarded) {
                throw new IllegalStateException("AFMA planner reference pixels were already discarded for this candidate");
            }
            if ((this.referencePatchBounds != null) || (this.referencePatchPixels != null)) {
                return false;
            }
            if (this.referenceBase == ReferenceBase.SOURCE_FRAME) {
                return true;
            }
            return sourceFrame == workingFrame;
        }

        public void discardReferencePixels() {
            if (this.referencePatchPixels == null) {
                return;
            }
            this.referencePatchPixels = null;
            this.referencePixelsDiscarded = true;
        }

        public long estimatedArchiveBytes(@NotNull Map<String, String> payloadPathsByFingerprint) {
            return this.estimatedPayloadBytes(payloadPathsByFingerprint) + this.estimateDescriptorBytes();
        }

        protected long estimatedPayloadBytes(@NotNull Map<String, String> payloadPathsByFingerprint) {
            long total = 0L;
            total += this.estimatedPayloadBytes(this.primaryPayloadPath, this.primaryPayload, payloadPathsByFingerprint);
            total += this.estimatedPayloadBytes(this.patchPayloadPath, this.patchPayload, payloadPathsByFingerprint);
            return total;
        }

        protected long estimatedPayloadBytes(@Nullable String path, @Nullable DeferredPayload payload, @NotNull Map<String, String> payloadPathsByFingerprint) {
            if ((path == null) || (payload == null)) {
                return 0L;
            }
            String fingerprint = payload.fingerprint();
            String existingPath = payloadPathsByFingerprint.get(fingerprint);
            if ((existingPath != null) && !existingPath.equals(path)) {
                return 0L;
            }
            if (Objects.equals(path, this.primaryPayloadPath) && (payload == this.primaryPayload)) {
                return this.estimatedPrimaryArchiveBytes;
            }
            if (Objects.equals(path, this.patchPayloadPath) && (payload == this.patchPayload)) {
                return this.estimatedPatchArchiveBytes;
            }
            return estimatePayloadArchiveBytes(payload, null);
        }

        protected int estimateDescriptorBytes() {
            AfmaFrameOperationType type = this.descriptor.getType();
            if (type == null) {
                return 1;
            }

            int bytes = 1;
            bytes += switch (type) {
                case FULL -> estimatedPayloadIdBytes();
                case DELTA_RECT -> estimatedPayloadIdBytes()
                        + estimateVarIntBytes(this.descriptor.getX())
                        + estimateVarIntBytes(this.descriptor.getY())
                        + estimateVarIntBytes(this.descriptor.getWidth())
                        + estimateVarIntBytes(this.descriptor.getHeight());
                case RESIDUAL_DELTA_RECT -> estimatedPayloadIdBytes()
                        + estimateVarIntBytes(this.descriptor.getX())
                        + estimateVarIntBytes(this.descriptor.getY())
                        + estimateVarIntBytes(this.descriptor.getWidth())
                        + estimateVarIntBytes(this.descriptor.getHeight())
                        + estimateVarIntBytes(Objects.requireNonNull(this.descriptor.getResidual()).getChannels())
                        + 2
                        + estimateVarIntBytes(Objects.requireNonNull(this.descriptor.getResidual()).getAlphaChangedPixelCount());
                case SPARSE_DELTA_RECT -> (2 * estimatedPayloadIdBytes())
                        + estimateVarIntBytes(this.descriptor.getX())
                        + estimateVarIntBytes(this.descriptor.getY())
                        + estimateVarIntBytes(this.descriptor.getWidth())
                        + estimateVarIntBytes(this.descriptor.getHeight())
                        + estimateVarIntBytes(Objects.requireNonNull(this.descriptor.getSparse()).getChangedPixelCount())
                        + estimateVarIntBytes(Objects.requireNonNull(this.descriptor.getSparse()).getChannels())
                        + 3
                        + estimateVarIntBytes(Objects.requireNonNull(this.descriptor.getSparse()).getAlphaChangedPixelCount());
                case SAME -> 0;
                case COPY_RECT_PATCH -> {
                    int copyBytes = estimateCopyRectBytes(Objects.requireNonNull(this.descriptor.getCopy()));
                    AfmaPatchRegion patch = this.descriptor.getPatch();
                    if (patch == null) {
                        yield copyBytes + 1;
                    }
                    yield copyBytes + 1 + estimatedPayloadIdBytes()
                            + estimateVarIntBytes(patch.getX())
                            + estimateVarIntBytes(patch.getY())
                            + estimateVarIntBytes(patch.getWidth())
                            + estimateVarIntBytes(patch.getHeight());
                }
                case MULTI_COPY_PATCH -> {
                    int copyBytes = estimateMultiCopyBytes(Objects.requireNonNull(this.descriptor.getMultiCopy()));
                    AfmaPatchRegion patch = this.descriptor.getPatch();
                    if (patch == null) {
                        yield copyBytes + 1;
                    }
                    yield copyBytes + 1 + estimatedPayloadIdBytes()
                            + estimateVarIntBytes(patch.getX())
                            + estimateVarIntBytes(patch.getY())
                            + estimateVarIntBytes(patch.getWidth())
                            + estimateVarIntBytes(patch.getHeight());
                }
                case COPY_RECT_RESIDUAL_PATCH -> estimateCopyRectBytes(Objects.requireNonNull(this.descriptor.getCopy()))
                        + estimatedPayloadIdBytes()
                        + estimateVarIntBytes(this.descriptor.getX())
                        + estimateVarIntBytes(this.descriptor.getY())
                        + estimateVarIntBytes(this.descriptor.getWidth())
                        + estimateVarIntBytes(this.descriptor.getHeight())
                        + estimateVarIntBytes(Objects.requireNonNull(this.descriptor.getResidual()).getChannels())
                        + 2
                        + estimateVarIntBytes(Objects.requireNonNull(this.descriptor.getResidual()).getAlphaChangedPixelCount());
                case MULTI_COPY_RESIDUAL_PATCH -> estimateMultiCopyBytes(Objects.requireNonNull(this.descriptor.getMultiCopy()))
                        + estimatedPayloadIdBytes()
                        + estimateVarIntBytes(this.descriptor.getX())
                        + estimateVarIntBytes(this.descriptor.getY())
                        + estimateVarIntBytes(this.descriptor.getWidth())
                        + estimateVarIntBytes(this.descriptor.getHeight())
                        + estimateVarIntBytes(Objects.requireNonNull(this.descriptor.getResidual()).getChannels())
                        + 2
                        + estimateVarIntBytes(Objects.requireNonNull(this.descriptor.getResidual()).getAlphaChangedPixelCount());
                case COPY_RECT_SPARSE_PATCH -> estimateCopyRectBytes(Objects.requireNonNull(this.descriptor.getCopy()))
                        + (2 * estimatedPayloadIdBytes())
                        + estimateVarIntBytes(this.descriptor.getX())
                        + estimateVarIntBytes(this.descriptor.getY())
                        + estimateVarIntBytes(this.descriptor.getWidth())
                        + estimateVarIntBytes(this.descriptor.getHeight())
                        + estimateVarIntBytes(Objects.requireNonNull(this.descriptor.getSparse()).getChangedPixelCount())
                        + estimateVarIntBytes(Objects.requireNonNull(this.descriptor.getSparse()).getChannels())
                        + 3
                        + estimateVarIntBytes(Objects.requireNonNull(this.descriptor.getSparse()).getAlphaChangedPixelCount());
                case MULTI_COPY_SPARSE_PATCH -> estimateMultiCopyBytes(Objects.requireNonNull(this.descriptor.getMultiCopy()))
                        + (2 * estimatedPayloadIdBytes())
                        + estimateVarIntBytes(this.descriptor.getX())
                        + estimateVarIntBytes(this.descriptor.getY())
                        + estimateVarIntBytes(this.descriptor.getWidth())
                        + estimateVarIntBytes(this.descriptor.getHeight())
                        + estimateVarIntBytes(Objects.requireNonNull(this.descriptor.getSparse()).getChangedPixelCount())
                        + estimateVarIntBytes(Objects.requireNonNull(this.descriptor.getSparse()).getChannels())
                        + 3
                        + estimateVarIntBytes(Objects.requireNonNull(this.descriptor.getSparse()).getAlphaChangedPixelCount());
                case BLOCK_INTER -> estimatedPayloadIdBytes()
                        + estimateVarIntBytes(this.descriptor.getX())
                        + estimateVarIntBytes(this.descriptor.getY())
                        + estimateVarIntBytes(this.descriptor.getWidth())
                        + estimateVarIntBytes(this.descriptor.getHeight())
                        + estimateVarIntBytes(Objects.requireNonNull(this.descriptor.getBlockInter()).getTileSize());
            };
            return bytes;
        }

        protected int estimatedPayloadIdBytes() {
            return 2;
        }

        protected int estimateCopyRectBytes(@NotNull AfmaCopyRect copyRect) {
            return estimateVarIntBytes(copyRect.getSrcX())
                    + estimateVarIntBytes(copyRect.getSrcY())
                    + estimateVarIntBytes(copyRect.getDstX())
                    + estimateVarIntBytes(copyRect.getDstY())
                    + estimateVarIntBytes(copyRect.getWidth())
                    + estimateVarIntBytes(copyRect.getHeight());
        }

        protected int estimateMultiCopyBytes(@NotNull AfmaMultiCopy multiCopy) {
            List<AfmaCopyRect> copyRects = multiCopy.getCopyRects();
            int bytes = estimateVarIntBytes(copyRects.size() - 1);
            AfmaCopyRect previousRect = null;
            for (AfmaCopyRect copyRect : copyRects) {
                AfmaCopyRect currentRect = Objects.requireNonNull(copyRect);
                bytes += 1;
                if (previousRect == null) {
                    bytes += estimateCopyRectBytes(currentRect);
                } else {
                    bytes += estimateSignedVarIntBytes(currentRect.getSrcX() - previousRect.getSrcX());
                    bytes += estimateSignedVarIntBytes(currentRect.getSrcY() - previousRect.getSrcY());
                    bytes += estimateSignedVarIntBytes(currentRect.getDstX() - previousRect.getDstX());
                    bytes += estimateSignedVarIntBytes(currentRect.getDstY() - previousRect.getDstY());
                    if (currentRect.getWidth() != previousRect.getWidth()) {
                        bytes += estimateVarIntBytes(currentRect.getWidth());
                    }
                    if (currentRect.getHeight() != previousRect.getHeight()) {
                        bytes += estimateVarIntBytes(currentRect.getHeight());
                    }
                }
                previousRect = currentRect;
            }
            return bytes;
        }

        protected int estimateVarIntBytes(int value) {
            if ((value & ~0x7F) == 0) {
                return 1;
            }
            if ((value & ~0x3FFF) == 0) {
                return 2;
            }
            if ((value & ~0x1FFFFF) == 0) {
                return 3;
            }
            if ((value & ~0xFFFFFFF) == 0) {
                return 4;
            }
            return 5;
        }

        protected int estimateSignedVarIntBytes(int value) {
            int zigZag = (value << 1) ^ (value >> 31);
            return estimateVarIntBytes(zigZag);
        }

        public boolean isBetterThan(@NotNull PlannedCandidate other, @NotNull Map<String, String> payloadPathsByFingerprint) {
            long archiveBytes = this.estimatedArchiveBytes(payloadPathsByFingerprint);
            long otherArchiveBytes = other.estimatedArchiveBytes(payloadPathsByFingerprint);
            if (archiveBytes != otherArchiveBytes) {
                return archiveBytes < otherArchiveBytes;
            }
            if (this.decodeCost != other.decodeCost) {
                return this.decodeCost.ordinal() < other.decodeCost.ordinal();
            }
            return this.complexityScore < other.complexityScore;
        }

        @NotNull
        public AfmaFrameDescriptor internPayloads(@NotNull Map<String, AfmaStoredPayload> payloads,
                                                  @NotNull Map<String, String> payloadPathsByFingerprint) throws IOException {
            String resolvedPrimaryPath = this.internPayload(this.primaryPayloadPath, this.primaryPayload, payloads, payloadPathsByFingerprint);
            String resolvedPatchPath = this.internPayload(this.patchPayloadPath, this.patchPayload, payloads, payloadPathsByFingerprint);

            AfmaFrameDescriptor finalizedDescriptor = this.descriptor;
            if (!Objects.equals(resolvedPrimaryPath, this.primaryPayloadPath)) {
                finalizedDescriptor = finalizedDescriptor.withPrimaryPath(resolvedPrimaryPath);
            }
            if (!Objects.equals(resolvedPatchPath, this.patchPayloadPath)) {
                finalizedDescriptor = finalizedDescriptor.withPatchPath(resolvedPatchPath);
            }
            return finalizedDescriptor;
        }

        @Nullable
        protected String internPayload(@Nullable String payloadPath, @Nullable DeferredPayload payload,
                                       @NotNull Map<String, AfmaStoredPayload> payloads,
                                       @NotNull Map<String, String> payloadPathsByFingerprint) throws IOException {
            if ((payloadPath == null) || (payload == null)) {
                return payloadPath;
            }

            String fingerprint = payload.fingerprint();
            String existingPath = payloadPathsByFingerprint.get(fingerprint);
            if ((existingPath != null) && !existingPath.equals(payloadPath)) {
                AfmaStoredPayload materializedPayload = payload.peekMaterializedPayload();
                if ((materializedPayload == null) || (payloads.get(existingPath) != materializedPayload)) {
                    payload.close();
                }
                return existingPath;
            }

            payloads.put(payloadPath, payload.materialize());
            payloadPathsByFingerprint.put(fingerprint, payloadPath);
            return payloadPath;
        }

        public void closePayloads() {
            if (this.primaryPayload != null) {
                this.primaryPayload.close();
            }
            if (this.patchPayload != null) {
                this.patchPayload.close();
            }
        }

        protected static long estimatePayloadArchiveBytes(@Nullable DeferredPayload payload, @Nullable PayloadKind payloadKind) {
            return (payload != null) ? payload.estimatedArchiveBytes() : 0L;
        }

    }

    protected static final class DeferredPayload implements AutoCloseable {

        @NotNull
        protected final AfmaStoredPayload.PayloadSummary payloadSummary;
        @Nullable
        protected AfmaStoredPayload materializedPayload;
        @Nullable
        protected byte[] payloadBytes;
        @Nullable
        protected AfmaStoredPayload.Writer payloadWriter;
        protected boolean closed = false;

        protected DeferredPayload(@NotNull AfmaStoredPayload.PayloadSummary payloadSummary,
                                  @Nullable AfmaStoredPayload materializedPayload,
                                  @Nullable byte[] payloadBytes,
                                  @Nullable AfmaStoredPayload.Writer payloadWriter) {
            this.payloadSummary = Objects.requireNonNull(payloadSummary);
            this.materializedPayload = materializedPayload;
            this.payloadBytes = payloadBytes;
            this.payloadWriter = payloadWriter;
        }

        @NotNull
        public static DeferredPayload fromBytes(@NotNull byte[] payloadBytes) {
            Objects.requireNonNull(payloadBytes);
            return new DeferredPayload(AfmaStoredPayload.summarize(payloadBytes), null, payloadBytes, null);
        }

        @NotNull
        public static DeferredPayload fromBytes(@NotNull AfmaStoredPayload.PayloadSummary payloadSummary, @NotNull byte[] payloadBytes) {
            Objects.requireNonNull(payloadSummary);
            Objects.requireNonNull(payloadBytes);
            if (payloadSummary.length() != payloadBytes.length) {
                throw new IllegalArgumentException("AFMA deferred payload summary length does not match payload bytes");
            }
            return new DeferredPayload(payloadSummary, null, payloadBytes, null);
        }

        @NotNull
        public static DeferredPayload fromWriter(@NotNull AfmaStoredPayload.PayloadSummary payloadSummary,
                                                 @NotNull AfmaStoredPayload.Writer payloadWriter) {
            return new DeferredPayload(payloadSummary, null, null, payloadWriter);
        }

        public int length() {
            this.ensureOpen();
            return this.payloadSummary.length();
        }

        public boolean isEmpty() {
            return this.length() <= 0;
        }

        public long estimatedArchiveBytes() {
            this.ensureOpen();
            return this.payloadSummary.estimatedArchiveBytes();
        }

        @NotNull
        public String fingerprint() {
            this.ensureOpen();
            return this.payloadSummary.fingerprint();
        }

        public long estimateChunkCompressionDelta(@NotNull byte[] previousTail) {
            this.ensureOpen();
            if (this.payloadSummary.length() <= 0) {
                return 0L;
            }
            if (previousTail.length == 0) {
                return this.payloadSummary.estimatedArchiveBytes();
            }
            if (this.materializedPayload != null) {
                return AfmaChunkedPayloadHelper.estimateChunkCompressionDelta(previousTail, this.materializedPayload);
            }
            if (this.payloadBytes != null) {
                return AfmaChunkedPayloadHelper.estimateChunkCompressionDelta(previousTail, this.payloadBytes);
            }
            if (this.payloadWriter != null) {
                return AfmaChunkedPayloadHelper.estimateChunkCompressionDelta(previousTail, this.payloadSummary, this.payloadWriter);
            }
            return 0L;
        }

        @NotNull
        public byte[] appendDeflateTail(@NotNull byte[] currentTail) {
            this.ensureOpen();
            if (this.materializedPayload != null) {
                return AfmaChunkedPayloadHelper.appendDeflateTail(currentTail, this.materializedPayload);
            }
            return AfmaChunkedPayloadHelper.appendDeflateTail(currentTail, this.payloadSummary);
        }

        @Nullable
        public AfmaStoredPayload peekMaterializedPayload() {
            return this.materializedPayload;
        }

        @NotNull
        public AfmaStoredPayload materialize() throws IOException {
            this.ensureOpen();
            if (this.materializedPayload != null) {
                return this.materializedPayload;
            }

            AfmaStoredPayload payload;
            if (this.payloadBytes != null) {
                payload = AfmaStoredPayload.fromBytes(this.payloadBytes);
            } else if (this.payloadWriter != null) {
                payload = AfmaStoredPayload.write(this.payloadWriter);
            } else {
                throw new IOException("AFMA deferred payload no longer has a materialization source");
            }

            AfmaStoredPayload.PayloadSummary materializedSummary = payload.summarize();
            if ((materializedSummary.length() != this.payloadSummary.length())
                    || (materializedSummary.estimatedArchiveBytes() != this.payloadSummary.estimatedArchiveBytes())
                    || !materializedSummary.fingerprint().equals(this.payloadSummary.fingerprint())) {
                payload.close();
                throw new IOException("AFMA deferred payload metrics changed during materialization");
            }

            this.materializedPayload = payload;
            this.payloadBytes = null;
            this.payloadWriter = null;
            return payload;
        }

        @Override
        public void close() {
            if (this.closed) {
                return;
            }
            this.closed = true;
            if (this.materializedPayload != null) {
                this.materializedPayload.close();
                this.materializedPayload = null;
            }
            this.payloadBytes = null;
            this.payloadWriter = null;
        }

        protected void ensureOpen() {
            if (this.closed) {
                throw new IllegalStateException("AFMA deferred payload has already been closed");
            }
        }
    }

    protected static final class PlanningStep {

        @Nullable
        protected final PlanningStep previous;
        protected final long delayMs;
        @Nullable
        protected final PlannedCandidate candidate;

        protected PlanningStep(@Nullable PlanningStep previous, @Nullable PlannedCandidate candidate, long delayMs) {
            this.previous = previous;
            this.candidate = candidate;
            this.delayMs = delayMs;
        }

        public boolean isDelayExtension() {
            return this.candidate == null;
        }

        public long delayMs() {
            return this.delayMs;
        }

        @Nullable
        public PlannedCandidate candidate() {
            return this.candidate;
        }

    }

    protected static final class BeamPlanningState {

        @Nullable
        protected final AfmaPixelFrame previousFrame;
        protected final int framesSinceKeyframe;
        protected final int decodeComplexitySinceKeyframe;
        protected final long interArchiveBytesSinceKeyframe;
        @NotNull
        protected final DriftState driftState;
        @NotNull
        protected final ArchivePlanningState archiveState;
        protected final boolean emittedFrameAvailable;
        @Nullable
        protected final PlanningStep tailStep;
        protected final double objectiveScore;
        protected final long estimatedArchiveBytes;

        protected BeamPlanningState(@Nullable AfmaPixelFrame previousFrame, int framesSinceKeyframe,
                                    int decodeComplexitySinceKeyframe, long interArchiveBytesSinceKeyframe,
                                    @NotNull DriftState driftState, @NotNull ArchivePlanningState archiveState,
                                    boolean emittedFrameAvailable, @Nullable PlanningStep tailStep,
                                    double objectiveScore, long estimatedArchiveBytes) {
            this.previousFrame = previousFrame;
            this.framesSinceKeyframe = framesSinceKeyframe;
            this.decodeComplexitySinceKeyframe = decodeComplexitySinceKeyframe;
            this.interArchiveBytesSinceKeyframe = interArchiveBytesSinceKeyframe;
            this.driftState = driftState;
            this.archiveState = archiveState;
            this.emittedFrameAvailable = emittedFrameAvailable;
            this.tailStep = tailStep;
            this.objectiveScore = objectiveScore;
            this.estimatedArchiveBytes = estimatedArchiveBytes;
        }

        @NotNull
        public static BeamPlanningState root(@NotNull ArchivePlanningState archiveState) {
            return new BeamPlanningState(null, 0, 0, 0L, DriftState.exact(), archiveState, false, null, 0D, 0L);
        }

        @Nullable
        public AfmaPixelFrame previousFrame() {
            return this.previousFrame;
        }

        public int framesSinceKeyframe() {
            return this.framesSinceKeyframe;
        }

        public int decodeComplexitySinceKeyframe() {
            return this.decodeComplexitySinceKeyframe;
        }

        public long interArchiveBytesSinceKeyframe() {
            return this.interArchiveBytesSinceKeyframe;
        }

        @NotNull
        public DriftState driftState() {
            return this.driftState;
        }

        @NotNull
        public ArchivePlanningState archiveState() {
            return this.archiveState;
        }

        public boolean emittedFrameAvailable() {
            return this.emittedFrameAvailable;
        }

        public double objectiveScore() {
            return this.objectiveScore;
        }

        public long estimatedArchiveBytes() {
            return this.estimatedArchiveBytes;
        }

        @NotNull
        public BeamPlanningState advanceDelay(long delayMs, @NotNull DriftState nextDriftState, double scoreIncrement) {
            return new BeamPlanningState(
                    this.previousFrame,
                    this.framesSinceKeyframe,
                    this.decodeComplexitySinceKeyframe,
                    this.interArchiveBytesSinceKeyframe,
                    nextDriftState,
                    this.archiveState,
                    this.emittedFrameAvailable,
                    new PlanningStep(this.tailStep, null, delayMs),
                    this.objectiveScore + scoreIncrement,
                    this.estimatedArchiveBytes
            );
        }

        @NotNull
        public BeamPlanningState advanceCandidate(@NotNull PlannedCandidate candidate, @NotNull AfmaPixelFrame reconstructedFrame,
                                                  int nextFramesSinceKeyframe, @NotNull DriftState nextDriftState,
                                                  @NotNull ArchivePlanningState nextArchiveState, long delayMs,
                                                  double scoreIncrement, long archiveBytesIncrement,
                                                  int nextDecodeComplexitySinceKeyframe,
                                                  long nextInterArchiveBytesSinceKeyframe) {
            return new BeamPlanningState(
                    reconstructedFrame,
                    nextFramesSinceKeyframe,
                    nextDecodeComplexitySinceKeyframe,
                    nextInterArchiveBytesSinceKeyframe,
                    nextDriftState,
                    nextArchiveState,
                    true,
                    new PlanningStep(this.tailStep, candidate, delayMs),
                    this.objectiveScore + scoreIncrement,
                    this.estimatedArchiveBytes + archiveBytesIncrement
            );
        }

        @NotNull
        public BeamPlanningState toCommittedBaseState() {
            return new BeamPlanningState(
                    this.previousFrame,
                    this.framesSinceKeyframe,
                    this.decodeComplexitySinceKeyframe,
                    this.interArchiveBytesSinceKeyframe,
                    this.driftState,
                    this.archiveState,
                    this.emittedFrameAvailable,
                    null,
                    this.objectiveScore,
                    this.estimatedArchiveBytes
            );
        }

        @NotNull
        public BeamPlanningState refineArchiveScore() {
            ArchivePlanningState refinedArchiveState = this.archiveState.refineArchiveMetrics();
            if (refinedArchiveState == this.archiveState) {
                return this;
            }

            long estimatedArchiveDelta = refinedArchiveState.estimatedArchiveBytes - this.archiveState.estimatedArchiveBytes;
            long scoredArchiveDelta = refinedArchiveState.scoredArchiveBytes - this.archiveState.scoredArchiveBytes;
            return new BeamPlanningState(
                    this.previousFrame,
                    this.framesSinceKeyframe,
                    this.decodeComplexitySinceKeyframe,
                    this.interArchiveBytesSinceKeyframe,
                    this.driftState,
                    refinedArchiveState,
                    this.emittedFrameAvailable,
                    this.tailStep,
                    this.objectiveScore + scoredArchiveDelta,
                    this.estimatedArchiveBytes + estimatedArchiveDelta
            );
        }

        @NotNull
        public List<PlanningStep> stepsInOrder() {
            ArrayList<PlanningStep> reversedSteps = new ArrayList<>();
            PlanningStep step = this.tailStep;
            while (step != null) {
                reversedSteps.add(step);
                step = step.previous;
            }
            ArrayList<PlanningStep> orderedSteps = new ArrayList<>(reversedSteps.size());
            for (int index = reversedSteps.size() - 1; index >= 0; index--) {
                orderedSteps.add(reversedSteps.get(index));
            }
            return orderedSteps;
        }

    }

    protected record CandidateArchiveCost(@NotNull ArchivePlanningState nextState, long marginalArchiveBytes, long marginalScoreBytes) {
    }

    protected record ArchivePayloadAppendResult(@NotNull ArchivePlanningState nextState, @Nullable String resolvedPayloadPath) {
    }

    protected static final class PersistentIdentityAllocator {

        protected long nextIdentity = 1L;

        public long allocateIdentity() {
            return this.nextIdentity++;
        }
    }

    protected static final class PersistentAppendMap<K, V> {

        protected static final int HASH_BITS_PER_LEVEL = 5;
        protected static final int HASH_BRANCH_FACTOR = 1 << HASH_BITS_PER_LEVEL;
        protected static final int HASH_INDEX_MASK = HASH_BRANCH_FACTOR - 1;

        @NotNull
        protected final PersistentIdentityAllocator identityAllocator;
        @Nullable
        protected final IndexNode<K, V> indexRoot;
        @Nullable
        protected final OrderedEntry<K, V> tailEntry;
        protected final int size;

        protected PersistentAppendMap(@NotNull PersistentIdentityAllocator identityAllocator,
                                      @Nullable IndexNode<K, V> indexRoot, @Nullable OrderedEntry<K, V> tailEntry, int size) {
            this.identityAllocator = Objects.requireNonNull(identityAllocator);
            this.indexRoot = indexRoot;
            this.tailEntry = tailEntry;
            this.size = Math.max(0, size);
        }

        @NotNull
        public static <K, V> PersistentAppendMap<K, V> empty() {
            return new PersistentAppendMap<>(new PersistentIdentityAllocator(), null, null, 0);
        }

        public int size() {
            return this.size;
        }

        public boolean isEmpty() {
            return this.size <= 0;
        }

        @Nullable
        public V get(@NotNull K key) {
            OrderedEntry<K, V> entry = IndexNode.find(this.indexRoot, Objects.requireNonNull(key), spreadHash(key.hashCode()), 0);
            return (entry != null) ? entry.value : null;
        }

        public long tailIdentity() {
            return (this.tailEntry != null) ? this.tailEntry.identity : 0L;
        }

        @NotNull
        public PersistentAppendMap<K, V> append(@NotNull K key, @NotNull V value) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);
            if (this.get(key) != null) {
                throw new IllegalArgumentException("AFMA persistent append map already contains key " + key);
            }

            OrderedEntry<K, V> nextTail = new OrderedEntry<>(this.tailEntry, this.identityAllocator.allocateIdentity(), key, value);
            return new PersistentAppendMap<>(
                    this.identityAllocator,
                    IndexNode.put(this.indexRoot, key, spreadHash(key.hashCode()), nextTail, 0),
                    nextTail,
                    this.size + 1
            );
        }

        public void forEachInInsertionOrder(@NotNull BiConsumer<K, V> consumer) {
            Objects.requireNonNull(consumer);
            if (this.tailEntry == null) {
                return;
            }

            ArrayList<OrderedEntry<K, V>> reversedEntries = new ArrayList<>(this.size);
            OrderedEntry<K, V> currentEntry = this.tailEntry;
            while (currentEntry != null) {
                reversedEntries.add(currentEntry);
                currentEntry = currentEntry.previous;
            }

            for (int index = reversedEntries.size() - 1; index >= 0; index--) {
                OrderedEntry<K, V> entry = reversedEntries.get(index);
                consumer.accept(entry.key, entry.value);
            }
        }

        protected static int spreadHash(int hash) {
            return hash ^ (hash >>> 16);
        }

        protected abstract static class IndexNode<K, V> {

            @Nullable
            @SuppressWarnings("unchecked")
            protected static <K, V> OrderedEntry<K, V> find(@Nullable IndexNode<K, V> node, @NotNull K key, int hash, int shift) {
                if (node == null) {
                    return null;
                }
                if (node instanceof LeafNode<?, ?>) {
                    LeafNode<K, V> leafNode = (LeafNode<K, V>) node;
                    if (leafNode.hash != hash) {
                        return null;
                    }
                    CollisionEntry<K, V> collisionEntry = leafNode.collisionChain;
                    while (collisionEntry != null) {
                        if (collisionEntry.key.equals(key)) {
                            return collisionEntry.entry;
                        }
                        collisionEntry = collisionEntry.next;
                    }
                    return null;
                }

                BranchNode<K, V> branchNode = (BranchNode<K, V>) node;
                int childIndex = (hash >>> shift) & HASH_INDEX_MASK;
                return find((IndexNode<K, V>) branchNode.children[childIndex], key, hash, shift + HASH_BITS_PER_LEVEL);
            }

            @NotNull
            @SuppressWarnings("unchecked")
            protected static <K, V> IndexNode<K, V> put(@Nullable IndexNode<K, V> node, @NotNull K key, int hash,
                                                        @NotNull OrderedEntry<K, V> entry, int shift) {
                if (node == null) {
                    return new LeafNode<>(hash, new CollisionEntry<>(key, entry, null));
                }
                if (node instanceof LeafNode<?, ?>) {
                    LeafNode<K, V> leafNode = (LeafNode<K, V>) node;
                    if (leafNode.hash == hash) {
                        return new LeafNode<>(hash, new CollisionEntry<>(key, entry, leafNode.collisionChain));
                    }
                    return mergeLeaves(leafNode, new LeafNode<>(hash, new CollisionEntry<>(key, entry, null)), shift);
                }

                BranchNode<K, V> branchNode = (BranchNode<K, V>) node;
                int childIndex = (hash >>> shift) & HASH_INDEX_MASK;
                Object[] nextChildren = branchNode.children.clone();
                nextChildren[childIndex] = put((IndexNode<K, V>) nextChildren[childIndex], key, hash, entry, shift + HASH_BITS_PER_LEVEL);
                return new BranchNode<>(nextChildren);
            }

            @NotNull
            protected static <K, V> IndexNode<K, V> mergeLeaves(@NotNull LeafNode<K, V> firstLeaf,
                                                                @NotNull LeafNode<K, V> secondLeaf, int shift) {
                int firstIndex = (firstLeaf.hash >>> shift) & HASH_INDEX_MASK;
                int secondIndex = (secondLeaf.hash >>> shift) & HASH_INDEX_MASK;
                Object[] children = new Object[HASH_BRANCH_FACTOR];
                if (firstIndex != secondIndex) {
                    children[firstIndex] = firstLeaf;
                    children[secondIndex] = secondLeaf;
                    return new BranchNode<>(children);
                }
                if (shift >= 30) {
                    throw new IllegalStateException("AFMA persistent append map exceeded its hash trie depth");
                }
                children[firstIndex] = mergeLeaves(firstLeaf, secondLeaf, shift + HASH_BITS_PER_LEVEL);
                return new BranchNode<>(children);
            }
        }

        protected static final class BranchNode<K, V> extends IndexNode<K, V> {

            @NotNull
            protected final Object[] children;

            protected BranchNode(@NotNull Object[] children) {
                this.children = Objects.requireNonNull(children);
            }
        }

        protected static final class LeafNode<K, V> extends IndexNode<K, V> {

            protected final int hash;
            @NotNull
            protected final CollisionEntry<K, V> collisionChain;

            protected LeafNode(int hash, @NotNull CollisionEntry<K, V> collisionChain) {
                this.hash = hash;
                this.collisionChain = Objects.requireNonNull(collisionChain);
            }
        }

        protected static final class CollisionEntry<K, V> {

            @NotNull
            protected final K key;
            @NotNull
            protected final OrderedEntry<K, V> entry;
            @Nullable
            protected final CollisionEntry<K, V> next;

            protected CollisionEntry(@NotNull K key, @NotNull OrderedEntry<K, V> entry, @Nullable CollisionEntry<K, V> next) {
                this.key = Objects.requireNonNull(key);
                this.entry = Objects.requireNonNull(entry);
                this.next = next;
            }
        }

        protected static final class OrderedEntry<K, V> {

            @Nullable
            protected final OrderedEntry<K, V> previous;
            protected final long identity;
            @NotNull
            protected final K key;
            @NotNull
            protected final V value;

            protected OrderedEntry(@Nullable OrderedEntry<K, V> previous, long identity, @NotNull K key, @NotNull V value) {
                this.previous = previous;
                this.identity = identity;
                this.key = Objects.requireNonNull(key);
                this.value = Objects.requireNonNull(value);
            }
        }
    }

    protected static final class PersistentAppendList<T> {

        @NotNull
        protected final PersistentIdentityAllocator identityAllocator;
        @Nullable
        protected final Node<T> tailNode;
        protected final int size;

        protected PersistentAppendList(@NotNull PersistentIdentityAllocator identityAllocator,
                                       @Nullable Node<T> tailNode, int size) {
            this.identityAllocator = Objects.requireNonNull(identityAllocator);
            this.tailNode = tailNode;
            this.size = Math.max(0, size);
        }

        @NotNull
        public static <T> PersistentAppendList<T> empty() {
            return new PersistentAppendList<>(new PersistentIdentityAllocator(), null, 0);
        }

        public boolean isEmpty() {
            return this.size <= 0;
        }

        public int size() {
            return this.size;
        }

        public long tailIdentity() {
            return (this.tailNode != null) ? this.tailNode.identity : 0L;
        }

        @NotNull
        public PersistentAppendList<T> append(@NotNull T value) {
            Objects.requireNonNull(value);
            return new PersistentAppendList<>(this.identityAllocator, new Node<>(this.tailNode, this.identityAllocator.allocateIdentity(), value), this.size + 1);
        }

        @NotNull
        public List<T> materialize() {
            if (this.tailNode == null) {
                return List.of();
            }

            ArrayList<T> reversedValues = new ArrayList<>(this.size);
            Node<T> currentNode = this.tailNode;
            while (currentNode != null) {
                reversedValues.add(currentNode.value);
                currentNode = currentNode.previous;
            }

            ArrayList<T> orderedValues = new ArrayList<>(reversedValues.size());
            for (int index = reversedValues.size() - 1; index >= 0; index--) {
                orderedValues.add(reversedValues.get(index));
            }
            return orderedValues;
        }

        protected static final class Node<T> {

            @Nullable
            protected final Node<T> previous;
            protected final long identity;
            @NotNull
            protected final T value;

            protected Node(@Nullable Node<T> previous, long identity, @NotNull T value) {
                this.previous = previous;
                this.identity = identity;
                this.value = Objects.requireNonNull(value);
            }
        }
    }

    protected record ExactArchiveRefinementKey(long payloadTailIdentity,
                                               long accessTraceTailIdentity) {
    }

    protected record ExactArchiveRefinementMetrics(@NotNull IncrementalArchiveScoreState plannerScoreState,
                                                   long estimatedArchiveBytes, long scoredArchiveBytes) {
    }

    protected static final class ExactArchiveRefinementCache {

        // Bound the cache so repeated survivor rescoring cannot pin old planner prefixes indefinitely.
        @NotNull
        protected final LinkedHashMap<ExactArchiveRefinementKey, ExactArchiveRefinementMetrics> metricsByPrefix
                = new LinkedHashMap<>(128, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<ExactArchiveRefinementKey, ExactArchiveRefinementMetrics> eldest) {
                return this.size() > PLANNER_MAX_EXACT_ARCHIVE_REFINEMENT_CACHE_ENTRIES;
            }
        };

        @Nullable
        public ExactArchiveRefinementMetrics get(@NotNull ExactArchiveRefinementKey key) {
            return this.metricsByPrefix.get(Objects.requireNonNull(key));
        }

        public void put(@NotNull ExactArchiveRefinementKey key, @NotNull ExactArchiveRefinementMetrics metrics) {
            this.metricsByPrefix.put(Objects.requireNonNull(key), Objects.requireNonNull(metrics));
        }
    }

    protected static final class IncrementalArchiveScoreState {
        // Planner-local proxy for archive scoring. This follows append order and decoder cache pressure,
        // which is cheap enough for inner-loop comparisons before exact survivor rescoring.

        @NotNull
        protected final PersistentAppendMap<String, AfmaChunkedPayloadHelper.PayloadLocator> payloadLocatorsByPath;
        protected final int currentChunkId;
        protected final int currentChunkLength;
        @NotNull
        protected final byte[] currentChunkTail;
        protected final int chunkCount;
        protected final int payloadCount;
        protected final long estimatedCompressedPayloadBytes;
        protected final long payloadLocatorBytes;
        protected final long chunkLengthBytes;
        @NotNull
        protected final LinkedHashMap<Integer, Integer> cachedChunkIds;
        protected final long archiveReads;
        protected final int multiChunkFrameCount;

        protected IncrementalArchiveScoreState(@NotNull PersistentAppendMap<String, AfmaChunkedPayloadHelper.PayloadLocator> payloadLocatorsByPath,
                                               int currentChunkId, int currentChunkLength,
                                               @NotNull byte[] currentChunkTail, int chunkCount, int payloadCount,
                                               long estimatedCompressedPayloadBytes, long payloadLocatorBytes,
                                               long chunkLengthBytes,
                                               @NotNull LinkedHashMap<Integer, Integer> cachedChunkIds,
                                               long archiveReads, int multiChunkFrameCount) {
            this.payloadLocatorsByPath = Objects.requireNonNull(payloadLocatorsByPath);
            this.currentChunkId = currentChunkId;
            this.currentChunkLength = currentChunkLength;
            this.currentChunkTail = Objects.requireNonNull(currentChunkTail);
            this.chunkCount = chunkCount;
            this.payloadCount = payloadCount;
            this.estimatedCompressedPayloadBytes = estimatedCompressedPayloadBytes;
            this.payloadLocatorBytes = payloadLocatorBytes;
            this.chunkLengthBytes = chunkLengthBytes;
            this.cachedChunkIds = Objects.requireNonNull(cachedChunkIds);
            this.archiveReads = archiveReads;
            this.multiChunkFrameCount = multiChunkFrameCount;
        }

        @NotNull
        public static IncrementalArchiveScoreState empty() {
            return new IncrementalArchiveScoreState(
                    PersistentAppendMap.empty(),
                    -1,
                    0,
                    EMPTY_BYTES,
                    0,
                    0,
                    0L,
                    0L,
                    0L,
                    new LinkedHashMap<>(PLANNER_MAX_CACHED_PAYLOAD_CHUNKS, 0.75F, true),
                    0L,
                    0
            );
        }

        @NotNull
        public static IncrementalArchiveScoreState fromExactArchive(@NotNull Map<String, AfmaStoredPayload> payloadsByPath,
                                                                    @NotNull AfmaChunkedPayloadHelper.ArchivePackingHints packingHints,
                                                                    @NotNull AfmaChunkedPayloadHelper.PackedPayloadArchive archive) {
            Objects.requireNonNull(payloadsByPath);
            Objects.requireNonNull(packingHints);
            Objects.requireNonNull(archive);
            if (archive.chunkPlans().isEmpty() || archive.payloadIdsByPath().isEmpty()) {
                return empty();
            }

            PersistentAppendMap<String, AfmaChunkedPayloadHelper.PayloadLocator> payloadLocatorsByPath = PersistentAppendMap.empty();
            for (Map.Entry<String, Integer> entry : archive.payloadIdsByPath().entrySet()) {
                int payloadId = Objects.requireNonNull(entry.getValue());
                if (payloadId < 0 || payloadId >= archive.payloadLocators().size()) {
                    continue;
                }
                payloadLocatorsByPath = payloadLocatorsByPath.append(entry.getKey(), archive.payloadLocators().get(payloadId));
            }

            long payloadLocatorBytes = 0L;
            for (AfmaChunkedPayloadHelper.PayloadLocator locator : archive.payloadLocators()) {
                payloadLocatorBytes += AfmaChunkedPayloadHelper.estimateVarIntBytes(locator.chunkId())
                        + AfmaChunkedPayloadHelper.estimateVarIntBytes(locator.offset())
                        + AfmaChunkedPayloadHelper.estimateVarIntBytes(locator.length());
            }

            long chunkLengthBytes = 0L;
            for (AfmaChunkedPayloadHelper.ChunkPlan chunkPlan : archive.chunkPlans()) {
                chunkLengthBytes += AfmaChunkedPayloadHelper.estimateVarIntBytes(chunkPlan.uncompressedLength());
            }

            AfmaChunkedPayloadHelper.ChunkPlan lastChunkPlan = archive.chunkPlans().get(archive.chunkPlans().size() - 1);
            IncrementalArchiveScoreState exactState = new IncrementalArchiveScoreState(
                    payloadLocatorsByPath,
                    archive.chunkPlans().size() - 1,
                    lastChunkPlan.uncompressedLength(),
                    buildChunkTail(lastChunkPlan.payloadPaths(), payloadsByPath),
                    archive.chunkPlans().size(),
                    archive.payloadIdsByPath().size(),
                    archive.packingMetrics().estimatedCompressedPayloadBytes(),
                    payloadLocatorBytes,
                    chunkLengthBytes,
                    new LinkedHashMap<>(PLANNER_MAX_CACHED_PAYLOAD_CHUNKS, 0.75F, true),
                    0L,
                    0
            );
            for (AfmaChunkedPayloadHelper.PayloadAccessFrame accessFrame : packingHints.accessFrames()) {
                exactState = exactState.appendFrameAccess(accessFrame);
            }
            return exactState;
        }

        @NotNull
        public IncrementalArchiveScoreState appendPayload(@NotNull String payloadPath, @NotNull DeferredPayload payload) {
            Objects.requireNonNull(payloadPath);
            Objects.requireNonNull(payload);
            if (payload.isEmpty()) {
                return this;
            }

            boolean startNewChunk = (this.chunkCount == 0)
                    || (((long) this.currentChunkLength + payload.length()) > PLANNER_TARGET_CHUNK_BYTES);
            int nextChunkId = startNewChunk ? this.chunkCount : this.currentChunkId;
            int chunkOffset = startNewChunk ? 0 : this.currentChunkLength;
            int nextChunkLength = startNewChunk ? payload.length() : (this.currentChunkLength + payload.length());
            byte[] previousTail = startNewChunk ? EMPTY_BYTES : this.currentChunkTail;

            long nextChunkLengthBytes = this.chunkLengthBytes;
            if (startNewChunk) {
                nextChunkLengthBytes += AfmaChunkedPayloadHelper.estimateVarIntBytes(nextChunkLength);
            } else {
                nextChunkLengthBytes += AfmaChunkedPayloadHelper.estimateVarIntBytes(nextChunkLength)
                        - AfmaChunkedPayloadHelper.estimateVarIntBytes(this.currentChunkLength);
            }

            PersistentAppendMap<String, AfmaChunkedPayloadHelper.PayloadLocator> nextLocatorsByPath = this.payloadLocatorsByPath.append(
                    payloadPath,
                    new AfmaChunkedPayloadHelper.PayloadLocator(nextChunkId, chunkOffset, payload.length())
            );
            return new IncrementalArchiveScoreState(
                    nextLocatorsByPath,
                    nextChunkId,
                    nextChunkLength,
                    payload.appendDeflateTail(previousTail),
                    startNewChunk ? (this.chunkCount + 1) : this.chunkCount,
                    this.payloadCount + 1,
                    this.estimatedCompressedPayloadBytes + payload.estimateChunkCompressionDelta(previousTail),
                    this.payloadLocatorBytes
                            + AfmaChunkedPayloadHelper.estimateVarIntBytes(nextChunkId)
                            + AfmaChunkedPayloadHelper.estimateVarIntBytes(chunkOffset)
                            + AfmaChunkedPayloadHelper.estimateVarIntBytes(payload.length()),
                    nextChunkLengthBytes,
                    this.cachedChunkIds,
                    this.archiveReads,
                    this.multiChunkFrameCount
            );
        }

        @NotNull
        public IncrementalArchiveScoreState appendFrameAccess(@NotNull AfmaChunkedPayloadHelper.PayloadAccessFrame accessFrame) {
            Objects.requireNonNull(accessFrame);
            if (accessFrame.payloadPaths().isEmpty()) {
                return this;
            }

            LinkedHashSet<Integer> accessedChunkIds = new LinkedHashSet<>();
            for (String payloadPath : accessFrame.payloadPaths()) {
                AfmaChunkedPayloadHelper.PayloadLocator locator = this.payloadLocatorsByPath.get(payloadPath);
                if (locator != null) {
                    accessedChunkIds.add(locator.chunkId());
                }
            }
            if (accessedChunkIds.isEmpty()) {
                return this;
            }

            LinkedHashMap<Integer, Integer> nextCachedChunkIds = new LinkedHashMap<>(Math.max(1, this.cachedChunkIds.size() + 1), 0.75F, true);
            nextCachedChunkIds.putAll(this.cachedChunkIds);
            long nextArchiveReads = this.archiveReads;
            for (Integer chunkId : accessedChunkIds) {
                if (nextCachedChunkIds.get(chunkId) != null) {
                    continue;
                }

                nextArchiveReads++;
                if (nextCachedChunkIds.size() >= PLANNER_MAX_CACHED_PAYLOAD_CHUNKS) {
                    java.util.Iterator<Map.Entry<Integer, Integer>> iterator = nextCachedChunkIds.entrySet().iterator();
                    if (iterator.hasNext()) {
                        iterator.next();
                        iterator.remove();
                    }
                }
                nextCachedChunkIds.put(chunkId, chunkId);
            }

            return new IncrementalArchiveScoreState(
                    this.payloadLocatorsByPath,
                    this.currentChunkId,
                    this.currentChunkLength,
                    this.currentChunkTail,
                    this.chunkCount,
                    this.payloadCount,
                    this.estimatedCompressedPayloadBytes,
                    this.payloadLocatorBytes,
                    this.chunkLengthBytes,
                    nextCachedChunkIds,
                    nextArchiveReads,
                    this.multiChunkFrameCount + ((accessedChunkIds.size() > 1) ? 1 : 0)
            );
        }

        public long predictedArchiveBytes() {
            if (this.payloadCount <= 0) {
                return 0L;
            }

            long payloadIndexBytes = 5L
                    + AfmaChunkedPayloadHelper.estimateVarIntBytes(this.chunkCount)
                    + AfmaChunkedPayloadHelper.estimateVarIntBytes(this.payloadCount)
                    + this.chunkLengthBytes
                    + this.payloadLocatorBytes;
            long chunkOverheadBytes = (long) this.chunkCount * ESTIMATED_ZIP_CHUNK_OVERHEAD_BYTES;
            return this.estimatedCompressedPayloadBytes + chunkOverheadBytes + payloadIndexBytes;
        }

        public long scoredArchiveBytes() {
            return this.predictedArchiveBytes()
                    + (this.archiveReads * PLANNER_CHUNK_CACHE_MISS_PENALTY_BYTES)
                    + ((long) this.multiChunkFrameCount * PLANNER_MULTI_CHUNK_FRAME_PENALTY_BYTES);
        }

        @NotNull
        protected static byte[] buildChunkTail(@NotNull List<String> chunkPayloadPaths, @NotNull Map<String, AfmaStoredPayload> payloadsByPath) {
            byte[] tail = EMPTY_BYTES;
            for (String payloadPath : chunkPayloadPaths) {
                AfmaStoredPayload payload = payloadsByPath.get(payloadPath);
                if (payload != null) {
                    tail = AfmaChunkedPayloadHelper.appendDeflateTail(tail, payload);
                }
            }
            return tail;
        }

    }

    protected record DriftState(double cumulativeAverageError, int cumulativeVisibleColorDelta,
                                int cumulativeAlphaDelta, int consecutiveLossyFrames) {

        @NotNull
        public static DriftState exact() {
            return new DriftState(0D, 0, 0, 0);
        }

        @NotNull
        public DriftState accumulate(@NotNull PerceptualDriftStats driftStats) {
            long nextVisibleDelta = (long) this.cumulativeVisibleColorDelta + driftStats.maxVisibleColorDelta();
            long nextAlphaDelta = (long) this.cumulativeAlphaDelta + driftStats.maxAlphaDelta();
            return new DriftState(
                    this.cumulativeAverageError + driftStats.averageError(),
                    (int) Math.min(Integer.MAX_VALUE, nextVisibleDelta),
                    (int) Math.min(Integer.MAX_VALUE, nextAlphaDelta),
                    this.consecutiveLossyFrames + 1
            );
        }

    }

    protected record DriftTransition(@NotNull DriftState nextState, @NotNull PerceptualDriftStats driftStats, double scorePenalty) {
    }

    protected static final class ArchivePlanningState {

        @NotNull
        protected final PersistentAppendMap<String, DeferredPayload> payloadsByPath;
        @NotNull
        protected final PersistentAppendMap<String, String> payloadPathsByFingerprint;
        @NotNull
        protected final PersistentAppendList<AfmaChunkedPayloadHelper.PayloadAccessFrame> accessTrace;
        protected final int nextSyntheticPayloadId;
        protected final int nextKeyframeRegionId;
        protected final int currentIntroKeyframeRegionId;
        protected final int currentMainKeyframeRegionId;
        @NotNull
        protected final IncrementalArchiveScoreState plannerScoreState;
        @NotNull
        protected final ExactArchiveRefinementCache exactRefinementCache;
        protected final boolean archiveMetricsExact;
        protected final long estimatedArchiveBytes;
        protected final long scoredArchiveBytes;

        protected ArchivePlanningState(@NotNull PersistentAppendMap<String, DeferredPayload> payloadsByPath,
                                       @NotNull PersistentAppendMap<String, String> payloadPathsByFingerprint,
                                       @NotNull PersistentAppendList<AfmaChunkedPayloadHelper.PayloadAccessFrame> accessTrace,
                                       int nextSyntheticPayloadId, int nextKeyframeRegionId,
                                       int currentIntroKeyframeRegionId, int currentMainKeyframeRegionId,
                                       @NotNull IncrementalArchiveScoreState plannerScoreState,
                                       @NotNull ExactArchiveRefinementCache exactRefinementCache,
                                       boolean archiveMetricsExact,
                                       long estimatedArchiveBytes, long scoredArchiveBytes) {
            this.payloadsByPath = Objects.requireNonNull(payloadsByPath);
            this.payloadPathsByFingerprint = Objects.requireNonNull(payloadPathsByFingerprint);
            this.accessTrace = Objects.requireNonNull(accessTrace);
            this.nextSyntheticPayloadId = nextSyntheticPayloadId;
            this.nextKeyframeRegionId = nextKeyframeRegionId;
            this.currentIntroKeyframeRegionId = currentIntroKeyframeRegionId;
            this.currentMainKeyframeRegionId = currentMainKeyframeRegionId;
            this.plannerScoreState = Objects.requireNonNull(plannerScoreState);
            this.exactRefinementCache = Objects.requireNonNull(exactRefinementCache);
            this.archiveMetricsExact = archiveMetricsExact;
            this.estimatedArchiveBytes = estimatedArchiveBytes;
            this.scoredArchiveBytes = scoredArchiveBytes;
        }

        @NotNull
        public static ArchivePlanningState empty() {
            return new ArchivePlanningState(
                    PersistentAppendMap.empty(),
                    PersistentAppendMap.empty(),
                    PersistentAppendList.empty(),
                    0,
                    0,
                    -1,
                    -1,
                    IncrementalArchiveScoreState.empty(),
                    new ExactArchiveRefinementCache(),
                    true,
                    0L,
                    0L
            );
        }

        @NotNull
        public CandidateArchiveCost appendCandidate(@NotNull PlannedCandidate candidate, boolean introSequence) {
            ArchivePlanningState nextState = this;
            long descriptorBytes = candidate.estimateDescriptorBytes();

            ArchivePayloadAppendResult primaryAppend = nextState.appendPayload(candidate.primaryPayload);
            nextState = primaryAppend.nextState();

            ArchivePayloadAppendResult patchAppend = nextState.appendPayload(candidate.patchPayload);
            nextState = patchAppend.nextState();

            nextState = nextState.appendFrameAccess(
                    candidate.descriptor(),
                    introSequence,
                    primaryAppend.resolvedPayloadPath(),
                    patchAppend.resolvedPayloadPath()
            );
            long marginalArchiveBytes = (nextState.estimatedArchiveBytes - this.estimatedArchiveBytes) + descriptorBytes;
            long marginalScoreBytes = (nextState.scoredArchiveBytes - this.scoredArchiveBytes) + descriptorBytes;
            return new CandidateArchiveCost(nextState, marginalArchiveBytes, marginalScoreBytes);
        }

        @NotNull
        protected ArchivePayloadAppendResult appendPayload(@Nullable DeferredPayload payload) {
            if (payload == null || payload.isEmpty()) {
                return new ArchivePayloadAppendResult(this, null);
            }

            String fingerprint = payload.fingerprint();
            String existingPath = this.payloadPathsByFingerprint.get(fingerprint);
            if (existingPath != null) {
                return new ArchivePayloadAppendResult(this, existingPath);
            }

            String syntheticPath = AfmaChunkedPayloadHelper.syntheticPayloadPath(this.nextSyntheticPayloadId);
            PersistentAppendMap<String, DeferredPayload> nextPayloadsByPath = this.payloadsByPath.append(syntheticPath, payload);
            PersistentAppendMap<String, String> nextPayloadPathsByFingerprint = this.payloadPathsByFingerprint.append(fingerprint, syntheticPath);
            return new ArchivePayloadAppendResult(
                    this.advancePlanningState(
                            nextPayloadsByPath,
                            nextPayloadPathsByFingerprint,
                            this.accessTrace,
                            this.nextSyntheticPayloadId + 1,
                            this.nextKeyframeRegionId,
                            this.currentIntroKeyframeRegionId,
                            this.currentMainKeyframeRegionId,
                            this.plannerScoreState.appendPayload(syntheticPath, payload)
                    ),
                    syntheticPath
            );
        }

        @NotNull
        protected ArchivePlanningState appendFrameAccess(@NotNull AfmaFrameDescriptor descriptor, boolean introSequence,
                                                         @Nullable String primaryPayloadPath, @Nullable String patchPayloadPath) {
            int currentIntroRegionId = this.currentIntroKeyframeRegionId;
            int currentMainRegionId = this.currentMainKeyframeRegionId;
            int nextRegionId = this.nextKeyframeRegionId;
            int currentRegionId = introSequence ? currentIntroRegionId : currentMainRegionId;
            if (currentRegionId < 0 || descriptor.isKeyframe()) {
                currentRegionId = nextRegionId++;
            }

            ArrayList<String> payloadPaths = new ArrayList<>(2);
            if (descriptor.requiresPrimaryPayload() && primaryPayloadPath != null) {
                payloadPaths.add(primaryPayloadPath);
            }
            if (descriptor.requiresPatchPayload() && patchPayloadPath != null && !payloadPaths.contains(patchPayloadPath)) {
                payloadPaths.add(patchPayloadPath);
            }

            if (introSequence) {
                currentIntroRegionId = currentRegionId;
            } else {
                currentMainRegionId = currentRegionId;
            }

            AfmaChunkedPayloadHelper.PayloadAccessFrame accessFrame = new AfmaChunkedPayloadHelper.PayloadAccessFrame(
                    payloadPaths,
                    currentRegionId,
                    introSequence ? AfmaChunkedPayloadHelper.SEQUENCE_KIND_INTRO : AfmaChunkedPayloadHelper.SEQUENCE_KIND_MAIN
            );
            PersistentAppendList<AfmaChunkedPayloadHelper.PayloadAccessFrame> nextAccessTrace = this.accessTrace.append(accessFrame);
            return this.advancePlanningState(
                    this.payloadsByPath,
                    this.payloadPathsByFingerprint,
                    nextAccessTrace,
                    this.nextSyntheticPayloadId,
                    nextRegionId,
                    currentIntroRegionId,
                    currentMainRegionId,
                    this.plannerScoreState.appendFrameAccess(accessFrame)
            );
        }

        @NotNull
        protected ArchivePlanningState advancePlanningState(@NotNull PersistentAppendMap<String, DeferredPayload> payloadsByPath,
                                                            @NotNull PersistentAppendMap<String, String> payloadPathsByFingerprint,
                                                            @NotNull PersistentAppendList<AfmaChunkedPayloadHelper.PayloadAccessFrame> accessTrace,
                                                            int nextSyntheticPayloadId, int nextKeyframeRegionId,
                                                            int currentIntroKeyframeRegionId, int currentMainKeyframeRegionId,
                                                            @NotNull IncrementalArchiveScoreState nextPlannerScoreState) {
            long estimatedArchiveDelta = nextPlannerScoreState.predictedArchiveBytes() - this.plannerScoreState.predictedArchiveBytes();
            long scoredArchiveDelta = nextPlannerScoreState.scoredArchiveBytes() - this.plannerScoreState.scoredArchiveBytes();
            return new ArchivePlanningState(
                    payloadsByPath,
                    payloadPathsByFingerprint,
                    accessTrace,
                    nextSyntheticPayloadId,
                    nextKeyframeRegionId,
                    currentIntroKeyframeRegionId,
                    currentMainKeyframeRegionId,
                    nextPlannerScoreState,
                    this.exactRefinementCache,
                    false,
                    this.estimatedArchiveBytes + estimatedArchiveDelta,
                    this.scoredArchiveBytes + scoredArchiveDelta
            );
        }

        @NotNull
        protected ArchivePlanningState refineArchiveMetrics() {
            if (this.archiveMetricsExact) {
                return this;
            }

            ExactArchiveRefinementKey refinementKey = new ExactArchiveRefinementKey(this.payloadsByPath.tailIdentity(), this.accessTrace.tailIdentity());
            ExactArchiveRefinementMetrics cachedMetrics = this.exactRefinementCache.get(refinementKey);
            if (cachedMetrics != null) {
                return this.withExactMetrics(cachedMetrics);
            }

            LinkedHashMap<String, AfmaStoredPayload> materializedPayloadsByPath = this.materializePayloadsByPath();
            AfmaChunkedPayloadHelper.ArchivePackingHints exactPackingHints = this.materializePackingHints();
            AfmaChunkedPayloadHelper.PackedPayloadArchive simulatedArchive = AfmaChunkedPayloadHelper.simulateArchiveLayout(materializedPayloadsByPath, exactPackingHints);
            ExactArchiveRefinementMetrics exactMetrics = new ExactArchiveRefinementMetrics(
                    IncrementalArchiveScoreState.fromExactArchive(materializedPayloadsByPath, exactPackingHints, simulatedArchive),
                    simulatedArchive.packingMetrics().predictedArchiveBytes(),
                    simulatedArchive.packingMetrics().scoredArchiveBytes()
            );
            this.exactRefinementCache.put(refinementKey, exactMetrics);
            return this.withExactMetrics(exactMetrics);
        }

        @NotNull
        protected ArchivePlanningState withExactMetrics(@NotNull ExactArchiveRefinementMetrics exactMetrics) {
            return new ArchivePlanningState(
                    this.payloadsByPath,
                    this.payloadPathsByFingerprint,
                    this.accessTrace,
                    this.nextSyntheticPayloadId,
                    this.nextKeyframeRegionId,
                    this.currentIntroKeyframeRegionId,
                    this.currentMainKeyframeRegionId,
                    exactMetrics.plannerScoreState(),
                    this.exactRefinementCache,
                    true,
                    exactMetrics.estimatedArchiveBytes(),
                    exactMetrics.scoredArchiveBytes()
            );
        }

        @NotNull
        protected LinkedHashMap<String, AfmaStoredPayload> materializePayloadsByPath() {
            LinkedHashMap<String, AfmaStoredPayload> materializedPayloadsByPath = new LinkedHashMap<>(this.payloadsByPath.size());
            this.payloadsByPath.forEachInInsertionOrder((payloadPath, payload) -> {
                try {
                    materializedPayloadsByPath.put(payloadPath, payload.materialize());
                } catch (IOException ex) {
                    throw new IllegalStateException("Failed to materialize AFMA planner payload " + payloadPath, ex);
                }
            });
            return materializedPayloadsByPath;
        }

        @NotNull
        protected AfmaChunkedPayloadHelper.ArchivePackingHints materializePackingHints() {
            if (this.accessTrace.isEmpty()) {
                return AfmaChunkedPayloadHelper.ArchivePackingHints.empty();
            }
            return new AfmaChunkedPayloadHelper.ArchivePackingHints(this.accessTrace.materialize());
        }

    }

    protected record Dimension(int width, int height) {
    }

    protected record LoadedDimensionFrame(@NotNull Dimension dimension, @NotNull AfmaPixelFrame frame) {
    }

    protected record PlannedSequence(@NotNull List<AfmaFrameDescriptor> frames, long defaultDelayMs,
                                     @NotNull Map<Integer, Long> customFrameTimes,
                                     @NotNull ArchivePlanningState archiveState) {
    }

    protected record PlannedTimedFrame(@NotNull AfmaFrameDescriptor descriptor, long delayMs) {

        @NotNull
        public PlannedTimedFrame withAdditionalDelay(long additionalDelayMs) {
            return new PlannedTimedFrame(this.descriptor, addDelaysSaturating(this.delayMs, Math.max(1L, additionalDelayMs)));
        }

    }

    protected record AdaptiveTiming(long defaultDelayMs, @NotNull LinkedHashMap<Integer, Long> customFrameTimes) {
    }

    protected record PerceptualDriftStats(double averageError, int maxVisibleColorDelta, int maxAlphaDelta) {

        public boolean isExact() {
            return this.averageError <= 0D
                    && this.maxVisibleColorDelta <= 0
                    && this.maxAlphaDelta <= 0;
        }

    }

    protected record ResidualPayloadData(@NotNull byte[] payloadBytes, @NotNull AfmaResidualPayload metadata, int complexityScore) {
    }

    protected record SparseResidualPayloadData(@NotNull byte[] layoutPayload, @NotNull byte[] residualPayload, int changedPixelCount,
                                               @NotNull AfmaSparseLayoutCodec layoutCodec, int channels,
                                               @NotNull AfmaResidualCodec residualCodec,
                                               @NotNull AfmaAlphaResidualMode alphaMode,
                                               int alphaChangedPixelCount,
                                               int complexityScore) {

        @NotNull
        public AfmaSparsePayload toMetadata(@Nullable String residualPayloadPath) {
            return new AfmaSparsePayload(residualPayloadPath, this.changedPixelCount, this.channels,
                    this.layoutCodec, this.residualCodec, this.alphaMode, this.alphaChangedPixelCount);
        }
    }

    protected record SparseLayoutCandidate(@NotNull AfmaSparseLayoutCodec layoutCodec, @NotNull byte[] layoutPayload, int complexityScore,
                                           long estimatedArchiveBytes) {

        protected SparseLayoutCandidate(@NotNull AfmaSparseLayoutCodec layoutCodec, @NotNull byte[] layoutPayload, int complexityScore) {
            this(layoutCodec, layoutPayload, complexityScore, AfmaPayloadMetricsHelper.estimateArchiveBytes(layoutPayload));
        }

        public boolean isBetterThan(@NotNull SparseLayoutCandidate other) {
            if (this.estimatedArchiveBytes != other.estimatedArchiveBytes) {
                return this.estimatedArchiveBytes < other.estimatedArchiveBytes;
            }
            if (this.complexityScore != other.complexityScore) {
                return this.complexityScore < other.complexityScore;
            }
            return this.layoutPayload.length < other.layoutPayload.length;
        }
    }

    protected record SparseLayoutEstimate(long bitmaskBytes, long rowSpanBytes, long tileMaskBytes, long coordListBytes) {
    }

    protected record SparseLayoutPlan(@NotNull AfmaSparseLayoutCodec layoutCodec, long estimatedRawBytes) {
    }

    protected record MotionTileStats(int changedPixelCount, boolean includeAlpha) {
    }

    protected record TileMotionSAD(@NotNull AfmaRectCopyDetector.MotionVector vector, long sad) {
    }

    protected sealed interface BlockInterTileEncoding permits SkipBlockInterTileEncoding, CopyBlockInterTileEncoding,
            DenseBlockInterTileEncoding, SparseBlockInterTileEncoding, RawBlockInterTileEncoding {

        @NotNull
        AfmaBlockInterPayloadHelper.TileMode mode();
    }

    protected record SkipBlockInterTileEncoding() implements BlockInterTileEncoding {

        @Override
        @NotNull
        public AfmaBlockInterPayloadHelper.TileMode mode() {
            return AfmaBlockInterPayloadHelper.TileMode.SKIP;
        }
    }

    protected record CopyBlockInterTileEncoding(int dx, int dy) implements BlockInterTileEncoding {

        @Override
        @NotNull
        public AfmaBlockInterPayloadHelper.TileMode mode() {
            return AfmaBlockInterPayloadHelper.TileMode.COPY;
        }
    }

    protected record DenseBlockInterTileEncoding(int dx, int dy, int dstX, int dstY, int width, int height, int channels)
            implements BlockInterTileEncoding {

        public DenseBlockInterTileEncoding {
            if (!AfmaResidualPayloadHelper.isValidChannelCount(channels)) {
                throw new IllegalArgumentException("AFMA block_inter dense tile channels are invalid: " + channels);
            }
        }

        @Override
        @NotNull
        public AfmaBlockInterPayloadHelper.TileMode mode() {
            return AfmaBlockInterPayloadHelper.TileMode.COPY_DENSE;
        }
    }

    protected record SparseBlockInterTileEncoding(int dx, int dy, @NotNull SparseResidualPayloadData sparsePayload)
            implements BlockInterTileEncoding {

        public SparseBlockInterTileEncoding {
            Objects.requireNonNull(sparsePayload);
        }

        @Override
        @NotNull
        public AfmaBlockInterPayloadHelper.TileMode mode() {
            return AfmaBlockInterPayloadHelper.TileMode.COPY_SPARSE;
        }
    }

    protected record RawBlockInterTileEncoding(int dstX, int dstY, int width, int height, int channels)
            implements BlockInterTileEncoding {

        public RawBlockInterTileEncoding {
            if (!AfmaResidualPayloadHelper.isValidChannelCount(channels)) {
                throw new IllegalArgumentException("AFMA block_inter raw tile channels are invalid: " + channels);
            }
        }

        @Override
        @NotNull
        public AfmaBlockInterPayloadHelper.TileMode mode() {
            return AfmaBlockInterPayloadHelper.TileMode.RAW;
        }
    }

    protected record BlockInterTileCandidate(@NotNull BlockInterTileEncoding encoding, long estimatedBytes,
                                             int complexityScore) {
    }

    protected record MotionSearchSeed(@NotNull AfmaRectCopyDetector.MotionVector motionVector,
                                      @NotNull BlockInterTileCandidate candidate) {
    }

    protected record BlockInterRegionAnalysis(int totalTileCount, int changedTileCount, @NotNull boolean[] changedTiles) {

        public int identicalTileCount() {
            return this.totalTileCount - this.changedTileCount;
        }

        public boolean isTileChanged(int tileIndex) {
            return this.changedTiles[tileIndex];
        }

    }

    @FunctionalInterface
    public interface ProgressListener {
        void update(@NotNull String detail, double progress);
    }

}
