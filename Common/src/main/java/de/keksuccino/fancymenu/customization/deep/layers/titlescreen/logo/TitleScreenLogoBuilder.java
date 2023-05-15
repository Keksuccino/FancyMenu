package de.keksuccino.fancymenu.customization.deep.layers.titlescreen.logo;

import de.keksuccino.fancymenu.customization.deep.DeepScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.deep.DeepElementBuilder;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepElement;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.fancymenu.properties.PropertyContainer;

public class TitleScreenLogoBuilder extends DeepElementBuilder {

    public TitleScreenLogoBuilder(DeepScreenCustomizationLayer parentLayer) {
        super("title_screen_logo", parentLayer);
    }

    @Override
    public AbstractDeepElement constructDefaultItemInstance() {
        return new TitleScreenLogoItemAbstract(this, new PropertyContainer(""));
    }

    @Override
    public AbstractDeepElement constructCustomizedItemInstance(PropertyContainer serializedItem) {
        return new TitleScreenLogoItemAbstract(this, serializedItem);
    }

    @Override
    public AbstractDeepEditorElement constructEditorElementInstance(AbstractDeepElement item, LayoutEditorScreen handler) {
        return new TitleScreenLogoLayoutElementAbstractDeepEditor(item.parentElement, item, handler);
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.logo");
    }

}