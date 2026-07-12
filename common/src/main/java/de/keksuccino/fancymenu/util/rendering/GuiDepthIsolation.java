package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

import java.util.Objects;

/**
 * Isolates GUI draws that need depth testing internally but must not leave depth behind for later GUI layers.
 */
public final class GuiDepthIsolation {

    private GuiDepthIsolation() {
    }

    public static void finishDepthWritingDraw(@NotNull GuiGraphics graphics) {
        finishDepthWritingDraw(graphics::flush, () -> {
            RenderSystem.depthMask(true);
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        });
    }

    static void finishDepthWritingDraw(@NotNull Runnable flush, @NotNull Runnable clearDepth) {
        Runnable checkedFlush = Objects.requireNonNull(flush);
        Runnable checkedClearDepth = Objects.requireNonNull(clearDepth);
        try {
            checkedFlush.run();
        } finally {
            checkedClearDepth.run();
        }
    }

}
