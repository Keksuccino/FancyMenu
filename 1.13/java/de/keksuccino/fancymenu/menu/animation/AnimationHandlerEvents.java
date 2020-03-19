package de.keksuccino.fancymenu.menu.animation;

import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class AnimationHandlerEvents {
	
	@SubscribeEvent
	public void onInitPre(GuiScreenEvent.InitGuiEvent.Pre e) {
		if (e.getGui() instanceof GuiMainMenu) {
			if (!AnimationHandler.isReady()) {
				AnimationHandler.setupAnimations();
			}
		}
	}

}
