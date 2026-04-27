package de.keksuccino.fancymenu.customization.panorama;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinBufferBuilder;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinGuiGraphicsExtractor;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.rendering.GuiScissorUtil;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import de.keksuccino.fancymenu.util.resource.ResourceSourceType;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertiesParser;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryUtil;

@SuppressWarnings("unused")
public class LocalTexturePanoramaRenderer implements Renderable, AutoCloseable {

	private static final Logger LOGGER = LogManager.getLogger();
	private static final VertexFormatElement PANORAMA_INFO_FANCYMENU = registerNextVertexFormatElement_FancyMenu();
	private static final VertexFormat PANORAMA_VERTEX_FORMAT_FANCYMENU = VertexFormat.builder()
			.add("Position", VertexFormatElement.POSITION)
			.add("Color", VertexFormatElement.COLOR)
			.add("UV0", VertexFormatElement.UV0)
			.add("PanoramaInfo", PANORAMA_INFO_FANCYMENU)
			.build();
	private static final RenderPipeline PANORAMA_PIPELINE_FANCYMENU = RenderPipeline.builder()
			.withLocation(Identifier.withDefaultNamespace("pipeline/fancymenu_panorama"))
			.withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
			.withUniform("Projection", UniformType.UNIFORM_BUFFER)
			.withVertexShader("core/fancymenu_gui_panorama")
			.withFragmentShader("core/fancymenu_gui_panorama")
			.withSampler("Sampler0")
			.withColorTargetState(new ColorTargetState(Optional.of(BlendFunction.TRANSLUCENT), ColorTargetState.WRITE_COLOR))
			.withDepthStencilState(Optional.empty())
			.withVertexFormat(PANORAMA_VERTEX_FORMAT_FANCYMENU, VertexFormat.Mode.QUADS)
			.build();

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
	private PanoramaCubeMapTexture panoramaCubeMapTexture = null;
	@Nullable
	private FlexibleCubeMap cubeMap = null;
	@Nullable
	private Identifier cubeMapLocation = null;
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
		this.cubeMap = new FlexibleCubeMap(this.cubeMapLocation, "fancymenu_panorama_" + this.uniqueId);
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
	public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
		this.render(graphics, mouseX, mouseY, partial);
	}

	public void render(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

		this.lastRenderCall = System.currentTimeMillis();
		this.startTickerThreadIfNeeded();

		// Check if cube map texture is loaded
		if (!this.isPanoramaTextureReady()) {
			if ((this.panoramaCubeMapTexture == null) || this.panoramaCubeMapTexture.isLoadFailed()) {
				this.renderMissingTexture(graphics, 0, 0, ScreenUtils.getScreenWidth(), ScreenUtils.getScreenHeight());
			}
			return;
		}

		this._render(graphics, this.opacity);

	}

	private void _render(@NotNull GuiGraphicsExtractor graphics, float alpha) {

		int screenW = ScreenUtils.getScreenWidth();
		int screenH = ScreenUtils.getScreenHeight();

		this.submitPanorama(graphics, 0.0F, 0.0F, screenW, screenH, alpha);

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

	public void renderInArea(@NotNull GuiGraphicsExtractor graphics, int x, int y, int width, int height, float partial) {
		if ((width <= 0) || (height <= 0)) {
			return;
		}

		this.lastRenderCall = System.currentTimeMillis();
		this.startTickerThreadIfNeeded();

		if (!this.isPanoramaTextureReady()) {
			if ((this.panoramaCubeMapTexture == null) || this.panoramaCubeMapTexture.isLoadFailed()) {
				this.renderMissingTexture(graphics, x, y, width, height);
			}
			return;
		}

		this.submitPanorama(graphics, x, y, width, height, this.opacity);

		if (this.overlayTextureSupplier != null) {
			ITexture texture = this.overlayTextureSupplier.get();
			if (texture != null) {
				Identifier location = texture.getResourceLocation();
				if (location != null) {
					graphics.blit(RenderPipelines.GUI_TEXTURED, location, x, y, 0.0F, 0.0F, width, height, width, height, ARGB.white(this.opacity));
				}
			}
		}
	}

	private boolean isPanoramaTextureReady() {
		if ((this.panoramaCubeMapTexture == null) || this.panoramaCubeMapTexture.isLoadFailed()) {
			return false;
		}
		if (!this.panoramaCubeMapTexture.isLoaded()) {
			try {
				this.panoramaCubeMapTexture.getTextureView();
			} catch (Exception ex) {
				LOGGER.error("[FANCYMENU] Failed to initialize cubic panorama texture: " + this.name, ex);
				return false;
			}
		}
		return this.panoramaCubeMapTexture.isLoaded() && !this.panoramaCubeMapTexture.isLoadFailed();
	}

	private void renderMissingTexture(@NotNull GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, ITexture.MISSING_TEXTURE_LOCATION, x, y, 0.0F, 0.0F, width, height, width, height);
	}

	private void submitPanorama(@NotNull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float alpha) {
		if ((this.panoramaCubeMapTexture == null) || (width <= 0.0F) || (height <= 0.0F)) {
			return;
		}

		float halfHeight = height * 0.5F;
		if (halfHeight <= 0.0F) {
			return;
		}

		float centerX = x + (width * 0.5F);
		float centerY = y + (height * 0.5F);
		float tangent = (float)Math.tan(Math.toRadians(this.fov) * 0.5D);
		float minPlaneX = ((x - centerX) / halfHeight) * tangent;
		float maxPlaneX = (((x + width) - centerX) / halfHeight) * tangent;
		float minPlaneY = (centerY - (y + height)) / halfHeight * tangent;
		float maxPlaneY = (centerY - y) / halfHeight * tangent;
		float pitchRadians = (float)Math.toRadians(-this.angle);
		float yawRadians = (float)Math.toRadians(this.currentRotation);
		int color = ARGB.white(alpha);

		((IMixinGuiGraphicsExtractor)graphics).get_guiRenderState_FancyMenu().addGuiElement(new PanoramaRenderState(
				new Matrix3x2f(graphics.pose()),
				this.panoramaCubeMapTexture,
				x,
				y,
				x + width,
				y + height,
				minPlaneX,
				maxPlaneX,
				minPlaneY,
				maxPlaneY,
				(float)Math.sin(pitchRadians),
				(float)Math.cos(pitchRadians),
				(float)Math.sin(yawRadians),
				(float)Math.cos(yawRadians),
				color,
				GuiScissorUtil.getActiveScissor(graphics)
		));
	}

	private static VertexFormatElement registerNextVertexFormatElement_FancyMenu() {
		for (int i = 0; i < VertexFormatElement.MAX_COUNT; i++) {
			if (VertexFormatElement.byId(i) == null) {
				return VertexFormatElement.register(i, 0, VertexFormatElement.Type.FLOAT, false, 4);
			}
		}
		throw new IllegalStateException("VertexFormatElement count limit exceeded");
	}

	private static void writeVec4_FancyMenu(@NotNull VertexConsumer consumer, @NotNull VertexFormatElement element, float x, float y, float z, float w) {
		long pointer = ((IMixinBufferBuilder)consumer).invoke_beginElement_FancyMenu(element);
		if (pointer == -1L) {
			return;
		}
		MemoryUtil.memPutFloat(pointer, x);
		MemoryUtil.memPutFloat(pointer + 4L, y);
		MemoryUtil.memPutFloat(pointer + 8L, z);
		MemoryUtil.memPutFloat(pointer + 12L, w);
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

	public void renderToCurrentOutput_FancyMenu(float pitch, float yaw, int targetWidth, int targetHeight) {
		if ((this.cubeMap != null) && (targetWidth > 0) && (targetHeight > 0)) {
			this.cubeMap.render(pitch, yaw, (float)this.fov, targetWidth, targetHeight);
		}
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
		if (this.cubeMapLocation != null) {
			Minecraft.getInstance().getTextureManager().release(this.cubeMapLocation);
			this.cubeMapLocation = null;
		}
	}

	private record PanoramaRenderState(
			Matrix3x2f transform,
			AbstractTexture texture,
			float minX,
			float minY,
			float maxX,
			float maxY,
			float minPlaneX,
			float maxPlaneX,
			float minPlaneY,
			float maxPlaneY,
			float pitchSin,
			float pitchCos,
			float yawSin,
			float yawCos,
			int color,
			@Nullable ScreenRectangle scissorArea,
			@Nullable ScreenRectangle bounds
	) implements GuiElementRenderState {

		private PanoramaRenderState(
				Matrix3x2f transform,
				AbstractTexture texture,
				float minX,
				float minY,
				float maxX,
				float maxY,
				float minPlaneX,
				float maxPlaneX,
				float minPlaneY,
				float maxPlaneY,
				float pitchSin,
				float pitchCos,
				float yawSin,
				float yawCos,
				int color,
				@Nullable ScreenRectangle scissorArea
		) {
			this(
					transform,
					texture,
					minX,
					minY,
					maxX,
					maxY,
					minPlaneX,
					maxPlaneX,
					minPlaneY,
					maxPlaneY,
					pitchSin,
					pitchCos,
					yawSin,
					yawCos,
					color,
					scissorArea,
					getBounds_FancyMenu(minX, minY, maxX, maxY, transform, scissorArea)
			);
		}

		@Override
		public void buildVertices(@NotNull VertexConsumer consumer) {
			this.addVertex_FancyMenu(consumer, this.minX, this.minY, this.minPlaneX, this.maxPlaneY);
			this.addVertex_FancyMenu(consumer, this.minX, this.maxY, this.minPlaneX, this.minPlaneY);
			this.addVertex_FancyMenu(consumer, this.maxX, this.maxY, this.maxPlaneX, this.minPlaneY);
			this.addVertex_FancyMenu(consumer, this.maxX, this.minY, this.maxPlaneX, this.maxPlaneY);
		}

		private void addVertex_FancyMenu(@NotNull VertexConsumer consumer, float x, float y, float planeX, float planeY) {
			consumer.addVertexWith2DPose(this.transform, x, y)
					.setColor(this.color)
					.setUv(planeX, planeY);
			writeVec4_FancyMenu(consumer, PANORAMA_INFO_FANCYMENU, this.pitchSin, this.pitchCos, this.yawSin, this.yawCos);
		}

		@Override
		public RenderPipeline pipeline() {
			return PANORAMA_PIPELINE_FANCYMENU;
		}

		@Override
		public TextureSetup textureSetup() {
			return TextureSetup.singleTexture(this.texture.getTextureView(), this.texture.getSampler());
		}

		@Nullable
		private static ScreenRectangle getBounds_FancyMenu(float minX, float minY, float maxX, float maxY, Matrix3x2f transform, @Nullable ScreenRectangle scissorArea) {
			int x = (int)Math.floor(Math.min(minX, maxX));
			int y = (int)Math.floor(Math.min(minY, maxY));
			int right = (int)Math.ceil(Math.max(minX, maxX));
			int bottom = (int)Math.ceil(Math.max(minY, maxY));
			int width = Math.max(1, right - x);
			int height = Math.max(1, bottom - y);
			ScreenRectangle rectangle = new ScreenRectangle(x, y, width, height).transformMaxBounds(transform);
			return scissorArea != null ? scissorArea.intersection(rectangle) : rectangle;
		}

	}

}
