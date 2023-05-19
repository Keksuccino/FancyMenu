//package de.keksuccino.fancymenu.customization.layout.editor.elements.button;
//
//import java.awt.Color;
//import java.io.File;
//import java.util.ArrayList;
//import java.util.List;
//
//import com.mojang.blaze3d.vertex.PoseStack;
//import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
//import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
//import de.keksuccino.fancymenu.rendering.ui.popup.FMNotificationPopup;
//import de.keksuccino.fancymenu.rendering.ui.texteditor.TextEditorScreen;
//import net.minecraft.network.chat.Component;
//import org.lwjgl.glfw.GLFW;
//
//import net.minecraft.client.resources.language.I18n;
//import de.keksuccino.fancymenu.FancyMenu;
//import de.keksuccino.fancymenu.customization.button.ButtonData;
//import de.keksuccino.fancymenu.customization.layout.editor.elements.ChooseFilePopup;
//import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
//import de.keksuccino.fancymenu.rendering.ui.popup.FMTextInputPopup;
//import de.keksuccino.konkrete.gui.content.AdvancedButton;
//import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
//import de.keksuccino.konkrete.input.CharacterFilter;
//import de.keksuccino.fancymenu.utils.LocalizationUtils;
//import de.keksuccino.konkrete.math.MathUtils;
//import de.keksuccino.fancymenu.properties.PropertyContainer;
//import net.minecraft.client.Minecraft;
//
//public class LayoutVanillaButton extends AbstractEditorElement {
//
//	public final ButtonData button;
//	public final ScreenCustomizationLayer.ButtonCustomizationContainer customizationContainer;
//
//	public LayoutVanillaButton(ScreenCustomizationLayer.ButtonCustomizationContainer customizationContainer, ButtonData button, LayoutEditorScreen handler) {
//		super(new LayoutButtonDummyCustomizationItem(customizationContainer, button.label, button.width, button.height, button.x, button.y), true, handler, false);
//		this.button = button;
//		this.element.anchorPoint = "original";
//		this.customizationContainer = customizationContainer;
//
//		this.element.loadingRequirementContainer = this.customizationContainer.loadingRequirementContainer;
//
//		this.init();
//	}
//
//	@Override
//	public void init() {
//		this.orderable = false;
//		this.copyable = false;
//
//		super.init();
//
//		AdvancedButton b0 = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.items.vanillabutton.resetorientation"), (press) -> {
//			if (!this.element.anchorPoint.equals("original")) {
//				this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//			}
//
//			this.element.anchorPoint = "original";
//			this.element.baseX = this.button.x;
//			this.element.baseY = this.button.y;
//			this.element.setWidth(this.button.width);
//			this.element.setHeight(this.button.height);
//			this.rightClickContextMenu.closeMenu();
//			this.editor.postRenderTasks.add(new Runnable() {
//				@Override
//				public void run() {
//					Minecraft.getInstance().setScreen(LayoutVanillaButton.this.editor);
//				}
//			});
//		});
//		this.rightClickContextMenu.addContent(b0);
//
//		this.rightClickContextMenu.addSeparator();
//
//		AdvancedButton buttonBackgroundButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.helper.editor.items.buttons.buttonbackground"), (press) -> {
//			ButtonBackgroundPopup pop = new ButtonBackgroundPopup(this.editor, this.customizationContainer);
//			PopupHandler.displayPopup(pop);
//		});
//		this.rightClickContextMenu.addContent(buttonBackgroundButton);
//
//		String loopAniLabel = I18n.get("fancymenu.helper.editor.items.buttons.buttonbackground.loopanimation.on");
//		if (!this.customizationContainer.loopAnimation) {
//			loopAniLabel = I18n.get("fancymenu.helper.editor.items.buttons.buttonbackground.loopanimation.off");
//		}
//		AdvancedButton loopBackgroundAnimationButton = new AdvancedButton(0, 0, 0, 0, loopAniLabel, (press) -> {
//			if (this.customizationContainer.loopAnimation) {
//				this.customizationContainer.loopAnimation = false;
//				((AdvancedButton)press).setMessage(I18n.get("fancymenu.helper.editor.items.buttons.buttonbackground.loopanimation.off"));
//			} else {
//				this.customizationContainer.loopAnimation = true;
//				((AdvancedButton)press).setMessage(I18n.get("fancymenu.helper.editor.items.buttons.buttonbackground.loopanimation.on"));
//			}
//		});
//		loopBackgroundAnimationButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.buttons.buttonbackground.loopanimation.btn.desc")));
//		this.rightClickContextMenu.addContent(loopBackgroundAnimationButton);
//
//		String restartAniLabel = I18n.get("fancymenu.helper.editor.items.buttons.buttonbackground.restartonhover.on");
//		if (!this.customizationContainer.restartAnimationOnHover) {
//			restartAniLabel = I18n.get("fancymenu.helper.editor.items.buttons.buttonbackground.restartonhover.off");
//		}
//		AdvancedButton restartAnimationOnHoverButton = new AdvancedButton(0, 0, 0, 0, restartAniLabel, (press) -> {
//			if (this.customizationContainer.restartAnimationOnHover) {
//				this.customizationContainer.restartAnimationOnHover = false;
//				((AdvancedButton)press).setMessage(I18n.get("fancymenu.helper.editor.items.buttons.buttonbackground.restartonhover.off"));
//			} else {
//				this.customizationContainer.restartAnimationOnHover = true;
//				((AdvancedButton)press).setMessage(I18n.get("fancymenu.helper.editor.items.buttons.buttonbackground.restartonhover.on"));
//			}
//		});
//		restartAnimationOnHoverButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.buttons.buttonbackground.restartonhover.btn.desc")));
//		this.rightClickContextMenu.addContent(restartAnimationOnHoverButton);
//
//		this.rightClickContextMenu.addSeparator();
//
//		AdvancedButton b2 = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.items.button.editlabel"), (press) -> {
//
//			TextEditorScreen s = new TextEditorScreen(Component.literal(I18n.get("fancymenu.editor.items.button.editlabel")), this.editor, null, (call) -> {
//				if (call != null) {
//					if ((this.customizationContainer.customButtonLabel == null) || !this.customizationContainer.customButtonLabel.equals(call)) {
//						this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//					}
//					this.customizationContainer.customButtonLabel = call;
//				}
//			});
//			s.multilineMode = false;
//			s.setText(StringUtils.convertFormatCodes(this.element.value, "§", "&"));
//			Minecraft.getInstance().setScreen(s);
//
//		});
//		this.rightClickContextMenu.addContent(b2);
//
//		AdvancedButton b3 = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.items.button.resetlabel"), (press) -> {
//			if (this.customizationContainer.customButtonLabel != null) {
//				this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//			}
//			this.customizationContainer.customButtonLabel = null;
//			this.rightClickContextMenu.closeMenu();
//		});
//		this.rightClickContextMenu.addContent(b3);
//
//		this.rightClickContextMenu.addSeparator();
//
//		AdvancedButton b5 = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.items.button.hoverlabel"), (press) -> {
//
//			TextEditorScreen s = new TextEditorScreen(Component.literal(I18n.get("fancymenu.editor.items.button.hoverlabel")), this.editor, null, (call) -> {
//				if (call != null) {
//					if ((this.customizationContainer.hoverLabel == null) || !this.customizationContainer.hoverLabel.equals(StringUtils.convertFormatCodes(call, "&", "§"))) {
//						this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//					}
//					this.customizationContainer.hoverLabel = StringUtils.convertFormatCodes(call, "&", "§");
//				}
//			});
//			s.multilineMode = false;
//			if (this.customizationContainer.hoverLabel != null) {
//				s.setText(StringUtils.convertFormatCodes(this.customizationContainer.hoverLabel, "§", "&"));
//			}
//			Minecraft.getInstance().setScreen(s);
//
//		});
//		this.rightClickContextMenu.addContent(b5);
//
//		AdvancedButton b6 = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.items.button.hoverlabel.reset"), (press) -> {
//			if (this.customizationContainer.hoverLabel != null) {
//				this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//			}
//			this.customizationContainer.hoverLabel = null;
//			this.rightClickContextMenu.closeMenu();
//			this.rightClickContextMenu.closeMenu();
//		});
//		this.rightClickContextMenu.addContent(b6);
//
//		this.rightClickContextMenu.addSeparator();
//
//		AdvancedButton b7 = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.items.button.hoversound"), (press) -> {
//			ChooseFilePopup cf = new ChooseFilePopup((call) -> {
//				if (call != null) {
//					if (!call.replace(" ", "").equals("")) {
//						File f = new File(call);
//						if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
//							f = new File(Minecraft.getInstance().gameDirectory, call);
//						}
//						if (f.exists() && f.isFile() && f.getName().endsWith(".wav")) {
//							if ((this.customizationContainer.hoverSound == null) || !this.customizationContainer.hoverSound.equals(call)) {
//								this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//							}
//
//							this.customizationContainer.hoverSound = call;
//						} else {
//							UIBase.displayNotification("§c§l" + I18n.get("fancymenu.editor.invalidaudio.title"), "", I18n.get("fancymenu.editor.invalidaudio.desc"), "", "", "", "", "", "");
//						}
//					} else {
//						if (this.customizationContainer.hoverSound != null) {
//							this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//						}
//						this.customizationContainer.hoverSound = null;
//						this.rightClickContextMenu.closeMenu();
//					}
//				}
//			}, "wav");
//
//			if (this.customizationContainer.hoverSound != null) {
//				cf.setText(this.customizationContainer.hoverSound);
//			}
//			PopupHandler.displayPopup(cf);
//		});
//		this.rightClickContextMenu.addContent(b7);
//
//		AdvancedButton b11 = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.items.button.clicksound"), (press) -> {
//			ChooseFilePopup cf = new ChooseFilePopup((call) -> {
//				if (call != null) {
//					if (!call.replace(" ", "").equals("")) {
//						File f = new File(call);
//						if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
//							f = new File(Minecraft.getInstance().gameDirectory, call);
//						}
//						if (f.exists() && f.isFile() && f.getName().endsWith(".wav")) {
//							if ((this.customizationContainer.clickSound == null) || !this.customizationContainer.clickSound.equals(call)) {
//								this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//							}
//
//							this.customizationContainer.clickSound = call;
//						} else {
//							UIBase.displayNotification("§c§l" + I18n.get("fancymenu.editor.invalidaudio.title"), "", I18n.get("fancymenu.editor.invalidaudio.desc"), "", "", "", "", "", "");
//						}
//					} else {
//						if (this.customizationContainer.clickSound != null) {
//							this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//						}
//						this.customizationContainer.clickSound = null;
//						this.rightClickContextMenu.closeMenu();
//					}
//				}
//			}, "wav");
//
//			if (this.customizationContainer.clickSound != null) {
//				cf.setText(this.customizationContainer.clickSound);
//			}
//			PopupHandler.displayPopup(cf);
//		});
//		this.rightClickContextMenu.addContent(b11);
//
//		this.rightClickContextMenu.addSeparator();
//
//		AdvancedButton b9 = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.vanillabutton.autoclick"), (press) -> {
//			FMTextInputPopup pop = new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + I18n.get("fancymenu.editor.vanillabutton.autoclick.popup"), CharacterFilter.getIntegerCharacterFiler(), 240, (call) -> {
//				if (call != null) {
//					if (call.replace(" ", "").equals("") || !MathUtils.isInteger(call)) {
//						if (this.customizationContainer.autoButtonClicks != 0) {
//							this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//						}
//
//						this.customizationContainer.autoButtonClicks = 0;
//					} else {
//						if (this.customizationContainer.autoButtonClicks != Integer.parseInt(call)) {
//							this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//						}
//
//						this.customizationContainer.autoButtonClicks = Integer.parseInt(call);
//					}
//				}
//			});
//			pop.setText("" + this.customizationContainer.autoButtonClicks);
//			PopupHandler.displayPopup(pop);
//		});
//		this.rightClickContextMenu.addContent(b9);
//
//		AdvancedButton b13 = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.items.button.btndescription"), (press) -> {
//
//			TextEditorScreen s = new TextEditorScreen(Component.literal(I18n.get("fancymenu.editor.items.button.btndescription")), this.editor, null, (call) -> {
//				if (call != null) {
//					call = call.replace("\n");
//					if (!call.replace(" ", "").equals("")) {
//						if ((this.customizationContainer.buttonDescription == null) || !this.customizationContainer.buttonDescription.equals(call)) {
//							this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//						}
//						this.customizationContainer.buttonDescription = call;
//					} else {
//						if (this.customizationContainer.buttonDescription != null) {
//							this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//						}
//						this.customizationContainer.buttonDescription = null;
//					}
//				}
//			});
//			if (this.customizationContainer.buttonDescription != null) {
//				s.setText(this.customizationContainer.buttonDescription.replace("%n%", "\n"));
//			}
//			Minecraft.getInstance().setScreen(s);
//
//		});
//		List<String> l = new ArrayList<String>();
//		for (String s : LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.editor.items.button.btndescription.desc"))) {
//			l.add(s.replace("#n#"));
//		}
//		b13.setDescription(l.toArray(new String[0]));
//		this.rightClickContextMenu.addContent(b13);
//
//	}
//
//	@Override
//	public void render(PoseStack matrix, int mouseX, int mouseY) {
//
//		if (this.customizationContainer.customButtonLabel != null) {
//			this.element.value = StringUtils.convertFormatCodes(this.customizationContainer.customButtonLabel, "&", "§");
//		} else {
//			this.element.value = button.label;
//		}
//
//		if (!this.canBeModified()) {
//			//Cancel dragging
//			if (this.isDragged() && this.editor.isFocused(this) && ((this.startX != this.element.baseX) || (this.startY != this.element.baseY))) {
//				this.editor.setObjectFocused(this, false, true);
//				this.dragging = false;
//				this.element.baseX = this.button.x;
//				this.element.baseY = this.button.y;
//				this.element.setWidth(this.button.width);
//				this.element.setHeight(this.button.height);
//				GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), CURSOR_NORMAL);
//				this.displaySetOrientationNotification();
//				return;
//			}
//			//Cancel resize
//			if ((this.isGrabberPressed() || this.resizing) && !this.isDragged() && this.editor.isFocused(this)) {
//				this.resizing = false;
//				this.dragging = false;
//				this.element.baseX = this.button.x;
//				this.element.baseY = this.button.y;
//				this.element.setWidth(this.button.width);
//				this.element.setHeight(this.button.height);
//				this.editor.setObjectFocused(this, false, true);
//				GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), CURSOR_NORMAL);
//				this.displaySetOrientationNotification();
//				return;
//			}
//		}
//
//		super.render(matrix, mouseX, mouseY);
//
//		if (this.element.delayAppearance) {
//			this.editor.vanillaDelayAppearance.put(this.button.getId(), this.element.appearanceDelayInSeconds);
//		} else {
//			this.editor.vanillaDelayAppearance.remove(this.button.getId());
//		}
//		if (!this.element.delayAppearanceEverytime) {
//			this.editor.vanillaDelayAppearanceFirstTime.put(this.button.getId(), true);
//		} else {
//			this.editor.vanillaDelayAppearanceFirstTime.put(this.button.getId(), false);
//		}
//		if (this.element.fadeIn) {
//			this.editor.vanillaFadeIn.put(this.button.getId(), this.element.fadeInSpeed);
//		} else {
//			this.editor.vanillaFadeIn.remove(this.button.getId());
//		}
//
//	}
//
//	@Override
//	public boolean isGrabberPressed() {
//		if (!this.canBeModified()) {
//			return false;
//		}
//		return super.isGrabberPressed();
//	}
//
//	public String getButtonId() {
//		if (this.button.getCompatibilityId() != null) {
//			return this.button.getCompatibilityId();
//		}
//		return "" + this.button.getId();
//	}
//
//	@Override
//	public List<PropertyContainer> getProperties() {
//		List<PropertyContainer> l = new ArrayList<PropertyContainer>();
//
//		//hidebutton
//		if (this.customizationContainer.isButtonHidden) {
//			PropertyContainer s = new PropertyContainer("customization");
//			s.putProperty("action", "hidebutton");
//			s.putProperty("identifier", "%id=" + this.getButtonId() + "%");
//			l.add(s);
//		}
//		//movebutton
//		if (this.canBeModified()) {
//			PropertyContainer s = new PropertyContainer("customization");
//			s.putProperty("action", "movebutton");
//			s.putProperty("identifier", "%id=" + this.getButtonId() + "%");
//			s.putProperty("orientation", this.element.anchorPoint);
//			if (this.element.anchorPoint.equals("element") && (this.element.anchorPointElementIdentifier != null)) {
//				s.putProperty("orientation_element", this.element.anchorPointElementIdentifier);
//			}
//			s.putProperty("x", "" + this.element.baseX);
//			s.putProperty("y", "" + this.element.baseY);
//			if (this.element.advancedX != null) {
//				s.putProperty("advanced_posx", this.element.advancedX);
//			}
//			if (this.element.advancedY != null) {
//				s.putProperty("advanced_posy", this.element.advancedY);
//			}
//			l.add(s);
//		}
//
//		// resizebutton
//		if (this.canBeModified() && ((this.getWidth() != this.button.width) || (this.getHeight() != this.button.height) || (this.element.advancedHeight != null) || (this.element.advancedWidth != null))) {
//			PropertyContainer s = new PropertyContainer("customization");
//			s.putProperty("action", "resizebutton");
//			s.putProperty("identifier", "%id=" + this.getButtonId() + "%");
//			s.putProperty("width", "" + this.element.width);
//			s.putProperty("height", "" + this.element.height);
//			if (this.element.advancedWidth != null) {
//				s.putProperty("advanced_width", this.element.advancedWidth);
//			}
//			if (this.element.advancedHeight != null) {
//				s.putProperty("advanced_height", this.element.advancedHeight);
//			}
//			l.add(s);
//		}
//
//		// renamebutton
//		if (this.customizationContainer.customButtonLabel != null) {
//			if (!this.customizationContainer.customButtonLabel.equals(this.button.label)) {
//				PropertyContainer s = new PropertyContainer("customization");
//				s.putProperty("action", "setbuttonlabel");
//				s.putProperty("identifier", "%id=" + this.getButtonId() + "%");
//				s.putProperty("value", this.customizationContainer.customButtonLabel);
//				l.add(s);
//			}
//		}
//		// setbuttontexture
//		if ((this.customizationContainer.normalBackground != null) || (this.customizationContainer.hoverBackground != null)) {
//			PropertyContainer s = new PropertyContainer("customization");
//			s.putProperty("action", "setbuttontexture");
//			s.putProperty("identifier", "%id=" + this.getButtonId() + "%");
//			if (this.customizationContainer.normalBackground != null) {
//				if (this.customizationContainer.normalBackground.startsWith("animation:")) {
//					String aniName = this.customizationContainer.normalBackground.split("[:]", 2)[1];
//					s.putProperty("backgroundanimationnormal", aniName);
//				} else {
//					s.putProperty("backgroundnormal", this.customizationContainer.normalBackground);
//				}
//			}
//			if (this.customizationContainer.hoverBackground != null) {
//				if (this.customizationContainer.hoverBackground.startsWith("animation:")) {
//					String aniName = this.customizationContainer.hoverBackground.split("[:]", 2)[1];
//					s.putProperty("backgroundanimationhovered", aniName);
//				} else {
//					s.putProperty("backgroundhovered", this.customizationContainer.hoverBackground);
//				}
//			}
//			s.putProperty("restartbackgroundanimations", "" + this.customizationContainer.restartAnimationOnHover);
//			s.putProperty("loopbackgroundanimations", "" + this.customizationContainer.loopAnimation);
//			l.add(s);
//		}
//		// clickbutton
//		if (this.customizationContainer.autoButtonClicks > 0) {
//			PropertyContainer s = new PropertyContainer("customization");
//			s.putProperty("action", "clickbutton");
//			s.putProperty("identifier", "%id=" + this.getButtonId() + "%");
//			s.putProperty("clicks", "" + this.customizationContainer.autoButtonClicks);
//			l.add(s);
//		}
//		//addhoversound
//		if (this.customizationContainer.hoverSound != null) {
//			PropertyContainer s = new PropertyContainer("customization");
//			s.putProperty("action", "addhoversound");
//			s.putProperty("identifier", "%id=" + this.getButtonId() + "%");
//			s.putProperty("path", this.customizationContainer.hoverSound);
//			l.add(s);
//		}
//		//sethoverlabel
//		if (this.customizationContainer.hoverLabel != null) {
//			PropertyContainer s = new PropertyContainer("customization");
//			s.putProperty("action", "sethoverlabel");
//			s.putProperty("identifier", "%id=" + this.getButtonId() + "%");
//			s.putProperty("label", this.customizationContainer.hoverLabel);
//			l.add(s);
//		}
//		//hidebuttonfor
//		if (this.element.delayAppearance) {
//			PropertyContainer s = new PropertyContainer("customization");
//			s.putProperty("action", "hidebuttonfor");
//			s.putProperty("identifier", "%id=" + this.getButtonId() + "%");
//			s.putProperty("seconds", "" + this.element.appearanceDelayInSeconds);
//			s.putProperty("onlyfirsttime", "" + !this.element.delayAppearanceEverytime);
//			if (this.element.fadeIn) {
//				s.putProperty("fadein", "true");
//				s.putProperty("fadeinspeed", "" + this.element.fadeInSpeed);
//			}
//			l.add(s);
//		}
//		//setbuttonclicksound
//		if (this.customizationContainer.clickSound != null) {
//			PropertyContainer s = new PropertyContainer("customization");
//			s.putProperty("action", "setbuttonclicksound");
//			s.putProperty("identifier", "%id=" + this.getButtonId() + "%");
//			s.putProperty("path", this.customizationContainer.clickSound);
//			l.add(s);
//		}
//		//setbuttondescription
//		if (this.customizationContainer.buttonDescription != null) {
//			PropertyContainer s = new PropertyContainer("customization");
//			s.putProperty("action", "setbuttondescription");
//			s.putProperty("identifier", "%id=" + this.getButtonId() + "%");
//			s.putProperty("description", this.customizationContainer.buttonDescription);
//			l.add(s);
//		}
//
//		//Visibility Requirements
//		PropertyContainer visReqs = new PropertyContainer("customization");
//		visReqs.putProperty("action", "vanilla_button_visibility_requirements");
//		visReqs.putProperty("identifier", "%id=" + this.getButtonId() + "%");
//
//		this.serializeLoadingRequirementsTo(visReqs);
//		if (visReqs.getProperties().size() > 2) {
//			l.add(visReqs);
//		}
//
//		return l;
//	}
//
//	public void displaySetOrientationNotification() {
//		if (FancyMenu.getConfig().getOrDefault("showvanillamovewarning", true)) {
//			FMNotificationPopup p = new FMNotificationPopup(300, new Color(0,0,0,0), 240, null, LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.element.vanilla.orientation_needed")));
//			PopupHandler.displayPopup(p);
//		}
//	}
//
//	private boolean canBeModified() {
//		return !this.element.anchorPoint.equals("original");
//	}
//
//	@Override
//	public void destroyElement() {
//		if (FancyMenu.getConfig().getOrDefault("editordeleteconfirmation", true)) {
//			FMNotificationPopup pop = new FMNotificationPopup(300, new Color(0,0,0,0), 240, null, LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.element.vanilla.delete.confirm")));
//			PopupHandler.displayPopup(pop);
//		}
//		this.editor.hideVanillaButton(this);
//	}
//
//}
