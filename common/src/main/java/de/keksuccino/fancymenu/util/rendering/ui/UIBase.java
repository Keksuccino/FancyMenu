package de.keksuccino.fancymenu.util.rendering.ui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UIColorThemeRegistry;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UIColorTheme;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.EditBoxSuggestions;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.AbstractExtendedSlider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector2f;

@SuppressWarnings("unused")
public class UIBase extends RenderingUtils {

    public static final int ELEMENT_BORDER_THICKNESS = 1;
    public static final int VERTICAL_SCROLL_BAR_WIDTH = 5;
    public static final int VERTICAL_SCROLL_BAR_HEIGHT = 40;
    public static final int HORIZONTAL_SCROLL_BAR_WIDTH = 40;
    public static final int HORIZONTAL_SCROLL_BAR_HEIGHT = 5;

    /**
     * Retrieves the currently active UI color theme for FancyMenu.
     *
     * @return active {@link UIColorTheme}
     */
    @NotNull
    public static UIColorTheme getUIColorTheme() {
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
            slider.setSliderBackgroundColorNormal(UIBase.getUIColorTheme().ui_blur_interface_widget_background_color_normal_type_1);
            slider.setSliderBorderColorNormal(UIBase.getUIColorTheme().ui_blur_interface_widget_border_color);
            slider.setSliderHandleColorNormal(UIBase.getUIColorTheme().ui_blur_interface_widget_background_color_normal_type_2);
            slider.setSliderHandleColorHover(UIBase.getUIColorTheme().ui_blur_interface_widget_background_color_hover_type_1);
            slider.setLabelColorNormal(UIBase.getUIColorTheme().ui_blur_interface_widget_label_color_normal);
            slider.setLabelColorInactive(UIBase.getUIColorTheme().ui_blur_interface_widget_label_color_inactive);
        } else {
            slider.setSliderBackgroundColorNormal(UIBase.getUIColorTheme().element_background_color_normal);
            slider.setSliderBorderColorNormal(UIBase.getUIColorTheme().element_border_color_normal);
            slider.setSliderHandleColorNormal(UIBase.getUIColorTheme().slider_handle_color_normal);
            slider.setSliderHandleColorHover(UIBase.getUIColorTheme().slider_handle_color_hover);
            slider.setLabelColorNormal(UIBase.getUIColorTheme().element_label_color_normal);
            slider.setLabelColorInactive(UIBase.getUIColorTheme().element_label_color_inactive);
        }
        slider.setLabelShadow(FancyMenu.getOptions().enableUiTextShadow.getValue());
        slider.setRoundedColorBackgroundEnabled(true);
        return slider;
    }

    /**
     * Applies FancyMenu's default suggestion list colors and shadow settings.
     */
    private static EditBoxSuggestions applyDefaultEditBoxSuggestionsSkinTo(EditBoxSuggestions editBoxSuggestions) {
        editBoxSuggestions.setBackgroundColor(UIBase.getUIColorTheme().suggestions_background_color);
        editBoxSuggestions.setNormalTextColor(UIBase.getUIColorTheme().suggestions_text_color_normal);
        editBoxSuggestions.setSelectedTextColor(UIBase.getUIColorTheme().suggestions_text_color_selected);
        editBoxSuggestions.setTextShadow(FancyMenu.getOptions().enableUiTextShadow.getValue());
        return editBoxSuggestions;
    }

    /**
     * Applies the default FancyMenu edit box skin, optionally using the blur palette.
     */
    private static ExtendedEditBox applyDefaultEditBoxSkinTo(ExtendedEditBox editBox, boolean forBlur) {
        UIColorTheme theme = UIBase.getUIColorTheme();
        if (forBlur) {
            editBox.setTextColor(theme.ui_blur_interface_input_field_text_color_normal);
            editBox.setTextColorUneditable(theme.ui_blur_interface_input_field_text_color_uneditable);
            editBox.setBackgroundColor(theme.ui_blur_interface_input_field_background_color);
            editBox.setBorderNormalColor(theme.ui_blur_interface_input_field_border_color_normal);
            editBox.setBorderFocusedColor(theme.ui_blur_interface_input_field_border_color_focused);
            editBox.setSuggestionTextColor(theme.ui_blur_interface_input_field_suggestion_text_color);
        } else {
            editBox.setTextColor(theme.edit_box_text_color_normal);
            editBox.setTextColorUneditable(theme.edit_box_text_color_uneditable);
            editBox.setBackgroundColor(theme.edit_box_background_color);
            editBox.setBorderNormalColor(theme.edit_box_border_color_normal);
            editBox.setBorderFocusedColor(theme.edit_box_border_color_focused);
            editBox.setSuggestionTextColor(theme.edit_box_suggestion_text_color);
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
            button.setBackgroundColorNormal(UIBase.getUIColorTheme().ui_blur_interface_widget_background_color_normal_type_1);
            button.setBackgroundColorHover(UIBase.getUIColorTheme().ui_blur_interface_widget_background_color_hover_type_1);
            button.setBackgroundColorInactive(UIBase.getUIColorTheme().ui_blur_interface_widget_background_color_normal_type_1);
            button.setBorderColorNormal(UIBase.getUIColorTheme().ui_blur_interface_widget_border_color);
            button.setBorderColorHover(UIBase.getUIColorTheme().ui_blur_interface_widget_border_color);
            button.setBorderColorInactive(UIBase.getUIColorTheme().ui_blur_interface_widget_border_color);
            button.setLabelBaseColorNormal(UIBase.getUIColorTheme().ui_blur_interface_widget_label_color_normal);
            button.setLabelBaseColorInactive(UIBase.getUIColorTheme().ui_blur_interface_widget_label_color_inactive);
        } else {
            button.setBackgroundColorNormal(UIBase.getUIColorTheme().element_background_color_normal);
            button.setBackgroundColorHover(UIBase.getUIColorTheme().element_background_color_hover);
            button.setBackgroundColorInactive(UIBase.getUIColorTheme().element_background_color_normal);
            button.setBorderColorNormal(UIBase.getUIColorTheme().element_border_color_normal);
            button.setBorderColorHover(UIBase.getUIColorTheme().element_border_color_hover);
            button.setBorderColorInactive(UIBase.getUIColorTheme().element_border_color_normal);
            button.setLabelBaseColorNormal(UIBase.getUIColorTheme().element_label_color_normal);
            button.setLabelBaseColorInactive(UIBase.getUIColorTheme().element_label_color_inactive);
        }
        button.setLabelShadowEnabled(FancyMenu.getOptions().enableUiTextShadow.getValue());
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
        return 4.0F;
    }

    /**
     * Default corner radius for UI interfaces.
     */
    public static float getInterfaceCornerRoundingRadius() {
        return 6.0F;
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

    /**
     * Draws a filled rounded rectangle using triangle fans for smooth corners.
     */
    public static void renderRoundedRect(@NotNull GuiGraphics graphics, float x, float y, float width, float height, float topLeftRadius, float topRightRadius, float bottomRightRadius, float bottomLeftRadius, int color) {
        float clampedWidth = Math.max(0.0F, width);
        float clampedHeight = Math.max(0.0F, height);
        if (clampedWidth <= 0.0F || clampedHeight <= 0.0F) {
            return;
        }

        float[] radii = clampCornerRadii(topLeftRadius, topRightRadius, bottomRightRadius, bottomLeftRadius, clampedWidth, clampedHeight);
        float topLeft = radii[0];
        float topRight = radii[1];
        float bottomRight = radii[2];
        float bottomLeft = radii[3];

        int cornerSegments = getCornerVertexCount(Math.max(Math.max(topLeft, topRight), Math.max(bottomRight, bottomLeft)));
        List<Vector2f> outline = buildRoundedRectOutline(x, y, clampedWidth, clampedHeight, topLeft, topRight, bottomRight, bottomLeft, cornerSegments);
        if (outline.isEmpty()) {
            return;
        }

        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f pose = graphics.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        float centerX = x + clampedWidth * 0.5F;
        float centerY = y + clampedHeight * 0.5F;
        for (int i = 0; i < outline.size(); i++) {
            Vector2f p1 = outline.get(i);
            Vector2f p2 = outline.get((i + 1) % outline.size());
            addColoredVertex(buffer, pose, centerX, centerY, color);
            addColoredVertex(buffer, pose, p1.x(), p1.y(), color);
            addColoredVertex(buffer, pose, p2.x(), p2.y(), color);
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /**
     * Draws a rounded border by extruding between outer and inner rounded outlines.
     */
    public static void renderRoundedBorder(@NotNull GuiGraphics graphics, float xMin, float yMin, float xMax, float yMax, float borderThickness, float innerTopLeftRadius, float innerTopRightRadius, float innerBottomRightRadius, float innerBottomLeftRadius, int borderColor) {
        float outerWidth = xMax - xMin;
        float outerHeight = yMax - yMin;
        float thickness = Math.max(0.0F, borderThickness);
        if (outerWidth <= 0.0F || outerHeight <= 0.0F || thickness <= 0.0F) {
            return;
        }

        float innerX = xMin + thickness;
        float innerY = yMin + thickness;
        float innerWidth = outerWidth - (thickness * 2.0F);
        float innerHeight = outerHeight - (thickness * 2.0F);

        if (innerWidth <= 0.0F || innerHeight <= 0.0F) {
            renderRoundedRect(graphics, xMin, yMin, outerWidth, outerHeight, innerTopLeftRadius, innerTopRightRadius, innerBottomRightRadius, innerBottomLeftRadius, borderColor);
            return;
        }

        float outerTopLeft = innerTopLeftRadius > 0.0F ? innerTopLeftRadius + thickness : 0.0F;
        float outerTopRight = innerTopRightRadius > 0.0F ? innerTopRightRadius + thickness : 0.0F;
        float outerBottomRight = innerBottomRightRadius > 0.0F ? innerBottomRightRadius + thickness : 0.0F;
        float outerBottomLeft = innerBottomLeftRadius > 0.0F ? innerBottomLeftRadius + thickness : 0.0F;

        float[] clampedOuterRadii = clampCornerRadii(outerTopLeft, outerTopRight, outerBottomRight, outerBottomLeft, outerWidth, outerHeight);
        float[] clampedInnerRadii = clampCornerRadii(innerTopLeftRadius, innerTopRightRadius, innerBottomRightRadius, innerBottomLeftRadius, innerWidth, innerHeight);

        int cornerSegments = getCornerVertexCount(Math.max(Math.max(clampedOuterRadii[0], clampedOuterRadii[1]), Math.max(clampedOuterRadii[2], clampedOuterRadii[3])));

        List<Vector2f> outerOutline = buildRoundedRectOutline(xMin, yMin, outerWidth, outerHeight, clampedOuterRadii[0], clampedOuterRadii[1], clampedOuterRadii[2], clampedOuterRadii[3], cornerSegments);
        List<Vector2f> innerOutline = buildRoundedRectOutline(innerX, innerY, innerWidth, innerHeight, clampedInnerRadii[0], clampedInnerRadii[1], clampedInnerRadii[2], clampedInnerRadii[3], cornerSegments);

        if (outerOutline.isEmpty() || innerOutline.isEmpty() || outerOutline.size() != innerOutline.size()) {
            renderBorder(graphics, xMin, yMin, xMax, yMax, thickness, borderColor, true, true, true, true);
            return;
        }

        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f pose = graphics.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < outerOutline.size(); i++) {
            Vector2f outer = outerOutline.get(i);
            Vector2f inner = innerOutline.get(i);
            addColoredVertex(buffer, pose, outer.x(), outer.y(), borderColor);
            addColoredVertex(buffer, pose, inner.x(), inner.y(), borderColor);
        }

        Vector2f firstOuter = outerOutline.get(0);
        Vector2f firstInner = innerOutline.get(0);
        addColoredVertex(buffer, pose, firstOuter.x(), firstOuter.y(), borderColor);
        addColoredVertex(buffer, pose, firstInner.x(), firstInner.y(), borderColor);

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }

    /**
     * Calculates the number of vertices used to approximate a rounded corner.
     */
    private static int getCornerVertexCount(float radius) {
        // Higher segment count keeps borders visually aligned with the smoother blur mask.
        return Math.max(8, Math.min(128, Mth.ceil(radius * 6.0F)));
    }

    /**
     * Clamps corner radii to fit within the given rectangle dimensions while keeping proportions.
     */
    private static float[] clampCornerRadii(float topLeft, float topRight, float bottomRight, float bottomLeft, float width, float height) {
        float tl = Math.max(0.0F, topLeft);
        float tr = Math.max(0.0F, topRight);
        float br = Math.max(0.0F, bottomRight);
        float bl = Math.max(0.0F, bottomLeft);

        float maxWidth = width * 0.5F;
        float maxHeight = height * 0.5F;
        tl = Math.min(tl, Math.min(maxWidth, maxHeight));
        tr = Math.min(tr, Math.min(maxWidth, maxHeight));
        br = Math.min(br, Math.min(maxWidth, maxHeight));
        bl = Math.min(bl, Math.min(maxWidth, maxHeight));

        float topSum = tl + tr;
        if (topSum > width) {
            float scale = width / topSum;
            tl *= scale;
            tr *= scale;
        }
        float bottomSum = bl + br;
        if (bottomSum > width) {
            float scale = width / bottomSum;
            bl *= scale;
            br *= scale;
        }
        float leftSum = tl + bl;
        if (leftSum > height) {
            float scale = height / leftSum;
            tl *= scale;
            bl *= scale;
        }
        float rightSum = tr + br;
        if (rightSum > height) {
            float scale = height / rightSum;
            tr *= scale;
            br *= scale;
        }
        return new float[]{tl, tr, br, bl};
    }

    /**
     * Builds the outline points for a rounded rectangle in clockwise order.
     */
    @NotNull
    private static List<Vector2f> buildRoundedRectOutline(float x, float y, float width, float height, float topLeftRadius, float topRightRadius, float bottomRightRadius, float bottomLeftRadius, int cornerSegments) {
        List<Vector2f> points = new ArrayList<>();
        float xMax = x + width;
        float yMax = y + height;

        points.add(new Vector2f(x + topLeftRadius, y));
        points.add(new Vector2f(xMax - topRightRadius, y));
        addCornerArc(points, xMax - topRightRadius, y + topRightRadius, topRightRadius, -90.0F, 0.0F, cornerSegments);

        points.add(new Vector2f(xMax, y + topRightRadius));
        points.add(new Vector2f(xMax, yMax - bottomRightRadius));
        addCornerArc(points, xMax - bottomRightRadius, yMax - bottomRightRadius, bottomRightRadius, 0.0F, 90.0F, cornerSegments);

        points.add(new Vector2f(xMax - bottomRightRadius, yMax));
        points.add(new Vector2f(x + bottomLeftRadius, yMax));
        addCornerArc(points, x + bottomLeftRadius, yMax - bottomLeftRadius, bottomLeftRadius, 90.0F, 180.0F, cornerSegments);

        points.add(new Vector2f(x, yMax - bottomLeftRadius));
        points.add(new Vector2f(x, y + topLeftRadius));
        addCornerArc(points, x + topLeftRadius, y + topLeftRadius, topLeftRadius, 180.0F, 270.0F, cornerSegments);

        return points;
    }

    /**
     * Adds arc points for a single rounded corner into an outline list.
     */
    private static void addCornerArc(@NotNull List<Vector2f> points, float centerX, float centerY, float radius, float startAngleDeg, float endAngleDeg, int segments) {
        if (radius <= 0.0F || segments <= 0) {
            return;
        }
        float angleStep = (endAngleDeg - startAngleDeg) / (float)segments;
        for (int i = 1; i <= segments; i++) {
            float angle = (startAngleDeg + (angleStep * i)) * ((float)Math.PI / 180.0F);
            points.add(new Vector2f(centerX + (Mth.cos(angle) * radius), centerY + (Mth.sin(angle) * radius)));
        }
    }

    /**
     * Writes a single colored vertex into the buffer with the given pose.
     */
    private static void addColoredVertex(@NotNull BufferBuilder buffer, @NotNull Matrix4f pose, float x, float y, int color) {
        buffer.addVertex(pose, x, y, 0.0F).setColor(color);
    }

    /**
     * Draws a default-colored label component at integer coordinates.
     */
    public static int drawElementLabel(GuiGraphics graphics, Font font, Component text, int x, int y) {
        return drawElementLabel(graphics, font, text, x, y, getUIColorTheme().element_label_color_normal.getColorInt());
    }

    /**
     * Draws a default-colored label string at integer coordinates.
     */
    public static int drawElementLabel(GuiGraphics graphics, Font font, String text, int x, int y) {
        return drawElementLabel(graphics, font, Component.literal(text), x, y, getUIColorTheme().element_label_color_normal.getColorInt());
    }

    /**
     * Draws a label component with the given base color.
     *
     * @return width of the rendered string
     */
    public static int drawElementLabel(GuiGraphics graphics, Font font, Component text, int x, int y, int baseColor) {
        return graphics.drawString(font, text, x, y, baseColor, FancyMenu.getOptions().enableUiTextShadow.getValue());
    }

    /**
     * Draws a label string with the given base color.
     *
     * @return width of the rendered string
     */
    public static int drawElementLabel(GuiGraphics graphics, Font font, String text, int x, int y, int baseColor) {
        return drawElementLabel(graphics, font, Component.literal(text), x, y, baseColor);
    }

}
