package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content;

import java.awt.Color;

import com.mojang.blaze3d.matrix.MatrixStack;

import de.keksuccino.core.gui.content.AdvancedButton;
import de.keksuccino.core.gui.content.HorizontalSwitcher;
import de.keksuccino.core.gui.screens.popup.Popup;
import de.keksuccino.core.gui.screens.popup.PopupHandler;
import de.keksuccino.core.input.KeyboardData;
import de.keksuccino.core.input.KeyboardHandler;
import de.keksuccino.fancymenu.localization.Locals;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutCreatorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

public class BackgroundOptionsPopup extends Popup {
	
	private LayoutCreatorScreen handler;
	
	protected AdvancedButton doneButton;
	
	protected AdvancedButton chooseTextureButton;
	
	protected AdvancedButton randomButton;
	protected AdvancedButton notRandomButton;
	
	protected AdvancedButton panoramaButton;
	protected AdvancedButton noPanoramaButton;
	
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
			BackgroundOptionsPopup.this.handler.setMenusUseable(false);
			ChooseFilePopup cf = new ChooseFilePopup((call) -> {
				BackgroundOptionsPopup.this.handler.setBackgroundTexture(call);
			}, "jpg", "jpeg", "png");
			if ((this.handler.backgroundTexture != null)) {
				cf.setText(this.handler.backgroundTexturePath);
			}
			PopupHandler.displayPopup(cf);
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
			((AdvancedButton)press).setMessage("§a" + Locals.localize("helper.creator.backgroundoptions.random"));
			this.notRandomButton.setMessage(Locals.localize("helper.creator.backgroundoptions.notrandom"));
		});
		LayoutCreatorScreen.colorizeCreatorButton(randomButton);
		this.notRandomButton = new AdvancedButton(0, 0, 100, 20, nRan, true, (press) -> {
			this.handler.randomBackgroundAnimation = false;
			((AdvancedButton)press).setMessage("§a" + Locals.localize("helper.creator.backgroundoptions.notrandom"));
			this.randomButton.setMessage(Locals.localize("helper.creator.backgroundoptions.random"));
		});
		LayoutCreatorScreen.colorizeCreatorButton(notRandomButton);
		
		String pan = Locals.localize("helper.creator.backgroundoptions.panorama");
		String nPan = Locals.localize("helper.creator.backgroundoptions.nopanorama");
		if (this.handler.panorama) {
			pan = "§a" + pan;
		} else {
			nPan = "§a" + nPan;
		}
		this.panoramaButton = new AdvancedButton(0, 0, 100, 20, pan, true, (press) -> {
			this.handler.panorama = true;
			((AdvancedButton)press).setMessage("§a" + Locals.localize("helper.creator.backgroundoptions.panorama"));
			this.noPanoramaButton.setMessage(Locals.localize("helper.creator.backgroundoptions.nopanorama"));
		});
		LayoutCreatorScreen.colorizeCreatorButton(panoramaButton);
		this.noPanoramaButton = new AdvancedButton(0, 0, 100, 20, nPan, true, (press) -> {
			this.handler.panorama = false;
			((AdvancedButton)press).setMessage("§a" + Locals.localize("helper.creator.backgroundoptions.nopanorama"));
			this.panoramaButton.setMessage(Locals.localize("helper.creator.backgroundoptions.panorama"));
		});
		LayoutCreatorScreen.colorizeCreatorButton(noPanoramaButton);
		
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
	public void render(MatrixStack matrix, int mouseX, int mouseY, Screen renderIn) {
		super.render(matrix, mouseX, mouseY, renderIn);
		
		if (this.isDisplayed()) {
			
			renderIn.func_238472_a_(matrix, Minecraft.getInstance().fontRenderer, new StringTextComponent("§l" + Locals.localize("helper.creator.backgroundoptions")), renderIn.field_230708_k_ / 2, (renderIn.field_230709_l_ / 2) - 110, Color.WHITE.getRGB());
			
			
			this.typeSwitcher.render(matrix, (renderIn.field_230708_k_ / 2) - (this.typeSwitcher.getTotalWidth() / 2), (renderIn.field_230709_l_ / 2) - 85);
			
			
			String s = this.typeSwitcher.getSelectedValue();
			if (s.equals(Locals.localize("helper.creator.backgroundoptions.backgroundanimation"))) {
				
				renderIn.func_238472_a_(matrix, Minecraft.getInstance().fontRenderer, new StringTextComponent(Locals.localize("helper.creator.backgroundoptions.animations")), renderIn.field_230708_k_ / 2, (renderIn.field_230709_l_ / 2) - 50, Color.WHITE.getRGB());
				
				this.animationSwitcher.render(matrix, (renderIn.field_230708_k_ / 2) - (this.animationSwitcher.getTotalWidth() / 2), (renderIn.field_230709_l_ / 2) - 35);
				
				if (this.animationSwitcher.getSelectedValue() != null) {
					this.addRemoveAnimationButton.setX((renderIn.field_230708_k_ / 2) - (this.addRemoveAnimationButton.getWidth() / 2));
					this.addRemoveAnimationButton.setY((renderIn.field_230709_l_ / 2) - 5);
					if (this.handler.backgroundAnimationNames.contains(this.animationSwitcher.getSelectedValue())) {
						this.addRemoveAnimationButton.setMessage(Locals.localize("helper.creator.backgroundoptions.removeanimation"));
					} else {
						this.addRemoveAnimationButton.setMessage(Locals.localize("helper.creator.backgroundoptions.addanimation"));
					}
					this.addRemoveAnimationButton.render(matrix, mouseX, mouseY, Minecraft.getInstance().getRenderPartialTicks());
				}
				
				renderIn.func_238472_a_(matrix, Minecraft.getInstance().fontRenderer, new StringTextComponent(Locals.localize("helper.creator.backgroundoptions.randomizeanimations")), renderIn.field_230708_k_ / 2, (renderIn.field_230709_l_ / 2) + 30, Color.WHITE.getRGB());
				
				this.randomButton.setX((renderIn.field_230708_k_ / 2) - this.randomButton.getWidth() - 5);
				this.randomButton.setY((renderIn.field_230709_l_ / 2) + 45);
				this.randomButton.render(matrix, mouseX, mouseY, Minecraft.getInstance().getRenderPartialTicks());
				
				this.notRandomButton.setX((renderIn.field_230708_k_ / 2) + 5);
				this.notRandomButton.setY((renderIn.field_230709_l_ / 2) + 45);
				this.notRandomButton.render(matrix, mouseX, mouseY, Minecraft.getInstance().getRenderPartialTicks());

			}
			
			if (s.equals(Locals.localize("helper.creator.backgroundoptions.backgroundimage"))) {
				this.chooseTextureButton.setX((renderIn.field_230708_k_ / 2) - (this.chooseTextureButton.getWidth() / 2));
				this.chooseTextureButton.setY((renderIn.field_230709_l_ / 2) - 25);
				this.chooseTextureButton.render(matrix, mouseX, mouseY, Minecraft.getInstance().getRenderPartialTicks());

				renderIn.func_238472_a_(matrix, Minecraft.getInstance().fontRenderer, new StringTextComponent(Locals.localize("helper.creator.backgroundoptions.setpanorama")), renderIn.field_230708_k_ / 2, (renderIn.field_230709_l_ / 2) + 13, Color.WHITE.getRGB());

				this.panoramaButton.setX((renderIn.field_230708_k_ / 2) - this.panoramaButton.getWidth() - 5);
				this.panoramaButton.setY((renderIn.field_230709_l_ / 2) + 30);
				this.panoramaButton.render(matrix, mouseX, mouseY, Minecraft.getInstance().getRenderPartialTicks());

				this.noPanoramaButton.setX((renderIn.field_230708_k_ / 2) + 5);
				this.noPanoramaButton.setY((renderIn.field_230709_l_ / 2) + 30);
				this.noPanoramaButton.render(matrix, mouseX, mouseY, Minecraft.getInstance().getRenderPartialTicks());
			}
			
			
			this.doneButton.setX((renderIn.field_230708_k_ / 2) - (this.doneButton.getWidth() / 2));
			this.doneButton.setY((renderIn.field_230709_l_ / 2) + 85);
			
			this.renderButtons(matrix, mouseX, mouseY);
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

