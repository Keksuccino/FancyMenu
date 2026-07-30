package de.keksuccino.fancymenu.util.rendering.video.rinku;

import de.keksuccino.rinku.RinkuClient;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.rinku.RinkuUtil;
import de.keksuccino.fancymenu.util.rinku.RinkuExecutors;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cef.CefSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import de.keksuccino.rinku.Rinku;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefDisplayHandlerAdapter;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Manages video player instances for the mod.
 * This class handles creation, tracking, and cleanup of video players.
 */
public class RinkuVideoManager {

    protected static final Logger LOGGER = LogManager.getLogger();
    protected static final RinkuVideoManager INSTANCE = new RinkuVideoManager();
    public static final ScheduledExecutorService EXECUTOR = RinkuExecutors.newSingleThreadScheduledExecutor("FancyMenu-RinkuVideoManager");
    private static final ScheduledExecutorService INITIALIZATION_EXECUTOR = RinkuExecutors.newSingleThreadScheduledExecutor("FancyMenu-RinkuVideoManager-Initialization");
    
    // Map to track all active video players
    protected final Map<String, RinkuVideoPlayer> players = new ConcurrentHashMap<>();
    private final Object playerLifecycleLock = new Object();
    // Flag to track if web resources have been registered
    protected boolean webResourcesRegistered = false;
    
    // For handling JS results
    private static volatile boolean jsResultHandlerRegistered = false;
    private static final Map<String, CompletableFuture<String>> pendingJsResults = new ConcurrentHashMap<>();
    private static final Object JS_RESULT_LIFECYCLE_LOCK = new Object();

    private static volatile boolean is_initializing = false;
    public static volatile boolean initialized = false;
    private static volatile boolean shuttingDown = false;
    
