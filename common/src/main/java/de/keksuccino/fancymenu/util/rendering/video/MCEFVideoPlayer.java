package de.keksuccino.fancymenu.util.rendering.video;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.mcef.MCEFUtil;
import de.keksuccino.fancymenu.util.mcef.WrappedMCEFBrowser;
import net.minecraft.client.gui.GuiGraphics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.network.CefLoadHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * A Minecraft video player using MCEF (Minecraft Chromium Embedded Framework).
 * This implementation uses a custom HTML/JS player to render and control videos.
 */
public class MCEFVideoPlayer {

    protected static final Logger LOGGER = LogManager.getLogger();

    protected volatile WrappedMCEFBrowser browser;

    protected volatile float volume = 1.0f;
    protected volatile boolean looping = false;
    protected volatile boolean fillScreen = false;
    protected volatile String currentVideoPath = null;
    protected volatile boolean isMuted = false;

    protected volatile int posX = 0;
    protected volatile int posY = 0;
    protected volatile int width = 200;
    protected volatile int height = 200;

    protected volatile boolean initialized = false;
    protected final CompletableFuture<Boolean> initFuture = new CompletableFuture<>();
    private final String instanceId = UUID.randomUUID().toString(); // For unique JS communication if needed
    private CefLoadHandler instanceSpecificLoadHandler; // To manage its lifecycle
    
    /**
     * Creates a new video player instance.
     * Note: This requires MCEF to be loaded to function.
     */
    public MCEFVideoPlayer() {
        initialize();
    }
    
    /**
     * Creates a new video player instance with specific dimensions.
     *
     * @param x The X position of the player
     * @param y The Y position of the player
     * @param width The width of the player
     * @param height The height of the player
     */
    public MCEFVideoPlayer(int x, int y, int width, int height) {
        this.posX = x;
        this.posY = y;
        this.width = width;
        this.height = height;
        initialize();
    }
    
    /**
     * Initializes the video player by creating the browser instance.
     * This is called automatically upon construction.
     */
    public void initialize() {
        if (!MCEFUtil.isMCEFLoaded()) {
            LOGGER.error("[FANCYMENU] Failed to initialize MCEFVideoPlayer: MCEF is not loaded");
            initFuture.complete(false);
            return;
        }
        
        try {
            String playerUrl = buildPlayerUrl();
            LOGGER.info("[FANCYMENU] Initializing video player with URL: " + playerUrl);
            
            File playerFile = new File(FancyMenu.TEMP_DATA_DIR, "web/videoplayer/player.html");
            if (!playerFile.exists()) {
                LOGGER.warn("[FANCYMENU] Player HTML file not found. Attempting to extract resources.");
                VideoManager.getInstance().initialize(); // Try to extract if missing
                if (!playerFile.exists()) {
                    LOGGER.error("[FANCYMENU] CRITICAL: Player HTML file does not exist at: " + playerFile.getAbsolutePath() + ". Video player will fail.");
                    initFuture.complete(false);
                    return;
                }
            }
            
            // Important: autoHandle should be false if VideoManager explicitly manages lifecycle
            browser = WrappedMCEFBrowser.build(playerUrl, false, false, posX, posY, width, height);
            if (browser == null || browser.getBrowser() == null) { // MCEF might fail to create browser
                LOGGER.error("[FANCYMENU] Failed to build WrappedMCEFBrowser or get underlying MCEFBrowser.");
                initFuture.complete(false);
                return;
            }

            final int browserId = browser.getIdentifier(); // Get ID of this browser instance

            instanceSpecificLoadHandler = new CefLoadHandlerAdapter() {
                private boolean handled = false;

                @Override
                public void onLoadEnd(CefBrowser cefBrowser, CefFrame frame, int httpStatusCode) {
                    if (cefBrowser.getIdentifier() == browserId && frame.isMain() && !handled) {
                        boolean success = (httpStatusCode >= 200 && httpStatusCode < 300) || // Typical success
                                          (frame.getURL() != null && frame.getURL().startsWith("file:") && httpStatusCode == 0); // File success

                        if (success) {
                            LOGGER.info("[FANCYMENU] MCEFVideoPlayer [{}] browser page loaded: {}", instanceId, frame.getURL());
                            initialized = true;
                            initFuture.complete(true);
                        } else {
                            LOGGER.error("[FANCYMENU] MCEFVideoPlayer [{}] browser page failed to load: {}, Status: {}", instanceId, frame.getURL(), httpStatusCode);
                            initialized = false; // Ensure it's marked as not initialized
                            initFuture.complete(false);
                        }
                        handled = true;
                        // Clean up this specific handler after first main frame load
                        if (browser != null && browser.getBrowser() != null && browser.getBrowser().getClient() != null) {
                            browser.getBrowser().getClient().removeLoadHandler(this);
                        }
                    }
                }

                @Override
                public void onLoadError(CefBrowser cefBrowser, CefFrame frame, CefLoadHandler.ErrorCode errorCode, String errorText, String failedUrl) {
                    if (cefBrowser.getIdentifier() == browserId && frame.isMain() && !handled) {
                        LOGGER.error("[FANCYMENU] MCEFVideoPlayer [{}] browser page load error: {}, {}, URL: {}", instanceId, errorCode, errorText, failedUrl);
                        initialized = false;
                        initFuture.complete(false);
                        handled = true;
                        if (browser != null && browser.getBrowser() != null && browser.getBrowser().getClient() != null) {
                           browser.getBrowser().getClient().removeLoadHandler(this);
                        }
                    }
                }
            };
            
            // Add the instance-specific load handler
            // This assumes browser.getBrowser() gives the CefBrowser and getClient() is available.
            // The MCEF library's structure for this is crucial.
            if (browser.getBrowser().getClient() != null) {
                 browser.getBrowser().getClient().addLoadHandler(instanceSpecificLoadHandler);
            } else {
                LOGGER.error("[FANCYMENU] Cannot get CefClient for MCEFVideoPlayer [{}]. Initialization will be unreliable.", instanceId);
                // Fallback: complete future optimistically after a delay, or mark as failed.
                // This is a last resort and indicates a problem with MCEF wrapper access.
                VideoManager.EXECUTOR.schedule(() -> {
                    if (!initFuture.isDone()) {
                        LOGGER.warn("[FANCYMENU] Fallback: Assuming player [{}] initialized after delay due to no CefClient access.", instanceId);
                        initialized = true; initFuture.complete(true);
                    }
                }, 2000, TimeUnit.MILLISECONDS);
            }
            
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Failed to initialize MCEFVideoPlayer [{}]", instanceId, e);
            initFuture.complete(false); // Ensure future is completed on exception
        }
    }
    
