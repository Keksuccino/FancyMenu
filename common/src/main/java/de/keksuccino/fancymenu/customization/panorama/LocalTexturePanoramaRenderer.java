package de.keksuccino.fancymenu.customization.panorama;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.*;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinRenderPipelines;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Custom panorama renderer that uses six individual local textures to form a skybox,
 * updated to be compatible with modern Minecraft rendering pipelines.
 */
@SuppressWarnings("unused")
public class LocalTexturePanoramaRenderer implements Renderable, AutoCloseable {

	private static final Logger LOGGER = LogManager.getLogger();

	private static final RenderPipeline CUSTOM_PANORAMA_PIPELINE = IMixinRenderPipelines.invoke_register_FancyMenu(
			RenderPipeline.builder(IMixinRenderPipelines.get_MATRICES_PROJECTION_SNIPPET_FancyMenu())
					.withLocation("pipeline/fancymenu_custom_panorama")
					.withVertexShader("core/rendertype_world_border")
					.withFragmentShader("core/rendertype_world_border")
					.withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
					.withSampler("Sampler0")
					.withCull(false)
					.withDepthWrite(false)
					// We don't need to set depth test here, as the RenderPass will handle it.
					// But keeping it doesn't hurt.
					.build()
	);

	@NotNull
	public File propertiesFile;
	@NotNull
	public File panoramaImageDir;
	@Nullable
	public File overlayImageFile;
	protected String name = null;
	public final List<ResourceSupplier<ITexture>> panoramaImageSuppliers = new ArrayList<>();
	public final List<ResourceLocation> sides = new ArrayList<>();
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
	private CachedPerspectiveProjectionMatrixBuffer projectionMatrixUbo;

