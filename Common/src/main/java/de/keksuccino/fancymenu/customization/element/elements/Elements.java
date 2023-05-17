package de.keksuccino.fancymenu.customization.element.elements;

import de.keksuccino.fancymenu.customization.element.ElementRegistry;
import de.keksuccino.fancymenu.customization.element.elements.button.custom.ButtonElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.image.ImageElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.inputfield.InputFieldElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.playerentity.PlayerEntityElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.slider.SliderElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.ticker.TickerElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.text.TextElementBuilder;

public class Elements {

    public static final ButtonElementBuilder BUTTON = new ButtonElementBuilder();
    public static final InputFieldElementBuilder INPUT_FIELD = new InputFieldElementBuilder();
    public static final SliderElementBuilder SLIDER = new SliderElementBuilder();
    public static final TextElementBuilder TEXT = new TextElementBuilder();
    public static final TickerElementBuilder TICKER = new TickerElementBuilder();
    public static final PlayerEntityElementBuilder PLAYER_ENTITY = new PlayerEntityElementBuilder();
    public static final ImageElementBuilder IMAGE = new ImageElementBuilder();

    public static void registerAll() {

        ElementRegistry.register(BUTTON);
        ElementRegistry.register(INPUT_FIELD);
        ElementRegistry.register(SLIDER);
        ElementRegistry.register(TEXT);
        ElementRegistry.register(TICKER);
        ElementRegistry.register(PLAYER_ENTITY);
        ElementRegistry.register(IMAGE);

    }

}
