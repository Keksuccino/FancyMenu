package de.keksuccino.fancymenu.customization.deep.layers.titlescreen.branding;

import de.keksuccino.fancymenu.customization.deep.DeepScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.deep.DeepElementBuilder;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepElement;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.fancymenu.properties.PropertyContainer;

public class TitleScreenBrandingBuilder extends DeepElementBuilder {

    public TitleScreenBrandingBuilder(DeepScreenCustomizationLayer parentLayer) {
        super("title_screen_branding", parentLayer);
    }

    @Override
    public AbstractDeepElement constructDefaultItemInstance() {
        return new TitleScreenBrandingItemAbstract(this, new PropertyContainer(""));
    }

    @Override
    public AbstractDeepElement constructCustomizedItemInstance(PropertyContainer serializedItem) {
        return new TitleScreenBrandingItemAbstract(this, serializedItem);
    }

    @Override
    public AbstractDeepEditorElement constructEditorElementInstance(AbstractDeepElement item, LayoutEditorScreen handler) {
        return new TitleScreenBrandingLayoutElementAbstractDeepEditor(item.parentElement, item, handler);
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.branding");
    }

}