package de.keksuccino.fancymenu.menu.fancy.layoutcreator.content;

import java.awt.Color;

import de.keksuccino.fancymenu.menu.button.ButtonData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

public class LayoutButton extends LayoutObject {
	
	public final ButtonData button;
	
	public LayoutButton(ButtonData parent) {
		super(parent.x, parent.y, parent.width, parent.height);
		this.button = parent;
	}
	
	@Override
	public void render(int mouseX, int mouseY) {
		GlStateManager.enableBlend();
        drawRect(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), new Color(138, 138, 138, 255).getRGB());
        
        this.drawCenteredString(Minecraft.getInstance().fontRenderer, this.button.label, this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, Color.WHITE.getRGB());
        GlStateManager.disableBlend();
        
        super.render(mouseX, mouseY);
	}
	
}
