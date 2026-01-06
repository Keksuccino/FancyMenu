package de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollbar;

import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.events.screen.ScreenMouseScrollEvent;
import de.keksuccino.fancymenu.events.ticking.ClientTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Deprecated
public class ScrollBarHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    private static boolean initialized = false;
    private static final List<ScrollBar> scrollBars = Collections.synchronizedList(new ArrayList<>());

    protected static void init() {
        if (!initialized) {
            EventHandler.INSTANCE.registerListenersOf(new ScrollBarHandler());
            initialized = true;
        }
    }

    public static void handleScrollBar(ScrollBar scrollBar) {
        init();
        if (scrollBar.active) {
            scrollBar.lastTick = System.currentTimeMillis();
            if (!scrollBars.contains(scrollBar)) {
                scrollBars.add(scrollBar);
            }
        }
    }

    @EventListener
    public void onClientTick(ClientTickEvent.Post e) {
        List<ScrollBar> old = new ArrayList<>(scrollBars);
        for (ScrollBar b : old) {
            long now = System.currentTimeMillis();
            if ((b.lastTick + 3000) < now) {
                scrollBars.remove(b);
            }
        }
    }

    @EventListener
    public void onMouseScrollPre(ScreenMouseScrollEvent.Pre e) {
        List<ScrollBar> bars = new ArrayList<>(scrollBars);
        for (ScrollBar b : bars) {
            b.handleWheelScrolling(e);
        }
    }

}
