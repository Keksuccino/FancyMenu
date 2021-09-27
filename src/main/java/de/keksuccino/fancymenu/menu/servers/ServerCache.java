package de.keksuccino.fancymenu.menu.servers;

import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerCache {

    static final Text CANT_CONNECT_TEXT = (new TranslatableText("multiplayer.status.cannot_connect")).formatted(Formatting.DARK_RED);

    protected static MultiplayerServerListPinger pinger = new MultiplayerServerListPinger();
    protected static Map<String, ServerInfo> servers = new HashMap<String, ServerInfo>();

    public static void init() {
        new Thread(() -> {
            while (true) {
                try {
                    if ((MinecraftClient.getInstance().currentScreen != null) && MenuCustomization.isMenuCustomizable(MinecraftClient.getInstance().currentScreen)) {
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

    public static void cacheServer(ServerInfo server) {
        if (server.address != null) {
            try {
                server.ping = -1L;
                servers.put(server.address, server);
                pingServers();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static ServerInfo getServer(String ip) {
        if (!servers.containsKey(ip)) {
            cacheServer(new ServerInfo(ip, ip, false));
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
        List<ServerInfo> l = new ArrayList<ServerInfo>();
        l.addAll(servers.values());
        for (ServerInfo d : l) {
            try {
                new Thread(() -> {
                    try {
                        pinger.add(d, () -> {});
                        if (d.playerCountLabel == LiteralText.EMPTY) {
                            d.ping = -1L;
                            d.label = CANT_CONNECT_TEXT;
                        }
                    } catch (Exception ex) {
                        d.ping = -1L;
                        d.label = CANT_CONNECT_TEXT;
                    }
                }).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
