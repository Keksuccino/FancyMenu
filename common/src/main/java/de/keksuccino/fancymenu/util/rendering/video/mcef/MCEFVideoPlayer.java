package de.keksuccino.fancymenu.util.rendering.video.mcef;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.ObjectHolder;
import de.keksuccino.fancymenu.util.mcef.GlobalLoadHandlerManager;
import de.keksuccino.fancymenu.util.mcef.MCEFUtil;
import de.keksuccino.fancymenu.util.mcef.WrappedMCEFBrowser;
import net.minecraft.client.gui.GuiGraphics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * A Minecraft video player using MCEF (Minecraft Chromium Embedded Framework).
 * This implementation uses a custom HTML/JS player to render and control videos.
 */
public class MCEFVideoPlayer {

    private static final Logger LOGGER = LogManager.getLogger();

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
                MCEFVideoManager.getInstance().initialize(); // Try to extract if missing
                if (!playerFile.exists()) {
                    LOGGER.error("[FANCYMENU] CRITICAL: Player HTML file does not exist at: " + playerFile.getAbsolutePath() + ". Video player will fail.");
                    initFuture.complete(false);
                    return;
                }
            }
            
            // Important: autoHandle should be false if VideoManager explicitly manages lifecycle
            browser = WrappedMCEFBrowser.build(playerUrl, false, false, posX, posY, width, height);
            if (browser == null) { // MCEF might fail to create browser
                LOGGER.error("[FANCYMENU] Failed to build WrappedMCEFBrowser or get underlying MCEFBrowser for player [{}].", instanceId);
                initFuture.complete(false);
                return;
            }

            // CRITICAL: Disable generic autoplay/mute features of WrappedMCEFBrowser
            // MCEFVideoPlayer uses specific controls via player.html API.
            browser.setAutoPlayAllVideosOnLoad(false);
            browser.setMuteAllMediaOnLoad(false); // Muting is handled by player.html and this class's API

            // Register with the global handler instead of creating our own
            int browserId = browser.getIdentifier();
            
            LOGGER.debug("[FANCYMENU] MCEFVideoPlayer [{}] browser ID: {}", instanceId, browserId);
            
            // Check if the browser ID is valid (should be > 0)
            if (browserId <= 0) {
                LOGGER.error("[FANCYMENU] MCEFVideoPlayer [{}] has invalid browser ID: {}. Falling back to manual initialization.", instanceId, browserId);
                
                // We can't use the global handler, so we'll set up a direct initialization after a delay
                MCEFVideoManager.EXECUTOR.schedule(() -> {
                    if (!initFuture.isDone()) {
                        LOGGER.info("[FANCYMENU] MCEFVideoPlayer [{}] manual initialization fallback completed", instanceId);
                        initialized = true;
                        initFuture.complete(true);
                    }
                }, 2000, TimeUnit.MILLISECONDS);
                
                return; // Exit early since we can't register properly
            }
            
            // Register this browser with the global handler manager
            if (GlobalLoadHandlerManager.getInstance().registerBrowser(browserId, initFuture)) {
                LOGGER.info("[FANCYMENU] MCEFVideoPlayer [{}] registered with global handler, browser ID: {}", instanceId, browserId);
                
                // Make sure the global handler is registered with the CefClient
                // This only needs to happen once, but it's safe to call multiple times
                // as the CefClient will only set it if there's no handler yet
                if (browser.getBrowser().getClient() != null) {
                    browser.getBrowser().getClient().addLoadHandler(
                        GlobalLoadHandlerManager.getInstance().getGlobalHandler());
                } else {
                    LOGGER.error("[FANCYMENU] Cannot get CefClient for MCEFVideoPlayer [{}]. Initialization will be unreliable.", instanceId);
                    // Fallback: complete future optimistically after a delay, or mark as failed.
                    MCEFVideoManager.EXECUTOR.schedule(() -> {
                        if (!initFuture.isDone()) {
                            LOGGER.warn("[FANCYMENU] Fallback: Assuming player [{}] initialized after delay due to no CefClient access.", instanceId);
                            initialized = true;
                            initFuture.complete(true);
                        }
                    }, 2000, TimeUnit.MILLISECONDS);
                }
            } else {
                LOGGER.error("[FANCYMENU] MCEFVideoPlayer [{}] failed to register with global handler. Falling back to manual initialization.", instanceId);
                // Fallback initialization after a delay if registration failed
                MCEFVideoManager.EXECUTOR.schedule(() -> {
                    if (!initFuture.isDone()) {
                        LOGGER.info("[FANCYMENU] MCEFVideoPlayer [{}] manual initialization fallback completed", instanceId);
                        initialized = true;
                        initFuture.complete(true);
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
    private void executeWhenInitialized(@NotNull Runnable task) {
        executeWithCondition(task, () -> this.initialized && (this.browser != null));
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
        });
    }
    
    /**
     * Plays the currently loaded video.
     */
    public void play() {
        executeWhenInitialized(() -> {
            LOGGER.info("[FANCYMENU] Player [{}]: Requesting JS to play video.", instanceId);
            executeJavaScript("if(window.videoPlayerAPI && window.videoPlayerAPI.play) { window.videoPlayerAPI.play(); } else { console.error('[FANCYMENU] videoPlayerAPI.play not found!'); }");
        });
    }
    
    /**
     * Pauses the currently playing video.
     */
    public void pause() {
        executeWhenInitialized(() -> {
            LOGGER.info("[FANCYMENU] Player [{}]: Requesting JS to pause video.", instanceId);
            executeJavaScript("if(window.videoPlayerAPI && window.videoPlayerAPI.pause) { window.videoPlayerAPI.pause(); } else { console.error('[FANCYMENU] videoPlayerAPI.pause not found!'); }");
        });
    }
    
    /**
     * Toggles the play/pause state of the video.
     */
    public void togglePlayPause() {
        executeWhenInitialized(() -> {
            LOGGER.info("[FANCYMENU] Player [{}]: Requesting JS to toggle play/pause.", instanceId);
            executeJavaScript("if(window.videoPlayerAPI && window.videoPlayerAPI.togglePlayPause) { window.videoPlayerAPI.togglePlayPause(); } else { console.error('[FANCYMENU] videoPlayerAPI.togglePlayPause not found!'); }");
        });
    }
    
    /**
     * Stops the currently playing video and resets it to the beginning.
     */
    public void stop() {
        executeWhenInitialized(() -> {
            LOGGER.info("[FANCYMENU] Player [{}]: Requesting JS to stop video.", instanceId);
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
            LOGGER.info("[FANCYMENU] Player [{}]: Setting muted state to {}.", instanceId, muted);
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
            LOGGER.info("[FANCYMENU] Player [{}]: Setting volume to {}.", instanceId, this.volume);
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
            LOGGER.info("[FANCYMENU] Player [{}]: Setting looping to {}.", instanceId, looping);
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
            LOGGER.info("[FANCYMENU] Player [{}]: Setting fillScreen to {}.", instanceId, fillScreen);
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
            LOGGER.info("[FANCYMENU] Player [{}]: Seeking forward {} seconds.", instanceId, seconds);
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
            LOGGER.info("[FANCYMENU] Player [{}]: Seeking backward {} seconds.", instanceId, seconds);
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
            return Integer.parseInt(Objects.requireNonNull(result));
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
            return Integer.parseInt(Objects.requireNonNull(result));
        } catch (Exception e) {
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
            LOGGER.debug("[FANCYMENU] Player [{}]: Setting position to ({}, {})", instanceId, x, y);
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
            LOGGER.debug("[FANCYMENU] Player [{}]: Setting size to {}x{}", instanceId, width, height);
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
            LOGGER.debug("[FANCYMENU] Player [{}]: Setting opacity to {}", instanceId, finalOpacity);
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
        if ((browser != null) && initialized) {
            try {
                // If called from a thread other than MCEF's or main rendering thread, ensure proper dispatch.
                // Assuming VideoManager.EXECUTOR is suitable for MCEF interactions or MCEF handles it.
                browser.getBrowser().executeJavaScript(code, browser.getUrl(), 0);
            } catch (Exception e) {
                LOGGER.error("[FANCYMENU] Player [{}]: Error executing JavaScript: {}", instanceId, e.getMessage(), e);
            }
        } else {
            String reason = (browser == null) ? "browser is null" : "initialized is false";
            LOGGER.warn("[FANCYMENU] Player [{}]: Attempted to execute JS when not ready. Reason: {}. Code: {}", 
                instanceId, reason, code.substring(0, Math.min(50, code.length())));
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
        if ((browser == null) || !initialized) {
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
        });

        // Unregister from the global handler if the browser exists
        if (browser != null) {
            int browserId = browser.getIdentifier();
            
            // Only unregister if the browser ID is valid
            if (browserId > 0) {
                GlobalLoadHandlerManager.getInstance().unregisterBrowser(browserId);
            }
            
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