    /**
     * Gets the singleton instance of the VideoManager.
     *
     * @return The VideoManager instance
     */
    public static RinkuVideoManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Initializes the VideoManager by extracting web resources to the temp directory.
     * This should be called during mod initialization.
     */
    public void initialize() {
        synchronized (RinkuVideoManager.class) {
            if (shuttingDown || initialized || is_initializing) return;
            is_initializing = true;
        }

        LOGGER.info("[FANCYMENU] Starting initialization of RinkuVideoManager..");

        if (!RinkuUtil.rinku_initialized) {
            LOGGER.warn("[FANCYMENU] Rinku not initialized yet! Will wait for Rinku to be ready before initializing RinkuVideoManager!");
        }

        INITIALIZATION_EXECUTOR.execute(() -> {
            try {
                while (!shuttingDown) {
                    if (RinkuUtil.rinku_initialized) {
                        MainThreadTaskExecutor.executeInMainThread(() -> {
                            if (shuttingDown) return;
                            try {

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
                                            LOGGER.info("[FANCYMENU] RinkuVideoManager: Successfully extracted video player web resources");
                                        } catch (Exception e) {
                                            LOGGER.error("[FANCYMENU] RinkuVideoManager: Failed to extract video player web resources", e);
                                        }
                                    }
                                }

                                synchronized (RinkuVideoManager.class) {
                                    if (shuttingDown) return;
                                    initialized = true;
                                    is_initializing = false;
                                }

                                LOGGER.info("[FANCYMENU] RinkuVideoManager successfully initialized!");

                            } catch (Exception ex) {
                                is_initializing = false;
                                LOGGER.error("[FANCYMENU] Failed to initialize RinkuVideoManager!", ex);
                            }
                        }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                        break;
                    }
                    Thread.sleep(100);
                }
            } catch (Exception ex) {
                is_initializing = false;
                if (!shuttingDown) LOGGER.error("[FANCYMENU] Failed to initialize RinkuVideoManager!", ex);
            }
        });

    }
    
    /**
     * Registers the JavaScript result handler with Rinku
     */
    private static synchronized void registerJsResultHandlerInternal() {
        if (shuttingDown || jsResultHandlerRegistered) return;

        try {
            RinkuClient client = Rinku.getClient(); // Get Rinku's CefClient instance
            // Add our custom display handler to intercept console messages
            client.addDisplayHandler(new CefDisplayHandlerAdapter() {
                @Override
                public boolean onConsoleMessage(CefBrowser browser, CefSettings.LogSeverity level, String message, String source, int line) {
                    if (message != null && message.startsWith("RINKU_ASYNC_RESULT:")) {
                        try {
                            String[] parts = message.split(":", 3); // Format: RINKU_ASYNC_RESULT:requestId:jsonData
                            if (parts.length == 3) {
                                String requestId = parts[1];
                                String jsonData = parts[2];
                                CompletableFuture<String> future = removePendingJsResult(requestId);
                                if (future != null) {
                                    if ("undefined".equals(jsonData)) { // JSON.stringify(undefined) results in "undefined"
                                        future.complete(null); // Treat JS undefined as Java null
                                    } else {
                                        future.complete(jsonData);
                                    }
                                } else if (!shuttingDown) {
                                    LOGGER.warn("[FANCYMENU] Received JS result for unknown or timed-out request ID: {}", requestId);
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.error("[FANCYMENU] Error processing RINKU_ASYNC_RESULT: " + message, e);
                        }
                        return true; // Indicate message is handled
                    }
                    return false; // Message not handled by us, let Rinku process it further if needed
                }
            });
            jsResultHandlerRegistered = true;
        } catch (Throwable t) { // Catch Throwable to include LinkageErrors etc. if JCEF classes are missing
            LOGGER.error("[FANCYMENU] Failed to register JS result display handler with Rinku.", t);
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
     * Checks if Rinku is available for video playback.
     *
     * @return True if Rinku is loaded and available, false otherwise
     */
    public boolean isVideoPlaybackAvailable() {
        return RinkuUtil.isRinkuLoaded();
    }

    @Nullable
    static CompletableFuture<String> registerPendingJsResult(@NotNull String requestId) {
        synchronized (JS_RESULT_LIFECYCLE_LOCK) {
            if (shuttingDown) return null;
            CompletableFuture<String> result = new CompletableFuture<>();
            pendingJsResults.put(requestId, result);
            return result;
        }
    }

    @Nullable
    static CompletableFuture<String> removePendingJsResult(@NotNull String requestId) {
        synchronized (JS_RESULT_LIFECYCLE_LOCK) {
            return pendingJsResults.remove(requestId);
        }
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
        if (shuttingDown) return null;
        if (!isVideoPlaybackAvailable()) {
            LOGGER.warn("[FANCYMENU] Cannot create video player: Rinku is not loaded");
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
            RinkuVideoPlayer player = new RinkuVideoPlayer(x, y, width, height);
            synchronized (this.playerLifecycleLock) {
                if (shuttingDown) {
                    player.dispose();
                    return null;
                }
                players.put(playerId, player);
            }
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
    public RinkuVideoPlayer getPlayer(@NotNull String playerId) {
        if (shuttingDown) return null;
        return players.get(playerId);
    }
    
    /**
     * Removes and disposes of a video player.
     *
     * @param playerId The player's unique identifier
     */
    public void removePlayer(@NotNull String playerId) {
        RinkuVideoPlayer player;
        synchronized (this.playerLifecycleLock) {
            player = players.remove(playerId);
        }
        if (player != null) {
            player.dispose();
        }
    }
    
    /**
     * Disposes of all video players.
     * Call this when shutting down or reloading the mod.
     */
    public void disposeAll() {
        shuttingDown = true;
        initialized = false;
        is_initializing = false;
        List<RinkuVideoPlayer> playersToDispose;
        synchronized (this.playerLifecycleLock) {
            playersToDispose = new ArrayList<>(this.players.values());
            this.players.clear();
        }
        for (RinkuVideoPlayer player : playersToDispose) {
            try {
                player.dispose();
            } catch (Throwable throwable) {
                LOGGER.error("[FANCYMENU] Error disposing Rinku video player", throwable);
            }
        }

        List<CompletableFuture<String>> jsResultsToCancel;
        synchronized (JS_RESULT_LIFECYCLE_LOCK) {
            jsResultsToCancel = new ArrayList<>(pendingJsResults.values());
            pendingJsResults.clear();
        }
        CancellationException cancellation = new CancellationException("FancyMenu Rinku video manager was disposed");
        for (CompletableFuture<String> pendingJsResult : jsResultsToCancel) {
            try {
                pendingJsResult.completeExceptionally(cancellation);
            } catch (Throwable throwable) {
                LOGGER.error("[FANCYMENU] Error cancelling a pending Rinku JavaScript result", throwable);
            }
        }
    }

}
