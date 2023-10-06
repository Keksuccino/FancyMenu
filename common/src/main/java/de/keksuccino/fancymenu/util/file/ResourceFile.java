package de.keksuccino.fancymenu.util.file;

import com.google.common.io.Files;
import de.keksuccino.fancymenu.FancyMenu;
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
    protected ResourceFileType type;

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
        resourceFile.type = ResourceFileType.getTypeForFile(resourceFile.file);
        return resourceFile;
    }

    protected ResourceFile() {
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
     * Returns the {@link ResourceFileType} of the file or {@link ResourceFileType#UNKNOWN} if the file does not have a known type.
     */
    @NotNull
    public ResourceFileType getType() {
        return this.type;
    }

    public enum ResourceFileType {

        UNKNOWN("unknown"),
        IMAGE_PNG("png"),
        IMAGE_JPEG_JPG("jpeg", "jpg"),
        IMAGE_GIF("gif"),
        AUDIO_OGG("ogg"),
        AUDIO_WAV("wav"),
        AUDIO_MP3("mp3"),
        VIDEO_MP4("mp4"),
        VIDEO_MKV("mkv"),
        TEXT_TXT("txt");

        private final String[] extensions;

        ResourceFileType(@NotNull String... extensions) {
            this.extensions = extensions;
        }

        public String[] getExtensions() {
            return this.extensions;
        }

        @NotNull
        public static ResourceFileType getTypeForFile(@NotNull File file) {
            if (!file.exists() || file.isDirectory()) return UNKNOWN;
            for (ResourceFileType type : ResourceFileType.values()) {
                for (String extension : type.getExtensions()) {
                    if (file.getPath().toLowerCase().endsWith("." + extension)) return type;
                }
            }
            return UNKNOWN;
        }

    }

}
