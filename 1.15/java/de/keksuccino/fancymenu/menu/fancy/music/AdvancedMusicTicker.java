package de.keksuccino.fancymenu.menu.fancy.music;

import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MusicTicker;


public class AdvancedMusicTicker extends MusicTicker {

	public AdvancedMusicTicker(Minecraft client) {
		super(client);
	}
	
	@Override
	public void play(MusicType type) {
		if ((type != null) && (type == MusicType.MENU) && !FancyMenu.config.getOrDefault("playmenumusic", true)) {
			this.stop();
			return;
		}
		super.play(type);
	}

}
