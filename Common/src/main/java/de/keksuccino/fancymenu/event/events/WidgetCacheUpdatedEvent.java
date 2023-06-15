package de.keksuccino.fancymenu.event.events;

import java.util.ArrayList;
import java.util.List;

import de.keksuccino.fancymenu.event.acara.EventBase;
import de.keksuccino.fancymenu.customization.widget.WidgetMeta;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinScreen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class WidgetCacheUpdatedEvent extends EventBase {

	private static final Logger LOGGER = LogManager.getLogger();
	
	private final Screen screen;
	private final List<WidgetMeta> widgetList;
	private final boolean updated;
	
	public WidgetCacheUpdatedEvent(Screen screen, List<WidgetMeta> widgetList, boolean updated) {
		this.widgetList = widgetList;
		this.screen = screen;
		this.updated = updated;
	}
	
	public Screen getScreen() {
		return this.screen;
	}

	/**
	 * Widgets need to extend {@link GuiEventListener} and {@link NarratableEntry}.
	 */
	public void addWidgetToScreen(@NotNull GuiEventListener widget) {
		if (widget instanceof NarratableEntry) {
			((IMixinScreen)this.getScreen()).getChildrenFancyMenu().add(widget);
		} else {
			LOGGER.error("[FANCYMENU] Failed to add widget! Needs to extend NarratableEntry!");
			new Throwable().printStackTrace();
		}
	}
	
	public List<WidgetMeta> getCachedWidgetMetaList() {
		return this.widgetList;
	}
	
	public List<AbstractWidget> getCachedWidgetsList() {
		List<AbstractWidget> l = new ArrayList<>();
		for (WidgetMeta d : this.widgetList) {
			l.add(d.getWidget());
		}
		return l;
	}
	
	public boolean cacheUpdated() {
		return this.updated;
	}
	
	@Override
	public boolean isCancelable() {
		return false;
	}

}
