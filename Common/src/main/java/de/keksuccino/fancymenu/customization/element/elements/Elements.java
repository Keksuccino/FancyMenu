package de.keksuccino.fancymenu.customization.element.elements;

import de.keksuccino.fancymenu.customization.element.ElementRegistry;
import de.keksuccino.fancymenu.customization.element.elements.inputfield.InputFieldElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.playerentity.PlayerEntityElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.slider.SliderElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.ticker.TickerElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.text.TextElementBuilder;

public class Elements {

    public static void registerAll() {

        ElementRegistry.register(new InputFieldElementBuilder());
        ElementRegistry.register(new SliderElementBuilder());
        ElementRegistry.register(new TextElementBuilder());
        ElementRegistry.register(new TickerElementBuilder());
        ElementRegistry.register(new PlayerEntityElementBuilder());

    }

}
