package de.keksuccino.fancymenu.menu.servers;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.ServerPinger;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

public class ServerCache {

    private static final ThreadPoolExecutor EXECUTOR = new ScheduledThreadPoolExecutor(5, (new ThreadFactoryBuilder()).setNameFormat("Server Pinger #%d").setDaemon(true).build());

    protected static GuiMultiplayer guiMultiplayer = new GuiMultiplayer(null);
    protected static ServerPinger pinger = guiMultiplayer.getOldServerPinger();
    protected static GuiScreen lastScreen = null;

    protected static Map<String, ServerData> servers = new HashMap<String, ServerData>();

    public static void init() {

        MinecraftForge.EVENT_BUS.register(new ServerCache());

        //Server pinging thread
        new Thread(() -> {
            while (true) {
                try {
                    if ((Minecraft.getMinecraft().currentScreen != null) && MenuCustomization.isMenuCustomizable(Minecraft.getMinecraft().currentScreen)) {
                        pingServers();
                    }
                } catch (Exception e) {}
                try {
                    Thread.sleep(30000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post e) {
        if (lastScreen != Minecraft.getMinecraft().currentScreen) {
            new Thread(() -> {
                try {
                    pingServers();
                } catch (Exception ex) {}
            }).start();
        }
        lastScreen = Minecraft.getMinecraft().currentScreen;
        pinger.pingPendingNetworks();
    }

    public static void cacheServer(ServerData server) {
        if (server.serverIP != null) {
            try {
                server.pingToServer = -1L;
                servers.put(server.serverIP, server);
                pingServers();
            } catch (Exception e) {}
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

                        pingServer(d);

//                        System.out.println(" \n" +
//                                "-------------------\n" +
//                                "IP: " + d.serverIP + "\n" +
//                                "MOTD: " + d.serverMOTD + "\n" +
//                                "POPULATION INFO: " + d.populationInfo + "\n" +
//                                "PING TO SERVER: " + d.pingToServer + "\n" +
//                                "GAME VERSION: " + d.gameVersion + "\n" +
//                                "PLAYER LIST: " + d.playerList + "\n" +
//                                "SERVER NAME: " + d.serverName + "\n" +
//                                "PINGED: " + d.pinged + "\n" +
//                                "VERSION: " + d.version + "\n" +
//                                "--------------------"
//                        );

                    } catch (Exception ex) {}
                }).start();
            } catch (Exception e) {}
        }
    }

    protected static void pingServer(ServerData server) {
        if (server != null) {
            try {

                Minecraft mc = Minecraft.getMinecraft();

                if (!server.pinged) {
                    server.pinged = true;
                    server.pingToServer = -2L;
                    server.serverMOTD = "";
                    server.populationInfo = "";
                    EXECUTOR.submit(new Runnable() {
                        public void run() {
                            try {
                                pinger.ping(server);
                            }
                            catch (UnknownHostException var2) {
                                server.pingToServer = -1L;
                                server.serverMOTD = TextFormatting.DARK_RED + I18n.format("multiplayer.status.cannot_resolve");
                            }
                            catch (Exception var3) {
                                server.pingToServer = -1L;
                                server.serverMOTD = TextFormatting.DARK_RED + I18n.format("multiplayer.status.cannot_connect");
                            }
                        }
                    });
                }

                long startTime = System.currentTimeMillis();
                while(true) {
                    long curTime = System.currentTimeMillis();
                    if ((startTime + 15000) <= curTime) {
                        break;
                    }
                    if (server.pinged && server.pingToServer != -2L) {
                        break;
                    }
                    Thread.sleep(100);
                }

                server.pinged = false;

            } catch (Exception e) {}
        }
    }

}
