package de.keksuccino.fancymenu.rendering.tooltip;

import de.keksuccino.fancymenu.event.acara.EventHandler;

public class WidgetTooltipHandler {

    public static final WidgetTooltipHandler INSTANCE = new WidgetTooltipHandler();

    public WidgetTooltipHandler() {
        EventHandler.INSTANCE.registerListenersOf(this);
    }

}
