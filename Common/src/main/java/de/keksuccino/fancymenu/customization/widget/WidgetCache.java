package de.keksuccino.fancymenu.customization.widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.keksuccino.fancymenu.customization.widget.identification.ButtonIdentificator;
import de.keksuccino.fancymenu.event.events.ButtonCacheUpdatedEvent;
import de.keksuccino.fancymenu.event.acara.EventHandler;
import de.keksuccino.fancymenu.event.acara.EventListener;
import de.keksuccino.fancymenu.event.events.screen.InitOrResizeScreenCompletedEvent;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinScreen;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WidgetCache {

	private static final Logger LOGGER = LogManager.getLogger();

	private static final Map<Long, WidgetMeta> WIDGET_METAS = new HashMap<>();

	public static void init() {
		EventHandler.INSTANCE.registerListenersOf(new WidgetCache());
	}

	@EventListener
	public void onInitCompleted(InitOrResizeScreenCompletedEvent e) {
		cacheWidgets(e.getScreen());
	}

	private static void cacheWidgets(Screen s) {
		if (!ScreenCustomization.isCustomizationEnabledForScreen(s) || ScreenCustomization.isScreenBlacklisted(s)) {
			WIDGET_METAS.clear();
			EventHandler.INSTANCE.postEvent(new ButtonCacheUpdatedEvent(s, new ArrayList<>(), false));
			return;
		}
		if ((s == Minecraft.getInstance().screen)) {
			updateWidgetCache(s);
		}
		EventHandler.INSTANCE.postEvent(new ButtonCacheUpdatedEvent(s, getWidgets(), true));
	}

	public static void updateWidgetCache(Screen s) {

		WIDGET_METAS.clear();

		//Use the new id calculation system
		List<WidgetMeta> ids = getWidgetsOfScreen(s, 1000, 1000);
		List<WidgetMeta> buttons = getWidgetsOfScreen(s, Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight());

		if (buttons.size() == ids.size()) {
			int i = 0;
			for (WidgetMeta id : ids) {
				WidgetMeta button = buttons.get(i);
				if (!WIDGET_METAS.containsKey(id.getLongIdentifier())) {
					WIDGET_METAS.put(id.getLongIdentifier(), new WidgetMeta(button.getWidget(), id.getLongIdentifier(), s));
				}
				i++;
			}
		}

		List<String> compIds = new ArrayList<>();
		for (WidgetMeta d : WIDGET_METAS.values()) {
			ButtonIdentificator.setCompatibilityIdentifierToWidgetMeta(d);
			if (compIds.contains(d.compatibilityId)) {
				d.compatibilityId = null;
			} else {
				compIds.add(d.compatibilityId);
			}
		}

	}

	public static List<WidgetMeta> getWidgetsOfScreen(Screen s, int screenWidth, int screenHeight) {
		List<WidgetMeta> widgetMetaList = new ArrayList<>();
		List<Long> ids = new ArrayList<>();
		try {

			//Set all important variables and init screen
			((IMixinScreen)s).setItemRendererFancyMenu(Minecraft.getInstance().getItemRenderer());
			((IMixinScreen)s).setFontFancyMenu(Minecraft.getInstance().font);

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
				widgetMetaList.add(new WidgetMeta(w, id, s));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return widgetMetaList;
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

	@Nullable
	private static WidgetMeta getWidgetForId(long id) {
		return WIDGET_METAS.get(id);
	}

	@Nullable
	private static WidgetMeta getWidgetForCompatibilityId(String id) {
		for (WidgetMeta d : WIDGET_METAS.values()) {
			if (d.getCompatibilityIdentifier() != null) {
				if (d.getCompatibilityIdentifier().equals(id)) {
					return d;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the cached widget with the given identifier or NULL if no widget for the given identifier was found.
	 */
	@Nullable
	public static WidgetMeta getWidget(String identifier) {
		identifier = identifier.replace("vanillabtn:", "");
		WidgetMeta data = getWidgetForCompatibilityId(identifier);
		if ((data == null) && MathUtils.isLong(identifier)) {
			data = getWidgetForId(Long.parseLong(identifier));
		}
		return data;
	}

	/**
	 * Returns all currently cached widgets as {@link WidgetMeta} objects.
	 */
	@NotNull
	public static List<WidgetMeta> getWidgets() {
		return new ArrayList<>(WIDGET_METAS.values());
	}

}
