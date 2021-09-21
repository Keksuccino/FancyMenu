package de.keksuccino.fancymenu.menu.fancy.item;

import java.io.IOException;

import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.fancymenu.menu.fancy.DynamicValueHelper;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

public class StringCustomizationItem extends CustomizationItemBase {
	
	public float scale = 1.0F;
	public boolean shadow = false;
	public Alignment alignment = Alignment.LEFT;

	public String valueRaw;
	
	public StringCustomizationItem(PropertiesSection item) {
		super(item);

		if ((this.action != null) && this.action.equalsIgnoreCase("addtext")) {
			this.valueRaw = item.getEntryValue("value");
			this.updateValue();
			
			String sh = item.getEntryValue("shadow");
			if ((sh != null)) {
				if (sh.equalsIgnoreCase("true")) {
					this.shadow = true;
				}
			}
			
			String sc = item.getEntryValue("scale");
			if ((sc != null) && MathUtils.isFloat(sc)) {
				this.scale = Float.parseFloat(sc);
			}
			
			String al = item.getEntryValue("alignment");
			if (al != null) {
				if (al.equalsIgnoreCase("right")) {
					this.alignment = Alignment.RIGHT;
				}
				if (al.equalsIgnoreCase("centered")) {
					this.alignment = Alignment.CENTERED;
				}
			}
			
		}
	}

	protected void updateValue() {

		if (this.valueRaw != null) {
			if (!isEditorActive()) {
				this.value = DynamicValueHelper.convertFromRaw(this.valueRaw);
			} else {
				this.value = StringUtils.convertFormatCodes(this.valueRaw, "&", "§");
			}
		}

		this.width = (int) (MinecraftClient.getInstance().textRenderer.getWidth(this.value) * this.scale);
		this.height = (int) (MinecraftClient.getInstance().textRenderer.fontHeight * this.scale);

	}

	public void render(MatrixStack matrix, Screen menu) throws IOException {
		if (!this.shouldRender()) {
			return;
		}

		this.updateValue();
		
		int x = this.getPosX(menu);
		int y = this.getPosY(menu);
		TextRenderer font = MinecraftClient.getInstance().textRenderer;

		RenderSystem.enableBlend();
		matrix.push();
		matrix.scale(this.scale, this.scale, this.scale);
		if (this.shadow) {
			font.drawWithShadow(matrix, "§f" + this.value, x, y, 0 | MathHelper.ceil(this.opacity * 255.0F) << 24);
		} else {
			font.draw(matrix, "§f" + this.value, x, y, 0 | MathHelper.ceil(this.opacity * 255.0F) << 24);
		}
		matrix.pop();
		RenderSystem.disableBlend();
	}

	@Override
	public int getPosX(Screen menu) {
		int x = super.getPosX(menu);
		if (this.value != null) {
			if (this.alignment == Alignment.CENTERED) {
				x -= (int) ((MinecraftClient.getInstance().textRenderer.getWidth(this.value) / 2) * this.scale);
			} else if (this.alignment == Alignment.RIGHT) {
				x -= (int) (MinecraftClient.getInstance().textRenderer.getWidth(this.value) * this.scale);
			}
		}
		x = (int)(x / this.scale);
		return x;
	}

	@Override
	public int getPosY(Screen menu) {
		return (int) (super.getPosY(menu) / this.scale);
	}

}
