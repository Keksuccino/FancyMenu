package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import de.keksuccino.core.gui.content.AdvancedButton;
import de.keksuccino.core.gui.content.PopupMenu;
import de.keksuccino.core.gui.screens.popup.PopupHandler;
import de.keksuccino.core.gui.screens.popup.TextInputPopup;
import de.keksuccino.core.input.CharacterFilter;
import de.keksuccino.core.math.MathUtils;
import de.keksuccino.core.properties.PropertiesSection;
import de.keksuccino.core.resources.TextureHandler;
import de.keksuccino.fancymenu.localization.Locals;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutCreatorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.ChooseFilePopup;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutObject;

public class LayoutButton extends LayoutObject {

	public String actionContent = "";
	public String actionType = "openlink";
	public String backNormal = null;
	public String backHovered = null;
	public String hoverSound;
	public String hoverLabel;
	public double hideforsec = 0;
	
	public LayoutButton(int width, int height, @Nonnull String label, LayoutCreatorScreen handler) {
		super(new LayoutButtonDummyCustomizationItem(label, width, height, 0, 0), true, handler);
	}

	@Override
	protected void init() {
		super.init();
		
		AdvancedButton b2 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.editlabel"), (press) -> {
			this.handler.setMenusUseable(false);
			TextInputPopup i = new TextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.items.button.editlabel") + ":", null, 240, this::editLabelCallback);
			i.setText(this.object.value);
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
		
		PopupMenu texturePopup = new PopupMenu(100, 16, -1);
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
			this.backHovered = null;
			this.backNormal = null;
			((LayoutButtonDummyCustomizationItem)this.object).setTexture(null);
		});
		texturePopup.addContent(tpop3);
		LayoutCreatorScreen.colorizeCreatorButton(tpop3);
		
		AdvancedButton b4 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.custombutton.config.texture"), (press) -> {
			texturePopup.openMenuAt(0, press.y);
		});
		this.rightclickMenu.addContent(b4);
		LayoutCreatorScreen.colorizeCreatorButton(b4);

		AdvancedButton b5 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.hoverlabel"), (press) -> {
			this.handler.setMenusUseable(false);
			TextInputPopup ip = new TextInputPopup(new Color(0, 0, 0, 0), "", null, 240, (call) -> {
				if (call != null) {
					this.hoverLabel = call;
				}
				this.handler.setMenusUseable(true);
			});
			
			if (this.hoverLabel != null) {
				ip.setText(this.hoverLabel);
			}
			PopupHandler.displayPopup(ip);
		});
		this.rightclickMenu.addContent(b5);
		LayoutCreatorScreen.colorizeCreatorButton(b5);

		AdvancedButton b6 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.hoverlabel.reset"), (press) -> {
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
			this.hoverSound = null;
			this.rightclickMenu.closeMenu();
		});
		this.rightclickMenu.addContent(b8);
		LayoutCreatorScreen.colorizeCreatorButton(b8);

		AdvancedButton b9 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.delayappearance"), (press) -> {
			this.handler.setMenusUseable(false);
			TextInputPopup in = new TextInputPopup(new Color(0, 0, 0, 0), Locals.localize("helper.creator.items.button.delayappearance.desc"), CharacterFilter.getDoubleCharacterFiler(), 240, (call) -> {
				if (call != null) {
					if (MathUtils.isDouble(call)) {
						this.hideforsec = Double.parseDouble(call);
						this.handler.setMenusUseable(true);
					} else {
						this.handler.displayNotification(300, Locals.localize("helper.creator.items.button.delayappearance.invalidvalue"));
					}
				} else {
					this.handler.setMenusUseable(true);
				}
			});
			in.setText("" + this.hideforsec);
			PopupHandler.displayPopup(in);
		});
		this.rightclickMenu.addContent(b9);
		LayoutCreatorScreen.colorizeCreatorButton(b9);
		
	}
	
	private void editLabelCallback(String text) {
		if (text == null) {
			this.handler.setMenusUseable(true);
			return;
		} else {
			this.object.value = text;
		}
		this.handler.setMenusUseable(true);
	}
	
	private void setActionContentCallback(String content) {
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
			s.addEntry("x", "" + this.object.posX);
			s.addEntry("y", "" + this.object.posY);
			s.addEntry("orientation", this.object.orientation);
			s.addEntry("width", "" + this.object.width);
			s.addEntry("height", "" + this.object.height);
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
			}
			l.add(s);
		}
		
		return l;
	}
	
}
