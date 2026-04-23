package de.keksuccino.fancymenu.customization.action.actions.other;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.remote.RemoteServerConnectionManager;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CloseRemoteServerConnectionAction extends Action {

    public CloseRemoteServerConnectionAction() {
        super("close_remote_server_connection");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(@Nullable String value) {
        if (value == null) {
            return;
        }

        String requestId = value.trim();
        if (!requestId.isBlank()) {
            RemoteServerConnectionManager.closeConnectionByRequestId(requestId);
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.close_remote_server_connection");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.close_remote_server_connection.desc");
    }

    @Override
    public @Nullable Component getValueDisplayName() {
        return Component.translatable("fancymenu.actions.close_remote_server_connection.value");
    }

    @Override
    public @Nullable String getValuePreset() {
        return "request_id";
    }

}
