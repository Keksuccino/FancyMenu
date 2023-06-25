package de.keksuccino.fancymenu.events.screen;

import de.keksuccino.fancymenu.util.event.acara.EventBase;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinScreen;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

public class InitOrResizeScreenCompletedEvent extends EventBase {
	
	protected Screen screen;
	
	public InitOrResizeScreenCompletedEvent(Screen screen) {
		this.screen = screen;
	}
	
	@Override
	public boolean isCancelable() {
		return false;
	}

	public <T extends GuiEventListener & NarratableEntry> void addWidget(T widget) {
		this.getWidgets().add(widget);
		this.getNarratables().add(widget);
	}

	public <T extends GuiEventListener & NarratableEntry & Renderable> void addRenderableWidget(T widget) {
		this.addWidget(widget);
		this.getRenderables().add(widget);
	}

	public List<GuiEventListener> getWidgets() {
		return ((IMixinScreen)this.getScreen()).getChildrenFancyMenu();
	}

	public List<Renderable> getRenderables() {
		return ((IMixinScreen)this.getScreen()).getRenderablesFancyMenu();
	}

	public List<NarratableEntry> getNarratables() {
		return ((IMixinScreen)this.getScreen()).getNarratablesFancyMenu();
	}
	
	public Screen getScreen() {
		return this.screen;
	}
	
}
