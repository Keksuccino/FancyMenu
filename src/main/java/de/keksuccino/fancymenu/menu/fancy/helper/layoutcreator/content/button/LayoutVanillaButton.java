package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.matrix.MatrixStack;

import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.button.ButtonData;
import de.keksuccino.fancymenu.menu.fancy.helper.DynamicValueInputPopup;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.ChooseFilePopup;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutElement;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMTextInputPopup;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMYesNoPopup;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.Minecraft;

public class LayoutVanillaButton extends LayoutElement {
	
	public final ButtonData button;
	//TODO übernehmen
	public final MenuHandlerBase.ButtonCustomizationContainer customizationContainer;
	//TODO übernehmen
//	public boolean hidden = false;
//	public String backNormal = null;
//	public String backHovered = null;
//	public int clicks = 0;
//	public String hoverLabel;
//	public String hoverSound;
//	public String clicksound = null;
//	public String description = null;

	//TODO übernehmen
	public LayoutVanillaButton(MenuHandlerBase.ButtonCustomizationContainer customizationContainer, ButtonData button, LayoutEditorScreen handler) {
		super(new LayoutButtonDummyCustomizationItem(customizationContainer, button.label, button.width, button.height, button.x, button.y), true, handler, false);
		this.button = button;
		this.object.orientation = "original";
		this.customizationContainer = customizationContainer;
		this.object.visibilityRequirementContainer = this.customizationContainer.visibilityRequirementContainer;
		this.init();
	}

	@Override
	public void init() {
		this.orderable = false;
		this.copyable = false;

		super.init();

		AdvancedButton b0 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.vanillabutton.resetorientation"), (press) -> {
			if (!this.object.orientation.equals("original")) {
				this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
			}

			this.object.orientation = "original";
			this.object.posX = this.button.x;
			this.object.posY = this.button.y;
			//TODO übernehmen
			this.object.setWidth(this.button.width);
			this.object.setHeight(this.button.height);
			//----------------
			this.rightclickMenu.closeMenu();
			this.handler.postRenderTasks.add(new Runnable() {
				@Override
				public void run() {
					Minecraft.getInstance().displayGuiScreen(LayoutVanillaButton.this.handler);
				}
			});
		});
		this.rightclickMenu.addContent(b0);

		//TODO übernehmen (edit label nach weiter unten zu edit hoverlabel verschieben)
