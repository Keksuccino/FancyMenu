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
    private static final float DEFAULT_SDF_SHARPNESS = getFloatProperty("fancymenu.smoothTextSharpness", 1.0F);
    private static final float DEFAULT_SDF_EDGE = getFloatProperty("fancymenu.smoothTextEdge", 0.5F);
    private static final float DEFAULT_SDF_PIXEL_RANGE = 4.0F;
    private static final boolean DEBUG_LOG = Boolean.getBoolean("fancymenu.debugSmoothTextShader");
    private static final int DEBUG_MODE = Math.max(0, Integer.getInteger("fancymenu.debugSmoothTextMode", 0));

    private static ShaderInstance shader;
    private static boolean shaderFailed;
    private static boolean reloadListenerRegistered;
    private static boolean debugLogged;
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
                debugLogged = false;
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
        if (DEBUG_MODE == 1) {
            sharpness = -1.0F;
        } else if (DEBUG_MODE == 2) {
            edge = -1.0F;
        }
        current.safeGetUniform("SdfSharpness").set(sharpness);
        current.safeGetUniform("SdfEdge").set(edge);
        current.safeGetUniform("SdfPixelRange").set(DEFAULT_SDF_PIXEL_RANGE);
        current.safeGetUniform("DebugMode").set(DEBUG_MODE);
        if (DEBUG_LOG && !debugLogged) {
            boolean sharpnessPresent = current.getUniform("SdfSharpness") != null;
            boolean edgePresent = current.getUniform("SdfEdge") != null;
            boolean pixelRangePresent = current.getUniform("SdfPixelRange") != null;
            boolean debugPresent = current.getUniform("DebugMode") != null;
            LOGGER.info("[FANCYMENU] SmoothTextShader active: name={} id={} sharpnessUniform={} edgeUniform={} pixelRangeUniform={} sharpness={} edge={} pixelRange={}",
                    current.getName(),
                    current.getId(),
                    sharpnessPresent,
                    edgePresent,
                    pixelRangePresent,
                    sharpness,
                    edge,
                    DEFAULT_SDF_PIXEL_RANGE
            );
            if (debugPresent && DEBUG_MODE != 0) {
                LOGGER.info("[FANCYMENU] SmoothTextShader debug mode active: {}", DEBUG_MODE);
            }
            debugLogged = true;
        }
    }

    static void applySdfRange(float sdfRange) {
        ShaderInstance current = RenderSystem.getShader();
        if (current == null) {
            return;
        }
        float range = Math.max(0.5F, sdfRange);
        current.safeGetUniform("SdfPixelRange").set(range);
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
        debugLogged = false;
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

    private static float getFloatProperty(String key, float fallback) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException ex) {
            LOGGER.warn("[FANCYMENU] Invalid float for {}: {}", key, value);
            return fallback;
        }
    }

}
