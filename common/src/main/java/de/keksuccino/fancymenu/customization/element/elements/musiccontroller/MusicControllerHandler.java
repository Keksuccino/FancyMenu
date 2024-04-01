package de.keksuccino.fancymenu.customization.element.elements.musiccontroller;

import de.keksuccino.fancymenu.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//TODO übernehmen
public class MusicControllerHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, ActiveController> CONTROLLERS = new HashMap<>();

    private static boolean playMusic = true;

    public static void init() {
        EventHandler.INSTANCE.registerListenersOf(new MusicControllerHandler());
    }

    @EventListener
    public void onClientTickPost(ClientTickEvent.Post e) {

        //Set updated to false and remove controllers that did not get updated
        List<ActiveController> cachedControllers = new ArrayList<>(CONTROLLERS.values());
        cachedControllers.forEach(activeController -> {
            if (!activeController.updated) CONTROLLERS.remove(activeController.controller.getInstanceIdentifier());
            activeController.updated = false;
        });

        playMusic = true;
        CONTROLLERS.values().forEach(activeController -> {
            //TODO if controller stops music, set playMusic to false <-------------------
        });

    }

    public static void notify(@NotNull MusicControllerElement controller) {
        ActiveController activeController = CONTROLLERS.computeIfAbsent(controller.getInstanceIdentifier(), k -> new ActiveController(controller));
        activeController.updated = true;
    }

    public static boolean shouldPlayMusic() {
        return playMusic;
    }

    protected static class ActiveController {

        protected MusicControllerElement controller;
        protected boolean updated = false;

        protected ActiveController(@NotNull MusicControllerElement controller) {
            this.controller = controller;
        }

    }

}
