package de.keksuccino.fancymenu.menu.fancy.guicreator;

import javax.annotation.Nullable;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class CustomGuiBase extends Screen {

	private final String identifier;
	private String menutitle;
	public boolean closeOnEsc;
	private Screen overrides;
	public Screen parent;

	public CustomGuiBase(String title, String identifier, boolean closeOnEsc,  Screen parent,  Screen overrides) {
		super(new StringTextComponent(""));
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
	public ITextComponent getTitle() {
		return new StringTextComponent(this.menutitle);
	}
	
	public String getTitleString() {
		return this.menutitle;
	}
	
	@Override
	public void render(MatrixStack matrix, int p_render_1_, int p_render_2_, float p_render_3_) {
		this.renderBackground(matrix);
		if (title != null) {
			AbstractGui.drawCenteredString(matrix, this.font, this.menutitle, this.width / 2, 8, 16777215);
		}
		super.render(matrix, p_render_1_, p_render_2_, p_render_3_);
	}
	
	public String getIdentifier() {
		return this.identifier;
	}
	
	public Screen getOverriddenScreen() {
		return this.overrides;
	}

}
