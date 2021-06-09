package de.keksuccino.fancymenu.menu.fancy.item;

import java.io.IOException;


import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.fancymenu.menu.fancy.DynamicValueHelper;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.MathHelper;

public class StringCustomizationItem extends CustomizationItemBase {
	
	public float scale = 1.0F;
	public boolean shadow = false;
	//TODO übernehmen
//	public boolean centered = false;
	//TODO übernehmen
	public Alignment alignment = Alignment.LEFT;
	
	public StringCustomizationItem(PropertiesSection item) {
		super(item);

		if ((this.action != null) && this.action.equalsIgnoreCase("addtext")) {
			this.value = item.getEntryValue("value");
			if (this.value != null) {
				//TODO übernehmen
				if (!isEditorActive()) {
					this.value = DynamicValueHelper.convertFromRaw(this.value);
				} else {
					this.value = StringUtils.convertFormatCodes(this.value, "&", "§");
				}
				//-------------
			}
			
			String sh = item.getEntryValue("shadow");
			if ((sh != null)) {
				if (sh.equalsIgnoreCase("true")) {
					this.shadow = true;
				}
			}
			
			//TODO übernehmen
//			String ce = item.getEntryValue("centered");
//			if ((ce != null)) {
//				if (ce.equalsIgnoreCase("true")) {
//					this.centered = true;
//				}
//			}
			
			String sc = item.getEntryValue("scale");
			if ((sc != null) && MathUtils.isFloat(sc)) {
				this.scale = Float.parseFloat(sc);
			}
			
			//TODO übernehmen
			String al = item.getEntryValue("alignment");
			if (al != null) {
				if (al.equalsIgnoreCase("right")) {
					this.alignment = Alignment.RIGHT;
				}
				if (al.equalsIgnoreCase("centered")) {
					this.alignment = Alignment.CENTERED;
				}
			}
			//--------------------
			
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
			//TODO übernehmen
			font.drawStringWithShadow("§f" + this.value, x, y, 0 | MathHelper.ceil(this.opacity * 255.0F) << 24);
		} else {
			//TODO übernehmen
			font.drawString("§f" + this.value, x, y, 0 | MathHelper.ceil(this.opacity * 255.0F) << 24);
		}
		RenderSystem.popMatrix();
		RenderSystem.disableBlend();

	}

	//TODO übernehmen
	@Override
	public int getPosX(Screen menu) {
		int x = super.getPosX(menu);
		if (this.value != null) {
			if (this.alignment == Alignment.CENTERED) {
				x -= (int) ((Minecraft.getInstance().fontRenderer.getStringWidth(this.value) / 2) * this.scale);
			} else if (this.alignment == Alignment.RIGHT) {
				x -= (int) (Minecraft.getInstance().fontRenderer.getStringWidth(this.value) * this.scale);
			}
		}
		x = (int)(x / this.scale);
		return x;
	}

	@Override
	public int getPosY(Screen menu) {
		return (int) (super.getPosY(menu) / this.scale);
	}
	
	//TODO übernehmen
//	@Override
//	public StringCustomizationItem clone() {
//		StringCustomizationItem item = new StringCustomizationItem(new PropertiesSection(""));
//		item.alignment = this.alignment;
//		item.height = this.height;
//		item.orientation = this.orientation;
//		item.posX = this.posX;
//		item.posY = this.posY;
//		item.scale = this.scale;
//		item.shadow = this.shadow;
//		item.value = this.value;
//		item.width = this.width;
//		item.action = this.action;
//		return item;
//	}

}
