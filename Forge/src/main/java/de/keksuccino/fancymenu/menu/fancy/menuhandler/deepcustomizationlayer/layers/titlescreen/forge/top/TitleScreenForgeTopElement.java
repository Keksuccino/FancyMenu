package de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers.titlescreen.forge.top;

import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationLayer;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationLayoutEditorElement;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;

public class TitleScreenForgeTopElement extends DeepCustomizationElement {

    public TitleScreenForgeTopElement(DeepCustomizationLayer parentLayer) {
        super("title_screen_forge_top", parentLayer);
    }

    @Override
    public DeepCustomizationItem constructDefaultItemInstance() {
        return new TitleScreenForgeTopItem(this, new PropertiesSection(""));
    }

    @Override
    public DeepCustomizationItem constructCustomizedItemInstance(PropertiesSection serializedItem) {
        return new TitleScreenForgeTopItem(this, serializedItem);
    }

    @Override
    public DeepCustomizationLayoutEditorElement constructEditorElementInstance(DeepCustomizationItem item, LayoutEditorScreen handler) {
        return new TitleScreenForgeTopLayoutElement(item.parentElement, item, handler);
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.forge.top");
    }

}