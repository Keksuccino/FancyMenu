package de.keksuccino.fancymenu.util.file.type;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class FileTypeRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<String, FileType<?>> FILE_TYPES = new LinkedHashMap<>();

    public static void register(@NotNull String fileTypeName, @NotNull FileType<?> fileType) {
        if (FILE_TYPES.containsKey(Objects.requireNonNull(fileTypeName))) {
            LOGGER.error("[FANCYMENU] Failed to register FileType! FileType name already registered: " + fileTypeName);
            return;
        }
        FILE_TYPES.put(fileTypeName, Objects.requireNonNull(fileType));
    }

    @Nullable
    public static FileType<?> getFileType(@NotNull String fileTypeName) {
        return FILE_TYPES.get(fileTypeName);
    }

    @NotNull
    public static List<FileType<?>> getFileTypes() {
        return new ArrayList<>(FILE_TYPES.values());
    }

}
