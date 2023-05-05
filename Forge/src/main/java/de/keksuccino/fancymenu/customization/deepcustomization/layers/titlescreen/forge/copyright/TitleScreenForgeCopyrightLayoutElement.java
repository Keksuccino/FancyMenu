package de.keksuccino.fancymenu.customization.deepcustomization.layers.titlescreen.forge.copyright;

import de.keksuccino.fancymenu.customization.layouteditor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.deepcustomization.DeepCustomizationElement;
import de.keksuccino.fancymenu.customization.deepcustomization.DeepCustomizationItem;
import de.keksuccino.fancymenu.customization.deepcustomization.DeepCustomizationLayoutEditorElement;
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