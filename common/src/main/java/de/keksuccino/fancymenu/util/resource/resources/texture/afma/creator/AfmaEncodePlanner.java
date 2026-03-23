package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaCopyRect;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameDescriptor;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameIndex;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameOperationType;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaMetadata;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaNativeImageHelper;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaPatchRegion;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaRect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AfmaEncodePlanner {

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
        Objects.requireNonNull(mainSequence);
        Objects.requireNonNull(options);

        AfmaSourceSequence intro = (introSequence != null) ? introSequence : AfmaSourceSequence.empty();
        options.validateForCounts(mainSequence.size(), intro.size());

        Dimension dimension = this.readDimension(mainSequence);
        this.validateSequenceDimensions(mainSequence, dimension, "main");
        if (!intro.isEmpty()) {
            this.validateSequenceDimensions(intro, dimension, "intro");
        }

        AfmaRectCopyDetector copyDetector = new AfmaRectCopyDetector(options.getMaxCopySearchDistance(), options.getMaxCandidateAxisOffsets());
        LinkedHashMap<String, byte[]> payloads = new LinkedHashMap<>();
        List<AfmaFrameDescriptor> plannedIntroFrames = this.planSequence(intro, true, dimension, options, copyDetector, payloads);
        List<AfmaFrameDescriptor> plannedMainFrames = this.planSequence(mainSequence, false, dimension, options, copyDetector, payloads);

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
                                                     @NotNull LinkedHashMap<String, byte[]> payloads) throws IOException {
        List<AfmaFrameDescriptor> plannedFrames = new ArrayList<>();
        if (sequence.isEmpty()) {
            return plannedFrames;
        }

        NativeImage previousFrame = null;
        int framesSinceKeyframe = 0;
        try {
            for (int frameIndex = 0; frameIndex < sequence.size(); frameIndex++) {
                File frameFile = Objects.requireNonNull(sequence.getFrame(frameIndex));
                NativeImage currentFrame = this.frameNormalizer.loadFrame(frameFile);
                try {
                    if ((currentFrame.getWidth() != dimension.width()) || (currentFrame.getHeight() != dimension.height())) {
                        throw new IOException("AFMA source frame dimensions do not match the expected canvas size: " + frameFile.getAbsolutePath());
                    }

                    PlannedCandidate selectedCandidate;
                    if (previousFrame == null) {
                        selectedCandidate = this.createFullCandidate(currentFrame, introSequence, frameIndex);
                    } else if (options.isDuplicateFrameElision() && AfmaNativeImageHelper.isIdentical(previousFrame, currentFrame)) {
                        selectedCandidate = PlannedCandidate.same();
                    } else if (framesSinceKeyframe >= options.getKeyframeInterval()) {
                        selectedCandidate = this.createFullCandidate(currentFrame, introSequence, frameIndex);
                    } else {
                        selectedCandidate = this.chooseBestCandidate(previousFrame, currentFrame, introSequence, frameIndex, options, copyDetector);
                    }

                    plannedFrames.add(selectedCandidate.descriptor());
                    selectedCandidate.writePayloads(payloads);
                    framesSinceKeyframe = selectedCandidate.descriptor().isKeyframe() ? 0 : (framesSinceKeyframe + 1);
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
    protected PlannedCandidate chooseBestCandidate(@NotNull NativeImage previousFrame, @NotNull NativeImage currentFrame,
                                                   boolean introSequence, int frameIndex, @NotNull AfmaEncodeOptions options,
                                                   @NotNull AfmaRectCopyDetector copyDetector) throws IOException {
        List<PlannedCandidate> candidates = new ArrayList<>();
        candidates.add(this.createFullCandidate(currentFrame, introSequence, frameIndex));

        AfmaRect deltaBounds = AfmaNativeImageHelper.findDifferenceBounds(previousFrame, currentFrame);
        if (deltaBounds != null) {
            candidates.add(this.createDeltaCandidate(currentFrame, introSequence, frameIndex, deltaBounds));
        }

        if (options.isRectCopyEnabled()) {
            AfmaRectCopyDetector.Detection detection = copyDetector.detect(previousFrame, currentFrame);
            if (detection != null) {
                PlannedCandidate copyCandidate = this.createCopyCandidate(currentFrame, introSequence, frameIndex, detection);
                if (copyCandidate != null) {
                    candidates.add(copyCandidate);
                }
            }
        }

        PlannedCandidate bestCandidate = null;
        for (PlannedCandidate candidate : candidates) {
            if ((bestCandidate == null) || candidate.isBetterThan(bestCandidate)) {
                bestCandidate = candidate;
            }
        }

        return Objects.requireNonNull(bestCandidate, "Failed to choose an AFMA encode candidate");
    }

    @NotNull
    protected PlannedCandidate createFullCandidate(@NotNull NativeImage currentFrame, boolean introSequence, int frameIndex) throws IOException {
        String payloadPath = this.buildPayloadPath(introSequence, frameIndex);
        return new PlannedCandidate(
                AfmaFrameDescriptor.full(payloadPath),
                payloadPath,
                currentFrame.asByteArray(),
                null,
                null,
                DecodeCost.FULL,
                1
        );
    }

    @NotNull
    protected PlannedCandidate createDeltaCandidate(@NotNull NativeImage currentFrame, boolean introSequence, int frameIndex, @NotNull AfmaRect deltaBounds) throws IOException {
        NativeImage patchImage = this.frameNormalizer.extractPatch(currentFrame, deltaBounds.x(), deltaBounds.y(), deltaBounds.width(), deltaBounds.height());
        try {
            String payloadPath = this.buildPayloadPath(introSequence, frameIndex);
            return new PlannedCandidate(
                    AfmaFrameDescriptor.deltaRect(payloadPath, deltaBounds.x(), deltaBounds.y(), deltaBounds.width(), deltaBounds.height()),
                    payloadPath,
                    patchImage.asByteArray(),
                    null,
                    null,
                    DecodeCost.DELTA,
                    2
            );
        } finally {
            patchImage.close();
        }
    }

    @Nullable
    protected PlannedCandidate createCopyCandidate(@NotNull NativeImage currentFrame, boolean introSequence, int frameIndex,
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
            NativeImage patchImage = this.frameNormalizer.extractPatch(currentFrame, patchBounds.x(), patchBounds.y(), patchBounds.width(), patchBounds.height());
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
                payloadPath,
                payloadBytes,
                DecodeCost.COPY_RECT_PATCH,
                3
        );
    }

    @NotNull
    protected String buildPayloadPath(boolean introSequence, int frameIndex) {
        return (introSequence ? "intro_frames/" : "frames/") + String.format("%06d.png", frameIndex);
    }

    @NotNull
    protected Dimension readDimension(@NotNull AfmaSourceSequence sequence) throws IOException {
        if (sequence.isEmpty()) {
            throw new IOException("AFMA encoding requires at least one source frame");
        }

        File firstFrame = Objects.requireNonNull(sequence.getFrame(0));
        try (NativeImage firstImage = this.frameNormalizer.loadFrame(firstFrame)) {
            return new Dimension(firstImage.getWidth(), firstImage.getHeight());
        }
    }

    protected void validateSequenceDimensions(@NotNull AfmaSourceSequence sequence, @NotNull Dimension dimension, @NotNull String sequenceName) throws IOException {
        for (File frame : sequence.getFrames()) {
            try (NativeImage image = this.frameNormalizer.loadFrame(frame)) {
                if ((image.getWidth() != dimension.width()) || (image.getHeight() != dimension.height())) {
                    throw new IOException("AFMA " + sequenceName + " frame dimensions do not match: " + frame.getAbsolutePath());
                }
            }
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
        @Nullable
        protected final String patchPayloadPath;
        @Nullable
        protected final byte[] patchPayload;
        @NotNull
        protected final DecodeCost decodeCost;
        protected final int complexityScore;

        protected PlannedCandidate(@NotNull AfmaFrameDescriptor descriptor,
                                   @Nullable String primaryPayloadPath, @Nullable byte[] primaryPayload,
                                   @Nullable String patchPayloadPath, @Nullable byte[] patchPayload,
                                   @NotNull DecodeCost decodeCost, int complexityScore) {
            this.descriptor = descriptor;
            this.primaryPayloadPath = primaryPayloadPath;
            this.primaryPayload = primaryPayload;
            this.patchPayloadPath = patchPayloadPath;
            this.patchPayload = patchPayload;
            this.decodeCost = decodeCost;
            this.complexityScore = complexityScore;
        }

        @NotNull
        public static PlannedCandidate same() {
            return new PlannedCandidate(AfmaFrameDescriptor.same(), null, null, null, null, DecodeCost.SAME, 0);
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

        public boolean isBetterThan(@NotNull PlannedCandidate other) {
            if (this.totalBytes() != other.totalBytes()) {
                return this.totalBytes() < other.totalBytes();
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

    }

    protected record Dimension(int width, int height) {
    }

}
