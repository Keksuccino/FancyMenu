package de.keksuccino.fancymenu.util.rendering.ui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPScreen;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UIColorTheme;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import java.awt.Color;
import java.util.Objects;

public class ColorPickerScreen extends PiPScreen {

    private static final int MIN_PICKER_SIZE = 80;
    private static final int MAX_PICKER_SIZE = 260;
    private static final int SLIDER_HEIGHT = 12;
    private static final int SLIDER_GAP = 8;
    private static final int COLUMN_GAP = 24;
    private static final int PANEL_PADDING = 10;
    private static final int PREVIEW_SIZE = 70;

    @NotNull
    private final Property.ColorProperty colorProperty;
    @Nullable
    private final DrawableColor presetColor;
    @Nullable
    private final String originalColorValue;

    private float hue = 0.0F;
    private float saturation = 0.0F;
    private float value = 1.0F;
    private float alpha = 1.0F;

    private ExtendedButton doneButton;
    private ExtendedButton cancelButton;

    private boolean draggingSV = false;
    private boolean draggingHue = false;
    private boolean draggingAlpha = false;

    private int pickerX;
    private int pickerY;
    private int pickerSize;
    private int hueLabelY;
    private int hueX;
    private int hueY;
    private int hueWidth;
    private int hueHeight;
    private int alphaLabelY;
    private int alphaX;
    private int alphaY;
    private int alphaWidth;
    private int alphaHeight;
    private int infoX;
    private int infoY;
    private int infoWidth;
    private int infoHeight;

    public ColorPickerScreen(@NotNull Property.ColorProperty colorProperty) {
        super(Component.translatable("fancymenu.ui.color_picker.title"));
        this.colorProperty = Objects.requireNonNull(colorProperty, "colorProperty");
        this.originalColorValue = colorProperty.get();
        DrawableColor preset = colorProperty.getDrawable();
        if (preset == DrawableColor.EMPTY) {
            preset = null;
        }
        this.presetColor = preset;
        this.applyPresetColor(preset);
    }

    @Override
    protected void init() {
        this.cancelButton = new ExtendedButton(0, 0, 100, 20, Component.translatable("fancymenu.common_components.cancel"), (button) -> {
            this.colorProperty.set(this.originalColorValue);
            this.closeWindow();
        });
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton);

        this.doneButton = new ExtendedButton(0, 0, 100, 20, Component.translatable("fancymenu.common_components.done"), (button) -> {
            this.closeWindow();
        });
        this.addWidget(this.doneButton);
        UIBase.applyDefaultWidgetSkinTo(this.doneButton);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        UIColorTheme theme = UIBase.getUIColorTheme();
        RenderSystem.enableBlend();
        graphics.fill(0, 0, this.width, this.height, theme.screen_background_color.getColorInt());

        this.updateLayout();

        MutableComponent title = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        int titleWidth = this.font.width(title);
        graphics.drawString(this.font, title, (this.width / 2) - (titleWidth / 2), 12, theme.generic_text_base_color.getColorInt(), false);

        this.renderLeftPanel(graphics, theme);
        this.renderInfoPanel(graphics, theme);

        this.cancelButton.setX((this.width / 2) - this.cancelButton.getWidth() - 5);
        this.cancelButton.setY(this.height - 36);
        this.cancelButton.render(graphics, mouseX, mouseY, partial);

