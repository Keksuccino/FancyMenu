package de.keksuccino.fancymenu.menu.fancy.menuhandler;

import com.mojang.blaze3d.platform.Window;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiBase;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.ScreenOpenEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class MenuHandlerEvents {

	private MenuHandlerBase current;
	
	@SubscribeEvent
	public void onOpenGui(ScreenOpenEvent e) {
		this.initHandler(e.getScreen());
	}
	
	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onScreenInitPre(ScreenEvent.InitScreenEvent.Pre e) {
		
		//Second try to register the menu handler, if onOpenGui failed because of changing the menu by another mod
		this.initHandler(e.getScreen());
		
	}

	@SubscribeEvent
	public void onTick(ClientTickEvent e) {

		//Resetting scale to the normal value when no GUI is active
		if ((MenuHandlerBase.scaleChangedIn != null) && (Minecraft.getInstance().screen == null)) {
			MenuHandlerBase.scaleChangedIn = null;
			int mcscale = Minecraft.getInstance().getWindow().calculateScale(Minecraft.getInstance().options.guiScale, Minecraft.getInstance().isEnforceUnicode());
			Window m = Minecraft.getInstance().getWindow();
			m.setGuiScale((double)mcscale);
		}

		//Resetting last active menu handler when no GUI is displayed
		if (Minecraft.getInstance().screen == null) {
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
					MenuHandlerRegistry.registerHandler(new MenuHandlerBase(s.getClass().getName()));
				}
			}
		}
	}

}
