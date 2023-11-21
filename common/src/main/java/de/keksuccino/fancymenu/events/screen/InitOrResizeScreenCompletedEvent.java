package de.keksuccino.fancymenu.events.screen;

import de.keksuccino.fancymenu.util.event.acara.EventBase;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinScreen;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Objects;

public class InitOrResizeScreenCompletedEvent extends EventBase {
	
	protected final Screen screen;
	protected final InitOrResizeScreenEvent.InitializationPhase phase;
	
	public InitOrResizeScreenCompletedEvent(@NotNull Screen screen, @NotNull InitOrResizeScreenEvent.InitializationPhase phase) {
		this.screen = Objects.requireNonNull(screen);
		this.phase = Objects.requireNonNull(phase);
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

	@NotNull
	public Screen getScreen() {
		return this.screen;
	}

	@NotNull
	public InitOrResizeScreenEvent.InitializationPhase getInitializationPhase() {
		return this.phase;
	}
	
}
