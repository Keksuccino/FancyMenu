package de.keksuccino.fancymenu.util.rendering.video;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.mcef.MCEFUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Manages video player instances for the mod.
 * This class handles creation, tracking, and cleanup of video players.
 */
public class VideoManager {

    protected static final Logger LOGGER = LogManager.getLogger();
    protected static final VideoManager INSTANCE = new VideoManager();

    protected static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    
    // Map to track all active video players
    protected final Map<String, MCEFVideoPlayer> players = new HashMap<>();
    
    // Flag to track if web resources have been registered
    protected boolean webResourcesRegistered = false;
    
    /**
     * Gets the singleton instance of the VideoManager.
     *
     * @return The VideoManager instance
     */
    public static VideoManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Initializes the VideoManager by extracting web resources to the temp directory.
     * This should be called during mod initialization.
     */
    public void initialize() {
        if (isVideoPlaybackAvailable() && !webResourcesRegistered) {
            try {
                // Extract the web resources to FancyMenu's temp directory
                extractWebResources();
                webResourcesRegistered = true;
                LOGGER.info("[FANCYMENU] Successfully extracted video player web resources");
            } catch (Exception e) {
                LOGGER.error("[FANCYMENU] Failed to extract video player web resources", e);
            }
        }
    }
    
    /**
     * Extracts the web resources from the mod JAR to FancyMenu's temp directory.
     */
    protected void extractWebResources() {
        File webDir = new File(FancyMenu.TEMP_DATA_DIR, "web/videoplayer");
        if (!webDir.exists() && !webDir.mkdirs()) {
            LOGGER.error("[FANCYMENU] Failed to create web resource directory: {}", webDir.getAbsolutePath());
            // This is critical, further extractions will fail.
            return;
        }

        // Helper method for extraction
        extractResourceInternal("/assets/fancymenu/web/videoplayer/player.html", new File(webDir, "player.html"), true);
        extractResourceInternal("/assets/fancymenu/web/videoplayer/simple_test.html", new File(webDir, "simple_test.html"), false);
        extractResourceInternal("/assets/fancymenu/web/videoplayer/standalone_test.html", new File(webDir, "standalone_test.html"), false);
        extractResourceInternal("/assets/fancymenu/web/videoplayer/README.txt", new File(webDir, "README.txt"), false);
    }

