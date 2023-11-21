package de.keksuccino.fancymenu.events.screen;

import de.keksuccino.fancymenu.util.event.acara.EventBase;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public class InitOrResizeScreenStartingEvent extends EventBase {

	protected final Screen screen;
	protected final InitOrResizeScreenEvent.InitializationPhase phase;

	public InitOrResizeScreenStartingEvent(@NotNull Screen screen, @NotNull InitOrResizeScreenEvent.InitializationPhase phase) {
		this.screen = Objects.requireNonNull(screen);
		this.phase = Objects.requireNonNull(phase);
	}
	
	@Override
	public boolean isCancelable() {
		return false;
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