    /**
     * Builds the URL for the video player with initial parameters.
     *
     * @return The URL to load in the browser
     */
    protected String buildPlayerUrl() {
        File playerFile = new File(FancyMenu.TEMP_DATA_DIR, "web/videoplayer/player.html");
        String basePath = playerFile.toURI().toString(); // Converts to file:///...
        
        Map<String, String> params = new HashMap<>();
        params.put("volume", String.valueOf(this.volume));
        params.put("loop", String.valueOf(this.looping));
        params.put("fillScreen", String.valueOf(this.fillScreen));
        // 'autoPlay' in player.html determines if *it* should try to play an initial video from URL or if play() is called early.
        // If Java side calls loadVideo then play(), player.html's playRequestPending handles it.
        // If player.html is loaded with a ?video=...&autoPlay=true, it will try.
        // Let's default to false, meaning explicit play() from Java is needed unless URL has autoPlay=true for an initial video.
        params.put("autoPlay", "false"); 
        
        return basePath + "?" + buildQueryString(params);
    }
    
    /**
     * Converts a map of parameters to a URL query string.
     *
     * @param params The parameters to include
     * @return The URL-encoded query string
     */
    protected String buildQueryString(Map<String, String> params) {
        StringBuilder result = new StringBuilder();
        
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (result.length() > 0) {
                result.append("&");
            }
            
            result.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        
        return result.toString();
    }
    
