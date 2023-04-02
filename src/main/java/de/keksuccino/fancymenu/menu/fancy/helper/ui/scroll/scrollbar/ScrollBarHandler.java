//TODO übernehmenn
package de.keksuccino.fancymenu.menu.fancy.helper.ui.scroll.scrollbar;

import de.keksuccino.konkrete.gui.content.scrollarea.ScrollArea;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScrollBarHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    private static boolean initialized = false;
    private static final List<ScrollBar> scrollBars = Collections.synchronizedList(new ArrayList<>());

    protected static void init() {
        if (!initialized) {
            MouseInput.registerMouseListener(ScrollBarHandler::onMouseScrollPre);
            MinecraftForge.EVENT_BUS.register(new ScrollBarHandler());
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

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END) {
            List<ScrollBar> old = new ArrayList<>(scrollBars);
            for (ScrollBar b : old) {
                long now = System.currentTimeMillis();
                if ((b.lastTick + 3000) < now) {
                    scrollBars.remove(b);
                }
            }
        }
    }

    public static void onMouseScrollPre(MouseInput.MouseData e) {
        if (e.deltaZ != 0) {
            List<ScrollBar> bars = new ArrayList<>(scrollBars);
            for (ScrollBar b : bars) {
                b.handleWheelScrolling(e);
            }
        }
    }

}
