package de.keksuccino.fancymenu.customization.item.v2.items.slider;

import de.keksuccino.fancymenu.api.item.CustomizationItem;
import de.keksuccino.fancymenu.api.item.CustomizationItemContainer;
import de.keksuccino.fancymenu.api.item.LayoutEditorElement;
import de.keksuccino.fancymenu.customization.customizationgui.layouteditor.LayoutEditorScreen;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;

public class SliderCustomizationItemContainer extends CustomizationItemContainer {

    public SliderCustomizationItemContainer() {
        super("fancymenu_customization_item_slider");
    }

    @Override
    public CustomizationItem constructDefaultItemInstance() {
        SliderCustomizationItem i = new SliderCustomizationItem(this, new PropertiesSection("dummy"));
        i.width = 100;
        i.height = 20;
        return i;
    }

    @Override
    public CustomizationItem constructCustomizedItemInstance(PropertiesSection serializedItem) {
        return new SliderCustomizationItem(this, serializedItem);
    }

    @Override
    public LayoutEditorElement constructEditorElementInstance(CustomizationItem item, LayoutEditorScreen handler) {
        return new SliderLayoutEditorElement(this, (SliderCustomizationItem) item, handler);
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.customization.items.slider");
    }

    @Override
    public String[] getDescription() {
        return StringUtils.splitLines(Locals.localize("fancymenu.customization.items.slider.desc"), "%n%");
    }

}
