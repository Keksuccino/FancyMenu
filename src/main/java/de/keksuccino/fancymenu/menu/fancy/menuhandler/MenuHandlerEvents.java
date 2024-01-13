package de.keksuccino.fancymenu.menu.fancy.menuhandler;

import com.mojang.blaze3d.platform.Window;
import de.keksuccino.fancymenu.events.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.events.OpenScreenEvent;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiBase;
import de.keksuccino.konkrete.events.EventPriority;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.ClientTickEvent;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class MenuHandlerEvents {

	private MenuHandlerBase current;
	private Screen lastScreen;

	@SubscribeEvent
	public void onOpenGui(OpenScreenEvent e) {
		this.initHandler(e.getScreen());
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onScreenInitPre(InitOrResizeScreenEvent.Pre e) {
		//Second try to register the menu handler, if onOpenGui failed because of changing the menu by another mod
		this.initHandler(e.getScreen());
	}

	@SubscribeEvent
	public void onTick(ClientTickEvent.Pre e) {

		//Resetting scale to the normal value when no GUI is active
		if ((MenuHandlerBase.scaleChangedIn != null) && (Minecraft.getInstance().screen == null)) {
			MenuHandlerBase.scaleChangedIn = null;
			Window m = Minecraft.getInstance().getWindow();
			m.setGuiScale(m.calculateScale(Minecraft.getInstance().options.guiScale, Minecraft.getInstance().isEnforceUnicode()));
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
