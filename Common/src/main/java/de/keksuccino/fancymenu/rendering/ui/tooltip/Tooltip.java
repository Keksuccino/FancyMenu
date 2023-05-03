package de.keksuccino.fancymenu.rendering.ui.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import de.keksuccino.fancymenu.rendering.texture.SimpleTexture;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A tooltip that gets rendered at the mouse position by default.<br>
 * It's possible to set a custom X and Y position to not render it at the mouse position.
 **/
public class Tooltip extends GuiComponent implements Renderable {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final Color DEFAULT_BACKGROUND_COLOR = new Color(26, 26, 26, 250);

    protected Font font = Minecraft.getInstance().font;
    protected List<Component> textLines = new ArrayList<>();
    protected int width = 0;
    protected int height = 0;
    protected int aspectWidth = -1;
    protected int aspectHeight = -1;
    protected Integer x = null;
    protected Integer y = null;
    protected int borderSize = 5;
    protected int mouseOffset = 12;
    protected SimpleTexture backgroundTexture = null;
    protected Color backgroundColor = null;
    protected boolean vanillaLike = true;
    protected boolean keepBackgroundAspectRatio = true;
    protected boolean textShadow = true;
    protected TooltipTextAlignment textAlignment = TooltipTextAlignment.LEFT;
    protected Color textBaseColor = null;

    @NotNull
    public static Tooltip create() {
        return new Tooltip();
    }

    @NotNull
    public static Tooltip create(String... tooltip) {
        Tooltip t = new Tooltip();
        if (tooltip != null) {
            t.setTooltipText(tooltip);
        }
        return t;
    }

    @NotNull
    public static Tooltip create(Component... tooltip) {
        Tooltip t = new Tooltip();
        if (tooltip != null) {
            t.setTooltipText(tooltip);
        }
        return t;
    }

    @Override
    public void render(PoseStack matrix, int mouseX, int mouseY, float partial) {
        Screen s = Minecraft.getInstance().screen;
        if (!this.isEmpty() && (s != null)) {

            this.updateAspectRatio();

            int x = this.calculateX(s, mouseX);
            int y = this.calculateY(s, mouseY);

            RenderSystem.enableBlend();
            RenderUtils.setZLevelPre(matrix, 400);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            this.renderBackground(matrix, x, y);
            this.renderTextLines(matrix, x, y);

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderUtils.setZLevelPost(matrix);
            RenderSystem.disableBlend();

        }
    }

    protected void renderTextLines(PoseStack matrix, int x, int y) {
        int yLine = y;
        for (Component c : this.textLines) {
            int w = this.font.width(c);
            int x2 = x + this.borderSize;
            int y2 = yLine + this.borderSize;
            if (this.textAlignment == TooltipTextAlignment.RIGHT) {
                int diff = Math.max(0, (x + this.getWidth() - this.borderSize) - (x2 + w));
                x2 += diff;
            }
            if (this.textAlignment == TooltipTextAlignment.CENTERED) {
                x2 = x + Math.max(0, (this.getWidth() / 2) - (w / 2));
            }
            if (this.hasTextShadow()) {
                this.font.drawShadow(matrix, c, x2, y2, (this.textBaseColor != null) ? this.textBaseColor.getRGB() : -1);
            } else {
                this.font.draw(matrix, c, x2, y2, (this.textBaseColor != null) ? this.textBaseColor.getRGB() : -1);
            }
            yLine += this.font.lineHeight + 2;
        }
    }

    protected void renderBackground(PoseStack matrix, int x, int y) {
        if (this.vanillaLike || ((this.backgroundTexture == null) && (this.backgroundColor == null))) {
            this.renderVanillaLikeBackground(matrix, x, y, this.getWidth(), this.getHeight());
        } else if (this.backgroundTexture != null) {
            RenderUtils.bindTexture(this.backgroundTexture.getTextureLocation());
            blit(matrix, x, y, 0.0F, 0.0F, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());
        } else {
            fill(matrix, x, y, x + this.getWidth(), y + this.getHeight(), this.backgroundColor.getRGB());
        }
    }

    protected void renderVanillaLikeBackground(PoseStack matrix, int x, int y, int width, int height) {

        matrix.pushPose();

        ShaderInstance shaderInstance = RenderSystem.getShader();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder2 = tesselator.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bufferBuilder2.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix4f2 = matrix.last().pose();

        TooltipRenderUtil.renderTooltipBackground(GuiComponent::fillGradient, matrix4f2, bufferBuilder2, x, y, width, height, 400);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        BufferUploader.drawWithShader(bufferBuilder2.end());
        if (shaderInstance != null) {
            RenderSystem.setShader(() -> shaderInstance);
        }

        matrix.popPose();

    }

