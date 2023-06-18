package de.keksuccino.fancymenu.events;

import de.keksuccino.fancymenu.util.event.acara.EventBase;
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
