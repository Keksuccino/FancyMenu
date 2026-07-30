package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.events.ModReloadEvent;
import de.keksuccino.fancymenu.util.file.LocalSourcePathResolver;
import de.keksuccino.fancymenu.util.threading.FancyMenuThreads;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.json.JsonUtils;
import net.minecraft.client.resources.language.I18n;
import de.keksuccino.konkrete.web.WebUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

public class JsonPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();
    protected static final long UPDATE_TIMEOUT = 120000; // 2 minutes
    private static final JsonPlaceholderWebCache WEB_CACHE = new JsonPlaceholderWebCache(task -> FancyMenuThreads.startDaemonThread(task, "JsonPlaceholder-WebLoader"), JsonPlaceholder::loadWebJson, System::currentTimeMillis, UPDATE_TIMEOUT);

    private static Timer cleanupTimer;
    protected static boolean initialized = false;

    public JsonPlaceholder() {
        super("json");
        if (!initialized) {
            EventHandler.INSTANCE.registerListenersOf(JsonPlaceholder.class);
            initCleanupTimer();
            initialized = true;
        }
    }

    /**
     * Initializes the timer to periodically clean up stale entries
     */
    private static void initCleanupTimer() {
        if (cleanupTimer == null) {
            cleanupTimer = new Timer("FancyMenu-JsonPlaceholder-Cleanup", true);
            cleanupTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    cleanupStaleUpdates();
                }
            }, 30000, 30000); // Check every 30 seconds
        }
    }

    /**
     * Cleans up placeholder update tasks that have been running too long
     */
    protected static void cleanupStaleUpdates() {
        WEB_CACHE.cleanupTimedOut();
    }

    @EventListener
    public static void onReload(ModReloadEvent e) {
        try {
            WEB_CACHE.reload();
            LOGGER.info("[FANCYMENU] JsonPlaceholder cache successfully cleared!");
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to reload JsonPlaceholder!", ex);
        }
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        // Always cleanup stale updates before processing a new request
        cleanupStaleUpdates();

        String source = dps.values.get("source");
        String jsonPath = dps.values.get("json_path");
        if ((source != null) && (jsonPath != null)) {
            source = StringUtils.convertFormatCodes(source, "§", "&");
            
            // First check if source is direct JSON content
            if (isDirectJsonContent(source)) {
                List<String> json = JsonUtils.getJsonValueByPath(source, jsonPath);
                return formatJsonToString(json);
            }
            
            LocalJsonLookup localLookup = isHttpSource(source) ? LocalJsonLookup.missing() : readLocalJsonWithProductionResolver(source, jsonPath);
            if (localLookup.status() == LocalJsonStatus.FOUND) {
                return formatJsonToString(Objects.requireNonNull(localLookup.json()));
            } else if (localLookup.status() == LocalJsonStatus.MISSING) {
                JsonPlaceholderWebCache.Lookup lookup = WEB_CACHE.getOrLoad(dps.placeholderString, source, jsonPath);
                if (lookup.status() == JsonPlaceholderWebCache.Status.LOADED) return formatJsonToString(lookup.values());
                if (lookup.status() == JsonPlaceholderWebCache.Status.LOADING) return "";
            }
        }
        return null;
    }

    private static LocalJsonLookup readLocalJsonWithProductionResolver(String source, String jsonPath) {
        try {
            return readLocalJson(source, jsonPath, LocalSourcePathResolver.createForGameDirectory());
        } catch (IOException | RuntimeException ignored) {
            return LocalJsonLookup.rejected();
        }
    }

    static boolean isHttpSource(String source) {
        return source.regionMatches(true, 0, "http://", 0, "http://".length()) || source.regionMatches(true, 0, "https://", 0, "https://".length());
    }

    @NotNull
    static LocalJsonLookup readLocalJson(@NotNull String source, @NotNull String jsonPath, @NotNull LocalSourcePathResolver resolver) {
        LocalSourcePathResolver.ResolvedPath resolvedPath;
        Path path;
        try {
            resolvedPath = resolver.resolve(source);
            path = resolvedPath.revalidate();
            if (!Files.isRegularFile(path)) return LocalJsonLookup.missing();
            // Revalidate after the metadata probe and immediately before JsonUtils opens the file.
            path = resolvedPath.revalidate();
        } catch (IOException | RuntimeException ignored) {
            return LocalJsonLookup.rejected();
        }
        // Keep parser failures visible to the placeholder framework as before; only path-validation failures fail closed.
        return LocalJsonLookup.found(JsonUtils.getJsonValueByPath(path.toFile(), jsonPath));
    }

    enum LocalJsonStatus {
        FOUND,
        MISSING,
        REJECTED
    }

    record LocalJsonLookup(@NotNull LocalJsonStatus status, @Nullable List<String> json) {

        private static LocalJsonLookup found(List<String> json) {
            return new LocalJsonLookup(LocalJsonStatus.FOUND, json);
        }

        private static LocalJsonLookup missing() {
            return new LocalJsonLookup(LocalJsonStatus.MISSING, null);
        }

        private static LocalJsonLookup rejected() {
            return new LocalJsonLookup(LocalJsonStatus.REJECTED, null);
        }
    }

    /**
     * Checks if the given string is direct JSON content (not a file path or URL).
     * This is a quick check that looks for JSON object or array indicators.
     * 
     * @param str The string to check
     * @return true if the string appears to be direct JSON content
     */
    static boolean isDirectJsonContent(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = str.trim();
        // Check if it starts with JSON object or array indicators
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) || 
            (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            try {
                // Do a quick validation by checking if JsonUtils can parse it
                // If getJsonValueByPath works with it, it's valid JSON
                JsonUtils.getJsonValueByPath(trimmed, "$");
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    protected static String formatJsonToString(@NotNull List<String> json) {
        if (!json.isEmpty()) {
            if (json.size() == 1) {
                return json.get(0);
            } else {
                StringBuilder rep = new StringBuilder();
                for (String s2 : json) {
                    if (rep.isEmpty()) {
                        rep.append(s2);
                    } else {
                        rep.append("%n%").append(s2);
                    }
                }
                return rep.toString();
            }
        }
        return "§c[error while formatting JSON string]";
    }

    protected static boolean isInvalidWebPlaceholderLink(String link) {
        try {
            return WEB_CACHE.isInvalidSource(link);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Error in JsonPlaceholder!", ex);
        }
        return true;
    }

    protected static List<String> getCachedWebPlaceholder(String placeholder) {
        try {
            return WEB_CACHE.getCached(placeholder);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Error in JsonPlaceholder!", ex);
        }
        return null;
    }

    protected static boolean isWebPlaceholderUpdating(String placeholder) {
        try {
            return WEB_CACHE.isLoading(placeholder);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Error in JsonPlaceholder!", ex);
        }
        return true;
    }

    protected static void cacheWebPlaceholder(@NotNull String placeholder, @NotNull String source, @NotNull String jsonPath) {
        try {
            WEB_CACHE.loadIfAbsent(placeholder, source, jsonPath);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Error while caching a web JSON in the JsonPlaceholder!", ex);
        }
    }

    @NotNull
    private static JsonPlaceholderWebCache.LoadResult loadWebJson(@NotNull String source, @NotNull String jsonPath) {
        if (!WebUtils.isValidUrl(source)) return JsonPlaceholderWebCache.LoadResult.invalid();
        String jsonString = getJsonStringFromURL(source);
        if (jsonString == null) return JsonPlaceholderWebCache.LoadResult.invalid();
        return JsonPlaceholderWebCache.LoadResult.loaded(JsonUtils.getJsonValueByPath(jsonString, jsonPath));
    }

    /**
     * Fetches JSON content from a URL as a string.
     *
     * @param url The URL to fetch JSON from
     * @return The JSON string or null if an error occurred
     */
    @Nullable
    protected static String getJsonStringFromURL(@NotNull String url) {
        try {
            var client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/91.0.4472.124 Safari/537.36")
                    .GET()
                    .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            return response.statusCode() >= 200 && response.statusCode() < 300
                    ? response.body()
                    : null;

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Error while getting the content of a web JSON in the JsonPlaceholder!", ex);
            return null;
        }
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("source");
        l.add("json_path");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.json");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.json.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("source", "path_or_link_or_json_content");
        values.put("json_path", "$.some.json.path");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
