package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator;

import java.awt.Color;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.AdvancedTextField;
import de.keksuccino.konkrete.gui.screens.popup.Popup;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.KeyboardData;
import de.keksuccino.konkrete.input.KeyboardHandler;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

public class LayoutCreatorSettingsPopup extends Popup {

	protected LayoutCreatorScreen handler;
	
	protected AdvancedButton doneButton;
	
	protected AdvancedButton vanillaNotificationButton;
	protected AdvancedTextField popupMenuScaleTextfield;
	
	public LayoutCreatorSettingsPopup(LayoutCreatorScreen parent) {
		super(240);
		this.handler = parent;
		
		String vanillaNotifyLabel = Locals.localize("helper.creator.sidemenu.settings.on");
		if (!FancyMenu.config.getOrDefault("showvanillamovewarning", true)) {
			vanillaNotifyLabel = Locals.localize("helper.creator.sidemenu.settings.off");
		}
		this.vanillaNotificationButton = new AdvancedButton(0, 0, 100, 20, vanillaNotifyLabel, true, (press) -> {
			try {
				if (FancyMenu.config.getOrDefault("showvanillamovewarning", true)) {
					FancyMenu.config.setValue("showvanillamovewarning", false);
					press.setMessage(new LiteralText(Locals.localize("helper.creator.sidemenu.settings.off")));
				} else {
					FancyMenu.config.setValue("showvanillamovewarning", true);
					press.setMessage(new LiteralText(Locals.localize("helper.creator.sidemenu.settings.on")));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		LayoutCreatorScreen.colorizeCreatorButton(this.vanillaNotificationButton);
		this.addButton(this.vanillaNotificationButton);
		
		this.popupMenuScaleTextfield = new AdvancedTextField(MinecraftClient.getInstance().textRenderer, 0, 0, 98, 20, true, CharacterFilter.getDoubleCharacterFiler());
		this.popupMenuScaleTextfield.setText("" + FancyMenu.config.getOrDefault("popupmenuscale", 1.0F));
		
		this.doneButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("popup.done"), true, (press) -> {
			this.updateValues();
			this.handler.updateMenuScale();
			this.setDisplayed(false);
			this.handler.setMenusUseable(true);
		});
		this.addButton(this.doneButton);
		
		KeyboardHandler.addKeyPressedListener(this::onEnterPressed);
		KeyboardHandler.addKeyPressedListener(this::onEscapePressed);
		
	}

	@Override
	public void render(MatrixStack matrix, int mouseX, int mouseY, Screen renderIn) {
		super.render(matrix, mouseX, mouseY, renderIn);
		
		if (this.isDisplayed()) {
			
			TextRenderer font = MinecraftClient.getInstance().textRenderer;
			int midX = renderIn.width / 2;
			int midY = renderIn.height / 2;
			
			fill(matrix, midX - 1, midY - 100, midX + 1, midY + 60, new Color(255, 255, 255, 100).getRGB());
			
			String vanillaNotify = Locals.localize("helper.creator.sidemenu.settings.vanillanotification");
			int vanillaNotifyWidth = font.getWidth(vanillaNotify);
			drawStringWithShadow(matrix, font, vanillaNotify, midX - vanillaNotifyWidth - 20, midY - 75, Color.WHITE.getRGB());
			
			this.vanillaNotificationButton.x = midX + 20;
			this.vanillaNotificationButton.y = midY - 80;
			
			String popScale = Locals.localize("helper.creator.sidemenu.settings.popupmenuscale");
			int popScaleWidth = font.getWidth(popScale);
			drawStringWithShadow(matrix, font, popScale, midX - popScaleWidth - 20, midY - 50, Color.WHITE.getRGB());
			
			this.popupMenuScaleTextfield.x = midX + 20 + 1;
			this.popupMenuScaleTextfield.y = midY - 55;
			this.popupMenuScaleTextfield.render(matrix, mouseX, mouseY, MinecraftClient.getInstance().getTickDelta());
			
			this.doneButton.x = midX - (this.doneButton.getWidth() / 2);
			this.doneButton.y = midY + 85;
			
			this.renderButtons(matrix, mouseX, mouseY);
			
		}
	}
	
	protected void updateValues() {
		try {
			
			//Popup Menu Scale
			String popscale = this.popupMenuScaleTextfield.getText();
			if ((popscale != null) && MathUtils.isFloat(popscale)) {
				FancyMenu.config.setValue("popupmenuscale", Float.parseFloat(popscale));
			}
			
			//Reloading editor after updating values
			MinecraftClient.getInstance().openScreen(this.handler);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void onEnterPressed(KeyboardData d) {
		if ((d.keycode == 257) && this.isDisplayed()) {
			this.updateValues();
			this.handler.updateMenuScale();
			this.setDisplayed(false);
			this.handler.setMenusUseable(true);
		}
	}
	
	public void onEscapePressed(KeyboardData d) {
		if ((d.keycode == 256) && this.isDisplayed()) {
			this.setDisplayed(false);
			this.handler.setMenusUseable(true);
		}
	}
}
