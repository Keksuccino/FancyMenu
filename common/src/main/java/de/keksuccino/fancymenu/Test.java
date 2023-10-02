package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import org.apache.logging.log4j.LogManager;

public class Test {

    @EventListener
    public void onInitOrResizePost(InitOrResizeScreenEvent.Post e) {
        e.addRenderableWidget(new ExtendedButton((e.getScreen().width / 2) - 100, 20, 200, 20, "Test Button", var1 -> {
            LogManager.getLogger().info("############ CLICK");
        }));
    }

}
