package de.keksuccino.fancymenu.customization.widget;

import java.util.*;
import de.keksuccino.fancymenu.customization.widget.identification.WidgetIdentifierHandler;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinScreen;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class ScreenWidgetDiscoverer {

	private static final Logger LOGGER = LogManager.getLogger();

	@NotNull
	public static List<WidgetMeta> getWidgetsOfScreen(@NotNull Screen screen, boolean updateScreenSize, boolean firstInit) {
		int newWidth = screen.width;
		int newHeight = screen.height;
		if (updateScreenSize) {
			newWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
			newHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
		}
		return getWidgetsOfScreen(screen, newWidth, newHeight, firstInit);
	}

	@NotNull
	public static List<WidgetMeta> getWidgetsOfScreen(@NotNull Screen screen, int newWidth, int newHeight, boolean firstInit) {
		Map<Long, WidgetMeta> widgetMetas = new LinkedHashMap<>();
		try {
			List<WidgetMeta> ids = getWidgetsOfScreenInternal(screen, 1000, 1000, firstInit);
			List<WidgetMeta> buttons = getWidgetsOfScreenInternal(screen, newWidth, newHeight, firstInit);
			if (buttons.size() == ids.size()) {
				int i = 0;
				for (WidgetMeta id : ids) {
					WidgetMeta button = buttons.get(i);
					if (!widgetMetas.containsKey(id.getLongIdentifier())) {
						widgetMetas.put(id.getLongIdentifier(), new WidgetMeta(button.getWidget(), id.getLongIdentifier(), screen));
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
	private static List<WidgetMeta> getWidgetsOfScreenInternal(@NotNull Screen screen, int screenWidth, int screenHeight, boolean firstInit) {
		List<WidgetMeta> widgetMetaList = new ArrayList<>();
		List<Long> ids = new ArrayList<>();
		try {

			//This is to avoid NullPointers
			if (firstInit) {
				screen.init(Minecraft.getInstance(), screenWidth, screenHeight);
			} else {
				screen.resize(Minecraft.getInstance(), screenWidth, screenHeight);
			}

			((IMixinScreen)screen).getRenderablesFancyMenu().forEach(renderable -> visitWidget(renderable, ids, widgetMetaList, screen));

		} catch (Exception ex) {
			LOGGER.error("[FANCYMENU] Failed to get widgets of screen!", ex);
		}
		return widgetMetaList;
	}

	private static void visitWidget(@NotNull Object widget, @NotNull List<Long> ids, @NotNull List<WidgetMeta> widgetMetaList, @NotNull Screen screen) {
		if (widget instanceof AbstractWidget w) {
			String idRaw = w.getX() + "" + w.getY();
			long id = 0;
			if (MathUtils.isLong(idRaw)) {
				id = getAvailableIdFromBaseId(Long.parseLong(idRaw), ids);
			}
			ids.add(id);
			widgetMetaList.add(new WidgetMeta(w, id, screen));
		}
	}

	private static Long getAvailableIdFromBaseId(long baseId, List<Long> ids) {
		if (ids.contains(baseId)) {
			String newId = baseId + "1";
			if (MathUtils.isLong(newId)) {
				return getAvailableIdFromBaseId(Long.parseLong(newId), ids);
			}
		}
		return baseId;
	}

}
