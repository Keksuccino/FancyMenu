package de.keksuccino.fancymenu.events;

import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public class OpenScreenEvent extends EventBase {

    protected final Screen screen;

    public OpenScreenEvent(@NotNull Screen screen) {
        this.screen = Objects.requireNonNull(screen);
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

    public Screen getScreen() {
        return screen;
    }

}
