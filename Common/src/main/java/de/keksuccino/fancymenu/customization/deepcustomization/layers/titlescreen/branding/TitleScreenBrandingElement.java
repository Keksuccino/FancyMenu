package de.keksuccino.fancymenu.customization.deepcustomization.layers.titlescreen.branding;

import de.keksuccino.fancymenu.customization.deepcustomization.DeepCustomizationLayer;
import de.keksuccino.fancymenu.customization.deepcustomization.DeepCustomizationLayoutEditorElement;
import de.keksuccino.fancymenu.customization.layouteditor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.deepcustomization.DeepCustomizationElement;
import de.keksuccino.fancymenu.customization.deepcustomization.DeepCustomizationItem;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;

public class TitleScreenBrandingElement extends DeepCustomizationElement {

    public TitleScreenBrandingElement(DeepCustomizationLayer parentLayer) {
        super("title_screen_branding", parentLayer);
    }

    @Override
    public DeepCustomizationItem constructDefaultItemInstance() {
        return new TitleScreenBrandingItem(this, new PropertiesSection(""));
    }

    @Override
    public DeepCustomizationItem constructCustomizedItemInstance(PropertiesSection serializedItem) {
        return new TitleScreenBrandingItem(this, serializedItem);
    }

    @Override
    public DeepCustomizationLayoutEditorElement constructEditorElementInstance(DeepCustomizationItem item, LayoutEditorScreen handler) {
        return new TitleScreenBrandingLayoutElement(item.parentElement, item, handler);
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.branding");
    }

}