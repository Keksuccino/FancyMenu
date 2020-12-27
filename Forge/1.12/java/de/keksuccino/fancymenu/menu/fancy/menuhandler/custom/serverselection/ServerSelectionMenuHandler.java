package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.serverselection;

import java.lang.reflect.Field;

import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class ServerSelectionMenuHandler extends MenuHandlerBase {
	
	public ServerSelectionMenuHandler() {
		super(GuiMultiplayer.class.getName());
	}
	
	@Override
	public void onButtonsCached(ButtonCachedEvent e) {
		if (this.shouldCustomize(e.getGui()) && MenuCustomization.isMenuCustomizable(e.getGui())) {
			try {
				Field f = ReflectionHelper.findField(GuiMultiplayer.class, "field_146804_i", "savedServerList");
				ServerList savedServers = (ServerList) f.get(e.getGui());
				
				ServerSelectionMenuList list = new ServerSelectionMenuList((GuiMultiplayer) e.getGui(), Minecraft.getMinecraft(), e.getGui().width, e.getGui().height, 32, e.getGui().height - 64, 36, this);
				list.updateOnlineServers(savedServers);
				
				Field f3 = ReflectionHelper.findField(GuiMultiplayer.class, "field_146803_h", "serverListSelector");
				f3.set(e.getGui(), list);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
		super.onButtonsCached(e);
	}

}
