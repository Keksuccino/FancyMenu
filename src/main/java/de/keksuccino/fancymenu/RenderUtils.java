package de.keksuccino.fancymenu;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class RenderUtils extends de.keksuccino.konkrete.rendering.RenderUtils {

    public static void doubleBlit(GuiGraphics graphics, ResourceLocation resourceLocation, double x, double y, float f1, float f2, int w, int h) {
        innerDoubleBlit(graphics, resourceLocation, x, x + (double)w, y, y + (double)h, 0, (f1 + 0.0F) / (float)w, (f1 + (float)w) / (float)w, (f2 + 0.0F) / (float)h, (f2 + (float)h) / (float)h);
    }

    public static void innerDoubleBlit(GuiGraphics graphics, ResourceLocation resourceLocation, double x, double xEnd, double y, double yEnd, int z, float f1, float f2, float f3, float f4) {
        RenderSystem.setShaderTexture(0, resourceLocation);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix4f = graphics.pose().last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.vertex(matrix4f, (float)x, (float)y, (float)z).uv(f1, f3).endVertex();
        bufferbuilder.vertex(matrix4f, (float)x, (float)yEnd, (float)z).uv(f1, f4).endVertex();
        bufferbuilder.vertex(matrix4f, (float)xEnd, (float)yEnd, (float)z).uv(f2, f4).endVertex();
        bufferbuilder.vertex(matrix4f, (float)xEnd, (float)y, (float)z).uv(f2, f3).endVertex();
        BufferUploader.drawWithShader(bufferbuilder.end());
    }

}
