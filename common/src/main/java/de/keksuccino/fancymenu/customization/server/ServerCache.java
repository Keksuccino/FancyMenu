package de.keksuccino.fancymenu.customization.server;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.threading.FancyMenuExecutors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.EventLoopGroupHolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerCache {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Component CANT_CONNECT_TEXT = Component.translatable("multiplayer.status.cannot_connect").withStyle(ChatFormatting.DARK_RED);
    private static final ServerStatusPinger PINGER = new ServerStatusPinger();
    private static final Map<String, ServerData> SERVERS = new ConcurrentHashMap<>();
    private static final Map<String, ServerData> UPDATED_SERVERS = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService EXECUTOR = FancyMenuExecutors.newScheduledThreadPool(2, "FancyMenu-ServerCache");
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();
    private static final AtomicBoolean SHUTTING_DOWN = new AtomicBoolean();

    public static void init() {
        if (!INITIALIZED.compareAndSet(false, true)) return;
        EXECUTOR.scheduleWithFixedDelay(ServerCache::refreshServers, 0L, 30L, TimeUnit.SECONDS);
    }

    public static void shutdown() {
        if (!SHUTTING_DOWN.compareAndSet(false, true)) return;
        EXECUTOR.shutdownNow();
        try {
            PINGER.removeAll();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to cancel cached server pings during client shutdown!", ex);
        }
        clear();
    }

    public static void cacheServer(ServerData server, ServerData updatedServer) {
        if (SHUTTING_DOWN.get() || server.ip == null) return;
        try {
            server.ping = -1L;
            updatedServer.ping = -1L;
            SERVERS.put(server.ip, server);
            UPDATED_SERVERS.put(server.ip, updatedServer);
            pingServers();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to cache server data for '{}'!", server.ip, ex);
        }
    }

    public static ServerData getServer(String ip) {
        if (!SERVERS.containsKey(ip)) {
            cacheServer(new ServerData(ip, ip, ServerData.Type.OTHER), new ServerData(ip, ip, ServerData.Type.OTHER));
        }
        ServerData server = SERVERS.get(ip);
        ServerData updatedServer = UPDATED_SERVERS.get(ip);
        if (server == null || updatedServer == null) return new ServerData(ip, ip, ServerData.Type.OTHER);

        // Only expose a new ping result after it is complete, keeping placeholders from flickering back to the pinging state.
        if (server.motd != null && !server.motd.equals(Component.translatable("multiplayer.status.pinging"))) {
            updatedServer.ping = server.ping;
            updatedServer.protocol = server.protocol;
            updatedServer.motd = server.motd;
            updatedServer.version = server.version;
            updatedServer.status = server.status;
            updatedServer.playerList = server.playerList;
        }
        return updatedServer;
    }

    public static void removeServer(String ip) {
        SERVERS.remove(ip);
        UPDATED_SERVERS.remove(ip);
    }

    public static void clear() {
        SERVERS.clear();
        UPDATED_SERVERS.clear();
    }

    public static void pingServers() {
        if (SHUTTING_DOWN.get()) return;
        List<ServerData> servers = new ArrayList<>(SERVERS.values());
        for (ServerData server : servers) {
            try {
                EXECUTOR.execute(() -> pingServer(server));
            } catch (RejectedExecutionException ex) {
                if (!SHUTTING_DOWN.get()) LOGGER.error("[FANCYMENU] Failed to queue cached server ping!", ex);
            }
        }
    }

    private static void refreshServers() {
        if (SHUTTING_DOWN.get()) return;
        try {
            if (ScreenCustomization.isCustomizationEnabledForScreen(ScreenUtils.getScreen())) pingServers();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to refresh cached servers!", ex);
        }
    }

    private static void pingServer(ServerData server) {
        if (SHUTTING_DOWN.get()) return;
        try {
            PINGER.pingServer(server, () -> {}, () -> {}, EventLoopGroupHolder.remote(Minecraft.getInstance().options.useNativeTransport()));
            if (server.status == null || server.status.getString().isEmpty()) {
                server.ping = -1L;
                server.motd = CANT_CONNECT_TEXT;
            }
        } catch (Exception ex) {
            server.ping = -1L;
            server.motd = CANT_CONNECT_TEXT;
        } finally {
            // DNS resolution happens inside pingServer and must never hold up the Render thread. A worker that loses the shutdown race cancels any connection it registered late.
            if (SHUTTING_DOWN.get()) PINGER.removeAll();
        }
    }

}
