package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import javax.annotation.Nonnull;
import java.util.Objects;

final class GuiTextureSamplerUtil {

    private GuiTextureSamplerUtil() {
    }

    /**
     * Runs a draw with clamp-to-edge wrapping without permanently changing the shared texture object.
     * Minecraft 1.19.2 binds raw texture IDs instead of separate sampler objects, so wrap modes live on the
     * texture itself. The exact modes and binding must be restored after the synchronous post pass because the
     * same texture can be rendered elsewhere with repeating coordinates later in the frame.
     */
    static void runWithClampToEdge(int textureId, @Nonnull Runnable draw) {
        Objects.requireNonNull(draw);
        RenderSystem.assertOnRenderThread();

        int activeTexture = GlStateManager._getActiveTexture();
        int boundTexture = GlStateManager._getInteger(GL11.GL_TEXTURE_BINDING_2D);
        RenderSystem.bindTexture(textureId);
        int wrapS = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S);
        int wrapT = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T);

        try {
            if (wrapS != GL12.GL_CLAMP_TO_EDGE) {
                RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            }
            if (wrapT != GL12.GL_CLAMP_TO_EDGE) {
                RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            }
            draw.run();
        } finally {
            // The post chain is free to leave another texture unit active, so select the captured unit before restoring.
            RenderSystem.activeTexture(activeTexture);
            RenderSystem.bindTexture(textureId);
            try {
                if (wrapS != GL12.GL_CLAMP_TO_EDGE) {
                    RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, wrapS);
                }
                if (wrapT != GL12.GL_CLAMP_TO_EDGE) {
                    RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, wrapT);
                }
            } finally {
                RenderSystem.bindTexture(boundTexture);
                RenderSystem.activeTexture(activeTexture);
            }
        }
    }
}
