package de.keksuccino.fancymenu.menu.button;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.keksuccino.fancymenu.menu.fancy.helper.CustomizationButton;
import de.keksuccino.locale.LocaleUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ButtonCache {
	
	private static Map<Integer, ButtonData> buttons = new HashMap<Integer, ButtonData>();
	
	@SubscribeEvent(priority = EventPriority.HIGH)
	public void updateCache(GuiScreenEvent.InitGuiEvent.Post e) {
		if (e.getGui() == Minecraft.getInstance().currentScreen) {
			buttons.clear();
			
			int i = 1;
			for (GuiButton w : sortButtons(e.getButtonList())) {
				buttons.put(i, new ButtonData(w, i, w.displayString, LocaleUtils.getKeyForString(w.displayString), e.getGui()));
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
				return m.getValue().getName();
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
			if (m.getValue().getName().equals(name)) {
				return m.getValue();
			}
		}
		return null;
	}

}
