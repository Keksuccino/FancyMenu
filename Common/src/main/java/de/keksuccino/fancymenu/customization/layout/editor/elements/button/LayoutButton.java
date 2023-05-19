//package de.keksuccino.fancymenu.customization.layout.editor.elements.button;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.List;
//
//import javax.annotation.Nonnull;
//import javax.annotation.Nullable;
//
//import de.keksuccino.fancymenu.customization.action.Action;
//import de.keksuccino.fancymenu.customization.action.ActionRegistry;
//import de.keksuccino.fancymenu.customization.action.ActionExecutor;
//import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
//import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
//import de.keksuccino.fancymenu.customization.layout.editor.actions.ManageActionsScreen;
//import de.keksuccino.fancymenu.customization.layout.editor.elements.ChooseFilePopup;
//import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
//import de.keksuccino.fancymenu.rendering.ui.texteditor.TextEditorScreen;
//import de.keksuccino.konkrete.gui.content.AdvancedButton;
//import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
//import de.keksuccino.fancymenu.utils.LocalizationUtils;
//import net.minecraft.client.resources.language.I18n;
//import net.minecraft.client.Minecraft;
//import net.minecraft.network.chat.Component;
//
//public class LayoutButton extends AbstractEditorElement {
//
//	public ScreenCustomizationLayer.ButtonCustomizationContainer customizationContainer;
//
////	public String actionContent = "";
////	public String actionType = "openlink";
//	public String onlydisplayin = null;
//	private AdvancedButton onlyOutgameBtn;
//	private AdvancedButton onlySingleplayerBtn;
//	private AdvancedButton onlyMultiplayerBtn;
//
//	public List<ActionExecutor.ActionContainer> actions = new ArrayList<>();
//
//	public LayoutButton(ScreenCustomizationLayer.ButtonCustomizationContainer customizationContainer, int width, int height, @Nonnull String label, @Nullable String onlydisplayin, LayoutEditorScreen handler) {
//		super(new LayoutButtonDummyCustomizationItem(customizationContainer, label, width, height, 0, 0), true, handler, false);
//		this.onlydisplayin = onlydisplayin;
//		this.customizationContainer = customizationContainer;
//		this.init();
//		this.initOnlyDisplayInMenu();
//	}
//
//	@Override
//	public void init() {
//
//		super.init();
//
//		AdvancedButton manageActionsButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.editor.action.screens.manage_screen.manage"), (press) -> {
//			List<ManageActionsScreen.ActionInstance> l = new ArrayList<>();
//			for (ActionExecutor.ActionContainer c : this.actions) {
//				Action bac = ActionRegistry.getActionByName(c.action);
//				if (bac != null) {
//					ManageActionsScreen.ActionInstance i = new ManageActionsScreen.ActionInstance(bac, c.value);
//					l.add(i);
//				}
//			}
//			ManageActionsScreen s = new ManageActionsScreen(this.editor, l, (call) -> {
//				if (call != null) {
//					this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//					this.actions.clear();
//					for (ManageActionsScreen.ActionInstance i : call) {
//						this.actions.add(new ActionExecutor.ActionContainer(i.action.getIdentifier(), i.value));
//					}
//				}
//			});
//			Minecraft.getInstance().setScreen(s);
//		});
//		manageActionsButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.editor.elements.button.manage_actions.desc")));
//		this.rightClickContextMenu.addContent(manageActionsButton);
//
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
//					if (!this.element.value.equals(call)) {
//						this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//					}
//					this.element.value = call;
//				}
//			});
//			s.multilineMode = false;
//			s.setText(StringUtils.convertFormatCodes(this.element.value, "§", "&"));
//			Minecraft.getInstance().setScreen(s);
//
//		});
//		this.rightClickContextMenu.addContent(b2);
//
//		this.rightClickContextMenu.addSeparator();
//
//		AdvancedButton b5 = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.items.button.hoverlabel"), (press) -> {
//
//			TextEditorScreen s = new TextEditorScreen(Component.literal(I18n.get("fancymenu.editor.items.button.hoverlabel")), this.editor, null, (call) -> {
//				if (call != null) {
//					if ((this.customizationContainer.hoverLabel == null) || !this.customizationContainer.hoverLabel.equals(call)) {
//						this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//					}
//
//					this.customizationContainer.hoverLabel = call;
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
//		AdvancedButton b10 = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.items.button.clicksound"), (press) -> {
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
//		this.rightClickContextMenu.addContent(b10);
//
//		AdvancedButton b12 = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.items.button.btndescription"), (press) -> {
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
//		b12.setDescription(l.toArray(new String[0]));
//		this.rightClickContextMenu.addContent(b12);
//
//	}
//
//}
