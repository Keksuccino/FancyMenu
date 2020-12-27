package de.keksuccino.fancymenu.menu.panorama;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.properties.PropertiesSerializer;
import de.keksuccino.konkrete.properties.PropertiesSet;
import de.keksuccino.konkrete.rendering.CurrentScreenHandler;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;

public class ExternalTexturePanoramaRenderer extends DrawableHelper {
	
	private ExternalTextureResourceLocation overlay_texture;
	private float time;
	private String name = null;
	private String dir;
	private boolean prepared = false;
	private List<ExternalTextureResourceLocation> pano = new ArrayList<ExternalTextureResourceLocation>();
	
	/**
	 * Loads a panorama cube from a directory containing:<br>
	 *   - A folder named 'panorama' containing the 6 panorama cube images<br>
	 *   - A properties file named 'properties.txt', containing the name of the panorama cube
	 */
	public ExternalTexturePanoramaRenderer(String panoDir) {
		this.dir = panoDir;
		File props = new File(this.dir + "/properties.txt");
		
		if (props.exists()) {
			
			PropertiesSet s = PropertiesSerializer.getProperties(props.getPath());
			
			if (s != null) {
				List<PropertiesSection> l = s.getPropertiesOfType("panorama-meta");
				if ((l != null) && !l.isEmpty()) {
					this.name = l.get(0).getEntryValue("name");
					if (this.name == null) {
						System.out.println("############## ERROR [FANCYMENU] ##############");
						System.out.println("Missing 'name' value in properties file for panorama cube: " + this.dir);
						System.out.println("###############################################");
					}
				} else {
					System.out.println("############## ERROR [FANCYMENU] ##############");
					System.out.println("Missing 'panorama-meta' section in properties file for panorama cube: " + this.dir);
					System.out.println("###############################################");
				}
			} else {
				System.out.println("############## ERROR [FANCYMENU] ##############");
				System.out.println("An error happened while trying to get properties for panorama cube: " + this.dir);
				System.out.println("###############################################");
			}
			
		} else {
			System.out.println("############## ERROR [FANCYMENU] ##############");
			System.out.println("Properties file not found for panorama cube: " + this.dir);
			System.out.println("###############################################");
		}
	}
	
	public void preparePanorama() {
		if (!this.prepared && (this.name != null)) {
			
			File imagesDir = new File(this.dir + "/panorama");
			
			if (imagesDir.exists() && imagesDir.isDirectory()) {
				
				int i = 0;
				while (i < 6) {
					File f = new File(this.dir + "/panorama/panorama_" + i + ".png");
					if (f.exists() && f.isFile()) {
						ExternalTextureResourceLocation r = new ExternalTextureResourceLocation(f.getPath());
						this.pano.add(r);
						
					} else {
						System.out.println("############## ERROR [FANCYMENU] ##############");
						System.out.println("Missing panorama image 'panorama_" + i + ".png' for panorama cube: " + this.name);
						System.out.println("###############################################");
						return;
					}
					i++;
				}
				
				File overlay = new File(this.dir + "/overlay.png");
				if (overlay.exists()) {
					this.overlay_texture = new ExternalTextureResourceLocation(overlay.getPath());
				}
				
				this.prepared = true;
				
			}
			
		}
	}

