package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OnTextClickedListener extends AbstractListener {

    @Nullable
    private String lastEventId;

    public OnTextClickedListener() {
        super("text_clicked");
    }

    public void onTextClicked(@NotNull String eventId) {
        this.lastEventId = eventId;
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("text_event_id", () -> this.lastEventId != null ? this.lastEventId : "ERROR"));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_text_clicked");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_text_clicked.desc"));
    }
}
