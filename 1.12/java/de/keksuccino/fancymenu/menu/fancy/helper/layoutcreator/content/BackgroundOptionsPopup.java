package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content;

import java.awt.Color;
import java.io.File;

import de.keksuccino.core.filechooser.FileChooser;
import de.keksuccino.core.gui.content.AdvancedButton;
import de.keksuccino.core.gui.content.HorizontalSwitcher;
import de.keksuccino.core.gui.screens.popup.Popup;
import de.keksuccino.core.input.KeyboardData;
import de.keksuccino.core.input.KeyboardHandler;
import de.keksuccino.fancymenu.localization.Locals;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutCreatorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

public class BackgroundOptionsPopup extends Popup {
	
	private LayoutCreatorScreen handler;
	
	protected AdvancedButton doneButton;
	
	protected AdvancedButton chooseTextureButton;
	
	protected AdvancedButton randomButton;
	protected AdvancedButton notRandomButton;
	
	protected AdvancedButton addRemoveAnimationButton;
	
	protected HorizontalSwitcher typeSwitcher;
	protected HorizontalSwitcher animationSwitcher;
	
	public BackgroundOptionsPopup(LayoutCreatorScreen handler) {
		super(240);
		
		this.handler = handler;
		
		this.typeSwitcher = new HorizontalSwitcher(120,
				Locals.localize("helper.creator.backgroundoptions.backgroundanimation"),
				Locals.localize("helper.creator.backgroundoptions.backgroundimage"));
		this.typeSwitcher.setButtonColor(new Color(102, 102, 153), new Color(133, 133, 173), new Color(163, 163, 194), new Color(163, 163, 194), 1);
		this.typeSwitcher.setValueBackgroundColor(new Color(102, 102, 153));
		
		this.animationSwitcher = new HorizontalSwitcher(120);
		for (String s : AnimationHandler.getCustomAnimationNames()) {
			this.animationSwitcher.addValue(s);
		}
		this.animationSwitcher.setButtonColor(new Color(102, 102, 153), new Color(133, 133, 173), new Color(163, 163, 194), new Color(163, 163, 194), 1);
		this.animationSwitcher.setValueBackgroundColor(new Color(102, 102, 153));
		
		this.chooseTextureButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("helper.creator.backgroundoptions.chooseimage"), true, (press) -> {
			FileChooser.askForFile(new File("").getAbsoluteFile(), (call) -> {
				BackgroundOptionsPopup.this.handler.setBackgroundTexture(call.getPath());
			}, "jpg", "jpeg", "png");
		});
		LayoutCreatorScreen.colorizeCreatorButton(chooseTextureButton);
		
		String ran = Locals.localize("helper.creator.backgroundoptions.random");
		String nRan = Locals.localize("helper.creator.backgroundoptions.notrandom");
		if (this.handler.randomBackgroundAnimation) {
			ran = "§a" + ran;
		} else {
			nRan = "§a" + nRan;
		}
		this.randomButton = new AdvancedButton(0, 0, 100, 20, ran, true, (press) -> {
			this.handler.randomBackgroundAnimation = true;
			press.displayString = "§a" + Locals.localize("helper.creator.backgroundoptions.random");
			this.notRandomButton.displayString = Locals.localize("helper.creator.backgroundoptions.notrandom");
		});
		LayoutCreatorScreen.colorizeCreatorButton(randomButton);
		this.notRandomButton = new AdvancedButton(0, 0, 100, 20, nRan, true, (press) -> {
			this.handler.randomBackgroundAnimation = false;
			press.displayString = "§a" + Locals.localize("helper.creator.backgroundoptions.notrandom");
			this.randomButton.displayString = Locals.localize("helper.creator.backgroundoptions.random");
		});
		LayoutCreatorScreen.colorizeCreatorButton(notRandomButton);
		
		this.addRemoveAnimationButton = new AdvancedButton(0, 0, 100, 20, "", true, (press) -> {
			if (this.animationSwitcher.getSelectedValue() != null) {
				if (this.handler.backgroundAnimationNames.contains(this.animationSwitcher.getSelectedValue())) {
					this.handler.backgroundAnimationNames.remove(this.animationSwitcher.getSelectedValue());
					if (this.handler.backgroundAnimationNames.isEmpty()) {
						this.handler.setBackgroundAnimations((String[])null);
					} else {
						this.handler.setBackgroundAnimations(this.handler.backgroundAnimationNames.toArray(new String[0]));
					}
				} else {
					this.handler.backgroundAnimationNames.add(this.animationSwitcher.getSelectedValue());
					this.handler.setBackgroundAnimations(this.handler.backgroundAnimationNames.toArray(new String[0]));
				}
			}
		});
		LayoutCreatorScreen.colorizeCreatorButton(addRemoveAnimationButton);
		
