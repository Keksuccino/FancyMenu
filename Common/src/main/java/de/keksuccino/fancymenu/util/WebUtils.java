package de.keksuccino.fancymenu.util;

import java.net.HttpURLConnection;
import java.net.URL;

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

}
