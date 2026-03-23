package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaCopyRect;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameDescriptor;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaNativeImageHelper;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaPatchRegion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AfmaPreviewController implements AutoCloseable {

    private static final int MAX_DECODED_PAYLOAD_CACHE = 8;

    private @Nullable AfmaCreatorAnalysisResult analysisResult;
    private @Nullable NativeImage canvas;
    private @Nullable DynamicTexture texture;
    private @Nullable ResourceLocation textureLocation;
    private final @NotNull LinkedHashMap<String, NativeImage> payloadCache = new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, NativeImage> eldest) {
            if (this.size() > MAX_DECODED_PAYLOAD_CACHE) {
                CloseableUtils.closeQuietly(eldest.getValue());
                return true;
            }
            return false;
        }
    };
    private final @NotNull List<PreviewFrameRef> previewFrames = new ArrayList<>();
    private int currentPreviewIndex = -1;
    private boolean playing = false;
    private long nextFrameAtMs = 0L;
    private int completedMainLoops = 0;

    public void setAnalysisResult(@Nullable AfmaCreatorAnalysisResult analysisResult) {
        this.analysisResult = analysisResult;
        this.clearPayloadCache();
        this.previewFrames.clear();
        this.currentPreviewIndex = -1;
        this.playing = false;
        this.completedMainLoops = 0;

        if (analysisResult != null) {
            int introCount = analysisResult.plan().getFrameIndex().getIntroFrames().size();
            int mainCount = analysisResult.plan().getFrameIndex().getFrames().size();
            for (int i = 0; i < introCount; i++) {
                this.previewFrames.add(new PreviewFrameRef(true, i));
            }
            for (int i = 0; i < mainCount; i++) {
                this.previewFrames.add(new PreviewFrameRef(false, i));
            }
        }

        if (!this.previewFrames.isEmpty()) {
            this.seekToFrame(0);
        } else {
            this.clearSurface();
        }
    }

    public boolean hasPreview() {
        return this.analysisResult != null && !this.previewFrames.isEmpty() && this.textureLocation != null;
    }

    public boolean isPlaying() {
        return this.playing;
    }

    public void setPlaying(boolean playing) {
        if (this.previewFrames.isEmpty()) {
            this.playing = false;
            return;
        }
        if (playing && this.currentPreviewIndex < 0) {
            this.seekToFrame(0);
        }
        this.playing = playing;
        this.nextFrameAtMs = System.currentTimeMillis() + this.getCurrentFrameDurationMs();
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
        int target = Math.max(0, this.currentPreviewIndex - 1);
        this.seekToFrame(target);
    }

    public void stepNext() {
        if (this.previewFrames.isEmpty()) return;
        int target = Math.min(this.previewFrames.size() - 1, this.currentPreviewIndex + 1);
        this.seekToFrame(target);
    }

    public void seekToProgress(double progress) {
        if (this.previewFrames.isEmpty()) return;
        progress = Math.max(0.0D, Math.min(1.0D, progress));
        int index = (int) Math.round(progress * Math.max(0, this.previewFrames.size() - 1));
        this.seekToFrame(index);
    }

    public void seekToFrame(int index) {
        if (this.previewFrames.isEmpty()) return;
        index = Math.max(0, Math.min(this.previewFrames.size() - 1, index));
        try {
            if ((this.currentPreviewIndex >= 0) && (index == (this.currentPreviewIndex + 1))) {
                this.applyFrame(this.previewFrames.get(index));
            } else {
                this.reconstructTo(index);
            }
            this.currentPreviewIndex = index;
            this.nextFrameAtMs = System.currentTimeMillis() + this.getCurrentFrameDurationMs();
        } catch (Exception ex) {
            this.playing = false;
        }
    }

    public void tick() {
        if (!this.playing || this.previewFrames.isEmpty()) {
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

        int introCount = result.plan().getFrameIndex().getIntroFrames().size();
        int mainCount = result.plan().getFrameIndex().getFrames().size();
        if (mainCount <= 0) {
            this.playing = false;
            return;
        }

        int loopCount = result.plan().getMetadata().getLoopCount();
        if ((loopCount > 0) && ((this.completedMainLoops + 1) >= loopCount)) {
            this.playing = false;
            return;
        }

        this.completedMainLoops++;
        this.seekToFrame(introCount);
    }

    protected void reconstructTo(int previewIndex) throws IOException {
        AfmaCreatorAnalysisResult result = Objects.requireNonNull(this.analysisResult);
        PreviewFrameRef target = this.previewFrames.get(previewIndex);
        List<AfmaFrameDescriptor> frames = target.intro() ? result.plan().getFrameIndex().getIntroFrames() : result.plan().getFrameIndex().getFrames();
        int keyframe = this.findNearestKeyframe(frames, target.sequenceIndex());
        this.ensureSurface(result.plan().getMetadata().getCanvasWidth(), result.plan().getMetadata().getCanvasHeight());
        this.applyFullFrame(frames.get(keyframe));
        for (int i = keyframe + 1; i <= target.sequenceIndex(); i++) {
            this.applyDescriptor(frames.get(i));
        }
        this.uploadSurface();
    }

    protected void applyFrame(@NotNull PreviewFrameRef ref) throws IOException {
        AfmaCreatorAnalysisResult result = Objects.requireNonNull(this.analysisResult);
        this.ensureSurface(result.plan().getMetadata().getCanvasWidth(), result.plan().getMetadata().getCanvasHeight());
        List<AfmaFrameDescriptor> frames = ref.intro() ? result.plan().getFrameIndex().getIntroFrames() : result.plan().getFrameIndex().getFrames();
        this.applyDescriptor(frames.get(ref.sequenceIndex()));
        this.uploadSurface();
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

    protected void applyDescriptor(@NotNull AfmaFrameDescriptor descriptor) throws IOException {
        switch (Objects.requireNonNull(descriptor.getType())) {
            case FULL -> this.applyFullFrame(descriptor);
            case DELTA_RECT -> this.applyDeltaFrame(descriptor);
            case SAME -> {
            }
            case COPY_RECT_PATCH -> this.applyCopyRectPatchFrame(descriptor);
        }
    }

    protected void applyFullFrame(@NotNull AfmaFrameDescriptor descriptor) throws IOException {
        NativeImage image = this.loadPayload(Objects.requireNonNull(descriptor.getPath()));
        Objects.requireNonNull(this.canvas).copyFrom(image);
    }

    protected void applyDeltaFrame(@NotNull AfmaFrameDescriptor descriptor) throws IOException {
        NativeImage patch = this.loadPayload(Objects.requireNonNull(descriptor.getPath()));
        AfmaNativeImageHelper.copyRect(patch, 0, 0, Objects.requireNonNull(this.canvas), descriptor.getX(), descriptor.getY(), descriptor.getWidth(), descriptor.getHeight());
    }

    protected void applyCopyRectPatchFrame(@NotNull AfmaFrameDescriptor descriptor) throws IOException {
        NativeImage canvasImage = Objects.requireNonNull(this.canvas);
        AfmaCopyRect copyRect = Objects.requireNonNull(descriptor.getCopy());
        AfmaNativeImageHelper.copyRectMemmove(canvasImage, copyRect);

        AfmaPatchRegion patchRegion = descriptor.getPatch();
        if ((patchRegion != null) && (patchRegion.getPath() != null)) {
            NativeImage patch = this.loadPayload(patchRegion.getPath());
            AfmaNativeImageHelper.copyRect(patch, 0, 0, canvasImage, patchRegion.getX(), patchRegion.getY(), patchRegion.getWidth(), patchRegion.getHeight());
        }
    }

    protected @NotNull NativeImage loadPayload(@NotNull String path) throws IOException {
        NativeImage cached = this.payloadCache.get(path);
        if (cached != null) {
            return cached;
        }

        AfmaCreatorAnalysisResult result = Objects.requireNonNull(this.analysisResult);
        byte[] bytes = result.plan().getPayloads().get(path);
        if (bytes == null) {
            throw new IOException("Missing AFMA preview payload: " + path);
        }

        NativeImage image = NativeImage.read(new ByteArrayInputStream(bytes));
        this.payloadCache.put(path, image);
        return image;
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
        for (NativeImage image : this.payloadCache.values()) {
            CloseableUtils.closeQuietly(image);
        }
        this.payloadCache.clear();
    }

    protected void clearSurface() {
        this.clearPayloadCache();
        if (this.textureLocation != null) {
            Minecraft.getInstance().getTextureManager().release(this.textureLocation);
            this.textureLocation = null;
        }
        this.texture = null;
        this.canvas = null;
    }

    @Override
    public void close() {
        this.playing = false;
        this.clearSurface();
        this.analysisResult = null;
        this.previewFrames.clear();
        this.currentPreviewIndex = -1;
    }

    protected record PreviewFrameRef(boolean intro, int sequenceIndex) {
    }

}
