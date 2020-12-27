package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.serverselection;

import java.lang.reflect.Field;
import java.util.List;

import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.helper.MenuReloadedEvent;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.BackgroundDrawnEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.DrawScreenEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.InitGuiEvent.Pre;
import de.keksuccino.konkrete.reflection.ReflectionHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.options.ServerList;

public class ServerSelectionMenuHandler extends MenuHandlerBase {
	
	public ServerSelectionMenuHandler() {
		super(MultiplayerScreen.class.getName());
	}
	
	@SubscribeEvent
	@Override
	public void onButtonsCached(ButtonCachedEvent e) {
		if (this.shouldCustomize(e.getGui()) && MenuCustomization.isMenuCustomizable(e.getGui())) {
			try {
				Field f = ReflectionHelper.findField(MultiplayerScreen.class, "serverList", "field_3040");
				ServerList savedServers = (ServerList) f.get(e.getGui());
				
				ServerSelectionMenuList list = new ServerSelectionMenuList((MultiplayerScreen) e.getGui(), MinecraftClient.getInstance(), e.getGui().width, e.getGui().height, 32, e.getGui().height - 64, 36, this);
				list.setServers(savedServers);

				Field f3 = ReflectionHelper.findField(MultiplayerScreen.class, "serverListWidget", "field_3043");
				
				e.getGui().children().remove(f3.get(e.getGui()));
				f3.set(e.getGui(), list);
				addChildren(e.getGui(), list);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
		super.onButtonsCached(e);
	}
	
	private static void addChildren(Screen s, Element e) {
		try {
			Field f = ReflectionHelper.findField(Screen.class, "children", "field_22786");
			((List<Element>)f.get(s)).add(e);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
	@SubscribeEvent
	@Override
	public void onInitPre(Pre e) {
		super.onInitPre(e);
	}
	
	@SubscribeEvent
	@Override
	public void onMenuReloaded(MenuReloadedEvent e) {
		super.onMenuReloaded(e);
	}
	
	@SubscribeEvent
	@Override
	public void onRenderPost(DrawScreenEvent.Post e) {
		super.onRenderPost(e);
	}
	
	@SubscribeEvent
	@Override
	public void drawToBackground(BackgroundDrawnEvent e) {
		super.drawToBackground(e);
	}

}
