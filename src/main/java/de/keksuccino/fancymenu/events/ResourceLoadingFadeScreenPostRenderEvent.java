package de.keksuccino.fancymenu.events;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.eventbus.api.Event;

public class ResourceLoadingFadeScreenPostRenderEvent extends Event {
	
	public Screen screen;
	public GuiGraphics graphics;
	
	public ResourceLoadingFadeScreenPostRenderEvent(Screen currentScreen, GuiGraphics graphics) {
		this.graphics = graphics;
		this.screen = currentScreen;
	}

}
