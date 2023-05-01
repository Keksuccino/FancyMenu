package de.keksuccino.fancymenu.customization.backend.item.v2.items;

import de.keksuccino.fancymenu.api.item.CustomizationItemRegistry;
import de.keksuccino.fancymenu.customization.backend.item.v2.items.inputfield.InputFieldCustomizationItemContainer;
import de.keksuccino.fancymenu.customization.backend.item.v2.items.playerentity.PlayerEntityCustomizationItemContainer;
import de.keksuccino.fancymenu.customization.backend.item.v2.items.slider.SliderCustomizationItemContainer;
import de.keksuccino.fancymenu.customization.backend.item.v2.items.text.TextCustomizationItemContainer;
import de.keksuccino.fancymenu.customization.backend.item.v2.items.ticker.TickerCustomizationItemContainer;

public class CustomizationItems {

    public static void registerAll() {

        CustomizationItemRegistry.registerItem(new InputFieldCustomizationItemContainer());
        CustomizationItemRegistry.registerItem(new SliderCustomizationItemContainer());
        CustomizationItemRegistry.registerItem(new TextCustomizationItemContainer());
        CustomizationItemRegistry.registerItem(new TickerCustomizationItemContainer());
        CustomizationItemRegistry.registerItem(new PlayerEntityCustomizationItemContainer());

    }

}
