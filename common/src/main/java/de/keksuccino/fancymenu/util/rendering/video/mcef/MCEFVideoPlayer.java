package de.keksuccino.fancymenu.util.rendering.video.mcef;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.ObjectHolder;
import de.keksuccino.fancymenu.util.mcef.MCEFUtil;
import de.keksuccino.fancymenu.util.mcef.WrappedMCEFBrowser;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * A Minecraft video player using MCEF (Minecraft Chromium Embedded Framework).
 * This implementation uses a custom HTML/JS player to render and control videos.
 */
public class MCEFVideoPlayer implements Renderable {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final long JS_RESULT_TIMEOUT_MS = 1000; // Timeout for waiting for JS result (e.g., 1 second)

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
    private final String instanceId = UUID.randomUUID().toString(); // For unique JS communication if needed

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
            return;
        }
        
        // Ensure MCEFVideoManager is initialized (which now also registers the JS result handler)
        MCEFVideoManager.getInstance().initialize();
        
        try {
            String playerUrl = buildPlayerUrl();
            
            File playerFile = new File(FancyMenu.TEMP_DATA_DIR, "web/videoplayer/player.html");
            if (!playerFile.exists()) {
                LOGGER.warn("[FANCYMENU] Player HTML file not found. Attempting to extract resources.");
                MCEFVideoManager.getInstance().initialize(); // Try to extract if missing
                if (!playerFile.exists()) {
                    LOGGER.error("[FANCYMENU] CRITICAL: Player HTML file does not exist at: " + playerFile.getAbsolutePath() + ". Video player will fail.");
                    return;
                }
            }
            
            // Important: autoHandle should be false if VideoManager explicitly manages lifecycle
            this.browser = WrappedMCEFBrowser.build(playerUrl, false, false, posX, posY, width, height, success -> {
                if (success) {
                    initialized = true;
                } else {
                    LOGGER.error("[FANCYMENU] Failed to initialize MCEFVideoPlayer for browser with ID: " + (this.browser != null ? this.browser.getIdentifier() : "unknown"));
                    initialized = false;
                }
            });

            if (this.browser != null) {
                // CRITICAL: Disable generic autoplay/mute features of WrappedMCEFBrowser
                // MCEFVideoPlayer uses specific controls via player.html API.
                this.browser.setAutoPlayAllVideosOnLoad(false);
                this.browser.setMuteAllMediaOnLoad(false); // Muting is handled by player.html and this class's API
            } else {
                LOGGER.error("[FANCYMENU] MCEFVideoPlayer: Browser was not created successfully. Player will not function.");
                initialized = false; // Ensure initialized is false if browser is null
            }
            
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Failed to initialize MCEFVideoPlayer [{}]", instanceId, e);
            initialized = false;
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
    private void executeWhenInitialized(@NotNull Runnable task) {
        executeWithCondition(task, () -> this.initialized && (this.browser != null) && (this.browser.getBrowser() != null)); // Added browser.getBrowser() check
    }
    
    /**
     * Renders the video player.
     *
     * @param graphics The GUI graphics context
     * @param mouseX The mouse X position
     * @param mouseY The mouse Y position
     * @param partialTick The partial tick time
     */
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
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
            String escapedVideoPath = videoPath.replace("\\", "\\\\").replace("'", "\\'");
            String script = String.format("if(window.videoPlayerAPI && window.videoPlayerAPI.loadVideo) { window.videoPlayerAPI.loadVideo('%s'); } else { console.error('[FANCYMENU] videoPlayerAPI.loadVideo not found!'); }", escapedVideoPath);
            executeJavaScript(script);
        });
    }
    
    /**
     * Plays the currently loaded video.
     */
    public void play() {
        executeWhenInitialized(() -> {
            executeJavaScript("if(window.videoPlayerAPI && window.videoPlayerAPI.play) { window.videoPlayerAPI.play(); } else { console.error('[FANCYMENU] videoPlayerAPI.play not found!'); }");
        });
    }
    
    /**
     * Pauses the currently playing video.
     */
    public void pause() {
        executeWhenInitialized(() -> {
            executeJavaScript("if(window.videoPlayerAPI && window.videoPlayerAPI.pause) { window.videoPlayerAPI.pause(); } else { console.error('[FANCYMENU] videoPlayerAPI.pause not found!'); }");
        });
    }
    
    /**
     * Toggles the play/pause state of the video.
     */
    public void togglePlayPause() {
        executeWhenInitialized(() -> {
            executeJavaScript("if(window.videoPlayerAPI && window.videoPlayerAPI.togglePlayPause) { window.videoPlayerAPI.togglePlayPause(); } else { console.error('[FANCYMENU] videoPlayerAPI.togglePlayPause not found!'); }");
        });
    }
    
    /**
     * Stops the currently playing video and resets it to the beginning.
     */
    public void stop() {
        executeWhenInitialized(() -> {
            executeJavaScript("if(window.videoPlayerAPI && window.videoPlayerAPI.stop) { window.videoPlayerAPI.stop(); } else { console.error('[FANCYMENU] videoPlayerAPI.stop not found!'); }");
        });
    }
    
    /**
     * Sets whether the audio is muted.
     *
     * @param muted True to mute audio, false to unmute
     */
    public void setMuted(boolean muted) {
        this.isMuted = muted;
        executeWhenInitialized(() -> {
            executeJavaScript("if(window.videoPlayerAPI) { window.videoPlayerAPI.setMuted(" + muted + "); }");
        });
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
            executeJavaScript("if(window.videoPlayerAPI) { window.videoPlayerAPI.setVolume(" + this.volume + "); }");
        });
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
            executeJavaScript("if(window.videoPlayerAPI) { window.videoPlayerAPI.setLoop(" + looping + "); }");
        });
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
            executeJavaScript("if(window.videoPlayerAPI) { window.videoPlayerAPI.setFillScreen(" + fillScreen + "); }");
        });
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
        if (!initialized) return 0;
        String jsCode = "(function() { try { var dur = window.videoPlayerAPI.getDuration(); return (dur === undefined || dur === null || isNaN(dur)) ? 0 : dur; } catch(e) { console.error('Error in getDuration:', e); return 0; } })()";
        String result = executeJavaScriptWithResult(jsCode);
        if (result == null) return 0;
        try {
            return Double.parseDouble(result);
        } catch (NumberFormatException e) {
            LOGGER.warn("[FANCYMENU] Player [{}]: Could not parse duration from JS result: '{}'. JS Code: {}", instanceId, result, jsCode);
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
        if (!initialized) return 0;
        String jsCode = "(function() { try { var time = window.videoPlayerAPI.getCurrentTime(); return (time === undefined || time === null || isNaN(time)) ? 0 : time; } catch(e) { console.error('Error in getCurrentTime:', e); return 0; } })()";
        String result = executeJavaScriptWithResult(jsCode);
        if (result == null) return 0;
        try {
            return Double.parseDouble(result);
        } catch (NumberFormatException e) {
            LOGGER.warn("[FANCYMENU] Player [{}]: Could not parse current time from JS result: '{}'. JS Code: {}", instanceId, result, jsCode);
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
            executeJavaScript("if(window.videoPlayerAPI) { window.videoPlayerAPI.setCurrentTime(" + secondsFinal + "); }");
        });
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
            executeJavaScript(
                "if(window.videoPlayerAPI) { " +
                "  var currentTime = window.videoPlayerAPI.getCurrentTime();" +
                "  var duration = window.videoPlayerAPI.getDuration();" +
                "  window.videoPlayerAPI.setCurrentTime(Math.min(currentTime + " + seconds + ", duration));" +
                "}"
            );
        });
    }
    
    /**
     * Seeks backward in the video by the specified number of seconds.
     *
     * @param seconds The number of seconds to seek backward
     */
    public void seekBackward(double seconds) {
        executeWhenInitialized(() -> {
            executeJavaScript(
                "if(window.videoPlayerAPI) { " +
                "  var currentTime = window.videoPlayerAPI.getCurrentTime();" +
                "  window.videoPlayerAPI.setCurrentTime(Math.max(currentTime - " + seconds + ", 0));" +
                "}"
            );
        });
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
        String jsCode = "(function() { try { return !!(window.videoPlayerAPI && window.videoPlayerAPI.isPlaying()); } catch(e) { console.error('Error in isPlaying:', e); return false; } })()";
        String result = executeJavaScriptWithResult(jsCode);
        if (result == null) return false;
        try {
            return Boolean.parseBoolean(result);
        } catch (Exception e) {
            LOGGER.warn("[FANCYMENU] Player [{}]: Could not parse isPlaying state from JS result: '{}'. JS Code: {}", instanceId, result, jsCode, e);
            return false;
        }
    }
    
    /**
     * Gets the natural width of the video.
     *
     * @return The video width in pixels, or 0 if unknown
     */
    public int getVideoWidth() {
        if (!initialized) return 0;
        String jsCode = "(function() { try { var w = window.videoPlayerAPI.getVideoWidth(); return (w === undefined || w === null || isNaN(w)) ? 0 : w; } catch(e) { console.error('Error in getVideoWidth:', e); return 0; } })()";
        String result = executeJavaScriptWithResult(jsCode);
        if (result == null) return 0;
        try {
            return (int) Double.parseDouble(result);
        } catch (NumberFormatException e) {
            LOGGER.warn("[FANCYMENU] Player [{}]: Could not parse video width from JS result: '{}'. JS Code: {}", instanceId, result, jsCode);
            return 0;
        }
    }
    
    /**
     * Gets the natural height of the video.
     *
     * @return The video height in pixels, or 0 if unknown
     */
    public int getVideoHeight() {
        if (!initialized) return 0;
        String jsCode = "(function() { try { var h = window.videoPlayerAPI.getVideoHeight(); return (h === undefined || h === null || isNaN(h)) ? 0 : h; } catch(e) { console.error('Error in getVideoHeight:', e); return 0; } })()";
        String result = executeJavaScriptWithResult(jsCode);
        if (result == null) return 0;
        try {
            return (int) Double.parseDouble(result);
        } catch (NumberFormatException e) {
            LOGGER.warn("[FANCYMENU] Player [{}]: Could not parse video height from JS result: '{}'. JS Code: {}", instanceId, result, jsCode);
            return 0;
        }
    }
    
    /**
     * Sets the position of the browser element on the screen.
     * This positions the entire browser element, not the video content within it.
     * Can be called before initialization - will be queued and applied when ready.
     *
     * @param x The X position
     * @param y The Y position
     */
    public void setPosition(int x, int y) {
        this.posX = x;
        this.posY = y;
        // Use the executeWhenInitialized pattern to ensure it gets applied
        // even if called before the browser is ready
        executeWhenInitialized(() -> {
            browser.setPosition(x, y);
        });
    }

    /**
     * Gets the current X position of the browser element.
     *
     * @return The X position
     */
    public int getX() {
        return this.posX;
    }

    /**
     * Gets the current Y position of the browser element.
     *
     * @return The Y position
     */
    public int getY() {
        return this.posY;
    }

    /**
     * Sets the size of the browser element.
     * This sets the overall size of the browser element in the GUI.
     * If fillScreen is enabled, the video will automatically adjust to fill the browser
     * while maintaining aspect ratio.
     * Can be called before initialization - will be queued and applied when ready.
     *
     * @param width The new width
     * @param height The new height
     */
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        // Use the executeWhenInitialized pattern to ensure it gets applied
        // even if called before the browser is ready
        executeWhenInitialized(() -> {
            browser.setSize(width, height);
        });
    }

    /**
     * Gets the current width of the browser element.
     *
     * @return The width
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * Gets the current height of the browser element.
     *
     * @return The height
     */
    public int getHeight() {
        return this.height;
    }
    
    /**
     * Sets the opacity/alpha value of the video player.
     * Useful for creating semi-transparent background videos.
     * Can be called before initialization - will be queued and applied when ready.
     * 
     * @param opacity Value between 0.0F (transparent) and 1.0F (opaque)
     */
    public void setOpacity(float opacity) {
        final float finalOpacity = Math.max(0.0F, Math.min(1.0F, opacity));
        executeWhenInitialized(() -> {
            if (browser != null) {
                browser.setOpacity(finalOpacity);
            }
        });
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
        if ((browser != null) && browser.getBrowser() != null && initialized) {
            try {
                // If called from a thread other than MCEF's or main rendering thread, ensure proper dispatch.
                // Assuming VideoManager.EXECUTOR is suitable for MCEF interactions or MCEF handles it.
                browser.getBrowser().executeJavaScript(code, browser.getUrl(), 0);
            } catch (Exception e) {
                LOGGER.error("[FANCYMENU] Player [{}]: Error executing JavaScript: {}", instanceId, e.getMessage(), e);
            }
        } else {
            String reason = (browser == null) ? "browser is null" : (!initialized ? "initialized is false" : "underlying MCEF browser is null");
            LOGGER.warn("[FANCYMENU] Player [{}]: Attempted to execute JS when not ready. Reason: {}. Code: {}", 
                instanceId, reason, code.substring(0, Math.min(50, code.length())));
        }
    }
    
    /**
     * Synchronously executes JavaScript to check for a result.
     * This implementation uses console.log with a specific prefix to communicate results back to Java.
     *
     * @param jsCodeToEvaluate The JavaScript code to execute
     * @return The result as a string, or null if execution failed
     */
    @Nullable
    protected String executeJavaScriptWithResult(String jsCodeToEvaluate) {
        if (browser == null || !initialized) {
            LOGGER.warn("[FANCYMENU] Player [{}]: executeJavaScriptWithResult called when not ready (browser null or not initialized).", instanceId);
            return null;
        }
        if (browser.getBrowser() == null) {
            LOGGER.warn("[FANCYMENU] Player [{}]: executeJavaScriptWithResult called but underlying MCEF browser is null.", instanceId);
            return null;
        }

        String requestId = UUID.randomUUID().toString();
        CompletableFuture<String> resultFuture = new CompletableFuture<>();
        // Get the static map from MCEFVideoManager
        MCEFVideoManager.getPendingJsResults().put(requestId, resultFuture);

        // This JavaScript will execute the provided 'jsCodeToEvaluate',
        // then take its result and send it back via a 'console.log' message
        // with a specific prefix and the requestId.
        String script = String.format(
            "try {" +
            "  var evalResult = (%s);" + // jsCodeToEvaluate is wrapped in parentheses to ensure it's an expression
            "  console.log('MCEF_ASYNC_RESULT:%s:' + JSON.stringify(evalResult));" +
            "} catch (e) {" +
            "  console.error('MCEF_ASYNC_RESULT:%s:' + JSON.stringify({error: e.toString(), message: e.message, stack: e.stack}));" +
            "}",
            jsCodeToEvaluate, requestId, requestId // requestId is used for both success and error paths
        );

        try {
            browser.getBrowser().executeJavaScript(script, browser.getUrl(), 0);
            // Block and wait for the result from the CefDisplayHandler, with a timeout
            String result = resultFuture.get(JS_RESULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            if (result != null && result.contains("\"error\":")) { // Check if the result is a JSON error object
                LOGGER.warn("[FANCYMENU] Player [{}]: JavaScript execution for request {} resulted in an error. JS Response: {}", instanceId, requestId, result);
                return null; // Or handle error more specifically
            }
            return result; // This is the JSON.stringified result from JS
        } catch (TimeoutException e) {
            LOGGER.warn("[FANCYMENU] Player [{}]: Timeout ({}ms) waiting for JavaScript result for request {}. Original log: Could not get JavaScript result after multiple attempts", 
                         instanceId, JS_RESULT_TIMEOUT_MS, requestId);
            MCEFVideoManager.getPendingJsResults().remove(requestId); // Clean up
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("[FANCYMENU] Player [{}]: executeJavaScriptWithResult interrupted for request {}", instanceId, requestId, e);
            MCEFVideoManager.getPendingJsResults().remove(requestId); // Clean up
            return null;
        } catch (ExecutionException e) {
            LOGGER.error("[FANCYMENU] Player [{}]: JavaScript execution future completed exceptionally for request {}", instanceId, requestId, e.getCause());
            MCEFVideoManager.getPendingJsResults().remove(requestId); // Clean up
            return null;
        } catch (Exception e) { // Catch any other unexpected errors
            LOGGER.error("[FANCYMENU] Player [{}]: Unexpected error in executeJavaScriptWithResult for request {}", instanceId, requestId, e);
            MCEFVideoManager.getPendingJsResults().remove(requestId); // Clean up
            return null;
        }
    }
    
    /**
     * Disposes of resources used by the video player.
     * Call this when the player is no longer needed.
     */
    public void dispose() {
        this.stop();
        if (browser != null) {
            try {
                browser.close();
            } catch (Exception e) {
                LOGGER.error("[FANCYMENU] Player [{}]: Error closing MCEFVideoPlayer browser", instanceId, e);
            }
        }
        browser = null;
        initialized = false;
    }

    protected static void executeWithCondition(@NotNull Runnable task, @NotNull Supplier<Boolean> condition) {
        final ScheduledFuture<?>[] futureHolder = new ScheduledFuture<?>[1];
        final ObjectHolder<Boolean> executed = ObjectHolder.of(false);
        futureHolder[0] = MCEFVideoManager.EXECUTOR.scheduleAtFixedRate(() -> {
            if (!executed.get() && condition.get()) {
                task.run();
                futureHolder[0].cancel(true);
                executed.set(true);
            }
        }, 0, 50, TimeUnit.MILLISECONDS);
    }

}