	@Nullable
	private GpuBuffer cubeMapBuffer = initializeVertices();

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
			this.overlayTextureSupplier = ResourceSupplier.image(ResourceSource.of(this.overlayImageFile.getAbsolutePath(), ResourceSourceType.LOCAL).getSourceWithPrefix());
		}
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

	@Override
	public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
		this.lastRenderCall = System.currentTimeMillis();
		this.startTickerThreadIfNeeded();

		this.sides.clear();
		this.panoramaImageSuppliers.forEach(sup -> {
			ITexture tex = sup.get();
			if (tex != null) {
				ResourceLocation loc = tex.getResourceLocation();
				if (loc != null) {
					this.sides.add(loc);
				}
			}
		});

		if (this.sides.size() < 6) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, ITexture.MISSING_TEXTURE_LOCATION, 0, 0, 0, 0, ScreenUtils.getScreenWidth(), ScreenUtils.getScreenHeight(), ScreenUtils.getScreenWidth(), ScreenUtils.getScreenHeight());
		} else {
			this._render(graphics, Minecraft.getInstance(), this.opacity);
		}
	}

	private void _render(@NotNull GuiGraphics graphics, Minecraft mc, float alpha) {

		int screenW = ScreenUtils.getScreenWidth();
		int screenH = ScreenUtils.getScreenHeight();

		if (this.projectionMatrixUbo == null) {
			this.projectionMatrixUbo = new CachedPerspectiveProjectionMatrixBuffer("panorama_proj", 0.05F, 10.0F);
		}
		if (this.cubeMapBuffer == null) {
			this.initializeVertices();
		}

		RenderSystem.setProjectionMatrix(this.projectionMatrixUbo.getBuffer(mc.getWindow().getWidth(), mc.getWindow().getHeight(), (float)this.fov), ProjectionType.PERSPECTIVE);

		RenderTarget renderTarget = mc.getMainRenderTarget();
		GpuTextureView colorView = renderTarget.getColorTextureView();
		GpuTextureView depthView = renderTarget.getDepthTextureView();
		RenderSystem.AutoStorageIndexBuffer sequentialIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
		GpuBuffer indexBuffer = sequentialIndexBuffer.getBuffer(36);
		Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();

		modelViewStack.pushMatrix();
		modelViewStack.rotationX((float) Math.PI);
		modelViewStack.rotateX(this.angle * (float) (Math.PI / 180.0));
		modelViewStack.rotateY(-this.currentRotation * (float) (Math.PI / 180.0));

		GpuBufferSlice dynamicUniforms = RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f(modelViewStack), new Vector4f(1.0F, 1.0F, 1.0F, alpha), new Vector3f(), new Matrix4f(), 0.0F);

		// ### THE FIX: Clear the depth buffer when starting the render pass ###
		// By providing OptionalDouble.of(1.0), we tell the RenderPass to clear the depth buffer to its
		// furthest value (1.0). This ensures that our panorama is drawn correctly over any
		// existing depth information from previous GUI rendering stages.
		try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Panorama", colorView, OptionalInt.empty(), depthView, OptionalDouble.of(1.0))) {

			renderPass.setPipeline(CUSTOM_PANORAMA_PIPELINE);
			RenderSystem.bindDefaultUniforms(renderPass);
			renderPass.setVertexBuffer(0, this.cubeMapBuffer);
			renderPass.setIndexBuffer(indexBuffer, sequentialIndexBuffer.type());
			renderPass.setUniform("DynamicTransforms", dynamicUniforms);

			for (int i = 0; i < 6; i++) {
				if (i < this.sides.size()) {
					ResourceLocation location = this.sides.get(i);
					if (location != null) {
						renderPass.bindSampler("Sampler0", mc.getTextureManager().getTexture(location).getTextureView());
						renderPass.drawIndexed(i * 6, 0, 6, 1);
					}
				}
			}

		}

		modelViewStack.popMatrix();

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

	private static GpuBuffer initializeVertices() {

		GpuBuffer gpubuffer;

		try (ByteBufferBuilder bytebufferbuilder = ByteBufferBuilder.exactlySized(DefaultVertexFormat.POSITION.getVertexSize() * 4 * 6)) {

			BufferBuilder bufferbuilder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
			bufferbuilder.addVertex(-1.0F, -1.0F, 1.0F);
			bufferbuilder.addVertex(-1.0F, 1.0F, 1.0F);
			bufferbuilder.addVertex(1.0F, 1.0F, 1.0F);
			bufferbuilder.addVertex(1.0F, -1.0F, 1.0F);
			bufferbuilder.addVertex(1.0F, -1.0F, 1.0F);
			bufferbuilder.addVertex(1.0F, 1.0F, 1.0F);
			bufferbuilder.addVertex(1.0F, 1.0F, -1.0F);
			bufferbuilder.addVertex(1.0F, -1.0F, -1.0F);
			bufferbuilder.addVertex(1.0F, -1.0F, -1.0F);
			bufferbuilder.addVertex(1.0F, 1.0F, -1.0F);
			bufferbuilder.addVertex(-1.0F, 1.0F, -1.0F);
			bufferbuilder.addVertex(-1.0F, -1.0F, -1.0F);
			bufferbuilder.addVertex(-1.0F, -1.0F, -1.0F);
			bufferbuilder.addVertex(-1.0F, 1.0F, -1.0F);
			bufferbuilder.addVertex(-1.0F, 1.0F, 1.0F);
			bufferbuilder.addVertex(-1.0F, -1.0F, 1.0F);
			bufferbuilder.addVertex(-1.0F, -1.0F, -1.0F);
			bufferbuilder.addVertex(-1.0F, -1.0F, 1.0F);
			bufferbuilder.addVertex(1.0F, -1.0F, 1.0F);
			bufferbuilder.addVertex(1.0F, -1.0F, -1.0F);
			bufferbuilder.addVertex(-1.0F, 1.0F, 1.0F);
			bufferbuilder.addVertex(-1.0F, 1.0F, -1.0F);
			bufferbuilder.addVertex(1.0F, 1.0F, -1.0F);
			bufferbuilder.addVertex(1.0F, 1.0F, 1.0F);

			try (MeshData meshdata = bufferbuilder.buildOrThrow()) {
				gpubuffer = RenderSystem.getDevice().createBuffer(() -> "Cube map vertex buffer", 32, meshdata.vertexBuffer());
			}

		}

		return gpubuffer;

	}

	@Override
	public void close() {
		if (this.cubeMapBuffer != null) {
			this.cubeMapBuffer.close();
			this.cubeMapBuffer = null;
		}
		if (this.projectionMatrixUbo != null) {
			this.projectionMatrixUbo.close();
			this.projectionMatrixUbo = null;
		}
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