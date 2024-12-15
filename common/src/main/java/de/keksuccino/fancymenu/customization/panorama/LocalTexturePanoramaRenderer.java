package de.keksuccino.fancymenu.customization.panorama;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import de.keksuccino.fancymenu.util.resource.ResourceSourceType;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertiesParser;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

@SuppressWarnings("unused")
public class LocalTexturePanoramaRenderer implements Renderable {

	private static final Logger LOGGER = LogManager.getLogger();

	@NotNull
	public File propertiesFile;
	@NotNull
	public File panoramaImageDir;
	@Nullable
	public File overlayImageFile;
	protected String name = null;
	public final List<ResourceSupplier<ITexture>> panoramaImageSuppliers = new ArrayList<>();
	@Nullable
	public ResourceSupplier<ITexture> overlayTextureSupplier;
	protected float speed = 1.0F;
	protected double fov = 85.0D;
	protected float angle = 25.0F;
	public float opacity = 1.0F;
	protected volatile boolean tickerThreadRunning = false;
	protected volatile float currentRotation = 0.0F; //0 - 360
	protected volatile long lastRenderCall = -1L;

	@Nullable
	public static LocalTexturePanoramaRenderer build(@NotNull File propertiesFile, @NotNull File panoramaImageDir, @Nullable File overlayImageFile) {
		LocalTexturePanoramaRenderer renderer = new LocalTexturePanoramaRenderer(propertiesFile, panoramaImageDir, overlayImageFile);
		try {
			if (renderer.propertiesFile.isFile() && renderer.panoramaImageDir.isDirectory()) {
				PropertyContainerSet panoProperties = PropertiesParser.deserializeSetFromFile(renderer.propertiesFile.getAbsolutePath());
				if (panoProperties != null) {
					PropertyContainer panoMeta = panoProperties.getFirstContainerOfType("panorama-meta");
					if (panoMeta != null) {
						renderer.name = panoMeta.getValue("name");
						if (renderer.name == null) {
							LOGGER.error("[FANCYMENU] Unable to load panorama! Missing 'name' value in panorama meta section: " + renderer.propertiesFile.getAbsolutePath(), new NullPointerException());
							return null;
						}
						String sp = panoMeta.getValue("speed");
						if ((sp != null) && MathUtils.isFloat(sp)) {
							renderer.speed = Float.parseFloat(sp);
						}
						String fo = panoMeta.getValue("fov");
						if ((fo != null) && MathUtils.isDouble(fo)) {
							renderer.fov = Double.parseDouble(fo);
						}
						String an = panoMeta.getValue("angle");
						if ((an != null) && MathUtils.isFloat(an)) {
							renderer.angle = Float.parseFloat(an);
						}
						String rot = panoMeta.getValue("start_rotation");
						if ((rot != null) && MathUtils.isFloat(rot)) {
							renderer.currentRotation = Float.parseFloat(rot);
							if ((renderer.currentRotation > 360.0F) || (renderer.currentRotation < 0.0F)) {
								renderer.currentRotation = 0;
							}
						}
						renderer.prepare();
						return renderer;
					} else {
						LOGGER.error("[FANCYMENU] Unable to load panorama! Missing 'panorama-meta' section in properties instance: " + renderer.propertiesFile.getAbsolutePath(), new NullPointerException());
					}
				} else {
					LOGGER.error("[FANCYMENU] Unable to load panorama! Parsed properties instance was NULL: " + renderer.propertiesFile.getAbsolutePath(), new NullPointerException());
				}
			}
		} catch (Exception ex) {
			LOGGER.error("[FANCYMENU] An error happened while trying to build a cubic panorama!", ex);
		}
		return null;
	}

	protected LocalTexturePanoramaRenderer(@NotNull File propertiesFile, @NotNull File panoramaImageDir, @Nullable File overlayImageFile) {
		this.propertiesFile = Objects.requireNonNull(propertiesFile);
		this.panoramaImageDir = Objects.requireNonNull(panoramaImageDir);
		this.overlayImageFile = overlayImageFile;
	}

