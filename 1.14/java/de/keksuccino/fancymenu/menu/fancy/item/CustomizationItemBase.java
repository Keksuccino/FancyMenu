package de.keksuccino.fancymenu.menu.fancy.item;

import java.io.IOException;

import de.keksuccino.core.math.MathUtils;
import de.keksuccino.core.properties.PropertiesSection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;

public abstract class CustomizationItemBase {
	
	/**
	 * This value CANNOT BE NULL!<br>
	 * If null, {@code CustomizationItemBase.shouldRender()} will never return true.
	 */
	public String value;
	public String action;
	/**
	 * NOT similar to {@code MenuCustomizationItem.getPosX()}! This is the raw value without the defined orientation and scale!
	 */
	public int posX = 0;
	/**
	 * NOT similar to {@code MenuCustomizationItem.getPosY()}! This is the raw value without the defined orientation and scale!
	 */
	public int posY = 0;
	public String orientation = "top-left";
	public int width = -1;
	public int height = -1;
	
	public CustomizationItemBase(PropertiesSection item) {
		this.action = item.getEntryValue("action");
		
		int guiwidth = 0;
		int guiheight = 0;
		if (Minecraft.getInstance().currentScreen != null) {
			guiwidth = Minecraft.getInstance().currentScreen.width;
			guiheight = Minecraft.getInstance().currentScreen.height;
		}

		String x = item.getEntryValue("x");
		String y = item.getEntryValue("y");
		if (x != null) {
			x = x.replace("%guiwidth%", "" + guiwidth).replace("%guiheight%", "" + guiheight);
			this.posX = (int) MathUtils.calculateFromString(x);
		}
		if (y != null) {
			y = y.replace("%guiwidth%", "" + guiwidth).replace("%guiheight%", "" + guiheight);
			this.posY = (int) MathUtils.calculateFromString(y);
		}

		String o = item.getEntryValue("orientation");
		if (o != null) {
			this.orientation = o;
		}

		String w = item.getEntryValue("width");
		if (w != null) {
			w = w.replace("%guiwidth%", "" + guiwidth).replace("%guiheight%", "" + guiheight);
			this.width = (int) MathUtils.calculateFromString(w);
			if (this.width < 0) {
				this.width = 0;
			}
		}

		String h = item.getEntryValue("height");
		if (h != null) {
			h = h.replace("%guiwidth%", "" + guiwidth).replace("%guiheight%", "" + guiheight);
			this.height = (int) MathUtils.calculateFromString(h);
			if (this.height < 0) {
				this.height = 0;
			}
		}
	}

	public abstract void render(Screen menu) throws IOException;
	
	/**
	 * Should be used to get the REAL and final X-position of this item.<br>
	 * NOT similar to {@code MenuCustomizationItem.posX}! 
	 */
	public int getPosX(Screen menu) {
		int w = menu.width;
		int x = this.posX;

		if (orientation.equalsIgnoreCase("top-centered")) {
			x += (w / 2);
		}
		if (orientation.equalsIgnoreCase("mid-centered")) {
			x += (w / 2);
		}
		if (orientation.equalsIgnoreCase("bottom-centered")) {
			x += (w / 2);
		}
		//-----------------------------
		if (orientation.equalsIgnoreCase("top-right")) {
			x += w;
		}
		if (orientation.equalsIgnoreCase("mid-right")) {
			x += w;
		}
		if (orientation.equalsIgnoreCase("bottom-right")) {
			x += w;
		}
		
		return x;
	}
	
	/**
	 * Should be used to get the REAL and final Y-position of this item.<br>
	 * NOT similar to {@code MenuCustomizationItem.posY}! 
	 */
	public int getPosY(Screen menu) {
		int h = menu.height;
		int y = this.posY;

		if (orientation.equalsIgnoreCase("mid-left")) {
			y += (h / 2);
		}
		if (orientation.equalsIgnoreCase("bottom-left")) {
			y += h;
		}
		//----------------------------
		if (orientation.equalsIgnoreCase("mid-centered")) {
			y += (h / 2);
		}
		if (orientation.equalsIgnoreCase("bottom-centered")) {
			y += h;
		}
		//-----------------------------
		if (orientation.equalsIgnoreCase("top-right")) {
		}
		if (orientation.equalsIgnoreCase("mid-right")) {
			y += (h / 2);
		}
		if (orientation.equalsIgnoreCase("bottom-right")) {
			y += h;
		}
		
		return y;
	}
	
	public boolean shouldRender() {
		if (this.value == null) {
			return false;
		}
		return true;
	}
	
	@Override
	public CustomizationItemBase clone() {
		CustomizationItemBase item = new CustomizationItemBase(new PropertiesSection("")) {
			@Override
			public void render(Screen menu) throws IOException {}
		};
		item.height = this.height;
		item.orientation = this.orientation;
		item.posX = this.posX;
		item.posY = this.posY;
		item.value = this.value;
		item.width = this.width;
		item.action = this.action;
		return item;
	}

}
