package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import de.keksuccino.core.gui.content.AdvancedButton;
import de.keksuccino.core.gui.screens.popup.Popup;
import de.keksuccino.core.input.KeyboardData;
import de.keksuccino.core.input.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

public class LayoutSavePopup extends Popup {
	
	private List<String> text;
	private AdvancedButton saveAndDisableOthersButton;
	private AdvancedButton saveButton;
	private AdvancedButton cancelButton;
	private Consumer<Integer> callback;
	
	public LayoutSavePopup(Consumer<Integer> callback) {
		super(240);

		this.callback = callback;
		
		this.setNotificationText("§c§lAre you sure?", "", "Do you want to save your layout to a customization file now?");
		
		this.saveButton = new AdvancedButton(0, 0, 230, 20, "Save and keep other layouts enabled", true, (press) -> {
			this.setDisplayed(false);
			if (this.callback != null) {
				this.callback.accept(1);
			}
		});
		this.addButton(this.saveButton);
		
		this.saveAndDisableOthersButton = new AdvancedButton(0, 0, 200, 20, "Save and disable other layouts", true, (press) -> {
			this.setDisplayed(false);
			if (this.callback != null) {
				this.callback.accept(2);
			}
		});
		this.addButton(this.saveAndDisableOthersButton);
		
		this.cancelButton = new AdvancedButton(0, 0, 80, 20, "Cancel", true, (press) -> {
			this.setDisplayed(false);
			if (this.callback != null) {
				this.callback.accept(3);
			}
		});
		this.addButton(this.cancelButton);
		
		KeyboardHandler.addKeyPressedListener(this::onEscapePressed);
	}
	
	@Override
	public void render(int mouseX, int mouseY, GuiScreen renderIn) {
		super.render(mouseX, mouseY, renderIn);
		
		if (this.isDisplayed()) {
			int height = 50;
			
			for (int i = 0; i < this.text.size(); i++) {
				height += 10;
			}

			int i = 0;
			for (String s : this.text) {
				renderIn.drawCenteredString(Minecraft.getInstance().fontRenderer, s, renderIn.width / 2, (renderIn.height / 2) - (height / 2) + 10 + i, Color.WHITE.getRGB());
				i += 10;
			}
			
			this.saveButton.x = (renderIn.width / 2) - (this.saveButton.width / 2);
			this.saveButton.y = ((renderIn.height / 2) + (height / 2));
			
			this.saveAndDisableOthersButton.x = (renderIn.width / 2) - (this.saveAndDisableOthersButton.width / 2);
			this.saveAndDisableOthersButton.y = ((renderIn.height / 2) + (height / 2) + 25);
			
			this.cancelButton.x = (renderIn.width / 2) - (this.cancelButton.width / 2);
			this.cancelButton.y = ((renderIn.height / 2) + (height / 2) + 50);
			
			this.renderButtons(mouseX, mouseY);
		}
	}
	
	public void setNotificationText(String... text) {
		if (text != null) {
			this.text = Arrays.asList(text);
		}
	}
	
	public void onEscapePressed(KeyboardData d) {
		if ((d.keycode == 256) && this.isDisplayed()) {
			this.setDisplayed(false);
			if (this.callback != null) {
				this.callback.accept(3);
			}
		}
	}
}
