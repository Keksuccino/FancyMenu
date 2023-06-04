package de.keksuccino.fancymenu.rendering.ui;

import java.awt.Color;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.rendering.DrawableColor;
import de.keksuccino.fancymenu.rendering.ui.colorschemes.LightUIColorScheme;
import de.keksuccino.fancymenu.rendering.ui.colorschemes.UIColorScheme;
import de.keksuccino.fancymenu.rendering.ui.contextmenu.ContextMenu;
import de.keksuccino.fancymenu.rendering.ui.popup.FMNotificationPopup;
import de.keksuccino.fancymenu.rendering.ui.widget.ExtendedButton;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;

public class UIBase extends GuiComponent {

	protected static UIColorScheme uiColorSchemeDefault = new UIColorScheme();
	protected static UIColorScheme uiColorSchemeLight = new LightUIColorScheme();

	public static final int ELEMENT_BORDER_THICKNESS = 1;
	public static final int VERTICAL_SCROLL_BAR_WIDTH = 5;
	public static final int VERTICAL_SCROLL_BAR_HEIGHT = 40;
	public static final int HORIZONTAL_SCROLL_BAR_WIDTH = 40;
	public static final int HORIZONTAL_SCROLL_BAR_HEIGHT = 5;

	/**
	 * Applies the default skin to the input button and returns it.
	 *
	 * @return The input button.
	 */
	public static AdvancedButton applyDefaultButtonSkinTo(AdvancedButton button) {
		button.setBackgroundColor(uiColorSchemeDefault.elementBackgroundColorNormal.getColor(), uiColorSchemeDefault.elementBackgroundColorHover.getColor(), uiColorSchemeDefault.elementBorderColorNormal.getColor(), uiColorSchemeDefault.elementBorderColorHover.getColor(), ELEMENT_BORDER_THICKNESS);
		return button;
	}

	/**
	 * Applies the default skin to the input button and returns it.
	 *
	 * @return The input button.
	 */
	public static ExtendedButton applyDefaultButtonSkinTo(ExtendedButton button) {
		button.setBackground(ExtendedButton.ColorButtonBackground.create(UIBase.getUIColorScheme().elementBackgroundColorNormal, UIBase.getUIColorScheme().elementBackgroundColorHover, UIBase.getUIColorScheme().elementBorderColorNormal, UIBase.getUIColorScheme().elementBorderColorHover, ELEMENT_BORDER_THICKNESS));
		button.setLabelBaseColorNormal(UIBase.getUIColorScheme().elementLabelColorNormal);
		button.setLabelBaseColorInactive(UIBase.getUIColorScheme().elementLabelColorInactive);
		button.setLabelShadowEnabled(false);
		return button;
	}

	public static float getUiScale() {
		float uiScale = FancyMenu.getConfig().getOrDefault("uiscale", 1.0F);
		if (Minecraft.getInstance().isEnforceUnicode() && (uiScale > 2.0F)) {
			uiScale = 2.0F;
		}
		return uiScale;
	}

	public static float getFixedUiScale() {
		return calculateFixedScale(getUiScale());
	}

	public static float calculateFixedScale(float fixedScale) {
		double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
		return (float)(1.0D * (1.0D / guiScale) * fixedScale);
	}

	public static boolean isXYInArea(int targetX, int targetY, int x, int y, int width, int height) {
		return (targetX >= x) && (targetX < (x + width)) && (targetY >= y) && (targetY < (y + height));
	}

	public static void openScaledContextMenuAt(ContextMenu menu, int x, int y) {
		Screen s = Minecraft.getInstance().screen;
		if (s != null) {
			menu.openMenuAt((int) (x / UIBase.getFixedUiScale()), (int) (y / UIBase.getFixedUiScale()), (int) (s.width / getFixedUiScale()), (int) (s.height / getFixedUiScale()));
		}
	}

	public static void openScaledContextMenuAtMouse(ContextMenu menu) {
		openScaledContextMenuAt(menu, MouseInput.getMouseX(), MouseInput.getMouseY());
	}

	public static void renderScaledContextMenu(PoseStack matrix, ContextMenu menu) {
		Screen s = Minecraft.getInstance().screen;
		if ((s != null) && (menu != null)) {

			matrix.pushPose();

			matrix.scale(UIBase.getFixedUiScale(), UIBase.getFixedUiScale(), UIBase.getFixedUiScale());

			MouseInput.setRenderScale(UIBase.getFixedUiScale());
			int mouseX = MouseInput.getMouseX();
			int mouseY = MouseInput.getMouseY();
			MouseInput.resetRenderScale();

			menu.render(matrix, mouseX, mouseY, (int) (s.width / getFixedUiScale()), (int) (s.height / getFixedUiScale()));

			matrix.popPose();

		}
	}

	public static void renderListingDot(PoseStack matrix, int x, int y, Color color) {
		fill(matrix, x, y, x + 4, y + 4, color.getRGB());
	}

	public static void renderBorder(PoseStack matrix, int xMin, int yMin, int xMax, int yMax, int borderThickness, Color borderColor, boolean renderTop, boolean renderLeft, boolean renderRight, boolean renderBottom) {
		if (renderTop) {
			fill(matrix, xMin, yMin, xMax, yMin + borderThickness, borderColor.getRGB());
		}
		if (renderLeft) {
			fill(matrix, xMin, yMin + borderThickness, xMin + borderThickness, yMax - borderThickness, borderColor.getRGB());
		}
		if (renderRight) {
			fill(matrix, xMax - borderThickness, yMin + borderThickness, xMax, yMax - borderThickness, borderColor.getRGB());
		}
		if (renderBottom) {
			fill(matrix, xMin, yMax - borderThickness, xMax, yMax, borderColor.getRGB());
		}
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
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	public static void displayNotification(String... notification) {
		PopupHandler.displayPopup(new FMNotificationPopup(300, new Color(0,0,0,0), 240, null, notification));
	}

	@NotNull
	public static UIColorScheme getUIColorScheme() {
		return isLightMode() ? uiColorSchemeLight : uiColorSchemeDefault;
	}

	public static boolean isLightMode() {
		return FancyMenu.getConfig().getOrDefault("light_mode", false);
	}

}
