package de.keksuccino.fancymenu.events;

import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;

public class ResourceLoadingFadeScreenPostRenderEvent extends EventBase {
	
	public Screen screen;
	public MatrixStack matrix;
	
	public ResourceLoadingFadeScreenPostRenderEvent(Screen currentScreen, MatrixStack matrix) {
		this.matrix = matrix;
		this.screen = currentScreen;
	}
	
	@Override
	public boolean isCancelable() {
		return false;
	}

}
