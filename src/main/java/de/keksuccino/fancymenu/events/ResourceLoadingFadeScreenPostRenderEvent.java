package de.keksuccino.fancymenu.events;

import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.client.gui.screens.Screen;

public class ResourceLoadingFadeScreenPostRenderEvent extends EventBase {
	
	public Screen screen;
	public GuiGraphics graphics;
	
	public ResourceLoadingFadeScreenPostRenderEvent(Screen currentScreen, GuiGraphics graphics) {
		this.graphics = graphics;
		this.screen = currentScreen;
	}
	
	@Override
	public boolean isCancelable() {
		return false;
	}

}
