package de.keksuccino.fancymenu.util.resource.resources.video;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.fancymenu.util.watermedia.WatermediaFrameTexture;
import de.keksuccino.fancymenu.util.watermedia.WatermediaReflectionBridge;
import de.keksuccino.fancymenu.util.watermedia.WatermediaUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

@SuppressWarnings("unused")
public class Mp4Video implements IVideo {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final File TEMP_VIDEO_DIR = FileUtils.createDirectory(new File(FancyMenu.TEMP_DATA_DIR, "/watermedia_videos"));

    @Nullable
    protected volatile Object mrl;
    @Nullable
    protected volatile Object mediaPlayer;
    @Nullable
    protected ResourceLocation sourceLocation;
    @Nullable
    protected File sourceFile;
    @Nullable
    protected String sourceURL;
    @Nullable
    protected volatile File generatedTempFile;
    protected volatile int width = 10;
    protected volatile int height = 10;
    protected volatile AspectRatio aspectRatio = new AspectRatio(10, 10);
    protected volatile float volume = 1.0F;
    protected volatile boolean looping = false;
    protected final String uniqueId = ScreenCustomization.generateUniqueIdentifier();
    protected final ResourceLocation frameLocation = ResourceLocation.fromNamespaceAndPath("fancymenu", "watermedia_video_frame_" + this.uniqueId.toLowerCase().replace("-", ""));
    protected final WatermediaFrameTexture frameTexture = new WatermediaFrameTexture(-1);
    protected volatile boolean ready = false;
    protected volatile boolean loadingCompleted = false;
    protected volatile boolean loadingFailed = false;
    protected volatile boolean dependencyMissing = false;
    protected volatile boolean playRequested = false;
    protected volatile boolean pausedRequested = false;
    protected volatile boolean closed = false;
    protected volatile boolean playerInitTaskQueued = false;
    protected final Object playerInitLock = new Object();

    @NotNull
    public static Mp4Video location(@NotNull ResourceLocation location) {
        return location(location, null);
    }

    @NotNull
    public static Mp4Video location(@NotNull ResourceLocation location, @Nullable Mp4Video writeTo) {

        Objects.requireNonNull(location);
        Mp4Video video = (writeTo != null) ? writeTo : new Mp4Video();
        video.sourceLocation = location;
        video.initializeAsync(location.toString());

        return video;

    }

    @NotNull
    public static Mp4Video local(@NotNull File videoFile) {
        return local(videoFile, null);
    }

    @NotNull
    public static Mp4Video local(@NotNull File videoFile, @Nullable Mp4Video writeTo) {

        Objects.requireNonNull(videoFile);
        Mp4Video video = (writeTo != null) ? writeTo : new Mp4Video();
        video.sourceFile = videoFile;

        if (!videoFile.isFile()) {
            video.fail("Failed to read MP4 video from file! File not found: " + videoFile.getPath(), null);
            return video;
        }

        video.initializeAsync(videoFile.getPath());

        return video;

    }

    @NotNull
    public static Mp4Video web(@NotNull String videoURL) {
        return web(videoURL, null);
    }

    @NotNull
    public static Mp4Video web(@NotNull String videoURL, @Nullable Mp4Video writeTo) {

        Objects.requireNonNull(videoURL);
        Mp4Video video = (writeTo != null) ? writeTo : new Mp4Video();
        video.sourceURL = videoURL;

        if (!TextValidators.BASIC_URL_TEXT_VALIDATOR.get(videoURL)) {
            video.fail("Failed to read MP4 video from URL! Invalid URL: " + videoURL, null);
            return video;
        }

        video.initializeAsync(videoURL);

        return video;

    }

    @NotNull
    public static Mp4Video of(@NotNull InputStream in, @Nullable String videoName, @Nullable Mp4Video writeTo) {

        Objects.requireNonNull(in);
        Mp4Video video = (writeTo != null) ? writeTo : new Mp4Video();
        String sourceName = (videoName != null) ? videoName : "[Generic InputStream Source]";

        new Thread(() -> {
            File temp = video.writeInputStreamToTempFile(in, sourceName);
            if (temp == null) {
                video.fail("Failed to decode MP4 video from input stream: " + sourceName, null);
                return;
            }
            video.generatedTempFile = temp;
            video.sourceFile = temp;
            video.initializeInternal(sourceName);
        }).start();

        return video;

    }

