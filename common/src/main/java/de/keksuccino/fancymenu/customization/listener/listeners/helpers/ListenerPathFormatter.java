package de.keksuccino.fancymenu.customization.listener.listeners.helpers;

import de.keksuccino.fancymenu.util.file.DotMinecraftUtils;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

/**
 * Formats file paths for listener variables, so users get short paths they can directly use in FancyMenu.
 */
public final class ListenerPathFormatter {

    private ListenerPathFormatter() {
    }

    /**
     * Returns paths inside the game directory as short path relative to it (like "/screenshots/image.png"),
     * paths inside the default .minecraft directory as ".minecraft/..." path and all other paths as sanitized path.<br>
     * Returns an empty string for NULL or blank paths.
     */
    @NotNull
    public static String toFriendlyPath(@Nullable String rawPath) {
        return toFriendlyPath(rawPath, () -> GameDirectoryUtils.getGameDirectory().toPath(), DotMinecraftUtils::getMinecraftDirectory);
    }

    @NotNull
    static String toFriendlyPath(@Nullable String rawPath, @NotNull Supplier<Path> gameDirectorySupplier, @NotNull Supplier<Path> minecraftDirectorySupplier) {
        if ((rawPath == null) || rawPath.isBlank()) {
            return "";
        }

        String sanitized = rawPath.replace("\\", "/");
        if (sanitized.startsWith(".minecraft/") || sanitized.equals(".minecraft")) {
            return sanitized;
        }

        try {
            Path resolved = Paths.get(rawPath).toAbsolutePath().normalize();
            Path gameDir = gameDirectorySupplier.get().toAbsolutePath().normalize();
            if (resolved.startsWith(gameDir)) {
                String relative = gameDir.relativize(resolved).toString().replace("\\", "/");
                if (!relative.startsWith("/")) {
                    relative = "/" + relative;
                }
                return relative;
            }
            Path minecraftDir = minecraftDirectorySupplier.get().toAbsolutePath().normalize();
            if (resolved.startsWith(minecraftDir)) {
                String relative = minecraftDir.relativize(resolved).toString().replace("\\", "/");
                if (!relative.isEmpty()) {
                    return ".minecraft/" + relative;
                }
                return ".minecraft";
            }
        } catch (Exception ignored) {
        }

        if (!sanitized.startsWith("/") && !sanitized.contains(":")) {
            return "/" + sanitized;
        }

        return sanitized;
    }

}
