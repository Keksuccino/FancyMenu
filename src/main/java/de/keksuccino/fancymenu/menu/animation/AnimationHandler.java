package de.keksuccino.fancymenu.menu.animation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.animation.AnimationData.Type;
import de.keksuccino.fancymenu.menu.animation.exceptions.AnimationNotFoundException;
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.rendering.animation.ExternalGifAnimationRenderer;
import de.keksuccino.konkrete.rendering.animation.ExternalTextureAnimationRenderer;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;

public class AnimationHandler {
	
	private static Map<String, AnimationData> animations = new HashMap<String, AnimationData>();
	private static List<String> custom = new ArrayList<String>();
	protected static boolean ready = false;

	public static void init() {
		MinecraftForge.EVENT_BUS.register(new AnimationHandlerEvents());
	}
	
	public static void registerAnimation(IAnimationRenderer animation, String name, Type type) {
		if (!animations.containsKey(name)) {
			animations.put(name, new AnimationData(animation, name, type));
			if (type == Type.EXTERNAL) {
				custom.add(name);
			}
		} else {
			System.out.println("######################################");
			System.out.println("[FM AnimationHandler] Animation name '" + name + "' already used!");
			System.out.println("######################################");
		}
	}
	
	public static void unregisterAnimation(IAnimationRenderer animation) {
		AnimationData d = null;
		for (AnimationData a : animations.values()) {
			if (a.animation == animation) {
				d = a;
				break;
			}
		}
		if (d != null) {
			unregisterAnimation(d.name);
		}
	}
	
	public static void unregisterAnimation(String name) {
		if (animationExists(name)) {
			animations.remove(name);
			if (custom.contains(name)) {
				custom.remove(name);
			}
		}
	}
	
