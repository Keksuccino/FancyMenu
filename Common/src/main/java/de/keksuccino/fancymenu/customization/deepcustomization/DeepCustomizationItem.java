package de.keksuccino.fancymenu.customization.deepcustomization;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.konkrete.properties.PropertiesSection;

public abstract class DeepCustomizationItem extends AbstractElement {

    public DeepCustomizationElement parentElement;

    public boolean hidden = false;

    public DeepCustomizationItem(DeepCustomizationElement parentElement, PropertiesSection item) {

        super(item);
        this.parentElement = parentElement;

        if (this.value == null) {
            this.value = parentElement.getDisplayName();
        }
        if (this.width == -1) {
            this.width = 10;
        }
        if (this.height == -1) {
            this.height = 10;
        }

        String hiddenString = item.getEntryValue("hidden");
        if ((hiddenString != null) && hiddenString.equals("true")) {
            this.hidden = true;
        }

    }

}