package de.keksuccino.fancymenu.menu.button;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.events.GuiInitCompletedEvent;
import de.keksuccino.fancymenu.mainwindow.MainWindowHandler;
import de.keksuccino.fancymenu.menu.button.identification.ButtonIdentificator;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.konkrete.gui.screens.SimpleLoadingScreen;
import de.keksuccino.konkrete.localization.LocaleUtils;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ButtonCache {
	
	private static Map<Long, ButtonData> buttons = new HashMap<Long, ButtonData>();
	private static Map<Long, GuiButton> replaced = new HashMap<Long, GuiButton>();
	private static GuiScreen current = null;
	private static boolean cached = false;
	private static boolean caching = false;
	private static Map<String, GuiButton> customButtons = new HashMap<String, GuiButton>();
	
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
	
	private static void cache(GuiScreen s) {
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
			if ((s != null) && (s instanceof GuiVideoSettings) && FancyMenu.isOptifineLoaded()) {
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
			
			if ((s == Minecraft.getMinecraft().currentScreen)) {
				updateButtons(s);
			}

			MinecraftForge.EVENT_BUS.post(new ButtonCachedEvent(s, getButtons(), true));
		}
	}
	
	/**
	 * The widget list (buttonlist) is only used by the old button id system.
	 */
	private static void updateButtons(GuiScreen s) {
		replaced.clear();
		buttons.clear();
		
		if (!MenuCustomization.isMenuCustomizable(s)) {
			return;
		}
		
		//Don't update video settings buttons if Optifine is active
		if ((s != null) && (s instanceof GuiVideoSettings) && FancyMenu.isOptifineLoaded()) {
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
		List<ButtonData> btns = cacheButtons(s, MainWindowHandler.getScaledWidth(), MainWindowHandler.getScaledHeight());

		if (btns.size() == ids.size()) {
			int i = 0;
			for (ButtonData id : ids) {
				ButtonData button = btns.get(i);
				if (!buttons.containsKey(id.getId())) {
					buttons.put(id.getId(), new ButtonData(button.getButton(), id.getId(), LocaleUtils.getKeyForString(button.getButton().displayString), s));
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

		List<String> compIds = new ArrayList<>();
		for (ButtonData d : buttons.values()) {
			ButtonIdentificator.setCompatibilityIdentifierToData(d);
			if (compIds.contains(d.compatibilityId)) {
				d.compatibilityId = null;
			} else {
				compIds.add(d.compatibilityId);
			}
		}

	}

	public static List<ButtonData> cacheButtons(GuiScreen s, int screenWidth, int screenHeight) {
		caching = true;
		List<ButtonData> buttonList = new ArrayList<>();
		List<Long> ids = new ArrayList<>();
		try {
			//Resetting the button list
			Field f0 = ObfuscationReflectionHelper.findField(GuiScreen.class, "field_146292_n"); //buttonList
			((List<GuiButton>)f0.get(s)).clear();

			//Setting all important values for the GuiScreen to allow it to initialize itself
			s.mc = Minecraft.getMinecraft();
			s.width = screenWidth;
			s.height = screenHeight;

			Field f1 = ObfuscationReflectionHelper.findField(GuiScreen.class, "field_146296_j"); //itemRenderer
			f1.set(s, Minecraft.getMinecraft().getRenderItem());

			Field f2 = ObfuscationReflectionHelper.findField(GuiScreen.class, "field_146289_q"); //fontRenderer
			f2.set(s, Minecraft.getMinecraft().fontRenderer);

			MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.InitGuiEvent.Pre(s, (List<GuiButton>) f0.get(s)));

			s.initGui();

			MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.InitGuiEvent.Post(s, (List<GuiButton>) f0.get(s)));

			//Reflecting the buttons list field to cache all buttons of the menu
			Field f = ObfuscationReflectionHelper.findField(GuiScreen.class, "field_146292_n"); //buttonList

			for (GuiButton w : (List<GuiButton>) f.get(s)) {
				String idRaw = w.x + "" + w.y;
				long id = 0;
				if (MathUtils.isLong(idRaw)) {
					id = getAvailableIdFromBaseId(Long.parseLong(idRaw), ids);
				}
				ids.add(id);
				buttonList.add(new ButtonData(w, id, LocaleUtils.getKeyForString(w.displayString), s));
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		caching = false;
		return buttonList;
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
	
	public static void cacheFrom(GuiScreen s, int screenWidth, int screenHeight) {
		updateButtons(s);
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
	 * Returns the button for this ID or null if no button with this ID was found.
	 */
	public static ButtonData getButtonForCompatibilityId(String id) {
		for (ButtonData d : buttons.values()) {
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

	public static void cacheCustomButton(String id, GuiButton w) {
		customButtons.put(id, w);
	}

	public static GuiButton getCustomButton(String id) {
		return customButtons.get(id);
	}

}
