package de.keksuccino.fancymenu.customization.customgui;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CustomGuiBaseScreen extends Screen {

	protected final CustomGui gui;
	protected final Screen overrideScreen;
	protected final Screen parentScreen;

	public CustomGuiBaseScreen(@NotNull CustomGui customGui, @Nullable Screen parentScreen, @Nullable Screen overrideScreen) {
		super(Components.empty());
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
	public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

		this.renderBackground(pose);

		String title = this.getTitleString();
		Component titleComp = LocalizationUtils.isLocalizationKey(title) ? Components.translatable(title) : Components.literal(PlaceholderParser.replacePlaceholders(title));
		drawCenteredString(pose, this.font, titleComp, this.width / 2, 8, -1);

		super.render(pose, mouseX, mouseY, partial);

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
