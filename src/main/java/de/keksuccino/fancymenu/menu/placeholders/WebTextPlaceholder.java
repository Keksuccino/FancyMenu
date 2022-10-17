//---
package de.keksuccino.fancymenu.menu.placeholders;

import de.keksuccino.fancymenu.api.placeholder.PlaceholderTextContainer;
import de.keksuccino.fancymenu.menu.fancy.helper.MenuReloadedEvent;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.web.WebUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebTextPlaceholder extends PlaceholderTextContainer {

    private static final Logger LOGGER = LogManager.getLogger("fancymenu/WebTextPlaceholder");

    protected static volatile Map<String, List<String>> cachedPlaceholders = new HashMap<>();
    protected static volatile List<String> currentlyUpdatingPlaceholders = new ArrayList<>();
    protected static volatile List<String> invalidWebPlaceholderLinks = new ArrayList<>();

    protected static boolean eventsRegistered = false;

    public WebTextPlaceholder() {
        super("fancymenu_placeholder_web_text");
        if (!eventsRegistered) {
            MinecraftForge.EVENT_BUS.register(WebTextPlaceholder.class);
            eventsRegistered = true;
        }
    }

    @SubscribeEvent
    public static void onReload(MenuReloadedEvent e) {
        try {
            cachedPlaceholders.clear();
            invalidWebPlaceholderLinks.clear();
            LOGGER.info("WebTextPlaceholder cache successfully cleared!");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public String replacePlaceholders(String rawIn) {

        String s = rawIn;

        List<String> l = getPlaceholdersWithValue(s, "%webtext:");
        for (String s2 : l) {
            if (s2.contains(":")) {
                String blank = getPlaceholderWithoutPercentPrefixSuffix(s2);
                String link = blank.split("[:]", 2)[1];
                if (!isInvalidWebPlaceholderLink(link)) {
                    List<String> lines = getCachedWebPlaceholder(s2);
                    if (lines != null) {
                        if (!lines.isEmpty()) {
                            s = s.replace(s2, lines.get(0));
                        }
                    } else {
                        if (!isWebPlaceholderUpdating(s2)) {
                            cacheWebPlaceholder(s2, link);
                        }
                        s = s.replace(s2, "");
                    }
                }
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

    protected static void cacheWebPlaceholder(String placeholder, String link) {
        try {
            if (!currentlyUpdatingPlaceholders.contains(placeholder)) {
                currentlyUpdatingPlaceholders.add(placeholder);
                new Thread(() -> {
                    try {
                        if (WebUtils.isValidUrl(link)) {
                            cachedPlaceholders.put(placeholder, WebUtils.getPlainTextContentOfPage(new URL(link)));
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

    @Override
    public String getPlaceholder() {
        return "%webtext:<link_to_text>%";
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.placeholder.webtext");
    }

    @Override
    public String[] getDescription() {
        return StringUtils.splitLines(Locals.localize("fancymenu.helper.placeholder.webtext.desc"), "%n%");
    }

}
