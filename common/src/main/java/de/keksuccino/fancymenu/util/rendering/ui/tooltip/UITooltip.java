package de.keksuccino.fancymenu.util.rendering.ui.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.rendering.GuiBlurRenderer;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.text.TextFormattingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
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
public class UITooltip implements Renderable {

    protected Font font = Minecraft.getInstance().font;
    protected List<Component> textLines = new ArrayList<>();
    protected int width = 0;
    protected int height = 0;
    @Nullable
    protected Integer x = null;
    @Nullable
    protected Integer y = null;
    protected int textBorderSize = 5;
    protected int mouseOffset = 12;
    protected boolean textShadow = FancyMenu.getOptions().enableUiTextShadow.getValue();
    protected TooltipTextAlignment textAlignment = TooltipTextAlignment.LEFT;

    @NotNull
    public static UITooltip empty() {
        return new UITooltip();
    }

    @NotNull
    public static UITooltip of(String... tooltip) {
        UITooltip t = new UITooltip();
        if (tooltip != null) {
            t.setTooltipText(tooltip);
        }
        return t;
    }

    @NotNull
    public static UITooltip of(Component... tooltip) {
        UITooltip t = new UITooltip();
        if (tooltip != null) {
            t.setTooltipText(tooltip);
        }
        return t;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (RenderingUtils.isTooltipRenderingBlocked()) return;

        Screen screen = Minecraft.getInstance().screen;

        if (!this.isEmpty() && (screen != null)) {

            float scale = UIBase.getFixedUIScale();
            int x = this.calculateX(screen, mouseX);
            int y = this.calculateY(screen, mouseY);

            RenderSystem.disableDepthTest();
            RenderingUtils.setDepthTestLocked(true);

            RenderSystem.enableBlend();

            graphics.pose().pushPose();
            graphics.pose().scale(scale, scale, scale);

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
            graphics.drawString(this.font, c, x2, y2, UIBase.getUIColorTheme().element_label_color_normal.getColorInt(), this.hasTextShadow());
            yLine += this.font.lineHeight + 2;
        }
    }

    protected void renderBackground(GuiGraphics graphics, int x, int y, float partial, float renderScale) {

        float normalRoundingRadius = UIBase.getInterfaceCornerRoundingRadius();
        boolean blurEnabled = UIBase.shouldBlur();
        int renderWidth = this.getWidth();
        int renderHeight = this.getHeight();
        float borderThickness = 1.0F;

        float backgroundX = x + borderThickness;
        float backgroundY = y + borderThickness;
        float backgroundWidth = renderWidth - (borderThickness * 2.0F);
        float backgroundHeight = renderHeight - (borderThickness * 2.0F);

        if (blurEnabled) {
            float blurX = backgroundX * renderScale;
            float blurY = backgroundY * renderScale;
            float blurWidth = backgroundWidth * renderScale;
            float blurHeight = backgroundHeight * renderScale;
            float blurRoundingRadius = normalRoundingRadius * renderScale;

            GuiBlurRenderer.renderBlurAreaWithIntensityRoundAllCorners(
                    graphics,
                    blurX,
                    blurY,
                    blurWidth,
                    blurHeight,
                    UIBase.getBlurRadius(),
                    blurRoundingRadius,
                    blurRoundingRadius,
                    blurRoundingRadius,
                    blurRoundingRadius,
                    UIBase.getUIColorTheme().ui_blur_tooltip_background_tint,
                    partial);
        } else {
            UIBase.renderRoundedRect(graphics, backgroundX, backgroundY, backgroundWidth, backgroundHeight, normalRoundingRadius, normalRoundingRadius, normalRoundingRadius, normalRoundingRadius, UIBase.getUIColorTheme().area_background_color.getColorInt());
        }

        int borderColorInt = blurEnabled ? UIBase.getUIColorTheme().ui_blur_overlay_element_border_color.getColorInt() : UIBase.getUIColorTheme().element_border_color_normal.getColorInt();
        UIBase.renderRoundedBorder(
                graphics,
                x,
                y,
                x + renderWidth,
                y + renderHeight,
                borderThickness,
                normalRoundingRadius,
                normalRoundingRadius,
                normalRoundingRadius,
                normalRoundingRadius,
                borderColorInt);

        RenderingUtils.resetShaderColor(graphics);

    }

    protected int calculateX(Screen screen, int mouseX) {
        float scale = UIBase.getFixedUIScale();
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
        float scale = UIBase.getFixedUIScale();
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
        return this.width;
    }

    protected int getHeight() {
        return this.height;
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

    public UITooltip setTooltipText(String... lines) {
        List<Component> l = new ArrayList<>();
        if (lines != null) {
            for (String s : lines) {
                l.add(Component.literal(s));
            }
        }
        return this.setTooltipText(l);
    }

    public UITooltip setTooltipText(Component... lines) {
        return this.setTooltipText((lines != null) ? Arrays.asList(lines) : null);
    }

    public UITooltip setTooltipText(List<Component> lines) {
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

    /** Returns a COPY of the Component list. **/
    public List<Component> getTooltipContent() {
        return new ArrayList<>(this.textLines);
    }

    public UITooltip setTextBorderSize(int size) {
        this.textBorderSize = size;
        this.updateSize();
        return this;
    }

    public int getTextBorderSize() {
        return this.textBorderSize;
    }

    public UITooltip setMouseOffset(int offset) {
        this.mouseOffset = offset;
        return this;
    }

    public int getMouseOffset() {
        return this.mouseOffset;
    }

    public UITooltip setTextShadow(boolean textShadow) {
        this.textShadow = textShadow;
        return this;
    }

    public boolean hasTextShadow() {
        return textShadow;
    }

    public UITooltip setTextAlignment(TooltipTextAlignment textAlignment) {
        this.textAlignment = textAlignment;
        return this;
    }

    public TooltipTextAlignment getTextAlignment() {
        return textAlignment;
    }

    public UITooltip setFont(Font font) {
        this.font = (font != null) ? font : Minecraft.getInstance().font;
        this.updateSize();
        return this;
    }

    public Font getFont() {
        return font;
    }

    public UITooltip setCustomX(@Nullable Integer x) {
        this.x = x;
        return this;
    }

    @Nullable
    public Integer getCustomX() {
        return x;
    }

    public UITooltip setCustomY(@Nullable Integer y) {
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
