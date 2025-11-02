package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OnServerJoinedListener extends AbstractListener {

    @Nullable
    private String lastServerIp;

    public OnServerJoinedListener() {
        super("server_joined");
    }

    public void onServerJoined(@Nullable String serverIp) {
        this.lastServerIp = serverIp;
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("server_ip", () -> this.formatServerIp(this.lastServerIp)));
    }

    private String formatServerIp(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return "ERROR";
        }
        return value;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_server_joined");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_server_joined.desc"));
    }
}