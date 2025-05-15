package de.keksuccino.fancymenu.customization.element.elements.video.mcef;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiBaseScreen;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.CloseableUtils;
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
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MCEFVideoElement extends AbstractElement implements Closeable {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

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
    protected volatile long lastRenderTickTime = -1L;
    protected volatile ScheduledFuture<?> garbageChecker = EXECUTOR.scheduleAtFixedRate(() -> {
        if (this.initialized && (this.lastRenderTickTime != -1) && ((this.lastRenderTickTime + 2000) < System.currentTimeMillis())) {
            this.resetElement();
        }
    }, 0, 100, TimeUnit.MILLISECONDS);
    protected boolean triedRestore = false;
    protected volatile boolean closed = false;

    public MCEFVideoElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (this.closed) {
            LOGGER.error("[FANCYMENU] Tried to render closed MCEFVideoElement: " + this.getInstanceIdentifier());
            return;
        }

        if (this.shouldRender()) {

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
                LOGGER.info("[FANCYMENU] Creating video player with dimensions: " + w + "x" + h + " at " + x + "," + y);
                playerId = videoManager.createPlayer(x, y, w, h);
                if (playerId != null) {
                    videoPlayer = videoManager.getPlayer(playerId);
                    if (videoPlayer != null) {
                        LOGGER.info("[FANCYMENU] Created video player");
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
                // Just update position and size directly - fillScreen is handled automatically
                this.videoPlayer.setPosition(x, y);
                this.videoPlayer.setSize(w, h);
                // The video content layout is now handled by the HTML/CSS based on the fillScreen flag
            }
            this.lastAbsoluteX = x;
            this.lastAbsoluteY = y;
            this.lastAbsoluteWidth = w;
            this.lastAbsoluteHeight = h;

            String finalVideoUrl = null;
            if (this.rawVideoUrlSource != null) {
                finalVideoUrl = PlaceholderParser.replacePlaceholders(this.rawVideoUrlSource.getSourceWithoutPrefix());
            }
            // Check if the video URL has changed since last time
            boolean videoUrlChanged = !Objects.equals(finalVideoUrl, this.lastFinalUrl);
            this.lastFinalUrl = finalVideoUrl;
            if (videoUrlChanged && (finalVideoUrl != null)) {
                LOGGER.info("[FANCYMENU] Video URL changed to: " + finalVideoUrl);

                // Stop any existing video before loading new one
                this.videoPlayer.stop();

                // Convert to URI format if it's a file
                try {
                    File videoFile = new File(finalVideoUrl);
                    if (videoFile.exists()) {
                        String videoUri = videoFile.toURI().toString();
                        LOGGER.info("[FANCYMENU] Using video URI: " + videoUri);

                        // Load the video
                        this.videoPlayer.loadVideo(videoUri);

                        // With the improved player, we can call play() immediately after loadVideo()
                        // The JS will queue the play request and execute it when the video is ready
                        this.videoPlayer.play();
                    } else {
                        // Try loading the URL as-is
                        LOGGER.info("[FANCYMENU] File not found, trying URL directly: " + finalVideoUrl);
                        this.videoPlayer.loadVideo(finalVideoUrl);
                        this.videoPlayer.play();
                    }
                } catch (Exception e) {
                    LOGGER.error("[FANCYMENU] Error processing video URL", e);
                }
            }

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
            this.getMemory().clear();
            this.disposePlayer();
        }
    }

    @Override
    public void onBeforeResizeScreen() {
        super.onBeforeResizeScreen();
        LOGGER.info("######################################## VIDEO ELEMENT BEFORE RESIZE SCREEN !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        this.garbageChecker.cancel(true);
        this.trySaveToMemory();
    }

    @Override
    public void onCloseScreen(@Nullable Screen closedScreen, @Nullable Screen newScreen) {
        super.onCloseScreen(closedScreen, newScreen);
        LOGGER.info("######################################## VIDEO ELEMENT CLOSE SCREEN !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        if ((closedScreen instanceof CustomGuiBaseScreen c1) && (newScreen instanceof CustomGuiBaseScreen c2)) {
            if (Objects.equals(c1.getIdentifier(), c2.getIdentifier())) {
                this.garbageChecker.cancel(true);
                this.trySaveToMemory();
                return;
            }
        } else if ((closedScreen != null) && (newScreen != null) && Objects.equals(closedScreen.getClass(), newScreen.getClass())) {
            this.garbageChecker.cancel(true);
            this.trySaveToMemory();
            return;
        }
        this.closeQuietly();
    }

    @Override
    public void onBecomeInvisible() {
        super.onBecomeInvisible();
        LOGGER.info("######################################## VIDEO ELEMENT BECOMES INVISIBLE!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        this.resetElement();
    }

    /**
     * @param volume Value between 0.0 and 1.0.
     */
    protected void setVolume(float volume, boolean updatePlayer) {
        if (this.closed) {
            LOGGER.error("[FANCYMENU] Tried to set volume of closed MCEFVideoElement: " + this.getInstanceIdentifier());
            return;
        }
        if (volume > 1.0F) volume = 1.0F;
        if (volume < 0.0F) volume = 0.0F;
        this.volume = volume;
        if ((this.videoPlayer != null)) {
            float actualVolume = this.volume;
            float masterVolume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER);
            float soundSourceVolume = Minecraft.getInstance().options.getSoundSourceVolume(this.soundSource);
            if (this.soundSource != SoundSource.MASTER) {
                soundSourceVolume *= masterVolume;
            }
            actualVolume *= soundSourceVolume;
            this.cachedActualVolume = actualVolume;
            if (updatePlayer) {
                this.videoPlayer.setVolume(Math.min(1.0F, Math.max(0.0F, actualVolume)));
            }
        }
    }

    protected void updateVolume() {
        this.setVolume(this.volume, false);
    }

    public void disposePlayer() {
        if ((this.videoManager != null) && (this.playerId != null) && (this.videoPlayer != null)) {
            this.videoPlayer.stop();
            this.videoManager.removePlayer(this.playerId);
        }
    }

    public void resetElement() {
        if (this.closed) {
            LOGGER.error("[FANCYMENU] Tried to reset closed MCEFVideoElement: " + this.getInstanceIdentifier());
            return;
        }
        if (this.initialized) {
            this.disposePlayer();
        }
        this.initialized = false;
        this.videoPlayer = null;
        this.playerId = null;
        this.lastFinalUrl = null;
        this.lastAbsoluteWidth = -10000;
        this.lastAbsoluteHeight = -10000;
        this.lastAbsoluteX = -10000;
        this.lastAbsoluteY = -10000;
    }

    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void close() throws IOException {
        this.closed = true;
        this.getMemory().clear();
        this.garbageChecker.cancel(true);
        this.disposePlayer();
    }

    public void closeQuietly() {
        CloseableUtils.closeQuietly(this);
    }

}