        this.doneButton.setX((this.width / 2) + 5);
        this.doneButton.setY(this.height - 36);
        this.doneButton.render(graphics, mouseX, mouseY, partial);

    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
    }

    @Override
    public boolean keyPressed(int button, int scanCode, int modifiers) {
        if (button == InputConstants.KEY_ENTER) {
            this.closeWindow();
            return true;
        }
        return super.keyPressed(button, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (RenderingUtils.isXYInArea(mouseX, mouseY, this.pickerX, this.pickerY, this.pickerSize, this.pickerSize)) {
                this.draggingSV = true;
                this.updateSVFromMouse(mouseX, mouseY);
                return true;
            }
            if (RenderingUtils.isXYInArea(mouseX, mouseY, this.hueX, this.hueY, this.hueWidth, this.hueHeight)) {
                this.draggingHue = true;
                this.updateHueFromMouse(mouseX);
                return true;
            }
            if (RenderingUtils.isXYInArea(mouseX, mouseY, this.alphaX, this.alphaY, this.alphaWidth, this.alphaHeight)) {
                this.draggingAlpha = true;
                this.updateAlphaFromMouse(mouseX);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.draggingSV) {
            this.updateSVFromMouse(mouseX, mouseY);
            return true;
        }
        if (this.draggingHue) {
            this.updateHueFromMouse(mouseX);
            return true;
        }
        if (this.draggingAlpha) {
            this.updateAlphaFromMouse(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.draggingSV = false;
        this.draggingHue = false;
        this.draggingAlpha = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateLayout() {
        int padding = 24;
        int top = 40;
        int bottom = 56;
        int availableWidth = this.width - (padding * 2);
        int availableHeight = this.height - top - bottom;
        int labelHeight = this.font.lineHeight;
        int extraHeight = (labelHeight + SLIDER_HEIGHT + 2) * 2 + (SLIDER_GAP * 3);

        this.pickerSize = Math.min(MAX_PICKER_SIZE, Math.max(MIN_PICKER_SIZE, availableHeight - extraHeight));

        int infoCandidateWidth = availableWidth - this.pickerSize - COLUMN_GAP;
        boolean stackedLayout = infoCandidateWidth < 160;

        if (stackedLayout) {
            int infoHeightEstimate = 150 + (labelHeight * 2);
            int maxPickerSize = availableHeight - extraHeight - infoHeightEstimate - 12;
            if (maxPickerSize > MIN_PICKER_SIZE) {
                this.pickerSize = Math.min(this.pickerSize, maxPickerSize);
            }
        }

        int totalWidth = stackedLayout ? this.pickerSize : (this.pickerSize + COLUMN_GAP + infoCandidateWidth);
        int startX = (this.width - totalWidth) / 2;

        this.pickerX = startX;
        this.pickerY = top;

        this.hueLabelY = this.pickerY + this.pickerSize + SLIDER_GAP;
        this.hueX = this.pickerX;
        this.hueY = this.hueLabelY + labelHeight + 2;
        this.hueWidth = this.pickerSize;
        this.hueHeight = SLIDER_HEIGHT;

        this.alphaLabelY = this.hueY + this.hueHeight + SLIDER_GAP;
        this.alphaX = this.pickerX;
        this.alphaY = this.alphaLabelY + labelHeight + 2;
        this.alphaWidth = this.pickerSize;
        this.alphaHeight = SLIDER_HEIGHT;

        int leftPanelHeight = (this.alphaY + this.alphaHeight) - this.pickerY;

        if (stackedLayout) {
            this.infoX = startX;
            this.infoY = this.alphaY + this.alphaHeight + SLIDER_GAP + labelHeight;
            this.infoWidth = this.pickerSize;
            this.infoHeight = Math.max(140, (labelHeight * 6) + PREVIEW_SIZE + 36);
        } else {
            this.infoX = this.pickerX + this.pickerSize + COLUMN_GAP;
            this.infoY = this.pickerY;
            this.infoWidth = infoCandidateWidth;
            this.infoHeight = leftPanelHeight;
        }
    }

    private void renderLeftPanel(@NotNull GuiGraphics graphics, @NotNull UIColorTheme theme) {
        int panelX = this.pickerX - PANEL_PADDING;
        int panelY = this.pickerY - PANEL_PADDING;
        int panelW = this.pickerSize + (PANEL_PADDING * 2);
        int panelH = (this.alphaY + this.alphaHeight - this.pickerY) + (PANEL_PADDING * 2);

        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, theme.area_background_color.getColorInt());
        UIBase.renderBorder(graphics, panelX, panelY, panelX + panelW, panelY + panelH, 1, theme.element_border_color_normal.getColorInt(), true, true, true, true);

        this.renderColorArea(graphics);
        this.renderColorAreaSelector(graphics, theme);

        UIBase.drawElementLabel(graphics, this.font, Component.translatable("fancymenu.ui.color_picker.hue"), this.hueX, this.hueLabelY);
        this.renderHueSlider(graphics, theme);

        UIBase.drawElementLabel(graphics, this.font, Component.translatable("fancymenu.ui.color_picker.alpha"), this.alphaX, this.alphaLabelY);
        this.renderAlphaSlider(graphics, theme);
    }

    private void renderInfoPanel(@NotNull GuiGraphics graphics, @NotNull UIColorTheme theme) {
        int panelX = this.infoX - PANEL_PADDING;
        int panelY = this.infoY - PANEL_PADDING;
        int panelW = this.infoWidth + (PANEL_PADDING * 2);
        int panelH = Math.max(this.infoHeight, 140) + (PANEL_PADDING * 2);

        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, theme.area_background_color.getColorInt());
        UIBase.renderBorder(graphics, panelX, panelY, panelX + panelW, panelY + panelH, 1, theme.element_border_color_normal.getColorInt(), true, true, true, true);

        int x = this.infoX;
        int y = this.infoY;
        int labelHeight = this.font.lineHeight;

        int previewLabelY = y;
        int previewRectY = previewLabelY + labelHeight + 4;
        UIBase.drawElementLabel(graphics, this.font, Component.translatable("fancymenu.ui.color_picker.preview"), x, previewLabelY);

        this.renderCheckerboard(graphics, x, previewRectY, PREVIEW_SIZE, PREVIEW_SIZE, 6);
        graphics.fill(x, previewRectY, x + PREVIEW_SIZE, previewRectY + PREVIEW_SIZE, this.getCurrentColorInt());
        UIBase.renderBorder(graphics, x, previewRectY, x + PREVIEW_SIZE, previewRectY + PREVIEW_SIZE, 1, theme.element_border_color_normal.getColorInt(), true, true, true, true);

        if (this.presetColor != null) {
            int presetX = x + PREVIEW_SIZE + 12;
            UIBase.drawElementLabel(graphics, this.font, Component.translatable("fancymenu.ui.color_picker.original"), presetX, previewLabelY);
            this.renderCheckerboard(graphics, presetX, previewRectY, PREVIEW_SIZE, PREVIEW_SIZE, 6);
            graphics.fill(presetX, previewRectY, presetX + PREVIEW_SIZE, previewRectY + PREVIEW_SIZE, this.presetColor.getColorInt());
            UIBase.renderBorder(graphics, presetX, previewRectY, presetX + PREVIEW_SIZE, previewRectY + PREVIEW_SIZE, 1, theme.element_border_color_normal.getColorInt(), true, true, true, true);
        }

        y = previewRectY + PREVIEW_SIZE + 12;

        UIBase.drawElementLabel(graphics, this.font, Component.translatable("fancymenu.ui.color_picker.hex"), x, y);
        y += labelHeight + 2;
        graphics.drawString(this.font, this.getCurrentHex(), x, y, theme.generic_text_base_color.getColorInt(), false);

        y += labelHeight + 8;
        UIBase.drawElementLabel(graphics, this.font, Component.translatable("fancymenu.ui.color_picker.rgba"), x, y);
        y += labelHeight + 2;
        int rgb = this.getCurrentColorInt();
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int a = Math.round(this.alpha * 255.0F);
        graphics.drawString(this.font, Component.literal("R: " + r + "  G: " + g + "  B: " + b + "  A: " + a), x, y, theme.generic_text_base_color.getColorInt(), false);

        y += labelHeight + 8;
        UIBase.drawElementLabel(graphics, this.font, Component.translatable("fancymenu.ui.color_picker.hsv"), x, y);
        y += labelHeight + 2;
        int h = Math.round(this.hue * 360.0F);
        int s = Math.round(this.saturation * 100.0F);
        int v = Math.round(this.value * 100.0F);
        graphics.drawString(this.font, Component.literal("H: " + h + "  S: " + s + "%  V: " + v + "%"), x, y, theme.generic_text_base_color.getColorInt(), false);
    }

    private void renderColorArea(@NotNull GuiGraphics graphics) {
        int hueRgb = Color.HSBtoRGB(this.hue, 1.0F, 1.0F);
        int hueColor = 0xFF000000 | (hueRgb & 0xFFFFFF);
        this.fillGradientHorizontal(graphics, this.pickerX, this.pickerY, this.pickerX + this.pickerSize, this.pickerY + this.pickerSize, 0xFFFFFFFF, hueColor);
        graphics.fillGradient(this.pickerX, this.pickerY, this.pickerX + this.pickerSize, this.pickerY + this.pickerSize, 0x00000000, 0xFF000000);
        UIBase.renderBorder(graphics, this.pickerX, this.pickerY, this.pickerX + this.pickerSize, this.pickerY + this.pickerSize, 1, UIBase.getUIColorTheme().element_border_color_normal.getColorInt(), true, true, true, true);
    }

    private void renderColorAreaSelector(@NotNull GuiGraphics graphics, @NotNull UIColorTheme theme) {
        int max = Math.max(1, this.pickerSize - 1);
        int selectorX = this.pickerX + Math.round(this.saturation * max);
        int selectorY = this.pickerY + Math.round((1.0F - this.value) * max);
        int outerSize = 7;
        int innerSize = 5;
        int outerX = selectorX - (outerSize / 2);
        int outerY = selectorY - (outerSize / 2);
        graphics.fill(outerX, outerY, outerX + outerSize, outerY + outerSize, theme.element_border_color_normal.getColorInt());
        int innerX = selectorX - (innerSize / 2);
        int innerY = selectorY - (innerSize / 2);
        graphics.fill(innerX, innerY, innerX + innerSize, innerY + innerSize, theme.generic_text_base_color.getColorInt());
    }

    private void renderHueSlider(@NotNull GuiGraphics graphics, @NotNull UIColorTheme theme) {
        int segments = 6;
        for (int i = 0; i < segments; i++) {
            float startHue = (float) i / (float) segments;
            float endHue = (float) (i + 1) / (float) segments;
            int startX = this.hueX + (i * this.hueWidth) / segments;
            int endX = (i == segments - 1) ? (this.hueX + this.hueWidth) : (this.hueX + ((i + 1) * this.hueWidth) / segments);
            if (endX <= startX) {
                continue;
            }
            int startRgb = Color.HSBtoRGB(startHue, 1.0F, 1.0F);
            int endRgb = Color.HSBtoRGB(endHue, 1.0F, 1.0F);
            int startColor = 0xFF000000 | (startRgb & 0xFFFFFF);
            int endColor = 0xFF000000 | (endRgb & 0xFFFFFF);
            this.fillGradientHorizontal(graphics, startX, this.hueY, endX, this.hueY + this.hueHeight, startColor, endColor);
        }
        UIBase.renderBorder(graphics, this.hueX, this.hueY, this.hueX + this.hueWidth, this.hueY + this.hueHeight, 1, theme.element_border_color_normal.getColorInt(), true, true, true, true);

        int max = Math.max(1, this.hueWidth - 1);
        int markerX = this.hueX + Math.round(this.hue * max);
        graphics.fill(markerX - 1, this.hueY - 2, markerX + 1, this.hueY + this.hueHeight + 2, theme.generic_text_base_color.getColorInt());
    }

    private void renderAlphaSlider(@NotNull GuiGraphics graphics, @NotNull UIColorTheme theme) {
        this.renderCheckerboard(graphics, this.alphaX, this.alphaY, this.alphaWidth, this.alphaHeight, 4);
        int rgb = this.getCurrentColorInt() & 0xFFFFFF;
        int leftColor = rgb;
        int rightColor = 0xFF000000 | rgb;
        this.fillGradientHorizontal(graphics, this.alphaX, this.alphaY, this.alphaX + this.alphaWidth, this.alphaY + this.alphaHeight, leftColor, rightColor);
        UIBase.renderBorder(graphics, this.alphaX, this.alphaY, this.alphaX + this.alphaWidth, this.alphaY + this.alphaHeight, 1, theme.element_border_color_normal.getColorInt(), true, true, true, true);

        int max = Math.max(1, this.alphaWidth - 1);
        int markerX = this.alphaX + Math.round(this.alpha * max);
        graphics.fill(markerX - 1, this.alphaY - 2, markerX + 1, this.alphaY + this.alphaHeight + 2, theme.generic_text_base_color.getColorInt());
    }

    private void renderCheckerboard(@NotNull GuiGraphics graphics, int x, int y, int width, int height, int cellSize) {
        int light = 0xFFB8B8B8;
        int dark = 0xFF7E7E7E;
        for (int yy = 0; yy < height; yy += cellSize) {
            for (int xx = 0; xx < width; xx += cellSize) {
                int color = (((xx / cellSize) + (yy / cellSize)) % 2 == 0) ? light : dark;
                graphics.fill(x + xx, y + yy, x + Math.min(xx + cellSize, width), y + Math.min(yy + cellSize, height), color);
            }
        }
    }

    private void updateSVFromMouse(double mouseX, double mouseY) {
        int max = Math.max(1, this.pickerSize - 1);
        this.saturation = clamp((float) ((mouseX - this.pickerX) / (double) max));
        this.value = clamp(1.0F - (float) ((mouseY - this.pickerY) / (double) max));
        this.applyCurrentColorToProperty();
    }

    private void updateHueFromMouse(double mouseX) {
        int max = Math.max(1, this.hueWidth - 1);
        this.hue = clamp((float) ((mouseX - this.hueX) / (double) max));
        this.applyCurrentColorToProperty();
    }

    private void updateAlphaFromMouse(double mouseX) {
        int max = Math.max(1, this.alphaWidth - 1);
        this.alpha = clamp((float) ((mouseX - this.alphaX) / (double) max));
        this.applyCurrentColorToProperty();
    }

    private void applyPresetColor(@Nullable DrawableColor preset) {
        DrawableColor color = (preset != null) ? preset : DrawableColor.of(new Color(255, 255, 255, 255));
        Color awt = color.getColor();
        float[] hsv = Color.RGBtoHSB(awt.getRed(), awt.getGreen(), awt.getBlue(), null);
        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.value = hsv[2];
        this.alpha = Math.min(1.0F, Math.max(0.0F, (float) awt.getAlpha() / 255.0F));
    }

    private int getCurrentColorInt() {
        int rgb = Color.HSBtoRGB(this.hue, this.saturation, this.value);
        int alphaInt = Math.round(this.alpha * 255.0F);
        return (alphaInt << 24) | (rgb & 0xFFFFFF);
    }

    private String getCurrentHex() {
        int rgb = Color.HSBtoRGB(this.hue, this.saturation, this.value);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int a = Math.round(this.alpha * 255.0F);
        return DrawableColor.of(r, g, b, a).getHex();
    }

    private void applyCurrentColorToProperty() {
        this.colorProperty.set(this.getCurrentHex());
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private void fillGradientHorizontal(@NotNull GuiGraphics graphics, int x1, int y1, int x2, int y2, int colorLeft, int colorRight) {
        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer consumer = graphics.bufferSource().getBuffer(RenderType.gui());
        consumer.addVertex(matrix, (float) x1, (float) y1, 0.0F).setColor(colorLeft);
        consumer.addVertex(matrix, (float) x1, (float) y2, 0.0F).setColor(colorLeft);
        consumer.addVertex(matrix, (float) x2, (float) y2, 0.0F).setColor(colorRight);
        consumer.addVertex(matrix, (float) x2, (float) y1, 0.0F).setColor(colorRight);
    }

}
