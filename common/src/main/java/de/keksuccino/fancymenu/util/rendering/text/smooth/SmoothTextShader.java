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
    private static final boolean DEBUG_LOG = Boolean.getBoolean("fancymenu.debugSmoothTextShader");

    private static ShaderInstance shader;
    private static boolean shaderFailed;
    private static boolean reloadListenerRegistered;
    private static boolean debugLogged;

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
        current.safeGetUniform("SdfSharpness").set(DEFAULT_SDF_SHARPNESS);
        current.safeGetUniform("SdfEdge").set(DEFAULT_SDF_EDGE);
        if (DEBUG_LOG && !debugLogged) {
            boolean sharpnessPresent = current.getUniform("SdfSharpness") != null;
            boolean edgePresent = current.getUniform("SdfEdge") != null;
            LOGGER.info("[FANCYMENU] SmoothTextShader active: name={} id={} sharpnessUniform={} edgeUniform={} sharpness={} edge={}",
                    current.getName(),
                    current.getId(),
                    sharpnessPresent,
                    edgePresent,
                    DEFAULT_SDF_SHARPNESS,
                    DEFAULT_SDF_EDGE
            );
            debugLogged = true;
        }
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

}
