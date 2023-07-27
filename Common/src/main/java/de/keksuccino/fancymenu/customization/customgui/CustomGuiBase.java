package de.keksuccino.fancymenu.customization.customgui;

import javax.annotation.Nullable;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class CustomGuiBase extends Screen {

	private final String identifier;
	private final String menuTitle;
	public boolean closeOnEsc;
	private final Screen overrides;
	public Screen parent;

	public CustomGuiBase(@NotNull String title, @NotNull String identifier, boolean closeOnEsc, @Nullable Screen parent, @Nullable Screen overrides) {
		super(Component.literal(title));
		this.menuTitle = title;
		this.identifier = identifier;
		this.closeOnEsc = closeOnEsc;
		this.overrides = overrides;
		this.parent = parent;
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreen(this.parent);
	}
	
	@Override
	public boolean shouldCloseOnEsc() {
		return this.closeOnEsc;
	}

	@NotNull
	public String getTitleString() {
		return this.menuTitle;
	}
	
	@Override
	public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
		this.renderBackground(pose);
		GuiComponent.drawCenteredString(pose, this.font, this.title, this.width / 2, 8, 16777215);
		super.render(pose, mouseX, mouseY, partial);
	}

	@NotNull
	public String getIdentifier() {
		return this.identifier;
	}

	@NotNull
	public Screen getOverriddenScreen() {
		return this.overrides;
	}

}
