package de.keksuccino.fancymenu.menu.fancy.layoutcreator.content;

import java.io.IOException;

import de.keksuccino.fancymenu.menu.fancy.item.CustomizationItemBase;
import de.keksuccino.fancymenu.menu.fancy.item.StringCustomizationItem;
import net.minecraft.client.Minecraft;

public class LayoutString extends LayoutObject {

	private StringCustomizationItem item;
	
	public LayoutString(CustomizationItemBase parent) {
		super(0, 0, 0, 0);
		if (!parent.action.equalsIgnoreCase("addtext")) {
			throw new IllegalArgumentException("Customization item is not a string!");
		} else {
			this.item = (StringCustomizationItem) parent.clone();

			this.setY((int)(this.item.getPosY(Minecraft.getInstance().currentScreen) * this.item.scale));
			this.setX((int)(this.item.getPosX(Minecraft.getInstance().currentScreen) * this.item.scale));
			
			this.setHeight((int)(7 * this.item.scale));
			this.setWidth((int)(Minecraft.getInstance().fontRenderer.getStringWidth(parent.value) * this.item.scale));
		}
	}
	
	@Override
	public void render(int mouseX, int mouseY) {
		super.render(mouseX, mouseY);
		
		if (!this.item.action.equalsIgnoreCase("addtext")) {
			return;
		}
		
		try {
			this.item.render(Minecraft.getInstance().currentScreen);
		} catch (IOException e) {
			e.printStackTrace();
		}
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
