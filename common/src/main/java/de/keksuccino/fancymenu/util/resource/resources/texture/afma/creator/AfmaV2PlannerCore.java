package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaAlphaResidualMode;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaBinIntraPayloadHelper;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaBlockInter;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaBlockInterPayloadHelper;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaChunkedPayloadHelper;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaContainerV2;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaCopyRect;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameDescriptor;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameIndex;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameOperationType;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaMetadata;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaMultiCopy;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaPatchRegion;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaPayloadArchiveLayout;
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

import java.io.ByteArrayOutputStream;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

final class AfmaV2PlannerCore {

    protected static final int MIN_SPARSE_DELTA_CHANGED_PIXELS = 1;
    protected static final double MAX_SPARSE_DELTA_CHANGED_DENSITY = 0.75D;
    protected static final int BLOCK_INTER_TILE_SIZE = 16;
    protected static final int MIN_PARALLEL_BLOCK_INTER_TILES = 24;
    protected static final int MAX_BLOCK_INTER_MOTION_VECTORS = 5;
    protected static final long BLOCK_INTER_MIN_REGION_AREA = (long) BLOCK_INTER_TILE_SIZE * BLOCK_INTER_TILE_SIZE * 3L;
    protected static final double BLOCK_INTER_REQUIRED_SAVINGS_RATIO = 0.96D;
    protected static final long FAMILY_SWITCH_MARGIN_BYTES = 48L;
    protected static final double FAMILY_SWITCH_MARGIN_RATIO = 0.05D;

    @NotNull
    protected final AfmaFrameNormalizer frameNormalizer;
    @NotNull
    protected final ThreadLocal<ResidualPlannerWorkspace> residualPlannerWorkspace = ThreadLocal.withInitial(ResidualPlannerWorkspace::new);

    AfmaV2PlannerCore(@NotNull AfmaFrameNormalizer frameNormalizer) {
        this.frameNormalizer = Objects.requireNonNull(frameNormalizer);
    }

    @NotNull
    public AfmaEncodePlan plan(@NotNull AfmaSourceSequence mainSequence, @Nullable AfmaSourceSequence introSequence,
                               @NotNull AfmaEncodeOptions options,
                               @Nullable BooleanSupplier cancellationRequested,
                               @Nullable AfmaEncodePlanner.ProgressListener progressListener) throws IOException {
        Objects.requireNonNull(mainSequence);
        Objects.requireNonNull(options);

        AfmaSourceSequence intro = (introSequence != null) ? introSequence : AfmaSourceSequence.empty();
        options.validateForCounts(mainSequence.size(), intro.size());

        ExecutorService executor = this.createExecutor();
        AfmaFastPixelBufferPool pixelBufferPool = new AfmaFastPixelBufferPool(Math.max(4, Runtime.getRuntime().availableProcessors()));
        try {
            checkCancelled(cancellationRequested);
            AfmaSourceSequence dimensionSource = !mainSequence.isEmpty() ? mainSequence : intro;
            LoadedDimensionFrame loadedDimension = this.loadDimensionFrame(dimensionSource, pixelBufferPool, cancellationRequested, progressListener);
            Dimension dimension = loadedDimension.dimension();
            AfmaPixelFrame preloadedMainFrame = (dimensionSource == mainSequence) ? loadedDimension.frame() : null;
            AfmaPixelFrame preloadedIntroFrame = (dimensionSource == intro) ? loadedDimension.frame() : null;
            int totalFrameCount = Math.max(1, mainSequence.size() + intro.size());
            PayloadInterner payloadInterner = new PayloadInterner();
            AfmaRectCopyDetector copyDetector = new AfmaRectCopyDetector(options.getMaxCopySearchDistance(), options.getMaxCandidateAxisOffsets());
            try {
                PlannedSequence plannedIntro = this.planSequence(
                        intro,
                        true,
                        List.of(),
                        dimension,
                        options,
                        copyDetector,
                        payloadInterner,
                        pixelBufferPool,
                        executor,
                        cancellationRequested,
                        progressListener,
                        0,
                        totalFrameCount,
                        preloadedIntroFrame
                );
                preloadedIntroFrame = null;
                PlannedSequence plannedMain = this.planSequence(
                        mainSequence,
                        false,
                        plannedIntro.frames(),
                        dimension,
                        options,
                        copyDetector,
                        payloadInterner,
                        pixelBufferPool,
                        executor,
                        cancellationRequested,
                        progressListener,
                        intro.size(),
                        totalFrameCount,
                        preloadedMainFrame
                );
                preloadedMainFrame = null;

                long mainFrameTime = plannedMain.defaultDelayMs();
                long introFrameTime = plannedIntro.defaultDelayMs();
                if (plannedMain.frames().isEmpty() && !plannedIntro.frames().isEmpty()) {
                    mainFrameTime = introFrameTime;
                } else if (plannedIntro.frames().isEmpty()) {
                    introFrameTime = mainFrameTime;
                }

                AfmaMetadata metadata = AfmaMetadata.create(
                        dimension.width(),
                        dimension.height(),
                        options.getLoopCount(),
                        mainFrameTime,
                        introFrameTime,
                        plannedMain.customFrameTimes(),
                        plannedIntro.customFrameTimes(),
                        options.isAdaptiveKeyframePlacementEnabled() ? options.getAdaptiveMaxKeyframeInterval() : options.getKeyframeInterval(),
                        options.isRectCopyEnabled(),
                        options.isDuplicateFrameElision()
                );
                return new AfmaEncodePlan(
                        metadata,
                        new AfmaFrameIndex(plannedMain.frames(), plannedIntro.frames()),
                        payloadInterner.payloads()
                );
            } finally {
                CloseableUtils.closeQuietly(preloadedIntroFrame);
                CloseableUtils.closeQuietly(preloadedMainFrame);
            }
        } finally {
            this.residualPlannerWorkspace.remove();
            pixelBufferPool.clear();
            if (executor != null) {
                executor.shutdownNow();
            }
        }
    }

