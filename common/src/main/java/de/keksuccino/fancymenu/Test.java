package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenCompletedEvent;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.event.acara.EventPriority;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Test {

    private static final Logger LOGGER = LogManager.getLogger();

    @EventListener(priority = EventPriority.VERY_LOW)
    public void onRenderPost(InitOrResizeScreenCompletedEvent e) {

//        e.addRenderableWidget(new ExtendedButton(20, 20, 200, 20, "Open Video Player", button -> {
//            Minecraft.getInstance().setScreen(new VideoPlayerExample());
//        }));

    }

}
