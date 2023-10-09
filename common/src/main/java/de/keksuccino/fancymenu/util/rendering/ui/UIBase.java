package de.keksuccino.fancymenu.util.rendering.ui;

import java.awt.Color;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UIColorThemeRegistry;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UIColorTheme;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.EditBoxSuggestions;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v1.ExtendedSliderButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class UIBase extends RenderingUtils {

	public static final int ELEMENT_BORDER_THICKNESS = 1;
	public static final int VERTICAL_SCROLL_BAR_WIDTH = 5;
	public static final int VERTICAL_SCROLL_BAR_HEIGHT = 40;
	public static final int HORIZONTAL_SCROLL_BAR_WIDTH = 40;
	public static final int HORIZONTAL_SCROLL_BAR_HEIGHT = 5;

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
		if (widget instanceof EditBoxSuggestions s) {
			return (T) applyDefaultEditBoxSuggestionsSkinTo(s);
		}
		if (widget instanceof ExtendedSliderButton s) {
			return (T) applyDefaultSliderSkinTo(s);
		}
		return widget;
	}

	private static ExtendedSliderButton applyDefaultSliderSkinTo(ExtendedSliderButton slider) {
		slider.setBackgroundColor(UIBase.getUIColorTheme().element_background_color_normal);
		slider.setBorderColor(UIBase.getUIColorTheme().element_border_color_normal);
		slider.setHandleColorNormal(UIBase.getUIColorTheme().slider_handle_color_normal);
		slider.setHandleColorHover(UIBase.getUIColorTheme().slider_handle_color_hover);
		slider.setLabelColorNormal(UIBase.getUIColorTheme().element_label_color_normal);
		slider.setLabelColorInactive(UIBase.getUIColorTheme().element_label_color_inactive);
		slider.setLabelShadow(FancyMenu.getOptions().enableUiTextShadow.getValue());
		return slider;
	}

	private static EditBoxSuggestions applyDefaultEditBoxSuggestionsSkinTo(EditBoxSuggestions editBoxSuggestions) {
		editBoxSuggestions.setBackgroundColor(UIBase.getUIColorTheme().suggestions_background_color);
		editBoxSuggestions.setNormalTextColor(UIBase.getUIColorTheme().suggestions_text_color_normal);
		editBoxSuggestions.setSelectedTextColor(UIBase.getUIColorTheme().suggestions_text_color_selected);
		editBoxSuggestions.setTextShadow(FancyMenu.getOptions().enableUiTextShadow.getValue());
		return editBoxSuggestions;
	}

	private static ExtendedEditBox applyDefaultEditBoxSkinTo(ExtendedEditBox editBox) {
		UIColorTheme theme = UIBase.getUIColorTheme();
		editBox.setTextColor(theme.edit_box_text_color_normal);
		editBox.setTextColorUneditable(theme.edit_box_text_color_uneditable);
		editBox.setBackgroundColor(theme.edit_box_background_color);
		editBox.setBorderNormalColor(theme.edit_box_border_color_normal);
		editBox.setBorderFocusedColor(theme.edit_box_border_color_focused);
		editBox.setSuggestionTextColor(theme.edit_box_suggestion_text_color);
		editBox.setTextShadow(false);
		return editBox;
	}

	private static ExtendedButton applyDefaultButtonSkinTo(ExtendedButton button) {
		button.setBackgroundColorNormal(UIBase.getUIColorTheme().element_background_color_normal);
		button.setBackgroundColorHover(UIBase.getUIColorTheme().element_background_color_hover);
		button.setBackgroundColorInactive(UIBase.getUIColorTheme().element_background_color_normal);
		button.setBorderColorNormal(UIBase.getUIColorTheme().element_border_color_normal);
		button.setBorderColorHover(UIBase.getUIColorTheme().element_border_color_hover);
		button.setBorderColorInactive(UIBase.getUIColorTheme().element_border_color_normal);
		button.setLabelBaseColorNormal(UIBase.getUIColorTheme().element_label_color_normal);
		button.setLabelBaseColorInactive(UIBase.getUIColorTheme().element_label_color_inactive);
		button.setLabelShadowEnabled(FancyMenu.getOptions().enableUiTextShadow.getValue());
		button.setForceDefaultTooltipStyle(true);
		return button;
	}

	public static float getUIScale() {
		float uiScale = FancyMenu.getOptions().uiScale.getValue();
		//Handle "Auto" scale (set scale to 2 if window bigger than 1920x1080)
		if (uiScale == 4) {
			uiScale = 1;
			if ((Minecraft.getInstance().getWindow().getWidth() > 1920) || (Minecraft.getInstance().getWindow().getHeight() > 1080)) {
				uiScale = 2;
			}
		}
		//Force a scale of 2 or bigger if Unicode font is enabled
		if (Minecraft.getInstance().isEnforceUnicode() && (uiScale < 2.0F)) {
			uiScale = 2.0F;
		}
		return uiScale;
	}

	public static float getFixedUIScale() {
		return calculateFixedScale(getUIScale());
	}

	public static float calculateFixedScale(float fixedScale) {
		double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
		return (float)(1.0D * (1.0D / guiScale) * fixedScale);
	}

	public static void renderListingDot(PoseStack matrix, float x, float y, int color) {
		fillF(matrix, x, y, x + 4, y + 4, color);
	}

	public static void renderListingDot(PoseStack matrix, int x, int y, Color color) {
		fill(matrix, x, y, x + 4, y + 4, color.getRGB());
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

	public static int drawElementLabelF(PoseStack pose, Font font, String text, float x, float y) {
		return drawElementLabelF(pose, font, Component.literal(text), x, y);
	}

	public static int drawElementLabelF(PoseStack pose, Font font, Component text, float x, float y) {
		if (!FancyMenu.getOptions().enableUiTextShadow.getValue()) {
			return font.draw(pose, text, x, y, getUIColorTheme().element_label_color_normal.getColorInt());
		}
		return font.drawShadow(pose, text, x, y, getUIColorTheme().element_label_color_normal.getColorInt());
	}

	public static int drawElementLabel(PoseStack pose, Font font, Component text, int x, int y) {
		return drawElementLabel(pose, font, text, x, y, getUIColorTheme().element_label_color_normal.getColorInt());
	}

	public static int drawElementLabel(PoseStack pose, Font font, String text, int x, int y) {
		return drawElementLabel(pose, font, Component.literal(text), x, y, getUIColorTheme().element_label_color_normal.getColorInt());
	}

	public static int drawElementLabel(PoseStack pose, Font font, Component text, int x, int y, int baseColor) {
		return FancyMenu.getOptions().enableUiTextShadow.getValue() ? font.drawShadow(pose, text, x, y, baseColor) : font.draw(pose, text, x, y, baseColor);
	}

	public static int drawElementLabel(PoseStack pose, Font font, String text, int x, int y, int baseColor) {
		return drawElementLabel(pose, font, Component.literal(text), x, y, baseColor);
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

	@NotNull
	public static UIColorTheme getUIColorTheme() {
		return UIColorThemeRegistry.getActiveTheme();
	}

}
