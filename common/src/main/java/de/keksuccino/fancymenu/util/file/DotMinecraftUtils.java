package de.keksuccino.fancymenu.util.file;

import de.keksuccino.fancymenu.util.OSUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DotMinecraftUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    @NotNull
    public static Path getMinecraftDirectory() {
        String userHome = System.getProperty("user.home");
        Path minecraftPath;
        
        if (OSUtils.isWindows()) {
            // Windows: %APPDATA%\.minecraft
            String appData = System.getenv("APPDATA");
            if (appData != null) {
                minecraftPath = Paths.get(appData, ".minecraft");
            } else {
                // Fallback if APPDATA is not set
                minecraftPath = Paths.get(userHome, "AppData", "Roaming", ".minecraft");
            }
        } else if (OSUtils.isMacOS()) {
            // macOS: ~/Library/Application Support/minecraft
            minecraftPath = Paths.get(userHome, "Library", "Application Support", "minecraft");
        } else if (OSUtils.isLinux()) {
            // Linux: ~/.minecraft
            minecraftPath = Paths.get(userHome, ".minecraft");
        } else {
            // Default fallback for unknown OS
            minecraftPath = Paths.get(userHome, ".minecraft");
        }
        
        // Create the directory if it doesn't exist
        if (!Files.exists(minecraftPath)) {
            try {
                Files.createDirectories(minecraftPath);
            } catch (IOException | SecurityException ex) {
                // Log the error but still return the path
                LOGGER.error("[FANCYMENU] Failed to create .minecraft directory at: " + minecraftPath, ex);
            }
        }
        
        return minecraftPath;
    }

    @NotNull
    public static File getMinecraftDirectoryAsFile() {
        return getMinecraftDirectory().toFile();
    }

    /**
     * Checks if the given path is inside the .minecraft directory.
     * This only validates the path structure, not whether the file/directory actually exists.
     *
     * @param path The path to check
     * @return true if the path is inside the .minecraft directory, false otherwise
     */
    public static boolean isInsideMinecraftDirectory(@Nullable Path path) {
        if (path == null) {
            return false;
        }
        try {
            Path minecraftDir = getMinecraftDirectory().toAbsolutePath().normalize();
            Path targetPath = path.toAbsolutePath().normalize();
            // Check if the target path starts with the minecraft directory path
            return targetPath.startsWith(minecraftDir);
        } catch (Exception e) {
            // Handle any potential path resolution exceptions
            return false;
        }
    }

    /**
     * Checks if the given file is inside the .minecraft directory.
     * This only validates the path structure, not whether the file/directory actually exists.
     *
     * @param file The file to check
     * @return true if the file is inside the .minecraft directory, false otherwise
     */
    public static boolean isInsideMinecraftDirectory(@Nullable File file) {
        if (file == null) {
            return false;
        }
        return isInsideMinecraftDirectory(file.toPath());
    }

    /**
     * Checks if the given path string is inside the .minecraft directory.
     * This only validates the path structure, not whether the file/directory actually exists.
     *
     * @param pathString The path string to check
     * @return true if the path is inside the .minecraft directory, false otherwise
     */
    public static boolean isInsideMinecraftDirectory(@Nullable String pathString) {
        if (pathString == null || pathString.isEmpty()) {
            return false;
        }
        try {
            return isInsideMinecraftDirectory(Paths.get(pathString));
        } catch (Exception e) {
            // Handle invalid path strings
            return false;
        }
    }

    /**
     * Resolves a path that starts with ".minecraft/" by replacing it with the actual minecraft directory path.
     * For example: ".minecraft/saves/world1" becomes "/actual/path/to/.minecraft/saves/world1"
     *
     * @param path The path to resolve
     * @return The resolved path with the correct minecraft directory, or the original path if it doesn't start with ".minecraft/"
     */
    @NotNull
    public static Path resolveMinecraftPath(@NotNull Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }
        
        String pathStr = path.toString().replace('\\', '/');
        
        // Check if path starts with .minecraft/ or just .minecraft
        if (isShortenedDotMinecraftPath(pathStr)) {
            Path minecraftDir = getMinecraftDirectory();
            
            if (pathStr.equals(".minecraft")) {
                return minecraftDir;
            }
            
            // Remove the .minecraft/ prefix and append the rest to the actual minecraft directory
            String relativePart = pathStr.substring(".minecraft/".length());
            return minecraftDir.resolve(relativePart);
        }
        
        // Return original path if it doesn't start with .minecraft/
        return path;
    }

    /**
     * Resolves a file that starts with ".minecraft/" by replacing it with the actual minecraft directory path.
     * For example: ".minecraft/saves/world1" becomes "/actual/path/to/.minecraft/saves/world1"
     *
     * @param file The file to resolve
     * @return The resolved file with the correct minecraft directory, or the original file if it doesn't start with ".minecraft/"
     */
    @NotNull
    public static File resolveMinecraftPath(@NotNull File file) {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        return resolveMinecraftPath(file.toPath()).toFile();
    }

    /**
     * Resolves a path string that starts with ".minecraft/" by replacing it with the actual minecraft directory path.
     * For example: ".minecraft/saves/world1" becomes "/actual/path/to/.minecraft/saves/world1"
     *
     * @param pathString The path string to resolve
     * @return The resolved path with the correct minecraft directory, or the original path if it doesn't start with ".minecraft/"
     */
    @NotNull
    public static String resolveMinecraftPath(@NotNull String pathString) {
        if (pathString == null) {
            throw new IllegalArgumentException("Path string cannot be null");
        }
        try {
            Path resolved = resolveMinecraftPath(Paths.get(pathString));
            return resolved.toString();
        } catch (Exception e) {
            // If path creation fails, try string manipulation directly
            String normalizedPath = pathString.replace('\\', '/');
            if (isShortenedDotMinecraftPath(normalizedPath)) {
                Path minecraftDir = getMinecraftDirectory();
                if (normalizedPath.equals(".minecraft")) {
                    return minecraftDir.toString();
                }
                String relativePart = normalizedPath.substring(".minecraft/".length());
                try {
                    return minecraftDir.resolve(relativePart).toString();
                } catch (Exception ex2) {
                    // Fallback to the original string if resolution still fails (e.g., invalid characters)
                    return pathString;
                }
            }
            return pathString;
        }
    }

    /**
     * If the given path String is a shortened .minecraft path.<br>
     * Shortened .minecraft paths starts with {@code .minecraft/} or equal {@code .minecraft}.
     */
    public static boolean isShortenedDotMinecraftPath(@NotNull String path) {
        path = path.trim();
        return path.startsWith(".minecraft/") || path.equals(".minecraft");
    }

    /**
     * Converts the given path to a shortened .minecraft path IF the input path is inside the .minecraft directory.<br>
     * Returns NULL if the input path is NOT in the .minecraft directory.
     */
    @Nullable
    public static String convertToShortenedDotMinecraftPath(@NotNull String path) {
        if (isShortenedDotMinecraftPath(path)) return path.trim();
        String dotMcDir = getMinecraftDirectoryAsFile().getAbsolutePath().replace("\\", "/");
        path = path.trim();
        File pathFile = new File(path);
        String absolutePath = pathFile.getAbsolutePath().replace("\\", "/");
        if (!absolutePath.startsWith(dotMcDir)) return null;
        return absolutePath.replace(dotMcDir, ".minecraft/");
    }

}
