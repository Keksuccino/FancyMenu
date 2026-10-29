package de.keksuccino.fancymenu.util.rendering.ui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UITheme;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import java.awt.Color;
import java.util.function.Consumer;

public class ColorPickerWindowBody extends PiPWindowBody {

    public static final int PIP_WINDOW_WIDTH = 401;
    public static final int PIP_WINDOW_HEIGHT = 328;

    private static final int MIN_PICKER_SIZE = 80;
    private static final int MAX_PICKER_SIZE = 260;
    private static final int SLIDER_HEIGHT = 12;
    private static final int SLIDER_GAP = 8;
    private static final int COLUMN_GAP = 24;
    private static final int PANEL_PADDING = 10;
    private static final int PREVIEW_SIZE = 70;
    private static final int CHANNEL_INPUT_HEIGHT = 16;
    private static final int CHANNEL_INPUT_MAX_WIDTH = 60;
    private static final int CHANNEL_INPUT_MAX_LENGTH = 3;
    private static final int CHANNEL_INPUT_COLUMNS = 2;
    private static final int CHANNEL_INPUT_COLUMN_GAP = 12;
    private static final int CHANNEL_INPUT_ROW_GAP = 6;
    private static final int CHANNEL_LABEL_GAP = 4;
    private static final ColorPickerState.Channel[] CHANNELS = ColorPickerState.Channel.values();

    @Nullable
    private final DrawableColor presetColor;
    @NotNull
    private final Consumer<DrawableColor> onColorUpdate;
    @NotNull
    private final Consumer<DrawableColor> onDone;
    @NotNull
    private final Consumer<DrawableColor> onCancel;

    @NotNull
    private final ColorPickerState colorState;

    private ExtendedButton doneButton;
    private ExtendedButton cancelButton;
    private final ExtendedEditBox[] channelInputs = new ExtendedEditBox[CHANNELS.length];
    private final String[] channelInputTexts = new String[CHANNELS.length];
    private int channelInputTextsArgb;
    private boolean syncingChannelInputs = false;

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

    public ColorPickerWindowBody(@Nullable DrawableColor presetColor, @NotNull Consumer<DrawableColor> onColorUpdate, @NotNull Consumer<DrawableColor> onDone, @NotNull Consumer<DrawableColor> onCancel) {
        super(Component.translatable("fancymenu.ui.color_picker.title"));
        this.onColorUpdate = onColorUpdate;
        this.onDone = onDone;
        this.onCancel = onCancel;
        this.presetColor = presetColor;
        this.colorState = new ColorPickerState((presetColor != null) ? presetColor.getColorInt() : 0xFFFFFFFF);
    }

    @Override
    protected void init() {
        // Channel inputs get added first, so Tab navigation starts with them
        for (int i = 0; i < CHANNELS.length; i++) {
            ColorPickerState.Channel channel = CHANNELS[i];
            ExtendedEditBox input = new ExtendedEditBox(Minecraft.getInstance().font, 0, 0, CHANNEL_INPUT_MAX_WIDTH, CHANNEL_INPUT_HEIGHT, Component.literal(channel.getLabel()));
            input.setMaxLength(CHANNEL_INPUT_MAX_LENGTH);
            input.setCharacterFilter(CharacterFilter.buildDigitFilter());
            input.setResponder(text -> this.onChannelInputChanged(channel, text));
            UIBase.applyDefaultWidgetSkinTo(input, UIBase.shouldBlur());
            this.channelInputs[i] = this.addWidget(input);
        }
        this.syncChannelInputs();

        this.cancelButton = new ExtendedButton(0, 0, 100, 20, Component.translatable("fancymenu.common_components.cancel"), (button) -> {
            this.onCancel.accept(this.presetColor);
            this.closeWindow();
        });
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton, UIBase.shouldBlur());

