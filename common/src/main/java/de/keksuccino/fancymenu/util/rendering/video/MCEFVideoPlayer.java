package de.keksuccino.fancymenu.util.rendering.video;

import de.keksuccino.fancymenu.FancyMenu;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * A Minecraft video player using MCEF (Minecraft Chromium Embedded Framework).
 * This implementation uses a custom HTML/JS player to render and control videos.
 */
public class MCEFVideoPlayer {

    protected static final Logger LOGGER = LogManager.getLogger();
    
    // The browser instance wrapped by MCEF
    protected WrappedMCEFBrowser browser;
    
    // Video options
    protected volatile float volume = 1.0f;
    protected volatile boolean looping = false;
    protected volatile boolean fillScreen = false;
    protected volatile String currentVideoPath = null;
    protected volatile boolean isCurrentlyPlaying = false;
    
    // Browser dimensions
    protected volatile int posX = 0;
    protected volatile int posY = 0;
    protected volatile int width = 200;
    protected volatile int height = 200;
    
    // Initialization state
    protected volatile boolean initialized = false;
    protected final CompletableFuture<Boolean> initFuture = new CompletableFuture<>();
    
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
            // Build the initial URL with parameters
            String playerUrl = buildPlayerUrl();
            LOGGER.info("[FANCYMENU] Initializing video player with URL: " + playerUrl);
            
            // Check if the player file exists
            File playerFile = new File(FancyMenu.TEMP_DATA_DIR, "web/videoplayer/player.html");
            if (playerFile.exists()) {
                LOGGER.info("[FANCYMENU] Player HTML file exists at: " + playerFile.getAbsolutePath());
            } else {
                LOGGER.error("[FANCYMENU] Player HTML file does not exist at: " + playerFile.getAbsolutePath());
            }
            
            // Create the browser instance - set transparency to false for testing
            browser = WrappedMCEFBrowser.build(playerUrl, false, true, posX, posY, width, height);
            
            // Apply initial settings
            browser.setHideVideoControls(true);
            browser.setVolume(volume);
            browser.setLoopAllVideos(looping);
            
            // Initialize JavaScript API settings after a short delay to ensure browser is ready
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(500);
                    
