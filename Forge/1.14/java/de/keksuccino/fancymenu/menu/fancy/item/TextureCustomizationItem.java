package de.keksuccino.fancymenu.menu.fancy.item;

import java.io.File;
import java.io.IOException;

import com.mojang.blaze3d.platform.GlStateManager;

import de.keksuccino.core.properties.PropertiesSection;
import de.keksuccino.core.resources.TextureHandler;
import de.keksuccino.core.resources.ExternalTextureResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.client.gui.screen.Screen;

public class TextureCustomizationItem extends CustomizationItemBase {
	
	public ExternalTextureResourceLocation texture;
	
	public TextureCustomizationItem(PropertiesSection item) {
		super(item);
		
		if ((this.action != null) && this.action.equalsIgnoreCase("addtexture")) {
			this.value = item.getEntryValue("path");
			if (this.value != null) {
				this.value = this.value.replace("\\", "/");
				
				File f = new File(this.value);
				if (f.exists() && f.isFile() && (f.getName().endsWith(".png") || f.getName().endsWith(".jpg") || f.getName().endsWith(".jpeg"))) {
					try {
						this.texture = TextureHandler.getResource(this.value);
						
						int w = this.texture.getWidth();
						int h = this.texture.getHeight();
						double ratio = (double) w / (double) h;

						//Calculate missing width
						if ((this.width < 0) && (this.height >= 0)) {
							this.width = (int)(this.height * ratio);
						}
						//Calculate missing height
						if ((this.height < 0) && (this.width >= 0)) {
							this.height = (int)(this.width / ratio);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public void render(Screen menu) throws IOException {
		if (this.shouldRender()) {
			
			int x = this.getPosX(menu);
			int y = this.getPosY(menu);
			
			Minecraft.getInstance().getTextureManager().bindTexture(this.texture.getResourceLocation());
			GlStateManager.enableBlend();
			GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			IngameGui.blit(x, y, 0.0F, 0.0F, this.width, this.height, this.width, this.height);
			GlStateManager.disableBlend();
		}
	}
	
	@Override
	public boolean shouldRender() {
		if (this.texture == null) {
			return false;
		}
		if ((this.width < 0) || (this.height < 0)) {
			return false;
		}
		return super.shouldRender();
	}

}
