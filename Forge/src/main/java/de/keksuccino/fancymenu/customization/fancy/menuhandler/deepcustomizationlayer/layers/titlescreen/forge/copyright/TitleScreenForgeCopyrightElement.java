package de.keksuccino.fancymenu.customization.fancy.menuhandler.deepcustomizationlayer.layers.titlescreen.forge.copyright;

import de.keksuccino.fancymenu.customization.customizationgui.layouteditor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.menuhandler.deepcustomizationlayer.DeepCustomizationElement;
import de.keksuccino.fancymenu.customization.menuhandler.deepcustomizationlayer.DeepCustomizationItem;
import de.keksuccino.fancymenu.customization.menuhandler.deepcustomizationlayer.DeepCustomizationLayer;
import de.keksuccino.fancymenu.customization.menuhandler.deepcustomizationlayer.DeepCustomizationLayoutEditorElement;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;

public class TitleScreenForgeCopyrightElement extends DeepCustomizationElement {

    public TitleScreenForgeCopyrightElement(DeepCustomizationLayer parentLayer) {
        super("title_screen_forge_copyright", parentLayer);
    }

    @Override
    public DeepCustomizationItem constructDefaultItemInstance() {
        return new TitleScreenForgeCopyrightItem(this, new PropertiesSection(""));
    }

    @Override
    public DeepCustomizationItem constructCustomizedItemInstance(PropertiesSection serializedItem) {
        return new TitleScreenForgeCopyrightItem(this, serializedItem);
    }

    @Override
    public DeepCustomizationLayoutEditorElement constructEditorElementInstance(DeepCustomizationItem item, LayoutEditorScreen handler) {
        return new TitleScreenForgeCopyrightLayoutElement(item.parentElement, item, handler);
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.forge.copyright");
    }

}