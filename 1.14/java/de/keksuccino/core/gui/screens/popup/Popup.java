package de.keksuccino.core.gui.screens.popup;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.GlStateManager;

import de.keksuccino.core.gui.content.AdvancedButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.client.gui.screen.Screen;

public abstract class Popup {
	
	private boolean displayed = false;
	private int alpha;
	private List<AdvancedButton> buttons = new ArrayList<AdvancedButton>();
	
	public Popup(int backgroundAlpha) {
		this.alpha = backgroundAlpha;
	}
	
	public void render(int mouseX, int mouseY, Screen renderIn) {
		if (!this.isDisplayed()) {
			return;
		}
		GlStateManager.enableBlend();
		IngameGui.fill(0, 0, renderIn.width, renderIn.height, new Color(0, 0, 0, this.alpha).getRGB());
		GlStateManager.disableBlend();
	}
	
	public boolean isDisplayed() {
		return this.displayed;
	}
	
	public void setDisplayed(boolean b) {
		this.displayed = b;
	}
	
	public List<AdvancedButton> getButtons() {
		return this.buttons;
	}
	
	protected void addButton(AdvancedButton b) {
		if (!this.buttons.contains(b)) {
			this.buttons.add(b);
			this.colorizePopupButton(b);
		}
	}
	
	protected void removeButton(AdvancedButton b) {
		if (this.buttons.contains(b)) {
			this.buttons.remove(b);
		}
	}
	
	protected void renderButtons(int mouseX, int mouseY) {
		for (AdvancedButton b : this.buttons) {
			b.render(mouseX, mouseY, Minecraft.getInstance().getRenderPartialTicks());
		}
	}
	
	protected void colorizePopupButton(AdvancedButton b) {
		b.setBackgroundColor(new Color(102, 102, 153), new Color(133, 133, 173), new Color(163, 163, 194), new Color(163, 163, 194), 1);
	}

}
