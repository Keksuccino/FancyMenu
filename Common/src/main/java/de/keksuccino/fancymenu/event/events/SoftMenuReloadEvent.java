package de.keksuccino.fancymenu.event.events;

import de.keksuccino.fancymenu.event.acara.EventBase;
import net.minecraft.client.gui.screens.Screen;

/**
 * Fired when soft-reloading the <b>current</b> menu.<br>
 * Not the same as {@link MenuReloadEvent}, which is fired when completely reloading the system and <b>all</b> menus, not just the current one.
 */
public class SoftMenuReloadEvent extends EventBase {

    private final Screen screen;

    public SoftMenuReloadEvent(Screen screen) {
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
