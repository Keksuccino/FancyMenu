package de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers.titlescreen.branding;

import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationLayoutEditorElement;

import javax.annotation.Nonnull;

import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationLayoutEditorElement.SimplePropertiesSection;

public class TitleScreenBrandingLayoutElement extends DeepCustomizationLayoutEditorElement {

    public TitleScreenBrandingLayoutElement(@Nonnull DeepCustomizationElement parentDeepCustomizationElement, @Nonnull DeepCustomizationItem customizationItemInstance, @Nonnull LayoutEditorScreen handler) {
        super(parentDeepCustomizationElement, customizationItemInstance, true, handler);
    }

    @Override
    public SimplePropertiesSection serializeItem() {
        SimplePropertiesSection sec = new SimplePropertiesSection();
        return sec;
    }

}