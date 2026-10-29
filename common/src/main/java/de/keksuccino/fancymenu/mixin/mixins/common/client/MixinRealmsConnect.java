package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.realmsclient.dto.RealmsServer;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.realms.RealmsConnect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RealmsConnect.class)
public class MixinRealmsConnect {

    /**
     * Realms connections don't go through ConnectScreen#startConnecting, so they need their own hook.
     * Vanilla calls this from the Realms long-running task thread, so the listener gets notified on the main thread.
     * Only the host gets passed to the listener, because that's the IP Realms stores as current server, which keeps $$server_ip consistent with "On Server Joined".
     */
    @Inject(method = "connect", at = @At("HEAD"))
    private void onConnectFancyMenu(RealmsServer server, ServerAddress address, CallbackInfo info) {
        if (!Listeners.ON_START_JOINING_SERVER.hasInstancesListening()) return;
        String serverIp = address.getHost();
        MainThreadTaskExecutor.executeInMainThread(() -> Listeners.ON_START_JOINING_SERVER.onStartJoiningServer(serverIp), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
    }

}
