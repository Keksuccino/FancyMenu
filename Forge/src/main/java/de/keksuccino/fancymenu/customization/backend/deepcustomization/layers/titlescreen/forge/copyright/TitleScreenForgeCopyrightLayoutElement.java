package de.keksuccino.fancymenu.customization.backend.deepcustomization.layers.titlescreen.forge.copyright;

import de.keksuccino.fancymenu.customization.frontend.layouteditor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.backend.deepcustomization.DeepCustomizationElement;
import de.keksuccino.fancymenu.customization.backend.deepcustomization.DeepCustomizationItem;
import de.keksuccino.fancymenu.customization.backend.deepcustomization.DeepCustomizationLayoutEditorElement;
import org.jetbrains.annotations.NotNull;

public class TitleScreenForgeCopyrightLayoutElement extends DeepCustomizationLayoutEditorElement {

    public TitleScreenForgeCopyrightLayoutElement(@NotNull DeepCustomizationElement parentDeepCustomizationElement, @NotNull DeepCustomizationItem customizationItemInstance, @NotNull LayoutEditorScreen handler) {
        super(parentDeepCustomizationElement, customizationItemInstance, true, handler);
    }

    @Override
    public SimplePropertiesSection serializeItem() {
        SimplePropertiesSection sec = new SimplePropertiesSection();
//        sec.addEntry("test_entry", "test");
        return sec;
    }

}