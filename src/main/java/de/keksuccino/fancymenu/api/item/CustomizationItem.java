package de.keksuccino.fancymenu.api.item;

import de.keksuccino.fancymenu.menu.fancy.item.CustomizationItemBase;
import de.keksuccino.konkrete.properties.PropertiesSection;

public abstract class CustomizationItem extends CustomizationItemBase {

    public CustomizationItemContainer parentItemContainer;

    public CustomizationItem(CustomizationItemContainer parentContainer, PropertiesSection item) {
        super(item);
        this.parentItemContainer = parentContainer;
        if (this.value == null) {
            this.value = parentContainer.getDisplayName();
        }
        if (this.getWidth() == -1) {
            this.setWidth(10);
        }
        if (this.getHeight() == -1) {
            this.setHeight(10);
        }
    }

}
