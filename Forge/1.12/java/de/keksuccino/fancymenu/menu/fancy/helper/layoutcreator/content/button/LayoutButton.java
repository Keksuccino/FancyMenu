package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutCreatorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.ChooseFilePopup;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutObject;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.ContextMenu;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.gui.screens.popup.TextInputPopup;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.resources.TextureHandler;

public class LayoutButton extends LayoutObject implements ILayoutButton {

	public String actionContent = "";
	public String actionType = "openlink";
	public String backNormal = null;
	public String backHovered = null;
	public String hoverSound;
	public String hoverLabel;
	public double hideforsec = 0;
	public boolean delayonlyfirsttime = false;
	public String onlydisplayin = null;
	public String clicksound = null;
	public String description;
	private AdvancedButton onlyOutgameBtn;
	private AdvancedButton onlySingleplayerBtn;
	private AdvancedButton onlyMultiplayerBtn;

	public LayoutButton(int width, int height, @Nonnull String label, @Nullable String onlydisplayin, LayoutCreatorScreen handler) {
		super(new LayoutButtonDummyCustomizationItem(label, width, height, 0, 0), true, handler);
		this.onlydisplayin = onlydisplayin;
		this.initOnlyDisplayInMenu();
	}

	@Override
	protected void init() {
		
		this.stretchable = true;
		
		super.init();
		
		AdvancedButton b2 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.editlabel"), (press) -> {
			this.handler.setMenusUseable(false);
			TextInputPopup i = new TextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.items.button.editlabel") + ":", null, 240, this::editLabelCallback);
			i.setText(StringUtils.convertFormatCodes(this.object.value, "§", "&"));
			PopupHandler.displayPopup(i);
		});
		this.rightclickMenu.addContent(b2);
		LayoutCreatorScreen.colorizeCreatorButton(b2);
		
		AdvancedButton b3 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.custombutton.config"), (press) -> {
			this.handler.setMenusUseable(false);
			ButtonActionPopup i = new ButtonActionPopup(this::setActionContentCallback, this::setActionTypeCallback, this.actionType);
			i.setText(this.actionContent);
			PopupHandler.displayPopup(i);
		});
		this.rightclickMenu.addContent(b3);
		LayoutCreatorScreen.colorizeCreatorButton(b3);
		
		ContextMenu texturePopup = new ContextMenu(100, 16, -1);
		this.rightclickMenu.addChild(texturePopup);
		
		AdvancedButton tpop1 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.custombutton.config.texture.normal"), (press) -> {
			this.handler.setMenusUseable(false);
			ChooseFilePopup cf = new ChooseFilePopup((call) -> {
				if (call != null) {
					File home = new File("");
					call = call.replace("\\", "/");
					File f = new File(call);
					String filename = CharacterFilter.getBasicFilenameCharacterFilter().filterForAllowedChars(f.getName());
					if (f.exists() && f.isFile() && (f.getName().endsWith(".jpg") || f.getName().endsWith(".jpeg") || f.getName().endsWith(".png"))) {
						if (filename.equals(f.getName())) {
							if (call.startsWith(home.getAbsolutePath())) {
								call = call.replace(home.getAbsolutePath(), "");
								if (call.startsWith("\\") || call.startsWith("/")) {
									call = call.substring(1);
								}
							}
							if ((this.backNormal == null) || !this.backNormal.equals(call)) {
								this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
							}
							
							this.backNormal = call;
							if (this.backHovered == null) {
								this.backHovered = call;
							}
							((LayoutButtonDummyCustomizationItem)this.object).setTexture(TextureHandler.getResource(this.backNormal).getResourceLocation());
							
							this.handler.setMenusUseable(true);
						} else {
							this.handler.displayNotification(300, Locals.localize("helper.creator.textures.invalidcharacters"), "", "", "", "", "", "");
						}
					} else {
						this.handler.displayNotification(300, "§c§l" + Locals.localize("helper.creator.invalidimage.title"), "", Locals.localize("helper.creator.invalidimage.desc"), "", "", "", "", "", "");
					}
				} else {
					this.handler.setMenusUseable(true);
				}
			}, "jpg", "jpeg", "png");
			
			if (this.backNormal != null) {
				cf.setText(this.backNormal);
			}
			
			PopupHandler.displayPopup(cf);
			
		});
		texturePopup.addContent(tpop1);
		LayoutCreatorScreen.colorizeCreatorButton(tpop1);
		
		AdvancedButton tpop2 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.custombutton.config.texture.hovered"), (press) -> {
			this.handler.setMenusUseable(false);
			ChooseFilePopup cf = new ChooseFilePopup((call) -> {
				if (call != null) {
					File home = new File("");
					call = call.replace("\\", "/");
					File f = new File(call);
					String filename = CharacterFilter.getBasicFilenameCharacterFilter().filterForAllowedChars(f.getName());
					if (f.exists() && f.isFile() && (f.getName().endsWith(".jpg") || f.getName().endsWith(".jpeg") || f.getName().endsWith(".png"))) {
						if (filename.equals(f.getName())) {
							if (call.startsWith(home.getAbsolutePath())) {
								call = call.replace(home.getAbsolutePath(), "");
								if (call.startsWith("\\") || call.startsWith("/")) {
									call = call.substring(1);
								}
							}
							if ((this.backHovered == null) || !this.backHovered.equals(call)) {
								this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
							}
							
							this.backHovered = call;
							if (this.backNormal == null) {
								this.backNormal = call;
							}
							((LayoutButtonDummyCustomizationItem)this.object).setTexture(TextureHandler.getResource(this.backNormal).getResourceLocation());
							
							this.handler.setMenusUseable(true);
						} else {
							this.handler.displayNotification(300, Locals.localize("helper.creator.textures.invalidcharacters"), "", "", "", "", "", "");
						}
					} else {
						this.handler.displayNotification(300, "§c§l" + Locals.localize("helper.creator.invalidimage.title"), "", Locals.localize("helper.creator.invalidimage.desc"), "", "", "", "", "", "");
					}
				} else {
					this.handler.setMenusUseable(true);
				}
			}, "jpg", "jpeg", "png");
			
			if (this.backHovered != null) {
				cf.setText(this.backHovered);
			}
			
			PopupHandler.displayPopup(cf);
		});
		texturePopup.addContent(tpop2);
		LayoutCreatorScreen.colorizeCreatorButton(tpop2);
		
		AdvancedButton tpop3 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.custombutton.config.texture.reset"), (press) -> {
			this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
			
			this.backHovered = null;
			this.backNormal = null;
			((LayoutButtonDummyCustomizationItem)this.object).setTexture(null);
		});
		texturePopup.addContent(tpop3);
		LayoutCreatorScreen.colorizeCreatorButton(tpop3);
		
		AdvancedButton b4 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.custombutton.config.texture"), (press) -> {
			texturePopup.openMenuAt(0, ((AdvancedButton)press).y);
		});
		this.rightclickMenu.addContent(b4);
		LayoutCreatorScreen.colorizeCreatorButton(b4);

		AdvancedButton b5 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.hoverlabel"), (press) -> {
			this.handler.setMenusUseable(false);
			TextInputPopup ip = new TextInputPopup(new Color(0, 0, 0, 0), "", null, 240, (call) -> {
				if (call != null) {
					if ((this.hoverLabel == null) || !this.hoverLabel.equals(StringUtils.convertFormatCodes(call, "&", "§"))) {
						this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
					}
					
					this.hoverLabel = StringUtils.convertFormatCodes(call, "&", "§");
				}
				this.handler.setMenusUseable(true);
			});
			
			if (this.hoverLabel != null) {
				ip.setText(StringUtils.convertFormatCodes(this.hoverLabel, "§", "&"));
			}
			PopupHandler.displayPopup(ip);
		});
		this.rightclickMenu.addContent(b5);
		LayoutCreatorScreen.colorizeCreatorButton(b5);

		AdvancedButton b6 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.hoverlabel.reset"), (press) -> {
			this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
			
			this.hoverLabel = null;
			this.rightclickMenu.closeMenu();
		});
		this.rightclickMenu.addContent(b6);
		LayoutCreatorScreen.colorizeCreatorButton(b6);

		AdvancedButton b7 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.hoversound"), (press) -> {
			this.handler.setMenusUseable(false);
			ChooseFilePopup cf = new ChooseFilePopup((call) -> {
				if (call != null) {
					File f = new File(call);
					if (f.exists() && f.isFile() && f.getName().endsWith(".wav")) {
						if ((this.hoverSound == null) || !this.hoverSound.equals(call)) {
							this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
						}
						
						this.hoverSound = call;
						this.handler.setMenusUseable(true);
					} else {
						this.handler.displayNotification(300, "§c§l" + Locals.localize("helper.creator.invalidaudio.title"), "", Locals.localize("helper.creator.invalidaudio.desc"), "", "", "", "", "", "");
					}
				} else {
					this.handler.setMenusUseable(true);
				}
			}, "wav");
			
			if (this.hoverSound != null) {
				cf.setText(this.hoverSound);
			}
			PopupHandler.displayPopup(cf);
		});
		this.rightclickMenu.addContent(b7);
		LayoutCreatorScreen.colorizeCreatorButton(b7);

		AdvancedButton b8 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.hoversound.reset"), (press) -> {
			this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
			
			this.hoverSound = null;
			this.rightclickMenu.closeMenu();
		});
		this.rightclickMenu.addContent(b8);
		LayoutCreatorScreen.colorizeCreatorButton(b8);

		AdvancedButton b10 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.clicksound"), (press) -> {
			this.handler.setMenusUseable(false);
			ChooseFilePopup cf = new ChooseFilePopup((call) -> {
				if (call != null) {
					File f = new File(call);
					if (f.exists() && f.isFile() && f.getName().endsWith(".wav")) {
						if ((this.clicksound == null) || !this.clicksound.equals(call)) {
							this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
						}
						
						this.clicksound = call;
						this.handler.setMenusUseable(true);
					} else {
						this.handler.displayNotification(300, "§c§l" + Locals.localize("helper.creator.invalidaudio.title"), "", Locals.localize("helper.creator.invalidaudio.desc"), "", "", "", "", "", "");
					}
				} else {
					this.handler.setMenusUseable(true);
				}
			}, "wav");
			
			if (this.clicksound != null) {
				cf.setText(this.clicksound);
			}
			PopupHandler.displayPopup(cf);
		});
		this.rightclickMenu.addContent(b10);
		LayoutCreatorScreen.colorizeCreatorButton(b10);

		AdvancedButton b11 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.clicksound.reset"), (press) -> {
			this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
			
			this.clicksound = null;
			this.rightclickMenu.closeMenu();
		});
		this.rightclickMenu.addContent(b11);
		LayoutCreatorScreen.colorizeCreatorButton(b11);

		AdvancedButton b9 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.delayappearance"), (press) -> {
			this.handler.setMenusUseable(false);
			TextInputPopup in = new HideForPopup(Locals.localize("helper.creator.items.button.delayappearance.desc"), CharacterFilter.getDoubleCharacterFiler(), 240, this);
			
			in.setText("" + this.hideforsec);
			PopupHandler.displayPopup(in);
		});
		this.rightclickMenu.addContent(b9);
		LayoutCreatorScreen.colorizeCreatorButton(b9);
		
		AdvancedButton b12 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.btndescription"), (press) -> {
			this.handler.setMenusUseable(false);
			TextInputPopup in = new TextInputPopup(new Color(0, 0, 0, 0), Locals.localize("helper.creator.items.button.btndescription"), null, 240, (call) -> {
				if (call != null) {
					if ((this.description == null) || (call == null) || !this.description.equals(call)) {
						this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
					}
					this.description = call;
					if (call.equals("")) {
						this.description = null;
					}
				}
				this.handler.setMenusUseable(true);
			});
			
			if (this.description != null) {
				in.setText(this.description);
			}
			PopupHandler.displayPopup(in);
		});
		b12.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.items.button.btndescription.desc"), "%n%"));
		this.rightclickMenu.addContent(b12);
		LayoutCreatorScreen.colorizeCreatorButton(b12);

	}

	private void initOnlyDisplayInMenu() {
		ContextMenu onlyDisplayInMenu = new ContextMenu(100, 16, -1);
		this.rightclickMenu.addChild(onlyDisplayInMenu);
		
		String outgame = Locals.localize("helper.creator.items.custombutton.onlydisplayin.outgame");
		if ((this.onlydisplayin != null) && this.onlydisplayin.equals("outgame")) {
			outgame = "§a" + outgame;
		}
		onlyOutgameBtn = new AdvancedButton(0, 0, 0, 0, outgame, (press) -> {
			this.onlydisplayin = "outgame";
			press.displayString = "§a" + Locals.localize("helper.creator.items.custombutton.onlydisplayin.outgame");
			this.onlySingleplayerBtn.displayString = Locals.localize("helper.creator.items.custombutton.onlydisplayin.singleplayer");
			this.onlyMultiplayerBtn.displayString = Locals.localize("helper.creator.items.custombutton.onlydisplayin.multiplayer");
		});
		onlyDisplayInMenu.addContent(onlyOutgameBtn);
		LayoutCreatorScreen.colorizeCreatorButton(onlyOutgameBtn);
		
		String sp = Locals.localize("helper.creator.items.custombutton.onlydisplayin.singleplayer");
		if ((this.onlydisplayin != null) && this.onlydisplayin.equals("singleplayer")) {
			sp = "§a" + sp;
		}
		onlySingleplayerBtn = new AdvancedButton(0, 0, 0, 0, sp, (press) -> {
			this.onlydisplayin = "singleplayer";
			press.displayString = "§a" + Locals.localize("helper.creator.items.custombutton.onlydisplayin.singleplayer");
			this.onlyOutgameBtn.displayString = Locals.localize("helper.creator.items.custombutton.onlydisplayin.outgame");
			this.onlyMultiplayerBtn.displayString = Locals.localize("helper.creator.items.custombutton.onlydisplayin.multiplayer");
		});
		onlyDisplayInMenu.addContent(onlySingleplayerBtn);
		LayoutCreatorScreen.colorizeCreatorButton(onlySingleplayerBtn);
		
		String mp = Locals.localize("helper.creator.items.custombutton.onlydisplayin.multiplayer");
		if ((this.onlydisplayin != null) && this.onlydisplayin.equals("multiplayer")) {
			mp = "§a" + mp;
		}
		onlyMultiplayerBtn = new AdvancedButton(0, 0, 0, 0, mp, (press) -> {
			this.onlydisplayin = "multiplayer";
			press.displayString = "§a" + Locals.localize("helper.creator.items.custombutton.onlydisplayin.multiplayer");
			this.onlySingleplayerBtn.displayString = Locals.localize("helper.creator.items.custombutton.onlydisplayin.singleplayer");
			this.onlyOutgameBtn.displayString = Locals.localize("helper.creator.items.custombutton.onlydisplayin.outgame");
		});
		onlyDisplayInMenu.addContent(onlyMultiplayerBtn);
		LayoutCreatorScreen.colorizeCreatorButton(onlyMultiplayerBtn);
		
		AdvancedButton odiResetBtn = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.items.custombutton.onlydisplayin.reset"), (press) -> {
			this.onlydisplayin = null;
			this.onlyMultiplayerBtn.displayString = Locals.localize("helper.creator.items.custombutton.onlydisplayin.multiplayer");
			this.onlySingleplayerBtn.displayString = Locals.localize("helper.creator.items.custombutton.onlydisplayin.singleplayer");
			this.onlyOutgameBtn.displayString = Locals.localize("helper.creator.items.custombutton.onlydisplayin.outgame");
		});
		onlyDisplayInMenu.addContent(odiResetBtn);
		LayoutCreatorScreen.colorizeCreatorButton(odiResetBtn);

		AdvancedButton b10 = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.items.custombutton.onlydisplayin"), (press) -> {
			onlyDisplayInMenu.openMenuAt(0, press.y);
		});
		this.rightclickMenu.addContent(b10);
		LayoutCreatorScreen.colorizeCreatorButton(b10);
	}
	
	private void editLabelCallback(String text) {
		if (text == null) {
			this.handler.setMenusUseable(true);
			return;
		} else {
			if ((this.object.value == null) || !this.object.value.equals(StringUtils.convertFormatCodes(text, "&", "§"))) {
				this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
			}
			
			this.object.value = StringUtils.convertFormatCodes(text, "&", "§");
		}
		this.handler.setMenusUseable(true);
	}
	
	private void setActionContentCallback(String content) {
		this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
				
		if (content != null) {
			this.actionContent = content;
		}
		this.handler.setMenusUseable(true);
	}
	
	private void setActionTypeCallback(String action) {
		this.actionType = action;
		this.handler.setMenusUseable(true);
	}

	@Override
	public List<PropertiesSection> getProperties() {
		List<PropertiesSection> l = new ArrayList<PropertiesSection>();
		if (this.actionType != null) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "addbutton");
			s.addEntry("label", this.object.value);
			if (this.stretchX) {
				s.addEntry("x", "0");
				s.addEntry("width", "%guiwidth%");
			} else {
				s.addEntry("x", "" + this.object.posX);
				s.addEntry("width", "" + this.object.width);
			}
			if (this.stretchY) {
				s.addEntry("y", "0");
				s.addEntry("height", "%guiheight%");
			} else {
				s.addEntry("y", "" + this.object.posY);
				s.addEntry("height", "" + this.object.height);
			}
			s.addEntry("orientation", this.object.orientation);
			s.addEntry("buttonaction", this.actionType);
			s.addEntry("value", this.actionContent);
			if ((this.backHovered != null) && (this.backNormal != null)) {
				s.addEntry("backgroundnormal", this.backNormal);
				s.addEntry("backgroundhovered", this.backHovered);
			}
			if (this.hoverSound != null) {
				s.addEntry("hoversound", this.hoverSound);
			}
			if (this.hoverLabel != null) {
				s.addEntry("hoverlabel", this.hoverLabel);
			}
			if (this.hideforsec != 0) {
				s.addEntry("hideforseconds", "" + this.hideforsec);
				if (this.delayonlyfirsttime) {
					s.addEntry("delayonlyfirsttime", "true");
				}
			}
			if (this.onlydisplayin != null) {
				s.addEntry("onlydisplayin", this.onlydisplayin);
			}
			if (this.clicksound != null) {
				s.addEntry("clicksound", this.clicksound);
			}
			if (this.description != null) {
				s.addEntry("description", this.description);
			}
			l.add(s);
		}
		
		return l;
	}

	@Override
	public void setAppearanceDelay(String sec, boolean onlyfirsttime) {
		if (sec != null) {
			if (MathUtils.isDouble(sec)) {
				double s = Double.parseDouble(sec);
				if ((this.hideforsec != s) || (this.delayonlyfirsttime != onlyfirsttime)) {
					this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
				}
				
				this.hideforsec = s;
				this.delayonlyfirsttime = onlyfirsttime;
				this.handler.setMenusUseable(true);
			} else {
				this.handler.displayNotification(300, Locals.localize("helper.creator.items.button.delayappearance.invalidvalue"));
			}
		} else {
			this.handler.setMenusUseable(true);
		}
	}

	@Override
	public boolean isDelayedOnlyFirstTime() {
		return this.delayonlyfirsttime;
	}

	@Override
	public double getAppearanceDelay() {
		return this.hideforsec;
	}
	
}
