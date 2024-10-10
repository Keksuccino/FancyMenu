package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.networking.PacketsNeoForge;
import de.keksuccino.fancymenu.platform.Services;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod(FancyMenu.MOD_ID)
public class FancyMenuNeoForge {

    //TODO übernehmen (add rrls to mods.toml)
    
    public FancyMenuNeoForge(@NotNull IEventBus eventBus) {

        FancyMenu.init();

        PacketsNeoForge.init(eventBus);

        if (Services.PLATFORM.isOnClient()) {
            FancyMenuNeoForgeClientEvents.registerAll();
        }

        FancyMenuNeoForgeServerEvents.registerAll();
        
    }

}