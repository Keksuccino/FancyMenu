package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
        list.add(new CustomVariable("selected_file_path", () -> (this.selectedFilePath != null) ? this.selectedFilePath : ""));
        list.add(new CustomVariable("target_file_path", () -> (this.targetFilePath != null) ? this.targetFilePath : ""));
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

}
