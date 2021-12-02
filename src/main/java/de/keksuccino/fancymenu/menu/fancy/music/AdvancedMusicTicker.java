package de.keksuccino.fancymenu.menu.fancy.music;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.sounds.Music;


public class AdvancedMusicTicker extends MusicManager {

	public AdvancedMusicTicker(Minecraft client) {
		super(client);
	}

	@Override
	public void startPlaying(Music type) {
		if ((Minecraft.getInstance().level == null) && !FancyMenu.config.getOrDefault("playmenumusic", true)) {
			this.stopPlaying();
			return;
		}
		if ((Minecraft.getInstance().level != null) && FancyMenu.config.getOrDefault("stopworldmusicwhencustomizable", false) && (Minecraft.getInstance().screen != null) && MenuCustomization.isMenuCustomizable(Minecraft.getInstance().screen)) {
			Minecraft.getInstance().getSoundManager().pause();
			return;
		}

		super.startPlaying(type);
	}

}
