package de.keksuccino.fancymenu.customization.customlocals;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.StringJoiner;

/**
 * Loads custom localization files as two deterministic layers: {@code en_us} first and the selected locale second.
 * Supported files are discovered recursively and sorted lexicographically by their root-relative path after path
 * separators are normalized to {@code /}. Later paths override earlier paths within a locale layer, and the selected
 * locale layer is always merged after the fallback layer. Files for every other locale are ignored.
 */
final class CustomLocalizationLoader {

    static final String FALLBACK_LOCALE = "en_us";

    private static final List<String> SUPPORTED_EXTENSIONS = List.of(".json", ".lang", ".properties");

    private CustomLocalizationLoader() {
    }

    static @NotNull Map<String, String> load(@NotNull Path rootDirectory, @Nullable String selectedLocale, @NotNull LoadErrorHandler errorHandler) {
        String normalizedSelectedLocale = normalizeLocale(selectedLocale);
        if (normalizedSelectedLocale.isEmpty()) normalizedSelectedLocale = FALLBACK_LOCALE;

        List<Path> files = discoverFiles(rootDirectory, errorHandler);
        Map<String, String> localizations = new LinkedHashMap<>();
        loadLocale(files, FALLBACK_LOCALE, localizations, errorHandler);
        if (!FALLBACK_LOCALE.equals(normalizedSelectedLocale)) loadLocale(files, normalizedSelectedLocale, localizations, errorHandler);
        return Collections.unmodifiableMap(new LinkedHashMap<>(localizations));
    }

    static @NotNull String normalizeLocale(@Nullable String locale) {
        if (locale == null) return "";
        return locale.strip().replace('-', '_').toLowerCase(Locale.ROOT);
    }

    private static @NotNull List<Path> discoverFiles(@NotNull Path rootDirectory, @NotNull LoadErrorHandler errorHandler) {
        if (!Files.isDirectory(rootDirectory, LinkOption.NOFOLLOW_LINKS)) return List.of();

        List<Path> files = new ArrayList<>();
        try {
            // The default visitor does not follow links, which avoids cycles and keeps traversal inside the configured localization tree.
            Files.walkFileTree(rootDirectory, new LocalizationFileVisitor(files, errorHandler));
        } catch (IOException e) {
            errorHandler.onError(rootDirectory, e);
        }
        // Sorting the complete relative path defines duplicate-key precedence independently of filesystem enumeration order.
        files.sort(Comparator.comparing(path -> relativeSortKey(rootDirectory, path)));
        return files;
    }

    private static void loadLocale(@NotNull List<Path> files, @NotNull String locale, @NotNull Map<String, String> localizations, @NotNull LoadErrorHandler errorHandler) {
        for (Path file : files) {
            if (!locale.equals(localeFromFilename(file))) continue;
            try {
                localizations.putAll(loadFile(file));
            } catch (Exception e) {
                errorHandler.onError(file, e);
            }
        }
    }

    private static @NotNull Map<String, String> loadFile(@NotNull Path file) throws IOException {
        String extension = supportedExtension(file);
        if (".json".equals(extension)) return loadJson(file);
        return loadProperties(file);
    }

    private static @NotNull Map<String, String> loadJson(@NotNull Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (!element.isJsonObject()) throw new JsonParseException("Custom localization JSON root must be an object");
            Map<String, String> localizations = new LinkedHashMap<>();
            loadJsonObject("", element.getAsJsonObject(), localizations);
            return localizations;
        }
    }

    private static void loadJsonObject(@NotNull String prefix, @NotNull JsonObject object, @NotNull Map<String, String> localizations) {
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            JsonElement value = entry.getValue();
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                localizations.put(key, value.getAsString());
            } else if (value.isJsonObject()) {
                loadJsonObject(key, value.getAsJsonObject(), localizations);
            }
        }
    }

    private static @NotNull Map<String, String> loadProperties(@NotNull Path file) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }

        Map<String, String> localizations = new LinkedHashMap<>();
        properties.stringPropertyNames().stream().sorted().forEach(key -> localizations.put(key, properties.getProperty(key)));
        return localizations;
    }

    private static @NotNull String localeFromFilename(@NotNull Path file) {
        String filename = file.getFileName().toString();
        String extension = supportedExtension(file);
        if (extension.isEmpty()) return "";
        return normalizeLocale(filename.substring(0, filename.length() - extension.length()));
    }

    private static @NotNull String supportedExtension(@NotNull Path file) {
        String filename = file.getFileName().toString().toLowerCase(Locale.ROOT);
        for (String extension : SUPPORTED_EXTENSIONS) {
            if (filename.endsWith(extension)) return extension;
        }
        return "";
    }

    private static @NotNull String relativeSortKey(@NotNull Path rootDirectory, @NotNull Path file) {
        StringJoiner path = new StringJoiner("/");
        rootDirectory.relativize(file).forEach(part -> path.add(part.toString()));
        return path.toString();
    }

    @FunctionalInterface
    interface LoadErrorHandler {
        void onError(@NotNull Path path, @NotNull Exception exception);
    }

    private static final class LocalizationFileVisitor extends SimpleFileVisitor<Path> {

        private final List<Path> files;
        private final LoadErrorHandler errorHandler;

        private LocalizationFileVisitor(@NotNull List<Path> files, @NotNull LoadErrorHandler errorHandler) {
            this.files = files;
            this.errorHandler = errorHandler;
        }

        @Override
        public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attributes) {
            if (attributes.isRegularFile() && !attributes.isSymbolicLink() && !supportedExtension(file).isEmpty()) this.files.add(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public @NotNull FileVisitResult visitFileFailed(@NotNull Path file, @NotNull IOException exception) {
            this.errorHandler.onError(file, exception);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public @NotNull FileVisitResult postVisitDirectory(@NotNull Path directory, @Nullable IOException exception) {
            if (exception != null) this.errorHandler.onError(directory, exception);
            return FileVisitResult.CONTINUE;
        }
    }
}
