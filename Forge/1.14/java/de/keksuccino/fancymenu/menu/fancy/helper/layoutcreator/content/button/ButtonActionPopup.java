package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button;

import java.awt.Color;
import java.util.function.Consumer;

import com.mojang.blaze3d.platform.GlStateManager;

import de.keksuccino.core.gui.content.AdvancedButton;
import de.keksuccino.core.gui.content.AdvancedTextField;
import de.keksuccino.core.gui.content.HorizontalSwitcher;
import de.keksuccino.core.gui.screens.popup.Popup;
import de.keksuccino.core.input.KeyboardData;
import de.keksuccino.core.input.KeyboardHandler;
import de.keksuccino.fancymenu.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;

public class ButtonActionPopup extends Popup {
	
	protected Consumer<String> contentCallback;
	protected Consumer<String> typeCallback;
	protected AdvancedTextField textField;
	protected AdvancedButton doneButton;
	protected int width = 250;
	
	protected HorizontalSwitcher actionSwitcher;
	
	public ButtonActionPopup(Consumer<String> contentCallback, Consumer<String> typeCallback, String selectedType) {
		super(240);
		
		this.textField = new AdvancedTextField(Minecraft.getInstance().fontRenderer, 0, 0, 200, 20, true, null);
		this.textField.setCanLoseFocus(true);
		this.textField.setFocused2(false);
		this.textField.setMaxStringLength(1000);
		
		this.actionSwitcher = new HorizontalSwitcher(120,
				"openlink",
				"sendmessage",
				"quitgame",
				"joinserver",
				"loadworld",
				"openfile",
				"prevbackground",
				"nextbackground");
		this.actionSwitcher.setButtonColor(new Color(102, 102, 153), new Color(133, 133, 173), new Color(163, 163, 194), new Color(163, 163, 194), 1);
		this.actionSwitcher.setValueBackgroundColor(new Color(102, 102, 153));
		
		if (selectedType != null) {
			this.actionSwitcher.setSelectedValue(selectedType);
		}
		
		this.doneButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("popup.done"), true, (press) -> {
			this.setDisplayed(false);
			if (this.contentCallback != null) {
				this.contentCallback.accept(this.textField.getText());
			}
			if (this.typeCallback != null) {
				this.typeCallback.accept(this.actionSwitcher.getSelectedValue());
			}
		});
		this.addButton(this.doneButton);

		this.contentCallback = contentCallback;
		this.typeCallback = typeCallback;
		
		KeyboardHandler.addKeyPressedListener(this::onEnterPressed);
		KeyboardHandler.addKeyPressedListener(this::onEscapePressed);
	}

	@Override
	public void render(int mouseX, int mouseY, Screen renderIn) {
		super.render(mouseX, mouseY, renderIn);
		
		if (this.isDisplayed()) {
			int height = 100;
			
			GlStateManager.enableBlend();
			Screen.fill((renderIn.width / 2) - (this.width / 2), (renderIn.height / 2) - (height / 2), (renderIn.width / 2) + (this.width / 2), (renderIn.height / 2) + (height / 2), new Color(0, 0, 0, 0).getRGB());
			GlStateManager.disableBlend();
			
			renderIn.drawCenteredString(Minecraft.getInstance().fontRenderer, "§l" + Locals.localize("helper.creator.custombutton.config"), renderIn.width / 2, (renderIn.height / 2) - (height / 2) - 40, Color.WHITE.getRGB());
			
			
			renderIn.drawCenteredString(Minecraft.getInstance().fontRenderer, Locals.localize("helper.creator.custombutton.config.actiontype"), renderIn.width / 2, (renderIn.height / 2) - 60, Color.WHITE.getRGB());
			
			this.actionSwitcher.render((renderIn.width / 2) - (this.actionSwitcher.getTotalWidth() / 2), (renderIn.height / 2) - 45);
			
			renderIn.drawCenteredString(Minecraft.getInstance().fontRenderer, Locals.localize("helper.creator.custombutton.config.actiontype." + this.actionSwitcher.getSelectedValue() + ".desc"), renderIn.width / 2, (renderIn.height / 2) - 20, Color.WHITE.getRGB());
			
			
			String s = this.actionSwitcher.getSelectedValue();
			if (s.equals("sendmessage") || s.equals("openlink") || (s.equals("joinserver") || (s.equals("loadworld") || s.equals("openfile")))) {
				renderIn.drawCenteredString(Minecraft.getInstance().fontRenderer, Locals.localize("helper.creator.custombutton.config.actionvalue", Locals.localize("helper.creator.custombutton.config.actiontype." + this.actionSwitcher.getSelectedValue() + ".desc.value")), renderIn.width / 2, (renderIn.height / 2) + 15, Color.WHITE.getRGB());
				
				this.textField.x = (renderIn.width / 2) - (this.textField.getWidth() / 2);
				this.textField.y = (renderIn.height / 2) + 30;
				this.textField.renderButton(mouseX, mouseY, Minecraft.getInstance().getRenderPartialTicks());
			}
			
			
			this.doneButton.x = (renderIn.width / 2) - (this.doneButton.getWidth() / 2);
			this.doneButton.y = ((renderIn.height / 2) + (height / 2)) + 20;
			
			this.renderButtons(mouseX, mouseY);
		}
	}
	
	public void setText(String text) {
		this.textField.setText("");
		this.textField.writeText(text);
	}
	
	public String getInput() {
		return this.textField.getText();
	}
	
	public void onEnterPressed(KeyboardData d) {
		if ((d.keycode == 257) && this.isDisplayed()) {
			this.setDisplayed(false);
			if (this.contentCallback != null) {
				this.contentCallback.accept(this.textField.getText());
			}
			if (this.typeCallback != null) {
				this.typeCallback.accept(this.actionSwitcher.getSelectedValue());
			}
		}
	}
	
	public void onEscapePressed(KeyboardData d) {
		if ((d.keycode == 256) && this.isDisplayed()) {
			this.setDisplayed(false);
			if (this.contentCallback != null) {
				this.contentCallback.accept(null);
			}
			if (this.typeCallback != null) {
				this.typeCallback.accept(null);
			}
		}
	}

}
