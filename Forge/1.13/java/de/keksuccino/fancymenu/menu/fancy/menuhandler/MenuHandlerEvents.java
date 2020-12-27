package de.keksuccino.fancymenu.menu.fancy.menuhandler;

import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class MenuHandlerEvents {
	
	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onScreenInitPre(GuiScreenEvent.InitGuiEvent.Pre e) {
		if (!MenuHandlerRegistry.isHandlerRegistered(e.getGui().getClass().getName())) {
			
			MenuHandlerRegistry.registerHandler(new MenuHandlerBase(e.getGui().getClass().getName()));
			
		}
	}

}
