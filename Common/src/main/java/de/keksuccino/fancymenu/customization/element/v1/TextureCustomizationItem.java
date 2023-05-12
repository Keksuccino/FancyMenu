package de.keksuccino.fancymenu.customization.element.v1;

import java.io.File;
import java.io.IOException;

import com.mojang.blaze3d.systems.RenderSystem;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.rendering.RenderUtils;
import de.keksuccino.konkrete.rendering.animation.ExternalGifAnimationRenderer;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import de.keksuccino.fancymenu.rendering.texture.ExternalTextureHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;

public class TextureCustomizationItem extends AbstractElement {
	
	public ExternalTextureResourceLocation texture;
	public ExternalGifAnimationRenderer gif;
	
	public TextureCustomizationItem(PropertiesSection item) {
		super(item);
		
		if ((this.elementType != null) && this.elementType.equalsIgnoreCase("addtexture")) {
			this.value = fixBackslashPath(item.getEntryValue("path"));
			if (this.value != null) {
				this.value = this.value.replace("\\", "/");
				
				File f = new File(this.value);
				String finalValue = this.value;
				if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
					finalValue = Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + this.value;
					f = new File(finalValue);
				}
				if (f.exists() && f.isFile() && (f.getName().endsWith(".png") || f.getName().endsWith(".jpg") || f.getName().endsWith(".jpeg") || f.getName().endsWith(".gif"))) {
					try {
						int w = 0;
					    int h = 0;
					    double ratio;

						if (f.getName().endsWith(".gif")) {
							this.gif = ExternalTextureHandler.INSTANCE.getGif(finalValue);
							if (this.gif != null) {
								w = this.gif.getWidth();
								h = this.gif.getHeight();
							}
						} else {
							this.texture = ExternalTextureHandler.INSTANCE.getTexture(finalValue);
							if (this.texture != null) {
								w = this.texture.getWidth();
								h = this.texture.getHeight();
							}
						}
						
						ratio = (double) w / (double) h;

					    //Calculate missing width
					    if ((this.getWidth() < 0) && (this.getHeight() >= 0)) {
					    	this.setWidth((int)(this.getHeight() * ratio));
					    }
					    //Calculate missing height
					    if ((this.getHeight() < 0) && (this.getWidth() >= 0)) {
					    	this.setHeight((int)(this.getWidth() / ratio));
					    }
						
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public void render(PoseStack matrix, Screen menu) throws IOException {
		if (this.shouldRender()) {
			
			int x = this.getX(menu);
			int y = this.getY(menu);
			
			if (this.gif != null) {
				int w = this.gif.getWidth();
				int h = this.gif.getHeight();
				int x2 = this.gif.getPosX();
				int y2 = this.gif.getPosY();
				
				RenderSystem.enableBlend();
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.opacity);
				
				this.gif.setPosX(x);
				this.gif.setPosY(y);
				this.gif.setWidth(this.getWidth());
				this.gif.setHeight(this.getHeight());
				this.gif.setOpacity(this.opacity);
				
				this.gif.render(matrix);
				
				this.gif.setPosX(x2);
				this.gif.setPosY(y2);
				this.gif.setWidth(w);
				this.gif.setHeight(h);
				
				RenderSystem.disableBlend();
				
			} else if (this.texture != null) {
				
				RenderUtils.bindTexture(this.texture.getResourceLocation());
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.opacity);
				GuiComponent.blit(matrix, x, y, 0.0F, 0.0F, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());
				RenderSystem.disableBlend();
				
			}
			
		}
	}
	
	@Override
	public boolean shouldRender() {
		if ((this.texture == null) && (this.gif == null)) {
			return false;
		}
		if ((this.getWidth() < 0) || (this.getHeight() < 0)) {
			return false;
		}
		return super.shouldRender();
	}

}