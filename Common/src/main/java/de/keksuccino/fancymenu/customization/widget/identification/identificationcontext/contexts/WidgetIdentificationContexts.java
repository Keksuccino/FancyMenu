package de.keksuccino.fancymenu.customization.widget.identification.identificationcontext.contexts;

import de.keksuccino.fancymenu.customization.widget.identification.identificationcontext.WidgetIdentificationContextRegistry;

public class WidgetIdentificationContexts {

    public static final TitleScreenWidgetIdentificationContext TITLE_SCREEN_CONTEXT = new TitleScreenWidgetIdentificationContext();
    public static final DeathScreenWidgetIdentificationContext DEATH_SCREEN_CONTEXT = new DeathScreenWidgetIdentificationContext();
    public static final PauseScreenWidgetIdentificationContext PAUSE_SCREEN_CONTEXT = new PauseScreenWidgetIdentificationContext();

    public static void registerAll() {

        WidgetIdentificationContextRegistry.register(TITLE_SCREEN_CONTEXT);
        WidgetIdentificationContextRegistry.register(DEATH_SCREEN_CONTEXT);
        WidgetIdentificationContextRegistry.register(PAUSE_SCREEN_CONTEXT);

    }

}
