package de.keksuccino.fancymenu.menu.fancy.music;

import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.sounds.Music;


public class AdvancedMusicTicker extends MusicManager {

	public AdvancedMusicTicker(Minecraft client) {
		super(client);
	}

	@Override
	public void startPlaying(Music type) {
		if ((Minecraft.getInstance().level == null) && !FancyMenu.getConfig().getOrDefault("playmenumusic", true)) {
			this.stopPlaying();
			return;
		}

		super.startPlaying(type);
	}

}
