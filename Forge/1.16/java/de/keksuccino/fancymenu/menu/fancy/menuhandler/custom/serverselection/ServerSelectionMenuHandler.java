package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.serverselection;

import java.lang.reflect.Field;
import java.util.List;

import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.MultiplayerScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class ServerSelectionMenuHandler extends MenuHandlerBase {
	
	public ServerSelectionMenuHandler() {
		super(MultiplayerScreen.class.getName());
	}
	
	@Override
	public void onButtonsCached(ButtonCachedEvent e) {
		if (this.shouldCustomize(e.getGui()) && MenuCustomization.isMenuCustomizable(e.getGui())) {
			try {
				Field f = ObfuscationReflectionHelper.findField(MultiplayerScreen.class, "field_146804_i");
				ServerList savedServers = (ServerList) f.get(e.getGui());
				
				ServerSelectionMenuList list = new ServerSelectionMenuList((MultiplayerScreen) e.getGui(), Minecraft.getInstance(), e.getGui().width, e.getGui().height, 32, e.getGui().height - 64, 36, this);
				list.updateOnlineServers(savedServers);
				
				Field f3 = ObfuscationReflectionHelper.findField(MultiplayerScreen.class, "field_146803_h");
				
				e.getGui().getEventListeners().remove(f3.get(e.getGui()));
				f3.set(e.getGui(), list);
				addChildren(e.getGui(), list);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
		super.onButtonsCached(e);
	}
	
	private static void addChildren(Screen s, IGuiEventListener e) {
		try {
			Field f = ObfuscationReflectionHelper.findField(Screen.class, "field_230705_e_");
			((List<IGuiEventListener>)f.get(s)).add(e);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

}
