package de.keksuccino.fancymenu.menu.fancy.music;

import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.BackgroundMusicSelector;
import net.minecraft.client.audio.MusicTicker;


public class AdvancedMusicTicker extends MusicTicker {

	public AdvancedMusicTicker(Minecraft client) {
		super(client);
	}

	@Override
	public void startPlaying(BackgroundMusicSelector type) {
		if ((Minecraft.getInstance().level == null) && !FancyMenu.config.getOrDefault("playmenumusic", true)) {
			this.stopPlaying();
			return;
		}
		
		super.startPlaying(type);
	}

}
