package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.TaskExecutor;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.file.LocalSourcePathResolver;
import de.keksuccino.fancymenu.util.resource.ResourceSourceType;
import de.keksuccino.fancymenu.util.threading.AsyncRefreshingValueCache;
import net.minecraft.client.resources.language.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.LongSupplier;

public class FileTextPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final long FILE_READ_COOLDOWN_MS = 1000L;
    private static final long URL_READ_COOLDOWN_MS = 10000L;
    private static final AsyncRefreshingValueCache<String, List<String>> FILE_CACHE = createContentCache(task -> TaskExecutor.execute(task, false), FileTextPlaceholder::loadSource, System::currentTimeMillis);

    private final AsyncRefreshingValueCache<String, List<String>> contentCache;

    public FileTextPlaceholder() {
        this(FILE_CACHE);
    }

    FileTextPlaceholder(@NotNull AsyncRefreshingValueCache.TaskLauncher taskLauncher, @NotNull AsyncRefreshingValueCache.ValueLoader<String, List<String>> sourceLoader, @NotNull LongSupplier timeSource) {
        this(createContentCache(taskLauncher, sourceLoader, timeSource));
    }

    private FileTextPlaceholder(@NotNull AsyncRefreshingValueCache<String, List<String>> contentCache) {
        super("file_text");
        this.contentCache = contentCache;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {

        String pathOrUrl = dps.values.get("path_or_url");
        String separator = dps.values.get("separator");
        String mode = dps.values.get("mode");
        String lastLinesStr = dps.values.get("last_lines");
        
        if (pathOrUrl == null || pathOrUrl.isEmpty()) {
            return "";
        }

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
        List<String> lines = getCachedOrLoadAsync(pathOrUrl);
        
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
        return joinLines(resultLines, separator);
    }

    @NotNull
    static String joinLines(@NotNull List<String> lines, @NotNull String separator) {
        // Only the documented textual newline is decoded. Actual line breaks and all unrelated escapes stay untouched.
        return String.join(separator.replace("\\n", "\n"), lines);
    }

    @Nullable
    List<String> getCachedOrLoadAsync(@NotNull String path) {
        long cooldownMs = isUrl(path) ? URL_READ_COOLDOWN_MS : FILE_READ_COOLDOWN_MS;
        return this.contentCache.getOrLoad(path, cooldownMs);
    }

    boolean isSourceLoading(@NotNull String path) {
        return this.contentCache.isLoading(path);
    }

    void clearContentCache() {
        this.contentCache.clear();
    }

    @NotNull
    private static AsyncRefreshingValueCache<String, List<String>> createContentCache(@NotNull AsyncRefreshingValueCache.TaskLauncher taskLauncher, @NotNull AsyncRefreshingValueCache.ValueLoader<String, List<String>> sourceLoader, @NotNull LongSupplier timeSource) {
        return new AsyncRefreshingValueCache<>(taskLauncher, source -> List.copyOf(sourceLoader.load(source)), (source, exception) -> {
            LOGGER.error("[FANCYMENU] Failed to read source asynchronously: " + source, exception);
            return List.of();
        }, (source, exception) -> LOGGER.error("[FANCYMENU] Failed to start the asynchronous source read: " + source, exception), timeSource);
    }

    @NotNull
    private static List<String> loadSource(@NotNull String source) throws IOException {
        return isUrl(source) ? loadFromUrl(source) : loadFromFile(source);
    }
    
    static boolean isUrl(String path) {
        return path != null && (path.startsWith("http://") || path.startsWith("https://"));
    }
    
    private static List<String> loadFromUrl(String url) throws IOException {
        if (!WebUtils.isInternetAvailable()) {
            return new ArrayList<>();
        }
        
        InputStream stream = WebUtils.openResourceStream(url);
        if (stream == null) {
            LOGGER.warn("[FANCYMENU] Failed to open URL stream: " + url);
            return new ArrayList<>();
        }

        try (stream) {
            return FileUtils.readTextLinesFrom(stream);
        }
    }
    
    private static List<String> loadFromFile(String filePath) {
        try {
            return loadFromFile(filePath, LocalSourcePathResolver.createForGameAndMinecraftDirectories());
        } catch (IOException | RuntimeException ignored) {
            return new ArrayList<>();
        }
    }

    @NotNull
    static List<String> loadFromFile(@NotNull String filePath, @NotNull LocalSourcePathResolver resolver) {
        try {
            if (ResourceSourceType.hasSourcePrefix(filePath) && (ResourceSourceType.getSourceTypeOf(filePath) != ResourceSourceType.LOCAL)) return new ArrayList<>();
            filePath = ResourceSourceType.getWithoutSourcePrefix(filePath);
            LocalSourcePathResolver.ResolvedPath resolvedPath = resolver.resolve(filePath);
            Path path = resolvedPath.revalidate();
            if (!Files.isRegularFile(path)) return new ArrayList<>();

            List<String> lines = new ArrayList<>();
            // Revalidate after the metadata probe and immediately before opening the file. The captured boundary also
            // catches a formerly nonexistent safe ancestor that was replaced with an escaping symbolic link.
            try (BufferedReader reader = Files.newBufferedReader(resolvedPath.revalidate(), StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
            return lines;
        } catch (IOException | RuntimeException ignored) {
            return new ArrayList<>();
        }
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return Arrays.asList("path_or_url", "mode", "separator", "last_lines");
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
        return I18n.get("fancymenu.requirements.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("path_or_url", "/config/fancymenu/assets/some_file.txt");
        values.put("mode", "all");
        values.put("separator", "\\n");
        values.put("last_lines", "1");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
