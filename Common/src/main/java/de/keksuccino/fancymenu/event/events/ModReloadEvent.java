package de.keksuccino.fancymenu.event.events;

import de.keksuccino.fancymenu.event.acara.EventBase;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public class ModReloadEvent extends EventBase {
	
	private final Screen screen;
	
	public ModReloadEvent(Screen screen) {
		this.screen = screen;
	}

	@Nullable
	public Screen getScreen() {
		return this.screen;
	}
	
	@Override
	public boolean isCancelable() {
		return false;
	}

}
