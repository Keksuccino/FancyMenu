package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.networking.PacketsForge;
import de.keksuccino.fancymenu.platform.Services;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FancyMenu.MOD_ID)
public class FancyMenuForge {
    
    public FancyMenuForge(FMLJavaModLoadingContext context) {

        // FancyMenu.init(); got moved to MixinMinecraft

        PacketsForge.init();

        FMLCommonSetupEvent.getBus(context.getModBusGroup()).addListener(this::onSetup);

        if (Services.PLATFORM.isOnClient()) {
            FancyMenuForgeClientEvents.registerAll();
        }

        FancyMenuForgeServerEvents.registerAll();
        
    }

    @SubscribeEvent
    public void onSetup(FMLCommonSetupEvent e) {

        if (Compat.isAudioExtensionLoaded() || Services.PLATFORM.isModLoaded("fmextension_audio")) {
            throw new RuntimeException("§rFancyMenu v2's §cAudio Extension§r is not supported by §cFancyMenu v3§r. Please remove the audio extension. The Audio element is now part of the base mod.");
        }
        if (Compat.isVideoExtensionLoaded() || Services.PLATFORM.isModLoaded("fmextension_video")) {
            throw new RuntimeException("§rFancyMenu v2's §cVideo Extension§r is not supported by §cFancyMenu v3§r. Please remove the video extension.");
        }

    }

}