    /**
     * Helper method to extract a resource file from the JAR to a destination file.
     * 
     * @param resourcePath Path to the resource in the JAR
     * @param destinationFile Destination file to extract to
     * @param isCritical Whether this resource is critical for operation
     */
    private void extractResourceInternal(String resourcePath, File destinationFile, boolean isCritical) {
        try (InputStream is = FancyMenu.class.getResourceAsStream(resourcePath)) {
            if (is != null) {
                Files.copy(is, destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("[FANCYMENU] Extracted {} to {}", destinationFile.getName(), destinationFile.getAbsolutePath());
            } else {
                String message = "[FANCYMENU] Could not find resource {} in mod JAR";
                if (isCritical) LOGGER.error(message + " (CRITICAL)", resourcePath);
                else LOGGER.warn(message, resourcePath);
                if (isCritical && resourcePath.endsWith("player.html")) {
                     // If player.html is critical and not found, video playback will fail.
                     this.webResourcesRegistered = false; // Mark as failed if player.html is missing
                }
            }
        } catch (Exception e) {
            String message = "[FANCYMENU] Failed to extract resource {}: {}";
            if (isCritical) LOGGER.error(message + " (CRITICAL)", resourcePath, e.getMessage(), e);
            else LOGGER.warn(message, resourcePath, e.getMessage());
             if (isCritical && resourcePath.endsWith("player.html")) {
                 this.webResourcesRegistered = false;
            }
        }
    }
    
    /**
     * Checks if MCEF is available for video playback.
     *
     * @return True if MCEF is loaded and available, false otherwise
     */
    public boolean isVideoPlaybackAvailable() {
        return MCEFUtil.isMCEFLoaded();
    }
    
    /**
     * Creates a new video player with default settings.
     * Automatically initializes web resources if needed.
     *
     * @return A unique identifier for the created player, or null if creation failed
     */
    @Nullable
    public String createPlayer() {
        return createPlayer(0, 0, 200, 200);
    }
    
    /**
     * Creates a new video player with specified dimensions.
     * Automatically initializes web resources if needed.
     *
     * @param x The X position of the player
     * @param y The Y position of the player
     * @param width The width of the player
     * @param height The height of the player
     * @return A unique identifier for the created player, or null if creation failed
     */
    @Nullable
    public String createPlayer(int x, int y, int width, int height) {
        if (!isVideoPlaybackAvailable()) {
            LOGGER.warn("[FANCYMENU] Cannot create video player: MCEF is not loaded");
            return null;
        }
        
        // Ensure web resources are initialized.
        // initialize() itself handles the webResourcesRegistered flag.
        if (!webResourcesRegistered) {
            initialize(); // This will attempt extraction and set webResourcesRegistered
            if (!webResourcesRegistered) { // Check again after attempt
                LOGGER.error("[FANCYMENU] Failed to initialize/verify web resources for video player. Cannot create player.");
                return null;
            }
        }
        
        try {
            String playerId = UUID.randomUUID().toString();
            MCEFVideoPlayer player = new MCEFVideoPlayer(x, y, width, height);
            players.put(playerId, player);
            return playerId;
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Failed to create video player", e);
            return null;
        }
    }
    
    /**
     * Gets a video player by its ID.
     *
     * @param playerId The player's unique identifier
     * @return The video player instance, or null if not found
     */
    @Nullable
    public MCEFVideoPlayer getPlayer(@NotNull String playerId) {
        return players.get(playerId);
    }
    
    /**
     * Removes and disposes of a video player.
     *
     * @param playerId The player's unique identifier
     */
    public void removePlayer(@NotNull String playerId) {
        MCEFVideoPlayer player = players.remove(playerId);
        if (player != null) {
            player.dispose();
        }
    }
    
    /**
     * Loads a video file into a player.
     *
     * @param playerId The player's unique identifier
     * @param videoFile The video file to load
     * @return True if the video was loaded successfully, false otherwise
     */
    public boolean loadVideo(@NotNull String playerId, @NotNull File videoFile) {
        MCEFVideoPlayer player = getPlayer(playerId);
        if (player == null) {
            return false;
        }
        
        if (!videoFile.exists()) {
            LOGGER.error("[FANCYMENU] Video file does not exist: " + videoFile.getAbsolutePath());
            return false;
        }
        
        try {
            // Convert to URI format for better compatibility
            String fileUri = videoFile.toURI().toString();
            LOGGER.info("[FANCYMENU] Loading video file (via URI): " + fileUri);
            player.loadVideo(fileUri);
            return true;
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Failed to load video: " + videoFile.getAbsolutePath(), e);
            return false;
        }
    }
    
    /**
     * Loads a video from a URL into a player.
     *
     * @param playerId The player's unique identifier
     * @param videoUrl The URL of the video to load
     * @return True if the video was loaded successfully, false otherwise
     */
    public boolean loadVideo(@NotNull String playerId, @NotNull URL videoUrl) {
        MCEFVideoPlayer player = getPlayer(playerId);
        if (player == null) {
            return false;
        }
        
        try {
            player.loadVideo(videoUrl.toString());
            return true;
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Failed to load video from URL: " + videoUrl, e);
            return false;
        }
    }
    
    /**
     * Loads a video from a path into a player.
     *
     * @param playerId The player's unique identifier
     * @param videoPath The path to the video file
     * @return True if the video was loaded successfully, false otherwise
     */
    public boolean loadVideo(@NotNull String playerId, @NotNull Path videoPath) {
        return loadVideo(playerId, videoPath.toFile());
    }
    
    /**
     * Loads a video from a path string into a player.
     *
     * @param playerId The player's unique identifier
     * @param videoPathStr The path to the video file
     * @return True if the video was loaded successfully, false otherwise
     */
    public boolean loadVideo(@NotNull String playerId, @NotNull String videoPathStr) {
        MCEFVideoPlayer player = getPlayer(playerId);
        if (player == null) {
            LOGGER.warn("[FANCYMENU] loadVideo: Player not found for ID: {}", playerId);
            return false;
        }
        
        try {
            String effectiveVideoPath = videoPathStr;
            // If it doesn't look like a URL (http, https, file), treat as a potential local file path.
            if (!videoPathStr.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
                File file = new File(videoPathStr);
                if (file.exists() && file.isFile()) {
                    effectiveVideoPath = file.toURI().toString();
                    LOGGER.info("[FANCYMENU] Converted video path to URI: {}", effectiveVideoPath);
                } else {
                    LOGGER.warn("[FANCYMENU] Video path does not seem to be a URL and file not found locally: {}. Passing as is.", videoPathStr);
                    // Let player.html try to resolve it, might be relative to player.html itself if not absolute.
                }
            }
            player.loadVideo(effectiveVideoPath);
            return true;
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Failed to load video from path: {}", videoPathStr, e);
            return false;
        }
    }
    
    /**
     * Disposes of all video players.
     * Call this when shutting down or reloading the mod.
     */
    public void disposeAll() {
        for (MCEFVideoPlayer player : players.values()) {
            try {
                player.dispose();
            } catch (Exception e) {
                LOGGER.error("[FANCYMENU] Error disposing video player", e);
            }
        }
        players.clear();
    }
}