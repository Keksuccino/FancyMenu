package de.keksuccino.fancymenu.menu.panorama;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.properties.PropertiesSerializer;
import de.keksuccino.konkrete.properties.PropertiesSet;
import de.keksuccino.konkrete.rendering.CurrentScreenHandler;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;

public class ExternalTexturePanoramaRenderer extends AbstractGui {
	
	private ExternalTextureResourceLocation overlay_texture;
	private float time;
	private String name = null;
	public String dir;
	private boolean prepared = false;
	private List<ExternalTextureResourceLocation> pano = new ArrayList<ExternalTextureResourceLocation>();
	private float speed = 1.0F;
	private double fov = 85.0D;
	private float angle = 25.0F;
	public float opacity = 1.0F;
	
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
					String sp = l.get(0).getEntryValue("speed");
					if ((sp != null) && MathUtils.isFloat(sp)) {
						this.speed = Float.parseFloat(sp);
					}
					String fo = l.get(0).getEntryValue("fov");
					if ((fo != null) && MathUtils.isDouble(fo)) {
						this.fov = Double.parseDouble(fo);
					}
					String an = l.get(0).getEntryValue("angle");
					if ((an != null) && MathUtils.isFloat(an)) {
						this.angle = Float.parseFloat(an);
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
		try {
			this.renderRaw(this.opacity);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void renderRaw(float panoramaAlpha) {
		if (this.prepared) {
			this.time += Minecraft.getInstance().getDeltaFrameTime() * this.speed;

			float pitch = MathHelper.sin(this.time * 0.001F) * 5.0F + this.angle;
			float yaw = -this.time * 0.1F;
			Minecraft mc = Minecraft.getInstance();
			Tessellator tessellator = Tessellator.getInstance();
			BufferBuilder bufferbuilder = tessellator.getBuilder();
			
			RenderSystem.matrixMode(5889);
			RenderSystem.pushMatrix();
			RenderSystem.loadIdentity();
			RenderSystem.multMatrix(Matrix4f.perspective(this.fov, (float)mc.getWindow().getWidth() / (float)mc.getWindow().getHeight(), 0.05F, 10.0F));
			RenderSystem.matrixMode(5888);
			RenderSystem.pushMatrix();
			RenderSystem.loadIdentity();
			RenderSystem.color4f(1.0f, 1.0f, 1.0f, this.opacity);
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
						mc.getTextureManager().bind(r.getResourceLocation());
						bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
						int l = Math.round(255.0F * panoramaAlpha) / (j + 1);
						if (k == 0) {
							bufferbuilder.vertex(-1.0D, -1.0D, 1.0D).uv(0.0F, 0.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.vertex(-1.0D, 1.0D, 1.0D).uv(0.0F, 1.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.vertex(1.0D, 1.0D, 1.0D).uv(1.0F, 1.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.vertex(1.0D, -1.0D, 1.0D).uv(1.0F, 0.0F).color(255, 255, 255, l).endVertex();
						}

						if (k == 1) {
							bufferbuilder.vertex(1.0D, -1.0D, 1.0D).uv(0.0F, 0.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.vertex(1.0D, 1.0D, 1.0D).uv(0.0F, 1.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.vertex(1.0D, 1.0D, -1.0D).uv(1.0F, 1.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.vertex(1.0D, -1.0D, -1.0D).uv(1.0F, 0.0F).color(255, 255, 255, l).endVertex();
						}

						if (k == 2) {
							bufferbuilder.vertex(1.0D, -1.0D, -1.0D).uv(0.0F, 0.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.vertex(1.0D, 1.0D, -1.0D).uv(0.0F, 1.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.vertex(-1.0D, 1.0D, -1.0D).uv(1.0F, 1.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.vertex(-1.0D, -1.0D, -1.0D).uv(1.0F, 0.0F).color(255, 255, 255, l).endVertex();
						}

						if (k == 3) {
							bufferbuilder.vertex(-1.0D, -1.0D, -1.0D).uv(0.0F, 0.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.vertex(-1.0D, 1.0D, -1.0D).uv(0.0F, 1.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.vertex(-1.0D, 1.0D, 1.0D).uv(1.0F, 1.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.vertex(-1.0D, -1.0D, 1.0D).uv(1.0F, 0.0F).color(255, 255, 255, l).endVertex();
						}

						if (k == 4) {
							bufferbuilder.vertex(-1.0D, -1.0D, -1.0D).uv(0.0F, 0.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.vertex(-1.0D, -1.0D, 1.0D).uv(0.0F, 1.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.vertex(1.0D, -1.0D, 1.0D).uv(1.0F, 1.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.vertex(1.0D, -1.0D, -1.0D).uv(1.0F, 0.0F).color(255, 255, 255, l).endVertex();
						}

						if (k == 5) {
							bufferbuilder.vertex(-1.0D, 1.0D, 1.0D).uv(0.0F, 0.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.vertex(-1.0D, 1.0D, -1.0D).uv(0.0F, 1.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.vertex(1.0D, 1.0D, -1.0D).uv(1.0F, 1.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.vertex(1.0D, 1.0D, 1.0D).uv(1.0F, 0.0F).color(255, 255, 255, l).endVertex();
						}

						tessellator.end();
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
				Minecraft.getInstance().getTextureManager().bind(this.overlay_texture.getResourceLocation());
				RenderSystem.color4f(1.0F, 1.0F, 1.0F, this.opacity);
				blit(CurrentScreenHandler.getMatrixStack(), 0, 0, 0.0F, 0.0F, Minecraft.getInstance().screen.width, Minecraft.getInstance().screen.height, Minecraft.getInstance().screen.width, Minecraft.getInstance().screen.height);
			}
		}
		RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0F);
	}
	
	public String getName() {
		return this.name;
	}

	public void setSpeed(float speed) {
		if (speed < 0.0F) {
			speed = 0.0F;
		}
		this.speed = speed;
	}

	public void setFov(double fov) {
		if (fov > 179.0D) {
			fov = 179.0D;
		}
		this.fov = fov;
	}

	public void setAngle(float angle) {
		this.angle = angle;
	}

}
