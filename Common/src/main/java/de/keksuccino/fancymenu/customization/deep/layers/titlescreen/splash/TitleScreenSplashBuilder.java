package de.keksuccino.fancymenu.customization.deep.layers.titlescreen.splash;

import de.keksuccino.fancymenu.customization.deep.DeepScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.deep.AbstractEditorDeepElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.deep.DeepElementBuilder;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepElement;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.fancymenu.properties.PropertyContainer;

public class TitleScreenSplashBuilder extends DeepElementBuilder {

    public TitleScreenSplashBuilder(DeepScreenCustomizationLayer parentLayer) {
        super("title_screen_splash", parentLayer);
    }

    @Override
    public AbstractDeepElement constructDefaultItemInstance() {
        TitleScreenSplashItemAbstract i = new TitleScreenSplashItemAbstract(this, new PropertyContainer(""));
        i.anchorPoint = "original";
        return i;
    }

    @Override
    public AbstractDeepElement constructCustomizedItemInstance(PropertyContainer serializedItem) {
        return new TitleScreenSplashItemAbstract(this, serializedItem);
    }

    @Override
    public AbstractEditorDeepElement constructEditorElementInstance(AbstractDeepElement item, LayoutEditorScreen handler) {
        return new TitleScreenSplashLayoutElementAbstractDeep(item.parentElement, item, handler);
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.splash");
    }

}