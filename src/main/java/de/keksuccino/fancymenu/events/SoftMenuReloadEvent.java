package de.keksuccino.fancymenu.events;

import de.keksuccino.fancymenu.menu.fancy.helper.MenuReloadedEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired to soft-reload the <b>current</b> menu.<br>
 * Different to the {@link MenuReloadedEvent}, which will completely reload the system and <b>all</b> menus, not just the current one.
 */
public class SoftMenuReloadEvent extends Event {

    public Screen screen;

    public SoftMenuReloadEvent(Screen screen) {
        this.screen = screen;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

}
