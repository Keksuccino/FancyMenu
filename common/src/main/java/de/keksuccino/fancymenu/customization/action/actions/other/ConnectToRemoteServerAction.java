package de.keksuccino.fancymenu.customization.action.actions.other;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.remote.RemoteServerConnectionManager;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConnectToRemoteServerAction extends Action {

    public ConnectToRemoteServerAction() {
        super("connect_to_remote_server");
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

        String remoteServerUrl = value.trim();
        if (remoteServerUrl.isBlank()) {
            return;
        }

        RemoteServerConnectionManager.connect(remoteServerUrl);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.connect_to_remote_server");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.connect_to_remote_server.desc");
    }

    @Override
    public @Nullable Component getValueDisplayName() {
        return Component.translatable("fancymenu.actions.connect_to_remote_server.value");
    }

    @Override
    public @Nullable String getValuePreset() {
        return "wss://example.com/ws";
    }

}
