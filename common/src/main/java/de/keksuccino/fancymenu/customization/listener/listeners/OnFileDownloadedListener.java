package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.List;

public class OnFileDownloadedListener extends AbstractListener {

    private static final Logger LOGGER = LogManager.getLogger();

    protected String downloadUrl;
    protected String targetFilePath;
    protected boolean successful = false;

    public OnFileDownloadedListener() {

        super("file_downloaded_via_action");

    }

    public void onFileDownloaded(@NotNull String downloadUrl, @NotNull String targetFilePath, boolean successful) {

        // Update cache before notifying instances, so they can use the up-to-date char
        this.downloadUrl = downloadUrl;
        this.targetFilePath = targetFilePath;
        this.successful = successful;

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

        // $$download_succeeded
        list.add(new CustomVariable("download_succeeded", () -> "" + this.successful));

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_file_downloaded");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_file_downloaded.desc"));
    }

}
