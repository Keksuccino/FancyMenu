package de.keksuccino.fancymenu.customization.frontend.layouteditor.elements.button;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import de.keksuccino.fancymenu.api.buttonaction.ButtonActionContainer;
import de.keksuccino.fancymenu.api.buttonaction.ButtonActionRegistry;
import de.keksuccino.fancymenu.customization.backend.button.ButtonScriptEngine;
import de.keksuccino.fancymenu.customization.backend.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.actions.ManageActionsScreen;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.elements.ChooseFilePopup;
import de.keksuccino.fancymenu.customization.backend.element.AbstractEditorElement;
import de.keksuccino.fancymenu.rendering.ui.FMContextMenu;
import de.keksuccino.fancymenu.rendering.ui.texteditor.TextEditorScreen;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class LayoutButton extends AbstractEditorElement {

	public ScreenCustomizationLayer.ButtonCustomizationContainer customizationContainer;
	
//	public String actionContent = "";
//	public String actionType = "openlink";
	public String onlydisplayin = null;
	private AdvancedButton onlyOutgameBtn;
	private AdvancedButton onlySingleplayerBtn;
	private AdvancedButton onlyMultiplayerBtn;
	
	public List<ButtonScriptEngine.ActionContainer> actions = new ArrayList<>();

	public LayoutButton(ScreenCustomizationLayer.ButtonCustomizationContainer customizationContainer, int width, int height, @Nonnull String label, @Nullable String onlydisplayin, LayoutEditorScreen handler) {
		super(new LayoutButtonDummyCustomizationItem(customizationContainer, label, width, height, 0, 0), true, handler, false);
		this.onlydisplayin = onlydisplayin;
		this.customizationContainer = customizationContainer;
		this.init();
		this.initOnlyDisplayInMenu();
	}

	@Override
	public void init() {

		this.stretchable = true;

		super.init();

		
//		AdvancedButton b3 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.custombutton.config"), (press) -> {
//			Minecraft.getInstance().setScreen(new ButtonActionScreen(this.handler, this));
//		});
//		this.rightclickMenu.addContent(b3);

		
//		AdvancedButton addActionButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.editor.action.screens.add_action"), (press) -> {
//			ButtonActionScreen s = new ButtonActionScreen(this.handler, (call) -> {
//				if (call != null) {
//					this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
//					this.actions.add(new ButtonScriptEngine.ActionContainer(call.get(0), call.get(1)));
//				}
//			});
//		});
//		addActionButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.customization.items.custom_button.add_action.desc"), "%n%"));
//		this.rightclickMenu.addContent(addActionButton);

		
		AdvancedButton manageActionsButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.editor.action.screens.manage_screen.manage"), (press) -> {
			List<ManageActionsScreen.ActionInstance> l = new ArrayList<>();
			for (ButtonScriptEngine.ActionContainer c : this.actions) {
				ButtonActionContainer bac = ButtonActionRegistry.getActionByName(c.action);
				if (bac != null) {
					ManageActionsScreen.ActionInstance i = new ManageActionsScreen.ActionInstance(bac, c.value);
					l.add(i);
				}
			}
			ManageActionsScreen s = new ManageActionsScreen(this.editor, l, (call) -> {
				if (call != null) {
					this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
					this.actions.clear();
					for (ManageActionsScreen.ActionInstance i : call) {
						this.actions.add(new ButtonScriptEngine.ActionContainer(i.action.getAction(), i.value));
					}
				}
			});
			Minecraft.getInstance().setScreen(s);
		});
		manageActionsButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.elements.button.manage_actions.desc"), "%n%"));
		this.rightClickContextMenu.addContent(manageActionsButton);
		

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
					if (!this.element.value.equals(call)) {
						this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
					}
					this.element.value = call;
				}
			});
			s.multilineMode = false;
			s.setText(StringUtils.convertFormatCodes(this.element.value, "§", "&"));
			Minecraft.getInstance().setScreen(s);
			
		});
		this.rightClickContextMenu.addContent(b2);

		this.rightClickContextMenu.addSeparator();

		AdvancedButton b5 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.hoverlabel"), (press) -> {
			
			TextEditorScreen s = new TextEditorScreen(Component.literal(Locals.localize("helper.creator.items.button.hoverlabel")), this.editor, null, (call) -> {
				if (call != null) {
					if ((this.customizationContainer.hoverLabel == null) || !this.customizationContainer.hoverLabel.equals(call)) {
						this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
					}

					this.customizationContainer.hoverLabel = call;
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

		AdvancedButton b10 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.clicksound"), (press) -> {
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
		this.rightClickContextMenu.addContent(b10);

		AdvancedButton b12 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.btndescription"), (press) -> {
			
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
		b12.setDescription(l.toArray(new String[0]));
		this.rightClickContextMenu.addContent(b12);

	}

	private void initOnlyDisplayInMenu() {
		FMContextMenu onlyDisplayInMenu = new FMContextMenu();
		this.rightClickContextMenu.addChild(onlyDisplayInMenu);

		String outgame = Locals.localize("helper.creator.items.custombutton.onlydisplayin.outgame");
		if ((this.onlydisplayin != null) && this.onlydisplayin.equals("outgame")) {
			outgame = "§a" + outgame;
		}
		onlyOutgameBtn = new AdvancedButton(0, 0, 0, 0, outgame, (press) -> {
			this.onlydisplayin = "outgame";
			press.setMessage(Component.literal("§a" + Locals.localize("helper.creator.items.custombutton.onlydisplayin.outgame")));
			this.onlySingleplayerBtn.setMessage(Component.literal(Locals.localize("helper.creator.items.custombutton.onlydisplayin.singleplayer")));
			this.onlyMultiplayerBtn.setMessage(Component.literal(Locals.localize("helper.creator.items.custombutton.onlydisplayin.multiplayer")));
		});
		onlyDisplayInMenu.addContent(onlyOutgameBtn);

		String sp = Locals.localize("helper.creator.items.custombutton.onlydisplayin.singleplayer");
		if ((this.onlydisplayin != null) && this.onlydisplayin.equals("singleplayer")) {
			sp = "§a" + sp;
		}
		onlySingleplayerBtn = new AdvancedButton(0, 0, 0, 0, sp, (press) -> {
			this.onlydisplayin = "singleplayer";
			press.setMessage(Component.literal("§a" + Locals.localize("helper.creator.items.custombutton.onlydisplayin.singleplayer")));
			this.onlyOutgameBtn.setMessage(Component.literal(Locals.localize("helper.creator.items.custombutton.onlydisplayin.outgame")));
			this.onlyMultiplayerBtn.setMessage(Component.literal(Locals.localize("helper.creator.items.custombutton.onlydisplayin.multiplayer")));
		});
		onlyDisplayInMenu.addContent(onlySingleplayerBtn);

		String mp = Locals.localize("helper.creator.items.custombutton.onlydisplayin.multiplayer");
		if ((this.onlydisplayin != null) && this.onlydisplayin.equals("multiplayer")) {
			mp = "§a" + mp;
		}
		onlyMultiplayerBtn = new AdvancedButton(0, 0, 0, 0, mp, (press) -> {
			this.onlydisplayin = "multiplayer";
			press.setMessage(Component.literal("§a" + Locals.localize("helper.creator.items.custombutton.onlydisplayin.multiplayer")));
			this.onlySingleplayerBtn.setMessage(Component.literal(Locals.localize("helper.creator.items.custombutton.onlydisplayin.singleplayer")));
			this.onlyOutgameBtn.setMessage(Component.literal(Locals.localize("helper.creator.items.custombutton.onlydisplayin.outgame")));
		});
		onlyDisplayInMenu.addContent(onlyMultiplayerBtn);

		AdvancedButton odiResetBtn = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.items.custombutton.onlydisplayin.reset"), (press) -> {
			this.onlydisplayin = null;
			this.onlyMultiplayerBtn.setMessage(Component.literal(Locals.localize("helper.creator.items.custombutton.onlydisplayin.multiplayer")));
			this.onlySingleplayerBtn.setMessage(Component.literal(Locals.localize("helper.creator.items.custombutton.onlydisplayin.singleplayer")));
			this.onlyOutgameBtn.setMessage(Component.literal(Locals.localize("helper.creator.items.custombutton.onlydisplayin.outgame")));
		});
		onlyDisplayInMenu.addContent(odiResetBtn);

		AdvancedButton b10 = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.items.custombutton.onlydisplayin"), (press) -> {
			onlyDisplayInMenu.setParentButton((AdvancedButton) press);
			onlyDisplayInMenu.openMenuAt(0, press.y);
		});
		this.rightClickContextMenu.addContent(b10);
	}

	
	@Override
	public List<PropertiesSection> getProperties() {

		List<PropertiesSection> l = new ArrayList<>();
		PropertiesSection s = new PropertiesSection("customization");

		s.addEntry("action", "addbutton");
		s.addEntry("actionid", this.element.getInstanceIdentifier());

		if (this.element.advancedX != null) {
			s.addEntry("advanced_posx", this.element.advancedX);
		}
		if (this.element.advancedY != null) {
			s.addEntry("advanced_posy", this.element.advancedY);
		}
		if (this.element.advancedWidth != null) {
			s.addEntry("advanced_width", this.element.advancedWidth);
		}
		if (this.element.advancedHeight != null) {
			s.addEntry("advanced_height", this.element.advancedHeight);
		}

		if (this.element.delayAppearance) {
			s.addEntry("delayappearance", "true");
			s.addEntry("delayappearanceeverytime", "" + this.element.delayAppearanceEverytime);
			s.addEntry("delayappearanceseconds", "" + this.element.delayAppearanceSec);
			if (this.element.fadeIn) {
				s.addEntry("fadein", "true");
				s.addEntry("fadeinspeed", "" + this.element.fadeInSpeed);
			}
		}

		if (this.stretchX) {
			s.addEntry("x", "0");
			s.addEntry("width", "%guiwidth%");
		} else {
			s.addEntry("x", "" + this.element.rawX);
			s.addEntry("width", "" + this.element.getWidth());
		}
		if (this.stretchY) {
			s.addEntry("y", "0");
			s.addEntry("height", "%guiheight%");
		} else {
			s.addEntry("y", "" + this.element.rawY);
			s.addEntry("height", "" + this.element.getHeight());
		}
		s.addEntry("orientation", this.element.orientation);
		if (this.element.orientation.equals("element") && (this.element.orientationElementIdentifier != null)) {
			s.addEntry("orientation_element", this.element.orientationElementIdentifier);
		}

		if (!this.actions.isEmpty()) {
			String buttonaction = "";
			for (ButtonScriptEngine.ActionContainer c : this.actions) {
				String s2 = c.action;
				if (c.value != null) {
					s2 += ";" + c.value;
				}
				buttonaction += s2 + "%btnaction_splitter_fm%";
			}
			s.addEntry("buttonaction", buttonaction);
		}

		if ((this.customizationContainer.normalBackground != null) || (this.customizationContainer.hoverBackground != null)) {
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
		}
		s.addEntry("restartbackgroundanimations", "" + this.customizationContainer.restartAnimationOnHover);
		s.addEntry("loopbackgroundanimations", "" + this.customizationContainer.loopAnimation);
		if (this.customizationContainer.hoverSound != null) {
			s.addEntry("hoversound", this.customizationContainer.hoverSound);
		}
		if (this.customizationContainer.hoverLabel != null) {
			s.addEntry("hoverlabel", this.customizationContainer.hoverLabel);
		}
		if (this.onlydisplayin != null) {
			s.addEntry("onlydisplayin", this.onlydisplayin);
		}
		if (this.customizationContainer.clickSound != null) {
			s.addEntry("clicksound", this.customizationContainer.clickSound);
		}
		if (this.customizationContainer.buttonDescription != null) {
			s.addEntry("description", this.customizationContainer.buttonDescription);
		}
		s.addEntry("label", this.element.value);

		
		this.serializeLoadingRequirementsTo(s);

		l.add(s);

		return l;
	}

}
