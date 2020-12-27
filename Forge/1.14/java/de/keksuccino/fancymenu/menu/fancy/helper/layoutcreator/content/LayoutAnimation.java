package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import de.keksuccino.core.gui.screens.popup.PopupHandler;
import de.keksuccino.core.gui.screens.popup.YesNoPopup;
import de.keksuccino.core.properties.PropertiesSection;
import de.keksuccino.fancymenu.localization.Locals;
import de.keksuccino.fancymenu.menu.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutCreatorScreen;
import de.keksuccino.fancymenu.menu.fancy.item.AnimationCustomizationItem;

public class LayoutAnimation extends LayoutObject {
	
	public LayoutAnimation(AnimationCustomizationItem parent, LayoutCreatorScreen handler) {
		super(parent, true, handler);
	}

	@Override
	public List<PropertiesSection> getProperties() {
		List<PropertiesSection> l = new ArrayList<PropertiesSection>();
		
		PropertiesSection p1 = new PropertiesSection("customization");
		p1.addEntry("action", "addanimation");
		p1.addEntry("name", this.object.value);
		p1.addEntry("x", "" + this.object.posX);
		p1.addEntry("y", "" + this.object.posY);
		p1.addEntry("orientation", this.object.orientation);
		p1.addEntry("width", "" + this.object.width);
		p1.addEntry("height", "" + this.object.height);
		l.add(p1);
		
		return l;
	}
	
	@Override
	public void destroyObject() {
		if (!this.isDestroyable()) {
			return;
		}
		this.handler.setMenusUseable(false);
		PopupHandler.displayPopup(new YesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
			if (call.booleanValue()) {
				this.handler.removeContent(this);
				((AdvancedAnimation)((AnimationCustomizationItem)this.object).renderer).stopAudio();
			}
			this.handler.setMenusUseable(true);
		}, "§c§l" + Locals.localize("helper.creator.messages.sure"), "", Locals.localize("helper.creator.deleteobject"), "", "", "", "", ""));
	}

}
