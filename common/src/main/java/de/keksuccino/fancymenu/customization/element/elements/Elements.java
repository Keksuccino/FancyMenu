package de.keksuccino.fancymenu.customization.element.elements;

import de.keksuccino.fancymenu.customization.element.ElementRegistry;
import de.keksuccino.fancymenu.customization.element.elements.animation.AnimationElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.button.custombutton.ButtonElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.cursor.CursorElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.image.ImageElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.inputfield.InputFieldElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.playerentity.PlayerEntityElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.progressbar.ProgressBarElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.shape.ShapeElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.slideshow.SlideshowElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.splash.SplashTextElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.ticker.TickerElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.text.v2.TextElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.slider.v2.SliderElementBuilder;

public class Elements {

    public static final ButtonElementBuilder BUTTON = new ButtonElementBuilder();
    public static final InputFieldElementBuilder INPUT_FIELD = new InputFieldElementBuilder();
    @Deprecated
    public static final de.keksuccino.fancymenu.customization.element.elements.slider.v1.SliderElementBuilder SLIDER_V1 = new de.keksuccino.fancymenu.customization.element.elements.slider.v1.SliderElementBuilder();
    public static final SliderElementBuilder SLIDER_V2 = new SliderElementBuilder();
    @Deprecated
    public static final de.keksuccino.fancymenu.customization.element.elements.text.v1.TextElementBuilder TEXT_V1 = new de.keksuccino.fancymenu.customization.element.elements.text.v1.TextElementBuilder();
    public static final TextElementBuilder TEXT_V2 = new TextElementBuilder();
    public static final TickerElementBuilder TICKER = new TickerElementBuilder();
    public static final PlayerEntityElementBuilder PLAYER_ENTITY = new PlayerEntityElementBuilder();
    public static final ImageElementBuilder IMAGE = new ImageElementBuilder();
    public static final SplashTextElementBuilder SPLASH_TEXT = new SplashTextElementBuilder();
    public static final AnimationElementBuilder ANIMATION = new AnimationElementBuilder();
    public static final SlideshowElementBuilder SLIDESHOW = new SlideshowElementBuilder();
    public static final ShapeElementBuilder SHAPE = new ShapeElementBuilder();
    public static final CursorElementBuilder CURSOR = new CursorElementBuilder();
    public static final ProgressBarElementBuilder PROGRESS_BAR = new ProgressBarElementBuilder();

    public static void registerAll() {

        ElementRegistry.register(BUTTON);
        ElementRegistry.register(INPUT_FIELD);
        ElementRegistry.register(SLIDER_V1);
        ElementRegistry.register(SLIDER_V2);
        ElementRegistry.register(TEXT_V1);
        ElementRegistry.register(TEXT_V2);
        ElementRegistry.register(TICKER);
        ElementRegistry.register(PLAYER_ENTITY);
        ElementRegistry.register(IMAGE);
        ElementRegistry.register(SPLASH_TEXT);
        ElementRegistry.register(ANIMATION);
        ElementRegistry.register(SLIDESHOW);
        ElementRegistry.register(SHAPE);
        ElementRegistry.register(CURSOR);
        ElementRegistry.register(PROGRESS_BAR);

    }

}
