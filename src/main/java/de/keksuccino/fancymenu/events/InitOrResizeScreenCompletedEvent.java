package de.keksuccino.fancymenu.events;

import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

//TODO übernehmen
public class InitOrResizeScreenCompletedEvent extends EventBase {

    @NotNull
    protected final Screen screen;

    public InitOrResizeScreenCompletedEvent(@NotNull Screen screen) {
        this.screen = Objects.requireNonNull(screen);
    }

    @NotNull
    public Screen getScreen() {
        return this.screen;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

}
