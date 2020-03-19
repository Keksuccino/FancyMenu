package de.keksuccino.fancymenu.menu.animation;

import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class AnimationHandlerEvents {
	
	@SubscribeEvent
	public void onInitPre(GuiScreenEvent.InitGuiEvent.Pre e) {
		if (e.getGui() instanceof MainMenuScreen) {
			if (!AnimationHandler.isReady()) {
				AnimationHandler.setupAnimations();
			}
		}
	}

}
