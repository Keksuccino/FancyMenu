package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaCopyRect;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameDescriptor;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameIndex;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameOperationType;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaMetadata;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaPatchRegion;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaRect;
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

public class AfmaEncodePlanner {

    protected static final int MIN_FFMPEG_PNG_REWRITE_BYTES = 96 * 1024;

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

                    PlannedCandidate selectedCandidate;
                    if (previousFrame == null) {
                        selectedCandidate = this.createFullCandidate(currentFrame, introSequence, frameIndex);
                    } else if (options.isDuplicateFrameElision() && AfmaPixelFrameHelper.isIdentical(previousFrame, currentFrame)) {
                        selectedCandidate = PlannedCandidate.same();
                    } else if ((framesSinceKeyframe + 1) >= options.getKeyframeInterval()) {
                        selectedCandidate = this.createFullCandidate(currentFrame, introSequence, frameIndex);
                    } else {
                        selectedCandidate = this.chooseBestCandidate(previousFrame, currentFrame, introSequence, frameIndex, options, copyDetector, payloadPathsByFingerprint);
                    }

                    selectedCandidate = this.optimizeSelectedCandidatePayloads(selectedCandidate, ffmpegBridge);
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
                                                   @NotNull Map<String, String> payloadPathsByFingerprint) throws IOException {
        List<PlannedCandidate> candidates = new ArrayList<>();
        PlannedCandidate fullCandidate = this.createFullCandidate(currentFrame, introSequence, frameIndex);
        candidates.add(fullCandidate);

        AfmaRect deltaBounds = AfmaPixelFrameHelper.findDifferenceBounds(previousFrame, currentFrame);
        if (deltaBounds != null) {
            PlannedCandidate deltaCandidate = this.createDeltaCandidate(currentFrame, introSequence, frameIndex, deltaBounds);
            if ((deltaCandidate != null) && this.shouldKeepComplexCandidate(deltaCandidate, fullCandidate,
                    deltaBounds.area(), currentFrame.getWidth(), currentFrame.getHeight(),
                    options.getMaxDeltaAreaRatioWithoutStrongSavings(), options, payloadPathsByFingerprint)) {
                candidates.add(deltaCandidate);
            }
        }

        if (options.isRectCopyEnabled()) {
            AfmaRectCopyDetector.Detection detection = copyDetector.detect(previousFrame, currentFrame);
            if (detection != null) {
                PlannedCandidate copyCandidate = this.createCopyCandidate(currentFrame, introSequence, frameIndex, detection);
                long patchArea = (detection.patchBounds() != null) ? detection.patchBounds().area() : 0L;
                if ((copyCandidate != null) && this.shouldKeepComplexCandidate(copyCandidate, fullCandidate,
                        patchArea, currentFrame.getWidth(), currentFrame.getHeight(),
                        options.getMaxCopyPatchAreaRatioWithoutStrongSavings(), options, payloadPathsByFingerprint)) {
                    candidates.add(copyCandidate);
                }
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
                payloadReusedFromSource,
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
                    false,
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
                false,
                payloadPath,
                payloadBytes,
                false,
                DecodeCost.COPY_RECT_PATCH,
                3
        );
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

        byte[] optimizedPrimaryPayload = this.optimizePayloadWithFfmpeg(candidate.primaryPayload, candidate.primaryPayloadReusedFromSource, ffmpegBridge);
        byte[] optimizedPatchPayload = this.optimizePayloadWithFfmpeg(candidate.patchPayload, candidate.patchPayloadReusedFromSource, ffmpegBridge);
        if (optimizedPrimaryPayload == candidate.primaryPayload && optimizedPatchPayload == candidate.patchPayload) {
            return candidate;
        }
        return candidate.withPayloads(optimizedPrimaryPayload, optimizedPatchPayload);
    }

    protected @Nullable byte[] optimizePayloadWithFfmpeg(@Nullable byte[] payloadBytes, boolean payloadReusedFromSource,
                                                         @NotNull AfmaFfmpegBridge ffmpegBridge) throws IOException {
        if (payloadBytes == null || payloadReusedFromSource || payloadBytes.length < MIN_FFMPEG_PNG_REWRITE_BYTES) {
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
        COPY_RECT_PATCH
    }

    protected static class PlannedCandidate {

        @NotNull
        protected final AfmaFrameDescriptor descriptor;
        @Nullable
        protected final String primaryPayloadPath;
        @Nullable
        protected final byte[] primaryPayload;
        protected final boolean primaryPayloadReusedFromSource;
        @Nullable
        protected final String patchPayloadPath;
        @Nullable
        protected final byte[] patchPayload;
        protected final boolean patchPayloadReusedFromSource;
        @NotNull
        protected final DecodeCost decodeCost;
        protected final int complexityScore;

        protected PlannedCandidate(@NotNull AfmaFrameDescriptor descriptor,
                                   @Nullable String primaryPayloadPath, @Nullable byte[] primaryPayload, boolean primaryPayloadReusedFromSource,
                                   @Nullable String patchPayloadPath, @Nullable byte[] patchPayload, boolean patchPayloadReusedFromSource,
                                   @NotNull DecodeCost decodeCost, int complexityScore) {
            this.descriptor = descriptor;
            this.primaryPayloadPath = primaryPayloadPath;
            this.primaryPayload = primaryPayload;
            this.primaryPayloadReusedFromSource = primaryPayloadReusedFromSource;
            this.patchPayloadPath = patchPayloadPath;
            this.patchPayload = patchPayload;
            this.patchPayloadReusedFromSource = patchPayloadReusedFromSource;
            this.decodeCost = decodeCost;
            this.complexityScore = complexityScore;
        }

        @NotNull
        public static PlannedCandidate same() {
            return new PlannedCandidate(AfmaFrameDescriptor.same(), null, null, false, null, null, false, DecodeCost.SAME, 0);
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
                    this.primaryPayloadReusedFromSource && primaryPayload == this.primaryPayload,
                    this.patchPayloadPath,
                    patchPayload,
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
            return ((existingPath != null) && !existingPath.equals(path)) ? 0L : payload.length;
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
            } else if ((this.descriptor.getPatch() != null) && (this.descriptor.getPatch().getPath() != null)) {
                bytes += this.descriptor.getPatch().getPath().length();
            }
            if (this.descriptor.getType() == AfmaFrameOperationType.DELTA_RECT) {
                bytes += 20;
            } else if (this.descriptor.getType() == AfmaFrameOperationType.COPY_RECT_PATCH) {
                bytes += 32;
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
                        false,
                        this.patchPayloadPath,
                        this.patchPayload,
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
                        this.primaryPayloadReusedFromSource,
                        existingPath,
                        null,
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

    }

    protected record Dimension(int width, int height) {
    }

    @FunctionalInterface
    public interface ProgressListener {
        void update(@NotNull String detail, double progress);
    }

}
