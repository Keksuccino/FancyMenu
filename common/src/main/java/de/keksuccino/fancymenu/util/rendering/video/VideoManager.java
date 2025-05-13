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

/**
 * Manages video player instances for the mod.
 * This class handles creation, tracking, and cleanup of video players.
 */
public class VideoManager {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final VideoManager INSTANCE = new VideoManager();
    
    // Map to track all active video players
    private final Map<String, MCEFVideoPlayer> players = new HashMap<>();
    
    // Flag to track if web resources have been registered
    private boolean webResourcesRegistered = false;
    
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
    private void extractWebResources() {
        try {
            // Use FancyMenu's temp directory
            File webDir = new File(FancyMenu.TEMP_DATA_DIR, "web/videoplayer");
            webDir.mkdirs();
            
            // Extract the player.html file
            try (InputStream is = FancyMenu.class.getResourceAsStream("/assets/fancymenu/web/videoplayer/player.html")) {
                if (is != null) {
                    File playerFile = new File(webDir, "player.html");
                    Files.copy(is, playerFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.info("[FANCYMENU] Extracted player.html to " + playerFile.getAbsolutePath());
                } else {
                    LOGGER.error("[FANCYMENU] Could not find player.html resource in mod JAR");
                }
            }
            
            // Extract the simple_test.html file
            try (InputStream is = FancyMenu.class.getResourceAsStream("/assets/fancymenu/web/videoplayer/simple_test.html")) {
                if (is != null) {
                    File testFile = new File(webDir, "simple_test.html");
                    Files.copy(is, testFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.info("[FANCYMENU] Extracted simple_test.html to " + testFile.getAbsolutePath());
                }
            } catch (Exception e) {
                LOGGER.error("[FANCYMENU] Could not extract simple_test.html: " + e.getMessage());
            }
            
            // Extract the standalone_test.html file
            try (InputStream is = FancyMenu.class.getResourceAsStream("/assets/fancymenu/web/videoplayer/standalone_test.html")) {
                if (is != null) {
                    File testFile = new File(webDir, "standalone_test.html");
                    Files.copy(is, testFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.info("[FANCYMENU] Extracted standalone_test.html to " + testFile.getAbsolutePath());
                }
            } catch (Exception e) {
                LOGGER.error("[FANCYMENU] Could not extract standalone_test.html: " + e.getMessage());
            }
            
            // Extract the README.txt file (not critical)
            try (InputStream is = FancyMenu.class.getResourceAsStream("/assets/fancymenu/web/videoplayer/README.txt")) {
                if (is != null) {
                    File readmeFile = new File(webDir, "README.txt");
                    Files.copy(is, readmeFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception e) {
                // Non-critical if README extraction fails
                LOGGER.debug("[FANCYMENU] Could not extract README.txt: " + e.getMessage());
            }
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Failed to extract web resources", e);
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
        
        // Make sure web resources are registered
        if (!webResourcesRegistered) {
            initialize();
            if (!webResourcesRegistered) {
                LOGGER.error("[FANCYMENU] Failed to register web resources for video player");
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
     * Creates a test player with simple HTML for debugging.
     * This is useful for verifying if the HTML loading works at all.
     *
     * @param x The X position of the player
     * @param y The Y position of the player
     * @param width The width of the player
     * @param height The height of the player
     * @return A unique identifier for the created player, or null if creation failed
     */
    @Nullable
    public String createTestPlayer(int x, int y, int width, int height) {
        String playerId = createPlayer(x, y, width, height);
        if (playerId != null) {
            MCEFVideoPlayer player = getPlayer(playerId);
            if (player != null) {
                player.loadSimpleTest();
                LOGGER.info("[FANCYMENU] Created test player with ID: " + playerId);
                return playerId;
            }
        }
        return null;
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
     * @param videoPath The path to the video file
     * @return True if the video was loaded successfully, false otherwise
     */
    public boolean loadVideo(@NotNull String playerId, @NotNull String videoPath) {
        MCEFVideoPlayer player = getPlayer(playerId);
        if (player == null) {
            return false;
        }
        
        try {
            // If it looks like a file path, convert to a proper URI
            if (videoPath.matches("^[a-zA-Z]:[/\\\\].*") || videoPath.startsWith("/")) {
                File file = new File(videoPath);
                if (file.exists()) {
                    videoPath = file.toURI().toString();
                }
            }
            
            player.loadVideo(videoPath);
            return true;
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Failed to load video from path: " + videoPath, e);
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