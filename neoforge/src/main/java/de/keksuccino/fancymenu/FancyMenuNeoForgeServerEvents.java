package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.commands.Commands;
import de.keksuccino.fancymenu.networking.PacketHandler;
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
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent e) {
        PacketHandler.onServerStopped(e.getServer());
    }

}
