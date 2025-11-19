package de.keksuccino.drippyloadingscreen.neoforge;

import de.keksuccino.fancymenu.util.reload.FancyMenuResourceReload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ReloadListenerBridge {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final ResourceLocation LISTENER_ID = ResourceLocation.fromNamespaceAndPath("fancymenu", "fancymenu_reload_listener");

    public static void onAddClientReloadListeners(AddClientReloadListenersEvent e) {
        e.addListener(LISTENER_ID, FancyMenuResourceReload.createMinecraftPreparableReloadListener());
    }

}