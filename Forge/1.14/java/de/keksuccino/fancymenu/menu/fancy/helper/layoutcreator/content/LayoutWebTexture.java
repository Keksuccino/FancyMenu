package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content;

import java.util.ArrayList;
import java.util.List;

import de.keksuccino.core.properties.PropertiesSection;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutCreatorScreen;
import de.keksuccino.fancymenu.menu.fancy.item.WebTextureCustomizationItem;

public class LayoutWebTexture extends LayoutObject {
	
	public LayoutWebTexture(WebTextureCustomizationItem parent, LayoutCreatorScreen handler) {
		super(parent, true, handler);
	}

	@Override
	public List<PropertiesSection> getProperties() {
		List<PropertiesSection> l = new ArrayList<PropertiesSection>();
		
		PropertiesSection p1 = new PropertiesSection("customization");
		p1.addEntry("action", "addwebtexture");
		p1.addEntry("url", this.object.value);
		p1.addEntry("x", "" + this.object.posX);
		p1.addEntry("y", "" + this.object.posY);
		p1.addEntry("orientation", this.object.orientation);
		p1.addEntry("width", "" + this.object.width);
		p1.addEntry("height", "" + this.object.height);
		l.add(p1);
		
		return l;
	}

}
