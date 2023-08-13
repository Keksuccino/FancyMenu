package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.networking.Packets;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.konkrete.Konkrete;
import net.minecraftforge.fml.common.Mod;

@Mod(FancyMenu.MOD_ID)
public class FancyMenuForge {
    
    public FancyMenuForge() {

        FancyMenu.init();

        if (Services.PLATFORM.isOnClient()) {
            Konkrete.addPostLoadingEvent(FancyMenu.MOD_ID, FancyMenu::onClientSetup);
        }

        Packets.registerAll();

        if (Services.PLATFORM.isOnClient()) {
            FancyMenuForgeClientEvents.registerAll();
        }

        FancyMenuForgeServerEvents.registerAll();
        
    }

}