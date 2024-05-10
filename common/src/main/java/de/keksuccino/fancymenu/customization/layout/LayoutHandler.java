package de.keksuccino.fancymenu.customization.layout;

import java.io.File;
import java.util.*;
import com.google.common.io.Files;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.file.FilenameComparator;
import de.keksuccino.fancymenu.util.Legacy;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.properties.PropertiesParser;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("all")
public class LayoutHandler {

	private static final Logger LOGGER = LogManager.getLogger();

	public static final File LAYOUT_DIR = FileUtils.createDirectory(new File(FancyMenu.MOD_DIR, "/customization"));
	public static final File ASSETS_DIR = FileUtils.createDirectory(new File(FancyMenu.MOD_DIR, "/assets"));

	private static final List<Layout> LAYOUTS = new ArrayList<>();
	private static final Map<String, UniversalLayoutInclusionRule> UNIVERSAL_LAYOUT_INCLUSION_RULES = new HashMap<>();

	public static void init() {

		convertLegacyDisabledLayouts();
		reloadLayouts();

		//Convert legacy layouts to new v3 format
		for (Layout l : LAYOUTS) {
			if (l.legacyLayout) {
				LOGGER.info("[FANCYMENU] Converting legacy (FMv2) layout '" + l.getLayoutName() + "' to new format..");
				if (!l.saveToFileIfPossible()) {
					LOGGER.error("[FANCYMENU] Failed to convert legacy layout '" + l.getLayoutName() + "' to new format!");
				}
			}
		}

	}

	public static void reloadLayouts() {

		ScreenCustomization.readCustomizableScreensFromFile();

		List<Layout> layouts = deserializeLayoutFilesInDirectory(LAYOUT_DIR);

		LAYOUTS.clear();
		LAYOUTS.addAll(layouts);

	}

