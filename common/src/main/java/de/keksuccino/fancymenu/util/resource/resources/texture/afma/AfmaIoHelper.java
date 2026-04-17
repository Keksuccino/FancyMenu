package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;

public final class AfmaIoHelper {

    public static final @NotNull String TEMP_DIR_PROPERTY = "fancymenu.afma.temp_dir";
    public static final @NotNull String TEMP_DIR_ENV = "FANCYMENU_AFMA_TEMP_DIR";
    public static final int DEFAULT_PAYLOAD_CHUNK_CACHE_SIZE = 2;
    private static final @NotNull String FALLBACK_TEMP_DIR_NAME = "fancymenu_afma";

    private AfmaIoHelper() {
    }

    @NotNull
    public static String normalizeEntryPath(@NotNull String entryPath) {
        String normalized = entryPath.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        return normalized;
    }

    @NotNull
    public static File createNamedTempDirectory(@NotNull String childName) {
        return ensureDirectory(new File(resolveBaseTempDirectory(), childName));
    }

    @NotNull
    protected static File resolveBaseTempDirectory() {
        String configuredPath = firstNonBlank(System.getProperty(TEMP_DIR_PROPERTY), System.getenv(TEMP_DIR_ENV));
        if (configuredPath != null) {
            return ensureDirectory(new File(configuredPath));
        }

        File fancyMenuTempDir = resolveFancyMenuTempDirectory();
        if (fancyMenuTempDir != null) {
            return ensureDirectory(fancyMenuTempDir);
        }

        String javaTemp = firstNonBlank(System.getProperty("java.io.tmpdir"), ".");
        return ensureDirectory(new File(javaTemp, FALLBACK_TEMP_DIR_NAME));
    }

    @Nullable
    protected static File resolveFancyMenuTempDirectory() {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) {
                classLoader = AfmaIoHelper.class.getClassLoader();
            }
            Class<?> fancyMenuClass = Class.forName("de.keksuccino.fancymenu.FancyMenu", false, classLoader);
            Field tempDirField = fancyMenuClass.getField("TEMP_DATA_DIR");
            Object tempDirValue = tempDirField.get(null);
            if (tempDirValue instanceof File tempDir) {
                return tempDir;
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    @NotNull
    protected static File ensureDirectory(@NotNull File directory) {
        try {
            Files.createDirectories(directory.toPath());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create AFMA temp directory: " + directory.getAbsolutePath(), ex);
        }
        return directory;
    }

    @Nullable
    protected static String firstNonBlank(@Nullable String first, @Nullable String second) {
        if ((first != null) && !first.isBlank()) {
            return first;
        }
        if ((second != null) && !second.isBlank()) {
            return second;
        }
        return null;
    }

}
