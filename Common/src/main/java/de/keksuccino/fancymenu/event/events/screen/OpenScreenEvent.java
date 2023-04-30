package de.keksuccino.fancymenu.event.events.screen;

import de.keksuccino.fancymenu.event.acara.EventBase;
import net.minecraft.client.gui.screens.Screen;

public class OpenScreenEvent extends EventBase {

    private final Screen screen;

    protected OpenScreenEvent(Screen screen) {
        this.screen = screen;
    }

    public Screen getScreen() {
        return this.screen;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

    public static class Pre extends OpenScreenEvent {

        public Pre(Screen screen) {
            super(screen);
        }

    }

}
