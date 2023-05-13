package de.keksuccino.fancymenu.customization.element.v1;

import java.io.IOException;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.properties.PropertyContainer;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.gui.screens.Screen;

public class AnimationCustomizationItem extends AbstractElement {

	public IAnimationRenderer renderer = null;
	
	public AnimationCustomizationItem(PropertyContainer item) {
		super(item);
		
		if ((this.elementType != null) && this.elementType.equalsIgnoreCase("addanimation")) {
			this.value = item.getValue("name");
			if ((this.value != null) && AnimationHandler.animationExists(this.value)) {
				this.renderer = AnimationHandler.getAnimation(this.value);
			} else {
				System.out.println("################################ WARNING ################################");
				System.out.println("ANIMATION NOT FOUND: " + this.value);
			}
		}
	}

	public void render(PoseStack matrix, Screen menu) throws IOException {
		if (!this.shouldRender()) {
			return;
		}

		int x = this.getX(menu);
		int y = this.getY(menu);
		
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
			
			this.renderer.render(matrix);
			
			this.renderer.setPosX(cachedX);
			this.renderer.setPosY(cachedY);
			this.renderer.setWidth(cachedWidth);
			this.renderer.setHeight(cachedHeight);
		}
	}

	@Override
	public AnimationCustomizationItem clone() {
		AnimationCustomizationItem item = new AnimationCustomizationItem(new PropertyContainer(""));
		item.setHeight(this.getHeight());
		item.anchorPoint = this.anchorPoint;
		item.baseX = this.baseX;
		item.baseY = this.baseY;
		item.renderer = this.renderer;
		item.value = this.value;
		item.setWidth(this.getWidth());
		item.elementType = this.elementType;
		return item;
	}

}
