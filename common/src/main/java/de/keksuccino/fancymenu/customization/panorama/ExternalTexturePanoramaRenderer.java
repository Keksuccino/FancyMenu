package de.keksuccino.fancymenu.customization.panorama;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertiesParser;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;

//TODO rewrite this at some point

@SuppressWarnings("all")
public class ExternalTexturePanoramaRenderer {

	private static final Logger LOGGER = LogManager.getLogger();

	private ExternalTextureResourceLocation overlay_texture;
	private float time;
	private String name = null;
	public String dir;
	private boolean prepared = false;
	private final List<ExternalTextureResourceLocation> panoramaImageLocations = new ArrayList<>();
	private float speed = 1.0F;
	private double fov = 85.0D;
	private float angle = 25.0F;
	private final Minecraft mc = Minecraft.getInstance();

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

			PropertyContainerSet s = PropertiesParser.deserializeSetFromFile(props.getPath());

			if (s != null) {
				List<PropertyContainer> l = s.getContainersOfType("panorama-meta");
				if ((l != null) && !l.isEmpty()) {
					this.name = l.get(0).getValue("name");
					if (this.name == null) {
						LOGGER.error("[FANCYMENU] Unable to load panorama! Missing 'name' value in properties file: " + this.dir);
					}
					String sp = l.get(0).getValue("speed");
					if ((sp != null) && MathUtils.isFloat(sp)) {
						this.speed = Float.parseFloat(sp);
					}
					String fo = l.get(0).getValue("fov");
					if ((fo != null) && MathUtils.isDouble(fo)) {
						this.fov = Double.parseDouble(fo);
					}
					String an = l.get(0).getValue("angle");
					if ((an != null) && MathUtils.isFloat(an)) {
						this.angle = Float.parseFloat(an);
					}
				} else {
					LOGGER.error("[FANCYMENU] Unable to load panorama! Missing 'panorama-meta' section in properties file: " + this.dir);
				}
			} else {
				LOGGER.error("[FANCYMENU] Unable to load panorama! PropertyContainerSet was NULL: " + this.dir);
			}

		} else {
			LOGGER.error("[FANCYMENU] Unable to load panorama! Properties file not found: " + this.dir);
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
						this.panoramaImageLocations.add(r);

					} else {
						LOGGER.error("[FANCYMENU] Unable to load panorama! Missing panorama image 'panorama_" + i + ".png': " + this.name);
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

	public void render(GuiGraphics graphics) {
		try {
			
			this.renderRaw(graphics, this.opacity);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void renderRaw(GuiGraphics graphics, float panoramaAlpha) {
		if (this.prepared) {

			this.time += Minecraft.getInstance().getDeltaFrameTime() * this.speed;

			float pitch = Mth.sin(this.time * 0.001F) * 5.0F + this.angle;
			float yaw = -this.time * 0.1F;
			float fovF = ((float)this.fov * ((float)Math.PI / 180));

			Tesselator tesselator = Tesselator.getInstance();
			BufferBuilder bufferBuilder = tesselator.getBuilder();
			Matrix4f matrix4f = new Matrix4f().setPerspective(fovF, (float)mc.getWindow().getWidth() / (float)mc.getWindow().getHeight(), 0.05F, 10.0F);
			RenderSystem.backupProjectionMatrix();
			RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.DISTANCE_TO_ORIGIN);
			PoseStack poseStack = RenderSystem.getModelViewStack();
			poseStack.pushPose();
			poseStack.setIdentity();
			poseStack.mulPose(Axis.XP.rotationDegrees(180.0f));
			RenderSystem.applyModelViewMatrix();
			RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
			
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, this.opacity);
			RenderSystem.enableBlend();
			RenderSystem.disableCull();
			RenderSystem.depthMask(false);
			RenderSystem.defaultBlendFunc();

			for(int j = 0; j < 4; ++j) {
				poseStack.pushPose();
				float k = ((float)(j % 2) / 2.0f - 0.5f) / 256.0f;
				float l = ((float)(j / 2) / 2.0f - 0.5f) / 256.0f;
				poseStack.translate(k, l, 0.0f);
				poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
				poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
				RenderSystem.applyModelViewMatrix();
				for (int n = 0; n < 6; ++n) {
					ExternalTextureResourceLocation r = this.panoramaImageLocations.get(n);
					if (r != null) {
						if (!r.isReady()) {
							r.loadTexture();
						}
						RenderSystem.setShaderTexture(0, r.getResourceLocation());
						bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
						int o = Math.round(255.0F * panoramaAlpha) / (j + 1);
						if (n == 0) {
							bufferBuilder.vertex(-1.0, -1.0, 1.0).uv(0.0f, 0.0f).color(255, 255, 255, o).endVertex();
							bufferBuilder.vertex(-1.0, 1.0, 1.0).uv(0.0f, 1.0f).color(255, 255, 255, o).endVertex();
							bufferBuilder.vertex(1.0, 1.0, 1.0).uv(1.0f, 1.0f).color(255, 255, 255, o).endVertex();
							bufferBuilder.vertex(1.0, -1.0, 1.0).uv(1.0f, 0.0f).color(255, 255, 255, o).endVertex();
						}
						if (n == 1) {
							bufferBuilder.vertex(1.0, -1.0, 1.0).uv(0.0f, 0.0f).color(255, 255, 255, o).endVertex();
							bufferBuilder.vertex(1.0, 1.0, 1.0).uv(0.0f, 1.0f).color(255, 255, 255, o).endVertex();
							bufferBuilder.vertex(1.0, 1.0, -1.0).uv(1.0f, 1.0f).color(255, 255, 255, o).endVertex();
							bufferBuilder.vertex(1.0, -1.0, -1.0).uv(1.0f, 0.0f).color(255, 255, 255, o).endVertex();
						}
						if (n == 2) {
							bufferBuilder.vertex(1.0, -1.0, -1.0).uv(0.0f, 0.0f).color(255, 255, 255, o).endVertex();
							bufferBuilder.vertex(1.0, 1.0, -1.0).uv(0.0f, 1.0f).color(255, 255, 255, o).endVertex();
							bufferBuilder.vertex(-1.0, 1.0, -1.0).uv(1.0f, 1.0f).color(255, 255, 255, o).endVertex();
							bufferBuilder.vertex(-1.0, -1.0, -1.0).uv(1.0f, 0.0f).color(255, 255, 255, o).endVertex();
						}
						if (n == 3) {
							bufferBuilder.vertex(-1.0, -1.0, -1.0).uv(0.0f, 0.0f).color(255, 255, 255, o).endVertex();
							bufferBuilder.vertex(-1.0, 1.0, -1.0).uv(0.0f, 1.0f).color(255, 255, 255, o).endVertex();
							bufferBuilder.vertex(-1.0, 1.0, 1.0).uv(1.0f, 1.0f).color(255, 255, 255, o).endVertex();
							bufferBuilder.vertex(-1.0, -1.0, 1.0).uv(1.0f, 0.0f).color(255, 255, 255, o).endVertex();
						}
						if (n == 4) {
							bufferBuilder.vertex(-1.0, -1.0, -1.0).uv(0.0f, 0.0f).color(255, 255, 255, o).endVertex();
							bufferBuilder.vertex(-1.0, -1.0, 1.0).uv(0.0f, 1.0f).color(255, 255, 255, o).endVertex();
							bufferBuilder.vertex(1.0, -1.0, 1.0).uv(1.0f, 1.0f).color(255, 255, 255, o).endVertex();
							bufferBuilder.vertex(1.0, -1.0, -1.0).uv(1.0f, 0.0f).color(255, 255, 255, o).endVertex();
						}
						if (n == 5) {
							bufferBuilder.vertex(-1.0, 1.0, 1.0).uv(0.0f, 0.0f).color(255, 255, 255, o).endVertex();
							bufferBuilder.vertex(-1.0, 1.0, -1.0).uv(0.0f, 1.0f).color(255, 255, 255, o).endVertex();
							bufferBuilder.vertex(1.0, 1.0, -1.0).uv(1.0f, 1.0f).color(255, 255, 255, o).endVertex();
							bufferBuilder.vertex(1.0, 1.0, 1.0).uv(1.0f, 0.0f).color(255, 255, 255, o).endVertex();
						}

						tesselator.end();
					}
				}

				poseStack.popPose();
				RenderSystem.applyModelViewMatrix();
				RenderSystem.colorMask(true, true, true, false);
			}

			RenderSystem.colorMask(true, true, true, true);
			RenderSystem.restoreProjectionMatrix();
			poseStack.popPose();
			RenderSystem.applyModelViewMatrix();
			RenderSystem.depthMask(true);
			RenderSystem.enableCull();
			RenderSystem.enableDepthTest();

			if (this.overlay_texture != null) {
				if (!this.overlay_texture.isReady()) {
					this.overlay_texture.loadTexture();
				}
				graphics.setColor(1.0F, 1.0F, 1.0F, this.opacity);
				RenderSystem.enableBlend();
				graphics.blit(this.overlay_texture.getResourceLocation(), 0, 0, 0.0F, 0.0F, Minecraft.getInstance().screen.width, Minecraft.getInstance().screen.height, Minecraft.getInstance().screen.width, Minecraft.getInstance().screen.height);
			}

		}

		RenderingUtils.resetShaderColor(graphics);
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

	public boolean isReady() {
		return this.prepared;
	}

}