		this.doneButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("popup.done"), true, (press) -> {
			this.setDisplayed(false);
			this.handler.setMenusUseable(true);
		});
		this.addButton(this.doneButton);
		
		KeyboardHandler.addKeyPressedListener(this::onEnterPressed);
		KeyboardHandler.addKeyPressedListener(this::onEscapePressed);
	}

	@Override
	public void render(int mouseX, int mouseY, GuiScreen renderIn) {
		super.render(mouseX, mouseY, renderIn);
		
		if (this.isDisplayed()) {
			
			renderIn.drawCenteredString(Minecraft.getMinecraft().fontRenderer, "§l" + Locals.localize("helper.creator.backgroundoptions"), renderIn.width / 2, (renderIn.height / 2) - 110, Color.WHITE.getRGB());
			
			
			this.typeSwitcher.render((renderIn.width / 2) - (this.typeSwitcher.getTotalWidth() / 2), (renderIn.height / 2) - 85);
			
			
			String s = this.typeSwitcher.getSelectedValue();
			if (s.equals(Locals.localize("helper.creator.backgroundoptions.backgroundanimation"))) {
				
				renderIn.drawCenteredString(Minecraft.getMinecraft().fontRenderer, Locals.localize("helper.creator.backgroundoptions.animations"), renderIn.width / 2, (renderIn.height / 2) - 50, Color.WHITE.getRGB());
				
				this.animationSwitcher.render((renderIn.width / 2) - (this.animationSwitcher.getTotalWidth() / 2), (renderIn.height / 2) - 35);
				
				if (this.animationSwitcher.getSelectedValue() != null) {
					this.addRemoveAnimationButton.x = (renderIn.width / 2) - (this.addRemoveAnimationButton.width / 2);
					this.addRemoveAnimationButton.y = (renderIn.height / 2) - 5;
					if (this.handler.backgroundAnimationNames.contains(this.animationSwitcher.getSelectedValue())) {
						this.addRemoveAnimationButton.displayString = Locals.localize("helper.creator.backgroundoptions.removeanimation");
					} else {
						this.addRemoveAnimationButton.displayString = Locals.localize("helper.creator.backgroundoptions.addanimation");
					}
					this.addRemoveAnimationButton.drawButton(Minecraft.getMinecraft(), mouseX, mouseY, Minecraft.getMinecraft().getRenderPartialTicks());
				}
				
				renderIn.drawCenteredString(Minecraft.getMinecraft().fontRenderer, Locals.localize("helper.creator.backgroundoptions.randomizeanimations"), renderIn.width / 2, (renderIn.height / 2) + 30, Color.WHITE.getRGB());
				
				this.randomButton.x = (renderIn.width / 2) - this.randomButton.width - 5;
				this.randomButton.y = (renderIn.height / 2) + 45;
				this.randomButton.drawButton(Minecraft.getMinecraft(), mouseX, mouseY, Minecraft.getMinecraft().getRenderPartialTicks());
				
				this.notRandomButton.x = (renderIn.width / 2) + 5;
				this.notRandomButton.y = (renderIn.height / 2) + 45;
				this.notRandomButton.drawButton(Minecraft.getMinecraft(), mouseX, mouseY, Minecraft.getMinecraft().getRenderPartialTicks());

			}
			
			if (s.equals(Locals.localize("helper.creator.backgroundoptions.backgroundimage"))) {
				this.chooseTextureButton.x = (renderIn.width / 2) - (this.chooseTextureButton.width / 2);
				this.chooseTextureButton.y = renderIn.height / 2;
				this.chooseTextureButton.drawButton(Minecraft.getMinecraft(), mouseX, mouseY, Minecraft.getMinecraft().getRenderPartialTicks());
			}
			
			
			this.doneButton.x = (renderIn.width / 2) - (this.doneButton.width / 2);
			this.doneButton.y = (renderIn.height / 2) + 85;
			
			this.renderButtons(mouseX, mouseY);
		}
	}
	
	public void onEnterPressed(KeyboardData d) {
		if ((d.keycode == 257) && this.isDisplayed()) {
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

