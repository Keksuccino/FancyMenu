//TODO übernehmen
package de.keksuccino.fancymenu.menu.fancy.guicreator;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class CustomGuiBase extends Screen {

	private final String identifier;
	private String menutitle;
	public boolean closeOnEsc;
	private Screen overrides;
	private Screen parent;
	
	protected CustomGuiBase(String title, String identifier, boolean closeOnEsc, @Nullable Screen parent, @Nullable Screen overrides) {
		super(new StringTextComponent(""));
		this.menutitle = title;
		this.identifier = identifier;
		this.closeOnEsc = closeOnEsc;
		this.overrides = overrides;
		this.parent = parent;
	}
	
	@Override
	public void onClose() {
		Minecraft.getInstance().displayGuiScreen(this.parent);
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
	public void render(int p_render_1_, int p_render_2_, float p_render_3_) {
		this.renderBackground();
		if (title != null) {
			this.drawCenteredString(this.font, this.menutitle, this.width / 2, 8, 16777215);
		}
		super.render(p_render_1_, p_render_2_, p_render_3_);
	}
	
	public String getIdentifier() {
		return this.identifier;
	}
	
	public Screen getOverriddenScreen() {
		return this.overrides;
	}

}
