package de.keksuccino.fancymenu.customization.deepcustomization.layers.titlescreen.logo;

import de.keksuccino.fancymenu.customization.deepcustomization.DeepCustomizationLayer;
import de.keksuccino.fancymenu.customization.deepcustomization.DeepCustomizationLayoutEditorElement;
import de.keksuccino.fancymenu.customization.layouteditor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.deepcustomization.DeepCustomizationElement;
import de.keksuccino.fancymenu.customization.deepcustomization.DeepCustomizationItem;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;

public class TitleScreenLogoElement extends DeepCustomizationElement {

    public TitleScreenLogoElement(DeepCustomizationLayer parentLayer) {
        super("title_screen_logo", parentLayer);
    }

    @Override
    public DeepCustomizationItem constructDefaultItemInstance() {
        return new TitleScreenLogoItem(this, new PropertiesSection(""));
    }

    @Override
    public DeepCustomizationItem constructCustomizedItemInstance(PropertiesSection serializedItem) {
        return new TitleScreenLogoItem(this, serializedItem);
    }

    @Override
    public DeepCustomizationLayoutEditorElement constructEditorElementInstance(DeepCustomizationItem item, LayoutEditorScreen handler) {
        return new TitleScreenLogoLayoutElement(item.parentElement, item, handler);
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.logo");
    }

}