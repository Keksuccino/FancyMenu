//TODO übernehmen
//package de.keksuccino.fancymenu.util.minecraftuser;
//
//import de.keksuccino.fancymenu.util.CloseableUtils;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.net.URL;
//import java.util.Objects;
//import java.util.Scanner;
//
//public class PlayerSkinUtils {
//
//    @Nullable
//    public static String getSkinURL(@NotNull String playerName) {
//        Objects.requireNonNull(playerName);
//        String skinUrl = null;
//        Scanner scanner = null;
//        InputStream in = null;
//        InputStreamReader reader = null;
//        try {
//            URL url = new URL("https://api.ashcon.app/mojang/v2/user/" + playerName);
//            in = url.openStream();
//            reader = new InputStreamReader(in);
//            scanner = new Scanner(reader);
//            boolean b = false;
//            boolean b2 = false;
//            while (scanner.hasNextLine()) {
//                String line = scanner.next();
//                if (b) {
//                    skinUrl = line.substring(1, line.length()-2);
//                    break;
//                }
//                if (line.contains("\"skin\":")) {
//                    b2 = true;
//                }
//                if (line.contains("\"code\":")) {
//                    break;
//                }
//                if (line.contains("\"url\":")) {
//                    if (b2) {
//                        b = true;
//                    }
//                }
//            }
//        } catch (IOException ignore) {
//            //empty
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        CloseableUtils.closeQuietly(scanner);
//        CloseableUtils.closeQuietly(in);
//        CloseableUtils.closeQuietly(reader);
//        return skinUrl;
//    }
//
//    @Nullable
//    public static String getCapeURL(@NotNull String playerName) {
//        Objects.requireNonNull(playerName);
//        String capeUrl = null;
//        Scanner scanner = null;
//        InputStream in = null;
//        InputStreamReader reader = null;
//        try {
//            URL url = new URL("https://api.ashcon.app/mojang/v2/user/" + playerName);
//            in = url.openStream();
//            reader = new InputStreamReader(in);
//            scanner = new Scanner(reader);
//            boolean b = false;
//            boolean b2 = false;
//            while (scanner.hasNextLine()) {
//                String line = scanner.next();
//                if (b) {
//                    capeUrl = line.substring(1, line.length()-2);
//                    break;
//                }
//                if (line.contains("\"cape\":")) {
//                    b2 = true;
//                }
//                if (line.contains("\"code\":")) {
//                    break;
//                }
//                if (line.contains("\"url\":")) {
//                    if (b2) {
//                        b = true;
//                    }
//                }
//            }
//        } catch (IOException ignore) {
//            //empty
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        CloseableUtils.closeQuietly(scanner);
//        CloseableUtils.closeQuietly(in);
//        CloseableUtils.closeQuietly(reader);
//        return capeUrl;
//    }
//
//    public static boolean hasSlimSkin(@NotNull String playerName) {
//        Objects.requireNonNull(playerName);
//        boolean slim = false;
//        Scanner scanner = null;
//        InputStream in = null;
//        InputStreamReader reader = null;
//        try {
//            URL url = new URL("https://api.ashcon.app/mojang/v2/user/" + playerName);
//            in = url.openStream();
//            reader = new InputStreamReader(in);
//            scanner = new Scanner(reader);
//            boolean b = false;
//            boolean b2 = false;
//            while (scanner.hasNextLine()) {
//                String line = scanner.next();
//                if (b) {
//                    String slimString = line.substring(1, line.length()-2);
//                    if (slimString.equalsIgnoreCase("true")) {
//                        slim = true;
//                    }
//                    break;
//                }
//                if (line.contains("\"textures\":")) {
//                    b2 = true;
//                }
//                if (line.contains("\"code\":")) {
//                    break;
//                }
//                if (line.contains("\"slim\":")) {
//                    if (b2) {
//                        b = true;
//                    }
//                }
//            }
//        } catch (IOException ignore) {
//            //empty
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        CloseableUtils.closeQuietly(scanner);
//        CloseableUtils.closeQuietly(in);
//        CloseableUtils.closeQuietly(reader);
//        return slim;
//    }
//
//}
