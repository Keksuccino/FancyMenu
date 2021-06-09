package de.keksuccino.fancymenu.events;

import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.eventbus.api.Event;

public class ResourceLoadingFadeScreenPostRenderEvent extends Event {
	
	public Screen screen;
	
	public ResourceLoadingFadeScreenPostRenderEvent(Screen currentScreen) {
		this.screen = currentScreen;
	}

}
