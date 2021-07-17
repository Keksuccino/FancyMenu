package de.keksuccino.fancymenu.events;

import de.keksuccino.fancymenu.menu.fancy.helper.MenuReloadedEvent;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Fired to soft-reload the <b>current</b> menu.<br>
 * Different to the {@link MenuReloadedEvent}, which will completely reload the system and <b>all</b> menus, not just the current one.
 */
public class SoftMenuReloadEvent extends Event {

    public GuiScreen screen;

    public SoftMenuReloadEvent(GuiScreen screen) {
        this.screen = screen;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

}
