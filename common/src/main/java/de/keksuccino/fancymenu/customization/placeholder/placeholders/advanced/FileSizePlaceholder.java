package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.file.DotMinecraftUtils;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import net.minecraft.client.resources.language.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class FileSizePlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    public FileSizePlaceholder() {
        super("file_size");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String filePath = dps.values.get("path");
        
        if (filePath == null || filePath.isEmpty()) {
            LOGGER.warn("[FANCYMENU] File Size placeholder: No path provided");
            return "0";
        }

        // Don't allow URLs, only local files
        if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
            LOGGER.warn("[FANCYMENU] File Size placeholder: URLs are not supported, only local files");
            return "0";
        }

        try {
            // Convert short .minecraft path to actual .minecraft path
            filePath = DotMinecraftUtils.resolveMinecraftPath(filePath);
            if (!DotMinecraftUtils.isInsideMinecraftDirectory(filePath)) {
                // Convert the path to a valid game directory path
                filePath = ResourceSource.of(filePath).getSourceWithoutPrefix();
            }
            
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                LOGGER.warn("[FANCYMENU] File Size placeholder: File not found: " + filePath);
                return "0";
            }
            
            if (!Files.isRegularFile(path)) {
                LOGGER.warn("[FANCYMENU] File Size placeholder: Path is not a regular file: " + filePath);
                return "0";
            }
            
            // Get file size in bytes
            long sizeInBytes = Files.size(path);
            return String.valueOf(sizeInBytes);
            
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] File Size placeholder: Failed to get file size for: " + filePath, e);
            return "0";
        }
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("path");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.file_size");
    }

    @Override
    public @Nullable List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.file_size.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.fancymenu.editor.dynamicvariabletextfield.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("path", "/config/fancymenu/config.txt");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
