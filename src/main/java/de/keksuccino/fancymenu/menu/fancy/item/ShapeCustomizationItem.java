package de.keksuccino.fancymenu.menu.fancy.item;

import java.awt.Color;
import java.io.IOException;

import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

public class ShapeCustomizationItem extends CustomizationItemBase {

	public Shape shape;
	protected String colorString = "#ffffff";
	protected Color color = Color.WHITE;
	
	public ShapeCustomizationItem(PropertiesSection item) {
		super(item);
		
		if (this.action.equals("addshape")) {
			
			String sh = item.getEntryValue("shape");
			if (sh != null) {
				this.value = sh;
				this.shape = Shape.byName(sh);
			}
			
			String c = item.getEntryValue("color");
			if (c != null) {
				this.setColor(c);
			}
			
		}
		
	}

	@Override
	public void render(MatrixStack matrix, Screen menu) throws IOException {
		if (this.shouldRender()) {
			if (this.shape != null) {
				
				int alpha = this.color.getAlpha();
				int i = MathHelper.ceil(this.opacity * 255.0F);
				if (i < alpha) {
					alpha = i;
				}
				Color c = new Color(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), alpha);
				
				if (this.shape == Shape.RECTANGLE) {

					fill(matrix, this.getPosX(menu), this.getPosY(menu), this.getPosX(menu) + this.width, this.getPosY(menu) + this.height, c.getRGB());
					
				}
				
			}
		}
	}
	
	public void setColor(String hex) {
		if (hex != null) {
			Color c = RenderUtils.getColorFromHexString(hex);
			if (c != null) {
				this.color = c;
				this.colorString = hex;
				return;
			}
		}
		
		this.color = Color.WHITE;
		this.colorString = "#ffffff";
	}
	
	public String getColorString() {
		return this.colorString;
	}
	
	public Color getColor() {
		return this.color;
	}
	
	public static enum Shape {
		
		RECTANGLE("rectangle");
		
		public String name;
		
		private Shape(String name) {
			this.name = name;
		}
		
		public static Shape byName(String name) {
			name = name.toLowerCase();
			for (Shape s : Shape.values()) {
				if (s.name.equals(name)) {
					return s;
				}
			}
			return null;
		}
		
	}

}
