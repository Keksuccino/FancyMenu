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
        
        // Create the player with some special settings
        playerId = videoManager.createPlayer(x, y, videoWidth, videoHeight);
        if (playerId != null) {
            videoPlayer = videoManager.getPlayer(playerId);
            if (videoPlayer != null) {
                LOGGER.info("[FANCYMENU] Created video player");
                
                // Set player options
                videoPlayer.setVolume(0.5f);
                videoPlayer.setLooping(true);
                videoPlayer.setFillScreen(true);
                
                // Don't load a video immediately
                LOGGER.info("[FANCYMENU] Ready to load videos - press 1, 2, or 3 to try different loading methods");
            }
        }
        
        if (videoPlayer == null) {
            // Try fallback to test browser
            LOGGER.info("[FANCYMENU] Using fallback test browser");
            testBrowser = VideoDebugger.createTestBrowser(x, y, videoWidth, videoHeight);
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
                    
                case 49: // 1 key - try loading video with method 1
                    tryLoadVideoMethod1();
                    return true;
                    
                case 50: // 2 key - try loading video with method 2
                    tryLoadVideoMethod2();
                    return true;
                    
                case 51: // 3 key - try loading video with method 3
                    tryLoadVideoMethod3();
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
    
    /**
     * Try to load the video using method 1: Direct absolute path
     */
    private void tryLoadVideoMethod1() {
        if (videoPlayer == null) return;
        
        File videoFile = new File("video.mp4");
        if (videoFile.exists()) {
            LOGGER.info("[FANCYMENU] Loading video via absolute path, method 1");
            
            // Absolute path with forward slashes
            String absolutePath = videoFile.getAbsolutePath().replace('\\', '/');
            videoPlayer.loadVideo(absolutePath);
        } else {
            LOGGER.error("[FANCYMENU] Video file not found at: " + videoFile.getAbsolutePath());
        }
    }
    
    /**
     * Try to load the video using method 2: URI format
     */
    private void tryLoadVideoMethod2() {
        if (videoPlayer == null) return;
        
        File videoFile = new File("video.mp4");
        if (videoFile.exists()) {
            LOGGER.info("[FANCYMENU] Loading video via URI, method 2");
            
            // URI format
            String fileUri = videoFile.toURI().toString();
            videoPlayer.loadVideo(fileUri);
        } else {
            LOGGER.error("[FANCYMENU] Video file not found at: " + videoFile.getAbsolutePath());
        }
    }
    
    /**
     * Try to load the video using method 3: Relative path
     */
    private void tryLoadVideoMethod3() {
        if (videoPlayer == null) return;
        
        LOGGER.info("[FANCYMENU] Loading video via relative path, method 3");
        
        // Simple relative path
        videoPlayer.loadVideo("video.mp4");
    }
}