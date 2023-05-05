package de.keksuccino.fancymenu.customization.layout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.google.common.io.Files;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
import de.keksuccino.fancymenu.customization.layouteditor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.layouteditor.PreloadedLayoutEditorScreen;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.properties.PropertiesSerializer;
import de.keksuccino.konkrete.properties.PropertiesSet;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class LayoutHandler {
	
	private static List<PropertiesSet> enabledLayouts = new ArrayList<>();
	private static List<PropertiesSet> disabledLayouts = new ArrayList<>();

	public static void init() {
		reloadLayouts();
	}

	public static void reloadLayouts() {
		ScreenCustomization.readCustomizableScreensFromFile();
		enabledLayouts = deserializeLayoutFilesInDir(FancyMenu.getCustomizationsDirectory());
		disabledLayouts = deserializeLayoutFilesInDir(new File(FancyMenu.getCustomizationsDirectory().getPath() + "/.disabled"));
	}

	public static List<PropertiesSet> deserializeLayoutFilesInDir(File dir) {
		List<PropertiesSet> props = new ArrayList<>();
		if (!dir.exists()) {
			dir.mkdirs();
		}
		for (File f2 : dir.listFiles()) {
			if (f2.getPath().toLowerCase().endsWith(".txt")) {
				PropertiesSet s = PropertiesSerializer.getProperties(f2.getAbsolutePath().replace("\\", "/"));
				if ((s != null) && s.getPropertiesType().equalsIgnoreCase("menu")) {
					List<PropertiesSection> l = s.getPropertiesOfType("customization-meta");
					if (l.isEmpty()) {
						l = s.getPropertiesOfType("type-meta");
					}
					if (!l.isEmpty()) {
						String s2 = l.get(0).getEntryValue("identifier");
						String s3 = l.get(0).getEntryValue("requiredmods");
						String s4 = l.get(0).getEntryValue("minimumfmversion");
						String s5 = l.get(0).getEntryValue("maximumfmversion");
						String s6 = l.get(0).getEntryValue("minimummcversion");
						String s7 = l.get(0).getEntryValue("maximummcversion");
						
						if (s2 == null) {
							continue;
						}
						if (!isVersionCompatible(s4, s5, FancyMenu.VERSION)) {
							continue;
						}
						if (!isVersionCompatible(s6, s7, FancyMenu.getMinecraftVersion())) {
							continue;
						}
						if (!allRequiredModsLoaded(s3)) {
							continue;
						}

						l.get(0).removeEntry("identifier");
						l.get(0).addEntry("identifier", ScreenCustomization.findValidMenuIdentifierFor(s2));
						
						l.get(0).addEntry("path", f2.getPath());
						props.add(s);
					}
				}
			}
		}
		return props;
	}
	
	private static String fillUpToLength(String s, String fillWith, int length) {
		String out = s;
		int add = length - s.length();
		for (int i = 1; i <= add; i++) {
			out += fillWith;
		}
		return out;
	}
	
	private static boolean isVersionCompatible(String minimum, String maximum, String version) {
		if (version == null) {
			return true;
		}
		String versionRaw = fillUpToLength(StringUtils.replaceAllExceptOf(version, "", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9"), "0", 9);

		if (MathUtils.isInteger(versionRaw)) {
			int ver = Integer.parseInt(versionRaw);
			if (minimum != null) {
				String minShort = StringUtils.replaceAllExceptOf(minimum, "", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
				if ((minShort.length() > 0) && MathUtils.isInteger(minShort)) {
					String minRaw = fillUpToLength(minShort, "0", 9);
					int min = Integer.parseInt(minRaw);
					if (ver < min) {
						return false;
					}
				}
			}
			if (maximum != null) {
				String maxShort = StringUtils.replaceAllExceptOf(maximum, "", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
				if ((maxShort.length() > 0) && MathUtils.isInteger(maxShort)) {
					String maxRaw = fillUpToLength(maxShort, "0", 9);
					int max = Integer.parseInt(maxRaw);
					return ver <= max;
				}
			}
		}
		return true;
	}
	
	private static boolean allRequiredModsLoaded(String requiredMods) {
		if ((requiredMods == null) || (requiredMods.replace(" ", "").length() == 0)) {
			return true;
		}
		List<String> mods = new ArrayList<>();
		if (requiredMods.contains(",")) {
			for (String s : requiredMods.replace(" ", "").split("[,]")) {
				if (s.length() > 0) {
					mods.add(s);
				}
			}
		} else {
			mods.add(requiredMods.replace(" ", ""));
		}
		for (String s : mods) {
			if (s.equals("optifine")) {
				if (!Konkrete.isOptifineLoaded) {
					return false;
				}
			} else {
				if (!Services.PLATFORM.isModLoaded(s)) {
					return false;
				}
			}
		}
		return true;
	}
	
	public static List<PropertiesSet> getEnabledLayouts() {
		return enabledLayouts;
	}

	public static List<PropertiesSet> getDisabledLayouts() {
		return disabledLayouts;
	}
	
	public static List<PropertiesSet> getEnabledLayoutsForMenuIdentifier(String identifier) {
		List<PropertiesSet> l = new ArrayList<>();
		for (PropertiesSet s : getEnabledLayouts()) {
			List<PropertiesSection> l2 = s.getPropertiesOfType("customization-meta");
			if (l2.isEmpty()) {
				l2 = s.getPropertiesOfType("type-meta");
			}
			if (l2.isEmpty()) {
				continue;
			}
			String s2 = l2.get(0).getEntryValue("identifier");
			if (s2 != null) {
				if (s2.equalsIgnoreCase(identifier)) {
					l.add(s);
				} else if (s2.equals("%fancymenu:universal_layout%")) {
					String whitelistRaw = l2.get(0).getEntryValue("universal_layout_whitelist");
					String blacklistRaw = l2.get(0).getEntryValue("universal_layout_blacklist");
					List<String> whitelist = new ArrayList<>();
					List<String> blacklist = new ArrayList<>();
					if ((whitelistRaw != null) && whitelistRaw.contains(";")) {
						for (String s3 : whitelistRaw.split(";")) {
							if (s3.length() > 0) {
								whitelist.add(ScreenCustomization.findValidMenuIdentifierFor(s3));
							}
						}
					}
					if ((blacklistRaw != null) && blacklistRaw.contains(";")) {
						for (String s3 : blacklistRaw.split(";")) {
							if (s3.length() > 0) {
								blacklist.add(ScreenCustomization.findValidMenuIdentifierFor(s3));
							}
						}
					}
					if (!whitelist.isEmpty() || !blacklist.isEmpty()) {
						if (!whitelist.isEmpty() && whitelist.contains(identifier)) {
							l.add(s);
						} else if (!blacklist.isEmpty() && !blacklist.contains(identifier)) {
							l.add(s);
						}
					} else {
						l.add(s);
					}
				}
			}
		}
		return l;
	}

	public static List<PropertiesSet> getDisabledLayoutsForMenuIdentifier(String identifier) {
		List<PropertiesSet> l = new ArrayList<>();
		for (PropertiesSet s : getDisabledLayouts()) {
			List<PropertiesSection> l2 = s.getPropertiesOfType("customization-meta");
			if (l2.isEmpty()) {
				l2 = s.getPropertiesOfType("type-meta");
			}
			if (l2.isEmpty()) {
				continue;
			}
			String s2 = l2.get(0).getEntryValue("identifier");
			if (s2.equalsIgnoreCase(identifier)) {
				l.add(s);
			}
		}
		return l;
	}

	public static List<LayoutProperties> getAsLayoutProperties(List<PropertiesSet> propsList) {
		List<LayoutProperties> l = new ArrayList<>();
		propsList.forEach((props) -> l.add(new LayoutProperties(props)));
		return l;
	}

	public static void enableLayout(String path) {
		try {
			File f = new File(path);
			String name = FileUtils.generateAvailableFilename(FancyMenu.getCustomizationsDirectory().getPath(), Files.getNameWithoutExtension(path), "txt");
			FileUtils.copyFile(f, new File(FancyMenu.getCustomizationsDirectory().getPath() + "/" + name));
			f.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
		ScreenCustomization.reloadFancyMenu();
	}

	public static void enableLayout(LayoutHandler.LayoutProperties layout) {
		if (layout.path != null) {
			enableLayout(layout.path);
		}
	}

	public static void disableLayout(String path) {
		try {
			File f = new File(path);
			String disPath = FancyMenu.getCustomizationsDirectory().getPath() + "/.disabled";
			String name = FileUtils.generateAvailableFilename(disPath, Files.getNameWithoutExtension(path), "txt");
			FileUtils.copyFile(f, new File(disPath + "/" + name));
			f.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
		ScreenCustomization.reloadFancyMenu();
	}

	public static void disableLayout(LayoutHandler.LayoutProperties layout) {
		if (layout.path != null) {
			disableLayout(layout.path);
		}
	}

	public static void editLayout(Screen current, File layout) {
		try {
			if ((layout != null) && (current != null) && (layout.exists()) && (layout.isFile())) {
				List<PropertiesSet> l = new ArrayList<>();
				PropertiesSet set = PropertiesSerializer.getProperties(layout.getPath());
				l.add(set);
				List<PropertiesSection> meta = set.getPropertiesOfType("customization-meta");
				if (meta.isEmpty()) {
					meta = set.getPropertiesOfType("type-meta");
				}
				if (!meta.isEmpty()) {
					meta.get(0).addEntry("path", layout.getPath());
					LayoutEditorScreen.isActive = true;
					Minecraft.getInstance().setScreen(new PreloadedLayoutEditorScreen(current, l));
					ScreenCustomization.stopSounds();
					ScreenCustomization.resetSounds();
					for (IAnimationRenderer r : AnimationHandler.getAnimations()) {
						if (r instanceof AdvancedAnimation) {
							((AdvancedAnimation)r).stopAudio();
							if (((AdvancedAnimation)r).replayIntro()) {
								r.resetAnimation();
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
	 * @param saveTo Full file path with file name + extension.
	 */
	public static boolean saveLayout(PropertiesSet layout, String saveTo) {
		File f = new File(saveTo);
		String s = Files.getFileExtension(saveTo);
		if (!s.equals("")) {
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
	 * @param saveTo Full file path with file name + extension.
	 */
	public static boolean saveLayout(List<PropertiesSection> layout, String saveTo) {
		PropertiesSet props = new PropertiesSet("menu");
		for (PropertiesSection sec : layout) {
			props.addProperties(sec);
		}
		return saveLayout(props, saveTo);
	}

	public static class LayoutProperties {

		public PropertiesSet properties;
		public String menuIdentifier;
		public String path;

		public LayoutProperties(PropertiesSet props) {

			List<PropertiesSection> l = props.getPropertiesOfType("customization-meta");
			if (!l.isEmpty()) {
				PropertiesSection meta = l.get(0);
				this.path = meta.getEntryValue("path");
				this.menuIdentifier = meta.getEntryValue("identifier");
			}

		}

	}

}
