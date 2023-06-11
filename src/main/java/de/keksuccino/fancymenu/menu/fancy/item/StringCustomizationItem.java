package de.keksuccino.fancymenu.menu.fancy.item;

import java.io.IOException;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;

@Deprecated
public class StringCustomizationItem extends CustomizationItemBase {

	public float scale = 1.0F;
	public boolean shadow = false;
	public Alignment alignment = Alignment.LEFT;

	public String valueRaw;

	@Deprecated
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
				this.value = de.keksuccino.fancymenu.menu.placeholder.v2.PlaceholderParser.replacePlaceholders(this.valueRaw);
			} else {
				this.value = StringUtils.convertFormatCodes(this.valueRaw, "&", "§");
			}
		}

		this.width = (int) (Minecraft.getInstance().font.width(this.value) * this.scale);
		this.height = (int) (Minecraft.getInstance().font.lineHeight * this.scale);

	}

	public void render(GuiGraphics graphics, Screen menu) throws IOException {
		if (!this.shouldRender()) {
			return;
		}

		this.updateValue();

		int x = this.getPosX(menu);
		int y = this.getPosY(menu);
		Font font = Minecraft.getInstance().font;

		RenderSystem.enableBlend();
		graphics.pose().pushPose();
		graphics.pose().scale(this.scale, this.scale, this.scale);
		graphics.drawString(font, "§f" + this.value, x, y, 0 | Mth.ceil(this.opacity * 255.0F) << 24, this.shadow);
		graphics.pose().popPose();
		RenderSystem.disableBlend();

	}

	@Override
	public int getPosX(Screen menu) {
		int x = super.getPosX(menu);
		if (this.value != null) {
			if (this.alignment == Alignment.CENTERED) {
				x -= (int) ((Minecraft.getInstance().font.width(this.value) / 2) * this.scale);
			} else if (this.alignment == Alignment.RIGHT) {
				x -= (int) (Minecraft.getInstance().font.width(this.value) * this.scale);
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