	public static void loadCustomAnimations() {
		File f = FancyMenu.getAnimationPath();
		if (!f.exists() || !f.isDirectory()) {
			return;
		}
		
		ready = false;
		clearCustomAnimations();
		
		for (File a : f.listFiles()) {
			String name = null;
			String mainAudio = null;
			String introAudio = null;
			int fps = 0;
			boolean loop = true;
			int width = 0;
			int height = 0;
			int x = 0;
			int y = 0;
			boolean replayIntro = false;
			
			if (a.isDirectory()) {
				File p = new File(a.getAbsolutePath() + "/properties.txt");
				Map<String, String> props = parseProperties(p);
				if (!props.isEmpty()) {
					
					if (props.containsKey("name")) {
						String s = props.get("name");
						if ((s != null) && !s.equals("")) {
							name = s;
						}
					}
					
					if (props.containsKey("fps")) {
						String s = props.get("fps");
						if ((s != null) && !s.equals("") && MathUtils.isInteger(s)) {
							fps = Integer.parseInt(s);
						}
					}
					
					if (props.containsKey("loop")) {
						String s = props.get("loop");
						if ((s != null) && !s.equals("")) {
							if (s.equalsIgnoreCase("false")) {
								loop = false;
							}
						}
					}
					
					if (props.containsKey("width")) {
						String s = props.get("width");
						if ((s != null) && !s.equals("") && MathUtils.isInteger(s)) {
							width = Integer.parseInt(s);
						}
					}
					
					if (props.containsKey("height")) {
						String s = props.get("height");
						if ((s != null) && !s.equals("") && MathUtils.isInteger(s)) {
							height = Integer.parseInt(s);
						}
					}
					
					if (props.containsKey("x")) {
						String s = props.get("x");
						if ((s != null) && !s.equals("") && MathUtils.isInteger(s)) {
							x = Integer.parseInt(s);
						}
					}
					
					if (props.containsKey("y")) {
						String s = props.get("y");
						if ((s != null) && !s.equals("") && MathUtils.isInteger(s)) {
							y = Integer.parseInt(s);
						}
					}
					
					if (props.containsKey("replayintro")) {
						String s = props.get("replayintro");
						if ((s != null) && s.equalsIgnoreCase("true")) {
							replayIntro = true;
						}
					}
					
				}
				
				File audio1 = new File(a.getAbsolutePath() + "/audio/mainaudio.wav");
				if (audio1.exists()) {
					mainAudio = audio1.getPath();
				}
				
				File audio2 = new File(a.getAbsolutePath() + "/audio/introaudio.wav");
				if (audio2.exists()) {
					introAudio = audio2.getPath();
				}
				
				if (name != null) {
					File gifIntro = new File(a.getPath() + "/intro.gif");
					File gifAni = new File(a.getPath() + "/animation.gif");
					IAnimationRenderer in = null;
					IAnimationRenderer an = null;
					
					if (gifAni.exists()) {
						if ((gifIntro.exists()) && (gifAni.exists())) {
							in = new ExternalGifAnimationRenderer(gifIntro.getPath(), loop, x, y, width, height);
							an = new ExternalGifAnimationRenderer(gifAni.getPath(), loop, x, y, width, height);
						} else if (gifAni.exists()) {
							an = new ExternalGifAnimationRenderer(gifAni.getPath(), loop, x, y, width, height);
						}
					} else {
						String intro = getIntroPath(a.getPath());
						String ani = getAnimationPath(a.getPath());
						if ((intro != null) && (ani != null)) {
							in = new ExternalTextureAnimationRenderer(intro, fps, loop, x, y, width, height);
							an = new ExternalTextureAnimationRenderer(ani, fps, loop, x, y, width, height);
						} else if (ani != null) {
							an = new ExternalTextureAnimationRenderer(ani, fps, loop, x, y, width, height);
						}
					}
					
					try {
						if ((in != null) && (an != null)) {
							registerAnimation(new AdvancedAnimation(in, an, introAudio, mainAudio, replayIntro), name, Type.EXTERNAL);
							System.out.println("[FM AnimationHandler] Custom animation found and registered: " + name + "");
						} else if (an != null) {
							registerAnimation(new AdvancedAnimation(null, an, introAudio, mainAudio, false), name, Type.EXTERNAL);
							System.out.println("[FM AnimationHandler] Custom animation found and registered: " + name + "");
						} else {
							System.out.println("[FM AnimationHandler] ### ERROR: This is not a valid animation: " + name);
						}
					} catch (AnimationNotFoundException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public static List<String> getCustomAnimationNames() {
		List<String> l = new ArrayList<String>();
		l.addAll(custom);
		return l;
	}
	
	private static String getIntroPath(String path) {
		File f = new File(path + "/intro");
		if (f.exists() && f.isDirectory()) {
			return f.getPath();
		}
		return null;
	}
	
	private static String getAnimationPath(String path) {
		File f = new File(path + "/animation");
		if (f.exists() && f.isDirectory()) {
			return f.getPath();
		}
		return null;
	}
	
	private static void clearCustomAnimations() {
		for (String s : custom) {
			if (animations.containsKey(s)) {
				animations.remove(s);
			}
		}
	}
	
	private static Map<String, String> parseProperties(File prop) {
		Map<String, String> m = new HashMap<String, String>();
		if (prop.exists() && prop.isFile()) {
			for (String s : FileUtils.getFileLines(prop)) {
				if (s.contains("=")) {
					String name = s.split("[=]", 2)[0].replace(" ", "");
					String value = s.split("[=]", 2)[1].replace(" ", "");
					m.put(name, value);
				}
			}
		}
		return m;
	}
	
	public static void setupAnimations(GuiOpenEvent e) {
		if (!animations.isEmpty()) {
			GuiScreen s = null;
			if (e.getGui() != null) {
				s = e.getGui();
			} else {
				s = new GuiMainMenu();
			}
			AnimationLoadingScreen l = new AnimationLoadingScreen(s, getAnimations().toArray(new IAnimationRenderer[0])) {
				@Override
				public void onFinished() {
					ready = true;
					super.onFinished();
				}
			};
			e.setGui(l);
		} else {
			ready = true;
		}
	}
	
	public static boolean animationExists(String name) {
		return animations.containsKey(name);
	}
	
	public static List<IAnimationRenderer> getAnimations() {
		List<IAnimationRenderer> renderers = new ArrayList<IAnimationRenderer>();
		for (Map.Entry<String, AnimationData> m : animations.entrySet()) {
			renderers.add(m.getValue().animation);
		}
		return renderers;
	}
	
	public static IAnimationRenderer getAnimation(String name) {
		if (animationExists(name)) {
			return animations.get(name).animation;
		}
		return null;
	}
	
	public static void resetAnimations() {
		for (AnimationData d : animations.values()) {
			d.animation.resetAnimation();
		}
	}

	public static void resetAnimationSounds() {
		for (AnimationData d : animations.values()) {
			if (d.animation instanceof AdvancedAnimation) {
				((AdvancedAnimation)d.animation).resetAudio();
			}
		}
	}

	public static void stopAnimationSounds() {
		for (AnimationData d : animations.values()) {
			if (d.animation instanceof AdvancedAnimation) {
				((AdvancedAnimation)d.animation).stopAudio();
			}
		}
	}
	
	public static boolean isReady() {
		return ready;
	}
	
}
