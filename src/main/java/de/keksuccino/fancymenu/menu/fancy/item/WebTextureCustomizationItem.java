package de.keksuccino.fancymenu.menu.fancy.item;

import java.io.IOException;


import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.fancymenu.menu.fancy.DynamicValueHelper;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.resources.TextureHandler;
import de.keksuccino.konkrete.resources.WebTextureResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.texture.TextureManager;

public class WebTextureCustomizationItem extends CustomizationItemBase {
	
	public WebTextureResourceLocation texture;
	//TODO übernehmen
	public String rawURL = "";
	
	public WebTextureCustomizationItem(PropertiesSection item) {
		super(item);
		
		if ((this.action != null) && this.action.equalsIgnoreCase("addwebtexture")) {
			this.value = item.getEntryValue("url");
			if (this.value != null) {
				//TODO übernehmen
				this.rawURL = this.value;
				//TODO übernehmen
				this.value = DynamicValueHelper.convertFromRaw(this.value);
				try {
					try {
						this.texture = TextureHandler.getWebResource(this.value);

						if ((this.texture == null) || !this.texture.isReady()) {
							//TODO übernehmen
							this.width = 100;
							this.height = 100;
							//---------------
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
			
			//TODO übernehmen
			if (this.texture != null) {
				Minecraft.getInstance().getTextureManager().bindTexture(this.texture.getResourceLocation());
			} else {
				Minecraft.getInstance().getTextureManager().bindTexture(TextureManager.RESOURCE_LOCATION_EMPTY);
			}
			//------------------
			
			RenderSystem.enableBlend();
			//TODO übernehmen
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, this.opacity);
			IngameGui.blit(x, y, 0.0F, 0.0F, this.width, this.height, this.width, this.height);
			RenderSystem.disableBlend();
		}
	}
	
	@Override
	public boolean shouldRender() {
		//TODO übernehmen
//		if (this.texture == null) {
//			return false;
//		}
		if ((this.width < 0) || (this.height < 0)) {
			return false;
		}
		return super.shouldRender();
	}

}
