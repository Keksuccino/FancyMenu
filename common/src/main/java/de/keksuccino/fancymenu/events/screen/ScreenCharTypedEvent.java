package de.keksuccino.fancymenu.events.screen;

import de.keksuccino.fancymenu.util.event.acara.EventBase;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;

public class ScreenCharTypedEvent extends EventBase {

    @NotNull
    private final Screen screen;
    private final char character;

    public ScreenCharTypedEvent(@NotNull Screen screen, char character) {
        this.screen = screen;
        this.character = character;
    }

    @NotNull
    public Screen getScreen() {
        return screen;
    }

    public char getCharacter() {
        return character;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

}