	public void render() {
		if (this.prepared) {
			this.time += MinecraftClient.getInstance().getTickDelta();
			
			float pitch = MathHelper.sin(this.time * 0.001F) * 5.0F + 25.0F;
			float yaw = -this.time * 0.1F;
			float alpha = 1.0F;
			MinecraftClient mc = MinecraftClient.getInstance();
			Tessellator tessellator = Tessellator.getInstance();
			BufferBuilder bufferbuilder = tessellator.getBuffer();
			
			RenderSystem.matrixMode(5889);
			RenderSystem.pushMatrix();
			RenderSystem.loadIdentity();
			RenderSystem.multMatrix(Matrix4f.viewboxMatrix(85.0D, (float)mc.getWindow().getFramebufferWidth() / (float)mc.getWindow().getFramebufferHeight(), 0.05F, 10.0F));
			RenderSystem.matrixMode(5888);
			RenderSystem.pushMatrix();
			RenderSystem.loadIdentity();
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.rotatef(180.0F, 1.0F, 0.0F, 0.0F);
			RenderSystem.enableBlend();
			RenderSystem.disableAlphaTest();
			RenderSystem.disableCull();
			RenderSystem.depthMask(false);
			RenderSystem.defaultBlendFunc();

			for(int j = 0; j < 4; ++j) {
				RenderSystem.pushMatrix();
				float f = ((float)(j % 2) / 2.0F - 0.5F) / 256.0F;
				float f1 = ((float)(j / 2) / 2.0F - 0.5F) / 256.0F;
				RenderSystem.translatef(f, f1, 0.0F);
				RenderSystem.rotatef(pitch, 1.0F, 0.0F, 0.0F);
				RenderSystem.rotatef(yaw, 0.0F, 1.0F, 0.0F);

				for(int k = 0; k < 6; ++k) {
					ExternalTextureResourceLocation r = this.pano.get(k);
					if (r != null) {
						if (!r.isReady()) {
							r.loadTexture();
						}
						mc.getTextureManager().bindTexture(r.getResourceLocation());
						bufferbuilder.begin(7, VertexFormats.POSITION_TEXTURE_COLOR);
						int l = Math.round(255.0F * alpha) / (j + 1);
						if (k == 0) {
							bufferbuilder.vertex(-1.0D, -1.0D, 1.0D).texture(0.0F, 0.0F).color(255, 255, 255, l).next();
							bufferbuilder.vertex(-1.0D, 1.0D, 1.0D).texture(0.0F, 1.0F).color(255, 255, 255, l).next();
							bufferbuilder.vertex(1.0D, 1.0D, 1.0D).texture(1.0F, 1.0F).color(255, 255, 255, l).next();
							bufferbuilder.vertex(1.0D, -1.0D, 1.0D).texture(1.0F, 0.0F).color(255, 255, 255, l).next();
						}

						if (k == 1) {
							bufferbuilder.vertex(1.0D, -1.0D, 1.0D).texture(0.0F, 0.0F).color(255, 255, 255, l).next();
							bufferbuilder.vertex(1.0D, 1.0D, 1.0D).texture(0.0F, 1.0F).color(255, 255, 255, l).next();
							bufferbuilder.vertex(1.0D, 1.0D, -1.0D).texture(1.0F, 1.0F).color(255, 255, 255, l).next();
							bufferbuilder.vertex(1.0D, -1.0D, -1.0D).texture(1.0F, 0.0F).color(255, 255, 255, l).next();
						}

						if (k == 2) {
							bufferbuilder.vertex(1.0D, -1.0D, -1.0D).texture(0.0F, 0.0F).color(255, 255, 255, l).next();
							bufferbuilder.vertex(1.0D, 1.0D, -1.0D).texture(0.0F, 1.0F).color(255, 255, 255, l).next();
							bufferbuilder.vertex(-1.0D, 1.0D, -1.0D).texture(1.0F, 1.0F).color(255, 255, 255, l).next();
							bufferbuilder.vertex(-1.0D, -1.0D, -1.0D).texture(1.0F, 0.0F).color(255, 255, 255, l).next();
						}

						if (k == 3) {
							bufferbuilder.vertex(-1.0D, -1.0D, -1.0D).texture(0.0F, 0.0F).color(255, 255, 255, l).next();
							bufferbuilder.vertex(-1.0D, 1.0D, -1.0D).texture(0.0F, 1.0F).color(255, 255, 255, l).next();
							bufferbuilder.vertex(-1.0D, 1.0D, 1.0D).texture(1.0F, 1.0F).color(255, 255, 255, l).next();
							bufferbuilder.vertex(-1.0D, -1.0D, 1.0D).texture(1.0F, 0.0F).color(255, 255, 255, l).next();
						}

						if (k == 4) {
							bufferbuilder.vertex(-1.0D, -1.0D, -1.0D).texture(0.0F, 0.0F).color(255, 255, 255, l).next();
							bufferbuilder.vertex(-1.0D, -1.0D, 1.0D).texture(0.0F, 1.0F).color(255, 255, 255, l).next();
							bufferbuilder.vertex(1.0D, -1.0D, 1.0D).texture(1.0F, 1.0F).color(255, 255, 255, l).next();
							bufferbuilder.vertex(1.0D, -1.0D, -1.0D).texture(1.0F, 0.0F).color(255, 255, 255, l).next();
						}

						if (k == 5) {
							bufferbuilder.vertex(-1.0D, 1.0D, 1.0D).texture(0.0F, 0.0F).color(255, 255, 255, l).next();
							bufferbuilder.vertex(-1.0D, 1.0D, -1.0D).texture(0.0F, 1.0F).color(255, 255, 255, l).next();
							bufferbuilder.vertex(1.0D, 1.0D, -1.0D).texture(1.0F, 1.0F).color(255, 255, 255, l).next();
							bufferbuilder.vertex(1.0D, 1.0D, 1.0D).texture(1.0F, 0.0F).color(255, 255, 255, l).next();
						}

						tessellator.draw();
					}
				}

				RenderSystem.popMatrix();
				RenderSystem.colorMask(true, true, true, false);
			}

			RenderSystem.colorMask(true, true, true, true);
			RenderSystem.matrixMode(5889);
			RenderSystem.popMatrix();
			RenderSystem.matrixMode(5888);
			RenderSystem.popMatrix();
			RenderSystem.depthMask(true);
			RenderSystem.enableCull();
			RenderSystem.enableDepthTest();
			
			if (this.overlay_texture != null) {
				if (!this.overlay_texture.isReady()) {
					this.overlay_texture.loadTexture();
				}
				MinecraftClient.getInstance().getTextureManager().bindTexture(this.overlay_texture.getResourceLocation());
				drawTexture(CurrentScreenHandler.getMatrixStack(), 0, 0, 0.0F, 0.0F, MinecraftClient.getInstance().currentScreen.width, MinecraftClient.getInstance().currentScreen.height, MinecraftClient.getInstance().currentScreen.width, MinecraftClient.getInstance().currentScreen.height);
			}
			
		}
	}
	
	public String getName() {
		return this.name;
	}

}
