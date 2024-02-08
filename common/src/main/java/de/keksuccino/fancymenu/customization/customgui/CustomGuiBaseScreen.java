package de.keksuccino.fancymenu.customization.customgui;

import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.events.screen.RenderedScreenBackgroundEvent;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CustomGuiBaseScreen extends Screen {

	protected final CustomGui gui;
	protected final Screen overrideScreen;
	protected final Screen parentScreen;

	public CustomGuiBaseScreen(@NotNull CustomGui customGui, @Nullable Screen parentScreen, @Nullable Screen overrideScreen) {
		super(Component.empty());
		this.gui = customGui;
		this.overrideScreen = overrideScreen;
		this.parentScreen = parentScreen;
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreen(this.parentScreen);
	}
	
	@Override
	public boolean shouldCloseOnEsc() {
		return this.gui.allowEsc;
	}

	@Override
	public boolean isPauseScreen() {
		return this.gui.pauseGame;
	}

	@Override
	public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

		if (this.gui.worldBackground) {
			this.renderBackground(graphics);
		} else {
			this.renderDirtBackground(graphics);
		}

		String title = this.getTitleString();
		Component titleComp = LocalizationUtils.isLocalizationKey(title) ? Component.translatable(title) : Component.literal(PlaceholderParser.replacePlaceholders(title));
		graphics.drawCenteredString(this.font, titleComp, this.width / 2, 8, -1);

		super.render(graphics, mouseX, mouseY, partial);

	}

	@Override
	public void renderBackground(@NotNull GuiGraphics graphics) {
		if (Minecraft.getInstance().level != null) {
			if (this.gui.worldBackgroundOverlay) {
				graphics.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
			}
			EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent(this, graphics));
		} else {
			this.renderDirtBackground(graphics);
		}
	}

	@NotNull
	public String getTitleString() {
		return this.gui.title;
	}

	@NotNull
	public String getIdentifier() {
		return this.gui.identifier;
	}

	@Nullable
	public Screen getOverriddenScreen() {
		return this.overrideScreen;
	}

}
