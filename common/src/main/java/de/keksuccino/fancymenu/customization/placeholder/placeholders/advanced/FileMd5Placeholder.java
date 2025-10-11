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

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;

public class FileMd5Placeholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    public FileMd5Placeholder() {
        super("file_md5");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String filePath = dps.values.get("path");
        
        if (filePath == null || filePath.isEmpty()) {
            LOGGER.warn("[FANCYMENU] File MD5 placeholder: No path provided");
            return "";
        }

        // Don't allow URLs, only local files
        if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
            LOGGER.warn("[FANCYMENU] File MD5 placeholder: URLs are not supported, only local files");
            return "";
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
                LOGGER.warn("[FANCYMENU] File MD5 placeholder: File not found: " + filePath);
                return "";
            }
            
            if (!Files.isRegularFile(path)) {
                LOGGER.warn("[FANCYMENU] File MD5 placeholder: Path is not a regular file: " + filePath);
                return "";
            }
            
            // Calculate MD5 hash
            MessageDigest md = MessageDigest.getInstance("MD5");
            
            try (InputStream is = new FileInputStream(path.toFile())) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) > 0) {
                    md.update(buffer, 0, read);
                }
            }
            
            // Convert byte array to hex string
            byte[] digest = md.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] File MD5 placeholder: Failed to calculate MD5 hash for: " + filePath, e);
            return "";
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
        return I18n.get("fancymenu.placeholders.file_md5");
    }

    @Override
    public @Nullable List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.file_md5.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("path", "/config/fancymenu/config.txt");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
