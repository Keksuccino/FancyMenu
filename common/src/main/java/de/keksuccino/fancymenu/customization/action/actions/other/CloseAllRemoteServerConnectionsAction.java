package de.keksuccino.fancymenu.customization.action.actions.other;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.remote.RemoteServerConnectionManager;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CloseAllRemoteServerConnectionsAction extends Action {

    public CloseAllRemoteServerConnectionsAction() {
        super("close_all_remote_server_connections");
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public void execute(@Nullable String value) {
        RemoteServerConnectionManager.closeAllConnections();
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.close_all_remote_server_connections");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.close_all_remote_server_connections.desc");
    }

    @Override
    public @Nullable Component getValueDisplayName() {
        return null;
    }

    @Override
    public @Nullable String getValuePreset() {
        return null;
    }

}
