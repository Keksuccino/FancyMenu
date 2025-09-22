package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;

public class OnQuitMinecraftListener extends AbstractListener {

    @Nullable
    private String cachedTimestampMillis;
    @Nullable
    private String cachedTimestampIso;

    public OnQuitMinecraftListener() {
        super("quit_minecraft");
    }

    public void onQuitMinecraft() {
        long now = System.currentTimeMillis();
        this.cachedTimestampMillis = Long.toString(now);
        this.cachedTimestampIso = Instant.ofEpochMilli(now).toString();
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("timestamp_millis", () -> this.cachedTimestampMillis != null ? this.cachedTimestampMillis : "ERROR"));
        list.add(new CustomVariable("timestamp_iso", () -> this.cachedTimestampIso != null ? this.cachedTimestampIso : "ERROR"));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_quit_minecraft");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_quit_minecraft.desc"));
    }
}