//		AdvancedButton b2 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.editlabel"), (press) -> {
//			FMTextInputPopup i = new DynamicValueInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.items.button.editlabel"), null, 240, (call) -> {
//				if (call != null) {
//					if ((this.customizationContainer.customButtonLabel == null) || (!this.customizationContainer.customButtonLabel.equals(call))) {
//						this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
//					}
//					this.customizationContainer.customButtonLabel = call;
//				}
//			});
//			i.setText(StringUtils.convertFormatCodes(this.object.value, "§", "&"));
//			PopupHandler.displayPopup(i);
//		});
//		this.rightclickMenu.addContent(b2);
//
//		AdvancedButton b3 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.resetlabel"), (press) -> {
//			this.customizationContainer.customButtonLabel = null;
//			this.rightclickMenu.closeMenu();
//		});
//		this.rightclickMenu.addContent(b3);

		//TODO übernehmen
		this.rightclickMenu.addSeparator();

		//TODO übernehmen (dafür alte button texture settings weg)
		AdvancedButton buttonBackgroundButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground"), (press) -> {
			ButtonBackgroundPopup pop = new ButtonBackgroundPopup(this.handler, this.customizationContainer);
			PopupHandler.displayPopup(pop);
		});
		this.rightclickMenu.addContent(buttonBackgroundButton);

		//TODO übernehmen
		String loopAniLabel = Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.loopanimation.on");
		if (!this.customizationContainer.loopAnimation) {
			loopAniLabel = Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.loopanimation.off");
		}
		AdvancedButton loopBackgroundAnimationButton = new AdvancedButton(0, 0, 0, 0, loopAniLabel, (press) -> {
			if (this.customizationContainer.loopAnimation) {
				this.customizationContainer.loopAnimation = false;
				((AdvancedButton)press).setMessage(Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.loopanimation.off"));
			} else {
				this.customizationContainer.loopAnimation = true;
				((AdvancedButton)press).setMessage(Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.loopanimation.on"));
			}
		});
		loopBackgroundAnimationButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.loopanimation.btn.desc"), "%n%"));
		this.rightclickMenu.addContent(loopBackgroundAnimationButton);

		//TODO übernehmen
		String restartAniLabel = Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.restartonhover.on");
		if (!this.customizationContainer.restartAnimationOnHover) {
			restartAniLabel = Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.restartonhover.off");
		}
		AdvancedButton restartAnimationOnHoverButton = new AdvancedButton(0, 0, 0, 0, restartAniLabel, (press) -> {
			if (this.customizationContainer.restartAnimationOnHover) {
				this.customizationContainer.restartAnimationOnHover = false;
				((AdvancedButton)press).setMessage(Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.restartonhover.off"));
			} else {
				this.customizationContainer.restartAnimationOnHover = true;
				((AdvancedButton)press).setMessage(Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.restartonhover.on"));
			}
		});
		restartAnimationOnHoverButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.restartonhover.btn.desc"), "%n%"));
		this.rightclickMenu.addContent(restartAnimationOnHoverButton);

		//TODO übernehmen
		this.rightclickMenu.addSeparator();

		//TODO übernehmen (von oben verschieben UND änderungen am Code übernehmen/ganzen Abschnitt kopieren)
		AdvancedButton b2 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.editlabel"), (press) -> {
			FMTextInputPopup i = new DynamicValueInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.items.button.editlabel"), null, 240, (call) -> {
				if (call != null) {
					if ((this.customizationContainer.customButtonLabel == null) || !this.customizationContainer.customButtonLabel.equals(call)) {
						this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
					}
					this.customizationContainer.customButtonLabel = call;
				}
			});
			i.setText(StringUtils.convertFormatCodes(this.object.value, "§", "&"));
			PopupHandler.displayPopup(i);
		});
		this.rightclickMenu.addContent(b2);

		//TODO übernehmen (von oben verschieben UND änderungen am Code übernehmen/ganzen Abschnitt kopieren)
		AdvancedButton b3 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.resetlabel"), (press) -> {
			if (this.customizationContainer.customButtonLabel != null) {
				this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
			}
			this.customizationContainer.customButtonLabel = null;
			this.rightclickMenu.closeMenu();
		});
		this.rightclickMenu.addContent(b3);

		//TODO übernehmen
		this.rightclickMenu.addSeparator();

		//TODO übernehmen
		AdvancedButton b5 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.hoverlabel"), (press) -> {
			FMTextInputPopup ip = new DynamicValueInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.items.button.hoverlabel"), null, 240, (call) -> {
				if (call != null) {
					if ((this.customizationContainer.hoverLabel == null) || !this.customizationContainer.hoverLabel.equals(StringUtils.convertFormatCodes(call, "&", "§"))) {
						this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
					}
					this.customizationContainer.hoverLabel = StringUtils.convertFormatCodes(call, "&", "§");
				}
			});

			if (this.customizationContainer.hoverLabel != null) {
				ip.setText(StringUtils.convertFormatCodes(this.customizationContainer.hoverLabel, "§", "&"));
			}
			PopupHandler.displayPopup(ip);
		});
		this.rightclickMenu.addContent(b5);

		//TODO übernehmen
		AdvancedButton b6 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.hoverlabel.reset"), (press) -> {
			if (this.customizationContainer.hoverLabel != null) {
				this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
			}
			this.customizationContainer.hoverLabel = null;
			this.rightclickMenu.closeMenu();
			this.rightclickMenu.closeMenu();
		});
		this.rightclickMenu.addContent(b6);

		//TODO übernehmen
		this.rightclickMenu.addSeparator();

		//TODO übernehmen
		AdvancedButton b7 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.hoversound"), (press) -> {
			ChooseFilePopup cf = new ChooseFilePopup((call) -> {
				if (call != null) {
					if (!call.replace(" ", "").equals("")) {
						File f = new File(call);
						if (f.exists() && f.isFile() && f.getName().endsWith(".wav")) {
							if ((this.customizationContainer.hoverSound == null) || !this.customizationContainer.hoverSound.equals(call)) {
								this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
							}

							this.customizationContainer.hoverSound = call;
						} else {
							LayoutEditorScreen.displayNotification("§c§l" + Locals.localize("helper.creator.invalidaudio.title"), "", Locals.localize("helper.creator.invalidaudio.desc"), "", "", "", "", "", "");
						}
					} else {
						if (this.customizationContainer.hoverSound != null) {
							this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
						}
						this.customizationContainer.hoverSound = null;
						this.rightclickMenu.closeMenu();
					}
				}
			}, "wav");

			if (this.customizationContainer.hoverSound != null) {
				cf.setText(this.customizationContainer.hoverSound);
			}
			PopupHandler.displayPopup(cf);
		});
		this.rightclickMenu.addContent(b7);

		//TODO übernehmen
