//---
package de.keksuccino.fancymenu.menu.fancy.item.items.inputfield;

import de.keksuccino.fancymenu.api.item.CustomizationItem;
import de.keksuccino.fancymenu.api.item.CustomizationItemContainer;
import de.keksuccino.fancymenu.api.item.LayoutEditorElement;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;

public class InputFieldCustomizationItemContainer extends CustomizationItemContainer {

    public InputFieldCustomizationItemContainer() {
        super("fancymenu_customization_item_input_field");
    }

    @Override
    public CustomizationItem constructDefaultItemInstance() {
        InputFieldCustomizationItem i = new InputFieldCustomizationItem(this, new PropertiesSection("dummy"));
        i.width = 100;
        i.height = 20;
        return i;
    }

    @Override
    public CustomizationItem constructCustomizedItemInstance(PropertiesSection serializedItem) {
        return new InputFieldCustomizationItem(this, serializedItem);
    }

    @Override
    public LayoutEditorElement constructEditorElementInstance(CustomizationItem item, LayoutEditorScreen handler) {
        return new InputFieldLayoutEditorElement(this, (InputFieldCustomizationItem) item, handler);
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.customization.items.input_field");
    }

    @Override
    public String[] getDescription() {
        return StringUtils.splitLines(Locals.localize("fancymenu.customization.items.input_field.desc"), "%n%");
    }

}
