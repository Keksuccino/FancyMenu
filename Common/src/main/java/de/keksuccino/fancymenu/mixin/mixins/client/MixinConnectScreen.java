package de.keksuccino.fancymenu.mixin.mixins.client;

import de.keksuccino.fancymenu.customization.backend.world.LastWorldHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public class MixinConnectScreen {

    private static final Logger LOGGER = LogManager.getLogger();

    @Inject(at = @At("HEAD"), method = "startConnecting")
    private static void onStartConnectingFancyMenu(Screen screen, Minecraft mc, ServerAddress address, ServerData data, CallbackInfo info) {
        if (address != null) {
            LastWorldHandler.setLastWorld(address.getHost() + ":" + address.getPort(), true);
        }
    }

    @Inject(at = @At("HEAD"), method = "connect", cancellable = true)
    private void onConnectFancyMenu(Minecraft p_251955_, ServerAddress address, ServerData p_252078_, CallbackInfo info) {
        if (address.getHost().equals("%fancymenu_dummy_address%")) {
            info.cancel();
        }
    }

}
