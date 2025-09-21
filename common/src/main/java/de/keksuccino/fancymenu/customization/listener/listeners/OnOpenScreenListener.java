package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OnOpenScreenListener extends AbstractListener {

    @Nullable
    private String lastScreenIdentifier;

    public OnOpenScreenListener() {
        super("screen_open");
    }

    public void onScreenOpened(@NotNull Screen screen) {
        this.lastScreenIdentifier = ScreenIdentifierHandler.getIdentifierOfScreen(screen);
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("screen_identifier", () -> this.lastScreenIdentifier != null ? this.lastScreenIdentifier : "ERROR"));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_open_screen");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_open_screen.desc"));
    }
}