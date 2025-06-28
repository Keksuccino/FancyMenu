package de.keksuccino.fancymenu.util;

import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class WebUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    private static volatile boolean isConnectionAvailable = true;

    public static void init() {

        new Thread(() -> {
            try {
                isConnectionAvailable = _isInternetAvailable();
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to update the cached internet availability value!", ex);
            }
            try {
                Thread.sleep(20000); // 20 seconds
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to sleep after updating cached internet availability value!", ex);
            }
        }, "FancyMenu-WebUtils-Thread").start();

    }

    /**
     * Checks if an internet connection is available.
     * The method can be called in the main thread, since the value it returns is updated asynchronously every 20 seconds.
     *
     * @return true if an internet connection is available, false otherwise
     */
    public static boolean isInternetAvailable() {
        return isConnectionAvailable;
    }

    /**
     * Checks if an internet connection is available.
     * Uses a 3-second timeout for both connection and read operations.
     *
     * @return true if an internet connection is available, false otherwise
     */
    private static boolean _isInternetAvailable() {
        try {
            var url = new URL("https://docs.fancymenu.net");
            var connection = (HttpURLConnection) url.openConnection();

            // Using 3 seconds (3000ms) as a reasonable timeout
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setRequestMethod("HEAD");

            int responseCode = connection.getResponseCode();
            return (responseCode >= 200 && responseCode < 300);
        } catch (IOException e) {
            return false;
        }
    }

    @Nullable
    public static InputStream openResourceStream(@NotNull String resourceURL) {
        try {
            URL actualURL = new URL(resourceURL);
            HttpURLConnection connection = (HttpURLConnection)actualURL.openConnection();
            connection.addRequestProperty("User-Agent", "Mozilla/4.0");
            return connection.getInputStream();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Nullable
    public static String getMimeType(@NotNull String url) {
        try {
            URL url2 = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) url2.openConnection();
            String mimeType = connection.getContentType();
            connection.disconnect();
            return mimeType;
        } catch (Exception ignore) {}
        return null;
    }

    public static boolean isValidUrl(@Nullable String url) {
        if ((url != null) && (url.startsWith("http://") || url.startsWith("https://"))) {
            try {
                URL u = new URL(url);
                HttpURLConnection c = (HttpURLConnection)u.openConnection();
                c.addRequestProperty("User-Agent", "Mozilla/4.0");
                c.setRequestMethod("HEAD");
                int r = c.getResponseCode();
                if (r == 200) {
                    return true;
                }
            } catch (Exception ex) {
                try {
                    URL u = new URL(url);
                    HttpURLConnection c = (HttpURLConnection)u.openConnection();
                    c.addRequestProperty("User-Agent", "Mozilla/4.0");
                    int r = c.getResponseCode();
                    if (r == 200) {
                        return true;
                    }
                } catch (Exception ignored) {}
            }

        }
        return false;
    }

    public static void openWebLink(@NotNull String url) {
        try {
            String s = System.getProperty("os.name").toLowerCase(Locale.ROOT);
            URL u = new URL(url);
            if (!Minecraft.ON_OSX) {
                if (s.contains("win")) {
                    Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
                } else {
                    if (u.getProtocol().equals("file")) {
                        url = url.replace("file:", "file://");
                    }
                    Runtime.getRuntime().exec(new String[]{"xdg-open", url});
                }
            } else {
                Runtime.getRuntime().exec(new String[]{"open", url});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
