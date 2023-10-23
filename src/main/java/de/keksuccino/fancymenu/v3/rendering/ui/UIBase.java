package de.keksuccino.fancymenu.v3.rendering.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.v3.rendering.DrawableColor;
import de.keksuccino.fancymenu.v3.rendering.RenderingUtils;
import de.keksuccino.fancymenu.v3.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.v3.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.fancymenu.v3.rendering.ui.widget.slider.ExtendedSliderButton;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.awt.*;

@SuppressWarnings("unused")
public class UIBase extends RenderingUtils {

	public static final int ELEMENT_BORDER_THICKNESS = 1;
	public static final int VERTICAL_SCROLL_BAR_WIDTH = 5;
	public static final int VERTICAL_SCROLL_BAR_HEIGHT = 40;
	public static final int HORIZONTAL_SCROLL_BAR_WIDTH = 40;
	public static final int HORIZONTAL_SCROLL_BAR_HEIGHT = 5;

	public static final DrawableColor area_background_color = DrawableColor.of(new Color(43, 43, 43));
	public static final DrawableColor scroll_grabber_color_normal = DrawableColor.of(new Color(89, 91, 93, 100));
	public static final DrawableColor scroll_grabber_color_hover = DrawableColor.of(new Color(102, 104, 104, 100));
	public static final DrawableColor list_entry_color_selected_hovered = DrawableColor.of(new Color(50, 50, 50));
	public static final DrawableColor element_border_color_normal = DrawableColor.of(new Color(93, 97, 100));
	public static final DrawableColor element_border_color_hover = DrawableColor.of(new Color(93, 97, 100));
	public static final DrawableColor element_background_color_normal = DrawableColor.of(new Color(71, 71, 71));
	public static final DrawableColor element_background_color_hover = DrawableColor.of(new Color(83, 156, 212));
	public static final DrawableColor edit_box_background_color = DrawableColor.of(new Color(43, 43, 43));
	public static final DrawableColor edit_box_border_color_normal = DrawableColor.of(new Color(209, 194, 209));
	public static final DrawableColor edit_box_border_color_focused = DrawableColor.of(new Color(227, 211, 227));
	public static final DrawableColor slider_handle_color_normal = DrawableColor.of(new Color(71, 132, 180));
	public static final DrawableColor slider_handle_color_hover = DrawableColor.of(new Color(83, 156, 212));
	public static final DrawableColor edit_box_text_color_normal = DrawableColor.of(new Color(206, 221, 237));
	public static final DrawableColor edit_box_text_color_uneditable = DrawableColor.of(new Color(113, 117, 119));
	public static final DrawableColor edit_box_suggestion_text_color = DrawableColor.of(new Color(-8355712));
	public static final DrawableColor element_label_color_normal = DrawableColor.of(new Color(206, 221, 237));
	public static final DrawableColor element_label_color_inactive = DrawableColor.of(new Color(113, 117, 119));
	public static final DrawableColor screen_background_color = DrawableColor.of(new Color(60, 63, 65));
	public static final DrawableColor screen_background_color_darker = DrawableColor.of(new Color(38, 38, 38));
	public static final DrawableColor generic_text_base_color = DrawableColor.of(new Color(255, 255, 255));

	/**
	 * Applies the default UI skin to the given widget and returns it.
	 */
	@SuppressWarnings("all")
	public static <T> T applyDefaultWidgetSkinTo(T widget) {
		if (widget instanceof ExtendedButton e) {
			return (T) applyDefaultButtonSkinTo(e);
		}
		if (widget instanceof ExtendedEditBox e) {
			return (T) applyDefaultEditBoxSkinTo(e);
		}
		if (widget instanceof ExtendedSliderButton s) {
			return (T) applyDefaultSliderSkinTo(s);
		}
		return widget;
	}

	private static ExtendedSliderButton applyDefaultSliderSkinTo(ExtendedSliderButton slider) {
		slider.setBackgroundColor(element_background_color_normal);
		slider.setBorderColor(element_border_color_normal);
		slider.setHandleColorNormal(slider_handle_color_normal);
		slider.setHandleColorHover(slider_handle_color_hover);
		slider.setLabelColorNormal(element_label_color_normal);
		slider.setLabelColorInactive(element_label_color_inactive);
		slider.setLabelShadow(false);
		return slider;
	}

