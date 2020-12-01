package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import de.keksuccino.fancymenu.menu.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutCreatorScreen;
import de.keksuccino.fancymenu.menu.fancy.item.AnimationCustomizationItem;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.gui.screens.popup.YesNoPopup;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;

public class LayoutAnimation extends LayoutObject {
	
	public LayoutAnimation(AnimationCustomizationItem parent, LayoutCreatorScreen handler) {
		super(parent, true, handler);
	}

	@Override
	protected void init() {
		this.stretchable = true;
		super.init();
	}

	@Override
	public List<PropertiesSection> getProperties() {
		List<PropertiesSection> l = new ArrayList<PropertiesSection>();
		
		PropertiesSection p1 = new PropertiesSection("customization");
		p1.addEntry("action", "addanimation");
		p1.addEntry("name", this.object.value);
		p1.addEntry("orientation", this.object.orientation);
		if (this.stretchX) {
			p1.addEntry("x", "0");
			p1.addEntry("width", "%guiwidth%");
		} else {
			p1.addEntry("x", "" + this.object.posX);
			p1.addEntry("width", "" + this.object.width);
		}
		if (this.stretchY) {
			p1.addEntry("y", "0");
			p1.addEntry("height", "%guiheight%");
		} else {
			p1.addEntry("y", "" + this.object.posY);
			p1.addEntry("height", "" + this.object.height);
		}
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
				if ((AdvancedAnimation)((AnimationCustomizationItem)this.object).renderer != null) {
					((AdvancedAnimation)((AnimationCustomizationItem)this.object).renderer).stopAudio();
				}
			}
			this.handler.setMenusUseable(true);
		}, "§c§l" + Locals.localize("helper.creator.messages.sure"), "", Locals.localize("helper.creator.deleteobject"), "", "", "", "", ""));
	}

}
