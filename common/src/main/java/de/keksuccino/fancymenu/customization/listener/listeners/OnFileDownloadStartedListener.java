package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.List;

public class OnFileDownloadStartedListener extends AbstractListener {

    protected String downloadUrl;
    protected String targetFilePath;

    public OnFileDownloadStartedListener() {

        super("file_download_started_via_action");

    }

    public void onFileDownloadStarted(@NotNull String downloadUrl, @NotNull String targetFilePath) {
        if (!this.hasInstancesListening()) {
            return;
        }

        // Update cache before notifying instances, so they can use the up-to-date values
        this.downloadUrl = downloadUrl;
        this.targetFilePath = targetFilePath;

        this.notifyAllInstances();

    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {

        // $$download_url
        list.add(new CustomVariable("download_url", () -> {
            if (this.downloadUrl == null) return "ERROR";
            return this.downloadUrl;
        }));

        // $$target_file_path
        list.add(new CustomVariable("target_file_path", () -> {
            if (this.targetFilePath == null) return "0";
            return this.targetFilePath;
        }));

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_file_download_started");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_file_download_started.desc"));
    }

}
