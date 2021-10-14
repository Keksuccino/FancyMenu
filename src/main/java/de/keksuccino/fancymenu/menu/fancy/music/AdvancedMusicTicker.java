package de.keksuccino.fancymenu.menu.fancy.music;

import java.lang.reflect.Field;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MusicTicker;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class AdvancedMusicTicker extends MusicTicker {

	public AdvancedMusicTicker(Minecraft client) {
		super(client);
	}
	
	@Override
	public void playMusic(MusicType type) {
		if ((type != null) && (type == MusicType.MENU) && !FancyMenu.config.getOrDefault("playmenumusic", true)) {
			return;
		}
		if ((Minecraft.getMinecraft().world != null) && FancyMenu.config.getOrDefault("stopworldmusicwhencustomizable", false) && (Minecraft.getMinecraft().currentScreen != null) && MenuCustomization.isMenuCustomizable(Minecraft.getMinecraft().currentScreen)) {
			Minecraft.getMinecraft().getSoundHandler().pauseSounds();
			return;
		}
		
		super.playMusic(type);
	}
	
	public void stop() {
		if (this.getCurrentMusic() != null) {
	         Minecraft.getMinecraft().getSoundHandler().stopSound(this.getCurrentMusic());
	         this.setCurrentMusic(null);
	         this.setTimeUntilNext(0);
	      }
	}
	
	protected ISound getCurrentMusic() {
		try {
			//TODO reflection
			//Field f = ReflectionHelper.findField(MusicTicker.class, "currentMusic", "field_147678_c");
			Field f = ObfuscationReflectionHelper.findField(MusicTicker.class, "field_147678_c"); //currentMusic
			return (ISound) f.get(this);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	protected void setCurrentMusic(ISound sound) {
		try {
			//TODO reflection
			//Field f = ReflectionHelper.findField(MusicTicker.class, "currentMusic", "field_147678_c");
			Field f = ObfuscationReflectionHelper.findField(MusicTicker.class, "field_147678_c"); //currentMusic
			f.set(this, sound);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void setTimeUntilNext(int time) {
		try {
			//TODO reflection
			//Field f = ReflectionHelper.findField(MusicTicker.class, "timeUntilNextMusic", "field_147676_d");
			Field f = ObfuscationReflectionHelper.findField(MusicTicker.class, "field_147676_d"); //timeUntilNextMusic
			f.set(this, time);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

}
