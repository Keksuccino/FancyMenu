package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.networking.Packets;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.konkrete.Konkrete;
import net.fabricmc.api.ModInitializer;

public class FancyMenuFabric implements ModInitializer {
    
    @Override
    public void onInitialize() {

        FancyMenu.init();

        if (Services.PLATFORM.isOnClient()) {
            Konkrete.addPostLoadingEvent(FancyMenu.MOD_ID, FancyMenu::onClientSetup);
        }

        Packets.registerAll();

        if (Services.PLATFORM.isOnClient()) {
            FancyMenuFabricClientEvents.registerAll();
        }

        FancyMenuFabricServerEvents.registerAll();

    }

}
