package de.keksuccino.fancymenu.customization.backend.deepcustomization.layers.titlescreen.realmsnotification;

import de.keksuccino.fancymenu.customization.backend.deepcustomization.DeepCustomizationLayoutEditorElement;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.backend.deepcustomization.DeepCustomizationElement;
import de.keksuccino.fancymenu.customization.backend.deepcustomization.DeepCustomizationItem;
import org.jetbrains.annotations.NotNull;

public class TitleScreenRealmsNotificationLayoutElement extends DeepCustomizationLayoutEditorElement {

    public TitleScreenRealmsNotificationLayoutElement(@NotNull DeepCustomizationElement parentDeepCustomizationElement, @NotNull DeepCustomizationItem customizationItemInstance, @NotNull LayoutEditorScreen handler) {
        super(parentDeepCustomizationElement, customizationItemInstance, true, handler);
    }

    @Override
    public SimplePropertiesSection serializeItem() {
        SimplePropertiesSection sec = new SimplePropertiesSection();
//        sec.addEntry("test_entry", "test");
        return sec;
    }

}