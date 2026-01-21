package de.keksuccino.fancymenu.util.rendering.ui;

import java.awt.Color;
import java.util.Objects;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.SmoothRectangleRenderer;
import de.keksuccino.fancymenu.util.rendering.text.smooth.SmoothFont;
import de.keksuccino.fancymenu.util.rendering.text.smooth.SmoothFonts;
import de.keksuccino.fancymenu.util.rendering.text.smooth.SmoothTextRenderer;
import de.keksuccino.fancymenu.util.rendering.text.smooth.TextDimensions;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UIColorThemeRegistry;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UITheme;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.EditBoxSuggestions;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.AbstractExtendedSlider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class UIBase extends RenderingUtils {

    private static final boolean USE_DEFAULT_MINECRAFT_FONT = false;

    public static final int ELEMENT_BORDER_THICKNESS = 1;
    public static final int VERTICAL_SCROLL_BAR_WIDTH = 5;
    public static final int VERTICAL_SCROLL_BAR_HEIGHT = 40;
    public static final int HORIZONTAL_SCROLL_BAR_WIDTH = 40;
    public static final int HORIZONTAL_SCROLL_BAR_HEIGHT = 5;

    @NotNull
    public static SmoothFont getUIFont() {
        return Objects.requireNonNull(SmoothFonts.NOTO_SANS.get());
    }

    public static float getUITextSizeNormal() {
        if (UIBase.getUIScale() == 1) return SmoothFonts.DEFAULT_TEXT_SIZE + 2; // make text easier to read on small scales
        return SmoothFonts.DEFAULT_TEXT_SIZE;
    }

    public static float getUITextSizeLarge() {
        return getUITextSizeNormal() + 5;
    }

    public static float getUITextSizeSmall() {
        return Math.max(3, getUITextSizeNormal() - 2);
    }

    public static float getUITextHeightNormal() {
        if (USE_DEFAULT_MINECRAFT_FONT) return Minecraft.getInstance().font.lineHeight;
        return getUIFont().getLineHeight(getUITextSizeNormal());
    }

    public static float getUITextHeightLarge() {
        if (USE_DEFAULT_MINECRAFT_FONT) return Minecraft.getInstance().font.lineHeight;
        return getUIFont().getLineHeight(getUITextSizeLarge());
    }

    public static float getUITextHeightSmall() {
        if (USE_DEFAULT_MINECRAFT_FONT) return Minecraft.getInstance().font.lineHeight;
        return getUIFont().getLineHeight(getUITextSizeSmall());
    }

    public static float getUITextHeight(float textSize) {
        if (USE_DEFAULT_MINECRAFT_FONT) return Minecraft.getInstance().font.lineHeight;
        return getUIFont().getLineHeight(textSize);
    }

    public static float getUITextWidthNormal(@NotNull Component text) {
        if (USE_DEFAULT_MINECRAFT_FONT) return Minecraft.getInstance().font.width(text);
        return SmoothTextRenderer.getTextWidth(getUIFont(), text, getUITextSizeNormal());
    }

    public static float getUITextWidthLarge(@NotNull Component text) {
        if (USE_DEFAULT_MINECRAFT_FONT) return Minecraft.getInstance().font.width(text);
        return SmoothTextRenderer.getTextWidth(getUIFont(), text, getUITextSizeLarge());
    }

    public static float getUITextWidthSmall(@NotNull Component text) {
        if (USE_DEFAULT_MINECRAFT_FONT) return Minecraft.getInstance().font.width(text);
        return SmoothTextRenderer.getTextWidth(getUIFont(), text, getUITextSizeSmall());
    }

    public static float getUITextWidth(@NotNull String text) {
        if (USE_DEFAULT_MINECRAFT_FONT) return Minecraft.getInstance().font.width(text);
        return SmoothTextRenderer.getTextWidth(getUIFont(), text, getUITextSizeNormal());
    }

    public static float getUITextWidth(@NotNull Component text, float textSize) {
        if (USE_DEFAULT_MINECRAFT_FONT) return Minecraft.getInstance().font.width(text);
        return SmoothTextRenderer.getTextWidth(getUIFont(), text, textSize);
    }

    public static float getUITextWidth(@NotNull String text, float textSize) {
        if (USE_DEFAULT_MINECRAFT_FONT) return Minecraft.getInstance().font.width(text);
        return SmoothTextRenderer.getTextWidth(getUIFont(), text, textSize);
    }

    /**
     * Retrieves the currently active UI color theme for FancyMenu.
     *
     * @return active {@link UITheme}
     */
    @NotNull
    public static UITheme getUITheme() {
        return UIColorThemeRegistry.getActiveTheme();
    }

    /**
     * Applies the default UI skin to the given widget and returns it.<br>
     * Does not apply skins for blurred environments.
     */
    public static <T> T applyDefaultWidgetSkinTo(@Nullable T widget) {
        return applyDefaultWidgetSkinTo(widget, false);
    }

    /**
     * Applies the default UI skin to the given widget and returns it.
     */
    @SuppressWarnings("all")
    public static <T> T applyDefaultWidgetSkinTo(@Nullable T widget, boolean forBlur) {
        if (widget == null) return null;
        if (widget instanceof ExtendedButton e) {
            return (T) applyDefaultButtonSkinTo(e, forBlur);
        }
        if (widget instanceof ExtendedEditBox e) {
            return (T) applyDefaultEditBoxSkinTo(e, forBlur);
        }
        if (widget instanceof EditBoxSuggestions s) {
            return (T) applyDefaultEditBoxSuggestionsSkinTo(s);
        }
        if (widget instanceof AbstractExtendedSlider s) {
            return (T) applyDefaultV2SliderSkinTo(s, forBlur);
        }
        return widget;
    }

    /**
     * Applies the default FancyMenu slider skin depending on blur state.
     */
    private static AbstractExtendedSlider applyDefaultV2SliderSkinTo(AbstractExtendedSlider slider, boolean forBlur) {
        if (forBlur) {
            slider.setSliderBackgroundColorNormal(UIBase.getUITheme().ui_blur_interface_widget_background_color_normal_type_1);
            slider.setSliderBorderColorNormal(UIBase.getUITheme().ui_blur_interface_widget_border_color);
            slider.setSliderHandleColorNormal(UIBase.getUITheme().ui_blur_interface_widget_background_color_normal_type_2);
            slider.setSliderHandleColorHover(UIBase.getUITheme().ui_blur_interface_widget_background_color_hover_type_1);
            slider.setLabelColorNormal(UIBase.getUITheme().ui_blur_interface_widget_label_color_normal);
            slider.setLabelColorInactive(UIBase.getUITheme().ui_blur_interface_widget_label_color_inactive);
        } else {
            slider.setSliderBackgroundColorNormal(UIBase.getUITheme().ui_interface_widget_background_color_normal_type_1);
            slider.setSliderBorderColorNormal(UIBase.getUITheme().ui_interface_widget_border_color);
            slider.setSliderHandleColorNormal(UIBase.getUITheme().ui_interface_widget_background_color_normal_type_2);
            slider.setSliderHandleColorHover(UIBase.getUITheme().ui_interface_widget_background_color_hover_type_1);
            slider.setLabelColorNormal(UIBase.getUITheme().ui_interface_widget_label_color_normal);
            slider.setLabelColorInactive(UIBase.getUITheme().ui_interface_widget_label_color_inactive);
        }
        slider.setLabelShadow(false);
        slider.setRoundedColorBackgroundEnabled(true);
        return slider;
    }

    /**
     * Applies FancyMenu's default suggestion list colors and shadow settings.
     */
    private static EditBoxSuggestions applyDefaultEditBoxSuggestionsSkinTo(EditBoxSuggestions editBoxSuggestions) {
        editBoxSuggestions.setBackgroundColor(UIBase.getUITheme().input_field_suggestions_background_color);
        editBoxSuggestions.setNormalTextColor(UIBase.getUITheme().input_field_suggestions_text_color_normal);
        editBoxSuggestions.setSelectedTextColor(UIBase.getUITheme().input_field_suggestions_text_color_selected);
        editBoxSuggestions.setTextShadow(false);
        return editBoxSuggestions;
    }

    /**
     * Applies the default FancyMenu edit box skin, optionally using the blur palette.
     */
    private static ExtendedEditBox applyDefaultEditBoxSkinTo(ExtendedEditBox editBox, boolean forBlur) {
        UITheme theme = UIBase.getUITheme();
        if (forBlur) {
            editBox.setTextColor(theme.ui_blur_interface_input_field_text_color_normal);
            editBox.setTextColorUneditable(theme.ui_blur_interface_input_field_text_color_uneditable);
            editBox.setBackgroundColor(theme.ui_blur_interface_input_field_background_color);
            editBox.setBorderNormalColor(theme.ui_blur_interface_input_field_border_color_normal);
            editBox.setBorderFocusedColor(theme.ui_blur_interface_input_field_border_color_focused);
            editBox.setSuggestionTextColor(theme.ui_blur_interface_input_field_suggestion_text_color);
        } else {
            editBox.setTextColor(theme.ui_interface_input_field_text_color_normal);
            editBox.setTextColorUneditable(theme.ui_interface_input_field_text_color_uneditable);
            editBox.setBackgroundColor(theme.ui_interface_input_field_background_color);
            editBox.setBorderNormalColor(theme.ui_interface_input_field_border_color_normal);
            editBox.setBorderFocusedColor(theme.ui_interface_input_field_border_color_focused);
            editBox.setSuggestionTextColor(theme.ui_interface_input_field_suggestion_text_color);
        }
        editBox.setTextShadow_FancyMenu(false);
        editBox.setRoundedColorBackgroundEnabled(true);
        return editBox;
    }

    /**
     * Applies the default FancyMenu button skin, optionally using the blur palette.
     */
    private static ExtendedButton applyDefaultButtonSkinTo(ExtendedButton button, boolean forBlur) {
        if (forBlur) {
            button.setBackgroundColorNormal(UIBase.getUITheme().ui_blur_interface_widget_background_color_normal_type_1);
            button.setBackgroundColorHover(UIBase.getUITheme().ui_blur_interface_widget_background_color_hover_type_1);
            button.setBackgroundColorInactive(UIBase.getUITheme().ui_blur_interface_widget_background_color_normal_type_1);
            button.setBorderColorNormal(UIBase.getUITheme().ui_blur_interface_widget_border_color);
            button.setBorderColorHover(UIBase.getUITheme().ui_blur_interface_widget_border_color);
            button.setBorderColorInactive(UIBase.getUITheme().ui_blur_interface_widget_border_color);
            button.setLabelBaseColorNormal(UIBase.getUITheme().ui_blur_interface_widget_label_color_normal);
            button.setLabelBaseColorInactive(UIBase.getUITheme().ui_blur_interface_widget_label_color_inactive);
        } else {
            button.setBackgroundColorNormal(UIBase.getUITheme().ui_interface_widget_background_color_normal_type_1);
            button.setBackgroundColorHover(UIBase.getUITheme().ui_interface_widget_background_color_hover_type_1);
            button.setBackgroundColorInactive(UIBase.getUITheme().ui_interface_widget_background_color_normal_type_1);
            button.setBorderColorNormal(UIBase.getUITheme().ui_interface_widget_border_color);
            button.setBorderColorHover(UIBase.getUITheme().ui_interface_widget_border_color);
            button.setBorderColorInactive(UIBase.getUITheme().ui_interface_widget_border_color);
            button.setLabelBaseColorNormal(UIBase.getUITheme().ui_interface_widget_label_color_normal);
            button.setLabelBaseColorInactive(UIBase.getUITheme().ui_interface_widget_label_color_inactive);
        }
        button.setLabelShadowEnabled(false);
        button.setRoundedColorBackgroundEnabled(true);
        return button;
    }

    /**
     * Returns the logical UI scale used for FancyMenu's UI elements, after applying
     * automatic adjustments (4K auto scale and Unicode font enforcement).
     */
    public static float getUIScale() {
        float uiScale = FancyMenu.getOptions().uiScale.getValue();
        //Handle "Auto" scale (set scale to 2 if window bigger than 3000x1700 to show 1080p and 2K screens on scale 1 and 4K on scale 2)
        if (uiScale == 4) {
            uiScale = 1;
            if ((Minecraft.getInstance().getWindow().getWidth() > 3000) || (Minecraft.getInstance().getWindow().getHeight() > 1700)) {
                uiScale = 2;
            }
        }
        //Force a scale of 2 or bigger if Unicode font is enabled
        if (Minecraft.getInstance().isEnforceUnicode() && (uiScale < 2.0F)) {
            uiScale = 2;
        }
        return uiScale;
    }

    /**
     * Returns the current FancyMenu UI scale corrected for the game's GUI scale,
     * ready to be used directly for rendering in window pixel space.
     */
    public static float getFixedUIScale() {
        return calculateFixedScale(getUIScale());
    }

    /**
     * Converts a logical FancyMenu UI scale to a render-ready scale by compensating
     * for the current Minecraft GUI scale.
     *
     * @param fixedScale logical FancyMenu UI scale (typically from user options)
     * @return render-scale multiplier that aligns with window pixels
     */
    public static float calculateFixedScale(float fixedScale) {
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        if (guiScale == 0.0D) return fixedScale; // fallback to avoid divide-by-zero
        return fixedScale / (float) guiScale;
    }

    public static float getWidgetCornerRoundingRadius() {
        return getUITheme().widget_corner_rounding_radius;
    }

    /**
     * Default corner radius for UI interfaces.
     */
    public static float getInterfaceCornerRoundingRadius() {
        return getUITheme().interface_corner_rounding_radius;
    }

    /**
     * Blur strength applied to supported UI surfaces.
     */
    public static float getBlurRadius() {
        return 4.0F;
    }

    /**
     * Whether UI blur is currently enabled in FancyMenu options.
     */
    public static boolean shouldBlur() {
        if (!getUITheme().allow_blur) return false;
        return FancyMenu.getOptions().enableUiBlur.getValue();
    }

    /**
     * Draws a 4x4 dot at the given floating-point position with the provided RGB color.
     */
    public static void renderListingDot(GuiGraphics graphics, float x, float y, int color) {
        fillF(graphics, x, y, x + 4, y + 4, color);
    }

    /**
     * Draws a 4x4 dot at the given integer position using the provided {@link Color}.
     */
    public static void renderListingDot(GuiGraphics graphics, int x, int y, Color color) {
        graphics.fill(x, y, x + 4, y + 4, color.getRGB());
    }

    /**
     * Renders a rectangular border using a {@link DrawableColor} abstraction.
     */
    public static void renderBorder(GuiGraphics graphics, int xMin, int yMin, int xMax, int yMax, int borderThickness, DrawableColor borderColor, boolean renderTop, boolean renderLeft, boolean renderRight, boolean renderBottom) {
        renderBorder(graphics, xMin, yMin, xMax, yMax, borderThickness, borderColor.getColorInt(), renderTop, renderLeft, renderRight, renderBottom);
    }

    /**
     * Renders a rectangular border using an AWT {@link Color}.
     */
    public static void renderBorder(GuiGraphics graphics, int xMin, int yMin, int xMax, int yMax, int borderThickness, Color borderColor, boolean renderTop, boolean renderLeft, boolean renderRight, boolean renderBottom) {
        renderBorder(graphics, xMin, yMin, xMax, yMax, borderThickness, borderColor.getRGB(), renderTop, renderLeft, renderRight, renderBottom);
    }

    /**
     * Renders a rectangular border with configurable sides and thickness.
     */
    public static void renderBorder(GuiGraphics graphics, float xMin, float yMin, float xMax, float yMax, float borderThickness, int borderColor, boolean renderTop, boolean renderLeft, boolean renderRight, boolean renderBottom) {
        if (renderTop) {
            RenderingUtils.fillF(graphics, xMin, yMin, xMax, yMin + borderThickness, borderColor);
        }
        if (renderLeft) {
            RenderingUtils.fillF(graphics, xMin, yMin + borderThickness, xMin + borderThickness, yMax - borderThickness, borderColor);
        }
        if (renderRight) {
            RenderingUtils.fillF(graphics, xMax - borderThickness, yMin + borderThickness, xMax, yMax - borderThickness, borderColor);
        }
        if (renderBottom) {
            RenderingUtils.fillF(graphics, xMin, yMax - borderThickness, xMax, yMax, borderColor);
        }
    }

    public static void renderRoundedRect(@NotNull GuiGraphics graphics, float x, float y, float width, float height, float topLeftRadius, float topRightRadius, float bottomRightRadius, float bottomLeftRadius, int color) {
        if (width <= 0.0F || height <= 0.0F) return;
        SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(graphics, x, y, width, height, topLeftRadius, topRightRadius, bottomRightRadius, bottomLeftRadius, color, 1.0F);
    }

    public static void renderRoundedBorder(@NotNull GuiGraphics graphics, float xMin, float yMin, float xMax, float yMax, float borderThickness, float innerTopLeftRadius, float innerTopRightRadius, float innerBottomRightRadius, float innerBottomLeftRadius, int borderColor) {
        float width = xMax - xMin;
        float height = yMax - yMin;
        if (width <= 0.0F || height <= 0.0F || borderThickness <= 0.0F) return;
        SmoothRectangleRenderer.renderSmoothBorderRoundAllCornersScaled(graphics, xMin, yMin, width, height, borderThickness, innerTopLeftRadius, innerTopRightRadius, innerBottomRightRadius, innerBottomLeftRadius, borderColor, 1.0F);
    }

    /**
     * Draws a default-colored label component at integer coordinates.
     */
    public static TextDimensions renderText(GuiGraphics graphics, Component text, float x, float y) {
        return renderText(graphics, text, x, y, getUITheme().ui_interface_widget_label_color_normal.getColorInt());
    }

    /**
     * Draws a default-colored label string at integer coordinates.
     */
    public static TextDimensions renderText(GuiGraphics graphics, String text, float x, float y) {
        return renderText(graphics, Component.literal(text), x, y, getUITheme().ui_interface_widget_label_color_normal.getColorInt());
    }

    /**
     * Draws a label string with the given base color.
     */
    public static TextDimensions renderText(GuiGraphics graphics, String text, float x, float y, int baseColor) {
        return renderText(graphics, Component.literal(text), x, y, baseColor);
    }

    /**
     * Draws a label component with the given base color.
     */
    public static TextDimensions renderText(GuiGraphics graphics, Component text, float x, float y, int baseColor) {
        return renderText(graphics, text, x, y, baseColor, getUITextSizeNormal());
    }

    /**
     * Draws a label component with the given base color, text size, and shadow state.
     */
    public static TextDimensions renderText(GuiGraphics graphics, Component text, float x, float y, int baseColor, float textSize) {
        if (USE_DEFAULT_MINECRAFT_FONT) {
            int width = (int)getUITextWidthNormal(text);
            TextDimensions dimensions = new TextDimensions(width, Minecraft.getInstance().font.lineHeight);
            graphics.drawString(Minecraft.getInstance().font, text, (int)x, (int)y, baseColor, false);
            return dimensions;
        }
        return SmoothTextRenderer.renderText(graphics, getUIFont(), text, x, y, baseColor, textSize, false);
    }

}