package de.keksuccino.fancymenu.menu.slideshow;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.helper.MenuReloadedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SlideshowHandler {
	
	private static Map<String, ExternalTextureSlideshowRenderer> slideshows = new HashMap<String, ExternalTextureSlideshowRenderer>();
	
	public static void init() {
		updateSlideshows();
		
		MinecraftForge.EVENT_BUS.register(new SlideshowHandler());
	}
	
	public static void updateSlideshows() {
		File f = FancyMenu.getSlideshowPath();
		
		slideshows.clear();
		for (File f2 : f.listFiles()) {
			if (f2.isDirectory()) {
				File f3 = new File(f2.getPath() + "/properties.txt");
				File f4 = new File(f2.getPath() + "/images");
				if (f3.exists() && f4.exists()) {
					ExternalTextureSlideshowRenderer render = new ExternalTextureSlideshowRenderer(f2.getPath());
					String name = render.getName();
					if (name != null) {
						render.prepareSlideshow();
						slideshows.put(name, render);
					} else {
						System.out.println("############## ERROR [FANCYMENU] ##############");
						System.out.println("Invalid slideshow found: " + f2.getPath());
						System.out.println("###############################################");
					}
				}
			}
		}
	}
	
	public static ExternalTextureSlideshowRenderer getSlideshow(String name) {
		return slideshows.get(name);
	}
	
	public static List<ExternalTextureSlideshowRenderer> getSlideshows() {
		List<ExternalTextureSlideshowRenderer> l = new ArrayList<ExternalTextureSlideshowRenderer>();
		l.addAll(slideshows.values());
		return l;
	}
	
	public static List<String> getSlideshowNames() {
		List<String> l = new ArrayList<String>();
		l.addAll(slideshows.keySet());
		return l;
	}
	
	public static boolean slideshowExists(String name) {
		return slideshows.containsKey(name);
	}
	
	@SubscribeEvent
	public void onMenuReload(MenuReloadedEvent e) {
		updateSlideshows();
	}

}
