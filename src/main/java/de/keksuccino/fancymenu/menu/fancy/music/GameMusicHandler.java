package de.keksuccino.fancymenu.menu.fancy.music;

import de.keksuccino.fancymenu.mixin.client.IMixinMinecraft;
import net.minecraft.client.Minecraft;

public class GameMusicHandler {
	
	public static void init() {
		((IMixinMinecraft)Minecraft.getInstance()).setMusicManagerFancyMenu(new AdvancedMusicTicker(Minecraft.getInstance()));
	}

}
