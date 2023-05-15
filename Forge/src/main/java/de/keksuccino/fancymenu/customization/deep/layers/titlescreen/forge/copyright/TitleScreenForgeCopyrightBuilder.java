package de.keksuccino.fancymenu.customization.deep.layers.titlescreen.forge.copyright;

import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.deep.DeepElementBuilder;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepElement;
import de.keksuccino.fancymenu.customization.deep.DeepScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepEditorElement;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.fancymenu.properties.PropertyContainer;

public class TitleScreenForgeCopyrightBuilder extends DeepElementBuilder {

    public TitleScreenForgeCopyrightBuilder(DeepScreenCustomizationLayer parentLayer) {
        super("title_screen_forge_copyright", parentLayer);
    }

    @Override
    public AbstractDeepElement constructDefaultItemInstance() {
        return new TitleScreenForgeCopyrightItemAbstract(this, new PropertyContainer(""));
    }

    @Override
    public AbstractDeepElement constructCustomizedItemInstance(PropertyContainer serializedItem) {
        return new TitleScreenForgeCopyrightItemAbstract(this, serializedItem);
    }

    @Override
    public AbstractDeepEditorElement constructEditorElementInstance(AbstractDeepElement item, LayoutEditorScreen handler) {
        return new TitleScreenForgeCopyrightLayoutElementAbstractDeepEditor(item.parentElement, item, handler);
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.forge.copyright");
    }

}