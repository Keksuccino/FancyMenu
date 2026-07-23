package de.keksuccino.fancymenu.util;

import net.minecraft.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class WebUtils {

    private static final URI INTERNET_AVAILABILITY_ENDPOINT = URI.create("https://docs.fancymenu.net");
    private static final int INTERNET_AVAILABILITY_TIMEOUT_MILLIS = 3000;
    private static final Duration INTERNET_AVAILABILITY_REFRESH_DELAY = Duration.ofSeconds(20L);
    private static final ThreadFactory INTERNET_AVAILABILITY_THREAD_FACTORY = runnable -> {
        Thread thread = new Thread(runnable, "FancyMenu-WebUtils-ConnectivityCheck");
        thread.setDaemon(true);
        return thread;
    };

    private static volatile boolean isConnectionAvailable = false;
    private static final InternetAvailabilityMonitor INTERNET_AVAILABILITY_MONITOR = new InternetAvailabilityMonitor(new HttpInternetAvailabilityProbe(INTERNET_AVAILABILITY_ENDPOINT, INTERNET_AVAILABILITY_TIMEOUT_MILLIS, INTERNET_AVAILABILITY_TIMEOUT_MILLIS, endpoint -> (HttpURLConnection) endpoint.toURL().openConnection()), () -> new ExecutorFixedDelayScheduler(Executors.newSingleThreadScheduledExecutor(INTERNET_AVAILABILITY_THREAD_FACTORY)), INTERNET_AVAILABILITY_REFRESH_DELAY, available -> isConnectionAvailable = available);

    public static void init() {
        INTERNET_AVAILABILITY_MONITOR.init();
    }

    public static void shutdown() {
        INTERNET_AVAILABILITY_MONITOR.shutdown();
    }

    /**
     * Checks if an internet connection is available.
     * The method can be called in the main thread, since the value it returns is updated asynchronously every 20 seconds.
     *
     * Before the first asynchronous probe completes, this deliberately reports false instead of assuming an
     * unverified connection is available.
     *
     * @return true if the latest internet availability probe succeeded, false otherwise
     */
    public static boolean isInternetAvailable() {
        return isConnectionAvailable;
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
            if (Util.getPlatform() != Util.OS.OSX) {
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
