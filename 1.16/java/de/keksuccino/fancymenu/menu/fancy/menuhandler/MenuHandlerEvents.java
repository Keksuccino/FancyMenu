package de.keksuccino.fancymenu.menu.fancy.menuhandler;

import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiBase;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class MenuHandlerEvents {
	
	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onScreenInitPre(GuiScreenEvent.InitGuiEvent.Pre e) {
		if (e.getGui() instanceof CustomGuiBase) {
			if (!MenuHandlerRegistry.isHandlerRegistered(((CustomGuiBase)e.getGui()).getIdentifier())) {
				MenuHandlerRegistry.registerHandler(new CustomGuiMenuHandlerBase(((CustomGuiBase)e.getGui()).getIdentifier()));
			}
		} else {
			if (!MenuHandlerRegistry.isHandlerRegistered(e.getGui().getClass().getName())) {
				MenuHandlerRegistry.registerHandler(new MenuHandlerBase(e.getGui().getClass().getName()));
			}
		}
	}

	@SubscribeEvent
	public void onTick(ClientTickEvent e) {
		
		//Resetting scale to the normal value when no GUI is active
		if ((MenuHandlerBase.scaleChangedIn != null) && (Minecraft.getInstance().currentScreen == null)) {
			MenuHandlerBase.scaleChangedIn = null;
			int mcscale = Minecraft.getInstance().getMainWindow().calcGuiScale(Minecraft.getInstance().gameSettings.guiScale, Minecraft.getInstance().getForceUnicodeFont());
			MainWindow m = Minecraft.getInstance().getMainWindow();
			m.setGuiScale((double)mcscale);
		}
		
	}

}
