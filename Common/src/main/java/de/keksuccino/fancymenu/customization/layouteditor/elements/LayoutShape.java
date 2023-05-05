package de.keksuccino.fancymenu.customization.layouteditor.elements;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import de.keksuccino.fancymenu.customization.element.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layouteditor.LayoutEditorScreen;
import de.keksuccino.fancymenu.rendering.ui.popup.FMTextInputPopup;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.v1.ShapeCustomizationItem;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.rendering.RenderUtils;

public class LayoutShape extends AbstractEditorElement {

	public LayoutShape(AbstractElement object, LayoutEditorScreen handler) {
		super(object, true, handler);
	}

	@Override
	public void init() {
		this.stretchable = true;
		super.init();
		
		this.rightClickContextMenu.setAutoclose(true);
		
		AdvancedButton colorB = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.shape.color"), true, (press) -> {

			FMTextInputPopup t = new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.items.shape.color") + ":", null, 240, (call) -> {
				if (call != null) {
					if (!call.equals("")) {
						Color c = RenderUtils.getColorFromHexString(call);
						if (c != null) {
							
							if (!this.getObject().getColorString().equalsIgnoreCase(call)) {
								this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
							}
							
							this.getObject().setColor(call);
							
						}
					} else {
						if (!this.getObject().getColorString().equalsIgnoreCase("#ffffff")) {
							this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
						}
						
						this.getObject().setColor("#ffffff");
					}
				}

			});
			
			t.setText(this.getObject().getColorString());
			
			PopupHandler.displayPopup(t);
			
		});
		colorB.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.items.shape.color.btndesc"), "%n%"));
		this.rightClickContextMenu.addContent(colorB);
		
	}
	
	@Override
	public List<PropertiesSection> getProperties() {
		List<PropertiesSection> l = new ArrayList<PropertiesSection>();
		PropertiesSection s = new PropertiesSection("customization");
		
		if (this.getObject().shape != null) {
			
			s.addEntry("action", "addshape");
			s.addEntry("actionid", this.element.getInstanceIdentifier());
			if (this.element.advancedX != null) {
				s.addEntry("advanced_posx", this.element.advancedX);
			}
			if (this.element.advancedY != null) {
				s.addEntry("advanced_posy", this.element.advancedY);
			}
			if (this.element.advancedWidth != null) {
				s.addEntry("advanced_width", this.element.advancedWidth);
			}
			if (this.element.advancedHeight != null) {
				s.addEntry("advanced_height", this.element.advancedHeight);
			}
			if (this.element.delayAppearance) {
				s.addEntry("delayappearance", "true");
				s.addEntry("delayappearanceeverytime", "" + this.element.delayAppearanceEverytime);
				s.addEntry("delayappearanceseconds", "" + this.element.delayAppearanceSec);
				if (this.element.fadeIn) {
					s.addEntry("fadein", "true");
					s.addEntry("fadeinspeed", "" + this.element.fadeInSpeed);
				}
			}
			s.addEntry("shape", this.getObject().shape.name);
			s.addEntry("color", this.getObject().getColorString());
			s.addEntry("orientation", this.element.anchorPoint);
			if (this.element.anchorPoint.equals("element") && (this.element.anchorPointElementIdentifier != null)) {
				s.addEntry("orientation_element", this.element.anchorPointElementIdentifier);
			}
			if (this.stretchX) {
				s.addEntry("x", "0");
				s.addEntry("width", "%guiwidth%");
			} else {
				s.addEntry("x", "" + this.element.rawX);
				s.addEntry("width", "" + this.element.getWidth());
			}
			if (this.stretchY) {
				s.addEntry("y", "0");
				s.addEntry("height", "%guiheight%");
			} else {
				s.addEntry("y", "" + this.element.rawY);
				s.addEntry("height", "" + this.element.getHeight());
			}

			this.serializeLoadingRequirementsTo(s);
			
			l.add(s);
		}
		
		return l;
	}
	
	protected ShapeCustomizationItem getObject() {
		return (ShapeCustomizationItem) this.element;
	}

}
