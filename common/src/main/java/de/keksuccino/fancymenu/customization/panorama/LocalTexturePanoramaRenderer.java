package de.keksuccino.fancymenu.customization.panorama;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
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
import net.minecraft.client.renderer.CachedPerspectiveProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * A vanilla-like panorama renderer that uses a custom CubeMap texture to handle loading and rendering.
 * This architecture is cleaner, more efficient, and aligns with modern rendering practices.
 */
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
	protected volatile float currentRotation = 0.0F;
	protected volatile long lastRenderCall = -1L;
	@Nullable
	private PanoramaCubeMapTexture cubemapTexture;
	@Nullable
	private ResourceLocation cubemapLocation;
	@Nullable
	private CachedPerspectiveProjectionMatrixBuffer projectionMatrixUbo;
	@Nullable
	private GpuBuffer vertexBuffer;
	private boolean isPrepared = false;
	private boolean hasInitialized = false;

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
							LOGGER.error("[FANCYMENU] Unable to load panorama! Missing 'name' value: " + renderer.propertiesFile.getAbsolutePath());
							return null;
						}
						String sp = panoMeta.getValue("speed");
						if ((sp != null) && MathUtils.isFloat(sp)) renderer.speed = Float.parseFloat(sp);
						String fo = panoMeta.getValue("fov");
						if ((fo != null) && MathUtils.isDouble(fo)) renderer.fov = Double.parseDouble(fo);
						String an = panoMeta.getValue("angle");
						if ((an != null) && MathUtils.isFloat(an)) renderer.angle = Float.parseFloat(an);
						String rot = panoMeta.getValue("start_rotation");
						if ((rot != null) && MathUtils.isFloat(rot)) renderer.currentRotation = Mth.clamp(Float.parseFloat(rot), 0.0F, 360.0F);

						renderer.prepare();
						return renderer;
					}
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

	/**
	 * Prepares the renderer by identifying the image files and creating the custom texture object.
	 * The actual loading to the GPU is deferred until the first render call.
	 */
	protected void prepare() {

		this.panoramaImageSuppliers.clear();
		this.overlayTextureSupplier = null;

		for (int i = 0; i < 6; i++) {
			File panoImage = new File(this.panoramaImageDir, "panorama_" + i + ".png");
			if (panoImage.isFile()) {
				this.panoramaImageSuppliers.add(ResourceSupplier.image(ResourceSource.of(panoImage.getAbsolutePath(), ResourceSourceType.LOCAL).getSourceWithPrefix()));
			} else {
				LOGGER.error("[FANCYMENU] Unable to load panorama! Missing image 'panorama_{}.png' for '{}'", i, this.name);
				return;
			}
		}

		if ((this.overlayImageFile != null) && this.overlayImageFile.isFile()) {
			this.overlayTextureSupplier = ResourceSupplier.image(ResourceSource.of(this.overlayImageFile.getAbsolutePath(), ResourceSourceType.LOCAL).getSourceWithPrefix());
		}

		this.isPrepared = true;

	}

	/**
	 * Initializes all GPU resources on the render thread right before the first use.
	 * This includes the UBO, vertex buffer, and registering/loading the cubemap texture.
	 */
	private void lazyInit() {
		// Only run once if the renderer is successfully prepared
		if (this.isPrepared && !this.hasInitialized) {
			this.projectionMatrixUbo = new CachedPerspectiveProjectionMatrixBuffer("panorama_proj", 0.05F, 10.0F);
			this.vertexBuffer = initializeVertices();

			this.cubemapTexture = new PanoramaCubeMapTexture(this.panoramaImageSuppliers);
			this.cubemapLocation = ResourceLocation.fromNamespaceAndPath("fancymenu", "panorama/" + this.name.toLowerCase().replaceAll("[^a-z0-9/._-]", "_"));

			Minecraft.getInstance().getTextureManager().register(this.cubemapLocation, this.cubemapTexture);
			try {
				this.cubemapTexture.load();
				this.hasInitialized = true; // Only mark as initialized on success
			} catch (IOException e) {
				LOGGER.error("[FANCYMENU] Failed to load custom panorama textures for '{}'", this.name, e);
				this.isPrepared = false; // Prevent further render attempts
			}
		}
	}

	@Override
	public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

		if (!this.isPrepared) return;

		lazyInit();

		// If initialization failed, don't try to render.
		if (!this.hasInitialized) return;

		this.lastRenderCall = System.currentTimeMillis();
		this.startTickerThreadIfNeeded();

		this._render(graphics, Minecraft.getInstance(), this.opacity);

	}

	private void _render(@NotNull GuiGraphics graphics, Minecraft mc, float alpha) {

		int screenW = ScreenUtils.getScreenWidth();
		int screenH = ScreenUtils.getScreenHeight();

		RenderSystem.setProjectionMatrix(this.projectionMatrixUbo.getBuffer(mc.getWindow().getWidth(), mc.getWindow().getHeight(), (float) this.fov), ProjectionType.PERSPECTIVE);

		RenderPipeline renderPipeline = RenderPipelines.PANORAMA;
		RenderTarget renderTarget = mc.getMainRenderTarget();
		RenderSystem.AutoStorageIndexBuffer sequentialIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
		GpuBuffer indexBuffer = sequentialIndexBuffer.getBuffer(36);

		Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
		modelViewStack.pushMatrix();
		modelViewStack.rotationX((float) Math.PI);
		modelViewStack.rotateX(this.angle * (float) (Math.PI / 180.0));
		modelViewStack.rotateY(-this.currentRotation * (float) (Math.PI / 180.0));

		GpuBufferSlice dynamicUniforms = RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f(modelViewStack), new Vector4f(1.0F, 1.0F, 1.0F, alpha), new Vector3f(), new Matrix4f(), 0.0F);

		try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Panorama", renderTarget.getColorTextureView(), OptionalInt.empty(), renderTarget.getDepthTextureView(), OptionalDouble.of(1.0))) {
			renderPass.setPipeline(renderPipeline);
			RenderSystem.bindDefaultUniforms(renderPass);
			renderPass.setVertexBuffer(0, this.vertexBuffer);
			renderPass.setIndexBuffer(indexBuffer, sequentialIndexBuffer.type());
			renderPass.setUniform("DynamicTransforms", dynamicUniforms);
			renderPass.bindSampler("Sampler0", mc.getTextureManager().getTexture(this.cubemapLocation).getTextureView());
			renderPass.drawIndexed(0, 0, 36, 1);
		}

		modelViewStack.popMatrix();
		RenderSystem.restoreProjectionMatrix();

		if (this.overlayTextureSupplier != null) {
			ITexture texture = this.overlayTextureSupplier.get();
			if (texture != null) {
				ResourceLocation location = texture.getResourceLocation();
				if (location != null) {
					graphics.blit(RenderPipelines.GUI_TEXTURED, location, 0, 0, 0, 0, screenW, screenH, screenW, screenH, ARGB.color(Mth.clamp((int)(alpha * 255), 0, 255), 255, 255, 255));
				}
			}
		}
	}

