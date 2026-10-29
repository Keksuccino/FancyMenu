package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OnStartJoiningServerListener extends AbstractServerIpListener {

    public OnStartJoiningServerListener() {
        super("start_joining_server");
    }

    public void onStartJoiningServer(@Nullable String serverIp) {
        this.notifyWithServerIp(serverIp);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_start_joining_server");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_start_joining_server.desc"));
    }

}
