package de.keksuccino.fancymenu.menu.fancy.menuhandler;

import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiBase;
import de.keksuccino.konkrete.events.EventPriority;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.ClientTickEvent;
import de.keksuccino.konkrete.events.client.GuiOpenEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;

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
		if ((MenuHandlerBase.scaleChangedIn != null) && (MinecraftClient.getInstance().currentScreen == null)) {
			MenuHandlerBase.scaleChangedIn = null;
			int mcscale = MinecraftClient.getInstance().getWindow().calculateScaleFactor(MinecraftClient.getInstance().options.guiScale, MinecraftClient.getInstance().forcesUnicodeFont());
			Window m = MinecraftClient.getInstance().getWindow();
			m.setScaleFactor((double)mcscale);
		}

		//Resetting last active menu handler when no GUI is displayed
		if (MinecraftClient.getInstance().currentScreen == null) {
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
	
	private void initHandler(Screen s) {
		if (s != null) {
			if (s instanceof CustomGuiBase) {
				if (!MenuHandlerRegistry.isHandlerRegistered(((CustomGuiBase)s).getIdentifier())) {
					MenuHandlerRegistry.registerHandler(new CustomGuiMenuHandlerBase(((CustomGuiBase)s).getIdentifier()));
				}
			} else {
				if (!MenuHandlerRegistry.isHandlerRegistered(s.getClass().getName())) {
					if (MenuCustomization.isValidScreen(s)) {
						MenuHandlerRegistry.registerHandler(new MenuHandlerBase(s.getClass().getName()));
					}
				}
			}
		}
	}

}
