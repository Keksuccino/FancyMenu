package de.keksuccino.fancymenu.menu.panorama;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.glu.Project;

import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.properties.PropertiesSerializer;
import de.keksuccino.konkrete.properties.PropertiesSet;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;

public class ExternalTexturePanoramaRenderer extends Gui {
	
	private ExternalTextureResourceLocation overlay_texture;
	private float time;
	private String name = null;
	private String dir;
	private boolean prepared = false;
	private List<ExternalTextureResourceLocation> pano = new ArrayList<ExternalTextureResourceLocation>();
	private float speed = 1.0F;
	private float fov = 85.0F;
	private float angle = 25.0F;
	
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
					if ((fo != null) && MathUtils.isFloat(fo)) {
						this.fov = Float.parseFloat(fo);
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
			this.renderRaw(1.0F);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void renderRaw(float panoramaAlpha) {
		if (this.prepared) {
			this.time += Minecraft.getMinecraft().getRenderPartialTicks() * this.speed;
			
			float pitch = MathHelper.sin(this.time * 0.001F) * 5.0F + this.angle;
			float yaw = -this.time * 0.1F;
			Minecraft mc = Minecraft.getMinecraft();
			Tessellator tessellator = Tessellator.getInstance();
			BufferBuilder bufferbuilder = tessellator.getBuffer();
			
			GlStateManager.matrixMode(5889);
			GlStateManager.pushMatrix();
			GlStateManager.loadIdentity();
//			GlStateManager.multMatrix(Matrix4f.perspective(85.0D, (float)mc.getMainWindow().getFramebufferWidth() / (float)mc.getMainWindow().getFramebufferHeight(), 0.05F, 10.0F));
//			Project.gluPerspective(120.0F, 1.0F, 0.05F, 10.0F);
			Project.gluPerspective(this.fov, (float)mc.getFramebuffer().framebufferWidth / (float)mc.getFramebuffer().framebufferHeight, 0.05F, 10.0F);
			GlStateManager.matrixMode(5888);
			GlStateManager.pushMatrix();
			GlStateManager.loadIdentity();
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
			GlStateManager.enableBlend();
			GlStateManager.disableAlpha();
			GlStateManager.disableCull();
			GlStateManager.depthMask(false);
			GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

			for(int j = 0; j < 4; ++j) {
				GlStateManager.pushMatrix();
				float f = ((float)(j % 2) / 2.0F - 0.5F) / 256.0F;
				float f1 = ((float)(j / 2) / 2.0F - 0.5F) / 256.0F;
				GlStateManager.translate(f, f1, 0.0F);
				GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
				GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);

				for(int k = 0; k < 6; ++k) {
					ExternalTextureResourceLocation r = this.pano.get(k);
					if (r != null) {
						if (!r.isReady()) {
							r.loadTexture();
						}
						mc.getTextureManager().bindTexture(r.getResourceLocation());
						bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
						int l = Math.round(255.0F * panoramaAlpha) / (j + 1);
						if (k == 0) {
							bufferbuilder.pos(-1.0D, -1.0D, 1.0D).tex(0.0F, 0.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.pos(-1.0D, 1.0D, 1.0D).tex(0.0F, 1.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.pos(1.0D, 1.0D, 1.0D).tex(1.0F, 1.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.pos(1.0D, -1.0D, 1.0D).tex(1.0F, 0.0F).color(255, 255, 255, l).endVertex();
						}

						if (k == 1) {
							bufferbuilder.pos(1.0D, -1.0D, 1.0D).tex(0.0F, 0.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.pos(1.0D, 1.0D, 1.0D).tex(0.0F, 1.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.pos(1.0D, 1.0D, -1.0D).tex(1.0F, 1.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.pos(1.0D, -1.0D, -1.0D).tex(1.0F, 0.0F).color(255, 255, 255, l).endVertex();
						}

						if (k == 2) {
							bufferbuilder.pos(1.0D, -1.0D, -1.0D).tex(0.0F, 0.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.pos(1.0D, 1.0D, -1.0D).tex(0.0F, 1.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.pos(-1.0D, 1.0D, -1.0D).tex(1.0F, 1.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.pos(-1.0D, -1.0D, -1.0D).tex(1.0F, 0.0F).color(255, 255, 255, l).endVertex();
						}

						if (k == 3) {
							bufferbuilder.pos(-1.0D, -1.0D, -1.0D).tex(0.0F, 0.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.pos(-1.0D, 1.0D, -1.0D).tex(0.0F, 1.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.pos(-1.0D, 1.0D, 1.0D).tex(1.0F, 1.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.pos(-1.0D, -1.0D, 1.0D).tex(1.0F, 0.0F).color(255, 255, 255, l).endVertex();
						}

						if (k == 4) {
							bufferbuilder.pos(-1.0D, -1.0D, -1.0D).tex(0.0F, 0.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.pos(-1.0D, -1.0D, 1.0D).tex(0.0F, 1.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.pos(1.0D, -1.0D, 1.0D).tex(1.0F, 1.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.pos(1.0D, -1.0D, -1.0D).tex(1.0F, 0.0F).color(255, 255, 255, l).endVertex();
						}

						if (k == 5) {
							bufferbuilder.pos(-1.0D, 1.0D, 1.0D).tex(0.0F, 0.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.pos(-1.0D, 1.0D, -1.0D).tex(0.0F, 1.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.pos(1.0D, 1.0D, -1.0D).tex(1.0F, 1.0F).color(255, 255, 255, l).endVertex();
							bufferbuilder.pos(1.0D, 1.0D, 1.0D).tex(1.0F, 0.0F).color(255, 255, 255, l).endVertex();
						}

						tessellator.draw();
					}
				}

				GlStateManager.popMatrix();
				GlStateManager.colorMask(true, true, true, false);
			}

			GlStateManager.colorMask(true, true, true, true);
			GlStateManager.matrixMode(5889);
			GlStateManager.popMatrix();
			GlStateManager.matrixMode(5888);
			GlStateManager.popMatrix();
			GlStateManager.depthMask(true);
			GlStateManager.enableCull();
			GlStateManager.enableDepth();
			
			if (this.overlay_texture != null) {
				if (!this.overlay_texture.isReady()) {
					this.overlay_texture.loadTexture();
				}
				Minecraft.getMinecraft().getTextureManager().bindTexture(this.overlay_texture.getResourceLocation());
				drawModalRectWithCustomSizedTexture(0, 0, 0.0F, 0.0F, Minecraft.getMinecraft().currentScreen.width, Minecraft.getMinecraft().currentScreen.height, Minecraft.getMinecraft().currentScreen.width, Minecraft.getMinecraft().currentScreen.height);
			}
			
		}
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

	public void setFov(float fov) {
		if (fov > 179.0F) {
			fov = 179.0F;
		}
		this.fov = fov;
	}

	public void setAngle(float angle) {
		this.angle = angle;
	}

}
