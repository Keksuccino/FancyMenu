package de.keksuccino.fancymenu.customization.backend.deepcustomization.layers.titlescreen.realmsnotification;

import de.keksuccino.fancymenu.customization.backend.deepcustomization.DeepCustomizationLayer;
import de.keksuccino.fancymenu.customization.backend.deepcustomization.DeepCustomizationLayoutEditorElement;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.backend.deepcustomization.DeepCustomizationElement;
import de.keksuccino.fancymenu.customization.backend.deepcustomization.DeepCustomizationItem;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;

public class TitleScreenRealmsNotificationElement extends DeepCustomizationElement {

    public TitleScreenRealmsNotificationElement(DeepCustomizationLayer parentLayer) {
        super("title_screen_realms_notification", parentLayer);
    }

    @Override
    public DeepCustomizationItem constructDefaultItemInstance() {
        return new TitleScreenRealmsNotificationItem(this, new PropertiesSection(""));
    }

    @Override
    public DeepCustomizationItem constructCustomizedItemInstance(PropertiesSection serializedItem) {
        return new TitleScreenRealmsNotificationItem(this, serializedItem);
    }

    @Override
    public DeepCustomizationLayoutEditorElement constructEditorElementInstance(DeepCustomizationItem item, LayoutEditorScreen handler) {
        return new TitleScreenRealmsNotificationLayoutElement(item.parentElement, item, handler);
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.realmsnotification");
    }

}