package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button;

import java.awt.Color;

import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.TextInputPopup;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.KeyboardData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;

public class HideForPopup extends TextInputPopup {
	
	private AdvancedButton delayEverytimeBtn;
	private AdvancedButton delayOnlyFirstTimeBtn;
	private ILayoutButton button;
	private boolean onlyfirsttime = false;
	private int backgroundalpha = 0;

	public HideForPopup(String title, CharacterFilter filter, int backgroundAlpha, ILayoutButton button) {
		super(new Color(0, 0, 0, 0), title, filter, backgroundAlpha, null);
		this.backgroundalpha = backgroundAlpha;
		this.button = button;
		this.onlyfirsttime = button.isDelayedOnlyFirstTime();
		
		String eBtn = "§a" + Locals.localize("helper.creator.popup.hidefor.delayeverytime");
		if (this.onlyfirsttime) {
			eBtn = Locals.localize("helper.creator.popup.hidefor.delayeverytime");
		}
		this.delayEverytimeBtn = new AdvancedButton(0, 0, 100, 20, eBtn, true, (press) -> {
			this.onlyfirsttime = false;
			press.displayString = "§a" + Locals.localize("helper.creator.popup.hidefor.delayeverytime");
			this.delayOnlyFirstTimeBtn.displayString = Locals.localize("helper.creator.popup.hidefor.delayfirsttime");
		});
		this.addButton(this.delayEverytimeBtn);
		
		String fBtn = Locals.localize("helper.creator.popup.hidefor.delayfirsttime");
		if (this.onlyfirsttime) {
			fBtn = "§a" + Locals.localize("helper.creator.popup.hidefor.delayfirsttime");
		}
		this.delayOnlyFirstTimeBtn = new AdvancedButton(0, 0, 100, 20, fBtn, true, (press) -> {
			this.onlyfirsttime = true;
			press.displayString = "§a" + Locals.localize("helper.creator.popup.hidefor.delayfirsttime");
			this.delayEverytimeBtn.displayString = Locals.localize("helper.creator.popup.hidefor.delayeverytime");
		});
		this.addButton(this.delayOnlyFirstTimeBtn);
		
		this.doneButton.setPressAction((press) -> {
			this.onDoneButtonPressed();
		});
		
	}
	
	@Override
	public void render(int mouseX, int mouseY, GuiScreen renderIn) {
		if (!this.isDisplayed()) {
			return;
		}
		
		int height = 100;
		
		GlStateManager.enableBlend();
		drawRect(0, 0, renderIn.width, renderIn.height, new Color(0, 0, 0, this.backgroundalpha).getRGB());
		GlStateManager.disableBlend();
		
		renderIn.drawCenteredString(Minecraft.getMinecraft().fontRenderer, title, renderIn.width / 2, (renderIn.height / 2) - (height / 2) + 10, Color.WHITE.getRGB());
		
		this.textField.x = (renderIn.width / 2) - (this.textField.getWidth() / 2);
		this.textField.y = (renderIn.height / 2) - (this.textField.height / 2);
		this.textField.drawTextBox();

		this.delayEverytimeBtn.x = (renderIn.width / 2) - this.delayEverytimeBtn.width - 5;
		this.delayEverytimeBtn.y = ((renderIn.height  / 2) + 50) - this.delayEverytimeBtn.height - 5;
		
		this.delayOnlyFirstTimeBtn.x = (renderIn.width / 2) + 5;
		this.delayOnlyFirstTimeBtn.y = ((renderIn.height  / 2) + 50) - this.delayOnlyFirstTimeBtn.height - 5;
		
		this.doneButton.x = (renderIn.width / 2) - (this.doneButton.width / 2);
		this.doneButton.y = ((renderIn.height / 2) + 90) - this.doneButton.height - 5;
		
		this.renderButtons(mouseX, mouseY);
	}
	
	@Override
	public void onEnterPressed(KeyboardData d) {
		if ((d.keycode == 28) && this.isDisplayed()) {
			this.onDoneButtonPressed();
		}
	}
	
	protected void onDoneButtonPressed() {
		this.input = this.textField.getText();
		String sec = CharacterFilter.getDoubleCharacterFiler().filterForAllowedChars(this.input);
		this.button.setAppearanceDelay(sec, this.onlyfirsttime);
		this.setDisplayed(false);
	}

}
