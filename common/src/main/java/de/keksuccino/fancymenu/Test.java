package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenCompletedEvent;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.event.acara.EventPriority;
import de.keksuccino.fancymenu.util.rendering.eastereggs.SnowfallOverlay;
import de.keksuccino.fancymenu.util.rendering.ui.toast.SimpleToast;
import de.keksuccino.fancymenu.util.rendering.ui.toast.ToastHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Test {

    private static final Logger LOGGER = LogManager.getLogger();

    private static SnowfallOverlay snowfallOverlay = new SnowfallOverlay(0, 0);

    @EventListener(priority = EventPriority.VERY_LOW)
    public void onRenderPost(RenderScreenEvent.Post e) {

        snowfallOverlay.setWidth(e.getScreen().width);
        snowfallOverlay.setHeight(e.getScreen().height);
        snowfallOverlay.render(e.getGraphics(), e.getMouseX(), e.getMouseY(), e.getPartial());

    }

//    @EventListener
//    public void onInitScreenPost(InitOrResizeScreenCompletedEvent e) {
//
////        e.addRenderableWidget(new ExtendedButton(50, 50, 200, 20, "Show Toast", button -> {
////            ToastHandler.showToast(new SimpleToast(new SimpleToast.Icon(ResourceLocation.fromNamespaceAndPath("fancymenu", "")), Component.literal("Title"), Component.literal("This is some text."), false), 10000);
////        }));
//
//    }

}