	private static ExtendedEditBox applyDefaultEditBoxSkinTo(ExtendedEditBox editBox) {
		editBox.setTextColor(edit_box_text_color_normal);
		editBox.setTextColorUneditable(edit_box_text_color_uneditable);
		editBox.setBackgroundColor(edit_box_background_color);
		editBox.setBorderNormalColor(edit_box_border_color_normal);
		editBox.setBorderFocusedColor(edit_box_border_color_focused);
		editBox.setSuggestionTextColor(edit_box_suggestion_text_color);
		editBox.setTextShadow(false);
		return editBox;
	}

	private static ExtendedButton applyDefaultButtonSkinTo(ExtendedButton button) {
		button.setBackground(ExtendedButton.ColorButtonBackground.create(element_background_color_normal, element_background_color_hover, element_border_color_normal, element_border_color_hover, ELEMENT_BORDER_THICKNESS));
		button.setLabelBaseColorNormal(element_label_color_normal);
		button.setLabelBaseColorInactive(element_label_color_inactive);
		button.setLabelShadowEnabled(false);
		button.setForceDefaultTooltipStyle(true);
		return button;
	}

	public static void renderListingDot(GuiGraphics graphics, float x, float y, int color) {
		fillF(graphics.pose(), x, y, x + 4, y + 4, color);
	}

	public static void renderListingDot(GuiGraphics graphics, int x, int y, Color color) {
		graphics.fill(x, y, x + 4, y + 4, color.getRGB());
	}

	public static void renderBorder(PoseStack matrix, int xMin, int yMin, int xMax, int yMax, int borderThickness, DrawableColor borderColor, boolean renderTop, boolean renderLeft, boolean renderRight, boolean renderBottom) {
		renderBorder(matrix, xMin, yMin, xMax, yMax, borderThickness, borderColor.getColorInt(), renderTop, renderLeft, renderRight, renderBottom);
	}

	public static void renderBorder(PoseStack matrix, int xMin, int yMin, int xMax, int yMax, int borderThickness, Color borderColor, boolean renderTop, boolean renderLeft, boolean renderRight, boolean renderBottom) {
		renderBorder(matrix, xMin, yMin, xMax, yMax, borderThickness, borderColor.getRGB(), renderTop, renderLeft, renderRight, renderBottom);
	}

	public static void renderBorder(PoseStack pose, float xMin, float yMin, float xMax, float yMax, float borderThickness, int borderColor, boolean renderTop, boolean renderLeft, boolean renderRight, boolean renderBottom) {
		if (renderTop) {
			RenderingUtils.fillF(pose, xMin, yMin, xMax, yMin + borderThickness, borderColor);
		}
		if (renderLeft) {
			RenderingUtils.fillF(pose, xMin, yMin + borderThickness, xMin + borderThickness, yMax - borderThickness, borderColor);
		}
		if (renderRight) {
			RenderingUtils.fillF(pose, xMax - borderThickness, yMin + borderThickness, xMax, yMax - borderThickness, borderColor);
		}
		if (renderBottom) {
			RenderingUtils.fillF(pose, xMin, yMax - borderThickness, xMax, yMax, borderColor);
		}
	}

	public static int drawElementLabel(GuiGraphics graphics, Font font, Component text, int x, int y) {
		return drawElementLabel(graphics, font, text, x, y, element_label_color_normal.getColorInt());
	}

	public static int drawElementLabel(GuiGraphics graphics, Font font, String text, int x, int y) {
		return drawElementLabel(graphics, font, Component.literal(text), x, y, element_label_color_normal.getColorInt());
	}

	public static int drawElementLabel(GuiGraphics graphics, Font font, Component text, int x, int y, int baseColor) {
		return graphics.drawString(font, text, x, y, baseColor, false);
	}

	public static int drawElementLabel(GuiGraphics graphics, Font font, String text, int x, int y, int baseColor) {
		return drawElementLabel(graphics, font, Component.literal(text), x, y, baseColor);
	}

	public static void setShaderColor(DrawableColor color) {
		Color c = color.getColor();
		float a = Math.min(1F, Math.max(0F, (float)c.getAlpha() / 255.0F));
		setShaderColor(color, a);
	}

	public static void setShaderColor(DrawableColor color, float alpha) {
		Color c = color.getColor();
		float r = Math.min(1F, Math.max(0F, (float)c.getRed() / 255.0F));
		float g = Math.min(1F, Math.max(0F, (float)c.getGreen() / 255.0F));
		float b = Math.min(1F, Math.max(0F, (float)c.getBlue() / 255.0F));
		RenderSystem.setShaderColor(r, g, b, alpha);
	}

}