    protected int calculateX(Screen screen, int mouseX) {
        if (this.x != null) {
            mouseX = this.x;
        }
        mouseX += this.mouseOffset;
        int w = this.getWidth();
        if (this.vanillaLike) {
            w += 4;
        }
        if ((mouseX + w) > screen.width) {
            return mouseX - ((mouseX + w) - screen.width);
        }
        return mouseX;
    }

    protected int calculateY(Screen screen, int mouseY) {
        if (this.y != null) {
            mouseY = this.y;
        }
        mouseY += this.mouseOffset;
        int h = this.getHeight();
        if (this.vanillaLike) {
            h += 4;
        }
        if ((mouseY + h) > screen.height) {
            return mouseY - ((mouseY + h) - screen.height);
        }
        return mouseY;
    }

    protected int getWidth() {
        if (this.aspectWidth != -1) {
            return this.aspectWidth;
        }
        return this.width;
    }

    protected int getHeight() {
        if (this.aspectHeight != -1) {
            return this.aspectHeight;
        }
        return this.height;
    }

    protected void updateAspectRatio() {
        if (!this.keepBackgroundAspectRatio || (this.backgroundTexture == null)) {
            this.aspectWidth = -1;
            this.aspectHeight = -1;
            return;
        }
        int[] ar = this.backgroundTexture.getAspectRatio().getAspectRatioSize(this.width, this.height);
        this.aspectWidth = ar[0];
        this.aspectHeight = ar[1];
    }

    protected void updateSize() {
        int w = 0;
        int h = 0;
        for (Component c : this.textLines) {
            int wl = this.font.width(c);
            if (wl > w) {
                w = wl;
            }
            h += this.font.lineHeight + 2;
        }
        this.width = w + (this.borderSize * 2);
        this.height = h + (this.borderSize * 2);
    }

    public boolean isEmpty() {
        return this.textLines.isEmpty();
    }

    public void setTooltipText(String... lines) {
        List<Component> l = new ArrayList<>();
        if (lines != null) {
            for (String s : lines) {
                l.add(Component.literal(s));
            }
        }
        this.setTooltipText(l);
    }

    public void setTooltipText(Component... lines) {
        this.setTooltipText((lines != null) ? Arrays.asList(lines) : null);
    }

    public void setTooltipText(List<Component> lines) {
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

    public void setBackgroundTexture(ResourceLocation texture) {
        this.backgroundTexture = SimpleTexture.create(texture);
        this.backgroundColor = null;
        this.vanillaLike = (texture == null);
    }

    public SimpleTexture getBackgroundTexture() {
        return backgroundTexture;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
        this.backgroundTexture = null;
        this.vanillaLike = (backgroundColor == null);
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setVanillaLike(boolean vanillaLike) {
        this.vanillaLike = vanillaLike;
        this.backgroundColor = null;
        this.backgroundTexture = null;
    }

    public boolean isVanillaLike() {
        return vanillaLike;
    }

    public void setKeepBackgroundAspectRatio(boolean keepBackgroundAspectRatio) {
        this.keepBackgroundAspectRatio = keepBackgroundAspectRatio;
    }

    public boolean keepBackgroundAspectRatio() {
        return keepBackgroundAspectRatio;
    }

    public void setTextShadow(boolean textShadow) {
        this.textShadow = textShadow;
    }

    public boolean hasTextShadow() {
        return textShadow;
    }

    public void setTextAlignment(TooltipTextAlignment textAlignment) {
        this.textAlignment = textAlignment;
    }

    public TooltipTextAlignment getTextAlignment() {
        return textAlignment;
    }

    public void setTextBaseColor(@Nullable Color textBaseColor) {
        this.textBaseColor = textBaseColor;
    }

    @Nullable
    public Color getTextBaseColor() {
        return textBaseColor;
    }

    public void setFont(Font font) {
        this.font = (font != null) ? font : Minecraft.getInstance().font;
        this.updateSize();
    }

    public Font getFont() {
        return font;
    }

    public void setCustomX(@Nullable Integer x) {
        this.x = x;
    }

    @Nullable
    public Integer getCustomX() {
        return x;
    }

    public void setCustomY(@Nullable Integer y) {
        this.y = y;
    }

    @Nullable
    public Integer getCustomY() {
        return y;
    }

    public enum TooltipTextAlignment {
        LEFT,
        RIGHT,
        CENTERED
    }

}
