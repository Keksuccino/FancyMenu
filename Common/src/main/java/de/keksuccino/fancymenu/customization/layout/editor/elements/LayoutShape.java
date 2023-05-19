//package de.keksuccino.fancymenu.customization.layout.editor.elements;
//
//import java.awt.Color;
//import java.util.ArrayList;
//import java.util.List;
//
//import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
//import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
//import de.keksuccino.fancymenu.rendering.ui.popup.FMTextInputPopup;
//import de.keksuccino.fancymenu.customization.element.AbstractElement;
//import de.keksuccino.fancymenu.customization.element.v1.ShapeCustomizationItem;
//import de.keksuccino.konkrete.gui.content.AdvancedButton;
//import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
//import de.keksuccino.fancymenu.utils.LocalizationUtils;
//import net.minecraft.client.resources.language.I18n;
//import de.keksuccino.fancymenu.properties.PropertyContainer;
//import de.keksuccino.konkrete.rendering.RenderUtils;
//
//public class LayoutShape extends AbstractEditorElement {
//
//	public LayoutShape(AbstractElement object, LayoutEditorScreen handler) {
//		super(object, true, handler);
//	}
//
//	@Override
//	public void init() {
//		this.stretchable = true;
//		super.init();
//
//		this.rightClickContextMenu.setAutoclose(true);
//
//		AdvancedButton colorB = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.items.shape.color"), true, (press) -> {
//
//			FMTextInputPopup t = new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + I18n.get("fancymenu.editor.items.shape.color") + ":", null, 240, (call) -> {
//				if (call != null) {
//					if (!call.equals("")) {
//						Color c = RenderUtils.getColorFromHexString(call);
//						if (c != null) {
//
//							if (!this.getObject().getColorString().equalsIgnoreCase(call)) {
//								this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//							}
//
//							this.getObject().setColor(call);
//
//						}
//					} else {
//						if (!this.getObject().getColorString().equalsIgnoreCase("#ffffff")) {
//							this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//						}
//
//						this.getObject().setColor("#ffffff");
//					}
//				}
//
//			});
//
//			t.setText(this.getObject().getColorString());
//
//			PopupHandler.displayPopup(t);
//
//		});
//		colorB.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.editor.items.shape.color.btndesc")));
//		this.rightClickContextMenu.addContent(colorB);
//
//	}
//
//	@Override
//	public List<PropertyContainer> getProperties() {
//		List<PropertyContainer> l = new ArrayList<PropertyContainer>();
//		PropertyContainer s = new PropertyContainer("customization");
//
//		if (this.getObject().shape != null) {
//
//			s.putProperty("action", "addshape");
//			s.putProperty("actionid", this.element.getInstanceIdentifier());
//			if (this.element.advancedX != null) {
//				s.putProperty("advanced_posx", this.element.advancedX);
//			}
//			if (this.element.advancedY != null) {
//				s.putProperty("advanced_posy", this.element.advancedY);
//			}
//			if (this.element.advancedWidth != null) {
//				s.putProperty("advanced_width", this.element.advancedWidth);
//			}
//			if (this.element.advancedHeight != null) {
//				s.putProperty("advanced_height", this.element.advancedHeight);
//			}
//			if (this.element.delayAppearance) {
//				s.putProperty("delayappearance", "true");
//				s.putProperty("delayappearanceeverytime", "" + this.element.delayAppearanceEverytime);
//				s.putProperty("delayappearanceseconds", "" + this.element.appearanceDelayInSeconds);
//				if (this.element.fadeIn) {
//					s.putProperty("fadein", "true");
//					s.putProperty("fadeinspeed", "" + this.element.fadeInSpeed);
//				}
//			}
//			s.putProperty("shape", this.getObject().shape.name);
//			s.putProperty("color", this.getObject().getColorString());
//			s.putProperty("orientation", this.element.anchorPoint);
//			if (this.element.anchorPoint.equals("element") && (this.element.anchorPointElementIdentifier != null)) {
//				s.putProperty("orientation_element", this.element.anchorPointElementIdentifier);
//			}
//			if (this.stretchX) {
//				s.putProperty("x", "0");
//				s.putProperty("width", "%guiwidth%");
//			} else {
//				s.putProperty("x", "" + this.element.baseX);
//				s.putProperty("width", "" + this.element.getWidth());
//			}
//			if (this.stretchY) {
//				s.putProperty("y", "0");
//				s.putProperty("height", "%guiheight%");
//			} else {
//				s.putProperty("y", "" + this.element.baseY);
//				s.putProperty("height", "" + this.element.getHeight());
//			}
//
//			this.serializeLoadingRequirementsTo(s);
//
//			l.add(s);
//		}
//
//		return l;
//	}
//
//	protected ShapeCustomizationItem getObject() {
//		return (ShapeCustomizationItem) this.element;
//	}
//
//}
