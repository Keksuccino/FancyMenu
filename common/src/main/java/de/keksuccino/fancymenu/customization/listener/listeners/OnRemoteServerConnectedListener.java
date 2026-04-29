package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class OnRemoteServerConnectedListener extends AbstractListener {

    private String lastRequestId = "";
    private String lastRemoteServerUrl = "";

    public OnRemoteServerConnectedListener() {
        super("remote_server_connected");
    }

    public void onRemoteServerConnected(@NotNull String requestId, @NotNull String remoteServerUrl) {
        this.lastRequestId = Objects.requireNonNullElse(requestId, "");
        this.lastRemoteServerUrl = Objects.requireNonNullElse(remoteServerUrl, "");
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("request_id", () -> this.lastRequestId));
        list.add(new CustomVariable("remote_server_url", () -> this.lastRemoteServerUrl));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_remote_server_connected");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_remote_server_connected.desc"));
    }

}
