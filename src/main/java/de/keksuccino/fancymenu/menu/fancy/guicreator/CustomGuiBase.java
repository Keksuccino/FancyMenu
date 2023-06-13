package de.keksuccino.fancymenu.menu.fancy.guicreator;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class CustomGuiBase extends Screen {

	private final String identifier;
	private String menutitle;
	public boolean closeOnEsc;
	private Screen overrides;
	public Screen parent;
	
	public CustomGuiBase(String title, String identifier, boolean closeOnEsc, @Nullable Screen parent, @Nullable Screen overrides) {
		super(Component.literal(""));
		this.menutitle = title;
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
	
	@Override
	public Component getTitle() {
		return Component.literal(this.menutitle);
	}
	
	public String getTitleString() {
		return this.menutitle;
	}
	
	@Override
	public void render(GuiGraphics graphics, int p_render_1_, int p_render_2_, float p_render_3_) {
		this.renderBackground(graphics);
		if (title != null) {
			graphics.drawCenteredString(this.font, this.menutitle, this.width / 2, 8, 16777215);
		}
		super.render(graphics, p_render_1_, p_render_2_, p_render_3_);
	}
	
	public String getIdentifier() {
		return this.identifier;
	}
	
	public Screen getOverriddenScreen() {
		return this.overrides;
	}

}
