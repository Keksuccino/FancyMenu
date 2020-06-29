package de.keksuccino.fancymenu.menu.fancy.music;

import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.BackgroundMusicSelector;
import net.minecraft.client.audio.MusicTicker;


public class AdvancedMusicTicker extends MusicTicker {

	public AdvancedMusicTicker(Minecraft client) {
		super(client);
	}
	
	//play
	@Override
	public void func_239539_a_(BackgroundMusicSelector type) {
		if ((Minecraft.getInstance().world == null) && !FancyMenu.config.getOrDefault("playmenumusic", true)) {
			this.stop();
			return;
		}
		
		super.func_239539_a_(type);
	}

}