	protected void prepare() {

		this.panoramaImageSuppliers.clear();
		this.overlayTextureSupplier = null;

		for (int i = 0; i < 6; i++) {
			File panoImage = new File(this.panoramaImageDir, "panorama_" + i + ".png");
			if (panoImage.isFile()) {
				this.panoramaImageSuppliers.add(ResourceSupplier.image(ResourceSource.of(panoImage.getAbsolutePath(), ResourceSourceType.LOCAL).getSourceWithPrefix()));
			} else {
				LOGGER.error("[FANCYMENU] Unable to load panorama! Missing panorama image 'panorama_" + i + ".png': " + this.name);
				return;
			}
		}

		if ((this.overlayImageFile != null) && this.overlayImageFile.isFile()) {
			this.panoramaImageSuppliers.add(ResourceSupplier.image(ResourceSource.of(this.overlayImageFile.getAbsolutePath(), ResourceSourceType.LOCAL).getSourceWithPrefix()));
		}

	}

	@SuppressWarnings("all")
	protected void startTickerThreadIfNeeded() {

		if (this.tickerThreadRunning) return;

		this.lastRenderCall = System.currentTimeMillis();
		this.tickerThreadRunning = true;

		new Thread(() -> {
			while ((this.lastRenderCall + 5000L) > System.currentTimeMillis()) {
				try {
					this.currentRotation += 0.03F;
					if (this.currentRotation >= 360) {
						this.currentRotation = 0;
					}
				} catch (Exception ex) {
					LOGGER.error("[FANCYMENU] Error while ticking panorama!", ex);
				}
				try {
					Thread.sleep(Math.max(2, (int)(20 / this.speed)));
				} catch (Exception ex) {
					LOGGER.error("[FANCYMENU] Error while ticking panorama!", ex);
				}
			}
			this.tickerThreadRunning = false;
		}, "FancyMenu Panorama Ticker Thread").start();

	}

