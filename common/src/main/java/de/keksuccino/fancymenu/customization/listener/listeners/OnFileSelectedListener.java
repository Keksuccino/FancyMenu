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

public class OnFileSelectedListener extends AbstractListener {

    protected @Nullable String selectedFilePath;
    protected @Nullable String targetFilePath;
    protected boolean selectionSucceeded = false;
    protected boolean selectionCancelled = false;
    protected @Nullable String failureReason;

    public OnFileSelectedListener() {
        super("file_selected_via_action");
    }

    public void onFileSelectionResult(@Nullable String selectedFilePath, @Nullable String targetFilePath, boolean successful, boolean cancelled, @Nullable String failureReason) {
        this.selectedFilePath = selectedFilePath;
        this.targetFilePath = targetFilePath;
        this.selectionSucceeded = successful;
        this.selectionCancelled = cancelled;
        this.failureReason = failureReason;
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("selected_file_path", () -> (this.selectedFilePath != null) ? this.selectedFilePath.replace("\\", "/") : ""));
        list.add(new CustomVariable("target_file_path", () -> this.buildFriendlyTargetPath(this.targetFilePath)));
        list.add(new CustomVariable("selection_succeeded", () -> Boolean.toString(this.selectionSucceeded)));
        list.add(new CustomVariable("selection_cancelled", () -> Boolean.toString(this.selectionCancelled)));
        list.add(new CustomVariable("failure_reason", () -> (this.failureReason != null) ? this.failureReason : ""));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_file_selected");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_file_selected.desc"));
    }

    private @NotNull String buildFriendlyTargetPath(@Nullable String rawPath) {
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
