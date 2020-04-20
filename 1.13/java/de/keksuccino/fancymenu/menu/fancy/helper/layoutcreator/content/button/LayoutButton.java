package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import de.keksuccino.core.gui.content.AdvancedButton;
import de.keksuccino.core.gui.screens.popup.PopupHandler;
import de.keksuccino.core.gui.screens.popup.TextInputPopup;
import de.keksuccino.core.properties.PropertiesSection;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutCreatorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutObject;

public class LayoutButton extends LayoutObject {
	
	private String actionContent = "";
	private int actionType = 0;
	
	public LayoutButton(int width, int height, @Nonnull String label, LayoutCreatorScreen handler) {
		super(new LayoutButtonDummyCustomizationItem(label, width, height, 0, 0), true, handler);
	}

	@Override
	protected void init() {
		super.init();
		
		AdvancedButton b2 = new AdvancedButton(0, 0, 0, 16, "Edit Label", (press) -> {
			this.handler.setMenusUseable(false);
			TextInputPopup i = new TextInputPopup(new Color(0, 0, 0, 0), "§lEdit Label:", null, 240, this::editLabelCallback);
			i.setText(this.object.value);
			PopupHandler.displayPopup(i);
		});
		this.rightclickMenu.addContent(b2);
		LayoutCreatorScreen.colorizeCreatorButton(b2);
		
		AdvancedButton b3 = new AdvancedButton(0, 0, 0, 16, "Button Configuration", (press) -> {
			this.handler.setMenusUseable(false);
			ButtonActionPopup i = new ButtonActionPopup(this::setActionContentCallback, this::setActionTypeCallback, this.actionType);
			i.setText(this.actionContent);
			PopupHandler.displayPopup(i);
		});
		this.rightclickMenu.addContent(b3);
		LayoutCreatorScreen.colorizeCreatorButton(b3);
		
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
	
	private void setActionTypeCallback(int action) {
		if (action > -1) {
			this.actionType = action;
		}
		this.handler.setMenusUseable(true);
	}
	
	@Override
	public List<PropertiesSection> getProperties() {
		List<PropertiesSection> l = new ArrayList<PropertiesSection>();
		PropertiesSection s = new PropertiesSection("customization");
		s.addEntry("action", "addbutton");
		s.addEntry("label", this.object.value);
		s.addEntry("x", "" + this.object.posX);
		s.addEntry("y", "" + this.object.posY);
		s.addEntry("orientation", this.object.orientation);
		s.addEntry("width", "" + this.object.width);
		s.addEntry("height", "" + this.object.height);
		if (this.actionType == 1) {
			s.addEntry("buttonaction", "openlink");
		} else {
			s.addEntry("buttonaction", "sendmessage");
		}
		s.addEntry("value", this.actionContent);
		l.add(s);
		
		return l;
	}
	
}
