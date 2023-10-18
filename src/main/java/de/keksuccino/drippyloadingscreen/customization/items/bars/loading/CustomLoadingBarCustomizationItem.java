package de.keksuccino.drippyloadingscreen.customization.items.bars.loading;

import de.keksuccino.drippyloadingscreen.customization.items.bars.AbstractProgressBarCustomizationItem;
import de.keksuccino.drippyloadingscreen.mixin.MixinCache;
import de.keksuccino.fancymenu.api.item.CustomizationItemContainer;
import de.keksuccino.konkrete.properties.PropertiesSection;

public class CustomLoadingBarCustomizationItem extends AbstractProgressBarCustomizationItem {

    public CustomLoadingBarCustomizationItem(CustomizationItemContainer parentContainer, PropertiesSection item) {
        super(parentContainer, item);
    }

    @Override
    public float getCurrentProgress() {
        if (isEditorActive()) {
            return 0.5F;
        }
        return MixinCache.cachedCurrentLoadingScreenProgress;
    }

}
