package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.events.ModReloadEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.json.JsonUtils;
import net.minecraft.client.resources.language.I18n;
import de.keksuccino.konkrete.web.WebUtils;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JsonPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static final Map<String, List<String>> CACHED_PLACEHOLDERS = Collections.synchronizedMap(new HashMap<>());
    protected static final Map<String, Long> CURRENTLY_UPDATING_PLACEHOLDERS = new ConcurrentHashMap<>();
    protected static final List<String> INVALID_WEB_PLACEHOLDER_URLS = Collections.synchronizedList(new ArrayList<>());
    protected static final long UPDATE_TIMEOUT = 120000; // 2 minutes

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
        long currentTime = System.currentTimeMillis();

        // Use iterator to safely remove while iterating
        Iterator<Map.Entry<String, Long>> iterator = CURRENTLY_UPDATING_PLACEHOLDERS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (currentTime - entry.getValue() > UPDATE_TIMEOUT) {
                String placeholder = entry.getKey();
                iterator.remove();
                LOGGER.warn("[FANCYMENU] Placeholder update timed out for: {}", placeholder);
            }
        }
    }

    @EventListener
    public static void onReload(ModReloadEvent e) {
        try {
            CACHED_PLACEHOLDERS.clear();
            INVALID_WEB_PLACEHOLDER_URLS.clear();
            CURRENTLY_UPDATING_PLACEHOLDERS.clear();
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
            File f = new File(source);
            if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
                String linkTemp = Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + source;
                f = new File(linkTemp);
            }
            if (f.isFile()) {
                List<String> json = JsonUtils.getJsonValueByPath(f, jsonPath);
                return formatJsonToString(json);
            } else {
                if (!isInvalidWebPlaceholderLink(source)) {
                    List<String> json = getCachedWebPlaceholder(dps.placeholderString);
                    if (json != null) {
                        return formatJsonToString(json);
                    } else {
                        if (!isWebPlaceholderUpdating(dps.placeholderString)) {
                            cacheWebPlaceholder(dps.placeholderString, source, jsonPath);
                        }
                        return "";
                    }
                }
            }
        }
        return null;
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
            return INVALID_WEB_PLACEHOLDER_URLS.contains(link);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Error in JsonPlaceholder!", ex);
        }
        return true;
    }

    protected static List<String> getCachedWebPlaceholder(String placeholder) {
        try {
            return CACHED_PLACEHOLDERS.get(placeholder);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Error in JsonPlaceholder!", ex);
        }
        return null;
    }

    protected static boolean isWebPlaceholderUpdating(String placeholder) {
        try {
            return CURRENTLY_UPDATING_PLACEHOLDERS.containsKey(placeholder);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Error in JsonPlaceholder!", ex);
        }
        return true;
    }

    protected static void cacheWebPlaceholder(@NotNull String placeholder, @NotNull String source, @NotNull String jsonPath) {
        try {
            if (!CURRENTLY_UPDATING_PLACEHOLDERS.containsKey(placeholder)) {
                // Record the timestamp when starting the update
                CURRENTLY_UPDATING_PLACEHOLDERS.put(placeholder, System.currentTimeMillis());

                new Thread(() -> {
                    try {
                        if (WebUtils.isValidUrl(source)) {
                            String jsonString = getJsonStringFromURL(source);
                            if (jsonString != null) {
                                CACHED_PLACEHOLDERS.put(placeholder, JsonUtils.getJsonValueByPath(jsonString, jsonPath));
                            } else {
                                INVALID_WEB_PLACEHOLDER_URLS.add(source);
                            }
                        } else {
                            INVALID_WEB_PLACEHOLDER_URLS.add(source);
                        }
                    } catch (Exception ex) {
                        LOGGER.error("[FANCYMENU] Error while caching a web JSON in the JsonPlaceholder!", ex);
                    } finally {
                        try {
                            // Always remove from updating list, even if an exception occurred
                            CURRENTLY_UPDATING_PLACEHOLDERS.remove(placeholder);
                        } catch (Exception ex) {
                            LOGGER.error("[FANCYMENU] Error while removing placeholder from updating list!", ex);
                        }
                    }
                }).start();
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Error while caching a web JSON in the JsonPlaceholder!", ex);
            // Make sure to remove from updating list if an exception occurs during setup
            CURRENTLY_UPDATING_PLACEHOLDERS.remove(placeholder);
        }
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
        return I18n.get("fancymenu.helper.placeholder.json");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.helper.placeholder.json.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.fancymenu.editor.dynamicvariabletextfield.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholderIdentifier = this.getIdentifier();
        dps.values.put("source", "path_or_link_to_json");
        dps.values.put("json_path", "$.some.json.path");
        return dps;
    }

}