package de.keksuccino.fancymenu.customization.layout.editor.elements;

import java.util.ArrayList;
import java.util.List;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.element.v1.WebTextureCustomizationItem;
import de.keksuccino.konkrete.properties.PropertiesSection;

public class LayoutWebTexture extends AbstractEditorElement {
	
	public LayoutWebTexture(WebTextureCustomizationItem parent, LayoutEditorScreen handler) {
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
		p1.addEntry("action", "addwebtexture");
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
			p1.addEntry("delayappearanceseconds", "" + this.element.appearanceDelayInSeconds);
			if (this.element.fadeIn) {
				p1.addEntry("fadein", "true");
				p1.addEntry("fadeinspeed", "" + this.element.fadeInSpeed);
			}
		}
		p1.addEntry("url", ((WebTextureCustomizationItem)this.element).rawURL);
		p1.addEntry("orientation", this.element.anchorPoint);
		if (this.element.anchorPoint.equals("element") && (this.element.anchorPointElementIdentifier != null)) {
			p1.addEntry("orientation_element", this.element.anchorPointElementIdentifier);
		}
		if (this.stretchX) {
			p1.addEntry("x", "0");
			p1.addEntry("width", "%guiwidth%");
		} else {
			p1.addEntry("x", "" + this.element.baseX);
			p1.addEntry("width", "" + this.element.getWidth());
		}
		if (this.stretchY) {
			p1.addEntry("y", "0");
			p1.addEntry("height", "%guiheight%");
		} else {
			p1.addEntry("y", "" + this.element.baseY);
			p1.addEntry("height", "" + this.element.getHeight());
		}

		this.serializeLoadingRequirementsTo(p1);

		l.add(p1);
		
		return l;
	}

	@Override
	protected void handleResize(int mouseX, int mouseY) {
		if (((WebTextureCustomizationItem)this.element).ready) {
			super.handleResize(mouseX, mouseY);
		}
	}

}
