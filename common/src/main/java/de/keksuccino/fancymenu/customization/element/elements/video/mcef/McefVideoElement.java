package de.keksuccino.fancymenu.customization.element.elements.video.mcef;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiBaseScreen;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.video.IVideoElement;
import de.keksuccino.fancymenu.customization.element.elements.video.VideoElementController;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.mcef.MCEFUtil;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.video.mcef.MCEFVideoManager;
import de.keksuccino.fancymenu.util.rendering.video.mcef.MCEFVideoPlayer;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.*;
import java.io.File;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class MCEFVideoElement extends AbstractElement implements IVideoElement {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static final DrawableColor MISSING_MCEF_COLOR = DrawableColor.of(Color.RED);

    @Nullable
    public ResourceSource rawVideoUrlSource = null;
    public boolean loop = false;
    /** Value between 0.0 and 1.0 **/
    public float volume = 1.0F;
    @NotNull
    public SoundSource soundSource = SoundSource.MASTER;

    protected volatile boolean initialized = false;
    protected final MCEFVideoManager videoManager = MCEFVideoManager.getInstance();
    protected MCEFVideoPlayer videoPlayer = null;
    protected String playerId = null;

    protected String lastFinalUrl = null;
    protected int lastAbsoluteWidth = -10000;
    protected int lastAbsoluteHeight = -10000;
    protected int lastAbsoluteX = -10000;
    protected int lastAbsoluteY = -10000;
    protected Boolean lastLoop = null;
    protected float cachedActualVolume = -10000F;
    protected float lastCachedActualVolume = -11000F;
    protected Boolean lastPausedState = null;
    protected volatile long lastRenderTickTime = -1L;
    protected final AtomicReference<Float> cachedDuration = new AtomicReference<>(0F);
    protected final AtomicReference<Float> cachedPlayTime = new AtomicReference<>(0F);
    protected volatile ScheduledFuture<?> garbageChecker = EXECUTOR.scheduleAtFixedRate(() -> {
        if (this.initialized && (this.lastRenderTickTime != -1) && ((this.lastRenderTickTime + 11000) < System.currentTimeMillis())) {
            this.resetElement();
        }
    }, 0, 100, TimeUnit.MILLISECONDS);
    protected volatile ScheduledFuture<?> asyncTicker = EXECUTOR.scheduleAtFixedRate(() -> {
        if (this.initialized) {
            this.cachedDuration.set(this._getDuration());
            this.cachedPlayTime.set(this._getPlayTime());
        }
    }, 0, 900, TimeUnit.MILLISECONDS);
    protected boolean triedRestore = false;

    public MCEFVideoElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (this.shouldRender()) {

            if (!MCEFUtil.isMCEFLoaded()) {
                graphics.fill(this.getAbsoluteX(), this.getAbsoluteY(), this.getAbsoluteX() + this.getAbsoluteWidth(), this.getAbsoluteY() + this.getAbsoluteHeight(), MISSING_MCEF_COLOR.getColorInt());
                graphics.drawCenteredString(Minecraft.getInstance().font, "§lMCEF IS NOT INSTALLED! PLEASE DOWNLOAD FROM CURSEFORGE!", (this.getAbsoluteX() + this.getAbsoluteWidth()) / 2, (this.getAbsoluteY() + this.getAbsoluteHeight()) / 2, -1);
                return;
            }

            this.lastRenderTickTime = System.currentTimeMillis();

            // Get the current dimensions and position
            int x = this.getAbsoluteX();
            int y = this.getAbsoluteY();
            int w = this.getAbsoluteWidth();
            int h = this.getAbsoluteHeight();

            if (!this.triedRestore) {
                this.triedRestore = true;
                this.tryRestoreFromMemory();
            }

            if (!this.initialized) {
                this.initialized = true;
                playerId = videoManager.createPlayer(x, y, w, h);
                if (playerId != null) {
                    videoPlayer = videoManager.getPlayer(playerId);
                    if (videoPlayer != null) {
                        videoPlayer.setFillScreen(true); // Enable fill screen by default
                    }
                }
            }

            if (this.videoPlayer == null) return;

            this.updateVolume();
            if ((this.lastCachedActualVolume == -11000F) || (this.cachedActualVolume != this.lastCachedActualVolume)) {
                this.setVolume(this.volume, true);
            }
            this.lastCachedActualVolume = this.cachedActualVolume;

            if ((this.lastLoop == null) || !Objects.equals(this.loop, this.lastLoop)) {
                this.videoPlayer.setLooping(this.loop);
            }
            this.lastLoop = this.loop;

            // Update size and position of player if needed
            if ((this.lastAbsoluteX != x) || (this.lastAbsoluteY != y) || (this.lastAbsoluteWidth != w) || (this.lastAbsoluteHeight != h)) {
                this.videoPlayer.setPosition(x, y);
                this.videoPlayer.setSize(w, h);
            }
            this.lastAbsoluteX = x;
            this.lastAbsoluteY = y;
            this.lastAbsoluteWidth = w;
            this.lastAbsoluteHeight = h;

            boolean pausedState = this.getControllerPausedState();

            String finalVideoUrl = null;
            if (this.rawVideoUrlSource != null) {
                finalVideoUrl = PlaceholderParser.replacePlaceholders(this.rawVideoUrlSource.getSourceWithoutPrefix());
            }
            // Check if the video URL has changed since last time
            boolean videoUrlChanged = !Objects.equals(finalVideoUrl, this.lastFinalUrl);
            this.lastFinalUrl = finalVideoUrl;
            if (videoUrlChanged && (finalVideoUrl != null)) {
                // Stop any existing video before loading new one
                this.videoPlayer.stop();
                // Convert to URI format if it's a file
                try {
                    File videoFile = new File(finalVideoUrl);
                    if (videoFile.exists()) {
                        String videoUri = videoFile.toURI().toString();
                        this.videoPlayer.loadVideo(videoUri);
                        if (!pausedState) this.videoPlayer.play();
                    } else {
                        // Try loading the URL as-is
                        this.videoPlayer.loadVideo(finalVideoUrl);
                        if (!pausedState) this.videoPlayer.play();
                    }
                } catch (Exception e) {
                }
            }

            if ((this.lastPausedState == null) || !Objects.equals(pausedState, this.lastPausedState)) {
                if (pausedState) {
                    this.videoPlayer.pause();
                } else {
                    this.videoPlayer.play();
                }
            }
            this.lastPausedState = pausedState;

            RenderSystem.enableBlend();

            // Always draw black background
            graphics.fill(x, y, x + w, y + h, DrawableColor.BLACK.getColorIntWithAlpha(this.opacity));

            this.videoPlayer.setOpacity(this.opacity);

            if (finalVideoUrl != null) {
                this.videoPlayer.render(graphics, mouseX, mouseY, partial);
            }

            RenderSystem.disableBlend();

        }

    }

    protected void tryRestoreFromMemory() {
        if (this.getMemory().hasProperty("video_player") && this.getMemory().hasProperty("player_id") && this.getMemory().hasProperty("last_final_url") && (this.getMemory().hasProperty("save_timestamp"))) {
            Long saveTimestamp = Objects.requireNonNullElse(this.getMemory().getProperty("save_timestamp", Long.class), -1L);
            if ((saveTimestamp + 10000L) > System.currentTimeMillis()) {
                this.videoPlayer = this.getMemory().getProperty("video_player", MCEFVideoPlayer.class);
                this.playerId = this.getMemory().getStringProperty("player_id");
                this.lastFinalUrl = this.getMemory().getStringProperty("last_final_url");
                this.initialized = true;
            } else {
                this.getMemory().clear();
            }
        } else {
            this.getMemory().clear();
        }
    }

    protected void trySaveToMemory() {
        if ((this.videoPlayer != null) && (this.playerId != null) && this.initialized) {
            this.getMemory().putProperty("save_timestamp", System.currentTimeMillis());
            this.getMemory().putProperty("video_player", this.videoPlayer);
            this.getMemory().putProperty("player_id", this.playerId);
            this.getMemory().putProperty("last_final_url", this.lastFinalUrl);
        } else {
            this.disposePlayer();
        }
    }

    @Override
    public void afterConstruction() {
        if (!VideoElementController.hasMetaFor(this.getInstanceIdentifier())) VideoElementController.putMeta(this.getInstanceIdentifier(), new VideoElementController.VideoElementMeta(this.getInstanceIdentifier(), 1.0F, false));
    }

    @Override
    public void onBeforeResizeScreen() {
        super.onBeforeResizeScreen();
        this.garbageChecker.cancel(true);
        this.asyncTicker.cancel(true);
        this.trySaveToMemory();
    }

    @Override
    public void onCloseScreen(@Nullable Screen closedScreen, @Nullable Screen newScreen) {
        super.onCloseScreen(closedScreen, newScreen);
        if ((closedScreen instanceof CustomGuiBaseScreen c1) && (newScreen instanceof CustomGuiBaseScreen c2)) {
            if (Objects.equals(c1.getIdentifier(), c2.getIdentifier())) {
                this.garbageChecker.cancel(true);
                this.asyncTicker.cancel(true);
                this.trySaveToMemory();
                return;
            }
        } else if ((closedScreen != null) && (newScreen != null) && Objects.equals(closedScreen.getClass(), newScreen.getClass())) {
            this.garbageChecker.cancel(true);
            this.asyncTicker.cancel(true);
            this.trySaveToMemory();
            return;
        }
        this.getMemory().clear();
        this.garbageChecker.cancel(true);
        this.asyncTicker.cancel(true);
        this.disposePlayer();
    }

    @Override
    public void onBecomeInvisible() {
        super.onBecomeInvisible();
        this.resetElement();
    }

    /**
     * @param volume Value between 0.0 and 1.0.
     */
    protected void setVolume(float volume, boolean updatePlayer) {
        volume = Math.max(0.0F, Math.min(1.0F, volume));
        this.volume = volume;
        if ((this.videoPlayer != null)) {
            float actualVolume = this.volume;
            float masterVolume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER);
            float soundSourceVolume = Minecraft.getInstance().options.getSoundSourceVolume(this.soundSource);
            if (this.soundSource != SoundSource.MASTER) {
                soundSourceVolume *= masterVolume;
            }
            actualVolume *= soundSourceVolume;
            actualVolume *= this.getControllerVolume();
            this.cachedActualVolume = actualVolume;
            if (updatePlayer) {
                this.videoPlayer.setVolume(Math.min(1.0F, Math.max(0.0F, actualVolume)));
            }
        }
    }

    protected void updateVolume() {
        this.setVolume(this.volume, false);
    }

    /**
     * Returns the volume of this element that is set in the {@link VideoElementController}.<br>
     * The controller volume is set by actions and similar things that are user-controlled in most cases.
     */
    public float getControllerVolume() {
        if (!VideoElementController.hasMetaFor(this.getInstanceIdentifier())) VideoElementController.putMeta(this.getInstanceIdentifier(), new VideoElementController.VideoElementMeta(this.getInstanceIdentifier(), 1.0F, false));
        VideoElementController.VideoElementMeta meta = VideoElementController.getMeta(this.getInstanceIdentifier());
        if (meta != null) return Math.max(0.0F, Math.min(1.0F, meta.volume));
        return 1.0F;
    }

    /**
     * Returns the paused state of this element that is set in the {@link VideoElementController}.<br>
     * The controller paused state is set by actions and similar things that are user-controlled in most cases.
     */
    public boolean getControllerPausedState() {
        if (!VideoElementController.hasMetaFor(this.getInstanceIdentifier())) VideoElementController.putMeta(this.getInstanceIdentifier(), new VideoElementController.VideoElementMeta(this.getInstanceIdentifier(), 1.0F, false));
        VideoElementController.VideoElementMeta meta = VideoElementController.getMeta(this.getInstanceIdentifier());
        if (meta == null) return false;
        return meta.paused;
    }

    public void disposePlayer() {
        if ((this.videoManager != null) && (this.playerId != null) && (this.videoPlayer != null)) {
            this.videoPlayer.stop();
            this.videoManager.removePlayer(this.playerId);
        }
    }

    public void resetElement() {
        this.disposePlayer();
        this.initialized = false;
        this.videoPlayer = null;
        this.playerId = null;
        this.lastFinalUrl = null;
        this.lastAbsoluteWidth = -10000;
        this.lastAbsoluteHeight = -10000;
        this.lastAbsoluteX = -10000;
        this.lastAbsoluteY = -10000;
        this.lastLoop = null;
        this.cachedActualVolume = -10000F;
        this.lastCachedActualVolume = -11000F;
        this.lastPausedState = null;
        this.lastRenderTickTime = -1L;
    }

    protected float _getDuration() {
        if (!this.initialized || (this.videoPlayer == null)) return 0;
        return (float) this.videoPlayer.getDuration();
    }

    protected float _getPlayTime() {
        if (!this.initialized || (this.videoPlayer == null)) return 0;
        return (float) this.videoPlayer.getCurrentTime();
    }

    @Override
    public float getDuration() {
        return this.cachedDuration.get();
    }

    @Override
    public float getPlayTime() {
        return this.cachedPlayTime.get();
    }

}