	@Override
	public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
		this.lastRenderCall = System.currentTimeMillis();
		this.startTickerThreadIfNeeded();
		if (this.panoramaImageSuppliers.size() < 6) {
			RenderSystem.enableBlend();
			graphics.blit(RenderType::guiTextured, ITexture.MISSING_TEXTURE_LOCATION, 0, 0, 0.0F, 0.0F, ScreenUtils.getScreenWidth(), ScreenUtils.getScreenHeight(), ScreenUtils.getScreenWidth(), ScreenUtils.getScreenHeight());
		} else {
			this._render(graphics, Minecraft.getInstance(), this.opacity);
		}
	}

	private void _render(@NotNull GuiGraphics graphics, Minecraft mc, float alpha) {

		int screenW = ScreenUtils.getScreenWidth();
		int screenH = ScreenUtils.getScreenHeight();

		float pitch = this.angle;
		float yaw = -this.currentRotation;
		float fovF = ((float)this.fov * ((float)Math.PI / 180));

		Tesselator tesselator = Tesselator.getInstance();
		Matrix4f matrix4f = new Matrix4f().setPerspective(fovF, (float)mc.getWindow().getWidth() / (float)mc.getWindow().getHeight(), 0.05F, 10.0F);
		RenderSystem.backupProjectionMatrix();
		RenderSystem.setProjectionMatrix(matrix4f, ProjectionType.PERSPECTIVE);
		Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
		matrix4fStack.pushMatrix();
		matrix4fStack.rotationX((float) Math.PI);
		RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
		RenderSystem.enableBlend();
		RenderSystem.disableCull();
		RenderSystem.depthMask(false);
//		RenderSystem.disableDepthTest();

		for(int $$8 = 0; $$8 < 4; ++$$8) {
			matrix4fStack.pushMatrix();
			float $$9 = ((float)($$8 % 2) / 2.0F - 0.5F) / 256.0F;
			float $$10 = ((float)($$8 / 2) / 2.0F - 0.5F) / 256.0F;
			float $$11 = 0.0F;
			matrix4fStack.translate($$9, $$10, 0.0F);
			matrix4fStack.rotateX(pitch * (float) (Math.PI / 180.0));
			matrix4fStack.rotateY(yaw * (float) (Math.PI / 180.0));

			for(int texNum = 0; texNum < 6; ++texNum) {
				ResourceLocation location = null;
				if (this.panoramaImageSuppliers.size() >= (texNum + 1)) {
					ResourceSupplier<ITexture> texSupplier = this.panoramaImageSuppliers.get(texNum);
					ITexture texture = texSupplier.get();
					if (texture != null) {
						location = texture.getResourceLocation();
					}
				}
				if (location == null) location = ITexture.MISSING_TEXTURE_LOCATION;
				RenderSystem.setShaderTexture(0, location);
				BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
				int $$14 = Math.round(255.0F * alpha) / ($$8 + 1);
				if (texNum == 0) {
					bufferBuilder.addVertex(-1.0F, -1.0F, 1.0F).setUv(0.0F, 0.0F).setWhiteAlpha($$14);
					bufferBuilder.addVertex(-1.0F, 1.0F, 1.0F).setUv(0.0F, 1.0F).setWhiteAlpha($$14);
					bufferBuilder.addVertex(1.0F, 1.0F, 1.0F).setUv(1.0F, 1.0F).setWhiteAlpha($$14);
					bufferBuilder.addVertex(1.0F, -1.0F, 1.0F).setUv(1.0F, 0.0F).setWhiteAlpha($$14);
				}

				if (texNum == 1) {
					bufferBuilder.addVertex(1.0F, -1.0F, 1.0F).setUv(0.0F, 0.0F).setWhiteAlpha($$14);
					bufferBuilder.addVertex(1.0F, 1.0F, 1.0F).setUv(0.0F, 1.0F).setWhiteAlpha($$14);
					bufferBuilder.addVertex(1.0F, 1.0F, -1.0F).setUv(1.0F, 1.0F).setWhiteAlpha($$14);
					bufferBuilder.addVertex(1.0F, -1.0F, -1.0F).setUv(1.0F, 0.0F).setWhiteAlpha($$14);
				}

				if (texNum == 2) {
					bufferBuilder.addVertex(1.0F, -1.0F, -1.0F).setUv(0.0F, 0.0F).setWhiteAlpha($$14);
					bufferBuilder.addVertex(1.0F, 1.0F, -1.0F).setUv(0.0F, 1.0F).setWhiteAlpha($$14);
					bufferBuilder.addVertex(-1.0F, 1.0F, -1.0F).setUv(1.0F, 1.0F).setWhiteAlpha($$14);
					bufferBuilder.addVertex(-1.0F, -1.0F, -1.0F).setUv(1.0F, 0.0F).setWhiteAlpha($$14);
				}

				if (texNum == 3) {
					bufferBuilder.addVertex(-1.0F, -1.0F, -1.0F).setUv(0.0F, 0.0F).setWhiteAlpha($$14);
					bufferBuilder.addVertex(-1.0F, 1.0F, -1.0F).setUv(0.0F, 1.0F).setWhiteAlpha($$14);
					bufferBuilder.addVertex(-1.0F, 1.0F, 1.0F).setUv(1.0F, 1.0F).setWhiteAlpha($$14);
					bufferBuilder.addVertex(-1.0F, -1.0F, 1.0F).setUv(1.0F, 0.0F).setWhiteAlpha($$14);
				}

				if (texNum == 4) {
					bufferBuilder.addVertex(-1.0F, -1.0F, -1.0F).setUv(0.0F, 0.0F).setWhiteAlpha($$14);
					bufferBuilder.addVertex(-1.0F, -1.0F, 1.0F).setUv(0.0F, 1.0F).setWhiteAlpha($$14);
					bufferBuilder.addVertex(1.0F, -1.0F, 1.0F).setUv(1.0F, 1.0F).setWhiteAlpha($$14);
					bufferBuilder.addVertex(1.0F, -1.0F, -1.0F).setUv(1.0F, 0.0F).setWhiteAlpha($$14);
				}

				if (texNum == 5) {
					bufferBuilder.addVertex(-1.0F, 1.0F, 1.0F).setUv(0.0F, 0.0F).setWhiteAlpha($$14);
					bufferBuilder.addVertex(-1.0F, 1.0F, -1.0F).setUv(0.0F, 1.0F).setWhiteAlpha($$14);
					bufferBuilder.addVertex(1.0F, 1.0F, -1.0F).setUv(1.0F, 1.0F).setWhiteAlpha($$14);
					bufferBuilder.addVertex(1.0F, 1.0F, 1.0F).setUv(1.0F, 0.0F).setWhiteAlpha($$14);
				}

				BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
			}

			matrix4fStack.popMatrix();
			RenderSystem.colorMask(true, true, true, false);
		}

		RenderSystem.colorMask(true, true, true, true);
		RenderSystem.restoreProjectionMatrix();
		matrix4fStack.popMatrix();
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.enableDepthTest();

		if (this.overlayTextureSupplier != null) {
			ITexture texture = this.overlayTextureSupplier.get();
			if (texture != null) {
				ResourceLocation location = texture.getResourceLocation();
				if (location != null) {
					RenderSystem.enableBlend();
					graphics.blit(RenderType::guiTextured, location, 0, 0, 0.0F, 0.0F, screenW, screenH, screenW, screenH, ARGB.colorFromFloat(1.0F, 1.0F, 1.0F, this.opacity));
				}
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
