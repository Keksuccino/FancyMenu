package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content;

import java.util.ArrayList;
import java.util.List;

import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutCreatorScreen;
import de.keksuccino.fancymenu.menu.fancy.item.TextureCustomizationItem;
import de.keksuccino.konkrete.properties.PropertiesSection;

public class LayoutTexture extends LayoutObject {
	
	public LayoutTexture(TextureCustomizationItem parent, LayoutCreatorScreen handler) {
		super(parent, true, handler);
	}

	@Override
	public List<PropertiesSection> getProperties() {
		List<PropertiesSection> l = new ArrayList<PropertiesSection>();
		
		PropertiesSection p1 = new PropertiesSection("customization");
		p1.addEntry("action", "addtexture");
		p1.addEntry("path", this.object.value);
		p1.addEntry("x", "" + this.object.posX);
		p1.addEntry("y", "" + this.object.posY);
		p1.addEntry("orientation", this.object.orientation);
		p1.addEntry("width", "" + this.object.width);
		p1.addEntry("height", "" + this.object.height);
		l.add(p1);
		
		return l;
	}

}
