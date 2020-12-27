package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content;

import java.awt.Color;

import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutCreatorScreen;
import de.keksuccino.fancymenu.menu.panorama.PanoramaHandler;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.HorizontalSwitcher;
import de.keksuccino.konkrete.gui.screens.popup.Popup;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.KeyboardData;
import de.keksuccino.konkrete.input.KeyboardHandler;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;

public class BackgroundOptionsPopup extends Popup {
	
	private LayoutCreatorScreen handler;
	
	protected AdvancedButton doneButton;
	
	protected AdvancedButton chooseTextureButton;
	
	protected AdvancedButton randomButton;
	protected AdvancedButton notRandomButton;
	
	protected AdvancedButton panoramaButton;
	protected AdvancedButton noPanoramaButton;
	
	protected AdvancedButton addRemoveAnimationButton;
	
	protected AdvancedButton setPanoramaButton;
	protected AdvancedButton clearPanoramaButton;
	
	protected HorizontalSwitcher typeSwitcher;
	protected HorizontalSwitcher animationSwitcher;
	protected HorizontalSwitcher panoramaSwitcher;
	
	public BackgroundOptionsPopup(LayoutCreatorScreen handler) {
		super(240);
		
		this.handler = handler;
		
		this.typeSwitcher = new HorizontalSwitcher(120, true,
				Locals.localize("helper.creator.backgroundoptions.backgroundanimation"),
				Locals.localize("helper.creator.backgroundoptions.backgroundimage"),
				Locals.localize("helper.creator.backgroundoptions.backgroundpanorama"));
		this.typeSwitcher.setButtonColor(new Color(102, 102, 153), new Color(133, 133, 173), new Color(163, 163, 194), new Color(163, 163, 194), 1);
		this.typeSwitcher.setValueBackgroundColor(new Color(102, 102, 153));
		
		this.animationSwitcher = new HorizontalSwitcher(120, true);
		for (String s : AnimationHandler.getCustomAnimationNames()) {
			this.animationSwitcher.addValue(s);
		}
		this.animationSwitcher.setButtonColor(new Color(102, 102, 153), new Color(133, 133, 173), new Color(163, 163, 194), new Color(163, 163, 194), 1);
		this.animationSwitcher.setValueBackgroundColor(new Color(102, 102, 153));
		
		this.panoramaSwitcher = new HorizontalSwitcher(120, true);
		for (String s : PanoramaHandler.getPanoramaNames()) {
			this.panoramaSwitcher.addValue(s);
		}
		this.panoramaSwitcher.setButtonColor(new Color(102, 102, 153), new Color(133, 133, 173), new Color(163, 163, 194), new Color(163, 163, 194), 1);
		this.panoramaSwitcher.setValueBackgroundColor(new Color(102, 102, 153));
		
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
		this.addButton(chooseTextureButton);
		
		String ran = Locals.localize("helper.creator.backgroundoptions.random");
		String nRan = Locals.localize("helper.creator.backgroundoptions.notrandom");
		if (this.handler.randomBackgroundAnimation) {
			ran = "§a" + ran;
		} else {
			nRan = "§a" + nRan;
		}
		this.randomButton = new AdvancedButton(0, 0, 100, 20, ran, true, (press) -> {
			this.handler.randomBackgroundAnimation = true;
			press.setMessage("§a" + Locals.localize("helper.creator.backgroundoptions.random"));
			this.notRandomButton.setMessage(Locals.localize("helper.creator.backgroundoptions.notrandom"));
		});
		this.addButton(randomButton);
		
		this.notRandomButton = new AdvancedButton(0, 0, 100, 20, nRan, true, (press) -> {
			this.handler.randomBackgroundAnimation = false;
			press.setMessage("§a" + Locals.localize("helper.creator.backgroundoptions.notrandom"));
			this.randomButton.setMessage(Locals.localize("helper.creator.backgroundoptions.random"));
		});
		this.addButton(notRandomButton);
		
		String pan = Locals.localize("helper.creator.backgroundoptions.panorama");
		String nPan = Locals.localize("helper.creator.backgroundoptions.nopanorama");
		if (this.handler.panorama) {
			pan = "§a" + pan;
		} else {
			nPan = "§a" + nPan;
		}
		this.panoramaButton = new AdvancedButton(0, 0, 100, 20, pan, true, (press) -> {
			this.handler.panorama = true;
			press.setMessage("§a" + Locals.localize("helper.creator.backgroundoptions.panorama"));
			this.noPanoramaButton.setMessage(Locals.localize("helper.creator.backgroundoptions.nopanorama"));
		});
		this.addButton(panoramaButton);
		
		this.noPanoramaButton = new AdvancedButton(0, 0, 100, 20, nPan, true, (press) -> {
			this.handler.panorama = false;
			press.setMessage("§a" + Locals.localize("helper.creator.backgroundoptions.nopanorama"));
			this.panoramaButton.setMessage(Locals.localize("helper.creator.backgroundoptions.panorama"));
		});
		this.addButton(noPanoramaButton);
		
		this.setPanoramaButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("helper.creator.backgroundoptions.panoramas.set"), true, (press) -> {
			if (this.panoramaSwitcher.getSelectedValue() != null) {
				if (PanoramaHandler.panoramaExists(this.panoramaSwitcher.getSelectedValue())) {
					this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
					this.handler.backgroundPanorama = PanoramaHandler.getPanorama(this.panoramaSwitcher.getSelectedValue());
				}
			}
		});
		this.setPanoramaButton.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.backgroundoptions.panoramas.set.desc"), "%n%"));
		this.addButton(setPanoramaButton);
		if (this.panoramaSwitcher.getSelectedValue() == null) {
			this.setPanoramaButton.active = false;
		}
		
