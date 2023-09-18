package de.keksuccino.fancymenu.events.screen;

import de.keksuccino.fancymenu.util.event.acara.EventBase;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;

public class ScreenTickEvent extends EventBase {

    private final Screen screen;

    public ScreenTickEvent(@NotNull Screen screen) {
        this.screen = screen;
    }

    public Screen getScreen() {
        return this.screen;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

    public static class Pre extends ScreenTickEvent {

        public Pre(@NotNull Screen screen) {
            super(screen);
        }

    }

    public static class Post extends ScreenTickEvent {

        public Post(@NotNull Screen screen) {
            super(screen);
        }

    }

}