    /**
     * Helper to ensure initialization and run on executor
     */
    private void executeWhenInitialized(Runnable action, String actionName) {
        VideoManager.EXECUTOR.execute(() -> {
            if (!initialized) {
                if (initFuture.isDone()) {
                    try {
                        if (!initFuture.getNow(false)) { // Check if initialization failed
                            LOGGER.warn("[FANCYMENU] Video player [{}] initialization previously failed. Action '{}' aborted.", instanceId, actionName);
                            return;
                        }
                        // If it was done and successful, but initialized is false, log and attempt to proceed.
                        LOGGER.warn("[FANCYMENU] initFuture for [{}] done and successful, but 'initialized' flag is false. Forcing to true for action '{}'.", instanceId, actionName);
                        initialized = true; 
                    } catch (Exception e) {
                        LOGGER.error("[FANCYMENU] Error checking initFuture state for [{}], action '{}' aborted.", instanceId, actionName, e);
                        return; 
                    }
                } else {
                    LOGGER.info("[FANCYMENU] Video player [{}] not initialized yet. Queueing action '{}'.", instanceId, actionName);
                    waitForInitialization(() -> executeWhenInitialized(action, actionName + " (retried)")); // Re-queue with this wrapper
                    return; 
                }
            }
            // If initialized is true, proceed.
            if (initialized) {
                 try {
                    action.run();
                } catch (Exception e) {
                    LOGGER.error("[FANCYMENU] Error executing action '{}' for player [{}]", actionName, instanceId, e);
                }
            } else {
                 LOGGER.warn("[FANCYMENU] Action '{}' for player [{}] skipped as it's still not initialized.", actionName, instanceId);
            }
        });
    }
    
