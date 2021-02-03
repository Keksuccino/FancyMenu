package de.keksuccino.fancymenu.menu.fancy.music;

import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class GameMusicHandler {
	
	public static void init() {
		try {
			Field f = ReflectionHelper.findField(Minecraft.class, "mcMusicTicker", "field_147126_aw");
			f.set(Minecraft.getMinecraft(), new AdvancedMusicTicker(Minecraft.getMinecraft()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
