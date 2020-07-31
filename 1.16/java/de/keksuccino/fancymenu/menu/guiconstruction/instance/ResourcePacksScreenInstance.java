package de.keksuccino.fancymenu.menu.guiconstruction.instance;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;

import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resources.ClientResourcePackInfo;
import net.minecraft.resources.ResourcePackList;

public class ResourcePacksScreenInstance extends GuiInstance {

	public ResourcePacksScreenInstance(Constructor<?> con, List<Object> paras, Class<?> gui) {
		super(con, paras, gui);
	}
	
	@Override
	protected void createInstance() {
		try {
			
			Minecraft mc = Minecraft.getInstance();
			
			Consumer<ResourcePackList<ClientResourcePackInfo>> c = new Consumer<ResourcePackList<ClientResourcePackInfo>>() {
				@Override
				public void accept(ResourcePackList<ClientResourcePackInfo> t) {
					GameSettings settings = Minecraft.getInstance().gameSettings;
					List<String> list = ImmutableList.copyOf(settings.resourcePacks);
					settings.resourcePacks.clear();
					settings.incompatibleResourcePacks.clear();

					for(ClientResourcePackInfo clientresourcepackinfo : t.getEnabledPacks()) {
						if (!clientresourcepackinfo.isOrderLocked()) {
							settings.resourcePacks.add(clientresourcepackinfo.getName());
							if (!clientresourcepackinfo.getCompatibility().isCompatible()) {
								settings.incompatibleResourcePacks.add(clientresourcepackinfo.getName());
							}
						}
					}

					settings.saveOptions();
					List<String> list1 = ImmutableList.copyOf(settings.resourcePacks);
					if (!list1.equals(list)) {
						Minecraft.getInstance().reloadResources();
					}
				}
			};
			
			this.instance = (Screen) con.newInstance(this.findParameter(Screen.class), mc.getResourcePackList(), c, mc.getFileResourcePacks());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
