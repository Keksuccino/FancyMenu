package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.commands.Commands;
import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.fancymenu.util.WebUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class FancyMenuForgeServerEvents {

    public static void registerAll() {

        MinecraftForge.EVENT_BUS.register(new FancyMenuForgeServerEvents());

    }

    @SubscribeEvent
    public void onRegisterServerCommands(RegisterCommandsEvent e) {
        Commands.registerAll(e.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            PacketHandler.sendHandshakeToClient(p);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent e) {
        // An integrated server can stop while its physical client keeps running, so only a dedicated-server process owns this lifecycle.
        if (!Services.PLATFORM.isOnClient()) WebUtils.shutdown();
    }

}
