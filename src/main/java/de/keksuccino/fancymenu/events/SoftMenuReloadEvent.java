//TODO neu in 1.17
package de.keksuccino.fancymenu.events;

import de.keksuccino.fancymenu.menu.fancy.helper.MenuReloadedEvent;
import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.client.gui.screen.Screen;

/**
 * Fired to soft-reload the <b>current</b> menu.<br>
 * Different to the {@link MenuReloadedEvent}, which will completely reload the system and <b>all</b> menus, not just the current one.
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
