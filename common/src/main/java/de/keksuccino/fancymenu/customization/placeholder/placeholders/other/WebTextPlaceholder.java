package de.keksuccino.fancymenu.customization.placeholder.placeholders.other;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.events.ModReloadEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.client.resources.language.I18n;
import de.keksuccino.konkrete.web.WebUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.*;

public class WebTextPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static volatile Map<String, List<String>> cachedPlaceholders = new HashMap<>();
    protected static volatile List<String> currentlyUpdatingPlaceholders = new ArrayList<>();
    protected static volatile List<String> invalidWebPlaceholderLinks = new ArrayList<>();

    protected static boolean eventsRegistered = false;

    public WebTextPlaceholder() {
        super("webtext");
        if (!eventsRegistered) {
            EventHandler.INSTANCE.registerListenersOf(WebTextPlaceholder.class);
            eventsRegistered = true;
        }
    }

    @EventListener
    public static void onReload(ModReloadEvent e) {
        try {
            cachedPlaceholders.clear();
            invalidWebPlaceholderLinks.clear();
            LOGGER.info("[FANCYMENU] V2 WebTextPlaceholder cache successfully cleared!");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String link = dps.values.get("link");
        if (link != null) {
            link = StringUtils.convertFormatCodes(link, "§", "&");
            if (!isInvalidWebPlaceholderLink(link)) {
                List<String> lines = getCachedWebPlaceholder(dps.placeholderString);
                if (lines != null) {
                    if (!lines.isEmpty()) {
                        return lines.get(0);
                    }
                } else {
                    if (!isWebPlaceholderUpdating(dps.placeholderString)) {
                        cacheWebPlaceholder(dps.placeholderString, link);
                    }
                    return "";
                }
            }
        }
        return null;
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
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("link");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.helper.placeholder.webtext");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.helper.placeholder.webtext.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.fancymenu.editor.dynamicvariabletextfield.categories.other");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholderIdentifier = this.getIdentifier();
        dps.values.put("link", "http://somewebsite.com/textfile.txt");
        return dps;
    }

}