    @NotNull
    public static Mp4Video of(@NotNull InputStream in) {
        return of(in, null, null);
    }

    protected Mp4Video() {
    }

    protected void initializeAsync(@NotNull String sourceName) {
        new Thread(() -> this.initializeInternal(sourceName)).start();
    }

    protected void initializeInternal(@NotNull String sourceName) {
        if (this.closed) return;
        if (!WatermediaUtil.isWatermediaLoaded()) {
            this.onDependencyMissing(sourceName);
            return;
        }
        try {
            String backendSource = this.resolveBackendSource(sourceName);
            if (backendSource == null) {
                if (!this.loadingFailed) this.fail("Failed to prepare MP4 video source for Watermedia: " + sourceName, null);
                return;
            }
            Object cachedMrl = WatermediaReflectionBridge.createMrl(backendSource);
            if (cachedMrl == null) {
                this.fail("Failed to create Watermedia MRL for MP4 video source: " + sourceName, null);
                return;
            }
            this.mrl = cachedMrl;
            this.ready = true;
            WatermediaUtil.WATERMEDIA_initialized = true;
            this.watchMrlStateAsync();
            if (this.playRequested) this.queuePlayerInitializationTask();
        } catch (Throwable ex) {
            this.fail("An error occurred while initializing MP4 video source: " + sourceName, ex);
        }
    }

    @Nullable
    protected String resolveBackendSource(@NotNull String sourceName) {
        if (this.sourceURL != null) {
            return this.sourceURL;
        }
        File cachedSourceFile = this.sourceFile;
        if (cachedSourceFile != null) {
            if (!cachedSourceFile.isFile()) {
                this.fail("MP4 video source file does not exist: " + cachedSourceFile.getPath(), null);
                return null;
            }
            return cachedSourceFile.getAbsolutePath();
        }
        ResourceLocation cachedSourceLocation = this.sourceLocation;
        if (cachedSourceLocation != null) {
            try {
                InputStream in = Minecraft.getInstance().getResourceManager().open(cachedSourceLocation);
                File temp = this.writeInputStreamToTempFile(in, sourceName);
                if (temp == null) {
                    this.fail("Failed to cache MP4 video ResourceLocation source into a temporary file: " + cachedSourceLocation, null);
                    return null;
                }
                this.generatedTempFile = temp;
                this.sourceFile = temp;
                return temp.getAbsolutePath();
            } catch (Exception ex) {
                this.fail("Failed to open MP4 video resource location: " + cachedSourceLocation, ex);
                return null;
            }
        }
        return null;
    }

    protected void watchMrlStateAsync() {
        new Thread(() -> {
            long waitStart = System.currentTimeMillis();
            while (!this.closed) {
                Object cachedMrl = this.mrl;
                if (cachedMrl == null) return;
                if (WatermediaReflectionBridge.isMrlError(cachedMrl)) {
                    this.fail("Watermedia MRL failed to resolve media source for MP4 video", null);
                    return;
                }
                if (!WatermediaReflectionBridge.isMrlBusy(cachedMrl)) {
                    this.loadingCompleted = true;
                    if (this.playRequested) this.queuePlayerInitializationTask();
                    return;
                }
                if ((waitStart + 30000L) < System.currentTimeMillis()) {
                    this.fail("Watermedia MRL timed out while resolving MP4 video source", null);
                    return;
                }
                try {
                    Thread.sleep(25);
                } catch (Exception ignored) {}
            }
        }).start();
    }

