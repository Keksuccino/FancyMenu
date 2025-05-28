package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.Pair;
import de.keksuccino.fancymenu.util.TaskExecutor;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import de.keksuccino.fancymenu.util.resource.ResourceSourceType;
import net.minecraft.client.resources.language.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileTextPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final long FILE_READ_COOLDOWN_MS = 1000L;
    // Cache structure: filePath -> Pair<lastReadTime, fileContent>
    private static final Map<String, Pair<Long, List<String>>> FILE_CACHE = new ConcurrentHashMap<>();
    // Track files currently being loaded to prevent multiple simultaneous reads
    private static final Set<String> LOADING_FILES = Collections.synchronizedSet(new HashSet<>());

    public FileTextPlaceholder() {
        super("file_text");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String filePath = dps.values.get("path");
        String separator = dps.values.get("separator");
        String mode = dps.values.get("mode");
        String lastLinesStr = dps.values.get("last_lines");
        
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }

        // Converts the path to a valid local game directory path
        filePath = ResourceSource.of(filePath, ResourceSourceType.LOCAL).getSourceWithoutPrefix();
        
        if (separator == null) {
            separator = "\\n"; // Default to newline
        }
        
        if (mode == null) {
            mode = "all"; // Default mode
        }
        
        int lastLines = 1; // Default to last line only
        if (lastLinesStr != null) {
            try {
                lastLines = Integer.parseInt(lastLinesStr);
                if (lastLines < 1) lastLines = 1;
            } catch (NumberFormatException e) {
                LOGGER.warn("[FANCYMENU] Invalid last_lines value: " + lastLinesStr);
            }
        }
        
        // Get cached content or trigger async load
        List<String> lines = getCachedOrLoadAsync(filePath);
        
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        
        List<String> resultLines;
        
        if ("last".equals(mode)) {
            // Get last X lines
            int startIndex = Math.max(0, lines.size() - lastLines);
            resultLines = lines.subList(startIndex, lines.size());
            
            // If only one line requested, return it without separator
            if (lastLines == 1 && !resultLines.isEmpty()) {
                return resultLines.get(0);
            }
        } else {
            // All lines mode
            resultLines = lines;
        }
        
        // Join lines with separator
        return String.join(separator, resultLines);
    }

    private List<String> getCachedOrLoadAsync(String filePath) {
        Pair<Long, List<String>> cached = FILE_CACHE.get(filePath);
        long currentTime = System.currentTimeMillis();
        
        // Check if we have cached content
        if (cached != null) {
            // If cache is still valid, return it
            if ((currentTime - cached.getKey()) < FILE_READ_COOLDOWN_MS) {
                return cached.getValue();
            }
            
            // Cache expired, trigger async reload if not already loading
            if (!LOADING_FILES.contains(filePath)) {
                triggerAsyncFileLoad(filePath);
            }
            
            // Return existing cached content while reloading
            return cached.getValue();
        }
        
        // No cache exists, trigger async load if not already loading
        if (!LOADING_FILES.contains(filePath)) {
            triggerAsyncFileLoad(filePath);
        }
        
        // Return empty for first load
        return null;
    }
    
    private void triggerAsyncFileLoad(String filePath) {
        // Mark file as loading
        LOADING_FILES.add(filePath);
        
        // Execute file reading asynchronously
        TaskExecutor.execute(() -> {
            try {
                Path path = Paths.get(filePath);
                if (!Files.exists(path) || !Files.isRegularFile(path)) {
                    LOGGER.warn("[FANCYMENU] File not found or is not a regular file: " + filePath);
                    // Cache empty result to avoid repeated attempts
                    FILE_CACHE.put(filePath, Pair.of(System.currentTimeMillis(), new ArrayList<>()));
                    return;
                }
                
                List<String> lines = new ArrayList<>();
                try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        lines.add(line);
                    }
                }
                
                // Update cache with new content
                FILE_CACHE.put(filePath, Pair.of(System.currentTimeMillis(), lines));
                
            } catch (Exception e) {
                LOGGER.error("[FANCYMENU] Failed to read file asynchronously: " + filePath, e);
                // Cache empty result on error to avoid repeated attempts
                FILE_CACHE.put(filePath, Pair.of(System.currentTimeMillis(), new ArrayList<>()));
            } finally {
                // Always remove from loading set
                LOADING_FILES.remove(filePath);
            }
        }, false); // Execute in background thread, not main thread
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return Arrays.asList("path", "mode", "separator", "last_lines");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.file_text");
    }

    @Override
    public @Nullable List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.file_text.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.fancymenu.editor.dynamicvariabletextfield.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("path", "/config/fancymenu/assets/some_file.txt");
        values.put("mode", "all");
        values.put("separator", "\\n");
        values.put("last_lines", "1");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
