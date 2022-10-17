//---
package de.keksuccino.fancymenu.menu.placeholders;

import de.keksuccino.fancymenu.api.placeholder.PlaceholderTextContainer;
import de.keksuccino.fancymenu.menu.fancy.helper.MenuReloadedEvent;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.json.JsonUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.web.WebUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonPlaceholder extends PlaceholderTextContainer {

    private static final Logger LOGGER = LogManager.getLogger("fancymenu/JsonPlaceholder");

    protected static volatile Map<String, List<String>> cachedPlaceholders = new HashMap<>();
    protected static volatile List<String> currentlyUpdatingPlaceholders = new ArrayList<>();
    protected static volatile List<String> invalidWebPlaceholderLinks = new ArrayList<>();

    protected static boolean eventsRegistered = false;

    public JsonPlaceholder() {
        super("fancymenu_placeholder_json");
        if (!eventsRegistered) {
            MinecraftForge.EVENT_BUS.register(JsonPlaceholder.class);
            eventsRegistered = true;
        }
    }

    @SubscribeEvent
    public static void onReload(MenuReloadedEvent e) {
        try {
            cachedPlaceholders.clear();
            invalidWebPlaceholderLinks.clear();
            LOGGER.info("JsonPlaceholder cache successfully cleared!");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public String replacePlaceholders(String rawIn) {

        String s = rawIn;

        try {

            if (s.contains("%json:")) {
                List<List<String>> placeholders = getJsonPlaceholders(rawIn);
                for (List<String> l : placeholders) {
                    String raw = l.get(0);
                    String link = l.get(1);
                    String jsonPath = l.get(2);
                    File f = new File(link);
                    if (f.isFile()) {
                        List<String> json = JsonUtils.getJsonValueByPath(f, jsonPath);
                        if (json != null) {
                            s = replace(s, raw, json);
                        }
                    } else {
                        if (!isInvalidWebPlaceholderLink(link)) {
                            List<String> json = getCachedWebPlaceholder(raw);
                            if (json != null) {
                                s = replace(s, raw, json);
                            } else {
                                if (!isWebPlaceholderUpdating(raw)) {
                                    cacheWebPlaceholder(raw, link, jsonPath);
                                }
                                s = s.replace(raw, "");
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return s;

    }

    protected static String replace(String s, String raw, List<String> json) {
        if (!json.isEmpty()) {
            if (json.size() == 1) {
                s = s.replace(raw, json.get(0));
            } else {
                String rep = "";
                for (String s2 : json) {
                    if (rep.length() == 0) {
                        rep += s2;
                    } else {
                        rep += "%n%" + s2;
                    }
                }
                s = s.replace(raw, rep);
            }
        }
        return s;
    }

    protected static boolean isInvalidWebPlaceholderLink(String link) {
        try {
            return invalidWebPlaceholderLinks.contains(link);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    protected static List<String> getCachedWebPlaceholder(String placeholder) {
        try {
            return cachedPlaceholders.get(placeholder);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    protected static boolean isWebPlaceholderUpdating(String placeholder) {
        try {
            return currentlyUpdatingPlaceholders.contains(placeholder);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    protected static void cacheWebPlaceholder(String placeholder, String link, String jsonPath) {
        try {
            if (!currentlyUpdatingPlaceholders.contains(placeholder)) {
                currentlyUpdatingPlaceholders.add(placeholder);
                new Thread(() -> {
                    try {
                        if (WebUtils.isValidUrl(link)) {
                            cachedPlaceholders.put(placeholder, JsonUtils.getJsonValueByPath(getJsonStringFromURL(link), jsonPath));
                        } else {
                            invalidWebPlaceholderLinks.add(link);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        currentlyUpdatingPlaceholders.remove(placeholder);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected static String getJsonStringFromURL(String url) {
        BufferedReader in = null;
        StringBuilder contentBuilder = new StringBuilder();
        try {
            URL u = new URL(url);
            URLConnection con = u.openConnection();
            con.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 5.1; rv:19.0) Gecko/20100101 Firefox/19.0");
            in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                contentBuilder.append(line);
            }
            in.close();
            String html = contentBuilder.toString();
            return html;
        } catch (Exception e) {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
            e.printStackTrace();
        }
        return null;
    }

    protected static List<List<String>> getJsonPlaceholders(String rawIn) {
        List<List<String>> l = new ArrayList<>();
        if (rawIn.contains("%json:")) {
            for (String s : rawIn.split("%json:")) {
                if (s.contains(";path:")) {
                    String link = s.split(";", 2)[0];
                    String jsonPath = s.split(";", 2)[1].replace("path:", "");
                    if (jsonPath.contains("%")) {
                        jsonPath = jsonPath.split("%", 2)[0];
                    }
                    String rawPlaceholder = "%json:" + link + ";path:" + jsonPath + "%";
                    List<String> values = new ArrayList<>();
                    values.add(rawPlaceholder);
                    values.add(link);
                    values.add(jsonPath);
                    l.add(values);
                }
            }
        }
        return l;
    }

    @Override
    public String getPlaceholder() {
        return "%json:<link_or_path_to_JSON_file>;path:<JSON_path>%";
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.placeholder.json");
    }

    @Override
    public String[] getDescription() {
        return StringUtils.splitLines(Locals.localize("fancymenu.helper.placeholder.json.desc"), "%n%");
    }

}
