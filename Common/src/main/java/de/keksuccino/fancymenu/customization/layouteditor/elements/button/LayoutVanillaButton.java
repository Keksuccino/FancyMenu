package de.keksuccino.fancymenu.customization.layouteditor.elements.button;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layouteditor.LayoutEditorScreen;
import de.keksuccino.fancymenu.rendering.ui.popup.FMNotificationPopup;
import de.keksuccino.fancymenu.rendering.ui.texteditor.TextEditorScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.button.ButtonData;
import de.keksuccino.fancymenu.customization.layouteditor.elements.ChooseFilePopup;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.rendering.ui.popup.FMTextInputPopup;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.Minecraft;

public class LayoutVanillaButton extends AbstractEditorElement {

	public final ButtonData button;
	public final ScreenCustomizationLayer.ButtonCustomizationContainer customizationContainer;

	public LayoutVanillaButton(ScreenCustomizationLayer.ButtonCustomizationContainer customizationContainer, ButtonData button, LayoutEditorScreen handler) {
		super(new LayoutButtonDummyCustomizationItem(customizationContainer, button.label, button.width, button.height, button.x, button.y), true, handler, false);
		this.button = button;
		this.element.anchorPoint = "original";
		this.customizationContainer = customizationContainer;
		
		this.element.loadingRequirementContainer = this.customizationContainer.loadingRequirementContainer;
		
		this.init();
	}

