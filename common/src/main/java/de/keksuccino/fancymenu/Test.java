package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenCompletedEvent;
import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.event.acara.EventPriority;
import de.keksuccino.fancymenu.util.rendering.overlay.SnowfallOverlay;
import net.minecraft.client.gui.components.Button;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Test {

    private static final Logger LOGGER = LogManager.getLogger();

//    private static final SnowfallOverlay SNOWFALL_OVERLAY = new SnowfallOverlay(0, 0);
//
//    @EventListener(priority = EventPriority.VERY_LOW)
//    public void onRenderPost(RenderScreenEvent.Post e) {
//
//        SNOWFALL_OVERLAY.setWidth(e.getScreen().width);
//        SNOWFALL_OVERLAY.setHeight(e.getScreen().height);
//        SNOWFALL_OVERLAY.render(e.getGraphics(), e.getMouseX(), e.getMouseY(), e.getPartial());
//
//    }
//
//    @EventListener
//    public void onInitScreenPost(InitOrResizeScreenCompletedEvent e) {
//
//        SNOWFALL_OVERLAY.clearCollisionAreas();
//        e.getWidgets().forEach(listener -> {
//            if (listener instanceof Button w) {
//                SNOWFALL_OVERLAY.addCollisionArea(w.getX(), w.getY(), w.getWidth(), w.getHeight());
//            }
//        });
//
//    }

}
