package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import de.keksuccino.fancymenu.menu.fancy.helper.PlaceholderInputPopup;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.ChooseFilePopup;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutElement;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button.buttonactions.ButtonActionScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.FMContextMenu;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMTextInputPopup;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class LayoutButton extends LayoutElement {

	public MenuHandlerBase.ButtonCustomizationContainer customizationContainer;
	public String actionContent = "";
	public String actionType = "openlink";
	public String onlydisplayin = null;
	private AdvancedButton onlyOutgameBtn;
	private AdvancedButton onlySingleplayerBtn;
	private AdvancedButton onlyMultiplayerBtn;

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

		AdvancedButton b3 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.custombutton.config"), (press) -> {
			Minecraft.getInstance().setScreen(new ButtonActionScreen(this.handler, this));
		});
		this.rightclickMenu.addContent(b3);

		this.rightclickMenu.addSeparator();

		AdvancedButton buttonBackgroundButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground"), (press) -> {
			ButtonBackgroundPopup pop = new ButtonBackgroundPopup(this.handler, this.customizationContainer);
			PopupHandler.displayPopup(pop);
		});
		this.rightclickMenu.addContent(buttonBackgroundButton);

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

		this.rightclickMenu.addSeparator();

		AdvancedButton b2 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.editlabel"), (press) -> {
			FMTextInputPopup i = new PlaceholderInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.items.button.editlabel") + ":", null, 240, (call) -> {
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

		this.rightclickMenu.addSeparator();

		AdvancedButton b5 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.hoverlabel"), (press) -> {
			FMTextInputPopup ip = new PlaceholderInputPopup(new Color(0, 0, 0, 0), "", null, 240, (call) -> {
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

		AdvancedButton b6 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.hoverlabel.reset"), (press) -> {
			if (this.customizationContainer.hoverLabel != null) {
				this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
			}
			this.customizationContainer.hoverLabel = null;
			this.rightclickMenu.closeMenu();
		});
		this.rightclickMenu.addContent(b6);

		this.rightclickMenu.addSeparator();

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
			FMTextInputPopup in = new PlaceholderInputPopup(new Color(0, 0, 0, 0), Locals.localize("helper.creator.items.button.btndescription"), null, 240, (call) -> {
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
		this.rightclickMenu.addContent(b10);
	}

	@Override
	public List<PropertiesSection> getProperties() {
		List<PropertiesSection> l = new ArrayList<PropertiesSection>();
		if (this.actionType != null) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("actionid", this.object.getActionId());
			
			if (this.object.advancedPosX != null) {
				s.addEntry("advanced_posx", this.object.advancedPosX);
			}
			if (this.object.advancedPosY != null) {
				s.addEntry("advanced_posy", this.object.advancedPosY);
			}
			if (this.object.advancedWidth != null) {
				s.addEntry("advanced_width", this.object.advancedWidth);
			}
			if (this.object.advancedHeight != null) {
				s.addEntry("advanced_height", this.object.advancedHeight);
			}
			
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
				s.addEntry("width", "" + this.object.getWidth());
			}
			if (this.stretchY) {
				s.addEntry("y", "0");
				s.addEntry("height", "%guiheight%");
			} else {
				s.addEntry("y", "" + this.object.posY);
				s.addEntry("height", "" + this.object.getHeight());
			}
			s.addEntry("orientation", this.object.orientation);
			if (this.object.orientation.equals("element") && (this.object.orientationElementIdentifier != null)) {
				s.addEntry("orientation_element", this.object.orientationElementIdentifier);
			}
			s.addEntry("buttonaction", this.actionType);
			s.addEntry("value", this.actionContent);
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

			this.addVisibilityPropertiesTo(s);

			l.add(s);
		}
		
		return l;
	}
	
}
