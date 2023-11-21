package de.keksuccino.fancymenu.events.screen;

import de.keksuccino.fancymenu.util.event.acara.EventBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

/**
 * Gets fired when a {@link Screen} gets opened via {@link Minecraft#setScreen(Screen)}.<br>
 * The {@link Screen} got initialized already at the time this event gets fired.<br><br>
 *
 * If there was a {@link Screen} active before, then it got closed already at the time this event gets fired.
 */
public class OpenScreenPostInitEvent extends EventBase {

    private final Screen screen;

    public OpenScreenPostInitEvent(@NotNull Screen screen) {
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
