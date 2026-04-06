package de.keksuccino.fancymenu.customization.panorama;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.jetbrains.annotations.Nullable;

public class FancyMenuPanoramaPictureInPictureRenderer implements AutoCloseable {

	private final MultiBufferSource.BufferSource bufferSource;
	@Nullable
	private GpuTexture texture;
	@Nullable
	private GpuTextureView textureView;
	@Nullable
	private GpuTexture depthTexture;
	@Nullable
	private GpuTextureView depthTextureView;

	public FancyMenuPanoramaPictureInPictureRenderer(MultiBufferSource.BufferSource bufferSource) {
		this.bufferSource = bufferSource;
	}

	public void prepare(FancyMenuPanoramaRenderState renderState, GuiRenderState guiRenderState, int guiScale) {
		int width = Math.max(1, (renderState.x1() - renderState.x0()) * guiScale);
		int height = Math.max(1, (renderState.y1() - renderState.y0()) * guiScale);
		boolean needsResize = this.texture == null || this.texture.getWidth(0) != width || this.texture.getHeight(0) != height;
		this.prepareTextures(needsResize, width, height);
		if ((this.textureView == null) || (this.depthTextureView == null)) {
			return;
		}

		RenderSystem.outputColorTextureOverride = this.textureView;
		RenderSystem.outputDepthTextureOverride = this.depthTextureView;
		try {
			renderState.panoramaRenderer().renderToCurrentOutput_FancyMenu(renderState.pitch(), renderState.yaw(), width, height);
			this.bufferSource.endBatch();
		} finally {
			RenderSystem.outputColorTextureOverride = null;
			RenderSystem.outputDepthTextureOverride = null;
		}

		this.blitTexture(renderState, guiRenderState);
	}

	private void blitTexture(FancyMenuPanoramaRenderState renderState, GuiRenderState guiRenderState) {
		if (this.textureView == null) {
			return;
		}

		guiRenderState.addBlitToCurrentLayer(
			new BlitRenderState(
				RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
				TextureSetup.singleTexture(this.textureView, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
				renderState.pose(),
				renderState.x0(),
				renderState.y0(),
				renderState.x1(),
				renderState.y1(),
				0.0F,
				1.0F,
				1.0F,
				0.0F,
				renderState.color(),
				renderState.scissorArea(),
				null
			)
		);
	}

	private void prepareTextures(boolean needsResize, int width, int height) {
		if (this.texture != null && needsResize) {
			this.texture.close();
			this.texture = null;
			if (this.textureView != null) {
				this.textureView.close();
				this.textureView = null;
			}
			if (this.depthTexture != null) {
				this.depthTexture.close();
				this.depthTexture = null;
			}
			if (this.depthTextureView != null) {
				this.depthTextureView.close();
				this.depthTextureView = null;
			}
		}

		GpuDevice device = RenderSystem.getDevice();
		if (this.texture == null) {
			this.texture = device.createTexture(() -> "UI fancymenu_panorama texture", 13, TextureFormat.RGBA8, width, height, 1, 1);
			this.textureView = device.createTextureView(this.texture);
			this.depthTexture = device.createTexture(() -> "UI fancymenu_panorama depth texture", 9, TextureFormat.DEPTH32, width, height, 1, 1);
			this.depthTextureView = device.createTextureView(this.depthTexture);
		}

		device.createCommandEncoder().clearColorAndDepthTextures(this.texture, 0, this.depthTexture, 1.0);
	}

	@Override
	public void close() {
		if (this.texture != null) {
			this.texture.close();
			this.texture = null;
		}
		if (this.textureView != null) {
			this.textureView.close();
			this.textureView = null;
		}
		if (this.depthTexture != null) {
			this.depthTexture.close();
			this.depthTexture = null;
		}
		if (this.depthTextureView != null) {
			this.depthTextureView.close();
			this.depthTextureView = null;
		}
	}

}
