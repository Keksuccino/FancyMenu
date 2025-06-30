package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenCompletedEvent;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.event.acara.EventPriority;

public class Test {

    @EventListener(priority = EventPriority.VERY_LOW)
    public void onRenderPost(InitOrResizeScreenCompletedEvent e) {

    }

}
