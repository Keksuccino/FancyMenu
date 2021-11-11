package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import de.keksuccino.fancymenu.menu.fancy.helper.DynamicValueInputPopup;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.ChooseFilePopup;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutElement;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.FMContextMenu;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMTextInputPopup;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.util.text.StringTextComponent;

public class LayoutButton extends LayoutElement {

	//TODO übernehmen
	public MenuHandlerBase.ButtonCustomizationContainer customizationContainer;
	public String actionContent = "";
	public String actionType = "openlink";
	//TODO übernehmen
//	public String backNormal = null;
//	public String backHovered = null;
//	public String hoverSound;
//	public String hoverLabel;
	public String onlydisplayin = null;
	//TODO übernehmen
//	public String clicksound = null;
//	public String description;
	private AdvancedButton onlyOutgameBtn;
	private AdvancedButton onlySingleplayerBtn;
	private AdvancedButton onlyMultiplayerBtn;

	//TODO übernehmen
	public LayoutButton(MenuHandlerBase.ButtonCustomizationContainer customizationContainer, int width, int height, @Nonnull String label, @Nullable String onlydisplayin, LayoutEditorScreen handler) {
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

		//TODO übernehmen (nach unten vor hover label verschieben)
//		AdvancedButton b2 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.editlabel"), (press) -> {
//			FMTextInputPopup i = new DynamicValueInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.items.button.editlabel") + ":", null, 240, (call) -> {
//				if (call != null) {
//					if (!this.object.value.equals(call)) {
//						this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
//					}
//					this.object.value = call;
//				}
//			});
//			i.setText(StringUtils.convertFormatCodes(this.object.value, "§", "&"));
//			PopupHandler.displayPopup(i);
//		});
//		this.rightclickMenu.addContent(b2);

		AdvancedButton b3 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.custombutton.config"), (press) -> {
			ButtonActionPopup i = new ButtonActionPopup(this::setActionContentCallback, this::setActionTypeCallback, this.actionType);
			i.setText(this.actionContent);
			PopupHandler.displayPopup(i);
		});
		this.rightclickMenu.addContent(b3);

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

		//TODO übernehmen (von oben hierher verschieben + änderungen im code übernehmen)
		AdvancedButton b2 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.editlabel"), (press) -> {
			FMTextInputPopup i = new DynamicValueInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.items.button.editlabel") + ":", null, 240, (call) -> {
				if (call != null) {
					if (!this.object.value.equals(call)) {
						this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
					}
					this.object.value = call;
				}
			});
			i.setText(StringUtils.convertFormatCodes(this.object.value, "§", "&"));
			PopupHandler.displayPopup(i);
		});
		this.rightclickMenu.addContent(b2);

		//TODO übernehmen
		this.rightclickMenu.addSeparator();

		//TODO übernehmen
		AdvancedButton b5 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.hoverlabel"), (press) -> {
			FMTextInputPopup ip = new DynamicValueInputPopup(new Color(0, 0, 0, 0), "", null, 240, (call) -> {
				if (call != null) {
					if ((this.customizationContainer.hoverLabel == null) || !this.customizationContainer.hoverLabel.equals(call)) {
						this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
					}

					this.customizationContainer.hoverLabel = call;
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
//			this.rightclickMenu.closeMenu();
//		});
//		this.rightclickMenu.addContent(b8);

		//TODO übernehmen
		AdvancedButton b10 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.clicksound"), (press) -> {
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
		this.rightclickMenu.addContent(b10);

		AdvancedButton b12 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.btndescription"), (press) -> {
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
		b12.setDescription(l.toArray(new String[0]));
		this.rightclickMenu.addContent(b12);

	}

	private void initOnlyDisplayInMenu() {
		FMContextMenu onlyDisplayInMenu = new FMContextMenu();
		this.rightclickMenu.addChild(onlyDisplayInMenu);
		
		String outgame = Locals.localize("helper.creator.items.custombutton.onlydisplayin.outgame");
		if ((this.onlydisplayin != null) && this.onlydisplayin.equals("outgame")) {
			outgame = "§a" + outgame;
		}
		onlyOutgameBtn = new AdvancedButton(0, 0, 0, 0, outgame, (press) -> {
			this.onlydisplayin = "outgame";
			press.setMessage(new StringTextComponent("§a" + Locals.localize("helper.creator.items.custombutton.onlydisplayin.outgame")));
			this.onlySingleplayerBtn.setMessage(new StringTextComponent(Locals.localize("helper.creator.items.custombutton.onlydisplayin.singleplayer")));
			this.onlyMultiplayerBtn.setMessage(new StringTextComponent(Locals.localize("helper.creator.items.custombutton.onlydisplayin.multiplayer")));
		});
		onlyDisplayInMenu.addContent(onlyOutgameBtn);
		
		String sp = Locals.localize("helper.creator.items.custombutton.onlydisplayin.singleplayer");
		if ((this.onlydisplayin != null) && this.onlydisplayin.equals("singleplayer")) {
			sp = "§a" + sp;
		}
		onlySingleplayerBtn = new AdvancedButton(0, 0, 0, 0, sp, (press) -> {
			this.onlydisplayin = "singleplayer";
			press.setMessage(new StringTextComponent("§a" + Locals.localize("helper.creator.items.custombutton.onlydisplayin.singleplayer")));
			this.onlyOutgameBtn.setMessage(new StringTextComponent(Locals.localize("helper.creator.items.custombutton.onlydisplayin.outgame")));
			this.onlyMultiplayerBtn.setMessage(new StringTextComponent(Locals.localize("helper.creator.items.custombutton.onlydisplayin.multiplayer")));
		});
		onlyDisplayInMenu.addContent(onlySingleplayerBtn);
		
		String mp = Locals.localize("helper.creator.items.custombutton.onlydisplayin.multiplayer");
		if ((this.onlydisplayin != null) && this.onlydisplayin.equals("multiplayer")) {
			mp = "§a" + mp;
		}
		onlyMultiplayerBtn = new AdvancedButton(0, 0, 0, 0, mp, (press) -> {
			this.onlydisplayin = "multiplayer";
			press.setMessage(new StringTextComponent("§a" + Locals.localize("helper.creator.items.custombutton.onlydisplayin.multiplayer")));
			this.onlySingleplayerBtn.setMessage(new StringTextComponent(Locals.localize("helper.creator.items.custombutton.onlydisplayin.singleplayer")));
			this.onlyOutgameBtn.setMessage(new StringTextComponent(Locals.localize("helper.creator.items.custombutton.onlydisplayin.outgame")));
		});
		onlyDisplayInMenu.addContent(onlyMultiplayerBtn);
		
		AdvancedButton odiResetBtn = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.items.custombutton.onlydisplayin.reset"), (press) -> {
			this.onlydisplayin = null;
			this.onlyMultiplayerBtn.setMessage(new StringTextComponent(Locals.localize("helper.creator.items.custombutton.onlydisplayin.multiplayer")));
			this.onlySingleplayerBtn.setMessage(new StringTextComponent(Locals.localize("helper.creator.items.custombutton.onlydisplayin.singleplayer")));
			this.onlyOutgameBtn.setMessage(new StringTextComponent(Locals.localize("helper.creator.items.custombutton.onlydisplayin.outgame")));
		});
		onlyDisplayInMenu.addContent(odiResetBtn);

		AdvancedButton b10 = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.items.custombutton.onlydisplayin"), (press) -> {
			onlyDisplayInMenu.setParentButton((AdvancedButton) press);
			onlyDisplayInMenu.openMenuAt(0, press.y);
		});
		this.rightclickMenu.addContent(b10);
	}
	
	private void setActionContentCallback(String content) {
		this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
				
		if (content != null) {
			this.actionContent = content;
		}
	}
	
	private void setActionTypeCallback(String action) {
		this.actionType = action;
	}

	@Override
	public List<PropertiesSection> getProperties() {
		List<PropertiesSection> l = new ArrayList<PropertiesSection>();
		if (this.actionType != null) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("actionid", this.object.getActionId());
			if (this.object.delayAppearance) {
				s.addEntry("delayappearance", "true");
				s.addEntry("delayappearanceeverytime", "" + this.object.delayAppearanceEverytime);
				s.addEntry("delayappearanceseconds", "" + this.object.delayAppearanceSec);
				if (this.object.fadeIn) {
					s.addEntry("fadein", "true");
					s.addEntry("fadeinspeed", "" + this.object.fadeInSpeed);
				}
			}
			s.addEntry("action", "addbutton");
			s.addEntry("label", this.object.value);
			if (this.stretchX) {
				s.addEntry("x", "0");
				s.addEntry("width", "%guiwidth%");
			} else {
				s.addEntry("x", "" + this.object.posX);
				//TODO übernehmen
				s.addEntry("width", "" + this.object.getWidth());
			}
			if (this.stretchY) {
				s.addEntry("y", "0");
				s.addEntry("height", "%guiheight%");
			} else {
				s.addEntry("y", "" + this.object.posY);
				//TODO übernehmen
				s.addEntry("height", "" + this.object.getHeight());
			}
			s.addEntry("orientation", this.object.orientation);
			if (this.object.orientation.equals("element") && (this.object.orientationElementIdentifier != null)) {
				s.addEntry("orientation_element", this.object.orientationElementIdentifier);
			}
			s.addEntry("buttonaction", this.actionType);
			s.addEntry("value", this.actionContent);
			//TODO übernehmen
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
			//TODO übernehmen
			s.addEntry("restartbackgroundanimations", "" + this.customizationContainer.restartAnimationOnHover);
			//TODO übernehmen
			s.addEntry("loopbackgroundanimations", "" + this.customizationContainer.loopAnimation);
			//TODO übernehmen
			if (this.customizationContainer.hoverSound != null) {
				s.addEntry("hoversound", this.customizationContainer.hoverSound);
			}
			//TODO übernehmen
			if (this.customizationContainer.hoverLabel != null) {
				s.addEntry("hoverlabel", this.customizationContainer.hoverLabel);
			}
			if (this.onlydisplayin != null) {
				s.addEntry("onlydisplayin", this.onlydisplayin);
			}
			//TODO übernehmen
			if (this.customizationContainer.clickSound != null) {
				s.addEntry("clicksound", this.customizationContainer.clickSound);
			}
			//TODO übernehmen
			if (this.customizationContainer.buttonDescription != null) {
				s.addEntry("description", this.customizationContainer.buttonDescription);
			}

			//TODO übernehmen
			this.addVisibilityPropertiesTo(s);

			l.add(s);
		}
		
		return l;
	}
	
}
