package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.networking.neoforge.PacketEvents;
import de.keksuccino.fancymenu.platform.Services;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod(FancyMenu.MOD_ID)
public class FancyMenuNeoForge {
    
    public FancyMenuNeoForge(@NotNull IEventBus eventBus) {

        if (Compat.isAudioExtensionLoaded() || Services.PLATFORM.isModLoaded("fmextension_audio")) {
            throw new RuntimeException("FancyMenu v2's Audio Extension is not supported by FancyMenu v3. Please remove the audio extension. The Audio element is now part of the base mod.");
        }
        if (Compat.isVideoExtensionLoaded() || Services.PLATFORM.isModLoaded("fmextension_video")) {
            throw new RuntimeException("FancyMenu v2's Video Extension is not supported by FancyMenu v3. Please remove the video extension.");
        }

        FancyMenu.init();

        //Register packets
        eventBus.addListener(PacketEvents::registerPackets);

        if (Services.PLATFORM.isOnClient()) {
            FancyMenuNeoForgeClientEvents.registerAll();
        }

        FancyMenuNeoForgeServerEvents.registerAll();
        
    }

}