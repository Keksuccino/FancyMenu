package de.keksuccino.fancymenu.util.rendering.ui.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.GuiBlurRenderer;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.text.TextFormattingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A tooltip that gets rendered at the mouse position by default.<br>
 * It is possible to set a custom X and Y position to not render it at the mouse position.
 **/
@SuppressWarnings("unused")
public class Tooltip implements Renderable {

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
    protected ITexture backgroundTexture = null;
    protected DrawableColor backgroundColor = null;
    protected DrawableColor borderColor = null;
    protected boolean keepBackgroundAspectRatio = true;
    protected boolean textShadow = true;
    protected TooltipTextAlignment textAlignment = TooltipTextAlignment.LEFT;
    protected DrawableColor textBaseColor = null;
    protected Float scale = null;

    @NotNull
    public static Tooltip empty() {
        return new Tooltip();
    }

    @NotNull
    public static Tooltip of(String... tooltip) {
        Tooltip t = new Tooltip();
        if (tooltip != null) {
            t.setTooltipText(tooltip);
        }
        return t;
    }

    @NotNull
    public static Tooltip of(Component... tooltip) {
        Tooltip t = new Tooltip();
        if (tooltip != null) {
            t.setTooltipText(tooltip);
        }
        return t;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (RenderingUtils.isTooltipRenderingBlocked()) return;

        Screen s = Minecraft.getInstance().screen;
        if (!this.isEmpty() && (s != null)) {

            this.updateAspectRatio();

            int x = this.calculateX(s, mouseX);
            int y = this.calculateY(s, mouseY);

            RenderSystem.disableDepthTest();
            RenderingUtils.setDepthTestLocked(true);

            RenderSystem.enableBlend();

            graphics.pose().pushPose();

            float scale;
            if (this.scale != null) {
                scale = UIBase.calculateFixedScale(this.scale);
                graphics.pose().scale(scale, scale, scale);
            } else {
                scale = 1.0F;
            }
            RenderingUtils.resetShaderColor(graphics);

            this.renderBackground(graphics, x, y, partial, scale);
            this.renderTextLines(graphics, x, y);

            graphics.pose().popPose();

            RenderingUtils.setDepthTestLocked(false);

            RenderingUtils.resetShaderColor(graphics);

        }
    }

