package de.keksuccino.fancymenu.customization.widget;

import java.util.*;
import de.keksuccino.fancymenu.customization.widget.identification.WidgetIdentifierHandler;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ScrollScreenNormalizer;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class ScreenWidgetDiscoverer {

	private static final Logger LOGGER = LogManager.getLogger();

	@NotNull
	public static List<WidgetMeta> getWidgetsOfScreen(@NotNull Screen screen) {
		return getWidgetsOfScreen(screen, false);
	}

	@NotNull
	public static List<WidgetMeta> getWidgetsOfScreen(@NotNull Screen screen, boolean updateScreenSize) {
		int newWidth = screen.width;
		int newHeight = screen.height;
		if (updateScreenSize) {
			newWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
			newHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
		}
		return getWidgetsOfScreen(screen, newWidth, newHeight);
	}

	@NotNull
	public static List<WidgetMeta> getWidgetsOfScreen(@NotNull Screen screen, int newWidth, int newHeight) {
		Map<Long, WidgetMeta> widgetMetas = new LinkedHashMap<>();
		try {
			List<WidgetMeta> ids = _getWidgetsOfScreen(screen, 1000, 1000);
			List<WidgetMeta> buttons = _getWidgetsOfScreen(screen, newWidth, newHeight);
			if (buttons.size() == ids.size()) {
				int i = 0;
				for (WidgetMeta id : ids) {
					WidgetMeta button = buttons.get(i);
					if (!widgetMetas.containsKey(id.getLongIdentifier())) {
						widgetMetas.put(id.getLongIdentifier(), new WidgetMeta(button.getWidget(), id.getLongIdentifier(), screen));
					} else {
						LOGGER.warn("[FANCYMENU] Duplicate widget ID found while discovering screen widgets: " + id.getLongIdentifier(), new IllegalStateException("There can't be multiple widgets with the same identifier!"));
					}
					i++;
				}
			}
			List<String> universalIdentifiers = new ArrayList<>();
			for (WidgetMeta meta : widgetMetas.values()) {
				WidgetIdentifierHandler.setUniversalIdentifierOfWidgetMeta(meta);
				if (universalIdentifiers.contains(meta.getUniversalIdentifier())) {
					meta.setUniversalIdentifier(null);
				} else {
					universalIdentifiers.add(meta.getUniversalIdentifier());
				}
			}
		} catch (Exception ex) {
			LOGGER.error("[FANCYMENU] Failed to get widgets of screen!", ex);
		}
		return new ArrayList<>(widgetMetas.values());
	}

	@NotNull
	private static List<WidgetMeta> _getWidgetsOfScreen(@NotNull Screen screen, int screenWidth, int screenHeight) {
		List<WidgetMeta> widgetMetaList = new ArrayList<>();
		List<Long> ids = new ArrayList<>();

		try {

			((IMixinScreen)screen).getRenderablesFancyMenu().forEach(renderable -> {
				if (renderable instanceof CustomizableWidget w) w.resetWidgetCustomizationsFancyMenu();
			});

			//This is to avoid NullPointers
			if (!((IMixinScreen)screen).get_initialized_FancyMenu()) {
				screen.init(Minecraft.getInstance(), screenWidth, screenHeight);
			} else {
				screen.resize(Minecraft.getInstance(), screenWidth, screenHeight);
			}

			ScrollScreenNormalizer.normalizeScrollableScreen(screen);

			((IMixinScreen)screen).getRenderablesFancyMenu().forEach(renderable -> visitWidget(renderable, ids, widgetMetaList, screen));

			widgetMetaList.forEach(widgetMeta -> LOGGER.info("+++++++++++++++++++++++ WIDGET DISCOVERY: " + widgetMeta.getIdentifier()));

		} catch (Exception ex) {
			LOGGER.error("[FANCYMENU] Failed to get widgets of screen!", ex);
		}
		return widgetMetaList;
	}

	private static void visitWidget(@NotNull Object widget, @NotNull List<Long> ids, @NotNull List<WidgetMeta> widgetMetaList, @NotNull Screen screen) {
		if (widget instanceof AbstractWidget w) {
			//Skip AbstractSelectionLists so they don't appear as customizable widget
			if (widget instanceof AbstractSelectionList<?>) return;
			String idRaw = w.getX() + "" + w.getY();
			long id = 0;
			if (MathUtils.isLong(idRaw)) {
				id = getAvailableIdFromBaseId(Long.parseLong(idRaw), ids);
			} else {
				LOGGER.error("[FANCYMENU] Widget ID is not a Long!", new NumberFormatException("Failed to parse widget identifier to Long!"));
			}
			ids.add(id);
			widgetMetaList.add(new WidgetMeta(w, id, screen));
		}
	}

	private static Long getAvailableIdFromBaseId(long baseId, @NotNull List<Long> ids) {
		if (ids.contains(baseId)) {
			String newId = baseId + "1";
			if (MathUtils.isLong(newId)) {
				return getAvailableIdFromBaseId(Long.parseLong(newId), ids);
			} else {
				LOGGER.error("[FANCYMENU] Widget ID is not a Long!", new NumberFormatException("Failed to parse widget identifier to Long!"));
			}
		}
		return baseId;
	}

}
