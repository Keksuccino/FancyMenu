package de.keksuccino.fancymenu.customization.deep.layers.titlescreen.realmsnotification;

import de.keksuccino.fancymenu.customization.deep.DeepScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.deep.AbstractEditorDeepElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.deep.DeepElementBuilder;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepElement;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.fancymenu.properties.PropertyContainer;

public class TitleScreenRealmsNotificationBuilder extends DeepElementBuilder {

    public TitleScreenRealmsNotificationBuilder(DeepScreenCustomizationLayer parentLayer) {
        super("title_screen_realms_notification", parentLayer);
    }

    @Override
    public AbstractDeepElement constructDefaultItemInstance() {
        return new TitleScreenRealmsNotificationItemAbstract(this, new PropertyContainer(""));
    }

    @Override
    public AbstractDeepElement constructCustomizedItemInstance(PropertyContainer serializedItem) {
        return new TitleScreenRealmsNotificationItemAbstract(this, serializedItem);
    }

    @Override
    public AbstractEditorDeepElement constructEditorElementInstance(AbstractDeepElement item, LayoutEditorScreen handler) {
        return new TitleScreenRealmsNotificationLayoutElementAbstractDeep(item.parentElement, item, handler);
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.realmsnotification");
    }

}