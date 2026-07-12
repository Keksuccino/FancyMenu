package de.keksuccino.fancymenu.util.rendering.glsl;

import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

/** Captures every OpenGL state mutated by the GLSL runtime so following Minecraft rendering starts unchanged. */
final class GlslOpenGlStateSnapshot {

    private final int[] viewport = new int[4];
    private final int[] scissorBox = new int[4];
    private final int[] textureBindings;
    private final int activeTexture;
    private final int program;
    private final int vertexArray;
    private final int arrayBuffer;
    private final int readFramebuffer;
    private final int drawFramebuffer;
    private final int blendSourceRgb;
    private final int blendDestinationRgb;
    private final int blendSourceAlpha;
    private final int blendDestinationAlpha;
    private final int blendEquationRgb;
    private final int blendEquationAlpha;
    private final int colorWriteMask;
    private final boolean blendEnabled;
    private final boolean depthTestEnabled;
    private final boolean depthWriteEnabled;
    private final boolean cullEnabled;
    private final boolean scissorEnabled;

    GlslOpenGlStateSnapshot(int textureUnitCount) {
        this.textureBindings = new int[textureUnitCount];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, this.viewport);
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, this.scissorBox);
        this.activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        this.program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        this.vertexArray = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        this.arrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        this.readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        this.drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        this.blendSourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        this.blendDestinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        this.blendSourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        this.blendDestinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        this.blendEquationRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB);
        this.blendEquationAlpha = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer colorMask = stack.malloc(4);
            GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, colorMask);
            this.colorWriteMask = (colorMask.get(0) != 0 ? 1 : 0) | (colorMask.get(1) != 0 ? 2 : 0) | (colorMask.get(2) != 0 ? 4 : 0) | (colorMask.get(3) != 0 ? 8 : 0);
        }
        this.blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        this.depthTestEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        this.depthWriteEnabled = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        this.cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        this.scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        for (int i = 0; i < this.textureBindings.length; i++) {
            GlStateManager._activeTexture(GL13.GL_TEXTURE0 + i);
            this.textureBindings[i] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        }
        GlStateManager._activeTexture(this.activeTexture);
    }

    void restoreFramebufferBindings() {
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, this.readFramebuffer);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.drawFramebuffer);
    }

    void restore() {
        for (int i = 0; i < this.textureBindings.length; i++) {
            GlStateManager._activeTexture(GL13.GL_TEXTURE0 + i);
            GlStateManager._bindTexture(this.textureBindings[i]);
        }
        GlStateManager._activeTexture(this.activeTexture);
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, this.arrayBuffer);
        GlStateManager._glBindVertexArray(this.vertexArray);
        GlStateManager._glUseProgram(this.program);
        this.restoreFramebufferBindings();
        GlStateManager._viewport(this.viewport[0], this.viewport[1], this.viewport[2], this.viewport[3]);
        GlStateManager._colorMask((this.colorWriteMask & 1) != 0, (this.colorWriteMask & 2) != 0, (this.colorWriteMask & 4) != 0, (this.colorWriteMask & 8) != 0);
        GlStateManager._depthMask(this.depthWriteEnabled);
        setCapability(GL11.GL_BLEND, this.blendEnabled);
        setCapability(GL11.GL_DEPTH_TEST, this.depthTestEnabled);
        setCapability(GL11.GL_CULL_FACE, this.cullEnabled);
        setCapability(GL11.GL_SCISSOR_TEST, this.scissorEnabled);
        GlStateManager._scissorBox(this.scissorBox[0], this.scissorBox[1], this.scissorBox[2], this.scissorBox[3]);
        GlStateManager._blendFuncSeparate(this.blendSourceRgb, this.blendDestinationRgb, this.blendSourceAlpha, this.blendDestinationAlpha);
        GL20.glBlendEquationSeparate(this.blendEquationRgb, this.blendEquationAlpha);
    }

    private static void setCapability(int capability, boolean enabled) {
        switch (capability) {
            case GL11.GL_BLEND -> {
                if (enabled) GlStateManager._enableBlend(); else GlStateManager._disableBlend();
            }
            case GL11.GL_DEPTH_TEST -> {
                if (enabled) GlStateManager._enableDepthTest(); else GlStateManager._disableDepthTest();
            }
            case GL11.GL_CULL_FACE -> {
                if (enabled) GlStateManager._enableCull(); else GlStateManager._disableCull();
            }
            case GL11.GL_SCISSOR_TEST -> {
                if (enabled) GlStateManager._enableScissorTest(); else GlStateManager._disableScissorTest();
            }
            default -> throw new IllegalArgumentException("Unsupported OpenGL capability: " + capability);
        }
    }

}
