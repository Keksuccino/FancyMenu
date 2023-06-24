package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class RenderUtils extends de.keksuccino.konkrete.rendering.RenderUtils {

    public static float getPartialTick() {
        return Minecraft.getInstance().isPaused() ? ((IMixinMinecraft)Minecraft.getInstance()).getPausePartialTickFancyMenu() : Minecraft.getInstance().getFrameTime();
    }

    public static void resetGuiScale() {
        Window m = Minecraft.getInstance().getWindow();
        m.setGuiScale(m.calculateScale(Minecraft.getInstance().options.guiScale().get(), Minecraft.getInstance().options.forceUnicodeFont().get()));
    }

    /**
     * @param color The color.
     * @param newAlpha Value between 0 and 255.
     * @return The given color with new alpha.
     */
    public static int replaceAlphaInColor(int color, int newAlpha) {
        newAlpha = Math.min(newAlpha, 255);
        return color & 16777215 | newAlpha << 24;
    }

    /**
     * @param color The color.
     * @param newAlpha Value between 0.0F and 1.0F.
     * @return The given color with new alpha.
     */
    public static int replaceAlphaInColor(int color, float newAlpha) {
        return replaceAlphaInColor(color, (int)(newAlpha * 255.0F));
    }

//    public static void renderBlurredTexture(@NotNull PoseStack pose, @NotNull ResourceLocation texture, int x, int y, int width, int height) {
//
//        float xF = x;
//        float yF = y;
//
//        RenderSystem.enableBlend();
//
//        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
//        RenderUtils.bindTexture(texture);
//        blitF(pose, xF, yF, 0.0F, 0.0F, width, height, width, height);
//
//        for (float f = 0; f < 1; f += 0.007F) {
//            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.05F);
//            blitF(pose, xF + f, yF, 0.0F, 0.0F, width, height, width, height);
//            blitF(pose, xF - f, yF, 0.0F, 0.0F, width, height, width, height);
//            blitF(pose, xF, yF + f, 0.0F, 0.0F, width, height, width, height);
//            blitF(pose, xF, yF - f, 0.0F, 0.0F, width, height, width, height);
//            blitF(pose, xF - f, yF - f, 0.0F, 0.0F, width, height, width, height);
//            blitF(pose, xF + f, yF - f, 0.0F, 0.0F, width, height, width, height);
//            blitF(pose, xF + f, yF + f, 0.0F, 0.0F, width, height, width, height);
//            blitF(pose, xF - f, yF + f, 0.0F, 0.0F, width, height, width, height);
//        }
//
//        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
//
//    }

    public static void blitF(PoseStack $$0, float $$1, float $$2, float $$3, float $$4, int $$5, int $$6, int $$7, int $$8) {
        blit($$0, $$1, $$2, $$5, $$6, $$3, $$4, $$5, $$6, $$7, $$8);
    }

    private static void blit(PoseStack $$0, float $$1, float $$2, float $$3, float $$4, float $$5, float $$6, int $$7, int $$8, int $$9, int $$10) {
        blit($$0, $$1, $$1 + $$3, $$2, $$2 + $$4, 0, $$7, $$8, $$5, $$6, $$9, $$10);
    }

    private static void blit(PoseStack $$0, float $$1, float $$2, float $$3, float $$4, float $$5, float $$6, float $$7, float $$8, float $$9, int $$10, int $$11) {
        innerBlit($$0.last().pose(), $$1, $$2, $$3, $$4, $$5, ($$8 + 0.0F) / (float)$$10, ($$8 + (float)$$6) / (float)$$10, ($$9 + 0.0F) / (float)$$11, ($$9 + (float)$$7) / (float)$$11);
    }

    private static void innerBlit(Matrix4f $$0, float $$1, float $$2, float $$3, float $$4, float $$5, float $$6, float $$7, float $$8, float $$9) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder $$10 = Tesselator.getInstance().getBuilder();
        $$10.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        $$10.vertex($$0, (float)$$1, (float)$$3, (float)$$5).uv($$6, $$8).endVertex();
        $$10.vertex($$0, (float)$$1, (float)$$4, (float)$$5).uv($$6, $$9).endVertex();
        $$10.vertex($$0, (float)$$2, (float)$$4, (float)$$5).uv($$7, $$9).endVertex();
        $$10.vertex($$0, (float)$$2, (float)$$3, (float)$$5).uv($$7, $$8).endVertex();
        BufferUploader.drawWithShader($$10.end());
    }

}
