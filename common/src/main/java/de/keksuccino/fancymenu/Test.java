package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.event.acara.EventPriority;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Test {

    private static final Logger LOGGER = LogManager.getLogger();

    @EventListener(priority = EventPriority.VERY_LOW)
    public void onRenderPost(RenderScreenEvent.Post e) {

    }

}