//		AdvancedButton b8 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.hoversound.reset"), (press) -> {
//			this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
//
//			this.hoverSound = null;
//			this.handler.setVanillaHoverSound(this, null);
//			this.rightclickMenu.closeMenu();
//		});
//		this.rightclickMenu.addContent(b8);

		//TODO übernehmen
		AdvancedButton b11 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.clicksound"), (press) -> {
			ChooseFilePopup cf = new ChooseFilePopup((call) -> {
				if (call != null) {
					if (!call.replace(" ", "").equals("")) {
						File f = new File(call);
						if (f.exists() && f.isFile() && f.getName().endsWith(".wav")) {
							if ((this.customizationContainer.clickSound == null) || !this.customizationContainer.clickSound.equals(call)) {
								this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
							}

							this.customizationContainer.clickSound = call;
						} else {
							LayoutEditorScreen.displayNotification("§c§l" + Locals.localize("helper.creator.invalidaudio.title"), "", Locals.localize("helper.creator.invalidaudio.desc"), "", "", "", "", "", "");
						}
					} else {
						if (this.customizationContainer.clickSound != null) {
							this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
						}
						this.customizationContainer.clickSound = null;
						this.rightclickMenu.closeMenu();
					}
				}
			}, "wav");

			if (this.customizationContainer.clickSound != null) {
				cf.setText(this.customizationContainer.clickSound);
			}
			PopupHandler.displayPopup(cf);
		});
		this.rightclickMenu.addContent(b11);

		//TODO übernehmen
