package de.keksuccino.fancymenu.menu.fancy.menuhandler;

import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiBase;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutCreatorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;

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
		
		//Reset scale when opening LayoutCreatorScreen
		if (e.getGui() instanceof LayoutCreatorScreen) {
			if ((MenuHandlerBase.scaleChangedIn != null) && (Minecraft.getMinecraft().currentScreen == null)) {
				MenuHandlerBase.scaleChangedIn = null;
				Minecraft.getMinecraft().gameSettings.guiScale = MenuHandlerBase.oriscale;
				e.setCanceled(true);
				ScaledResolution res = new ScaledResolution(Minecraft.getMinecraft());
				e.getGui().setWorldAndResolution(Minecraft.getMinecraft(), res.getScaledWidth(), res.getScaledHeight());
			}
		}
	}
	
	@SubscribeEvent
	public void onTick(ClientTickEvent e) {
		
		//Resetting scale to the normal value when no GUI is active
		if ((MenuHandlerBase.scaleChangedIn != null) && (Minecraft.getMinecraft().currentScreen == null)) {
			MenuHandlerBase.scaleChangedIn = null;
			Minecraft.getMinecraft().gameSettings.guiScale = MenuHandlerBase.oriscale;
		}
		
	}

}
