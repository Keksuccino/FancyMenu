package de.keksuccino.fancymenu.customization.panorama;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.OptionalDouble;
import java.util.OptionalInt;

public class FlexibleCubeMap implements AutoCloseable {

	private final GpuBuffer vertexBuffer;
	private final Projection projection = new Projection();
	private final ProjectionMatrixBuffer projectionMatrixBuffer;
	private final Identifier location;

	public FlexibleCubeMap(@NotNull Identifier location, @NotNull String label) {
		this.location = location;
		this.projectionMatrixBuffer = new ProjectionMatrixBuffer(label);
		this.vertexBuffer = initializeVertices();
	}

	public void render(float rotXInDegrees, float rotYInDegrees, float fov, int width, int height) {
		RenderSystem.backupProjectionMatrix();
		try {
			this.projection.setupPerspective(0.05F, 10.0F, fov, width, height);
			RenderSystem.setProjectionMatrix(this.projectionMatrixBuffer.getBuffer(this.projection), ProjectionType.PERSPECTIVE);
			GpuTextureView colorTexture = getOutputColorTexture();
			GpuTextureView depthTexture = getOutputDepthTexture();
			RenderPipeline renderPipeline = RenderPipelines.PANORAMA;
			RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
			GpuBuffer indexBuffer = indices.getBuffer(36);
			Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
			modelViewStack.pushMatrix();
			GpuBufferSlice dynamicTransforms;
			try {
				modelViewStack.rotationX((float)Math.PI);
				modelViewStack.rotateX(rotXInDegrees * (float)(Math.PI / 180.0));
				modelViewStack.rotateY(rotYInDegrees * (float)(Math.PI / 180.0));
				dynamicTransforms = RenderSystem.getDynamicUniforms()
					.writeTransform(new Matrix4f(modelViewStack), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());
			} finally {
				modelViewStack.popMatrix();
			}

			try (RenderPass renderPass = RenderSystem.getDevice()
				.createCommandEncoder()
				.createRenderPass(() -> "FancyMenu Cubemap", colorTexture, OptionalInt.empty(), depthTexture, OptionalDouble.empty())) {
				renderPass.setPipeline(renderPipeline);
				RenderSystem.bindDefaultUniforms(renderPass);
				renderPass.setVertexBuffer(0, this.vertexBuffer);
				renderPass.setIndexBuffer(indexBuffer, indices.type());
				renderPass.setUniform("DynamicTransforms", dynamicTransforms);
				AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(this.location);
				renderPass.bindTexture("Sampler0", texture.getTextureView(), texture.getSampler());
				renderPass.drawIndexed(0, 0, 36, 1);
			}
		} finally {
			RenderSystem.restoreProjectionMatrix();
		}
	}

	@NotNull
	private static GpuBuffer initializeVertices() {
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
				return RenderSystem.getDevice().createBuffer(() -> "FancyMenu cube map vertex buffer", 32, meshData.vertexBuffer());
			}
		}
	}

	@NotNull
	private static GpuTextureView getOutputColorTexture() {
		GpuTextureView outputColorTexture = RenderSystem.outputColorTextureOverride;
		if (outputColorTexture != null) {
			return outputColorTexture;
		}

		RenderTarget mainRenderTarget = Minecraft.getInstance().getMainRenderTarget();
		return mainRenderTarget.getColorTextureView();
	}

	@NotNull
	private static GpuTextureView getOutputDepthTexture() {
		GpuTextureView outputDepthTexture = RenderSystem.outputDepthTextureOverride;
		if (outputDepthTexture != null) {
			return outputDepthTexture;
		}

		RenderTarget mainRenderTarget = Minecraft.getInstance().getMainRenderTarget();
		return mainRenderTarget.getDepthTextureView();
	}

	@Override
	public void close() {
		this.vertexBuffer.close();
		this.projectionMatrixBuffer.close();
	}

}
