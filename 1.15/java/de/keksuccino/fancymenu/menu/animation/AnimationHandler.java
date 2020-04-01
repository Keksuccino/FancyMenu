package de.keksuccino.fancymenu.menu.animation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.animation.AnimationData.Type;
import de.keksuccino.fancymenu.menu.animation.exceptions.AnimationNotFoundException;
import de.keksuccino.file.FileUtils;
import de.keksuccino.math.MathUtils;
import de.keksuccino.rendering.animation.ExternalTextureAnimationRenderer;
import de.keksuccino.rendering.animation.IAnimationRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.common.MinecraftForge;

public class AnimationHandler {
	
	private static Map<String, AnimationData> animations = new HashMap<String, AnimationData>();
	private static List<String> custom = new ArrayList<String>();
	protected static boolean ready = false;

	public static void registerAnimation(IAnimationRenderer animation, String name, Type type) {
		if (!animations.containsKey(name)) {
			animations.put(name, new AnimationData(animation, name, type));
			if (type == Type.EXTERNAL) {
				custom.add(name);
			}
		} else {
			throw new IllegalArgumentException("[FM AnimationHandler] Animation name '" + name + "' already used!");
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
	
	public static void init() {
		MinecraftForge.EVENT_BUS.register(new AnimationHandlerEvents());
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
			int fps = 1;
			boolean loop = true;
			int width = 20;
			int height = 20;
			int x = 0;
			int y = 0;
			
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
					
				}
				
				if (name != null) {
					String intro = getIntroPath(a.getPath());
					String ani = getAnimationPath(a.getPath());
					if ((intro != null) && (ani != null)) {
						ExternalTextureAnimationRenderer in = new ExternalTextureAnimationRenderer(intro, fps, loop, x, y, width, height);
						ExternalTextureAnimationRenderer an = new ExternalTextureAnimationRenderer(ani, fps, loop, x, y, width, height);
						try {
							registerAnimation(new AdvancedAnimation(in, an), name, Type.EXTERNAL);
							System.out.println("[FM AnimationHandler] Custom animation found and registered: " + name + "");
						} catch (AnimationNotFoundException e) {
							e.printStackTrace();
						}
					} else if (ani != null) {
						ExternalTextureAnimationRenderer an = new ExternalTextureAnimationRenderer(ani, fps, loop, x, y, width, height);
						registerAnimation(an, name, Type.EXTERNAL);
						System.out.println("[FM AnimationHandler] Custom animation found and registered: " + name + "");
					} else {
						System.out.println("[FM AnimationHandler] ### ERROR: This is not a valid animation: " + name);
					}
				}
			}
		}
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
	
	public static void setupAnimations() {
		Screen s = null;
		if (Minecraft.getInstance().currentScreen != null) {
			s = Minecraft.getInstance().currentScreen;
		} else {
			s = new MainMenuScreen();
		}
		AnimationLoadingScreen l = new AnimationLoadingScreen(s, getAnimations().toArray(new IAnimationRenderer[0])) {
			@Override
			public void onFinished() {
				ready = true;
				super.onFinished();
			}
		};
		Minecraft.getInstance().displayGuiScreen(l);
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
	
	public static boolean isReady() {
		return ready;
	}
	
}
