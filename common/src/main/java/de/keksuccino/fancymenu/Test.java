package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.event.acara.EventPriority;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;

public class Test {

    @EventListener(priority = EventPriority.VERY_LOW)
    public void onRenderPost(RenderScreenEvent.Post e) {

//        RenderingUtils.renderBlurredArea(e.getGraphics(), 30, 30, 200, 200, e.getPartial(), 10);

//        RenderingUtils.renderBlurredArea_Claude(e.getGraphics(), 30, 30, 200, 200, 1, 1.0F, 1.0F, 1.0F, 1.0F);

    }

}
