package de.keksuccino.fancymenu.customization.background.backgrounds.video.mcef;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.background.backgrounds.video.IVideoMenuBackground;
import de.keksuccino.fancymenu.customization.element.elements.video.VideoElementController;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.SerializationUtils;
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

public class MCEFVideoMenuBackground extends MenuBackground implements IVideoMenuBackground {

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

    public boolean parallaxEnabled = false;
    /** Value between 0.0 and 1.0, where 0.0 is no movement and 1.0 is maximum movement **/
    @NotNull
    public String parallaxIntensityString = "0.02";
    public float lastParallaxIntensity = -10000.0F;
    /** When TRUE, the parallax effect will move in the SAME direction as the mouse, otherwise it moves in the opposite direction **/
    public boolean invertParallax = false;

    protected volatile boolean initialized = false;
    @Nullable
    protected MCEFVideoManager videoManager = null;
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
    protected boolean pausedBySystem = false;
    protected final AtomicReference<Float> cachedDuration = new AtomicReference<>(0F);
    protected final AtomicReference<Float> cachedPlayTime = new AtomicReference<>(0F);
    protected volatile ScheduledFuture<?> garbageChecker = EXECUTOR.scheduleAtFixedRate(() -> {
        if (this.initialized && (this.lastRenderTickTime != -1) && ((this.lastRenderTickTime + 11000) < System.currentTimeMillis())) {
            this.resetBackground();
        }
    }, 0, 100, TimeUnit.MILLISECONDS);
    protected volatile ScheduledFuture<?> asyncTicker = EXECUTOR.scheduleAtFixedRate(() -> {
        if (this.initialized) {
            this.cachedDuration.set(this._getDuration());
            this.cachedPlayTime.set(this._getPlayTime());
        }
    }, 0, 900, TimeUnit.MILLISECONDS);

    public MCEFVideoMenuBackground(MenuBackgroundBuilder<MCEFVideoMenuBackground> builder) {
        super(builder);
        if (MCEFUtil.isMCEFLoaded() && MCEFUtil.MCEF_initialized) this.videoManager = MCEFVideoManager.getInstance();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.lastParallaxIntensity = SerializationUtils.deserializeNumber(Float.class, 0.02F, PlaceholderParser.replacePlaceholders(this.parallaxIntensityString));

        if (!MCEFUtil.isMCEFLoaded() || !MCEFUtil.MCEF_initialized) {
            graphics.fill(0, 0, getScreenWidth(), getScreenHeight(), MISSING_MCEF_COLOR.getColorInt());
            graphics.drawCenteredString(Minecraft.getInstance().font, "§lMCEF IS NOT INSTALLED! PLEASE DOWNLOAD FROM CURSEFORGE!", getScreenWidth() / 2, getScreenHeight() / 2, -1);
            return;
        }

        this.lastRenderTickTime = System.currentTimeMillis();

        RenderSystem.enableBlend();

        float[] parallaxOffset = calculateParallaxOffset(mouseX, mouseY);
        int x = 0;
        int y = 0;
        int w = getScreenWidth();
        int h = getScreenHeight();

        if (parallaxEnabled) {
            // Reduce the expansion amount for parallax
            w = (int)(getScreenWidth() * (1.0F + lastParallaxIntensity));
            h = (int)(getScreenHeight() * (1.0F + lastParallaxIntensity));
            // Center the expanded area and apply parallax offset
            x = -((w - getScreenWidth()) / 2) + (int)parallaxOffset[0];
            y = -((h - getScreenHeight()) / 2) + (int)parallaxOffset[1];
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

        boolean pausedState = this._isPaused();

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
                LOGGER.error("[FANCYMENU] Error processing video URL", e);
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

    protected float[] calculateParallaxOffset(int mouseX, int mouseY) {

        if (!parallaxEnabled) {
            return new float[]{0, 0};
        }

        // Calculate mouse position as a percentage from the center of the screen
        float mouseXPercent = (2.0f * mouseX / getScreenWidth()) - 1.0f;
        float mouseYPercent = (2.0f * mouseY / getScreenHeight()) - 1.0f;

        // Apply inversion if enabled
        float directionMultiplier = invertParallax ? 1.0f : -1.0f;

        // Calculate offset based on screen dimensions and center-adjusted mouse position
        float xOffset = directionMultiplier * lastParallaxIntensity * mouseXPercent * getScreenWidth() * 0.5f;
        float yOffset = directionMultiplier * lastParallaxIntensity * mouseYPercent * getScreenHeight() * 0.5f;

        return new float[]{xOffset, yOffset};

    }

    @Override
    public void onOpenScreen() {
        super.onOpenScreen();
        if (this.initialized && (this.videoPlayer != null) && this.pausedBySystem) {
            this.pausedBySystem = false;
            this.videoPlayer.play();
        }
    }

    @Override
    public void onCloseScreen(@Nullable Screen closedScreen, @Nullable Screen newScreen) {
        super.onCloseScreen(closedScreen, newScreen);
        if (this.initialized && (this.videoPlayer != null)) {
            this.pausedBySystem = true;
            this.videoPlayer.pause();
        }
    }

    @Override
    public void onAfterEnable() {
        super.onAfterEnable();
        if (this.initialized && (this.videoPlayer != null) && this.pausedBySystem) {
            this.pausedBySystem = false;
            this.videoPlayer.play();
        }
    }

    @Override
    public void onDisableOrRemove() {
        super.onDisableOrRemove();
        if (this.initialized && (this.videoPlayer != null)) {
            this.pausedBySystem = true;
            this.videoPlayer.pause();
        }
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

    public void resetBackground() {
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
        this.pausedBySystem = false;
    }

    protected float _getDuration() {
        if (!this.initialized || (this.videoPlayer == null)) return 0;
        return (float) this.videoPlayer.getDuration();
    }

    protected float _getPlayTime() {
        if (!this.initialized || (this.videoPlayer == null)) return 0;
        return (float) this.videoPlayer.getCurrentTime();
    }

    protected boolean _isPaused() {
        return (this.getControllerPausedState() || this.pausedBySystem);
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