//	private GpuBuffer initializeVertices() {
//		GpuBuffer newBuffer;
//		try (ByteBufferBuilder byteBufferBuilder = ByteBufferBuilder.exactlySized(DefaultVertexFormat.POSITION.getVertexSize() * 24)) {
//			BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
//			for (int i = 0; i < 6; ++i) {
//				bufferBuilder.addVertex(-1.0F, -1.0F, 1.0F);
//				bufferBuilder.addVertex(-1.0F, 1.0F, 1.0F);
//				bufferBuilder.addVertex(1.0F, 1.0F, 1.0F);
//				bufferBuilder.addVertex(1.0F, -1.0F, 1.0F);
//			}
//			try (MeshData meshData = bufferBuilder.buildOrThrow()) {
//				newBuffer = RenderSystem.getDevice().createBuffer(() -> "FancyMenu Panorama Position Buffer", 32, meshData.vertexBuffer());
//			}
//		}
//		return newBuffer;
//	}

	private GpuBuffer initializeVertices() {
		GpuBuffer gpuBuffer;
		try (ByteBufferBuilder byteBufferBuilder = ByteBufferBuilder.exactlySized(DefaultVertexFormat.POSITION.getVertexSize() * 4 * 6)) {
			BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
			bufferBuilder.addVertex(-1.0F, -1.0F, 1.0F);
			bufferBuilder.addVertex(-1.0F, 1.0F, 1.0F);
			bufferBuilder.addVertex(1.0F, 1.0F, 1.0F);
			bufferBuilder.addVertex(1.0F, -1.0F, 1.0F);
			bufferBuilder.addVertex(1.0F, -1.0F, 1.0F);
			bufferBuilder.addVertex(1.0F, 1.0F, 1.0F);
			bufferBuilder.addVertex(1.0F, 1.0F, -1.0F);
			bufferBuilder.addVertex(1.0F, -1.0F, -1.0F);
			bufferBuilder.addVertex(1.0F, -1.0F, -1.0F);
			bufferBuilder.addVertex(1.0F, 1.0F, -1.0F);
			bufferBuilder.addVertex(-1.0F, 1.0F, -1.0F);
			bufferBuilder.addVertex(-1.0F, -1.0F, -1.0F);
			bufferBuilder.addVertex(-1.0F, -1.0F, -1.0F);
			bufferBuilder.addVertex(-1.0F, 1.0F, -1.0F);
			bufferBuilder.addVertex(-1.0F, 1.0F, 1.0F);
			bufferBuilder.addVertex(-1.0F, -1.0F, 1.0F);
			bufferBuilder.addVertex(-1.0F, -1.0F, -1.0F);
			bufferBuilder.addVertex(-1.0F, -1.0F, 1.0F);
			bufferBuilder.addVertex(1.0F, -1.0F, 1.0F);
			bufferBuilder.addVertex(1.0F, -1.0F, -1.0F);
			bufferBuilder.addVertex(-1.0F, 1.0F, 1.0F);
			bufferBuilder.addVertex(-1.0F, 1.0F, -1.0F);
			bufferBuilder.addVertex(1.0F, 1.0F, -1.0F);
			bufferBuilder.addVertex(1.0F, 1.0F, 1.0F);
			try (MeshData meshData = bufferBuilder.buildOrThrow()) {
				gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "FancyMenu Panorama Position Buffer", 32, meshData.vertexBuffer());
			}
		}
		return gpuBuffer;
	}

	@Override
	public void close() {
		if (this.vertexBuffer != null) {
			this.vertexBuffer.close();
			this.vertexBuffer = null;
		}
		if (this.projectionMatrixUbo != null) {
			this.projectionMatrixUbo.close();
			this.projectionMatrixUbo = null;
		}
		if (this.cubemapTexture != null && this.cubemapLocation != null) {
			// This tells the texture manager to remove it from its registry, which in turn calls close() on our texture.
			Minecraft.getInstance().getTextureManager().release(this.cubemapLocation);
			this.cubemapTexture = null;
			this.cubemapLocation = null;
		}
		this.isPrepared = false;
		this.hasInitialized = false;
	}

	protected void startTickerThreadIfNeeded() {
		if (this.tickerThreadRunning) return;

		this.lastRenderCall = System.currentTimeMillis();
		this.tickerThreadRunning = true;

		new Thread(() -> {
			while ((this.lastRenderCall + 5000L) > System.currentTimeMillis()) {
				this.currentRotation += 0.03F;
				if (this.currentRotation >= 360) {
					this.currentRotation = 0;
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

	public String getName() {
		return this.name;
	}

	public void setSpeed(float speed) {
		this.speed = Math.max(0.0F, speed);
	}

	public void setFov(double fov) {
		this.fov = Math.min(fov, 179.0D);
	}

	public void setAngle(float angle) {
		this.angle = angle;
	}

}