package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.networking.PacketsForge;
import de.keksuccino.fancymenu.platform.Services;
import net.minecraftforge.fml.common.Mod;

@Mod(FancyMenu.MOD_ID)
public class FancyMenuForge {
    
    public FancyMenuForge() {

        if (Compat.isAudioExtensionLoaded() || Services.PLATFORM.isModLoaded("fmextension_audio")) {
            throw new RuntimeException("FancyMenu v2's Audio Extension is not supported by FancyMenu v3. Please remove the audio extension. The Audio element is now part of the base mod.");
        }
        if (Compat.isVideoExtensionLoaded() || Services.PLATFORM.isModLoaded("fmextension_video")) {
            throw new RuntimeException("FancyMenu v2's Video Extension is not supported by FancyMenu v3. Please remove the video extension.");
        }

        FancyMenu.init();

        //TODO übernehmen
        PacketsForge.init();

        if (Services.PLATFORM.isOnClient()) {
            FancyMenuForgeClientEvents.registerAll();
        }

        FancyMenuForgeServerEvents.registerAll();
        
    }

}