package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class OnRemoteServerDataReceivedListener extends AbstractListener {

    private String lastRequestId = "";
    private String lastRemoteServerUrl = "";
    private String lastData = "";

    public OnRemoteServerDataReceivedListener() {
        super("remote_server_data_received");
    }

    public void onRemoteServerDataReceived(@NotNull String requestId, @NotNull String remoteServerUrl, @NotNull String data) {
        this.lastRequestId = Objects.requireNonNullElse(requestId, "");
        this.lastRemoteServerUrl = Objects.requireNonNullElse(remoteServerUrl, "");
        this.lastData = Objects.requireNonNullElse(data, "");
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("request_id", () -> this.lastRequestId));
        list.add(new CustomVariable("remote_server_url", () -> this.lastRemoteServerUrl));
        list.add(new CustomVariable("data", () -> this.lastData));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_remote_server_data_received");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_remote_server_data_received.desc"));
    }

}
