package de.keksuccino.fancymenu.menu.servers;

import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.ServerPinger;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerCache {

    static final ITextComponent CANT_CONNECT_TEXT = (new TranslationTextComponent("multiplayer.status.cannot_connect")).mergeStyle(TextFormatting.DARK_RED);

    protected static ServerPinger pinger = new ServerPinger();
    protected static Map<String, ServerData> servers = new HashMap<String, ServerData>();

    public static void init() {
        new Thread(() -> {
            while (true) {
                try {
                    if ((Minecraft.getInstance().currentScreen != null) && MenuCustomization.isMenuCustomizable(Minecraft.getInstance().currentScreen)) {
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
        if (server.serverIP != null) {
            try {
                server.pingToServer = -1L;
                servers.put(server.serverIP, server);
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
                        pinger.ping(d, () -> {});
                        if (d.populationInfo == StringTextComponent.EMPTY) {
                            d.pingToServer = -1L;
                            d.serverMOTD = CANT_CONNECT_TEXT;
                        }
                    } catch (Exception ex) {
                        d.pingToServer = -1L;
                        d.serverMOTD = CANT_CONNECT_TEXT;
                    }
                }).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
