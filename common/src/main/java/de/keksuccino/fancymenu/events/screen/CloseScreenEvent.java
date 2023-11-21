package de.keksuccino.fancymenu.events.screen;

import de.keksuccino.fancymenu.util.event.acara.EventBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Gets fired before a {@link Screen} gets closed by setting a new {@link Screen} (or no screen) via {@link Minecraft#setScreen(Screen)}.<br>
 * The new {@link Screen} is not opened yet when this event gets fired.
 */
public class CloseScreenEvent extends EventBase {

    private final Screen screen;

    public CloseScreenEvent(Screen closedScreen) {
        this.screen = closedScreen;
    }

    public Screen getScreen() {
        return this.screen;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

}
