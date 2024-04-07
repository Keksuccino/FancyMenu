package de.keksuccino.fancymenu.customization.element.elements.musiccontroller;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinMusicManager;
import de.keksuccino.fancymenu.util.WorldUtils;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import net.minecraft.client.Minecraft;
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

    private static boolean initialized = false;
    private static boolean playMenuMusic = true;
    private static boolean playWorldMusic = true;
    private static boolean worldMusicManuallyStopped = false;
    private static boolean menuMusicManuallyStopped = false;
    private static boolean lastTickScreenOpen = false;

    public static void init() {
        if (!initialized) {
            initialized = true;
            EventHandler.INSTANCE.registerListenersOf(new MusicControllerHandler());
        }
    }

    @EventListener
    public void onClientTickPost(ClientTickEvent.Post e) {

        boolean screenOpen = (Minecraft.getInstance().screen != null);

        //Set updated to false and remove controllers that did not get updated
        List<ActiveController> cachedControllers = new ArrayList<>(CONTROLLERS.values());
        cachedControllers.forEach(activeController -> {
            if (!activeController.updated) CONTROLLERS.remove(activeController.controller.getInstanceIdentifier());
            activeController.updated = false;
        });

        playMenuMusic = true;
        playWorldMusic = true;

        //Only handle if there's at least one active MusicControllerElement
        if (!CONTROLLERS.isEmpty() && screenOpen) {

            CONTROLLERS.values().forEach(activeController -> {
                if (!activeController.controller.playMenuMusic) playMenuMusic = false;
                if (!activeController.controller.playWorldMusic) playWorldMusic = false;
            });

            if (!isWorldLoaded()) {
                //Handle menu music only if NOT globally disabled
                if (FancyMenu.getOptions().playVanillaMenuMusic.getValue()) {
                    if (!playMenuMusic && isMenuMusicPlaying()) {
                        Minecraft.getInstance().getMusicManager().stopPlaying();
                        Minecraft.getInstance().getSoundManager().stop();
                        menuMusicManuallyStopped = true;
                        //LOGGER.info("############ MENU MUSIC STOPPED!");
                    } else if (playMenuMusic && !isMenuMusicPlaying()) {
                        Minecraft.getInstance().getMusicManager().startPlaying(Minecraft.getInstance().getSituationalMusic());
                        menuMusicManuallyStopped = false;
                        //LOGGER.info("############ MENU MUSIC RESUMED!");
                    }
                }
            } else {
                if (!playWorldMusic && !worldMusicManuallyStopped) {
                    Minecraft.getInstance().getSoundManager().pause();
                    worldMusicManuallyStopped = true;
                    //LOGGER.info("############ WORLD MUSIC STOPPED!");
                } else if (playWorldMusic && worldMusicManuallyStopped) {
                    Minecraft.getInstance().getSoundManager().resume();
                    worldMusicManuallyStopped = false;
                    //LOGGER.info("############ WORLD MUSIC RESUMED!");
                }
            }

        }

        //Resume world music after closing screen
        if (!screenOpen) {
            if (isWorldLoaded() && lastTickScreenOpen && worldMusicManuallyStopped) {
                Minecraft.getInstance().getSoundManager().resume();
                //LOGGER.info("############ WORLD MUSIC RESUMED BECAUSE: SCREEN CLOSED");
            }
            worldMusicManuallyStopped = false;
        }

        if (CONTROLLERS.isEmpty()) {
            //Resume menu music if no controller is disabling it anymore
            if (menuMusicManuallyStopped) {
                if (!isMenuMusicPlaying()) {
                    Minecraft.getInstance().getMusicManager().startPlaying(Minecraft.getInstance().getSituationalMusic());
                    //LOGGER.info("############ MENU MUSIC RESUMED BECAUSE: NO ACTIVE CONTROLLERS");
                }
                menuMusicManuallyStopped = false;
            }
            //Resume world music (in menu) if no controller is disabling it anymore and the current screen isn't pausing the game
            if (isWorldLoaded() && worldMusicManuallyStopped) {
                if (((Minecraft.getInstance().screen != null) && !Minecraft.getInstance().screen.isPauseScreen()) || WorldUtils.isMultiplayer()) {
                    Minecraft.getInstance().getSoundManager().resume();
                    //LOGGER.info("############ WORLD MUSIC RESUMED BECAUSE: NO ACTIVE CONTROLLERS");
                }
                worldMusicManuallyStopped = false;
            }
        }

        lastTickScreenOpen = screenOpen;

    }

    public static void notify(@NotNull MusicControllerElement controller) {
        ActiveController activeController = CONTROLLERS.computeIfAbsent(controller.getInstanceIdentifier(), k -> new ActiveController(controller));
        activeController.updated = true;
    }

    public static boolean shouldPlayMenuMusic() {
        return playMenuMusic;
    }

    public static boolean shouldPlayWorldMusic() {
        return playWorldMusic;
    }

    private static boolean isMenuMusicPlaying() {
        return (((IMixinMusicManager)Minecraft.getInstance().getMusicManager()).getCurrentMusic_FancyMenu() != null);
    }

    private static boolean isWorldLoaded() {
        return (Minecraft.getInstance().level != null);
    }

    protected static class ActiveController {

        protected MusicControllerElement controller;
        protected boolean updated = false;

        protected ActiveController(@NotNull MusicControllerElement controller) {
            this.controller = controller;
        }

    }

}
