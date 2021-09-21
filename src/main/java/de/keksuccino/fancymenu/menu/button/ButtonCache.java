package de.keksuccino.fancymenu.menu.button;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.events.GuiInitCompletedEvent;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.konkrete.gui.screens.SimpleLoadingScreen;
import de.keksuccino.konkrete.localization.LocaleUtils;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.reflection.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.VideoSettingsScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class ButtonCache {

	private static Map<Long, ButtonData> buttons = new HashMap<Long, ButtonData>();
	private static Map<Long, Widget> replaced = new HashMap<Long, Widget>();
	private static Screen current = null;
	private static boolean cached = false;
	private static boolean caching = false;
	//TODO übernehmen
	private static Map<String, Widget> customButtons = new HashMap<String, Widget>();

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
				MinecraftForge.EVENT_BUS.post(new ButtonCachedEvent(s, new ArrayList<ButtonData>(), false));
				return;
			}

			//Don't cache video settings if Optifine is active
			if ((s != null) && (s instanceof VideoSettingsScreen) && FancyMenu.isOptifineLoaded()) {
				replaced.clear();
				buttons.clear();
				MinecraftForge.EVENT_BUS.post(new ButtonCachedEvent(s, new ArrayList<ButtonData>(), false));
				return;
			}

			//Don't cache screen if from one of slimeknights mods
			if (s.getClass().getName().startsWith("slimeknights.")) {
				replaced.clear();
				buttons.clear();
				MinecraftForge.EVENT_BUS.post(new ButtonCachedEvent(s, new ArrayList<ButtonData>(), false));
				return;
			}

			//Don't cache screen if from Optifine
			if (s.getClass().getName().startsWith("net.optifine")) {
				replaced.clear();
				buttons.clear();
				MinecraftForge.EVENT_BUS.post(new ButtonCachedEvent(s, new ArrayList<ButtonData>(), false));
				return;
			}

			if ((s == Minecraft.getInstance().currentScreen)) {
				updateButtons(s);
			}

			MinecraftForge.EVENT_BUS.post(new ButtonCachedEvent(s, getButtons(), true));
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
		if ((s != null) && (s instanceof VideoSettingsScreen) && FancyMenu.isOptifineLoaded()) {
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
		List<ButtonData> btns = cacheButtons(s, Minecraft.getInstance().getMainWindow().getScaledWidth(), Minecraft.getInstance().getMainWindow().getScaledHeight());

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
						System.out.println("At: X=" + button.x + " Y=" + button.y + "!");
						System.out.println("Labels: " + button.label + ", " + buttons.get(id.getId()).label);
						System.out.println("");
						System.out.println("If one or both of these buttons are added by a mod, please contact the developer(s) to fix this!");
						System.out.println("FancyMenu cannot customize overlapping buttons!");
						System.out.println("#####################################################");
						System.out.println("");
					}
				}
				i++;
			}
		}
	}

	private static List<ButtonData> cacheButtons(Screen s, int screenWidth, int screenHeight) {
		caching = true;
		List<ButtonData> buttonlist = new ArrayList<ButtonData>();
		try {
			//Resetting the button list
			Field f0 = ReflectionHelper.findField(Screen.class, "field_230710_m_");
			f0.set(s, new ArrayList<Widget>());

			//Setting all important values for the GuiScreen to be able to initialize itself
			//itemRenderer field
			Field f1 = ReflectionHelper.findField(Screen.class, "field_230707_j_");
			f1.set(s, Minecraft.getInstance().getItemRenderer());
			//font field
			Field f2 = ReflectionHelper.findField(Screen.class, "field_230712_o_");
			f2.set(s, Minecraft.getInstance().fontRenderer);

			//init
			s.init(Minecraft.getInstance(), screenWidth, screenHeight);

			//Reflecting the buttons list field to cache all buttons of the menu
			Field f = ReflectionHelper.findField(Screen.class, "field_230710_m_");

			for (Widget w : (List<Widget>) f.get(s)) {
				String idRaw = w.x + "" + w.y;
				long id = 0;
				if (MathUtils.isLong(idRaw)) {
					id = Long.parseLong(idRaw);
				}
				buttonlist.add(new ButtonData(w, id, LocaleUtils.getKeyForString(w.getMessage().getString()), s));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		caching = false;
		return buttonlist;
	}

	public static void replaceButton(long id, Widget w) {
		ButtonData d = getButtonForId(id);
		Widget ori = null;
		if ((d != null) && (current != null)) {
			try {
				Field f = ObfuscationReflectionHelper.findField(Screen.class, "field_230710_m_");
				List<Widget> l = (List<Widget>) f.get(current);
				List<Widget> l2 = new ArrayList<Widget>();

				for (Widget b : l) {
					if (b == d.getButton()) {
						l2.add(w);
						ori = b;
					} else {
						l2.add(b);
					}
				}

				f.set(current, l2);
				if (ori != null) {
					replaced.put(d.getId(), ori);
				}
				d.replaceButton(w);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void replaceButton(String key, Widget w) {
		ButtonData d = getButtonForKey(key);
		if (d != null) {
			replaceButton(d.getId(), w);
		}
	}

	public static void cacheFrom(Screen s, int screenWidth, int screenHeight) {
		updateButtons(s);
	}

	/**
	 * Returns the button id or -1 if the button has no cached id.
	 */
	public static long getIdForButton(Widget w) {
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
	public static String getNameForButton(Widget w) {
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
	public static String getKeyForButton(Widget w) {
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

	//TODO übernehmen
	public static void clearCustomButtonCache() {
		customButtons.clear();
	}

	//TODO übernehmen
	public static void cacheCustomButton(String id, Widget w) {
		customButtons.put(id, w);
	}

	//TODO übernehmen
	public static Widget getCustomButton(String id) {
		return customButtons.get(id);
	}

}
