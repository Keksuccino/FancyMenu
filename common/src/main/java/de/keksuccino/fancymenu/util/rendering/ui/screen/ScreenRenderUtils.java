package de.keksuccino.fancymenu.util.rendering.ui.screen;

import net.minecraft.client.gui.GuiGraphics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ScreenRenderUtils {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final List<ScreenRenderContext> PRE_RENDER_CONTEXTS = new ArrayList<>();
    private static final List<ScreenRenderContext> POST_RENDER_CONTEXTS = new ArrayList<>();

    public static void postPreRenderTask(@NotNull ScreenRenderContext context) {
        PRE_RENDER_CONTEXTS.add(Objects.requireNonNull(context));
    }

    public static void postPostRenderTask(@NotNull ScreenRenderContext context) {
        POST_RENDER_CONTEXTS.add(Objects.requireNonNull(context));
    }

    @ApiStatus.Internal
    public static void executeAllPreRenderTasks(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        List<ScreenRenderContext> copy = new ArrayList<>(PRE_RENDER_CONTEXTS);
        PRE_RENDER_CONTEXTS.clear();
        for (ScreenRenderContext context : copy) {
            try {
                context.render(graphics, mouseX, mouseY, partial);
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to execute pre-screen-render task!", ex);
            }
        }
    }

    @ApiStatus.Internal
    public static void executeAllPostRenderTasks(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        List<ScreenRenderContext> copy = new ArrayList<>(POST_RENDER_CONTEXTS);
        POST_RENDER_CONTEXTS.clear();
        for (ScreenRenderContext context : copy) {
            try {
                context.render(graphics, mouseX, mouseY, partial);
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to execute post-screen-render task!", ex);
            }
        }
    }

    @FunctionalInterface
    public interface ScreenRenderContext {
        void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial);
    }

}
