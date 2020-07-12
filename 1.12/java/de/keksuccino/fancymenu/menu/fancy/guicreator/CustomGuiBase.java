package de.keksuccino.fancymenu.menu.fancy.guicreator;

import java.io.IOException;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

public class CustomGuiBase extends GuiScreen {

	private final String identifier;
	private String menutitle;
	public boolean closeOnEsc;
	public String title;
	private GuiScreen overrides;
	private GuiScreen parent;
	
	protected CustomGuiBase(String title, String identifier, boolean closeOnEsc, @Nullable GuiScreen parent, @Nullable GuiScreen overrides) {
		this.menutitle = title;
		this.identifier = identifier;
		this.closeOnEsc = closeOnEsc;
		this.overrides = overrides;
		this.parent = parent;
		this.title = title;
	}

	public void onClose() {
		//TODO wird in 1.12 nicht von gameloop gecallt
		if (this.parent != null) {
			Minecraft.getMinecraft().displayGuiScreen(this.parent);
		} else {
			Minecraft.getMinecraft().displayGuiScreen((GuiScreen)null);
		}
	}

	public ITextComponent getTitle() {
		return new TextComponentString(this.menutitle);
	}
	
	public String getTitleString() {
		return this.menutitle;
	}
	
	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            if (this.closeOnEsc) {
            	if (this.parent != null) {
                	this.mc.displayGuiScreen(this.parent);
                } else {
                	this.mc.displayGuiScreen((GuiScreen)null);
                }

                if (this.mc.currentScreen == null) {
                    this.mc.setIngameFocus();
                }
            }
        }
    }
	
	@Override
	public void drawScreen(int p_render_1_, int p_render_2_, float p_render_3_) {
		this.drawDefaultBackground();
		if (title != null) {
			this.drawCenteredString(Minecraft.getMinecraft().fontRenderer, this.menutitle, this.width / 2, 8, 16777215);
		}
		super.drawScreen(p_render_1_, p_render_2_, p_render_3_);
	}
	
	public String getIdentifier() {
		return this.identifier;
	}
	
	public GuiScreen getOverriddenScreen() {
		return this.overrides;
	}

}
