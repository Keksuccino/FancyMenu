package de.keksuccino.fancymenu.menu.guiconstruction.instance;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;

import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.resources.ResourcePackInfo;
import net.minecraft.resources.ResourcePackList;
import net.minecraft.util.text.StringTextComponent;

public class ResourcePacksScreenInstance extends GuiInstance {

	public ResourcePacksScreenInstance(Constructor<?> con, List<Object> paras, Class<?> gui) {
		super(con, paras, gui);
	}
	
	@Override
	protected void createInstance() {
		try {

			Minecraft mc = Minecraft.getInstance();
			
			Consumer<ResourcePackList> c = new Consumer<ResourcePackList>() {
				@Override
				public void accept(ResourcePackList t) {
					GameSettings settings = Minecraft.getInstance().options;
					List<String> list = ImmutableList.copyOf(settings.resourcePacks);
					settings.resourcePacks.clear();
					settings.incompatibleResourcePacks.clear();

					for(ResourcePackInfo resourcepackinfo : t.getSelectedPacks()) {
						if (!resourcepackinfo.isFixedPosition()) {
							settings.resourcePacks.add(resourcepackinfo.getId());
							if (!resourcepackinfo.getCompatibility().isCompatible()) {
								settings.incompatibleResourcePacks.add(resourcepackinfo.getId());
							}
						}
					}

					settings.save();
					List<String> list1 = ImmutableList.copyOf(settings.resourcePacks);
					if (!list1.equals(list)) {
						Minecraft.getInstance().reloadResourcePacks();
					}
				}
			};

			try {
				this.instance = (Screen) con.newInstance(this.findParameter(Screen.class), mc.getResourcePackRepository(), c, mc.getResourcePackDirectory());
			} catch (Exception e) {
				this.instance = (Screen) con.newInstance(this.findParameter(Screen.class), mc.getResourcePackRepository(), c, mc.getResourcePackDirectory(), new StringTextComponent(""));
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
