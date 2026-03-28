package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaBlockInter;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaBlockInterPayloadHelper;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaCopyRect;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameDescriptor;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameIndex;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameOperationType;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaMetadata;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaPatchRegion;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaRect;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaResidualPayload;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaResidualPayloadHelper;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaSparsePayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.zip.Deflater;

public class AfmaEncodePlanner {

    protected static final int MIN_FFMPEG_PNG_REWRITE_BYTES = 96 * 1024;
    protected static final int MIN_SPARSE_DELTA_CHANGED_PIXELS = 4096;
    protected static final double MAX_SPARSE_DELTA_CHANGED_DENSITY = 0.30D;
    protected static final int BLOCK_INTER_TILE_SIZE = 16;

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
        Dimension dimension = this.readDimension(dimensionSource);
        int totalFrameCount = Math.max(1, mainSequence.size() + intro.size());
        if (!mainSequence.isEmpty()) {
            this.validateSequenceDimensions(mainSequence, dimension, "main", cancellationRequested, progressListener, 0, totalFrameCount);
        }
        if (!intro.isEmpty()) {
            this.validateSequenceDimensions(intro, dimension, "intro", cancellationRequested, progressListener, mainSequence.size(), totalFrameCount);
        }

        AfmaRectCopyDetector copyDetector = new AfmaRectCopyDetector(options.getMaxCopySearchDistance(), options.getMaxCandidateAxisOffsets());
        AfmaFfmpegBridge ffmpegBridge = new AfmaFfmpegBridge();
        LinkedHashMap<String, byte[]> payloads = new LinkedHashMap<>();
        Map<String, String> payloadPathsByFingerprint = new LinkedHashMap<>();
        List<AfmaFrameDescriptor> plannedIntroFrames = this.planSequence(intro, true, dimension, options, copyDetector, ffmpegBridge, payloads, payloadPathsByFingerprint, cancellationRequested, progressListener, 0, totalFrameCount);
        List<AfmaFrameDescriptor> plannedMainFrames = this.planSequence(mainSequence, false, dimension, options, copyDetector, ffmpegBridge, payloads, payloadPathsByFingerprint, cancellationRequested, progressListener, intro.size(), totalFrameCount);

        AfmaMetadata metadata = AfmaMetadata.create(
                dimension.width(),
                dimension.height(),
                options.getLoopCount(),
                options.getFrameTimeMs(),
                options.getIntroFrameTimeMs(),
                options.getCustomFrameTimes(),
                options.getCustomIntroFrameTimes(),
                options.getKeyframeInterval(),
                options.isRectCopyEnabled(),
                options.isDuplicateFrameElision()
        );

