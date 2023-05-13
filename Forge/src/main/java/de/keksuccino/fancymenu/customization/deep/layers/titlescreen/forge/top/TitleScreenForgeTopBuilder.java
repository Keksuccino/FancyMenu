package de.keksuccino.fancymenu.customization.deep.layers.titlescreen.forge.top;

import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.deep.DeepElementBuilder;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepElement;
import de.keksuccino.fancymenu.customization.deep.DeepScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.deep.AbstractEditorDeepElement;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.fancymenu.properties.PropertyContainer;

public class TitleScreenForgeTopBuilder extends DeepElementBuilder {

    public TitleScreenForgeTopBuilder(DeepScreenCustomizationLayer parentLayer) {
        super("title_screen_forge_top", parentLayer);
    }

    @Override
    public AbstractDeepElement constructDefaultItemInstance() {
        return new TitleScreenForgeTopItemAbstract(this, new PropertyContainer(""));
    }

    @Override
    public AbstractDeepElement constructCustomizedItemInstance(PropertyContainer serializedItem) {
        return new TitleScreenForgeTopItemAbstract(this, serializedItem);
    }

    @Override
    public AbstractEditorDeepElement constructEditorElementInstance(AbstractDeepElement item, LayoutEditorScreen handler) {
        return new TitleScreenForgeTopLayoutElementAbstractDeep(item.parentElement, item, handler);
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.forge.top");
    }

}