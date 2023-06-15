package de.keksuccino.fancymenu.customization.widget;

import java.util.*;

import de.keksuccino.fancymenu.customization.widget.identification.ButtonIdentificator;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinScreen;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import de.keksuccino.fancymenu.event.events.screen.InitOrResizeScreenCompletedEvent;

public class ScreenWidgetDiscoverer {

	private static final Logger LOGGER = LogManager.getLogger();

	/**
	 * It is recommended to only call this in {@link InitOrResizeScreenCompletedEvent}s, if the target screen is currently active.
	 */
	@NotNull
	public static List<WidgetMeta> getWidgetMetasOfScreen(Screen screen) {
		Map<Long, WidgetMeta> widgetMetas = new LinkedHashMap<>();
		try {
			int oriScreenWidth = screen.width;
			int oriScreenHeight = screen.height;
			List<WidgetMeta> ids = getWidgetMetasOfScreen(screen, 1000, 1000);
			List<WidgetMeta> buttons = getWidgetMetasOfScreen(screen, oriScreenWidth, oriScreenHeight);
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
			List<String> compIds = new ArrayList<>();
			for (WidgetMeta d : widgetMetas.values()) {
				ButtonIdentificator.setCompatibilityIdentifierOfWidgetMeta(d);
				if (compIds.contains(d.compatibilityId)) {
					d.compatibilityId = null;
				} else {
					compIds.add(d.compatibilityId);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return new ArrayList<>(widgetMetas.values());
	}

	@NotNull
	protected static List<WidgetMeta> getWidgetMetasOfScreen(Screen screen, int screenWidth, int screenHeight) {
		List<WidgetMeta> widgetMetaList = new ArrayList<>();
		List<Long> ids = new ArrayList<>();
		try {

			//This is to avoid NullPointers
			((IMixinScreen)screen).setItemRendererFancyMenu(Minecraft.getInstance().getItemRenderer());
			((IMixinScreen)screen).setFontFancyMenu(Minecraft.getInstance().font);

			screen.resize(Minecraft.getInstance(), screenWidth, screenHeight);

			List<AbstractWidget> widgets = new ArrayList<>();
			for (Renderable r : ((IMixinScreen)screen).getRenderablesFancyMenu()) {
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
				widgetMetaList.add(new WidgetMeta(w, id, screen));
			}

		} catch (Exception ex) {
			ex.printStackTrace();
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

}
