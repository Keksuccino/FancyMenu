package de.keksuccino.fancymenu.customization.menuhandler.deepcustomizationlayer.layers.titlescreen.logo;

import de.keksuccino.fancymenu.customization.customizationgui.layouteditor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.menuhandler.deepcustomizationlayer.DeepCustomizationElement;
import de.keksuccino.fancymenu.customization.menuhandler.deepcustomizationlayer.DeepCustomizationItem;
import de.keksuccino.fancymenu.customization.menuhandler.deepcustomizationlayer.DeepCustomizationLayoutEditorElement;
import org.jetbrains.annotations.NotNull;

public class TitleScreenLogoLayoutElement extends DeepCustomizationLayoutEditorElement {

    public TitleScreenLogoLayoutElement(@NotNull DeepCustomizationElement parentDeepCustomizationElement, @NotNull DeepCustomizationItem customizationItemInstance, @NotNull LayoutEditorScreen handler) {
        super(parentDeepCustomizationElement, customizationItemInstance, true, handler);
    }

    @Override
    public SimplePropertiesSection serializeItem() {
        SimplePropertiesSection sec = new SimplePropertiesSection();
//        sec.addEntry("test_entry", "test");
        return sec;
    }

}