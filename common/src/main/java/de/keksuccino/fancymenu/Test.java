package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.event.acara.EventPriority;
//import it.crystalnest.fancy_entity_renderer.api.entity.player.FancyPlayerWidget;
//import net.minecraft.client.gui.screens.TitleScreen;

public class Test {

    @EventListener(priority = EventPriority.VERY_LOW)
    public void onInitPost(InitOrResizeScreenEvent.Post e) {

//        if (e.getScreen() instanceof TitleScreen) {
//
//            FancyPlayerWidget widget = new FancyPlayerWidget(100, 100, 200, 200);
//            widget.setMoving(true);
//            widget.copyLocalPlayer();
//
//            e.addRenderableWidget(widget);
//
//        }

    }

}
