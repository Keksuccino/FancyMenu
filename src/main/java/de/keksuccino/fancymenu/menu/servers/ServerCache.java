package de.keksuccino.fancymenu.menu.servers;

import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerCache {

    protected static final Component CANT_CONNECT_TEXT = (new TranslatableComponent("multiplayer.status.cannot_connect")).withStyle(ChatFormatting.DARK_RED);

    protected static ServerStatusPinger pinger = new ServerStatusPinger();
    protected static Map<String, ServerData> servers = new HashMap<String, ServerData>();
    //TODO übernehmen
    protected static Map<String, ServerData> serversUpdated = new HashMap<String, ServerData>();

    public static void init() {
        new Thread(() -> {
            while (true) {
                try {
                    if ((Minecraft.getInstance().screen != null) && MenuCustomization.isMenuCustomizable(Minecraft.getInstance().screen)) {
                        pingServers();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(30000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //TODO übernehmen
    public static void cacheServer(ServerData server, ServerData serverUpdated) {
        if (server.ip != null) {
            try {
                server.ping = -1L;
                serverUpdated.ping = -1L;
                servers.put(server.ip, server);
                serversUpdated.put(server.ip, serverUpdated);
                pingServers();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //TODO übernehmen
    public static ServerData getServer(String ip) {
        if (!servers.containsKey(ip)) {
            cacheServer(new ServerData(ip, ip, false), new ServerData(ip, ip, false));
        }

        //Copy server data from old to new array only when server is done pinging
        if (servers.get(ip).motd != null) {
            if (!servers.get(ip).motd.equals(new TranslatableComponent("multiplayer.status.pinging"))) {
                serversUpdated.get(ip).ping = servers.get(ip).ping;
                serversUpdated.get(ip).protocol = servers.get(ip).protocol;
                serversUpdated.get(ip).motd = servers.get(ip).motd;
                serversUpdated.get(ip).version = servers.get(ip).version;
                serversUpdated.get(ip).status = servers.get(ip).status;
                serversUpdated.get(ip).playerList = servers.get(ip).playerList;
            }
        }

        return serversUpdated.get(ip);
    }

    //TODO übernehmen
    public static void removeServer(String ip) {
        servers.remove(ip);
        serversUpdated.remove(ip);
    }

    //TODO übernehmen
    public static void clear() {
        servers.clear();
        serversUpdated.clear();
    }

    public static void pingServers() {
        List<ServerData> l = new ArrayList<ServerData>();
        l.addAll(servers.values());
        for (ServerData d : l) {
            try {
                new Thread(() -> {
                    try {
                        pinger.pingServer(d, () -> {});
                        if (d.status == TextComponent.EMPTY) {
                            d.ping = -1L;
                            d.motd = CANT_CONNECT_TEXT;
                        }
                    } catch (Exception ex) {
                        d.ping = -1L;
                        d.motd = CANT_CONNECT_TEXT;
                    }
                }).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
