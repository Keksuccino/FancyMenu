package de.keksuccino.fancymenu.events;

import de.keksuccino.fancymenu.util.event.acara.EventBase;
import net.minecraft.client.gui.screens.Screen;

/**
 * Fired when soft-reloading the <b>current</b> menu.<br>
 * Not the same as {@link ModReloadEvent}, which is fired when completely reloading the system and <b>all</b> menus, not just the current one.
 */
public class ScreenReloadEvent extends EventBase {

    private final Screen screen;

    public ScreenReloadEvent(Screen screen) {
        this.screen = screen;
    }

    public Screen getScreen() {
        return screen;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

}
