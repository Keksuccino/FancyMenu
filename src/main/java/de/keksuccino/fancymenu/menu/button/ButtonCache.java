package de.keksuccino.fancymenu.menu.button;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.GuiInitCompletedEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent;
import de.keksuccino.konkrete.gui.screens.SimpleLoadingScreen;
import de.keksuccino.konkrete.localization.LocaleUtils;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.reflection.ReflectionHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.gui.widget.ClickableWidget;

public class ButtonCache {

	private static Map<Long, ButtonData> buttons = new HashMap<Long, ButtonData>();
	private static Map<Long, ClickableWidget> replaced = new HashMap<Long, ClickableWidget>();
	private static Screen current = null;
	private static boolean cached = false;
	private static boolean caching = false;
	private static Map<String, ClickableWidget> customButtons = new HashMap<String, ClickableWidget>();
	
	@SubscribeEvent
	public void updateCache(GuiScreenEvent.InitGuiEvent.Post e) {
		if (!caching) {
			cached = false;
			current = e.getGui();
		}
	}
	
	@SubscribeEvent
	public void onInitCompleted(GuiInitCompletedEvent e) {
		cache(e.getGui());
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
			
			if (!MenuCustomization.isMenuCustomizable(s)) {
				replaced.clear();
				buttons.clear();
				Konkrete.getEventHandler().callEventsFor(new ButtonCachedEvent(s, new ArrayList<ButtonData>(), false));
				return;
			}
			
			//Don't cache video settings if Optifine is active
			if ((s != null) && (s instanceof VideoOptionsScreen) && FancyMenu.isOptifineLoaded()) {
				replaced.clear();
				buttons.clear();
				Konkrete.getEventHandler().callEventsFor(new ButtonCachedEvent(s, new ArrayList<ButtonData>(), false));
				return;
			}

			//Don't cache screen if from one of slimeknights mods
			if (s.getClass().getName().startsWith("slimeknights.")) {
				replaced.clear();
				buttons.clear();
				Konkrete.getEventHandler().callEventsFor(new ButtonCachedEvent(s, new ArrayList<ButtonData>(), false));
				return;
			}

			//Don't cache screen if from Optifine
			if (s.getClass().getName().startsWith("net.optifine")) {
				replaced.clear();
				buttons.clear();
				Konkrete.getEventHandler().callEventsFor(new ButtonCachedEvent(s, new ArrayList<ButtonData>(), false));
				return;
			}
			
			if ((s == MinecraftClient.getInstance().currentScreen)) {
				updateButtons(s);
			}

			Konkrete.getEventHandler().callEventsFor(new ButtonCachedEvent(s, getButtons(), true));
		}
	}

	/**
	 * The widget list (buttonlist) is only used by the old button id system.
	 */
	private static void updateButtons(Screen s) {
		replaced.clear();
		buttons.clear();

		if (!MenuCustomization.isMenuCustomizable(s)) {
			return;
		}
		
		//Don't update video settings buttons if Optifine is active
		if ((s != null) && (s instanceof VideoOptionsScreen) && FancyMenu.isOptifineLoaded()) {
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

		List<ButtonData> ids = cacheButtons(s, 1000, 1000);
		List<ButtonData> btns = cacheButtons(s, MinecraftClient.getInstance().getWindow().getScaledWidth(), MinecraftClient.getInstance().getWindow().getScaledHeight());

		if (btns.size() == ids.size()) {
			int i = 0;
			for (ButtonData id : ids) {
				ButtonData button = btns.get(i);
				if (!buttons.containsKey(id.getId())) {
					buttons.put(id.getId(), new ButtonData(button.getButton(), id.getId(), LocaleUtils.getKeyForString(button.getButton().getMessage().getString()), s));
				} else {
					if (FancyMenu.config.getOrDefault("showdebugwarnings", true)) {
						System.out.println("");
						System.out.println("## WARNING [FANCYMENU]: Overlapping buttons found! ##");
						System.out.println("At: X=" + button.x + " Y=" + button.y);
						System.out.println("Labels: " + button.label + ", " + buttons.get(id.getId()).label);
						System.out.println("");
						System.out.println("FancyMenu found overlapping buttons and wasn't able to generate working IDs for them to make them customizable!");
						System.out.println("Please report this to the mod author of FancyMenu and give informations about what buttons caused it.");
						System.out.println("#####################################################");
						System.out.println("");
					}
				}
				i++;
			}
		}
	}

	public static List<ButtonData> cacheButtons(Screen s, int screenWidth, int screenHeight) {
		caching = true;
		List<ButtonData> buttonlist = new ArrayList<ButtonData>();
		List<Long> ids = new ArrayList<Long>();
		try {
			//Resetting the button list
			Field f0 = ReflectionHelper.findField(Screen.class, "drawables", "field_33816");
			f0.set(s, new ArrayList<Drawable>());
			
			//Setting all important values for the GuiScreen to be able to initialize itself
			Field f1 = ReflectionHelper.findField(Screen.class, "itemRenderer", "field_22788");
			f1.set(s, MinecraftClient.getInstance().getItemRenderer());
			
			Field f2 = ReflectionHelper.findField(Screen.class, "textRenderer", "field_22793");
			f2.set(s, MinecraftClient.getInstance().textRenderer);

			s.init(MinecraftClient.getInstance(), screenWidth, screenHeight);
			
			//Reflecting the buttons list field to cache all buttons of the menu
			Field f = ReflectionHelper.findField(Screen.class, "drawables", "field_33816");

			for (Drawable d : (List<Drawable>) f.get(s)) {
				if (d instanceof ClickableWidget) {
					ClickableWidget w = (ClickableWidget) d;
					String idRaw = w.x + "" + w.y;
					long id = 0;
					if (MathUtils.isLong(idRaw)) {
						id = getAvailableIdFromBaseId(Long.parseLong(idRaw), ids);
					}
					ids.add(id);
					buttonlist.add(new ButtonData(w, id, LocaleUtils.getKeyForString(w.getMessage().getString()), s));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		caching = false;
		return buttonlist;
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
	 * Returns the button id or -1 if the button has no cached id.
	 */
	public static long getIdForButton(ClickableWidget w) {
		for (Map.Entry<Long, ButtonData> m : buttons.entrySet()) {
			if (m.getValue().getButton() == w) {
				return m.getValue().getId();
			}
		}
		return -1;
	}
	
	/**
	 * Returns the button name or null if the button has no cached name.
	 */
	public static String getNameForButton(ClickableWidget w) {
		for (Map.Entry<Long, ButtonData> m : buttons.entrySet()) {
			if (m.getValue().getButton() == w) {
				return m.getValue().label;
			}
		}
		return null;
	}
	
	/**
	 * Returns the button key or null if the button has no cached key.
	 */
	public static String getKeyForButton(ClickableWidget w) {
		for (Map.Entry<Long, ButtonData> m : buttons.entrySet()) {
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
		return buttons.get(id);
	}
	
	/**
	 * Returns the button for this key or null if no button with this key was found.
	 */
	public static ButtonData getButtonForKey(String key) {
		for (Map.Entry<Long, ButtonData> m : buttons.entrySet()) {
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
		for (Map.Entry<Long, ButtonData> m : buttons.entrySet()) {
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
		List<ButtonData> b = new ArrayList<ButtonData>();
		b.addAll(buttons.values());
		return b;
	}

	public static boolean isCaching() {
		return caching;
	}

	public static void clearCustomButtonCache() {
		customButtons.clear();
	}

	public static void cacheCustomButton(String id, ClickableWidget w) {
		customButtons.put(id, w);
	}

	public static ClickableWidget getCustomButton(String id) {
		return customButtons.get(id);
	}

}
