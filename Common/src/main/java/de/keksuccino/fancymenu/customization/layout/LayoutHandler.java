package de.keksuccino.fancymenu.customization.layout;

import java.io.File;
import java.util.*;

import com.google.common.io.Files;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.audio.SoundRegistry;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.utils.ListUtils;
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.fancymenu.properties.PropertiesSerializer;
import de.keksuccino.fancymenu.properties.PropertyContainerSet;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class LayoutHandler {
	
	private static final List<Layout> ENABLED_LAYOUTS = new ArrayList<>();
	private static final List<Layout> DISABLED_LAYOUTS = new ArrayList<>();

	public static void init() {
		reloadLayouts();
	}

	public static void reloadLayouts() {
		ScreenCustomization.readCustomizableScreensFromFile();
		ENABLED_LAYOUTS.clear();
		DISABLED_LAYOUTS.clear();
		ENABLED_LAYOUTS.addAll(deserializeLayoutFilesInDirectory(FancyMenu.getCustomizationsDirectory()));
		DISABLED_LAYOUTS.addAll(deserializeLayoutFilesInDirectory(new File(FancyMenu.getCustomizationsDirectory().getPath() + "/.disabled")));
	}

	@SuppressWarnings("all")
	public static List<Layout> deserializeLayoutFilesInDirectory(File dir) {
		List<Layout> layouts = new ArrayList<>();
		if (!dir.exists()) {
			dir.mkdirs();
		}
		for (File f : dir.listFiles()) {
			if (f.getPath().toLowerCase().endsWith(".txt")) {
				PropertyContainerSet s = PropertiesSerializer.deserializePropertyContainerSet(f.getAbsolutePath().replace("\\", "/"));
				if (s != null) {
					Layout layout = deserializeLayout(s, f);
					if (layout != null) {
						layouts.add(layout);
					}
				}
			}
		}
		return layouts;
	}

	@Nullable
	public static Layout deserializeLayout(@NotNull PropertyContainerSet serialized, @Nullable File layoutFile) {
		return Layout.deserialize(serialized, layoutFile);
	}

	@NotNull
	public static List<Layout> getEnabledLayouts() {
		return new ArrayList<>(ENABLED_LAYOUTS);
	}

	@NotNull
	public static List<Layout> getDisabledLayouts() {
		return new ArrayList<>(DISABLED_LAYOUTS);
	}

	@NotNull
	public static List<Layout> getAllLayouts() {
		return ListUtils.mergeLists(getEnabledLayouts(), getDisabledLayouts());
	}

	@NotNull
	public static List<Layout> getEnabledLayoutsForMenuIdentifier(@NotNull String menuIdentifier, boolean includeUniversalLayouts) {
		List<Layout> l = new ArrayList<>();
		for (Layout layout : ENABLED_LAYOUTS) {
			if (layout.menuIdentifier.equals(menuIdentifier)) {
				l.add(layout);
			} else if (layout.isUniversalLayout() && includeUniversalLayouts) {
				if (!layout.universalLayoutMenuWhitelist.isEmpty() || !layout.universalLayoutMenuBlacklist.isEmpty()) {
					if (!layout.universalLayoutMenuWhitelist.isEmpty() && layout.universalLayoutMenuWhitelist.contains(menuIdentifier)) {
						l.add(layout);
					} else if (!layout.universalLayoutMenuBlacklist.isEmpty() && !layout.universalLayoutMenuBlacklist.contains(menuIdentifier)) {
						l.add(layout);
					}
				} else {
					l.add(layout);
				}
			}
		}
		return l;
	}

	@NotNull
	public static List<Layout> getDisabledLayoutsForMenuIdentifier(@NotNull String menuIdentifier) {
		List<Layout> l = new ArrayList<>();
		for (Layout layout : DISABLED_LAYOUTS) {
			if (layout.menuIdentifier.equals(menuIdentifier)) {
				l.add(layout);
			}
		}
		return l;
	}

	@NotNull
	public static List<Layout> getAllLayoutsForMenuIdentifier(@NotNull String menuIdentifier, boolean includeUniversalLayouts) {
		return ListUtils.mergeLists(getEnabledLayoutsForMenuIdentifier(menuIdentifier, includeUniversalLayouts), getDisabledLayoutsForMenuIdentifier(menuIdentifier));
	}

	/**
	 * Gets the 8 most recently edited layouts.
	 */
	@NotNull
	public static List<Layout> getRecentlyEditedLayoutsForMenuIdentifier(@NotNull String menuIdentifier, boolean includeUniversalLayouts) {
		List<Layout> all = getAllLayoutsForMenuIdentifier(menuIdentifier, includeUniversalLayouts);
		all.sort(Comparator.comparingLong(value -> value.lastEditedTime));
		Collections.reverse(all);
		all.removeIf(l -> (l.lastEditedTime == -1));
//		if (!menuIdentifier.equals(Layout.UNIVERSAL_LAYOUT_IDENTIFIER)) {
//			all.removeIf(l -> Objects.equals(l.menuIdentifier, Layout.UNIVERSAL_LAYOUT_IDENTIFIER));
//		}
		if (!all.isEmpty()) return all.subList(0, Math.min(8, all.size()));
		return new ArrayList<>();
	}

	public static void enableLayout(File layoutFile) {
		try {
			String name = FileUtils.generateAvailableFilename(FancyMenu.getCustomizationsDirectory().getPath(), Files.getNameWithoutExtension(layoutFile.getPath()), "txt");
			FileUtils.copyFile(layoutFile, new File(FancyMenu.getCustomizationsDirectory().getPath() + "/" + name));
			layoutFile.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
		ScreenCustomization.reloadFancyMenu();
	}

	public static void enableLayout(Layout layout) {
		if (layout.layoutFile != null) {
			enableLayout(layout.layoutFile);
		} else {
			DISABLED_LAYOUTS.remove(layout);
			ENABLED_LAYOUTS.add(layout);
			ScreenCustomization.reloadFancyMenu();
		}
	}

	public static void disableLayout(File layoutFile) {
		try {
			String disPath = FancyMenu.getCustomizationsDirectory().getPath() + "/.disabled";
			String name = FileUtils.generateAvailableFilename(disPath, Files.getNameWithoutExtension(layoutFile.getPath()), "txt");
			FileUtils.copyFile(layoutFile, new File(disPath + "/" + name));
			layoutFile.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
		ScreenCustomization.reloadFancyMenu();
	}

	public static void disableLayout(Layout layout) {
		if (layout.layoutFile != null) {
			disableLayout(layout.layoutFile);
		} else {
			ENABLED_LAYOUTS.remove(layout);
			DISABLED_LAYOUTS.add(layout);
			ScreenCustomization.reloadFancyMenu();
		}
	}

	public static void openLayoutEditor(@NotNull Layout layout, @Nullable Screen layoutTargetScreen) {
		try {
			SoundRegistry.stopSounds();
			SoundRegistry.resetSounds();
			for (IAnimationRenderer r : AnimationHandler.getAnimations()) {
				if (r instanceof AdvancedAnimation) {
					((AdvancedAnimation)r).stopAudio();
					if (((AdvancedAnimation)r).replayIntro()) {
						r.resetAnimation();
					}
				}
			}
			Minecraft.getInstance().setScreen(new LayoutEditorScreen(layoutTargetScreen, layout));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Will save the layout as layout file.
	 *
	 * @param saveTo Full file path with file name + extension.
	 */
	public static boolean saveLayoutToFile(Layout layout, String saveTo) {
		File f = new File(saveTo);
		String s = Files.getFileExtension(saveTo);
		if (!s.equals("")) {
			if (f.exists() && f.isFile()) {
				f.delete();
			}
			PropertyContainerSet set = layout.serialize();
			if (set != null) {
				PropertiesSerializer.serializePropertyContainerSet(set, f.getPath());
				return true;
			}
		}
		return false;
	}

}
