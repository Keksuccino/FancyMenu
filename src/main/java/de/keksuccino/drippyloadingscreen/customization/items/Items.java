package de.keksuccino.drippyloadingscreen.customization.items;

import de.keksuccino.drippyloadingscreen.customization.items.bars.generic.GenericProgressBarCustomizationItemContainer;
import de.keksuccino.drippyloadingscreen.customization.items.bars.loading.CustomLoadingBarCustomizationItemContainer;
import de.keksuccino.fancymenu.api.item.CustomizationItemRegistry;

public class Items {

    public static void registerAll() {

        CustomizationItemRegistry.registerItem(new CustomLoadingBarCustomizationItemContainer());
        CustomizationItemRegistry.registerItem(new GenericProgressBarCustomizationItemContainer());

    }

}
