package de.keksuccino.fancymenu.menu.button;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.keksuccino.core.gui.screens.SimpleLoadingScreen;
import de.keksuccino.core.locale.LocaleUtils;
import de.keksuccino.core.reflection.ReflectionHelper;
import de.keksuccino.fancymenu.menu.fancy.helper.CustomizationButton;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutCreatorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class ButtonCache {
	
	private static Map<Integer, ButtonData> buttons = new HashMap<Integer, ButtonData>();
	private static Screen current = null;
	private static boolean cached = false;
	
	@SubscribeEvent
	public void updateCache(GuiScreenEvent.InitGuiEvent.Post e) {
		cached = false;
		current = e.getGui();
	}
	
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onBackDrawn(GuiScreenEvent.BackgroundDrawnEvent e) {
		cache(e.getGui());
	}
	
	@SubscribeEvent
	public void onRenderPre(GuiScreenEvent.DrawScreenEvent.Pre e) {
		cache(e.getGui());
	}
	
	private static void cache(Screen s) {
		if (!cached) {
			cached = true;
			
			boolean cache = true;
			//Don't refresh cache if screen is instance of LayoutCreator
			if (s instanceof LayoutCreatorScreen) {
				cache = false;
			}
			//Don't refresh cache if screen is instance of one of FancyMenu's loading screens
			if (s instanceof SimpleLoadingScreen) {
				cache = false;
			}
			
			if ((s == Minecraft.getInstance().currentScreen) && cache) {
				buttons.clear();
				
				int i = 1;
				for (Widget w : sortButtons(getGuiButtons(s))) {
					buttons.put(i, new ButtonData(w, i, LocaleUtils.getKeyForString(w.getMessage().getString()), s));
					i++;
				}
			}

			MinecraftForge.EVENT_BUS.post(new ButtonCachedEvent(s, getButtons(), cache));
		}
	}
	
	public static void addButton(Widget w) {
		List<Widget> l = new ArrayList<Widget>();
		for (ButtonData d : getButtons()) {
			l.add(d.getButton());
		}
		l.add(w);
		
		buttons.clear();
		int i = 1;
		for (Widget wi : sortButtons(l)) {
			buttons.put(i, new ButtonData(wi, i, LocaleUtils.getKeyForString(wi.getMessage().getString()), current));
			i++;
		}
	}
	
	private static List<Widget> getGuiButtons(Screen s) {
		List<Widget> l = new ArrayList<Widget>();
		try {
			Field f = ObfuscationReflectionHelper.findField(Screen.class, "field_230710_m_");
			l = (List<Widget>) f.get(s);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return l;
	}
	
	public static void cacheFrom(Screen s, int screenWidth, int screenHeight) {
		List<Widget> l = new ArrayList<Widget>();
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
			l.addAll((List<Widget>) f.get(s));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		buttons.clear();
		int i = 1;
		for (Widget w : sortButtons(l)) {
			buttons.put(i, new ButtonData(w, i, LocaleUtils.getKeyForString(w.getMessage().getString()), s));
			i++;
		}
	}
	
	/**
	 * Will sort all buttons by its height and width.
	 */
	private static List<Widget> sortButtons(List<Widget> widgets) {
		List<Widget> l = new ArrayList<Widget>();
		Map<Integer, List<Widget>> m = new HashMap<Integer, List<Widget>>(); 
		
		for (Widget w : widgets) {
			if (CustomizationButton.isCustomizationButton(w)) {
				continue;
			}
			if (m.containsKey(w.y)) {
				m.get(w.y).add(w);
			} else {
				m.put(w.y, new ArrayList<Widget>());
				m.get(w.y).add(w);
			}
		}
		
		List<Integer> ints = new ArrayList<Integer>();
		ints.addAll(m.keySet());
		Collections.sort(ints, new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				if (o1 > o2) {
					return 1;
				}
				if (o1 < o2) {
					return -1;
				}
				return 0;
			}
		});
		
		List<Widget> l2;
		for (Integer i : ints) {
			l2 = m.get(i);
			Collections.sort(l2, new Comparator<Widget>() {
				@Override
				public int compare(Widget o1, Widget o2) {
					//func_230998_h_() = getWidth
					if (o1.getWidth() > o2.getWidth()) {
						return 1;
					}
					if (o1.getWidth() < o2.getWidth()) {
						return -1;
					}
					return 0;
				}
			});
			l.addAll(l2);
		}
		
		return l;
	}
	
	/**
	 * Returns the button id or -1 if the button has no cached id.
	 */
	public static int getIdForButton(Widget w) {
		for (Map.Entry<Integer, ButtonData> m : buttons.entrySet()) {
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
		for (Map.Entry<Integer, ButtonData> m : buttons.entrySet()) {
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
		for (Map.Entry<Integer, ButtonData> m : buttons.entrySet()) {
			if (m.getValue().getButton() == w) {
				return m.getValue().getKey();
			}
		}
		return null;
	}
	
	/**
	 * Returns the button for this id or null if no button with this id was found.
	 */
	public static ButtonData getButtonForId(int id) {
		return buttons.get(id);
	}
	
	/**
	 * Returns the button for this key or null if no button with this key was found.
	 */
	public static ButtonData getButtonForKey(String key) {
		for (Map.Entry<Integer, ButtonData> m : buttons.entrySet()) {
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
		for (Map.Entry<Integer, ButtonData> m : buttons.entrySet()) {
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

}
