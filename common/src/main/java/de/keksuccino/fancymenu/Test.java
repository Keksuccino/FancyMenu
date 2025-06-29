package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenCompletedEvent;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.event.acara.EventPriority;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import net.minecraft.client.Minecraft;

public class Test {

    @EventListener(priority = EventPriority.VERY_LOW)
    public void onRenderPost(InitOrResizeScreenCompletedEvent e) {

//        e.addRenderableWidget(new ExtendedButton(20, 20, 200, 20, "Open Video Player", button -> {
//            Minecraft.getInstance().setScreen(new VideoPlayerExample());
//        }));

    }

}
