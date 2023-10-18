package de.keksuccino.fancymenu.menu.fancy.menuhandler;

import com.mojang.blaze3d.platform.Window;
import de.keksuccino.fancymenu.events.InitOrResizeScreenStartingEvent;
import de.keksuccino.fancymenu.events.OpenScreenEvent;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiBase;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.ClientTickEvent;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MenuHandlerEvents {

	private static final Logger LOGGER = LogManager.getLogger();
	
	private MenuHandlerBase current;
	private Screen lastScreen;
	
	@SubscribeEvent
	public void onOpenGui(OpenScreenEvent e) {
		this.initHandler(e.getScreen());
	}

	//TODO übernehmen
	public void onScreenInitPre(InitOrResizeScreenStartingEvent e) {
		//Second try to register the menu handler, if onOpenGui failed because of changing the menu by another mod
		this.initHandler(e.getScreen());
	}

	@SubscribeEvent
	public void onTick(ClientTickEvent.Pre e) {
		
		//Resetting scale to the normal value when no GUI is active
		if ((MenuHandlerBase.scaleChangedIn != null) && (Minecraft.getInstance().screen == null)) {
			MenuHandlerBase.scaleChangedIn = null;
			Window m = Minecraft.getInstance().getWindow();
			m.setGuiScale(m.calculateScale(Minecraft.getInstance().options.guiScale().get(), Minecraft.getInstance().isEnforceUnicode()));
		}

		//Resetting last active menu handler when no GUI is displayed
		if (Minecraft.getInstance().screen == null) {
			MenuHandlerRegistry.setActiveHandler(null);
		}

		//Play menu close audio on menu close/switch
		if (this.lastScreen != Minecraft.getInstance().screen) {
			if (this.lastScreen != null) {
				MenuHandlerBase handler = MenuHandlerRegistry.getHandlerFor(this.lastScreen);
				if (handler != null) {
					String audio = handler.closeAudio;
					if (audio != null) {
						SoundHandler.resetSound(audio);
						SoundHandler.playSound(audio);
					}
				}
			}
		}

		this.current = MenuHandlerRegistry.getLastActiveHandler();
		this.lastScreen = Minecraft.getInstance().screen;
		
	}
	
	private void initHandler(Screen s) {
		if (s != null) {
			if (MenuCustomization.isBlacklistedMenu(s.getClass().getName())) {
				return;
			}
			if (s instanceof CustomGuiBase c) {
				if (!MenuHandlerRegistry.isHandlerRegistered(c.getIdentifier())) {
					LOGGER.info("[FANCYMENU] Registering MenuHandler for Custom GUI: " + c.getIdentifier());
					MenuHandlerRegistry.registerHandler(new CustomGuiMenuHandlerBase(c.getIdentifier()));
				}
			} else {
				if (!MenuHandlerRegistry.isHandlerRegistered(s.getClass().getName())) {
					if (MenuCustomization.isValidScreen(s)) {
						LOGGER.info("[FANCYMENU] Registering MenuHandler for screen: " + s.getClass().getName());
						MenuHandlerRegistry.registerHandler(new MenuHandlerBase(s.getClass().getName()));
					}
				}
			}
		}
	}

}
