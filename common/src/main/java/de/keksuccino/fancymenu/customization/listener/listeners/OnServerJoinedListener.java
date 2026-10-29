package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OnServerJoinedListener extends AbstractServerIpListener {

    public OnServerJoinedListener() {
        super("server_joined");
    }

    public void onServerJoined(@Nullable String serverIp) {
        this.notifyWithServerIp(serverIp);
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
