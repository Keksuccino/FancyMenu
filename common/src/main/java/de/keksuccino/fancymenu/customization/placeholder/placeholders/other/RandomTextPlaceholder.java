package de.keksuccino.fancymenu.customization.placeholder.placeholders.other;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.SerializationHelper;
import de.keksuccino.fancymenu.util.TaskExecutor;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import de.keksuccino.fancymenu.util.resource.ResourceSourceType;
import de.keksuccino.fancymenu.util.threading.AsyncRefreshingValueCache;
import net.minecraft.client.resources.language.I18n;
import de.keksuccino.konkrete.math.MathUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.function.LongSupplier;

public class RandomTextPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final long CONTENT_RELOAD_COOLDOWN_MS = 30000L; // 30 seconds
    private static final AsyncRefreshingValueCache<String, List<String>> CONTENT_CACHE = createContentCache(task -> TaskExecutor.execute(task, false), RandomTextPlaceholder::loadSource, System::currentTimeMillis);

    public static Map<String, RandomTextPackage> randomTextIntervals = new HashMap<>();

    private final AsyncRefreshingValueCache<String, List<String>> contentCache;

    public RandomTextPlaceholder() {
        this(CONTENT_CACHE);
    }

    RandomTextPlaceholder(@NotNull AsyncRefreshingValueCache.TaskLauncher taskLauncher, @NotNull AsyncRefreshingValueCache.ValueLoader<String, List<String>> sourceLoader, @NotNull LongSupplier timeSource) {
        this(createContentCache(taskLauncher, sourceLoader, timeSource));
    }

    private RandomTextPlaceholder(@NotNull AsyncRefreshingValueCache<String, List<String>> contentCache) {
        super("randomtext");
        this.contentCache = contentCache;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {

        String sourceString = dps.values.get("source");
        if (sourceString == null) sourceString = dps.values.get("path");
        long intervalRaw = SerializationHelper.INSTANCE.deserializeNumber(Long.class, 10L, dps.values.get("interval"));
        
        if (sourceString == null) {
            return null;
        }
        
        // Get cached content or trigger load
        List<String> lines = getCachedOrLoadContent(sourceString);
        
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        
        // Handle randomization with user-defined interval
        long interval = intervalRaw * 1000;
        if (interval < 0L) {
            interval = 0L;
        }
        
        long currentTime = System.currentTimeMillis();
        RandomTextPackage p;
        
        if (randomTextIntervals.containsKey(sourceString)) {
            p = randomTextIntervals.get(sourceString);
        } else {
            p = new RandomTextPackage();
            randomTextIntervals.put(sourceString, p);
        }
        
        if ((interval > 0) || (p.currentText == null)) {
            if ((p.lastChange + interval) <= currentTime) {
                p.lastChange = currentTime;
                p.currentText = lines.get(MathUtils.getRandomNumberInRange(0, lines.size()-1));
            }
        }
        
        return p.currentText != null ? p.currentText : "";
    }
    
    @Nullable
    List<String> getCachedOrLoadContent(@NotNull String pathOrUrl) {
        long refreshIntervalMillis = isPlainText(pathOrUrl) ? AsyncRefreshingValueCache.NO_REFRESH : CONTENT_RELOAD_COOLDOWN_MS;
        return this.contentCache.getOrLoad(pathOrUrl, refreshIntervalMillis);
    }

    boolean isSourceLoading(@NotNull String pathOrUrl) {
        return this.contentCache.isLoading(pathOrUrl);
    }

    void clearContentCache() {
        this.contentCache.clear();
    }

    @NotNull
    private static AsyncRefreshingValueCache<String, List<String>> createContentCache(@NotNull AsyncRefreshingValueCache.TaskLauncher taskLauncher, @NotNull AsyncRefreshingValueCache.ValueLoader<String, List<String>> sourceLoader, @NotNull LongSupplier timeSource) {
        return new AsyncRefreshingValueCache<>(taskLauncher, source -> List.copyOf(sourceLoader.load(source)), (source, exception) -> {
            LOGGER.error("[FANCYMENU] Failed to read source for RandomTextPlaceholder: " + source, exception);
            return List.of();
        }, (source, exception) -> LOGGER.error("[FANCYMENU] Failed to start the asynchronous Random Text source read: " + source, exception), timeSource);
    }

    @NotNull
    private static List<String> loadSource(@NotNull String source) {
        if (isUrl(source)) return loadFromUrl(source);
        if (isPlainText(source)) return parsePlainText(source);
        return loadFromFile(source);
    }
    
    static boolean isUrl(String path) {
        return path != null && (path.startsWith("http://") || path.startsWith("https://"));
    }
    
    private static List<String> loadFromUrl(String url) {
        if (!WebUtils.isInternetAvailable()) {
            return new ArrayList<>();
        }
        
        List<String> lines = new ArrayList<>();
        try (InputStream stream = WebUtils.openResourceStream(url)) {
            if (stream == null) {
                LOGGER.warn("[FANCYMENU] Failed to open URL stream: " + url);
                return lines;
            }
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Error reading from URL: " + url, e);
        }
        
        return lines;
    }
    
    private static List<String> loadFromFile(String pathString) {
        try {
            ResourceSource source = ResourceSource.of(pathString, ResourceSourceType.LOCAL);
            File path = source.getValidatedLocalFile();
            if (path == null) return new ArrayList<>();
            
            if (!path.isFile() || !path.getPath().toLowerCase().endsWith(".txt")) {
                LOGGER.warn("[FANCYMENU] File not found or not a .txt file: " + pathString);
                return new ArrayList<>();
            }
            
            List<String> lines = new ArrayList<>();
            path = source.getValidatedLocalFile();
            if (path == null) return lines;
            lines.addAll(Files.readAllLines(path.toPath(), StandardCharsets.UTF_8));
            return lines;
            
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Error reading file: " + pathString, e);
            return new ArrayList<>();
        }
    }
    
    static boolean isPlainText(String path) {
        // If it's a URL, it's not plain text
        if (isUrl(path)) return false;
        
        // If it contains newline characters, it's likely plain text
        if (path.contains("\\n")) return true;
        
        // Check if it looks like a file path
        boolean looksLikeFilePath = path.trim().toLowerCase().endsWith(".txt");

        return !looksLikeFilePath;
    }
    
    @NotNull
    static List<String> parsePlainText(@NotNull String plainText) {
        // Split by escaped \n
        String[] lines = plainText.split("\\\\n");
        List<String> result = new ArrayList<>();
        
        for (String line : lines) {
            // Trim each line but keep empty lines if they exist
            result.add(line);
        }
        
        if (result.isEmpty()) {
            // If splitting by \n didn't work, treat the whole text as one line
            result.add(plainText);
        }
        
        return result;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("path");
        l.add("source");
        l.add("interval");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.randomtext");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.randomtext.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.other");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("source", "/config/fancymenu/assets/<file_name.txt>");
        values.put("interval", "10");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

    public static class RandomTextPackage {
        public String currentText = null;
        public long lastChange = 0L;
    }

}
