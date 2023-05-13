package de.keksuccino.fancymenu.customization.layout.editor.elements;

import java.util.ArrayList;
import java.util.List;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.element.v1.TextureCustomizationItem;
import de.keksuccino.fancymenu.properties.PropertyContainer;

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
	public List<PropertyContainer> getProperties() {
		List<PropertyContainer> l = new ArrayList<PropertyContainer>();
		
		PropertyContainer p1 = new PropertyContainer("customization");
		p1.putProperty("action", "addtexture");
		p1.putProperty("actionid", this.element.getInstanceIdentifier());
		if (this.element.advancedX != null) {
			p1.putProperty("advanced_posx", this.element.advancedX);
		}
		if (this.element.advancedY != null) {
			p1.putProperty("advanced_posy", this.element.advancedY);
		}
		if (this.element.advancedWidth != null) {
			p1.putProperty("advanced_width", this.element.advancedWidth);
		}
		if (this.element.advancedHeight != null) {
			p1.putProperty("advanced_height", this.element.advancedHeight);
		}
		if (this.element.delayAppearance) {
			p1.putProperty("delayappearance", "true");
			p1.putProperty("delayappearanceeverytime", "" + this.element.delayAppearanceEverytime);
			p1.putProperty("delayappearanceseconds", "" + this.element.appearanceDelayInSeconds);
			if (this.element.fadeIn) {
				p1.putProperty("fadein", "true");
				p1.putProperty("fadeinspeed", "" + this.element.fadeInSpeed);
			}
		}
		p1.putProperty("path", this.element.value);
		p1.putProperty("orientation", this.element.anchorPoint);
		if (this.element.anchorPoint.equals("element") && (this.element.anchorPointElementIdentifier != null)) {
			p1.putProperty("orientation_element", this.element.anchorPointElementIdentifier);
		}
		if (this.stretchX) {
			p1.putProperty("x", "0");
			p1.putProperty("width", "%guiwidth%");
		} else {
			p1.putProperty("x", "" + this.element.baseX);
			p1.putProperty("width", "" + this.element.getWidth());
		}
		if (this.stretchY) {
			p1.putProperty("y", "0");
			p1.putProperty("height", "%guiheight%");
		} else {
			p1.putProperty("y", "" + this.element.baseY);
			p1.putProperty("height", "" + this.element.getHeight());
		}

		this.serializeLoadingRequirementsTo(p1);

		l.add(p1);
		
		return l;
	}

}