	@Override
	public void init() {
		this.orderable = false;
		this.copyable = false;

		super.init();

		AdvancedButton b0 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.vanillabutton.resetorientation"), (press) -> {
			if (!this.element.anchorPoint.equals("original")) {
				this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
			}

			this.element.anchorPoint = "original";
			this.element.rawX = this.button.x;
			this.element.rawY = this.button.y;
			this.element.setWidth(this.button.width);
			this.element.setHeight(this.button.height);
			this.rightClickContextMenu.closeMenu();
			this.editor.postRenderTasks.add(new Runnable() {
				@Override
				public void run() {
					Minecraft.getInstance().setScreen(LayoutVanillaButton.this.editor);
				}
			});
		});
		this.rightClickContextMenu.addContent(b0);

		this.rightClickContextMenu.addSeparator();

		AdvancedButton buttonBackgroundButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground"), (press) -> {
			ButtonBackgroundPopup pop = new ButtonBackgroundPopup(this.editor, this.customizationContainer);
			PopupHandler.displayPopup(pop);
		});
		this.rightClickContextMenu.addContent(buttonBackgroundButton);

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
		this.rightClickContextMenu.addContent(loopBackgroundAnimationButton);

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
		this.rightClickContextMenu.addContent(restartAnimationOnHoverButton);

		this.rightClickContextMenu.addSeparator();

		AdvancedButton b2 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.editlabel"), (press) -> {
			
			TextEditorScreen s = new TextEditorScreen(Component.literal(Locals.localize("helper.creator.items.button.editlabel")), this.editor, null, (call) -> {
				if (call != null) {
					if ((this.customizationContainer.customButtonLabel == null) || !this.customizationContainer.customButtonLabel.equals(call)) {
						this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
					}
					this.customizationContainer.customButtonLabel = call;
				}
			});
			s.multilineMode = false;
			s.setText(StringUtils.convertFormatCodes(this.element.value, "§", "&"));
			Minecraft.getInstance().setScreen(s);
			
		});
		this.rightClickContextMenu.addContent(b2);

		AdvancedButton b3 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.resetlabel"), (press) -> {
			if (this.customizationContainer.customButtonLabel != null) {
				this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
			}
			this.customizationContainer.customButtonLabel = null;
			this.rightClickContextMenu.closeMenu();
		});
		this.rightClickContextMenu.addContent(b3);

		this.rightClickContextMenu.addSeparator();

		AdvancedButton b5 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.hoverlabel"), (press) -> {
			
			TextEditorScreen s = new TextEditorScreen(Component.literal(Locals.localize("helper.creator.items.button.hoverlabel")), this.editor, null, (call) -> {
				if (call != null) {
					if ((this.customizationContainer.hoverLabel == null) || !this.customizationContainer.hoverLabel.equals(StringUtils.convertFormatCodes(call, "&", "§"))) {
						this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
					}
					this.customizationContainer.hoverLabel = StringUtils.convertFormatCodes(call, "&", "§");
				}
			});
			s.multilineMode = false;
			if (this.customizationContainer.hoverLabel != null) {
				s.setText(StringUtils.convertFormatCodes(this.customizationContainer.hoverLabel, "§", "&"));
			}
			Minecraft.getInstance().setScreen(s);
			
		});
		this.rightClickContextMenu.addContent(b5);

		AdvancedButton b6 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.hoverlabel.reset"), (press) -> {
			if (this.customizationContainer.hoverLabel != null) {
				this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
			}
			this.customizationContainer.hoverLabel = null;
			this.rightClickContextMenu.closeMenu();
			this.rightClickContextMenu.closeMenu();
		});
		this.rightClickContextMenu.addContent(b6);

		this.rightClickContextMenu.addSeparator();

		AdvancedButton b7 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.hoversound"), (press) -> {
			ChooseFilePopup cf = new ChooseFilePopup((call) -> {
				if (call != null) {
					if (!call.replace(" ", "").equals("")) {
						File f = new File(call);
						if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
							f = new File(Minecraft.getInstance().gameDirectory, call);
						}
						if (f.exists() && f.isFile() && f.getName().endsWith(".wav")) {
							if ((this.customizationContainer.hoverSound == null) || !this.customizationContainer.hoverSound.equals(call)) {
								this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
							}

							this.customizationContainer.hoverSound = call;
						} else {
							LayoutEditorScreen.displayNotification("§c§l" + Locals.localize("helper.creator.invalidaudio.title"), "", Locals.localize("helper.creator.invalidaudio.desc"), "", "", "", "", "", "");
						}
					} else {
						if (this.customizationContainer.hoverSound != null) {
							this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
						}
						this.customizationContainer.hoverSound = null;
						this.rightClickContextMenu.closeMenu();
					}
				}
			}, "wav");

			if (this.customizationContainer.hoverSound != null) {
				cf.setText(this.customizationContainer.hoverSound);
			}
			PopupHandler.displayPopup(cf);
		});
		this.rightClickContextMenu.addContent(b7);

		AdvancedButton b11 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.clicksound"), (press) -> {
			ChooseFilePopup cf = new ChooseFilePopup((call) -> {
				if (call != null) {
					if (!call.replace(" ", "").equals("")) {
						File f = new File(call);
						if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
							f = new File(Minecraft.getInstance().gameDirectory, call);
						}
						if (f.exists() && f.isFile() && f.getName().endsWith(".wav")) {
							if ((this.customizationContainer.clickSound == null) || !this.customizationContainer.clickSound.equals(call)) {
								this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
							}

							this.customizationContainer.clickSound = call;
						} else {
							LayoutEditorScreen.displayNotification("§c§l" + Locals.localize("helper.creator.invalidaudio.title"), "", Locals.localize("helper.creator.invalidaudio.desc"), "", "", "", "", "", "");
						}
					} else {
						if (this.customizationContainer.clickSound != null) {
							this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
						}
						this.customizationContainer.clickSound = null;
						this.rightClickContextMenu.closeMenu();
					}
				}
			}, "wav");

			if (this.customizationContainer.clickSound != null) {
				cf.setText(this.customizationContainer.clickSound);
			}
			PopupHandler.displayPopup(cf);
		});
		this.rightClickContextMenu.addContent(b11);

		this.rightClickContextMenu.addSeparator();

		AdvancedButton b9 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.vanillabutton.autoclick"), (press) -> {
			FMTextInputPopup pop = new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.vanillabutton.autoclick.popup"), CharacterFilter.getIntegerCharacterFiler(), 240, (call) -> {
				if (call != null) {
					if (call.replace(" ", "").equals("") || !MathUtils.isInteger(call)) {
						if (this.customizationContainer.autoButtonClicks != 0) {
							this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
						}

						this.customizationContainer.autoButtonClicks = 0;
					} else {
						if (this.customizationContainer.autoButtonClicks != Integer.parseInt(call)) {
							this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
						}

						this.customizationContainer.autoButtonClicks = Integer.parseInt(call);
					}
				}
			});
			pop.setText("" + this.customizationContainer.autoButtonClicks);
			PopupHandler.displayPopup(pop);
		});
		this.rightClickContextMenu.addContent(b9);

		AdvancedButton b13 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.btndescription"), (press) -> {
			
			TextEditorScreen s = new TextEditorScreen(Component.literal(Locals.localize("helper.creator.items.button.btndescription")), this.editor, null, (call) -> {
				if (call != null) {
					call = call.replace("\n", "%n%");
					if (!call.replace(" ", "").equals("")) {
						if ((this.customizationContainer.buttonDescription == null) || !this.customizationContainer.buttonDescription.equals(call)) {
							this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
						}
						this.customizationContainer.buttonDescription = call;
					} else {
						if (this.customizationContainer.buttonDescription != null) {
							this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
						}
						this.customizationContainer.buttonDescription = null;
					}
				}
			});
			if (this.customizationContainer.buttonDescription != null) {
				s.setText(this.customizationContainer.buttonDescription.replace("%n%", "\n"));
			}
			Minecraft.getInstance().setScreen(s);
			
		});
		List<String> l = new ArrayList<String>();
		for (String s : StringUtils.splitLines(Locals.localize("helper.creator.items.button.btndescription.desc"), "%n%")) {
			l.add(s.replace("#n#", "%n%"));
		}
		b13.setDescription(l.toArray(new String[0]));
		this.rightClickContextMenu.addContent(b13);

	}

	@Override
	public void render(PoseStack matrix, int mouseX, int mouseY) {

		if (this.customizationContainer.customButtonLabel != null) {
			this.element.value = StringUtils.convertFormatCodes(this.customizationContainer.customButtonLabel, "&", "§");
		} else {
			this.element.value = button.label;
		}

		if (!this.canBeModified()) {
			//Cancel dragging
			if (this.isDragged() && this.editor.isFocused(this) && ((this.startX != this.element.rawX) || (this.startY != this.element.rawY))) {
				this.editor.setObjectFocused(this, false, true);
				this.dragging = false;
				this.element.rawX = this.button.x;
				this.element.rawY = this.button.y;
				this.element.setWidth(this.button.width);
				this.element.setHeight(this.button.height);
				GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), CURSOR_NORMAL);
				this.displaySetOrientationNotification();
				return;
			}
			//Cancel resize
			if ((this.isGrabberPressed() || this.resizing) && !this.isDragged() && this.editor.isFocused(this)) {
				this.resizing = false;
				this.dragging = false;
				this.element.rawX = this.button.x;
				this.element.rawY = this.button.y;
				this.element.setWidth(this.button.width);
				this.element.setHeight(this.button.height);
				this.editor.setObjectFocused(this, false, true);
				GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), CURSOR_NORMAL);
				this.displaySetOrientationNotification();
				return;
			}
		}

		super.render(matrix, mouseX, mouseY);

		if (this.element.delayAppearance) {
			this.editor.vanillaDelayAppearance.put(this.button.getId(), this.element.delayAppearanceSec);
		} else {
			this.editor.vanillaDelayAppearance.remove(this.button.getId());
		}
		if (!this.element.delayAppearanceEverytime) {
			this.editor.vanillaDelayAppearanceFirstTime.put(this.button.getId(), true);
		} else {
			this.editor.vanillaDelayAppearanceFirstTime.put(this.button.getId(), false);
		}
		if (this.element.fadeIn) {
			this.editor.vanillaFadeIn.put(this.button.getId(), this.element.fadeInSpeed);
		} else {
			this.editor.vanillaFadeIn.remove(this.button.getId());
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
			s.addEntry("identifier", "%id=" + this.getButtonId() + "%");
			l.add(s);
		}
		//movebutton
		if (this.canBeModified()) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "movebutton");
			s.addEntry("identifier", "%id=" + this.getButtonId() + "%");
			s.addEntry("orientation", this.element.anchorPoint);
			if (this.element.anchorPoint.equals("element") && (this.element.anchorPointElementIdentifier != null)) {
				s.addEntry("orientation_element", this.element.anchorPointElementIdentifier);
			}
			s.addEntry("x", "" + this.element.rawX);
			s.addEntry("y", "" + this.element.rawY);
			if (this.element.advancedX != null) {
				s.addEntry("advanced_posx", this.element.advancedX);
			}
			if (this.element.advancedY != null) {
				s.addEntry("advanced_posy", this.element.advancedY);
			}
			l.add(s);
		}

		// resizebutton
		if (this.canBeModified() && ((this.getWidth() != this.button.width) || (this.getHeight() != this.button.height) || (this.element.advancedHeight != null) || (this.element.advancedWidth != null))) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "resizebutton");
			s.addEntry("identifier", "%id=" + this.getButtonId() + "%");
			s.addEntry("width", "" + this.element.width);
			s.addEntry("height", "" + this.element.height);
			if (this.element.advancedWidth != null) {
				s.addEntry("advanced_width", this.element.advancedWidth);
			}
			if (this.element.advancedHeight != null) {
				s.addEntry("advanced_height", this.element.advancedHeight);
			}
			l.add(s);
		}

		// renamebutton
		if (this.customizationContainer.customButtonLabel != null) {
			if (!this.customizationContainer.customButtonLabel.equals(this.button.label)) {
				PropertiesSection s = new PropertiesSection("customization");
				s.addEntry("action", "setbuttonlabel");
				s.addEntry("identifier", "%id=" + this.getButtonId() + "%");
				s.addEntry("value", this.customizationContainer.customButtonLabel);
				l.add(s);
			}
		}
		// setbuttontexture
		if ((this.customizationContainer.normalBackground != null) || (this.customizationContainer.hoverBackground != null)) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "setbuttontexture");
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
			s.addEntry("identifier", "%id=" + this.getButtonId() + "%");
			s.addEntry("clicks", "" + this.customizationContainer.autoButtonClicks);
			l.add(s);
		}
		//addhoversound
		if (this.customizationContainer.hoverSound != null) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "addhoversound");
			s.addEntry("identifier", "%id=" + this.getButtonId() + "%");
			s.addEntry("path", this.customizationContainer.hoverSound);
			l.add(s);
		}
		//sethoverlabel
		if (this.customizationContainer.hoverLabel != null) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "sethoverlabel");
			s.addEntry("identifier", "%id=" + this.getButtonId() + "%");
			s.addEntry("label", this.customizationContainer.hoverLabel);
			l.add(s);
		}
		//hidebuttonfor
		if (this.element.delayAppearance) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "hidebuttonfor");
			s.addEntry("identifier", "%id=" + this.getButtonId() + "%");
			s.addEntry("seconds", "" + this.element.delayAppearanceSec);
			s.addEntry("onlyfirsttime", "" + !this.element.delayAppearanceEverytime);
			if (this.element.fadeIn) {
				s.addEntry("fadein", "true");
				s.addEntry("fadeinspeed", "" + this.element.fadeInSpeed);
			}
			l.add(s);
		}
		//setbuttonclicksound
		if (this.customizationContainer.clickSound != null) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "setbuttonclicksound");
			s.addEntry("identifier", "%id=" + this.getButtonId() + "%");
			s.addEntry("path", this.customizationContainer.clickSound);
			l.add(s);
		}
		//setbuttondescription
		if (this.customizationContainer.buttonDescription != null) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "setbuttondescription");
			s.addEntry("identifier", "%id=" + this.getButtonId() + "%");
			s.addEntry("description", this.customizationContainer.buttonDescription);
			l.add(s);
		}

		//Visibility Requirements
		PropertiesSection visReqs = new PropertiesSection("customization");
		visReqs.addEntry("action", "vanilla_button_visibility_requirements");
		visReqs.addEntry("identifier", "%id=" + this.getButtonId() + "%");
		
		this.serializeLoadingRequirementsTo(visReqs);
		if (visReqs.getEntries().size() > 2) {
			l.add(visReqs);
		}

		return l;
	}

	public void displaySetOrientationNotification() {
		if (FancyMenu.getConfig().getOrDefault("showvanillamovewarning", true)) {
			FMNotificationPopup p = new FMNotificationPopup(300, new Color(0,0,0,0), 240, null, StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.element.vanilla.orientation_needed"), "%n%"));
			PopupHandler.displayPopup(p);
		}
	}

	private boolean canBeModified() {
		return !this.element.anchorPoint.equals("original");
	}

	@Override
	public void destroyElement() {
		if (FancyMenu.getConfig().getOrDefault("editordeleteconfirmation", true)) {
			FMNotificationPopup pop = new FMNotificationPopup(300, new Color(0,0,0,0), 240, null, StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.element.vanilla.delete.confirm"), "%n%"));
			PopupHandler.displayPopup(pop);
		}
		this.editor.hideVanillaButton(this);
	}

}
