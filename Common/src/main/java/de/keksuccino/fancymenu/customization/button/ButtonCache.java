package de.keksuccino.fancymenu.customization.button;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.button.identification.ButtonIdentificator;
import de.keksuccino.fancymenu.customization.layouteditor.LayoutEditorScreen;
import de.keksuccino.fancymenu.event.events.ButtonCacheUpdatedEvent;
import de.keksuccino.fancymenu.event.acara.EventHandler;
import de.keksuccino.fancymenu.event.acara.EventListener;
import de.keksuccino.fancymenu.event.events.screen.InitOrResizeScreenCompletedEvent;
import de.keksuccino.fancymenu.event.events.screen.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinScreen;
import de.keksuccino.konkrete.gui.screens.SimpleLoadingScreen;
import de.keksuccino.konkrete.localization.LocaleUtils;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.VideoSettingsScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ButtonCache {

	private static final Logger LOGGER = LogManager.getLogger();

	private static final Map<Long, ButtonData> BUTTONS = new HashMap<>();
	private static boolean cached = false;
	private static boolean caching = false;

	public static void init() {
		EventHandler.INSTANCE.registerListenersOf(new ButtonCache());
	}

	@EventListener
	public void updateCache(InitOrResizeScreenEvent.Post e) {
		if (!caching) {
			cached = false;
		}
	}

	@EventListener
	public void onInitCompleted(InitOrResizeScreenCompletedEvent e) {
		cache(e.getScreen());
	}

	private static void cache(Screen s) {
		if (!cached) {
			cached = true;

			if (s instanceof LayoutEditorScreen) {
				return;
			}
			if (s instanceof SimpleLoadingScreen) {
				return;
			}

			if (!ScreenCustomization.isCustomizationEnabledForScreen(s)) {
				BUTTONS.clear();
				EventHandler.INSTANCE.postEvent(new ButtonCacheUpdatedEvent(s, new ArrayList<>(), false));
				return;
			}

			//Don't cache video settings if OptiFine is active
			if ((s instanceof VideoSettingsScreen) && FancyMenu.isOptifineLoaded()) {
				BUTTONS.clear();
				EventHandler.INSTANCE.postEvent(new ButtonCacheUpdatedEvent(s, new ArrayList<>(), false));
				return;
			}

			//Don't cache screen if from one of slimeknights mods
			if (s.getClass().getName().startsWith("slimeknights.")) {
				BUTTONS.clear();
				EventHandler.INSTANCE.postEvent(new ButtonCacheUpdatedEvent(s, new ArrayList<>(), false));
				return;
			}

			//Don't cache screen if from Optifine
			if (s.getClass().getName().startsWith("net.optifine")) {
				BUTTONS.clear();
				EventHandler.INSTANCE.postEvent(new ButtonCacheUpdatedEvent(s, new ArrayList<>(), false));
				return;
			}

			if ((s == Minecraft.getInstance().screen)) {
				updateButtons(s);
			}

			EventHandler.INSTANCE.postEvent(new ButtonCacheUpdatedEvent(s, getButtons(), true));
		}
	}

	private static void updateButtons(Screen s) {
		BUTTONS.clear();

		if (!ScreenCustomization.isCustomizationEnabledForScreen(s)) {
			return;
		}

		//Don't update video settings buttons if Optifine is active
		if ((s instanceof VideoSettingsScreen) && FancyMenu.isOptifineLoaded()) {
			return;
		}

		//Don't update buttons if screen is from one of slimeknights mods
		if (s.getClass().getName().startsWith("slimeknights.")) {
			return;
		}

		//Don't update buttons if from Optifine
		if (s.getClass().getName().startsWith("net.optifine")) {
			return;
		}
		//Use the new id calculation system
		List<ButtonData> ids = cacheButtons(s, 1000, 1000);
		List<ButtonData> btns = cacheButtons(s, Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight());

		if (btns.size() == ids.size()) {
			int i = 0;
			for (ButtonData id : ids) {
				ButtonData button = btns.get(i);
				if (!BUTTONS.containsKey(id.getId())) {
					BUTTONS.put(id.getId(), new ButtonData(button.getButton(), id.getId(), LocaleUtils.getKeyForString(button.getButton().getMessage().getString()), s));
				}
				i++;
			}
		}

		List<String> compIds = new ArrayList<>();
		for (ButtonData d : BUTTONS.values()) {
			ButtonIdentificator.setCompatibilityIdentifierToData(d);
			if (compIds.contains(d.compatibilityId)) {
				d.compatibilityId = null;
			} else {
				compIds.add(d.compatibilityId);
			}
		}

	}

	public static List<ButtonData> cacheButtons(Screen s, int screenWidth, int screenHeight) {
		caching = true;
		List<ButtonData> buttonDataList = new ArrayList<>();
		List<Long> ids = new ArrayList<>();
		try {

			//Set all important variables and init screen
			((IMixinScreen)s).setItemRendererFancyMenu(Minecraft.getInstance().getItemRenderer());
			((IMixinScreen)s).setFontFancyMenu(Minecraft.getInstance().font);

			//TODO resize instead of init
			s.resize(Minecraft.getInstance(), screenWidth, screenHeight);

			//Reflecting the buttons list field to cache all buttons of the menu
			List<AbstractWidget> widgets = new ArrayList<>();
			for (Renderable r : ((IMixinScreen)s).getRenderablesFancyMenu()) {
				if (r instanceof AbstractWidget) {
					widgets.add((AbstractWidget)r);
				}
			}
			for (AbstractWidget w : widgets) {
				String idRaw = w.x + "" + w.y;
				long id = 0;
				if (MathUtils.isLong(idRaw)) {
					id = getAvailableIdFromBaseId(Long.parseLong(idRaw), ids);
				}
				ids.add(id);
				buttonDataList.add(new ButtonData(w, id, LocaleUtils.getKeyForString(w.getMessage().getString()), s));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		caching = false;
		return buttonDataList;
	}

	protected static Long getAvailableIdFromBaseId(long baseId, List<Long> ids) {
		if (ids.contains(baseId)) {
			String newId = baseId + "1";
			if (MathUtils.isLong(newId)) {
				return getAvailableIdFromBaseId(Long.parseLong(newId), ids);
			}
		}
		return baseId;
	}

	public static void cacheFrom(Screen s, int screenWidth, int screenHeight) {
		updateButtons(s);
	}

	/**
	 * Returns the button key or null if the button has no cached key.
	 */
	public static String getKeyForButton(AbstractWidget w) {
		for (Map.Entry<Long, ButtonData> m : BUTTONS.entrySet()) {
			if (m.getValue().getButton() == w) {
				return m.getValue().getKey();
			}
		}
		return null;
	}

	/**
	 * Returns the button for this id or null if no button with this id was found.
	 */
	public static ButtonData getButtonForId(long id) {
		return BUTTONS.get(id);
	}

	/**
	 * Returns the button for this ID or null if no button with this ID was found.
	 */
	public static ButtonData getButtonForCompatibilityId(String id) {
		for (ButtonData d : BUTTONS.values()) {
			if (d.getCompatibilityId() != null) {
				if (d.getCompatibilityId().equals(id)) {
					return d;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the button for this key or null if no button with this key was found.
	 */
	public static ButtonData getButtonForKey(String key) {
		for (Map.Entry<Long, ButtonData> m : BUTTONS.entrySet()) {
			if (m.getValue().getKey().equalsIgnoreCase(key)) {
				return m.getValue();
			}
		}
		return null;
	}

	/**
	 * Returns the button for this name or null if no button with this name was found.
	 */
	public static ButtonData getButtonForName(String name) {
		for (Map.Entry<Long, ButtonData> m : BUTTONS.entrySet()) {
			if (m.getValue().label.equals(name)) {
				return m.getValue();
			}
		}
		return null;
	}

	/**
	 * Returns all currently cached buttons as {@link ButtonData}.
	 */
	public static List<ButtonData> getButtons() {
		return new ArrayList<>(BUTTONS.values());
	}

	public static boolean isCaching() {
		return caching;
	}

}
