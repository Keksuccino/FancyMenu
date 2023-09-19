package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.networking.Packets;
import de.keksuccino.fancymenu.platform.Services;
import net.fabricmc.api.ModInitializer;

public class FancyMenuFabric implements ModInitializer {
    
    @Override
    public void onInitialize() {

        FancyMenu.init();

        Packets.registerAll();

        if (Services.PLATFORM.isOnClient()) {
            FancyMenuFabricClientEvents.registerAll();
        }

        FancyMenuFabricServerEvents.registerAll();

    }

}
