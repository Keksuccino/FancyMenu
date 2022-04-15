package de.keksuccino.fancymenu.menu.guiconstruction.instance;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import com.google.common.collect.ImmutableList;

public class ResourcePacksScreenInstance extends GuiInstance {

	public ResourcePacksScreenInstance(Constructor<?> con, List<Object> paras, Class<?> gui) {
		super(con, paras, gui);
	}

	protected void createInstance() {
		try {

			Minecraft mc = Minecraft.getInstance();

			Consumer<PackRepository> updateList = new Consumer<PackRepository>() {
				@Override
				public void accept(PackRepository repo) {
					Options options = Minecraft.getInstance().options;
					List<String> list = ImmutableList.copyOf(options.resourcePacks);
					options.resourcePacks.clear();
					options.incompatibleResourcePacks.clear();

					for(Pack pack : repo.getSelectedPacks()) {
						if (!pack.isFixedPosition()) {
							options.resourcePacks.add(pack.getId());
							if (!pack.getCompatibility().isCompatible()) {
								options.incompatibleResourcePacks.add(pack.getId());
							}
						}
					}

					options.save();
					List<String> list1 = ImmutableList.copyOf(options.resourcePacks);
					if (!list1.equals(list)) {
						Minecraft.getInstance().reloadResourcePacks();
					}
				}
			};

			this.instance = (Screen) con.newInstance(this.findParameter(Screen.class), mc.getResourcePackRepository(), updateList, mc.getResourcePackDirectory(), new TranslatableComponent("resourcePack.title"));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
