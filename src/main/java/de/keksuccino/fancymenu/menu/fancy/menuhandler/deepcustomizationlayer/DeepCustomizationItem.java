package de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer;

import de.keksuccino.fancymenu.menu.fancy.item.CustomizationItemBase;
import de.keksuccino.konkrete.properties.PropertiesSection;

public abstract class DeepCustomizationItem extends CustomizationItemBase {

    public DeepCustomizationElement parentElement;

    public boolean hidden = false;

    public DeepCustomizationItem(DeepCustomizationElement parentElement, PropertiesSection item) {

        super(item);
        this.parentElement = parentElement;

        if (this.value == null) {
            this.value = parentElement.getDisplayName();
        }
        if (this.getWidth() == -1) {
            this.setWidth(10);
        }
        if (this.getHeight() == -1) {
            this.setHeight(10);
        }

        String hiddenString = item.getEntryValue("hidden");
        if ((hiddenString != null) && hiddenString.equals("true")) {
            this.hidden = true;
        }

    }

}
