package de.keksuccino.fancymenu.util.rendering.video.mcef;

import com.cinemamod.mcef.MCEFClient;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.mcef.MCEFUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cef.CefSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.cinemamod.mcef.MCEF;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefDisplayHandlerAdapter;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Manages video player instances for the mod.
 * This class handles creation, tracking, and cleanup of video players.
 */
public class MCEFVideoManager {

    protected static final Logger LOGGER = LogManager.getLogger();
    protected static final MCEFVideoManager INSTANCE = new MCEFVideoManager();
    public static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    
    // Map to track all active video players
    protected final Map<String, MCEFVideoPlayer> players = new HashMap<>();
    // Flag to track if web resources have been registered
    protected boolean webResourcesRegistered = false;
    
    // For handling JS results
    private static volatile boolean jsResultHandlerRegistered = false;
    private static final Map<String, CompletableFuture<String>> pendingJsResults = new ConcurrentHashMap<>();
    
    /**
     * Gets the singleton instance of the VideoManager.
     *
     * @return The VideoManager instance
     */
    public static MCEFVideoManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Public getter for MCEFVideoPlayer to access the pending results map
     */
    public static Map<String, CompletableFuture<String>> getPendingJsResults() {
        return pendingJsResults;
    }
    
    /**
     * Initializes the VideoManager by extracting web resources to the temp directory.
     * This should be called during mod initialization.
     */
    public void initialize() {
        if (isVideoPlaybackAvailable()) {
            // Register JS result handler if not already done
            if (!jsResultHandlerRegistered) {
                registerJsResultHandlerInternal();
            }
            
            // Existing web resource extraction logic
            if (!webResourcesRegistered) {
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
    }
    
    /**
     * Registers the JavaScript result handler with MCEF
     */
    private static synchronized void registerJsResultHandlerInternal() {
        if (jsResultHandlerRegistered) {
            return;
        }

        try {
            MCEFClient client = MCEF.getClient(); // Get MCEF's CefClient instance
            // Add our custom display handler to intercept console messages
            client.addDisplayHandler(new CefDisplayHandlerAdapter() {
                @Override
                public boolean onConsoleMessage(CefBrowser browser, CefSettings.LogSeverity level, String message, String source, int line) {
                    if (message != null && message.startsWith("MCEF_ASYNC_RESULT:")) {
                        try {
                            String[] parts = message.split(":", 3); // Format: MCEF_ASYNC_RESULT:requestId:jsonData
                            if (parts.length == 3) {
                                String requestId = parts[1];
                                String jsonData = parts[2];
                                CompletableFuture<String> future = pendingJsResults.remove(requestId);
                                if (future != null) {
                                    if ("undefined".equals(jsonData)) { // JSON.stringify(undefined) results in "undefined"
                                        future.complete(null); // Treat JS undefined as Java null
                                    } else {
                                        future.complete(jsonData);
                                    }
                                } else {
                                    LOGGER.warn("[FANCYMENU] Received JS result for unknown or timed-out request ID: {}", requestId);
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.error("[FANCYMENU] Error processing MCEF_ASYNC_RESULT: " + message, e);
                        }
                        return true; // Indicate message is handled
                    }
                    return false; // Message not handled by us, let MCEF process it further if needed
                }
            });
            jsResultHandlerRegistered = true;
        } catch (Throwable t) { // Catch Throwable to include LinkageErrors etc. if JCEF classes are missing
            LOGGER.error("[FANCYMENU] Failed to register JS result display handler with MCEF.", t);
        }
        
        if (!jsResultHandlerRegistered) {
            LOGGER.warn("[FANCYMENU] JS result handler NOT registered. Getting duration/playtime will likely fail.");
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
            } else {
                String message = "[FANCYMENU] Could not find resource {} in mod JAR";
                if (isCritical) {
                    LOGGER.error(message + " (CRITICAL)", resourcePath);
                } else {
                    LOGGER.warn(message, resourcePath);
                }
                if (isCritical && resourcePath.endsWith("player.html")) {
                     // If player.html is critical and not found, video playback will fail.
                     this.webResourcesRegistered = false; // Mark as failed if player.html is missing
                }
            }
        } catch (Exception e) {
            String message = "[FANCYMENU] Failed to extract resource {}: {}";
            if (isCritical) {
                LOGGER.error(message + " (CRITICAL)", resourcePath, e.getMessage(), e);
            } else {
                LOGGER.warn(message, resourcePath, e.getMessage());
            }
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
        
        // Also ensure JS handler is registered (initialize() above should handle this)
        if (!jsResultHandlerRegistered && isVideoPlaybackAvailable()) {
             registerJsResultHandlerInternal(); // Attempt registration again if initialize() didn't set it
             if (!jsResultHandlerRegistered) {
                 LOGGER.error("[FANCYMENU] JS Result Handler not registered. Video info (duration, etc.) may not work.");
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