package de.keksuccino.fancymenu.events;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.eventbus.api.Event;

public class ResourceLoadingFadeScreenPostRenderEvent extends Event {
	
	public Screen screen;
	public MatrixStack matrix;
	
	public ResourceLoadingFadeScreenPostRenderEvent(Screen currentScreen, MatrixStack matrix) {
		this.matrix = matrix;
		this.screen = currentScreen;
	}

}
