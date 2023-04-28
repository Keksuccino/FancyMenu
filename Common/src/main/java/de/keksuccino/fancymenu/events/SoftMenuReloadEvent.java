package de.keksuccino.fancymenu.events;

import de.keksuccino.fancymenu.events.acara.EventBase;
import de.keksuccino.fancymenu.menu.fancy.helper.MenuReloadedEvent;
import net.minecraft.client.gui.screens.Screen;

/**
 * Fired when soft-reloading the <b>current</b> menu.<br>
 * Not the same as {@link MenuReloadedEvent}, which is fired when completely reloading the system and <b>all</b> menus, not just the current one.
 */
public class SoftMenuReloadEvent extends EventBase {

    public Screen screen;

    public SoftMenuReloadEvent(Screen screen) {
        this.screen = screen;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

}
