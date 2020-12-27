package de.keksuccino.fancymenu.menu.fancy.music;

import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MusicTracker;
import net.minecraft.sound.MusicSound;


public class AdvancedMusicTicker extends MusicTracker {

	public AdvancedMusicTicker(MinecraftClient client) {
		super(client);
	}
	
	@Override
	public void play(MusicSound type) {
		if ((MinecraftClient.getInstance().world == null) && !FancyMenu.config.getOrDefault("playmenumusic", true)) {
			this.stop();
			return;
		}

		super.play(type);
	}
	
}