	@NotNull
	public static List<Layout> deserializeLayoutFilesInDirectory(File directory) {
		List<Layout> layouts = new ArrayList<>();
		if (!directory.exists()) {
			directory.mkdirs();
		}
		File[] filesArray = directory.listFiles();
		if (filesArray != null) {
			for (File f : filesArray) {
				if (f.getPath().toLowerCase().endsWith(".txt")) {
					PropertyContainerSet s = PropertiesParser.deserializeSetFromFile(f.getAbsolutePath().replace("\\", "/"));
					if (s != null) {
						Layout layout = deserializeLayout(s, f);
						if (layout != null) {
							layouts.add(layout);
						}
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
		List<Layout> enabled = new ArrayList<>();
		LAYOUTS.forEach(layout -> { if (layout.isEnabled()) enabled.add(layout); });
		return enabled;
	}

	@NotNull
	public static List<Layout> getDisabledLayouts() {
		List<Layout> disabled = new ArrayList<>();
		LAYOUTS.forEach(layout -> { if (!layout.isEnabled()) disabled.add(layout); });
		return disabled;
	}

	@NotNull
	public static List<Layout> getAllLayouts() {
		return new ArrayList<>(LAYOUTS);
	}

	@NotNull
	public static List<Layout> getEnabledLayoutsForScreenIdentifier(@NotNull String screenIdentifier, boolean includeUniversalLayouts) {
		for (UniversalLayoutInclusionRule inclusionRule : UNIVERSAL_LAYOUT_INCLUSION_RULES.values()) {
			if (!inclusionRule.includeUniversalLayouts(screenIdentifier)) {
				includeUniversalLayouts = false;
				break;
			}
		}
		List<Layout> layouts = new ArrayList<>();
		for (Layout layout : getEnabledLayouts()) {
			if (ScreenIdentifierHandler.equalIdentifiers(screenIdentifier, layout.screenIdentifier)) {
				layouts.add(layout);
			} else if (layout.isUniversalLayout() && includeUniversalLayouts) {
				if (!layout.universalLayoutMenuWhitelist.isEmpty() || !layout.universalLayoutMenuBlacklist.isEmpty()) {
					if (!layout.universalLayoutMenuWhitelist.isEmpty() && layout.universalLayoutMenuWhitelist.contains(screenIdentifier)) {
						layouts.add(layout);
					} else if (!layout.universalLayoutMenuBlacklist.isEmpty() && !layout.universalLayoutMenuBlacklist.contains(screenIdentifier)) {
						layouts.add(layout);
					}
				} else {
					layouts.add(layout);
				}
			}
		}
		//Sort layouts by their index
		layouts.sort(Comparator.comparingInt(value -> value.layoutIndex));
		//Sort layouts to load universal layouts BEFORE normal ones
		if (includeUniversalLayouts) {
			List<Layout> normal = new ArrayList<>();
			List<Layout> universal = new ArrayList<>();
			for (Layout layout : layouts) {
				if (layout.isUniversalLayout()) {
					universal.add(layout);
				} else {
					normal.add(layout);
				}
			}
			layouts.clear();
			layouts.addAll(universal);
			layouts.addAll(normal);
		}
		return layouts;
	}

	@NotNull
	public static List<Layout> getDisabledLayoutsForScreenIdentifier(@NotNull String screenIdentifier) {
		List<Layout> l = new ArrayList<>();
		for (Layout layout : getDisabledLayouts()) {
			if (ScreenIdentifierHandler.equalIdentifiers(screenIdentifier, layout.screenIdentifier)) {
				l.add(layout);
			}
		}
		return l;
	}

	@NotNull
	public static List<Layout> getAllLayoutsForScreenIdentifier(@NotNull String screenIdentifier, boolean includeUniversalLayouts) {
		return ListUtils.mergeLists(getEnabledLayoutsForScreenIdentifier(screenIdentifier, includeUniversalLayouts), getDisabledLayoutsForScreenIdentifier(screenIdentifier));
	}

	@Nullable
	public static Layout getLayout(String name) {
		for (Layout l : LAYOUTS) {
			if (l.getLayoutName().equals(name)) return l;
		}
		return null;
	}

	@NotNull
	public static List<Layout> sortLayoutListByLastEdited(@NotNull List<Layout> layouts, boolean removeNeverEdited) {
		layouts.sort(Comparator.comparingLong(value -> value.lastEditedTime));
		Collections.reverse(layouts);
		if (removeNeverEdited) layouts.removeIf(l -> (l.lastEditedTime == -1));
		return layouts;
	}

	@NotNull
	public static List<Layout> sortLayoutListByLastEdited(@NotNull List<Layout> layouts, boolean removeNeverEdited, int maxLayouts) {
		sortLayoutListByLastEdited(layouts, removeNeverEdited);
		if (!layouts.isEmpty()) {
			List<Layout> temp = new ArrayList<>(layouts.subList(0, Math.min(maxLayouts, layouts.size())));
			layouts.clear();
			layouts.addAll(temp);
		}
		return layouts;
	}

	@NotNull
	public static List<Layout> sortLayoutListByName(@NotNull List<Layout> layouts) {
		FilenameComparator comp = new FilenameComparator();
		layouts.sort((o1, o2) -> comp.compare(o1.getLayoutName(), o2.getLayoutName()));
		return layouts;
	}

	@NotNull
	public static List<Layout> sortLayoutListByStatus(@NotNull List<Layout> layouts, boolean disabledFirst) {
		sortLayoutListByName(layouts);
		List<Layout> enabled = new ArrayList<>();
		List<Layout> disabled = new ArrayList<>();
		layouts.forEach(layout -> {
			if (layout.isEnabled()) {
				enabled.add(layout);
			} else {
				disabled.add(layout);
			}
		});
		layouts.clear();
		if (disabledFirst) {
			layouts.addAll(disabled);
			layouts.addAll(enabled);
		} else {
			layouts.addAll(enabled);
			layouts.addAll(disabled);
		}
		return layouts;
	}

	public static void deleteLayout(@NotNull Layout layout, boolean reInitCurrentScreen) {
		try {
			if (layout.layoutFile != null) {
				layout.layoutFile.delete();
			}
			LAYOUTS.remove(layout);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		if (reInitCurrentScreen) ScreenCustomization.reInitCurrentScreen();
	}

	public static void addLayout(@NotNull Layout layout, boolean saveToFile) {
		if (!LAYOUTS.contains(layout)) {
			LAYOUTS.add(layout);
			if (saveToFile) layout.saveToFileIfPossible();
		}
	}

	//TODO übernehmen
	public static void openLayoutEditor(@NotNull Layout layout, @Nullable Screen layoutTargetScreen) {
		try {
			for (IAnimationRenderer r : AnimationHandler.getAnimations()) {
				if (r instanceof AdvancedAnimation) {
					((AdvancedAnimation)r).stopAudio();
					if (((AdvancedAnimation)r).replayIntro()) {
						r.resetAnimation();
					}
				}
			}
			GenericMessageScreen msgScreen = new GenericMessageScreen(Component.literal("Opening editor.."));
			msgScreen.init(Minecraft.getInstance(), Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight());
			Minecraft.getInstance().screen = msgScreen;
			Minecraft.getInstance().setScreen(new LayoutEditorScreen(layoutTargetScreen, layout).setAsCurrentInstance());
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
		if (layout.screenIdentifier == null) return false;
		File f = new File(saveTo);
		if (f.isFile()) {
			f.delete();
		}
		PropertyContainerSet set = layout.serialize();
		PropertiesParser.serializeSetToFile(set, f.getPath());
		return true;
	}

	@Legacy("This basically copies all layouts from the old '.disabled' directory to the main directory and sets them to disabled.")
	private static void convertLegacyDisabledLayouts() {
		File disabledDir = new File(LAYOUT_DIR.getPath() + "/.disabled");
		if (disabledDir.isDirectory()) {
			List<Layout> legacyDisabled = deserializeLayoutFilesInDirectory(disabledDir);
			for (Layout l : legacyDisabled) {
				try {
					if (l.layoutFile != null) {
						String name = FileUtils.generateAvailableFilename(LAYOUT_DIR.getPath(), Files.getNameWithoutExtension(l.layoutFile.getPath()), "txt");
						File newFile = new File(LAYOUT_DIR.getPath() + "/" + name);
						FileUtils.copyFile(l.layoutFile, newFile);
						l.layoutFile.delete();
						l.layoutFile = newFile;
						l.enabled = false;
						l.saveToFileIfPossible();
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	/**
	 * Registers a new {@link UniversalLayoutInclusionRule}. These rules can be used to control if universal layouts should get applied to a specific {@link Screen} type.
	 * @param rule
	 * @return The generated unique identifier of the newly registered rule. Use the identifier to unregister the rule if needed.
	 */
	@NotNull
	public static String registerUniversalLayoutInclusionRule(@NotNull UniversalLayoutInclusionRule rule) {
		String id = ScreenCustomization.generateUniqueIdentifier();
		UNIVERSAL_LAYOUT_INCLUSION_RULES.put(id, rule);
		return id;
	}

	/**
	 * Unregisters a {@link UniversalLayoutInclusionRule} that got registered via {@link LayoutHandler#registerUniversalLayoutInclusionRule(UniversalLayoutInclusionRule)}.
	 */
	public static void unregisterUniversalLayoutInclusionRule(@NotNull String identifier) {
		UNIVERSAL_LAYOUT_INCLUSION_RULES.remove(identifier);
	}

	@FunctionalInterface
	public interface UniversalLayoutInclusionRule {
		boolean includeUniversalLayouts(@NotNull String screenIdentifier);
	}

}
