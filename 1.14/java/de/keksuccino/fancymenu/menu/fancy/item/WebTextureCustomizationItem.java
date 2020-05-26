package de.keksuccino.fancymenu.menu.fancy.item;

import java.io.IOException;

import com.mojang.blaze3d.platform.GlStateManager;

import de.keksuccino.core.properties.PropertiesSection;
import de.keksuccino.core.resources.TextureHandler;
import de.keksuccino.core.resources.WebTextureResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.client.gui.screen.Screen;

public class WebTextureCustomizationItem extends CustomizationItemBase {
	
	public WebTextureResourceLocation texture;
	
	public WebTextureCustomizationItem(PropertiesSection item) {
		super(item);
		
		if ((this.action != null) && this.action.equalsIgnoreCase("addwebtexture")) {
			this.value = item.getEntryValue("url");
			if (this.value != null) {
				try {
					try {
						this.texture = TextureHandler.getWebResource(this.value);

						if ((this.texture == null) || !this.texture.isReady()) {
							return;
						}
						
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
				} catch (Exception e1) {
					e1.printStackTrace();
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
