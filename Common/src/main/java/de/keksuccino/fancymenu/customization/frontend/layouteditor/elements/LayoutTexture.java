package de.keksuccino.fancymenu.customization.frontend.layouteditor.elements;

import java.util.ArrayList;
import java.util.List;

import de.keksuccino.fancymenu.customization.backend.element.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.backend.element.v1.TextureCustomizationItem;
import de.keksuccino.konkrete.properties.PropertiesSection;

public class LayoutTexture extends AbstractEditorElement {
	
	public LayoutTexture(TextureCustomizationItem parent, LayoutEditorScreen handler) {
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
		p1.addEntry("action", "addtexture");
		p1.addEntry("actionid", this.element.getInstanceIdentifier());
		if (this.element.advancedX != null) {
			p1.addEntry("advanced_posx", this.element.advancedX);
		}
		if (this.element.advancedY != null) {
			p1.addEntry("advanced_posy", this.element.advancedY);
		}
		if (this.element.advancedWidth != null) {
			p1.addEntry("advanced_width", this.element.advancedWidth);
		}
		if (this.element.advancedHeight != null) {
			p1.addEntry("advanced_height", this.element.advancedHeight);
		}
		if (this.element.delayAppearance) {
			p1.addEntry("delayappearance", "true");
			p1.addEntry("delayappearanceeverytime", "" + this.element.delayAppearanceEverytime);
			p1.addEntry("delayappearanceseconds", "" + this.element.delayAppearanceSec);
			if (this.element.fadeIn) {
				p1.addEntry("fadein", "true");
				p1.addEntry("fadeinspeed", "" + this.element.fadeInSpeed);
			}
		}
		p1.addEntry("path", this.element.value);
		p1.addEntry("orientation", this.element.orientation);
		if (this.element.orientation.equals("element") && (this.element.orientationElementIdentifier != null)) {
			p1.addEntry("orientation_element", this.element.orientationElementIdentifier);
		}
		if (this.stretchX) {
			p1.addEntry("x", "0");
			p1.addEntry("width", "%guiwidth%");
		} else {
			p1.addEntry("x", "" + this.element.rawX);
			p1.addEntry("width", "" + this.element.getWidth());
		}
		if (this.stretchY) {
			p1.addEntry("y", "0");
			p1.addEntry("height", "%guiheight%");
		} else {
			p1.addEntry("y", "" + this.element.rawY);
			p1.addEntry("height", "" + this.element.getHeight());
		}

		this.serializeLoadingRequirementsTo(p1);

		l.add(p1);
		
		return l;
	}

}
