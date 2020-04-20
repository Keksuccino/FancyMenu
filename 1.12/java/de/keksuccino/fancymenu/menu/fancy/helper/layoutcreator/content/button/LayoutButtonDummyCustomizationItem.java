package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button;

import java.awt.Color;
import java.io.IOException;

import de.keksuccino.core.properties.PropertiesSection;
import de.keksuccino.fancymenu.menu.fancy.item.CustomizationItemBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;

/**
 * Dummy item class to use its orientation handling for LayoutButtons
 */
public class LayoutButtonDummyCustomizationItem  extends CustomizationItemBase {

	public LayoutButtonDummyCustomizationItem(String label, int width, int height, int x, int y) {
		super(new PropertiesSection("customization"));
		this.value = label;
		this.action = "handlelayoutbutton";
		this.width = width;
		this.height = height;
		this.posX = x;
		this.posY = y;
	}

	@Override
	public void render(GuiScreen menu) throws IOException {
		GlStateManager.enableBlend();
        Gui.drawRect(this.getPosX(menu), this.getPosY(menu), this.getPosX(menu) + this.width, this.getPosY(menu) + this.height, new Color(138, 138, 138, 255).getRGB());
        menu.drawCenteredString(Minecraft.getMinecraft().fontRenderer, this.value, this.getPosX(menu) + this.width / 2, this.getPosY(menu) + (this.height - 8) / 2, new Color(255, 255, 255, 255).getRGB());
        GlStateManager.disableBlend();
	}

}
