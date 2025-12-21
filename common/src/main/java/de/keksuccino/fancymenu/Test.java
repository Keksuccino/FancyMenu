package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenCompletedEvent;
import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.util.ObjectUtils;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.event.acara.EventPriority;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.overlay.RainOverlay;
import de.keksuccino.fancymenu.util.rendering.overlay.SnowfallOverlay;
import de.keksuccino.fancymenu.util.rendering.overlay.SunshineOverlay;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlainTextButton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;

public class Test {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final SnowfallOverlay SNOWFALL_OVERLAY = ObjectUtils.build(() -> {
        var o = new SnowfallOverlay(0, 0);
        o.setColor(DrawableColor.of(new Color(255, 0, 221)).getColorInt());
        o.setIntensity(1.5F);
        return o;
    });
    private static final RainOverlay RAIN_OVERLAY = ObjectUtils.build(() -> {
        var o = new RainOverlay(0, 0);
        o.setColor(DrawableColor.of(new Color(80, 103, 16)).getColorInt());
        o.setIntensity(1.5F);
        return o;
    });
    private static final SunshineOverlay SUNSHINE_OVERLAY = ObjectUtils.build(() -> {
        var o = new SunshineOverlay(0, 0);
        o.setSide(SunshineOverlay.SunshineSide.RIGHT);
        return o;
    });

    @EventListener(priority = EventPriority.VERY_LOW)
    public void onRenderPost(RenderScreenEvent.Post e) {

//        SNOWFALL_OVERLAY.setWidth(e.getScreen().width);
//        SNOWFALL_OVERLAY.setHeight(e.getScreen().height);
//        SNOWFALL_OVERLAY.render(e.getGraphics(), e.getMouseX(), e.getMouseY(), e.getPartial());

//        RAIN_OVERLAY.setWidth(e.getScreen().width);
//        RAIN_OVERLAY.setHeight(e.getScreen().height);
//        RAIN_OVERLAY.render(e.getGraphics(), e.getMouseX(), e.getMouseY(), e.getPartial());

//        SUNSHINE_OVERLAY.setWidth(e.getScreen().width);
//        SUNSHINE_OVERLAY.setHeight(e.getScreen().height);
//        SUNSHINE_OVERLAY.render(e.getGraphics(), e.getMouseX(), e.getMouseY(), e.getPartial());

    }

    @EventListener
    public void onInitScreenPost(InitOrResizeScreenCompletedEvent e) {

//        SNOWFALL_OVERLAY.clearCollisionAreas();
//        e.getWidgets().forEach(listener -> {
//            if ((listener instanceof Button w) && !(listener instanceof PlainTextButton)) {
//                SNOWFALL_OVERLAY.addCollisionArea(w.getX(), w.getY(), w.getWidth(), w.getHeight());
//            }
//        });

//        RAIN_OVERLAY.clearCollisionAreas();
//        e.getWidgets().forEach(listener -> {
//            if ((listener instanceof Button w) && !(listener instanceof PlainTextButton)) {
//                RAIN_OVERLAY.addCollisionArea(w.getX(), w.getY(), w.getWidth(), w.getHeight());
//            }
//        });

    }

}
