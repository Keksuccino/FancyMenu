package de.keksuccino.fancymenu.util.rendering.video;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.mcef.MCEFUtil;
import de.keksuccino.fancymenu.util.mcef.WrappedMCEFBrowser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A Minecraft video player using MCEF (Minecraft Chromium Embedded Framework).
 * This implementation uses a custom HTML/JS player to render and control videos.
 */
public class MCEFVideoPlayer {

    private static final Logger LOGGER = LogManager.getLogger();
    
    // The browser instance wrapped by MCEF
    private WrappedMCEFBrowser browser;
    
    // Video options
    private float volume = 1.0f;
    private boolean looping = false;
    private boolean fillScreen = false;
    private String currentVideoPath = null;
    
    // Browser dimensions
    private int posX = 0;
    private int posY = 0;
    private int width = 200;
    private int height = 200;
    
    // Initialization state
    private boolean initialized = false;
    private final CompletableFuture<Boolean> initFuture = new CompletableFuture<>();
    
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
            
            // Add logging script to help debug
            String debugScript = "console.log('FancyMenu Video Player loaded'); " +
                                "document.body.style.backgroundColor = 'blue'; " +
                                "document.body.insertAdjacentHTML('beforeend', '<div style=\"color:white;padding:20px;\">Video Player Debug</div>');";
            browser.getBrowser().executeJavaScript(debugScript, browser.getUrl(), 0);
            
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
    private String buildPlayerUrl() {
        // Use a file:// URL to access the player HTML from FancyMenu's temp directory
        File playerFile = new File(FancyMenu.TEMP_DATA_DIR, "web/videoplayer/player.html");
        String basePath = "file:///" + playerFile.getAbsolutePath().replace('\\', '/');
        
        // Build parameters
        Map<String, String> params = new HashMap<>();
        params.put("volume", String.valueOf(volume));
        params.put("loop", String.valueOf(looping));
        params.put("fillScreen", String.valueOf(fillScreen));
        
        if (currentVideoPath != null) {
            params.put("video", currentVideoPath);
        }
        
        // Append parameters to URL
        return basePath + "?" + buildQueryString(params);
    }
    
    /**
     * Converts a map of parameters to a URL query string.
     *
     * @param params The parameters to include
     * @return The URL-encoded query string
     */
    private String buildQueryString(Map<String, String> params) {
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
            
            // If it's a file path, convert to proper file:/// URL
            if (!videoPath.startsWith("http")) {
                File videoFile = new File(videoPath);
                if (videoFile.exists()) {
                    URI uri = videoFile.toURI();
                    String fileUrl = uri.toURL().toString();
                    executeJavaScript("window.videoPlayerAPI.loadVideo('" + fileUrl + "')");
                    return;
                }
            }
            
            // Otherwise, assume it's already a valid URL
            executeJavaScript("window.videoPlayerAPI.loadVideo('" + videoPath + "')");
            
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Failed to load video: " + videoPath, e);
        }
    }
    
    /**
     * Plays the currently loaded video.
     */
    public void play() {
        if (!initialized) {
            waitForInitialization(this::play);
            return;
        }
        
        executeJavaScript("window.videoPlayerAPI.play()");
    }
    
    /**
     * Pauses the currently playing video.
     */
    public void pause() {
        if (!initialized) {
            waitForInitialization(this::pause);
            return;
        }
        
        executeJavaScript("window.videoPlayerAPI.pause()");
    }
    
    /**
     * Stops the currently playing video and resets it to the beginning.
     */
    public void stop() {
        if (!initialized) {
            waitForInitialization(this::stop);
            return;
        }
        
        executeJavaScript("window.videoPlayerAPI.stop()");
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
        
        String result = executeJavaScriptWithResult("return window.videoPlayerAPI.getDuration()");
        try {
            return Double.parseDouble(result);
        } catch (Exception e) {
            return 0;
        }
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
        
        String result = executeJavaScriptWithResult("return window.videoPlayerAPI.getCurrentTime()");
        try {
            return Double.parseDouble(result);
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Sets the current playback position.
     *
     * @param seconds The position to seek to, in seconds
     */
    public void setCurrentTime(double seconds) {
        if (!initialized) {
            waitForInitialization(() -> setCurrentTime(seconds));
            return;
        }
        
        executeJavaScript("window.videoPlayerAPI.setCurrentTime(" + seconds + ")");
    }
    
    /**
     * Checks if the video is currently playing.
     *
     * @return True if playing, false if paused or stopped
     */
    public boolean isPlaying() {
        if (!initialized) {
            return false;
        }
        
        String result = executeJavaScriptWithResult("return window.videoPlayerAPI.isPlaying()");
        return Boolean.parseBoolean(result);
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
        
        String result = executeJavaScriptWithResult("return window.videoPlayerAPI.getVideoWidth()");
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
        
        String result = executeJavaScriptWithResult("return window.videoPlayerAPI.getVideoHeight()");
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
    private void executeJavaScript(String code) {
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
    private String executeJavaScriptWithResult(String code) {
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
    private void waitForInitialization(Runnable action) {
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