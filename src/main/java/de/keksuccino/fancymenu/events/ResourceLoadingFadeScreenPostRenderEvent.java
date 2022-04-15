package de.keksuccino.fancymenu.events;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.client.gui.screens.Screen;

public class ResourceLoadingFadeScreenPostRenderEvent extends EventBase {
	
	public Screen screen;
	public PoseStack matrix;
	
	public ResourceLoadingFadeScreenPostRenderEvent(Screen currentScreen, PoseStack matrix) {
		this.matrix = matrix;
		this.screen = currentScreen;
	}
	
	@Override
	public boolean isCancelable() {
		return false;
	}

}
