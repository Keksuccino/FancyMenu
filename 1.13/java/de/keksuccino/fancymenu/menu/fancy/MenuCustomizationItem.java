package de.keksuccino.fancymenu.menu.fancy;

import java.io.IOException;

import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.math.MathUtils;
import de.keksuccino.properties.PropertiesSection;
import de.keksuccino.rendering.RenderUtils;
import de.keksuccino.rendering.animation.ExternalTextureAnimationRenderer;
import de.keksuccino.rendering.animation.IAnimationRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;

public class MenuCustomizationItem {
	
	private String value;
	public final Type type;
	private int posX = 0;
	private int posY = 0;
	private String orientation = "top-left";
	private int width = -1;
	private int height = -1;
	
	//Only used if item is a string
	private float scale = 1.0F;
	private boolean shadow = false;
	private boolean centered = false;
	
	//Only used if item is an animation or a texture
	private IAnimationRenderer renderer = null;
	
	public MenuCustomizationItem(PropertiesSection item, Type type) {
		this.type = type;

		String x = item.getEntryValue("x");
		String y = item.getEntryValue("y");
		if ((x != null) && MathUtils.isInteger(x)) {
			this.posX = Integer.parseInt(x);
		}
		if ((y != null) && MathUtils.isInteger(y)) {
			this.posY = Integer.parseInt(y);
		}
	
		String o = item.getEntryValue("orientation");
		if (o != null) {
			this.orientation = o;
		}
		
		String w = item.getEntryValue("width");
		if ((w != null) && MathUtils.isInteger(w)) {
			this.width = Integer.parseInt(w);
		}
		
		String h = item.getEntryValue("height");
		if ((h != null) && MathUtils.isInteger(h)) {
			this.height = Integer.parseInt(h);
		}

		if (type == Type.STRING) {
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
		
		if (type == Type.TEXTURE) {
			this.value = item.getEntryValue("path");
			if (this.value != null) {
				//Yes, this is retarded, but it saves me from writing more code than needed and looks cleaner ( ͡° ͜ʖ ͡°)
				this.renderer = new ExternalTextureAnimationRenderer(1, false, 0, 0, 20, 20, this.value);
			}
		}
		
		if (type == Type.ANIMATION) {
			this.value = item.getEntryValue("name");
			if ((this.value != null) && AnimationHandler.animationExists(this.value)) {
				this.renderer = AnimationHandler.getAnimation(this.value);
			} else {
				System.out.println("################################ WARNING ################################");
				System.out.println("ANIMATION NOT FOUND: " + this.value);
			}
		}
	}

	public void render(GuiScreen menu) throws IOException {
		if (!this.shouldRender()) {
			return;
		}
		
		int w = menu.width;
		int h = menu.height;
		int x = this.posX;
		int y = this.posY;

		if (orientation.equalsIgnoreCase("mid-left")) {
			y += (h / 2);
		}
		if (orientation.equalsIgnoreCase("bottom-left")) {
			y += h;
		}
		//----------------------------
		if (orientation.equalsIgnoreCase("top-centered")) {
			x += (w / 2);
		}
		if (orientation.equalsIgnoreCase("mid-centered")) {
			x += (w / 2);
			y += (h / 2);
		}
		if (orientation.equalsIgnoreCase("bottom-centered")) {
			x += (w / 2);
			y += h;
		}
		//-----------------------------
		if (orientation.equalsIgnoreCase("top-right")) {
			x += w;
		}
		if (orientation.equalsIgnoreCase("mid-right")) {
			x += w;
			y += (h / 2);
		}
		if (orientation.equalsIgnoreCase("bottom-right")) {
			x += w;
			y += h;
		}
		
		if (this.type == Type.STRING) {
			FontRenderer font = Minecraft.getInstance().fontRenderer;
			if (this.centered) {
				x -= (int) ((font.getStringWidth(this.value) / 2) * this.scale);
			}
			
			x = (int)(x / this.scale);
			y = (int)(y / this.scale);

			RenderUtils.setScale(this.scale);
			if (this.shadow) {
				font.drawStringWithShadow("§f" + this.value, x, y, 0);
			} else {
				font.drawString("§f" + this.value, x, y, 0);
			}
			RenderUtils.postScale();
		}
		
		if (this.type == Type.TEXTURE) {
			if ((this.renderer != null) && !this.renderer.isReady()) {
				this.renderer.prepareAnimation();
			}
		}
		
		if ((this.type == Type.ANIMATION) || (this.type == Type.TEXTURE)) {
			if ((this.renderer != null) && this.renderer.isReady()) {
				int cachedX = this.renderer.getPosX();
				int cachedY = this.renderer.getPosY();
				int cachedWidth = this.renderer.getWidth();
				int cachedHeight = this.renderer.getHeight();
				
				this.renderer.setPosX(x);
				this.renderer.setPosY(y);
				
				if (this.height > -1) {
					this.renderer.setHeight(this.height);
				}
				if (this.width > -1) {
					this.renderer.setWidth(this.width);
				}
				
				this.renderer.render();
				
				this.renderer.setPosX(cachedX);
				this.renderer.setPosY(cachedY);
				this.renderer.setWidth(cachedWidth);
				this.renderer.setHeight(cachedHeight);
			}
		}
	}
	
	private boolean shouldRender() {
		if (this.value == null) {
			return false;
		}
		if (this.type == Type.TEXTURE) {
			if ((this.width < 0) || (this.height < 0)) {
				return false;
			}
		}
		return true;
	}
	
	public static enum Type {
		TEXTURE,
		STRING,
		ANIMATION;
	}

}
