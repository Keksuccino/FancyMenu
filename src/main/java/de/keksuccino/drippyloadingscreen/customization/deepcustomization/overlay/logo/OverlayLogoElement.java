package de.keksuccino.drippyloadingscreen.customization.deepcustomization.overlay.logo;

import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationLayer;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationLayoutEditorElement;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.resources.language.I18n;

public class OverlayLogoElement extends DeepCustomizationElement {

    public OverlayLogoElement(DeepCustomizationLayer parentLayer) {
        super("drippy_overlay_logo", parentLayer);
    }

    @Override
    public DeepCustomizationItem constructDefaultItemInstance() {
        return new OverlayLogoItem(this, new PropertiesSection(""));
    }

    @Override
    public DeepCustomizationItem constructCustomizedItemInstance(PropertiesSection serializedItem) {
        return new OverlayLogoItem(this, serializedItem);
    }

    @Override
    public DeepCustomizationLayoutEditorElement constructEditorElementInstance(DeepCustomizationItem item, LayoutEditorScreen handler) {
        return new OverlayLogoLayoutElement(item.parentElement, item, handler);
    }

    @Override
    public String getDisplayName() {
        return I18n.get("drippyloadingscreen.deepcustomization.overlay.logo.display_name");
    }

}