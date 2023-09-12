package de.keksuccino.fancymenu.customization.widget;

import de.keksuccino.fancymenu.customization.screenidentifiers.ScreenIdentifierHandler;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WidgetMeta {

	private final long id;
	protected String compatibilityId;
	private final AbstractWidget widget;
	private final Screen screen;
	public Component label;
	public int x;
	public int y;
	public int width;
	public int height;

	public WidgetMeta(@NotNull AbstractWidget widget, long id, @NotNull Screen parentScreen) {
		this.id = id;
		this.widget = widget;
		this.screen = parentScreen;
		this.label = widget.getMessage();
		this.x = widget.x;
		this.y = widget.y;
		this.width = widget.getWidth();
		this.height = widget.getHeight();
	}

	@NotNull
	public AbstractWidget getWidget() {
		return widget;
	}

	@NotNull
	public Screen getScreen() {
		return screen;
	}

	public long getLongIdentifier() {
		return id;
	}

	@Nullable
	public String getCompatibilityIdentifier() {
		return this.compatibilityId;
	}

	public void setCompatibilityId(String id) {
		this.compatibilityId = id;
	}

	@NotNull
	public String getIdentifier() {
		if (this.getCompatibilityIdentifier() != null) {
			return this.getCompatibilityIdentifier();
		}
		return "" + this.getLongIdentifier();
	}

	@NotNull
	public String getLocator() {
		return ScreenIdentifierHandler.getIdentifierOfScreen(this.screen) + ":" + this.getIdentifier();
	}

}
