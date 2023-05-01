package de.keksuccino.fancymenu.rendering.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WidgetTooltip extends GuiComponent implements Renderable {

    //TODO renderTextLines() methode adden 

    //TODO handling für screen rand (wenn zu sehr an Rand, render offset, damit nicht abgeschnitten)
    // - nicht komplett auf anderer seite rendern, sondern nur so großes offset, wie zu weit über Rand

    //TODO alle alten setDescription calls mit neuem system ersetzen

    protected Font font;
    protected List<Component> textLines = new ArrayList<>();
    protected int width = 0;
    protected int height = 0;
    protected int borderSize = 3;
    protected int mouseOffset = 12;
    protected ResourceLocation backgroundImageLocation = null;
    protected Color backgroundColor = new Color(26, 26, 26, 250);
    protected boolean vanillaLike = false;

    public WidgetTooltip(Font font) {
        this.font = font;
    }

    @Override
    public void render(PoseStack matrix, int mouseX, int mouseY, float partial) {
        if (!this.isEmpty()) {
            //TODO do stuff
        }
    }

    protected void renderBackground(PoseStack matrix, int x, int y, int width, int height) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        if (this.vanillaLike || ((this.backgroundImageLocation == null) && (this.backgroundColor == null))) {
            this.renderVanillaLikeBackground(matrix, x, y, width, height);
        } else if (this.backgroundImageLocation != null) {
            RenderUtils.bindTexture(this.backgroundImageLocation);
            blit(matrix, x, y, 1.0F, 1.0F, width, height, width, height);
        } else {
            fill(matrix, x, y, x + width, y + height, this.backgroundColor.getRGB());
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    protected void renderVanillaLikeBackground(PoseStack matrix, int x, int y, int width, int height) {

        matrix.pushPose();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder2 = tesselator.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bufferBuilder2.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix4f2 = matrix.last().pose();

        TooltipRenderUtil.renderTooltipBackground(GuiComponent::fillGradient, matrix4f2, bufferBuilder2, x, y, width, height, 400);

        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        BufferUploader.drawWithShader(bufferBuilder2.end());

//        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
//        matrix.translate(0.0f, 0.0f, 400.0f);
//        int s = y;
//        for (t = 0; t < list.size(); ++t) {
//            clientTooltipComponent2 = list.get(t);
//            clientTooltipComponent2.renderText(this.font, x, s, matrix4f2, bufferSource);
//            s += clientTooltipComponent2.getHeight() + (t == 0 ? 2 : 0);
//        }
//        bufferSource.endBatch();
//        s = y;
//        for (t = 0; t < list.size(); ++t) {
//            clientTooltipComponent2 = list.get(t);
//            clientTooltipComponent2.renderImage(this.font, x, s, matrix, this.itemRenderer);
//            s += clientTooltipComponent2.getHeight() + (t == 0 ? 2 : 0);
//        }

        matrix.popPose();

    }

    protected void updateSize() {
        int w = 0;
        int h = 0;
        for (Component c : this.textLines) {
            int wl = this.font.width(c);
            if (wl > w) {
                w = wl;
            }
            h += this.font.lineHeight;
        }
        this.width = w + (this.borderSize * 2);
        this.height = h + (this.borderSize * 2);
    }

    public boolean isEmpty() {
        return this.textLines.isEmpty();
    }

    public void setTooltip(String... lines) {
        List<Component> l = new ArrayList<>();
        if (lines != null) {
            for (String s : lines) {
                l.add(Component.literal(s));
            }
        }
        this.setTooltip(l);
    }

    public void setTooltip(Component... lines) {
        this.setTooltip((lines != null) ? Arrays.asList(lines) : null);
    }

    public void setTooltip(List<Component> lines) {
        this.textLines = (lines != null) ? lines : new ArrayList<>();
        this.updateSize();
    }

    /** Returns a COPY of the tooltip list. **/
    public List<Component> getTooltip() {
        return new ArrayList<>(this.textLines);
    }

    public void setBorderSize(int size) {
        this.borderSize = size;
        this.updateSize();
    }

    public int getBorderSize() {
        return this.borderSize;
    }

    public void setMouseOffset(int offset) {
        this.mouseOffset = offset;
    }

    public int getMouseOffset() {
        return this.mouseOffset;
    }

    public void setBackgroundImageLocation(ResourceLocation backgroundImageLocation) {
        this.backgroundImageLocation = backgroundImageLocation;
    }

    public ResourceLocation getBackgroundImageLocation() {
        return backgroundImageLocation;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setVanillaLike(boolean vanillaLike) {
        this.vanillaLike = vanillaLike;
    }

    public boolean isVanillaLike() {
        return vanillaLike;
    }

}
