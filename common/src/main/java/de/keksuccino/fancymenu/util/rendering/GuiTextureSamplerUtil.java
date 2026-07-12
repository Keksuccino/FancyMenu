package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import javax.annotation.Nonnull;
import java.util.Objects;

final class GuiTextureSamplerUtil {

    private static final TextureParameterBackend OPEN_GL_BACKEND = new OpenGlTextureParameterBackend();

    private GuiTextureSamplerUtil() {
    }

    /**
     * Temporarily clamps a texture to its edges without changing its filtering or leaking texture state.
     * Smooth image post effects sample partially covered boundary fragments, where repeat wrapping would expose
     * pixels from the opposite texture edge. The original wrapping, active unit, and binding must survive even
     * when the post effect changes OpenGL state or fails.
     */
    static void runWithClampToEdge(int textureId, @Nonnull Runnable action) {
        runWithClampToEdge(textureId, action, OPEN_GL_BACKEND);
    }

    static void runWithClampToEdge(int textureId, @Nonnull Runnable action, @Nonnull TextureParameterBackend backend) {
        Objects.requireNonNull(action);
        Objects.requireNonNull(backend);
        int previousActiveTexture = backend.getActiveTexture();
        int previousTextureBinding = backend.getBoundTexture2D();
        try {
            backend.bindTexture2D(textureId);
            int previousWrapS = backend.getTextureParameter(GL11.GL_TEXTURE_WRAP_S);
            int previousWrapT = backend.getTextureParameter(GL11.GL_TEXTURE_WRAP_T);
            boolean restoreWrapS = previousWrapS != GL12.GL_CLAMP_TO_EDGE;
            boolean restoreWrapT = previousWrapT != GL12.GL_CLAMP_TO_EDGE;
            try {
                if (restoreWrapS) {
                    backend.setTextureParameter(GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                }
                if (restoreWrapT) {
                    backend.setTextureParameter(GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
                }
                backend.bindTexture2D(previousTextureBinding);
                action.run();
            } finally {
                // The action can leave any texture unit and texture active, so re-select the target before restoring it.
                backend.setActiveTexture(previousActiveTexture);
                backend.bindTexture2D(textureId);
                if (restoreWrapS) {
                    backend.setTextureParameter(GL11.GL_TEXTURE_WRAP_S, previousWrapS);
                }
                if (restoreWrapT) {
                    backend.setTextureParameter(GL11.GL_TEXTURE_WRAP_T, previousWrapT);
                }
            }
        } finally {
            backend.setActiveTexture(previousActiveTexture);
            backend.bindTexture2D(previousTextureBinding);
        }
    }

    interface TextureParameterBackend {

        int getActiveTexture();

        int getBoundTexture2D();

        int getTextureParameter(int parameter);

        void setActiveTexture(int textureUnit);

        void bindTexture2D(int textureId);

        void setTextureParameter(int parameter, int value);
    }

    private static final class OpenGlTextureParameterBackend implements TextureParameterBackend {

        @Override
        public int getActiveTexture() {
            return GlStateManager._getActiveTexture();
        }

        @Override
        public int getBoundTexture2D() {
            return GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        }

        @Override
        public int getTextureParameter(int parameter) {
            return GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, parameter);
        }

        @Override
        public void setActiveTexture(int textureUnit) {
            GlStateManager._activeTexture(textureUnit);
        }

        @Override
        public void bindTexture2D(int textureId) {
            GlStateManager._bindTexture(textureId);
        }

        @Override
        public void setTextureParameter(int parameter, int value) {
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, parameter, value);
        }
    }
}
