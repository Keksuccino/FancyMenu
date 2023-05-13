package de.keksuccino.fancymenu.customization.deep.layers.titlescreen.logo;

import de.keksuccino.fancymenu.customization.deep.AbstractEditorDeepElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.deep.DeepElementBuilder;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepElement;
import org.jetbrains.annotations.NotNull;

public class TitleScreenLogoLayoutElementAbstractDeep extends AbstractEditorDeepElement {

    public TitleScreenLogoLayoutElementAbstractDeep(@NotNull DeepElementBuilder parentDeepElementBuilder, @NotNull AbstractDeepElement customizationItemInstance, @NotNull LayoutEditorScreen handler) {
        super(parentDeepElementBuilder, customizationItemInstance, true, handler);
    }

    @Override
    public SimplePropertiesSection serializeItem() {
        SimplePropertiesSection sec = new SimplePropertiesSection();
//        sec.addEntry("test_entry", "test");
        return sec;
    }

}