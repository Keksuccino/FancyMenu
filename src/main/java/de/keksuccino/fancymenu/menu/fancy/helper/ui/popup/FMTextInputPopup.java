package de.keksuccino.fancymenu.menu.fancy.helper.ui.popup;

import java.awt.Color;
import java.util.function.Consumer;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.UIBase;
import de.keksuccino.fancymenu.mixin.client.IMixinScreen;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.TextInputPopup;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class FMTextInputPopup extends TextInputPopup {

	protected int backgroundAlpha;

	public FMTextInputPopup(Color color, String title, CharacterFilter filter, int backgroundAlpha, Consumer<String> callback) {
		super(color, title, filter, backgroundAlpha, callback);
		this.backgroundAlpha = backgroundAlpha;
	}
	
	public FMTextInputPopup(Color color, String title, CharacterFilter filter, int backgroundAlpha) {
		super(color, title, filter, backgroundAlpha);
		this.backgroundAlpha = backgroundAlpha;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, Screen renderIn) {

		MouseInput.unblockVanillaInput("popupgui");

		if (this.isDisplayed()) {

			RenderSystem.enableBlend();
			graphics.fill(0, 0, renderIn.width, renderIn.height, (new Color(0, 0, 0, this.backgroundAlpha)).getRGB());

			int height = 100;

			RenderSystem.enableBlend();
			graphics.fill(renderIn.width / 2 - this.width / 2, renderIn.height / 2 - height / 2, renderIn.width / 2 + this.width / 2, renderIn.height / 2 + height / 2, this.color.getRGB());

			RenderSystem.enableBlend();
			graphics.drawCenteredString(Minecraft.getInstance().font, Component.literal(this.title), renderIn.width / 2, renderIn.height / 2 - height / 2 + 10, Color.WHITE.getRGB());

			this.textField.setX(renderIn.width / 2 - this.textField.getWidth() / 2);
			this.textField.setY(renderIn.height / 2 - this.textField.getHeight() / 2);
			this.textField.render(graphics, mouseX, mouseY, Minecraft.getInstance().getFrameTime());

			this.doneButton.setX(renderIn.width / 2 - this.doneButton.getWidth() / 2);
			this.doneButton.setY(renderIn.height / 2 + height / 2 - this.doneButton.getHeight() - 5);
			this.renderButtons(graphics, mouseX, mouseY);

		}

	}

	@Override
	public void setDisplayed(boolean b) {
		if (!b && (Minecraft.getInstance().screen != null)) {
			((IMixinScreen)Minecraft.getInstance().screen).getChildrenFancyMenu().remove(this.textField);
		}
		super.setDisplayed(b);
	}

	@Override
	protected void colorizePopupButton(AdvancedButton b) {
		UIBase.colorizeButton(b);
	}

}
