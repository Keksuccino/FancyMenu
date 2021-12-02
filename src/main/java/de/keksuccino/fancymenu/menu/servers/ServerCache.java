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

    static final Component CANT_CONNECT_TEXT = (new TranslatableComponent("multiplayer.status.cannot_connect")).withStyle(ChatFormatting.DARK_RED);

    protected static ServerStatusPinger pinger = new ServerStatusPinger();
    protected static Map<String, ServerData> servers = new HashMap<String, ServerData>();

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

    public static void cacheServer(ServerData server) {
        if (server.ip != null) {
            try {
                server.ping = -1L;
                servers.put(server.ip, server);
                pingServers();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static ServerData getServer(String ip) {
        if (!servers.containsKey(ip)) {
            cacheServer(new ServerData(ip, ip, false));
        }
        return servers.get(ip);
    }

    public static void removeServer(String ip) {
        servers.remove(ip);
    }

    public static void clear() {
        servers.clear();
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
