package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator;

import java.awt.Color;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMPopup;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.AdvancedTextField;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.KeyboardData;
import de.keksuccino.konkrete.input.KeyboardHandler;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;

public class WindowSizePopup extends FMPopup {

	private ActionType type;
	private LayoutEditorScreen parent;
	
	private AdvancedButton cancelButton;
	private AdvancedButton doneButton;
	
	private AdvancedTextField widthText;
	private AdvancedTextField heightText;
	
	public WindowSizePopup(LayoutEditorScreen parent, ActionType type) {
		super(240);
		
		this.type = type;
		this.parent = parent;
		
		this.cancelButton = new AdvancedButton(0, 0, 80, 20, Locals.localize("popup.yesno.cancel"), true, (press) -> {
			this.setDisplayed(false);
		});
		this.addButton(cancelButton);
		
		this.doneButton = new AdvancedButton(0, 0, 80, 20, Locals.localize("popup.done"), true, (press) -> {
			this.onDoneButtonPressed();
		});
		this.addButton(doneButton);
		
		this.widthText = new AdvancedTextField(Minecraft.getMinecraft().fontRenderer, 0, 0, 200, 20, true, CharacterFilter.getIntegerCharacterFiler());
		if (type == ActionType.BIGGERTHAN) {
			this.widthText.setText("" + parent.biggerThanWidth);
		} else {
			this.widthText.setText("" + parent.smallerThanWidth);
		}
		
		this.heightText = new AdvancedTextField(Minecraft.getMinecraft().fontRenderer, 0, 0, 200, 20, true, CharacterFilter.getIntegerCharacterFiler());
		if (type == ActionType.BIGGERTHAN) {
			this.heightText.setText("" + parent.biggerThanHeight);
		} else {
			this.heightText.setText("" + parent.smallerThanHeight);
		}
		
		KeyboardHandler.addKeyPressedListener(this::onEnterPressed);
		KeyboardHandler.addKeyPressedListener(this::onEscapePressed);
	}
	
	@Override
	public void render(int mouseX, int mouseY, GuiScreen renderIn) {
		super.render(mouseX, mouseY, renderIn);
		
		FontRenderer font = Minecraft.getMinecraft().fontRenderer;	
		
		if (this.type == ActionType.BIGGERTHAN) {
			renderIn.drawCenteredString(font, "§l" + Locals.localize("helper.creator.windowsize.biggerthan.desc"), renderIn.width / 2, (renderIn.height / 2) - 110, Color.WHITE.getRGB());
		} else {
			renderIn.drawCenteredString(font, "§l" + Locals.localize("helper.creator.windowsize.smallerthan.desc"), renderIn.width / 2, (renderIn.height / 2) - 110, Color.WHITE.getRGB());
		}
		
		renderIn.drawCenteredString(font, Locals.localize("general.width"), renderIn.width / 2, (renderIn.height / 2) - 80, Color.WHITE.getRGB());
		this.widthText.x = (renderIn.width / 2) - (this.widthText.getWidth() / 2);
		this.widthText.y = (renderIn.height / 2) - 65;
		this.widthText.drawTextBox();
		
		renderIn.drawCenteredString(font, Locals.localize("general.height"), renderIn.width / 2, (renderIn.height / 2) - 37, Color.WHITE.getRGB());
		this.heightText.x = (renderIn.width / 2) - (this.heightText.getWidth() / 2);
		this.heightText.y = (renderIn.height / 2) - 22;
		this.heightText.drawTextBox();

		renderIn.drawCenteredString(font, Locals.localize("helper.creator.windowsize.currentwidth") + ": " + Minecraft.getMinecraft().displayWidth, renderIn.width / 2, (renderIn.height / 2) + 15, Color.WHITE.getRGB());
		renderIn.drawCenteredString(font, Locals.localize("helper.creator.windowsize.currentheight") + ": " + Minecraft.getMinecraft().displayHeight, renderIn.width / 2, (renderIn.height / 2) + 30, Color.WHITE.getRGB());
		
		this.doneButton.x = (renderIn.width / 2) - this.doneButton.width - 5;
		this.doneButton.y = (renderIn.height / 2) + 80;
		
		this.cancelButton.x = (renderIn.width / 2) + 5;
		this.cancelButton.y = (renderIn.height / 2) + 80;

		this.renderButtons(mouseX, mouseY);
		
	}
	
	private void onDoneButtonPressed() {
		try {
			if (MathUtils.isInteger(this.widthText.getText()) && MathUtils.isInteger(this.heightText.getText())) {
				int w = Integer.parseInt(this.widthText.getText());
				int h = Integer.parseInt(this.heightText.getText());
				if (type == ActionType.BIGGERTHAN) {
					if ((this.parent.biggerThanWidth != w) || (this.parent.biggerThanHeight != h)) {
						this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
					}
					
					this.parent.biggerThanWidth = w;
					this.parent.biggerThanHeight = h;
				} else {
					if ((this.parent.smallerThanWidth != w) || (this.parent.smallerThanHeight != h)) {
						this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
					}
					
					this.parent.smallerThanWidth = w;
					this.parent.smallerThanHeight = h;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.setDisplayed(false);
	}
	
	public void onEnterPressed(KeyboardData d) {
		if ((d.keycode == 28) && this.isDisplayed()) {
			if ((this.doneButton != null) && this.doneButton.visible) {
				this.onDoneButtonPressed();
			}
		}
	}
	
	public void onEscapePressed(KeyboardData d) {
		if ((d.keycode == 1) && this.isDisplayed()) {
			this.setDisplayed(false);
		}
	}
	
	
	public static enum ActionType {
		BIGGERTHAN,
		SMALLERTHAN;
	}

}
