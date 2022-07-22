package de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers.titlescreen.logo;

import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationLayoutEditorElement;

import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationLayoutEditorElement.SimplePropertiesSection;

public class TitleScreenLogoLayoutElement extends DeepCustomizationLayoutEditorElement {

    public TitleScreenLogoLayoutElement(DeepCustomizationElement parentDeepCustomizationElement, DeepCustomizationItem customizationItemInstance, LayoutEditorScreen handler) {
        super(parentDeepCustomizationElement, customizationItemInstance, true, handler);
    }

    @Override
    public SimplePropertiesSection serializeItem() {
        SimplePropertiesSection sec = new SimplePropertiesSection();
        return sec;
    }

}