    protected void renderTextLines(GuiGraphics graphics, int x, int y) {
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
            graphics.drawString(this.font, c, x2, y2, (this.textBaseColor != null) ? this.textBaseColor.getColorInt() : -1, this.hasTextShadow());
            yLine += this.font.lineHeight + 2;
        }
    }

    protected void renderBackground(GuiGraphics graphics, int x, int y, float partial, float renderScale) {
        if (this.backgroundTexture != null) {
            ResourceLocation loc = this.backgroundTexture.getResourceLocation();
            if (loc != null) {
                graphics.blit(loc, x, y, 0.0F, 0.0F, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());
            }
        } else {
            float radius = UIBase.getCornerRoundingRadius();
            boolean blurEnabled = UIBase.shouldBlur();
            int renderWidth = this.getWidth();
            int renderHeight = this.getHeight();

            if (blurEnabled) {
                float blurX = x * renderScale;
                float blurY = y * renderScale;
                float blurWidth = renderWidth * renderScale;
                float blurHeight = renderHeight * renderScale;

                GuiBlurRenderer.renderBlurAreaWithIntensityRoundAllCorners(
                        graphics,
                        blurX,
                        blurY,
                        blurWidth,
                        blurHeight,
                        UIBase.getBlurRadius(),
                        Math.max(0.0F, radius - 0.9F),
                        Math.max(0.0F, radius - 0.9F),
                        Math.max(0.0F, radius - 0.9F),
                        Math.max(0.0F, radius - 0.9F),
                        UIBase.getUIColorTheme().ui_blur_tooltip_background_tint,
                        partial);
            } else {
                UIBase.renderRoundedRect(graphics, x, y, renderWidth, renderHeight, radius, radius, radius, radius, this.backgroundColor.getColorInt());
            }

            if (this.borderColor != null) {
                int borderColorInt = blurEnabled ? UIBase.getUIColorTheme().ui_blur_overlay_element_border_color.getColorInt() : this.borderColor.getColorInt();
                UIBase.renderRoundedBorder(
                        graphics,
                        x,
                        y,
                        x + renderWidth,
                        y + renderHeight,
                        1.0F,
                        radius,
                        radius,
                        radius,
                        radius,
                        borderColorInt);
            }
        }
    }

    protected int calculateX(Screen screen, int mouseX) {
        float scale = 1;
        if (this.scale != null) {
            scale = UIBase.calculateFixedScale(this.scale);
        }
        if (this.x != null) {
            mouseX = this.x;
        }
        int width = this.getWidth();
        int scaledWidth = (int)((float)width * scale);
        int scaledMouseX = (int)((float)mouseX / scale) + this.mouseOffset;
        int scaledScreenWidth = (int)((float)screen.width / scale);
        int x = (mouseX + this.mouseOffset);
        if ((x + scaledWidth) > screen.width) {
            int offset = (x + scaledWidth) - screen.width;
            x = x - offset;
        }
        return (int)((float)x / scale);
    }

    protected int calculateY(Screen screen, int mouseY) {
        float scale = 1;
        if (this.scale != null) {
            scale = UIBase.calculateFixedScale(this.scale);
        }
        if (this.y != null) {
            mouseY = this.y;
        }
        int height = this.getHeight();
        int scaledHeight = (int)((float)height * scale);
        int scaledMouseY = (int)((float)mouseY / scale) + this.mouseOffset;
        int scaledScreenHeight = (int)((float)screen.height / scale);
        int y = (mouseY + this.mouseOffset);
        if ((y + scaledHeight) > screen.height) {
            int offset = (y + scaledHeight) - screen.height;
            y = y - offset;
        }
        return (int)((float)y / scale);
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
        int[] size = this.backgroundTexture.getAspectRatio().getAspectRatioSizeByMinimumSize(this.width, this.height);
        this.aspectWidth = size[0];
        this.aspectHeight = size[1];
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
        if (h > 0) {
            h -= 2;
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
        MutableComponent merged = Component.empty();
        if (lines != null) {
            boolean b = true;
            for (Component c : lines) {
                if (!b) merged.append("\n");
                merged.append(c);
                b = false;
            }
        }
        this.textLines = new ArrayList<>();
        for (FormattedText s : Minecraft.getInstance().font.getSplitter().splitLines(merged, 300, Style.EMPTY)) {
            this.textLines.add(TextFormattingUtils.convertFormattedTextToComponent(s));
        }
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

    public Tooltip setBackgroundTexture(@NotNull ITexture texture) {
        this.backgroundTexture = texture;
        this.backgroundColor = null;
        return this;
    }

    @Nullable
    public ITexture getBackgroundTexture() {
        return this.backgroundTexture;
    }

    public Tooltip setBackgroundColor(@NotNull DrawableColor backgroundColor, @Nullable DrawableColor borderColor) {
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
        this.backgroundTexture = null;
        return this;
    }

    public Tooltip setDefaultStyle() {
        this.setTextShadow(FancyMenu.getOptions().enableUiTextShadow.getValue());
        this.setTextBaseColor(UIBase.getUIColorTheme().element_label_color_normal);
        return this.setBackgroundColor(UIBase.getUIColorTheme().element_background_color_normal, UIBase.getUIColorTheme().element_border_color_normal);
    }

    @Nullable
    public DrawableColor getBackgroundColor() {
        return this.backgroundColor;
    }

    @Nullable
    public DrawableColor getBorderColor() {
        return this.borderColor;
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

    public Tooltip setTextBaseColor(@Nullable DrawableColor textBaseColor) {
        this.textBaseColor = textBaseColor;
        return this;
    }

    @Nullable
    public DrawableColor getTextBaseColor() {
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

    public Tooltip applyStyleOf(@NotNull Tooltip tooltip) {
        this.borderColor = tooltip.borderColor;
        this.backgroundColor = tooltip.backgroundColor;
        this.backgroundTexture = tooltip.backgroundTexture;
        this.keepBackgroundAspectRatio = tooltip.keepBackgroundAspectRatio;
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

    @Nullable
    public Float getScale() {
        return this.scale;
    }

    public Tooltip setScale(@Nullable Float scale) {
        this.scale = scale;
        return this;
    }

    public enum TooltipTextAlignment {
        LEFT,
        RIGHT,
        CENTERED
    }

}
