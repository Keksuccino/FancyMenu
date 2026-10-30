package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.commands.Commands;
import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.fancymenu.util.WebUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

public class FancyMenuNeoForgeServerEvents {

    public static void registerAll() {

        NeoForge.EVENT_BUS.register(new FancyMenuNeoForgeServerEvents());

    }

    @SubscribeEvent
    public void onRegisterServerCommands(RegisterCommandsEvent e) {
        Commands.registerAll(e.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            PacketHandler.onServerPlayerConnected(p);
            PacketHandler.sendHandshakeToClient(p);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) PacketHandler.onServerPlayerDisconnected(p);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent e) {
        PacketHandler.onServerStarting(e.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent e) {
        // Clear before player teardown. ServerStopped repeats this idempotently to cover partial shutdown paths.
        PacketHandler.onServerStopped(e.getServer());
        // An integrated server can stop while its physical client keeps running, so only a dedicated-server process owns the
        // shared internet availability monitor lifecycle.
        if (!Services.PLATFORM.isOnClient()) WebUtils.shutdown();
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent e) {
        PacketHandler.onServerStopped(e.getServer());
    }

}
