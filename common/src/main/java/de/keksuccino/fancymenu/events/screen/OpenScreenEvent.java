package de.keksuccino.fancymenu.events.screen;

import de.keksuccino.fancymenu.util.event.acara.EventBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Gets fired when a {@link Screen} gets opened via {@link Minecraft#setScreen(Screen)}.<br>
 * The {@link Screen} is NOT initialized yet at the time this event gets fired.
 * For a post-init version of this event, use {@link OpenScreenPostInitEvent}.
 */
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
