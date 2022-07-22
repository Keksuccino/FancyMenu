package de.keksuccino.fancymenu.menu.fancy.item.visibilityrequirements;

import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.EventPriority;
import de.keksuccino.konkrete.events.SubscribeEvent;
import java.util.Calendar;
import java.util.Locale;
import net.minecraft.client.Minecraft;

public class VisibilityRequirementHandler {

    public static boolean isSingleplayer = false;
    public static int windowWidth = 0;
    public static int windowHeight = 0;
    public static boolean worldLoaded = false;

    public static void init() {
        Konkrete.getEventHandler().registerEventsFrom(new VisibilityRequirementHandler());
    }

    public static void update() {

        //VR: World Loaded
        worldLoaded = (Minecraft.getInstance().level != null);

        //VR: Is Singleplayer & Is Multiplayer
        isSingleplayer = Minecraft.getInstance().hasSingleplayerServer();
        if (!worldLoaded) {
            isSingleplayer = false;
        }

        //VR: Is Window Width/Height X
        windowWidth = Minecraft.getInstance().getWindow().getScreenWidth();
        windowHeight = Minecraft.getInstance().getWindow().getScreenHeight();

    }

    public static boolean isMacOS() {
        return Minecraft.ON_OSX;
    }

    public static boolean isWindows() {
        String s = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        return (s.contains("win"));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onButtonsCached(ButtonCachedEvent e) {
        update();
    }

}
