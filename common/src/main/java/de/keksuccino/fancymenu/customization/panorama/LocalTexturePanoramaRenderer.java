package de.keksuccino.fancymenu.customization.panorama;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
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
import net.minecraft.client.renderer.CachedPerspectiveProjectionMatrixBuffer;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class LocalTexturePanoramaRenderer implements Renderable, AutoCloseable {

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
	private CubeMap cubeMap = null;
	@Nullable
	private PanoramaCubeMapTexture panoramaCubeMapTexture = null;
	@Nullable
	private Identifier cubeMapLocation = null;
	@Nullable
	private CachedPerspectiveProjectionMatrixBuffer projectionMatrixBuffer = null;
	private final String uniqueId = UUID.randomUUID().toString();

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
						renderer.initialize();
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
			this.overlayTextureSupplier = ResourceSupplier.image(ResourceSource.of(this.overlayImageFile.getAbsolutePath(), ResourceSourceType.LOCAL).getSourceWithPrefix());
		}

	}

	protected void initialize() {
		// Create custom cube map texture
		this.panoramaCubeMapTexture = new PanoramaCubeMapTexture(this.name != null ? this.name : "unnamed", this.panoramaImageSuppliers);
		
		// Register the texture with a unique Identifier
		this.cubeMapLocation = Identifier.fromNamespaceAndPath("fancymenu", "panorama_" + this.uniqueId);
		TextureManager textureManager = Minecraft.getInstance().getTextureManager();
		textureManager.register(this.cubeMapLocation, this.panoramaCubeMapTexture);
		
		// Create CubeMap instance for rendering
		this.cubeMap = new CubeMap(this.cubeMapLocation);
		
		// Create projection matrix buffer
		this.projectionMatrixBuffer = new CachedPerspectiveProjectionMatrixBuffer("fancymenu_panorama_" + this.uniqueId, 0.05F, 10.0F);
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

		// Check if cube map texture is loaded
		if ((this.panoramaCubeMapTexture == null) || this.panoramaCubeMapTexture.isLoadFailed()) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, ITexture.MISSING_TEXTURE_LOCATION, 0, 0, 0.0F, 0.0F, ScreenUtils.getScreenWidth(), ScreenUtils.getScreenHeight(), ScreenUtils.getScreenWidth(), ScreenUtils.getScreenHeight());
			return;
		}

		if (!this.panoramaCubeMapTexture.isLoaded()) {
			// Trigger loading by trying to get the texture view
			this.panoramaCubeMapTexture.getTextureView();
			// Show loading screen or previous frame while loading
			return;
		}

		this._render(graphics, Minecraft.getInstance(), this.opacity);

	}

	private void _render(@NotNull GuiGraphics graphics, Minecraft mc, float alpha) {

		int screenW = ScreenUtils.getScreenWidth();
		int screenH = ScreenUtils.getScreenHeight();

		float pitch = this.angle;
		float yaw = -this.currentRotation;
		
		// Render the cube map using vanilla's approach with custom alpha support
		this.renderCubeMapWithAlpha(mc, pitch, yaw, alpha);

		// Render overlay if present
		if (this.overlayTextureSupplier != null) {
			ITexture texture = this.overlayTextureSupplier.get();
			if (texture != null) {
				Identifier location = texture.getResourceLocation();
				if (location != null) {
					graphics.blit(RenderPipelines.GUI_TEXTURED, location, 0, 0, 0.0F, 0.0F, screenW, screenH, screenW, screenH, ARGB.white(this.opacity));
				}
			}
		}

	}

	private void renderCubeMapWithAlpha(Minecraft mc, float pitch, float yaw, float alpha) {
		// We can't use CubeMap.render() directly because it doesn't support custom FOV and alpha
		// So we need to set our own projection matrix with custom FOV before rendering
		
		if (this.cubeMap != null && this.projectionMatrixBuffer != null) {
			// Backup current projection matrix
			RenderSystem.backupProjectionMatrix();
			
			// Set our custom projection matrix with desired FOV
			RenderSystem.setProjectionMatrix(
				this.projectionMatrixBuffer.getBuffer(mc.getWindow().getWidth(), mc.getWindow().getHeight(), (float)this.fov), 
				ProjectionType.PERSPECTIVE
			);
			
			// Apply alpha by modifying shader color
//			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
			
			// Use vanilla cube map render
			this.cubeMap.render(mc, pitch, yaw);
			
			// Reset shader color
//			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			
			// Restore original projection matrix
			RenderSystem.restoreProjectionMatrix();
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

	@Override
	public void close() {
		if (this.cubeMap != null) {
			this.cubeMap.close();
			this.cubeMap = null;
		}
		if (this.panoramaCubeMapTexture != null) {
			this.panoramaCubeMapTexture.close();
			this.panoramaCubeMapTexture = null;
		}
		if (this.projectionMatrixBuffer != null) {
			this.projectionMatrixBuffer.close();
			this.projectionMatrixBuffer = null;
		}
		if (this.cubeMapLocation != null) {
			Minecraft.getInstance().getTextureManager().release(this.cubeMapLocation);
			this.cubeMapLocation = null;
		}
	}

}