    protected void queuePlayerInitializationTask() {
        if (this.closed || this.playerInitTaskQueued) return;
        this.playerInitTaskQueued = true;
        MainThreadTaskExecutor.executeInMainThread(() -> {
            this.playerInitTaskQueued = false;
            this.createPlayerIfPossible();
        }, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
    }

    protected void createPlayerIfPossible() {
        if (this.closed || this.dependencyMissing || this.loadingFailed) return;
        if (!Minecraft.getInstance().isSameThread()) {
            this.queuePlayerInitializationTask();
            return;
        }
        if (this.mediaPlayer != null) return;
        Object cachedMrl = this.mrl;
        if (cachedMrl == null) return;
        if (WatermediaReflectionBridge.isMrlBusy(cachedMrl)) return;
        if (WatermediaReflectionBridge.isMrlError(cachedMrl)) {
            this.fail("Cannot create MP4 video player because Watermedia MRL is in error state", null);
            return;
        }
        synchronized (this.playerInitLock) {
            if (this.mediaPlayer != null || this.closed) return;
            Object createdPlayer = WatermediaReflectionBridge.createPlayer(cachedMrl, Thread.currentThread(), Minecraft.getInstance()::execute, true, true);
            if (createdPlayer == null) {
                this.fail("Failed to create Watermedia media player for MP4 video source", null);
                return;
            }
            this.mediaPlayer = createdPlayer;
            this.applyVolumeToPlayer();
            this.applyLoopingToPlayer();
            if (this.playRequested) {
                if (this.pausedRequested) {
                    WatermediaReflectionBridge.playerStartPaused(createdPlayer);
                    WatermediaReflectionBridge.playerPause(createdPlayer, true);
                } else {
                    WatermediaReflectionBridge.playerStart(createdPlayer);
                }
            } else {
                WatermediaReflectionBridge.playerStop(createdPlayer);
            }
        }
    }

    protected void applyVolumeToPlayer() {
        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer != null) {
            int volumePercent = (int) Math.max(0, Math.min(100, Math.round(this.volume * 100.0F)));
            WatermediaReflectionBridge.setPlayerVolume(cachedPlayer, volumePercent);
        }
    }

