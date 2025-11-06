package de.keksuccino.fancymenu.events.screen;

import de.keksuccino.fancymenu.util.event.acara.EventBase;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import org.jetbrains.annotations.NotNull;

public class ScreenCharTypedEvent extends EventBase {

    private final Screen screen;
    private final CharacterEvent event;

    public ScreenCharTypedEvent(@NotNull Screen screen, @NotNull CharacterEvent event) {
        this.screen = screen;
        this.event = event;
    }

    @NotNull
    public Screen getScreen() {
        return screen;
    }

    @NotNull
    public CharacterEvent getCharacterEvent() {
        return this.event;
    }

    public char getCharacter() {
        return (char) this.event.codepoint();
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

}
