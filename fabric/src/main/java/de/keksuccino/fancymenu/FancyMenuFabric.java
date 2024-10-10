package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.networking.PacketsFabric;
import de.keksuccino.fancymenu.platform.Services;
import net.fabricmc.api.ModInitializer;

public class FancyMenuFabric implements ModInitializer {

    //TODO übernehmen (add rrls to mod.json)
    
    @Override
    public void onInitialize() {

        FancyMenu.init();

        PacketsFabric.init();

        if (Services.PLATFORM.isOnClient()) {
            FancyMenuFabricClientEvents.registerAll();
        }

        FancyMenuFabricServerEvents.registerAll();

    }

}
