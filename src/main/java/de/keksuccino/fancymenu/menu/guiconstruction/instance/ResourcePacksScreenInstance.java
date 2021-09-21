package de.keksuccino.fancymenu.menu.guiconstruction.instance;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.text.TranslatableText;

public class ResourcePacksScreenInstance extends GuiInstance {

	public ResourcePacksScreenInstance(Constructor<?> con, List<Object> paras, Class<?> gui) {
		super(con, paras, gui);
	}

	protected void createInstance() {
		try {

			MinecraftClient mc = MinecraftClient.getInstance();

			Consumer<ResourcePackManager> updateList = new Consumer<ResourcePackManager>() {
				@Override
				public void accept(ResourcePackManager repo) {
					GameOptions options = MinecraftClient.getInstance().options;
					List<String> list = ImmutableList.copyOf(options.resourcePacks);
					options.resourcePacks.clear();
					options.incompatibleResourcePacks.clear();

					for(ResourcePackProfile pack : repo.getEnabledProfiles()) {
						if (!pack.isPinned()) {
							options.resourcePacks.add(pack.getName());
							if (!pack.getCompatibility().isCompatible()) {
								options.incompatibleResourcePacks.add(pack.getName());
							}
						}
					}

					options.write();
					List<String> list1 = ImmutableList.copyOf(options.resourcePacks);
					if (!list1.equals(list)) {
						MinecraftClient.getInstance().reloadResources();
					}
				}
			};

			this.instance = (Screen) con.newInstance(this.findParameter(Screen.class), mc.getResourcePackManager(), updateList, mc.getResourcePackDir(), new TranslatableText("resourcePack.title"));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
