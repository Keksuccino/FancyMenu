package de.keksuccino.fancymenu.menu.button;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import de.keksuccino.core.gui.content.AdvancedButton;
import de.keksuccino.core.gui.screens.SimpleLoadingScreen;
import de.keksuccino.core.input.MouseInput;
import de.keksuccino.core.locale.LocaleUtils;
import de.keksuccino.core.math.MathUtils;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.mainwindow.MainWindowHandler;
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
	
	private static Map<Long, ButtonData> buttons = new HashMap<Long, ButtonData>();
	private static Map<Long, GuiButton> replaced = new HashMap<Long, GuiButton>();
	private static GuiScreen current = null;
	private static boolean cached = false;
	private static boolean caching = false;
	
	@SubscribeEvent
	public void updateCache(GuiScreenEvent.InitGuiEvent.Post e) {
		if (!caching) {
			cached = false;
			current = e.getGui();
		}
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
				updateButtons(s, getGuiButtons(s));
			}

			MinecraftForge.EVENT_BUS.post(new ButtonCachedEvent(s, getButtons(), cache));
		}
	}
	
	/**
	 * The widget list (buttonlist) is only used by the old button id system.
	 */
	private static void updateButtons(GuiScreen s, @Nullable List<GuiButton> buttonlist) {
		replaced.clear();
		buttons.clear();
		
		if (useLegacyButtonIds()) {
			//Calculate button ids with the old, unstable id system
			if (buttonlist != null) {
				long i = 1;
				for (GuiButton w : sortButtons(buttonlist)) {
					buttons.put(i, new ButtonData(w, i, LocaleUtils.getKeyForString(w.displayString), s));
					i++;
				}
			} else {
				System.out.println("#### ERROR [FANCYMENU]: Buttonlist is NULL and ID calculation set to legacy!");
			}
		} else {
			//Use the new id calculation system
			List<ButtonData> ids = cacheButtons(s, 1000, 1000);
			List<ButtonData> btns = cacheButtons(s, MainWindowHandler.getScaledWidth(), MainWindowHandler.getScaledHeight());
			
			if (btns.size() == ids.size()) {
				int i = 0;
				for (ButtonData id : ids) {
					ButtonData button = btns.get(i);
					if (!buttons.containsKey(id.getId())) {
						buttons.put(id.getId(), new ButtonData(button.getButton(), id.getId(), LocaleUtils.getKeyForString(button.getButton().displayString), s));
					} else {
						System.out.println("");
						System.out.println("#### WARNING [FANCYMENU]: Overlapping buttons found!");
						System.out.println("At: X=" + button.x + " Y=" + button.y + "!");
						System.out.println("Labels: " + button.label + ", " + buttons.get(id.getId()).label);
						System.out.println("");
						System.out.println("If one or booth of these buttons are added by a mod, please contact the developer(s) to fix this!");
						System.out.println("FancyMenu cannot customize overlapping buttons!");
						System.out.println("#####################################################");
						System.out.println("");
					}
					i++;
				}
			}
		}
	}
	
	private static List<ButtonData> cacheButtons(GuiScreen s, int screenWidth, int screenHeight) {
		caching = true;
		List<ButtonData> buttonlist = new ArrayList<ButtonData>();
		try {
			//Resetting the button list
			Field f0 = net.minecraftforge.fml.relauncher.ReflectionHelper.findField(GuiScreen.class, "buttonList", "field_146292_n");
			f0.set(s, new ArrayList<GuiButton>());

			//Setting all important values for the GuiScreen to allow it to initialize itself
			s.mc = Minecraft.getMinecraft();
			s.width = screenWidth;
			s.height = screenHeight;
			//itemRender field
			Field f1 = net.minecraftforge.fml.relauncher.ReflectionHelper.findField(GuiScreen.class, "itemRender", "field_146296_j");
			f1.set(s, Minecraft.getMinecraft().getRenderItem());
			//fontRenderer field
			Field f2 = net.minecraftforge.fml.relauncher.ReflectionHelper.findField(GuiScreen.class, "fontRenderer", "field_146289_q");
			f2.set(s, Minecraft.getMinecraft().fontRenderer);

			MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.InitGuiEvent.Pre(s, (List<GuiButton>) f0.get(s)));

			s.initGui();

			//Reflecting the buttons list field to cache all buttons of the menu
			Field f = net.minecraftforge.fml.relauncher.ReflectionHelper.findField(GuiScreen.class, "buttonList", "field_146292_n");

			for (GuiButton w : (List<GuiButton>) f.get(s)) {
				String idRaw = w.x + "" + w.y;
				long id = 0;
				if (MathUtils.isLong(idRaw)) {
					id = Long.parseLong(idRaw);
				}
				buttonlist.add(new ButtonData(w, id, LocaleUtils.getKeyForString(w.displayString), s));
			}
			
			MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.InitGuiEvent.Post(s, (List<GuiButton>) f0.get(s)));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		caching = false;
		return buttonlist;
	}
	
	public static void replaceButton(long id, GuiButton w) {
		ButtonData d = getButtonForId(id);
		GuiButton ori = null;
		if ((d != null) && (current != null)) {
			try {
				Field f = net.minecraftforge.fml.relauncher.ReflectionHelper.findField(GuiScreen.class, "buttonList", "field_146292_n");
				List<GuiButton> l = (List<GuiButton>) f.get(current);
				List<GuiButton> l2 = new ArrayList<GuiButton>();
				
				for (GuiButton b : l) {
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

	public static void replaceButton(String key, GuiButton w) {
		ButtonData d = getButtonForKey(key);
		if (d != null) {
			replaceButton(d.getId(), w);
		}
	}

	public static AdvancedButton convertToAdvancedButton(long id, boolean handleClick) {
		ButtonData d = getButtonForId(id);
		if ((d != null) && !(d.getButton() instanceof AdvancedButton)) {
			AdvancedButton b = new AdvancedButton(d.getButton().x, d.getButton().y, d.getButton().width, d.getButton().height, d.getButton().displayString, handleClick, (press) -> {
				GuiButton w = replaced.get(d.getId());
				if (w != null) {
					//TODO experimental
					clickButton(w);
				}
			});
			replaceButton(id, b);
			return b;
		}
		return null;
	}

	public static AdvancedButton convertToAdvancedButton(String key, boolean handleClick) {
		ButtonData d = getButtonForKey(key);
		if (d != null) {
			return convertToAdvancedButton(d.getId(), handleClick);
		}
		return null;
	}
	
	public static void addButton(GuiButton w) {
		List<GuiButton> l = new ArrayList<GuiButton>();
		for (ButtonData d : getButtons()) {
			l.add(d.getButton());
		}
		l.add(w);

		if (current != null) {
			updateButtons(current, l);
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
		if (useLegacyButtonIds()) {
			List<GuiButton> l = new ArrayList<GuiButton>();
			try {
				//Resetting the button list
				Field f0 = net.minecraftforge.fml.relauncher.ReflectionHelper.findField(GuiScreen.class, "buttonList", "field_146292_n");
				f0.set(s, new ArrayList<GuiButton>());

				//Setting all important values for the GuiScreen to allow it to initialize itself
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
			
			updateButtons(s, l);
		} else {
			updateButtons(s, null);
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
	public static long getIdForButton(GuiButton w) {
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
	public static String getNameForButton(GuiButton w) {
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
	public static String getKeyForButton(GuiButton w) {
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
	
	public static boolean useLegacyButtonIds() {
		return FancyMenu.config.getOrDefault("legacybuttonids", false);
	}
	
	private static void clickButton(GuiButton b) {
		b.mousePressed(Minecraft.getMinecraft(), MouseInput.getMouseX(), MouseInput.getMouseY());
		
		try {
			Method m = ReflectionHelper.findMethod(GuiScreen.class, "actionPerformed", "func_146284_a", GuiButton.class);
			m.invoke(Minecraft.getMinecraft().currentScreen, b);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
