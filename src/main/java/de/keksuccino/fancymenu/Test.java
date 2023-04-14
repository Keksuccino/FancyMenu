package de.keksuccino.fancymenu;

import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Test {

    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public void onInitPre(ScreenEvent.Init.Pre e) {

        LOGGER.info("---------------- TEST: INIT PRE");

    }

    @SubscribeEvent
    public void onInitPost(ScreenEvent.Init.Post e) {

        LOGGER.info("---------------- TEST: INIT POST");

    }

}
