package de.keksuccino.fancymenu.util.rendering.video;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.mcef.WrappedMCEFBrowser;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility class for debugging video playback issues.
 */
public class VideoDebugger {

    private static final Logger LOGGER = LogManager.getLogger();
    
    /**
     * Creates a test browser with direct HTML content for debugging.
     *
     * @param x The X position
     * @param y The Y position
     * @param width The width
     * @param height The height
     * @return The created browser or null if failed
     */
    @Nullable
    public static WrappedMCEFBrowser createTestBrowser(int x, int y, int width, int height) {
        try {
            // Test HTML content with bright colors and animated elements
            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body {
                            margin: 0;
                            padding: 0;
                            background: linear-gradient(90deg, #f00, #0f0, #00f);
                            animation: gradient 5s ease infinite;
                            background-size: 300% 300%;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            height: 100vh;
                            overflow: hidden;
                            font-family: Arial, sans-serif;
                        }
                        
                        @keyframes gradient {
                            0% { background-position: 0% 50%; }
                            50% { background-position: 100% 50%; }
                            100% { background-position: 0% 50%; }
                        }
                        
                        .container {
                            background: rgba(0,0,0,0.7);
                            padding: 30px;
                            border-radius: 10px;
                            text-align: center;
                            color: white;
                            border: 5px solid yellow;
                        }
                        
                        h1 {
                            color: yellow;
                            font-size: 28px;
                        }
                        
                        .time {
                            font-size: 20px;
                            margin-top: 20px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>MCEF Test Page</h1>
                        <p>If you can see this content, MCEF is rendering HTML correctly.</p>
                        <p>This page was generated directly via data URL.</p>
                        <div class="time" id="clock">Loading time...</div>
                    </div>
                    
                    <script>
                        // Update clock
                        function updateClock() {
                            document.getElementById('clock').textContent = new Date().toLocaleTimeString();
                        }
                        
                        updateClock();
                        setInterval(updateClock, 1000);
                        
                        // Log info to console
                        console.log('MCEF test page loaded at ' + new Date().toLocaleTimeString());
                        console.log('Browser info:', navigator.userAgent);
                        console.log('Window size:', window.innerWidth, 'x', window.innerHeight);
                    </script>
                </body>
                </html>
            """;
            
            // Create data URL
            String dataUrl = "data:text/html;charset=utf-8," + java.net.URLEncoder.encode(htmlContent, "UTF-8");
            
            // Create browser with the data URL
            WrappedMCEFBrowser browser = WrappedMCEFBrowser.build(dataUrl, false, true, x, y, width, height);
            
            LOGGER.info("[FANCYMENU] Created test browser with data URL HTML content");
            
            // Save debugging info
            writeDebugInfo("Created test browser at " + new Date().toString());
            
            return browser;
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Error creating test browser", e);
            return null;
        }
    }
    
    /**
     * Creates a diagnostic test of the video player system.
     * This writes a comprehensive report about the environment.
     */
    public static void runDiagnostics() {
        try {
            StringBuilder report = new StringBuilder();
            report.append("=== FancyMenu Video Player Diagnostics ===\n");
            report.append("Time: ").append(new Date().toString()).append("\n\n");
            
            // System info
            report.append("--- System Information ---\n");
            report.append("OS: ").append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.version")).append("\n");
            report.append("Java: ").append(System.getProperty("java.version")).append("\n");
            report.append("Memory: ").append((Runtime.getRuntime().maxMemory() / 1024 / 1024)).append("MB\n\n");
            
            // Minecraft info
            report.append("--- Minecraft Information ---\n");
            report.append("Minecraft Version: ").append(Minecraft.getInstance().getLaunchedVersion()).append("\n");
            report.append("FancyMenu Version: ").append(FancyMenu.VERSION).append("\n");
            report.append("Mod Loader: ").append(FancyMenu.MOD_LOADER).append("\n\n");
            
            // MCEF info
            report.append("--- MCEF Information ---\n");
            report.append("MCEF Loaded: ").append(VideoManager.getInstance().isVideoPlaybackAvailable()).append("\n\n");
            
            // Temp directory info
            report.append("--- Directory Information ---\n");
            report.append("Temp Directory: ").append(FancyMenu.TEMP_DATA_DIR.getAbsolutePath()).append("\n");
            report.append("Temp Directory Exists: ").append(FancyMenu.TEMP_DATA_DIR.exists()).append("\n");
            report.append("Temp Directory Writable: ").append(FancyMenu.TEMP_DATA_DIR.canWrite()).append("\n\n");
            
            // Check web directory
            File webDir = new File(FancyMenu.TEMP_DATA_DIR, "web/videoplayer");
            report.append("Web Directory: ").append(webDir.getAbsolutePath()).append("\n");
            report.append("Web Directory Exists: ").append(webDir.exists()).append("\n");
            if (webDir.exists()) {
                report.append("Web Directory Contents:\n");
                File[] files = webDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        report.append("  - ").append(file.getName())
                               .append(" (").append(file.length()).append(" bytes")
                               .append(", readable: ").append(file.canRead()).append(")\n");
                    }
                } else {
                    report.append("  [Could not list files]\n");
                }
            }
            
            // Write report to file
            File reportFile = new File(FancyMenu.TEMP_DATA_DIR, "video_player_diagnostics.txt");
            try (FileWriter writer = new FileWriter(reportFile)) {
                writer.write(report.toString());
            }
            
            LOGGER.info("[FANCYMENU] Video diagnostics report created at: " + reportFile.getAbsolutePath());
            
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Error running diagnostics", e);
        }
    }
    
    /**
     * Writes debug information to a log file.
     *
     * @param message The message to log
     */
    public static void writeDebugInfo(@NotNull String message) {
        try {
            File logFile = new File(FancyMenu.TEMP_DATA_DIR, "video_debug.log");
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
            String logEntry = timestamp + " - " + message + "\n";
            
            Files.write(logFile.toPath(), logEntry.getBytes(), java.nio.file.StandardOpenOption.APPEND);
            
        } catch (IOException e) {
            LOGGER.error("[FANCYMENU] Error writing debug info", e);
        }
    }
    
    /**
     * Creates a minimal HTML file with just a solid color background.
     * This is useful to test if any HTML content displays at all.
     *
     * @return The path to the created file, or null if creation failed
     */
    @Nullable
    public static String createMinimalHtmlTest() {
        try {
            File testDir = new File(FancyMenu.TEMP_DATA_DIR, "web/videoplayer");
            testDir.mkdirs();
            
            File testFile = new File(testDir, "minimal_test.html");
            
            // Super simple HTML with just a bright red background
            String html = "<!DOCTYPE html><html><head><style>body{background-color:red;margin:0;overflow:hidden;}</style></head><body></body></html>";
            
            try (FileWriter writer = new FileWriter(testFile)) {
                writer.write(html);
            }
            
            LOGGER.info("[FANCYMENU] Created minimal HTML test at: " + testFile.getAbsolutePath());
            return testFile.getAbsolutePath();
            
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Failed to create minimal HTML test", e);
            return null;
        }
    }
}