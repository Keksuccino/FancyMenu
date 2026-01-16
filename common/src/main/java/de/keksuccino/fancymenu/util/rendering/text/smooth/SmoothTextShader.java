package de.keksuccino.fancymenu.util.rendering.text.smooth;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.MinecraftResourceReloadObserver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

final class SmoothTextShader {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String SHADER_NAME = "fancymenu_gui_smooth_text";
    private static final float DEFAULT_SDF_SHARPNESS = 1.0F;
    private static final float DEFAULT_SDF_EDGE = 0.5F;
    private static final float DEFAULT_SDF_PIXEL_RANGE = 4.0F;

    private static ShaderInstance shader;
    private static boolean shaderFailed;
    private static boolean reloadListenerRegistered;
    private static volatile Float runtimeSharpness;
    private static volatile Float runtimeEdge;

    private SmoothTextShader() {
    }

    @Nullable
    static ShaderInstance getShader() {
        ensureReloadListener();
        if (shaderFailed) {
            return GameRenderer.getPositionTexColorShader();
        }
        if (shader == null) {
            try {
                shader = new ShaderInstance(Minecraft.getInstance().getResourceManager(), SHADER_NAME, DefaultVertexFormat.POSITION_TEX_COLOR);
            } catch (Exception ex) {
                shaderFailed = true;
                LOGGER.error("[FANCYMENU] Failed to load smooth text shader!", ex);
                return GameRenderer.getPositionTexColorShader();
            }
        }
        return shader;
    }

    static void applyDefaults() {
        ShaderInstance current = getShader();
        if (current == null) {
            return;
        }
        float sharpness = runtimeSharpness != null ? runtimeSharpness : DEFAULT_SDF_SHARPNESS;
        float edge = runtimeEdge != null ? runtimeEdge : DEFAULT_SDF_EDGE;
        current.safeGetUniform("SdfSharpness").set(sharpness);
        current.safeGetUniform("SdfEdge").set(edge);
        current.safeGetUniform("SdfPixelRange").set(DEFAULT_SDF_PIXEL_RANGE);
    }

    static void applySdfRange(float sdfRange) {
        ShaderInstance current = RenderSystem.getShader();
        if (current == null) {
            return;
        }
        float range = Math.max(0.5F, sdfRange);
        current.safeGetUniform("SdfPixelRange").set(range);
    }

    static void applyEdge(float edge) {
        ShaderInstance current = RenderSystem.getShader();
        if (current == null) {
            return;
        }
        current.safeGetUniform("SdfEdge").set(edge);
    }

    static void applySharpness(float sharpness) {
        ShaderInstance current = RenderSystem.getShader();
        if (current == null) {
            return;
        }
        current.safeGetUniform("SdfSharpness").set(sharpness);
    }

    static void applyUseTrueSdf(boolean useTrueSdf) {
        ShaderInstance current = RenderSystem.getShader();
        if (current == null) {
            return;
        }
        current.safeGetUniform("UseTrueSdf").set(useTrueSdf ? 1 : 0);
    }

    static void setRuntimeSharpness(float sharpness) {
        runtimeSharpness = Math.max(0.05F, sharpness);
    }

    static void clearRuntimeSharpness() {
        runtimeSharpness = null;
    }

    static float getResolvedSharpness() {
        return runtimeSharpness != null ? runtimeSharpness : DEFAULT_SDF_SHARPNESS;
    }

    static void setRuntimeEdge(float edge) {
        runtimeEdge = edge;
    }

    static void clearRuntimeEdge() {
        runtimeEdge = null;
    }

    static float getResolvedEdge() {
        return runtimeEdge != null ? runtimeEdge : DEFAULT_SDF_EDGE;
    }

    static void clear() {
        if (shader != null) {
            shader.close();
            shader = null;
        }
        shaderFailed = false;
    }

    private static void ensureReloadListener() {
        if (reloadListenerRegistered) {
            return;
        }
        reloadListenerRegistered = true;
        MinecraftResourceReloadObserver.addReloadListener(action -> {
            if (action == MinecraftResourceReloadObserver.ReloadAction.STARTING) {
                RenderSystem.recordRenderCall(SmoothTextShader::clear);
            }
        });
    }

}
