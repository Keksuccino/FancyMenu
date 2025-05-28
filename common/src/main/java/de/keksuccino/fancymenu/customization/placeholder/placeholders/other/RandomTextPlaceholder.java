package de.keksuccino.fancymenu.customization.placeholder.placeholders.other;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.Pair;
import de.keksuccino.fancymenu.util.SerializationUtils;
import de.keksuccino.fancymenu.util.TaskExecutor;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import de.keksuccino.fancymenu.util.resource.ResourceSourceType;
import net.minecraft.client.resources.language.I18n;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
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
import java.util.concurrent.ConcurrentHashMap;

public class RandomTextPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final long CONTENT_RELOAD_COOLDOWN_MS = 30000L; // 30 seconds
    // Cache structure: path/url -> Pair<lastLoadTime, List<lines>>
    private static final Map<String, Pair<Long, List<String>>> CONTENT_CACHE = new ConcurrentHashMap<>();
    // Track sources currently being loaded to prevent multiple simultaneous reads
    private static final Set<String> LOADING_SOURCES = Collections.synchronizedSet(new HashSet<>());

    public static Map<String, RandomTextPackage> randomTextIntervals = new HashMap<>();

    public RandomTextPlaceholder() {
        super("randomtext");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String pathString = dps.values.get("path");
        long intervalRaw = SerializationUtils.deserializeNumber(Long.class, 10L, dps.values.get("interval"));
        
        if (pathString == null) {
            return null;
        }
        
        // Get cached content or trigger load
        List<String> lines = getCachedOrLoadContent(pathString);
        
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
        
        if (randomTextIntervals.containsKey(pathString)) {
            p = randomTextIntervals.get(pathString);
        } else {
            p = new RandomTextPackage();
            randomTextIntervals.put(pathString, p);
        }
        
        if ((interval > 0) || (p.currentText == null)) {
            if ((p.lastChange + interval) <= currentTime) {
                p.lastChange = currentTime;
                p.currentText = lines.get(MathUtils.getRandomNumberInRange(0, lines.size()-1));
            }
        }
        
        return p.currentText != null ? p.currentText : "";
    }
    
    private List<String> getCachedOrLoadContent(String pathOrUrl) {
        Pair<Long, List<String>> cached = CONTENT_CACHE.get(pathOrUrl);
        long currentTime = System.currentTimeMillis();
        
        // Check if we have cached content
        if (cached != null) {
            // If cache is still valid, return it
            if ((currentTime - cached.getKey()) < CONTENT_RELOAD_COOLDOWN_MS) {
                return cached.getValue();
            }
            
            // Cache expired, trigger async reload if not already loading
            if (!LOADING_SOURCES.contains(pathOrUrl)) {
                triggerAsyncLoad(pathOrUrl);
            }
            
            // Return existing cached content while reloading
            return cached.getValue();
        }
        
        // No cache exists, trigger async load if not already loading
        if (!LOADING_SOURCES.contains(pathOrUrl)) {
            triggerAsyncLoad(pathOrUrl);
        }
        
        // Return empty for first load
        return null;
    }
    
    private void triggerAsyncLoad(String pathOrUrl) {
        // Mark source as loading
        LOADING_SOURCES.add(pathOrUrl);
        
        // Execute loading asynchronously
        TaskExecutor.execute(() -> {
            try {
                List<String> lines;
                
                if (isUrl(pathOrUrl)) {
                    // Load from URL
                    lines = loadFromUrl(pathOrUrl);
                } else {
                    // Load from local file
                    lines = loadFromFile(pathOrUrl);
                }
                
                // Update cache with new content
                CONTENT_CACHE.put(pathOrUrl, Pair.of(System.currentTimeMillis(), lines));
                
            } catch (Exception e) {
                LOGGER.error("[FANCYMENU] Failed to read source for RandomTextPlaceholder: " + pathOrUrl, e);
                // Cache empty result on error to avoid repeated attempts
                CONTENT_CACHE.put(pathOrUrl, Pair.of(System.currentTimeMillis(), new ArrayList<>()));
            } finally {
                // Always remove from loading set
                LOADING_SOURCES.remove(pathOrUrl);
            }
        }, false); // Execute in background thread
    }
    
    private boolean isUrl(String path) {
        return path != null && (path.startsWith("http://") || path.startsWith("https://"));
    }
    
    private List<String> loadFromUrl(String url) {
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
    
    private List<String> loadFromFile(String pathString) {
        try {
            File path = new File(ResourceSource.of(pathString, ResourceSourceType.LOCAL).getSourceWithoutPrefix());
            
            if (!path.isFile() || !path.getPath().toLowerCase().endsWith(".txt")) {
                LOGGER.warn("[FANCYMENU] File not found or not a .txt file: " + pathString);
                return new ArrayList<>();
            }
            
            List<String> lines = new ArrayList<>();
            lines.addAll(Files.readAllLines(path.toPath(), StandardCharsets.UTF_8));
            return lines;
            
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Error reading file: " + pathString, e);
            return new ArrayList<>();
        }
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("path");
        l.add("interval");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.fancymenu.editor.dynamicvariabletextfield.variables.randomtext");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.fancymenu.editor.dynamicvariabletextfield.variables.randomtext.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.fancymenu.editor.dynamicvariabletextfield.categories.other");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("path", "/config/fancymenu/assets/<file_name.txt>");
        values.put("interval", "10");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

    public static class RandomTextPackage {
        public String currentText = null;
        public long lastChange = 0L;
    }

}
