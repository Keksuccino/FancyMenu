package de.keksuccino.fancymenu.util;

import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

public class WebUtils {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final URI INTERNET_AVAILABILITY_ENDPOINT = URI.create("https://google.com");
    private static final int INTERNET_AVAILABILITY_TIMEOUT_MILLIS = 3000;
    private static final Duration INTERNET_AVAILABILITY_REFRESH_DELAY = Duration.ofSeconds(20L);
    private static final Duration RESOURCE_CONNECT_TIMEOUT = Duration.ofSeconds(10L);
    private static final Duration RESOURCE_READ_TIMEOUT = Duration.ofSeconds(30L);
    private static final Duration METADATA_CONNECT_TIMEOUT = Duration.ofSeconds(5L);
    private static final Duration METADATA_READ_TIMEOUT = Duration.ofSeconds(5L);
    private static final Duration METADATA_OVERALL_TIMEOUT = Duration.ofSeconds(10L);
    private static final BoundedWebResourceClient.RequestLimits METADATA_LIMITS = new BoundedWebResourceClient.RequestLimits(METADATA_CONNECT_TIMEOUT, METADATA_READ_TIMEOUT, METADATA_OVERALL_TIMEOUT, Long.MAX_VALUE);

    private static volatile boolean isConnectionAvailable = false;
    private static final InternetAvailabilityMonitor INTERNET_AVAILABILITY_MONITOR = new InternetAvailabilityMonitor(new HttpInternetAvailabilityProbe(INTERNET_AVAILABILITY_ENDPOINT, INTERNET_AVAILABILITY_TIMEOUT_MILLIS, INTERNET_AVAILABILITY_TIMEOUT_MILLIS, endpoint -> (HttpURLConnection) endpoint.toURL().openConnection()), WebUtils::createConnectivityScheduler, INTERNET_AVAILABILITY_REFRESH_DELAY, available -> isConnectionAvailable = available);
    private static final BoundedWebResourceClient RESOURCE_CLIENT = new BoundedWebResourceClient(resourceUri -> (HttpURLConnection) resourceUri.toURL().openConnection(), new ExecutorDeadlineScheduler(createScheduledExecutor("FancyMenu-WebUtils-ResourceDeadline")), System::nanoTime);

    public static void init() {
        INTERNET_AVAILABILITY_MONITOR.init();
    }

    public static void shutdown() {
        try {
            RESOURCE_CLIENT.shutdown();
        } finally {
            INTERNET_AVAILABILITY_MONITOR.shutdown();
        }
    }

    private static FixedDelayScheduler createConnectivityScheduler() {
        return new ExecutorFixedDelayScheduler(createScheduledExecutor("FancyMenu-WebUtils-ConnectivityCheck"));
    }

    private static ScheduledExecutorService createScheduledExecutor(String threadName) {
        return Executors.newSingleThreadScheduledExecutor(runnable -> createDaemonThread(runnable, threadName));
    }

    private static Thread createDaemonThread(Runnable runnable, String threadName) {
        Thread thread = new Thread(runnable, threadName);
        thread.setDaemon(true);
        return thread;
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

    /**
     * Opens an HTTP(S) resource with the conservative {@link WebResourceType#GENERAL} limits. The returned stream owns
     * its response connection and must be closed when the caller stops before EOF; EOF, failure, deadline, and shutdown
     * also release it automatically.
     */
    @Nullable
    public static InputStream openResourceStream(@NotNull String resourceURL) {
        return openResourceStream(resourceURL, WebResourceType.GENERAL);
    }

    /**
     * Opens an HTTP(S) resource with limits selected for its consumption model. The byte limit is enforced while
     * streaming even when the response omits Content-Length, and the overall deadline is cumulative across all reads.
     */
    @Nullable
    public static InputStream openResourceStream(@NotNull String resourceURL, @NotNull WebResourceType resourceType) {
        URI resourceUri = parseHttpUri(resourceURL);
        if (resourceUri == null) return null;
        try {
            return RESOURCE_CLIENT.openResourceStream(resourceUri, Objects.requireNonNull(resourceType, "resourceType").limits());
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to open bounded web resource stream: {}", resourceURL, ex);
        }
        return null;
    }

    @Nullable
    public static String getMimeType(@NotNull String url) {
        URI resourceUri = parseHttpUri(url);
        if (resourceUri == null) return null;
        try {
            return RESOURCE_CLIENT.getMimeType(resourceUri, METADATA_LIMITS);
        } catch (Exception ignore) {}
        return null;
    }

    public static boolean isValidUrl(@Nullable String url) {
        URI resourceUri = parseHttpUri(url);
        if (resourceUri == null) return false;
        try {
            return RESOURCE_CLIENT.isValidUrl(resourceUri, METADATA_LIMITS);
        } catch (Exception ignored) {}
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

    @Nullable
    private static URI parseHttpUri(@Nullable String value) {
        if (value == null) return null;
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            if ((scheme == null) || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) return null;
            return uri;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Download policies sized for FancyMenu's built-in consumers. Buffered formats stay below practical Java-array
     * bounds, while FMA/AFMA and MP4 streams receive larger disk-spooling allowances without becoming unbounded.
     */
    public enum WebResourceType {
        TEXT(8L * 1024L * 1024L, Duration.ofMinutes(1L)),
        IMAGE(128L * 1024L * 1024L, Duration.ofMinutes(3L)),
        BUFFERED_ANIMATED_TEXTURE(256L * 1024L * 1024L, Duration.ofMinutes(5L)),
        STREAMED_ANIMATED_ARCHIVE(2L * 1024L * 1024L * 1024L, Duration.ofMinutes(10L)),
        AUDIO(512L * 1024L * 1024L, Duration.ofMinutes(10L)),
        VIDEO(2L * 1024L * 1024L * 1024L, Duration.ofMinutes(10L)),
        GENERAL(256L * 1024L * 1024L, Duration.ofMinutes(5L));

        private final BoundedWebResourceClient.RequestLimits limits;

        WebResourceType(long maximumBytes, @NotNull Duration overallTimeout) {
            this.limits = new BoundedWebResourceClient.RequestLimits(RESOURCE_CONNECT_TIMEOUT, RESOURCE_READ_TIMEOUT, overallTimeout, maximumBytes);
        }

        private @NotNull BoundedWebResourceClient.RequestLimits limits() {
            return this.limits;
        }
    }

}
