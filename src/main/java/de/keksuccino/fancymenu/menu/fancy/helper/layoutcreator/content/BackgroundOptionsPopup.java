package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content;

import de.keksuccino.fancymenu.api.background.MenuBackground;
import de.keksuccino.fancymenu.api.background.MenuBackgroundType;
import de.keksuccino.fancymenu.api.background.MenuBackgroundTypeRegistry;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMNotificationPopup;
import de.keksuccino.konkrete.input.*;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.UIBase;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMPopup;
import de.keksuccino.fancymenu.menu.panorama.PanoramaHandler;
import de.keksuccino.fancymenu.menu.slideshow.SlideshowHandler;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.HorizontalSwitcher;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;

import java.awt.*;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackgroundOptionsPopup extends FMPopup {

	public LayoutEditorScreen handler;

	public AdvancedButton doneButton;

	public AdvancedButton chooseTextureButton;

	public AdvancedButton setPanoramaButton;
	public AdvancedButton setSlideshowButton;
	public AdvancedButton clearPanoramaButton;
	public AdvancedButton clearSlideshowButton;
	public AdvancedButton setAnimationButton;
	public AdvancedButton clearAnimationButton;
	public AdvancedButton clearImageButton;
	public AdvancedButton setCustomBackgroundButton;
	public AdvancedButton clearCustomBackgroundButton;
	public AdvancedButton chooseCustomBackgroundButton;

	public BackgroundOptionsSwitcher typeSwitcher;
	public HorizontalSwitcher animationSwitcher;
	public HorizontalSwitcher panoramaSwitcher;
	public HorizontalSwitcher slideshowSwitcher;
	public HorizontalSwitcher customBackgroundSwitcher;

	public Map<Integer, MenuBackgroundType> customBackgroundTypes = new HashMap<>();
	public int lastSelectedTypeIndex = -10;

	public BackgroundOptionsPopup(LayoutEditorScreen handler) {
		super(240);

		this.handler = handler;

		this.typeSwitcher = new BackgroundOptionsSwitcher(120, true);
		this.typeSwitcher.addValue(Locals.localize("helper.creator.backgroundoptions.backgroundanimation"));
		this.typeSwitcher.addValue(Locals.localize("helper.creator.backgroundoptions.backgroundimage"));
		this.typeSwitcher.addValue(Locals.localize("helper.creator.backgroundoptions.backgroundpanorama"));
		this.typeSwitcher.addValue(Locals.localize("helper.creator.backgroundoptions.backgroundslideshow"));
		int index = 4; //there are already 4 entries in the switcher, so we start with index 4 for the first custom entry
		for (MenuBackgroundType t : MenuBackgroundTypeRegistry.getBackgroundTypes()) {
			this.typeSwitcher.addValue(t.getDisplayName());
			this.customBackgroundTypes.put(index, t);
			index++;
		}
		this.typeSwitcher.setButtonColor(UIBase.getButtonIdleColor(), UIBase.getButtonHoverColor(), UIBase.getButtonBorderIdleColor(), UIBase.getButtonBorderHoverColor(), 1);
		this.typeSwitcher.setValueBackgroundColor(UIBase.getButtonIdleColor());

		this.animationSwitcher = new HorizontalSwitcher(120, true);
		for (String s : AnimationHandler.getCustomAnimationNames()) {
			this.animationSwitcher.addValue(s);
		}
		this.animationSwitcher.setButtonColor(UIBase.getButtonIdleColor(), UIBase.getButtonHoverColor(), UIBase.getButtonBorderIdleColor(), UIBase.getButtonBorderHoverColor(), 1);
		this.animationSwitcher.setValueBackgroundColor(UIBase.getButtonIdleColor());

		this.panoramaSwitcher = new HorizontalSwitcher(120, true);
		for (String s : PanoramaHandler.getPanoramaNames()) {
			this.panoramaSwitcher.addValue(s);
		}
		this.panoramaSwitcher.setButtonColor(UIBase.getButtonIdleColor(), UIBase.getButtonHoverColor(), UIBase.getButtonBorderIdleColor(), UIBase.getButtonBorderHoverColor(), 1);
		this.panoramaSwitcher.setValueBackgroundColor(UIBase.getButtonIdleColor());

		this.slideshowSwitcher = new HorizontalSwitcher(120, true);
		for (String s : SlideshowHandler.getSlideshowNames()) {
			this.slideshowSwitcher.addValue(s);
		}
		this.slideshowSwitcher.setButtonColor(UIBase.getButtonIdleColor(), UIBase.getButtonHoverColor(), UIBase.getButtonBorderIdleColor(), UIBase.getButtonBorderHoverColor(), 1);
		this.slideshowSwitcher.setValueBackgroundColor(UIBase.getButtonIdleColor());

		this.chooseTextureButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("helper.creator.backgroundoptions.chooseimage"), true, (press) -> {
			ChooseFilePopup cf = new ChooseFilePopup((call) -> {
				File f = new File(call);
				if (f.isFile()) {
					String filename = CharacterFilter.getBasicFilenameCharacterFilter().filterForAllowedChars(f.getName());
					if (filename.equals(f.getName())) {
						BackgroundOptionsPopup.this.handler.setBackgroundTexture(call);
						PopupHandler.displayPopup(this);
					} else {
						FMNotificationPopup pop = new FMNotificationPopup(300, new Color(0,0,0,0), 240, () -> {
							PopupHandler.displayPopup(this);
						}, Locals.localize("helper.creator.textures.invalidcharacters"), "", "", "", "", "", "");
						PopupHandler.displayPopup(pop);
					}
				} else {
					FMNotificationPopup pop = new FMNotificationPopup(300, new Color(0,0,0,0), 240, () -> {
						PopupHandler.displayPopup(this);
					}, "§c§l" + Locals.localize("helper.creator.invalidimage.title"), "", Locals.localize("helper.creator.invalidimage.desc"), "", "", "", "", "", "");
					PopupHandler.displayPopup(pop);
				}
			}, "jpg", "jpeg", "png");
			if ((this.handler.backgroundTexture != null)) {
				cf.setText(this.handler.backgroundTexturePath);
			}
			PopupHandler.displayPopup(cf);
		});
		this.addButton(chooseTextureButton);

		this.setPanoramaButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("helper.creator.backgroundoptions.panoramas.set"), true, (press) -> {
			if (this.panoramaSwitcher.getSelectedValue() != null) {
				if (PanoramaHandler.panoramaExists(this.panoramaSwitcher.getSelectedValue())) {
					this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
					this.resetBackgrounds();
					this.handler.backgroundPanorama = PanoramaHandler.getPanorama(this.panoramaSwitcher.getSelectedValue());
				}
			}
		});
		this.setPanoramaButton.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.backgroundoptions.panoramas.set.desc"), "%n%"));
		this.addButton(setPanoramaButton);
		if (this.panoramaSwitcher.getSelectedValue() == null) {
			this.setPanoramaButton.enabled = false;
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
			this.clearPanoramaButton.enabled = false;
		}

		this.setSlideshowButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("helper.creator.backgroundoptions.backgroundslideshow.set"), true, (press) -> {
			if (this.slideshowSwitcher.getSelectedValue() != null) {
				if (SlideshowHandler.slideshowExists(this.slideshowSwitcher.getSelectedValue())) {
					this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
					this.resetBackgrounds();
					this.handler.backgroundSlideshow = SlideshowHandler.getSlideshow(this.slideshowSwitcher.getSelectedValue());
				}
			}
		});
		this.setSlideshowButton.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.backgroundoptions.backgroundslideshow.set.desc"), "%n%"));
		this.addButton(setSlideshowButton);
		if (this.slideshowSwitcher.getSelectedValue() == null) {
			this.setSlideshowButton.enabled = false;
		}

		this.clearSlideshowButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("helper.creator.backgroundoptions.backgroundslideshow.clear"), true, (press) -> {
			if (this.handler.backgroundSlideshow != null) {
				this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
			}
			this.handler.backgroundSlideshow = null;
		});
		this.clearSlideshowButton.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.backgroundoptions.backgroundslideshow.clear.desc"), "%n%"));
		this.addButton(clearSlideshowButton);
		if (this.slideshowSwitcher.getSelectedValue() == null) {
			this.clearSlideshowButton.enabled = false;
		}

		this.setAnimationButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("fancymenu.helper.creator.backgroundoptions.backgroundanimation.set"), true, (press) -> {
			if (this.animationSwitcher.getSelectedValue() != null) {
				this.handler.backgroundAnimationNames.clear();
				this.handler.setBackgroundAnimations(this.animationSwitcher.getSelectedValue());
			}
		});
		this.setAnimationButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.creator.backgroundoptions.backgroundanimation.set.desc"), "%n%"));
		this.addButton(setAnimationButton);
		if (this.animationSwitcher.getSelectedValue() == null) {
			this.setAnimationButton.enabled = false;
		}

		this.clearAnimationButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("fancymenu.helper.creator.backgroundoptions.backgroundanimation.clear"), true, (press) -> {
			if (!this.handler.backgroundAnimationNames.isEmpty()) {
				this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
			}
			this.handler.setBackgroundAnimations((String[])null);
			this.handler.backgroundAnimationNames.clear();
		});
		this.clearAnimationButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.creator.backgroundoptions.backgroundanimation.clear.desc"), "%n%"));
		this.addButton(clearAnimationButton);
		if (this.animationSwitcher.getSelectedValue() == null) {
			this.clearAnimationButton.enabled = false;
		}

		this.chooseCustomBackgroundButton = new AdvancedButton(0, 0, 100, 20, "", true, (press) -> {
			if (isCustomType(this.typeSwitcher.getSelectedValueIndex())) {
				MenuBackgroundType t = this.customBackgroundTypes.get(this.typeSwitcher.getSelectedValueIndex());
				if (t != null) {
					if (t.needsInputString()) {
						t.onInputStringButtonPress(this.handler, this);
					}
				}
			}
		}) {

			@Override
			public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {

				//Set correct button label for selected custom background type
				if (isCustomType(typeSwitcher.getSelectedValueIndex())) {
					MenuBackgroundType t = customBackgroundTypes.get(typeSwitcher.getSelectedValueIndex());
					if (t != null) {
						if (t.needsInputString()) {
							this.displayString = t.inputStringButtonLabel();
							if (t.inputStringButtonTooltip() != null) {
								this.setDescription(t.inputStringButtonTooltip().toArray(new String[0]));
							}
						}
					}
				}

				super.drawButton(mc, mouseX, mouseY, partialTicks);

			}

		};
		this.addButton(chooseCustomBackgroundButton);

		this.setCustomBackgroundButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("fancymenu.helper.editor.backgrounds.custom.setbackground"), true, (press) -> {
			if (isCustomType(this.typeSwitcher.getSelectedValueIndex())) {
				MenuBackgroundType t = this.customBackgroundTypes.get(this.typeSwitcher.getSelectedValueIndex());
				if (t != null) {
					if (!t.needsInputString()) {
						if (this.customBackgroundSwitcher.getSelectedValue() != null) {
							MenuBackground b = t.getBackgroundByIdentifier(this.customBackgroundSwitcher.getSelectedValue());
							if (b != null) {
								if ((handler.customMenuBackground == null) || (handler.customMenuBackground != b)) {
									handler.history.saveSnapshot(handler.history.createSnapshot());
								}
								this.resetBackgrounds();
								b.onOpenMenu();
								handler.customMenuBackground = b;
								handler.customMenuBackgroundInputString = null;
							}
						}
					}
				}
			}
		});
		this.setCustomBackgroundButton.enabled = false;
		this.addButton(setCustomBackgroundButton);

		this.clearCustomBackgroundButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("fancymenu.helper.editor.backgrounds.custom.clearbackground"), true, (press) -> {
			if (this.handler.customMenuBackground != null) {
				this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
			}
			this.handler.customMenuBackground = null;
			this.handler.customMenuBackgroundInputString = null;
		});
		this.clearCustomBackgroundButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.backgrounds.custom.clearbackground.desc"), "%n%"));
		this.clearCustomBackgroundButton.enabled = false;
		this.addButton(clearCustomBackgroundButton);

		this.clearImageButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("fancymenu.helper.editor.layoutoptions.backgroundoptions.image.clear"), true, (press) -> {
			if (this.handler.backgroundTexture != null) {
				this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
			}
			this.handler.backgroundTexture = null;
			this.handler.backgroundTexturePath = null;
		});
		this.clearImageButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.layoutoptions.backgroundoptions.image.clear.btn.desc"), "%n%"));
		this.addButton(clearImageButton);

		this.doneButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("popup.done"), true, (press) -> {
			this.setDisplayed(false);
		});
		this.addButton(this.doneButton);

		KeyboardHandler.addKeyPressedListener(this::onEnterPressed);
		KeyboardHandler.addKeyPressedListener(this::onEscapePressed);
	}

	@Override
	public void render(int mouseX, int mouseY, GuiScreen renderIn) {
		super.render(mouseX, mouseY, renderIn);

		if (this.isDisplayed()) {

			List<String> typeDescription = null;

			drawCenteredString(Minecraft.getMinecraft().fontRenderer, "§l" + Locals.localize("fancymenu.helper.editor.layoutoptions.backgroundoptions.setbackground"), renderIn.width / 2, (renderIn.height / 2) - 110, -1);

			this.typeSwitcher.render((renderIn.width / 2) - (this.typeSwitcher.getTotalWidth() / 2), (renderIn.height / 2) - 85);

			this.chooseTextureButton.visible = false;
			this.setPanoramaButton.visible = false;
			this.setSlideshowButton.visible = false;
			this.clearPanoramaButton.visible = false;
			this.clearSlideshowButton.visible = false;
			this.setAnimationButton.visible = false;
			this.clearAnimationButton.visible = false;
			this.clearImageButton.visible = false;
			this.setCustomBackgroundButton.visible = false;
			this.clearCustomBackgroundButton.visible = false;
			this.chooseCustomBackgroundButton.visible = false;

			String selectedType = this.typeSwitcher.getSelectedValue();

			if (selectedType.equals(Locals.localize("helper.creator.backgroundoptions.backgroundanimation"))) {

				typeDescription = Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.backgrounds.animation.desc"), "%n%"));

				drawCenteredString(Minecraft.getMinecraft().fontRenderer, Locals.localize("fancymenu.helper.editor.backgrounds.choose"), renderIn.width / 2, (renderIn.height / 2) - 50, -1);

				this.animationSwitcher.render((renderIn.width / 2) - (this.animationSwitcher.getTotalWidth() / 2), (renderIn.height / 2) - 35);

				this.setAnimationButton.x = (renderIn.width / 2) - (this.setAnimationButton.width / 2);
				this.setAnimationButton.y = (renderIn.height / 2) - 5;

				this.clearAnimationButton.x = (renderIn.width / 2) - (this.clearAnimationButton.width / 2);
				this.clearAnimationButton.y = (renderIn.height / 2) + 20;

				this.setAnimationButton.visible = true;
				this.clearAnimationButton.visible = true;

			} else if (selectedType.equals(Locals.localize("helper.creator.backgroundoptions.backgroundimage"))) {

				typeDescription = Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.backgrounds.image.desc"), "%n%"));

				this.chooseTextureButton.x = (renderIn.width / 2) - (this.chooseTextureButton.width / 2);
				this.chooseTextureButton.y = (renderIn.height / 2) - 25;
				this.chooseTextureButton.visible = true;

				this.clearImageButton.x = (renderIn.width / 2) - (this.clearPanoramaButton.width / 2);
				this.clearImageButton.y = (renderIn.height / 2);
				this.clearImageButton.visible = true;

			} else if (selectedType.equals(Locals.localize("helper.creator.backgroundoptions.backgroundpanorama"))) {

				typeDescription = Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.backgrounds.panorama.desc"), "%n%"));

				drawCenteredString(Minecraft.getMinecraft().fontRenderer, Locals.localize("fancymenu.helper.editor.backgrounds.choose"), renderIn.width / 2, (renderIn.height / 2) - 50, -1);

				this.panoramaSwitcher.render((renderIn.width / 2) - (this.panoramaSwitcher.getTotalWidth() / 2), (renderIn.height / 2) - 35);

				this.setPanoramaButton.x = (renderIn.width / 2) - (this.setPanoramaButton.width / 2);
				this.setPanoramaButton.y = (renderIn.height / 2) - 5;

				this.clearPanoramaButton.x = (renderIn.width / 2) - (this.clearPanoramaButton.width / 2);
				this.clearPanoramaButton.y = (renderIn.height / 2) + 20;

				this.setPanoramaButton.visible = true;
				this.clearPanoramaButton.visible = true;

			} else if (selectedType.equals(Locals.localize("helper.creator.backgroundoptions.backgroundslideshow"))) {

				typeDescription = Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.backgrounds.slideshow.desc"), "%n%"));

				drawCenteredString(Minecraft.getMinecraft().fontRenderer, Locals.localize("fancymenu.helper.editor.backgrounds.choose"), renderIn.width / 2, (renderIn.height / 2) - 50, -1);

				this.slideshowSwitcher.render((renderIn.width / 2) - (this.slideshowSwitcher.getTotalWidth() / 2), (renderIn.height / 2) - 35);

				this.setSlideshowButton.x = (renderIn.width / 2) - (this.setSlideshowButton.width / 2);
				this.setSlideshowButton.y = (renderIn.height / 2) - 5;

				this.clearSlideshowButton.x = (renderIn.width / 2) - (this.clearSlideshowButton.width / 2);
				this.clearSlideshowButton.y = (renderIn.height / 2) + 20;

				this.setSlideshowButton.visible = true;
				this.clearSlideshowButton.visible = true;

			} else if (isCustomType(this.typeSwitcher.getSelectedValueIndex())) {

				MenuBackgroundType t = this.customBackgroundTypes.get(this.typeSwitcher.getSelectedValueIndex());
				if (t != null) {

					typeDescription = t.getDescription();

					if (t.needsInputString()) {

						this.chooseCustomBackgroundButton.x = (renderIn.width / 2) - (this.chooseCustomBackgroundButton.width / 2);
						this.chooseCustomBackgroundButton.y = (renderIn.height / 2) - 25;
						this.chooseCustomBackgroundButton.visible = true;
						this.chooseCustomBackgroundButton.enabled = true;

						this.clearCustomBackgroundButton.x = (renderIn.width / 2) - (this.clearCustomBackgroundButton.width / 2);
						this.clearCustomBackgroundButton.y = (renderIn.height / 2);
						this.clearCustomBackgroundButton.visible = true;
						if (handler.customMenuBackground == null) {
							this.clearCustomBackgroundButton.enabled = false;
						} else {
							this.clearCustomBackgroundButton.enabled = true;
						}

					} else {

						List<MenuBackground> backgrounds = t.getBackgrounds();

						if ((this.lastSelectedTypeIndex != this.typeSwitcher.getSelectedValueIndex()) || (this.customBackgroundSwitcher == null)) {
							this.customBackgroundSwitcher = new HorizontalSwitcher(120, true);
							this.customBackgroundSwitcher.setButtonColor(UIBase.getButtonIdleColor(), UIBase.getButtonHoverColor(), UIBase.getButtonBorderIdleColor(), UIBase.getButtonBorderHoverColor(), 1);
							this.customBackgroundSwitcher.setValueBackgroundColor(UIBase.getButtonIdleColor());
							for (MenuBackground b : backgrounds) {
								this.customBackgroundSwitcher.addValue(b.getIdentifier());
							}
						}

						this.setCustomBackgroundButton.visible = true;
						this.clearCustomBackgroundButton.visible = true;
						if (this.customBackgroundSwitcher.getSelectedValue() == null) {
							this.setCustomBackgroundButton.enabled = false;
						} else {
							this.setCustomBackgroundButton.enabled = true;
						}
						if (handler.customMenuBackground == null) {
							this.clearCustomBackgroundButton.enabled = false;
						} else {
							this.clearCustomBackgroundButton.enabled = true;
						}

						drawCenteredString(Minecraft.getMinecraft().fontRenderer, Locals.localize("fancymenu.helper.editor.backgrounds.choose"), renderIn.width / 2, (renderIn.height / 2) - 50, -1);

						this.customBackgroundSwitcher.render((renderIn.width / 2) - (this.customBackgroundSwitcher.getTotalWidth() / 2), (renderIn.height / 2) - 35);

						this.setCustomBackgroundButton.x = (renderIn.width / 2) - (this.setCustomBackgroundButton.width / 2);
						this.setCustomBackgroundButton.y = (renderIn.height / 2) - 5;

						this.clearCustomBackgroundButton.x = (renderIn.width / 2) - (this.clearCustomBackgroundButton.width / 2);
						this.clearCustomBackgroundButton.y = (renderIn.height / 2) + 20;

					}

				}

			}

			this.lastSelectedTypeIndex = this.typeSwitcher.getSelectedValueIndex();

			this.doneButton.x = (renderIn.width / 2) - (this.doneButton.width / 2);
			this.doneButton.y = (renderIn.height / 2) + 85;

			this.renderButtons(mouseX, mouseY);

			if ((typeDescription != null) && !typeDescription.isEmpty()) {
				int xStart = (renderIn.width / 2) - (this.typeSwitcher.getTotalWidth() / 2);
				int yStart = (renderIn.height / 2) - 85;
				int xEnd = xStart + this.typeSwitcher.getTotalWidth();
				int yEnd = yStart + this.typeSwitcher.getHeight();
				int mX = MouseInput.getMouseX();
				int mY = MouseInput.getMouseY();
				if ((mX >= xStart) && (mX <= xEnd) && (mY >= yStart) && (mY <= yEnd)) {
					renderDescription(mX, mY, typeDescription);
				}
			}

		}
	}

	public void onEnterPressed(KeyboardData d) {
		if ((d.keycode == 257) && this.isDisplayed()) {
			this.setDisplayed(false);
		}
	}

	public void onEscapePressed(KeyboardData d) {
		if ((d.keycode == 256) && this.isDisplayed()) {
			this.setDisplayed(false);
		}
	}

	public void resetBackgrounds() {
		this.handler.setBackgroundAnimations((String[])null);
		this.handler.backgroundAnimationNames.clear();
		this.handler.backgroundTexture = null;
		this.handler.backgroundTexturePath = null;
		this.handler.backgroundPanorama = null;
		this.handler.backgroundSlideshow = null;
		this.handler.customMenuBackground = null;
		this.handler.customMenuBackgroundInputString = null;
	}

	public boolean isCustomType(int index) {
		return this.customBackgroundTypes.containsKey(index);
	}

	public static class BackgroundOptionsSwitcher extends HorizontalSwitcher {

		public BackgroundOptionsSwitcher(int displayWidth, boolean ignoreBlockedInput, String... values) {
			super(displayWidth, ignoreBlockedInput, values);
		}

		public int getSelectedValueIndex() {
			try {
				Field f = HorizontalSwitcher.class.getDeclaredField("selected");
				f.setAccessible(true);
				return f.getInt(this);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return -1;
		}

	}

	public static void renderDescriptionBackground(int x, int y, int width, int height) {
		Gui.drawRect(x, y, x + width, y + height, new Color(26, 26, 26, 250).getRGB());
	}

	public static void renderDescription(int mouseX, int mouseY, List<String> desc) {
		if (desc != null) {
			int width = 10;
			int height = 10;

			for (String s : desc) {
				int i = Minecraft.getMinecraft().fontRenderer.getStringWidth(s) + 10;
				if (i > width) {
					width = i;
				}
				height += 10;
			}

			mouseX += 5;
			mouseY += 5;

			if (Minecraft.getMinecraft().currentScreen.width < mouseX + width) {
				mouseX -= width + 10;
			}

			if (Minecraft.getMinecraft().currentScreen.height < mouseY + height) {
				mouseY -= height + 10;
			}

			renderDescriptionBackground(mouseX, mouseY, width, height);

			GlStateManager.enableBlend();

			int i2 = 5;
			for (String s : desc) {
				Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(s, mouseX + 5, mouseY + i2, -1);
				i2 += 10;
			}

			GlStateManager.disableBlend();
		}
	}

}

