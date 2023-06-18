package de.keksuccino.fancymenu.events.screen;

import de.keksuccino.fancymenu.util.event.acara.EventBase;
import net.minecraft.client.gui.screens.Screen;

/** Fired before opening a new screen and after closing the old screen. **/
public class OpenScreenEvent extends EventBase {

    private final Screen screen;

    public OpenScreenEvent(Screen screen) {
        this.screen = screen;
    }

    public Screen getScreen() {
        return this.screen;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

}
