package de.keksuccino.drippyloadingscreen.customization.items.bars.generic;

import de.keksuccino.drippyloadingscreen.customization.items.bars.AbstractProgressBarCustomizationItem;
import de.keksuccino.fancymenu.api.item.CustomizationItemContainer;
import de.keksuccino.fancymenu.menu.placeholder.v2.PlaceholderParser;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;

public class GenericProgressBarCustomizationItem extends AbstractProgressBarCustomizationItem {

    public String progressSourceString;

    public GenericProgressBarCustomizationItem(CustomizationItemContainer parentContainer, PropertiesSection item) {

        super(parentContainer, item);

        this.progressSourceString = item.getEntryValue("progress_source");

    }

    @Override
    public float getCurrentProgress() {
        if (this.progressSourceString != null) {
            String s = PlaceholderParser.replacePlaceholders(this.progressSourceString).replace(" ", "");
            if (MathUtils.isFloat(s)) {
                return Float.parseFloat(s);
            }
        }
        return 0.5F;
    }

}
