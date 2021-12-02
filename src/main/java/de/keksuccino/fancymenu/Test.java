package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.events.GuiInitCompletedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class Test {

    @SubscribeEvent
    public void onInitCompleted(GuiInitCompletedEvent e) {
        FancyMenu.LOGGER.info("INIT COMPLETED!");
    }

}
