package de.keksuccino.fancymenu.menu.fancy.music;

import java.lang.reflect.Field;

import de.keksuccino.konkrete.reflection.ReflectionHelper;
import net.minecraft.client.Minecraft;

public class GameMusicHandler {
	
	public static void init() {
		try {
			Field f = ReflectionHelper.findField(Minecraft.class, "f_91044_");
			f.set(Minecraft.getInstance(), new AdvancedMusicTicker(Minecraft.getInstance()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
