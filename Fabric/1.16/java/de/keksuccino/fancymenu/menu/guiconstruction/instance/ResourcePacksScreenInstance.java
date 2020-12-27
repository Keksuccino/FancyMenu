package de.keksuccino.fancymenu.menu.guiconstruction.instance;

import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.options.GameOptions;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;

public class ResourcePacksScreenInstance extends GuiInstance {

	public ResourcePacksScreenInstance(Constructor<?> con, List<Object> paras, Class<?> gui) {
		super(con, paras, gui);
	}
	
	@Override
	protected void createInstance() {
		try {

			MinecraftClient mc = MinecraftClient.getInstance();
			GameOptions settings = mc.options;
			Consumer<ResourcePackManager> c = new Consumer<ResourcePackManager>() {
				@Override
				public void accept(ResourcePackManager t) {
					List<String> list = ImmutableList.copyOf(settings.resourcePacks);
					settings.resourcePacks.clear();
					settings.incompatibleResourcePacks.clear();
					Iterator<ResourcePackProfile> var3 = t.getEnabledProfiles().iterator();

					while(var3.hasNext()) {
						ResourcePackProfile resourcePackProfile = (ResourcePackProfile)var3.next();
						if (!resourcePackProfile.isPinned()) {
							settings.resourcePacks.add(resourcePackProfile.getName());
							if (!resourcePackProfile.getCompatibility().isCompatible()) {
								settings.incompatibleResourcePacks.add(resourcePackProfile.getName());
							}
						}
					}

					settings.write();
					List<String> list2 = ImmutableList.copyOf(settings.resourcePacks);
					if (!list2.equals(list)) {
						mc.reloadResources();
					}
				}
			};
			
			this.instance = (Screen) con.newInstance(this.findParameter(Screen.class), mc.getResourceManager(), c, mc.getResourcePackDir());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
