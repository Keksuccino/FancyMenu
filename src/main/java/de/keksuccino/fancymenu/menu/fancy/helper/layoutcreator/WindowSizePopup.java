package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator;

import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import de.keksuccino.konkrete.localization.Locals;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMPopup;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.AdvancedTextField;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.KeyboardData;
import de.keksuccino.konkrete.input.KeyboardHandler;
import de.keksuccino.konkrete.math.MathUtils;

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
		
		this.widthText = new AdvancedTextField(Minecraft.getInstance().font, 0, 0, 200, 20, true, CharacterFilter.getIntegerCharacterFiler());
		if (type == ActionType.BIGGERTHAN) {
			this.widthText.setValue("" + parent.biggerThanWidth);
		} else {
			this.widthText.setValue("" + parent.smallerThanWidth);
		}
		
		this.heightText = new AdvancedTextField(Minecraft.getInstance().font, 0, 0, 200, 20, true, CharacterFilter.getIntegerCharacterFiler());
		if (type == ActionType.BIGGERTHAN) {
			this.heightText.setValue("" + parent.biggerThanHeight);
		} else {
			this.heightText.setValue("" + parent.smallerThanHeight);
		}
		
		KeyboardHandler.addKeyPressedListener(this::onEnterPressed);
		KeyboardHandler.addKeyPressedListener(this::onEscapePressed);
	}
	
	@Override
	public void render(PoseStack matrix, int mouseX, int mouseY, Screen renderIn) {
		super.render(matrix, mouseX, mouseY, renderIn);
		
		float partial = Minecraft.getInstance().getFrameTime();
		Font font = Minecraft.getInstance().font;	
		
		if (this.type == ActionType.BIGGERTHAN) {
			drawCenteredString(matrix, font, "§l" + Locals.localize("helper.creator.windowsize.biggerthan.desc"), renderIn.width / 2, (renderIn.height / 2) - 110, Color.WHITE.getRGB());
		} else {
			drawCenteredString(matrix, font, "§l" + Locals.localize("helper.creator.windowsize.smallerthan.desc"), renderIn.width / 2, (renderIn.height / 2) - 110, Color.WHITE.getRGB());
		}
		
		drawCenteredString(matrix, font, Locals.localize("general.width"), renderIn.width / 2, (renderIn.height / 2) - 80, Color.WHITE.getRGB());
		this.widthText.x = (renderIn.width / 2) - (this.widthText.getWidth() / 2);
		this.widthText.y = (renderIn.height / 2) - 65;
		this.widthText.render(matrix, mouseX, mouseY, partial);
		
		drawCenteredString(matrix, font, Locals.localize("general.height"), renderIn.width / 2, (renderIn.height / 2) - 37, Color.WHITE.getRGB());
		this.heightText.x = (renderIn.width / 2) - (this.heightText.getWidth() / 2);
		this.heightText.y = (renderIn.height / 2) - 22;
		this.heightText.render(matrix, mouseX, mouseY, partial);

		drawCenteredString(matrix, font, Locals.localize("helper.creator.windowsize.currentwidth") + ": " + Minecraft.getInstance().getWindow().getScreenWidth(), renderIn.width / 2, (renderIn.height / 2) + 15, Color.WHITE.getRGB());
		drawCenteredString(matrix, font, Locals.localize("helper.creator.windowsize.currentheight") + ": " + Minecraft.getInstance().getWindow().getScreenHeight(), renderIn.width / 2, (renderIn.height / 2) + 30, Color.WHITE.getRGB());
		
		this.doneButton.x = (renderIn.width / 2) - this.doneButton.getWidth() - 5;
		this.doneButton.y = (renderIn.height / 2) + 80;
		
		this.cancelButton.x = (renderIn.width / 2) + 5;
		this.cancelButton.y = (renderIn.height / 2) + 80;

		this.renderButtons(matrix, mouseX, mouseY);
		
	}
	
	private void onDoneButtonPressed() {
		try {
			if (MathUtils.isInteger(this.widthText.getValue()) && MathUtils.isInteger(this.heightText.getValue())) {
				int w = Integer.parseInt(this.widthText.getValue());
				int h = Integer.parseInt(this.heightText.getValue());
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
		if ((d.keycode == 257) && this.isDisplayed()) {
			if ((this.doneButton != null) && this.doneButton.visible) {
				this.onDoneButtonPressed();
			}
		}
	}
	
	public void onEscapePressed(KeyboardData d) {
		if ((d.keycode == 256) && this.isDisplayed()) {
			this.setDisplayed(false);
		}
	}
	
	public static enum ActionType {
		BIGGERTHAN,
		SMALLERTHAN;
	}

}