        this.doneButton = new ExtendedButton(0, 0, 100, 20, Component.translatable("fancymenu.common_components.done"), (button) -> this.confirmAndCloseWindow());
        this.addWidget(this.doneButton);
        UIBase.applyDefaultWidgetSkinTo(this.doneButton, UIBase.shouldBlur());
    }

    @Override
    public void onWindowClosedExternally() {
        this.onCancel.accept(this.presetColor);
    }

    @Override
    public void renderBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        UITheme theme = UIBase.getUITheme();
        RenderSystem.enableBlend();

        this.updateLayout();
        this.syncChannelInputs();

        this.renderLeftPanel(graphics, theme);
        this.renderInfoPanel(graphics, theme, mouseX, mouseY, partial);

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
            this.confirmAndCloseWindow();
            return true;
        }
        return super.keyPressed(button, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (RenderingUtils.isXYInArea(mouseX, mouseY, this.pickerX, this.pickerY, this.pickerSize, this.pickerSize)) {
                this.clearInputFocus();
                this.draggingSV = true;
                this.updateSVFromMouse(mouseX, mouseY);
                return true;
            }
            if (RenderingUtils.isXYInArea(mouseX, mouseY, this.hueX, this.hueY, this.hueWidth, this.hueHeight)) {
                this.clearInputFocus();
                this.draggingHue = true;
                this.updateHueFromMouse(mouseX);
                return true;
            }
            if (RenderingUtils.isXYInArea(mouseX, mouseY, this.alphaX, this.alphaY, this.alphaWidth, this.alphaHeight)) {
                this.clearInputFocus();
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
        int top = 24;
        int bottom = 56;
        int availableWidth = this.width - (padding * 2);
        int availableHeight = this.height - top - bottom;
        int labelHeight = (int)UIBase.getUITextHeightNormal();
        int extraHeight = (labelHeight + SLIDER_HEIGHT + 2) * 2 + (SLIDER_GAP * 3);
        int minInfoWidth = 160;

        this.pickerSize = Math.min(MAX_PICKER_SIZE, Math.max(MIN_PICKER_SIZE, availableHeight - extraHeight));

        int maxPickerSizeByWidth = Math.max(MIN_PICKER_SIZE, availableWidth - COLUMN_GAP - minInfoWidth);
        this.pickerSize = Math.min(this.pickerSize, maxPickerSizeByWidth);

        int infoCandidateWidth = Math.max(0, availableWidth - this.pickerSize - COLUMN_GAP);
        int totalWidth = this.pickerSize + COLUMN_GAP + infoCandidateWidth;
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

        this.infoX = this.pickerX + this.pickerSize + COLUMN_GAP;
        this.infoY = this.pickerY;
        this.infoWidth = infoCandidateWidth;
        this.infoHeight = leftPanelHeight;
    }

    private void renderLeftPanel(@NotNull GuiGraphics graphics, @NotNull UITheme theme) {
        int panelX = this.pickerX - PANEL_PADDING;
        int panelY = this.pickerY - PANEL_PADDING;
        int panelW = this.pickerSize + (PANEL_PADDING * 2);
        int panelH = (this.alphaY + this.alphaHeight - this.pickerY) + (PANEL_PADDING * 2);
        float rounding = UIBase.getInterfaceCornerRoundingRadius();

        UIBase.renderRoundedRect(graphics, panelX, panelY, panelW, panelH, rounding, rounding, rounding, rounding, UIBase.shouldBlur() ? theme.ui_blur_interface_area_background_color_type_1.getColorInt() : theme.ui_interface_area_background_color_type_1.getColorInt());
        UIBase.renderRoundedBorder(graphics, panelX, panelY, panelX + panelW, panelY + panelH, 1, rounding, rounding, rounding, rounding, UIBase.shouldBlur() ? theme.ui_blur_interface_area_border_color.getColorInt() : theme.ui_interface_area_border_color.getColorInt());

        this.renderColorArea(graphics);
        this.renderColorAreaSelector(graphics, theme);

        UIBase.renderText(graphics, Component.translatable("fancymenu.ui.color_picker.hue"), this.hueX, this.hueLabelY);
        this.renderHueSlider(graphics, theme);

        UIBase.renderText(graphics, Component.translatable("fancymenu.ui.color_picker.alpha"), this.alphaX, this.alphaLabelY);
        this.renderAlphaSlider(graphics, theme);
    }

    private void renderInfoPanel(@NotNull GuiGraphics graphics, @NotNull UITheme theme, int mouseX, int mouseY, float partial) {
        int panelX = this.infoX - PANEL_PADDING;
        int panelY = this.infoY - PANEL_PADDING;
        int panelW = this.infoWidth + (PANEL_PADDING * 2);
        int panelH = Math.max(this.infoHeight, 140) + (PANEL_PADDING * 2);
        float rounding = UIBase.getInterfaceCornerRoundingRadius();

        UIBase.renderRoundedRect(graphics, panelX, panelY, panelW, panelH, rounding, rounding, rounding, rounding, UIBase.shouldBlur() ? theme.ui_blur_interface_area_background_color_type_1.getColorInt() : theme.ui_interface_area_background_color_type_1.getColorInt());
        UIBase.renderRoundedBorder(graphics, panelX, panelY, panelX + panelW, panelY + panelH, 1, rounding, rounding, rounding, rounding, UIBase.shouldBlur() ? theme.ui_blur_interface_area_border_color.getColorInt() : theme.ui_interface_area_border_color.getColorInt());

        int x = this.infoX;
        int y = this.infoY;
        int labelHeight = (int)UIBase.getUITextHeightNormal();

        int previewLabelY = y;
        int previewRectY = previewLabelY + labelHeight + 4;
        UIBase.renderText(graphics, Component.translatable("fancymenu.ui.color_picker.preview"), x, previewLabelY);

        this.renderCheckerboard(graphics, x, previewRectY, PREVIEW_SIZE, PREVIEW_SIZE, 6);
        graphics.fill(x, previewRectY, x + PREVIEW_SIZE, previewRectY + PREVIEW_SIZE, this.getCurrentColorInt());
        UIBase.renderBorder(graphics, x, previewRectY, x + PREVIEW_SIZE, previewRectY + PREVIEW_SIZE, 1, theme.ui_interface_widget_border_color.getColorInt(), true, true, true, true);

        if (this.presetColor != null) {
            int presetX = x + PREVIEW_SIZE + 12;
            UIBase.renderText(graphics, Component.translatable("fancymenu.ui.color_picker.original"), presetX, previewLabelY);
            this.renderCheckerboard(graphics, presetX, previewRectY, PREVIEW_SIZE, PREVIEW_SIZE, 6);
            graphics.fill(presetX, previewRectY, presetX + PREVIEW_SIZE, previewRectY + PREVIEW_SIZE, this.presetColor.getColorInt());
            UIBase.renderBorder(graphics, presetX, previewRectY, presetX + PREVIEW_SIZE, previewRectY + PREVIEW_SIZE, 1, theme.ui_interface_widget_border_color.getColorInt(), true, true, true, true);
        }

        y = previewRectY + PREVIEW_SIZE + 12;

        UIBase.renderText(graphics, Component.translatable("fancymenu.ui.color_picker.hex"), x, y);
        y += labelHeight + 2;
        UIBase.renderText(graphics, Component.literal(this.getCurrentHex()), x, y, theme.ui_interface_generic_text_color.getColorInt());

        y += labelHeight + 8;
        UIBase.renderText(graphics, Component.translatable("fancymenu.ui.color_picker.rgba"), x, y);
        y += labelHeight + 4;
        y = this.renderChannelInputs(graphics, x, y, mouseX, mouseY, partial);

        y += 8;
        UIBase.renderText(graphics, Component.translatable("fancymenu.ui.color_picker.hsv"), x, y);
        y += labelHeight + 2;
        int h = Math.round(this.colorState.getHue() * 360.0F);
        int s = Math.round(this.colorState.getSaturation() * 100.0F);
        int v = Math.round(this.colorState.getValue() * 100.0F);
        UIBase.renderText(graphics, Component.literal("H: " + h + "  S: " + s + "%  V: " + v + "%"), x, y, theme.ui_interface_generic_text_color.getColorInt());
    }

    /**
     * Lays out and renders the RGBA channel inputs in a grid, each with its channel letter in front of it.
     *
     * @return The bottom Y of the input grid.
     */
    private int renderChannelInputs(@NotNull GuiGraphics graphics, int x, int y, int mouseX, int mouseY, float partial) {
        float labelHeight = UIBase.getUITextHeightNormal();
        int labelWidth = 0;
        for (ColorPickerState.Channel channel : CHANNELS) {
            labelWidth = Math.max(labelWidth, (int) Math.ceil(UIBase.getUITextWidth(channel.getLabel())));
        }
        int columnWidth = (this.infoWidth - ((CHANNEL_INPUT_COLUMNS - 1) * CHANNEL_INPUT_COLUMN_GAP)) / CHANNEL_INPUT_COLUMNS;
        int inputWidth = Math.max(1, Math.min(CHANNEL_INPUT_MAX_WIDTH, columnWidth - labelWidth - CHANNEL_LABEL_GAP));
        int cellWidth = labelWidth + CHANNEL_LABEL_GAP + inputWidth + CHANNEL_INPUT_COLUMN_GAP;
        int cellHeight = CHANNEL_INPUT_HEIGHT + CHANNEL_INPUT_ROW_GAP;
        for (int i = 0; i < CHANNELS.length; i++) {
            int cellX = x + ((i % CHANNEL_INPUT_COLUMNS) * cellWidth);
            int cellY = y + ((i / CHANNEL_INPUT_COLUMNS) * cellHeight);
            UIBase.renderText(graphics, CHANNELS[i].getLabel(), cellX, cellY + ((CHANNEL_INPUT_HEIGHT - labelHeight) / 2.0F));
            ExtendedEditBox input = this.channelInputs[i];
            input.setX(cellX + labelWidth + CHANNEL_LABEL_GAP);
            input.setY(cellY);
            input.setWidth(inputWidth);
            input.render(graphics, mouseX, mouseY, partial);
        }
        int rows = (CHANNELS.length + CHANNEL_INPUT_COLUMNS - 1) / CHANNEL_INPUT_COLUMNS;
        return y + (rows * cellHeight) - CHANNEL_INPUT_ROW_GAP;
    }

    private void renderColorArea(@NotNull GuiGraphics graphics) {
        int hueRgb = Color.HSBtoRGB(this.colorState.getHue(), 1.0F, 1.0F);
        int hueColor = 0xFF000000 | (hueRgb & 0xFFFFFF);
        this.fillGradientHorizontal(graphics, this.pickerX, this.pickerY, this.pickerX + this.pickerSize, this.pickerY + this.pickerSize, 0xFFFFFFFF, hueColor);
        graphics.fillGradient(this.pickerX, this.pickerY, this.pickerX + this.pickerSize, this.pickerY + this.pickerSize, 0x00000000, 0xFF000000);
        UIBase.renderBorder(graphics, this.pickerX, this.pickerY, this.pickerX + this.pickerSize, this.pickerY + this.pickerSize, 1, UIBase.getUITheme().ui_interface_widget_border_color.getColorInt(), true, true, true, true);
    }

    private void renderColorAreaSelector(@NotNull GuiGraphics graphics, @NotNull UITheme theme) {
        int max = Math.max(1, this.pickerSize - 1);
        int selectorX = this.pickerX + Math.round(this.colorState.getSaturation() * max);
        int selectorY = this.pickerY + Math.round((1.0F - this.colorState.getValue()) * max);
        int outerSize = 7;
        int innerSize = 5;
        int outerX = selectorX - (outerSize / 2);
        int outerY = selectorY - (outerSize / 2);
        graphics.fill(outerX, outerY, outerX + outerSize, outerY + outerSize, theme.ui_interface_widget_border_color.getColorInt());
        int innerX = selectorX - (innerSize / 2);
        int innerY = selectorY - (innerSize / 2);
        graphics.fill(innerX, innerY, innerX + innerSize, innerY + innerSize, theme.ui_interface_generic_text_color.getColorInt());
    }

    private void renderHueSlider(@NotNull GuiGraphics graphics, @NotNull UITheme theme) {
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
        UIBase.renderBorder(graphics, this.hueX, this.hueY, this.hueX + this.hueWidth, this.hueY + this.hueHeight, 1, theme.ui_interface_widget_border_color.getColorInt(), true, true, true, true);

        int max = Math.max(1, this.hueWidth - 1);
        int markerX = this.hueX + Math.round(this.colorState.getHue() * max);
        graphics.fill(markerX - 1, this.hueY - 2, markerX + 1, this.hueY + this.hueHeight + 2, theme.ui_interface_generic_text_color.getColorInt());
    }

    private void renderAlphaSlider(@NotNull GuiGraphics graphics, @NotNull UITheme theme) {
        this.renderCheckerboard(graphics, this.alphaX, this.alphaY, this.alphaWidth, this.alphaHeight, 4);
        int rgb = this.getCurrentColorInt() & 0xFFFFFF;
        int leftColor = rgb;
        int rightColor = 0xFF000000 | rgb;
        this.fillGradientHorizontal(graphics, this.alphaX, this.alphaY, this.alphaX + this.alphaWidth, this.alphaY + this.alphaHeight, leftColor, rightColor);
        UIBase.renderBorder(graphics, this.alphaX, this.alphaY, this.alphaX + this.alphaWidth, this.alphaY + this.alphaHeight, 1, theme.ui_interface_widget_border_color.getColorInt(), true, true, true, true);

        int max = Math.max(1, this.alphaWidth - 1);
        int markerX = this.alphaX + Math.round(this.colorState.getAlpha() * max);
        graphics.fill(markerX - 1, this.alphaY - 2, markerX + 1, this.alphaY + this.alphaHeight + 2, theme.ui_interface_generic_text_color.getColorInt());
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
        float saturation = (float) ((mouseX - this.pickerX) / (double) max);
        float value = 1.0F - (float) ((mouseY - this.pickerY) / (double) max);
        if (this.colorState.setSaturationAndValue(saturation, value)) {
            this.notifyOnColorUpdate();
        }
    }

    private void updateHueFromMouse(double mouseX) {
        int max = Math.max(1, this.hueWidth - 1);
        if (this.colorState.setHue((float) ((mouseX - this.hueX) / (double) max))) {
            this.notifyOnColorUpdate();
        }
    }

    private void updateAlphaFromMouse(double mouseX) {
        int max = Math.max(1, this.alphaWidth - 1);
        if (this.colorState.setAlpha((float) ((mouseX - this.alphaX) / (double) max))) {
            this.notifyOnColorUpdate();
        }
    }

    private void onChannelInputChanged(@NotNull ColorPickerState.Channel channel, @NotNull String text) {
        if (this.syncingChannelInputs) return;
        int channelValue = ColorPickerState.parseChannelValue(text);
        if (channelValue < 0) return;
        if (this.colorState.setChannel(channel, channelValue)) {
            this.notifyOnColorUpdate();
        }
    }

    /**
     * Writes the current color to every channel input that is not focused.<br>
     * The focused input gets skipped, so text the user is still typing (empty input, leading zeros, values above 255) never gets replaced mid-edit.
     * It gets normalized to the actual channel value as soon as it loses focus.<br>
     * This runs every frame instead of reacting to focus changes, because inputs can lose focus in many ways (clicks, Tab navigation, re-init) that differ between Minecraft versions.
     * The channel texts are cached per color, so this does not allocate anything while the color stays the same.
     */
    private void syncChannelInputs() {
        int argb = this.colorState.getArgb();
        if ((this.channelInputTexts[0] == null) || (this.channelInputTextsArgb != argb)) {
            for (int i = 0; i < CHANNELS.length; i++) {
                this.channelInputTexts[i] = String.valueOf(this.colorState.getChannel(CHANNELS[i]));
            }
            this.channelInputTextsArgb = argb;
        }
        this.syncingChannelInputs = true;
        for (int i = 0; i < CHANNELS.length; i++) {
            ExtendedEditBox input = this.channelInputs[i];
            if ((input == null) || input.isFocused()) continue;
            if (!this.channelInputTexts[i].equals(input.getValue())) {
                input.setValue(this.channelInputTexts[i]);
            }
        }
        this.syncingChannelInputs = false;
    }

    private void clearInputFocus() {
        for (ExtendedEditBox input : this.channelInputs) {
            if (input != null) input.setFocused(false);
        }
        this.setFocused(null);
    }

    private void confirmAndCloseWindow() {
        this.onDone.accept(DrawableColor.of(this.getCurrentHex()));
        this.closeWindow();
    }

    private int getCurrentColorInt() {
        return this.colorState.getArgb();
    }

    private String getCurrentHex() {
        return DrawableColor.of(this.colorState.getChannel(ColorPickerState.Channel.RED), this.colorState.getChannel(ColorPickerState.Channel.GREEN), this.colorState.getChannel(ColorPickerState.Channel.BLUE), this.colorState.getChannel(ColorPickerState.Channel.ALPHA)).getHex();
    }

    private void notifyOnColorUpdate() {
        this.onColorUpdate.accept(DrawableColor.of(this.getCurrentHex()));
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
