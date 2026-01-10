package de.keksuccino.fancymenu.util.rendering.ui;

import java.awt.Color;
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
		if (widget instanceof AbstractExtendedSlider s) {
			return (T) applyDefaultV2SliderSkinTo(s);
		}
		return widget;
	}

	private static AbstractExtendedSlider applyDefaultV2SliderSkinTo(AbstractExtendedSlider slider) {
		slider.setSliderBackgroundColorNormal(UIBase.getUIColorTheme().element_background_color_normal);
		slider.setSliderBorderColorNormal(UIBase.getUIColorTheme().element_border_color_normal);
		slider.setSliderHandleColorNormal(UIBase.getUIColorTheme().slider_handle_color_normal);
		slider.setSliderHandleColorHover(UIBase.getUIColorTheme().slider_handle_color_hover);
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
		editBox.setTextShadow_FancyMenu(false);
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
		//Handle "Auto" scale (set scale to 2 if window bigger than 3000x1700 to show 1080p and 2K screens on scale 1 and 4K on scale 2)
		if (uiScale == 4) {
			uiScale = 1;
			if ((Minecraft.getInstance().getWindow().getWidth() > 3000) || (Minecraft.getInstance().getWindow().getHeight() > 1700)) {
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

    public static boolean shouldBlur() {
        return FancyMenu.getOptions().enableUiBlur.getValue();
    }

	public static void renderListingDot(GuiGraphics graphics, float x, float y, int color) {
		fillF(graphics, x, y, x + 4, y + 4, color);
	}

	public static void renderListingDot(GuiGraphics graphics, int x, int y, Color color) {
		graphics.fill(x, y, x + 4, y + 4, color.getRGB());
	}

	public static void renderBorder(GuiGraphics graphics, int xMin, int yMin, int xMax, int yMax, int borderThickness, DrawableColor borderColor, boolean renderTop, boolean renderLeft, boolean renderRight, boolean renderBottom) {
		renderBorder(graphics, xMin, yMin, xMax, yMax, borderThickness, borderColor.getColorInt(), renderTop, renderLeft, renderRight, renderBottom);
	}

	public static void renderBorder(GuiGraphics graphics, int xMin, int yMin, int xMax, int yMax, int borderThickness, Color borderColor, boolean renderTop, boolean renderLeft, boolean renderRight, boolean renderBottom) {
		renderBorder(graphics, xMin, yMin, xMax, yMax, borderThickness, borderColor.getRGB(), renderTop, renderLeft, renderRight, renderBottom);
	}

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

	public static int drawElementLabel(GuiGraphics graphics, Font font, Component text, int x, int y) {
		return drawElementLabel(graphics, font, text, x, y, getUIColorTheme().element_label_color_normal.getColorInt());
	}

	public static int drawElementLabel(GuiGraphics graphics, Font font, String text, int x, int y) {
		return drawElementLabel(graphics, font, Component.literal(text), x, y, getUIColorTheme().element_label_color_normal.getColorInt());
	}

	public static int drawElementLabel(GuiGraphics graphics, Font font, Component text, int x, int y, int baseColor) {
		return graphics.drawString(font, text, x, y, baseColor, FancyMenu.getOptions().enableUiTextShadow.getValue());
	}

	public static int drawElementLabel(GuiGraphics graphics, Font font, String text, int x, int y, int baseColor) {
		return drawElementLabel(graphics, font, Component.literal(text), x, y, baseColor);
	}

	@NotNull
	public static UIColorTheme getUIColorTheme() {
		return UIColorThemeRegistry.getActiveTheme();
	}

}
