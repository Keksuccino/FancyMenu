package de.keksuccino.fancymenu.menu.fancy.music;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.BackgroundMusicSelector;
import net.minecraft.client.audio.MusicTicker;


public class AdvancedMusicTicker extends MusicTicker {

	public AdvancedMusicTicker(Minecraft client) {
		super(client);
	}

	@Override
	public void selectRandomBackgroundMusic(BackgroundMusicSelector type) {
		if ((Minecraft.getInstance().world == null) && !FancyMenu.config.getOrDefault("playmenumusic", true)) {
			this.stop();
			return;
		}
		if ((Minecraft.getInstance().world != null) && FancyMenu.config.getOrDefault("stopworldmusicwhencustomizable", false) && (Minecraft.getInstance().currentScreen != null) && MenuCustomization.isMenuCustomizable(Minecraft.getInstance().currentScreen)) {
			Minecraft.getInstance().getSoundHandler().pause();
			return;
		}
		
		super.selectRandomBackgroundMusic(type);
	}

}
