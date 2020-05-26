package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import de.keksuccino.core.filechooser.FileChooser;
import de.keksuccino.core.gui.content.AdvancedButton;
import de.keksuccino.core.gui.content.PopupMenu;
import de.keksuccino.core.gui.screens.popup.PopupHandler;
import de.keksuccino.core.gui.screens.popup.TextInputPopup;
import de.keksuccino.core.properties.PropertiesSection;
import de.keksuccino.core.resources.TextureHandler;
import de.keksuccino.fancymenu.localization.Locals;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutCreatorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutObject;

public class LayoutButton extends LayoutObject {
	
	private String actionContent = "";
	private String actionType = null;
	private String backNormal = null;
	private String backHovered = null;
	
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
			FileChooser.askForFile(new File("").getAbsoluteFile(), (call) -> {
				if (call != null) {
					File home = new File("");
					String path = call.getPath();
					if (path.startsWith(home.getAbsolutePath())) {
						path = path.replace(home.getAbsolutePath(), "");
						if (path.startsWith("\\") || path.startsWith("/")) {
							path = path.substring(1);
						}
					}
					this.backNormal = path;
					if (this.backHovered == null) {
						this.backHovered = path;
					}
					((LayoutButtonDummyCustomizationItem)this.object).setTexture(TextureHandler.getResource(this.backNormal).getResourceLocation());
				}
			}, "jpg", "jpeg", "png");
		});
		texturePopup.addContent(tpop1);
		LayoutCreatorScreen.colorizeCreatorButton(tpop1);
		
		AdvancedButton tpop2 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.custombutton.config.texture.hovered"), (press) -> {
			FileChooser.askForFile(new File("").getAbsoluteFile(), (call) -> {
				if (call != null) {
					File home = new File("");
					String path = call.getPath();
					if (path.startsWith(home.getAbsolutePath())) {
						path = path.replace(home.getAbsolutePath(), "");
						if (path.startsWith("\\") || path.startsWith("/")) {
							path = path.substring(1);
						}
					}
					this.backHovered = path;
					if (this.backNormal == null) {
						this.backNormal = path;
					}
					((LayoutButtonDummyCustomizationItem)this.object).setTexture(TextureHandler.getResource(this.backNormal).getResourceLocation());
				}
			}, "jpg", "jpeg", "png");
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
			l.add(s);
		}
		
		return l;
	}
	
}
