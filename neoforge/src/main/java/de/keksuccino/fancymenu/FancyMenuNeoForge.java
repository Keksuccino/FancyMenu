package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.networking.PacketsNeoForge;
import de.keksuccino.fancymenu.platform.Services;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod(FancyMenu.MOD_ID)
public class FancyMenuNeoForge {
    
    public FancyMenuNeoForge(@NotNull IEventBus eventBus) {

        // FancyMenu.init(); got moved to MixinMinecraft

        PacketsNeoForge.init(eventBus);

        if (Services.PLATFORM.isOnClient()) {
            FancyMenuNeoForgeClientEvents.registerAll();
        }

        FancyMenuNeoForgeServerEvents.registerAll();
        
    }

}