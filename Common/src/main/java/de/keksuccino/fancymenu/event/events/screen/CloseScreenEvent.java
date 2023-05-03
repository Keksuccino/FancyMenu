package de.keksuccino.fancymenu.event.events.screen;

import de.keksuccino.fancymenu.event.acara.EventBase;
import net.minecraft.client.gui.screens.Screen;

/** Fired before a screen gets closed and before opening the new screen. **/
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
