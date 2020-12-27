package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button;

import java.awt.Color;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.TextInputPopup;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.KeyboardData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

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
			press.setMessage(new StringTextComponent("§a" + Locals.localize("helper.creator.popup.hidefor.delayeverytime")));
			this.delayOnlyFirstTimeBtn.setMessage(Locals.localize("helper.creator.popup.hidefor.delayfirsttime"));
		});
		this.addButton(this.delayEverytimeBtn);
		
		String fBtn = Locals.localize("helper.creator.popup.hidefor.delayfirsttime");
		if (this.onlyfirsttime) {
			fBtn = "§a" + Locals.localize("helper.creator.popup.hidefor.delayfirsttime");
		}
		this.delayOnlyFirstTimeBtn = new AdvancedButton(0, 0, 100, 20, fBtn, true, (press) -> {
			this.onlyfirsttime = true;
			press.setMessage(new StringTextComponent("§a" + Locals.localize("helper.creator.popup.hidefor.delayfirsttime")));
			this.delayEverytimeBtn.setMessage(Locals.localize("helper.creator.popup.hidefor.delayeverytime"));
		});
		this.addButton(this.delayOnlyFirstTimeBtn);
		
		this.doneButton.setPressAction((press) -> {
			this.onDoneButtonPressed();
		});
		
	}
	
	@Override
	public void render(MatrixStack matrix, int mouseX, int mouseY, Screen renderIn) {
		if (!this.isDisplayed()) {
			return;
		}
		
		int height = 100;
		
		RenderSystem.enableBlend();
		fill(matrix, 0, 0, renderIn.width, renderIn.height, new Color(0, 0, 0, this.backgroundalpha).getRGB());
		RenderSystem.disableBlend();
		
		AbstractGui.drawCenteredString(matrix, Minecraft.getInstance().fontRenderer, new StringTextComponent(title), renderIn.width / 2, (renderIn.height / 2) - (height / 2) + 10, Color.WHITE.getRGB());
		
		this.textField.setX((renderIn.width / 2) - (this.textField.getWidth() / 2));
		this.textField.setY((renderIn.height / 2) - (this.textField.getHeightRealms() / 2));
		this.textField.renderButton(matrix, mouseX, mouseY, Minecraft.getInstance().getRenderPartialTicks());

		this.delayEverytimeBtn.setX((renderIn.width / 2) - this.delayEverytimeBtn.getWidth() - 5);
		this.delayEverytimeBtn.setY(((renderIn.height  / 2) + 50) - this.delayEverytimeBtn.getHeightRealms() - 5);
		
		this.delayOnlyFirstTimeBtn.setX((renderIn.width / 2) + 5);
		this.delayOnlyFirstTimeBtn.setY(((renderIn.height  / 2) + 50) - this.delayOnlyFirstTimeBtn.getHeightRealms() - 5);
		
		this.doneButton.setX((renderIn.width / 2) - (this.doneButton.getWidth() / 2));
		this.doneButton.setY(((renderIn.height / 2) + 90) - this.doneButton.getHeightRealms() - 5);
		
		this.renderButtons(matrix, mouseX, mouseY);
	}
	
	@Override
	public void onEnterPressed(KeyboardData d) {
		if ((d.keycode == 257) && this.isDisplayed()) {
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
