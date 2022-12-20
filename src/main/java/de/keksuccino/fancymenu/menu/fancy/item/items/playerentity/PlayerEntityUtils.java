package de.keksuccino.fancymenu.menu.fancy.item.items.playerentity;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Scanner;

public class PlayerEntityUtils {

    public static String getSkinURL(String playerName) {
        String skinUrl = null;
        Scanner scanner = null;
        try {
            URL url = new URL("https://api.ashcon.app/mojang/v2/user/" + playerName);
            scanner = new Scanner(new InputStreamReader(url.openStream()));
            boolean b = false;
            boolean b2 = false;
            while (scanner.hasNextLine()) {
                String line = scanner.next();
                if (b) {
                    skinUrl = line.substring(1, line.length()-2);
                    break;
                }
                if (line.contains("\"skin\":")) {
                    b2 = true;
                }
                if (line.contains("\"code\":")) {
                    break;
                }
                if (line.contains("\"url\":")) {
                    if (b2) {
                        b = true;
                    }
                }
            }
        } catch (IOException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (scanner != null) {
            IOUtils.closeQuietly(scanner);
        }
        return skinUrl;
    }

    public static String getCapeURL(String playerName) {
        String capeUrl = null;
        Scanner scanner = null;
        try {
            URL url = new URL("https://api.ashcon.app/mojang/v2/user/" + playerName);
            scanner = new Scanner(new InputStreamReader(url.openStream()));
            boolean b = false;
            boolean b2 = false;
            while (scanner.hasNextLine()) {
                String line = scanner.next();
                if (b) {
                    capeUrl = line.substring(1, line.length()-2);
                    break;
                }
                if (line.contains("\"cape\":")) {
                    b2 = true;
                }
                if (line.contains("\"code\":")) {
                    break;
                }
                if (line.contains("\"url\":")) {
                    if (b2) {
                        b = true;
                    }
                }
            }
        } catch (IOException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (scanner != null) {
            IOUtils.closeQuietly(scanner);
        }
        return capeUrl;
    }

    public static boolean hasSlimSkin(String playerName) {
        boolean slim = false;
        Scanner scanner = null;
        try {
            URL url = new URL("https://api.ashcon.app/mojang/v2/user/" + playerName);
            scanner = new Scanner(new InputStreamReader(url.openStream()));
            boolean b = false;
            boolean b2 = false;
            while (scanner.hasNextLine()) {
                String line = scanner.next();
                if (b) {
                    String slimString = line.substring(1, line.length()-2);
                    if (slimString.equalsIgnoreCase("true")) {
                        slim = true;
                    }
                    break;
                }
                if (line.contains("\"textures\":")) {
                    b2 = true;
                }
                if (line.contains("\"code\":")) {
                    break;
                }
                if (line.contains("\"slim\":")) {
                    if (b2) {
                        b = true;
                    }
                }
            }
        } catch (IOException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (scanner != null) {
            IOUtils.closeQuietly(scanner);
        }
        return slim;
    }

}
