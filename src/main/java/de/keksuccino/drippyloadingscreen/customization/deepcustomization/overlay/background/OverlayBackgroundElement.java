package de.keksuccino.drippyloadingscreen.customization.deepcustomization.overlay.background;

import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationLayer;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationLayoutEditorElement;
import de.keksuccino.konkrete.properties.PropertiesSection;

public class OverlayBackgroundElement extends DeepCustomizationElement {

    public OverlayBackgroundElement(DeepCustomizationLayer parentLayer) {
        super("drippy_overlay_background", parentLayer);
    }

    @Override
    public DeepCustomizationItem constructDefaultItemInstance() {
        return new OverlayBackgroundItem(this, new PropertiesSection(""));
    }

    @Override
    public DeepCustomizationItem constructCustomizedItemInstance(PropertiesSection serializedItem) {
        return new OverlayBackgroundItem(this, serializedItem);
    }

    @Override
    public DeepCustomizationLayoutEditorElement constructEditorElementInstance(DeepCustomizationItem item, LayoutEditorScreen handler) {
        return new OverlayBackgroundLayoutElement(item.parentElement, item, handler);
    }

    @Override
    public String getDisplayName() {
        return "";
    }

}