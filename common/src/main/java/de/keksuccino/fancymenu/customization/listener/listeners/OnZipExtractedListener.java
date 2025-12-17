package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.file.DotMinecraftUtils;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class OnZipExtractedListener extends AbstractListener {

    protected @Nullable String sourceZipPath;
    protected @Nullable String targetDirectoryPath;
    protected boolean successful;
    protected @Nullable String failureReason;

    public OnZipExtractedListener() {
        super("zip_extracted_via_action");
    }

    public void onZipExtracted(@NotNull String sourceZipPath, @NotNull String targetDirectoryPath, boolean successful, @Nullable String failureReason) {
        this.sourceZipPath = sourceZipPath;
        this.targetDirectoryPath = targetDirectoryPath;
        this.successful = successful;
        this.failureReason = failureReason;
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("source_zip_path", () -> this.buildFriendlyPath(this.sourceZipPath)));
        list.add(new CustomVariable("target_folder_path", () -> this.buildFriendlyPath(this.targetDirectoryPath)));
        list.add(new CustomVariable("extract_succeeded", () -> Boolean.toString(this.successful)));
        list.add(new CustomVariable("failure_reason", () -> (this.failureReason != null) ? this.failureReason : ""));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_zip_extracted");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_zip_extracted.desc"));
    }

    private @NotNull String buildFriendlyPath(@Nullable String rawPath) {
        if ((rawPath == null) || rawPath.isBlank()) {
            return "";
        }

        String sanitized = rawPath.replace("\\", "/");
        if (sanitized.startsWith(".minecraft/") || sanitized.equals(".minecraft")) {
            return sanitized;
        }

        try {
            Path resolved = Paths.get(rawPath).toAbsolutePath().normalize();
            Path gameDir = GameDirectoryUtils.getGameDirectory().toPath().toAbsolutePath().normalize();
            if (resolved.startsWith(gameDir)) {
                String relative = gameDir.relativize(resolved).toString().replace("\\", "/");
                if (!relative.startsWith("/")) {
                    relative = "/" + relative;
                }
                return relative;
            }
            Path minecraftDir = DotMinecraftUtils.getMinecraftDirectory().toAbsolutePath().normalize();
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
