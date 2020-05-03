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
import de.keksuccino.fancymenu.menu.fancy.helper.CustomizationButton;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutCreatorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class ButtonCache {
	
	private static Map<Integer, ButtonData> buttons = new HashMap<Integer, ButtonData>();
	private static GuiScreen current = null;
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
	
	private static void cache(GuiScreen s) {
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
			
			if ((s == Minecraft.getMinecraft().currentScreen) && cache) {
				buttons.clear();
				
				int i = 1;
				for (GuiButton w : sortButtons(getGuiButtons(s))) {
					buttons.put(i, new ButtonData(w, i, LocaleUtils.getKeyForString(w.displayString), s));
					i++;
				}
			}

			MinecraftForge.EVENT_BUS.post(new ButtonCachedEvent(s, getButtons(), cache));
		}
	}
	
	public static void addButton(GuiButton w) {
		List<GuiButton> l = new ArrayList<GuiButton>();
		for (ButtonData d : getButtons()) {
			l.add(d.getButton());
		}
		l.add(w);
		
		buttons.clear();
		int i = 1;
		for (GuiButton wi : sortButtons(l)) {
			buttons.put(i, new ButtonData(wi, i, LocaleUtils.getKeyForString(wi.displayString), current));
			i++;
		}
	}
	
	private static List<GuiButton> getGuiButtons(GuiScreen s) {
		List<GuiButton> l = new ArrayList<GuiButton>();
		try {
			Field f = ReflectionHelper.findField(GuiScreen.class, "buttonList", "field_146292_n");
			l = (List<GuiButton>) f.get(s);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return l;
	}
	
	public static void cacheFrom(GuiScreen s, int screenWidth, int screenHeight) {
		List<GuiButton> l = new ArrayList<GuiButton>();
		try {
			//Resetting the button list
			Field f0 = net.minecraftforge.fml.relauncher.ReflectionHelper.findField(GuiScreen.class, "buttonList", "field_146292_n");
			f0.set(s, new ArrayList<GuiButton>());
			
			//Setting all important values for the GuiScreen to be able to initialize itself
			s.mc = Minecraft.getMinecraft();
			s.width = screenWidth;
			s.height = screenHeight;
			//itemRender field
			Field f1 = net.minecraftforge.fml.relauncher.ReflectionHelper.findField(GuiScreen.class, "itemRender", "field_146296_j");
			f1.set(s, Minecraft.getMinecraft().getRenderItem());
			//fontRenderer field
			Field f2 = net.minecraftforge.fml.relauncher.ReflectionHelper.findField(GuiScreen.class, "fontRenderer", "field_146289_q");
			f2.set(s, Minecraft.getMinecraft().fontRenderer);
			
			MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.InitGuiEvent.Pre(s, l));
			
			s.initGui();
			
			//Reflecting the buttons list field to cache all buttons of the menu
			Field f = net.minecraftforge.fml.relauncher.ReflectionHelper.findField(GuiScreen.class, "buttonList", "field_146292_n");
			l.addAll((List<GuiButton>) f.get(s));
			
			MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.InitGuiEvent.Post(s, l));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (!l.isEmpty()) {
			buttons.clear();
			int i = 1;
			for (GuiButton w : sortButtons(l)) {
				buttons.put(i, new ButtonData(w, i, LocaleUtils.getKeyForString(w.displayString), s));
				i++;
			}
		}
	}
	
	/**
	 * Will sort all buttons by its height and width.
	 */
	private static List<GuiButton> sortButtons(List<GuiButton> widgets) {
		List<GuiButton> l = new ArrayList<GuiButton>();
		Map<Integer, List<GuiButton>> m = new HashMap<Integer, List<GuiButton>>(); 
		
		for (GuiButton w : widgets) {
			if (CustomizationButton.isCustomizationButton(w)) {
				continue;
			}
			if (m.containsKey(w.y)) {
				m.get(w.y).add(w);
			} else {
				m.put(w.y, new ArrayList<GuiButton>());
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
		
		List<GuiButton> l2;
		for (Integer i : ints) {
			l2 = m.get(i);
			Collections.sort(l2, new Comparator<GuiButton>() {
				@Override
				public int compare(GuiButton o1, GuiButton o2) {
					if (o1.width > o2.width) {
						return 1;
					}
					if (o1.width < o2.width) {
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
	public static int getIdForButton(GuiButton w) {
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
	public static String getNameForButton(GuiButton w) {
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
	public static String getKeyForButton(GuiButton w) {
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