    protected void applyLoopingToPlayer() {
        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer != null) {
            WatermediaReflectionBridge.setPlayerRepeat(cachedPlayer, this.looping);
        }
    }

    protected void updateSizeFromPlayer(@NotNull Object player) {
        int newWidth = WatermediaReflectionBridge.playerWidth(player);
        int newHeight = WatermediaReflectionBridge.playerHeight(player);
        if ((newWidth > 0) && (newHeight > 0)) {
            this.width = newWidth;
            this.height = newHeight;
            this.aspectRatio = new AspectRatio(newWidth, newHeight);
        }
    }

    protected void ensureFrameTextureRegistered() {
        var textureManager = Minecraft.getInstance().getTextureManager();
        if (textureManager.getTexture(this.frameLocation, MissingTextureAtlasSprite.getTexture()) != this.frameTexture) {
            textureManager.register(this.frameLocation, this.frameTexture);
        }
    }

    @Nullable
    @Override
    public ResourceLocation getResourceLocation() {
        if (this.closed) return FULLY_TRANSPARENT_TEXTURE;
        if (this.dependencyMissing || this.loadingFailed) return MISSING_TEXTURE_LOCATION;
        if ((this.mediaPlayer == null) && this.playRequested) {
            if (Minecraft.getInstance().isSameThread()) this.createPlayerIfPossible();
            else this.queuePlayerInitializationTask();
        }
        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer == null) return MISSING_TEXTURE_LOCATION;
        this.updateSizeFromPlayer(cachedPlayer);
        int textureId = WatermediaReflectionBridge.playerTextureId(cachedPlayer);
        if (textureId <= 0) return MISSING_TEXTURE_LOCATION;
        this.frameTexture.setId(textureId);
        this.ensureFrameTextureRegistered();
        return this.frameLocation;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public @NotNull AspectRatio getAspectRatio() {
        return this.aspectRatio;
    }

    @Override
    public void play() {
        if (this.closed || this.dependencyMissing || this.loadingFailed) return;
        this.playRequested = true;
        this.pausedRequested = false;
        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer != null) {
            if (WatermediaReflectionBridge.playerIsPaused(cachedPlayer)) {
                WatermediaReflectionBridge.playerPause(cachedPlayer, false);
            } else if (!WatermediaReflectionBridge.playerIsPlaying(cachedPlayer)) {
                WatermediaReflectionBridge.playerStart(cachedPlayer);
            }
        } else {
            this.queuePlayerInitializationTask();
        }
    }

    @Override
    public boolean isPlaying() {
        if (this.closed || this.dependencyMissing || this.loadingFailed) return false;
        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer != null) {
            if (WatermediaReflectionBridge.playerIsPlaying(cachedPlayer)) return true;
            String statusName = WatermediaReflectionBridge.playerStatusName(cachedPlayer);
            if (statusName.equals("STOPPED") || statusName.equals("ENDED") || statusName.equals("ERROR")) {
                this.playRequested = false;
                return false;
            }
            if (this.playRequested && !this.pausedRequested) {
                return statusName.equals("WAITING") || statusName.equals("LOADING") || statusName.equals("BUFFERING");
            }
            return false;
        }
        return this.playRequested && !this.pausedRequested;
    }

    @Override
    public void pause() {
        if (this.closed || this.dependencyMissing || this.loadingFailed) return;
        this.pausedRequested = true;
        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer != null) {
            WatermediaReflectionBridge.playerPause(cachedPlayer, true);
        }
    }

    @Override
    public boolean isPaused() {
        if (this.closed || this.dependencyMissing || this.loadingFailed) return false;
        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer != null) return WatermediaReflectionBridge.playerIsPaused(cachedPlayer);
        return this.playRequested && this.pausedRequested;
    }

    @Override
    public void stop() {
        this.playRequested = false;
        this.pausedRequested = false;
        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer != null) {
            WatermediaReflectionBridge.playerStop(cachedPlayer);
        }
    }

    @Override
    public void setVolume(float volume) {
        this.volume = Math.max(0.0F, Math.min(1.0F, volume));
        this.applyVolumeToPlayer();
    }

    @Override
    public float getVolume() {
        return this.volume;
    }

    @Override
    public float getDuration() {
        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer == null) return 0.0F;
        long durationMs = WatermediaReflectionBridge.playerDuration(cachedPlayer);
        if (durationMs <= 0L) return 0.0F;
        return (durationMs / 1000.0F);
    }

    @Override
    public float getPlayTime() {
        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer == null) return 0.0F;
        long timeMs = WatermediaReflectionBridge.playerTime(cachedPlayer);
        if (timeMs <= 0L) return 0.0F;
        return (timeMs / 1000.0F);
    }

    @Override
    public void setLooping(boolean looping) {
        this.looping = looping;
        this.applyLoopingToPlayer();
    }

    @Override
    public boolean isLooping() {
        return this.looping;
    }

    @Override
    public @Nullable InputStream open() throws IOException {
        if (this.sourceURL != null) return WebUtils.openResourceStream(this.sourceURL);
        if (this.sourceFile != null) return new FileInputStream(this.sourceFile);
        if (this.sourceLocation != null) return Minecraft.getInstance().getResourceManager().open(this.sourceLocation);
        return null;
    }

    @Override
    public boolean isReady() {
        return !this.closed && (this.ready || this.loadingFailed || this.dependencyMissing);
    }

    @Override
    public boolean isLoadingCompleted() {
        return !this.closed && !this.loadingFailed && this.loadingCompleted;
    }

    @Override
    public boolean isLoadingFailed() {
        return this.loadingFailed;
    }

    @Override
    public void reset() {
        this.stop();
    }

    @Override
    public void close() {
        this.closed = true;
        this.playRequested = false;
        this.pausedRequested = false;
        this.looping = false;
        Object cachedPlayer = this.mediaPlayer;
        this.mediaPlayer = null;
        this.mrl = null;
        if (cachedPlayer != null) {
            WatermediaReflectionBridge.playerRelease(cachedPlayer);
        }
        try {
            Minecraft.getInstance().getTextureManager().release(this.frameLocation);
        } catch (Exception ignored) {}
        this.frameTexture.setId(-1);
        File temp = this.generatedTempFile;
        if ((temp != null) && temp.isFile()) {
            temp.delete();
        }
        this.ready = false;
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Nullable
    protected File writeInputStreamToTempFile(@NotNull InputStream in, @NotNull String sourceName) {
        File targetFile = new File(TEMP_VIDEO_DIR, "mp4_video_" + this.uniqueId.toLowerCase().replace("-", "") + "_" + System.nanoTime() + ".mp4");
        try (InputStream input = in; FileOutputStream out = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return targetFile;
        } catch (Exception ex) {
            if (targetFile.isFile()) targetFile.delete();
            LOGGER.error("[FANCYMENU] Failed to write MP4 video stream to temporary file: {}", sourceName, ex);
        }
        return null;
    }

    protected void onDependencyMissing(@NotNull String sourceName) {
        this.dependencyMissing = true;
        this.loadingFailed = true;
        this.ready = true;
        this.playRequested = false;
        LOGGER.warn("[FANCYMENU] Watermedia is not loaded, MP4 source will render as missing texture: {}", sourceName);
    }

    protected void fail(@NotNull String message, @Nullable Throwable cause) {
        this.loadingFailed = true;
        this.ready = true;
        this.playRequested = false;
        if (cause != null) LOGGER.error("[FANCYMENU] {}", message, cause);
        else LOGGER.error("[FANCYMENU] {}", message);
    }

}
