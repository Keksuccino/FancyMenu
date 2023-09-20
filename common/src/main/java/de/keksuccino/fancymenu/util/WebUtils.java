package de.keksuccino.fancymenu.util;

import net.minecraft.client.Minecraft;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class WebUtils {

    public static boolean isValidUrl(String url) {
        if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
            try {
                URL u = new URL(url);
                HttpURLConnection c = (HttpURLConnection)u.openConnection();
                c.addRequestProperty("User-Agent", "Mozilla/4.0");
                c.setRequestMethod("HEAD");
                int r = c.getResponseCode();
                if (r == 200) {
                    return true;
                }
            } catch (Exception ex) {
                try {
                    URL u = new URL(url);
                    HttpURLConnection c = (HttpURLConnection)u.openConnection();
                    c.addRequestProperty("User-Agent", "Mozilla/4.0");
                    int r = c.getResponseCode();
                    if (r == 200) {
                        return true;
                    }
                } catch (Exception ignored) {}
            }

        }
        return false;
    }

    public static void openWebLink(String url) {
        try {
            String s = System.getProperty("os.name").toLowerCase(Locale.ROOT);
            URL u = new URL(url);
            if (!Minecraft.ON_OSX) {
                if (s.contains("win")) {
                    Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
                } else {
                    if (u.getProtocol().equals("file")) {
                        url = url.replace("file:", "file://");
                    }
                    Runtime.getRuntime().exec(new String[]{"xdg-open", url});
                }
            } else {
                Runtime.getRuntime().exec(new String[]{"open", url});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
