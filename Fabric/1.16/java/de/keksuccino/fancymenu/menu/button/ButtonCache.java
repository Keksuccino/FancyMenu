package de.keksuccino.fancymenu.menu.button;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.helper.CustomizationButton;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutCreatorScreen;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.videosettings.VideoSettingsMenuHandler;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.EventPriority;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.SimpleLoadingScreen;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.localization.LocaleUtils;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.reflection.ReflectionHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.VideoOptionsScreen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.util.math.MatrixStack;

public class ButtonCache {

	private static Map<Long, ButtonData> buttons = new HashMap<Long, ButtonData>();
	private static Map<Long, AbstractButtonWidget> replaced = new HashMap<Long, AbstractButtonWidget>();
	private static Screen current = null;
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
	
	private static void cache(Screen s) {
		if (!cached) {
			cached = true;
			
			boolean cache = true;

			if (s instanceof LayoutCreatorScreen) {
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
			if ((s != null) && (s instanceof VideoOptionsScreen) && !VideoSettingsMenuHandler.isScrollable()) {
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
			
			if ((s == MinecraftClient.getInstance().currentScreen) && cache) {
				updateButtons(s, getGuiButtons(s));
			}

			Konkrete.getEventHandler().callEventsFor(new ButtonCachedEvent(s, getButtons(), cache));
		}
	}

	/**
	 * The widget list (buttonlist) is only used by the old button id system.
	 */
	private static void updateButtons(Screen s, @Nullable List<AbstractButtonWidget> buttonlist) {
		replaced.clear();
		buttons.clear();

		if (!MenuCustomization.isMenuCustomizable(s)) {
			return;
		}
		
		//Don't update video settings buttons if Optifine is active
		if ((s != null) && (s instanceof VideoOptionsScreen) && !VideoSettingsMenuHandler.isScrollable()) {
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
		
		if (useLegacyButtonIds()) {
			//Calculate button ids with the old, unstable id system
			if (buttonlist != null) {
				long i = 1;
				for (AbstractButtonWidget w : sortButtons(buttonlist)) {
					buttons.put(i, new ButtonData(w, i, LocaleUtils.getKeyForString(w.getMessage().getString()), s));
					i++;
				}
			} else {
				System.out.println("#### ERROR [FANCYMENU]: Buttonlist is NULL and ID calculation set to legacy!");
			}
		} else {
			//Use the new id calculation system
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
	}

	private static List<ButtonData> cacheButtons(Screen s, int screenWidth, int screenHeight) {
		caching = true;
		List<ButtonData> buttonlist = new ArrayList<ButtonData>();
		try {
			//Resetting the button list
			Field f0 = ReflectionHelper.findField(Screen.class, "buttons", "field_22791");
			f0.set(s, new ArrayList<AbstractButtonWidget>());
			
			//Setting all important values for the GuiScreen to be able to initialize itself
			//itemRenderer field
			Field f1 = ReflectionHelper.findField(Screen.class, "itemRenderer", "field_22788");
			f1.set(s, MinecraftClient.getInstance().getItemRenderer());
			//font field
			Field f2 = ReflectionHelper.findField(Screen.class, "textRenderer", "field_22793");
			f2.set(s, MinecraftClient.getInstance().textRenderer);

			//init
			s.init(MinecraftClient.getInstance(), screenWidth, screenHeight);
			
			//Reflecting the buttons list field to cache all buttons of the menu
			Field f = ReflectionHelper.findField(Screen.class, "buttons", "field_22791");

			for (AbstractButtonWidget w : (List<AbstractButtonWidget>) f.get(s)) {
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

	public static void replaceButton(long id, AbstractButtonWidget w) {
		ButtonData d = getButtonForId(id);
		AbstractButtonWidget ori = null;
		if ((d != null) && (current != null)) {
			try {
				Field f = ReflectionHelper.findField(Screen.class, "buttons", "field_22791");
				List<AbstractButtonWidget> l = (List<AbstractButtonWidget>) f.get(current);
				List<AbstractButtonWidget> l2 = new ArrayList<AbstractButtonWidget>();
				
				for (AbstractButtonWidget b : l) {
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

	public static void replaceButton(String key, AbstractButtonWidget w) {
		ButtonData d = getButtonForKey(key);
		if (d != null) {
			replaceButton(d.getId(), w);
		}
	}

	public static AdvancedButton convertToAdvancedButton(long id, boolean handleClick) {
		ButtonData d = getButtonForId(id);
		if ((d != null) && !(d.getButton() instanceof AdvancedButton)) {
			AdvancedButton b = new AdvancedButton(d.getButton().x, d.getButton().y, d.getButton().getWidth(), d.getButton().getHeight(), d.getButton().getMessage().getString(), handleClick, (press) -> {
				AbstractButtonWidget w = replaced.get(d.getId());
				if (w != null) {
					w.onClick(MouseInput.getMouseX(), MouseInput.getMouseY());
				}
			}) {
				AbstractButtonWidget w;
				@Override
				public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
					if (w != null) {
						this.active = w.active;
						this.visible = w.visible;
					} else {
						w = replaced.get(d.getId());
					}
					super.render(matrices, mouseX, mouseY, delta);
				}
			};
			b.active = d.getButton().active;
			b.visible = d.getButton().visible;
			
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
	
	public static void addButton(AbstractButtonWidget w) {
		List<AbstractButtonWidget> l = new ArrayList<AbstractButtonWidget>();
		for (ButtonData d : getButtons()) {
			l.add(d.getButton());
		}
		l.add(w);

		if (current != null) {
			updateButtons(current, l);
		}
	}
	
	private static List<AbstractButtonWidget> getGuiButtons(Screen s) {
		List<AbstractButtonWidget> l = new ArrayList<AbstractButtonWidget>();
		try {
			Field f = ReflectionHelper.findField(Screen.class, "buttons", "field_22791");
			l = (List<AbstractButtonWidget>) f.get(s);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return l;
	}

	public static void cacheFrom(Screen s, int screenWidth, int screenHeight) {
		if (useLegacyButtonIds()) {
			List<AbstractButtonWidget> l = new ArrayList<AbstractButtonWidget>();
			try {
				//Resetting the button list
				Field f0 = ReflectionHelper.findField(Screen.class, "buttons", "field_22791");
				f0.set(s, new ArrayList<AbstractButtonWidget>());
				
				//Setting all important values for the GuiScreen to be able to initialize itself
				//itemRenderer field
				Field f1 = ReflectionHelper.findField(Screen.class, "itemRenderer", "field_22788");
				f1.set(s, MinecraftClient.getInstance().getItemRenderer());
				//font field
				Field f2 = ReflectionHelper.findField(Screen.class, "textRenderer", "field_22793");
				f2.set(s, MinecraftClient.getInstance().textRenderer);

				//init
				s.init(MinecraftClient.getInstance(), screenWidth, screenHeight);
				
				//Reflecting the buttons list field to cache all buttons of the menu
				Field f = ReflectionHelper.findField(Screen.class, "buttons", "field_22791");
				l.addAll((List<AbstractButtonWidget>) f.get(s));
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
	@Deprecated
	private static List<AbstractButtonWidget> sortButtons(List<AbstractButtonWidget> widgets) {
		List<AbstractButtonWidget> l = new ArrayList<AbstractButtonWidget>();
		Map<Integer, List<AbstractButtonWidget>> m = new HashMap<Integer, List<AbstractButtonWidget>>(); 
		
		for (AbstractButtonWidget w : widgets) {
			if (CustomizationButton.isCustomizationButton(w)) {
				continue;
			}
			if (m.containsKey(w.y)) {
				m.get(w.y).add(w);
			} else {
				m.put(w.y, new ArrayList<AbstractButtonWidget>());
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
		
		List<AbstractButtonWidget> l2;
		for (Integer i : ints) {
			l2 = m.get(i);
			Collections.sort(l2, new Comparator<AbstractButtonWidget>() {
				@Override
				public int compare(AbstractButtonWidget o1, AbstractButtonWidget o2) {
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
	public static long getIdForButton(AbstractButtonWidget w) {
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
	public static String getNameForButton(AbstractButtonWidget w) {
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
	public static String getKeyForButton(AbstractButtonWidget w) {
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

	public static boolean isCaching() {
		return caching;
	}

}
