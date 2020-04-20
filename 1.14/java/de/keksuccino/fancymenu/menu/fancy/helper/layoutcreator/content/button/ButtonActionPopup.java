package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button;

import java.awt.Color;
import java.util.function.Consumer;

import com.mojang.blaze3d.platform.GlStateManager;

import de.keksuccino.core.gui.content.AdvancedButton;
import de.keksuccino.core.gui.content.AdvancedTextField;
import de.keksuccino.core.gui.screens.popup.Popup;
import de.keksuccino.core.input.KeyboardData;
import de.keksuccino.core.input.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;

public class ButtonActionPopup extends Popup {
	
	protected Consumer<String> contentCallback;
	protected Consumer<Integer> typeCallback;
	protected String input = null;
	protected int type = 0;
	protected AdvancedTextField textField;
	protected AdvancedButton doneButton;
	protected AdvancedButton messageButton;
	protected AdvancedButton linkButton;
	protected int width = 250;
	
	public ButtonActionPopup(Consumer<String> contentCallback, Consumer<Integer> typeCallback, int type) {
		super(240);
		
		this.textField = new AdvancedTextField(Minecraft.getInstance().fontRenderer, 0, 0, 200, 20, true, null);
		this.textField.setCanLoseFocus(true);
		this.textField.setFocused2(false);
		this.textField.setMaxStringLength(1000);
		
		this.doneButton = new AdvancedButton(0, 0, 100, 20, "Done", true, (press) -> {
			this.input = this.textField.getText();
			this.setDisplayed(false);
			if (this.contentCallback != null) {
				this.contentCallback.accept(this.input);
			}
			if (this.typeCallback != null) {
				this.typeCallback.accept(this.type);
			}
		});
		this.addButton(this.doneButton);
		
		String m = "§aSend Message";
		String l = "Open Weblink";
		if (type == 1) {
			m = "Send Message";
			l = "§aOpen Weblink";
		}
		this.messageButton = new AdvancedButton(0, 0, 100, 20, m, true, (press) -> {
			this.type = 0;
			press.setMessage("§aSend Message");;
			this.linkButton.setMessage("Open Weblink");;
		});
		this.addButton(this.messageButton);
		
		this.linkButton = new AdvancedButton(0, 0, 100, 20, l, true, (press) -> {
			this.type = 1;
			press.setMessage("§aOpen Weblink");;
			this.messageButton.setMessage("Send Message");;
		});
		this.addButton(this.linkButton);

		this.contentCallback = contentCallback;
		this.typeCallback = typeCallback;
		this.type = type;
		
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
			
			renderIn.drawCenteredString(Minecraft.getInstance().fontRenderer, "§lButton Configuration", renderIn.width / 2, (renderIn.height / 2) - (height / 2) - 40, Color.WHITE.getRGB());
			
			renderIn.drawCenteredString(Minecraft.getInstance().fontRenderer, "Action Value [Link or Message/Command]:", renderIn.width / 2, (renderIn.height / 2) - 45, Color.WHITE.getRGB());
			
			this.textField.x = (renderIn.width / 2) - (this.textField.getWidth() / 2);
			this.textField.y = (renderIn.height / 2) - 25;
			this.textField.renderButton(mouseX, mouseY, Minecraft.getInstance().getRenderPartialTicks());
			
			renderIn.drawCenteredString(Minecraft.getInstance().fontRenderer, "Action Type:", renderIn.width / 2, ((renderIn.height / 2) + (height / 2)) - 45, Color.WHITE.getRGB());
			
			this.messageButton.x = (renderIn.width / 2) - (this.messageButton.getWidth() + 5);
			this.messageButton.y = ((renderIn.height / 2) + (height / 2)) - this.messageButton.getHeight() - 5;
			
			this.linkButton.x = (renderIn.width / 2) + 5;
			this.linkButton.y = ((renderIn.height / 2) + (height / 2)) - this.linkButton.getHeight() - 5;
			
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
		return this.input;
	}
	
	public void onEnterPressed(KeyboardData d) {
		if ((d.keycode == 257) && this.isDisplayed()) {
			this.input = this.textField.getText();
			this.setDisplayed(false);
			if (this.contentCallback != null) {
				this.contentCallback.accept(this.input);
			}
			if (this.typeCallback != null) {
				this.typeCallback.accept(this.type);
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
				this.typeCallback.accept(-1);
			}
		}
	}

}
