package de.keksuccino.fancymenu.menu.fancy.music;

import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class GameMusicHandler {
	
	public static void init() {
		try {
			Field f = ObfuscationReflectionHelper.findField(Minecraft.class, "field_147126_aw");
			f.set(Minecraft.getInstance(), new AdvancedMusicTicker(Minecraft.getInstance()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
