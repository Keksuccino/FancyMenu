package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class OnFmDataReceivedListener extends AbstractListener {

    private String lastDataIdentifier = "";
    private String lastData = "";
    private String lastSentBy = "unknown_server";

    public OnFmDataReceivedListener() {
        super("fm_data_received");
    }

    public void onDataReceived(@NotNull String dataIdentifier, @NotNull String data, @NotNull String sentBy) {
        this.lastDataIdentifier = Objects.requireNonNullElse(dataIdentifier, "");
        this.lastData = Objects.requireNonNullElse(data, "");
        this.lastSentBy = Objects.requireNonNullElse(sentBy, "unknown_server");
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("data_identifier", () -> this.lastDataIdentifier));
        list.add(new CustomVariable("data", () -> this.lastData));
        list.add(new CustomVariable("sent_by", () -> this.lastSentBy));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_fm_data_received");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_fm_data_received.desc"));
    }

}
