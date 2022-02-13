package de.keksuccino.fancymenu.menu.fancy.helper;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.common.io.Files;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiBase;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiLoader;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.PreloadedLayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerRegistry;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.EventPriority;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.properties.PropertiesSerializer;
import de.keksuccino.konkrete.properties.PropertiesSet;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public class CustomizationHelper {

	public static List<Runnable> mainThreadTasks = new ArrayList<>();
	
	public static void init() {
		
		Konkrete.getEventHandler().registerEventsFrom(new CustomizationHelper());

		CustomizationHelperUI.init();
		
	}

	@SubscribeEvent(priority = EventPriority.LOW)
	public void onRenderPost(GuiScreenEvent.DrawScreenEvent.Post e) {

		List<Runnable> runs = new ArrayList<>();
		runs.addAll(CustomizationHelper.mainThreadTasks);
		for (Runnable r : runs) {
			try {
				r.run();
				CustomizationHelper.mainThreadTasks.remove(r);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		if (!e.getGui().getClass().getName().startsWith("de.keksuccino.spiffyhud.")) {
			if (!e.getGui().getClass().getName().startsWith("de.keksuccino.drippyloadingscreen.")) {

				CustomizationHelperUI.render(e.getMatrixStack(), e.getGui());

			}
		}

	}
	
	public static void updateUI() {
		CustomizationHelperUI.updateUI();
	}

	public static void reloadSystemAndMenu() {
		
		FancyMenu.updateConfig();
		
		MenuCustomization.resetSounds();
		MenuCustomization.stopSounds();
		AnimationHandler.resetAnimations();
		AnimationHandler.resetAnimationSounds();
		AnimationHandler.stopAnimationSounds();
		MenuCustomization.reload();
		MenuHandlerRegistry.setActiveHandler(null);
		CustomGuiLoader.loadCustomGuis();
		if (!FancyMenu.config.getOrDefault("showcustomizationbuttons", true)) {
			CustomizationHelperUI.showButtonInfo = false;
			CustomizationHelperUI.showMenuInfo = false;
		}

		Konkrete.getEventHandler().callEventsFor(new MenuReloadedEvent(MinecraftClient.getInstance().currentScreen));
		
		try {
			MinecraftClient.getInstance().setScreen(MinecraftClient.getInstance().currentScreen);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public static void openFile(File f) {
		try {
			String url = f.toURI().toURL().toString();
			String s = System.getProperty("os.name").toLowerCase(Locale.ROOT);
			URL u = new URL(url);
			if (!MinecraftClient.IS_SYSTEM_MAC) {
				if (s.contains("win")) {
					Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
				} else {
					if (u.getProtocol().equals("file")) {
						url = url.replace("file:", "file://");
					}
					Runtime.getRuntime().exec(new String[]{"xdg-open", url});
				}
			} else {
				Runtime.getRuntime().exec(new String[]{"open", url});
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void editLayout(Screen current, File layout) {
		
		try {
			
			if ((layout != null) && (current != null) && (layout.exists()) && (layout.isFile())) {
				
				List<PropertiesSet> l = new ArrayList<PropertiesSet>();
				PropertiesSet set = PropertiesSerializer.getProperties(layout.getPath());
				
				l.add(set);
				
				List<PropertiesSection> meta = set.getPropertiesOfType("customization-meta");
				if (meta.isEmpty()) {
					meta = set.getPropertiesOfType("type-meta");
				}
				
				if (!meta.isEmpty()) {
					
					meta.get(0).addEntry("path", layout.getPath());
					
					LayoutEditorScreen.isActive = true;
					MinecraftClient.getInstance().setScreen(new PreloadedLayoutEditorScreen(current, l));
					MenuCustomization.stopSounds();
					MenuCustomization.resetSounds();
					for (IAnimationRenderer r : AnimationHandler.getAnimations()) {
						if (r instanceof AdvancedAnimation) {
							((AdvancedAnimation)r).stopAudio();
							if (((AdvancedAnimation)r).replayIntro()) {
								((AdvancedAnimation)r).resetAnimation();
							}
						}
					}
					
				}
				
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Will save the layout as layout file.
	 * 
	 * @param to Full file path with file name + extension.
	 */
	public static boolean saveLayoutTo(PropertiesSet layout, String to) {
		
		File f = new File(to);
		String s = Files.getFileExtension(to);
		if ((s != null) && !s.equals("")) {
			
			if (f.exists() && f.isFile()) {
				f.delete();
			}
			
			PropertiesSerializer.writeProperties(layout, f.getPath());
			
			return true;
			
		}
		
		return false;
		
	}
	
	/**
	 * Will save the layout as layout file.
	 * 
	 * @param to Full file path with file name + extension.
	 */
	public static boolean saveLayoutTo(List<PropertiesSection> layout, String to) {
		
		PropertiesSet props = new PropertiesSet("menu");
		for (PropertiesSection sec : layout) {
			props.addProperties(sec);
		}
		
		return saveLayoutTo(props, to);
		
	}
	
	public static boolean isScreenOverridden(Screen current) {
		if ((current != null) && (current instanceof CustomGuiBase) && (((CustomGuiBase)current).getOverriddenScreen() != null)) {
			return true;
		}
		return false;
	}

	public static void runTaskInMainThread(Runnable task) {
		mainThreadTasks.add(task);
	}

}