        return new AfmaEncodePlan(metadata, new AfmaFrameIndex(plannedMainFrames, plannedIntroFrames), payloads);
    }

    @NotNull
    protected List<AfmaFrameDescriptor> planSequence(@NotNull AfmaSourceSequence sequence, boolean introSequence, @NotNull Dimension dimension,
                                                     @NotNull AfmaEncodeOptions options, @NotNull AfmaRectCopyDetector copyDetector,
                                                     @NotNull AfmaFfmpegBridge ffmpegBridge,
                                                     @NotNull LinkedHashMap<String, byte[]> payloads,
                                                     @NotNull Map<String, String> payloadPathsByFingerprint,
                                                     @Nullable BooleanSupplier cancellationRequested, @Nullable ProgressListener progressListener,
                                                     int startOffset, int totalFrameCount) throws IOException {
        List<AfmaFrameDescriptor> plannedFrames = new ArrayList<>();
        if (sequence.isEmpty()) {
            return plannedFrames;
        }

        AfmaPixelFrame previousFrame = null;
        int framesSinceKeyframe = 0;
        try {
            for (int frameIndex = 0; frameIndex < sequence.size(); frameIndex++) {
                checkCancelled(cancellationRequested);
                reportProgress(progressListener,
                        "Planning " + (introSequence ? "intro" : "main") + " frame " + (frameIndex + 1) + "/" + sequence.size(),
                        0.25D + (0.65D * ((double) (startOffset + frameIndex + 1) / Math.max(1, totalFrameCount))));
                File frameFile = Objects.requireNonNull(sequence.getFrame(frameIndex));
                AfmaPixelFrame currentFrame = this.frameNormalizer.loadFrame(frameFile);
                try {
                    if ((currentFrame.getWidth() != dimension.width()) || (currentFrame.getHeight() != dimension.height())) {
                        throw new IOException("AFMA source frame dimensions do not match the expected canvas size: " + frameFile.getAbsolutePath());
                    }

                    if ((previousFrame != null) && options.isNearLosslessEnabled() && ((framesSinceKeyframe + 1) < options.getKeyframeInterval())) {
                        currentFrame = this.applyNearLosslessTemporalMerge(previousFrame, currentFrame, options.getNearLosslessMaxChannelDelta());
                    }

                    PlannedCandidate selectedCandidate;
                    boolean candidateAlreadyScoredWithOptimizedPayloads = false;
                    if (previousFrame == null) {
                        selectedCandidate = this.createFullCandidate(currentFrame, introSequence, frameIndex);
                    } else if (options.isDuplicateFrameElision() && AfmaPixelFrameHelper.isIdentical(previousFrame, currentFrame)) {
                        selectedCandidate = PlannedCandidate.same();
                    } else if ((framesSinceKeyframe + 1) >= options.getKeyframeInterval()) {
                        selectedCandidate = this.createFullCandidate(currentFrame, introSequence, frameIndex);
                    } else {
                        selectedCandidate = this.chooseBestCandidate(previousFrame, currentFrame, introSequence, frameIndex, options, copyDetector, ffmpegBridge, payloadPathsByFingerprint);
                        candidateAlreadyScoredWithOptimizedPayloads = true;
                    }

                    if (!candidateAlreadyScoredWithOptimizedPayloads) {
                        selectedCandidate = this.optimizeSelectedCandidatePayloads(selectedCandidate, ffmpegBridge);
                    }
                    PlannedCandidate finalizedCandidate = selectedCandidate.internPayloads(payloads, payloadPathsByFingerprint);
                    plannedFrames.add(finalizedCandidate.descriptor());
                    framesSinceKeyframe = finalizedCandidate.descriptor().isKeyframe() ? 0 : (framesSinceKeyframe + 1);
                } finally {
                    CloseableUtils.closeQuietly(previousFrame);
                    previousFrame = currentFrame;
                }
            }
        } finally {
            CloseableUtils.closeQuietly(previousFrame);
        }

        return plannedFrames;
    }

    @NotNull
    protected PlannedCandidate chooseBestCandidate(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                                   boolean introSequence, int frameIndex, @NotNull AfmaEncodeOptions options,
                                                   @NotNull AfmaRectCopyDetector copyDetector,
                                                   @NotNull AfmaFfmpegBridge ffmpegBridge,
                                                   @NotNull Map<String, String> payloadPathsByFingerprint) throws IOException {
        List<PlannedCandidate> candidates = new ArrayList<>();
        PlannedCandidate fullCandidate = this.optimizeSelectedCandidatePayloads(this.createFullCandidate(currentFrame, introSequence, frameIndex), ffmpegBridge);
        candidates.add(fullCandidate);

        AfmaRect deltaBounds = AfmaPixelFrameHelper.findDifferenceBounds(previousFrame, currentFrame);
        if (deltaBounds != null) {
            PlannedCandidate deltaCandidate = this.optimizeNullableCandidatePayloads(this.createDeltaCandidate(currentFrame, introSequence, frameIndex, deltaBounds), ffmpegBridge);
            if ((deltaCandidate != null) && this.shouldKeepComplexCandidate(deltaCandidate, fullCandidate,
                    deltaBounds.area(), currentFrame.getWidth(), currentFrame.getHeight(),
                    options.getMaxDeltaAreaRatioWithoutStrongSavings(), options, payloadPathsByFingerprint)) {
                candidates.add(deltaCandidate);
            }

            PlannedCandidate residualDeltaCandidate = this.optimizeNullableCandidatePayloads(this.createResidualDeltaCandidate(previousFrame, currentFrame, introSequence, frameIndex, deltaBounds), ffmpegBridge);
            if ((residualDeltaCandidate != null) && this.shouldKeepResidualCandidate(residualDeltaCandidate, fullCandidate,
                    deltaBounds.area(), currentFrame.getWidth(), currentFrame.getHeight(),
                    options.getMaxDeltaAreaRatioWithoutStrongSavings(), options, payloadPathsByFingerprint)) {
                candidates.add(residualDeltaCandidate);
            }

            PlannedCandidate sparseDeltaCandidate = this.optimizeNullableCandidatePayloads(this.createSparseDeltaCandidate(previousFrame, currentFrame, introSequence, frameIndex, deltaBounds), ffmpegBridge);
            if ((sparseDeltaCandidate != null) && this.shouldKeepSparseCandidate(sparseDeltaCandidate, fullCandidate,
                    deltaBounds.area(), currentFrame.getWidth(), currentFrame.getHeight(),
                    options.getMaxDeltaAreaRatioWithoutStrongSavings(), options, payloadPathsByFingerprint)) {
                candidates.add(sparseDeltaCandidate);
            }
        }

        if (options.isRectCopyEnabled()) {
            AfmaRectCopyDetector.Detection detection = copyDetector.detect(previousFrame, currentFrame);
            if (detection != null) {
                PlannedCandidate copyCandidate = this.optimizeNullableCandidatePayloads(this.createCopyCandidate(currentFrame, introSequence, frameIndex, detection), ffmpegBridge);
                long patchArea = (detection.patchBounds() != null) ? detection.patchBounds().area() : 0L;
                if ((copyCandidate != null) && this.shouldKeepComplexCandidate(copyCandidate, fullCandidate,
                        patchArea, currentFrame.getWidth(), currentFrame.getHeight(),
                        options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options, payloadPathsByFingerprint)) {
                    candidates.add(copyCandidate);
                }

                PlannedCandidate copyResidualCandidate = this.optimizeNullableCandidatePayloads(this.createCopyResidualCandidate(previousFrame, currentFrame, introSequence, frameIndex, detection), ffmpegBridge);
                if ((copyResidualCandidate != null) && this.shouldKeepResidualCandidate(copyResidualCandidate, fullCandidate,
                        patchArea, currentFrame.getWidth(), currentFrame.getHeight(),
                        options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options, payloadPathsByFingerprint)) {
                    candidates.add(copyResidualCandidate);
                }

                PlannedCandidate copySparseCandidate = this.optimizeNullableCandidatePayloads(this.createCopySparseCandidate(previousFrame, currentFrame, introSequence, frameIndex, detection), ffmpegBridge);
                if ((copySparseCandidate != null) && this.shouldKeepSparseCandidate(copySparseCandidate, fullCandidate,
                        patchArea, currentFrame.getWidth(), currentFrame.getHeight(),
                        options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options, payloadPathsByFingerprint)) {
                    candidates.add(copySparseCandidate);
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
    protected AfmaPixelFrame applyNearLosslessTemporalMerge(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame, int maxChannelDelta) {
        AfmaPixelFrameHelper.ensureSameSize(previousFrame, currentFrame);
        if (maxChannelDelta <= 0) {
            return currentFrame;
        }

        int width = currentFrame.getWidth();
        int height = currentFrame.getHeight();
        int[] mergedPixels = null;
        for (int y = 0; y < height; y++) {
            int rowOffset = y * width;
            for (int x = 0; x < width; x++) {
                int previousColor = previousFrame.getPixelRGBA(x, y);
                int currentColor = currentFrame.getPixelRGBA(x, y);
                if (!shouldMergeNearLossless(previousColor, currentColor, maxChannelDelta)) {
                    continue;
                }

                if (mergedPixels == null) {
                    mergedPixels = currentFrame.copyPixels();
                }
                mergedPixels[rowOffset + x] = previousColor;
            }
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

    @NotNull
    protected PlannedCandidate createFullCandidate(@NotNull AfmaPixelFrame currentFrame, boolean introSequence, int frameIndex) throws IOException {
        String payloadPath = this.buildPayloadPath(introSequence, frameIndex);
        byte[] encodedPayload = currentFrame.asByteArray();
        byte[] reusableSourcePayload = currentFrame.getReusableSourcePngPayload();
        byte[] payloadBytes = ((reusableSourcePayload != null) && (reusableSourcePayload.length <= encodedPayload.length))
                ? reusableSourcePayload
                : encodedPayload;
        boolean payloadReusedFromSource = payloadBytes == reusableSourcePayload;
        return new PlannedCandidate(
                AfmaFrameDescriptor.full(payloadPath),
                payloadPath,
                payloadBytes,
                PayloadKind.PNG,
                payloadReusedFromSource,
                null,
                null,
                null,
                false,
                DecodeCost.FULL,
                1
        );
    }

    @Nullable
    protected PlannedCandidate createDeltaCandidate(@NotNull AfmaPixelFrame currentFrame, boolean introSequence, int frameIndex, @NotNull AfmaRect deltaBounds) throws IOException {
        if (deltaBounds.area() >= ((long) currentFrame.getWidth() * currentFrame.getHeight())) {
            return null;
        }

        AfmaPixelFrame patchImage = this.frameNormalizer.extractPatch(currentFrame, deltaBounds.x(), deltaBounds.y(), deltaBounds.width(), deltaBounds.height());
        try {
            String payloadPath = this.buildPayloadPath(introSequence, frameIndex);
            return new PlannedCandidate(
                    AfmaFrameDescriptor.deltaRect(payloadPath, deltaBounds.x(), deltaBounds.y(), deltaBounds.width(), deltaBounds.height()),
                    payloadPath,
                    patchImage.asByteArray(),
                    PayloadKind.PNG,
                    false,
                    null,
                    null,
                    null,
                    false,
                    DecodeCost.DELTA,
                    2
            );
        } finally {
            patchImage.close();
        }
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
                        new AfmaResidualPayload(residualPayload.channels())
                ),
                payloadPath,
                residualPayload.payloadBytes(),
                PayloadKind.RAW,
                false,
                null,
                null,
                null,
                false,
                DecodeCost.RESIDUAL_DELTA_RECT,
                3
        );
    }

    @Nullable
    protected PlannedCandidate createSparseDeltaCandidate(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                                          boolean introSequence, int frameIndex, @NotNull AfmaRect deltaBounds) {
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
                        new AfmaSparsePayload(residualPayloadPath, sparsePayload.changedPixelCount(), sparsePayload.channels())
                ),
                maskPayloadPath,
                sparsePayload.maskPayload(),
                PayloadKind.RAW,
                false,
                residualPayloadPath,
                sparsePayload.residualPayload(),
                PayloadKind.RAW,
                false,
                DecodeCost.SPARSE_DELTA_RECT,
                4
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
                        new AfmaResidualPayload(residualPayload.channels())
                ),
                payloadPath,
                residualPayload.payloadBytes(),
                PayloadKind.RAW,
                false,
                null,
                null,
                null,
                false,
                DecodeCost.COPY_RECT_RESIDUAL_PATCH,
                5
        );
    }

    @Nullable
    protected PlannedCandidate createCopySparseCandidate(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                                         boolean introSequence, int frameIndex,
                                                         @NotNull AfmaRectCopyDetector.Detection detection) {
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
                        new AfmaSparsePayload(residualPayloadPath, sparsePayload.changedPixelCount(), sparsePayload.channels())
                ),
                maskPayloadPath,
                sparsePayload.maskPayload(),
                PayloadKind.RAW,
                false,
                residualPayloadPath,
                sparsePayload.residualPayload(),
                PayloadKind.RAW,
                false,
                DecodeCost.COPY_RECT_SPARSE_PATCH,
                6
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

        boolean includeAlpha = this.hasAlphaResidual(previousFrame, currentFrame, deltaBounds);
        int channels = AfmaResidualPayloadHelper.channelCount(includeAlpha);
        int expectedBytes = AfmaResidualPayloadHelper.expectedDenseResidualBytes(width, height, channels);
        if (expectedBytes <= 0) {
            return null;
        }

        byte[] payloadBytes = new byte[expectedBytes];
        int payloadOffset = 0;
        for (int localY = 0; localY < height; localY++) {
            int sourceY = deltaBounds.y() + localY;
            for (int localX = 0; localX < width; localX++) {
                int sourceX = deltaBounds.x() + localX;
                int predictedColor = previousFrame.getPixelRGBA(sourceX, sourceY);
                int currentColor = currentFrame.getPixelRGBA(sourceX, sourceY);
                payloadOffset = AfmaResidualPayloadHelper.writeResidual(payloadBytes, payloadOffset, predictedColor, currentColor, includeAlpha);
            }
        }
        return new ResidualPayloadData(payloadBytes, channels);
    }

    @Nullable
    protected SparseResidualPayloadData buildSparseDeltaPayload(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                                                @NotNull AfmaRect deltaBounds) {
        int width = deltaBounds.width();
        int height = deltaBounds.height();
        long bboxArea = deltaBounds.area();
        if ((bboxArea <= 0L) || (bboxArea > Integer.MAX_VALUE)) {
            return null;
        }

        int changedPixelCount = 0;
        boolean includeAlpha = false;
        for (int localY = 0; localY < height; localY++) {
            int sourceY = deltaBounds.y() + localY;
            for (int localX = 0; localX < width; localX++) {
                int sourceX = deltaBounds.x() + localX;
                int previousColor = previousFrame.getPixelRGBA(sourceX, sourceY);
                int currentColor = currentFrame.getPixelRGBA(sourceX, sourceY);
                if (previousColor == currentColor) {
                    continue;
                }

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

        int channels = AfmaResidualPayloadHelper.channelCount(includeAlpha);
        int maskByteCount = AfmaResidualPayloadHelper.expectedSparseMaskBytes(width, height);
        int residualByteCount = AfmaResidualPayloadHelper.expectedSparseResidualBytes(changedPixelCount, channels);
        if ((maskByteCount <= 0) || (residualByteCount <= 0)) {
            return null;
        }

        byte[] maskPayload = new byte[maskByteCount];
        byte[] residualPayload = new byte[residualByteCount];
        int residualOffset = 0;
        int bitIndex = 0;
        for (int localY = 0; localY < height; localY++) {
            int sourceY = deltaBounds.y() + localY;
            for (int localX = 0; localX < width; localX++, bitIndex++) {
                int sourceX = deltaBounds.x() + localX;
                int previousColor = previousFrame.getPixelRGBA(sourceX, sourceY);
                int currentColor = currentFrame.getPixelRGBA(sourceX, sourceY);
                if (previousColor == currentColor) {
                    continue;
                }

                AfmaResidualPayloadHelper.setMaskBit(maskPayload, bitIndex);
                residualOffset = AfmaResidualPayloadHelper.writeResidual(residualPayload, residualOffset, previousColor, currentColor, includeAlpha);
            }
        }
        return new SparseResidualPayloadData(maskPayload, residualPayload, changedPixelCount, channels);
    }

    @Nullable
    protected ResidualPayloadData buildCopyResidualPayload(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                                           @NotNull AfmaCopyRect copyRect, @NotNull AfmaRect patchBounds) {
        int width = patchBounds.width();
        int height = patchBounds.height();
        if ((width <= 0) || (height <= 0)) {
            return null;
        }

        boolean includeAlpha = this.hasCopyAlphaResidual(previousFrame, currentFrame, copyRect, patchBounds);
        int channels = AfmaResidualPayloadHelper.channelCount(includeAlpha);
        int expectedBytes = AfmaResidualPayloadHelper.expectedDenseResidualBytes(width, height, channels);
        if (expectedBytes <= 0) {
            return null;
        }

        byte[] payloadBytes = new byte[expectedBytes];
        int payloadOffset = 0;
        for (int localY = 0; localY < height; localY++) {
            int sourceY = patchBounds.y() + localY;
            for (int localX = 0; localX < width; localX++) {
                int sourceX = patchBounds.x() + localX;
                int predictedColor = this.getExpectedColorAfterCopy(previousFrame, copyRect, sourceX, sourceY);
                int currentColor = currentFrame.getPixelRGBA(sourceX, sourceY);
                payloadOffset = AfmaResidualPayloadHelper.writeResidual(payloadBytes, payloadOffset, predictedColor, currentColor, includeAlpha);
            }
        }
        return new ResidualPayloadData(payloadBytes, channels);
    }

    @Nullable
    protected SparseResidualPayloadData buildCopySparsePayload(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                                               @NotNull AfmaCopyRect copyRect, @NotNull AfmaRect patchBounds) {
        int width = patchBounds.width();
        int height = patchBounds.height();
        long bboxArea = patchBounds.area();
        if ((bboxArea <= 0L) || (bboxArea > Integer.MAX_VALUE)) {
            return null;
        }

        int changedPixelCount = 0;
        boolean includeAlpha = false;
        for (int localY = 0; localY < height; localY++) {
            int sourceY = patchBounds.y() + localY;
            for (int localX = 0; localX < width; localX++) {
                int sourceX = patchBounds.x() + localX;
                int predictedColor = this.getExpectedColorAfterCopy(previousFrame, copyRect, sourceX, sourceY);
                int currentColor = currentFrame.getPixelRGBA(sourceX, sourceY);
                if (predictedColor == currentColor) {
                    continue;
                }

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

        int channels = AfmaResidualPayloadHelper.channelCount(includeAlpha);
        int maskByteCount = AfmaResidualPayloadHelper.expectedSparseMaskBytes(width, height);
        int residualByteCount = AfmaResidualPayloadHelper.expectedSparseResidualBytes(changedPixelCount, channels);
        if ((maskByteCount <= 0) || (residualByteCount <= 0)) {
            return null;
        }

        byte[] maskPayload = new byte[maskByteCount];
        byte[] residualPayload = new byte[residualByteCount];
        int residualOffset = 0;
        int bitIndex = 0;
        for (int localY = 0; localY < height; localY++) {
            int sourceY = patchBounds.y() + localY;
            for (int localX = 0; localX < width; localX++, bitIndex++) {
                int sourceX = patchBounds.x() + localX;
                int predictedColor = this.getExpectedColorAfterCopy(previousFrame, copyRect, sourceX, sourceY);
                int currentColor = currentFrame.getPixelRGBA(sourceX, sourceY);
                if (predictedColor == currentColor) {
                    continue;
                }

                AfmaResidualPayloadHelper.setMaskBit(maskPayload, bitIndex);
                residualOffset = AfmaResidualPayloadHelper.writeResidual(residualPayload, residualOffset, predictedColor, currentColor, includeAlpha);
            }
        }
        return new SparseResidualPayloadData(maskPayload, residualPayload, changedPixelCount, channels);
    }

    protected boolean hasAlphaResidual(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame, @NotNull AfmaRect bounds) {
        for (int localY = 0; localY < bounds.height(); localY++) {
            int sourceY = bounds.y() + localY;
            for (int localX = 0; localX < bounds.width(); localX++) {
                int sourceX = bounds.x() + localX;
                int previousColor = previousFrame.getPixelRGBA(sourceX, sourceY);
                int currentColor = currentFrame.getPixelRGBA(sourceX, sourceY);
                if (((previousColor ^ currentColor) & 0xFF000000) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean hasCopyAlphaResidual(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                           @NotNull AfmaCopyRect copyRect, @NotNull AfmaRect bounds) {
        for (int localY = 0; localY < bounds.height(); localY++) {
            int sourceY = bounds.y() + localY;
            for (int localX = 0; localX < bounds.width(); localX++) {
                int sourceX = bounds.x() + localX;
                int predictedColor = this.getExpectedColorAfterCopy(previousFrame, copyRect, sourceX, sourceY);
                int currentColor = currentFrame.getPixelRGBA(sourceX, sourceY);
                if (((predictedColor ^ currentColor) & 0xFF000000) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    protected int getExpectedColorAfterCopy(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaCopyRect copyRect, int x, int y) {
        if ((x >= copyRect.getDstX()) && (x < (copyRect.getDstX() + copyRect.getWidth()))
                && (y >= copyRect.getDstY()) && (y < (copyRect.getDstY() + copyRect.getHeight()))) {
            int srcX = copyRect.getSrcX() + (x - copyRect.getDstX());
            int srcY = copyRect.getSrcY() + (y - copyRect.getDstY());
            return previousFrame.getPixelRGBA(srcX, srcY);
        }
        return previousFrame.getPixelRGBA(x, y);
    }

    @Nullable
    protected PlannedCandidate createCopyCandidate(@NotNull AfmaPixelFrame currentFrame, boolean introSequence, int frameIndex,
                                                   @NotNull AfmaRectCopyDetector.Detection detection) throws IOException {
        AfmaCopyRect copyRect = detection.copyRect();
        AfmaRect patchBounds = detection.patchBounds();
        if ((patchBounds != null) && (patchBounds.area() >= ((long)currentFrame.getWidth() * currentFrame.getHeight()))) {
            return null;
        }

        String payloadPath = (patchBounds != null) ? this.buildPayloadPath(introSequence, frameIndex) : null;
        AfmaPatchRegion patchRegion = (patchBounds != null) ? patchBounds.toPatchRegion(payloadPath) : null;
        byte[] payloadBytes = null;

        if (patchBounds != null) {
            AfmaPixelFrame patchImage = this.frameNormalizer.extractPatch(currentFrame, patchBounds.x(), patchBounds.y(), patchBounds.width(), patchBounds.height());
            try {
                payloadBytes = patchImage.asByteArray();
            } finally {
                patchImage.close();
            }
        }

        return new PlannedCandidate(
                AfmaFrameDescriptor.copyRectPatch(copyRect, patchRegion),
                null,
                null,
                null,
                false,
                payloadPath,
                payloadBytes,
                PayloadKind.PNG,
                false,
                DecodeCost.COPY_RECT_PATCH,
                3
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

        List<AfmaBlockInterPayloadHelper.TileOperation> tileOperations = new ArrayList<>(tileCountX * tileCountY);
        for (int tileY = 0; tileY < tileCountY; tileY++) {
            int localY = tileY * BLOCK_INTER_TILE_SIZE;
            int dstY = regionBounds.y() + localY;
            int tileHeight = AfmaBlockInterPayloadHelper.tileDimension(tileY, tileCountY, BLOCK_INTER_TILE_SIZE, regionBounds.height());
            for (int tileX = 0; tileX < tileCountX; tileX++) {
                int localX = tileX * BLOCK_INTER_TILE_SIZE;
                int dstX = regionBounds.x() + localX;
                int tileWidth = AfmaBlockInterPayloadHelper.tileDimension(tileX, tileCountX, BLOCK_INTER_TILE_SIZE, regionBounds.width());
                tileOperations.add(this.chooseBestBlockInterTile(previousFrame, currentFrame, dstX, dstY, tileWidth, tileHeight, motionVectors));
            }
        }

        String payloadPath = this.buildRawPayloadPath(introSequence, frameIndex, "bi");
        byte[] payloadBytes = AfmaBlockInterPayloadHelper.writePayload(BLOCK_INTER_TILE_SIZE, regionBounds.width(), regionBounds.height(), tileOperations);
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
                DecodeCost.BLOCK_INTER,
                7
        );
    }

    @NotNull
    protected AfmaBlockInterPayloadHelper.TileOperation chooseBestBlockInterTile(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                                                                 int dstX, int dstY, int width, int height,
                                                                                 @NotNull List<AfmaRectCopyDetector.MotionVector> motionVectors) {
        if (this.isTileIdentical(previousFrame, currentFrame, dstX, dstY, width, height)) {
            return new AfmaBlockInterPayloadHelper.TileOperation(AfmaBlockInterPayloadHelper.TileMode.SKIP, 0, 0, 0, 0, null, null);
        }

        RawTileData rawTile = this.buildRawTileBytes(currentFrame, dstX, dstY, width, height);
        BlockInterTileCandidate bestCandidate = new BlockInterTileCandidate(
                new AfmaBlockInterPayloadHelper.TileOperation(AfmaBlockInterPayloadHelper.TileMode.RAW, 0, 0, rawTile.channels(), 0, rawTile.payloadBytes(), null),
                this.estimateBlockInterTileBytes(AfmaBlockInterPayloadHelper.TileMode.RAW, rawTile.payloadBytes().length, 0)
        );

        for (AfmaRectCopyDetector.MotionVector motionVector : motionVectors) {
            int srcX = dstX + motionVector.dx();
            int srcY = dstY + motionVector.dy();
            if (!this.isMotionTileInBounds(previousFrame, srcX, srcY, width, height)) {
                continue;
            }

            MotionTileStats tileStats = this.scanMotionTile(previousFrame, currentFrame, dstX, dstY, srcX, srcY, width, height);
            if (tileStats.changedPixelCount() <= 0) {
                BlockInterTileCandidate copyCandidate = new BlockInterTileCandidate(
                        new AfmaBlockInterPayloadHelper.TileOperation(AfmaBlockInterPayloadHelper.TileMode.COPY, motionVector.dx(), motionVector.dy(), 0, 0, null, null),
                        this.estimateBlockInterTileBytes(AfmaBlockInterPayloadHelper.TileMode.COPY, 0, 0)
                );
                if (copyCandidate.estimatedBytes() < bestCandidate.estimatedBytes()) {
                    bestCandidate = copyCandidate;
                }
                continue;
            }

            ResidualPayloadData denseResidual = this.buildMotionResidualPayload(previousFrame, currentFrame, dstX, dstY, srcX, srcY, width, height, tileStats.includeAlpha());
            if (denseResidual != null) {
                BlockInterTileCandidate denseCandidate = new BlockInterTileCandidate(
                        new AfmaBlockInterPayloadHelper.TileOperation(AfmaBlockInterPayloadHelper.TileMode.COPY_DENSE,
                                motionVector.dx(), motionVector.dy(), denseResidual.channels(), 0, denseResidual.payloadBytes(), null),
                        this.estimateBlockInterTileBytes(AfmaBlockInterPayloadHelper.TileMode.COPY_DENSE, denseResidual.payloadBytes().length, 0)
                );
                if (denseCandidate.estimatedBytes() < bestCandidate.estimatedBytes()) {
                    bestCandidate = denseCandidate;
                }
            }

            SparseResidualPayloadData sparseResidual = this.buildMotionSparseResidualPayload(previousFrame, currentFrame, dstX, dstY, srcX, srcY, width, height, tileStats);
            if (sparseResidual != null) {
                BlockInterTileCandidate sparseCandidate = new BlockInterTileCandidate(
                        new AfmaBlockInterPayloadHelper.TileOperation(AfmaBlockInterPayloadHelper.TileMode.COPY_SPARSE,
                                motionVector.dx(), motionVector.dy(), sparseResidual.channels(), sparseResidual.changedPixelCount(),
                                sparseResidual.maskPayload(), sparseResidual.residualPayload()),
                        this.estimateBlockInterTileBytes(AfmaBlockInterPayloadHelper.TileMode.COPY_SPARSE,
                                sparseResidual.maskPayload().length, sparseResidual.residualPayload().length)
                );
                if (sparseCandidate.estimatedBytes() < bestCandidate.estimatedBytes()) {
                    bestCandidate = sparseCandidate;
                }
            }
        }

        return bestCandidate.operation();
    }

    protected boolean isTileIdentical(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                      int dstX, int dstY, int width, int height) {
        for (int localY = 0; localY < height; localY++) {
            int sourceY = dstY + localY;
            for (int localX = 0; localX < width; localX++) {
                int sourceX = dstX + localX;
                if (previousFrame.getPixelRGBA(sourceX, sourceY) != currentFrame.getPixelRGBA(sourceX, sourceY)) {
                    return false;
                }
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

    @NotNull
    protected MotionTileStats scanMotionTile(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                             int dstX, int dstY, int srcX, int srcY, int width, int height) {
        int changedPixelCount = 0;
        boolean includeAlpha = false;
        for (int localY = 0; localY < height; localY++) {
            for (int localX = 0; localX < width; localX++) {
                int predictedColor = previousFrame.getPixelRGBA(srcX + localX, srcY + localY);
                int currentColor = currentFrame.getPixelRGBA(dstX + localX, dstY + localY);
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

    @Nullable
    protected ResidualPayloadData buildMotionResidualPayload(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                                             int dstX, int dstY, int srcX, int srcY, int width, int height, boolean includeAlpha) {
        int channels = AfmaResidualPayloadHelper.channelCount(includeAlpha);
        int expectedBytes = AfmaBlockInterPayloadHelper.expectedDenseResidualBytes(width, height, channels);
        if (expectedBytes <= 0) {
            return null;
        }

        byte[] payloadBytes = new byte[expectedBytes];
        int payloadOffset = 0;
        for (int localY = 0; localY < height; localY++) {
            for (int localX = 0; localX < width; localX++) {
                int predictedColor = previousFrame.getPixelRGBA(srcX + localX, srcY + localY);
                int currentColor = currentFrame.getPixelRGBA(dstX + localX, dstY + localY);
                payloadOffset = AfmaResidualPayloadHelper.writeResidual(payloadBytes, payloadOffset, predictedColor, currentColor, includeAlpha);
            }
        }
        return new ResidualPayloadData(payloadBytes, channels);
    }

    @Nullable
    protected SparseResidualPayloadData buildMotionSparseResidualPayload(@NotNull AfmaPixelFrame previousFrame, @NotNull AfmaPixelFrame currentFrame,
                                                                         int dstX, int dstY, int srcX, int srcY, int width, int height,
                                                                         @NotNull MotionTileStats tileStats) {
        if (tileStats.changedPixelCount() <= 0 || tileStats.changedPixelCount() >= (width * height)) {
            return null;
        }

        int channels = AfmaResidualPayloadHelper.channelCount(tileStats.includeAlpha());
        int maskByteCount = AfmaResidualPayloadHelper.expectedSparseMaskBytes(width, height);
        int residualByteCount = AfmaResidualPayloadHelper.expectedSparseResidualBytes(tileStats.changedPixelCount(), channels);
        if ((maskByteCount <= 0) || (residualByteCount <= 0)) {
            return null;
        }

        byte[] maskPayload = new byte[maskByteCount];
        byte[] residualPayload = new byte[residualByteCount];
        int residualOffset = 0;
        int bitIndex = 0;
        for (int localY = 0; localY < height; localY++) {
            for (int localX = 0; localX < width; localX++, bitIndex++) {
                int predictedColor = previousFrame.getPixelRGBA(srcX + localX, srcY + localY);
                int currentColor = currentFrame.getPixelRGBA(dstX + localX, dstY + localY);
                if (predictedColor == currentColor) {
                    continue;
                }
                AfmaResidualPayloadHelper.setMaskBit(maskPayload, bitIndex);
                residualOffset = AfmaResidualPayloadHelper.writeResidual(residualPayload, residualOffset, predictedColor, currentColor, tileStats.includeAlpha());
            }
        }
        return new SparseResidualPayloadData(maskPayload, residualPayload, tileStats.changedPixelCount(), channels);
    }

    @NotNull
    protected RawTileData buildRawTileBytes(@NotNull AfmaPixelFrame currentFrame, int dstX, int dstY, int width, int height) {
        boolean includeAlpha = false;
        for (int localY = 0; localY < height && !includeAlpha; localY++) {
            for (int localX = 0; localX < width; localX++) {
                if (((currentFrame.getPixelRGBA(dstX + localX, dstY + localY) >>> 24) & 0xFF) != 0xFF) {
                    includeAlpha = true;
                    break;
                }
            }
        }

        int channels = AfmaResidualPayloadHelper.channelCount(includeAlpha);
        byte[] payloadBytes = new byte[AfmaBlockInterPayloadHelper.expectedRawTileBytes(width, height, channels)];
        int payloadOffset = 0;
        for (int localY = 0; localY < height; localY++) {
            for (int localX = 0; localX < width; localX++) {
                int color = currentFrame.getPixelRGBA(dstX + localX, dstY + localY);
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
            case COPY_SPARSE -> 8L + primaryBytes + secondaryBytes;
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

    protected long computeRequiredComplexCandidateSavings(long referenceBytes, long minAbsoluteSavings, double minSavingsRatio) {
        long ratioSavings = (referenceBytes > 0L && minSavingsRatio > 0D)
                ? (long) Math.ceil(referenceBytes * minSavingsRatio)
                : 0L;
        return Math.max(minAbsoluteSavings, ratioSavings);
    }

    @NotNull
    protected PlannedCandidate optimizeSelectedCandidatePayloads(@NotNull PlannedCandidate candidate, @NotNull AfmaFfmpegBridge ffmpegBridge) throws IOException {
        if (!ffmpegBridge.isReady()) {
            return candidate;
        }

        byte[] optimizedPrimaryPayload = this.optimizePayloadWithFfmpeg(candidate.primaryPayload, candidate.primaryPayloadKind, candidate.primaryPayloadReusedFromSource, ffmpegBridge);
        byte[] optimizedPatchPayload = this.optimizePayloadWithFfmpeg(candidate.patchPayload, candidate.patchPayloadKind, candidate.patchPayloadReusedFromSource, ffmpegBridge);
        if (optimizedPrimaryPayload == candidate.primaryPayload && optimizedPatchPayload == candidate.patchPayload) {
            return candidate;
        }
        return candidate.withPayloads(optimizedPrimaryPayload, optimizedPatchPayload);
    }

    @Nullable
    protected PlannedCandidate optimizeNullableCandidatePayloads(@Nullable PlannedCandidate candidate, @NotNull AfmaFfmpegBridge ffmpegBridge) throws IOException {
        if (candidate == null) {
            return null;
        }
        return this.optimizeSelectedCandidatePayloads(candidate, ffmpegBridge);
    }

    protected @Nullable byte[] optimizePayloadWithFfmpeg(@Nullable byte[] payloadBytes, @Nullable PayloadKind payloadKind,
                                                         boolean payloadReusedFromSource,
                                                         @NotNull AfmaFfmpegBridge ffmpegBridge) throws IOException {
        if (payloadBytes == null || payloadKind != PayloadKind.PNG || payloadReusedFromSource || payloadBytes.length < MIN_FFMPEG_PNG_REWRITE_BYTES) {
            return payloadBytes;
        }

        try {
            byte[] optimizedPayload = ffmpegBridge.optimizePngPayload(payloadBytes);
            return (optimizedPayload != null) ? optimizedPayload : payloadBytes;
        } catch (IOException ignored) {
            return payloadBytes;
        }
    }

    @NotNull
    protected String buildPayloadPath(boolean introSequence, int frameIndex) {
        return (introSequence ? "intro_frames/" : "frames/") + Integer.toUnsignedString(frameIndex, 36) + ".png";
    }

    @NotNull
    protected String buildRawPayloadPath(boolean introSequence, int frameIndex, @NotNull String suffix) {
        return (introSequence ? "intro_frames/" : "frames/") + Integer.toUnsignedString(frameIndex, 36) + "_" + suffix + ".bin";
    }

    @NotNull
    protected Dimension readDimension(@NotNull AfmaSourceSequence sequence) throws IOException {
        if (sequence.isEmpty()) {
            throw new IOException("AFMA encoding requires at least one source frame");
        }

        File firstFrame = Objects.requireNonNull(sequence.getFrame(0));
        try (AfmaPixelFrame firstImage = this.frameNormalizer.loadFrame(firstFrame)) {
            return new Dimension(firstImage.getWidth(), firstImage.getHeight());
        }
    }

    protected void validateSequenceDimensions(@NotNull AfmaSourceSequence sequence, @NotNull Dimension dimension,
                                              @NotNull String sequenceName, @Nullable BooleanSupplier cancellationRequested,
                                              @Nullable ProgressListener progressListener, int startOffset, int totalFrameCount) throws IOException {
        List<File> frames = sequence.getFrames();
        for (int i = 0; i < frames.size(); i++) {
            checkCancelled(cancellationRequested);
            File frame = frames.get(i);
            reportProgress(progressListener,
                    "Validating " + sequenceName + " frame " + (i + 1) + "/" + frames.size(),
                    0.25D * ((double) (startOffset + i + 1) / Math.max(1, totalFrameCount)));
            try (AfmaPixelFrame image = this.frameNormalizer.loadFrame(frame)) {
                if ((image.getWidth() != dimension.width()) || (image.getHeight() != dimension.height())) {
                    throw new IOException("AFMA " + sequenceName + " frame dimensions do not match: " + frame.getAbsolutePath());
                }
            }
        }
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
        RESIDUAL_DELTA_RECT,
        COPY_RECT_RESIDUAL_PATCH,
        SPARSE_DELTA_RECT,
        COPY_RECT_SPARSE_PATCH,
        BLOCK_INTER
    }

    protected enum PayloadKind {
        PNG,
        RAW
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
        protected final DecodeCost decodeCost;
        protected final int complexityScore;

        protected PlannedCandidate(@NotNull AfmaFrameDescriptor descriptor,
                                   @Nullable String primaryPayloadPath, @Nullable byte[] primaryPayload, @Nullable PayloadKind primaryPayloadKind, boolean primaryPayloadReusedFromSource,
                                   @Nullable String patchPayloadPath, @Nullable byte[] patchPayload, @Nullable PayloadKind patchPayloadKind, boolean patchPayloadReusedFromSource,
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
            this.decodeCost = decodeCost;
            this.complexityScore = complexityScore;
        }

        @NotNull
        public static PlannedCandidate same() {
            return new PlannedCandidate(AfmaFrameDescriptor.same(), null, null, null, false, null, null, null, false, DecodeCost.SAME, 0);
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
                    this.decodeCost,
                    this.complexityScore
            );
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
            String fingerprint = fingerprintPayload(payload);
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
            int bytes = 16;
            if (this.descriptor.getType() != null) {
                bytes += this.descriptor.getType().name().length();
            }
            if (this.primaryPayloadPath != null) {
                bytes += this.primaryPayloadPath.length();
            } else if (this.descriptor.getPath() != null) {
                bytes += this.descriptor.getPath().length();
            }
            if (this.patchPayloadPath != null) {
                bytes += this.patchPayloadPath.length();
            } else if (this.descriptor.getSecondaryPayloadPath() != null) {
                bytes += Objects.requireNonNull(this.descriptor.getSecondaryPayloadPath()).length();
            }
            if (this.descriptor.getType() == AfmaFrameOperationType.DELTA_RECT) {
                bytes += 20;
            } else if (this.descriptor.getType() == AfmaFrameOperationType.RESIDUAL_DELTA_RECT) {
                bytes += 24;
            } else if (this.descriptor.getType() == AfmaFrameOperationType.COPY_RECT_PATCH) {
                bytes += 32;
            } else if (this.descriptor.getType() == AfmaFrameOperationType.COPY_RECT_RESIDUAL_PATCH) {
                bytes += 36;
            } else if (this.descriptor.getType() == AfmaFrameOperationType.SPARSE_DELTA_RECT) {
                bytes += 28;
            } else if (this.descriptor.getType() == AfmaFrameOperationType.COPY_RECT_SPARSE_PATCH) {
                bytes += 40;
            } else if (this.descriptor.getType() == AfmaFrameOperationType.BLOCK_INTER) {
                bytes += 28;
            }
            return bytes;
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

            String fingerprint = fingerprintPayload(this.primaryPayload);
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

            String fingerprint = fingerprintPayload(this.patchPayload);
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
                        this.decodeCost,
                        this.complexityScore
                );
            }

            payloadPathsByFingerprint.put(fingerprint, this.patchPayloadPath);
            return this;
        }

        @NotNull
        protected static String fingerprintPayload(@NotNull byte[] payload) {
            try {
                byte[] digest = MessageDigest.getInstance("SHA-256").digest(payload);
                StringBuilder builder = new StringBuilder(digest.length * 2);
                for (byte digestByte : digest) {
                    builder.append(Character.forDigit((digestByte >>> 4) & 0xF, 16));
                    builder.append(Character.forDigit(digestByte & 0xF, 16));
                }
                return builder.toString();
            } catch (NoSuchAlgorithmException ex) {
                return payload.length + ":" + Arrays.hashCode(payload);
            }
        }

        protected static long estimatePayloadArchiveBytes(@Nullable byte[] payload, @Nullable PayloadKind payloadKind) {
            if (payload == null) {
                return 0L;
            }
            if (payloadKind != PayloadKind.RAW || payload.length < 1024) {
                return payload.length;
            }

            Deflater deflater = new Deflater(9, true);
            byte[] buffer = new byte[8192];
            long compressedBytes = 0L;
            try {
                deflater.setInput(payload);
                deflater.finish();
                while (!deflater.finished()) {
                    compressedBytes += deflater.deflate(buffer);
                }
            } finally {
                deflater.end();
            }
            return Math.max(1L, compressedBytes);
        }

    }

    protected record Dimension(int width, int height) {
    }

    protected record ResidualPayloadData(@NotNull byte[] payloadBytes, int channels) {
    }

    protected record SparseResidualPayloadData(@NotNull byte[] maskPayload, @NotNull byte[] residualPayload, int changedPixelCount, int channels) {
    }

    protected record MotionTileStats(int changedPixelCount, boolean includeAlpha) {
    }

    protected record RawTileData(@NotNull byte[] payloadBytes, int channels) {
    }

    protected record BlockInterTileCandidate(@NotNull AfmaBlockInterPayloadHelper.TileOperation operation, long estimatedBytes) {
    }

    @FunctionalInterface
    public interface ProgressListener {
        void update(@NotNull String detail, double progress);
    }

}