    /**
     * Renders the video player.
     *
     * @param graphics The GUI graphics context
     * @param mouseX The mouse X position
     * @param mouseY The mouse Y position
     * @param partialTick The partial tick time
     */
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (browser != null && initialized) {
            browser.render(graphics, mouseX, mouseY, partialTick);
        }
    }
    
    /**
     * Loads a video from a local file or URL.
     *
     * @param videoPath The path or URL to the video
     */
    public void loadVideo(@NotNull String videoPath) {
        executeWhenInitialized(() -> {
            this.currentVideoPath = videoPath;
            LOGGER.info("[FANCYMENU] Player [{}]: Requesting JS to load video: {}", instanceId, videoPath);
            String escapedVideoPath = videoPath.replace("\\", "\\\\").replace("'", "\\'");
            String script = String.format("if(window.videoPlayerAPI && window.videoPlayerAPI.loadVideo) { window.videoPlayerAPI.loadVideo('%s'); } else { console.error('[FANCYMENU] videoPlayerAPI.loadVideo not found!'); }", escapedVideoPath);
            executeJavaScript(script);
        }, "loadVideo");
    }
    
    /**
     * Plays the currently loaded video.
     */
    public void play() {
        executeWhenInitialized(() -> {
            LOGGER.info("[FANCYMENU] Player [{}]: Requesting JS to play video.", instanceId);
            executeJavaScript("if(window.videoPlayerAPI && window.videoPlayerAPI.play) { window.videoPlayerAPI.play(); } else { console.error('[FANCYMENU] videoPlayerAPI.play not found!'); }");
        }, "play");
    }
    
    /**
     * Pauses the currently playing video.
     */
    public void pause() {
        executeWhenInitialized(() -> {
            LOGGER.info("[FANCYMENU] Player [{}]: Requesting JS to pause video.", instanceId);
            executeJavaScript("if(window.videoPlayerAPI && window.videoPlayerAPI.pause) { window.videoPlayerAPI.pause(); } else { console.error('[FANCYMENU] videoPlayerAPI.pause not found!'); }");
        }, "pause");
    }
    
    /**
     * Toggles the play/pause state of the video.
     */
    public void togglePlayPause() {
        executeWhenInitialized(() -> {
            LOGGER.info("[FANCYMENU] Player [{}]: Requesting JS to toggle play/pause.", instanceId);
            executeJavaScript("if(window.videoPlayerAPI && window.videoPlayerAPI.togglePlayPause) { window.videoPlayerAPI.togglePlayPause(); } else { console.error('[FANCYMENU] videoPlayerAPI.togglePlayPause not found!'); }");
        }, "togglePlayPause");
    }
    
    /**
     * Stops the currently playing video and resets it to the beginning.
     */
    public void stop() {
        executeWhenInitialized(() -> {
            LOGGER.info("[FANCYMENU] Player [{}]: Requesting JS to stop video.", instanceId);
            executeJavaScript("if(window.videoPlayerAPI && window.videoPlayerAPI.stop) { window.videoPlayerAPI.stop(); } else { console.error('[FANCYMENU] videoPlayerAPI.stop not found!'); }");
        }, "stop");
    }
    
    /**
     * Sets whether the audio is muted.
     *
     * @param muted True to mute audio, false to unmute
     */
    public void setMuted(boolean muted) {
        this.isMuted = muted;
        executeWhenInitialized(() -> {
            LOGGER.info("[FANCYMENU] Player [{}]: Setting muted state to {}.", instanceId, muted);
            executeJavaScript("if(window.videoPlayerAPI) { window.videoPlayerAPI.setMuted(" + muted + "); }");
        }, "setMuted");
    }
    
    /**
     * Checks if the audio is muted.
     *
     * @return True if audio is muted, false otherwise
     */
    public boolean getMuted() {
        return this.isMuted;
    }
    
    /**
     * Toggles the muted state of the audio.
     */
    public void toggleMuted() {
        setMuted(!getMuted());
    }
    
    /**
     * Sets the player volume.
     *
     * @param volume A value between 0.0 (mute) and 1.0 (full volume)
     */
    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume)); // Store new desired volume
        executeWhenInitialized(() -> {
            LOGGER.info("[FANCYMENU] Player [{}]: Setting volume to {}.", instanceId, this.volume);
            executeJavaScript("if(window.videoPlayerAPI) { window.videoPlayerAPI.setVolume(" + this.volume + "); }");
        }, "setVolume");
    }
    
    /**
     * Gets the current player volume.
     *
     * @return The volume level (0.0 to 1.0)
     */
    public float getVolume() {
        return this.volume;
    }
    
    /**
     * Enables or disables video looping.
     *
     * @param looping True to enable looping, false to disable
     */
    public void setLooping(boolean looping) {
        this.looping = looping;
        executeWhenInitialized(() -> {
            LOGGER.info("[FANCYMENU] Player [{}]: Setting looping to {}.", instanceId, looping);
            executeJavaScript("if(window.videoPlayerAPI) { window.videoPlayerAPI.setLoop(" + looping + "); }");
        }, "setLooping");
    }
    
    /**
     * Checks if video looping is enabled.
     *
     * @return True if looping is enabled, false otherwise
     */
    public boolean isLooping() {
        return this.looping;
    }
    
    /**
     * Sets whether the video should fill the screen while preserving aspect ratio.
     *
     * @param fillScreen True to enable fill screen mode, false to disable
     */
    public void setFillScreen(boolean fillScreen) {
        this.fillScreen = fillScreen;
        executeWhenInitialized(() -> {
            LOGGER.info("[FANCYMENU] Player [{}]: Setting fillScreen to {}.", instanceId, fillScreen);
            executeJavaScript("if(window.videoPlayerAPI) { window.videoPlayerAPI.setFillScreen(" + fillScreen + "); }");
        }, "setFillScreen");
    }
    
    /**
     * Checks if fill screen mode is enabled.
     *
     * @return True if fill screen is enabled, false otherwise
     */
    public boolean isFillScreen() {
        return this.fillScreen;
    }
    
    /**
     * Gets the duration of the current video in seconds.
     *
     * @return The video duration in seconds, or 0 if unknown
     */
    public double getDuration() {
        if (!initialized) {
            return 0;
        }
        String result = executeJavaScriptWithResult("(function() { try { return window.videoPlayerAPI && window.videoPlayerAPI.getDuration(); } catch(e) { return 0; } })()");
        try {
            return Double.parseDouble(result);
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Gets the duration of the current video in milliseconds.
     *
     * @return The video duration in milliseconds, or 0 if unknown
     */
    public long getDurationMillis() {
        return (long)(getDuration() * 1000);
    }
    
    /**
     * Gets the current playback position in seconds.
     *
     * @return The current position in seconds
     */
    public double getCurrentTime() {
        if (!initialized) {
            return 0;
        }
        String result = executeJavaScriptWithResult("(function() { try { return window.videoPlayerAPI && window.videoPlayerAPI.getCurrentTime(); } catch(e) { return 0; } })()");
        try {
            return Double.parseDouble(result);
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Gets the current playback position in milliseconds.
     *
     * @return The current position in milliseconds
     */
    public long getCurrentTimeMillis() {
        return (long)(getCurrentTime() * 1000);
    }
    
    /**
     * Sets the current playback position.
     *
     * @param seconds The position to seek to, in seconds
     */
    public void setCurrentTime(double seconds) {
        final double secondsFinal = Math.max(0, seconds);
        executeWhenInitialized(() -> {
            LOGGER.info("[FANCYMENU] Player [{}]: Setting video position to {} seconds.", instanceId, secondsFinal);
            executeJavaScript("if(window.videoPlayerAPI) { window.videoPlayerAPI.setCurrentTime(" + secondsFinal + "); }");
        }, "setCurrentTime");
    }
    
    /**
     * Sets the current playback position in milliseconds.
     *
     * @param milliseconds The position to seek to, in milliseconds
     */
    public void setCurrentTimeMillis(long milliseconds) {
        setCurrentTime(milliseconds / 1000.0);
    }
    
    /**
     * Seeks forward in the video by the specified number of seconds.
     *
     * @param seconds The number of seconds to seek forward
     */
    public void seekForward(double seconds) {
        executeWhenInitialized(() -> {
            LOGGER.info("[FANCYMENU] Player [{}]: Seeking forward {} seconds.", instanceId, seconds);
            executeJavaScript(
                "if(window.videoPlayerAPI) { " +
                "  var currentTime = window.videoPlayerAPI.getCurrentTime();" +
                "  var duration = window.videoPlayerAPI.getDuration();" +
                "  window.videoPlayerAPI.setCurrentTime(Math.min(currentTime + " + seconds + ", duration));" +
                "}"
            );
        }, "seekForward");
    }
    
    /**
     * Seeks backward in the video by the specified number of seconds.
     *
     * @param seconds The number of seconds to seek backward
     */
    public void seekBackward(double seconds) {
        executeWhenInitialized(() -> {
            LOGGER.info("[FANCYMENU] Player [{}]: Seeking backward {} seconds.", instanceId, seconds);
            executeJavaScript(
                "if(window.videoPlayerAPI) { " +
                "  var currentTime = window.videoPlayerAPI.getCurrentTime();" +
                "  window.videoPlayerAPI.setCurrentTime(Math.max(currentTime - " + seconds + ", 0));" +
                "}"
            );
        }, "seekBackward");
    }
    
    /**
     * Gets a formatted string of the current playback position in MM:SS format.
     *
     * @return Formatted time string (e.g., "1:45")
     */
    public String getFormattedCurrentTime() {
        return formatTime(getCurrentTime());
    }
    
    /**
     * Gets a formatted string of the video duration in MM:SS format.
     *
     * @return Formatted time string (e.g., "3:30")
     */
    public String getFormattedDuration() {
        return formatTime(getDuration());
    }
    
    /**
     * Gets a more detailed formatted string of the current playback position in HH:MM:SS format.
     * Hours are only shown if the time is at least 1 hour.
     *
     * @return Formatted time string (e.g., "1:45:30" or "12:20")
     */
    public String getDetailedFormattedCurrentTime() {
        return formatTimeDetailed(getCurrentTime());
    }
    
    /**
     * Gets a more detailed formatted string of the video duration in HH:MM:SS format.
     * Hours are only shown if the time is at least 1 hour.
     *
     * @return Formatted time string (e.g., "1:45:30" or "12:20")
     */
    public String getDetailedFormattedDuration() {
        return formatTimeDetailed(getDuration());
    }
    
    /**
     * Gets a formatted string showing both current position and total duration.
     *
     * @return Formatted time string (e.g., "1:45 / 3:30")
     */
    public String getFormattedTimeInfo() {
        return getFormattedCurrentTime() + " / " + getFormattedDuration();
    }
    
    /**
     * Gets a detailed formatted string showing both current position and total duration.
     *
     * @return Formatted time string (e.g., "1:45:30 / 3:20:15")
     */
    public String getDetailedFormattedTimeInfo() {
        return getDetailedFormattedCurrentTime() + " / " + getDetailedFormattedDuration();
    }
    
    /**
     * Gets the playback progress as a percentage (0-100).
     *
     * @return Percentage of playback progress
     */
    public int getProgressPercentage() {
        double duration = getDuration();
        if (duration <= 0) return 0;
        return (int)((getCurrentTime() / duration) * 100);
    }
    
    /**
     * Formats a time value in seconds to MM:SS format.
     *
     * @param seconds Time in seconds
     * @return Formatted time string
     */
    protected String formatTime(double seconds) {
        int totalSeconds = (int)Math.floor(seconds);
        int minutes = totalSeconds / 60;
        int remainingSeconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, remainingSeconds);
    }
    
    /**
     * Formats a time value in seconds to HH:MM:SS format (hours only shown if ≥ 1 hour).
     *
     * @param seconds Time in seconds
     * @return Formatted time string
     */
    protected String formatTimeDetailed(double seconds) {
        int totalSeconds = (int)Math.floor(seconds);
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int remainingSeconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, remainingSeconds);
        } else {
            return String.format("%d:%02d", minutes, remainingSeconds);
        }
    }
    
    /**
     * Checks if the video is currently playing.
     *
     * @return True if playing, false if paused or stopped
     */
    public boolean isPlaying() {
        if (!initialized) return false;
        String result = executeJavaScriptWithResult("(function() { try { return window.videoPlayerAPI && window.videoPlayerAPI.isPlaying(); } catch(e) { return false; } })()");
        try {
            return Boolean.parseBoolean(result);
        } catch (Exception e) {
            LOGGER.warn("[FANCYMENU] Player [{}]: Could not parse isPlaying state: {}", instanceId, result, e);
            return false; // Default to false on error
        }
    }
    
    /**
     * Gets the natural width of the video.
     *
     * @return The video width in pixels, or 0 if unknown
     */
    public int getVideoWidth() {
        if (!initialized) {
            return 0;
        }
        String result = executeJavaScriptWithResult("(function() { try { return window.videoPlayerAPI && window.videoPlayerAPI.getVideoWidth(); } catch(e) { return 0; } })()");
        try {
            return Integer.parseInt(result);
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Gets the natural height of the video.
     *
     * @return The video height in pixels, or 0 if unknown
     */
    public int getVideoHeight() {
        if (!initialized) {
            return 0;
        }
        String result = executeJavaScriptWithResult("(function() { try { return window.videoPlayerAPI && window.videoPlayerAPI.getVideoHeight(); } catch(e) { return 0; } })()");
        try {
            return Integer.parseInt(result);
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Sets the position of the video player.
     *
     * @param x The X position
     * @param y The Y position
     */
    public void setPosition(int x, int y) {
        this.posX = x;
        this.posY = y;
        if (browser != null && initialized) {
            VideoManager.EXECUTOR.execute(() -> browser.setPosition(x, y));
        }
    }
    
    /**
     * Gets the current X position of the player.
     * 
     * @return The X position
     */
    public int getX() {
        return this.posX;
    }
    
    /**
     * Gets the current Y position of the player.
     * 
     * @return The Y position
     */
    public int getY() {
        return this.posY;
    }
    
    /**
     * Sets the size of the video player.
     *
     * @param width The new width
     * @param height The new height
     */
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        if (browser != null && initialized) {
            VideoManager.EXECUTOR.execute(() -> browser.setSize(width, height));
        }
    }
    
    /**
     * Gets the current width of the player.
     * 
     * @return The width
     */
    public int getWidth() {
        return this.width;
    }
    
    /**
     * Gets the current height of the player.
     * 
     * @return The height
     */
    public int getHeight() {
        return this.height;
    }
    
    /**
     * Resizes the player to fit the specified dimensions while maintaining aspect ratio.
     * The player will be sized to fit entirely within the specified dimensions.
     * 
     * @param containerWidth The width of the container
     * @param containerHeight The height of the container
     */
    public void resizeToFit(int containerWidth, int containerHeight) {
        if (browser == null || !initialized) return;
        
        int videoWidth = getVideoWidth();
        int videoHeight = getVideoHeight();
        
        // If video dimensions are not available, use the container dimensions
        if (videoWidth <= 0 || videoHeight <= 0) {
            setSize(containerWidth, containerHeight);
            setPosition(0, 0);
            return;
        }
        
        // Calculate the scaling factors
        float scaleWidth = (float)containerWidth / videoWidth;
        float scaleHeight = (float)containerHeight / videoHeight;
        
        // Use the smaller scale factor to ensure the video fits within the container
        float scale = Math.min(scaleWidth, scaleHeight);
        
        // Calculate the new dimensions
        int newWidth = (int)(videoWidth * scale);
        int newHeight = (int)(videoHeight * scale);
        
        // Calculate position to center the video
        int x = (containerWidth - newWidth) / 2;
        int y = (containerHeight - newHeight) / 2;
        
        // Set the new size and position
        setSize(newWidth, newHeight);
        setPosition(x, y);
    }
    
    /**
     * Resizes the player to fill the specified dimensions while maintaining aspect ratio.
     * The player will be sized to fill the entire container, which might crop some content.
     * 
     * @param containerWidth The width of the container
     * @param containerHeight The height of the container
     */
    public void resizeToFill(int containerWidth, int containerHeight) {
        if (browser == null || !initialized) return;
        
        int videoWidth = getVideoWidth();
        int videoHeight = getVideoHeight();
        
        // If video dimensions are not available, use the container dimensions
        if (videoWidth <= 0 || videoHeight <= 0) {
            setSize(containerWidth, containerHeight);
            setPosition(0, 0);
            return;
        }
        
        // Calculate the scaling factors
        float scaleWidth = (float)containerWidth / videoWidth;
        float scaleHeight = (float)containerHeight / videoHeight;
        
        // Use the larger scale factor to ensure the video fills the container
        float scale = Math.max(scaleWidth, scaleHeight);
        
        // Calculate the new dimensions
        int newWidth = (int)(videoWidth * scale);
        int newHeight = (int)(videoHeight * scale);
        
        // Calculate position to center the video
        int x = (containerWidth - newWidth) / 2;
        int y = (containerHeight - newHeight) / 2;
        
        // Set the new size and position
        setSize(newWidth, newHeight);
        setPosition(x, y);
    }
    
    /**
     * Resizes the player to exactly match the container dimensions.
     * This will stretch/squash the video if the aspect ratios don't match.
     * 
     * @param containerWidth The width of the container
     * @param containerHeight The height of the container
     */
    public void resizeToStretch(int containerWidth, int containerHeight) {
        setSize(containerWidth, containerHeight);
        setPosition(0, 0);
    }
    
    /**
     * Sets the opacity/alpha value of the video player.
     * Useful for creating semi-transparent background videos.
     * 
     * @param opacity Value between 0.0F (transparent) and 1.0F (opaque)
     */
    public void setOpacity(float opacity) {
        if (browser != null && initialized) {
            VideoManager.EXECUTOR.execute(() -> browser.setOpacity(Math.max(0.0F, Math.min(1.0F, opacity))));
        }
    }
    
    /**
     * Gets the underlying MCEF browser instance.
     *
     * @return The wrapped browser instance
     */
    @Nullable
    public WrappedMCEFBrowser getBrowser() {
        return browser;
    }
    
    /**
     * Executes JavaScript in the browser.
     * Ensures it runs on the VideoManager's executor for thread safety if called from other threads.
     */
    protected void executeJavaScript(String code) {
        if (browser != null && browser.getBrowser() != null && initialized) {
            // If called from a thread other than MCEF's or main rendering thread, ensure proper dispatch.
            // Assuming VideoManager.EXECUTOR is suitable for MCEF interactions or MCEF handles it.
            browser.getBrowser().executeJavaScript(code, browser.getUrl(), 0);
        } else {
            LOGGER.warn("[FANCYMENU] Player [{}]: Attempted to execute JS when browser not ready or null. Code: {}", instanceId, code.substring(0, Math.min(50, code.length())));
        }
    }
    
    /**
     * Synchronously executes JavaScript to check for a result.
     * This implementation uses a DOM element to transfer the result back.
     *
     * @param code The JavaScript code to execute
     * @return The result as a string, or null if execution failed
     */
    @Nullable
    protected String executeJavaScriptWithResult(String code) {
        if (browser == null || browser.getBrowser() == null || !initialized) {
            LOGGER.warn("[FANCYMENU] Player [{}]: executeJavaScriptWithResult called when not ready.", instanceId);
            return null;
        }
        
        try {
            String resultVar = "_javaCallResult_" + System.nanoTime();
            String fullScript = "try { window." + resultVar + " = JSON.stringify(" + code + "); } catch(e) { window." + resultVar + " = 'JS_ERROR:' + e.toString(); }";
            browser.getBrowser().executeJavaScript(fullScript, browser.getUrl(), 0);

            // THIS IS THE CORE PROBLEM: How to get window.resultVar back to Java?
            // The original implementation using title hack or DOM attributes is problematic
            // For now, we'll use a similar approach but acknowledge its limitations

            // Create a unique identifier for this request
            String requestId = "request_" + System.currentTimeMillis();
            
            // Create an element to hold the result if it doesn't exist
            browser.getBrowser().executeJavaScript(
                    "if (!document.getElementById('javaResultHolder')) {" +
                    "  var holder = document.createElement('div');" +
                    "  holder.id = 'javaResultHolder';" +
                    "  holder.style.display = 'none';" +
                    "  document.body.appendChild(holder);" +
                    "}",
                    browser.getUrl(), 0);
            
            // Execute the code and store the result with the request ID
            browser.getBrowser().executeJavaScript(
                    "try {" +
                    "  document.getElementById('javaResultHolder').setAttribute('data-" + requestId + "', window." + resultVar + ");" +
                    "} catch(e) {" +
                    "  document.getElementById('javaResultHolder').setAttribute('data-" + requestId + "', 'ERROR:' + e.message);" +
                    "}",
                    browser.getUrl(), 0);
            
            // Give time for JavaScript to execute
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Now retrieve the result
            for (int attempt = 0; attempt < 3; attempt++) {
                browser.getBrowser().executeJavaScript(
                        "window._tempResult = document.getElementById('javaResultHolder').getAttribute('data-" + requestId + "');",
                        browser.getUrl(), 0);
                
                // Wait a little more
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // Check if result exists by setting a class on body
                browser.getBrowser().executeJavaScript(
                        "if (window._tempResult) {" +
                        "  document.body.classList.add('has-result-" + requestId + "');" +
                        "} else {" +
                        "  document.body.classList.remove('has-result-" + requestId + "');" +
                        "}",
                        browser.getUrl(), 0);
                
                // Check if the class exists
                browser.getBrowser().executeJavaScript(
                        "window._hasResult = document.body.classList.contains('has-result-" + requestId + "');",
                        browser.getUrl(), 0);
                
                // Wait again
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // Now create a simple proxy that will add the result to document title temporarily
                browser.getBrowser().executeJavaScript(
                        "if (window._hasResult && window._tempResult) {" +
                        "  var oldTitle = document.title;" +
                        "  document.title = '[MCEF_RESULT]' + window._tempResult + '[/MCEF_RESULT]';" +
                        "  setTimeout(function() { document.title = oldTitle; }, 50);" +
                        "}",
                        browser.getUrl(), 0);
                
                // Wait once more
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // Get the title which should now contain our result
                String title = browser.getBrowser().getURL(); // Using URL as a hack since we can get it
                if (title != null && title.contains("[MCEF_RESULT]")) {
                    int start = title.indexOf("[MCEF_RESULT]") + 13;
                    int end = title.indexOf("[/MCEF_RESULT]");
                    if (start >= 13 && end > start) {
                        String result = title.substring(start, end);
                        
                        // Clean up
                        browser.getBrowser().executeJavaScript(
                                "document.getElementById('javaResultHolder').removeAttribute('data-" + requestId + "');" +
                                "document.body.classList.remove('has-result-" + requestId + "');" +
                                "delete window._tempResult;" +
                                "delete window._hasResult;",
                                browser.getUrl(), 0);
                        
                        // Return the result
                        return result;
                    }
                }
                
                // If we didn't get a result, wait longer before next attempt
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            // If we get here, we couldn't get a result
            LOGGER.debug("[FANCYMENU] Could not get JavaScript result after multiple attempts");
            return null;
            
        } catch (Exception e) {
            LOGGER.debug("[FANCYMENU] Error in JavaScript execution", e);
            return null;
        }
    }
    
    /**
     * Waits for the player to initialize before executing an action.
     *
     * @param action The action to perform after initialization
     */
    protected void waitForInitialization(Runnable action) {
        CompletableFuture.runAsync(() -> {
            try {
                boolean success = initFuture.get(5, TimeUnit.SECONDS);
                if (success) {
                    action.run();
                }
            } catch (Exception e) {
                LOGGER.error("[FANCYMENU] Timeout waiting for MCEFVideoPlayer initialization", e);
            }
        });
    }
    
    /**
     * Disposes of resources used by the video player.
     * Call this when the player is no longer needed.
     */
    public void dispose() {
        executeWhenInitialized(() -> { // Ensure it's initialized before trying to stop
             LOGGER.info("[FANCYMENU] Player [{}]: Disposing.", instanceId);
             try {
                 // Call JS stop, then close browser.
                 executeJavaScript("if(window.videoPlayerAPI && window.videoPlayerAPI.stop) { window.videoPlayerAPI.stop(); }");
             } catch (Exception ex) {
                 LOGGER.error("[FANCYMENU] Player [{}]: Error stopping video during dispose", instanceId, ex);
             }
        }, "dispose.stopVideo");

        // Clean up the load handler if it's still attached (e.g., if onLoadEnd/Error never fired)
        if (browser != null && browser.getBrowser() != null && browser.getBrowser().getClient() != null && instanceSpecificLoadHandler != null) {
            browser.getBrowser().getClient().removeLoadHandler(instanceSpecificLoadHandler);
            instanceSpecificLoadHandler = null;
        }

        if (browser != null) {
            try {
                browser.close(); // This should handle MCEF browser closure
            } catch (Exception e) { // Catch IOException from close()
                LOGGER.error("[FANCYMENU] Player [{}]: Error closing MCEFVideoPlayer browser", instanceId, e);
            }
            browser = null;
        }
        initialized = false; // Mark as not initialized
        // initFuture might already be completed. If not, complete it as failure.
        if (!initFuture.isDone()) {
            initFuture.complete(false);
        }
    }
}