package de.keksuccino.fancymenu.menu.fancy.menuhandler;

import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiBase;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;

@SuppressWarnings("deprecation")
public class MenuHandlerEvents {

	private MenuHandlerBase current;
	
	@SubscribeEvent
	public void onOpenGui(GuiOpenEvent e) {
		this.initHandler(e.getGui());
	}
	
	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onScreenInitPre(GuiScreenEvent.InitGuiEvent.Pre e) {
		//Second try to register the menu handler, if onOpenGui failed because of changing the menu by another mod
		this.initHandler(e.getGui());
	}
	
	@SubscribeEvent
	public void onTick(ClientTickEvent e) {
		
		//Resetting scale to the normal value when no GUI is active
		if ((MenuHandlerBase.scaleChangedIn != null) && (Minecraft.getMinecraft().currentScreen == null)) {
			MenuHandlerBase.scaleChangedIn = null;
			Minecraft.getMinecraft().gameSettings.guiScale = MenuHandlerBase.oriscale;
		}

		//Resetting last active menu handler when no GUI is displayed
		if (Minecraft.getMinecraft().currentScreen == null) {
			MenuHandlerRegistry.setActiveHandler(null);
		}

		//Play menu close audio on menu close/switch
		if (this.current != MenuHandlerRegistry.getLastActiveHandler()) {
			if (this.current != null) {
				String audio = this.current.closeAudio;
				if (audio != null) {
					SoundHandler.resetSound(audio);
					SoundHandler.playSound(audio);
				}
			}
		}
		this.current = MenuHandlerRegistry.getLastActiveHandler();
		
	}

	private void initHandler(GuiScreen s) {
		if (s != null) {
			if (s instanceof CustomGuiBase) {
				if (!MenuHandlerRegistry.isHandlerRegistered(((CustomGuiBase)s).getIdentifier())) {
					MenuHandlerRegistry.registerHandler(new CustomGuiMenuHandlerBase(((CustomGuiBase)s).getIdentifier()));
				}
			} else {
				if (!MenuHandlerRegistry.isHandlerRegistered(s.getClass().getName())) {
					MenuHandlerRegistry.registerHandler(new MenuHandlerBase(s.getClass().getName()));
				}
			}
		}
	}

}
