package de.keksuccino.fancymenu.menu.fancy.item;

import java.io.IOException;

import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.gui.GuiScreen;

public class AnimationCustomizationItem extends CustomizationItemBase {

	public IAnimationRenderer renderer = null;
	
	public AnimationCustomizationItem(PropertiesSection item) {
		super(item);
		
		if ((this.action != null) && this.action.equalsIgnoreCase("addanimation")) {
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

		int x = this.getPosX(menu);
		int y = this.getPosY(menu);
		
		if ((this.renderer != null) && this.renderer.isReady()) {
			int cachedX = this.renderer.getPosX();
			int cachedY = this.renderer.getPosY();
			int cachedWidth = this.renderer.getWidth();
			int cachedHeight = this.renderer.getHeight();
			
			this.renderer.setOpacity(this.opacity);
			
			this.renderer.setPosX(x);
			this.renderer.setPosY(y);

			if (this.getHeight() > -1) {
				this.renderer.setHeight(this.getHeight());
			}
			if (this.getWidth() > -1) {
				this.renderer.setWidth(this.getWidth());
			}
			
			this.renderer.render();
			
			this.renderer.setPosX(cachedX);
			this.renderer.setPosY(cachedY);
			this.renderer.setWidth(cachedWidth);
			this.renderer.setHeight(cachedHeight);
		}
	}

	@Override
	public AnimationCustomizationItem clone() {
		AnimationCustomizationItem item = new AnimationCustomizationItem(new PropertiesSection(""));
		item.setHeight(this.getHeight());
		item.orientation = this.orientation;
		item.posX = this.posX;
		item.posY = this.posY;
		item.renderer = this.renderer;
		item.value = this.value;
		item.setWidth(this.getWidth());
		item.action = this.action;
		return item;
	}

}
