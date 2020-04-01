package de.keksuccino.fancymenu.menu.fancy.layoutcreator.content;

import java.io.IOException;

import de.keksuccino.fancymenu.menu.fancy.item.AnimationCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.CustomizationItemBase;
import net.minecraft.client.Minecraft;

public class LayoutTexture extends LayoutObject {

	private AnimationCustomizationItem item;
	
	public LayoutTexture(CustomizationItemBase parent) {
		super(parent.getPosX(Minecraft.getInstance().currentScreen), parent.getPosY(Minecraft.getInstance().currentScreen), parent.width, parent.height);
		if (!parent.action.equalsIgnoreCase("addtexture")) {
			throw new IllegalArgumentException("Customization item is not a texture!");
		}
	}
	
	@Override
	public void render(int mouseX, int mouseY) {
		super.render(mouseX, mouseY);
		
		if (!this.item.action.equalsIgnoreCase("addtexture")) {
			return;
		}
		
		try {
			this.item.render(Minecraft.getInstance().currentScreen);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void setHeight(int height) {
		super.setHeight(height);
		this.item.height = height;
	}
	
	@Override
	public void setWidth(int width) {
		super.setWidth(width);
		this.item.width = width;
	}
	
	@Override
	public void setX(int x) {
		int x2 = this.item.getPosX(Minecraft.getInstance().currentScreen);
		int i = Math.abs(Math.max(x2, x) - Math.min(x2, x));
		
		if (this.item.getPosX(Minecraft.getInstance().currentScreen) < x) {
			super.setX(this.getX() + i);
			this.item.posX += i;
		}
		if (this.item.getPosX(Minecraft.getInstance().currentScreen) > x) {
			super.setX(this.getX() - i);
			this.item.posX -= i;
		}
	}
	
	@Override
	public void setY(int y) {
		int y2 = this.item.getPosY(Minecraft.getInstance().currentScreen);
		int i = Math.abs(Math.max(y2, y) - Math.min(y2, y));
		
		if (this.item.getPosY(Minecraft.getInstance().currentScreen) < y) {
			super.setY(this.getY() + i);
			this.item.posY += i;
		}
		if (this.item.getPosY(Minecraft.getInstance().currentScreen) > y) {
			super.setY(this.getY() - i);
			this.item.posY -= i;
		}
	}

}
