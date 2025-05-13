package de.keksuccino.fancymenu.customization.element.elements.video.mcef;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.video.MCEFVideoPlayer;
import de.keksuccino.fancymenu.util.rendering.video.VideoManager;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Objects;

public class McefVideoElement extends AbstractElement {

    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    public ResourceSource rawVideoUrlSource = null;
    @NotNull
    public DrawableColor imageTint = DrawableColor.of("#FFFFFF");
    public boolean repeat = false;
    public boolean nineSlice = false;
    public int nineSliceBorderX = 5;
    public int nineSliceBorderY = 5;

    protected boolean initialized = false;
    protected final VideoManager videoManager = VideoManager.getInstance();
    protected MCEFVideoPlayer videoPlayer = null;
    protected String playerId = null;

    protected String lastFinalUrl = null;
    protected int lastAbsoluteWidth = -10000;
    protected int lastAbsoluteHeight = -10000;
    protected int lastAbsoluteX = -10000;
    protected int lastAbsoluteY = -10000;
    
    // Additional fields to track player state
    protected boolean isInitialCreation = true;
    protected boolean hasLoadedVideo = false;
    protected boolean videoPlayRequested = false;

    public McefVideoElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (this.shouldRender()) {

            if (!this.initialized) {
                this.initialized = true;
                
                // Create player with proper size and position from the start
                int x = this.getAbsoluteX();
                int y = this.getAbsoluteY();
                int w = Math.max(100, this.getAbsoluteWidth());  // Ensure minimum size
                int h = Math.max(100, this.getAbsoluteHeight());
                
                LOGGER.info("[FANCYMENU] Creating video player with dimensions: " + w + "x" + h + " at " + x + "," + y);
                playerId = videoManager.createPlayer(x, y, w, h);
                
                if (playerId != null) {
                    videoPlayer = videoManager.getPlayer(playerId);
                    if (videoPlayer != null) {
                        LOGGER.info("[FANCYMENU] Created video player");

                        // Set player options immediately
                        videoPlayer.setVolume(0.1f);
                        videoPlayer.setLooping(true);
                        videoPlayer.setFillScreen(true);
                        
                        // Make sure these initial values are tracked
                        this.lastAbsoluteX = x;
                        this.lastAbsoluteY = y;
                        this.lastAbsoluteWidth = w;
                        this.lastAbsoluteHeight = h;
                        
                        // Process video URL right away - don't wait for next render
                        String finalVideoUrl = null;
                        if (this.rawVideoUrlSource != null) {
                            finalVideoUrl = PlaceholderParser.replacePlaceholders(this.rawVideoUrlSource.getSourceWithoutPrefix());
                            if (finalVideoUrl != null && !finalVideoUrl.isEmpty()) {
                                LOGGER.info("[FANCYMENU] Loading new video during initialization: " + finalVideoUrl);
                                this.lastFinalUrl = finalVideoUrl;
                                hasLoadedVideo = true;
                                
                                // Directly use the URI format for most reliable loading
                                try {
                                    File videoFile = new File(finalVideoUrl);
                                    if (videoFile.exists()) {
                                        // Convert to URI format which is more reliable
                                        final String videoUri = videoFile.toURI().toString();
                                        LOGGER.info("[FANCYMENU] Using video URI: " + videoUri);
                                        
                                        // First, stop any existing playback
                                        videoPlayer.stop();
                                        
                                        // Just load the video - play will happen on a delay
                                        videoPlayer.loadVideo(videoUri);
                                        
                                        // Schedule a play attempt with a good delay
                                        new Thread(() -> {
                                            try {
                                                Thread.sleep(1500); // Longer delay for first load
                                                LOGGER.info("[FANCYMENU] First play attempt after initialization");
                                                videoPlayer.play();
                                                
                                                // Try again after another delay if first attempt fails
                                                Thread.sleep(2000);
                                                LOGGER.info("[FANCYMENU] Second play attempt after initialization");
                                                videoPlayer.play();
                                            } catch (Exception e) {
                                                LOGGER.error("[FANCYMENU] Error in delayed play", e);
                                            }
                                        }).start();
                                    } else {
                                        // Try loading the URL as-is
                                        LOGGER.info("[FANCYMENU] File not found, trying URL directly: " + finalVideoUrl);
                                        videoPlayer.loadVideo(finalVideoUrl);
                                        
                                        // Play with a delay
                                        new Thread(() -> {
                                            try {
                                                Thread.sleep(1500);
                                                LOGGER.info("[FANCYMENU] First play attempt after initialization");
                                                videoPlayer.play();
                                                
                                                // Try again after another delay if first attempt fails
                                                Thread.sleep(2000);
                                                LOGGER.info("[FANCYMENU] Second play attempt after initialization");
                                                videoPlayer.play();
                                            } catch (Exception e) {
                                                LOGGER.error("[FANCYMENU] Error in delayed play", e);
                                            }
                                        }).start();
                                    }
                                } catch (Exception e) {
                                    LOGGER.error("[FANCYMENU] Error processing video URL during initialization", e);
                                }
                            }
                        }
                    }
                }
                
                // We've handled everything during initialization, so return from this render pass
                return;
            }

            if (this.videoPlayer == null) return;

            // First update position and size, regardless of video URL
            int x = this.getAbsoluteX();
            int y = this.getAbsoluteY();
            int w = Math.max(100, this.getAbsoluteWidth());
            int h = Math.max(100, this.getAbsoluteHeight());

            if ((this.lastAbsoluteX != x) || (this.lastAbsoluteY != y)) {
                this.videoPlayer.setPosition(x, y);
                this.lastAbsoluteX = x;
                this.lastAbsoluteY = y;
            }
            
            if ((this.lastAbsoluteWidth != w) || (this.lastAbsoluteHeight != h)) {
                this.videoPlayer.resizeToFill(w, h);
                this.lastAbsoluteWidth = w;
                this.lastAbsoluteHeight = h;
            }
            
            // Process video URL if it hasn't been processed yet
            String finalVideoUrl = null;
            if (this.rawVideoUrlSource != null) {
                finalVideoUrl = PlaceholderParser.replacePlaceholders(this.rawVideoUrlSource.getSourceWithoutPrefix());
            }
            
            // Check if the video URL has changed since last time
            boolean videoUrlChanged = !Objects.equals(finalVideoUrl, this.lastFinalUrl);
            if (videoUrlChanged && finalVideoUrl != null) {
                LOGGER.info("[FANCYMENU] Video URL changed to: " + finalVideoUrl);
                
                // Stop any existing video before loading new one
                this.videoPlayer.stop();
                videoPlayRequested = false;
                hasLoadedVideo = false;
                
                // Convert to URI format if it's a file
                try {
                    File videoFile = new File(finalVideoUrl);
                    if (videoFile.exists()) {
                        String videoUri = videoFile.toURI().toString();
                        LOGGER.info("[FANCYMENU] Using video URI: " + videoUri);
                        
                        // Load the video
                        this.videoPlayer.loadVideo(videoUri);
                        hasLoadedVideo = true;
                        
                        // Play with a delay
                        new Thread(() -> {
                            try {
                                Thread.sleep(1000);
                                LOGGER.info("[FANCYMENU] Playing video after URL change");
                                videoPlayer.play();
                                videoPlayRequested = true;
                            } catch (Exception e) {
                                LOGGER.error("[FANCYMENU] Error in delayed play", e);
                            }
                        }).start();
                    } else {
                        // Try loading the URL as-is
                        LOGGER.info("[FANCYMENU] File not found, trying URL directly: " + finalVideoUrl);
                        this.videoPlayer.loadVideo(finalVideoUrl);
                        hasLoadedVideo = true;
                        
                        // Play with a delay
                        new Thread(() -> {
                            try {
                                Thread.sleep(1000);
                                LOGGER.info("[FANCYMENU] Playing video after URL change");
                                videoPlayer.play();
                                videoPlayRequested = true;
                            } catch (Exception e) {
                                LOGGER.error("[FANCYMENU] Error in delayed play", e);
                            }
                        }).start();
                    }
                } catch (Exception e) {
                    LOGGER.error("[FANCYMENU] Error processing video URL", e);
                }
                
                this.lastFinalUrl = finalVideoUrl;
            }
            
            // If we have a video loaded but haven't requested play, try to play it periodically
            if (hasLoadedVideo && !videoPlayRequested && finalVideoUrl != null) {
                // Periodically try to play if we haven't successfully started
                if (System.currentTimeMillis() % 5000 < 50) { // Try every 5 seconds (roughly)
                    LOGGER.info("[FANCYMENU] Periodic play attempt for unstarted video");
                    this.videoPlayer.play();
                    videoPlayRequested = true;
                }
            }

            // Render the player or a black background
            RenderSystem.enableBlend();
            
            if (finalVideoUrl != null && hasLoadedVideo) {
                this.videoPlayer.render(graphics, mouseX, mouseY, partial);
            } else {
                // Draw black background if no video
                graphics.fill(x, y, x + w, y + h, DrawableColor.BLACK.getColorInt());
            }

            RenderSystem.disableBlend();
        }
    }

    @Override
    public void onDestroyElement() {
        if ((this.videoManager != null) && (this.playerId != null)) {
            this.videoManager.removePlayer(this.playerId);
        }
    }

    public void restoreAspectRatio() {
//        ITexture t = this.getTextureResource();
//        AspectRatio ratio = (t != null) ? t.getAspectRatio() : new AspectRatio(10, 10);
//        this.baseWidth = ratio.getAspectRatioWidth(this.getAbsoluteHeight());
    }
}