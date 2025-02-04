package de.keksuccino.fancymenu.events.screen;

import de.keksuccino.fancymenu.util.event.acara.EventBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Gets fired before a {@link Screen} gets closed by setting a new {@link Screen} (or no screen) via {@link Minecraft#setScreen(Screen)}.<br>
 * The new {@link Screen} is not opened yet when this event gets fired.
 */
public class CloseScreenEvent extends EventBase {

    private final Screen closedScreen;
    private final Screen newScreen;

    public CloseScreenEvent(@NotNull Screen closedScreen, @Nullable Screen newScreen) {
        this.closedScreen = closedScreen;
        this.newScreen = newScreen;
    }

    @Deprecated
    public Screen getScreen() {
        return this.closedScreen;
    }

    @NotNull
    public Screen getClosedScreen() {
        return this.closedScreen;
    }

    @Nullable
    public Screen getNewScreen() {
        return this.newScreen;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

}
