package de.keksuccino.fancymenu.customization.backend.element.elements;

import de.keksuccino.fancymenu.customization.backend.element.ElementRegistry;
import de.keksuccino.fancymenu.customization.backend.element.elements.inputfield.InputFieldElementBuilder;
import de.keksuccino.fancymenu.customization.backend.element.elements.playerentity.PlayerEntityElementBuilder;
import de.keksuccino.fancymenu.customization.backend.element.elements.slider.SliderElementBuilder;
import de.keksuccino.fancymenu.customization.backend.element.elements.ticker.TickerElementBuilder;
import de.keksuccino.fancymenu.customization.backend.element.elements.text.TextElementBuilder;

public class Elements {

    public static void registerAll() {

        ElementRegistry.register(new InputFieldElementBuilder());
        ElementRegistry.register(new SliderElementBuilder());
        ElementRegistry.register(new TextElementBuilder());
        ElementRegistry.register(new TickerElementBuilder());
        ElementRegistry.register(new PlayerEntityElementBuilder());

    }

}
