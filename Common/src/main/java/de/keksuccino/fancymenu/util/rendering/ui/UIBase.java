package de.keksuccino.fancymenu.util.rendering.ui;

import java.awt.Color;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UIColorThemeRegistry;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UIColorTheme;
import de.keksuccino.fancymenu.util.rendering.ui.popup.FMNotificationPopup;
import de.keksuccino.fancymenu.util.rendering.ui.widget.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.ExtendedEditBox;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

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
	public static <T extends GuiEventListener> T applyDefaultWidgetSkinTo(T widget) {
		if (widget instanceof ExtendedButton e) {
			return (T) applyDefaultButtonSkinTo(e);
		}
		if (widget instanceof ExtendedEditBox e) {
			return (T) applyDefaultEditBoxSkinTo(e);
		}
		return widget;
	}

	private static ExtendedEditBox applyDefaultEditBoxSkinTo(ExtendedEditBox editBox) {
		UIColorTheme theme = UIBase.getUIColorScheme();
		editBox.setTextColor(theme.edit_box_text_color_normal);
		editBox.setTextColorUneditable(theme.edit_box_text_color_uneditable);
		editBox.setBackgroundColor(theme.edit_box_background_color);
		editBox.setBorderNormalColor(theme.edit_box_border_color_normal);
		editBox.setBorderFocusedColor(theme.edit_box_border_color_focused);
		editBox.setTextShadow(false);
		return editBox;
	}

	private static ExtendedButton applyDefaultButtonSkinTo(ExtendedButton button) {
		button.setBackground(ExtendedButton.ColorButtonBackground.create(UIBase.getUIColorScheme().element_background_color_normal, UIBase.getUIColorScheme().element_background_color_hover, UIBase.getUIColorScheme().element_border_color_normal, UIBase.getUIColorScheme().element_border_color_hover, ELEMENT_BORDER_THICKNESS));
		button.setLabelBaseColorNormal(UIBase.getUIColorScheme().element_label_color_normal);
		button.setLabelBaseColorInactive(UIBase.getUIColorScheme().element_label_color_inactive);
		button.setLabelShadowEnabled(FancyMenu.getOptions().enableUiTextShadow.getValue());
		button.setForceDefaultTooltipStyle(true);
		return button;
	}

	public static float getUIScale() {
		float uiScale = FancyMenu.getOptions().uiScale.getValue();
		if (Minecraft.getInstance().isEnforceUnicode() && (uiScale > 2.0F)) {
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

	public static boolean isXYInArea(int targetX, int targetY, int x, int y, int width, int height) {
		return isXYInArea((double)targetX, targetY, x, y, width, height);
	}

	public static boolean isXYInArea(double targetX, double targetY, double x, double y, double width, double height) {
		return (targetX >= x) && (targetX < (x + width)) && (targetY >= y) && (targetY < (y + height));
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

	public static int drawElementLabel(PoseStack pose, Font font, Component text, int x, int y) {
		return drawElementLabel(pose, font, text, x, y, getUIColorScheme().element_label_color_normal.getColorInt());
	}

	public static int drawElementLabel(PoseStack pose, Font font, String text, int x, int y) {
		return drawElementLabel(pose, font, Component.literal(text), x, y, getUIColorScheme().element_label_color_normal.getColorInt());
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

	public static void resetShaderColor() {
		RenderingUtils.resetShaderColor();
	}

	//TODO remove this
	@Deprecated
	public static void displayNotification(String... notification) {
		PopupHandler.displayPopup(new FMNotificationPopup(300, new Color(0,0,0,0), 240, null, notification));
	}

	@NotNull
	public static UIColorTheme getUIColorScheme() {
		return UIColorThemeRegistry.getActiveTheme();
	}

}
