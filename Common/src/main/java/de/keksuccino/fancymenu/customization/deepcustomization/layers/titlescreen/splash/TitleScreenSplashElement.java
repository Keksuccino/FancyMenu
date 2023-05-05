package de.keksuccino.fancymenu.customization.deepcustomization.layers.titlescreen.splash;

import de.keksuccino.fancymenu.customization.deepcustomization.DeepCustomizationLayer;
import de.keksuccino.fancymenu.customization.deepcustomization.DeepCustomizationLayoutEditorElement;
import de.keksuccino.fancymenu.customization.layouteditor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.deepcustomization.DeepCustomizationElement;
import de.keksuccino.fancymenu.customization.deepcustomization.DeepCustomizationItem;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;

public class TitleScreenSplashElement extends DeepCustomizationElement {

    public TitleScreenSplashElement(DeepCustomizationLayer parentLayer) {
        super("title_screen_splash", parentLayer);
    }

    @Override
    public DeepCustomizationItem constructDefaultItemInstance() {
        TitleScreenSplashItem i = new TitleScreenSplashItem(this, new PropertiesSection(""));
        i.anchorPoint = "original";
        return i;
    }

    @Override
    public DeepCustomizationItem constructCustomizedItemInstance(PropertiesSection serializedItem) {
        return new TitleScreenSplashItem(this, serializedItem);
    }

    @Override
    public DeepCustomizationLayoutEditorElement constructEditorElementInstance(DeepCustomizationItem item, LayoutEditorScreen handler) {
        return new TitleScreenSplashLayoutElement(item.parentElement, item, handler);
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.splash");
    }

}