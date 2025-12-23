package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenCompletedEvent;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.event.acara.EventPriority;
import de.keksuccino.fancymenu.util.rendering.ui.toast.SimpleToast;
import de.keksuccino.fancymenu.util.rendering.ui.toast.ToastHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Test {

    private static final Logger LOGGER = LogManager.getLogger();

    @EventListener(priority = EventPriority.VERY_LOW)
    public void onRenderPost(RenderScreenEvent.Post e) {

    }

    @EventListener
    public void onInitScreenPost(InitOrResizeScreenCompletedEvent e) {

//        e.addRenderableWidget(new ExtendedButton(50, 50, 200, 20, "Show Toast", button -> {
//            ToastHandler.showToast(new SimpleToast(new SimpleToast.Icon(Identifier.fromNamespaceAndPath("fancymenu", "")), Component.literal("Title"), Component.literal("This is some text."), false), 10000);
//        }));

    }

}
