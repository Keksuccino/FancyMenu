package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class OnRemoteServerConnectionClosedListener extends AbstractListener {

    private String lastRequestId = "";
    private String lastRemoteServerUrl = "";
    private boolean lastIntentionallyClosed = false;
    private boolean lastCrashed = false;
    private boolean lastUnknownCloseReason = false;

    public OnRemoteServerConnectionClosedListener() {
        super("remote_server_connection_closed");
    }

    public void onRemoteServerConnectionClosed(@NotNull String requestId, @NotNull String remoteServerUrl, boolean intentionallyClosed, boolean crashed, boolean unknownCloseReason) {
        this.lastRequestId = Objects.requireNonNullElse(requestId, "");
        this.lastRemoteServerUrl = Objects.requireNonNullElse(remoteServerUrl, "");
        this.lastIntentionallyClosed = intentionallyClosed;
        this.lastCrashed = crashed;
        this.lastUnknownCloseReason = unknownCloseReason;
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("request_id", () -> this.lastRequestId));
        list.add(new CustomVariable("remote_server_url", () -> this.lastRemoteServerUrl));
        list.add(new CustomVariable("intentionally_closed", () -> Boolean.toString(this.lastIntentionallyClosed)));
        list.add(new CustomVariable("crashed", () -> Boolean.toString(this.lastCrashed)));
        list.add(new CustomVariable("unknown_close_reason", () -> Boolean.toString(this.lastUnknownCloseReason)));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_remote_server_connection_closed");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_remote_server_connection_closed.desc"));
    }

}
