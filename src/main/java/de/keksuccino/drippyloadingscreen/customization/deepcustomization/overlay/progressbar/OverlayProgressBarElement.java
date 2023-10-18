package de.keksuccino.drippyloadingscreen.customization.deepcustomization.overlay.progressbar;

import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationLayer;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationLayoutEditorElement;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.resources.language.I18n;

public class OverlayProgressBarElement extends DeepCustomizationElement {

    public OverlayProgressBarElement(DeepCustomizationLayer parentLayer) {
        super("drippy_overlay_progress_bar", parentLayer);
    }

    @Override
    public DeepCustomizationItem constructDefaultItemInstance() {
        return new OverlayProgressBarItem(this, new PropertiesSection(""));
    }

    @Override
    public DeepCustomizationItem constructCustomizedItemInstance(PropertiesSection serializedItem) {
        return new OverlayProgressBarItem(this, serializedItem);
    }

    @Override
    public DeepCustomizationLayoutEditorElement constructEditorElementInstance(DeepCustomizationItem item, LayoutEditorScreen handler) {
        return new OverlayProgressBarLayoutElement(item.parentElement, item, handler);
    }

    @Override
    public String getDisplayName() {
        return I18n.get("drippyloadingscreen.deepcustomization.overlay.progress_bar.display_name");
    }

}