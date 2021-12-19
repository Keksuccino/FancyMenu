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
    protected static Map<String, ServerData> serversUpdated = new HashMap<String, ServerData>();

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

    public static void cacheServer(ServerData server, ServerData serverUpdated) {
        if (server.serverIP != null) {
            try {
                server.pingToServer = -1L;
                serverUpdated.pingToServer = -1L;
                servers.put(server.serverIP, server);
                serversUpdated.put(server.serverIP, serverUpdated);
                pingServers();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static ServerData getServer(String ip) {
        if (!servers.containsKey(ip)) {
            cacheServer(new ServerData(ip, ip, false), new ServerData(ip, ip, false));
        }

        //Copy server data from old to new array only when server is done pinging
        if (servers.get(ip).serverMOTD != null) {
            if (!servers.get(ip).serverMOTD.equals(new TranslationTextComponent("multiplayer.status.pinging"))) {
                serversUpdated.get(ip).pingToServer = servers.get(ip).pingToServer;
                serversUpdated.get(ip).version = servers.get(ip).version;
                serversUpdated.get(ip).serverMOTD = servers.get(ip).serverMOTD;
                serversUpdated.get(ip).gameVersion = servers.get(ip).gameVersion;
                serversUpdated.get(ip).populationInfo = servers.get(ip).populationInfo;
                serversUpdated.get(ip).playerList = servers.get(ip).playerList;
            }
        }

        return serversUpdated.get(ip);
    }

    public static void removeServer(String ip) {
        servers.remove(ip);
        serversUpdated.remove(ip);
    }

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
