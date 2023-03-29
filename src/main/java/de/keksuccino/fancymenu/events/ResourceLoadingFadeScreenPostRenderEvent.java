package de.keksuccino.fancymenu.events;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.eventbus.api.Event;

public class ResourceLoadingFadeScreenPostRenderEvent extends Event {
	
	public Screen screen;
	public PoseStack matrix;
	
	public ResourceLoadingFadeScreenPostRenderEvent(Screen currentScreen, PoseStack matrix) {
		this.matrix = matrix;
		this.screen = currentScreen;
	}

}
