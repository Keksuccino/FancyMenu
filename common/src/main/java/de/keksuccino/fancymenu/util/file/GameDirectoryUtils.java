package de.keksuccino.fancymenu.util.file;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.platform.Services;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

public class GameDirectoryUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * FancyMenu's own directories, relative to a game directory. Path confinement treats them as relocatable, so users
     * can move them elsewhere (for example to share them between instances) and leave a symbolic link or junction behind.
     */
    public static final List<String> RELOCATABLE_FANCYMENU_DIRECTORIES = List.of(FancyMenu.MOD_DIR_PATH, FancyMenu.INSTANCE_DATA_DIR_PATH);

    public static File getGameDirectory() {
        try {
            if (Services.PLATFORM.isOnClient()) {
                Minecraft minecraft = Minecraft.getInstance();
                if ((minecraft != null) && (minecraft.gameDirectory != null)) {
                    return minecraft.gameDirectory;
                }
            } else {
                Path path = Paths.get("server.properties");
                return path.toAbsolutePath().getParent().toFile();
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to get game directory!", ex);
        }
        Path workingDirectory = Paths.get("").toAbsolutePath().normalize();
        if (Files.isDirectory(workingDirectory)) {
            return workingDirectory.toFile();
        }
        return new File(".");
    }

    public static boolean isExistingGameDirectoryPath(@NotNull String path) {
        Objects.requireNonNull(path);
        path = path.replace("\\", "/");
        String gameDir = getGameDirectory().getAbsolutePath().replace("\\", "/");
        if (!path.startsWith(gameDir)) {
            path = gameDir + "/" + path;
        }
        return new File(path).exists();
    }

    public static String getAbsoluteGameDirectoryPath(@NotNull String path) {
        try {
            path = path.replace("\\", "/");
            String gameDir = getGameDirectory().getAbsolutePath().replace("\\", "/");
            if (!path.startsWith(gameDir)) {
                if (path.startsWith("/")) path = path.substring(1);
                return gameDir + "/" + path;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }

    public static String getPathWithoutGameDirectory(@NotNull String path) {
        Objects.requireNonNull(path);
        File f = new File(getAbsoluteGameDirectoryPath(path));
        String p = f.getAbsolutePath().replace("\\", "/").replace(getGameDirectory().getAbsolutePath().replace("\\", "/"), "");
        if (p.startsWith("/")) p = p.substring(1);
        return p;
    }

}
