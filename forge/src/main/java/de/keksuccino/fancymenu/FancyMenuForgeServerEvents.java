package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.commands.Commands;
import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.fancymenu.util.WebUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
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
        if (!Services.PLATFORM.isOnClient()) WebUtils.shutdown();
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent e) {
        PacketHandler.onServerStopped(e.getServer());
    }

}
