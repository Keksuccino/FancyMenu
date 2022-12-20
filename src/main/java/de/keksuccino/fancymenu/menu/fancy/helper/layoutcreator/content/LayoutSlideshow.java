package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content;

import java.util.ArrayList;
import java.util.List;

import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.item.SlideshowCustomizationItem;
import de.keksuccino.konkrete.properties.PropertiesSection;

public class LayoutSlideshow extends LayoutElement {
	
	public LayoutSlideshow(SlideshowCustomizationItem parent, LayoutEditorScreen handler) {
		super(parent, true, handler);
	}

	@Override
	public void init() {
		this.stretchable = true;
		super.init();
	}

	@Override
	public List<PropertiesSection> getProperties() {
		List<PropertiesSection> l = new ArrayList<PropertiesSection>();
		
		PropertiesSection p1 = new PropertiesSection("customization");
		p1.addEntry("actionid", this.object.getActionId());
		if (this.object.advancedPosX != null) {
			p1.addEntry("advanced_posx", this.object.advancedPosX);
		}
		if (this.object.advancedPosY != null) {
			p1.addEntry("advanced_posy", this.object.advancedPosY);
		}
		if (this.object.advancedWidth != null) {
			p1.addEntry("advanced_width", this.object.advancedWidth);
		}
		if (this.object.advancedHeight != null) {
			p1.addEntry("advanced_height", this.object.advancedHeight);
		}
		if (this.object.delayAppearance) {
			p1.addEntry("delayappearance", "true");
			p1.addEntry("delayappearanceeverytime", "" + this.object.delayAppearanceEverytime);
			p1.addEntry("delayappearanceseconds", "" + this.object.delayAppearanceSec);
			if (this.object.fadeIn) {
				p1.addEntry("fadein", "true");
				p1.addEntry("fadeinspeed", "" + this.object.fadeInSpeed);
			}
		}
		p1.addEntry("action", "addslideshow");
		p1.addEntry("name", this.object.value);
		p1.addEntry("orientation", this.object.orientation);
		if (this.object.orientation.equals("element") && (this.object.orientationElementIdentifier != null)) {
			p1.addEntry("orientation_element", this.object.orientationElementIdentifier);
		}
		if (this.stretchX) {
			p1.addEntry("x", "0");
			p1.addEntry("width", "%guiwidth%");
		} else {
			p1.addEntry("x", "" + this.object.posX);
			
			p1.addEntry("width", "" + this.object.getWidth());
		}
		if (this.stretchY) {
			p1.addEntry("y", "0");
			p1.addEntry("height", "%guiheight%");
		} else {
			p1.addEntry("y", "" + this.object.posY);
			
			p1.addEntry("height", "" + this.object.getHeight());
		}

		
		this.addVisibilityPropertiesTo(p1);

		l.add(p1);
		
		return l;
	}

}
