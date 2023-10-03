package de.keksuccino.fancymenu.customization.widget;

import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WidgetMeta {

	private final long longIdentifier;
	private String universalIdentifier;
	private final AbstractWidget widget;
	private final Screen screen;
	public Component label;
	public int x;
	public int y;
	public int width;
	public int height;

	public WidgetMeta(@NotNull AbstractWidget widget, long longIdentifier, @NotNull Screen parentScreen) {
		this.longIdentifier = longIdentifier;
		this.widget = widget;
		this.screen = parentScreen;
		this.label = widget.getMessage();
		this.x = widget.getX();
		this.y = widget.getY();
		this.width = widget.getWidth();
		this.height = widget.getHeight();
	}

	@NotNull
	public AbstractWidget getWidget() {
		return this.widget;
	}

	@NotNull
	public Screen getScreen() {
		return this.screen;
	}

	public long getLongIdentifier() {
		return this.longIdentifier;
	}

	@Nullable
	public String getUniversalIdentifier() {
		if ((this.widget instanceof UniqueWidget u) && (u.getWidgetIdentifierFancyMenu() != null)) return u.getWidgetIdentifierFancyMenu();
		return this.universalIdentifier;
	}

	public void setUniversalIdentifier(String identifier) {
		this.universalIdentifier = identifier;
	}

	@NotNull
	public String getIdentifier() {
		if (this.getUniversalIdentifier() != null) {
			return this.getUniversalIdentifier();
		}
		return "" + this.getLongIdentifier();
	}

	@NotNull
	public String getLocator() {
		return ScreenIdentifierHandler.getIdentifierOfScreen(this.getScreen()) + ":" + this.getIdentifier();
	}

	@Nullable
	public String getWidgetLocalizationKey() {
		return LocalizationUtils.getComponentLocalizationKey(this.getWidget().getMessage());
	}

}
