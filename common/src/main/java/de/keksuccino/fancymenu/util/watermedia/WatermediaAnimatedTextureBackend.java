package de.keksuccino.fancymenu.util.watermedia;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.resource.RenderableResource;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.FileOutputStream;

public class WatermediaAnimatedTextureBackend implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final File TEMP_TEXTURE_DIR = FileUtils.createDirectory(new File(FancyMenu.TEMP_DATA_DIR, "/watermedia_animated_textures"));

    @NotNull
    protected final ResourceLocation frameLocation;
    @NotNull
    protected final WatermediaFrameTexture frameTexture = new WatermediaFrameTexture(-1);
    @NotNull
    protected final String logTypeName;
    @NotNull
    protected final Object playerInitLock = new Object();

    @Nullable
    protected volatile Object mrl;
    @Nullable
    protected volatile Object mediaPlayer;
    @Nullable
    protected volatile File generatedTempFile;

    protected volatile int width = 10;
    protected volatile int height = 10;
    @NotNull
    protected volatile AspectRatio aspectRatio = new AspectRatio(10, 10);
    protected volatile boolean ready = false;
    protected volatile boolean loadingCompleted = false;
    protected volatile boolean loadingFailed = false;
    protected volatile boolean dependencyMissing = false;
    protected volatile boolean playRequested = true;
    protected volatile boolean pausedRequested = false;
    protected volatile boolean closed = false;
    protected volatile boolean playerInitTaskQueued = false;
    protected volatile boolean loopForever = true;
    protected volatile long lastPlaybackTimeMs = 0L;
    protected volatile int completedLoops = 0;
    protected volatile int maxLoops = -1;

    public WatermediaAnimatedTextureBackend(@NotNull String uniqueId, @NotNull String typeName) {
        this.logTypeName = typeName.toUpperCase();
        String cleanType = typeName.toLowerCase();
        String cleanId = uniqueId.toLowerCase().replace("-", "");
        this.frameLocation = ResourceLocation.fromNamespaceAndPath("fancymenu", "watermedia_" + cleanType + "_frame_" + cleanId);
    }

    public boolean initializeFromBytes(@NotNull byte[] data, @NotNull String extension, @NotNull String sourceName) {
        if (this.closed) return false;
        if (!WatermediaUtil.isWatermediaLoaded()) {
            this.onDependencyMissing(sourceName);
            return false;
        }
        File temp = this.writeDataToTempFile(data, extension, sourceName);
        if (temp == null) {
            this.fail("Failed to create temporary source file for Watermedia " + this.logTypeName + " texture: " + sourceName, null);
            return false;
        }
        this.generatedTempFile = temp;
        return this.initializeInternal(temp.getAbsolutePath(), sourceName);
    }

    public boolean initializeFromSource(@NotNull String source, @NotNull String sourceName) {
        if (this.closed) return false;
        if (!WatermediaUtil.isWatermediaLoaded()) {
            this.onDependencyMissing(sourceName);
            return false;
        }
        return this.initializeInternal(source, sourceName);
    }

    protected boolean initializeInternal(@NotNull String source, @NotNull String sourceName) {
        if (this.closed) return false;
        try {
            Object cachedMrl = WatermediaReflectionBridge.createMrl(source);
            if (cachedMrl == null) {
                this.fail("Failed to create Watermedia MRL for " + this.logTypeName + " texture source: " + sourceName, null);
                return false;
            }
            this.mrl = cachedMrl;
            this.ready = true;
            WatermediaUtil.WATERMEDIA_initialized = true;
            this.watchMrlStateAsync(sourceName);
            if (this.playRequested) this.queuePlayerInitializationTask();
            return true;
        } catch (Throwable ex) {
            this.fail("Failed to initialize Watermedia " + this.logTypeName + " texture source: " + sourceName, ex);
            return false;
        }
    }

    protected void watchMrlStateAsync(@NotNull String sourceName) {
        new Thread(() -> {
            long waitStart = System.currentTimeMillis();
            while (!this.closed) {
                Object cachedMrl = this.mrl;
                if (cachedMrl == null) return;
                if (WatermediaReflectionBridge.isMrlError(cachedMrl)) {
                    this.fail("Watermedia MRL failed to resolve " + this.logTypeName + " texture source: " + sourceName, null);
                    return;
                }
                if (!WatermediaReflectionBridge.isMrlBusy(cachedMrl)) {
                    this.loadingCompleted = true;
                    if (this.playRequested) this.queuePlayerInitializationTask();
                    return;
                }
                if ((waitStart + 30000L) < System.currentTimeMillis()) {
                    this.fail("Watermedia MRL timed out while resolving " + this.logTypeName + " texture source: " + sourceName, null);
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
        if (this.closed || this.loadingFailed || this.dependencyMissing) return;
        if (!Minecraft.getInstance().isSameThread()) {
            this.queuePlayerInitializationTask();
            return;
        }
        if (this.mediaPlayer != null) return;
        Object cachedMrl = this.mrl;
        if (cachedMrl == null) return;
        if (WatermediaReflectionBridge.isMrlBusy(cachedMrl)) return;
        if (WatermediaReflectionBridge.isMrlError(cachedMrl)) {
            this.fail("Cannot create Watermedia player because MRL is in error state for " + this.logTypeName + " texture", null);
            return;
        }
        synchronized (this.playerInitLock) {
            if (this.mediaPlayer != null || this.closed) return;
            Object createdPlayer = WatermediaReflectionBridge.createPlayer(cachedMrl, Thread.currentThread(), Minecraft.getInstance()::execute, true, false);
            if (createdPlayer == null) {
                this.fail("Failed to create Watermedia player for " + this.logTypeName + " texture", null);
                return;
            }
            this.mediaPlayer = createdPlayer;
            this.applyRepeatMode(createdPlayer);
            this.updateSizeFromPlayer(createdPlayer);
            this.lastPlaybackTimeMs = 0L;
            this.completedLoops = 0;
            if (this.playRequested) {
                if (this.pausedRequested) {
                    WatermediaReflectionBridge.playerStartPaused(createdPlayer);
                    WatermediaReflectionBridge.playerPause(createdPlayer, true);
                } else {
                    WatermediaReflectionBridge.playerStart(createdPlayer);
                    WatermediaReflectionBridge.playerPause(createdPlayer, false);
                }
            } else {
                WatermediaReflectionBridge.playerStop(createdPlayer);
            }
        }
    }

    protected void applyRepeatMode(@Nullable Object player) {
        if (player == null) return;
        boolean repeat = this.loopForever || (this.maxLoops > 1);
        WatermediaReflectionBridge.setPlayerRepeat(player, repeat);
    }

    protected void updateSizeFromPlayer(@Nullable Object player) {
        if (player == null) return;
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
    public ResourceLocation getResourceLocation() {
        if (this.closed) return RenderableResource.FULLY_TRANSPARENT_TEXTURE;
        if (this.dependencyMissing || this.loadingFailed) return RenderableResource.FULLY_TRANSPARENT_TEXTURE;
        if (!this.loadingCompleted) return RenderableResource.FULLY_TRANSPARENT_TEXTURE;

        if ((this.mediaPlayer == null) && this.playRequested) {
            if (Minecraft.getInstance().isSameThread()) this.createPlayerIfPossible();
            else this.queuePlayerInitializationTask();
        }

        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer == null) return RenderableResource.FULLY_TRANSPARENT_TEXTURE;

        this.applyFiniteLoopStop(cachedPlayer);
        this.updateSizeFromPlayer(cachedPlayer);

        int textureId = WatermediaReflectionBridge.playerTextureId(cachedPlayer);
        if (textureId <= 0) return RenderableResource.FULLY_TRANSPARENT_TEXTURE;
        this.frameTexture.setId(textureId);
        this.ensureFrameTextureRegistered();
        return this.frameLocation;
    }

    protected void applyFiniteLoopStop(@NotNull Object player) {
        if (this.loopForever || (this.maxLoops <= 1)) return;
        long durationMs = WatermediaReflectionBridge.playerDuration(player);
        if (durationMs <= 0L) return;

        long currentTimeMs = Math.max(0L, WatermediaReflectionBridge.playerTime(player));
        long previousTimeMs = Math.max(0L, this.lastPlaybackTimeMs);
        this.lastPlaybackTimeMs = currentTimeMs;

        if (previousTimeMs > (currentTimeMs + 100L)) {
            this.completedLoops++;
            if (this.completedLoops >= (this.maxLoops - 1)) {
                WatermediaReflectionBridge.setPlayerRepeat(player, false);
            }
        }
    }

    public void setLoopCount(int loops) {
        this.maxLoops = loops;
        this.loopForever = loops <= 0;
        this.applyRepeatMode(this.mediaPlayer);
    }

    public void play() {
        if (this.closed || this.dependencyMissing || this.loadingFailed) return;
        this.playRequested = true;
        this.pausedRequested = false;
        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer != null) {
            String statusName = WatermediaReflectionBridge.playerStatusName(cachedPlayer);
            if (statusName.equals("STOPPED") || statusName.equals("ENDED") || statusName.equals("ERROR")) {
                WatermediaReflectionBridge.playerStart(cachedPlayer);
            }
            WatermediaReflectionBridge.playerPause(cachedPlayer, false);
        } else {
            this.queuePlayerInitializationTask();
        }
    }

    public boolean isPlaying() {
        if (this.closed || this.dependencyMissing || this.loadingFailed) return false;
        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer != null) {
            if (WatermediaReflectionBridge.playerIsPlaying(cachedPlayer)) return true;
            String statusName = WatermediaReflectionBridge.playerStatusName(cachedPlayer);
            return this.playRequested && !this.pausedRequested
                    && (statusName.equals("WAITING") || statusName.equals("LOADING") || statusName.equals("BUFFERING"));
        }
        return this.playRequested && !this.pausedRequested;
    }

    public void pause() {
        if (this.closed || this.dependencyMissing || this.loadingFailed) return;
        this.pausedRequested = true;
        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer != null) {
            WatermediaReflectionBridge.playerPause(cachedPlayer, true);
        }
    }

    public boolean isPaused() {
        if (this.closed || this.dependencyMissing || this.loadingFailed) return false;
        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer != null) return WatermediaReflectionBridge.playerIsPaused(cachedPlayer);
        return this.playRequested && this.pausedRequested;
    }

    public void stop() {
        if (this.closed) return;
        this.playRequested = true;
        this.pausedRequested = false;
        this.completedLoops = 0;
        this.lastPlaybackTimeMs = 0L;
        Object cachedPlayer = this.mediaPlayer;
        if (cachedPlayer != null) {
            WatermediaReflectionBridge.playerStop(cachedPlayer);
            WatermediaReflectionBridge.playerStart(cachedPlayer);
            WatermediaReflectionBridge.playerPause(cachedPlayer, false);
            this.applyRepeatMode(cachedPlayer);
        }
    }

    public void reset() {
        this.stop();
    }

    public boolean isReady() {
        return !this.closed && (this.ready || this.loadingFailed || this.dependencyMissing);
    }

    public boolean isLoadingCompleted() {
        return !this.closed && !this.loadingFailed && this.loadingCompleted;
    }

    public boolean isLoadingFailed() {
        return this.loadingFailed;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    @NotNull
    public AspectRatio getAspectRatio() {
        return this.aspectRatio;
    }

    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void close() {
        if (this.closed) return;
        this.closed = true;
        this.playRequested = false;
        this.pausedRequested = false;
        Object cachedPlayer = this.mediaPlayer;
        this.mediaPlayer = null;
        this.mrl = null;
        if (cachedPlayer != null) {
            WatermediaReflectionBridge.playerPause(cachedPlayer, true);
            WatermediaReflectionBridge.playerStop(cachedPlayer);
            WatermediaReflectionBridge.playerRelease(cachedPlayer);
        }
        try {
            Minecraft.getInstance().getTextureManager().release(this.frameLocation);
        } catch (Exception ignored) {}
        this.frameTexture.setId(-1);
        File temp = this.generatedTempFile;
        this.generatedTempFile = null;
        if ((temp != null) && temp.isFile()) {
            temp.delete();
        }
    }

    protected void onDependencyMissing(@NotNull String sourceName) {
        this.dependencyMissing = true;
        this.loadingFailed = true;
        this.ready = true;
        LOGGER.warn("[FANCYMENU] Watermedia is not loaded, {} source will use fallback decoder: {}", this.logTypeName, sourceName);
    }

    protected void fail(@NotNull String message, @Nullable Throwable cause) {
        this.loadingFailed = true;
        this.ready = true;
        if (cause != null) LOGGER.error("[FANCYMENU] {}", message, cause);
        else LOGGER.error("[FANCYMENU] {}", message);
    }

    @Nullable
    protected File writeDataToTempFile(@NotNull byte[] data, @NotNull String extension, @NotNull String sourceName) {
        String suffix = extension.startsWith(".") ? extension : "." + extension;
        File targetFile = new File(TEMP_TEXTURE_DIR, "watermedia_texture_" + System.nanoTime() + suffix);
        try (FileOutputStream out = new FileOutputStream(targetFile)) {
            out.write(data);
            return targetFile;
        } catch (Exception ex) {
            if (targetFile.isFile()) targetFile.delete();
            LOGGER.error("[FANCYMENU] Failed to write {} stream to temporary file: {}", this.logTypeName, sourceName, ex);
        }
        return null;
    }

}