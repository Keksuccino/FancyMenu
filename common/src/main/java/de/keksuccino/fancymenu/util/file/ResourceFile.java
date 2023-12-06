package de.keksuccino.fancymenu.util.file;

import com.google.common.io.Files;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.file.type.FileMediaType;
import de.keksuccino.fancymenu.util.file.type.FileType;
import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.resource.ResourceSourceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;

@SuppressWarnings("unused")
public class ResourceFile {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final File ASSETS_DIR = new File(FancyMenu.MOD_DIR, "/assets");

    protected String shortPath;
    protected File file;
    protected FileType<?> type;
    @Nullable
    protected ResourceSourceType resourceSourceType;

    /**
     * Returns a new {@link ResourceFile} instance for the given asset.<br>
     * Asset files need to be in '/config/fancymenu/assets/' and need to be valid, existing files/directories.<br>
     * Will return NULL if the given file does not exist or is not in '/config/fancymenu/assets/'.
     */
    @Nullable
    public static ResourceFile asset(@NotNull File gameDirectoryFile) {
        return asset(gameDirectoryFile.getAbsolutePath());
    }

    /**
     * Returns a new {@link ResourceFile} instance for the given asset.<br>
     * Asset files need to be in '/config/fancymenu/assets/' and need to be valid, existing files/directories.<br>
     * Will return NULL if the given file does not exist or is not in '/config/fancymenu/assets/'.
     */
    @Nullable
    public static ResourceFile asset(@NotNull String gameDirectoryFilePath) {
        ResourceFile resourceFile = of(gameDirectoryFilePath);
        if (!resourceFile.isExistingAsset()) {
            LOGGER.error("[FANCYMENU] Asset ResourceFile does not exist or is not in '/config/fancymenu/assets/': " + gameDirectoryFilePath);
            return null;
        }
        return resourceFile;
    }

    /**
     * Returns a new {@link ResourceFile} instance for the given file.<br>
     * The file needs to be in the game instance directory.<br>
     * The file does not need to exist.
     */
    @NotNull
    public static ResourceFile of(@NotNull File gameDirectoryFile) {
        return of(gameDirectoryFile.getAbsolutePath());
    }

    /**
     * Returns a new {@link ResourceFile} instance for the given file.<br>
     * The file needs to be in the game instance directory.<br>
     * The file does not need to exist.
     */
    @NotNull
    public static ResourceFile of(@NotNull String gameDirectoryFilePath) {
        ResourceFile resourceFile = new ResourceFile();
        if (ResourceSourceType.hasSourcePrefix(gameDirectoryFilePath)) {
            resourceFile.resourceSourceType = ResourceSourceType.getSourceTypeOf(gameDirectoryFilePath);
        }
        gameDirectoryFilePath = ResourceSourceType.getWithoutSourcePrefix(gameDirectoryFilePath);
        gameDirectoryFilePath = gameDirectoryFilePath.replace("\\", "/");
        gameDirectoryFilePath = GameDirectoryUtils.getPathWithoutGameDirectory(gameDirectoryFilePath).replace("\\", "/");
        if (!gameDirectoryFilePath.startsWith("/")) {
            gameDirectoryFilePath = "/" + gameDirectoryFilePath;
        }
        if (gameDirectoryFilePath.replace(" ", "").replace("/", "").isEmpty()) {
            gameDirectoryFilePath = "";
        }
        resourceFile.file = new File(GameDirectoryUtils.getGameDirectory(), gameDirectoryFilePath);
        resourceFile.shortPath = gameDirectoryFilePath;
        resourceFile.type = FileTypes.getLocalType(resourceFile.file);
        if (resourceFile.type == null) {
            resourceFile.type = FileTypes.UNKNOWN;
        }
        if (resourceFile.resourceSourceType == null) {
            resourceFile.resourceSourceType = ResourceSourceType.getSourceTypeOf(gameDirectoryFilePath);
        }
        return resourceFile;
    }

    protected ResourceFile() {
    }

    @Nullable
    public ResourceSourceType getResourceSourceType() {
        return this.resourceSourceType;
    }

    @NotNull
    public String getAsResourceSource() {
        String prefix = (this.resourceSourceType != null) ? this.resourceSourceType.getSourcePrefix() : "";
        return prefix + this.getShortPath();
    }

    @NotNull
    public String getShortPath() {
        return this.shortPath;
    }

    @NotNull
    public String getAbsolutePath() {
        return this.file.getAbsolutePath();
    }

    @NotNull
    public File getFile() {
        return this.file;
    }

    /**
     * Returns the file extension or an empty String if the given file has no extension or is a directory.
     */
    @NotNull
    public String getFileExtension() {
        return Files.getFileExtension(this.shortPath);
    }

    @NotNull
    public String getFileNameWithoutExtension() {
        return Files.getNameWithoutExtension(this.shortPath);
    }

    @NotNull
    public String getFileName() {
        return this.file.getName();
    }

    public boolean exists() {
        return this.file.exists();
    }

    /**
     * Returns TRUE if the file exists AND is a file, otherwise returns FALSE.
     */
    public boolean isFile() {
        return this.file.isFile();
    }

    /**
     * Returns TRUE if the file exists AND is a directory, otherwise returns FALSE.
     */
    public boolean isDirectory() {
        return this.file.isDirectory();
    }

    public boolean isExistingAsset() {
        return this.exists() && this.isAsset();
    }

    public boolean isAsset() {
        return this.shortPath.startsWith("/config/fancymenu/assets/");
    }

    /**
     * Returns the {@link FileType} of the file or {@link FileTypes#UNKNOWN} if the file does not have a known type.
     */
    @NotNull
    public FileType<?> getType() {
        return this.type;
    }

    @NotNull
    public FileMediaType getMediaType() {
        return this.type.getMediaType();
    }

}
