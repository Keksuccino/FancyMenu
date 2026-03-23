package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaCopyRect;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameDescriptor;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameIndex;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameOperationType;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaMetadata;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaPatchRegion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class AfmaPreviewController implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final int MAX_PAYLOAD_CACHE = 12;

    private final @NotNull ExecutorService previewExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "FancyMenu-AFMA-Preview");
        thread.setDaemon(true);
        return thread;
    });
    private final @NotNull AtomicInteger previewGeneration = new AtomicInteger(0);
    private final @NotNull LinkedHashMap<String, AfmaPixelFrame> payloadFrameCache = new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, AfmaPixelFrame> eldest) {
            if (this.size() > MAX_PAYLOAD_CACHE) {
                CloseableUtils.closeQuietly(eldest.getValue());
                return true;
            }
            return false;
        }
    };
    private final @NotNull List<PreviewFrameRef> previewFrames = new ArrayList<>();

    private @Nullable AfmaCreatorAnalysisResult analysisResult;
    private @Nullable AfmaEncodePlan previewPlan;
    private @Nullable NativeImage canvas;
    private @Nullable DynamicTexture texture;
    private @Nullable ResourceLocation textureLocation;
    private @Nullable PreviewTaskResult completedTaskResult;

    private int requestedPreviewIndex = -1;
    private int currentPreviewIndex = -1;
    private boolean playing = false;
    private long nextFrameAtMs = 0L;
    private int completedMainLoops = 0;
    private @Nullable String lastFailureMessage = null;

    public void setAnalysisContext(@Nullable AfmaCreatorAnalysisResult analysisResult, @Nullable AfmaEncodePlan previewPlan) {
        if ((this.analysisResult == analysisResult) && (this.previewPlan == previewPlan)) {
            return;
        }

        this.analysisResult = analysisResult;
        this.previewPlan = previewPlan;
        this.previewGeneration.incrementAndGet();
        this.completedTaskResult = null;
        this.clearPayloadCache();
        this.previewFrames.clear();
        this.currentPreviewIndex = -1;
        this.requestedPreviewIndex = -1;
        this.playing = false;
        this.completedMainLoops = 0;
        this.lastFailureMessage = null;

        if (analysisResult != null) {
            AfmaFrameIndex frameIndex = analysisResult.plan().getFrameIndex();
            int introCount = frameIndex.getIntroFrames().size();
            int mainCount = frameIndex.getFrames().size();
            for (int i = 0; i < introCount; i++) {
                this.previewFrames.add(new PreviewFrameRef(true, i));
            }
            for (int i = 0; i < mainCount; i++) {
                this.previewFrames.add(new PreviewFrameRef(false, i));
            }
        }

        if (this.previewFrames.isEmpty()) {
            this.clearSurface();
            return;
        }

        this.seekToFrame(0);
    }

    public boolean hasPreview() {
        return (this.analysisResult != null) && !this.previewFrames.isEmpty() && (this.textureLocation != null);
    }

    public boolean isPlaying() {
        return this.playing;
    }

    public void setPlaying(boolean playing) {
        if (this.previewFrames.isEmpty()) {
            this.playing = false;
            return;
        }
        if (playing && (this.currentPreviewIndex < 0)) {
            this.seekToFrame(0);
        }
        this.playing = playing;
        this.nextFrameAtMs = System.currentTimeMillis() + Math.max(1L, this.getCurrentFrameDurationMs());
    }

    public void togglePlaying() {
        this.setPlaying(!this.playing);
    }

    public int getTimelineSize() {
        return this.previewFrames.size();
    }

    public int getCurrentTimelineIndex() {
        return this.currentPreviewIndex;
    }

    public long getCurrentFrameDurationMs() {
        AfmaCreatorAnalysisResult result = this.analysisResult;
        if ((result == null) || (this.currentPreviewIndex < 0) || (this.currentPreviewIndex >= this.previewFrames.size())) {
            return 0L;
        }
        PreviewFrameRef ref = this.previewFrames.get(this.currentPreviewIndex);
        return result.plan().getMetadata().getFrameTimeForFrame(ref.sequenceIndex(), ref.intro());
    }

    public @NotNull String getCurrentFrameLabel() {
        if ((this.currentPreviewIndex < 0) || (this.currentPreviewIndex >= this.previewFrames.size())) {
            return "-";
        }
        PreviewFrameRef ref = this.previewFrames.get(this.currentPreviewIndex);
        return (ref.intro() ? "Intro " : "Main ") + "#" + (ref.sequenceIndex() + 1);
    }

    public @Nullable ResourceLocation getTextureLocation() {
        return this.textureLocation;
    }

    public @Nullable AfmaCreatorAnalysisResult getAnalysisResult() {
        return this.analysisResult;
    }

    public @Nullable String getLastFailureMessage() {
        return this.lastFailureMessage;
    }

    public int getCanvasWidth() {
        AfmaCreatorAnalysisResult result = this.analysisResult;
        return (result != null) ? result.plan().getMetadata().getCanvasWidth() : 0;
    }

    public int getCanvasHeight() {
        AfmaCreatorAnalysisResult result = this.analysisResult;
        return (result != null) ? result.plan().getMetadata().getCanvasHeight() : 0;
    }

    public void stepPrevious() {
        if (this.previewFrames.isEmpty()) return;
        this.seekToFrame(Math.max(0, this.currentPreviewIndex - 1));
    }

    public void stepNext() {
        if (this.previewFrames.isEmpty()) return;
        int baseIndex = (this.currentPreviewIndex >= 0) ? this.currentPreviewIndex : 0;
        this.seekToFrame(Math.min(this.previewFrames.size() - 1, baseIndex + 1));
    }

    public void seekToProgress(double progress) {
        if (this.previewFrames.isEmpty()) return;
        progress = Math.max(0.0D, Math.min(1.0D, progress));
        int index = (int) Math.round(progress * Math.max(0, this.previewFrames.size() - 1));
        this.seekToFrame(index);
    }

    public void seekToFrame(int index) {
        if (this.previewFrames.isEmpty()) return;
        final int normalizedIndex = Math.max(0, Math.min(this.previewFrames.size() - 1, index));
        this.requestedPreviewIndex = normalizedIndex;
        this.lastFailureMessage = null;

        if (this.previewPlan == null) {
            this.lastFailureMessage = "AFMA preview data is not ready yet.";
            this.playing = false;
            return;
        }

        int generation = this.previewGeneration.incrementAndGet();
        this.previewExecutor.execute(() -> this.runPreviewTask(generation, normalizedIndex));
    }

    public void tick() {
        this.applyCompletedPreviewIfAvailable();

        if (!this.playing || this.previewFrames.isEmpty()) {
            return;
        }

        if (this.currentPreviewIndex < 0) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now < this.nextFrameAtMs) {
            return;
        }

        if ((this.currentPreviewIndex + 1) < this.previewFrames.size()) {
            this.seekToFrame(this.currentPreviewIndex + 1);
            return;
        }

        AfmaCreatorAnalysisResult result = this.analysisResult;
        if (result == null) {
            this.playing = false;
            return;
        }

        AfmaFrameIndex frameIndex = result.plan().getFrameIndex();
        int introCount = frameIndex.getIntroFrames().size();
        int mainCount = frameIndex.getFrames().size();
        if ((introCount + mainCount) <= 0) {
            this.playing = false;
            return;
        }

        int loopCount = result.plan().getMetadata().getLoopCount();
        if ((loopCount > 0) && ((this.completedMainLoops + 1) >= loopCount)) {
            this.playing = false;
            return;
        }

        this.completedMainLoops++;
        this.seekToFrame(mainCount > 0 ? introCount : 0);
    }

    protected void runPreviewTask(int generation, int previewIndex) {
        try {
            PreviewSurfaceSnapshot snapshot = this.buildPreviewSnapshot(previewIndex, generation);
            if (generation != this.previewGeneration.get()) {
                return;
            }
            this.completedTaskResult = new PreviewTaskResult(generation, snapshot, null);
        } catch (Throwable throwable) {
            if (generation != this.previewGeneration.get()) {
                return;
            }
            this.completedTaskResult = new PreviewTaskResult(generation, null, throwable);
        }
    }

    protected @NotNull PreviewSurfaceSnapshot buildPreviewSnapshot(int previewIndex, int generation) throws IOException {
        AfmaCreatorAnalysisResult result = Objects.requireNonNull(this.analysisResult, "AFMA preview analysis result is missing");
        AfmaEncodePlan plan = Objects.requireNonNull(this.previewPlan, "AFMA preview plan is missing");
        AfmaMetadata metadata = plan.getMetadata();

        PreviewFrameRef target = this.previewFrames.get(previewIndex);
        List<AfmaFrameDescriptor> frames = target.intro() ? plan.getFrameIndex().getIntroFrames() : plan.getFrameIndex().getFrames();
        if (frames.isEmpty()) {
            throw new IOException("AFMA preview frame sequence is empty.");
        }

        int[] canvasPixels = new int[metadata.getCanvasWidth() * metadata.getCanvasHeight()];
        int keyframe = this.findNearestKeyframe(frames, target.sequenceIndex());
        this.applyDescriptorToPixels(canvasPixels, metadata.getCanvasWidth(), metadata.getCanvasHeight(), frames.get(keyframe), generation);
        for (int i = keyframe + 1; i <= target.sequenceIndex(); i++) {
            this.applyDescriptorToPixels(canvasPixels, metadata.getCanvasWidth(), metadata.getCanvasHeight(), frames.get(i), generation);
        }

        long delay = result.plan().getMetadata().getFrameTimeForFrame(target.sequenceIndex(), target.intro());
        return new PreviewSurfaceSnapshot(previewIndex, metadata.getCanvasWidth(), metadata.getCanvasHeight(), canvasPixels, delay);
    }

    protected void applyDescriptorToPixels(@NotNull int[] canvasPixels, int canvasWidth, int canvasHeight,
                                           @NotNull AfmaFrameDescriptor descriptor, int generation) throws IOException {
        AfmaFrameOperationType type = Objects.requireNonNull(descriptor.getType(), "AFMA preview descriptor type was NULL");
        switch (type) {
            case SAME -> {
            }
            case FULL -> this.copyPixelFrame(this.loadPayloadFrame(Objects.requireNonNull(descriptor.getPath()), generation), canvasPixels, canvasWidth, 0, 0);
            case DELTA_RECT -> this.copyPixelFrame(this.loadPayloadFrame(Objects.requireNonNull(descriptor.getPath()), generation), canvasPixels, canvasWidth, descriptor.getX(), descriptor.getY());
            case COPY_RECT_PATCH -> {
                AfmaCopyRect copyRect = Objects.requireNonNull(descriptor.getCopy(), "AFMA preview copy rect is missing");
                this.copyRectMemmove(canvasPixels, canvasWidth, canvasHeight, copyRect);

                AfmaPatchRegion patch = descriptor.getPatch();
                if ((patch != null) && (patch.getPath() != null)) {
                    this.copyPixelFrame(this.loadPayloadFrame(patch.getPath(), generation), canvasPixels, canvasWidth, patch.getX(), patch.getY());
                }
            }
        }
    }

    protected void copyRectMemmove(@NotNull int[] canvasPixels, int canvasWidth, int canvasHeight, @NotNull AfmaCopyRect copyRect) {
        int width = copyRect.getWidth();
        int height = copyRect.getHeight();
        if ((width <= 0) || (height <= 0)) {
            return;
        }

        int startY = (copyRect.getDstY() > copyRect.getSrcY()) ? (height - 1) : 0;
        int endY = (copyRect.getDstY() > copyRect.getSrcY()) ? -1 : height;
        int stepY = (copyRect.getDstY() > copyRect.getSrcY()) ? -1 : 1;

        for (int localY = startY; localY != endY; localY += stepY) {
            int srcY = copyRect.getSrcY() + localY;
            int dstY = copyRect.getDstY() + localY;
            if ((srcY < 0) || (srcY >= canvasHeight) || (dstY < 0) || (dstY >= canvasHeight)) {
                continue;
            }

            int srcIndex = (srcY * canvasWidth) + copyRect.getSrcX();
            int dstIndex = (dstY * canvasWidth) + copyRect.getDstX();
            System.arraycopy(canvasPixels, srcIndex, canvasPixels, dstIndex, width);
        }
    }

    protected void copyPixelFrame(@NotNull AfmaPixelFrame frame, @NotNull int[] canvasPixels, int canvasWidth, int dstX, int dstY) {
        for (int y = 0; y < frame.getHeight(); y++) {
            int dstRowStart = ((dstY + y) * canvasWidth) + dstX;
            for (int x = 0; x < frame.getWidth(); x++) {
                canvasPixels[dstRowStart + x] = frame.getPixelRGBA(x, y);
            }
        }
    }

    protected @NotNull AfmaPixelFrame loadPayloadFrame(@NotNull String payloadPath, int generation) throws IOException {
        synchronized (this.payloadFrameCache) {
            AfmaPixelFrame cached = this.payloadFrameCache.get(payloadPath);
            if (cached != null) {
                return cached;
            }
        }

        if (generation != this.previewGeneration.get()) {
            throw new IOException("AFMA preview payload decode was invalidated by a newer request.");
        }

        AfmaEncodePlan plan = Objects.requireNonNull(this.previewPlan, "AFMA preview plan is missing");
        byte[] payloadBytes = plan.getPayloads().get(payloadPath);
        if (payloadBytes == null) {
            throw new IOException("AFMA preview payload is missing: " + payloadPath);
        }

        AfmaPixelFrame decodedFrame = this.decodePayloadFrame(payloadBytes, payloadPath);
        synchronized (this.payloadFrameCache) {
            AfmaPixelFrame cached = this.payloadFrameCache.get(payloadPath);
            if (cached != null) {
                decodedFrame.close();
                return cached;
            }
            this.payloadFrameCache.put(payloadPath, decodedFrame);
            return decodedFrame;
        }
    }

    protected @NotNull AfmaPixelFrame decodePayloadFrame(@NotNull byte[] payloadBytes, @NotNull String payloadPath) throws IOException {
        try (ByteArrayInputStream input = new ByteArrayInputStream(payloadBytes)) {
            BufferedImage image = ImageIO.read(input);
            if (image == null) {
                throw new IOException("Failed to decode AFMA preview payload: " + payloadPath);
            }

            BufferedImage normalized = image;
            if ((image.getType() != BufferedImage.TYPE_INT_ARGB) && (image.getType() != BufferedImage.TYPE_INT_RGB)) {
                int targetType = image.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
                normalized = new BufferedImage(image.getWidth(), image.getHeight(), targetType);
                Graphics2D graphics = normalized.createGraphics();
                try {
                    graphics.drawImage(image, 0, 0, null);
                } finally {
                    graphics.dispose();
                }
            }

            int width = normalized.getWidth();
            int height = normalized.getHeight();
            int[] pixels = new int[width * height];
            normalized.getRGB(0, 0, width, height, pixels, 0, width);
            return new AfmaPixelFrame(width, height, pixels);
        }
    }

    protected int findNearestKeyframe(@NotNull List<AfmaFrameDescriptor> frames, int targetIndex) {
        for (int i = targetIndex; i >= 0; i--) {
            AfmaFrameDescriptor descriptor = frames.get(i);
            if ((descriptor != null) && descriptor.isKeyframe()) {
                return i;
            }
        }
        return 0;
    }

    protected void applyCompletedPreviewIfAvailable() {
        PreviewTaskResult result = this.completedTaskResult;
        if (result == null) {
            return;
        }
        if (result.generation != this.previewGeneration.get()) {
            this.completedTaskResult = null;
            return;
        }

        this.completedTaskResult = null;
        if (result.failure != null) {
            this.playing = false;
            this.lastFailureMessage = (result.failure.getMessage() != null) ? result.failure.getMessage() : "AFMA preview reconstruction failed.";
            LOGGER.error("[FANCYMENU] AFMA creator preview failed while preparing frame {}", this.requestedPreviewIndex, result.failure);
            return;
        }

        PreviewSurfaceSnapshot snapshot = result.snapshot;
        if (snapshot == null) {
            return;
        }

        try {
            this.ensureSurface(snapshot.width, snapshot.height);
            NativeImage activeCanvas = Objects.requireNonNull(this.canvas, "AFMA preview canvas was NULL");
            int pixelIndex = 0;
            for (int y = 0; y < snapshot.height; y++) {
                for (int x = 0; x < snapshot.width; x++) {
                    activeCanvas.setPixelRGBA(x, y, snapshot.pixels[pixelIndex++]);
                }
            }
            this.uploadSurface();
            this.currentPreviewIndex = snapshot.previewIndex;
            this.nextFrameAtMs = System.currentTimeMillis() + Math.max(1L, snapshot.delayMs);
            this.lastFailureMessage = null;
        } catch (Exception ex) {
            this.playing = false;
            this.lastFailureMessage = (ex.getMessage() != null) ? ex.getMessage() : "AFMA preview upload failed.";
            LOGGER.error("[FANCYMENU] AFMA creator preview failed while applying frame {}", snapshot.previewIndex, ex);
        }
    }

    protected void ensureSurface(int width, int height) {
        if ((this.canvas != null) && (this.canvas.getWidth() == width) && (this.canvas.getHeight() == height) && (this.texture != null) && (this.textureLocation != null)) {
            return;
        }

        this.clearSurface();
        this.canvas = new NativeImage(width, height, true);
        this.texture = new DynamicTexture(this.canvas);
        this.textureLocation = Minecraft.getInstance().getTextureManager().register("fancymenu_afma_creator_preview", this.texture);
    }

    protected void uploadSurface() {
        if (this.texture != null) {
            this.texture.upload();
        }
    }

    protected void clearPayloadCache() {
        synchronized (this.payloadFrameCache) {
            for (AfmaPixelFrame frame : this.payloadFrameCache.values()) {
                CloseableUtils.closeQuietly(frame);
            }
            this.payloadFrameCache.clear();
        }
    }

    protected void clearSurface() {
        this.clearPayloadCache();
        if (this.textureLocation != null) {
            Minecraft.getInstance().getTextureManager().release(this.textureLocation);
            this.textureLocation = null;
        }
        if (this.texture != null) {
            try {
                this.texture.close();
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to close AFMA creator preview texture", ex);
            }
        }
        this.texture = null;
        if (this.canvas != null) {
            try {
                this.canvas.close();
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to close AFMA creator preview canvas", ex);
            }
        }
        this.canvas = null;
    }

    @Override
    public void close() {
        this.playing = false;
        this.previewGeneration.incrementAndGet();
        this.previewExecutor.shutdownNow();
        this.clearSurface();
        this.analysisResult = null;
        this.previewPlan = null;
        this.previewFrames.clear();
        this.currentPreviewIndex = -1;
        this.requestedPreviewIndex = -1;
        this.completedTaskResult = null;
    }

    protected record PreviewFrameRef(boolean intro, int sequenceIndex) {
    }

    protected record PreviewSurfaceSnapshot(int previewIndex, int width, int height, @NotNull int[] pixels, long delayMs) {
    }

    protected record PreviewTaskResult(int generation, @Nullable PreviewSurfaceSnapshot snapshot, @Nullable Throwable failure) {
    }

}
