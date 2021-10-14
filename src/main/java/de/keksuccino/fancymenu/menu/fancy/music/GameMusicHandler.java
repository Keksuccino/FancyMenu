package de.keksuccino.fancymenu.menu.fancy.music;

import java.lang.reflect.Field;

import de.keksuccino.konkrete.reflection.ReflectionHelper;
import net.minecraft.client.MinecraftClient;

public class GameMusicHandler {
	
	public static void init() {
		try {
			Field f = ReflectionHelper.findField(MinecraftClient.class, "musicTracker", "field_1714");
			f.set(MinecraftClient.getInstance(), new AdvancedMusicTicker(MinecraftClient.getInstance()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