//		AdvancedButton b12 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.clicksound.reset"), (press) -> {
//			this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
//
//			this.clicksound = null;
//			this.handler.setVanillaClickSound(this, null);
//			this.rightclickMenu.closeMenu();
//		});
//		this.rightclickMenu.addContent(b12);

		//TODO übernehmen
		this.rightclickMenu.addSeparator();

		//TODO übernehmen
		AdvancedButton b9 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.vanillabutton.autoclick"), (press) -> {
			FMTextInputPopup pop = new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.vanillabutton.autoclick.popup"), CharacterFilter.getIntegerCharacterFiler(), 240, (call) -> {
				if (call != null) {
					if (call.replace(" ", "").equals("") || !MathUtils.isInteger(call)) {
						if (this.customizationContainer.autoButtonClicks != 0) {
							this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
						}

						this.customizationContainer.autoButtonClicks = 0;
					} else {
						if (this.customizationContainer.autoButtonClicks != Integer.parseInt(call)) {
							this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
						}

						this.customizationContainer.autoButtonClicks = Integer.parseInt(call);
					}
				}
			});
			pop.setText("" + this.customizationContainer.autoButtonClicks);
			PopupHandler.displayPopup(pop);
		});
		this.rightclickMenu.addContent(b9);

		//TODO übernehmen
		AdvancedButton b13 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.btndescription"), (press) -> {
			FMTextInputPopup in = new DynamicValueInputPopup(new Color(0, 0, 0, 0), Locals.localize("helper.creator.items.button.btndescription"), null, 240, (call) -> {
				if (call != null) {
					if (!call.replace(" ", "").equals("")) {
						if ((this.customizationContainer.buttonDescription == null) || !this.customizationContainer.buttonDescription.equals(call)) {
							this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
						}
						this.customizationContainer.buttonDescription = call;
					} else {
						if (this.customizationContainer.buttonDescription != null) {
							this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
						}
						this.customizationContainer.buttonDescription = null;
					}
				}
			});

			if (this.customizationContainer.buttonDescription != null) {
				in.setText(this.customizationContainer.buttonDescription);
			}
			PopupHandler.displayPopup(in);
		});
		List<String> l = new ArrayList<String>();
		for (String s : StringUtils.splitLines(Locals.localize("helper.creator.items.button.btndescription.desc"), "%n%")) {
			l.add(s.replace("#n#", "%n%"));
		}
		b13.setDescription(l.toArray(new String[0]));
		this.rightclickMenu.addContent(b13);

	}

	@Override
	public void render(MatrixStack matrix, int mouseX, int mouseY) {

		//TODO übernehmen
		if (this.customizationContainer.customButtonLabel != null) {
			this.object.value = StringUtils.convertFormatCodes(this.customizationContainer.customButtonLabel, "&", "§");
		} else {
			this.object.value = button.label;
		}

		if (!this.canBeModified()) {
			//Cancel dragging
			if (this.isDragged() && this.handler.isFocused(this) && ((this.startX != this.object.posX) || (this.startY != this.object.posY))) {
				this.handler.setObjectFocused(this, false, true);
				this.dragging = false;
				this.object.posX = this.button.x;
				this.object.posY = this.button.y;
				//TODO übernehmen
				this.object.setWidth(this.button.width);
				this.object.setHeight(this.button.height);
				//-------------------
				GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), normalCursor);
				this.displaySetOrientationNotification();
				return;
			}
			//Cancel resize
			if ((this.isGrabberPressed() || this.resizing) && !this.isDragged() && this.handler.isFocused(this)) {
				this.resizing = false;
				this.dragging = false;
				this.object.posX = this.button.x;
				this.object.posY = this.button.y;
				//TODO übernehmen
				this.object.setWidth(this.button.width);
				this.object.setHeight(this.button.height);
				//-------------------
				this.handler.setObjectFocused(this, false, true);
				GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), normalCursor);
				this.displaySetOrientationNotification();
				return;
			}
		}

        super.render(matrix, mouseX, mouseY);

        if (this.object.delayAppearance) {
			this.handler.vanillaDelayAppearance.put(this.button.getId(), this.object.delayAppearanceSec);
		} else {
			this.handler.vanillaDelayAppearance.remove(this.button.getId());
		}
		if (!this.object.delayAppearanceEverytime) {
			this.handler.vanillaDelayAppearanceFirstTime.put(this.button.getId(), true);
		} else {
			this.handler.vanillaDelayAppearanceFirstTime.put(this.button.getId(), false);
		}
		if (this.object.fadeIn) {
			this.handler.vanillaFadeIn.put(this.button.getId(), this.object.fadeInSpeed);
		} else {
			this.handler.vanillaFadeIn.remove(this.button.getId());
		}

	}

	@Override
	public boolean isGrabberPressed() {
		if (!this.canBeModified()) {
			return false;
		}
		return super.isGrabberPressed();
	}

	public String getButtonId() {
		if (this.button.getCompatibilityId() != null) {
			return this.button.getCompatibilityId();
		}
		return "" + this.button.getId();
	}

	@Override
	public List<PropertiesSection> getProperties() {
		List<PropertiesSection> l = new ArrayList<PropertiesSection>();

		//hidebutton
		if (this.customizationContainer.isButtonHidden) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "hidebutton");
			//TODO übernehmen
			s.addEntry("identifier", "%id=" + this.getButtonId() + "%");
			l.add(s);
		}
		//movebutton
		if (this.canBeModified()) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "movebutton");
			//TODO übernehmen
			s.addEntry("identifier", "%id=" + this.getButtonId() + "%");
			s.addEntry("orientation", this.object.orientation);
			if (this.object.orientation.equals("element") && (this.object.orientationElementIdentifier != null)) {
				s.addEntry("orientation_element", this.object.orientationElementIdentifier);
			}
			s.addEntry("x", "" + this.object.posX);
			s.addEntry("y", "" + this.object.posY);
			l.add(s);
		}
		// resizebutton
		if (this.canBeModified() && ((this.getWidth() != this.button.width) || (this.getHeight() != this.button.height))) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "resizebutton");
			//TODO übernehmen
			s.addEntry("identifier", "%id=" + this.getButtonId() + "%");
			s.addEntry("width", "" + this.object.getWidth());
			s.addEntry("height", "" + this.object.getHeight());
			l.add(s);
		}
		// renamebutton
		if (this.customizationContainer.customButtonLabel != null) {
			if (!this.customizationContainer.customButtonLabel.equals(this.button.label)) {
				PropertiesSection s = new PropertiesSection("customization");
				s.addEntry("action", "setbuttonlabel");
				//TODO übernehmen
				s.addEntry("identifier", "%id=" + this.getButtonId() + "%");
				s.addEntry("value", this.customizationContainer.customButtonLabel);
				l.add(s);
			}
		}
		// setbuttontexture
		if ((this.customizationContainer.normalBackground != null) || (this.customizationContainer.hoverBackground != null)) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "setbuttontexture");
			//TODO übernehmen
			s.addEntry("identifier", "%id=" + this.getButtonId() + "%");
			if (this.customizationContainer.normalBackground != null) {
				if (this.customizationContainer.normalBackground.startsWith("animation:")) {
					String aniName = this.customizationContainer.normalBackground.split("[:]", 2)[1];
					s.addEntry("backgroundanimationnormal", aniName);
				} else {
					s.addEntry("backgroundnormal", this.customizationContainer.normalBackground);
				}
			}
			if (this.customizationContainer.hoverBackground != null) {
				if (this.customizationContainer.hoverBackground.startsWith("animation:")) {
					String aniName = this.customizationContainer.hoverBackground.split("[:]", 2)[1];
					s.addEntry("backgroundanimationhovered", aniName);
				} else {
					s.addEntry("backgroundhovered", this.customizationContainer.hoverBackground);
				}
			}
			s.addEntry("restartbackgroundanimations", "" + this.customizationContainer.restartAnimationOnHover);
			s.addEntry("loopbackgroundanimations", "" + this.customizationContainer.loopAnimation);
			l.add(s);
		}
		// clickbutton
		if (this.customizationContainer.autoButtonClicks > 0) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "clickbutton");
			//TODO übernehmen
			s.addEntry("identifier", "%id=" + this.getButtonId() + "%");
			s.addEntry("clicks", "" + this.customizationContainer.autoButtonClicks);
			l.add(s);
		}
		//addhoversound
		if (this.customizationContainer.hoverSound != null) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "addhoversound");
			//TODO übernehmen
			s.addEntry("identifier", "%id=" + this.getButtonId() + "%");
			s.addEntry("path", this.customizationContainer.hoverSound);
			l.add(s);
		}
		//sethoverlabel
		if (this.customizationContainer.hoverLabel != null) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "sethoverlabel");
			//TODO übernehmen
			s.addEntry("identifier", "%id=" + this.getButtonId() + "%");
			s.addEntry("label", this.customizationContainer.hoverLabel);
			l.add(s);
		}
		//hidebuttonfor
		if (this.object.delayAppearance) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "hidebuttonfor");
			//TODO übernehmen
			s.addEntry("identifier", "%id=" + this.getButtonId() + "%");
			s.addEntry("seconds", "" + this.object.delayAppearanceSec);
			s.addEntry("onlyfirsttime", "" + !this.object.delayAppearanceEverytime);
			if (this.object.fadeIn) {
				s.addEntry("fadein", "true");
				s.addEntry("fadeinspeed", "" + this.object.fadeInSpeed);
			}
			l.add(s);
		}
		//setbuttonclicksound
		if (this.customizationContainer.clickSound != null) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "setbuttonclicksound");
			//TODO übernehmen
			s.addEntry("identifier", "%id=" + this.getButtonId() + "%");
			s.addEntry("path", this.customizationContainer.clickSound);
			l.add(s);
		}
		//setbuttondescription
		if (this.customizationContainer.buttonDescription != null) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "setbuttondescription");
			//TODO übernehmen
			s.addEntry("identifier", "%id=" + this.getButtonId() + "%");
			s.addEntry("description", this.customizationContainer.buttonDescription);
			l.add(s);
		}

		//Visibility Requirements
		PropertiesSection visReqs = new PropertiesSection("customization");
		visReqs.addEntry("action", "vanilla_button_visibility_requirements");
		//TODO übernehmen
		visReqs.addEntry("identifier", "%id=" + this.getButtonId() + "%");
		this.addVisibilityPropertiesTo(visReqs);
		if (visReqs.getEntries().size() > 2) {
			l.add(visReqs);
		}

		return l;
	}

	public void displaySetOrientationNotification() {
		if (FancyMenu.config.getOrDefault("showvanillamovewarning", true)) {
			PopupHandler.displayPopup(new VanillaButtonMovePopup(this.handler));
		}
	}
	
	private boolean canBeModified() {
		return !this.object.orientation.equals("original");
	}

	//TODO übernehmen
//	private void editLabelCallback(String text) {
//		if (text == null) {
//			return;
//		} else {
//			this.handler.setVanillaButtonName(this, StringUtils.convertFormatCodes(text, "&", "§"));
//		}
//	}

	@Override
	public void destroyObject() {
		if (FancyMenu.config.getOrDefault("editordeleteconfirmation", true)) {
			PopupHandler.displayPopup(new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
				if (call) {
					
					this.handler.hideVanillaButton(this);
					
				}
			}, "§c§l" + Locals.localize("helper.creator.messages.sure"), "", Locals.localize("helper.creator.deleteobject"), "", "", "", "", ""));
		} else {

			this.handler.hideVanillaButton(this);
			
		}
	}
	
}