		this.clearPanoramaButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("helper.creator.backgroundoptions.panoramas.clear"), true, (press) -> {
			if (this.handler.backgroundPanorama != null) {
				this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
			}
			this.handler.backgroundPanorama = null;
		});
		this.clearPanoramaButton.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.backgroundoptions.panoramas.clear.desc"), "%n%"));
		this.addButton(clearPanoramaButton);
		if (this.panoramaSwitcher.getSelectedValue() == null) {
			this.clearPanoramaButton.active = false;
		}
		
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
		this.addButton(addRemoveAnimationButton);
		
		this.doneButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("popup.done"), true, (press) -> {
			this.setDisplayed(false);
			this.handler.setMenusUseable(true);
		});
		this.addButton(this.doneButton);
		
		KeyboardHandler.addKeyPressedListener(this::onEnterPressed);
		KeyboardHandler.addKeyPressedListener(this::onEscapePressed);
	}

	@Override
	public void render(int mouseX, int mouseY, Screen renderIn) {
		super.render(mouseX, mouseY, renderIn);
		
		if (this.isDisplayed()) {
			
			drawCenteredString(Minecraft.getInstance().fontRenderer, "§l" + Locals.localize("helper.creator.backgroundoptions"), renderIn.width / 2, (renderIn.height / 2) - 110, Color.WHITE.getRGB());
			
			
			this.typeSwitcher.render((renderIn.width / 2) - (this.typeSwitcher.getTotalWidth() / 2), (renderIn.height / 2) - 85);
			
			
			String s = this.typeSwitcher.getSelectedValue();
			if (s.equals(Locals.localize("helper.creator.backgroundoptions.backgroundanimation"))) {
				
				drawCenteredString(Minecraft.getInstance().fontRenderer, Locals.localize("helper.creator.backgroundoptions.animations"), renderIn.width / 2, (renderIn.height / 2) - 50, Color.WHITE.getRGB());
				
				this.animationSwitcher.render((renderIn.width / 2) - (this.animationSwitcher.getTotalWidth() / 2), (renderIn.height / 2) - 35);
				
				if (this.animationSwitcher.getSelectedValue() != null) {
					this.addRemoveAnimationButton.x = (renderIn.width / 2) - (this.addRemoveAnimationButton.getWidth() / 2);
					this.addRemoveAnimationButton.y = (renderIn.height / 2) - 5;
					if (this.handler.backgroundAnimationNames.contains(this.animationSwitcher.getSelectedValue())) {
						this.addRemoveAnimationButton.setMessage(Locals.localize("helper.creator.backgroundoptions.removeanimation"));
					} else {
						this.addRemoveAnimationButton.setMessage(Locals.localize("helper.creator.backgroundoptions.addanimation"));
					}
					this.addRemoveAnimationButton.visible = true;
				} else {
					this.addRemoveAnimationButton.visible = false;
				}
				
				drawCenteredString(Minecraft.getInstance().fontRenderer, Locals.localize("helper.creator.backgroundoptions.randomizeanimations"), renderIn.width / 2, (renderIn.height / 2) + 30, Color.WHITE.getRGB());
				
				this.randomButton.x = (renderIn.width / 2) - this.randomButton.getWidth() - 5;
				this.randomButton.y = (renderIn.height / 2) + 45;
				this.randomButton.visible = true;
				
				this.notRandomButton.x = (renderIn.width / 2) + 5;
				this.notRandomButton.y = (renderIn.height / 2) + 45;
				this.notRandomButton.visible = true;

				this.chooseTextureButton.visible = false;
				this.panoramaButton.visible = false;
				this.noPanoramaButton.visible = false;
				this.setPanoramaButton.visible = false;
				this.clearPanoramaButton.visible = false;
			}
			
			if (s.equals(Locals.localize("helper.creator.backgroundoptions.backgroundimage"))) {
				this.chooseTextureButton.x = (renderIn.width / 2) - (this.chooseTextureButton.getWidth() / 2);
				this.chooseTextureButton.y = (renderIn.height / 2) - 25;
				this.chooseTextureButton.visible = true;

				drawCenteredString(Minecraft.getInstance().fontRenderer, Locals.localize("helper.creator.backgroundoptions.setpanorama"), renderIn.width / 2, (renderIn.height / 2) + 13, Color.WHITE.getRGB());

				this.panoramaButton.x = (renderIn.width / 2) - this.panoramaButton.getWidth() - 5;
				this.panoramaButton.y = (renderIn.height / 2) + 30;
				this.panoramaButton.visible = true;

				this.noPanoramaButton.x = (renderIn.width / 2) + 5;
				this.noPanoramaButton.y = (renderIn.height / 2) + 30;
				this.noPanoramaButton.visible = true;

				this.addRemoveAnimationButton.visible = false;
				this.randomButton.visible = false;
				this.notRandomButton.visible = false;
				this.setPanoramaButton.visible = false;
				this.clearPanoramaButton.visible = false;
			}
			
			if (s.equals(Locals.localize("helper.creator.backgroundoptions.backgroundpanorama"))) {
				
				drawCenteredString(Minecraft.getInstance().fontRenderer, Locals.localize("helper.creator.backgroundoptions.panoramas"), renderIn.width / 2, (renderIn.height / 2) - 50, Color.WHITE.getRGB());
				
				this.panoramaSwitcher.render((renderIn.width / 2) - (this.panoramaSwitcher.getTotalWidth() / 2), (renderIn.height / 2) - 35);
				
				this.setPanoramaButton.x = (renderIn.width / 2) - (this.setPanoramaButton.getWidth() / 2);
				this.setPanoramaButton.y = (renderIn.height / 2) - 5;
				
				this.clearPanoramaButton.x = (renderIn.width / 2) - (this.clearPanoramaButton.getWidth() / 2);
				this.clearPanoramaButton.y = (renderIn.height / 2) + 20;
				
				this.setPanoramaButton.visible = true;
				this.clearPanoramaButton.visible = true;
				
				this.addRemoveAnimationButton.visible = false;
				this.randomButton.visible = false;
				this.notRandomButton.visible = false;
				this.chooseTextureButton.visible = false;
				this.panoramaButton.visible = false;
				this.noPanoramaButton.visible = false;
			}
			
			
			this.doneButton.x = (renderIn.width / 2) - (this.doneButton.getWidth() / 2);
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

