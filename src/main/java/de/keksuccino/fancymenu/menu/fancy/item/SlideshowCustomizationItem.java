package de.keksuccino.fancymenu.menu.fancy.item;

import java.io.IOException;
import net.minecraft.client.gui.screens.Screen;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.slideshow.ExternalTextureSlideshowRenderer;
import de.keksuccino.fancymenu.menu.slideshow.SlideshowHandler;
import de.keksuccino.konkrete.properties.PropertiesSection;

public class SlideshowCustomizationItem extends CustomizationItemBase {

	public ExternalTextureSlideshowRenderer renderer = null;
	
	public SlideshowCustomizationItem(PropertiesSection item) {
		super(item);
		
		if ((this.action != null) && this.action.equalsIgnoreCase("addslideshow")) {
			this.value = item.getEntryValue("name");
			if ((this.value != null) && SlideshowHandler.slideshowExists(this.value)) {
				this.renderer = SlideshowHandler.getSlideshow(this.value);
			} else {
				if (FancyMenu.config.getOrDefault("showdebugwarnings", true)) {
					System.out.println("###################### WARNING [FANCYMENU] ######################");
					System.out.println("SLIDESHOW NOT FOUND: " + this.value);
					System.out.println("#################################################################");
				}
			}
		}
	}

	public void render(PoseStack matrix, Screen menu) throws IOException {
		if (!this.shouldRender()) {
			return;
		}

		int x = this.getPosX(menu);
		int y = this.getPosY(menu);
		
		if ((this.renderer != null) && this.renderer.isReady()) {
			int cachedX = this.renderer.x;
			int cachedY = this.renderer.y;
			int cachedWidth = this.renderer.width;
			int cachedHeight = this.renderer.height;
			
			this.renderer.slideshowOpacity = this.opacity;
			
			this.renderer.x = x;
			this.renderer.y = y;

			if (this.getHeight() > -1) {
				this.renderer.height = this.getHeight();
			}
			if (this.getWidth() > -1) {
				this.renderer.width = this.getWidth();
			}
			
			this.renderer.render(matrix);
			
			this.renderer.x = cachedX;
			this.renderer.y = cachedY;
			this.renderer.width = cachedWidth;
			this.renderer.height = cachedHeight;
		}
	}

}
