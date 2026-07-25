package de.keksuccino.fancymenu.util.rendering.glsl;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;

/** Lazily allocated floating-point feedback target for one Shadertoy-style Buffer pass. */
final class GlslPingPongTarget implements AutoCloseable {

    private final String label;
    @Nullable
    private TextureTarget readTarget;
    @Nullable
    private TextureTarget writeTarget;
    private int width;
    private int height;

    GlslPingPongTarget(@NotNull String label) {
        this.label = label;
    }

    boolean ensureSize(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("GLSL feedback target dimensions must be positive.");
        }
        if (this.readTarget != null && this.writeTarget != null && this.width == width && this.height == height) {
            return false;
        }

        this.close();
        this.readTarget = new TextureTarget(this.label + " read", width, height, false, GpuFormat.RGBA16_FLOAT);
        this.writeTarget = new TextureTarget(this.label + " write", width, height, false, GpuFormat.RGBA16_FLOAT);
        this.width = width;
        this.height = height;

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.clearColorTexture(this.readTarget.getColorTexture(), new Vector4f(0.0F));
        encoder.clearColorTexture(this.writeTarget.getColorTexture(), new Vector4f(0.0F));
        return true;
    }

    boolean isReady() {
        return this.readTarget != null && this.writeTarget != null && this.readTarget.getColorTextureView() != null && this.writeTarget.getColorTextureView() != null;
    }

    @Nullable
    GpuTextureView readView() {
        return this.readTarget == null ? null : this.readTarget.getColorTextureView();
    }

    @Nullable
    GpuTextureView writeView() {
        return this.writeTarget == null ? null : this.writeTarget.getColorTextureView();
    }

    int width() {
        return this.width;
    }

    int height() {
        return this.height;
    }

    void swap() {
        if (!this.isReady()) {
            throw new IllegalStateException("Cannot swap an uninitialized GLSL feedback target.");
        }
        TextureTarget previousRead = this.readTarget;
        this.readTarget = this.writeTarget;
        this.writeTarget = previousRead;
    }

    @Override
    public void close() {
        RenderSystem.assertOnRenderThread();
        if (this.readTarget != null) {
            this.readTarget.destroyBuffers();
            this.readTarget = null;
        }
        if (this.writeTarget != null) {
            this.writeTarget.destroyBuffers();
            this.writeTarget = null;
        }
        this.width = 0;
        this.height = 0;
    }
}
