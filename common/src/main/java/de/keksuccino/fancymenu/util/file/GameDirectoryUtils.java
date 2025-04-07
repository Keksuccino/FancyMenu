package de.keksuccino.fancymenu.util.file;

import de.keksuccino.fancymenu.platform.Services;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class GameDirectoryUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    public static File getGameDirectory() {
        try {
            if (Services.PLATFORM.isOnClient()) {
                return Minecraft.getInstance().gameDirectory;
            } else {
                Path path = Paths.get("server.properties");
                return path.toAbsolutePath().getParent().toFile();
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to get game directory!", ex);
        }
        return new File("");
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
