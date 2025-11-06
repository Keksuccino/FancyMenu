package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class OnOtherPlayerLeftWorldListener extends AbstractListener {

    @Nullable
    private String lastPlayerName;

    @Nullable
    private UUID lastPlayerUuid;

    public OnOtherPlayerLeftWorldListener() {
        super("other_player_left_world");
    }

    public void onOtherPlayerLeft(@Nullable String playerName, @NotNull UUID playerUuid) {
        this.lastPlayerName = playerName;
        this.lastPlayerUuid = playerUuid;
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("player_name", this::formatLastPlayerName));
        list.add(new CustomVariable("player_uuid", this::formatLastPlayerUuid));
    }

    private String formatLastPlayerName() {
        if (this.lastPlayerName == null || this.lastPlayerName.isBlank()) {
            return "ERROR";
        }
        return this.lastPlayerName;
    }

    private String formatLastPlayerUuid() {
        if (this.lastPlayerUuid == null) {
            return "ERROR";
        }
        return this.lastPlayerUuid.toString();
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_other_player_left_world");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_other_player_left_world.desc"));
    }
}