                    // Set initial API state to match our Java-side settings
                    String initScript = 
                        "if (window.videoPlayerAPI) {" +
                        "  console.log('[FANCYMENU] Initializing video player API settings');" +
                        "  window.videoPlayerAPI.currentSettings.volume = " + volume + ";" +
                        "  window.videoPlayerAPI.currentSettings.loop = " + looping + ";" + 
                        "  window.videoPlayerAPI.currentSettings.fillScreen = " + fillScreen + ";" +
                        "  window.videoPlayerAPI.currentSettings.muted = " + isMuted + ";" +
                        "  console.log('[FANCYMENU] Initial settings stored in API');" +
                        "} else {" +
                        "  console.log('[FANCYMENU] Warning: videoPlayerAPI not available yet');" +
                        "}";
                    browser.getBrowser().executeJavaScript(initScript, browser.getUrl(), 0);
                } catch (Exception e) {
                    LOGGER.error("[FANCYMENU] Error setting initial API state", e);
                }
            });
            
            initialized = true;
            initFuture.complete(true);
            
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Failed to initialize MCEFVideoPlayer", e);
            initFuture.complete(false);
        }
    }
    
    /**
     * Builds the URL for the video player with initial parameters.
     *
     * @return The URL to load in the browser
     */
    protected String buildPlayerUrl() {
        // Use a file:// URL to access the player HTML from FancyMenu's temp directory
        File playerFile = new File(FancyMenu.TEMP_DATA_DIR, "web/videoplayer/player.html");
        String basePath = "file:///" + playerFile.getAbsolutePath().replace('\\', '/');
        
        // Build parameters
        Map<String, String> params = new HashMap<>();
        params.put("volume", String.valueOf(volume));
        params.put("loop", String.valueOf(looping));
        params.put("fillScreen", String.valueOf(fillScreen));
        params.put("autoPlay", String.valueOf(true)); // Force autoPlay to true
        
        // Don't include video in initial URL - we'll load it via JavaScript
        // Directly including video path in URL can cause issues
        
        // Append parameters to URL
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
        if (!initialized) {
            waitForInitialization(() -> loadVideo(videoPath));
            return;
        }
        
        try {
            this.currentVideoPath = videoPath;
            LOGGER.info("[FANCYMENU] Loading video: " + videoPath);
            
            // Try a more direct approach to load the video - directly set the video src
            // Convert file path to URI if needed
            String finalPath = videoPath;
            if (!videoPath.startsWith("http") && !videoPath.startsWith("file:")) {
                File videoFile = new File(videoPath);
                if (videoFile.exists()) {
                    finalPath = videoFile.toURI().toString();
                    LOGGER.info("[FANCYMENU] Converted video path to: " + finalPath);
                }
            }
            
            // Direct, simplified approach that bypasses most of the API layer
            String loadScript = 
                "try {\n" +
                "  console.log('[FANCYMENU] Loading video with direct method: " + finalPath + "');\n" +
                "  var video = document.getElementById('videoPlayer');\n" +
                "  if (video) {\n" +
                "    // Reset video state\n" +
                "    video.pause();\n" +
                "    video.currentTime = 0;\n" +
                "    video.removeAttribute('src');\n" +
                "    video.load();\n" +
                "    \n" +
                "    // Set new source\n" +
                "    video.src = '" + finalPath.replace("'", "\\'") + "';\n" +
                "    \n" +
                "    // Force preload\n" +
                "    video.preload = 'auto';\n" +
                "    \n" +
                "    // Set proper attributes\n" +
                "    video.setAttribute('playsinline', '');\n" +
                "    video.setAttribute('webkit-playsinline', '');\n" +
                "    video.crossOrigin = 'anonymous';\n" +
                "    \n" +
                "    // Apply settings\n" +
                "    video.volume = " + volume + ";\n" +
                "    video.loop = " + looping + ";\n" +
                "    video.muted = " + isMuted + ";\n" +
                "    \n" +
                "    // Start loading\n" +
                "    video.load();\n" +
                "    console.log('[FANCYMENU] Video element configured with source: ' + video.src);\n" +
                "    \n" +
                "    // Store in API if it exists\n" +
                "    if (window.videoPlayerAPI) {\n" +
                "      window.videoPlayerAPI.currentSettings.videoLoaded = true;\n" +
                "    }\n" +
                "  } else {\n" +
                "    console.error('[FANCYMENU] Video element not found');\n" +
                "  }\n" +
                "} catch(e) {\n" +
                "  console.error('[FANCYMENU] Error in direct video loading: ' + e.message);\n" +
                "}";
            
            executeJavaScript(loadScript);
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Failed to load video: " + videoPath, e);
        }
    }
    
    /**
     * Applies all current settings to the video player after a video is loaded.
     * This ensures settings are properly applied to newly loaded videos.
     * 
     * @param shouldAutoPlay Whether to automatically start playing the video after loading
     */
    protected void applySettingsAfterLoad(boolean shouldAutoPlay) {
        // Use a small delay to ensure the video is actually loaded
        CompletableFuture.runAsync(() -> {
            try {
                // Wait for video to start loading
                Thread.sleep(500);
                
                // Check if video element exists and has a source
                executeJavaScript(
                    "try {\n" +
                    "  console.log('[FANCYMENU] Checking video element status');\n" +
                    "  var video = document.getElementById('videoPlayer');\n" +
                    "  if (video) {\n" +
                    "    console.log('[FANCYMENU] Video element exists, src: ' + (video.src || 'none') + ', readyState: ' + video.readyState);\n" +
                    "    // Try to force a proper load if src is set but readyState is 0\n" +
                    "    if (video.src && video.readyState === 0) {\n" +
                    "      console.log('[FANCYMENU] Forcing video load');\n" +
                    "      video.load();\n" +
                    "    }\n" +
                    "  } else {\n" +
                    "    console.error('[FANCYMENU] Video element not found');\n" +
                    "  }\n" +
                    "} catch(e) {\n" +
                    "  console.error('[FANCYMENU] Error checking video status: ' + e);\n" +
                    "}"
                );
                
                // Re-apply all current settings
                LOGGER.info("[FANCYMENU] Re-applying settings to newly loaded video");
                setVolume(volume);
                setLooping(looping);
                setFillScreen(fillScreen);
                setMuted(isMuted);
                
                // If we should autoplay but the video isn't playing, try more aggressively
                if (shouldAutoPlay) {
                    LOGGER.info("[FANCYMENU] Auto-playing video after load (delayed attempt)");
                    
                    // Try additional direct method
                    executeJavaScript(
                        "try {\n" +
                        "  console.log('[FANCYMENU] Direct play attempt...');\n" +
                        "  var video = document.getElementById('videoPlayer');\n" +
                        "  if (video) {\n" +
                        "    if (!video.src) {\n" +
                        "      console.log('[FANCYMENU] No source set yet, skipping play attempt');\n" +
                        "    } else {\n" +
                        "      console.log('[FANCYMENU] Source exists: ' + video.src);\n" +
                        "      video.load();\n" +
                        "      video.play().catch(e => {\n" +
                        "        console.error('[FANCYMENU] Play attempt 1 failed: ' + e);\n" +
                        "        // Try muted play\n" +
                        "        video.muted = true;\n" +
                        "        video.play().catch(e2 => {\n" +
                        "          console.error('[FANCYMENU] Muted play failed: ' + e2);\n" +
                        "          // Try with additional attributes\n" +
                        "          video.setAttribute('playsinline', '');\n" +
                        "          video.setAttribute('webkit-playsinline', '');\n" +
                        "          video.play();\n" +
                        "        });\n" +
                        "      });\n" +
                        "    }\n" +
                        "  } else {\n" +
                        "    console.error('[FANCYMENU] Video element not found in direct play attempt');\n" +
                        "  }\n" +
                        "} catch(e) {\n" +
                        "  console.error('[FANCYMENU] Error in direct play: ' + e);\n" +
                        "}"
                    );
                    
                    // Also call the regular play method
                    play();
                    
                    // Try once more after another delay with a more direct approach
                    Thread.sleep(2000);
                    
                    LOGGER.info("[FANCYMENU] Final play attempt");
                    executeJavaScript(
                        "try {\n" +
                        "  var video = document.getElementById('videoPlayer');\n" +
                        "  if (video && video.paused) {\n" +
                        "    console.log('[FANCYMENU] Final direct play attempt');\n" +
                        "    // For final attempt, set autoplay attribute and reload\n" +
                        "    video.setAttribute('autoplay', '');\n" +
                        "    video.muted = true;\n" +
                        "    // Store current time\n" +
                        "    var currentTime = video.currentTime;\n" +
                        "    var currentSrc = video.src;\n" +
                        "    // Force a reload\n" +
                        "    video.src = currentSrc;\n" +
                        "    video.load();\n" +
                        "    video.play();\n" +
                        "    setTimeout(function() {\n" +
                        "      video.muted = " + isMuted + ";\n" +
                        "    }, 1000);\n" +
                        "  }\n" +
                        "} catch(e) {\n" +
                        "  console.error('[FANCYMENU] Error in final play attempt: ' + e);\n" +
                        "}"
                    );
                    
                    // Update our state to match what we expect
                    isCurrentlyPlaying = true;
                }
            } catch (Exception e) {
                LOGGER.error("[FANCYMENU] Error applying settings after video load", e);
            }
        });
    }
    
    /**
     * Applies all current settings to the video player after a video is loaded.
     * Keeps the previous playing state.
     */
    protected void applySettingsAfterLoad() {
        applySettingsAfterLoad(isCurrentlyPlaying);
    }
    
    // Track muted state locally
    protected boolean isMuted = false;
    
    /**
     * Sets whether the audio is muted.
     *
     * @param muted True to mute audio, false to unmute
     */
    public void setMuted(boolean muted) {
        if (!initialized) {
            this.isMuted = muted;
            waitForInitialization(() -> setMuted(muted));
            return;
        }
        
        this.isMuted = muted;
        LOGGER.info("[FANCYMENU] Setting muted state to: " + muted);
        executeJavaScript("window.videoPlayerAPI.setMuted(" + muted + ")");
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
     * Plays the currently loaded video.
     */
    public void play() {
        if (!initialized) {
            waitForInitialization(this::play);
            return;
        }
        
        LOGGER.info("[FANCYMENU] Playing video");
        
        // Use the most direct approach possible to play the video
        String playScript = 
            "try {\n" +
            "  console.log('[FANCYMENU] Attempting to play video directly');\n" +
            "  var video = document.getElementById('videoPlayer');\n" +
            "  if (video) {\n" +
            "    // First ensure the video has a source\n" +
            "    if (!video.src && '" + currentVideoPath + "' != 'null') {\n" +
            "      console.log('[FANCYMENU] Setting missing video source: " + currentVideoPath + "');\n" +
            "      video.src = '" + (currentVideoPath != null ? currentVideoPath.replace("'", "\\'") : "") + "';\n" +
            "      video.load();\n" +
            "    }\n" +
            "    \n" +
            "    // Multiple play attempts with different approaches\n" +
            "    var playPromise = video.play();\n" +
            "    \n" +
            "    if (playPromise !== undefined) {\n" +
            "      playPromise.then(function() {\n" +
            "        console.log('[FANCYMENU] Video playback started successfully');\n" +
            "      }).catch(function(error) {\n" +
            "        console.error('[FANCYMENU] Play failed: ' + error);\n" +
            "        \n" +
            "        // First fallback: try with muted\n" +
            "        console.log('[FANCYMENU] Trying muted autoplay...');\n" +
            "        video.muted = true;\n" +
            "        \n" +
            "        video.play().then(function() {\n" +
            "          console.log('[FANCYMENU] Muted playback succeeded');\n" +
            "          // Try to unmute after a moment\n" +
            "          setTimeout(function() {\n" +
            "            video.muted = " + isMuted + ";\n" +
            "          }, 1000);\n" +
            "        }).catch(function(error2) {\n" +
            "          console.error('[FANCYMENU] Even muted playback failed: ' + error2);\n" +
            "          \n" +
            "          // Try inline attribute as last resort\n" +
            "          video.setAttribute('playsinline', '');\n" +
            "          video.setAttribute('webkit-playsinline', '');\n" +
            "          video.setAttribute('autoplay', '');\n" +
            "          video.load(); // Reload to apply autoplay attribute\n" +
            "        });\n" +
            "      });\n" +
            "    } else {\n" +
            "      console.log('[FANCYMENU] Play method did not return a promise');\n" +
            "    }\n" +
            "  } else {\n" +
            "    console.error('[FANCYMENU] Video element not found');\n" +
            "  }\n" +
            "} catch(e) {\n" +
            "  console.error('[FANCYMENU] Error during play attempt: ' + e);\n" +
            "}";
        
        executeJavaScript(playScript);
        isCurrentlyPlaying = true;
    }
    
    /**
     * Pauses the currently playing video.
     */
    public void pause() {
        if (!initialized) {
            waitForInitialization(this::pause);
            return;
        }
        
        LOGGER.info("[FANCYMENU] Pausing video");
        executeJavaScript("window.videoPlayerAPI.pause()");
        isCurrentlyPlaying = false;
    }
    
    /**
     * Toggles the play/pause state of the video.
     */
    public void togglePlayPause() {
        if (!initialized) {
            waitForInitialization(this::togglePlayPause);
            return;
        }
        
        // For the first toggle, check the actual state from the video element
        String script = "try {" +
                      "  const video = document.getElementById('videoPlayer');" +
                      "  if (video) {" +
                      "    return video.paused ? 'paused' : 'playing';" +
                      "  }" +
                      "  return 'unknown';" +
                      "} catch(e) { return 'error'; }";
        
        String result = executeJavaScriptWithResult("(function() { " + script + " })()");
        
        if (result != null) {
            // Update our state based on the actual video state
            boolean actuallyPlaying = "playing".equals(result);
            
            // Only log if there's a mismatch
            if (actuallyPlaying != isCurrentlyPlaying) {
                LOGGER.info("[FANCYMENU] Correcting play state mismatch: was " + 
                           isCurrentlyPlaying + ", actually " + actuallyPlaying);
                isCurrentlyPlaying = actuallyPlaying;
            }
        }
        
        // Now toggle based on the corrected state
        if (isCurrentlyPlaying) {
            pause();
        } else {
            play();
        }
    }
    
    /**
     * Stops the currently playing video and resets it to the beginning.
     */
    public void stop() {
        if (!initialized) {
            waitForInitialization(this::stop);
            return;
        }
        
        LOGGER.info("[FANCYMENU] Stopping video");
        executeJavaScript("window.videoPlayerAPI.stop()");
        isCurrentlyPlaying = false;
    }
    
    /**
     * Synchronizes the play state with the actual browser state.
     * Call this periodically to ensure the Java state matches the browser.
     */
    public void syncPlayState() {
        if (!initialized) return;
        
        // Use a simple script to check if the video is playing or paused
        String script = "try {" +
                      "  const video = document.getElementById('videoPlayer');" +
                      "  if (video) {" +
                      "    return !video.paused;" +
                      "  }" +
                      "  return false;" +
                      "} catch(e) { return false; }";
        
        String result = executeJavaScriptWithResult("(function() { " + script + " })()");
        
        try {
            if (result != null && !result.isEmpty()) {
                boolean actuallyPlaying = Boolean.parseBoolean(result);
                if (isCurrentlyPlaying != actuallyPlaying) {
                    LOGGER.debug("[FANCYMENU] Syncing play state: Java=" + isCurrentlyPlaying + ", Browser=" + actuallyPlaying);
                    isCurrentlyPlaying = actuallyPlaying;
                }
            }
        } catch (Exception e) {
            // Silently ignore parsing errors
        }
    }
    
    /**
     * Sets the player volume.
     *
     * @param volume A value between 0.0 (mute) and 1.0 (full volume)
     */
    public void setVolume(float volume) {
        if (!initialized) {
            this.volume = volume;
            waitForInitialization(() -> setVolume(volume));
            return;
        }
        
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
        browser.setVolume(this.volume);
        executeJavaScript("window.videoPlayerAPI.setVolume(" + this.volume + ")");
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
        if (!initialized) {
            this.looping = looping;
            waitForInitialization(() -> setLooping(looping));
            return;
        }
        
        this.looping = looping;
        browser.setLoopAllVideos(looping);
        executeJavaScript("window.videoPlayerAPI.setLoop(" + looping + ")");
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
        if (!initialized) {
            this.fillScreen = fillScreen;
            waitForInitialization(() -> setFillScreen(fillScreen));
            return;
        }
        
        this.fillScreen = fillScreen;
        executeJavaScript("window.videoPlayerAPI.setFillScreen(" + fillScreen + ")");
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
        
        String result = executeJavaScriptWithResult("(function() { return window.videoPlayerAPI ? window.videoPlayerAPI.getDuration() : 0; })()");
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
        
        String result = executeJavaScriptWithResult("(function() { return window.videoPlayerAPI ? window.videoPlayerAPI.getCurrentTime() : 0; })()");
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
        if (!initialized) {
            double finalSeconds = seconds;
            waitForInitialization(() -> setCurrentTime(finalSeconds));
            return;
        }
        
        LOGGER.info("[FANCYMENU] Setting video position to " + seconds + " seconds");
        
        // Make sure we have a valid value
        seconds = Math.max(0, seconds);
        
        // Use a more direct approach to set the video time
        String script = "try {" +
                        "  const videoElement = document.getElementById('videoPlayer');" +
                        "  if (videoElement) {" +
                        "    videoElement.currentTime = " + seconds + ";" +
                        "    console.log('[FANCYMENU] Video time set to: " + seconds + "');" +
                        "  } else {" +
                        "    console.error('[FANCYMENU] Video element not found');" +
                        "  }" +
                        "} catch(e) {" +
                        "  console.error('[FANCYMENU] Error setting time: ' + e.message);" +
                        "}";
        
        executeJavaScript(script);
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
        if (!initialized) return;
        
        // Use a direct approach to seek forward
        String script = "try {" +
                        "  const video = document.getElementById('videoPlayer');" +
                        "  if (video) {" +
                        "    const currentTime = video.currentTime;" +
                        "    const duration = video.duration || 0;" +
                        "    const newTime = Math.min(currentTime + " + seconds + ", duration);" +
                        "    video.currentTime = newTime;" +
                        "    console.log('[FANCYMENU] Seeking forward to: ' + newTime);" +
                        "  }" +
                        "} catch(e) {" +
                        "  console.error('[FANCYMENU] Error seeking forward: ' + e.message);" +
                        "}";
        
        executeJavaScript(script);
    }
    
    /**
     * Seeks backward in the video by the specified number of seconds.
     *
     * @param seconds The number of seconds to seek backward
     */
    public void seekBackward(double seconds) {
        if (!initialized) return;
        
        // Use a direct approach to seek backward
        String script = "try {" +
                        "  const video = document.getElementById('videoPlayer');" +
                        "  if (video) {" +
                        "    const currentTime = video.currentTime;" +
                        "    const newTime = Math.max(currentTime - " + seconds + ", 0);" +
                        "    video.currentTime = newTime;" +
                        "    console.log('[FANCYMENU] Seeking backward to: ' + newTime);" +
                        "  }" +
                        "} catch(e) {" +
                        "  console.error('[FANCYMENU] Error seeking backward: ' + e.message);" +
                        "}";
        
        executeJavaScript(script);
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
        // Return local state, which should be kept in sync
        if (!initialized) return false;
        
        // This is a lightweight call - we only check the actual state occasionally
        if (!stateVerified && initialized) {
            syncPlayState();
            stateVerified = true;
        }
        
        return isCurrentlyPlaying;
    }
    
    // Flag to track if we've verified the state at least once
    private boolean stateVerified = false;
    
    /**
     * Gets the natural width of the video.
     *
     * @return The video width in pixels, or 0 if unknown
     */
    public int getVideoWidth() {
        if (!initialized) {
            return 0;
        }
        
        String result = executeJavaScriptWithResult("(function() { return window.videoPlayerAPI ? window.videoPlayerAPI.getVideoWidth() : 0; })()");
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
        
        String result = executeJavaScriptWithResult("(function() { return window.videoPlayerAPI ? window.videoPlayerAPI.getVideoHeight() : 0; })()");
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
            browser.setPosition(x, y);
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
            browser.setSize(width, height);
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
            browser.setOpacity(Math.max(0.0F, Math.min(1.0F, opacity)));
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
     *
     * @param code The JavaScript code to execute
     */
    protected void executeJavaScript(String code) {
        if (browser != null && initialized) {
            browser.getBrowser().executeJavaScript(code, browser.getUrl(), 0);
        }
    }
    
    /**
     * Synchronously executes JavaScript to check for a result.
     * This implementation uses a DOM element to transfer the result back.
     *
     * @param code The JavaScript code to execute
     * @return The result as a string, or null if execution failed
     */
    protected String executeJavaScriptWithResult(String code) {
        if (browser == null) {
            return null;
        }
        
        try {
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
                    "  var result = " + code + ";" +
                    "  document.getElementById('javaResultHolder').setAttribute('data-" + requestId + "', " +
                    "    typeof result === 'object' ? JSON.stringify(result) : String(result));" +
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
     * Loads the simple test HTML file for debugging purposes.
     * This can help determine if the HTML files are loading correctly.
     */
    public void loadSimpleTest() {
        if (!initialized) {
            waitForInitialization(this::loadSimpleTest);
            return;
        }
        
        try {
            // Try using a data URL instead of a file URL
            String htmlContent = "<!DOCTYPE html><html><head><style>body{background:red;color:white;font-size:30px;display:flex;justify-content:center;align-items:center;height:100vh;margin:0;}</style></head><body>MCEF TEST PAGE</body></html>";
            String dataUrl = "data:text/html;charset=utf-8," + java.net.URLEncoder.encode(htmlContent, "UTF-8");
            
            LOGGER.info("[FANCYMENU] Loading inline test page with data URL");
            browser.setUrl(dataUrl);
            
            // Also try to load a direct file URL as backup
            File testFile = new File(FancyMenu.TEMP_DATA_DIR, "web/videoplayer/simple_test.html");
            if (testFile.exists()) {
                LOGGER.info("[FANCYMENU] File exists at: " + testFile.getAbsolutePath());
                String testUrl = "file:///" + testFile.getAbsolutePath().replace('\\', '/');
                
                // Add a delayed load of the file URL as backup
                new Thread(() -> {
                    try {
                        Thread.sleep(5000); // Wait 5 seconds
                        LOGGER.info("[FANCYMENU] Trying backup method with direct file URL: " + testUrl);
                        browser.setUrl(testUrl);
                    } catch (Exception e) {
                        LOGGER.error("[FANCYMENU] Error in delayed URL load", e);
                    }
                }).start();
            } else {
                LOGGER.error("[FANCYMENU] Test file does not exist at: " + testFile.getAbsolutePath());
            }
            
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Failed to load simple test page", e);
        }
    }
    
    /**
     * Disposes of resources used by the video player.
     * Call this when the player is no longer needed.
     */
    public void dispose() {
        if (browser != null) {
            try {
                browser.close();
            } catch (Exception e) {
                LOGGER.error("[FANCYMENU] Error closing MCEFVideoPlayer browser", e);
            }
        }
    }
}