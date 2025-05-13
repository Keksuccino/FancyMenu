package de.keksuccino.fancymenu.util.rendering.video;

import de.keksuccino.fancymenu.util.mcef.WrappedMCEFBrowser;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Example screen showing how to use the MCEFVideoPlayer.
 * This demonstrates basic video player usage.
 */
public class VideoPlayerExample extends Screen {

    private static final Logger LOGGER = LogManager.getLogger();
    
    private final VideoManager videoManager;
    private String playerId;
    private MCEFVideoPlayer videoPlayer;
    private WrappedMCEFBrowser testBrowser;
    
    public VideoPlayerExample() {
        super(Component.literal("Video Player Example"));
        this.videoManager = VideoManager.getInstance();
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Check if MCEF is available
        if (!videoManager.isVideoPlaybackAvailable()) {
            LOGGER.error("[FANCYMENU] Cannot initialize video player: MCEF is not loaded");
            return;
        }
        
        // Run diagnostics
        VideoDebugger.runDiagnostics();
        
        // Create a video player centered on the screen
        int videoWidth = 640;
        int videoHeight = 360;
        int x = (this.width - videoWidth) / 2;
        int y = (this.height - videoHeight) / 2;
        
        // Try both approaches: regular player and direct browser
        playerId = videoManager.createTestPlayer(x, y, videoWidth, videoHeight);
        if (playerId != null) {
            videoPlayer = videoManager.getPlayer(playerId);
            if (videoPlayer != null) {
                LOGGER.info("[FANCYMENU] Created video test player");
                VideoDebugger.writeDebugInfo("Video test player created");
            }
        }
        
        // Try a direct browser test as a fallback
        if (videoPlayer == null) {
            LOGGER.info("[FANCYMENU] Trying direct browser approach");
            testBrowser = VideoDebugger.createTestBrowser(x, y + 400, videoWidth, videoHeight);
            VideoDebugger.writeDebugInfo("Direct test browser created");
        }
        
        // Try loading a video only if regular player works
        if (videoPlayer != null) {
            // Load a video file
            File videoFile = new File("video.mp4");
            if (videoFile.exists()) {
                LOGGER.info("[FANCYMENU] Loading video file: " + videoFile.getAbsolutePath());
                videoManager.loadVideo(playerId, videoFile);
            } else {
                LOGGER.error("[FANCYMENU] Video file does not exist: " + videoFile.getAbsolutePath());
            }
        }
    }
    
    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Renders the screen background in MC 1.21.1
        super.render(graphics, mouseX, mouseY, partialTick);
        
        // Render the video player
        if (videoPlayer != null) {
            videoPlayer.render(graphics, mouseX, mouseY, partialTick);
        }
        
        // Render the test browser if we're using it
        if (testBrowser != null) {
            testBrowser.render(graphics, mouseX, mouseY, partialTick);
        }
    }
    
    @Override
    public void onClose() {
        // Clean up the video player when the screen is closed
        if (playerId != null) {
            videoManager.removePlayer(playerId);
            videoPlayer = null;
        }
        
        // Clean up the test browser if we created one
        if (testBrowser != null) {
            try {
                testBrowser.close();
            } catch (Exception e) {
                LOGGER.error("[FANCYMENU] Error closing test browser", e);
            }
            testBrowser = null;
        }
        
        super.onClose();
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Example of player control with keyboard shortcuts
        if (videoPlayer != null) {
            switch (keyCode) {
                case 32: // Spacebar - toggle play/pause
                    if (videoPlayer.isPlaying()) {
                        videoPlayer.pause();
                    } else {
                        videoPlayer.play();
                    }
                    return true;
                
                case 82: // R key - restart video
                    videoPlayer.setCurrentTime(0);
                    videoPlayer.play();
                    return true;
                    
                case 70: // F key - toggle fill screen mode
                    videoPlayer.setFillScreen(!videoPlayer.isFillScreen());
                    return true;
                    
                case 76: // L key - toggle loop
                    videoPlayer.setLooping(!videoPlayer.isLooping());
                    return true;
                    
                case 84: // T key - load simple test page
                    videoPlayer.loadSimpleTest();
                    return true;
                    
                // Volume controls
                case 263: // Arrow left - volume down
                    videoPlayer.setVolume(Math.max(0, videoPlayer.getVolume() - 0.1f));
                    return true;
                    
                case 262: // Arrow right - volume up
                    videoPlayer.setVolume(Math.min(1, videoPlayer.getVolume() + 0.1f));
                    return true;
            }
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}