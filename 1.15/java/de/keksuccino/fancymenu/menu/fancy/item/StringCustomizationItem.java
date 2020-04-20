package de.keksuccino.fancymenu.menu.fancy.item;

import java.io.IOException;

import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.core.math.MathUtils;
import de.keksuccino.core.properties.PropertiesSection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;

public class StringCustomizationItem extends CustomizationItemBase {
	
	public float scale = 1.0F;
	public boolean shadow = false;
	public boolean centered = false;
	
	public StringCustomizationItem(PropertiesSection item) {
		super(item);

		if ((this.action != null) && this.action.equalsIgnoreCase("addtext")) {
			this.value = item.getEntryValue("value");
			
			String sh = item.getEntryValue("shadow");
			if ((sh != null)) {
				if (sh.equalsIgnoreCase("true")) {
					this.shadow = true;
				}
			}

			String ce = item.getEntryValue("centered");
			if ((ce != null)) {
				if (ce.equalsIgnoreCase("true")) {
					this.centered = true;
				}
			}
			
			String sc = item.getEntryValue("scale");
			if ((sc != null) && MathUtils.isFloat(sc)) {
				this.scale = Float.parseFloat(sc);
			}
		}
	}

	public void render(Screen menu) throws IOException {
		if (!this.shouldRender()) {
			return;
		}
		
		int x = this.getPosX(menu);
		int y = this.getPosY(menu);
		FontRenderer font = Minecraft.getInstance().fontRenderer;

		RenderSystem.enableBlend();
		RenderSystem.pushMatrix();
		RenderSystem.scalef(this.scale, this.scale, this.scale);
		if (this.shadow) {
			font.drawStringWithShadow("§f" + this.value, x, y, 0);
		} else {
			font.drawString("§f" + this.value, x, y, 0);
		}
		RenderSystem.popMatrix();
		RenderSystem.disableBlend();
	}

	@Override
	public int getPosX(Screen menu) {
		int x = super.getPosX(menu);
		if (this.centered) {
			x -= (int) ((Minecraft.getInstance().fontRenderer.getStringWidth(this.value) / 2) * this.scale);
		}
		x = (int)(x / this.scale);
		return x;
	}

	@Override
	public int getPosY(Screen menu) {
		return (int) (super.getPosY(menu) / this.scale);
	}
	
	@Override
	public StringCustomizationItem clone() {
		StringCustomizationItem item = new StringCustomizationItem(new PropertiesSection(""));
		item.centered = this.centered;
		item.height = this.height;
		item.orientation = this.orientation;
		item.posX = this.posX;
		item.posY = this.posY;
		item.scale = this.scale;
		item.shadow = this.shadow;
		item.value = this.value;
		item.width = this.width;
		item.action = this.action;
		return item;
	}

}
