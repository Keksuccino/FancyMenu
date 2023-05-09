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
    public static final Color DEFAULT_BORDER_COLOR = new Color(64, 64, 64, 250);

    protected Font font = Minecraft.getInstance().font;
    protected List<Component> textLines = new ArrayList<>();
    protected int width = 0;
    protected int height = 0;
    protected int aspectWidth = -1;
    protected int aspectHeight = -1;
    protected Integer x = null;
    protected Integer y = null;
    protected int textBorderSize = 5;
    protected int mouseOffset = 12;
    protected SimpleTexture backgroundTexture = null;
    protected Color backgroundColor = null;
    protected Color borderColor = null;
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
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        Screen s = Minecraft.getInstance().screen;
        if (!this.isEmpty() && (s != null)) {

            this.updateAspectRatio();

            int x = this.calculateX(s, mouseX);
            int y = this.calculateY(s, mouseY);

            RenderSystem.enableBlend();
            RenderUtils.setZLevelPre(pose, 400);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            this.renderBackground(pose, x, y);
            this.renderTextLines(pose, x, y);

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderUtils.setZLevelPost(pose);
            RenderSystem.disableBlend();

        }
    }

    protected void renderTextLines(PoseStack pose, int x, int y) {
        int yLine = y;
        for (Component c : this.textLines) {
            int w = this.font.width(c);
            int x2 = x + this.textBorderSize;
            int y2 = yLine + this.textBorderSize;
            if (this.textAlignment == TooltipTextAlignment.RIGHT) {
                int diff = Math.max(0, (x + this.getWidth() - this.textBorderSize) - (x2 + w));
                x2 += diff;
            }
            if (this.textAlignment == TooltipTextAlignment.CENTERED) {
                x2 = x + Math.max(0, (this.getWidth() / 2) - (w / 2));
            }
            if (this.hasTextShadow()) {
                this.font.drawShadow(pose, c, x2, y2, (this.textBaseColor != null) ? this.textBaseColor.getRGB() : -1);
            } else {
                this.font.draw(pose, c, x2, y2, (this.textBaseColor != null) ? this.textBaseColor.getRGB() : -1);
            }
            yLine += this.font.lineHeight + 2;
        }
    }

    protected void renderBackground(PoseStack pose, int x, int y) {
        if (this.vanillaLike || ((this.backgroundTexture == null) && (this.backgroundColor == null))) {
            this.renderVanillaLikeBackground(pose, x, y, this.getWidth(), this.getHeight());
        } else if (this.backgroundTexture != null) {
            RenderUtils.bindTexture(this.backgroundTexture.getTextureLocation());
            blit(pose, x, y, 0.0F, 0.0F, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());
        } else {
            if (this.borderColor != null) {
                //BACKGROUND
                fill(pose, x + 1, y + 1, x + this.getWidth() - 2, y + this.getHeight() - 2, this.backgroundColor.getRGB());
                //TOP
                fill(pose, x + 1, y, x + this.getWidth() - 2, y + 1, this.borderColor.getRGB());
                //BOTTOM
                fill(pose, x + 1, y + this.getHeight() - 1, x + this.getWidth() - 2, y + this.getHeight(), this.borderColor.getRGB());
                //LEFT
                fill(pose, x, y, x + 1, y + this.getHeight(), this.borderColor.getRGB());
                //RIGHT
                fill(pose, x + this.getWidth() - 1, y, x + this.getWidth(), y + this.getHeight(), this.borderColor.getRGB());
            } else {
                fill(pose, x, y, x + this.getWidth(), y + this.getHeight(), this.backgroundColor.getRGB());
            }
        }
    }

    protected void renderVanillaLikeBackground(PoseStack pose, int x, int y, int width, int height) {

        pose.pushPose();

        ShaderInstance shaderInstance = RenderSystem.getShader();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder2 = tesselator.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bufferBuilder2.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix4f2 = pose.last().pose();

        TooltipRenderUtil.renderTooltipBackground(GuiComponent::fillGradient, matrix4f2, bufferBuilder2, x, y, width, height, 400);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        BufferUploader.drawWithShader(bufferBuilder2.end());
        if (shaderInstance != null) {
            RenderSystem.setShader(() -> shaderInstance);
        }

        pose.popPose();

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
        this.width = w + (this.textBorderSize * 2);
        this.height = h + (this.textBorderSize * 2);
    }

    public boolean isEmpty() {
        return this.textLines.isEmpty();
    }

    public Tooltip setTooltipText(String... lines) {
        List<Component> l = new ArrayList<>();
        if (lines != null) {
            for (String s : lines) {
                l.add(Component.literal(s));
            }
        }
        return this.setTooltipText(l);
    }

    public Tooltip setTooltipText(Component... lines) {
        return this.setTooltipText((lines != null) ? Arrays.asList(lines) : null);
    }

    public Tooltip setTooltipText(List<Component> lines) {
        this.textLines = (lines != null) ? lines : new ArrayList<>();
        this.updateSize();
        return this;
    }

    /** Returns a COPY of the tooltip list. **/
    public List<Component> getTooltip() {
        return new ArrayList<>(this.textLines);
    }

    public Tooltip setTextBorderSize(int size) {
        this.textBorderSize = size;
        this.updateSize();
        return this;
    }

    public int getTextBorderSize() {
        return this.textBorderSize;
    }

    public Tooltip setMouseOffset(int offset) {
        this.mouseOffset = offset;
        return this;
    }

    public int getMouseOffset() {
        return this.mouseOffset;
    }

    public Tooltip setBackgroundTexture(ResourceLocation texture) {
        this.backgroundTexture = SimpleTexture.create(texture);
        this.backgroundColor = null;
        this.vanillaLike = (texture == null);
        return this;
    }

    public SimpleTexture getBackgroundTexture() {
        return backgroundTexture;
    }

    public Tooltip setBackgroundColor(Color backgroundColor, @Nullable Color borderColor) {
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
        this.backgroundTexture = null;
        this.vanillaLike = (backgroundColor == null);
        return this;
    }

    public Color getBackgroundColor() {
        return this.backgroundColor;
    }

    public Color getBorderColor() {
        return this.borderColor;
    }

    public Tooltip setVanillaLike(boolean vanillaLike) {
        this.vanillaLike = vanillaLike;
        this.backgroundColor = null;
        this.backgroundTexture = null;
        return this;
    }

    public boolean isVanillaLike() {
        return vanillaLike;
    }

    public Tooltip setKeepBackgroundAspectRatio(boolean keepBackgroundAspectRatio) {
        this.keepBackgroundAspectRatio = keepBackgroundAspectRatio;
        return this;
    }

    public boolean keepBackgroundAspectRatio() {
        return keepBackgroundAspectRatio;
    }

    public Tooltip setTextShadow(boolean textShadow) {
        this.textShadow = textShadow;
        return this;
    }

    public boolean hasTextShadow() {
        return textShadow;
    }

    public Tooltip setTextAlignment(TooltipTextAlignment textAlignment) {
        this.textAlignment = textAlignment;
        return this;
    }

    public TooltipTextAlignment getTextAlignment() {
        return textAlignment;
    }

    public Tooltip setTextBaseColor(@Nullable Color textBaseColor) {
        this.textBaseColor = textBaseColor;
        return this;
    }

    @Nullable
    public Color getTextBaseColor() {
        return textBaseColor;
    }

    public Tooltip setFont(Font font) {
        this.font = (font != null) ? font : Minecraft.getInstance().font;
        this.updateSize();
        return this;
    }

    public Font getFont() {
        return font;
    }

    public Tooltip copyStyleOf(Tooltip tooltip) {
        this.borderColor = tooltip.borderColor;
        this.backgroundColor = tooltip.backgroundColor;
        this.backgroundTexture = tooltip.backgroundTexture;
        this.keepBackgroundAspectRatio = tooltip.keepBackgroundAspectRatio;
        this.vanillaLike = tooltip.vanillaLike;
        this.textBorderSize = tooltip.textBorderSize;
        this.textAlignment = tooltip.textAlignment;
        this.textShadow = tooltip.textShadow;
        this.textBaseColor = tooltip.textBaseColor;
        this.font = tooltip.font;
        return this;
    }

    public Tooltip setCustomX(@Nullable Integer x) {
        this.x = x;
        return this;
    }

    @Nullable
    public Integer getCustomX() {
        return x;
    }

    public Tooltip setCustomY(@Nullable Integer y) {
        this.y = y;
        return this;
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
