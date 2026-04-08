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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;

public class AfmaEncodePlanner {

    protected static final int MIN_SPARSE_DELTA_CHANGED_PIXELS = 1;
    protected static final double MAX_SPARSE_DELTA_CHANGED_DENSITY = 0.75D;
    protected static final int BLOCK_INTER_TILE_SIZE = 16;
    protected static final int MIN_PARALLEL_BLOCK_INTER_TILES = 32;
    protected static final int BLOCK_INTER_LOCAL_REFINEMENT_RADIUS = 2;
    protected static final int BLOCK_INTER_LOCAL_REFINEMENT_SEEDS = 3;
    protected static final int BLOCK_INTER_LOCAL_REFINEMENT_PASSES = 2;
    protected static final int PLANNER_DEFLATE_TAIL_BYTES = 32 * 1024;
    protected static final int ESTIMATED_ZIP_CHUNK_OVERHEAD_BYTES = 96;
    protected static final byte[] EMPTY_BYTES = new byte[0];

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
        LinkedHashMap<String, byte[]> payloads = new LinkedHashMap<>();
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
                                           @NotNull LinkedHashMap<String, byte[]> payloads,
                                           @NotNull Map<String, String> payloadPathsByFingerprint,
                                           @Nullable BooleanSupplier cancellationRequested, @Nullable ProgressListener progressListener,
                                           int startOffset, int totalFrameCount, @Nullable AfmaPixelFrame firstFrameOverride) throws IOException {
        List<PlannedTimedFrame> plannedFrames = new ArrayList<>();
        if (sequence.isEmpty()) {
            return this.buildPlannedSequence(plannedFrames, this.resolveSequenceDefaultDelay(options, introSequence), initialArchiveState);
        }

        BeamPlanningState baseState = BeamPlanningState.root(initialArchiveState);
        AfmaPixelFrame preloadedFrame = firstFrameOverride;
        int plannerWindowFrames = Math.max(1, options.getPlannerSearchWindowFrames());
        try {
            for (int windowStart = 0; windowStart < sequence.size(); windowStart += plannerWindowFrames) {
                checkCancelled(cancellationRequested);
                int windowEnd = Math.min(sequence.size(), windowStart + plannerWindowFrames);
                List<AfmaPixelFrame> windowFrames = this.loadPlanningWindowFrames(sequence, windowStart, windowEnd, dimension, preloadedFrame, cancellationRequested);
                preloadedFrame = null;
                BeamPlanningState bestWindowState = this.planWindow(windowFrames, windowStart, introSequence, options, copyDetector, baseState, cancellationRequested);
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

    @NotNull
    protected List<AfmaPixelFrame> loadPlanningWindowFrames(@NotNull AfmaSourceSequence sequence, int windowStart, int windowEnd,
                                                            @NotNull Dimension dimension, @Nullable AfmaPixelFrame firstFrameOverride,
                                                            @Nullable BooleanSupplier cancellationRequested) throws IOException {
        ArrayList<AfmaPixelFrame> windowFrames = new ArrayList<>(Math.max(0, windowEnd - windowStart));
        AfmaPixelFrame preloadedFrame = firstFrameOverride;
        for (int frameIndex = windowStart; frameIndex < windowEnd; frameIndex++) {
            checkCancelled(cancellationRequested);
            File frameFile = Objects.requireNonNull(sequence.getFrame(frameIndex));
            AfmaPixelFrame sourceFrame;
            if ((frameIndex == 0) && (preloadedFrame != null)) {
                sourceFrame = preloadedFrame;
                preloadedFrame = null;
            } else {
                sourceFrame = this.frameNormalizer.loadFrame(frameFile);
            }
            if ((sourceFrame.getWidth() != dimension.width()) || (sourceFrame.getHeight() != dimension.height())) {
                throw new IOException("AFMA source frame dimensions do not match the expected canvas size: " + frameFile.getAbsolutePath());
            }
            windowFrames.add(sourceFrame);
        }
        return windowFrames;
    }

    @NotNull
    protected BeamPlanningState planWindow(@NotNull List<AfmaPixelFrame> windowFrames, int windowStartFrameIndex,
                                           boolean introSequence, @NotNull AfmaEncodeOptions options,
                                           @NotNull AfmaRectCopyDetector copyDetector, @NotNull BeamPlanningState baseState,
                                           @Nullable BooleanSupplier cancellationRequested) throws IOException {
        List<BeamPlanningState> beam = new ArrayList<>();
        beam.add(baseState.toCommittedBaseState());
        for (int localFrameIndex = 0; localFrameIndex < windowFrames.size(); localFrameIndex++) {
            checkCancelled(cancellationRequested);
            AfmaPixelFrame sourceFrame = windowFrames.get(localFrameIndex);
            int absoluteFrameIndex = windowStartFrameIndex + localFrameIndex;
            long frameDelayMs = this.resolveSourceFrameDelay(options, introSequence, absoluteFrameIndex);
            ArrayList<BeamPlanningState> expandedBeam = new ArrayList<>();
            for (BeamPlanningState state : beam) {
                AfmaPixelFrame previousFrame = state.previousFrame();
                AfmaPixelFrame workingFrame = sourceFrame;
                if ((previousFrame != null) && options.isNearLosslessEnabled()
                        && this.shouldAllowPerceptualContinuation(state.framesSinceKeyframe(), options)) {
                    workingFrame = this.applyNearLosslessTemporalMerge(previousFrame, sourceFrame, options.getNearLosslessMaxChannelDelta());
                }

                if ((previousFrame != null) && state.emittedFrameAvailable()
                        && options.isDuplicateFrameElision()
                        && AfmaPixelFrameHelper.isIdentical(previousFrame, workingFrame)) {
                    BeamPlanningState duplicateState = this.evaluateDuplicateContinuation(state, sourceFrame, frameDelayMs, options);
                    if (duplicateState != null) {
                        expandedBeam.add(duplicateState);
                    }
                }

                List<PlannedCandidate> candidates = this.collectWindowCandidates(previousFrame, sourceFrame, workingFrame,
                        introSequence, absoluteFrameIndex, state.framesSinceKeyframe(), state.decodeComplexitySinceKeyframe(),
                        options, copyDetector, state.archiveState());
                for (PlannedCandidate candidate : candidates) {
                    BeamPlanningState candidateState = this.evaluateCandidateTransition(state, candidate, sourceFrame, workingFrame,
                            frameDelayMs, introSequence, options);
                    if (candidateState != null) {
                        expandedBeam.add(candidateState);
                    }
                }
            }

            beam = this.prunePlanningBeam(expandedBeam, options.getPlannerBeamWidth());
            if (beam.isEmpty()) {
                throw new IOException("AFMA planner failed to find a valid candidate for source frame " + (absoluteFrameIndex + 1));
            }
        }
        return beam.get(0);
    }

    @NotNull
    protected List<PlannedCandidate> collectWindowCandidates(@Nullable AfmaPixelFrame previousFrame,
                                                             @NotNull AfmaPixelFrame sourceFrame,
                                                             @NotNull AfmaPixelFrame workingFrame,
                                                             boolean introSequence, int frameIndex, int framesSinceKeyframe,
                                                             int decodeComplexitySinceKeyframe,
                                                             @NotNull AfmaEncodeOptions options,
                                                             @NotNull AfmaRectCopyDetector copyDetector,
                                                             @NotNull ArchivePlanningState archiveState) throws IOException {
        PlannedCandidate fullCandidate = this.createFullCandidate(workingFrame, introSequence, frameIndex, options, true, ReferenceBase.WORKING_FRAME);
        PlannedCandidate exactFullCandidate = fullCandidate.isExactRelativeToSourceFrame(sourceFrame, workingFrame)
                ? fullCandidate
                : this.createExactFullCandidate(sourceFrame, introSequence, frameIndex, options);
        ArrayList<PlannedCandidate> candidates = new ArrayList<>();
        if ((previousFrame == null) || this.isHardKeyframeRefreshRequired(framesSinceKeyframe, decodeComplexitySinceKeyframe, options)) {
            candidates.add(exactFullCandidate);
            return candidates;
        }

        candidates.add(fullCandidate);
        if (!fullCandidate.isExactRelativeToSourceFrame(sourceFrame, workingFrame)) {
            candidates.add(exactFullCandidate);
        }

        AfmaRect deltaBounds = AfmaPixelFrameHelper.findDifferenceBounds(previousFrame, workingFrame);
        if (deltaBounds != null) {
            PlannedCandidate deltaCandidate = this.createDeltaCandidate(workingFrame, introSequence, frameIndex, deltaBounds, options);
            if ((deltaCandidate != null) && this.shouldKeepComplexCandidate(deltaCandidate, fullCandidate,
                    deltaBounds.area(), workingFrame.getWidth(), workingFrame.getHeight(),
                    options.getMaxDeltaAreaRatioWithoutStrongSavings(), options, archiveState, introSequence)) {
                candidates.add(deltaCandidate);
            }

            PlannedCandidate residualDeltaCandidate = this.createResidualDeltaCandidate(previousFrame, workingFrame, introSequence, frameIndex, deltaBounds);
            if ((residualDeltaCandidate != null) && this.shouldKeepResidualCandidate(residualDeltaCandidate, fullCandidate,
                    deltaBounds.area(), workingFrame.getWidth(), workingFrame.getHeight(),
                    options.getMaxDeltaAreaRatioWithoutStrongSavings(), options, archiveState, introSequence)) {
                candidates.add(residualDeltaCandidate);
            }

            PlannedCandidate sparseDeltaCandidate = this.createSparseDeltaCandidate(previousFrame, workingFrame, introSequence, frameIndex, deltaBounds);
            if ((sparseDeltaCandidate != null) && this.shouldKeepSparseCandidate(sparseDeltaCandidate, fullCandidate,
                    deltaBounds.area(), workingFrame.getWidth(), workingFrame.getHeight(),
                    options.getMaxDeltaAreaRatioWithoutStrongSavings(), options, archiveState, introSequence)) {
                candidates.add(sparseDeltaCandidate);
            }
        }

        if (options.isRectCopyEnabled()) {
            AfmaRectCopyDetector.Detection detection = copyDetector.detect(previousFrame, workingFrame);
            if (detection != null) {
                PlannedCandidate copyCandidate = this.createCopyCandidate(workingFrame, introSequence, frameIndex, detection, options);
                long patchArea = (detection.patchBounds() != null) ? detection.patchBounds().area() : 0L;
                if ((copyCandidate != null) && this.shouldKeepComplexCandidate(copyCandidate, fullCandidate,
                        patchArea, workingFrame.getWidth(), workingFrame.getHeight(),
                        options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options, archiveState, introSequence)) {
                    candidates.add(copyCandidate);
                }

                PlannedCandidate copyResidualCandidate = this.createCopyResidualCandidate(previousFrame, workingFrame, introSequence, frameIndex, detection);
                if ((copyResidualCandidate != null) && this.shouldKeepResidualCandidate(copyResidualCandidate, fullCandidate,
                        patchArea, workingFrame.getWidth(), workingFrame.getHeight(),
                        options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options, archiveState, introSequence)) {
                    candidates.add(copyResidualCandidate);
                }

                PlannedCandidate copySparseCandidate = this.createCopySparseCandidate(previousFrame, workingFrame, introSequence, frameIndex, detection);
                if ((copySparseCandidate != null) && this.shouldKeepSparseCandidate(copySparseCandidate, fullCandidate,
                        patchArea, workingFrame.getWidth(), workingFrame.getHeight(),
                        options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options, archiveState, introSequence)) {
                    candidates.add(copySparseCandidate);
                }
            }

            AfmaRectCopyDetector.MultiDetection multiDetection = copyDetector.detectMulti(previousFrame, workingFrame);
            if (multiDetection != null) {
                AfmaPixelFrame multiCopyReferenceFrame = this.buildMultiCopyReferenceFrame(previousFrame, multiDetection.multiCopy());
                PlannedCandidate multiCopyCandidate = this.createMultiCopyCandidate(workingFrame, introSequence, frameIndex, multiDetection, options);
                long patchArea = multiDetection.patchArea();
                if ((multiCopyCandidate != null) && this.shouldKeepComplexCandidate(multiCopyCandidate, fullCandidate,
                        patchArea, workingFrame.getWidth(), workingFrame.getHeight(),
                        options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options, archiveState, introSequence)) {
                    candidates.add(multiCopyCandidate);
                }

                PlannedCandidate multiCopyResidualCandidate = this.createMultiCopyResidualCandidate(multiCopyReferenceFrame, workingFrame,
                        introSequence, frameIndex, multiDetection);
                if ((multiCopyResidualCandidate != null) && this.shouldKeepResidualCandidate(multiCopyResidualCandidate, fullCandidate,
                        patchArea, workingFrame.getWidth(), workingFrame.getHeight(),
                        options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options, archiveState, introSequence)) {
                    candidates.add(multiCopyResidualCandidate);
                }

                PlannedCandidate multiCopySparseCandidate = this.createMultiCopySparseCandidate(multiCopyReferenceFrame, workingFrame,
                        introSequence, frameIndex, multiDetection);
                if ((multiCopySparseCandidate != null) && this.shouldKeepSparseCandidate(multiCopySparseCandidate, fullCandidate,
                        patchArea, workingFrame.getWidth(), workingFrame.getHeight(),
                        options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options, archiveState, introSequence)) {
                    candidates.add(multiCopySparseCandidate);
                }
            }
        }

        if (options.isRectCopyEnabled() && (deltaBounds != null)) {
            PlannedCandidate blockInterCandidate = this.createBlockInterCandidate(previousFrame, workingFrame, introSequence, frameIndex, deltaBounds, copyDetector);
            if ((blockInterCandidate != null) && this.shouldKeepComplexCandidate(blockInterCandidate, fullCandidate,
                    (long) blockInterCandidate.descriptor().getWidth() * blockInterCandidate.descriptor().getHeight(),
                    workingFrame.getWidth(), workingFrame.getHeight(),
                    options.getMaxDeltaAreaRatioWithoutStrongSavings(), options, archiveState, introSequence)) {
                candidates.add(blockInterCandidate);
            }
        }

        return candidates;
    }

    @Nullable
    protected BeamPlanningState evaluateDuplicateContinuation(@NotNull BeamPlanningState state, @NotNull AfmaPixelFrame sourceFrame,
                                                              long frameDelayMs, @NotNull AfmaEncodeOptions options) {
        AfmaPixelFrame previousFrame = state.previousFrame();
        if (previousFrame == null) {
            return null;
        }

        DriftTransition driftTransition = this.evaluateDriftTransition(state.driftState(), sourceFrame, previousFrame, options);
        if (driftTransition == null) {
            return null;
        }
        return state.advanceDelay(frameDelayMs, driftTransition.nextState(), driftTransition.scorePenalty());
    }

    @Nullable
    protected BeamPlanningState evaluateCandidateTransition(@NotNull BeamPlanningState state, @NotNull PlannedCandidate candidate,
                                                            @NotNull AfmaPixelFrame sourceFrame, @NotNull AfmaPixelFrame workingFrame,
                                                            long frameDelayMs, boolean introSequence, @NotNull AfmaEncodeOptions options) {
        AfmaPixelFrame reconstructedFrame = candidate.materializeReferenceFrame(sourceFrame, workingFrame);
        DriftTransition driftTransition = this.evaluateDriftTransition(state.driftState(), sourceFrame, reconstructedFrame, options);
        if (driftTransition == null) {
            return null;
        }

        CandidateArchiveCost archiveCost = state.archiveState().appendCandidate(candidate, introSequence);
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
                                       @NotNull Map<String, byte[]> payloads, @NotNull Map<String, String> payloadPathsByFingerprint) {
        for (PlanningStep planningStep : bestWindowState.stepsInOrder()) {
            if (planningStep.isDelayExtension()) {
                this.extendPlannedFrameDelay(plannedFrames, planningStep.delayMs());
                continue;
            }

            PlannedCandidate finalizedCandidate = Objects.requireNonNull(planningStep.candidate()).internPayloads(payloads, payloadPathsByFingerprint);
            plannedFrames.add(new PlannedTimedFrame(finalizedCandidate.descriptor(), planningStep.delayMs()));
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

    @Nullable
    protected DriftTransition evaluateDriftTransition(@NotNull DriftState currentDriftState,
                                                      @NotNull AfmaPixelFrame sourceFrame,
                                                      @NotNull AfmaPixelFrame reconstructedFrame,
                                                      @NotNull AfmaEncodeOptions options) {
        PerceptualDriftStats driftStats = this.measurePerceptualDrift(sourceFrame, reconstructedFrame);
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

        AfmaRect deltaBounds = AfmaPixelFrameHelper.findDifferenceBounds(previousFrame, currentFrame);
        if (deltaBounds != null) {
            PlannedCandidate deltaCandidate = this.createDeltaCandidate(currentFrame, introSequence, frameIndex, deltaBounds, options);
            if ((deltaCandidate != null) && this.shouldKeepComplexCandidate(deltaCandidate, fullCandidate,
                    deltaBounds.area(), currentFrame.getWidth(), currentFrame.getHeight(),
                    options.getMaxDeltaAreaRatioWithoutStrongSavings(), options, payloadPathsByFingerprint)) {
                candidates.add(deltaCandidate);
            }

            PlannedCandidate residualDeltaCandidate = this.createResidualDeltaCandidate(previousFrame, currentFrame, introSequence, frameIndex, deltaBounds);
            if ((residualDeltaCandidate != null) && this.shouldKeepResidualCandidate(residualDeltaCandidate, fullCandidate,
                    deltaBounds.area(), currentFrame.getWidth(), currentFrame.getHeight(),
                    options.getMaxDeltaAreaRatioWithoutStrongSavings(), options, payloadPathsByFingerprint)) {
                candidates.add(residualDeltaCandidate);
            }

            PlannedCandidate sparseDeltaCandidate = this.createSparseDeltaCandidate(previousFrame, currentFrame, introSequence, frameIndex, deltaBounds);
            if ((sparseDeltaCandidate != null) && this.shouldKeepSparseCandidate(sparseDeltaCandidate, fullCandidate,
                    deltaBounds.area(), currentFrame.getWidth(), currentFrame.getHeight(),
                    options.getMaxDeltaAreaRatioWithoutStrongSavings(), options, payloadPathsByFingerprint)) {
                candidates.add(sparseDeltaCandidate);
            }
        }

        if (options.isRectCopyEnabled()) {
            AfmaRectCopyDetector.Detection detection = copyDetector.detect(previousFrame, currentFrame);
            if (detection != null) {
                PlannedCandidate copyCandidate = this.createCopyCandidate(currentFrame, introSequence, frameIndex, detection, options);
                long patchArea = (detection.patchBounds() != null) ? detection.patchBounds().area() : 0L;
                if ((copyCandidate != null) && this.shouldKeepComplexCandidate(copyCandidate, fullCandidate,
                        patchArea, currentFrame.getWidth(), currentFrame.getHeight(),
                        options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options, payloadPathsByFingerprint)) {
                    candidates.add(copyCandidate);
                }

                PlannedCandidate copyResidualCandidate = this.createCopyResidualCandidate(previousFrame, currentFrame, introSequence, frameIndex, detection);
                if ((copyResidualCandidate != null) && this.shouldKeepResidualCandidate(copyResidualCandidate, fullCandidate,
                        patchArea, currentFrame.getWidth(), currentFrame.getHeight(),
                        options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options, payloadPathsByFingerprint)) {
                    candidates.add(copyResidualCandidate);
                }

                PlannedCandidate copySparseCandidate = this.createCopySparseCandidate(previousFrame, currentFrame, introSequence, frameIndex, detection);
                if ((copySparseCandidate != null) && this.shouldKeepSparseCandidate(copySparseCandidate, fullCandidate,
                        patchArea, currentFrame.getWidth(), currentFrame.getHeight(),
                        options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options, payloadPathsByFingerprint)) {
                    candidates.add(copySparseCandidate);
                }
            }

            AfmaRectCopyDetector.MultiDetection multiDetection = copyDetector.detectMulti(previousFrame, currentFrame);
            if (multiDetection != null) {
                AfmaPixelFrame multiCopyReferenceFrame = this.buildMultiCopyReferenceFrame(previousFrame, multiDetection.multiCopy());
                PlannedCandidate multiCopyCandidate = this.createMultiCopyCandidate(currentFrame, introSequence, frameIndex, multiDetection, options);
                long patchArea = multiDetection.patchArea();
                if ((multiCopyCandidate != null) && this.shouldKeepComplexCandidate(multiCopyCandidate, fullCandidate,
                        patchArea, currentFrame.getWidth(), currentFrame.getHeight(),
                        options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options, payloadPathsByFingerprint)) {
                    candidates.add(multiCopyCandidate);
                }

                PlannedCandidate multiCopyResidualCandidate = this.createMultiCopyResidualCandidate(multiCopyReferenceFrame, currentFrame,
                        introSequence, frameIndex, multiDetection);
                if ((multiCopyResidualCandidate != null) && this.shouldKeepResidualCandidate(multiCopyResidualCandidate, fullCandidate,
                        patchArea, currentFrame.getWidth(), currentFrame.getHeight(),
                        options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options, payloadPathsByFingerprint)) {
                    candidates.add(multiCopyResidualCandidate);
                }

                PlannedCandidate multiCopySparseCandidate = this.createMultiCopySparseCandidate(multiCopyReferenceFrame, currentFrame,
                        introSequence, frameIndex, multiDetection);
                if ((multiCopySparseCandidate != null) && this.shouldKeepSparseCandidate(multiCopySparseCandidate, fullCandidate,
                        patchArea, currentFrame.getWidth(), currentFrame.getHeight(),
                        options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options, payloadPathsByFingerprint)) {
                    candidates.add(multiCopySparseCandidate);
                }
            }
        }

        if (options.isRectCopyEnabled() && (deltaBounds != null)) {
            PlannedCandidate blockInterCandidate = this.createBlockInterCandidate(previousFrame, currentFrame, introSequence, frameIndex, deltaBounds, copyDetector);
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
    protected PerceptualDriftStats measurePerceptualDrift(@NotNull AfmaPixelFrame sourceFrame, @NotNull AfmaPixelFrame reconstructedFrame) {
        AfmaPixelFrameHelper.ensureSameSize(sourceFrame, reconstructedFrame);
        int[] sourcePixels = sourceFrame.getPixelsUnsafe();
        int[] reconstructedPixels = reconstructedFrame.getPixelsUnsafe();
        double totalError = 0D;
        int maxVisibleColorDelta = 0;
        int maxAlphaDelta = 0;
        for (int pixelIndex = 0; pixelIndex < sourcePixels.length; pixelIndex++) {
            int sourceColor = sourcePixels[pixelIndex];
            int reconstructedColor = reconstructedPixels[pixelIndex];
            int sourceAlpha = (sourceColor >>> 24) & 0xFF;
            int reconstructedAlpha = (reconstructedColor >>> 24) & 0xFF;
            int alphaDelta = Math.abs(sourceAlpha - reconstructedAlpha);
            if (alphaDelta > maxAlphaDelta) {
                maxAlphaDelta = alphaDelta;
            }

            int visibilityAlpha = Math.max(sourceAlpha, reconstructedAlpha);
            if (visibilityAlpha <= 0) {
                continue;
            }

            int redDelta = channelDifference(sourceColor >> 16, reconstructedColor >> 16);
            int greenDelta = channelDifference(sourceColor >> 8, reconstructedColor >> 8);
            int blueDelta = channelDifference(sourceColor, reconstructedColor);
            int visibleColorDelta = Math.max(redDelta, Math.max(greenDelta, blueDelta));
            if (visibleColorDelta > maxVisibleColorDelta) {
                maxVisibleColorDelta = visibleColorDelta;
            }

            double visibilityWeight = visibilityAlpha / 255.0D;
            totalError += (alphaDelta * 2.0D) + ((redDelta + greenDelta + blueDelta) * visibilityWeight);
        }
        return new PerceptualDriftStats(totalError / Math.max(1, sourcePixels.length), maxVisibleColorDelta, maxAlphaDelta);
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
    protected AfmaBinIntraPayloadHelper.EncodedPayloadResult encodeBinIntraPayload(@NotNull AfmaPixelFrame frame,
                                                                                    @NotNull AfmaEncodeOptions options,
                                                                                    boolean allowPerceptual) throws IOException {
        return this.encodeBinIntraPayload(frame.getWidth(), frame.getHeight(), frame.getPixelsUnsafe(), 0, frame.getWidth(), options, allowPerceptual);
    }

    @NotNull
    protected AfmaBinIntraPayloadHelper.EncodedPayloadResult encodeBinIntraPayloadRegion(@NotNull AfmaPixelFrame frame,
                                                                                          int x, int y, int width, int height,
                                                                                          @NotNull AfmaEncodeOptions options,
                                                                                          boolean allowPerceptual) throws IOException {
        return this.encodeBinIntraPayload(width, height, frame.getPixelsUnsafe(), (y * frame.getWidth()) + x, frame.getWidth(), options, allowPerceptual);
    }

    @NotNull
    protected AfmaBinIntraPayloadHelper.EncodedPayloadResult encodeBinIntraPayload(int width, int height,
                                                                                    @NotNull int[] pixels, int offset, int scanlineStride,
                                                                                    @NotNull AfmaEncodeOptions options,
                                                                                    boolean allowPerceptual) throws IOException {
        AfmaBinIntraPayloadHelper.EncodePreferences preferences = (allowPerceptual && options.isPerceptualBinIntraEnabled())
                ? AfmaBinIntraPayloadHelper.EncodePreferences.perceptual(
                options.getPerceptualBinIntraMaxVisibleColorDelta(),
                options.getPerceptualBinIntraMaxAlphaDelta(),
                options.getPerceptualBinIntraMaxAverageError()
        )
                : AfmaBinIntraPayloadHelper.EncodePreferences.lossless();
        return AfmaBinIntraPayloadHelper.encodePayloadDetailed(width, height, pixels, offset, scanlineStride, preferences);
    }

    @NotNull
    protected PlannedCandidate createFullCandidate(@NotNull AfmaPixelFrame currentFrame, boolean introSequence, int frameIndex,
                                                   @NotNull AfmaEncodeOptions options, boolean allowPerceptual,
                                                   @NotNull ReferenceBase referenceBase) throws IOException {
        String payloadPath = this.buildPayloadPath(introSequence, frameIndex);
        AfmaBinIntraPayloadHelper.EncodedPayloadResult encodedPayload = this.encodeBinIntraPayload(currentFrame, options, allowPerceptual);
        AfmaRect referencePatchBounds = encodedPayload.lossless() ? null : new AfmaRect(0, 0, currentFrame.getWidth(), currentFrame.getHeight());
        return new PlannedCandidate(
                AfmaFrameDescriptor.full(payloadPath),
                payloadPath,
                encodedPayload.payloadBytes(),
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
        AfmaBinIntraPayloadHelper.EncodedPayloadResult encodedPayload = this.encodeBinIntraPayloadRegion(
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
                encodedPayload.payloadBytes(),
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
                                                            boolean introSequence, int frameIndex, @NotNull AfmaRect deltaBounds) {
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
                residualPayload.payloadBytes(),
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
                sparsePayload.layoutPayload(),
                PayloadKind.RAW,
                false,
                residualPayloadPath,
                sparsePayload.residualPayload(),
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
                                                           @NotNull AfmaRectCopyDetector.Detection detection) {
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
                residualPayload.payloadBytes(),
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
                sparsePayload.layoutPayload(),
                PayloadKind.RAW,
                false,
                residualPayloadPath,
                sparsePayload.residualPayload(),
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
                                                                @NotNull AfmaRectCopyDetector.MultiDetection detection) {
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
                residualPayload.payloadBytes(),
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
                sparsePayload.layoutPayload(),
                PayloadKind.RAW,
                false,
                residualPayloadPath,
                sparsePayload.residualPayload(),
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
        ArrayList<SparseLayoutCandidate> candidates = new ArrayList<>(4);
        candidates.add(new SparseLayoutCandidate(
                AfmaSparseLayoutCodec.BITMASK,
                AfmaSparsePayloadHelper.buildBitmaskLayout(width, height, changedIndices, changedPixelCount),
                AfmaSparseLayoutCodec.BITMASK.getComplexityScore()
        ));
        candidates.add(new SparseLayoutCandidate(
                AfmaSparseLayoutCodec.ROW_SPANS,
                AfmaSparsePayloadHelper.buildRowSpanLayout(width, changedIndices, changedPixelCount),
                AfmaSparseLayoutCodec.ROW_SPANS.getComplexityScore()
        ));
        candidates.add(new SparseLayoutCandidate(
                AfmaSparseLayoutCodec.TILE_MASK,
                AfmaSparsePayloadHelper.buildTileMaskLayout(width, height, changedIndices, changedPixelCount),
                AfmaSparseLayoutCodec.TILE_MASK.getComplexityScore()
        ));
        candidates.add(new SparseLayoutCandidate(
                AfmaSparseLayoutCodec.COORD_LIST,
                AfmaSparsePayloadHelper.buildCoordListLayout(changedIndices, changedPixelCount),
                AfmaSparseLayoutCodec.COORD_LIST.getComplexityScore()
        ));

        SparseLayoutCandidate bestCandidate = null;
        for (SparseLayoutCandidate candidate : candidates) {
            if ((bestCandidate == null) || candidate.isBetterThan(bestCandidate)) {
                bestCandidate = candidate;
            }
        }
        return Objects.requireNonNull(bestCandidate, "Failed to choose an AFMA sparse layout");
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
        byte[] payloadBytes = null;
        int[] referencePatchPixels = null;

        if (patchBounds != null) {
            AfmaBinIntraPayloadHelper.EncodedPayloadResult encodedPayload = this.encodeBinIntraPayloadRegion(
                    currentFrame,
                    patchBounds.x(),
                    patchBounds.y(),
                    patchBounds.width(),
                    patchBounds.height(),
                    options,
                    true
            );
            payloadBytes = encodedPayload.payloadBytes();
            referencePatchPixels = encodedPayload.lossless() ? null : encodedPayload.reconstructedPixels();
        }

        return new PlannedCandidate(
                AfmaFrameDescriptor.copyRectPatch(copyRect, patchRegion),
                null,
                null,
                null,
                false,
                payloadPath,
                payloadBytes,
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
        byte[] payloadBytes = null;
        int[] referencePatchPixels = null;

        if (patchBounds != null) {
            AfmaBinIntraPayloadHelper.EncodedPayloadResult encodedPayload = this.encodeBinIntraPayloadRegion(
                    currentFrame,
                    patchBounds.x(),
                    patchBounds.y(),
                    patchBounds.width(),
                    patchBounds.height(),
                    options,
                    true
            );
            payloadBytes = encodedPayload.payloadBytes();
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
                payloadBytes,
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
    protected PlannedCandidate createBlockInterCandidate(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                                         boolean introSequence, int frameIndex, @NotNull AfmaRect deltaBounds,
                                                         @NotNull AfmaRectCopyDetector copyDetector) throws IOException {
        AfmaRect regionBounds = this.alignBoundsToTileGrid(deltaBounds, BLOCK_INTER_TILE_SIZE, currentFrame.getWidth(), currentFrame.getHeight());
        List<AfmaRectCopyDetector.MotionVector> motionVectors = copyDetector.collectMotionVectors(previousFrame, currentFrame, true);
        if (motionVectors.isEmpty()) {
            return null;
        }

        int tileCountX = AfmaBlockInterPayloadHelper.tileCount(regionBounds.width(), BLOCK_INTER_TILE_SIZE);
        int tileCountY = AfmaBlockInterPayloadHelper.tileCount(regionBounds.height(), BLOCK_INTER_TILE_SIZE);
        if ((tileCountX <= 0) || (tileCountY <= 0)) {
            return null;
        }

        int totalTileCount = tileCountX * tileCountY;
        AfmaBlockInterPayloadHelper.TileOperation[] tileOperations = new AfmaBlockInterPayloadHelper.TileOperation[totalTileCount];
        if (totalTileCount >= MIN_PARALLEL_BLOCK_INTER_TILES) {
            IntStream.range(0, totalTileCount).parallel().forEach(tileIndex ->
                    tileOperations[tileIndex] = this.buildBlockInterTileOperation(previousFrame, currentFrame, motionVectors, regionBounds, tileCountX, tileCountY, tileIndex));
        } else {
            for (int tileIndex = 0; tileIndex < totalTileCount; tileIndex++) {
                tileOperations[tileIndex] = this.buildBlockInterTileOperation(previousFrame, currentFrame, motionVectors, regionBounds, tileCountX, tileCountY, tileIndex);
            }
        }

        String payloadPath = this.buildRawPayloadPath(introSequence, frameIndex, "bi");
        byte[] payloadBytes = AfmaBlockInterPayloadHelper.writePayload(BLOCK_INTER_TILE_SIZE, regionBounds.width(), regionBounds.height(), Arrays.asList(tileOperations));
        return new PlannedCandidate(
                AfmaFrameDescriptor.blockInter(payloadPath, regionBounds.x(), regionBounds.y(), regionBounds.width(), regionBounds.height(), new AfmaBlockInter(BLOCK_INTER_TILE_SIZE)),
                payloadPath,
                payloadBytes,
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
    protected AfmaBlockInterPayloadHelper.TileOperation buildBlockInterTileOperation(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                                                                     @NotNull List<AfmaRectCopyDetector.MotionVector> motionVectors,
                                                                                     @NotNull AfmaRect regionBounds, int tileCountX, int tileCountY,
                                                                                     int tileIndex) {
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
    protected AfmaBlockInterPayloadHelper.TileOperation chooseBestBlockInterTile(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                                                                 int dstX, int dstY, int width, int height,
                                                                                 @NotNull List<AfmaRectCopyDetector.MotionVector> motionVectors) {
        if (this.isTileIdentical(previousFrame, currentFrame, dstX, dstY, width, height)) {
            return new AfmaBlockInterPayloadHelper.TileOperation(AfmaBlockInterPayloadHelper.TileMode.SKIP, 0, 0, 0, 0, null, null, null);
        }

        RawTileData rawTile = this.buildRawTileBytes(currentFrame, dstX, dstY, width, height);
        BlockInterTileCandidate bestCandidate = new BlockInterTileCandidate(
                new AfmaBlockInterPayloadHelper.TileOperation(AfmaBlockInterPayloadHelper.TileMode.RAW, 0, 0, rawTile.channels(), 0, rawTile.payloadBytes(), null, null),
                this.estimateBlockInterTileBytes(AfmaBlockInterPayloadHelper.TileMode.RAW, rawTile.payloadBytes().length, 0),
                0
        );
        List<MotionSearchSeed> refinementSeeds = new ArrayList<>(BLOCK_INTER_LOCAL_REFINEMENT_SEEDS);
        Set<Long> testedVectors = new HashSet<>(motionVectors.size() * 2);

        for (AfmaRectCopyDetector.MotionVector motionVector : motionVectors) {
            testedVectors.add(packMotionVector(motionVector.dx(), motionVector.dy()));
            BlockInterTileCandidate motionCandidate = this.evaluateBlockInterMotionCandidate(previousFrame, currentFrame, dstX, dstY, width, height,
                    motionVector.dx(), motionVector.dy(), bestCandidate);
            if (motionCandidate == null) {
                continue;
            }
            bestCandidate = this.selectBetterBlockInterCandidate(bestCandidate, motionCandidate);
            if (this.isOptimalBlockInterTileCandidate(bestCandidate)) {
                return bestCandidate.operation();
            }
            this.addMotionSearchSeed(refinementSeeds, motionVector, motionCandidate);
        }

        if (!refinementSeeds.isEmpty()) {
            bestCandidate = this.refineBlockInterTileMotion(previousFrame, currentFrame, dstX, dstY, width, height,
                    bestCandidate, refinementSeeds, testedVectors);
        }

        return bestCandidate.operation();
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
                    new AfmaBlockInterPayloadHelper.TileOperation(AfmaBlockInterPayloadHelper.TileMode.COPY, dx, dy, 0, 0, null, null, null),
                    this.estimateBlockInterTileBytes(AfmaBlockInterPayloadHelper.TileMode.COPY, 0, 0),
                    0
            );
        }

        BlockInterTileCandidate bestCandidate = null;
        ResidualPayloadData denseResidual = this.buildMotionResidualPayload(previousFrame, currentFrame, dstX, dstY, srcX, srcY, width, height, tileStats.includeAlpha());
        if (denseResidual != null) {
            bestCandidate = new BlockInterTileCandidate(
                    new AfmaBlockInterPayloadHelper.TileOperation(AfmaBlockInterPayloadHelper.TileMode.COPY_DENSE,
                            dx, dy, denseResidual.metadata().getChannels(), 0, denseResidual.payloadBytes(), null, null),
                    this.estimateBlockInterTileBytes(AfmaBlockInterPayloadHelper.TileMode.COPY_DENSE, denseResidual.payloadBytes().length, 0),
                    denseResidual.complexityScore()
            );
        }

        SparseResidualPayloadData sparseResidual = this.buildMotionSparseResidualPayload(previousFrame, currentFrame, dstX, dstY, srcX, srcY, width, height, tileStats);
        if (sparseResidual != null) {
            BlockInterTileCandidate sparseCandidate = new BlockInterTileCandidate(
                    new AfmaBlockInterPayloadHelper.TileOperation(AfmaBlockInterPayloadHelper.TileMode.COPY_SPARSE,
                            dx, dy, sparseResidual.channels(), sparseResidual.changedPixelCount(),
                            sparseResidual.layoutPayload(), sparseResidual.residualPayload(), sparseResidual.toMetadata(null)),
                    this.estimateBlockInterTileBytes(AfmaBlockInterPayloadHelper.TileMode.COPY_SPARSE,
                            sparseResidual.layoutPayload().length, sparseResidual.residualPayload().length),
                    sparseResidual.complexityScore()
            );
            bestCandidate = this.selectBetterBlockInterCandidate(bestCandidate, sparseCandidate);
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
            return Integer.compare(first.candidate().operation().mode().ordinal(), second.candidate().operation().mode().ordinal());
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
        return candidate.operation().mode().ordinal() < currentBest.operation().mode().ordinal();
    }

    protected boolean isOptimalBlockInterTileCandidate(@NotNull BlockInterTileCandidate candidate) {
        return candidate.operation().mode() == AfmaBlockInterPayloadHelper.TileMode.COPY;
    }

    protected boolean canPotentialBlockInterModeBeat(@NotNull BlockInterTileCandidate currentBestCandidate,
                                                     @NotNull AfmaBlockInterPayloadHelper.TileMode candidateMode,
                                                     long optimisticEstimatedBytes) {
        if (optimisticEstimatedBytes != currentBestCandidate.estimatedBytes()) {
            return optimisticEstimatedBytes < currentBestCandidate.estimatedBytes();
        }
        return candidateMode.ordinal() < currentBestCandidate.operation().mode().ordinal();
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
        return this.buildSparseResidualPayload(width, height, changedIndices, predictedColors, changedColors, changedPixelCount, tileStats.includeAlpha());
    }

    @NotNull
    protected RawTileData buildRawTileBytes(@NotNull AfmaPixelFrame currentFrame, int dstX, int dstY, int width, int height) {
        int frameWidth = currentFrame.getWidth();
        int[] currentPixels = currentFrame.getPixelsUnsafe();
        boolean includeAlpha = false;
        for (int localY = 0; localY < height && !includeAlpha; localY++) {
            int rowOffset = ((dstY + localY) * frameWidth) + dstX;
            for (int localX = 0; localX < width; localX++) {
                if (((currentPixels[rowOffset + localX] >>> 24) & 0xFF) != 0xFF) {
                    includeAlpha = true;
                    break;
                }
            }
        }

        int channels = AfmaResidualPayloadHelper.channelCount(includeAlpha);
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
        return new RawTileData(payloadBytes, channels);
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
        long frameArea = (long) frameWidth * frameHeight;
        if (frameArea <= 0L || patchArea >= frameArea) {
            return false;
        }

        long fullArchiveBytes = fullCandidate.estimatedArchiveBytes(payloadPathsByFingerprint);
        long candidateArchiveBytes = candidate.estimatedArchiveBytes(payloadPathsByFingerprint);
        long byteSavings = fullArchiveBytes - candidateArchiveBytes;
        if (byteSavings <= 0L) {
            return false;
        }
        if (patchArea <= 0L) {
            return true;
        }

        long requiredSavings = this.computeRequiredComplexCandidateSavings(
                fullArchiveBytes,
                options.getMinComplexCandidateSavingsBytes(),
                options.getMinComplexCandidateSavingsRatio()
        );
        double areaRatio = (double) patchArea / (double) frameArea;
        if (areaRatio > maxAreaRatioWithoutStrongSavings) {
            requiredSavings = Math.max(requiredSavings, this.computeRequiredComplexCandidateSavings(
                    fullArchiveBytes,
                    options.getMinStrongComplexCandidateSavingsBytes(),
                    options.getMinStrongComplexCandidateSavingsRatio()
            ));
        }
        return byteSavings >= requiredSavings;
    }

    protected boolean shouldKeepResidualCandidate(@NotNull PlannedCandidate candidate, @NotNull PlannedCandidate fullCandidate,
                                                  long patchArea, int frameWidth, int frameHeight, double maxAreaRatioWithoutStrongSavings,
                                                  @NotNull AfmaEncodeOptions options, @NotNull Map<String, String> payloadPathsByFingerprint) {
        long frameArea = (long) frameWidth * frameHeight;
        if (frameArea <= 0L || patchArea <= 0L) {
            return false;
        }

        long boundedPatchArea = Math.min(patchArea, frameArea);
        long fullArchiveBytes = fullCandidate.estimatedArchiveBytes(payloadPathsByFingerprint);
        long candidateArchiveBytes = candidate.estimatedArchiveBytes(payloadPathsByFingerprint);
        long byteSavings = fullArchiveBytes - candidateArchiveBytes;
        if (byteSavings <= 0L) {
            return false;
        }

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
        return byteSavings >= requiredSavings;
    }

    protected boolean shouldKeepSparseCandidate(@NotNull PlannedCandidate candidate, @NotNull PlannedCandidate fullCandidate,
                                                long patchArea, int frameWidth, int frameHeight, double maxAreaRatioWithoutStrongSavings,
                                                @NotNull AfmaEncodeOptions options, @NotNull Map<String, String> payloadPathsByFingerprint) {
        long frameArea = (long) frameWidth * frameHeight;
        if (frameArea <= 0L || patchArea <= 0L) {
            return false;
        }

        long boundedPatchArea = Math.min(patchArea, frameArea);
        long fullArchiveBytes = fullCandidate.estimatedArchiveBytes(payloadPathsByFingerprint);
        long candidateArchiveBytes = candidate.estimatedArchiveBytes(payloadPathsByFingerprint);
        long byteSavings = fullArchiveBytes - candidateArchiveBytes;
        if (byteSavings <= 0L) {
            return false;
        }

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
        return byteSavings >= requiredSavings;
    }

    protected boolean shouldKeepComplexCandidate(@NotNull PlannedCandidate candidate, @NotNull PlannedCandidate fullCandidate,
                                                 long patchArea, int frameWidth, int frameHeight, double maxAreaRatioWithoutStrongSavings,
                                                 @NotNull AfmaEncodeOptions options, @NotNull ArchivePlanningState archiveState,
                                                 boolean introSequence) {
        long frameArea = (long) frameWidth * frameHeight;
        if (frameArea <= 0L || patchArea >= frameArea) {
            return false;
        }

        long fullArchiveBytes = archiveState.appendCandidate(fullCandidate, introSequence).marginalArchiveBytes();
        long candidateArchiveBytes = archiveState.appendCandidate(candidate, introSequence).marginalArchiveBytes();
        long byteSavings = fullArchiveBytes - candidateArchiveBytes;
        if (byteSavings <= 0L) {
            return false;
        }
        if (patchArea <= 0L) {
            return true;
        }

        long requiredSavings = this.computeRequiredComplexCandidateSavings(
                fullArchiveBytes,
                options.getMinComplexCandidateSavingsBytes(),
                options.getMinComplexCandidateSavingsRatio()
        );
        double areaRatio = (double) patchArea / (double) frameArea;
        if (areaRatio > maxAreaRatioWithoutStrongSavings) {
            requiredSavings = Math.max(requiredSavings, this.computeRequiredComplexCandidateSavings(
                    fullArchiveBytes,
                    options.getMinStrongComplexCandidateSavingsBytes(),
                    options.getMinStrongComplexCandidateSavingsRatio()
            ));
        }
        return byteSavings >= requiredSavings;
    }

    protected boolean shouldKeepResidualCandidate(@NotNull PlannedCandidate candidate, @NotNull PlannedCandidate fullCandidate,
                                                  long patchArea, int frameWidth, int frameHeight, double maxAreaRatioWithoutStrongSavings,
                                                  @NotNull AfmaEncodeOptions options, @NotNull ArchivePlanningState archiveState,
                                                  boolean introSequence) {
        long frameArea = (long) frameWidth * frameHeight;
        if (frameArea <= 0L || patchArea <= 0L) {
            return false;
        }

        long boundedPatchArea = Math.min(patchArea, frameArea);
        long fullArchiveBytes = archiveState.appendCandidate(fullCandidate, introSequence).marginalArchiveBytes();
        long candidateArchiveBytes = archiveState.appendCandidate(candidate, introSequence).marginalArchiveBytes();
        long byteSavings = fullArchiveBytes - candidateArchiveBytes;
        if (byteSavings <= 0L) {
            return false;
        }

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
        return byteSavings >= requiredSavings;
    }

    protected boolean shouldKeepSparseCandidate(@NotNull PlannedCandidate candidate, @NotNull PlannedCandidate fullCandidate,
                                                long patchArea, int frameWidth, int frameHeight, double maxAreaRatioWithoutStrongSavings,
                                                @NotNull AfmaEncodeOptions options, @NotNull ArchivePlanningState archiveState,
                                                boolean introSequence) {
        long frameArea = (long) frameWidth * frameHeight;
        if (frameArea <= 0L || patchArea <= 0L) {
            return false;
        }

        long boundedPatchArea = Math.min(patchArea, frameArea);
        long fullArchiveBytes = archiveState.appendCandidate(fullCandidate, introSequence).marginalArchiveBytes();
        long candidateArchiveBytes = archiveState.appendCandidate(candidate, introSequence).marginalArchiveBytes();
        long byteSavings = fullArchiveBytes - candidateArchiveBytes;
        if (byteSavings <= 0L) {
            return false;
        }

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
        return byteSavings >= requiredSavings;
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

    protected static long estimateChunkCompressionDelta(@NotNull byte[] previousTail, @NotNull byte[] payloadBytes) {
        if (payloadBytes.length == 0) {
            return 0L;
        }
        if (previousTail.length == 0) {
            return AfmaPayloadMetricsHelper.estimateArchiveBytes(payloadBytes);
        }

        byte[] combinedBytes = new byte[previousTail.length + payloadBytes.length];
        System.arraycopy(previousTail, 0, combinedBytes, 0, previousTail.length);
        System.arraycopy(payloadBytes, 0, combinedBytes, previousTail.length, payloadBytes.length);
        long combinedEstimate = AfmaPayloadMetricsHelper.estimateArchiveBytes(combinedBytes);
        long tailEstimate = AfmaPayloadMetricsHelper.estimateArchiveBytes(previousTail);
        return Math.max(0L, combinedEstimate - tailEstimate);
    }

    @NotNull
    protected static byte[] appendDeflateTail(@NotNull byte[] currentTail, @NotNull byte[] payloadBytes) {
        if (payloadBytes.length == 0) {
            return currentTail;
        }
        int resultLength = Math.min(PLANNER_DEFLATE_TAIL_BYTES, currentTail.length + payloadBytes.length);
        byte[] resultTail = new byte[resultLength];
        int payloadBytesInTail = Math.min(payloadBytes.length, resultLength);
        System.arraycopy(payloadBytes, payloadBytes.length - payloadBytesInTail, resultTail, resultLength - payloadBytesInTail, payloadBytesInTail);
        int carriedPrefixBytes = resultLength - payloadBytesInTail;
        if (carriedPrefixBytes > 0) {
            System.arraycopy(currentTail, currentTail.length - carriedPrefixBytes, resultTail, 0, carriedPrefixBytes);
        }
        return resultTail;
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

    protected static void checkCancelled(@Nullable BooleanSupplier cancellationRequested) {
        if ((cancellationRequested != null) && cancellationRequested.getAsBoolean()) {
            throw new CancellationException("AFMA encode planning was cancelled");
        }
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
        protected final byte[] primaryPayload;
        @Nullable
        protected final PayloadKind primaryPayloadKind;
        protected final boolean primaryPayloadReusedFromSource;
        protected final long estimatedPrimaryArchiveBytes;
        @Nullable
        protected final String patchPayloadPath;
        @Nullable
        protected final byte[] patchPayload;
        @Nullable
        protected final PayloadKind patchPayloadKind;
        protected final boolean patchPayloadReusedFromSource;
        protected final long estimatedPatchArchiveBytes;
        @NotNull
        protected final ReferenceBase referenceBase;
        @Nullable
        protected final AfmaRect referencePatchBounds;
        @Nullable
        protected final int[] referencePatchPixels;
        @NotNull
        protected final DecodeCost decodeCost;
        protected final int complexityScore;

        protected PlannedCandidate(@NotNull AfmaFrameDescriptor descriptor,
                                   @Nullable String primaryPayloadPath, @Nullable byte[] primaryPayload, @Nullable PayloadKind primaryPayloadKind, boolean primaryPayloadReusedFromSource,
                                   @Nullable String patchPayloadPath, @Nullable byte[] patchPayload, @Nullable PayloadKind patchPayloadKind, boolean patchPayloadReusedFromSource,
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
            if (this.primaryPayload != null) total += this.primaryPayload.length;
            if (this.patchPayload != null) total += this.patchPayload.length;
            return total;
        }

        @NotNull
        public PlannedCandidate withPayloads(@Nullable byte[] primaryPayload, @Nullable byte[] patchPayload) {
            return new PlannedCandidate(
                    this.descriptor,
                    this.primaryPayloadPath,
                    primaryPayload,
                    this.primaryPayloadKind,
                    this.primaryPayloadReusedFromSource && primaryPayload == this.primaryPayload,
                    this.patchPayloadPath,
                    patchPayload,
                    this.patchPayloadKind,
                    this.patchPayloadReusedFromSource && patchPayload == this.patchPayload,
                    this.referenceBase,
                    this.referencePatchBounds,
                    this.referencePatchPixels,
                    this.decodeCost,
                    this.complexityScore
            );
        }

        @NotNull
        public AfmaPixelFrame materializeReferenceFrame(@NotNull AfmaPixelFrame sourceFrame, @NotNull AfmaPixelFrame workingFrame) {
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
            if ((this.referencePatchBounds != null) || (this.referencePatchPixels != null)) {
                return false;
            }
            if (this.referenceBase == ReferenceBase.SOURCE_FRAME) {
                return true;
            }
            return sourceFrame == workingFrame;
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

        protected long estimatedPayloadBytes(@Nullable String path, @Nullable byte[] payload, @NotNull Map<String, String> payloadPathsByFingerprint) {
            if ((path == null) || (payload == null)) {
                return 0L;
            }
            String fingerprint = AfmaPayloadMetricsHelper.fingerprintPayload(payload);
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

        public void writePayloads(@NotNull Map<String, byte[]> payloads) {
            if ((this.primaryPayloadPath != null) && (this.primaryPayload != null)) {
                payloads.put(this.primaryPayloadPath, this.primaryPayload);
            }
            if ((this.patchPayloadPath != null) && (this.patchPayload != null)) {
                payloads.put(this.patchPayloadPath, this.patchPayload);
            }
        }

        @NotNull
        public PlannedCandidate internPayloads(@NotNull Map<String, byte[]> payloads, @NotNull Map<String, String> payloadPathsByFingerprint) {
            PlannedCandidate candidate = this.internPrimaryPayload(payloadPathsByFingerprint);
            candidate = candidate.internPatchPayload(payloadPathsByFingerprint);
            candidate.writePayloads(payloads);
            return candidate;
        }

        @NotNull
        protected PlannedCandidate internPrimaryPayload(@NotNull Map<String, String> payloadPathsByFingerprint) {
            if ((this.primaryPayloadPath == null) || (this.primaryPayload == null)) {
                return this;
            }

            String fingerprint = AfmaPayloadMetricsHelper.fingerprintPayload(this.primaryPayload);
            String existingPath = payloadPathsByFingerprint.get(fingerprint);
            if ((existingPath != null) && !existingPath.equals(this.primaryPayloadPath)) {
                return new PlannedCandidate(
                        this.descriptor.withPrimaryPath(existingPath),
                        existingPath,
                        null,
                        this.primaryPayloadKind,
                        false,
                        this.patchPayloadPath,
                        this.patchPayload,
                        this.patchPayloadKind,
                        this.patchPayloadReusedFromSource,
                        this.referenceBase,
                        this.referencePatchBounds,
                        this.referencePatchPixels,
                        this.decodeCost,
                        this.complexityScore
                );
            }

            payloadPathsByFingerprint.put(fingerprint, this.primaryPayloadPath);
            return this;
        }

        @NotNull
        protected PlannedCandidate internPatchPayload(@NotNull Map<String, String> payloadPathsByFingerprint) {
            if ((this.patchPayloadPath == null) || (this.patchPayload == null)) {
                return this;
            }

            String fingerprint = AfmaPayloadMetricsHelper.fingerprintPayload(this.patchPayload);
            String existingPath = payloadPathsByFingerprint.get(fingerprint);
            if ((existingPath != null) && !existingPath.equals(this.patchPayloadPath)) {
                return new PlannedCandidate(
                        this.descriptor.withPatchPath(existingPath),
                        this.primaryPayloadPath,
                        this.primaryPayload,
                        this.primaryPayloadKind,
                        this.primaryPayloadReusedFromSource,
                        existingPath,
                        null,
                        this.patchPayloadKind,
                        false,
                        this.referenceBase,
                        this.referencePatchBounds,
                        this.referencePatchPixels,
                        this.decodeCost,
                        this.complexityScore
                );
            }

            payloadPathsByFingerprint.put(fingerprint, this.patchPayloadPath);
            return this;
        }

        protected static long estimatePayloadArchiveBytes(@Nullable byte[] payload, @Nullable PayloadKind payloadKind) {
            return AfmaPayloadMetricsHelper.estimateArchiveBytes(payload);
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
        protected final LinkedHashMap<String, byte[]> payloadsByPath;
        @NotNull
        protected final Map<String, String> payloadPathsByFingerprint;
        @NotNull
        protected final AfmaChunkedPayloadHelper.ArchivePackingHints packingHints;
        protected final int nextSyntheticPayloadId;
        protected final int nextKeyframeRegionId;
        protected final int currentIntroKeyframeRegionId;
        protected final int currentMainKeyframeRegionId;
        protected final long estimatedArchiveBytes;
        protected final long scoredArchiveBytes;

        protected ArchivePlanningState(@NotNull LinkedHashMap<String, byte[]> payloadsByPath,
                                       @NotNull Map<String, String> payloadPathsByFingerprint,
                                       @NotNull AfmaChunkedPayloadHelper.ArchivePackingHints packingHints,
                                       int nextSyntheticPayloadId, int nextKeyframeRegionId,
                                       int currentIntroKeyframeRegionId, int currentMainKeyframeRegionId,
                                       long estimatedArchiveBytes, long scoredArchiveBytes) {
            this.payloadsByPath = payloadsByPath;
            this.payloadPathsByFingerprint = payloadPathsByFingerprint;
            this.packingHints = packingHints;
            this.nextSyntheticPayloadId = nextSyntheticPayloadId;
            this.nextKeyframeRegionId = nextKeyframeRegionId;
            this.currentIntroKeyframeRegionId = currentIntroKeyframeRegionId;
            this.currentMainKeyframeRegionId = currentMainKeyframeRegionId;
            this.estimatedArchiveBytes = estimatedArchiveBytes;
            this.scoredArchiveBytes = scoredArchiveBytes;
        }

        @NotNull
        public static ArchivePlanningState empty() {
            return new ArchivePlanningState(
                    new LinkedHashMap<>(),
                    Map.of(),
                    AfmaChunkedPayloadHelper.ArchivePackingHints.empty(),
                    0,
                    0,
                    -1,
                    -1,
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
        protected ArchivePayloadAppendResult appendPayload(@Nullable byte[] payloadBytes) {
            if ((payloadBytes == null) || (payloadBytes.length == 0)) {
                return new ArchivePayloadAppendResult(this, null);
            }

            String fingerprint = AfmaPayloadMetricsHelper.fingerprintPayload(payloadBytes);
            String existingPath = this.payloadPathsByFingerprint.get(fingerprint);
            if (existingPath != null) {
                return new ArchivePayloadAppendResult(this, existingPath);
            }

            LinkedHashMap<String, byte[]> nextPayloadsByPath = new LinkedHashMap<>(this.payloadsByPath);
            LinkedHashMap<String, String> nextPayloadPathsByFingerprint = new LinkedHashMap<>(this.payloadPathsByFingerprint);
            String syntheticPath = AfmaChunkedPayloadHelper.syntheticPayloadPath(this.nextSyntheticPayloadId);
            nextPayloadsByPath.put(syntheticPath, payloadBytes);
            nextPayloadPathsByFingerprint.put(fingerprint, syntheticPath);
            return new ArchivePayloadAppendResult(
                    this.recomputeState(
                            nextPayloadsByPath,
                            nextPayloadPathsByFingerprint,
                            this.packingHints,
                            this.nextSyntheticPayloadId + 1,
                            this.nextKeyframeRegionId,
                            this.currentIntroKeyframeRegionId,
                            this.currentMainKeyframeRegionId
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

            AfmaChunkedPayloadHelper.ArchivePackingHints nextPackingHints = this.packingHints.append(
                    new AfmaChunkedPayloadHelper.PayloadAccessFrame(
                            payloadPaths,
                            currentRegionId,
                            introSequence ? AfmaChunkedPayloadHelper.SEQUENCE_KIND_INTRO : AfmaChunkedPayloadHelper.SEQUENCE_KIND_MAIN
                    )
            );
            return this.recomputeState(
                    this.payloadsByPath,
                    this.payloadPathsByFingerprint,
                    nextPackingHints,
                    this.nextSyntheticPayloadId,
                    nextRegionId,
                    currentIntroRegionId,
                    currentMainRegionId
            );
        }

        @NotNull
        protected ArchivePlanningState recomputeState(@NotNull LinkedHashMap<String, byte[]> payloadsByPath,
                                                      @NotNull Map<String, String> payloadPathsByFingerprint,
                                                      @NotNull AfmaChunkedPayloadHelper.ArchivePackingHints packingHints,
                                                      int nextSyntheticPayloadId, int nextKeyframeRegionId,
                                                      int currentIntroKeyframeRegionId, int currentMainKeyframeRegionId) {
            AfmaChunkedPayloadHelper.PackedPayloadArchive simulatedArchive = AfmaChunkedPayloadHelper.simulateArchiveLayout(payloadsByPath, packingHints);
            return new ArchivePlanningState(
                    new LinkedHashMap<>(payloadsByPath),
                    Collections.unmodifiableMap(new LinkedHashMap<>(payloadPathsByFingerprint)),
                    packingHints,
                    nextSyntheticPayloadId,
                    nextKeyframeRegionId,
                    currentIntroKeyframeRegionId,
                    currentMainKeyframeRegionId,
                    simulatedArchive.packingMetrics().predictedArchiveBytes(),
                    simulatedArchive.packingMetrics().scoredArchiveBytes()
            );
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

    protected record MotionTileStats(int changedPixelCount, boolean includeAlpha) {
    }

    protected record RawTileData(@NotNull byte[] payloadBytes, int channels) {
    }

    protected record BlockInterTileCandidate(@NotNull AfmaBlockInterPayloadHelper.TileOperation operation, long estimatedBytes,
                                             int complexityScore) {
    }

    protected record MotionSearchSeed(@NotNull AfmaRectCopyDetector.MotionVector motionVector,
                                      @NotNull BlockInterTileCandidate candidate) {
    }

    @FunctionalInterface
    public interface ProgressListener {
        void update(@NotNull String detail, double progress);
    }

}