    @Nullable
    protected ExecutorService createExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        if (processors <= 1) {
            return null;
        }
        int threads = Math.max(1, processors - 1);
        AtomicInteger counter = new AtomicInteger(0);
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "FancyMenu-AfmaV2Planner-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newFixedThreadPool(threads, threadFactory);
    }

    @NotNull
    protected PlannedSequence planSequence(@NotNull AfmaSourceSequence sequence, boolean introSequence,
                                           @NotNull List<AfmaFrameDescriptor> companionSequenceFrames,
                                           @NotNull Dimension dimension,
                                           @NotNull AfmaEncodeOptions options, @NotNull AfmaRectCopyDetector copyDetector,
                                           @NotNull PayloadInterner payloadInterner, @NotNull AfmaFastPixelBufferPool pixelBufferPool,
                                           @Nullable ExecutorService executor,
                                           @Nullable BooleanSupplier cancellationRequested,
                                           @Nullable AfmaEncodePlanner.ProgressListener progressListener,
                                           int startOffset, int totalFrameCount,
                                           @Nullable AfmaPixelFrame firstFrameOverride) throws IOException {
        List<PlannedTimedFrame> plannedFrames = new ArrayList<>();
        List<AfmaFrameDescriptor> plannedDescriptors = new ArrayList<>();
        if (sequence.isEmpty()) {
            return this.buildPlannedSequence(plannedFrames, this.resolveSequenceDefaultDelay(options, introSequence));
        }

        AsyncFrameLoader frameLoader = new AsyncFrameLoader(sequence, dimension, firstFrameOverride, pixelBufferPool, executor, cancellationRequested);
        AfmaPixelFrame previousEncodedFrame = null;
        QualityBudgetState qualityBudgetState = QualityBudgetState.lossless();
        int framesSinceKeyframe = 0;
        try {
            for (int frameIndex = 0; frameIndex < sequence.size(); frameIndex++) {
                checkCancelled(cancellationRequested);
                AfmaPixelFrame sourceFrame = frameLoader.takeFrame(frameIndex);
                AfmaPixelFrame workingFrame = sourceFrame;
                AfmaPixelFrame nextPreviousEncodedFrame = previousEncodedFrame;
                boolean keepSourceFrame = false;
                boolean keepWorkingFrame = false;
                try {
                    long frameDelayMs = this.resolveSourceFrameDelay(options, introSequence, frameIndex);
                    reportPlanningFrameProgress(
                            progressListener,
                            "Planning",
                            introSequence,
                            frameIndex + 1,
                            sequence.size(),
                            startOffset + frameIndex + 1D,
                            totalFrameCount
                    );

                    boolean bootstrapFrame = plannedFrames.isEmpty();
                    if (!bootstrapFrame && options.isNearLosslessEnabled()) {
                        workingFrame = this.applyNearLosslessTemporalMerge(Objects.requireNonNull(previousEncodedFrame), sourceFrame, options.getNearLosslessMaxChannelDelta());
                    }

                    FrameDecision frameDecision = this.planFrame(
                            previousEncodedFrame,
                            sourceFrame,
                            workingFrame,
                            introSequence,
                            plannedDescriptors,
                            companionSequenceFrames,
                            frameIndex,
                            framesSinceKeyframe,
                            qualityBudgetState,
                            options,
                            copyDetector,
                            payloadInterner,
                            executor,
                            cancellationRequested
                    );
                    nextPreviousEncodedFrame = frameDecision.outputFrame();
                    keepSourceFrame = nextPreviousEncodedFrame == sourceFrame;
                    keepWorkingFrame = nextPreviousEncodedFrame == workingFrame;

                    qualityBudgetState = qualityBudgetState.advance(frameDecision.candidate());
                    if ((frameDecision.candidate().kind() == CandidateKind.SAME) && options.isDuplicateFrameElision()) {
                        this.extendPlannedFrameDelay(plannedFrames, frameDelayMs);
                    } else {
                        AfmaFrameDescriptor finalizedDescriptor = payloadInterner.intern(frameDecision.candidate());
                        plannedFrames.add(new PlannedTimedFrame(finalizedDescriptor, frameDelayMs));
                        plannedDescriptors.add(finalizedDescriptor);
                        if (finalizedDescriptor.isKeyframe()) {
                            framesSinceKeyframe = 0;
                        } else {
                            framesSinceKeyframe++;
                        }
                    }

                    AfmaPixelFrame oldPreviousEncodedFrame = previousEncodedFrame;
                    previousEncodedFrame = nextPreviousEncodedFrame;
                    if ((oldPreviousEncodedFrame != null) && (oldPreviousEncodedFrame != previousEncodedFrame)) {
                        CloseableUtils.closeQuietly(oldPreviousEncodedFrame);
                    }
                } finally {
                    if (!keepWorkingFrame && (workingFrame != sourceFrame)) {
                        CloseableUtils.closeQuietly(workingFrame);
                    }
                    if (!keepSourceFrame) {
                        CloseableUtils.closeQuietly(sourceFrame);
                    }
                }
            }
        } finally {
            frameLoader.close();
            CloseableUtils.closeQuietly(previousEncodedFrame);
        }

        return this.buildPlannedSequence(plannedFrames, this.resolveSequenceDefaultDelay(options, introSequence));
    }

    @NotNull
    protected FrameDecision planFrame(@Nullable AfmaPixelFrame previousFrame,
                                      @NotNull AfmaPixelFrame sourceFrame,
                                      @NotNull AfmaPixelFrame workingFrame,
                                      boolean introSequence,
                                      @NotNull List<AfmaFrameDescriptor> currentSequenceFrames,
                                      @NotNull List<AfmaFrameDescriptor> companionSequenceFrames,
                                      int frameIndex, int framesSinceKeyframe,
                                      @NotNull QualityBudgetState qualityBudgetState,
                                      @NotNull AfmaEncodeOptions options,
                                      @NotNull AfmaRectCopyDetector copyDetector,
                                      @NotNull PayloadInterner payloadInterner,
                                      @Nullable ExecutorService executor,
                                      @Nullable BooleanSupplier cancellationRequested) throws IOException {
        Objects.requireNonNull(sourceFrame);
        Objects.requireNonNull(workingFrame);
        if (previousFrame == null) {
            FrameCandidate fullCandidate = this.withQualityMetrics(
                    this.createExactFullCandidate(sourceFrame, introSequence, frameIndex, options),
                    sourceFrame
            );
            return new FrameDecision(fullCandidate, fullCandidate.outputFrame());
        }

        checkCancelled(cancellationRequested);
        AfmaPixelFrame candidateFrame = workingFrame;
        AfmaFramePairAnalysis pairAnalysis = new AfmaFramePairAnalysis(previousFrame, candidateFrame);
        AfmaRect deltaBounds = pairAnalysis.differenceBounds();
        if (deltaBounds == null) {
            FrameCandidate sameCandidate = this.withQualityMetrics(new FrameCandidate(
                    CandidateKind.SAME,
                    AfmaFrameDescriptor.same(),
                    null,
                    null,
                    null,
                    null,
                    previousFrame,
                    0,
                    CandidateQualityMetrics.losslessMetrics()
            ), sourceFrame);
            if (this.isCandidateAllowed(sameCandidate, qualityBudgetState, options)) {
                return new FrameDecision(sameCandidate, previousFrame);
            }
            candidateFrame = sourceFrame;
            pairAnalysis = new AfmaFramePairAnalysis(previousFrame, sourceFrame);
            deltaBounds = pairAnalysis.differenceBounds();
            if (deltaBounds == null) {
                FrameCandidate fullCandidate = this.withQualityMetrics(
                        this.createExactFullCandidate(sourceFrame, introSequence, frameIndex, options),
                        sourceFrame
                );
                return new FrameDecision(fullCandidate, fullCandidate.outputFrame());
            }
        }

        boolean allowPerceptual = this.shouldAllowPerceptualContinuation(framesSinceKeyframe, options);

        FrameCandidate bestCandidate = null;
        FrameCandidate deltaCandidate = this.withQualityMetrics(this.createBestDeltaFamilyCandidate(
                previousFrame,
                candidateFrame,
                pairAnalysis,
                introSequence,
                frameIndex,
                deltaBounds,
                options,
                allowPerceptual
        ), sourceFrame);
        bestCandidate = this.pickBetterCandidate(bestCandidate, deltaCandidate, payloadInterner, qualityBudgetState, options, framesSinceKeyframe);

        CopyEvaluation copyEvaluation = options.isRectCopyEnabled()
                ? this.evaluateCopyDetections(previousFrame, candidateFrame, pairAnalysis, copyDetector)
                : CopyEvaluation.EMPTY;
        FrameCandidate copyCandidate = options.isRectCopyEnabled()
                ? this.withQualityMetrics(this.createBestCopyFamilyCandidate(
                previousFrame,
                candidateFrame,
                pairAnalysis,
                introSequence,
                frameIndex,
                options,
                allowPerceptual,
                copyEvaluation
        ), sourceFrame)
                : null;
        bestCandidate = this.pickBetterCandidate(bestCandidate, copyCandidate, payloadInterner, qualityBudgetState, options, framesSinceKeyframe);

        long blockInterReferenceBytes = (bestCandidate != null)
                ? bestCandidate.totalArchiveBytes(payloadInterner)
                : 0L;
        FrameCandidate blockInterCandidate = this.withQualityMetrics(this.createBlockInterFamilyCandidate(
                previousFrame,
                candidateFrame,
                pairAnalysis,
                introSequence,
                frameIndex,
                deltaBounds,
                copyDetector,
                copyEvaluation,
                blockInterReferenceBytes,
                executor,
                cancellationRequested
        ), sourceFrame);
        bestCandidate = this.pickBetterCandidate(bestCandidate, blockInterCandidate, payloadInterner, qualityBudgetState, options, framesSinceKeyframe);

        int hardInterval = options.isAdaptiveKeyframePlacementEnabled()
                ? options.getAdaptiveMaxKeyframeInterval()
                : options.getKeyframeInterval();
        boolean hardKeyframe = (framesSinceKeyframe + 1) >= hardInterval;
        boolean preferredKeyframe = !options.isAdaptiveKeyframePlacementEnabled()
                ? (framesSinceKeyframe + 1) >= options.getKeyframeInterval()
                : (framesSinceKeyframe + 1) >= options.getKeyframeInterval();

        FrameCandidate fullCandidate = this.withQualityMetrics(
                this.createExactFullCandidate(sourceFrame, introSequence, frameIndex, options),
                sourceFrame
        );
        Map<FrameCandidate, Long> packedArchiveBytesByCandidate = this.estimatePackedArchiveBytesByCandidate(
                Arrays.asList(deltaCandidate, copyCandidate, blockInterCandidate, fullCandidate),
                payloadInterner,
                introSequence,
                currentSequenceFrames,
                companionSequenceFrames,
                qualityBudgetState,
                options
        );
        deltaCandidate = this.filterWeakCandidateAgainstFull(
                deltaCandidate,
                fullCandidate,
                packedArchiveBytesByCandidate,
                sourceFrame.getWidth(),
                sourceFrame.getHeight(),
                options
        );
        copyCandidate = this.filterWeakCandidateAgainstFull(
                copyCandidate,
                fullCandidate,
                packedArchiveBytesByCandidate,
                sourceFrame.getWidth(),
                sourceFrame.getHeight(),
                options
        );
        blockInterCandidate = this.filterWeakCandidateAgainstFull(
                blockInterCandidate,
                fullCandidate,
                packedArchiveBytesByCandidate,
                sourceFrame.getWidth(),
                sourceFrame.getHeight(),
                options
        );
        bestCandidate = null;
        bestCandidate = this.pickBetterCandidate(bestCandidate, deltaCandidate, payloadInterner, qualityBudgetState, options, framesSinceKeyframe);
        bestCandidate = this.pickBetterCandidate(bestCandidate, copyCandidate, payloadInterner, qualityBudgetState, options, framesSinceKeyframe);
        bestCandidate = this.pickBetterCandidate(bestCandidate, blockInterCandidate, payloadInterner, qualityBudgetState, options, framesSinceKeyframe);

        if (hardKeyframe) {
            FrameCandidate selected = Objects.requireNonNull(fullCandidate, "AFMA v2 hard-keyframe candidate was NULL");
            return this.toFrameDecision(selected, deltaCandidate, copyCandidate, blockInterCandidate, fullCandidate);
        }

        if (bestCandidate == null) {
            FrameCandidate selected = Objects.requireNonNull(fullCandidate, "AFMA v2 fallback full candidate was NULL");
            return this.toFrameDecision(selected, deltaCandidate, copyCandidate, blockInterCandidate, fullCandidate);
        }

        if (fullCandidate != null) {
            if (!options.isAdaptiveKeyframePlacementEnabled() && preferredKeyframe) {
                return this.toFrameDecision(fullCandidate, deltaCandidate, copyCandidate, blockInterCandidate, fullCandidate);
            }
            if (options.isAdaptiveKeyframePlacementEnabled() && preferredKeyframe) {
                long continuationSavings = fullCandidate.totalArchiveBytes(payloadInterner) - bestCandidate.totalArchiveBytes(payloadInterner);
                long requiredSavings = this.resolveAdaptiveContinuationSavings(fullCandidate.totalArchiveBytes(payloadInterner), options);
                if (continuationSavings < requiredSavings) {
                    return this.toFrameDecision(fullCandidate, deltaCandidate, copyCandidate, blockInterCandidate, fullCandidate);
                }
            }
            if (this.pickBetterCandidate(bestCandidate, fullCandidate, payloadInterner, qualityBudgetState, options, framesSinceKeyframe) == fullCandidate) {
                return this.toFrameDecision(fullCandidate, deltaCandidate, copyCandidate, blockInterCandidate, fullCandidate);
            }
        }

        FrameCandidate packedArchiveWinner = this.pickBestPackedArchiveCandidate(
                Arrays.asList(deltaCandidate, copyCandidate, blockInterCandidate, fullCandidate),
                packedArchiveBytesByCandidate,
                payloadInterner,
                qualityBudgetState,
                options,
                framesSinceKeyframe
        );
        if (packedArchiveWinner != null) {
            return this.toFrameDecision(packedArchiveWinner, deltaCandidate, copyCandidate, blockInterCandidate, fullCandidate);
        }

        return this.toFrameDecision(bestCandidate, deltaCandidate, copyCandidate, blockInterCandidate, fullCandidate);
    }

    protected long resolveAdaptiveContinuationSavings(long fullBytes, @NotNull AfmaEncodeOptions options) {
        long ratioSavings = Math.round(Math.max(0D, fullBytes) * Math.max(0D, options.getAdaptiveContinuationMinSavingsRatio()));
        return Math.max(options.getAdaptiveContinuationMinSavingsBytes(), ratioSavings);
    }

    @Nullable
    protected FrameCandidate pickBetterCandidate(@Nullable FrameCandidate first, @Nullable FrameCandidate second,
                                                 @NotNull PayloadInterner payloadInterner,
                                                 @NotNull QualityBudgetState qualityBudgetState,
                                                 @NotNull AfmaEncodeOptions options,
                                                 int framesSinceKeyframe) {
        if (!this.isCandidateAllowed(first, qualityBudgetState, options)) {
            first = null;
        }
        if (!this.isCandidateAllowed(second, qualityBudgetState, options)) {
            second = null;
        }
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }

        double firstScore = this.scoreCandidate(first, payloadInterner, qualityBudgetState, options, framesSinceKeyframe);
        double secondScore = this.scoreCandidate(second, payloadInterner, qualityBudgetState, options, framesSinceKeyframe);
        if (Double.compare(firstScore, secondScore) != 0) {
            return (firstScore < secondScore) ? first : second;
        }
        long firstBytes = first.totalArchiveBytes(payloadInterner);
        long secondBytes = second.totalArchiveBytes(payloadInterner);
        if (firstBytes != secondBytes) {
            return (firstBytes < secondBytes) ? first : second;
        }
        if (first.decodeComplexity() != second.decodeComplexity()) {
            return (first.decodeComplexity() < second.decodeComplexity()) ? first : second;
        }
        return (first.kind().stabilityRank() <= second.kind().stabilityRank()) ? first : second;
    }

    protected boolean isCandidateAllowed(@Nullable FrameCandidate candidate,
                                         @NotNull QualityBudgetState qualityBudgetState,
                                         @NotNull AfmaEncodeOptions options) {
        if (candidate == null) {
            return false;
        }

        QualityBudgetState projectedState = qualityBudgetState.advance(candidate);
        if ((options.getPlannerMaxConsecutiveLossyFrames() > 0)
                && (projectedState.consecutiveLossyFrames() > options.getPlannerMaxConsecutiveLossyFrames())) {
            return false;
        }
        if ((options.getPlannerMaxCumulativeAverageError() > 0D)
                && (projectedState.cumulativeAverageError() > options.getPlannerMaxCumulativeAverageError())) {
            return false;
        }
        if ((options.getPlannerMaxCumulativeVisibleColorDelta() > 0)
                && (projectedState.cumulativeVisibleColorDelta() > options.getPlannerMaxCumulativeVisibleColorDelta())) {
            return false;
        }
        return (options.getPlannerMaxCumulativeAlphaDelta() <= 0)
                || (projectedState.cumulativeAlphaDelta() <= options.getPlannerMaxCumulativeAlphaDelta());
    }

    protected double scoreCandidate(@NotNull FrameCandidate candidate,
                                    @NotNull PayloadInterner payloadInterner,
                                    @NotNull QualityBudgetState qualityBudgetState,
                                    @NotNull AfmaEncodeOptions options,
                                    int framesSinceKeyframe) {
        return this.scoreCandidate(candidate, candidate.totalArchiveBytes(payloadInterner), qualityBudgetState, options, framesSinceKeyframe);
    }

    protected double scoreCandidate(@NotNull FrameCandidate candidate,
                                    long archiveBytes,
                                    @NotNull QualityBudgetState qualityBudgetState,
                                    @NotNull AfmaEncodeOptions options,
                                    int framesSinceKeyframe) {
        QualityBudgetState projectedState = qualityBudgetState.advance(candidate);
        double score = archiveBytes;
        score += (double) candidate.decodeComplexity() * (double) options.getPlannerDecodeCostPenaltyBytes();
        score += (double) candidate.kind().stabilityRank() * (double) options.getPlannerComplexityPenaltyBytes();
        score += projectedState.cumulativeAverageError() * options.getPlannerAverageDriftPenaltyBytes();
        score += (double) projectedState.cumulativeVisibleColorDelta() * (double) options.getPlannerVisibleColorDriftPenaltyBytes();
        score += (double) projectedState.cumulativeAlphaDelta() * (double) options.getPlannerAlphaDriftPenaltyBytes();
        score += (double) projectedState.consecutiveLossyFrames() * (double) options.getPlannerLossyContinuationPenaltyBytes();
        if (!candidate.descriptor().isKeyframe()) {
            score += (double) (framesSinceKeyframe + 1) * (double) options.getPlannerKeyframeDistancePenaltyBytes();
        }
        return score;
    }

    @Nullable
    protected FrameCandidate pickBestPackedArchiveCandidate(@NotNull List<FrameCandidate> candidates,
                                                            @NotNull Map<FrameCandidate, Long> packedArchiveBytesByCandidate,
                                                            @NotNull PayloadInterner payloadInterner,
                                                            @NotNull QualityBudgetState qualityBudgetState,
                                                            @NotNull AfmaEncodeOptions options,
                                                            int framesSinceKeyframe) throws IOException {
        FrameCandidate bestCandidate = null;
        long bestPackedArchiveBytes = Long.MAX_VALUE;
        double bestPackedScore = Double.POSITIVE_INFINITY;
        for (FrameCandidate candidate : candidates) {
            if (!this.isCandidateAllowed(candidate, qualityBudgetState, options)) {
                continue;
            }

            long packedArchiveBytes = packedArchiveBytesByCandidate.getOrDefault(
                    candidate,
                    candidate.totalArchiveBytes(payloadInterner)
            );
            double packedScore = this.scoreCandidate(candidate, packedArchiveBytes, qualityBudgetState, options, framesSinceKeyframe);
            if (bestCandidate == null
                    || (Double.compare(packedScore, bestPackedScore) < 0)
                    || ((Double.compare(packedScore, bestPackedScore) == 0) && (packedArchiveBytes < bestPackedArchiveBytes))
                    || ((Double.compare(packedScore, bestPackedScore) == 0) && (packedArchiveBytes == bestPackedArchiveBytes)
                    && (candidate.decodeComplexity() < bestCandidate.decodeComplexity()))
                    || ((Double.compare(packedScore, bestPackedScore) == 0) && (packedArchiveBytes == bestPackedArchiveBytes)
                    && (candidate.decodeComplexity() == bestCandidate.decodeComplexity())
                    && (candidate.kind().stabilityRank() < bestCandidate.kind().stabilityRank()))) {
                bestCandidate = candidate;
                bestPackedArchiveBytes = packedArchiveBytes;
                bestPackedScore = packedScore;
            }
        }
        return bestCandidate;
    }

    @NotNull
    protected Map<FrameCandidate, Long> estimatePackedArchiveBytesByCandidate(@NotNull List<FrameCandidate> candidates,
                                                                              @NotNull PayloadInterner payloadInterner,
                                                                              boolean introSequence,
                                                                              @NotNull List<AfmaFrameDescriptor> currentSequenceFrames,
                                                                              @NotNull List<AfmaFrameDescriptor> companionSequenceFrames,
                                                                              @NotNull QualityBudgetState qualityBudgetState,
                                                                              @NotNull AfmaEncodeOptions options) throws IOException {
        IdentityHashMap<FrameCandidate, Long> packedArchiveBytesByCandidate = new IdentityHashMap<>();
        for (FrameCandidate candidate : candidates) {
            if ((candidate == null) || !this.isCandidateAllowed(candidate, qualityBudgetState, options)) {
                continue;
            }
            packedArchiveBytesByCandidate.put(
                    candidate,
                    payloadInterner.estimatePackedCandidateArchiveBytes(
                            candidate,
                            introSequence,
                            currentSequenceFrames,
                            companionSequenceFrames,
                            options.getLoopCount()
                    )
            );
        }
        return packedArchiveBytesByCandidate;
    }

    @Nullable
    protected FrameCandidate filterWeakCandidateAgainstFull(@Nullable FrameCandidate candidate,
                                                            @Nullable FrameCandidate fullCandidate,
                                                            @NotNull Map<FrameCandidate, Long> packedArchiveBytesByCandidate,
                                                            int frameWidth, int frameHeight,
                                                            @NotNull AfmaEncodeOptions options) {
        if ((candidate == null) || (fullCandidate == null) || (candidate == fullCandidate)) {
            return candidate;
        }
        if (!this.shouldKeepCandidateAgainstFull(candidate, fullCandidate, packedArchiveBytesByCandidate, frameWidth, frameHeight, options)) {
            return null;
        }
        return candidate;
    }

    protected boolean shouldKeepCandidateAgainstFull(@NotNull FrameCandidate candidate,
                                                     @NotNull FrameCandidate fullCandidate,
                                                     @NotNull Map<FrameCandidate, Long> packedArchiveBytesByCandidate,
                                                     int frameWidth, int frameHeight,
                                                     @NotNull AfmaEncodeOptions options) {
        if ((candidate.kind() == CandidateKind.FULL) || (candidate.kind() == CandidateKind.SAME)) {
            return true;
        }

        Long candidateArchiveBytes = packedArchiveBytesByCandidate.get(candidate);
        Long fullArchiveBytes = packedArchiveBytesByCandidate.get(fullCandidate);
        if ((candidateArchiveBytes == null) || (fullArchiveBytes == null)) {
            return true;
        }

        double maxAreaRatioWithoutStrongSavings = this.resolveMaxAreaRatioWithoutStrongSavings(candidate.kind(), options);
        long patchArea = this.resolveCandidatePatchArea(candidate);
        return switch (candidate.kind()) {
            case DELTA_BIN_INTRA, COPY_BIN_INTRA, MULTI_COPY_BIN_INTRA, BLOCK_INTER ->
                    this.shouldKeepComplexCandidateByArchiveBytes(
                            candidateArchiveBytes,
                            fullArchiveBytes,
                            patchArea,
                            frameWidth,
                            frameHeight,
                            maxAreaRatioWithoutStrongSavings,
                            options
                    );
            case DELTA_RESIDUAL, COPY_RESIDUAL, MULTI_COPY_RESIDUAL ->
                    this.shouldKeepResidualCandidateByArchiveBytes(
                            candidateArchiveBytes,
                            fullArchiveBytes,
                            patchArea,
                            frameWidth,
                            frameHeight,
                            maxAreaRatioWithoutStrongSavings,
                            options
                    );
            case DELTA_SPARSE, COPY_SPARSE, MULTI_COPY_SPARSE ->
                    this.shouldKeepSparseCandidateByArchiveBytes(
                            candidateArchiveBytes,
                            fullArchiveBytes,
                            patchArea,
                            frameWidth,
                            frameHeight,
                            maxAreaRatioWithoutStrongSavings,
                            options
                    );
            default -> true;
        };
    }

    protected double resolveMaxAreaRatioWithoutStrongSavings(@NotNull CandidateKind kind, @NotNull AfmaEncodeOptions options) {
        return switch (kind) {
            case DELTA_BIN_INTRA, DELTA_RESIDUAL, DELTA_SPARSE, BLOCK_INTER -> options.getMaxDeltaAreaRatioWithoutStrongSavings();
            case COPY_BIN_INTRA, COPY_RESIDUAL, COPY_SPARSE,
                 MULTI_COPY_BIN_INTRA, MULTI_COPY_RESIDUAL, MULTI_COPY_SPARSE -> options.getMaxCopyPatchAreaRatioWithoutStrongSavings();
            case SAME, FULL -> 1D;
        };
    }

    protected long resolveCandidatePatchArea(@NotNull FrameCandidate candidate) {
        AfmaFrameDescriptor descriptor = candidate.descriptor();
        AfmaPatchRegion patchRegion = descriptor.getPatch();
        if (patchRegion != null) {
            return (long) patchRegion.getWidth() * (long) patchRegion.getHeight();
        }
        return (long) descriptor.getWidth() * (long) descriptor.getHeight();
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

        return byteSavings >= this.computeRequiredCandidateSavings(
                fullArchiveBytes,
                patchArea,
                frameArea,
                maxAreaRatioWithoutStrongSavings,
                options
        );
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
        return byteSavings >= this.computeRequiredCandidateSavings(
                fullArchiveBytes,
                boundedPatchArea,
                frameArea,
                maxAreaRatioWithoutStrongSavings,
                options
        );
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
        return byteSavings >= this.computeRequiredCandidateSavings(
                fullArchiveBytes,
                boundedPatchArea,
                frameArea,
                maxAreaRatioWithoutStrongSavings,
                options
        );
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
            requiredSavings = Math.max(
                    requiredSavings,
                    this.computeRequiredComplexCandidateSavings(
                            fullArchiveBytes,
                            options.getMinStrongComplexCandidateSavingsBytes(),
                            options.getMinStrongComplexCandidateSavingsRatio()
                    )
            );
        }
        return requiredSavings;
    }

    protected long computeRequiredComplexCandidateSavings(long referenceBytes, long minAbsoluteSavings, double minSavingsRatio) {
        long ratioSavings = (referenceBytes > 0L && minSavingsRatio > 0D)
                ? (long) Math.ceil(referenceBytes * minSavingsRatio)
                : 0L;
        return Math.max(minAbsoluteSavings, ratioSavings);
    }

    @NotNull
    protected FrameDecision toFrameDecision(@NotNull FrameCandidate selectedCandidate, @Nullable FrameCandidate... candidates) {
        this.closeDiscardedCandidatePayloads(selectedCandidate, candidates);
        return new FrameDecision(selectedCandidate, selectedCandidate.outputFrame());
    }

    protected void closeDiscardedCandidatePayloads(@NotNull FrameCandidate selectedCandidate, @Nullable FrameCandidate... candidates) {
        Set<FrameCandidate> seenCandidates = Collections.newSetFromMap(new IdentityHashMap<>());
        seenCandidates.add(selectedCandidate);
        for (FrameCandidate candidate : candidates) {
            if ((candidate == null) || !seenCandidates.add(candidate)) {
                continue;
            }
            CloseableUtils.closeQuietly(candidate.primaryPayload());
            CloseableUtils.closeQuietly(candidate.patchPayload());
        }
    }

    @Nullable
    protected FrameCandidate withQualityMetrics(@Nullable FrameCandidate candidate, @NotNull AfmaPixelFrame sourceFrame) {
        if (candidate == null) {
            return null;
        }
        return candidate.withQualityMetrics(this.measureCandidateQuality(sourceFrame, candidate.outputFrame()));
    }

    @NotNull
    protected CandidateQualityMetrics measureCandidateQuality(@NotNull AfmaPixelFrame sourceFrame, @NotNull AfmaPixelFrame outputFrame) {
        if (sourceFrame == outputFrame) {
            return CandidateQualityMetrics.losslessMetrics();
        }

        AfmaFramePairAnalysis driftAnalysis = new AfmaFramePairAnalysis(sourceFrame, outputFrame);
        if (driftAnalysis.isIdentical()) {
            return CandidateQualityMetrics.losslessMetrics();
        }
        AfmaFramePairAnalysis.PerceptualDriftMetrics driftMetrics = driftAnalysis.perceptualDriftMetrics();
        return new CandidateQualityMetrics(
                false,
                driftMetrics.averageError(),
                driftMetrics.maxVisibleColorDelta(),
                driftMetrics.maxAlphaDelta()
        );
    }

    @NotNull
    protected FrameCandidate createExactFullCandidate(@NotNull AfmaPixelFrame currentFrame,
                                                      boolean introSequence, int frameIndex,
                                                      @NotNull AfmaEncodeOptions options) throws IOException {
        String payloadPath = this.buildPayloadPath(introSequence, frameIndex);
        AfmaBinIntraPayloadHelper.ScoredPayloadResult encodedPayload = this.scoreBinIntraPayload(
                currentFrame,
                options,
                false
        );
        return new FrameCandidate(
                CandidateKind.FULL,
                AfmaFrameDescriptor.full(payloadPath),
                payloadPath,
                this.storePayload(encodedPayload.payloadSummary(), encodedPayload.payloadBytes()),
                null,
                null,
                encodedPayload.lossless()
                        ? currentFrame
                        : new AfmaPixelFrame(currentFrame.getWidth(), currentFrame.getHeight(), encodedPayload.reconstructedPixels()),
                1,
                CandidateQualityMetrics.losslessMetrics()
        );
    }

    @Nullable
    protected FrameCandidate createBestDeltaFamilyCandidate(@NotNull AfmaPixelFrame previousFrame,
                                                            @NotNull AfmaPixelFrame currentFrame,
                                                            @NotNull AfmaFramePairAnalysis pairAnalysis,
                                                            boolean introSequence, int frameIndex,
                                                            @NotNull AfmaRect deltaBounds,
                                                            @NotNull AfmaEncodeOptions options,
                                                            boolean allowPerceptual) throws IOException {
        if (deltaBounds.area() >= ((long) currentFrame.getWidth() * currentFrame.getHeight())) {
            return null;
        }

        AfmaFramePairAnalysis.RegionDiffAnalysis regionAnalysis = pairAnalysis.regionDiffAnalysis(
                deltaBounds.x(),
                deltaBounds.y(),
                deltaBounds.width(),
                deltaBounds.height()
        );
        if (regionAnalysis == null) {
            return null;
        }

        List<StrategyEstimate> rankedStrategies = this.rankDeltaStrategies(regionAnalysis);
        IOException lastException = null;
        for (StrategyEstimate strategy : rankedStrategies) {
            try {
                FrameCandidate candidate = switch (strategy.kind()) {
                    case DELTA_BIN_INTRA -> this.createDeltaBinIntraCandidate(
                            previousFrame,
                            currentFrame,
                            introSequence,
                            frameIndex,
                            deltaBounds,
                            options,
                            allowPerceptual
                    );
                    case DELTA_RESIDUAL -> this.createResidualDeltaCandidate(pairAnalysis, currentFrame, introSequence, frameIndex, deltaBounds);
                    case DELTA_SPARSE -> this.createSparseDeltaCandidate(pairAnalysis, currentFrame, introSequence, frameIndex, deltaBounds);
                    default -> null;
                };
                if (candidate != null) {
                    return candidate;
                }
            } catch (IOException ex) {
                lastException = ex;
            }
        }
        if (lastException != null) {
            throw lastException;
        }
        return null;
    }

    @NotNull
    protected List<StrategyEstimate> rankDeltaStrategies(@NotNull AfmaFramePairAnalysis.RegionDiffAnalysis regionAnalysis) {
        ResidualRegionStats residualStats = this.analyzeResidualStats(
                regionAnalysis.predictedColors(),
                regionAnalysis.currentColors(),
                regionAnalysis.pixelCount(),
                regionAnalysis.includeAlpha()
        );
        long binIntraEstimate = this.estimateBinIntraRegionBytes(
                regionAnalysis.width(),
                regionAnalysis.height(),
                regionAnalysis.pixelCount(),
                regionAnalysis.changedPixelCount(),
                regionAnalysis.includeAlpha()
        );
        ArrayList<StrategyEstimate> estimates = new ArrayList<>(3);
        estimates.add(new StrategyEstimate(CandidateKind.DELTA_BIN_INTRA, binIntraEstimate, CandidateKind.DELTA_BIN_INTRA.stabilityRank()));

        long denseResidualEstimate = this.estimateResidualEncodedBytes(
                regionAnalysis.pixelCount(),
                AfmaResidualPayloadHelper.channelCount(regionAnalysis.includeAlpha()),
                residualStats.averageMagnitude()
        );
        estimates.add(new StrategyEstimate(CandidateKind.DELTA_RESIDUAL, denseResidualEstimate, CandidateKind.DELTA_RESIDUAL.stabilityRank()));

        if (this.canAttemptSparse(regionAnalysis.pixelCount(), regionAnalysis.changedPixelCount())) {
            ChangedRegionData changedRegion = this.copyChangedRegion(regionAnalysis);
            long sparseLayoutEstimate = this.estimateBestSparseLayoutBytes(
                    regionAnalysis.width(),
                    regionAnalysis.height(),
                    changedRegion.changedIndices(),
                    changedRegion.changedCount()
            );
            long sparseResidualEstimate = this.estimateResidualEncodedBytes(
                    changedRegion.changedCount(),
                    AfmaResidualPayloadHelper.channelCount(regionAnalysis.includeAlpha()),
                    residualStats.averageChangedMagnitude()
            );
            estimates.add(new StrategyEstimate(
                    CandidateKind.DELTA_SPARSE,
                    sparseLayoutEstimate + sparseResidualEstimate,
                    CandidateKind.DELTA_SPARSE.stabilityRank()
            ));
        }
        estimates.sort(this::compareStrategyEstimates);
        return estimates;
    }

    @Nullable
    protected FrameCandidate createDeltaBinIntraCandidate(@NotNull AfmaPixelFrame previousFrame,
                                                          @NotNull AfmaPixelFrame currentFrame,
                                                          boolean introSequence, int frameIndex,
                                                          @NotNull AfmaRect deltaBounds,
                                                          @NotNull AfmaEncodeOptions options,
                                                          boolean allowPerceptual) throws IOException {
        String payloadPath = this.buildPayloadPath(introSequence, frameIndex);
        AfmaBinIntraPayloadHelper.ScoredPayloadResult encodedPayload = this.scoreBinIntraPayloadRegion(
                currentFrame,
                deltaBounds.x(),
                deltaBounds.y(),
                deltaBounds.width(),
                deltaBounds.height(),
                options,
                allowPerceptual
        );
        AfmaPixelFrame outputFrame = encodedPayload.lossless()
                ? currentFrame
                : this.applyReconstructedPatch(previousFrame, deltaBounds, encodedPayload.reconstructedPixels());
        return new FrameCandidate(
                CandidateKind.DELTA_BIN_INTRA,
                AfmaFrameDescriptor.deltaRect(
                        payloadPath,
                        deltaBounds.x(),
                        deltaBounds.y(),
                        deltaBounds.width(),
                        deltaBounds.height()
                ),
                payloadPath,
                this.storePayload(encodedPayload.payloadSummary(), encodedPayload.payloadBytes()),
                null,
                null,
                outputFrame,
                2,
                CandidateQualityMetrics.losslessMetrics()
        );
    }

    @Nullable
    protected FrameCandidate createResidualDeltaCandidate(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                                          @NotNull AfmaPixelFrame currentFrame,
                                                          boolean introSequence, int frameIndex,
                                                          @NotNull AfmaRect deltaBounds) throws IOException {
        ResidualPayloadData residualPayload = this.buildResidualPayload(pairAnalysis, deltaBounds);
        if (residualPayload == null) {
            return null;
        }

        String payloadPath = this.buildRawPayloadPath(introSequence, frameIndex, "r");
        return new FrameCandidate(
                CandidateKind.DELTA_RESIDUAL,
                AfmaFrameDescriptor.residualDeltaRect(
                        payloadPath,
                        deltaBounds.x(),
                        deltaBounds.y(),
                        deltaBounds.width(),
                        deltaBounds.height(),
                        residualPayload.metadata()
                ),
                payloadPath,
                this.storePayload(residualPayload.payloadSummary(), residualPayload.payloadWriter()),
                null,
                null,
                currentFrame,
                3 + residualPayload.complexityScore(),
                CandidateQualityMetrics.losslessMetrics()
        );
    }

    @Nullable
    protected FrameCandidate createSparseDeltaCandidate(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                                        @NotNull AfmaPixelFrame currentFrame,
                                                        boolean introSequence, int frameIndex,
                                                        @NotNull AfmaRect deltaBounds) throws IOException {
        SparseResidualPayloadData sparsePayload = this.buildSparseDeltaPayload(pairAnalysis, deltaBounds);
        if (sparsePayload == null) {
            return null;
        }

        String layoutPath = this.buildRawPayloadPath(introSequence, frameIndex, "m");
        String residualPath = this.buildRawPayloadPath(introSequence, frameIndex, "s");
        return new FrameCandidate(
                CandidateKind.DELTA_SPARSE,
                AfmaFrameDescriptor.sparseDeltaRect(
                        layoutPath,
                        deltaBounds.x(),
                        deltaBounds.y(),
                        deltaBounds.width(),
                        deltaBounds.height(),
                        sparsePayload.toMetadata(residualPath)
                ),
                layoutPath,
                this.storePayload(sparsePayload.layoutPayload()),
                residualPath,
                this.storePayload(sparsePayload.residualPayloadSummary(), sparsePayload.residualPayloadWriter()),
                currentFrame,
                4 + sparsePayload.complexityScore(),
                CandidateQualityMetrics.losslessMetrics()
        );
    }

    @NotNull
    protected CopyEvaluation evaluateCopyDetections(@NotNull AfmaPixelFrame previousFrame,
                                                    @NotNull AfmaPixelFrame currentFrame,
                                                    @NotNull AfmaFramePairAnalysis pairAnalysis,
                                                    @NotNull AfmaRectCopyDetector copyDetector) {
        AfmaRectCopyDetector.Detection singleDetection = copyDetector.detect(pairAnalysis);
        AfmaRectCopyDetector.MultiDetection multiDetection = (singleDetection != null)
                ? copyDetector.detectMulti(pairAnalysis, singleDetection)
                : copyDetector.detectMulti(pairAnalysis);
        return new CopyEvaluation(singleDetection, multiDetection);
    }

    @Nullable
    protected FrameCandidate createBestCopyFamilyCandidate(@NotNull AfmaPixelFrame previousFrame,
                                                           @NotNull AfmaPixelFrame currentFrame,
                                                           @NotNull AfmaFramePairAnalysis pairAnalysis,
                                                           boolean introSequence, int frameIndex,
                                                           @NotNull AfmaEncodeOptions options,
                                                           boolean allowPerceptual,
                                                           @NotNull CopyEvaluation copyEvaluation) throws IOException {
        CopyPlan bestPlan = null;
        if (copyEvaluation.singleDetection() != null) {
            bestPlan = this.pickBetterCopyPlan(bestPlan, this.buildSingleCopyPlan(pairAnalysis, copyEvaluation.singleDetection()));
        }
        if (copyEvaluation.multiDetection() != null) {
            bestPlan = this.pickBetterCopyPlan(bestPlan, this.buildMultiCopyPlan(previousFrame, currentFrame, copyEvaluation.multiDetection()));
        }
        if (bestPlan == null) {
            return null;
        }

        IOException lastException = null;
        for (StrategyEstimate strategy : bestPlan.rankedStrategies()) {
            try {
                FrameCandidate candidate;
                if (bestPlan.multiCopy() != null) {
                    candidate = switch (strategy.kind()) {
                        case MULTI_COPY_BIN_INTRA -> this.createMultiCopyBinIntraCandidate(
                                previousFrame,
                                currentFrame,
                                introSequence,
                                frameIndex,
                                Objects.requireNonNull(copyEvaluation.multiDetection()),
                                options,
                                allowPerceptual
                        );
                        case MULTI_COPY_RESIDUAL -> this.createMultiCopyResidualCandidate(
                                currentFrame,
                                introSequence,
                                frameIndex,
                                Objects.requireNonNull(copyEvaluation.multiDetection()),
                                Objects.requireNonNull(bestPlan.referenceFrameAfterCopy())
                        );
                        case MULTI_COPY_SPARSE -> this.createMultiCopySparseCandidate(
                                currentFrame,
                                introSequence,
                                frameIndex,
                                Objects.requireNonNull(copyEvaluation.multiDetection()),
                                Objects.requireNonNull(bestPlan.referenceFrameAfterCopy())
                        );
                        default -> null;
                    };
                } else {
                    candidate = switch (strategy.kind()) {
                        case COPY_BIN_INTRA -> this.createCopyBinIntraCandidate(
                                previousFrame,
                                currentFrame,
                                introSequence,
                                frameIndex,
                                Objects.requireNonNull(copyEvaluation.singleDetection()),
                                options,
                                allowPerceptual
                        );
                        case COPY_RESIDUAL -> this.createCopyResidualCandidate(
                                currentFrame,
                                introSequence,
                                frameIndex,
                                Objects.requireNonNull(copyEvaluation.singleDetection()),
                                pairAnalysis
                        );
                        case COPY_SPARSE -> this.createCopySparseCandidate(
                                currentFrame,
                                introSequence,
                                frameIndex,
                                Objects.requireNonNull(copyEvaluation.singleDetection()),
                                pairAnalysis
                        );
                        default -> null;
                    };
                }
                if (candidate != null) {
                    return candidate;
                }
            } catch (IOException ex) {
                lastException = ex;
            }
        }

        if (lastException != null) {
            throw lastException;
        }
        return null;
    }

    @Nullable
    protected CopyPlan buildSingleCopyPlan(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                           @NotNull AfmaRectCopyDetector.Detection detection) {
        AfmaRect patchBounds = detection.patchBounds();
        if (patchBounds == null) {
            return new CopyPlan(
                    null,
                    detection.copyRect(),
                    null,
                    null,
                    List.of(new StrategyEstimate(CandidateKind.COPY_BIN_INTRA, 0L, CandidateKind.COPY_BIN_INTRA.stabilityRank()))
            );
        }

        AfmaFramePairAnalysis.RegionDiffAnalysis regionAnalysis = pairAnalysis.copyAdjustedRegionDiffAnalysis(
                detection.copyRect(),
                patchBounds.x(),
                patchBounds.y(),
                patchBounds.width(),
                patchBounds.height()
        );
        if (regionAnalysis == null) {
            return null;
        }
        return new CopyPlan(
                null,
                detection.copyRect(),
                null,
                patchBounds,
                this.rankCopyStrategies(regionAnalysis, false)
        );
    }

    @Nullable
    protected CopyPlan buildMultiCopyPlan(@NotNull AfmaPixelFrame previousFrame,
                                          @NotNull AfmaPixelFrame currentFrame,
                                          @NotNull AfmaRectCopyDetector.MultiDetection detection) {
        AfmaRect patchBounds = detection.patchBounds();
        AfmaPixelFrame referenceFrame = this.buildMultiCopyReferenceFrame(previousFrame, detection.multiCopy());
        if (patchBounds == null) {
            return new CopyPlan(
                    referenceFrame,
                    null,
                    detection.multiCopy(),
                    null,
                    List.of(new StrategyEstimate(CandidateKind.MULTI_COPY_BIN_INTRA, 0L, CandidateKind.MULTI_COPY_BIN_INTRA.stabilityRank()))
            );
        }

        AfmaFramePairAnalysis pairAnalysis = new AfmaFramePairAnalysis(referenceFrame, currentFrame);
        AfmaFramePairAnalysis.RegionDiffAnalysis regionAnalysis = pairAnalysis.regionDiffAnalysis(
                patchBounds.x(),
                patchBounds.y(),
                patchBounds.width(),
                patchBounds.height()
        );
        if (regionAnalysis == null) {
            return null;
        }
        return new CopyPlan(
                referenceFrame,
                null,
                detection.multiCopy(),
                patchBounds,
                this.rankCopyStrategies(regionAnalysis, true)
        );
    }

    @Nullable
    protected CopyPlan pickBetterCopyPlan(@Nullable CopyPlan first, @Nullable CopyPlan second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }

        StrategyEstimate firstEstimate = first.rankedStrategies().get(0);
        StrategyEstimate secondEstimate = second.rankedStrategies().get(0);
        int compare = this.compareStrategyEstimates(firstEstimate, secondEstimate);
        if (compare != 0) {
            return (compare <= 0) ? first : second;
        }

        long firstPatchArea = (first.patchBounds() != null) ? first.patchBounds().area() : 0L;
        long secondPatchArea = (second.patchBounds() != null) ? second.patchBounds().area() : 0L;
        if (firstPatchArea != secondPatchArea) {
            return (firstPatchArea < secondPatchArea) ? first : second;
        }
        return (first.multiCopy() == null) ? first : second;
    }

    @NotNull
    protected List<StrategyEstimate> rankCopyStrategies(@NotNull AfmaFramePairAnalysis.RegionDiffAnalysis regionAnalysis,
                                                        boolean multiCopy) {
        ResidualRegionStats residualStats = this.analyzeResidualStats(
                regionAnalysis.predictedColors(),
                regionAnalysis.currentColors(),
                regionAnalysis.pixelCount(),
                regionAnalysis.includeAlpha()
        );
        CandidateKind binIntraKind = multiCopy ? CandidateKind.MULTI_COPY_BIN_INTRA : CandidateKind.COPY_BIN_INTRA;
        CandidateKind residualKind = multiCopy ? CandidateKind.MULTI_COPY_RESIDUAL : CandidateKind.COPY_RESIDUAL;
        CandidateKind sparseKind = multiCopy ? CandidateKind.MULTI_COPY_SPARSE : CandidateKind.COPY_SPARSE;

        ArrayList<StrategyEstimate> estimates = new ArrayList<>(3);
        estimates.add(new StrategyEstimate(
                binIntraKind,
                this.estimateBinIntraRegionBytes(
                        regionAnalysis.width(),
                        regionAnalysis.height(),
                        regionAnalysis.pixelCount(),
                        regionAnalysis.changedPixelCount(),
                        regionAnalysis.includeAlpha()
                ),
                binIntraKind.stabilityRank()
        ));
        estimates.add(new StrategyEstimate(
                residualKind,
                this.estimateResidualEncodedBytes(
                        regionAnalysis.pixelCount(),
                        AfmaResidualPayloadHelper.channelCount(regionAnalysis.includeAlpha()),
                        residualStats.averageMagnitude()
                ),
                residualKind.stabilityRank()
        ));

        if (this.canAttemptSparse(regionAnalysis.pixelCount(), regionAnalysis.changedPixelCount())) {
            ChangedRegionData changedRegion = this.copyChangedRegion(regionAnalysis);
            estimates.add(new StrategyEstimate(
                    sparseKind,
                    this.estimateBestSparseLayoutBytes(regionAnalysis.width(), regionAnalysis.height(), changedRegion.changedIndices(), changedRegion.changedCount())
                            + this.estimateResidualEncodedBytes(
                            changedRegion.changedCount(),
                            AfmaResidualPayloadHelper.channelCount(regionAnalysis.includeAlpha()),
                            residualStats.averageChangedMagnitude()
                    ),
                    sparseKind.stabilityRank()
            ));
        }

        estimates.sort(this::compareStrategyEstimates);
        return estimates;
    }

    @Nullable
    protected FrameCandidate createCopyBinIntraCandidate(@NotNull AfmaPixelFrame previousFrame,
                                                         @NotNull AfmaPixelFrame currentFrame,
                                                         boolean introSequence, int frameIndex,
                                                         @NotNull AfmaRectCopyDetector.Detection detection,
                                                         @NotNull AfmaEncodeOptions options,
                                                         boolean allowPerceptual) throws IOException {
        AfmaCopyRect copyRect = detection.copyRect();
        AfmaRect patchBounds = detection.patchBounds();
        AfmaPixelFrame referenceFrame = this.buildSingleCopyReferenceFrame(previousFrame, copyRect);
        if (patchBounds == null) {
            return new FrameCandidate(
                    CandidateKind.COPY_BIN_INTRA,
                    AfmaFrameDescriptor.copyRectPatch(copyRect, null),
                    null,
                    null,
                    null,
                    null,
                    currentFrame,
                    3,
                    CandidateQualityMetrics.losslessMetrics()
            );
        }

        String payloadPath = this.buildPayloadPath(introSequence, frameIndex);
        AfmaBinIntraPayloadHelper.ScoredPayloadResult encodedPayload = this.scoreBinIntraPayloadRegion(
                currentFrame,
                patchBounds.x(),
                patchBounds.y(),
                patchBounds.width(),
                patchBounds.height(),
                options,
                allowPerceptual
        );
        AfmaPatchRegion patchRegion = patchBounds.toPatchRegion(payloadPath);
        AfmaPixelFrame outputFrame = encodedPayload.lossless()
                ? currentFrame
                : this.applyReconstructedPatch(referenceFrame, patchBounds, encodedPayload.reconstructedPixels());
        return new FrameCandidate(
                CandidateKind.COPY_BIN_INTRA,
                AfmaFrameDescriptor.copyRectPatch(copyRect, patchRegion),
                null,
                null,
                payloadPath,
                this.storePayload(encodedPayload.payloadSummary(), encodedPayload.payloadBytes()),
                outputFrame,
                3,
                CandidateQualityMetrics.losslessMetrics()
        );
    }

    @Nullable
    protected FrameCandidate createCopyResidualCandidate(@NotNull AfmaPixelFrame currentFrame,
                                                         boolean introSequence, int frameIndex,
                                                         @NotNull AfmaRectCopyDetector.Detection detection,
                                                         @NotNull AfmaFramePairAnalysis pairAnalysis) {
        AfmaRect patchBounds = detection.patchBounds();
        if (patchBounds == null) {
            return null;
        }

        ResidualPayloadData residualPayload = this.buildCopyResidualPayload(pairAnalysis, detection.copyRect(), patchBounds);
        if (residualPayload == null) {
            return null;
        }

        String payloadPath = this.buildRawPayloadPath(introSequence, frameIndex, "cr");
        return new FrameCandidate(
                CandidateKind.COPY_RESIDUAL,
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
                this.storePayload(residualPayload.payloadSummary(), residualPayload.payloadWriter()),
                null,
                null,
                currentFrame,
                4 + residualPayload.complexityScore(),
                CandidateQualityMetrics.losslessMetrics()
        );
    }

    @Nullable
    protected FrameCandidate createCopySparseCandidate(@NotNull AfmaPixelFrame currentFrame,
                                                       boolean introSequence, int frameIndex,
                                                       @NotNull AfmaRectCopyDetector.Detection detection,
                                                       @NotNull AfmaFramePairAnalysis pairAnalysis) throws IOException {
        AfmaRect patchBounds = detection.patchBounds();
        if (patchBounds == null) {
            return null;
        }

        SparseResidualPayloadData sparsePayload = this.buildCopySparsePayload(pairAnalysis, detection.copyRect(), patchBounds);
        if (sparsePayload == null) {
            return null;
        }

        String layoutPath = this.buildRawPayloadPath(introSequence, frameIndex, "cm");
        String residualPath = this.buildRawPayloadPath(introSequence, frameIndex, "cs");
        return new FrameCandidate(
                CandidateKind.COPY_SPARSE,
                AfmaFrameDescriptor.copyRectSparsePatch(
                        detection.copyRect(),
                        layoutPath,
                        patchBounds.x(),
                        patchBounds.y(),
                        patchBounds.width(),
                        patchBounds.height(),
                        sparsePayload.toMetadata(residualPath)
                ),
                layoutPath,
                this.storePayload(sparsePayload.layoutPayload()),
                residualPath,
                this.storePayload(sparsePayload.residualPayloadSummary(), sparsePayload.residualPayloadWriter()),
                currentFrame,
                5 + sparsePayload.complexityScore(),
                CandidateQualityMetrics.losslessMetrics()
        );
    }

    @Nullable
    protected FrameCandidate createMultiCopyBinIntraCandidate(@NotNull AfmaPixelFrame previousFrame,
                                                              @NotNull AfmaPixelFrame currentFrame,
                                                              boolean introSequence, int frameIndex,
                                                              @NotNull AfmaRectCopyDetector.MultiDetection detection,
                                                              @NotNull AfmaEncodeOptions options,
                                                              boolean allowPerceptual) throws IOException {
        AfmaRect patchBounds = detection.patchBounds();
        if (patchBounds == null) {
            return new FrameCandidate(
                    CandidateKind.MULTI_COPY_BIN_INTRA,
                    AfmaFrameDescriptor.multiCopyPatch(detection.multiCopy(), null),
                    null,
                    null,
                    null,
                    null,
                    currentFrame,
                    3 + detection.multiCopy().getCopyRectCount(),
                    CandidateQualityMetrics.losslessMetrics()
            );
        }

        AfmaPixelFrame referenceFrame = this.buildMultiCopyReferenceFrame(previousFrame, detection.multiCopy());
        String payloadPath = this.buildPayloadPath(introSequence, frameIndex);
        AfmaBinIntraPayloadHelper.ScoredPayloadResult encodedPayload = this.scoreBinIntraPayloadRegion(
                currentFrame,
                patchBounds.x(),
                patchBounds.y(),
                patchBounds.width(),
                patchBounds.height(),
                options,
                allowPerceptual
        );
        AfmaPatchRegion patchRegion = patchBounds.toPatchRegion(payloadPath);
        AfmaPixelFrame outputFrame = encodedPayload.lossless()
                ? currentFrame
                : this.applyReconstructedPatch(referenceFrame, patchBounds, encodedPayload.reconstructedPixels());
        return new FrameCandidate(
                CandidateKind.MULTI_COPY_BIN_INTRA,
                AfmaFrameDescriptor.multiCopyPatch(detection.multiCopy(), patchRegion),
                null,
                null,
                payloadPath,
                this.storePayload(encodedPayload.payloadSummary(), encodedPayload.payloadBytes()),
                outputFrame,
                3 + detection.multiCopy().getCopyRectCount(),
                CandidateQualityMetrics.losslessMetrics()
        );
    }

    @Nullable
    protected FrameCandidate createMultiCopyResidualCandidate(@NotNull AfmaPixelFrame currentFrame,
                                                              boolean introSequence, int frameIndex,
                                                              @NotNull AfmaRectCopyDetector.MultiDetection detection,
                                                              @NotNull AfmaPixelFrame referenceFrame) {
        AfmaRect patchBounds = detection.patchBounds();
        if (patchBounds == null) {
            return null;
        }

        AfmaFramePairAnalysis pairAnalysis = new AfmaFramePairAnalysis(referenceFrame, currentFrame);
        ResidualPayloadData residualPayload = this.buildResidualPayload(pairAnalysis, patchBounds);
        if (residualPayload == null) {
            return null;
        }

        String payloadPath = this.buildRawPayloadPath(introSequence, frameIndex, "mr");
        return new FrameCandidate(
                CandidateKind.MULTI_COPY_RESIDUAL,
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
                this.storePayload(residualPayload.payloadSummary(), residualPayload.payloadWriter()),
                null,
                null,
                currentFrame,
                4 + detection.multiCopy().getCopyRectCount() + residualPayload.complexityScore(),
                CandidateQualityMetrics.losslessMetrics()
        );
    }

    @Nullable
    protected FrameCandidate createMultiCopySparseCandidate(@NotNull AfmaPixelFrame currentFrame,
                                                            boolean introSequence, int frameIndex,
                                                            @NotNull AfmaRectCopyDetector.MultiDetection detection,
                                                            @NotNull AfmaPixelFrame referenceFrame) throws IOException {
        AfmaRect patchBounds = detection.patchBounds();
        if (patchBounds == null) {
            return null;
        }

        AfmaFramePairAnalysis pairAnalysis = new AfmaFramePairAnalysis(referenceFrame, currentFrame);
        SparseResidualPayloadData sparsePayload = this.buildSparseDeltaPayload(pairAnalysis, patchBounds);
        if (sparsePayload == null) {
            return null;
        }

        String layoutPath = this.buildRawPayloadPath(introSequence, frameIndex, "mm");
        String residualPath = this.buildRawPayloadPath(introSequence, frameIndex, "ms");
        return new FrameCandidate(
                CandidateKind.MULTI_COPY_SPARSE,
                AfmaFrameDescriptor.multiCopySparsePatch(
                        detection.multiCopy(),
                        layoutPath,
                        patchBounds.x(),
                        patchBounds.y(),
                        patchBounds.width(),
                        patchBounds.height(),
                        sparsePayload.toMetadata(residualPath)
                ),
                layoutPath,
                this.storePayload(sparsePayload.layoutPayload()),
                residualPath,
                this.storePayload(sparsePayload.residualPayloadSummary(), sparsePayload.residualPayloadWriter()),
                currentFrame,
                5 + detection.multiCopy().getCopyRectCount() + sparsePayload.complexityScore(),
                CandidateQualityMetrics.losslessMetrics()
        );
    }

    @Nullable
    protected FrameCandidate createBlockInterFamilyCandidate(@NotNull AfmaPixelFrame previousFrame,
                                                             @NotNull AfmaPixelFrame currentFrame,
                                                             @NotNull AfmaFramePairAnalysis pairAnalysis,
                                                             boolean introSequence, int frameIndex,
                                                             @NotNull AfmaRect deltaBounds,
                                                             @NotNull AfmaRectCopyDetector copyDetector,
                                                             @NotNull CopyEvaluation copyEvaluation,
                                                             long referenceBytes,
                                                             @Nullable ExecutorService executor,
                                                             @Nullable BooleanSupplier cancellationRequested) throws IOException {
        if (deltaBounds.area() < BLOCK_INTER_MIN_REGION_AREA) {
            return null;
        }

        AfmaRect regionBounds = this.alignBoundsToTileGrid(deltaBounds, BLOCK_INTER_TILE_SIZE, currentFrame.getWidth(), currentFrame.getHeight());
        int tileCountX = AfmaBlockInterPayloadHelper.tileCount(regionBounds.width(), BLOCK_INTER_TILE_SIZE);
        int tileCountY = AfmaBlockInterPayloadHelper.tileCount(regionBounds.height(), BLOCK_INTER_TILE_SIZE);
        int totalTileCount = tileCountX * tileCountY;
        if (totalTileCount < 4) {
            return null;
        }

        List<MotionVector> motionVectors = this.collectBlockInterMotionVectors(copyDetector, pairAnalysis, copyEvaluation);
        if (motionVectors.isEmpty()) {
            return null;
        }

        ApproxBlockInterTile[] tilePlans = this.planApproximateBlockInterTiles(
                previousFrame,
                currentFrame,
                pairAnalysis,
                regionBounds,
                tileCountX,
                tileCountY,
                motionVectors,
                executor,
                cancellationRequested
        );
        if (tilePlans == null) {
            return null;
        }

        long estimatedPayloadBytes = 9L;
        int changedTiles = 0;
        for (ApproxBlockInterTile tilePlan : tilePlans) {
            estimatedPayloadBytes += tilePlan.estimatedBytes();
            if (tilePlan.mode() != AfmaBlockInterPayloadHelper.TileMode.SKIP) {
                changedTiles++;
            }
        }
        if (changedTiles < 2) {
            return null;
        }

        long estimatedTotalBytes = estimatedPayloadBytes
                + this.estimateDescriptorBytes(AfmaFrameDescriptor.blockInter(
                this.buildRawPayloadPath(introSequence, frameIndex, "bi"),
                regionBounds.x(),
                regionBounds.y(),
                regionBounds.width(),
                regionBounds.height(),
                new AfmaBlockInter(BLOCK_INTER_TILE_SIZE)
        ));
        if (estimatedTotalBytes >= Math.round(referenceBytes * BLOCK_INTER_REQUIRED_SAVINGS_RATIO)
                && (estimatedTotalBytes + FAMILY_SWITCH_MARGIN_BYTES) >= referenceBytes) {
            return null;
        }

        String payloadPath = this.buildRawPayloadPath(introSequence, frameIndex, "bi");
        List<AfmaBlockInterPayloadHelper.TileOperation> tileOperations = this.materializeBlockInterTileOperations(
                tilePlans,
                previousFrame,
                currentFrame,
                pairAnalysis
        );
        AfmaStoredPayload.Writer payloadWriter = out -> AfmaBlockInterPayloadHelper.writePayload(
                out,
                BLOCK_INTER_TILE_SIZE,
                regionBounds.width(),
                regionBounds.height(),
                tileOperations
        );
        return new FrameCandidate(
                CandidateKind.BLOCK_INTER,
                AfmaFrameDescriptor.blockInter(
                        payloadPath,
                        regionBounds.x(),
                        regionBounds.y(),
                        regionBounds.width(),
                        regionBounds.height(),
                        new AfmaBlockInter(BLOCK_INTER_TILE_SIZE)
                ),
                payloadPath,
                this.storePayload(AfmaStoredPayload.summarize(payloadWriter), payloadWriter),
                null,
                null,
                currentFrame,
                7,
                CandidateQualityMetrics.losslessMetrics()
        );
    }

    @Nullable
    protected ApproxBlockInterTile[] planApproximateBlockInterTiles(@NotNull AfmaPixelFrame previousFrame,
                                                                    @NotNull AfmaPixelFrame currentFrame,
                                                                    @NotNull AfmaFramePairAnalysis pairAnalysis,
                                                                    @NotNull AfmaRect regionBounds,
                                                                    int tileCountX, int tileCountY,
                                                                    @NotNull List<MotionVector> motionVectors,
                                                                    @Nullable ExecutorService executor,
                                                                    @Nullable BooleanSupplier cancellationRequested) {
        AfmaFramePairAnalysis.TileGridSummary tileGridSummary = pairAnalysis.tileGridSummary(BLOCK_INTER_TILE_SIZE);
        int baseTileX = regionBounds.x() / BLOCK_INTER_TILE_SIZE;
        int baseTileY = regionBounds.y() / BLOCK_INTER_TILE_SIZE;
        int totalTileCount = tileCountX * tileCountY;
        ApproxBlockInterTile[] tilePlans = new ApproxBlockInterTile[totalTileCount];
        if ((executor != null) && (totalTileCount >= MIN_PARALLEL_BLOCK_INTER_TILES) && (Runtime.getRuntime().availableProcessors() > 1)) {
            int workerCount = Math.min(totalTileCount, Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
            int tileBatchSize = Math.max(1, (totalTileCount + workerCount - 1) / workerCount);
            ArrayList<CompletableFuture<Void>> futures = new ArrayList<>(workerCount);
            for (int startTileIndex = 0; startTileIndex < totalTileCount; startTileIndex += tileBatchSize) {
                int rangeStart = startTileIndex;
                int rangeEnd = Math.min(totalTileCount, rangeStart + tileBatchSize);
                futures.add(CompletableFuture.runAsync(() -> this.planApproximateBlockInterTileRange(
                        previousFrame,
                        currentFrame,
                        tileGridSummary,
                        regionBounds,
                        tileCountX,
                        tileCountY,
                        baseTileX,
                        baseTileY,
                        rangeStart,
                        rangeEnd,
                        motionVectors,
                        tilePlans,
                        cancellationRequested
                ), executor));
            }
            try {
                CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
            } catch (CompletionException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof CancellationException cancellationException) {
                    throw cancellationException;
                }
                if (cause instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                if (cause instanceof Error error) {
                    throw error;
                }
                throw new IllegalStateException("Failed to approximate AFMA v2 block_inter tiles", cause);
            }
        } else {
            this.planApproximateBlockInterTileRange(
                    previousFrame,
                    currentFrame,
                    tileGridSummary,
                    regionBounds,
                    tileCountX,
                    tileCountY,
                    baseTileX,
                    baseTileY,
                    0,
                    totalTileCount,
                    motionVectors,
                    tilePlans,
                    cancellationRequested
            );
        }
        return tilePlans;
    }

    protected void planApproximateBlockInterTileRange(@NotNull AfmaPixelFrame previousFrame,
                                                      @NotNull AfmaPixelFrame currentFrame,
                                                      @NotNull AfmaFramePairAnalysis.TileGridSummary tileGridSummary,
                                                      @NotNull AfmaRect regionBounds,
                                                      int tileCountX,
                                                      int tileCountY,
                                                      int baseTileX,
                                                      int baseTileY,
                                                      int startTileIndex,
                                                      int endTileIndex,
                                                      @NotNull List<MotionVector> motionVectors,
                                                      @NotNull ApproxBlockInterTile[] tilePlans,
                                                      @Nullable BooleanSupplier cancellationRequested) {
        for (int tileIndex = startTileIndex; tileIndex < endTileIndex; tileIndex++) {
            checkCancelled(cancellationRequested);
            int tileY = tileIndex / tileCountX;
            int tileX = tileIndex % tileCountX;
            int dstX = regionBounds.x() + (tileX * BLOCK_INTER_TILE_SIZE);
            int dstY = regionBounds.y() + (tileY * BLOCK_INTER_TILE_SIZE);
            int width = AfmaBlockInterPayloadHelper.tileDimension(tileX, tileCountX, BLOCK_INTER_TILE_SIZE, regionBounds.width());
            int height = AfmaBlockInterPayloadHelper.tileDimension(tileY, tileCountY, BLOCK_INTER_TILE_SIZE, regionBounds.height());
            AfmaFramePairAnalysis.TileStats tileStats = tileGridSummary.tileStats(baseTileX + tileX, baseTileY + tileY);
            tilePlans[tileIndex] = this.planApproximateBlockInterTile(previousFrame, currentFrame, tileStats, dstX, dstY, width, height, motionVectors);
        }
    }

    @NotNull
    protected ApproxBlockInterTile planApproximateBlockInterTile(@NotNull AfmaPixelFrame previousFrame,
                                                                 @NotNull AfmaPixelFrame currentFrame,
                                                                 @NotNull AfmaFramePairAnalysis.TileStats tileStats,
                                                                 int dstX, int dstY, int width, int height,
                                                                 @NotNull List<MotionVector> motionVectors) {
        int rawChannels = tileStats.nextHasNonOpaquePixels()
                ? AfmaResidualPayloadHelper.RGBA_CHANNELS
                : AfmaResidualPayloadHelper.RGB_CHANNELS;
        ApproxBlockInterTile bestTile = new ApproxBlockInterTile(
                AfmaBlockInterPayloadHelper.TileMode.RAW,
                dstX,
                dstY,
                width,
                height,
                0,
                0,
                rawChannels,
                0,
                0,
                this.estimateBlockInterTileBytes(
                        AfmaBlockInterPayloadHelper.TileMode.RAW,
                        AfmaBlockInterPayloadHelper.expectedRawTileBytes(width, height, rawChannels),
                        0
                )
        );
        if (tileStats.isIdentical()) {
            return new ApproxBlockInterTile(
                    AfmaBlockInterPayloadHelper.TileMode.SKIP,
                    dstX,
                    dstY,
                    width,
                    height,
                    0,
                    0,
                    0,
                    0,
                    0,
                    this.estimateBlockInterTileBytes(AfmaBlockInterPayloadHelper.TileMode.SKIP, 0, 0)
            );
        }

        for (MotionVector motionVector : motionVectors) {
            int srcX = dstX + motionVector.dx();
            int srcY = dstY + motionVector.dy();
            if (!this.isMotionTileInBounds(previousFrame, srcX, srcY, width, height)) {
                continue;
            }

            MotionTileStats motionStats = this.scanMotionTile(previousFrame, currentFrame, dstX, dstY, srcX, srcY, width, height);
            if (motionStats.changedPixelCount() <= 0) {
                ApproxBlockInterTile copyTile = new ApproxBlockInterTile(
                        AfmaBlockInterPayloadHelper.TileMode.COPY,
                        dstX,
                        dstY,
                        width,
                        height,
                        motionVector.dx(),
                        motionVector.dy(),
                        0,
                        0,
                        0,
                        this.estimateBlockInterTileBytes(AfmaBlockInterPayloadHelper.TileMode.COPY, 0, 0)
                );
                if (copyTile.estimatedBytes() < bestTile.estimatedBytes()) {
                    bestTile = copyTile;
                }
                continue;
            }

            int motionChannels = AfmaResidualPayloadHelper.channelCount(motionStats.includeAlpha());
            long denseBytes = this.estimateBlockInterTileBytes(
                    AfmaBlockInterPayloadHelper.TileMode.COPY_DENSE,
                    AfmaBlockInterPayloadHelper.expectedDenseResidualBytes(width, height, motionChannels),
                    0
            );
            ApproxBlockInterTile denseTile = new ApproxBlockInterTile(
                    AfmaBlockInterPayloadHelper.TileMode.COPY_DENSE,
                    dstX,
                    dstY,
                    width,
                    height,
                    motionVector.dx(),
                    motionVector.dy(),
                    motionChannels,
                    motionStats.changedPixelCount(),
                    motionStats.changedPixelCount(),
                    denseBytes
            );
            bestTile = this.pickBetterBlockInterTile(bestTile, denseTile);

            if (this.canAttemptSparse(width * height, motionStats.changedPixelCount())) {
                long sparseBytes = this.estimateBlockInterTileBytes(
                        AfmaBlockInterPayloadHelper.TileMode.COPY_SPARSE,
                        this.estimateOptimisticSparseLayoutBytes(width, height, motionStats.changedPixelCount()),
                        this.estimateResidualEncodedBytes(motionStats.changedPixelCount(), motionChannels, 8D)
                );
                ApproxBlockInterTile sparseTile = new ApproxBlockInterTile(
                        AfmaBlockInterPayloadHelper.TileMode.COPY_SPARSE,
                        dstX,
                        dstY,
                        width,
                        height,
                        motionVector.dx(),
                        motionVector.dy(),
                        motionChannels,
                        motionStats.changedPixelCount(),
                        motionStats.changedPixelCount(),
                        sparseBytes
                );
                bestTile = this.pickBetterBlockInterTile(bestTile, sparseTile);
            }
        }

        return bestTile;
    }

    @NotNull
    protected ApproxBlockInterTile pickBetterBlockInterTile(@NotNull ApproxBlockInterTile first, @NotNull ApproxBlockInterTile second) {
        if (first.estimatedBytes() != second.estimatedBytes()) {
            return (first.estimatedBytes() < second.estimatedBytes()) ? first : second;
        }
        if (first.mode() != second.mode()) {
            return (first.mode().ordinal() <= second.mode().ordinal()) ? first : second;
        }
        return (first.changedPixelCount() <= second.changedPixelCount()) ? first : second;
    }

    @NotNull
    protected List<MotionVector> collectBlockInterMotionVectors(@NotNull AfmaRectCopyDetector copyDetector,
                                                                @NotNull AfmaFramePairAnalysis pairAnalysis,
                                                                @NotNull CopyEvaluation copyEvaluation) {
        LinkedHashSet<MotionVector> vectors = new LinkedHashSet<>();
        if (copyEvaluation.singleDetection() != null) {
            AfmaCopyRect copyRect = copyEvaluation.singleDetection().copyRect();
            vectors.add(new MotionVector(copyRect.getSrcX() - copyRect.getDstX(), copyRect.getSrcY() - copyRect.getDstY()));
        }
        for (AfmaRectCopyDetector.MotionVector motionVector : copyDetector.collectMotionVectors(pairAnalysis, false)) {
            vectors.add(new MotionVector(-motionVector.dx(), -motionVector.dy()));
            if (vectors.size() >= MAX_BLOCK_INTER_MOTION_VECTORS) {
                break;
            }
        }
        if (vectors.isEmpty()) {
            return List.of();
        }
        return List.copyOf(vectors);
    }

    @NotNull
    protected List<AfmaBlockInterPayloadHelper.TileOperation> materializeBlockInterTileOperations(@NotNull ApproxBlockInterTile[] tilePlans,
                                                                                                  @NotNull AfmaPixelFrame previousFrame,
                                                                                                  @NotNull AfmaPixelFrame currentFrame,
                                                                                                  @NotNull AfmaFramePairAnalysis pairAnalysis) {
        List<AfmaBlockInterPayloadHelper.TileOperation> operations = new ArrayList<>(tilePlans.length);
        for (ApproxBlockInterTile tilePlan : tilePlans) {
            operations.add(this.materializeBlockInterTileOperation(tilePlan, previousFrame, currentFrame, pairAnalysis));
        }
        return operations;
    }

    @NotNull
    protected AfmaBlockInterPayloadHelper.TileOperation materializeBlockInterTileOperation(@NotNull ApproxBlockInterTile tilePlan,
                                                                                           @NotNull AfmaPixelFrame previousFrame,
                                                                                           @NotNull AfmaPixelFrame currentFrame,
                                                                                           @NotNull AfmaFramePairAnalysis pairAnalysis) {
        return switch (tilePlan.mode()) {
            case SKIP -> new AfmaBlockInterPayloadHelper.TileOperation(AfmaBlockInterPayloadHelper.TileMode.SKIP, 0, 0, 0, 0, null, null, null);
            case COPY -> new AfmaBlockInterPayloadHelper.TileOperation(
                    AfmaBlockInterPayloadHelper.TileMode.COPY,
                    tilePlan.dx(),
                    tilePlan.dy(),
                    0,
                    0,
                    null,
                    null,
                    null
            );
            case COPY_DENSE -> {
                byte[] denseResidual = Objects.requireNonNull(
                        this.buildMotionResidualPayload(
                                previousFrame,
                                currentFrame,
                                tilePlan.dstX(),
                                tilePlan.dstY(),
                                tilePlan.dstX() + tilePlan.dx(),
                                tilePlan.dstY() + tilePlan.dy(),
                                tilePlan.width(),
                                tilePlan.height(),
                                tilePlan.channels() == AfmaResidualPayloadHelper.RGBA_CHANNELS
                        ),
                        "AFMA v2 block_inter dense residual payload was NULL"
                );
                yield new AfmaBlockInterPayloadHelper.TileOperation(
                        AfmaBlockInterPayloadHelper.TileMode.COPY_DENSE,
                        tilePlan.dx(),
                        tilePlan.dy(),
                        tilePlan.channels(),
                        0,
                        denseResidual,
                        null,
                        null
                );
            }
            case COPY_SPARSE -> {
                MotionTileStats tileStats = new MotionTileStats(tilePlan.changedPixelCount(), tilePlan.channels() == AfmaResidualPayloadHelper.RGBA_CHANNELS);
                SparseResidualPayloadData sparsePayload = this.buildMotionSparseResidualPayload(
                        pairAnalysis,
                        previousFrame,
                        currentFrame,
                        tilePlan.dstX(),
                        tilePlan.dstY(),
                        tilePlan.dstX() + tilePlan.dx(),
                        tilePlan.dstY() + tilePlan.dy(),
                        tilePlan.width(),
                        tilePlan.height(),
                        tileStats
                );
                if (sparsePayload != null) {
                    yield new AfmaBlockInterPayloadHelper.TileOperation(
                            AfmaBlockInterPayloadHelper.TileMode.COPY_SPARSE,
                            tilePlan.dx(),
                            tilePlan.dy(),
                            sparsePayload.channels(),
                            sparsePayload.changedPixelCount(),
                            sparsePayload.layoutPayload(),
                            sparsePayload.materializeResidualPayload(),
                            sparsePayload.toMetadata(null)
                    );
                }

                byte[] denseResidual = Objects.requireNonNull(
                        this.buildMotionResidualPayload(
                                previousFrame,
                                currentFrame,
                                tilePlan.dstX(),
                                tilePlan.dstY(),
                                tilePlan.dstX() + tilePlan.dx(),
                                tilePlan.dstY() + tilePlan.dy(),
                                tilePlan.width(),
                                tilePlan.height(),
                                tilePlan.channels() == AfmaResidualPayloadHelper.RGBA_CHANNELS
                        ),
                        "AFMA v2 block_inter dense fallback payload was NULL"
                );
                yield new AfmaBlockInterPayloadHelper.TileOperation(
                        AfmaBlockInterPayloadHelper.TileMode.COPY_DENSE,
                        tilePlan.dx(),
                        tilePlan.dy(),
                        tilePlan.channels(),
                        0,
                        denseResidual,
                        null,
                        null
                );
            }
            case RAW -> new AfmaBlockInterPayloadHelper.TileOperation(
                    AfmaBlockInterPayloadHelper.TileMode.RAW,
                    0,
                    0,
                    tilePlan.channels(),
                    0,
                    this.buildRawTileBytes(currentFrame, tilePlan.dstX(), tilePlan.dstY(), tilePlan.width(), tilePlan.height(), tilePlan.channels()),
                    null,
                    null
            );
        };
    }

    @NotNull
    protected AfmaPixelFrame buildSingleCopyReferenceFrame(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaCopyRect copyRect) {
        int[] copiedPixels = previousFrame.copyPixels();
        AfmaPixelFrameHelper.applyCopyRect(copiedPixels, previousFrame.getWidth(), copyRect);
        return new AfmaPixelFrame(previousFrame.getWidth(), previousFrame.getHeight(), copiedPixels);
    }

    @NotNull
    protected AfmaPixelFrame buildMultiCopyReferenceFrame(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaMultiCopy multiCopy) {
        int[] copiedPixels = previousFrame.copyPixels();
        AfmaPixelFrameHelper.applyCopyRects(copiedPixels, previousFrame.getWidth(), multiCopy.getCopyRects());
        return new AfmaPixelFrame(previousFrame.getWidth(), previousFrame.getHeight(), copiedPixels);
    }

    @NotNull
    protected AfmaPixelFrame applyReconstructedPatch(@NotNull AfmaPixelFrame referenceFrame,
                                                     @NotNull AfmaRect patchBounds,
                                                     @NotNull int[] reconstructedPixels) {
        int[] pixels = referenceFrame.copyPixels();
        int frameWidth = referenceFrame.getWidth();
        int offset = 0;
        for (int localY = 0; localY < patchBounds.height(); localY++) {
            int rowOffset = ((patchBounds.y() + localY) * frameWidth) + patchBounds.x();
            System.arraycopy(reconstructedPixels, offset, pixels, rowOffset, patchBounds.width());
            offset += patchBounds.width();
        }
        return new AfmaPixelFrame(referenceFrame.getWidth(), referenceFrame.getHeight(), pixels);
    }

    protected boolean canAttemptSparse(int pixelCount, int changedPixelCount) {
        if (pixelCount <= 0 || changedPixelCount < MIN_SPARSE_DELTA_CHANGED_PIXELS || changedPixelCount >= pixelCount) {
            return false;
        }
        return ((double) changedPixelCount / (double) pixelCount) <= MAX_SPARSE_DELTA_CHANGED_DENSITY;
    }

    protected int compareStrategyEstimates(@NotNull StrategyEstimate first, @NotNull StrategyEstimate second) {
        long minBytes = Math.min(first.estimatedBytes(), second.estimatedBytes());
        long margin = Math.max(FAMILY_SWITCH_MARGIN_BYTES, Math.round(minBytes * FAMILY_SWITCH_MARGIN_RATIO));
        long delta = first.estimatedBytes() - second.estimatedBytes();
        if (Math.abs(delta) <= margin) {
            return Integer.compare(first.stabilityRank(), second.stabilityRank());
        }
        return Long.compare(first.estimatedBytes(), second.estimatedBytes());
    }

    protected long estimateBinIntraRegionBytes(int width, int height, int pixelCount, int changedPixelCount, boolean includeAlpha) {
        double density = (pixelCount > 0) ? ((double) changedPixelCount / (double) pixelCount) : 1D;
        double bytesPerPixel = includeAlpha ? 2.00D : 1.60D;
        if (pixelCount <= 4096) {
            bytesPerPixel += 0.18D;
        }
        if (density <= 0.25D) {
            bytesPerPixel *= 0.92D;
        }
        if (density <= 0.10D) {
            bytesPerPixel *= 0.88D;
        }
        return Math.max(24L, Math.round(24D + ((long) width * height * bytesPerPixel)));
    }

    protected long estimateResidualEncodedBytes(int sampleCount, int channels, double averageMagnitude) {
        if (sampleCount <= 0 || channels <= 0) {
            return 0L;
        }
        double compressionFactor;
        if (averageMagnitude <= 4D) {
            compressionFactor = 0.34D;
        } else if (averageMagnitude <= 10D) {
            compressionFactor = 0.48D;
        } else if (averageMagnitude <= 20D) {
            compressionFactor = 0.62D;
        } else {
            compressionFactor = 0.78D;
        }
        return Math.max(8L, Math.round(8D + ((long) sampleCount * channels * compressionFactor)));
    }

    protected long estimateBestSparseLayoutBytes(int width, int height, @NotNull int[] changedIndices, int changedPixelCount) {
        SparseLayoutEstimate estimate = this.estimateSparseLayoutBytes(width, height, changedIndices, changedPixelCount);
        return Math.min(Math.min(estimate.bitmaskBytes(), estimate.rowSpanBytes()), Math.min(estimate.tileMaskBytes(), estimate.coordListBytes()));
    }

    protected long estimateOptimisticSparseLayoutBytes(int width, int height, int changedPixelCount) {
        long bitmaskBytes = AfmaResidualPayloadHelper.expectedSparseMaskBytes(width, height);
        long coordListBytes = Math.max(1L, changedPixelCount) * 2L;
        long rowSpanBytes = Math.max(4L, changedPixelCount * 2L);
        long tileSize = AfmaSparsePayloadHelper.TILE_MASK_TILE_SIZE;
        long tileCountX = (width + tileSize - 1L) / tileSize;
        long tileCountY = (height + tileSize - 1L) / tileSize;
        long tileMaskBytes = AfmaResidualPayloadHelper.expectedSparseBitsetBytes(tileCountX * tileCountY)
                + (Math.max(1L, (changedPixelCount + 15L) / 16L) * 8L);
        return Math.min(Math.min(bitmaskBytes, coordListBytes), Math.min(rowSpanBytes, tileMaskBytes));
    }

    @NotNull
    protected ResidualRegionStats analyzeResidualStats(@NotNull int[] predictedColors, @NotNull int[] currentColors,
                                                       int sampleCount, boolean includeAlpha) {
        if (sampleCount <= 0) {
            return ResidualRegionStats.EMPTY;
        }

        long totalMagnitude = 0L;
        int changedSamples = 0;
        long changedMagnitude = 0L;
        for (int i = 0; i < sampleCount; i++) {
            int predictedColor = predictedColors[i];
            int currentColor = currentColors[i];
            int magnitude = channelDifference(predictedColor >> 16, currentColor >> 16)
                    + channelDifference(predictedColor >> 8, currentColor >> 8)
                    + channelDifference(predictedColor, currentColor);
            if (includeAlpha) {
                magnitude += channelDifference(predictedColor >>> 24, currentColor >>> 24);
            }
            totalMagnitude += magnitude;
            if (predictedColor != currentColor) {
                changedSamples++;
                changedMagnitude += magnitude;
            }
        }
        return new ResidualRegionStats(
                totalMagnitude / (double) sampleCount,
                (changedSamples > 0) ? (changedMagnitude / (double) changedSamples) : 0D
        );
    }

    @NotNull
    protected ChangedRegionData copyChangedRegion(@NotNull AfmaFramePairAnalysis.RegionDiffAnalysis regionAnalysis) {
        int changedPixelCount = regionAnalysis.changedPixelCount();
        ResidualPlannerWorkspace workspace = this.residualPlannerWorkspace.get();
        int[] changedIndices = Arrays.copyOf(workspace.changedIndices(changedPixelCount), changedPixelCount);
        int[] predictedColors = Arrays.copyOf(workspace.predictedColors(changedPixelCount), changedPixelCount);
        int[] currentColors = Arrays.copyOf(workspace.currentColors(changedPixelCount), changedPixelCount);
        regionAnalysis.copyChangedPixelsTo(changedIndices, predictedColors, currentColors);
        return new ChangedRegionData(changedIndices, predictedColors, currentColors, changedPixelCount);
    }

    @Nullable
    protected ResidualPayloadData buildResidualPayload(@NotNull AfmaFramePairAnalysis pairAnalysis, @NotNull AfmaRect deltaBounds) {
        AfmaFramePairAnalysis.RegionDiffAnalysis regionAnalysis = pairAnalysis.regionDiffAnalysis(
                deltaBounds.x(),
                deltaBounds.y(),
                deltaBounds.width(),
                deltaBounds.height()
        );
        return this.buildResidualPayload(regionAnalysis);
    }

    @Nullable
    protected ResidualPayloadData buildResidualPayload(@Nullable AfmaFramePairAnalysis.RegionDiffAnalysis regionAnalysis) {
        if ((regionAnalysis == null) || (regionAnalysis.pixelCount() <= 0)) {
            return null;
        }

        ResidualPlannerWorkspace workspace = this.residualPlannerWorkspace.get();
        AfmaResidualPayloadHelper.EncodedResidualPayload encodedPayload = AfmaResidualPayloadHelper.encodeBestResidualPayload(
                regionAnalysis.predictedColors(),
                regionAnalysis.currentColors(),
                regionAnalysis.pixelCount(),
                regionAnalysis.includeAlpha(),
                workspace.residualEncodeWorkspace()
        );
        return new ResidualPayloadData(
                encodedPayload.payloadSummary(),
                encodedPayload.payloadWriter(),
                encodedPayload.toResidualMetadata(),
                encodedPayload.complexityScore()
        );
    }

    @Nullable
    protected ResidualPayloadData buildCopyResidualPayload(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                                           @NotNull AfmaCopyRect copyRect,
                                                           @NotNull AfmaRect patchBounds) {
        AfmaFramePairAnalysis.RegionDiffAnalysis regionAnalysis = pairAnalysis.copyAdjustedRegionDiffAnalysis(
                copyRect,
                patchBounds.x(),
                patchBounds.y(),
                patchBounds.width(),
                patchBounds.height()
        );
        return this.buildResidualPayload(regionAnalysis);
    }

    @Nullable
    protected SparseResidualPayloadData buildSparseDeltaPayload(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                                                @NotNull AfmaRect deltaBounds) throws IOException {
        AfmaFramePairAnalysis.RegionDiffAnalysis regionAnalysis = pairAnalysis.regionDiffAnalysis(
                deltaBounds.x(),
                deltaBounds.y(),
                deltaBounds.width(),
                deltaBounds.height()
        );
        return this.buildSparseResidualPayload(regionAnalysis);
    }

    @Nullable
    protected SparseResidualPayloadData buildCopySparsePayload(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                                               @NotNull AfmaCopyRect copyRect,
                                                               @NotNull AfmaRect patchBounds) throws IOException {
        AfmaFramePairAnalysis.RegionDiffAnalysis regionAnalysis = pairAnalysis.copyAdjustedRegionDiffAnalysis(
                copyRect,
                patchBounds.x(),
                patchBounds.y(),
                patchBounds.width(),
                patchBounds.height()
        );
        return this.buildSparseResidualPayload(regionAnalysis);
    }

    @Nullable
    protected SparseResidualPayloadData buildSparseResidualPayload(@Nullable AfmaFramePairAnalysis.RegionDiffAnalysis regionAnalysis) throws IOException {
        if (regionAnalysis == null || !this.canAttemptSparse(regionAnalysis.pixelCount(), regionAnalysis.changedPixelCount())) {
            return null;
        }

        ResidualPlannerWorkspace workspace = this.residualPlannerWorkspace.get();
        int changedPixelCount = regionAnalysis.changedPixelCount();
        int[] changedIndices = workspace.changedIndices(changedPixelCount);
        int[] predictedColors = workspace.predictedColors(changedPixelCount);
        int[] currentColors = workspace.currentColors(changedPixelCount);
        regionAnalysis.copyChangedPixelsTo(changedIndices, predictedColors, currentColors);
        return this.buildSparseResidualPayload(
                regionAnalysis.width(),
                regionAnalysis.height(),
                changedIndices,
                predictedColors,
                currentColors,
                changedPixelCount,
                regionAnalysis.includeAlpha()
        );
    }

    @Nullable
    protected SparseResidualPayloadData buildSparseResidualPayload(int width, int height,
                                                                  @NotNull int[] changedIndices,
                                                                  @NotNull int[] predictedColors,
                                                                  @NotNull int[] currentColors,
                                                                  int changedPixelCount,
                                                                  boolean includeAlpha) throws IOException {
        if (!this.canAttemptSparse(width * height, changedPixelCount)) {
            return null;
        }

        ResidualPlannerWorkspace workspace = this.residualPlannerWorkspace.get();
        AfmaResidualPayloadHelper.EncodedResidualPayload encodedResidual = AfmaResidualPayloadHelper.encodeBestResidualPayload(
                predictedColors,
                currentColors,
                changedPixelCount,
                includeAlpha,
                workspace.residualEncodeWorkspace()
        );
        SparseLayoutCandidate bestLayout = this.chooseBestSparseLayout(width, height, changedIndices, changedPixelCount);
        return new SparseResidualPayloadData(
                bestLayout.layoutPayload(),
                encodedResidual.payloadSummary(),
                encodedResidual.payloadWriter(),
                changedPixelCount,
                bestLayout.layoutCodec(),
                encodedResidual.channels(),
                encodedResidual.codec(),
                encodedResidual.alphaMode(),
                encodedResidual.alphaChangedPixelCount(),
                bestLayout.complexityScore() + encodedResidual.complexityScore()
        );
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
        return Objects.requireNonNull(bestCandidate, "Failed to choose an AFMA v2 sparse layout");
    }

    @NotNull
    protected ArrayList<SparseLayoutPlan> selectSparseLayoutPlans(int width, int height, @NotNull int[] changedIndices, int changedPixelCount) {
        ArrayList<SparseLayoutPlan> layoutPlans = new ArrayList<>(4);
        SparseLayoutEstimate layoutEstimate = this.estimateSparseLayoutBytes(width, height, changedIndices, changedPixelCount);
        layoutPlans.add(new SparseLayoutPlan(AfmaSparseLayoutCodec.BITMASK, layoutEstimate.bitmaskBytes()));
        layoutPlans.add(new SparseLayoutPlan(AfmaSparseLayoutCodec.ROW_SPANS, layoutEstimate.rowSpanBytes()));
        layoutPlans.add(new SparseLayoutPlan(AfmaSparseLayoutCodec.TILE_MASK, layoutEstimate.tileMaskBytes()));
        layoutPlans.add(new SparseLayoutPlan(AfmaSparseLayoutCodec.COORD_LIST, layoutEstimate.coordListBytes()));
        layoutPlans.sort((first, second) -> {
            int sizeCompare = Long.compare(first.estimatedRawBytes(), second.estimatedRawBytes());
            if (sizeCompare != 0) {
                return sizeCompare;
            }
            return Integer.compare(first.layoutCodec().getComplexityScore(), second.layoutCodec().getComplexityScore());
        });
        return layoutPlans;
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

        return new SparseLayoutEstimate(
                bitmaskBytes,
                this.estimateRowSpanLayoutBytes(width, changedIndices, changedPixelCount),
                tileMaskBytes,
                coordListBytes
        );
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

    @Nullable
    protected byte[] buildMotionResidualPayload(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
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
        return payloadBytes;
    }

    @Nullable
    protected SparseResidualPayloadData buildMotionSparseResidualPayload(@NotNull AfmaFramePairAnalysis pairAnalysis,
                                                                         @NotNull AfmaPixelFrame previousFrame,
                                                                         @NotNull AfmaPixelFrame currentFrame,
                                                                         int dstX, int dstY, int srcX, int srcY,
                                                                         int width, int height,
                                                                         @NotNull MotionTileStats tileStats) {
        if (tileStats.changedPixelCount() <= 0 || tileStats.changedPixelCount() >= (width * height)) {
            return null;
        }

        if ((dstX == srcX) && (dstY == srcY)) {
            try {
                AfmaFramePairAnalysis.RegionDiffAnalysis regionAnalysis = pairAnalysis.regionDiffAnalysis(dstX, dstY, width, height);
                return (regionAnalysis != null) ? this.buildSparseResidualPayload(regionAnalysis) : null;
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to encode AFMA v2 block_inter sparse tile payload", ex);
            }
        }

        int frameWidth = currentFrame.getWidth();
        int[] previousPixels = previousFrame.getPixelsUnsafe();
        int[] currentPixels = currentFrame.getPixelsUnsafe();
        int changedPixelCount = tileStats.changedPixelCount();
        ResidualPlannerWorkspace workspace = this.residualPlannerWorkspace.get();
        int[] changedIndices = workspace.changedIndices(changedPixelCount);
        int[] predictedColors = workspace.predictedColors(changedPixelCount);
        int[] changedColors = workspace.currentColors(changedPixelCount);
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
            throw new IllegalStateException("Failed to encode AFMA v2 block_inter sparse tile payload", ex);
        }
    }

    @NotNull
    protected MotionTileStats scanMotionTile(@NotNull AfmaPixelFrame previousFrame,
                                             @NotNull AfmaPixelFrame currentFrame,
                                             int dstX, int dstY, int srcX, int srcY, int width, int height) {
        int frameWidth = currentFrame.getWidth();
        int[] previousPixels = previousFrame.getPixelsUnsafe();
        int[] currentPixels = currentFrame.getPixelsUnsafe();
        int changedPixelCount = 0;
        boolean includeAlpha = false;
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
            }
        }
        return new MotionTileStats(changedPixelCount, includeAlpha);
    }

    protected boolean isMotionTileInBounds(@NotNull AfmaPixelFrame previousFrame, int srcX, int srcY, int width, int height) {
        return srcX >= 0
                && srcY >= 0
                && (srcX + width) <= previousFrame.getWidth()
                && (srcY + height) <= previousFrame.getHeight();
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

    protected long estimateBlockInterTileBytes(@NotNull AfmaBlockInterPayloadHelper.TileMode mode, long primaryBytes, long secondaryBytes) {
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
    protected AfmaPixelFrame applyNearLosslessTemporalMerge(@NotNull AfmaPixelFrame previousFrame,
                                                            @NotNull AfmaPixelFrame currentFrame,
                                                            int maxChannelDelta) {
        AfmaPixelFrameHelper.ensureSameSize(previousFrame, currentFrame);
        if (maxChannelDelta <= 0) {
            return currentFrame;
        }

        int[] previousPixels = previousFrame.getPixelsUnsafe();
        int[] currentPixels = currentFrame.getPixelsUnsafe();
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
        return new AfmaPixelFrame(currentFrame.getWidth(), currentFrame.getHeight(), mergedPixels);
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
        return options.isPerceptualBinIntraEnabled() && ((framesSinceKeyframe + 1) < maxInterval);
    }

    @NotNull
    protected AfmaBinIntraPayloadHelper.ScoredPayloadResult scoreBinIntraPayload(@NotNull AfmaPixelFrame frame,
                                                                                 @NotNull AfmaEncodeOptions options,
                                                                                 boolean allowPerceptual) throws IOException {
        return AfmaBinIntraPayloadHelper.scorePayloadDetailed(
                frame.getWidth(),
                frame.getHeight(),
                frame.getPixelsUnsafe(),
                0,
                frame.getWidth(),
                this.resolveBinIntraEncodePreferences(options, allowPerceptual)
        );
    }

    @NotNull
    protected AfmaBinIntraPayloadHelper.ScoredPayloadResult scoreBinIntraPayloadRegion(@NotNull AfmaPixelFrame frame,
                                                                                        int x, int y, int width, int height,
                                                                                        @NotNull AfmaEncodeOptions options,
                                                                                        boolean allowPerceptual) throws IOException {
        return AfmaBinIntraPayloadHelper.scorePayloadDetailed(
                width,
                height,
                frame.getPixelsUnsafe(),
                (y * frame.getWidth()) + x,
                frame.getWidth(),
                this.resolveBinIntraEncodePreferences(options, allowPerceptual)
        );
    }

    @NotNull
    protected AfmaBinIntraPayloadHelper.EncodePreferences resolveBinIntraEncodePreferences(@NotNull AfmaEncodeOptions options,
                                                                                           boolean allowPerceptual) {
        if (allowPerceptual && options.isPerceptualBinIntraEnabled()) {
            return AfmaBinIntraPayloadHelper.EncodePreferences.perceptual(
                    options.getPerceptualBinIntraMaxVisibleColorDelta(),
                    options.getPerceptualBinIntraMaxAlphaDelta(),
                    options.getPerceptualBinIntraMaxAverageError()
            );
        }
        return AfmaBinIntraPayloadHelper.EncodePreferences.lossless();
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
            throw new IllegalStateException("AFMA v2 temporal frame collapsing requires at least one emitted frame");
        }

        int lastIndex = plannedFrames.size() - 1;
        plannedFrames.set(lastIndex, plannedFrames.get(lastIndex).withAdditionalDelay(additionalDelayMs));
    }

    @NotNull
    protected PlannedSequence buildPlannedSequence(@NotNull List<PlannedTimedFrame> plannedFrames, long fallbackDefaultDelayMs) {
        List<AfmaFrameDescriptor> descriptors = new ArrayList<>(plannedFrames.size());
        List<Long> frameDelays = new ArrayList<>(plannedFrames.size());
        for (PlannedTimedFrame plannedFrame : plannedFrames) {
            descriptors.add(plannedFrame.descriptor());
            frameDelays.add(plannedFrame.delayMs());
        }
        AdaptiveTiming adaptiveTiming = this.buildAdaptiveTiming(frameDelays, fallbackDefaultDelayMs);
        return new PlannedSequence(descriptors, adaptiveTiming.defaultDelayMs(), adaptiveTiming.customFrameTimes());
    }

    @NotNull
    protected AdaptiveTiming buildAdaptiveTiming(@NotNull List<Long> frameDelays, long fallbackDefaultDelayMs) {
        long normalizedFallbackDelay = Math.max(1L, fallbackDefaultDelayMs);
        if (frameDelays.isEmpty()) {
            return new AdaptiveTiming(normalizedFallbackDelay, new LinkedHashMap<>());
        }

        List<Long> normalizedFrameDelays = new ArrayList<>(frameDelays.size());
        LinkedHashSet<Long> candidateDefaultDelays = new LinkedHashSet<>();
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

    protected static int estimateSignedVarIntBytes(int value) {
        int zigZag = (value << 1) ^ (value >> 31);
        return estimateVarIntBytes(zigZag);
    }

    protected int estimateDescriptorBytes(@NotNull AfmaFrameDescriptor descriptor) {
        AfmaFrameOperationType type = descriptor.getType();
        if (type == null) {
            return 1;
        }

        int bytes = 1;
        bytes += switch (type) {
            case FULL -> 2;
            case DELTA_RECT -> 2
                    + estimateVarIntBytes(descriptor.getX())
                    + estimateVarIntBytes(descriptor.getY())
                    + estimateVarIntBytes(descriptor.getWidth())
                    + estimateVarIntBytes(descriptor.getHeight());
            case RESIDUAL_DELTA_RECT -> 2
                    + estimateVarIntBytes(descriptor.getX())
                    + estimateVarIntBytes(descriptor.getY())
                    + estimateVarIntBytes(descriptor.getWidth())
                    + estimateVarIntBytes(descriptor.getHeight())
                    + estimateVarIntBytes(Objects.requireNonNull(descriptor.getResidual()).getChannels())
                    + 2
                    + estimateVarIntBytes(Objects.requireNonNull(descriptor.getResidual()).getAlphaChangedPixelCount());
            case SPARSE_DELTA_RECT -> 4
                    + estimateVarIntBytes(descriptor.getX())
                    + estimateVarIntBytes(descriptor.getY())
                    + estimateVarIntBytes(descriptor.getWidth())
                    + estimateVarIntBytes(descriptor.getHeight())
                    + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getChangedPixelCount())
                    + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getChannels())
                    + 3
                    + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getAlphaChangedPixelCount());
            case SAME -> 0;
            case COPY_RECT_PATCH -> {
                int copyBytes = this.estimateCopyRectBytes(Objects.requireNonNull(descriptor.getCopy()));
                AfmaPatchRegion patch = descriptor.getPatch();
                if (patch == null) {
                    yield copyBytes + 1;
                }
                yield copyBytes + 1 + 2
                        + estimateVarIntBytes(patch.getX())
                        + estimateVarIntBytes(patch.getY())
                        + estimateVarIntBytes(patch.getWidth())
                        + estimateVarIntBytes(patch.getHeight());
            }
            case MULTI_COPY_PATCH -> {
                int copyBytes = this.estimateMultiCopyBytes(Objects.requireNonNull(descriptor.getMultiCopy()));
                AfmaPatchRegion patch = descriptor.getPatch();
                if (patch == null) {
                    yield copyBytes + 1;
                }
                yield copyBytes + 1 + 2
                        + estimateVarIntBytes(patch.getX())
                        + estimateVarIntBytes(patch.getY())
                        + estimateVarIntBytes(patch.getWidth())
                        + estimateVarIntBytes(patch.getHeight());
            }
            case COPY_RECT_RESIDUAL_PATCH -> this.estimateCopyRectBytes(Objects.requireNonNull(descriptor.getCopy()))
                    + 2
                    + estimateVarIntBytes(descriptor.getX())
                    + estimateVarIntBytes(descriptor.getY())
                    + estimateVarIntBytes(descriptor.getWidth())
                    + estimateVarIntBytes(descriptor.getHeight())
                    + estimateVarIntBytes(Objects.requireNonNull(descriptor.getResidual()).getChannels())
                    + 2
                    + estimateVarIntBytes(Objects.requireNonNull(descriptor.getResidual()).getAlphaChangedPixelCount());
            case MULTI_COPY_RESIDUAL_PATCH -> this.estimateMultiCopyBytes(Objects.requireNonNull(descriptor.getMultiCopy()))
                    + 2
                    + estimateVarIntBytes(descriptor.getX())
                    + estimateVarIntBytes(descriptor.getY())
                    + estimateVarIntBytes(descriptor.getWidth())
                    + estimateVarIntBytes(descriptor.getHeight())
                    + estimateVarIntBytes(Objects.requireNonNull(descriptor.getResidual()).getChannels())
                    + 2
                    + estimateVarIntBytes(Objects.requireNonNull(descriptor.getResidual()).getAlphaChangedPixelCount());
            case COPY_RECT_SPARSE_PATCH -> this.estimateCopyRectBytes(Objects.requireNonNull(descriptor.getCopy()))
                    + 4
                    + estimateVarIntBytes(descriptor.getX())
                    + estimateVarIntBytes(descriptor.getY())
                    + estimateVarIntBytes(descriptor.getWidth())
                    + estimateVarIntBytes(descriptor.getHeight())
                    + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getChangedPixelCount())
                    + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getChannels())
                    + 3
                    + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getAlphaChangedPixelCount());
            case MULTI_COPY_SPARSE_PATCH -> this.estimateMultiCopyBytes(Objects.requireNonNull(descriptor.getMultiCopy()))
                    + 4
                    + estimateVarIntBytes(descriptor.getX())
                    + estimateVarIntBytes(descriptor.getY())
                    + estimateVarIntBytes(descriptor.getWidth())
                    + estimateVarIntBytes(descriptor.getHeight())
                    + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getChangedPixelCount())
                    + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getChannels())
                    + 3
                    + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getAlphaChangedPixelCount());
            case BLOCK_INTER -> 2
                    + estimateVarIntBytes(descriptor.getX())
                    + estimateVarIntBytes(descriptor.getY())
                    + estimateVarIntBytes(descriptor.getWidth())
                    + estimateVarIntBytes(descriptor.getHeight())
                    + estimateVarIntBytes(Objects.requireNonNull(descriptor.getBlockInter()).getTileSize());
        };
        return bytes;
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

    @NotNull
    protected LoadedDimensionFrame loadDimensionFrame(@NotNull AfmaSourceSequence sequence,
                                                      @NotNull AfmaFastPixelBufferPool pixelBufferPool,
                                                      @Nullable BooleanSupplier cancellationRequested,
                                                      @Nullable AfmaEncodePlanner.ProgressListener progressListener) throws IOException {
        if (sequence.isEmpty()) {
            throw new IOException("AFMA v2 encoding requires at least one source frame");
        }

        reportProgress(progressListener, "Reading source frame dimensions...", 0.02D);
        checkCancelled(cancellationRequested);
        File firstFrame = Objects.requireNonNull(sequence.getFrame(0));
        AfmaPixelFrame firstImage = this.frameNormalizer.loadFrame(firstFrame, pixelBufferPool);
        return new LoadedDimensionFrame(new Dimension(firstImage.getWidth(), firstImage.getHeight()), firstImage);
    }

    protected static void reportProgress(@Nullable AfmaEncodePlanner.ProgressListener progressListener,
                                         @NotNull String detail, double progress) {
        if (progressListener != null) {
            progressListener.update(detail, progress);
        }
    }

    protected void reportPlanningFrameProgress(@Nullable AfmaEncodePlanner.ProgressListener progressListener,
                                               @NotNull String action,
                                               boolean introSequence,
                                               int sequenceFrameNumber,
                                               int sequenceFrameCount,
                                               double absoluteFrameProgress,
                                               int totalFrameCount) {
        if (sequenceFrameCount <= 0) {
            return;
        }
        int clampedSequenceFrameNumber = Math.max(1, Math.min(sequenceFrameCount, sequenceFrameNumber));
        reportProgress(
                progressListener,
                action + " " + (introSequence ? "intro" : "main") + " frame " + clampedSequenceFrameNumber + "/" + sequenceFrameCount,
                this.computePlanningProgress(absoluteFrameProgress, totalFrameCount)
        );
    }

    protected double computePlanningProgress(double absoluteFrameProgress, int totalFrameCount) {
        double clampedFrameProgress = Math.max(0D, Math.min(Math.max(1, totalFrameCount), absoluteFrameProgress));
        return 0.08D + (0.92D * (clampedFrameProgress / Math.max(1D, (double) totalFrameCount)));
    }

    protected static void checkCancelled(@Nullable BooleanSupplier cancellationRequested) {
        if ((cancellationRequested != null) && cancellationRequested.getAsBoolean()) {
            throw new CancellationException("AFMA v2 encode planning was cancelled");
        }
    }

    protected record Dimension(int width, int height) {
    }

    protected record LoadedDimensionFrame(@NotNull Dimension dimension, @NotNull AfmaPixelFrame frame) {
    }

    protected record PlannedSequence(@NotNull List<AfmaFrameDescriptor> frames,
                                     long defaultDelayMs,
                                     @NotNull Map<Integer, Long> customFrameTimes) {
    }

    protected record PlannedTimedFrame(@NotNull AfmaFrameDescriptor descriptor, long delayMs) {

        @NotNull
        public PlannedTimedFrame withAdditionalDelay(long additionalDelayMs) {
            return new PlannedTimedFrame(this.descriptor, addDelaysSaturating(this.delayMs, Math.max(1L, additionalDelayMs)));
        }
    }

    protected record AdaptiveTiming(long defaultDelayMs, @NotNull LinkedHashMap<Integer, Long> customFrameTimes) {
    }

    protected record FrameDecision(@NotNull FrameCandidate candidate, @NotNull AfmaPixelFrame outputFrame) {
    }

    protected record CandidateQualityMetrics(boolean lossless,
                                             double averageError,
                                             int maxVisibleColorDelta,
                                             int maxAlphaDelta) {

        @NotNull
        public static CandidateQualityMetrics losslessMetrics() {
            return new CandidateQualityMetrics(true, 0D, 0, 0);
        }
    }

    protected record QualityBudgetState(int consecutiveLossyFrames,
                                        double cumulativeAverageError,
                                        int cumulativeVisibleColorDelta,
                                        int cumulativeAlphaDelta) {

        @NotNull
        public static QualityBudgetState lossless() {
            return new QualityBudgetState(0, 0D, 0, 0);
        }

        @NotNull
        public QualityBudgetState advance(@NotNull FrameCandidate candidate) {
            CandidateQualityMetrics metrics = candidate.qualityMetrics();
            if (metrics.lossless()) {
                return lossless();
            }
            if (candidate.descriptor().isKeyframe()) {
                return new QualityBudgetState(1, metrics.averageError(), metrics.maxVisibleColorDelta(), metrics.maxAlphaDelta());
            }
            return new QualityBudgetState(
                    this.consecutiveLossyFrames + 1,
                    this.cumulativeAverageError + metrics.averageError(),
                    this.cumulativeVisibleColorDelta + metrics.maxVisibleColorDelta(),
                    this.cumulativeAlphaDelta + metrics.maxAlphaDelta()
            );
        }
    }

    protected enum CandidateKind {
        SAME(0),
        FULL(1),
        DELTA_BIN_INTRA(2),
        DELTA_RESIDUAL(3),
        DELTA_SPARSE(4),
        COPY_BIN_INTRA(5),
        COPY_RESIDUAL(6),
        COPY_SPARSE(7),
        MULTI_COPY_BIN_INTRA(8),
        MULTI_COPY_RESIDUAL(9),
        MULTI_COPY_SPARSE(10),
        BLOCK_INTER(11);

        private final int stabilityRank;

        CandidateKind(int stabilityRank) {
            this.stabilityRank = stabilityRank;
        }

        public int stabilityRank() {
            return this.stabilityRank;
        }
    }

    protected record StrategyEstimate(@NotNull CandidateKind kind, long estimatedBytes, int stabilityRank) {
    }

    protected record CopyPlan(@Nullable AfmaPixelFrame referenceFrameAfterCopy,
                              @Nullable AfmaCopyRect copyRect,
                              @Nullable AfmaMultiCopy multiCopy,
                              @Nullable AfmaRect patchBounds,
                              @NotNull List<StrategyEstimate> rankedStrategies) {
    }

    protected record CopyEvaluation(@Nullable AfmaRectCopyDetector.Detection singleDetection,
                                    @Nullable AfmaRectCopyDetector.MultiDetection multiDetection) {
        protected static final CopyEvaluation EMPTY = new CopyEvaluation(null, null);
    }

    protected record MotionVector(int dx, int dy) {
    }

    protected record ApproxBlockInterTile(@NotNull AfmaBlockInterPayloadHelper.TileMode mode,
                                          int dstX, int dstY, int width, int height,
                                          int dx, int dy,
                                          int channels,
                                          int changedPixelCount,
                                          int changedPixelBudget,
                                          long estimatedBytes) {
    }

    protected record MotionTileStats(int changedPixelCount, boolean includeAlpha) {
    }

    protected record ResidualRegionStats(double averageMagnitude, double averageChangedMagnitude) {
        protected static final ResidualRegionStats EMPTY = new ResidualRegionStats(0D, 0D);
    }

    protected record ChangedRegionData(@NotNull int[] changedIndices,
                                       @NotNull int[] predictedColors,
                                       @NotNull int[] currentColors,
                                       int changedCount) {
    }

    protected record ResidualPayloadData(@NotNull AfmaStoredPayload.PayloadSummary payloadSummary,
                                         @NotNull AfmaStoredPayload.Writer payloadWriter,
                                         @NotNull AfmaResidualPayload metadata,
                                         int complexityScore) {
    }

    protected record SparseResidualPayloadData(@NotNull byte[] layoutPayload,
                                               @NotNull AfmaStoredPayload.PayloadSummary residualPayloadSummary,
                                               @NotNull AfmaStoredPayload.Writer residualPayloadWriter,
                                               int changedPixelCount,
                                               @NotNull AfmaSparseLayoutCodec layoutCodec,
                                               int channels,
                                               @NotNull AfmaResidualCodec residualCodec,
                                               @NotNull AfmaAlphaResidualMode alphaMode,
                                               int alphaChangedPixelCount,
                                               int complexityScore) {

        public int residualPayloadLength() {
            return this.residualPayloadSummary.length();
        }

        @NotNull
        public byte[] materializeResidualPayload() {
            try {
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream(Math.max(32, this.residualPayloadSummary.length()));
                this.residualPayloadWriter.write(byteStream);
                byte[] payloadBytes = byteStream.toByteArray();
                AfmaStoredPayload.PayloadSummary materializedSummary = AfmaStoredPayload.summarize(payloadBytes);
                if ((materializedSummary.length() != this.residualPayloadSummary.length())
                        || (materializedSummary.estimatedArchiveBytes() != this.residualPayloadSummary.estimatedArchiveBytes())
                        || !materializedSummary.fingerprint().equals(this.residualPayloadSummary.fingerprint())) {
                    throw new IOException("AFMA v2 sparse residual payload changed during materialization");
                }
                return payloadBytes;
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to materialize AFMA v2 sparse residual payload", ex);
            }
        }

        @NotNull
        public AfmaSparsePayload toMetadata(@Nullable String residualPayloadPath) {
            return new AfmaSparsePayload(
                    residualPayloadPath,
                    this.changedPixelCount,
                    this.channels,
                    this.layoutCodec,
                    this.residualCodec,
                    this.alphaMode,
                    this.alphaChangedPixelCount
            );
        }
    }

    protected record SparseLayoutCandidate(@NotNull AfmaSparseLayoutCodec layoutCodec,
                                           @NotNull byte[] layoutPayload,
                                           int complexityScore,
                                           long estimatedArchiveBytes) {

        protected SparseLayoutCandidate(@NotNull AfmaSparseLayoutCodec layoutCodec,
                                        @NotNull byte[] layoutPayload,
                                        int complexityScore) {
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
                throw new IllegalArgumentException("AFMA v2 deferred payload summary length does not match payload bytes");
            }
            return new DeferredPayload(payloadSummary, null, payloadBytes, null);
        }

        @NotNull
        public static DeferredPayload fromWriter(@NotNull AfmaStoredPayload.PayloadSummary payloadSummary,
                                                 @NotNull AfmaStoredPayload.Writer payloadWriter) {
            return new DeferredPayload(payloadSummary, null, null, payloadWriter);
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

        @NotNull
        public AfmaStoredPayload.PayloadSummary payloadSummary() {
            this.ensureOpen();
            return this.payloadSummary;
        }

        public long estimateChunkAppendBytes(@NotNull byte[] previousTail) {
            this.ensureOpen();
            Objects.requireNonNull(previousTail);
            if (this.payloadSummary.length() <= 0) {
                return 0L;
            }
            if (previousTail.length == 0) {
                return this.payloadSummary.estimatedArchiveBytes();
            }
            if (this.payloadBytes != null) {
                return AfmaChunkedPayloadHelper.estimateChunkCompressionDelta(previousTail, this.payloadBytes);
            }
            if (this.payloadWriter != null) {
                return AfmaChunkedPayloadHelper.estimateChunkCompressionDelta(previousTail, this.payloadSummary, this.payloadWriter);
            }
            if (this.materializedPayload != null) {
                return AfmaChunkedPayloadHelper.estimateChunkCompressionDelta(previousTail, this.materializedPayload);
            }
            throw new IllegalStateException("AFMA v2 deferred payload no longer has bytes available for archive estimation");
        }

        @NotNull
        public AfmaStoredPayload materialize() throws IOException {
            this.ensureOpen();
            if (this.materializedPayload != null) {
                return this.materializedPayload;
            }

            AfmaStoredPayload payload;
            if (this.payloadBytes != null) {
                payload = AfmaStoredPayload.fromBytes(this.payloadSummary, this.payloadBytes);
            } else if (this.payloadWriter != null) {
                payload = AfmaStoredPayload.write(this.payloadWriter);
            } else {
                throw new IOException("AFMA v2 deferred payload no longer has a materialization source");
            }

            AfmaStoredPayload.PayloadSummary materializedSummary = payload.summarize();
            if ((materializedSummary.length() != this.payloadSummary.length())
                    || (materializedSummary.estimatedArchiveBytes() != this.payloadSummary.estimatedArchiveBytes())
                    || !materializedSummary.fingerprint().equals(this.payloadSummary.fingerprint())) {
                payload.close();
                throw new IOException("AFMA v2 deferred payload metrics changed during materialization");
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
                throw new IllegalStateException("AFMA v2 deferred payload has already been closed");
            }
        }
    }

    protected record FrameCandidate(@NotNull CandidateKind kind,
                                    @NotNull AfmaFrameDescriptor descriptor,
                                    @Nullable String primaryPayloadPath,
                                    @Nullable DeferredPayload primaryPayload,
                                    @Nullable String patchPayloadPath,
                                    @Nullable DeferredPayload patchPayload,
                                    @NotNull AfmaPixelFrame outputFrame,
                                    int decodeComplexity,
                                    @NotNull CandidateQualityMetrics qualityMetrics) {

        public long totalArchiveBytes(@NotNull PayloadInterner payloadInterner) {
            return payloadInterner.estimateCandidateArchiveBytes(this);
        }

        @NotNull
        public FrameCandidate withQualityMetrics(@NotNull CandidateQualityMetrics updatedQualityMetrics) {
            return new FrameCandidate(
                    this.kind,
                    this.descriptor,
                    this.primaryPayloadPath,
                    this.primaryPayload,
                    this.patchPayloadPath,
                    this.patchPayload,
                    this.outputFrame,
                    this.decodeComplexity,
                    updatedQualityMetrics
            );
        }
    }

    protected static final class ArchivePreviewState {

        protected long closedChunkBytes = 0L;
        protected int closedChunkCount = 0;
        protected long payloadLocatorBytes = 0L;
        protected int payloadCount = 0;
        protected long closedChunkLengthBytes = 0L;
        protected int currentChunkLength = 0;
        protected long currentChunkCompressedBytes = 0L;
        @NotNull
        protected byte[] currentChunkTail = AfmaEncodePlanner.EMPTY_BYTES;

        protected ArchivePreviewState() {
        }

        protected ArchivePreviewState(@NotNull ArchivePreviewState other) {
            this.closedChunkBytes = other.closedChunkBytes;
            this.closedChunkCount = other.closedChunkCount;
            this.payloadLocatorBytes = other.payloadLocatorBytes;
            this.payloadCount = other.payloadCount;
            this.closedChunkLengthBytes = other.closedChunkLengthBytes;
            this.currentChunkLength = other.currentChunkLength;
            this.currentChunkCompressedBytes = other.currentChunkCompressedBytes;
            this.currentChunkTail = other.currentChunkTail.clone();
        }

        public long previewArchiveBytes(@Nullable DeferredPayload primaryPayload,
                                        @Nullable DeferredPayload patchPayload,
                                        @NotNull Set<String> knownFingerprints) {
            long baseBytes = this.estimatedTotalBytes();
            ArchivePreviewState previewState = new ArchivePreviewState(this);
            HashSet<String> previewFingerprints = new HashSet<>(knownFingerprints);
            previewState.previewCommitPayload(primaryPayload, previewFingerprints);
            previewState.previewCommitPayload(patchPayload, previewFingerprints);
            return Math.max(0L, previewState.estimatedTotalBytes() - baseBytes);
        }

        public void commitPayload(@NotNull DeferredPayload payload) {
            this.appendPayload(payload);
        }

        protected void previewCommitPayload(@Nullable DeferredPayload payload, @NotNull Set<String> previewFingerprints) {
            if (payload == null) {
                return;
            }
            if (!previewFingerprints.add(payload.fingerprint())) {
                return;
            }
            this.appendPayload(payload);
        }

        protected void appendPayload(@NotNull DeferredPayload payload) {
            AfmaStoredPayload.PayloadSummary payloadSummary = payload.payloadSummary();
            if (payloadSummary.length() <= 0) {
                return;
            }

            if ((this.currentChunkLength > 0) && this.shouldStartNewChunk(payloadSummary.length())) {
                this.finalizeCurrentChunk();
            }

            int chunkId = this.closedChunkCount;
            this.payloadLocatorBytes += estimateVarIntBytes(chunkId)
                    + estimateVarIntBytes(this.currentChunkLength)
                    + estimateVarIntBytes(payloadSummary.length());
            this.payloadCount++;

            this.currentChunkCompressedBytes += payload.estimateChunkAppendBytes(this.currentChunkTail);
            this.currentChunkLength += payloadSummary.length();
            this.currentChunkTail = AfmaChunkedPayloadHelper.appendDeflateTail(this.currentChunkTail, payloadSummary);
        }

        protected long estimatedTotalBytes() {
            long totalChunkBytes = this.closedChunkBytes;
            long totalChunkLengthBytes = this.closedChunkLengthBytes;
            int totalChunkCount = this.closedChunkCount;
            if (this.currentChunkLength > 0) {
                totalChunkBytes += Math.min(this.currentChunkCompressedBytes, (long) this.currentChunkLength);
                totalChunkLengthBytes += estimateVarIntBytes(this.currentChunkLength);
                totalChunkCount++;
            }
            return 5L
                    + estimateVarIntBytes(this.payloadCount)
                    + estimateVarIntBytes(totalChunkCount)
                    + this.payloadLocatorBytes
                    + totalChunkLengthBytes
                    + totalChunkBytes
                    + ((long) totalChunkCount * (long) AfmaContainerV2.CHUNK_DESCRIPTOR_BYTES);
        }

        protected void finalizeCurrentChunk() {
            if (this.currentChunkLength <= 0) {
                return;
            }
            this.closedChunkBytes += Math.min(this.currentChunkCompressedBytes, (long) this.currentChunkLength);
            this.closedChunkLengthBytes += estimateVarIntBytes(this.currentChunkLength);
            this.closedChunkCount++;
            this.currentChunkLength = 0;
            this.currentChunkCompressedBytes = 0L;
            this.currentChunkTail = AfmaEncodePlanner.EMPTY_BYTES;
        }

        protected boolean shouldStartNewChunk(int payloadLength) {
            return AfmaPayloadArchiveLayout.shouldStartNewChunk(this.currentChunkLength, payloadLength);
        }
    }

    protected static final class PayloadInterner {

        @NotNull
        protected final LinkedHashMap<String, AfmaStoredPayload> payloads = new LinkedHashMap<>();
        @NotNull
        protected final Map<String, String> payloadPathsByFingerprint = new LinkedHashMap<>();
        @NotNull
        protected final Map<FrameCandidate, Long> candidateArchiveBytesCache = new IdentityHashMap<>();
        @NotNull
        protected final ArchivePreviewState archivePreviewState = new ArchivePreviewState();

        public long estimateCandidateArchiveBytes(@NotNull FrameCandidate candidate) {
            return this.candidateArchiveBytesCache.computeIfAbsent(candidate, this::computeCandidateArchiveBytes);
        }

        protected long computeCandidateArchiveBytes(@NotNull FrameCandidate candidate) {
            return this.archivePreviewState.previewArchiveBytes(
                    candidate.primaryPayload(),
                    candidate.patchPayload(),
                    this.payloadPathsByFingerprint.keySet()
            ) + this.estimateDescriptorBytes(candidate.descriptor());
        }

        public int estimateDescriptorBytes(@NotNull AfmaFrameDescriptor descriptor) {
            return new AfmaV2DescriptorSizer().estimate(descriptor);
        }

        public long estimatePackedCandidateArchiveBytes(@NotNull FrameCandidate candidate,
                                                        boolean introSequence,
                                                        @NotNull List<AfmaFrameDescriptor> currentSequenceFrames,
                                                        @NotNull List<AfmaFrameDescriptor> companionSequenceFrames,
                                                        int loopCount) throws IOException {
            CandidateArchivePreview preview = this.previewCandidate(candidate);
            ArrayList<AfmaFrameDescriptor> introFrames;
            ArrayList<AfmaFrameDescriptor> mainFrames;
            if (introSequence) {
                introFrames = new ArrayList<>(currentSequenceFrames.size() + 1);
                introFrames.addAll(currentSequenceFrames);
                introFrames.add(preview.descriptor());
                mainFrames = new ArrayList<>(companionSequenceFrames);
            } else {
                introFrames = new ArrayList<>(companionSequenceFrames);
                mainFrames = new ArrayList<>(currentSequenceFrames.size() + 1);
                mainFrames.addAll(currentSequenceFrames);
                mainFrames.add(preview.descriptor());
            }

            AfmaFrameIndex frameIndex = new AfmaFrameIndex(mainFrames, introFrames);
            AfmaChunkedPayloadHelper.PackedPayloadArchive packedArchive = AfmaChunkedPayloadHelper.simulateArchiveLayout(
                    preview.payloads(),
                    AfmaChunkedPayloadHelper.buildPackingHints(frameIndex, loopCount)
            );
            return packedArchive.packingMetrics().predictedArchiveBytes() + this.estimateDescriptorBytes(preview.descriptor());
        }

        @NotNull
        public AfmaFrameDescriptor intern(@NotNull FrameCandidate candidate) throws IOException {
            AfmaFrameDescriptor descriptor = candidate.descriptor();
            String resolvedPrimaryPath = this.internPayload(candidate.primaryPayloadPath(), candidate.primaryPayload());
            if (!Objects.equals(resolvedPrimaryPath, candidate.primaryPayloadPath()) && (resolvedPrimaryPath != null)) {
                descriptor = descriptor.withPrimaryPath(resolvedPrimaryPath);
            }
            String resolvedPatchPath = this.internPayload(candidate.patchPayloadPath(), candidate.patchPayload());
            if (!Objects.equals(resolvedPatchPath, candidate.patchPayloadPath()) && (resolvedPatchPath != null)) {
                descriptor = descriptor.withPatchPath(resolvedPatchPath);
            }
            return descriptor;
        }

        @Nullable
        protected String internPayload(@Nullable String path, @Nullable DeferredPayload payload) throws IOException {
            if ((path == null) || (payload == null)) {
                return path;
            }

            String fingerprint = payload.fingerprint();
            String existingPath = this.payloadPathsByFingerprint.get(fingerprint);
            if (existingPath != null) {
                payload.close();
                return existingPath;
            }

            this.payloads.put(path, payload.materialize());
            this.payloadPathsByFingerprint.put(fingerprint, path);
            this.archivePreviewState.commitPayload(payload);
            this.candidateArchiveBytesCache.clear();
            return path;
        }

        @NotNull
        protected CandidateArchivePreview previewCandidate(@NotNull FrameCandidate candidate) throws IOException {
            LinkedHashMap<String, AfmaStoredPayload> previewPayloads = new LinkedHashMap<>(this.payloads);
            LinkedHashMap<String, String> previewPathsByFingerprint = new LinkedHashMap<>(this.payloadPathsByFingerprint);
            AfmaFrameDescriptor descriptor = candidate.descriptor();

            String resolvedPrimaryPath = this.previewPayload(candidate.primaryPayloadPath(), candidate.primaryPayload(), previewPayloads, previewPathsByFingerprint);
            if (!Objects.equals(resolvedPrimaryPath, candidate.primaryPayloadPath()) && (resolvedPrimaryPath != null)) {
                descriptor = descriptor.withPrimaryPath(resolvedPrimaryPath);
            }

            String resolvedPatchPath = this.previewPayload(candidate.patchPayloadPath(), candidate.patchPayload(), previewPayloads, previewPathsByFingerprint);
            if (!Objects.equals(resolvedPatchPath, candidate.patchPayloadPath()) && (resolvedPatchPath != null)) {
                descriptor = descriptor.withPatchPath(resolvedPatchPath);
            }

            return new CandidateArchivePreview(previewPayloads, descriptor);
        }

        @Nullable
        protected String previewPayload(@Nullable String path,
                                        @Nullable DeferredPayload payload,
                                        @NotNull LinkedHashMap<String, AfmaStoredPayload> previewPayloads,
                                        @NotNull Map<String, String> previewPathsByFingerprint) throws IOException {
            if ((path == null) || (payload == null)) {
                return path;
            }

            String fingerprint = payload.fingerprint();
            String existingPath = previewPathsByFingerprint.get(fingerprint);
            if (existingPath != null) {
                return existingPath;
            }

            previewPayloads.put(path, payload.materialize());
            previewPathsByFingerprint.put(fingerprint, path);
            return path;
        }

        @NotNull
        public LinkedHashMap<String, AfmaStoredPayload> payloads() {
            return new LinkedHashMap<>(this.payloads);
        }
    }

    protected record CandidateArchivePreview(@NotNull LinkedHashMap<String, AfmaStoredPayload> payloads,
                                             @NotNull AfmaFrameDescriptor descriptor) {
    }

    protected static final class AfmaV2DescriptorSizer {

        protected int estimate(@NotNull AfmaFrameDescriptor descriptor) {
            AfmaFrameOperationType type = descriptor.getType();
            if (type == null) {
                return 1;
            }
            int bytes = 1;
            bytes += switch (type) {
                case FULL -> 2;
                case DELTA_RECT -> 2 + estimateVarIntBytes(descriptor.getX()) + estimateVarIntBytes(descriptor.getY())
                        + estimateVarIntBytes(descriptor.getWidth()) + estimateVarIntBytes(descriptor.getHeight());
                case RESIDUAL_DELTA_RECT -> 2 + estimateVarIntBytes(descriptor.getX()) + estimateVarIntBytes(descriptor.getY())
                        + estimateVarIntBytes(descriptor.getWidth()) + estimateVarIntBytes(descriptor.getHeight())
                        + estimateVarIntBytes(Objects.requireNonNull(descriptor.getResidual()).getChannels()) + 2
                        + estimateVarIntBytes(Objects.requireNonNull(descriptor.getResidual()).getAlphaChangedPixelCount());
                case SPARSE_DELTA_RECT -> 4 + estimateVarIntBytes(descriptor.getX()) + estimateVarIntBytes(descriptor.getY())
                        + estimateVarIntBytes(descriptor.getWidth()) + estimateVarIntBytes(descriptor.getHeight())
                        + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getChangedPixelCount())
                        + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getChannels()) + 3
                        + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getAlphaChangedPixelCount());
                case SAME -> 0;
                case COPY_RECT_PATCH -> {
                    int copyBytes = estimateCopyRectBytes(Objects.requireNonNull(descriptor.getCopy()));
                    AfmaPatchRegion patch = descriptor.getPatch();
                    if (patch == null) {
                        yield copyBytes + 1;
                    }
                    yield copyBytes + 1 + 2 + estimateVarIntBytes(patch.getX()) + estimateVarIntBytes(patch.getY())
                            + estimateVarIntBytes(patch.getWidth()) + estimateVarIntBytes(patch.getHeight());
                }
                case MULTI_COPY_PATCH -> {
                    int copyBytes = estimateMultiCopyBytes(Objects.requireNonNull(descriptor.getMultiCopy()));
                    AfmaPatchRegion patch = descriptor.getPatch();
                    if (patch == null) {
                        yield copyBytes + 1;
                    }
                    yield copyBytes + 1 + 2 + estimateVarIntBytes(patch.getX()) + estimateVarIntBytes(patch.getY())
                            + estimateVarIntBytes(patch.getWidth()) + estimateVarIntBytes(patch.getHeight());
                }
                case COPY_RECT_RESIDUAL_PATCH -> estimateCopyRectBytes(Objects.requireNonNull(descriptor.getCopy()))
                        + 2 + estimateVarIntBytes(descriptor.getX()) + estimateVarIntBytes(descriptor.getY())
                        + estimateVarIntBytes(descriptor.getWidth()) + estimateVarIntBytes(descriptor.getHeight())
                        + estimateVarIntBytes(Objects.requireNonNull(descriptor.getResidual()).getChannels()) + 2
                        + estimateVarIntBytes(Objects.requireNonNull(descriptor.getResidual()).getAlphaChangedPixelCount());
                case MULTI_COPY_RESIDUAL_PATCH -> estimateMultiCopyBytes(Objects.requireNonNull(descriptor.getMultiCopy()))
                        + 2 + estimateVarIntBytes(descriptor.getX()) + estimateVarIntBytes(descriptor.getY())
                        + estimateVarIntBytes(descriptor.getWidth()) + estimateVarIntBytes(descriptor.getHeight())
                        + estimateVarIntBytes(Objects.requireNonNull(descriptor.getResidual()).getChannels()) + 2
                        + estimateVarIntBytes(Objects.requireNonNull(descriptor.getResidual()).getAlphaChangedPixelCount());
                case COPY_RECT_SPARSE_PATCH -> estimateCopyRectBytes(Objects.requireNonNull(descriptor.getCopy()))
                        + 4 + estimateVarIntBytes(descriptor.getX()) + estimateVarIntBytes(descriptor.getY())
                        + estimateVarIntBytes(descriptor.getWidth()) + estimateVarIntBytes(descriptor.getHeight())
                        + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getChangedPixelCount())
                        + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getChannels()) + 3
                        + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getAlphaChangedPixelCount());
                case MULTI_COPY_SPARSE_PATCH -> estimateMultiCopyBytes(Objects.requireNonNull(descriptor.getMultiCopy()))
                        + 4 + estimateVarIntBytes(descriptor.getX()) + estimateVarIntBytes(descriptor.getY())
                        + estimateVarIntBytes(descriptor.getWidth()) + estimateVarIntBytes(descriptor.getHeight())
                        + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getChangedPixelCount())
                        + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getChannels()) + 3
                        + estimateVarIntBytes(Objects.requireNonNull(descriptor.getSparse()).getAlphaChangedPixelCount());
                case BLOCK_INTER -> 2 + estimateVarIntBytes(descriptor.getX()) + estimateVarIntBytes(descriptor.getY())
                        + estimateVarIntBytes(descriptor.getWidth()) + estimateVarIntBytes(descriptor.getHeight())
                        + estimateVarIntBytes(Objects.requireNonNull(descriptor.getBlockInter()).getTileSize());
            };
            return bytes;
        }

        protected static int estimateCopyRectBytes(@NotNull AfmaCopyRect copyRect) {
            return estimateVarIntBytes(copyRect.getSrcX()) + estimateVarIntBytes(copyRect.getSrcY())
                    + estimateVarIntBytes(copyRect.getDstX()) + estimateVarIntBytes(copyRect.getDstY())
                    + estimateVarIntBytes(copyRect.getWidth()) + estimateVarIntBytes(copyRect.getHeight());
        }

        protected static int estimateMultiCopyBytes(@NotNull AfmaMultiCopy multiCopy) {
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
    }

    protected static final class ResidualPlannerWorkspace {

        private int[] changedIndices = new int[0];
        private int[] predictedColors = new int[0];
        private int[] currentColors = new int[0];
        private final AfmaResidualPayloadHelper.ResidualEncodeWorkspace residualEncodeWorkspace = new AfmaResidualPayloadHelper.ResidualEncodeWorkspace();

        @NotNull
        protected int[] changedIndices(int minLength) {
            if (this.changedIndices.length < minLength) {
                this.changedIndices = new int[minLength];
            }
            return this.changedIndices;
        }

        @NotNull
        protected int[] predictedColors(int minLength) {
            if (this.predictedColors.length < minLength) {
                this.predictedColors = new int[minLength];
            }
            return this.predictedColors;
        }

        @NotNull
        protected int[] currentColors(int minLength) {
            if (this.currentColors.length < minLength) {
                this.currentColors = new int[minLength];
            }
            return this.currentColors;
        }

        @NotNull
        protected AfmaResidualPayloadHelper.ResidualEncodeWorkspace residualEncodeWorkspace() {
            return this.residualEncodeWorkspace;
        }
    }

    protected final class AsyncFrameLoader implements AutoCloseable {

        @NotNull
        protected final AfmaSourceSequence sequence;
        @NotNull
        protected final Dimension dimension;
        @Nullable
        protected final ExecutorService executor;
        @NotNull
        protected final AfmaFastPixelBufferPool pixelBufferPool;
        @Nullable
        protected final BooleanSupplier cancellationRequested;
        @Nullable
        protected CompletableFuture<AfmaPixelFrame> nextFrameFuture;
        protected int nextFutureIndex = -1;
        @Nullable
        protected AfmaPixelFrame currentFrame;
        protected int currentIndex = -1;
        protected volatile boolean closed = false;

        protected AsyncFrameLoader(@NotNull AfmaSourceSequence sequence,
                                   @NotNull Dimension dimension,
                                   @Nullable AfmaPixelFrame firstFrameOverride,
                                   @NotNull AfmaFastPixelBufferPool pixelBufferPool,
                                   @Nullable ExecutorService executor,
                                   @Nullable BooleanSupplier cancellationRequested) {
            this.sequence = Objects.requireNonNull(sequence);
            this.dimension = Objects.requireNonNull(dimension);
            this.pixelBufferPool = Objects.requireNonNull(pixelBufferPool);
            this.executor = executor;
            this.cancellationRequested = cancellationRequested;
            this.currentFrame = firstFrameOverride;
            this.currentIndex = (firstFrameOverride != null) ? 0 : -1;
            if (firstFrameOverride != null) {
                this.validateFrameSize(firstFrameOverride, 0);
            }
            this.schedule(1);
        }

        @NotNull
        protected AfmaPixelFrame takeFrame(int frameIndex) throws IOException {
            checkCancelled(this.cancellationRequested);
            if ((this.currentFrame != null) && (this.currentIndex == frameIndex)) {
                AfmaPixelFrame frame = this.currentFrame;
                this.currentFrame = null;
                this.currentIndex = -1;
                this.schedule(frameIndex + 1);
                return frame;
            }

            if ((this.nextFrameFuture != null) && (this.nextFutureIndex == frameIndex)) {
                AfmaPixelFrame frame = this.joinFrameFuture(this.nextFrameFuture);
                this.nextFrameFuture = null;
                this.nextFutureIndex = -1;
                this.validateFrameSize(frame, frameIndex);
                this.schedule(frameIndex + 1);
                return frame;
            }

            AfmaPixelFrame frame = this.loadFrame(frameIndex);
            this.schedule(frameIndex + 1);
            return frame;
        }

        protected void schedule(int frameIndex) {
            if ((frameIndex < 0) || (frameIndex >= this.sequence.size())) {
                return;
            }
            if ((this.nextFrameFuture != null) || (this.executor == null)) {
                return;
            }

            File frameFile = Objects.requireNonNull(this.sequence.getFrame(frameIndex));
            this.nextFutureIndex = frameIndex;
            this.nextFrameFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    checkCancelled(this.cancellationRequested);
                    AfmaPixelFrame loadedFrame = AfmaV2PlannerCore.this.frameNormalizer.loadFrame(
                            frameFile,
                            this.dimension.width(),
                            this.dimension.height(),
                            this.pixelBufferPool
                    );
                    try {
                        this.validateFrameSize(loadedFrame, frameIndex);
                        if (this.closed) {
                            throw new CancellationException("AFMA v2 frame prefetch was closed");
                        }
                        return loadedFrame;
                    } catch (RuntimeException | Error ex) {
                        CloseableUtils.closeQuietly(loadedFrame);
                        throw ex;
                    }
                } catch (IOException ex) {
                    throw new CompletionException(ex);
                }
            }, this.executor);
        }

        @NotNull
        protected AfmaPixelFrame loadFrame(int frameIndex) throws IOException {
            File frameFile = Objects.requireNonNull(this.sequence.getFrame(frameIndex));
            AfmaPixelFrame frame = AfmaV2PlannerCore.this.frameNormalizer.loadFrame(
                    frameFile,
                    this.dimension.width(),
                    this.dimension.height(),
                    this.pixelBufferPool
            );
            try {
                this.validateFrameSize(frame, frameIndex);
                if (this.closed) {
                    throw new CancellationException("AFMA v2 frame loader was closed");
                }
                return frame;
            } catch (RuntimeException | Error ex) {
                CloseableUtils.closeQuietly(frame);
                throw ex;
            }
        }

        @NotNull
        protected AfmaPixelFrame joinFrameFuture(@NotNull CompletableFuture<AfmaPixelFrame> frameFuture) throws IOException {
            try {
                return frameFuture.join();
            } catch (CompletionException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof IOException ioEx) {
                    throw ioEx;
                }
                if (cause instanceof RuntimeException runtimeEx) {
                    throw runtimeEx;
                }
                if (cause instanceof Error error) {
                    throw error;
                }
                throw new IOException("Failed to load AFMA v2 source frame", cause);
            }
        }

        protected void validateFrameSize(@NotNull AfmaPixelFrame frame, int frameIndex) {
            if ((frame.getWidth() != this.dimension.width()) || (frame.getHeight() != this.dimension.height())) {
                throw new IllegalArgumentException("AFMA v2 frame " + frameIndex + " has mismatched dimensions");
            }
        }

        @Override
        public void close() {
            this.closed = true;
            if (this.nextFrameFuture != null) {
                CompletableFuture<AfmaPixelFrame> future = this.nextFrameFuture;
                future.cancel(true);
                if (future.isDone() && !future.isCompletedExceptionally() && !future.isCancelled()) {
                    try {
                        CloseableUtils.closeQuietly(future.getNow(null));
                    } catch (CancellationException | CompletionException ignored) {
                    }
                }
                this.nextFrameFuture = null;
                this.nextFutureIndex = -1;
            }
            CloseableUtils.closeQuietly(this.currentFrame);
            this.currentFrame = null;
            this.currentIndex = -1;
        }
    }

}
