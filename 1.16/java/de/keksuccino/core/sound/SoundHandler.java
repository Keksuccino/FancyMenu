package de.keksuccino.core.sound;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.FloatControl.Type;

import net.minecraft.client.Minecraft;
import net.minecraft.util.SoundCategory;

public class SoundHandler {
	
	private static Map<String, Clip> sounds = new HashMap<String, Clip>();
	private static boolean init = false;
	
	public static void init() {
		if (!init) {
			
			//Observation thread to check if minecraft's master volume was changed and set the new volume to all registered sounds
			new Thread(new Runnable() {
				@Override
				public void run() {
					float lastMaster = 0.0F;
					while (true) {
						try {
							float currentMaster = Minecraft.getInstance().gameSettings.getSoundLevel(SoundCategory.MASTER);
							if (lastMaster != currentMaster) {
								SoundHandler.updateVolume();
							}
							lastMaster = currentMaster;

							Thread.sleep(100);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}).start();
			
			init = true;
		}
	}
	
	public static void registerSound(String key, String path) {
		if (!sounds.containsKey(key)) {
			try {
				Clip c = AudioSystem.getClip();
				BufferedInputStream s = new BufferedInputStream(new FileInputStream(new File(path)));
				AudioInputStream inputStream = AudioSystem.getAudioInputStream(s);
				c.open(inputStream);
				
				sounds.put(key, c);
				setVolume(key, getMinecraftMasterVolume());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void unregisterSound(String key) {
		if (sounds.containsKey(key)) {
			sounds.get(key).stop();
			sounds.get(key).close();
			sounds.remove(key);
		}
	}
	
	public static void playSound(String key) {
		if (sounds.containsKey(key)) {
			sounds.get(key).start();
		}
	}
	
	public static void stopSound(String key) {
		if (sounds.containsKey(key)) {
			sounds.get(key).stop();
		}
	}
	
	public static void resetSound(String key) {
		if (sounds.containsKey(key)) {
			sounds.get(key).setMicrosecondPosition(0);
		}
	}
	
	public static boolean soundExists(String key) {
		return sounds.containsKey(key);
	}
	
	public static void setLooped(String key, boolean looped) {
		if (sounds.containsKey(key)) {
			Clip c = sounds.get(key);
			if (looped) {
				c.setLoopPoints(0, -1);
				c.loop(-1);
			} else {
				c.loop(0);
			}
		}
	}
	
	public static boolean isPlaying(String key) {
		return (sounds.containsKey(key) && sounds.get(key).isRunning());
	}
	
	private static void updateVolume() {
		for (String s : sounds.keySet()) {
			setVolume(s, getMinecraftMasterVolume());
		}
	}
	
	private static void setVolume(String key, int percentage) {
		FloatControl f = ((FloatControl)sounds.get(key).getControl(Type.MASTER_GAIN));
		int gain = (int) ((int) f.getMinimum() + ((f.getMaximum() - f.getMinimum()) / 100 * percentage));
		f.setValue(gain);
	}
	
	private static int getMinecraftMasterVolume() {
		return (int)(Minecraft.getInstance().gameSettings.getSoundLevel(SoundCategory.MASTER) * 100);
	}

}
