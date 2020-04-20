package de.keksuccino.fancymenu.menu.fancy.item;

import java.io.IOException;

import de.keksuccino.core.properties.PropertiesSection;
import de.keksuccino.core.rendering.animation.ExternalTextureAnimationRenderer;
import net.minecraft.client.gui.GuiScreen;

public class TextureCustomizationItem extends AnimationCustomizationItem {
	
	public TextureCustomizationItem(PropertiesSection item) {
		super(item);
		
		if ((this.action != null) && this.action.equalsIgnoreCase("addtexture")) {
			this.value = item.getEntryValue("path");
			if (this.value != null) {
				//Yes, this is retarded, but it saves me from writing more code than needed and looks cleaner ( ͡° ͜ʖ ͡°)
				this.renderer = new ExternalTextureAnimationRenderer(1, false, 0, 0, 20, 20, this.value);
			}
		}
	}

	public void render(GuiScreen menu) throws IOException {
		if ((this.renderer != null) && !this.renderer.isReady()) {
			this.renderer.prepareAnimation();
		}
		super.render(menu);
	}
	
	@Override
	public boolean shouldRender() {
		if ((this.width < 0) || (this.height < 0)) {
			return false;
		}
		return super.shouldRender();
	}
	
	@Override
	public TextureCustomizationItem clone() {
		TextureCustomizationItem item = new TextureCustomizationItem(new PropertiesSection(""));
		item.height = this.height;
		item.orientation = this.orientation;
		item.posX = this.posX;
		item.posY = this.posY;
		item.renderer = this.renderer;
		item.value = this.value;
		item.width = this.width;
		item.action = this.action;
		return item;
	}

}
